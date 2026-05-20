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

        for member in orderedMembers.reversed() {
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

            let wakeUpTime = maxBathroomEnd.subtracting(minutes: member.bathroomDurationMinutes)

            if wakeUpTime < allowedEarliest {
                if !includeInvalid {
                    return FamilySchedule(memberSchedules: [], breakfastTime: nil, isValid: false, scheduleMessage: .memberConflict(""))
                }
                isValid = false
            }

            let activeProfile = member.dayProfiles?.sorted(by: { $0.key < $1.key }).first?.value
            var effectiveBuffer = globalBufferMinutes
            if let pBuffer = activeProfile?.bufferMinutes, pBuffer > 0 {
                effectiveBuffer = pBuffer
            }

            schedules.append(MemberSchedule(
                member: member,
                wakeUpTime: wakeUpTime,
                bathroomStart: wakeUpTime,
                bathroomEnd: maxBathroomEnd,
                bufferAfter: effectiveBuffer
            ))
            
            currentLatestBathroomEnd = wakeUpTime.subtracting(minutes: effectiveBuffer)
        }

        // Post-Validation Frühstück
        if let bt = breakfastTime, isValid {
            for s in schedules {
                if s.member.wantsBreakfast && bt < s.bathroomEnd {
                    isValid = false
                    if !includeInvalid {
                        return FamilySchedule(memberSchedules: [], breakfastTime: nil, isValid: false, scheduleMessage: .memberConflict(""))
                    }
                }
            }
        }

        return FamilySchedule(
            memberSchedules: schedules.reversed(),
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
        if total < 0 { total = 0 }
        return DateComponents(hour: total / 60, minute: total % 60)
    }
}
