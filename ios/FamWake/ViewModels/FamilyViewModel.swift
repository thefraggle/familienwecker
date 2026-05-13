import Foundation
import Combine
import FirebaseFirestore
import FirebaseAuth
import FirebaseFunctions

/// Äquivalent zu FamilyViewModel.kt (aufgeteilt in Extensions)
@MainActor
class FamilyViewModel: ObservableObject {
    // MARK: - Published State
    @Published var members: [FamilyMember] = []
    @Published var schedule: FamilySchedule? = nil
    @Published var familyId: String? = nil
    @Published var familyName: String? = nil
    @Published var joinCode: String? = nil
    @Published var myMemberId: String? = UserDefaults.standard.string(forKey: "my_member_id")
    @Published var isAlarmEnabled: Bool = UserDefaults.standard.bool(forKey: "alarm_enabled")
    @Published var isAwakeTodayLocal: Bool = false
    @Published var isSyncing: Bool = false
    @Published var isOffline: Bool = false
    @Published var isJoiningFamily: Bool = false
    @Published var errorMessage: String? = nil
    @Published var pendingJoinCode: String? = nil
    @Published var snoozeUntil: Date? = nil
    @Published var alarmSoundUri: String? = UserDefaults.standard.string(forKey: "alarm_sound_uri")
    @Published var themePreference: String = UserDefaults.standard.string(forKey: "theme_preference") ?? "system"
    @Published var language: String = UserDefaults.standard.string(forKey: "language") ?? "system"
    @Published var tooltipsEnabled: Bool = UserDefaults.standard.bool(forKey: "tooltips_enabled")
    @Published var tooltipAwakeSeen: Bool = false
    @Published var tooltipDragSeen: Bool = false
    @Published var tooltipSwitchSeen: Bool = false
    @Published var tooltipInviteSeen: Bool = false
    @Published var tooltipWakeWindowSeen: Bool = false
    @Published var tooltipBathroomSeen: Bool = false
    @Published var tooltipWeekdaysSeen: Bool = false
    @Published var onboardingCompleted: Bool = UserDefaults.standard.bool(forKey: "onboarding_completed")
    @Published var isAdmin: Bool = false
    @Published var isGlobalAdmin: Bool = false
    @Published var isSendingFeedback: Bool = false
    @Published var feedbackSubmitted: Bool = false
    @Published var feedbackError: String? = nil
    @Published var pendingPauseIds: Set<String> = []

    // MARK: - Tooltip Keys
    let tooltipKeyAwake = "tooltip_awake_seen"
    let tooltipKeyDrag = "tooltip_drag_seen"
    let tooltipKeySwitch = "tooltip_switch_seen"
    let tooltipKeyInvite = "tooltip_invite_seen"
    let tooltipKeyWakeWindow = "tooltip_wake_window_seen"
    let tooltipKeyBathroom = "tooltip_bathroom_seen"
    let tooltipKeyWeekdays = "tooltip_weekdays_seen"

    var hasFamilyId: Bool { familyId != nil }

    private var db = Firestore.firestore()
    private var functions = Functions.functions(region: "europe-west3")
    private var familyListener: ListenerRegistration?
    private var membersListener: ListenerRegistration?

    init() {
        tooltipsEnabled = UserDefaults.standard.object(forKey: "tooltips_enabled") as? Bool ?? true
        loadTooltipStates()
        loadFamilyFromLocal()
        startNetworkMonitor()
        // Auto-Restore: Wenn familyId lokal nicht bekannt, via Cloud Function laden
        Task { await restoreUserContextIfNeeded() }
    }

    deinit {
        familyListener?.remove()
        membersListener?.remove()
    }

    // MARK: - Family Operations
    func createFamily(_ name: String, completion: @escaping (Bool) -> Void) {
        guard let uid = Auth.auth().currentUser?.uid else { completion(false); return }
        errorMessage = nil
        isSyncing = true
        Task {
            do {
                let result = try await functions.httpsCallable("createFamily")
                    .call(["familyName": name, "userId": uid])
                guard let data = result.data as? [String: Any],
                      let familyId = data["familyId"] as? String,
                      let joinCode = data["joinCode"] as? String else {
                    errorMessage = L.errorGeneric
                    completion(false)
                    isSyncing = false
                    return
                }
                let familyName = data["familyName"] as? String ?? name
                saveFamilyLocally(id: familyId, name: familyName, code: joinCode)
                listenToFamily(id: familyId)
                completion(true)
            } catch {
                errorMessage = mapFirebaseError(error)
                completion(false)
            }
            isSyncing = false
        }
    }

