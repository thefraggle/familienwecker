import Foundation

// MARK: - Scheduler
// 1:1 Port von shared/src/commonMain/.../algorithm/Scheduler.kt

struct Scheduler {

    func calculateIdealSchedule(
        members: [FamilyMember],
        breakfastDurationMinutes: Int = 30
    ) -> FamilySchedule {

        let activeMembers = members.filter { !$0.isPaused }.prefix(6).map { $0 }

        if activeMembers.isEmpty {
            return FamilySchedule(memberSchedules: [], breakfastTime: nil, isValid: true, scheduleMessage: .noActiveSchedule)
        }

        // 1. Exakte Reihung ohne Verschiebung
        let initial = evaluatePermutation(activeMembers, breakfastDurationMinutes: breakfastDurationMinutes, shiftMinutes: 0, includeInvalid: true)
        if initial.isValid { return initial }

        // 2. Fallback: moderate Zeitverschiebung (5–15 Min)
        for shift in stride(from: 5, through: 15, by: 5) {
            let flexible = evaluatePermutation(activeMembers, breakfastDurationMinutes: breakfastDurationMinutes, shiftMinutes: shift)
            if flexible.isValid {
                return FamilySchedule(memberSchedules: flexible.memberSchedules, breakfastTime: flexible.breakfastTime, isValid: true, scheduleMessage: .timeAdjusted)
            }
        }

        // 3. Fallback: Frühstück leicht verkürzen
        if breakfastDurationMinutes >= 15 {
            for reduce in stride(from: 5, through: 10, by: 5) {
                let reduced = breakfastDurationMinutes - reduce
                let r = evaluatePermutation(activeMembers, breakfastDurationMinutes: reduced, shiftMinutes: 0)
                if r.isValid {
                    return FamilySchedule(memberSchedules: r.memberSchedules, breakfastTime: r.breakfastTime, isValid: true, scheduleMessage: .breakfastReduced)
                }
                for shift in stride(from: 5, through: 15, by: 5) {
                    let fr = evaluatePermutation(activeMembers, breakfastDurationMinutes: reduced, shiftMinutes: shift)
                    if fr.isValid {
                        return FamilySchedule(memberSchedules: fr.memberSchedules, breakfastTime: fr.breakfastTime, isValid: true, scheduleMessage: .breakfastAndTimeAdjusted)
                    }
                }
            }
        }

        // 4. Best-Effort (ungültig)
        let conflict = extractConflictMessage(initial)
        return FamilySchedule(memberSchedules: initial.memberSchedules, breakfastTime: initial.breakfastTime, isValid: false, scheduleMessage: conflict)
    }

    // MARK: - Private

    private func evaluatePermutation(
        _ orderedMembers: [FamilyMember],
        breakfastDurationMinutes: Int,
        shiftMinutes: Int,
        includeInvalid: Bool = false
    ) -> FamilySchedule {

        let breakfastEaters = orderedMembers.filter { $0.wantsBreakfast }
        var breakfastTime: DateComponents? = nil

        if !breakfastEaters.isEmpty {
            var minLeave = DateComponents(hour: 23, minute: 59) // spätester Grenzwert
            for m in breakfastEaters {
                let naturalBathEnd = m.latestWakeUp.adding(minutes: m.bathroomDurationMinutes)
                let leave = m.leaveHomeTime ?? naturalBathEnd
                if leave.totalMinutes < minLeave.totalMinutes {
                    minLeave = leave
                }
            }
            // Clamp: frühestens 04:00
            let startTime = minLeave.totalMinutes < 4 * 60 ? DateComponents(hour: 4, minute: 0) : minLeave
            breakfastTime = startTime.subtracting(minutes: breakfastDurationMinutes)
        }

        var schedules: [MemberSchedule] = []
        var currentLatestBathroomEnd = DateComponents(hour: 23, minute: 59)
        var isValid = true

        for member in orderedMembers.reversed() {
            let allowedLatest = member.latestWakeUp.adding(minutes: shiftMinutes)
            let allowedEarliest = member.earliestWakeUp.subtracting(minutes: shiftMinutes)

            var maxBathroomEnd = allowedLatest.adding(minutes: member.bathroomDurationMinutes)

            if currentLatestBathroomEnd.totalMinutes < maxBathroomEnd.totalMinutes {
                maxBathroomEnd = currentLatestBathroomEnd
            }

            if member.wantsBreakfast, let bt = breakfastTime, bt.totalMinutes <= maxBathroomEnd.totalMinutes {
                maxBathroomEnd = bt
            }

            if let leave = member.leaveHomeTime, leave.totalMinutes < maxBathroomEnd.totalMinutes {
                maxBathroomEnd = leave
            }

            let wakeUpTime = maxBathroomEnd.subtracting(minutes: member.bathroomDurationMinutes)

            if wakeUpTime.totalMinutes < allowedEarliest.totalMinutes {
                if !includeInvalid {
                    return FamilySchedule(memberSchedules: [], breakfastTime: nil, isValid: false, scheduleMessage: .memberConflict(""))
                }
                isValid = false
            }

            schedules.append(MemberSchedule(
                member: member,
                wakeUpTime: wakeUpTime,
                bathroomStart: wakeUpTime,
                bathroomEnd: maxBathroomEnd
            ))
            currentLatestBathroomEnd = wakeUpTime
        }

        // Post-Validation Frühstück
        if let bt = breakfastTime, isValid {
            for s in schedules {
                if s.member.wantsBreakfast && s.bathroomEnd.totalMinutes > bt.totalMinutes {
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

    private func extractConflictMessage(_ schedule: FamilySchedule) -> ScheduleMessage {
        for s in schedule.memberSchedules {
            if s.wakeUpTime.totalMinutes < s.member.earliestWakeUp.totalMinutes {
                return .memberConflict(s.member.name)
            }
        }
        return .memberConflict("")
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
