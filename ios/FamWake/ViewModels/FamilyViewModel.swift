import Foundation
import Combine
import FirebaseFirestore
import FirebaseAuth
import FirebaseFunctions
import TelemetryClient
import Network

/// Äquivalent zu FamilyViewModel.kt (aufgeteilt in Extensions)
@MainActor
class FamilyViewModel: ObservableObject {
    // MARK: - Published State
    @Published var members: [FamilyMember] = []
    @Published var schedule: FamilySchedule? = nil
    @Published var deviceSchedule: FamilySchedule? = nil
    @Published var selectedDayOfWeek: Int? = nil
    @Published var familyId: String? = nil
    @Published var familyName: String? = nil
    @Published var globalBufferMinutes: Int = 0
    @Published var joinCode: String? = nil
    @Published var myMemberId: String? = UserDefaults.standard.string(forKey: "my_member_id") {
        didSet {
            if let oldValue = oldValue, oldValue != myMemberId {
                AlarmService.shared.cancelWakeUp(memberId: oldValue)
            }
        }
    }
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
    @Published var tooltipAlarmSoundSeen: Bool = false
    @Published var tooltipBufferSeen: Bool = false
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
    let tooltipKeyAlarmSound = "tooltip_alarm_sound_seen"
    let tooltipKeyBuffer = "tooltip_buffer_seen"

    var hasFamilyId: Bool { familyId != nil }
    var isLoggedIn: Bool {
        guard let user = Auth.auth().currentUser else { return false }
        return !user.isAnonymous
    }

    private var db = Firestore.firestore()
    private var functions = Functions.functions(region: "europe-west3")
    private var familyListener: ListenerRegistration?
    private var membersListener: ListenerRegistration?
    private var recalcTimer: AnyCancellable?
    private var pathMonitor: NWPathMonitor?
    private let monitorQueue = DispatchQueue(label: "de.familienwecker.famwake.NetworkMonitorQueue")

    init() {
        tooltipsEnabled = UserDefaults.standard.object(forKey: "tooltips_enabled") as? Bool ?? true
        
        if let snoozeTime = UserDefaults.standard.value(forKey: "snooze_until") as? Double {
            let date = Date(timeIntervalSince1970: snoozeTime)
            if date > Date() {
                self.snoozeUntil = date
            } else {
                UserDefaults.standard.removeObject(forKey: "snooze_until")
            }
        }
        
        loadTooltipStates()
        loadFamilyFromLocal()
        startNetworkMonitor()
        // Auto-Restore: Wenn familyId lokal nicht bekannt, via Cloud Function laden
        Task { await restoreUserContextIfNeeded() }
        
        // Regelmäßiges Recalculate (wie Android scheduleJob), um alte Wecker von der UI zu entfernen
        recalcTimer = Timer.publish(every: 60, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                self?.recalculateSchedule()
                self?.checkAndResetMembers()
            }
    }

    deinit {
        pathMonitor?.cancel()
        familyListener?.remove()
        membersListener?.remove()
        recalcTimer?.cancel()
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
        errorMessage = nil
        isSyncing = true
        Task {
            do {
                let result = try await FamilyFirestoreService.shared.createFamily(name: name)
                await MainActor.run {
                    saveFamilyLocally(id: result.familyId, name: result.familyName, code: result.joinCode)
                    TelemetryManager.send("family.created")
                    listenToFamily(id: result.familyId)
                    completion(true)
                }
            } catch {
                await MainActor.run {
                    errorMessage = (error as NSError).userInfo[NSLocalizedDescriptionKey] as? String ?? L.errorGeneric
                    completion(false)
                }
            }
            await MainActor.run { isSyncing = false }
        }
    }

    func joinFamily(_ code: String, completion: @escaping (Bool) -> Void) {
        errorMessage = nil
        isJoiningFamily = true
        stopSyncJobs()
        Task {
            do {
                let result = try await FamilyFirestoreService.shared.joinFamily(code: code)
                await MainActor.run {
                    saveFamilyLocally(id: result.familyId, name: result.familyName, code: result.joinCode)
                    TelemetryManager.send("family.joined")
                    listenToFamily(id: result.familyId)
                    completion(true)
                }
            } catch {
                await MainActor.run {
                    errorMessage = (error as NSError).userInfo[NSLocalizedDescriptionKey] as? String ?? L.errorGeneric
                    completion(false)
                }
            }
            await MainActor.run { isJoiningFamily = false }
        }
    }
    