    func joinFamily(_ code: String, completion: @escaping (Bool) -> Void) {
        errorMessage = nil
        isJoiningFamily = true
        stopSyncJobs()
        Task {
            do {
                let result = try await functions.httpsCallable("joinFamilyByCode")
                    .call(["code": code.uppercased()])
                guard let data = result.data as? [String: Any],
                      let familyId = data["familyId"] as? String,
                      let joinCode = data["joinCode"] as? String else {
                    errorMessage = L.errorFamilyNotFound
                    isJoiningFamily = false
                    completion(false)
                    return
                }
                let familyName = data["familyName"] as? String ?? ""
                saveFamilyLocally(id: familyId, name: familyName, code: joinCode)
                listenToFamily(id: familyId)
                isJoiningFamily = false
                completion(true)
            } catch {
                errorMessage = mapFirebaseError(error)
                isJoiningFamily = false
                completion(false)
            }
        }
    }

    func leaveFamily() {
        guard let fid = familyId else { clearFamilyLocally(); return }
        let myId = myMemberId
        stopSyncJobs()
        Task {
            var params: [String: Any] = ["familyId": fid]
            if let mid = myId { params["memberId"] = mid }
            try? await functions.httpsCallable("leaveFamily").call(params)
        }
        clearFamilyLocally()
        familyListener?.remove()
        membersListener?.remove()
        members = []
        schedule = nil
    }

    func deleteFamily(completion: @escaping (Bool) -> Void) {
        guard let fid = familyId, isAdmin else { completion(false); return }
        stopSyncJobs()
        Task {
            do {
                try await functions.httpsCallable("deleteFamily").call(["familyId": fid])
                leaveFamily()
                completion(true)
            } catch {
                errorMessage = error.localizedDescription
                completion(false)
            }
        }
    }

    func addOrUpdateMember(_ member: FamilyMember) {
        guard let fid = familyId else { return }
        isSyncing = true
        var updatedMember = member
        let shouldClaim = myMemberId == nil
        if shouldClaim {
            updatedMember.claimedByUserId = Auth.auth().currentUser?.uid
        }
        Task {
            do {
                let data = try memberToFirestore(updatedMember)
                try await functions.httpsCallable("saveMember")
                    .call(["familyId": fid, "memberId": updatedMember.id, "data": data])
                if shouldClaim {
                    myMemberId = updatedMember.id
                    UserDefaults.standard.set(updatedMember.id, forKey: "my_member_id")
                }
            } catch {
                errorMessage = mapFirebaseError(error)
            }
            isSyncing = false
        }
    }

    func deleteMember(_ memberId: String) {
        guard let fid = familyId else { return }
        Task {
            do {
                try await functions.httpsCallable("deleteFamilyMember")
                    .call(["familyId": fid, "memberId": memberId])
            } catch {
                errorMessage = mapFirebaseError(error)
            }
        }
    }

    func toggleAwakeMember(_ memberId: String) {
        guard let fid = familyId else { return }
        let newValue = !isAwakeTodayLocal
        isAwakeTodayLocal = newValue
        Task {
            do {
                try await functions.httpsCallable("saveMember")
                    .call(["familyId": fid, "memberId": memberId, "data": ["isAwakeToday": newValue]])
            } catch {
                isAwakeTodayLocal = !newValue
                errorMessage = mapFirebaseError(error)
            }
        }
    }

    func setAlarmEnabled(_ enabled: Bool) {
        isAlarmEnabled = enabled
        UserDefaults.standard.set(enabled, forKey: "alarm_enabled")
        if enabled {
            AlarmService.shared.scheduleAlarms(for: members, myMemberId: myMemberId, schedule: schedule)
        } else {
            AlarmService.shared.cancelAll()
        }
    }

