import Foundation

// MARK: - Scheduler
// 1:1 Port von shared/src/commonMain/.../algorithm/Scheduler.kt

struct Scheduler {

    func calculateIdealSchedule(
        members: [FamilyMember],
        breakfastDurationMinutes: Int = 30,
        globalBufferMinutes: Int = 0
    ) -> FamilySchedule {

        let activeMembers = members.filter { !$0.isPaused }.prefix(6).map { $0 }

        if activeMembers.isEmpty {
            return FamilySchedule(memberSchedules: [], breakfastTime: nil, isValid: true, scheduleMessage: .noActiveSchedule)
        }

        // 1. Versuche die exakte Reihung ohne Zeit-Verschiebung, mit vollem Puffer
        let initialResult = evaluatePermutation(activeMembers, breakfastDurationMinutes: breakfastDurationMinutes, shiftMinutes: 0, globalBufferMinutes: globalBufferMinutes, includeInvalid: true)
        if initialResult.isValid { return initialResult }

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
        return .memberConflict("")
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

        if !breakfastEaters.isEmpty {
            var minLeaveForBreakfastEaters = DateComponents(hour: 23, minute: 59)
            for m in breakfastEaters {
                let naturalBathEnd = m.latestWakeUp.adding(minutes: m.bathroomDurationMinutes)
                let leave = m.leaveHomeTime ?? naturalBathEnd
                if leave < minLeaveForBreakfastEaters {
                    minLeaveForBreakfastEaters = leave
                }
            }
            
            // Safety-Guard: Clamp auf 04:00
            let startTime = minLeaveForBreakfastEaters.totalMinutes < 4 * 60 ? DateComponents(hour: 4, minute: 0) : minLeaveForBreakfastEaters
            breakfastTime = startTime.subtracting(minutes: breakfastDurationMinutes)
            
            // Wraparound Check (03:30)
            if let bt = breakfastTime, startTime < bt {
                breakfastTime = DateComponents(hour: 3, minute: 30)
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

            let allowedLatest = member.latestWakeUp.adding(minutes: shiftMinutes)
            let allowedEarliest = member.earliestWakeUp.subtracting(minutes: shiftMinutes)

            // Fixierte Weckzeit (z.B. durch Snooze): earliestWakeUp == latestWakeUp
            let isFixed = member.earliestWakeUp == member.latestWakeUp

            var maxBathroomEnd = allowedLatest.adding(minutes: member.bathroomDurationMinutes)

            if currentLatestBathroomEnd < maxBathroomEnd {
                maxBathroomEnd = currentLatestBathroomEnd
            }

            if member.wantsBreakfast, let bt = breakfastTime, maxBathroomEnd.totalMinutes >= bt.totalMinutes {
                maxBathroomEnd = bt
            }

            if let leave = member.leaveHomeTime, leave < maxBathroomEnd {
                maxBathroomEnd = leave
            }

            // Bei fixierter Weckzeit: exakt diese Zeit verwenden
            let wakeUpTime: DateComponents
            let bathroomEnd: DateComponents
            if isFixed {
                wakeUpTime = member.latestWakeUp
                bathroomEnd = wakeUpTime.adding(minutes: member.bathroomDurationMinutes)
            } else {
                wakeUpTime = maxBathroomEnd.subtracting(minutes: member.bathroomDurationMinutes)
                bathroomEnd = maxBathroomEnd
            }

            if !isFixed && wakeUpTime < allowedEarliest {
                if !includeInvalid {
                    return FamilySchedule(memberSchedules: [], breakfastTime: nil, isValid: false, scheduleMessage: .memberConflict(""))
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
            
            currentLatestBathroomEnd = wakeUpTime.subtracting(minutes: prevBuffer)
        }

        // Vorwärts-Korrektur-Pass: Wenn ein fixierter Member (Snooze) seine Badzeit
        // in den Slot des Nachfolgers schiebt, werden nachfolgende Members verschoben.
        var forwardSchedules = Array(schedules.reversed())
        for i in 0..<(forwardSchedules.count - 1) {
            let current = forwardSchedules[i]
            let next = forwardSchedules[i + 1]
            if next.member.isSimpleMode { continue }

            let buffer = current.bufferAfter
            let requiredNextStart = current.bathroomEnd.adding(minutes: buffer)

            if next.wakeUpTime < requiredNextStart {
                let shiftedWakeUp = requiredNextStart
                let shiftedBathroomEnd = shiftedWakeUp.adding(minutes: next.member.bathroomDurationMinutes)
                forwardSchedules[i + 1] = MemberSchedule(
                    member: next.member,
                    wakeUpTime: shiftedWakeUp,
                    bathroomStart: shiftedWakeUp,
                    bathroomEnd: shiftedBathroomEnd,
                    bufferAfter: next.bufferAfter
                )
            }
        }

        // Prüfe ob ein Snooze (fixierter Member) aktiv ist – dann toleriere Verschiebungen
        let hasSnoozeActive = orderedMembers.contains { $0.earliestWakeUp == $0.latestWakeUp }

        // Post-Validation Frühstück (bei Snooze tolerieren, da temporär)
        if let bt = breakfastTime, isValid, !hasSnoozeActive {
            for s in forwardSchedules {
                if s.member.wantsBreakfast && bt < s.bathroomEnd {
                    isValid = false
                    if !includeInvalid {
                        return FamilySchedule(memberSchedules: [], breakfastTime: nil, isValid: false, scheduleMessage: .memberConflict(""))
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
            scheduleMessage: isValid ? .optimal : .memberConflict("")
        )
    }
}

// MARK: - DateComponents Arithmetic Helpers (Scheduler)
private extension DateComponents {
    func adding(minutes: Int) -> DateComponents {
        let total = self.totalMinutes + minutes
        return DateComponents(hour: total / 60, minute: total % 60)
    }

    func subtracting(minutes: Int) -> DateComponents {
        var total = self.totalMinutes - minutes
        // M10: Mitternachts-Wraparound – bei negativer Differenz auf den Vortag wrappen
        // statt auf 0 zu clampen, z.B. 01:00 - 90min = 23:30 statt 00:00
        if total < 0 { total += 24 * 60 }
        return DateComponents(hour: total / 60, minute: total % 60)
    }
}