    func checkAndResetMembers() {
        guard !members.isEmpty, let familyId = familyId else { return }
        let now = Date()
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        let todayStr = formatter.string(from: now)
        
        let timeFormatter = DateFormatter()
        timeFormatter.dateFormat = "HH:mm"
        
        var toUpdate = [FamilyMember]()
        var localMembersChanged = false
        
        for i in 0..<members.count {
            var member = members[i]
            let latestWakeUp = member.latestWakeUp
            
            // Berechne Schwellenwert (latestWakeUp + 2 Stunden)
            let h = (latestWakeUp.hour ?? 0) + 2
            let m = latestWakeUp.minute ?? 0
            guard let resetThreshold = Calendar.current.date(bySettingHour: h, minute: m, second: 0, of: now) else { continue }
            
            if now > resetThreshold && member.lastResetDate != todayStr {
                let isUnclaimed = member.claimedByUserId == nil
                let newIsPaused = isUnclaimed ? false : member.isPaused
                
                member.isPaused = newIsPaused
                member.isAwakeToday = false
                member.lastResetDate = todayStr
                
                members[i] = member
                toUpdate.append(member)
                localMembersChanged = true
                
                if member.id == myMemberId {
                    UserDefaults.standard.set(false, forKey: "is_awake_today_\(member.id)")
                }
            }
        }
        
        if localMembersChanged {
            recalculateSchedule()
            
            // Auf Firestore synchronisieren
            let batch = db.batch()
            for member in toUpdate {
                let ref = db.collection("families").document(familyId).collection("members").document(member.id)
                var updates: [String: Any] = [
                    "isAwakeToday": false,
                    "lastResetDate": todayStr,
                    "lastUpdatedAt": FieldValue.serverTimestamp()
                ]
                if member.claimedByUserId == nil {
                    updates["isPaused"] = false
                }
                batch.updateData(updates, forDocument: ref)
            }
            batch.commit()
        }
    }

    func leaveFamily() {
        guard let fid = familyId else { clearFamilyLocally(); return }
        let myId = myMemberId
        stopSyncJobs()
        Task {
            try? await FamilyFirestoreService.shared.leaveFamily(familyId: fid, memberId: myId)
        }
        TelemetryManager.send("family.left")
        clearFamilyLocally()
        familyListener?.remove()
        membersListener?.remove()
        members = []
        schedule = nil
    }

    var isAwakeButtonVisible: Bool {
        guard isAlarmEnabled, let myId = myMemberId, let myMember = members.first(where: { $0.id == myId }) else { return false }
        
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
        
        let myScheduledTime = deviceSchedule?.memberSchedules.first(where: { $0.id == myId })?.wakeUpTime
        let alarmTime = myScheduledTime ?? profile.earliestWakeUp
        
        let startOfToday = cal.startOfDay(for: now)
        guard let targetDayDate = cal.date(byAdding: .day, value: offset, to: startOfToday) else { return false }
        guard let targetDate = cal.date(bySettingHour: alarmTime.hour ?? 0, minute: alarmTime.minute ?? 0, second: 0, of: targetDayDate) else { return false }
        
        if isAwakeTodayLocal {
            // Nur anzeigen, wenn der Alarm für HEUTE ist und noch in der Zukunft liegt.
            // Sobald offset > 0 (nächster Alarm ist morgen), verschwindet der Button für heute.
            return offset == 0 && now < targetDate
        }
        
        let isToday = offset == 0
        guard let windowStart = cal.date(byAdding: .hour, value: -4, to: targetDate) else { return false }
        return (isToday || now >= windowStart) && now < targetDate
    }