    func setMyMemberId(_ memberId: String, completion: @escaping (Bool) -> Void) {
        guard let fid = familyId, let uid = Auth.auth().currentUser?.uid else {
            completion(false); return
        }
        errorMessage = nil
        Task {
            do {
                try await functions.httpsCallable("claimMember")
                    .call(["familyId": fid, "memberId": memberId])
                myMemberId = memberId
                UserDefaults.standard.set(memberId, forKey: "my_member_id")
                completion(true)
            } catch {
                let msg = error.localizedDescription.lowercased()
                if msg.contains("profile_taken") || msg.contains("already-exists") {
                    errorMessage = L.errorProfileTaken
                } else {
                    errorMessage = mapFirebaseError(error)
                }
                completion(false)
            }
        }
    }

    func moveMemberOrder(_ from: Int, _ to: Int) {
        guard from != to, from >= 0, to < members.count else { return }
        var updated = members
        let item = updated.remove(at: from)
        updated.insert(item, at: to)
        members = updated
        recalculateSchedule()
        saveMemberOrder()
    }

    func saveMemberOrder() {
        guard let fid = familyId else { return }
        Task {
            for (idx, member) in members.enumerated() {
                try? await functions.httpsCallable("saveMember")
                    .call(["familyId": fid, "memberId": member.id, "data": ["order": idx]])
            }
        }
    }

    func cancelSnooze(_ memberId: String) {
        snoozeUntil = nil
        UserDefaults.standard.removeObject(forKey: "snooze_until")
        AlarmService.shared.cancelSnooze(memberId: memberId)
    }

    func setTooltipsEnabled(_ enabled: Bool) {
        tooltipsEnabled = enabled
        UserDefaults.standard.set(enabled, forKey: "tooltips_enabled")
    }

    func markTooltipSeen(_ key: String) {
        UserDefaults.standard.set(true, forKey: key)
        loadTooltipStates()
    }

    func resetAllTooltips() {
        [tooltipKeyAwake, tooltipKeyDrag, tooltipKeySwitch, tooltipKeyInvite,
         tooltipKeyWakeWindow, tooltipKeyBathroom, tooltipKeyWeekdays].forEach {
            UserDefaults.standard.removeObject(forKey: $0)
        }
        loadTooltipStates()
    }

    func setThemePreference(_ theme: String) {
        themePreference = theme
        UserDefaults.standard.set(theme, forKey: "theme_preference")
    }

    func setLanguage(_ lang: String) {
        language = lang
        UserDefaults.standard.set(lang, forKey: "language")
    }

    func setAlarmSoundUri(_ uri: String?) {
        alarmSoundUri = uri
        UserDefaults.standard.set(uri, forKey: "alarm_sound_uri")
    }

    func clearErrorMessage() { errorMessage = nil }
    func clearError() { errorMessage = nil }

    func clearPendingJoinCode() { pendingJoinCode = nil }

    // MARK: - Error Mapping
    private func mapFirebaseError(_ error: Error) -> String {
        let msg = error.localizedDescription.lowercased()
        if msg.contains("family_not_found") || msg.contains("not-found") || msg.contains("not found") {
            return L.errorFamilyNotFound
        }
        if msg.contains("invalid-argument") || msg.contains("invalid_code") {
            return L.errorInvalidCode
        }
        if msg.contains("network") || msg.contains("offline") || msg.contains("internet") {
            return L.errorNetwork
        }
        return L.errorGeneric
    }

    // MARK: - Feedback
    func sendFeedback(category: String, message: String, email: String, appVersion: String, device: String) {
        isSendingFeedback = true
        feedbackError = nil
        let data: [String: Any] = [
            "category": category,
            "message": message,
            "email": email,
            "appVersion": appVersion,
            "device": device,
            "platform": "iOS",
            "timestamp": FieldValue.serverTimestamp()
        ]
        Task {
            do {
                try await db.collection("feedback").addDocument(data: data)
                feedbackSubmitted = true
            } catch {
                feedbackError = error.localizedDescription
            }
            isSendingFeedback = false
        }
    }

    func resetFeedbackState() {
        feedbackSubmitted = false
        feedbackError = nil
    }

