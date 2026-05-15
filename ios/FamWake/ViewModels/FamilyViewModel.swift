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

    /// Nach Account-Wechsel (anon → Google/Email): alte Daten löschen, neue laden
    func reloadForNewUser() {
        stopSyncJobs()
        clearFamilyLocally()
        members = []
        schedule = nil
        errorMessage = nil
        Task { await restoreUserContextIfNeeded() }
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

    var isAwakeButtonVisible: Bool {
        guard isAlarmEnabled, let myId = myMemberId, let myMember = members.first(where: { $0.id == myId }) else { return false }
        if isAwakeTodayLocal { return true }
        
        let cal = Calendar.current
        let now = Date()
        let nowMinutes = cal.component(.hour, from: now) * 60 + cal.component(.minute, from: now)
        let weekdayRaw = cal.component(.weekday, from: now)
        let todayDow = weekdayRaw == 1 ? 7 : weekdayRaw - 1
        
        var offsetDays: Int? = nil
        var targetProfile: DayProfile? = nil
        
        for offset in 0..<7 {
            let checkDow = ((todayDow - 1 + offset) % 7) + 1
            if let p = myMember.dayProfiles?[checkDow], p.isActive {
                if offset == 0 {
                    if nowMinutes < p.latestWakeUp.totalMinutes {
                        offsetDays = offset
                        targetProfile = p
                        break
                    }
                } else {
                    offsetDays = offset
                    targetProfile = p
                    break
                }
            }
        }
        
        guard let offset = offsetDays, let profile = targetProfile else { return false }
        
        let myScheduledTime = schedule?.memberSchedules.first(where: { $0.id == myId })?.wakeUpTime
        let alarmTime = myScheduledTime ?? profile.earliestWakeUp
        
        let startOfToday = cal.startOfDay(for: now)
        guard let targetDayDate = cal.date(byAdding: .day, value: offset, to: startOfToday) else { return false }
        guard let targetDate = cal.date(bySettingHour: alarmTime.hour ?? 0, minute: alarmTime.minute ?? 0, second: 0, of: targetDayDate) else { return false }
        
        guard let windowStart = cal.date(byAdding: .hour, value: -2, to: targetDate) else { return false }
        return now >= windowStart && now < targetDate
    }

    func deleteFamily(completion: @escaping (Bool) -> Void) {
        guard let fid = familyId, isAdmin else { completion(false); return }
        stopSyncJobs()
        Task {
            do {
                try await functions.httpsCallable("deleteFamily").call(["familyId": fid])
                clearFamilyLocally()
                familyListener?.remove()
                membersListener?.remove()
                members = []
                schedule = nil
                completion(true)
            } catch {
                await MainActor.run {
                    errorMessage = mapFirebaseError(error)
                }
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
                let data = updatedMember.toFirestoreMap()
                try await db.collection("families").document(fid)
                    .collection("members").document(updatedMember.id)
                    .setData(data)
                
                await MainActor.run {
                    if shouldClaim {
                        myMemberId = updatedMember.id
                        UserDefaults.standard.set(updatedMember.id, forKey: "my_member_id")
                    }
                    isSyncing = false
                }
            } catch {
                await MainActor.run {
                    errorMessage = mapFirebaseError(error)
                    isSyncing = false
                }
            }
        }
    }

    func deleteMember(_ memberId: String) {
        guard let fid = familyId else { return }
        // Prüfe ob das gelöschte Profil das eigene war
        let wasMyMember = (memberId == myMemberId)
        Task {
            do {
                try await db.collection("families").document(fid).collection("members").document(memberId).delete()
                await MainActor.run {
                    members.removeAll { $0.id == memberId }
                    if wasMyMember {
                        myMemberId = nil
                        UserDefaults.standard.removeObject(forKey: "my_member_id")
                        isAlarmEnabled = false
                        UserDefaults.standard.set(false, forKey: "alarm_enabled")
                    }
                    recalculateSchedule()
                }
            } catch {
                await MainActor.run {
                    errorMessage = mapFirebaseError(error)
                }
            }
        }
    }

    func togglePauseMember(_ memberId: String) {
        guard let fid = familyId else { return }
        guard let member = members.first(where: { $0.id == memberId }) else { return }
        
        // Nur unclaimed Member dürfen pausiert werden.
        // Geclaimte Member (auch das eigene Profil) sind nicht pausierbar.
        if member.claimedByUserId != nil { return }
        
        let newPausedState = !member.isPaused
        
        // Lokales Update für sofortiges Feedback
        if let idx = members.firstIndex(where: { $0.id == memberId }) {
            members[idx].isPaused = newPausedState
            recalculateSchedule()
        }
        
        Task {
            do {
                try await db.collection("families").document(fid).collection("members").document(memberId)
                    .updateData(["isPaused": newPausedState])
            } catch {
                await MainActor.run {
                    errorMessage = mapFirebaseError(error)
                    // Rollback
                    if let idx = members.firstIndex(where: { $0.id == memberId }) {
                        members[idx].isPaused = !newPausedState
                        recalculateSchedule()
                    }
                }
            }
        }
    }

    func toggleAwakeMember(_ memberId: String) {
        guard let fid = familyId else { return }
        let newValue = !isAwakeTodayLocal
        isAwakeTodayLocal = newValue
        Task {
            do {
                try await db.collection("families").document(fid).collection("members").document(memberId)
                    .updateData(["isAwakeToday": newValue])
            } catch {
                await MainActor.run {
                    isAwakeTodayLocal = !newValue
                    errorMessage = mapFirebaseError(error)
                }
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

    func setMyMemberId(_ memberId: String, force: Bool = false, completion: @escaping (Bool) -> Void) {
        guard let fid = familyId, let uid = Auth.auth().currentUser?.uid else {
            completion(false); return
        }
        let userName = Auth.auth().currentUser?.displayName ?? L.s("settings_fallback_username")
        let deviceId = UIDevice.current.identifierForVendor?.uuidString ?? UUID().uuidString
        errorMessage = nil
        
        Task {
            do {
                let docRef = db.collection("families").document(fid).collection("members").document(memberId)
                let success = try await db.runTransaction({ (transaction, errorPointer) -> Any? in
                    let snapshot: DocumentSnapshot
                    do {
                        try snapshot = transaction.getDocument(docRef)
                    } catch let error as NSError {
                        errorPointer?.pointee = error
                        return false
                    }
                    
                    let existingClaim = snapshot.data()?["claimedByUserId"] as? String
                    if force || existingClaim == nil || existingClaim == uid {
                        transaction.updateData([
                            "claimedByUserId": uid,
                            "claimedByUserName": userName,
                            "claimedByDeviceId": deviceId,
                            "deviceAlarmEnabled": true,
                            "lastUpdatedAt": FieldValue.serverTimestamp()
                        ], forDocument: docRef)
                        return true
                    } else {
                        let err = NSError(domain: "FamWake", code: 409, userInfo: [NSLocalizedDescriptionKey: "profile_taken"])
                        errorPointer?.pointee = err
                        return false
                    }
                }) as? Bool ?? false
                
                if success {
                    await MainActor.run {
                        self.myMemberId = memberId
                        UserDefaults.standard.set(memberId, forKey: "my_member_id")
                        self.recalculateSchedule()
                    }
                    completion(true)
                } else {
                    await MainActor.run {
                        self.errorMessage = L.errorProfileTaken
                    }
                    completion(false)
                }
            } catch {
                await MainActor.run {
                    let msg = error.localizedDescription.lowercased()
                    if msg.contains("profile_taken") || msg.contains("already-exists") {
                        self.errorMessage = L.errorProfileTaken
                    } else {
                        self.errorMessage = self.mapFirebaseError(error)
                    }
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
                try? await db.collection("families").document(fid)
                    .collection("members").document(member.id)
                    .updateData(["sequenceOrder": idx])
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
        return "\(L.errorGeneric) (\(error.localizedDescription))"
    }

    // MARK: - Feedback
    func sendFeedback(category: String, message: String, email: String, appVersion: String, device: String) {
        isSendingFeedback = true
        feedbackError = nil
        let data: [String: Any] = [
            "category": category,
            "message": message.trimmingCharacters(in: .whitespacesAndNewlines),
            "email": email.trimmingCharacters(in: .whitespacesAndNewlines),
            "appVersion": appVersion,
            "device": device
        ]
        Task {
            do {
                try await functions.httpsCallable("sendFeedbackEmail").call(data)
                feedbackSubmitted = true
            } catch {
                feedbackError = mapFirebaseError(error)
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
            guard let self else { return }
            guard let data = snap?.data() else {
                // Family does not exist or was deleted -> kick user out
                self.leaveFamily()
                return
            }
            self.familyName = data["name"] as? String
            self.joinCode = data["joinCode"] as? String
            let uid = Auth.auth().currentUser?.uid
            self.isAdmin = (data["createdByUserId"] as? String) == uid
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
                let parsed = docs.compactMap { FamilyMember.fromFirestore($0.data(), id: $0.documentID) }
                self.members = parsed.sorted { $0.sequenceOrder < $1.sequenceOrder }
                self.updateAwakeState()
                
                // Claim-Sync: Prüfe ob eigenes Profil noch gültig ist (Android FamilyViewModel.kt:251-283)
                if let uid = Auth.auth().currentUser?.uid {
                    let deviceId = UIDevice.current.identifierForVendor?.uuidString
                    let claimedByMe = self.members.first { m in
                        m.claimedByUserId == uid && (m.claimedByDeviceId == deviceId || m.claimedByDeviceId == nil)
                    }
                    if let claimed = claimedByMe, claimed.id != self.myMemberId {
                        // Anderes Gerät hat unser Profil auf ein anderes Member umgeclaimt
                        self.myMemberId = claimed.id
                        UserDefaults.standard.set(claimed.id, forKey: "my_member_id")
                    } else if claimedByMe == nil && self.myMemberId != nil && !self.members.isEmpty {
                        // Profil wurde von einem anderen Gerät "gestohlen"
                        self.myMemberId = nil
                        UserDefaults.standard.removeObject(forKey: "my_member_id")
                        self.isAlarmEnabled = false
                        UserDefaults.standard.set(false, forKey: "alarm_enabled")
                    }
                }
                
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
        #if targetEnvironment(simulator)
        return true // Simulator-Fix: Bypasses network weirdness
        #else
        guard let url = URL(string: "https://captive.apple.com") else { return true }
        var request = URLRequest(url: url)
        request.timeoutInterval = 3
        request.httpMethod = "GET"
        request.cachePolicy = .reloadIgnoringLocalAndRemoteCacheData
        do {
            let (_, response) = try await URLSession.shared.data(for: request)
            return (response as? HTTPURLResponse)?.statusCode == 200
        } catch {
            return false
        }
        #endif
    }

    private func generateJoinCode() -> String {
        let chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return String((0..<6).map { _ in chars.randomElement()! })
    }

    // MARK: - Firestore Mapping
    // Moved to Data/FirestoreMapper.swift (FamilyMember.fromFirestore / .toFirestoreMap)

    // MARK: - Schedule Calculation
    func recalculateSchedule() {
        let resolved = members.map { resolveEffectiveMember($0) }
        let result = Scheduler().calculateIdealSchedule(members: resolved)
        schedule = result

        if isAlarmEnabled {
            AlarmService.shared.scheduleAlarms(for: members, myMemberId: myMemberId, schedule: schedule)
        }
    }

    func applyAutoFix() {
        guard let currentSchedule = schedule, !currentSchedule.isValid else { return }

        Task {
            var updatedMembersMap = [String: FamilyMember]()

            let cal = Calendar.current
            let now = Date()
            let nowComps = cal.dateComponents([.hour, .minute], from: now)
            let nowMinutes = (nowComps.hour ?? 0) * 60 + (nowComps.minute ?? 0)
            let weekdayRaw = cal.component(.weekday, from: now)
            let todayDow = weekdayRaw == 1 ? 7 : weekdayRaw - 1

            for s in currentSchedule.memberSchedules {
                guard let originalMember = self.members.first(where: { $0.id == s.member.id }) else { continue }
                var member = originalMember

                let todayProfile = member.dayProfiles?[todayDow]
                let latestMinutesToday = (todayProfile?.latestWakeUp.hour ?? 0) * 60 + (todayProfile?.latestWakeUp.minute ?? 0)
                let useToday = todayProfile?.isActive == true && nowMinutes < latestMinutesToday
                let targetDow = useToday ? todayDow : (todayDow == 7 ? 1 : todayDow + 1)

                // Update DayProfile if it exists
                var updatedProfiles = member.dayProfiles ?? [:]
                if let currentProfile = updatedProfiles[targetDow] {
                    var newEarliest = currentProfile.earliestWakeUp
                    var newLatest = currentProfile.latestWakeUp

                    if s.wakeUpTime < newEarliest {
                        newEarliest = s.wakeUpTime
                    }
                    if newLatest < s.wakeUpTime {
                        newLatest = s.wakeUpTime
                    }

                    var newProfile = currentProfile
                    newProfile.earliestWakeUp = newEarliest
                    newProfile.latestWakeUp = newLatest
                    updatedProfiles[targetDow] = newProfile
                }

                // Update Top-level times
                var newTopEarliest = member.earliestWakeUp
                var newTopLatest = member.latestWakeUp

                if s.wakeUpTime < newTopEarliest {
                    newTopEarliest = s.wakeUpTime
                }
                if newTopLatest < s.wakeUpTime {
                    newTopLatest = s.wakeUpTime
                }

                member.earliestWakeUp = newTopEarliest
                member.latestWakeUp = newTopLatest
                member.dayProfiles = updatedProfiles

                updatedMembersMap[member.id] = member
            }

            // Local update for immediate feedback
            var newMembers = self.members
            for i in 0..<newMembers.count {
                if let updated = updatedMembersMap[newMembers[i].id] {
                    newMembers[i] = updated
                }
            }
            
            await MainActor.run {
                self.members = newMembers
                self.recalculateSchedule()
            }

            // Save to Firestore directly
            for (_, updatedMember) in updatedMembersMap {
                self.addOrUpdateMember(updatedMember)
            }
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

        let todayProfile = member.dayProfiles?[todayDow]
        let latestMinutesToday = (todayProfile?.latestWakeUp.hour ?? 0) * 60 + (todayProfile?.latestWakeUp.minute ?? 0)

        // Heute noch relevant, wenn aktives Profil und latest noch nicht vorbei
        let useToday = todayProfile?.isActive == true && nowMinutes < latestMinutesToday

        let targetDow: Int
        if useToday {
            targetDow = todayDow
        } else {
            targetDow = todayDow == 7 ? 1 : todayDow + 1
        }

        guard let profile = member.dayProfiles?[targetDow] else {
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
