import Foundation

// MARK: - Scheduler
// 1:1 Port von shared/src/commonMain/.../algorithm/Scheduler.kt

struct Scheduler {

    func calculateIdealSchedule(
        members: [FamilyMember],
        breakfastDurationMinutes: Int = 30,
        globalBufferMinutes: Int = 0
    ) -> FamilySchedule {

        let allActiveMembers = members.filter { !$0.isPaused }
        let activeMembers = Array(allActiveMembers.prefix(6))
        let memberLimitExceeded = allActiveMembers.count > 6

        if activeMembers.isEmpty {
            return FamilySchedule(memberSchedules: [], breakfastTime: nil, isValid: true, scheduleMessage: .noActiveSchedule)
        }

        // 1. Versuche die exakte Reihung ohne Zeit-Verschiebung, mit vollem Puffer
        let initialResult = evaluatePermutation(activeMembers, breakfastDurationMinutes: breakfastDurationMinutes, shiftMinutes: 0, globalBufferMinutes: globalBufferMinutes, includeInvalid: true)
        if initialResult.isValid {
            if memberLimitExceeded {
                var limitedResult = initialResult
                limitedResult.scheduleMessage = .memberLimitExceeded(allActiveMembers.count, 6)
                return limitedResult
            }
            return initialResult
        }

        // 2. Puffer schrittweise reduzieren (5er-Schritte)
        if globalBufferMinutes > 0 {
            var reducedBuffer = globalBufferMinutes - 5
            while reducedBuffer >= 0 {
                let bufferResult = evaluatePermutation(activeMembers, breakfastDurationMinutes: breakfastDurationMinutes, shiftMinutes: 0, globalBufferMinutes: reducedBuffer)
                if bufferResult.isValid {
                    var finalRes = bufferResult
                    finalRes.scheduleMessage = .bufferReduced(globalBufferMinutes, reducedBuffer)
                    return finalRes
                }
                reducedBuffer -= 5
            }
        }

        // 3. Fallback: Erlaube moderate Zeit-Verschiebungen (5-15 Min) bei festgehaltener Reihung, ohne Puffer
        for shift in stride(from: 5, through: 15, by: 5) {
            let flexibleSchedule = evaluatePermutation(activeMembers, breakfastDurationMinutes: breakfastDurationMinutes, shiftMinutes: shift, globalBufferMinutes: 0)
            if flexibleSchedule.isValid {
                var finalRes = flexibleSchedule
                finalRes.scheduleMessage = .timeAdjusted(shift)
                return finalRes
            }
        }

        // 4. Fallback: Frühstück leicht verkürzen und mit Verschiebungen erneut probieren
        if breakfastDurationMinutes >= 15 {
            for reduceBreakfast in stride(from: 5, through: 10, by: 5) {
                let reducedDuration = breakfastDurationMinutes - reduceBreakfast
                let reductionSchedule = evaluatePermutation(activeMembers, breakfastDurationMinutes: reducedDuration, shiftMinutes: 0, globalBufferMinutes: 0)

                if reductionSchedule.isValid {
                    var finalRes = reductionSchedule
                    finalRes.scheduleMessage = .breakfastReduced(reduceBreakfast)
                    return finalRes
                }

                // Kombiniere Frühstücksverkürzung mit Zeit-Verschiebung
                for shift in stride(from: 5, through: 15, by: 5) {
                    let flexibleReductionSchedule = evaluatePermutation(activeMembers, breakfastDurationMinutes: reducedDuration, shiftMinutes: shift, globalBufferMinutes: 0)
                    if flexibleReductionSchedule.isValid {
                        var finalRes = flexibleReductionSchedule
                        finalRes.scheduleMessage = .breakfastAndTimeAdjusted(reduceBreakfast, shift)
                        return finalRes
                    }
                }
            }
        }

        // 5. Endgültiger Fehlschlag: Best-Effort Plan zurückgeben (vom ersten Versuch)
        var failedResult = initialResult
        failedResult.isValid = false
        failedResult.scheduleMessage = extractConflictMessage(initialResult)
        return failedResult
    }

    // MARK: - Private