    // MARK: - Firestore Listener
    private func stopSyncJobs() {
        familyListener?.remove()
        membersListener?.remove()
    }

    private func listenToFamily(id: String) {
        self.familyId = id
        familyListener?.remove()
        familyListener = db.collection("families").document(id).addSnapshotListener { [weak self] snap, _ in
            guard let self, let data = snap?.data() else { return }
            self.familyName = data["name"] as? String
            self.joinCode = data["joinCode"] as? String
            let uid = Auth.auth().currentUser?.uid
            self.isAdmin = (data["adminId"] as? String) == uid
        }
        listenToMembers(familyId: id)
    }

    private func listenToMembers(familyId: String) {
        membersListener?.remove()
        membersListener = db.collection("families/\(familyId)/members")
            .addSnapshotListener { [weak self] snap, error in
                guard let self else { return }
                if let err = error as NSError?, err.domain == FirestoreErrorDomain, err.code == FirestoreErrorCode.permissionDenied.rawValue {
                    // Suppress error during transition
                    return
                }
                guard let docs = snap?.documents else { return }
                let parsed = docs.compactMap { try? self.firestoreToMember($0.data(), id: $0.documentID) }
                self.members = parsed.sorted { $0.order < $1.order }
                self.updateAwakeState()
                self.recalculateSchedule()
            }
    }

    private func updateAwakeState() {
        guard let mid = myMemberId else { return }
        isAwakeTodayLocal = members.first(where: { $0.id == mid })?.isAwakeToday ?? false
    }

    var hasFamilyIdPublished: Bool { familyId != nil }

    private func loadFamilyFromLocal() {
        if let fid = UserDefaults.standard.string(forKey: "family_id") {
            listenToFamily(id: fid)
        }
    }

    private func restoreUserContextIfNeeded() async {
        guard familyId == nil else { return }
        guard Auth.auth().currentUser != nil else { return }
        do {
            let result = try await functions.httpsCallable("getUserContext").call([:] as [String: Any])
            guard let data = result.data as? [String: Any],
                  let fid = data["familyId"] as? String,
                  let code = data["joinCode"] as? String else { return }
            let name = data["familyName"] as? String ?? ""
            saveFamilyLocally(id: fid, name: name, code: code)
            listenToFamily(id: fid)
        } catch {
            // Kein Kontext gefunden – User muss Familie erstellen/joinen
        }
    }

    private func saveFamilyLocally(id: String, name: String, code: String) {
        familyId = id
        familyName = name
        joinCode = code
        UserDefaults.standard.set(id, forKey: "family_id")
        UserDefaults.standard.set(name, forKey: "family_name")
        UserDefaults.standard.set(code, forKey: "family_join_code")
    }

    private func clearFamilyLocally() {
        familyId = nil
        familyName = nil
        joinCode = nil
        myMemberId = nil
        UserDefaults.standard.removeObject(forKey: "family_id")
        UserDefaults.standard.removeObject(forKey: "family_name")
        UserDefaults.standard.removeObject(forKey: "family_join_code")
        UserDefaults.standard.removeObject(forKey: "my_member_id")
    }

    private func loadTooltipStates() {
        tooltipAwakeSeen = UserDefaults.standard.bool(forKey: tooltipKeyAwake)
        tooltipDragSeen = UserDefaults.standard.bool(forKey: tooltipKeyDrag)
        tooltipSwitchSeen = UserDefaults.standard.bool(forKey: tooltipKeySwitch)
        tooltipInviteSeen = UserDefaults.standard.bool(forKey: tooltipKeyInvite)
        tooltipWakeWindowSeen = UserDefaults.standard.bool(forKey: tooltipKeyWakeWindow)
        tooltipBathroomSeen = UserDefaults.standard.bool(forKey: tooltipKeyBathroom)
        tooltipWeekdaysSeen = UserDefaults.standard.bool(forKey: tooltipKeyWeekdays)
    }

    private func startNetworkMonitor() {
        // Einfacher Reachability-Check via URLSession
        Task {
            while true {
                try? await Task.sleep(nanoseconds: 10_000_000_000) // 10s
                let reachable = await checkReachability()
                isOffline = !reachable
            }
        }
    }

