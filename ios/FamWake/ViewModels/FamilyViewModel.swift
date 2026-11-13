import Foundation
import Combine
import FirebaseFirestore
import FirebaseAuth
import FirebaseFunctions
import Aptabase
import Network
import UserNotifications

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
    @Published var isLocalOnlyFamily: Bool = PendingSyncManager.shared.isLocalOnlyFamily
    @Published var isJoiningFamily: Bool = false
    @Published var errorMessage: String? = nil
    @Published var pendingJoinCode: String? = nil
    @Published var snoozeUntil: Date? = nil
    @Published var snoozeCount: Int = UserDefaults.standard.integer(forKey: "snooze_count")
    @Published var alarmSoundUri: String? = UserDefaults.standard.string(forKey: "alarm_sound_uri")
    @Published var isGentleWakeEnabled: Bool = UserDefaults.standard.object(forKey: "gentle_wake_enabled") as? Bool ?? true
    @Published var isEveningReminderEnabled: Bool = UserDefaults.standard.object(forKey: "evening_reminder_enabled") as? Bool ?? true
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
    @Published var vacationUntil: String? = UserDefaults.standard.string(forKey: "vacation_until")
    @Published var bathroomFreeSending: Bool = false
    @Published var bathroomFreeSent: Bool = false


    /// IDs von Members deren Pause-Toggle noch auf Firestore-Bestätigung wartet.
    /// Verhindert visuelles Flackern durch vorzeitige Snapshot-Überschreibung.
    private var pendingPauseToggleIds: Set<String> = []

    /// Letzte bekannte Weckzeit – für Shift-Notification bei Snooze anderer Member
    var lastKnownWakeUpTime: Date?

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
    var functions = Functions.functions(region: "europe-west3")
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
        
        self.isAwakeTodayLocal = isAwakeTodayEffective()
        
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
            if isOffline {
                // Offline-First: Familie lokal erstellen mit temporärer ID
                let tempFamilyId = UUID().uuidString
                await MainActor.run {
                    saveFamilyLocally(id: tempFamilyId, name: name, code: "")
                    PendingSyncManager.shared.isLocalOnlyFamily = true
                    isLocalOnlyFamily = true
                    Aptabase.shared.trackEvent("family_created", with: ["offline": true])
                    completion(true)
                    isSyncing = false
                }
                return
            }
            do {
                let result = try await FamilyFirestoreService.shared.createFamily(name: name)
                await MainActor.run {
                    saveFamilyLocally(id: result.familyId, name: result.familyName, code: result.joinCode)
                    Aptabase.shared.trackEvent("family_created", with: ["offline": false])
                    listenToFamily(id: result.familyId)
                    completion(true)
                }
            } catch {
                await MainActor.run {
                    errorMessage = mapFirebaseError(error)
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
                    Aptabase.shared.trackEvent("family_joined")
                    listenToFamily(id: result.familyId)
                    completion(true)
                }
            } catch {
                await MainActor.run {
                    errorMessage = mapFirebaseError(error)
                    completion(false)
                }
            }
            await MainActor.run { isJoiningFamily = false }
        }
    }
    
    private func currentTodayStr(from now: Date = Date()) -> String {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: now)
    }

    func isAwakeTodayEffective() -> Bool {
        let awake = UserDefaults.standard.bool(forKey: "awake_today")
        let savedDate = UserDefaults.standard.string(forKey: "awake_today_date")
        return awake && savedDate == currentTodayStr()
    }

    private func setAwakeTodayLocal(_ awake: Bool) {
        isAwakeTodayLocal = awake
        UserDefaults.standard.set(awake, forKey: "awake_today")
        if awake {
            UserDefaults.standard.set(currentTodayStr(), forKey: "awake_today_date")
        } else {
            UserDefaults.standard.removeObject(forKey: "awake_today_date")
        }
    }

    func checkAndResetMembers() {
        guard !members.isEmpty, let familyId = familyId else { return }
        let now = Date()
        let cal = Calendar.current
        let todayStr = currentTodayStr(from: now)
        let weekdayRaw = cal.component(.weekday, from: now)
        let todayIso = weekdayRaw == 1 ? 7 : weekdayRaw - 1
        let nowMinutes = cal.component(.hour, from: now) * 60 + cal.component(.minute, from: now)
        
        var toUpdate = [FamilyMember]()
        var localMembersChanged = false
        
        for i in 0..<members.count {
            var member = members[i]
            let latestWakeUp = member.dayProfiles?[todayIso]?.latestWakeUp ?? member.latestWakeUp
            let latestMinutes = (latestWakeUp.hour ?? 7) * 60 + (latestWakeUp.minute ?? 0)
            // Bei sehr später Weckzeit (z. B. >= 22:00) auf 23:59 deckeln, damit +120 Min nicht über Mitternacht läuft
            let thresholdMinutes = min(23 * 60 + 59, latestMinutes + 120)
            let isPastTodayThreshold = nowMinutes >= thresholdMinutes
            // Falls die App gestern nach dem Schwellenwert nicht mehr offen war, morgens vor der Weckzeit nachholen
            let isStaleFromPreviousDay = !member.lastResetDate.isEmpty && member.lastResetDate < todayStr && nowMinutes < latestMinutes
            
            if member.lastResetDate != todayStr && (isPastTodayThreshold || isStaleFromPreviousDay) {
                let isUnclaimed = member.claimedByUserId == nil
                let newIsPaused = isUnclaimed ? false : member.isPaused
                
                member.isPaused = newIsPaused
                member.isAwakeToday = false
                member.lastResetDate = todayStr
                member.snoozeUntil = nil
                member.snoozeCount = 0
                
                members[i] = member
                toUpdate.append(member)
                localMembersChanged = true
                
                if member.id == myMemberId {
                    setAwakeTodayLocal(false)
                    // Snooze-Count nur resetten wenn kein aktiver Snooze läuft –
                    // sonst wird der Count zwischen zwei Snoozes gelöscht
                    let activeSnooze = UserDefaults.standard.double(forKey: "snooze_until")
                    if activeSnooze <= Date().timeIntervalSince1970 {
                        UserDefaults.standard.set(0, forKey: "snooze_count")
                        UserDefaults.standard.removeObject(forKey: "snooze_until")
                        snoozeUntil = nil
                        snoozeCount = 0
                    }
                }
            }
        }
        
        if localMembersChanged {
            LocalMemberStore.shared.save(members: members, familyId: familyId)
            recalculateSchedule()
            
            guard !isLocalOnlyFamily else { return }
            // Auf Firestore synchronisieren
            Task {
                do {
                    try await FamilyFirestoreService.shared.resetMembersBatch(
                        familyId: familyId,
                        membersToUpdate: toUpdate,
                        todayStr: todayStr
                    )
                } catch {
                    print("Batch commit failed during member reset: \(error.localizedDescription)")
                    await MainActor.run {
                        self.errorMessage = self.mapFirebaseError(error)
                    }
                }
            }
        }
    }

    func leaveFamily(completion: @escaping (Bool) -> Void = { _ in }) {
        guard let fid = familyId else { clearFamilyLocally(); completion(true); return }
        let myId = myMemberId
        stopSyncJobs()
        Task {
            // L17: Fehler-Feedback statt try? – Fehler nicht mehr verschlucken
            do {
                try await FamilyFirestoreService.shared.leaveFamily(familyId: fid, memberId: myId)
                await MainActor.run {
                    Aptabase.shared.trackEvent("family_left")
                    self.clearFamilyLocally()
                    self.familyListener?.remove()
                    self.membersListener?.remove()
                    self.members = []
                    self.schedule = nil
                    completion(true)
                }
            } catch {
                print("leaveFamily Cloud Function fehlgeschlagen: \(error.localizedDescription)")
                await MainActor.run {
                    // Server sagt: User gehört nicht (mehr) zur Familie → lokal aufräumen
                    let msg = error.localizedDescription.uppercased()
                    if msg.contains("NOT_A_MEMBER") || msg.contains("PERMISSION") || msg.contains("NOT_FOUND") {
                        self.clearFamilyLocally()
                        self.familyListener?.remove()
                        self.membersListener?.remove()
                        self.members = []
                        self.schedule = nil
                        completion(true)
                    } else {
                        self.errorMessage = self.mapFirebaseError(error)
                        completion(false)
                    }
                }
            }
        }
    }

    var isVacationActive: Bool {
        guard let vac = vacationUntil, !vac.isEmpty else { return false }
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        let todayStr = f.string(from: Date())
        return todayStr <= vac
    }

    var isAwakeButtonVisible: Bool {
        guard isAlarmEnabled, !isAwakeTodayLocal, !isVacationActive, let myId = myMemberId, let myMember = members.first(where: { $0.id == myId }) else { return false }
        
        let cal = Calendar.current
        let now = Date()
        let weekdayRaw = cal.component(.weekday, from: now)
        let todayDow = weekdayRaw == 1 ? 7 : weekdayRaw - 1
        let startOfToday = cal.startOfDay(for: now)
        
        for offset in 0..<7 {
            let checkDow = ((todayDow - 1 + offset) % 7) + 1
            if let p = myMember.dayProfiles?[checkDow], p.isActive {
                let myScheduledTime = (offset == 0) ? deviceSchedule?.memberSchedules.first(where: { $0.id == myId })?.wakeUpTime : nil
                let alarmTime = myScheduledTime ?? p.earliestWakeUp
                guard let targetDayDate = cal.date(byAdding: .day, value: offset, to: startOfToday),
                      let targetDate = cal.date(bySettingHour: alarmTime.hour ?? 0, minute: alarmTime.minute ?? 0, second: 0, of: targetDayDate) else {
                    continue
                }
                if now < targetDate {
                    guard let windowStart = cal.date(byAdding: .hour, value: -4, to: targetDate) else { return false }
                    return now >= windowStart && now < targetDate
                }
            }
        }
        return false
    }

    var isBathroomFreeButtonVisible: Bool {
        guard isAlarmEnabled, !isVacationActive, let myId = myMemberId, let myMember = members.first(where: { $0.id == myId }) else { return false }
        
        let cal = Calendar.current
        let now = Date()
        let weekdayRaw = cal.component(.weekday, from: now)
        let todayDow = weekdayRaw == 1 ? 7 : weekdayRaw - 1
        let startOfToday = cal.startOfDay(for: now)
        
        guard let p = myMember.dayProfiles?[todayDow], p.isActive else { return false }
        let myScheduledTime = deviceSchedule?.memberSchedules.first(where: { $0.id == myId })?.wakeUpTime
        let alarmTime = myScheduledTime ?? p.earliestWakeUp
        guard let todayAlarmDate = cal.date(bySettingHour: alarmTime.hour ?? 0, minute: alarmTime.minute ?? 0, second: 0, of: startOfToday),
              let windowEnd = cal.date(byAdding: .minute, value: 120, to: todayAlarmDate) else {
            return false
        }
        
        if isAwakeTodayLocal {
            guard let windowStart = cal.date(byAdding: .hour, value: -4, to: todayAlarmDate) else { return false }
            return now >= windowStart && now <= windowEnd
        } else {
            return now >= todayAlarmDate && now <= windowEnd
        }
    }

    func deleteFamily(completion: @escaping (Bool) -> Void) {
        guard let fid = familyId, isAdmin else { completion(false); return }
        stopSyncJobs()
        Task {
            do {
                try await FamilyFirestoreService.shared.deleteFamily(familyId: fid)
                await MainActor.run {
                    Aptabase.shared.trackEvent("family_deleted")
                    clearFamilyLocally()
                    familyListener?.remove()
                    membersListener?.remove()
                    members = []
                    schedule = nil
                    completion(true)
                }
            } catch {
                await MainActor.run {
                    errorMessage = mapFirebaseError(error)
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

        let cal = Calendar.current
        let now = Date()
        let weekdayRaw = cal.component(.weekday, from: now)
        let todayDow = weekdayRaw == 1 ? 7 : weekdayRaw - 1

        let oldMember = members.first(where: { $0.id == member.id })
        let oldProfile = oldMember?.dayProfiles?[todayDow]
        let newProfile = member.dayProfiles?[todayDow]

        let oldEarliest = oldProfile?.earliestWakeUp ?? oldMember?.earliestWakeUp
        let newEarliest = newProfile?.earliestWakeUp ?? member.earliestWakeUp
        let oldLatest = oldProfile?.latestWakeUp ?? oldMember?.latestWakeUp
        let newLatest = newProfile?.latestWakeUp ?? member.latestWakeUp
        let oldIsActive = oldProfile?.isActive ?? !(oldMember?.isPaused ?? false)
        let newIsActive = newProfile?.isActive ?? !member.isPaused

        let todayProfileChanged = oldProfile != newProfile || oldEarliest != newEarliest || oldLatest != newLatest || oldIsActive != newIsActive

        var hasFutureAlarmToday = false
        let neMin = (newEarliest.hour ?? 0) * 60 + (newEarliest.minute ?? 0)
        let nlMin = (newLatest.hour ?? 0) * 60 + (newLatest.minute ?? 0)
        let effectiveNewTime = nlMin >= neMin ? newLatest : newEarliest

        if newIsActive, let targetHour = effectiveNewTime.hour, let targetMin = effectiveNewTime.minute,
           let targetDate = cal.date(bySettingHour: targetHour, minute: targetMin, second: 0, of: now) {
            hasFutureAlarmToday = targetDate > now
        }

        let isMyMember = member.id == myMemberId || shouldClaim
        let shouldResetAwake = todayProfileChanged && hasFutureAlarmToday
        if shouldResetAwake {
            updatedMember.isAwakeToday = false
            if isMyMember {
                setAwakeTodayLocal(false)
            }
        }
        // Optimistisches lokales Update für sofortige UI-Aktualisierung (Offline-First)
        if let idx = members.firstIndex(where: { $0.id == updatedMember.id }) {
            members[idx] = updatedMember
        } else {
            members.append(updatedMember)
        }
        recalculateSchedule()

        if isLocalOnlyFamily {
            // Offline-Only: Nur lokal speichern, kein Firestore
            if shouldClaim {
                myMemberId = updatedMember.id
                UserDefaults.standard.set(updatedMember.id, forKey: "my_member_id")
                isAlarmEnabled = true
                UserDefaults.standard.set(true, forKey: "alarm_enabled")
            }
            LocalMemberStore.shared.save(members: members, familyId: fid)
            isSyncing = false
            recalculateSchedule()
            return
        }
        Task {
            await FamilyFirestoreService.shared.trackUserAction(familyId: fid)
            do {
                try await FamilyFirestoreService.shared.addOrUpdateMember(familyId: fid, member: updatedMember)
                await MainActor.run {
                    Aptabase.shared.trackEvent("member_created")
                    if shouldClaim {
                        myMemberId = updatedMember.id
                        UserDefaults.standard.set(updatedMember.id, forKey: "my_member_id")
                        isAlarmEnabled = true
                        UserDefaults.standard.set(true, forKey: "alarm_enabled")
                    }
                    isSyncing = false
                    self.recalculateSchedule()
                    if let fid = self.familyId {
                        LocalMemberStore.shared.save(members: self.members, familyId: fid)
                    }
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
        let wasMyMember = (memberId == myMemberId)
        if isLocalOnlyFamily {
            members.removeAll { $0.id == memberId }
            if wasMyMember {
                myMemberId = nil
                UserDefaults.standard.removeObject(forKey: "my_member_id")
                isAlarmEnabled = false
                UserDefaults.standard.set(false, forKey: "alarm_enabled")
            }
            LocalMemberStore.shared.save(members: members, familyId: fid)
            recalculateSchedule()
            return
        }
        Task {
            await FamilyFirestoreService.shared.trackUserAction(familyId: fid)
            do {
                try await FamilyFirestoreService.shared.deleteMember(familyId: fid, memberId: memberId)
                await MainActor.run {
                    Aptabase.shared.trackEvent("member_deleted")
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
        if member.claimedByUserId != nil { return }
        let oldPausedState = member.isPaused
        let newPausedState = !oldPausedState
        
        pendingPauseToggleIds.insert(memberId)
        if let idx = members.firstIndex(where: { $0.id == memberId }) {
            members[idx].isPaused = newPausedState
            recalculateSchedule()
        }
        Aptabase.shared.trackEvent(newPausedState ? "member_paused" : "member_resumed")
        
        Task {
            await FamilyFirestoreService.shared.trackUserAction(familyId: fid)
            do {
                try await FamilyFirestoreService.shared.togglePauseMember(familyId: fid, memberId: memberId, newPausedState: newPausedState)
                await MainActor.run {
                    self.pendingPauseToggleIds.remove(memberId)
                }
            } catch {
                await MainActor.run {
                    self.pendingPauseToggleIds.remove(memberId)
                    if let idx = self.members.firstIndex(where: { $0.id == memberId }) {
                        self.members[idx].isPaused = oldPausedState
                        self.recalculateSchedule()
                    }
                    self.errorMessage = L.errorGeneric
                }
            }
        }
    }

    func toggleAwakeMember(_ memberId: String) {
        setAwake(memberId: memberId, awake: !isAwakeTodayLocal)
    }

    func setAwake(memberId: String, awake: Bool) {
        guard let fid = familyId else { return }
        let oldState = isAwakeTodayLocal
        setAwakeTodayLocal(awake)
        
        if let idx = members.firstIndex(where: { $0.id == memberId }) {
            members[idx].isAwakeToday = awake
            if awake {
                members[idx].snoozeUntil = nil
                members[idx].snoozeCount = 0
            }
            LocalMemberStore.shared.save(members: members, familyId: fid)
        }
        
        if awake {
            snoozeUntil = nil
            snoozeCount = 0
            UserDefaults.standard.removeObject(forKey: "snooze_until")
            UserDefaults.standard.set(0, forKey: "snooze_count")
            AlarmService.shared.cancelWakeUp(memberId: memberId)
        }
        
        // Direkt neu berechnen, damit deviceTargetDate bei awake == true sofort auf morgen
        // umschaltet und den morgigen Wecker plant (bzw. bei awake == false wieder auf heute).
        recalculateSchedule()
        
        guard !isLocalOnlyFamily else {
            Aptabase.shared.trackEvent(awake ? "awake_marked" : "awake_reset")
            return
        }
        
        Task {
            await FamilyFirestoreService.shared.trackUserAction(familyId: fid)
            do {
                if awake {
                    let memberRef = db.collection("families").document(fid)
                        .collection("members").document(memberId)
                    try await memberRef.updateData([
                        "isAwakeToday": true,
                        "snoozeUntil": FieldValue.delete(),
                        "snoozeCount": 0,
                        "lastUpdatedAt": FieldValue.serverTimestamp()
                    ])
                } else {
                    try await FamilyFirestoreService.shared.setAwake(familyId: fid, memberId: memberId, awake: awake)
                }
                await MainActor.run {
                    Aptabase.shared.trackEvent(awake ? "awake_marked" : "awake_reset")
                }
            } catch {
                await MainActor.run {
                    self.setAwakeTodayLocal(oldState)
                    if let idx = self.members.firstIndex(where: { $0.id == memberId }) {
                        self.members[idx].isAwakeToday = oldState
                    }
                    self.recalculateSchedule()
                    self.errorMessage = L.errorGeneric
                }
            }
        }
    }

    func setAlarmEnabled(_ enabled: Bool) {
        Aptabase.shared.trackEvent("alarm_enabled_toggled", with: ["enabled": enabled])
        isAlarmEnabled = enabled
        UserDefaults.standard.set(enabled, forKey: "alarm_enabled")
        
        if let mid = myMemberId, let idx = members.firstIndex(where: { $0.id == mid }) {
            members[idx].deviceAlarmEnabled = enabled
        }

        // Beim Aus- UND Einschalten wird „Schon wach" zurückgesetzt analog zu Android.
        // Bewusster Toggle = expliziter Neustart – unabhängig vom vorherigen Zustand.
        setAwakeTodayLocal(false)
        if let mid = myMemberId {
            if let idx = members.firstIndex(where: { $0.id == mid }) {
                members[idx].isAwakeToday = false
            }
            if let fid = familyId, !isLocalOnlyFamily {
                Task {
                    try? await FamilyFirestoreService.shared.setAwake(familyId: fid, memberId: mid, awake: false)
                }
            }
        }
        
        // KRITISCH: Bei OFF sofort ALLE Alarme canceln und WARTEN bis fertig,
        // bevor recalculateSchedule läuft. Verhindert Race Condition mit neuen Alarm-UUIDs.
        if !enabled {
            Task {
                await AlarmService.shared.cancelAll()
                UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
                UNUserNotificationCenter.current().removeAllDeliveredNotifications()
                recalculateSchedule()
            }
        } else {
            recalculateSchedule()
        }
        
        if let fid = familyId, let mid = myMemberId {
            Task {
                await FamilyFirestoreService.shared.trackUserAction(familyId: fid)
                do {
                    try await FamilyFirestoreService.shared.updateDeviceAlarmEnabled(familyId: fid, memberId: mid, enabled: enabled)
                } catch {
                    print("Error updating deviceAlarmEnabled: \(error)")
                }
            }
        }
        
        if enabled {
            // applied in recalculateSchedule()
        } else {
            // ALLE Alarme + Snooze-State komplett aufräumen
            if let myId = myMemberId {
                AlarmService.shared.cancelWakeUp(memberId: myId)
            }
            // Snooze-State löschen
            snoozeUntil = nil
            UserDefaults.standard.removeObject(forKey: "snooze_until")
            UserDefaults.standard.set(0, forKey: "snooze_count")
        }
    }

    func setMyMemberId(_ memberId: String?, force: Bool = false, completion: @escaping (Bool) -> Void) {
        guard let fid = familyId, let uid = Auth.auth().currentUser?.uid else {
            completion(false); return
        }
        let userName = Auth.auth().currentUser?.displayName ?? L.s("settings_fallback_username")
        let deviceId = UIDevice.current.identifierForVendor?.uuidString ?? UUID().uuidString
        errorMessage = nil
        
        if isLocalOnlyFamily {
            // Offline-Claiming: nur lokaler State, kein Firestore
            if let oldId = self.myMemberId, oldId != memberId {
                if let idx = members.firstIndex(where: { $0.id == oldId }) {
                    members[idx].claimedByUserId = nil
                    members[idx].claimedByUserName = nil
                    members[idx].claimedByDeviceId = nil
                }
            }
            if let memberId = memberId {
                if let idx = members.firstIndex(where: { $0.id == memberId }) {
                    members[idx].claimedByUserId = uid
                    members[idx].claimedByUserName = userName
                    members[idx].claimedByDeviceId = deviceId
                    members[idx].deviceAlarmEnabled = true
                }
                self.myMemberId = memberId
                UserDefaults.standard.set(memberId, forKey: "my_member_id")
                Aptabase.shared.trackEvent("member_claimed", with: ["offline": true])
            } else {
                self.myMemberId = nil
                UserDefaults.standard.removeObject(forKey: "my_member_id")
                isAlarmEnabled = false
                UserDefaults.standard.set(false, forKey: "alarm_enabled")
            }
            if let fid = familyId {
                LocalMemberStore.shared.save(members: members, familyId: fid)
            }
            recalculateSchedule()
            completion(true)
            return
        }
        
        let currentMyMemberId = self.myMemberId
        
        Task {
            // pushMeta VOR dem Write, damit CF den Sender erkennt (kein Self-Push)
            await FamilyFirestoreService.shared.trackUserAction(familyId: fid)
            do {
                if let oldId = currentMyMemberId, oldId != memberId {
                    try? await FamilyFirestoreService.shared.unclaimProfile(familyId: fid, memberId: oldId, uid: uid)
                }
                
                if let memberId = memberId {
                    let success = try await FamilyFirestoreService.shared.claimProfile(
                        familyId: fid,
                        memberId: memberId,
                        uid: uid,
                        userName: userName,
                        deviceId: deviceId,
                        force: force
                    )
                    
                    if success {
                        await MainActor.run {
                            self.myMemberId = memberId
                            UserDefaults.standard.set(memberId, forKey: "my_member_id")
                            Aptabase.shared.trackEvent("member_claimed", with: ["offline": false])
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
                        isSimpleMode: m.isSimpleMode,
                        breakfastDurationMinutes: m.breakfastDurationMinutes
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
            
            do {
                try await FamilyFirestoreService.shared.saveMembersOrderBatch(familyId: fid, members: members)
            } catch {
                await MainActor.run {
                    self.errorMessage = self.mapFirebaseError(error)
                }
            }
        }
    }

    func checkSnoozeStatus() {
        let staleThreshold = Date().addingTimeInterval(-30 * 60)
        if let snoozeTime = UserDefaults.standard.value(forKey: "snooze_until") as? Double {
            let date = Date(timeIntervalSince1970: snoozeTime)
            if date > Date() {
                self.snoozeUntil = date
                self.snoozeCount = UserDefaults.standard.integer(forKey: "snooze_count")
            } else if date < staleThreshold {
                // Snooze deutlich abgelaufen (>30 Min) → alles aufräumen
                self.snoozeUntil = nil
                self.snoozeCount = 0
                UserDefaults.standard.removeObject(forKey: "snooze_until")
                UserDefaults.standard.set(0, forKey: "snooze_count")
            } else {
                // Snooze gerade erst abgelaufen – Count behalten (Max-Schutz)
                self.snoozeUntil = nil
            }
        } else {
            self.snoozeUntil = nil
            // Kein snooze_until → Count nur zurücksetzen wenn nicht gerade
            // ein Snooze-Alarm feuert (Count bleibt für den nächsten Snooze erhalten)
        }
    }



    func snooze(memberId: String, memberName: String) {
        guard SnoozeController.shared.canSnooze,
              let snoozeTime = SnoozeController.shared.triggerSnooze(durationMinutes: SnoozeConfig.snoozeDurationMinutes) else {
            errorMessage = L.snoozeMaxReached
            return
        }
        let newCount = SnoozeController.shared.snoozeCount
        snoozeCount = newCount
        snoozeUntil = snoozeTime

        // Lokalen members-State SOFORT aktualisieren, damit resolveEffectiveMember
        // die snooze-fixierte Weckzeit berechnet und der Scheduler nachfolgende
        // Members korrekt verschiebt (ohne auf den Firestore-Roundtrip zu warten).
        if let idx = members.firstIndex(where: { $0.id == memberId }) {
            members[idx].snoozeUntil = snoozeTime
            members[idx].snoozeCount = newCount
        }
        recalculateSchedule()
        
        // AlarmKit: Alten Alarm canceln + neuen Snooze-Alarm planen.
        // scheduleWakeUpDirect() (intern) cancelled die alte UUID und plant die neue.
        let savedSoundUri = UserDefaults.standard.string(forKey: "alarm_sound_uri")
        AlarmService.shared.scheduleWakeUp(
            wakeUpTime: snoozeTime,
            memberId: memberId,
            memberName: memberName,
            soundUri: savedSoundUri,
            isSnooze: true
        )

        // Snooze-State nach Firestore synchronisieren
        if let familyId = familyId {
            let memberRef = db.collection("families").document(familyId)
                .collection("members").document(memberId)
            Task {
                // pushMeta setzen, damit CF den Sender erkennt und keine Self-Push schickt
                if let uid = Auth.auth().currentUser?.uid {
                    do {
                        try await db.collection("users").document(uid)
                            .collection("pushMeta").document("user_action")
                            .setData([
                                "familyId": familyId,
                                "timestamp": FieldValue.serverTimestamp()
                            ])
                    } catch {
                        print("Error writing pushMeta in snooze: \(error)")
                    }
                }
                do {
                    try await memberRef.updateData([
                        "snoozeUntil": Timestamp(date: snoozeTime),
                        "snoozeCount": newCount
                    ])
                } catch {
                    print("Error syncing snooze to Firestore: \(error)")
                }
            }
        }
    }

    func cancelSnooze(_ memberId: String) {
        snoozeUntil = nil
        snoozeCount = 0
        UserDefaults.standard.removeObject(forKey: "snooze_until")
        UserDefaults.standard.set(0, forKey: "snooze_count")
        // Bei nativem .countdown-Snooze läuft der Countdown unter der Haupt-UUID,
        // daher beide canceln (Haupt + ggf. alte Snooze-UUID).
        AlarmService.shared.cancelWakeUp(memberId: memberId)

        // Snooze-State in Firestore löschen
        if let familyId = familyId {
            let memberRef = db.collection("families").document(familyId)
                .collection("members").document(memberId)
            Task {
                // pushMeta setzen, damit CF den Sender erkennt und keine Self-Push schickt
                if let uid = Auth.auth().currentUser?.uid {
                    do {
                        try await db.collection("users").document(uid)
                            .collection("pushMeta").document("user_action")
                            .setData([
                                "familyId": familyId,
                                "timestamp": FieldValue.serverTimestamp()
                            ])
                    } catch {
                        print("Error writing pushMeta in cancelSnooze: \(error)")
                    }
                }
                do {
                    try await memberRef.updateData([
                        "snoozeUntil": FieldValue.delete(),
                        "snoozeCount": 0
                    ])
                } catch {
                    print("Error syncing cancelSnooze to Firestore: \(error)")
                }
            }
        }
    }

    #if DEBUG
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
            
            self.myMemberId = newId
            UserDefaults.standard.set(newId, forKey: "my_member_id")
            self.setAlarmEnabled(true)
            completion("Reset & Test User angelegt")
        }
    }
    #endif

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
        Aptabase.shared.trackEvent("alarm_sound_changed", with: ["sound": uri ?? "default"])
    }

    func setGentleWakeEnabled(_ enabled: Bool) {
        isGentleWakeEnabled = enabled
        UserDefaults.standard.set(enabled, forKey: "gentle_wake_enabled")
    }

    func setEveningReminderEnabled(_ enabled: Bool) {
        isEveningReminderEnabled = enabled
        UserDefaults.standard.set(enabled, forKey: "evening_reminder_enabled")
        if enabled {
            AlarmService.shared.scheduleEveningReminder()
        } else {
            AlarmService.shared.cancelEveningReminder()
        }
        Aptabase.shared.trackEvent("evening_reminder_toggled", with: ["enabled": enabled])
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
            await FamilyFirestoreService.shared.trackUserAction(familyId: fid)
            do {
                try await FamilyFirestoreService.shared.setGlobalBufferMinutes(familyId: fid, minutes: minutes)
            } catch {
                await MainActor.run {
                    errorMessage = mapFirebaseError(error)
                }
            }
        }
    }

    func clearPendingJoinCode() { pendingJoinCode = nil }

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
            if let error = error {
                // Bei Offline / Netzwerkfehlern / Permission-Denied niemals Session löschen!
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
            if let vac = data["vacationUntil"] as? String {
                let f = DateFormatter()
                f.calendar = Calendar(identifier: .gregorian)
                f.locale = Locale(identifier: "en_US_POSIX")
                f.dateFormat = "yyyy-MM-dd"
                let todayStr = f.string(from: Date())
                if vac < todayStr {
                    // Zombie-Cleanup: Abgelaufener Urlaub wird automatisch gelöscht
                    self.vacationUntil = nil
                    UserDefaults.standard.removeObject(forKey: "vacation_until")
                    self.clearVacation()
                } else {
                    let changed = self.vacationUntil != vac
                    self.vacationUntil = vac
                    UserDefaults.standard.set(vac, forKey: "vacation_until")
                    if changed {
                        self.recalculateSchedule()
                    }
                }
            } else if data["vacationUntil"] == nil && self.vacationUntil != nil {
                self.vacationUntil = nil
                UserDefaults.standard.removeObject(forKey: "vacation_until")
                self.recalculateSchedule()
            }
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
                if let error = error {
                    return
                }
                // M7 revert: isFromCache ist nicht zuverlässig für Offline-Erkennung,
                // da Firestore IMMER zuerst einen Cache-Snapshot liefert (Race mit NWPathMonitor).
                // Der NWPathMonitor (startNetworkMonitor) ist die einzige Quelle für isOffline.
                guard let docs = snap?.documents else { return }
                let parsed = docs.compactMap { FamilyMember.fromFirestore($0.data(), id: $0.documentID) }
                // Schutz gegen 0-Docs-Snapshot: Wenn die lokale Liste bereits Members hat und der Snapshot leer ist, nicht wipen
                if parsed.isEmpty && !self.members.isEmpty {
                    return
                }
                var sorted = parsed.sorted { $0.sequenceOrder < $1.sequenceOrder }
                // Lokalen State für eigenen Member beibehalten,
                // um Flackern zwischen lokalem Toggle/Snooze und Firestore-Roundtrip zu verhindern
                if let myId = self.myMemberId,
                   let localIdx = sorted.firstIndex(where: { $0.id == myId }) {
                    sorted[localIdx].deviceAlarmEnabled = self.isAlarmEnabled
                    // Snooze-State: Lokal gesetzten Snooze beibehalten, da der Firestore-Write
                    // noch unterwegs sein kann und der Snapshot sonst den Snooze "verschluckt"
                    if let localSnooze = self.snoozeUntil, localSnooze > Date() {
                        sorted[localIdx].snoozeUntil = localSnooze
                        sorted[localIdx].snoozeCount = UserDefaults.standard.integer(forKey: "snooze_count")
                    }
                }
                // Pause-Flicker-Guard: Lokalen isPaused-Wert beibehalten solange
                // der Firestore-Write für dieses Member noch aussteht (H1)
                for i in sorted.indices {
                    if self.pendingPauseToggleIds.contains(sorted[i].id),
                       let localMember = self.members.first(where: { $0.id == sorted[i].id }) {
                        sorted[i].isPaused = localMember.isPaused
                    }
                }
                // Awake-State-Guard: Lokales Flag hat am heutigen Tag Vorrang, damit ein noch
                // ausstehender Firestore-Write das Flag nicht wieder auf false zurücksetzt.
                if let mid = self.myMemberId,
                   let myIdx = sorted.firstIndex(where: { $0.id == mid }) {
                    if self.isAwakeTodayEffective() {
                        sorted[myIdx].isAwakeToday = true
                    } else if !self.isAwakeTodayLocal {
                        sorted[myIdx].isAwakeToday = false
                    }
                }
                self.members = sorted
                // Members lokal persistieren für App-Kill-Szenario
                LocalMemberStore.shared.save(members: sorted, familyId: familyId)
                #if DEBUG
                for m in sorted {
                    if m.snoozeUntil != nil || m.snoozeCount > 0 {
                        print("[Sync] \(m.name): snoozeUntil=\(String(describing: m.snoozeUntil)), snoozeCount=\(m.snoozeCount), deviceAlarm=\(String(describing: m.deviceAlarmEnabled))")
                    }
                }
                #endif
                self.checkAndResetMembers()
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
                        if !self.members.isEmpty && !myIdExistsInList {
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
        let memberAwake = members.first(where: { $0.id == mid })?.isAwakeToday ?? false
        if memberAwake || isAwakeTodayEffective() {
            setAwakeTodayLocal(true)
        } else {
            setAwakeTodayLocal(false)
        }
    }

    var hasFamilyIdPublished: Bool { familyId != nil }

    private func loadFamilyFromLocal() {
        if let fid = UserDefaults.standard.string(forKey: "family_id") {
            self.familyId = fid
            self.familyName = UserDefaults.standard.string(forKey: "family_name")
            self.joinCode = UserDefaults.standard.string(forKey: "family_join_code")
            // Sofort: gecachte Members laden für Instant-Anzeige
            let cachedMembers = LocalMemberStore.shared.load(familyId: fid)
            if !cachedMembers.isEmpty {
                self.members = cachedMembers
                self.checkAndResetMembers()
                self.recalculateSchedule()
            }
            if !isLocalOnlyFamily {
                listenToFamily(id: fid)
            }
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

    func logout() {
        stopSyncJobs()
        Task {
            await AlarmService.shared.cancelAll()
        }
        AlarmService.shared.cancelEveningReminder()
        SnoozeController.shared.reset()
        clearFamilyLocally()
        members = []
        schedule = nil
        snoozeUntil = nil
        snoozeCount = 0
    }

    private func clearFamilyLocally() {
        if let fid = familyId {
            LocalMemberStore.shared.delete(familyId: fid)
        }
        PendingSyncManager.shared.clear()
        isLocalOnlyFamily = false
        familyId = nil
        familyName = nil
        joinCode = nil
        myMemberId = nil
        UserDefaults.standard.removeObject(forKey: "family_id")
        UserDefaults.standard.removeObject(forKey: "family_name")
        UserDefaults.standard.removeObject(forKey: "family_join_code")
        UserDefaults.standard.removeObject(forKey: "my_member_id")
        UserDefaults.standard.removeObject(forKey: "vacation_until")
        UserDefaults.standard.removeObject(forKey: "awake_today")
        UserDefaults.standard.removeObject(forKey: "awake_today_date")
        UserDefaults.standard.removeObject(forKey: "is_awake_today")
        UserDefaults.standard.removeObject(forKey: "is_awake_today_date")
        SnoozeController.shared.reset()
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
                // Offline → Online: ausstehende lokale Familie synchronisieren
                if path.status == .satisfied, let self, self.isLocalOnlyFamily {
                    self.syncPendingFamily()
                }
            }
        }
        monitor.start(queue: monitorQueue)
        self.pathMonitor = monitor
    }

    /// Synchronisiert eine lokal erstellte Familie zur Cloud.
    /// Wird automatisch getriggert wenn das Netzwerk zurückkehrt.
    private func syncPendingFamily() {
        guard isLocalOnlyFamily, let localFamilyId = familyId else { return }
        guard let name = familyName, !name.isEmpty else { return }
        guard !isOffline else { return }
        guard !isSyncing else { return }
        
        isSyncing = true
        Task {
            do {
                // 1. Cloud Function aufrufen für echte familyId + joinCode
                let result = try await FamilyFirestoreService.shared.createFamily(name: name)
                let newFamilyId = result.familyId
                let newJoinCode = result.joinCode
                
                // 2. Alle lokalen Members in Firestore hochladen
                let currentMembers = self.members
                for member in currentMembers {
                    try await FamilyFirestoreService.shared.addOrUpdateMember(familyId: newFamilyId, member: member)
                }
                
                // 3. Lokal migrieren
                await MainActor.run {
                    LocalMemberStore.shared.migrateFamilyId(from: localFamilyId, to: newFamilyId)
                    saveFamilyLocally(id: newFamilyId, name: name, code: newJoinCode)
                    PendingSyncManager.shared.isLocalOnlyFamily = false
                    isLocalOnlyFamily = false
                    listenToFamily(id: newFamilyId)
                }
            } catch {
                print("syncPendingFamily failed (will retry on next connect): \(error.localizedDescription)")
            }
            await MainActor.run { isSyncing = false }
        }
    }

    // MARK: - Firestore Mapping
    // Moved to Data/FirestoreMapper.swift (FamilyMember.fromFirestore / .toFirestoreMap)

}