    func deleteFamily(completion: @escaping (Bool) -> Void) {
        guard let fid = familyId, isAdmin else { completion(false); return }
        stopSyncJobs()
        Task {
            do {
                try await FamilyFirestoreService.shared.deleteFamily(familyId: fid)
                await MainActor.run {
                    TelemetryManager.send("family.deleted")
                    clearFamilyLocally()
                    familyListener?.remove()
                    membersListener?.remove()
                    members = []
                    schedule = nil
                    completion(true)
                }
            } catch {
                await MainActor.run {
                    errorMessage = (error as NSError).userInfo[NSLocalizedDescriptionKey] as? String ?? L.errorGeneric
                    completion(false)
                }
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
            updatedMember.claimedByUserName = Auth.auth().currentUser?.displayName ?? L.s("settings_fallback_username")
            updatedMember.claimedByDeviceId = UIDevice.current.identifierForVendor?.uuidString
            updatedMember.deviceAlarmEnabled = true
        }
        Task {
            await FamilyFirestoreService.shared.trackUserAction(familyId: fid)
            do {
                try await FamilyFirestoreService.shared.addOrUpdateMember(familyId: fid, member: updatedMember)
                await MainActor.run {
                    TelemetryManager.send("member.created")
                    if shouldClaim {
                        myMemberId = updatedMember.id
                        UserDefaults.standard.set(updatedMember.id, forKey: "my_member_id")
                        isAlarmEnabled = true
                        UserDefaults.standard.set(true, forKey: "alarm_enabled")
                    }
                    isSyncing = false
                    self.recalculateSchedule()
                }
            } catch {
                await MainActor.run {
                    errorMessage = (error as NSError).userInfo[NSLocalizedDescriptionKey] as? String ?? L.errorGeneric
                    isSyncing = false
                }
            }
        }
    }

    func deleteMember(_ memberId: String) {
        guard let fid = familyId else { return }
        let wasMyMember = (memberId == myMemberId)
        Task {
            await FamilyFirestoreService.shared.trackUserAction(familyId: fid)
            do {
                try await FamilyFirestoreService.shared.deleteMember(familyId: fid, memberId: memberId)
                await MainActor.run {
                    TelemetryManager.send("member.deleted")
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
                    errorMessage = (error as NSError).userInfo[NSLocalizedDescriptionKey] as? String ?? L.errorGeneric
                }
            }
        }
    }

    func togglePauseMember(_ memberId: String) {
        guard let fid = familyId else { return }
        guard let member = members.first(where: { $0.id == memberId }) else { return }
        if member.claimedByUserId != nil { return }
        let newPausedState = !member.isPaused
        
        if let idx = members.firstIndex(where: { $0.id == memberId }) {
            members[idx].isPaused = newPausedState
            recalculateSchedule()
        }
        
        Task {
            await FamilyFirestoreService.shared.trackUserAction(familyId: fid)
            do {
                try await FamilyFirestoreService.shared.togglePauseMember(familyId: fid, memberId: memberId, newPausedState: newPausedState)
            } catch {
                // NO ROLLBACK: Vertraue auf Firestores Offline-Queue.
            }
        }
    }

    func toggleAwakeMember(_ memberId: String) {
        setAwake(memberId: memberId, awake: !isAwakeTodayLocal)
    }

    func setAwake(memberId: String, awake: Bool) {
        guard let fid = familyId else { return }
        isAwakeTodayLocal = awake
        Task {
            await FamilyFirestoreService.shared.trackUserAction(familyId: fid)
            do {
                try await FamilyFirestoreService.shared.setAwake(familyId: fid, memberId: memberId, awake: awake)
                await MainActor.run {
                    TelemetryManager.send(awake ? "awake.markedAwake" : "awake.reset")
                }
            } catch {
                // NO ROLLBACK: Vertraue auf Firestores Offline-Queue.
            }
        }
    }

    func setAlarmEnabled(_ enabled: Bool) {
        isAlarmEnabled = enabled
        UserDefaults.standard.set(enabled, forKey: "alarm_enabled")
        
        if let mid = myMemberId, let idx = members.firstIndex(where: { $0.id == mid }) {
            members[idx].deviceAlarmEnabled = enabled
        }
        
        recalculateSchedule()
        
        if let fid = familyId, let mid = myMemberId {
            Task {
                if let uid = Auth.auth().currentUser?.uid {
                    try? await db.collection("users").document(uid)
                        .collection("pushMeta").document("user_action")
                        .setData([
                            "familyId": fid,
                            "timestamp": FieldValue.serverTimestamp()
                        ])
                }
                do {
                    try await db.collection("families").document(fid).collection("members").document(mid)
                        .updateData(["deviceAlarmEnabled": enabled])
                } catch {
                    print("Error updating deviceAlarmEnabled: \(error)")
                }
            }
        }
        
        if enabled {
            // applied in recalculateSchedule()
        } else {
            if let myId = myMemberId {
                AlarmService.shared.cancelWakeUp(memberId: myId)
            }
        }
    }

    func setMyMemberId(_ memberId: String?, force: Bool = false, completion: @escaping (Bool) -> Void) {
        guard let fid = familyId, let uid = Auth.auth().currentUser?.uid else {
            completion(false); return
        }
        let userName = Auth.auth().currentUser?.displayName ?? L.s("settings_fallback_username")
        let deviceId = UIDevice.current.identifierForVendor?.uuidString ?? UUID().uuidString
        errorMessage = nil
        
        let currentMyMemberId = self.myMemberId
        
        Task {
            do {
                if let oldId = currentMyMemberId, oldId != memberId {
                    // Unclaim old member in Firestore
                    let oldDocRef = db.collection("families").document(fid).collection("members").document(oldId)
                    _ = try? await db.runTransaction({ (transaction, errorPointer) -> Any? in
                        let snapshot: DocumentSnapshot
                        do {
                            try snapshot = transaction.getDocument(oldDocRef)
                        } catch let error as NSError {
                            errorPointer?.pointee = error
                            return false
                        }
                        let existingClaim = snapshot.data()?["claimedByUserId"] as? String
                        if existingClaim == uid {
                            transaction.updateData([
                                "claimedByUserId": FieldValue.delete(),
                                "claimedByUserName": FieldValue.delete(),
                                "claimedByDeviceId": FieldValue.delete(),
                                "isAwakeToday": false,
                                "lastUpdatedAt": FieldValue.serverTimestamp()
                            ], forDocument: oldDocRef)
                            return true
                        }
                        return false
                    })
                }
                
                if let memberId = memberId {
                    // Claim new member in Firestore
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
                            TelemetryManager.send("member.claimed")
                            self.recalculateSchedule()
                        }
                        completion(true)
                    } else {
                        await MainActor.run {
                            self.errorMessage = L.errorProfileTaken
                        }
                        completion(false)
                    }
                } else {
                    // Just unclaiming
                    await MainActor.run {
                        self.myMemberId = nil
                        UserDefaults.standard.removeObject(forKey: "my_member_id")
                        self.isAlarmEnabled = false
                        UserDefaults.standard.set(false, forKey: "alarm_enabled")
                        self.recalculateSchedule()
                    }
                    completion(true)
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
    func moveMemberOrder(fromIndex: Int, toIndex: Int, wholeWeek: Bool = false) {
        guard let sched = schedule else { return }
        let cal = Calendar.current
        let now = Date()
        let today = cal.startOfDay(for: now)
        
        let targetDate = sched.targetDate ?? today
        let weekdayRaw = cal.component(.weekday, from: targetDate)
        let dayOfWeek = selectedDayOfWeek ?? (weekdayRaw == 1 ? 7 : weekdayRaw - 1)
        
        let visibleIds = sched.memberSchedules.map { $0.member.id }
        guard fromIndex >= 0, fromIndex < visibleIds.count, toIndex >= 0, toIndex <= visibleIds.count else { return }
        
        var targetIds = visibleIds
        let movedId = targetIds.remove(at: fromIndex)
        let insertionIndex = toIndex > fromIndex ? toIndex - 1 : toIndex
        targetIds.insert(movedId, at: insertionIndex)
        
        let updatedMembers = members.map { m -> FamilyMember in
            if let indexInTarget = targetIds.firstIndex(of: m.id) {
                var updatedMember = m
                if wholeWeek {
                    updatedMember.sequenceOrder = indexInTarget
                    var currentProfiles = m.dayProfiles ?? [:]
                    for key in currentProfiles.keys {
                        guard var p = currentProfiles[key] else { continue }
                        p.sequenceOrder = nil
                        currentProfiles[key] = p
                    }
                    updatedMember.dayProfiles = currentProfiles
                } else {
                    var currentProfiles = m.dayProfiles ?? [:]
                    let profile = currentProfiles[dayOfWeek] ?? DayProfile(
                        isActive: !m.isPaused,
                        earliestWakeUp: m.earliestWakeUp,
                        latestWakeUp: m.latestWakeUp,
                        bathroomDurationMinutes: m.bathroomDurationMinutes,
                        wantsBreakfast: m.wantsBreakfast,
                        leaveHomeTime: m.leaveHomeTime,
                        isSimpleMode: m.isSimpleMode
                    )
                    var updatedProfile = profile
                    updatedProfile.sequenceOrder = indexInTarget
                    currentProfiles[dayOfWeek] = updatedProfile
                    updatedMember.dayProfiles = currentProfiles
                }
                return updatedMember
            } else {
                return m
            }
        }
        
        self.members = updatedMembers
        recalculateSchedule()
        saveMemberOrder()
    }

    func saveMemberOrder() {
        guard let fid = familyId else { return }
        let currentUid = Auth.auth().currentUser?.uid
        
        Task {
            if let uid = currentUid {
                try? await db.collection("users").document(uid)
                    .collection("pushMeta").document("reorder")
                    .setData([
                        "familyId": fid,
                        "timestamp": FieldValue.serverTimestamp()
                    ])
                try? await db.collection("users").document(uid)
                    .collection("pushMeta").document("user_action")
                    .setData([
                        "familyId": fid,
                        "timestamp": FieldValue.serverTimestamp()
                    ])
                
                // Legacy Map Field support
                try? await db.collection("users").document(uid)
                    .setData([
                        "pushMeta": [
                            "reorder": [
                                "familyId": fid,
                                "timestamp": FieldValue.serverTimestamp()
                            ]
                        ]
                    ], merge: true)
            }
            
            for member in members {
                let dpData = member.toFirestoreMap()["dayProfiles"]
                var updatePayload: [String: Any] = [
                    "sequenceOrder": member.sequenceOrder,
                    "lastUpdatedAt": FieldValue.serverTimestamp()
                ]
                if let dp = dpData {
                    updatePayload["dayProfiles"] = dp
                }
                try? await db.collection("families").document(fid)
                    .collection("members").document(member.id)
                    .updateData(updatePayload)
            }
        }
    }

    func snooze(memberId: String, memberName: String) {
        let snoozeTime = Date().addingTimeInterval(5 * 60)
        snoozeUntil = snoozeTime
        UserDefaults.standard.set(snoozeTime.timeIntervalSince1970, forKey: "snooze_until")
        AlarmService.shared.scheduleWakeUp(
            wakeUpTime: snoozeTime,
            memberId: memberId,
            memberName: memberName,
            soundUri: nil,
            isSnooze: true
        )
    }

    func cancelSnooze(_ memberId: String) {
        snoozeUntil = nil
        UserDefaults.standard.removeObject(forKey: "snooze_until")
        AlarmService.shared.cancelWakeUp(memberId: memberId, isSnooze: true)
    }

    func setupTestAlarmAndMembers(completion: @escaping (String) -> Void) {
        guard let fid = familyId else { completion("Fehler: keine FamilyId"); return }
        guard let userId = Auth.auth().currentUser?.uid else { completion("Fehler: nicht eingeloggt"); return }
        let userName = Auth.auth().currentUser?.displayName ?? "Test User"
        let deviceId = UIDevice.current.identifierForVendor?.uuidString ?? ""
        
        Task {
            // Delete existing members
            for m in members {
                AlarmService.shared.cancelWakeUp(memberId: m.id)
                self.deleteMember(m.id)
            }
            
            try? await Task.sleep(nanoseconds: 1_000_000_000) // Wait 1 second
            
            let newId = UUID().uuidString
            let cal = Calendar.current
            let inTwoMinutes = cal.date(byAdding: .minute, value: 2, to: Date()) ?? Date()
            let comps = cal.dateComponents([.hour, .minute], from: inTwoMinutes)
            let wakeTime = DateComponents.from(hour: comps.hour ?? 0, minute: comps.minute ?? 0)
            
            let todayDow = cal.component(.weekday, from: Date())
            let todayIso = todayDow == 1 ? 7 : todayDow - 1
            
            let testProfile = DayProfile(
                isActive: true,
                earliestWakeUp: wakeTime,
                latestWakeUp: wakeTime,
                bathroomDurationMinutes: 10,
                wantsBreakfast: false,
                leaveHomeTime: nil,
                bufferMinutes: nil
            )
            
            let newMember = FamilyMember(
                id: newId,
                name: userName,
                earliestWakeUp: DateComponents.from(hour: 6, minute: 0),
                latestWakeUp: DateComponents.from(hour: 7, minute: 0),
                bathroomDurationMinutes: 10,
                wantsBreakfast: false,
                isPaused: false,
                claimedByUserId: userId,
                claimedByUserName: userName,
                claimedByDeviceId: deviceId,
                sequenceOrder: 0,
                createdAt: Date().timeIntervalSince1970 * 1000,
                dayProfiles: [todayIso: testProfile]
            )
            
            self.addOrUpdateMember(newMember)
            
            DispatchQueue.main.async {
                self.myMemberId = newId
                UserDefaults.standard.set(newId, forKey: "my_member_id")
                self.setAlarmEnabled(true)
                completion("Reset & Test User angelegt")
            }
        }
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
         tooltipKeyWakeWindow, tooltipKeyBathroom, tooltipKeyWeekdays, tooltipKeyAlarmSound, tooltipKeyBuffer].forEach {
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

    func selectDayOfWeek(_ day: Int?) {
        selectedDayOfWeek = day
        recalculateSchedule()
    }

    func setGlobalBufferMinutes(_ minutes: Int) {
        guard let fid = familyId, isAdmin else { return }
        // Local update for immediate feedback
        globalBufferMinutes = minutes
        recalculateSchedule()
        
        Task {
            do {
                try await db.collection("families").document(fid)
                    .updateData(["globalBufferMinutes": minutes])
            } catch {
                await MainActor.run {
                    errorMessage = mapFirebaseError(error)
                }
            }
        }
    }

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
        familyListener = db.collection("families").document(id).addSnapshotListener { [weak self] snap, error in
            guard let self else { return }
            if let err = error as NSError?, err.domain == FirestoreErrorDomain, err.code == FirestoreErrorCode.permissionDenied.rawValue {
                return
            }
            guard let snapshot = snap, snapshot.exists else {
                if snap?.metadata.isFromCache == true { return }
                // Family does not exist or was deleted -> kick user out
                self.leaveFamily()
                return
            }
            guard let data = snapshot.data() else { return }
            self.familyName = data["name"] as? String
            self.joinCode = data["joinCode"] as? String
            self.globalBufferMinutes = (data["globalBufferMinutes"] as? NSNumber)?.intValue ?? 0
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
                    let claimedByMe = self.members.first { $0.claimedByUserId == uid }
                    if let claimed = claimedByMe, claimed.id != self.myMemberId {
                        // Anderes Gerät hat unser Profil auf ein anderes Member umgeclaimt
                        self.myMemberId = claimed.id
                        UserDefaults.standard.set(claimed.id, forKey: "my_member_id")
                    } else if claimedByMe == nil && self.myMemberId != nil {
                        let myIdExistsInList = self.members.contains { $0.id == self.myMemberId }
                        let shouldClear = self.members.isEmpty ? !self.isSyncing : !myIdExistsInList
                        if shouldClear {
                            self.myMemberId = nil
                            UserDefaults.standard.removeObject(forKey: "my_member_id")
                            self.isAlarmEnabled = false
                            UserDefaults.standard.set(false, forKey: "alarm_enabled")
                        }
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

    func restoreUserContextIfNeeded() async {
        guard familyId == nil else { return }
        guard let uid = Auth.auth().currentUser?.uid else { return }
        do {
            let result = try await functions.httpsCallable("getUserContext").call([:] as [String: Any])
            guard let data = result.data as? [String: Any],
                  let fid = data["familyId"] as? String,
                  let code = data["joinCode"] as? String else { return }
            let name = data["familyName"] as? String ?? ""
            saveFamilyLocally(id: fid, name: name, code: code)
            listenToFamily(id: fid)
            return
        } catch {
            print("getUserContext failed, falling back to Firestore: \(error.localizedDescription)")
        }
        
        // Fallback: Direkter Lesezugriff wie in Android
        do {
            let userDoc = try await db.collection("users").document(uid).getDocument()
            guard let data = userDoc.data(),
                  let fid = data["familyId"] as? String else { return }
            
            let familyDoc = try await db.collection("families").document(fid).getDocument()
            guard let familyData = familyDoc.data(),
                  let code = familyData["joinCode"] as? String else { return }
            
            let name = familyData["name"] as? String ?? ""
            saveFamilyLocally(id: fid, name: name, code: code)
            listenToFamily(id: fid)
        } catch {
            print("Fallback also failed: \(error.localizedDescription)")
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
        tooltipAlarmSoundSeen = UserDefaults.standard.bool(forKey: tooltipKeyAlarmSound)
        tooltipBufferSeen = UserDefaults.standard.bool(forKey: tooltipKeyBuffer)
    }

    private func startNetworkMonitor() {
        let monitor = NWPathMonitor()
        monitor.pathUpdateHandler = { [weak self] path in
            Task { @MainActor in
                self?.isOffline = (path.status != .satisfied)
            }
        }
        monitor.start(queue: monitorQueue)
        self.pathMonitor = monitor
    }

    private func generateJoinCode() -> String {
        let chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return String((0..<6).compactMap { _ in chars.randomElement() })
    }

    // MARK: - Firestore Mapping
    // Moved to Data/FirestoreMapper.swift (FamilyMember.fromFirestore / .toFirestoreMap)

    // MARK: - Schedule Calculation
    func recalculateSchedule() {
        let currentMyMemberId = self.myMemberId
        let rawMembers: [FamilyMember]
        if isAlarmEnabled {
            rawMembers = members.filter { $0.deviceAlarmEnabled != false }
        } else {
            rawMembers = members.filter { $0.id != currentMyMemberId && $0.deviceAlarmEnabled != false }
        }

        if rawMembers.isEmpty {
            schedule = nil
            if let myId = currentMyMemberId {
                AlarmService.shared.cancelWakeUp(memberId: myId)
            }
            return
        }

        let cal = Calendar.current
        let now = Date()
        let today = cal.startOfDay(for: now)
        guard let tomorrow = cal.date(byAdding: .day, value: 1, to: today) else { return }

        // 1. UI Target Date & Calculation
        let uiTargetDate: Date
        if let selectedDay = selectedDayOfWeek {
            let targetWeekday = selectedDay == 7 ? 1 : selectedDay + 1
            var checkDate = today
            while cal.component(.weekday, from: checkDate) != targetWeekday {
                guard let next = cal.date(byAdding: .day, value: 1, to: checkDate) else { break }
                checkDate = next
            }
            if cal.isDate(checkDate, inSameDayAs: today) {
                let todayMembers = rawMembers.map { resolveEffectiveMember($0, forDate: today) }
                let todayHasActive = todayMembers.contains { !$0.isPaused }
                if todayHasActive {
                    let todaySchedule = Scheduler().calculateIdealSchedule(members: todayMembers, globalBufferMinutes: globalBufferMinutes)
                    let latestAlarm = todaySchedule.memberSchedules.compactMap { date(from: $0.wakeUpTime, on: today) }.max()
                    if let latest = latestAlarm, now < latest {
                        uiTargetDate = today
                    } else {
                        uiTargetDate = cal.date(byAdding: .day, value: 7, to: today) ?? today
                    }
                } else {
                    uiTargetDate = cal.date(byAdding: .day, value: 7, to: today) ?? today
                }
            } else {
                uiTargetDate = checkDate
            }
        } else {
            let todayMembers = rawMembers.map { resolveEffectiveMember($0, forDate: today) }
            let todayHasActive = todayMembers.contains { !$0.isPaused }
            if todayHasActive {
                let todaySchedule = Scheduler().calculateIdealSchedule(members: todayMembers, globalBufferMinutes: globalBufferMinutes)
                let latestAlarm = todaySchedule.memberSchedules.compactMap { date(from: $0.wakeUpTime, on: today) }.max()
                if let latest = latestAlarm, now < latest {
                    uiTargetDate = today
                } else {
                    uiTargetDate = tomorrow
                }
            } else {
                uiTargetDate = tomorrow
            }
        }

        let uiDayOfWeekRaw = cal.component(.weekday, from: uiTargetDate)
        let uiIsoDow = uiDayOfWeekRaw == 1 ? 7 : uiDayOfWeekRaw - 1
        let uiCalculationMembers = rawMembers.map { resolveEffectiveMember($0, forDate: uiTargetDate) }
            .sorted { m1, m2 in
                let s1 = m1.dayProfiles?[uiIsoDow]?.sequenceOrder ?? m1.sequenceOrder
                let s2 = m2.dayProfiles?[uiIsoDow]?.sequenceOrder ?? m2.sequenceOrder
                if s1 != s2 {
                    return s1 < s2
                }
                return (m1.createdAt ?? 0) < (m2.createdAt ?? 0)
            }
        
        var uiResult: FamilySchedule
        if !uiCalculationMembers.contains(where: { !$0.isPaused }) {
            uiResult = FamilySchedule(memberSchedules: [], breakfastTime: nil, isValid: true, scheduleMessage: .noActiveSchedule)
        } else {
            uiResult = Scheduler().calculateIdealSchedule(members: uiCalculationMembers, globalBufferMinutes: globalBufferMinutes)
        }
        uiResult.targetDate = uiTargetDate
        schedule = uiResult

        // 2. Device Alarm Target Date & Calculation (ALWAYS Auto Mode)
        let deviceTargetDate: Date
        let todayMembers = rawMembers.map { resolveEffectiveMember($0, forDate: today) }
        let todayHasActive = todayMembers.contains { !$0.isPaused }
        if todayHasActive {
            let todaySchedule = Scheduler().calculateIdealSchedule(members: todayMembers, globalBufferMinutes: globalBufferMinutes)
            let latestAlarm = todaySchedule.memberSchedules.compactMap { date(from: $0.wakeUpTime, on: today) }.max()
            if let latest = latestAlarm, now < latest {
                deviceTargetDate = today
            } else {
                deviceTargetDate = tomorrow
            }
        } else {
            deviceTargetDate = tomorrow
        }

        let deviceDayOfWeekRaw = cal.component(.weekday, from: deviceTargetDate)
        let deviceIsoDow = deviceDayOfWeekRaw == 1 ? 7 : deviceDayOfWeekRaw - 1
        let deviceCalculationMembers = rawMembers.map { resolveEffectiveMember($0, forDate: deviceTargetDate) }
            .sorted { m1, m2 in
                let s1 = m1.dayProfiles?[deviceIsoDow]?.sequenceOrder ?? m1.sequenceOrder
                let s2 = m2.dayProfiles?[deviceIsoDow]?.sequenceOrder ?? m2.sequenceOrder
                if s1 != s2 {
                    return s1 < s2
                }
                return (m1.createdAt ?? 0) < (m2.createdAt ?? 0)
            }
        var deviceResult: FamilySchedule
        if !deviceCalculationMembers.contains(where: { !$0.isPaused }) {
            deviceResult = FamilySchedule(memberSchedules: [], breakfastTime: nil, isValid: true, scheduleMessage: .noActiveSchedule)
        } else {
            deviceResult = Scheduler().calculateIdealSchedule(members: deviceCalculationMembers, globalBufferMinutes: globalBufferMinutes)
        }
        deviceResult.targetDate = deviceTargetDate
        deviceSchedule = deviceResult

        // 3. Apply Alarms
        if deviceResult.memberSchedules.isEmpty {
            if let myId = currentMyMemberId {
                AlarmService.shared.cancelWakeUp(memberId: myId)
            }
        } else {
            if isAlarmEnabled {
                applyAlarms(deviceResult)
            } else {
                if let myId = currentMyMemberId {
                    AlarmService.shared.cancelWakeUp(memberId: myId)
                }
            }
        }
    }

    private func applyAlarms(_ schedule: FamilySchedule) {
        let cal = Calendar.current
        let now = Date()
        let today = cal.startOfDay(for: now)
        guard let tomorrow = cal.date(byAdding: .day, value: 1, to: today) else { return }

        guard let currentMyMemberId = myMemberId else { return }
        guard let memberSchedule = schedule.memberSchedules.first(where: { $0.member.id == currentMyMemberId }) else { return }

        let wakeUpTime = memberSchedule.wakeUpTime
        let targetDate = schedule.targetDate ?? (now > (date(from: wakeUpTime, on: today) ?? now) ? tomorrow : today)

        // Grace-Period: verhindert, dass ein soeben gefeuerter Alarm den nächsten
        // Tag überspringt, weil targetDate nun bereits auf morgen zeigt.
        if targetDate != today {
            if let todayAlarmDate = date(from: wakeUpTime, on: today) {
                let millisSince = now.timeIntervalSince(todayAlarmDate) * 1000
                if millisSince >= 0 && millisSince <= 300_000 { return }
            }
        }

        let dayOfWeek = cal.component(.weekday, from: targetDate)
        let isoDow = dayOfWeek == 1 ? 7 : dayOfWeek - 1
        let dayProfile = memberSchedule.member.dayProfiles?[isoDow]

        if dayProfile?.isActive == false {
            AlarmService.shared.cancelWakeUp(memberId: currentMyMemberId)
            self.schedule = FamilySchedule(memberSchedules: [], breakfastTime: nil, isValid: true, scheduleMessage: .noActiveSchedule)
            return
        }

        guard let targetDateTime = date(from: wakeUpTime, on: targetDate) else { return }

        if isAwakeTodayLocal && targetDate == today {
            AlarmService.shared.cancelWakeUp(memberId: currentMyMemberId)
            return
        }

        AlarmService.shared.scheduleWakeUp(
            wakeUpTime: targetDateTime,
            memberId: memberSchedule.member.id,
            memberName: memberSchedule.member.name,
            soundUri: alarmSoundUri,
            isSnooze: false,
            onPermissionDenied: { [weak self] in
                self?.errorMessage = L.errorAlarmPermission
            },
            onSuccess: { [weak self] in
                if self?.errorMessage == L.errorAlarmPermission {
                    self?.errorMessage = nil
                }
            }
        )
    }

    private func date(from comps: DateComponents, on date: Date) -> Date? {
        let cal = Calendar.current
        return cal.date(bySettingHour: comps.hour ?? 0, minute: comps.minute ?? 0, second: 0, of: date)
    }

    func applyAutoFix() {
        guard let currentSchedule = schedule, !currentSchedule.isValid else { return }

        Task {
            var updatedMembersMap = [String: FamilyMember]()

            let cal = Calendar.current
            let now = Date()
            let todayDow = cal.component(.weekday, from: now)
            let todayIso = todayDow == 1 ? 7 : todayDow - 1
            
            let targetDow: Int
            if let targetDate = currentSchedule.targetDate {
                let tDow = cal.component(.weekday, from: targetDate)
                targetDow = tDow == 1 ? 7 : tDow - 1
            } else {
                targetDow = todayIso
            }

            for s in currentSchedule.memberSchedules {
                guard let originalMember = self.members.first(where: { $0.id == s.member.id }) else { continue }
                var member = originalMember

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
                    if newLatest < newEarliest {
                        newLatest = newEarliest
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
                if newTopLatest < newTopEarliest {
                    newTopLatest = newTopEarliest
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
    private func resolveEffectiveMember(_ member: FamilyMember, forDate: Date? = nil) -> FamilyMember {
        let cal = Calendar.current
        let now = Date()
        let today = cal.startOfDay(for: now)
        let nowMinutes = cal.component(.hour, from: now) * 60 + cal.component(.minute, from: now)

        let targetDate: Date
        if let fd = forDate {
            targetDate = fd
        } else {
            let todayDow = cal.component(.weekday, from: today)
            let todayIso = todayDow == 1 ? 7 : todayDow - 1
            let todayProfile = member.dayProfiles?[todayIso]
            let latestMinutes = (todayProfile?.latestWakeUp.hour ?? 0) * 60 + (todayProfile?.latestWakeUp.minute ?? 0)
            
            if todayProfile?.isActive == true && nowMinutes < latestMinutes {
                targetDate = today
            } else {
                targetDate = cal.date(byAdding: .day, value: 1, to: today) ?? today
            }
        }

        let targetDowRaw = cal.component(.weekday, from: targetDate)
        let targetDow = targetDowRaw == 1 ? 7 : targetDowRaw - 1

        guard let profile = member.dayProfiles?[targetDow], profile.isActive else {
            var paused = member
            paused.isPaused = true
            return paused
        }

        var resolved = member
        resolved.earliestWakeUp = profile.earliestWakeUp
        resolved.latestWakeUp = profile.latestWakeUp
        resolved.bathroomDurationMinutes = profile.bathroomDurationMinutes
        resolved.wantsBreakfast = profile.wantsBreakfast
        resolved.leaveHomeTime = profile.leaveHomeTime
        resolved.dayProfiles = [targetDow: profile]
        resolved.isSimpleMode = profile.isSimpleMode
        return resolved
    }
}