    private func checkReachability() async -> Bool {
        // Nutze Firebase-Domain statt google.com (vermeidet Corporate-Proxy-Probleme)
        guard let url = URL(string: "https://firebaseio.com") else { return true }
        var request = URLRequest(url: url)
        request.timeoutInterval = 3
        request.httpMethod = "HEAD"
        do {
            let (_, response) = try await URLSession.shared.data(for: request)
            return (response as? HTTPURLResponse)?.statusCode != nil
        } catch {
            return false
        }
    }

    private func generateJoinCode() -> String {
        let chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return String((0..<6).map { _ in chars.randomElement()! })
    }

    // MARK: - Firestore Mapping
    private func memberToFirestore(_ member: FamilyMember) throws -> [String: Any] {
        var data: [String: Any] = [
            "id": member.id,
            "name": member.name,
            "earliestWakeUpHour": member.earliestWakeUp.hour ?? 6,
            "earliestWakeUpMinute": member.earliestWakeUp.minute ?? 0,
            "latestWakeUpHour": member.latestWakeUp.hour ?? 7,
            "latestWakeUpMinute": member.latestWakeUp.minute ?? 30,
            "bathroomDurationMinutes": member.bathroomDurationMinutes,
            "wantsBreakfast": member.wantsBreakfast,
            "isPaused": member.isPaused,
            "order": member.order
        ]
        if let leave = member.leaveHomeTime {
            data["leaveHomeHour"] = leave.hour ?? 8
            data["leaveHomeMinute"] = leave.minute ?? 0
        }
        if let claimed = member.claimedByUserId { data["claimedByUserId"] = claimed }
        if let name = member.claimedByUserName { data["claimedByUserName"] = name }
        // DayProfiles
        var dayProfilesData: [String: Any] = [:]
        for (day, profile) in member.dayProfiles {
            dayProfilesData["\(day)"] = [
                "isActive": profile.isActive,
                "earliestWakeUpHour": profile.earliestWakeUp.hour ?? 6,
                "earliestWakeUpMinute": profile.earliestWakeUp.minute ?? 0,
                "latestWakeUpHour": profile.latestWakeUp.hour ?? 7,
                "latestWakeUpMinute": profile.latestWakeUp.minute ?? 30,
                "bathroomDurationMinutes": profile.bathroomDurationMinutes,
                "wantsBreakfast": profile.wantsBreakfast,
                "leaveHomeHour": profile.leaveHomeTime?.hour ?? 8,
                "leaveHomeMinute": profile.leaveHomeTime?.minute ?? 0
            ]
        }
        data["dayProfiles"] = dayProfilesData
        return data
    }

