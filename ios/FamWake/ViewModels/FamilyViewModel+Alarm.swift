import Foundation
import UserNotifications

// MARK: - FamilyViewModel Alarm & Schedule Calculation Extension

extension FamilyViewModel {

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
                    let tomorrowMembers = rawMembers.map { resolveEffectiveMember($0, forDate: tomorrow) }
                    let tomorrowHasActive = tomorrowMembers.contains { !$0.isPaused }
                    uiTargetDate = tomorrowHasActive ? tomorrow : today
                }
            } else {
                let tomorrowMembers = rawMembers.map { resolveEffectiveMember($0, forDate: tomorrow) }
                let tomorrowHasActive = tomorrowMembers.contains { !$0.isPaused }
                uiTargetDate = tomorrowHasActive ? tomorrow : today
            }
        }

        let effectiveUiMembers = rawMembers.map { resolveEffectiveMember($0, forDate: uiTargetDate) }
        var uiResult = Scheduler().calculateIdealSchedule(members: effectiveUiMembers, globalBufferMinutes: globalBufferMinutes)
        uiResult.targetDate = uiTargetDate
        self.schedule = uiResult

        if let myId = currentMyMemberId,
           let mySchedule = uiResult.memberSchedules.first(where: { $0.member.id == myId }) {
            let newWakeUpDate = date(from: mySchedule.wakeUpTime, on: uiTargetDate)
            if let oldTime = lastKnownWakeUpTime, let newTime = newWakeUpDate, oldTime != newTime {
                let someoneElseSnoozed = members.contains { member in
                    member.id != myId
                    && (member.snoozeUntil ?? .distantPast) > now
                }
                if someoneElseSnoozed {
                    let snoozingMemberName = members.first { $0.id != myId && ($0.snoozeUntil ?? .distantPast) > now }?.name ?? L.s("default_someone")
                    let formatter = DateFormatter()
                    formatter.locale = LanguageManager.shared.currentLocale
                    formatter.timeStyle = .short
                    let timeStr = formatter.string(from: newTime)
                    sendSnoozeShiftNotification(name: snoozingMemberName, time: timeStr)
                }
            }
            lastKnownWakeUpTime = newWakeUpDate
        }

        // 2. Alarm Target Date & Calculation
        let alarmTargetDate: Date
        let currentIsoDow = cal.component(.weekday, from: today) == 1 ? 7 : cal.component(.weekday, from: today) - 1
        var nextAlarmDate: Date? = nil

        for offset in 0..<7 {
            guard let candidate = cal.date(byAdding: .day, value: offset, to: today) else { continue }
            let candidateWeekday = cal.component(.weekday, from: candidate)
            let checkIsoDow = candidateWeekday == 1 ? 7 : candidateWeekday - 1

            let dayHasActiveAlarms = rawMembers.contains { m in
                if let myId = currentMyMemberId, m.id == myId {
                    return !m.isPaused && (m.dayProfiles?[checkIsoDow]?.isActive ?? false)
                }
                return currentMyMemberId == nil && !m.isPaused && (m.dayProfiles?[checkIsoDow]?.isActive ?? false)
            }

            guard dayHasActiveAlarms else { continue }

            if offset == 0 {
                let candidateMembers = rawMembers.map { resolveEffectiveMember($0, forDate: candidate) }
                let candidateSchedule = Scheduler().calculateIdealSchedule(members: candidateMembers, globalBufferMinutes: globalBufferMinutes)
                if let myId = currentMyMemberId,
                   let mySched = candidateSchedule.memberSchedules.first(where: { $0.member.id == myId }),
                   let myAlarmTime = date(from: mySched.wakeUpTime, on: candidate) {
                    if now < myAlarmTime {
                        nextAlarmDate = candidate
                        break
                    }
                } else if currentMyMemberId == nil {
                    let latestAlarm = candidateSchedule.memberSchedules.compactMap { date(from: $0.wakeUpTime, on: candidate) }.max()
                    if let latest = latestAlarm, now < latest {
                        nextAlarmDate = candidate
                        break
                    }
                }
            } else {
                nextAlarmDate = candidate
                break
            }
        }

        if let validNext = nextAlarmDate {
            alarmTargetDate = validNext
        } else {
            let todayMembers = rawMembers.map { resolveEffectiveMember($0, forDate: today) }
            let todayHasActive = todayMembers.contains { !$0.isPaused }
            if todayHasActive {
                let todaySchedule = Scheduler().calculateIdealSchedule(members: todayMembers, globalBufferMinutes: globalBufferMinutes)
                let latestAlarm = todaySchedule.memberSchedules.compactMap { date(from: $0.wakeUpTime, on: today) }.max()
                if let latest = latestAlarm, now < latest {
                    alarmTargetDate = today
                } else {
                    alarmTargetDate = tomorrow
                }
            } else {
                alarmTargetDate = tomorrow
            }
        }

        let effectiveAlarmMembers = rawMembers.map { resolveEffectiveMember($0, forDate: alarmTargetDate) }
        var alarmResult = Scheduler().calculateIdealSchedule(members: effectiveAlarmMembers, globalBufferMinutes: globalBufferMinutes)
        alarmResult.targetDate = alarmTargetDate

        #if DEBUG
        print("[Schedule] UI Date: \(uiTargetDate), Alarm Date: \(alarmTargetDate)")
        #endif

        applyAlarms(alarmResult)
    }

    func applyAlarms(_ schedule: FamilySchedule) {
        guard isAlarmEnabled else {
            if let myId = myMemberId {
                AlarmService.shared.cancelWakeUp(memberId: myId)
            }
            return
        }

        let cal = Calendar.current
        let now = Date()
        let today = cal.startOfDay(for: now)
        guard let tomorrow = cal.date(byAdding: .day, value: 1, to: today) else { return }

        let targetDate: Date
        if let target = schedule.targetDate {
            targetDate = target
        } else if let myId = myMemberId,
                  let mySched = schedule.memberSchedules.first(where: { $0.member.id == myId }) {
            let wakeUpTime = mySched.wakeUpTime
            let todayAlarmDate = date(from: wakeUpTime, on: today) ?? now
            targetDate = now > todayAlarmDate ? tomorrow : today
        } else {
            targetDate = today
        }

        if isAwakeTodayLocal && targetDate == today {
            if let myId = myMemberId {
                AlarmService.shared.cancelWakeUp(memberId: myId)
            }
            return
        }

        let dateFormatter = DateFormatter()
        dateFormatter.calendar = Calendar(identifier: .gregorian)
        dateFormatter.locale = Locale(identifier: "en_US_POSIX")
        dateFormatter.dateFormat = "yyyy-MM-dd"
        let targetDateStr = dateFormatter.string(from: targetDate)
        if let vacUntil = vacationUntil, !vacUntil.isEmpty, targetDateStr <= vacUntil {
            if let myId = myMemberId {
                AlarmService.shared.cancelWakeUp(memberId: myId)
            }
            return
        }

        if let snoozeUntil = snoozeUntil, snoozeUntil > Date() {
            if let myId = myMemberId,
               let mySched = schedule.memberSchedules.first(where: { $0.member.id == myId }) {
                AlarmService.shared.scheduleWakeUp(
                    wakeUpTime: snoozeUntil,
                    memberId: myId,
                    memberName: mySched.member.name,
                    soundUri: alarmSoundUri,
                    isSnooze: true
                )
            }
            return
        }

        guard let currentMyMemberId = myMemberId else { return }
        guard let memberSchedule = schedule.memberSchedules.first(where: { $0.member.id == currentMyMemberId }) else {
            AlarmService.shared.cancelWakeUp(memberId: currentMyMemberId)
            return
        }

        let wakeUpTime = memberSchedule.wakeUpTime

        if targetDate != today && !isAwakeTodayEffective() {
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
                if ProcessInfo.processInfo.arguments.contains("-screenshotMode") {
                    return
                }
                if let err = UserDefaults.standard.string(forKey: "last_alarm_error") {
                    self?.errorMessage = L.errorAlarmPermission + "\nERROR: " + err
                } else {
                    self?.errorMessage = L.errorAlarmPermission
                }
            },
            onSuccess: { [weak self] in
                if self?.errorMessage == L.errorAlarmPermission {
                    self?.errorMessage = nil
                }
            }
        )
    }

    func date(from comps: DateComponents, on date: Date) -> Date? {
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

            for (_, updatedMember) in updatedMembersMap {
                self.addOrUpdateMember(updatedMember)
            }
        }
    }

    /// Ermittelt den aktuell relevanten DayProfile-Slot für einen Member.
    func resolveEffectiveMember(_ member: FamilyMember, forDate: Date? = nil) -> FamilyMember {
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
        resolved.breakfastDurationMinutes = profile.breakfastDurationMinutes ?? member.breakfastDurationMinutes
        resolved.dayProfiles = [targetDow: profile]
        resolved.isSimpleMode = profile.isSimpleMode

        let isToday = cal.isDate(targetDate, inSameDayAs: today)
        if !isToday {
            resolved.snoozeUntil = nil
            resolved.snoozeCount = 0
        }

        if isToday, let snoozeEnd = resolved.snoozeUntil, snoozeEnd > now, cal.isDate(snoozeEnd, inSameDayAs: targetDate) {
            let snoozeComps = cal.dateComponents([.hour, .minute], from: snoozeEnd)

            resolved.earliestWakeUp = DateComponents.from(
                hour: snoozeComps.hour ?? 0,
                minute: snoozeComps.minute ?? 0
            )
            resolved.latestWakeUp = resolved.earliestWakeUp

            let absorbableMinutes: Int
            if resolved.snoozeCount <= 1 {
                absorbableMinutes = min(
                    SnoozeConfig.snoozeDurationMinutes,
                    max(0, resolved.bathroomDurationMinutes - Int(SnoozeConfig.minBathroomMinutes))
                )
            } else {
                absorbableMinutes = 0
            }
            resolved.bathroomDurationMinutes = resolved.bathroomDurationMinutes - absorbableMinutes
            #if DEBUG
            print("[Snooze] \(member.name): count=\(resolved.snoozeCount), absorbed=\(absorbableMinutes), bath=\(resolved.bathroomDurationMinutes)")
            #endif
        } else {
            resolved.snoozeUntil = nil
            resolved.snoozeCount = 0
        }

        return resolved
    }

    func sendSnoozeShiftNotification(name: String, time: String) {
        let content = UNMutableNotificationContent()
        content.title = L.notifSnoozeShiftTitle
        content.body = L.notifSnoozeShiftBody(name, time)
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: "snooze_shift_\(UUID().uuidString)",
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request) { error in
            if let error {
                print("Snooze-Shift Notification Fehler: \(error.localizedDescription)")
            }
        }
    }
}