    private func extractConflictMessage(_ schedule: FamilySchedule) -> ScheduleMessage {
        for s in schedule.memberSchedules {
            if s.wakeUpTime < s.member.earliestWakeUp {
                return .memberConflict(s.member.name)
            }
        }
        return .noValidScheduleFound
    }

    private func evaluatePermutation(
        _ orderedMembers: [FamilyMember],
        breakfastDurationMinutes: Int,
        shiftMinutes: Int,
        globalBufferMinutes: Int,
        includeInvalid: Bool = false
    ) -> FamilySchedule {

        let breakfastEaters = orderedMembers.filter { $0.wantsBreakfast && !$0.isSimpleMode }
        var breakfastTime: DateComponents? = nil
        var minLeaveForBreakfastEaters = DateComponents(hour: 23, minute: 59)

        if !breakfastEaters.isEmpty {
            for m in breakfastEaters {
                let naturalBathEnd = m.latestWakeUp.adding(minutes: max(0, m.bathroomDurationMinutes))
                let leave = m.leaveHomeTime ?? naturalBathEnd
                if leave < minLeaveForBreakfastEaters {
                    minLeaveForBreakfastEaters = leave
                }
            }
            
            // Safety-Guard: Verhindert Mitternachts-Überlauf (negative Zeit bzw. Sprung zurück zum Vortag).
            // Unterstützt auch frühe Schichten (z. B. vor 04:00 Uhr).
            let startTime = minLeaveForBreakfastEaters
            let calculatedBreakfast = startTime.subtracting(minutes: breakfastDurationMinutes)
            
            // Wraparound Check (über Mitternacht zurück)
            if startTime < calculatedBreakfast {
                breakfastTime = DateComponents(hour: 0, minute: 0)
            } else {
                breakfastTime = calculatedBreakfast
            }
        }

        var schedules: [MemberSchedule] = []
        var currentLatestBathroomEnd = DateComponents(hour: 23, minute: 59)
        var isValid = true

        for index in stride(from: orderedMembers.count - 1, through: 0, by: -1) {
            let member = orderedMembers[index]
            if member.isSimpleMode {
                let wakeUpTime = member.latestWakeUp
                schedules.append(MemberSchedule(
                    member: member,
                    wakeUpTime: wakeUpTime,
                    bathroomStart: wakeUpTime,
                    bathroomEnd: wakeUpTime,
                    bufferAfter: 0
                ))
                continue
            }

            let rawAllowedLatest = member.latestWakeUp.adding(minutes: shiftMinutes)
            let allowedLatest = rawAllowedLatest < member.latestWakeUp ? DateComponents(hour: 23, minute: 59) : rawAllowedLatest
            let rawAllowedEarliest = member.earliestWakeUp.subtracting(minutes: shiftMinutes)
            let allowedEarliest = member.earliestWakeUp < rawAllowedEarliest ? DateComponents(hour: 0, minute: 0) : rawAllowedEarliest

            // Fixierte Weckzeit (z.B. durch Snooze oder 0-Minuten-Fenster): earliestWakeUp == latestWakeUp
            let isFixed = member.earliestWakeUp == member.latestWakeUp
            let isMemberSnoozed = member.snoozeUntil != nil

            let bathDuration = max(0, member.bathroomDurationMinutes)
            let rawMaxBathroomEnd = allowedLatest.adding(minutes: bathDuration)
            var maxBathroomEnd = rawMaxBathroomEnd < allowedLatest ? DateComponents(hour: 23, minute: 59) : rawMaxBathroomEnd

            if currentLatestBathroomEnd < maxBathroomEnd {
                maxBathroomEnd = currentLatestBathroomEnd
            }

            if member.wantsBreakfast, let bt = breakfastTime {
                let effectiveDuration = member.breakfastDurationMinutes ?? breakfastDurationMinutes
                let memberBreakfastStart: DateComponents
                if effectiveDuration == breakfastDurationMinutes {
                    memberBreakfastStart = bt
                } else {
                    let mStart = !breakfastEaters.isEmpty ? minLeaveForBreakfastEaters : DateComponents(hour: 8, minute: 0)
                    let calculated = mStart.subtracting(minutes: effectiveDuration)
                    memberBreakfastStart = mStart < calculated ? DateComponents(hour: 0, minute: 0) : calculated
                }

                if maxBathroomEnd.totalMinutes >= memberBreakfastStart.totalMinutes {
                    maxBathroomEnd = memberBreakfastStart
                }
            }

            if let leave = member.leaveHomeTime, leave < maxBathroomEnd {
                maxBathroomEnd = leave
            }

            // Bei fixierter Weckzeit: exakt diese Zeit verwenden
            let wakeUpTime: DateComponents
            let bathroomEnd: DateComponents
            if isFixed {
                wakeUpTime = member.latestWakeUp
                let rawEnd = wakeUpTime.adding(minutes: bathDuration)
                bathroomEnd = rawEnd < wakeUpTime ? DateComponents(hour: 23, minute: 59) : rawEnd
                if !isMemberSnoozed && maxBathroomEnd < bathroomEnd {
                    if !includeInvalid {
                        return FamilySchedule(memberSchedules: [], breakfastTime: nil, isValid: false, scheduleMessage: .noValidScheduleFound)
                    }
                    isValid = false
                }
            } else {
                let rawWake = maxBathroomEnd.subtracting(minutes: bathDuration)
                wakeUpTime = maxBathroomEnd < rawWake ? DateComponents(hour: 0, minute: 0) : rawWake
                bathroomEnd = maxBathroomEnd
            }

            if !isFixed && wakeUpTime < allowedEarliest {
                if !includeInvalid {
                    return FamilySchedule(memberSchedules: [], breakfastTime: nil, isValid: false, scheduleMessage: .noValidScheduleFound)
                }
                isValid = false
            }

            // Der Puffer nach diesem Mitglied bestimmt den Abstand zum Nachfolgenden.
            // Wenn es kein nachfolgendes Mitglied gibt (letztes Element), ist der Puffer 0.
            var effectiveBuffer = 0
            if index < orderedMembers.count - 1 {
                // dayProfiles enthält nach resolveEffectiveMember() nur 1 Entry (den aktuellen Wochentag),
                // daher ist .first hier korrekt und liefert immer das richtige Tagesprofil.
                let activeProfile = member.dayProfiles?.sorted(by: { $0.key < $1.key }).first?.value
                effectiveBuffer = activeProfile?.bufferMinutes ?? globalBufferMinutes
            }

            // Für den nächsten Schritt (das vorhergehende Mitglied) ziehen wir den Puffer
            // des vorhergehenden Mitglieds von unserer Wakeup-Zeit ab.
            var prevBuffer = 0
            if index > 0 {
                let prevMember = orderedMembers[index - 1]
                if !prevMember.isSimpleMode {
                    // dayProfiles enthält nach resolveEffectiveMember() nur 1 Entry (den aktuellen Wochentag),
                    // daher ist .first hier korrekt und liefert immer das richtige Tagesprofil.
                    let activeProfile = prevMember.dayProfiles?.sorted(by: { $0.key < $1.key }).first?.value
                    prevBuffer = activeProfile?.bufferMinutes ?? globalBufferMinutes
                }
            }

            schedules.append(MemberSchedule(
                member: member,
                wakeUpTime: wakeUpTime,
                bathroomStart: wakeUpTime,
                bathroomEnd: bathroomEnd,
                bufferAfter: effectiveBuffer
            ))
            
            let rawPrevEnd = wakeUpTime.subtracting(minutes: prevBuffer)
            currentLatestBathroomEnd = wakeUpTime < rawPrevEnd ? DateComponents(hour: 0, minute: 0) : rawPrevEnd
        }

        // Prüfe ob ein Snooze aktiv ist – nur bei echtem Snooze werden temporäre Verschiebungen toleriert
        let hasSnoozeActive = orderedMembers.contains { !$0.isSimpleMode && $0.snoozeUntil != nil }

        // Vorwärts-Korrektur-Pass: Wenn ein fixierter Member (z.B. Snooze) seine Badzeit
        // in den Slot des Nachfolgers schiebt, werden nachfolgende Bad-nutzende Members verschoben.
        var forwardSchedules = Array(schedules.reversed())
        var lastBathIndex = -1
        for i in 0..<forwardSchedules.count {
            let next = forwardSchedules[i]
            if next.member.isSimpleMode { continue }

            if lastBathIndex >= 0 {
                let current = forwardSchedules[lastBathIndex]
                let buffer = current.bufferAfter
                let rawRequiredStart = current.bathroomEnd.adding(minutes: buffer)
                let requiredNextStart = rawRequiredStart < current.bathroomEnd ? DateComponents(hour: 23, minute: 59) : rawRequiredStart

                if next.wakeUpTime < requiredNextStart {
                    let shiftedWakeUp = requiredNextStart
                    let nextBathDuration = max(0, next.member.bathroomDurationMinutes)
                    let rawShiftedEnd = shiftedWakeUp.adding(minutes: nextBathDuration)
                    let shiftedBathroomEnd = rawShiftedEnd < shiftedWakeUp ? DateComponents(hour: 23, minute: 59) : rawShiftedEnd

                    if !hasSnoozeActive {
                        let rawNextAllowedLatest = next.member.latestWakeUp.adding(minutes: shiftMinutes)
                        let nextAllowedLatest = rawNextAllowedLatest < next.member.latestWakeUp ? DateComponents(hour: 23, minute: 59) : rawNextAllowedLatest
                        if nextAllowedLatest < shiftedWakeUp || (next.member.leaveHomeTime != nil && next.member.leaveHomeTime! < shiftedBathroomEnd) {
                            isValid = false
                            if !includeInvalid {
                                return FamilySchedule(memberSchedules: [], breakfastTime: nil, isValid: false, scheduleMessage: .noValidScheduleFound)
                            }
                        }
                    }

                    forwardSchedules[i] = MemberSchedule(
                        member: next.member,
                        wakeUpTime: shiftedWakeUp,
                        bathroomStart: shiftedWakeUp,
                        bathroomEnd: shiftedBathroomEnd,
                        bufferAfter: next.bufferAfter
                    )
                }
            }
            lastBathIndex = i
        }

        // Post-Validation Frühstück (bei Snooze tolerieren, da temporär)
        if let bt = breakfastTime, isValid, !hasSnoozeActive {
            for s in forwardSchedules {
                if s.member.wantsBreakfast && !s.member.isSimpleMode {
                    let cutoff: DateComponents
                    if let dur = s.member.breakfastDurationMinutes, dur != breakfastDurationMinutes {
                        let mStart = !breakfastEaters.isEmpty ? minLeaveForBreakfastEaters : DateComponents(hour: 8, minute: 0)
                        let calculated = mStart.subtracting(minutes: dur)
                        cutoff = mStart < calculated ? DateComponents(hour: 0, minute: 0) : calculated
                    } else {
                        cutoff = bt
                    }

                    if cutoff < s.bathroomEnd {
                        isValid = false
                        if !includeInvalid {
                            return FamilySchedule(memberSchedules: [], breakfastTime: nil, isValid: false, scheduleMessage: .noValidScheduleFound)
                        }
                    }
                }
            }
        }

        // Bei Snooze-bedingter Verschiebung: Plan als gültig markieren, da die
        // Verschiebung temporär ist und kein User-Eingriff erforderlich ist.
        if hasSnoozeActive && !isValid {
            isValid = true
        }

        return FamilySchedule(
            memberSchedules: forwardSchedules,
            breakfastTime: breakfastTime,
            isValid: isValid,
            scheduleMessage: isValid ? .optimal : .noValidScheduleFound
        )
    }
}

// MARK: - DateComponents Arithmetic Helpers (Scheduler)
private extension DateComponents {
    func adding(minutes: Int) -> DateComponents {
        var total = (self.totalMinutes + minutes) % (24 * 60)
        if total < 0 { total += 24 * 60 }
        return DateComponents(hour: total / 60, minute: total % 60)
    }

    func subtracting(minutes: Int) -> DateComponents {
        return adding(minutes: -minutes)
    }
}
