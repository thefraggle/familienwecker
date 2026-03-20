package de.familienwecker.famwake.algorithm

import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.model.FamilySchedule
import de.familienwecker.famwake.model.ScheduleMessage
import de.familienwecker.famwake.model.ScheduleResult
import java.time.LocalTime

class Scheduler {

    fun calculateIdealSchedule(
        members: List<FamilyMember>,
        breakfastDurationMinutes: Long = 30
    ): FamilySchedule {
        // Limit active members (max 6 per UI constraint)
        val activeMembers = members.filter { !it.isPaused }.take(6)

        if (activeMembers.isEmpty()) {
            return FamilySchedule(emptyList(), null, true, ScheduleMessage.NoActiveMembers)
        }

        // Nutze exakt die übergebene Reihung (Manuelle Sortierung)
        val fixedPermutations = listOf(activeMembers)

        // 1. Versuche die exakte Reihung ohne Zeit-Verschiebung
        val initialResult = evaluatePermutation(activeMembers, breakfastDurationMinutes, 0, includeInvalid = true)
        if (initialResult.isValid) return initialResult

        // 2. Fallback: Erlaube moderate Zeit-Verschiebungen (5-15 Min) bei festgehaltener Reihung
        for (shiftMinutes in 5..15 step 5) {
            val flexibleSchedule = evaluatePermutation(activeMembers, breakfastDurationMinutes, shiftMinutes)
            if (flexibleSchedule.isValid) {
                return flexibleSchedule.copy(scheduleMessage = ScheduleMessage.TimeAdjusted(shiftMinutes))
            }
        }

        // 3. Fallback: Frühstück leicht verkürzen und mit Verschiebungen erneut probieren
        if (breakfastDurationMinutes >= 15) {
            for (reduceBreakfast in 5..10 step 5) {
                val reducedDuration = breakfastDurationMinutes - reduceBreakfast
                val reductionSchedule = evaluatePermutation(activeMembers, reducedDuration, 0)

                if (reductionSchedule.isValid) {
                    return reductionSchedule.copy(scheduleMessage = ScheduleMessage.BreakfastReduced(reduceBreakfast))
                }

                // Kombiniere Frühstücksverkürzung mit Zeit-Verschiebung
                for (shiftMinutes in 5..15 step 5) {
                    val flexibleReductionSchedule = evaluatePermutation(activeMembers, reducedDuration, shiftMinutes)
                    if (flexibleReductionSchedule.isValid) {
                        return flexibleReductionSchedule.copy(scheduleMessage = ScheduleMessage.BreakfastAndTimeAdjusted(reduceBreakfast, shiftMinutes))
                    }
                }
            }
        }

        // 4. Endgültiger Fehlschlag: Best-Effort Plan zurückgeben (vom ersten Versuch)
        return initialResult.copy(
            isValid = false,
            scheduleMessage = extractConflictMessage(initialResult)
        )
    }

    private fun extractConflictMessage(schedule: FamilySchedule): ScheduleMessage {
        // Findet das erste Mitglied, das den Check gerissen hat (wakeUpTime < earliestWakeUp)
        for (s in schedule.memberSchedules) {
            if (s.wakeUpTime.isBefore(s.member.earliestWakeUp)) {
                return ScheduleMessage.MemberConflict(s.member.name)
            }
        }
        return ScheduleMessage.NoValidScheduleFound
    }

    private fun evaluatePermutation(
        orderedMembers: List<FamilyMember>,
        breakfastDurationMinutes: Long,
        shiftToleranceMinutes: Int = 0,
        includeInvalid: Boolean = false
    ): FamilySchedule {
        val breakfastEaters = orderedMembers.filter { it.wantsBreakfast }
        var breakfastTime: LocalTime? = null

        if (breakfastEaters.isNotEmpty()) {
            var minLeaveForBreakfastEaters = LocalTime.of(23, 59)
            for (m in breakfastEaters) {
                val naturalBathEnd = m.latestWakeUp.plusMinutes(m.bathroomDurationMinutes)
                val leave = m.leaveHomeTime ?: naturalBathEnd
                if (leave.isBefore(minLeaveForBreakfastEaters)) {
                    minLeaveForBreakfastEaters = leave
                }
            }
            val startTime = if (minLeaveForBreakfastEaters.isBefore(LocalTime.of(4, 0)))
                LocalTime.of(4, 0) else minLeaveForBreakfastEaters

            breakfastTime = startTime.minusMinutes(breakfastDurationMinutes)
        }

        val schedules = mutableListOf<ScheduleResult>()
        var currentLatestBathroomEndTime = LocalTime.of(23, 59)
        var isValid = true

        for (member in orderedMembers.reversed()) {
            val allowedLatestWakeUp = member.latestWakeUp.plusMinutes(shiftToleranceMinutes.toLong())
            val allowedEarliestWakeUp = member.earliestWakeUp.minusMinutes(shiftToleranceMinutes.toLong())

            var maxAllowedBathroomEnd = allowedLatestWakeUp.plusMinutes(member.bathroomDurationMinutes)

            if (currentLatestBathroomEndTime.isBefore(maxAllowedBathroomEnd)) {
                maxAllowedBathroomEnd = currentLatestBathroomEndTime
            }

            if (member.wantsBreakfast && breakfastTime != null && !breakfastTime.isAfter(maxAllowedBathroomEnd)) {
                maxAllowedBathroomEnd = breakfastTime
            }

            val leaveTime = member.leaveHomeTime
            if (leaveTime != null && leaveTime.isBefore(maxAllowedBathroomEnd)) {
                maxAllowedBathroomEnd = leaveTime
            }

            val wakeUpTime = maxAllowedBathroomEnd.minusMinutes(member.bathroomDurationMinutes)

            if (wakeUpTime.isBefore(allowedEarliestWakeUp)) {
                if (!includeInvalid) {
                    return FamilySchedule(emptyList(), null, false, ScheduleMessage.NoValidScheduleFound)
                }
                isValid = false
            }

            schedules.add(
                ScheduleResult(
                    member = member,
                    wakeUpTime = wakeUpTime,
                    bathroomStartTime = wakeUpTime,
                    bathroomEndTime = maxAllowedBathroomEnd
                )
            )
            currentLatestBathroomEndTime = wakeUpTime
        }

        // Post-Validation
        if (breakfastTime != null && isValid) {
            for (s in schedules) {
                if (s.member.wantsBreakfast && s.bathroomEndTime.isAfter(breakfastTime)) {
                    isValid = false
                    if (!includeInvalid) return FamilySchedule(emptyList(), null, false, ScheduleMessage.NoValidScheduleFound)
                }
            }
        }

        return FamilySchedule(
            memberSchedules = schedules.reversed(),
            breakfastTime = breakfastTime,
            isValid = isValid,
            scheduleMessage = if (isValid) ScheduleMessage.OptimalPlan else ScheduleMessage.NoValidScheduleFound
        )
    }
}