    private func firestoreToMember(_ data: [String: Any], id: String) throws -> FamilyMember {
        let name = data["name"] as? String ?? ""
        let earliestH = data["earliestWakeUpHour"] as? Int ?? 6
        let earliestM = data["earliestWakeUpMinute"] as? Int ?? 0
        let latestH = data["latestWakeUpHour"] as? Int ?? 7
        let latestM = data["latestWakeUpMinute"] as? Int ?? 30
        let bathroom = data["bathroomDurationMinutes"] as? Int ?? 20
        let breakfast = data["wantsBreakfast"] as? Bool ?? true
        let paused = data["isPaused"] as? Bool ?? false
        let order = data["order"] as? Int ?? 0
        let claimedUid = data["claimedByUserId"] as? String
        let claimedName = data["claimedByUserName"] as? String
        let isAwake = data["isAwakeToday"] as? Bool ?? false

        var leaveHome: DateComponents? = nil
        if let lh = data["leaveHomeHour"] as? Int, let lm = data["leaveHomeMinute"] as? Int {
            leaveHome = DateComponents(hour: lh, minute: lm)
        }

        var dayProfiles: [Int: DayProfile] = DayProfile.defaults()
        if let dpData = data["dayProfiles"] as? [String: [String: Any]] {
            for (key, dp) in dpData {
                if let day = Int(key) {
                    let active = dp["isActive"] as? Bool ?? (day <= 5)
                    let eh = dp["earliestWakeUpHour"] as? Int ?? 6
                    let em = dp["earliestWakeUpMinute"] as? Int ?? 0
                    let lh2 = dp["latestWakeUpHour"] as? Int ?? 7
                    let lm2 = dp["latestWakeUpMinute"] as? Int ?? 30
                    let bat = dp["bathroomDurationMinutes"] as? Int ?? 20
                    let bfst = dp["wantsBreakfast"] as? Bool ?? true
                    var dpLeave: DateComponents? = nil
                    if let dlh = dp["leaveHomeHour"] as? Int, let dlm = dp["leaveHomeMinute"] as? Int {
                        dpLeave = DateComponents(hour: dlh, minute: dlm)
                    }
                    dayProfiles[day] = DayProfile(
                        isActive: active,
                        earliestWakeUp: .from(hour: eh, minute: em),
                        latestWakeUp: .from(hour: lh2, minute: lm2),
                        bathroomDurationMinutes: bat,
                        wantsBreakfast: bfst,
                        leaveHomeTime: dpLeave
                    )
                }
            }
        }

        var member = FamilyMember(
            id: id,
            name: name,
            earliestWakeUp: .from(hour: earliestH, minute: earliestM),
            latestWakeUp: .from(hour: latestH, minute: latestM),
            bathroomDurationMinutes: bathroom,
            wantsBreakfast: breakfast,
            leaveHomeTime: leaveHome,
            isPaused: paused,
            claimedByUserId: claimedUid,
            claimedByUserName: claimedName,
            dayProfiles: dayProfiles,
            order: order
        )
        member.isAwakeToday = isAwake
        return member
    }

    // MARK: - Schedule Calculation
    func recalculateSchedule() {
        let resolved = members.map { resolveEffectiveMember($0) }
        let result = Scheduler().calculateIdealSchedule(members: resolved)
        schedule = result

        if isAlarmEnabled {
            AlarmService.shared.scheduleAlarms(for: members, myMemberId: myMemberId, schedule: schedule)
        }
    }

    /// Ermittelt den aktuell relevanten DayProfile-Slot für einen Member.
    /// Entspricht resolveEffectiveMember() in FamilyViewModel+Alarm.kt
    private func resolveEffectiveMember(_ member: FamilyMember) -> FamilyMember {
        let cal = Calendar.current
        let now = Date()
        let nowComps = cal.dateComponents([.hour, .minute], from: now)
        let nowMinutes = (nowComps.hour ?? 0) * 60 + (nowComps.minute ?? 0)

        // Wochentag: 1=Mo...7=So (wie Android)
        let weekdayRaw = cal.component(.weekday, from: now) // 1=So, 2=Mo...7=Sa
        let todayDow = weekdayRaw == 1 ? 7 : weekdayRaw - 1

        let todayProfile = member.dayProfiles[todayDow]
        let latestMinutesToday = (todayProfile?.latestWakeUp.hour ?? 0) * 60 + (todayProfile?.latestWakeUp.minute ?? 0)

        // Heute noch relevant, wenn aktives Profil und latest noch nicht vorbei
        let useToday = todayProfile?.isActive == true && nowMinutes < latestMinutesToday

        let targetDow: Int
        if useToday {
            targetDow = todayDow
        } else {
            targetDow = todayDow == 7 ? 1 : todayDow + 1
        }

        guard let profile = member.dayProfiles[targetDow] else {
            var paused = member; paused.isPaused = true; return paused
        }
        guard profile.isActive else {
            var paused = member; paused.isPaused = true; return paused
        }

        var resolved = member
        resolved.earliestWakeUp = profile.earliestWakeUp
        resolved.latestWakeUp = profile.latestWakeUp
        resolved.bathroomDurationMinutes = profile.bathroomDurationMinutes
        resolved.wantsBreakfast = profile.wantsBreakfast
        resolved.leaveHomeTime = profile.leaveHomeTime
        return resolved
    }
}

// Extend FamilyMember for runtime isAwakeToday
extension FamilyMember {
    var isAwakeToday: Bool {
        get { _awakeStorage[id] ?? false }
        set { _awakeStorage[id] = newValue }
    }
}

private var _awakeStorage: [String: Bool] = [:]
