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
        val fixedPermutation = listOf(activeMembers)

        // Versuche die exakte Reihung ohne Zeit-Verschiebung
        val result = findBestScheduleOverPermutations(fixedPermutation, activeMembers, breakfastDurationMinutes, 0)

        if (result.isSuccess) return result.getOrThrow()

        // Fallback 1: Erlaube moderate Zeit-Verschiebungen (5-15 Min) bei festgehaltener Reihung
        for (shiftMinutes in 5..15 step 5) {
            val flexibleResult = findBestScheduleOverPermutations(fixedPermutation, activeMembers, breakfastDurationMinutes, shiftMinutes)
            flexibleResult.onSuccess { flexibleSchedule ->
                return flexibleSchedule.copy(scheduleMessage = ScheduleMessage.TimeAdjusted(shiftMinutes))
            }
        }

        // Fallback 2: Frühstück leicht verkürzen und mit Verschiebungen erneut probieren
        if (breakfastDurationMinutes >= 15) {
            for (reduceBreakfast in 5..10 step 5) {
                val reducedDuration = breakfastDurationMinutes - reduceBreakfast
                val reductionResult = findBestScheduleOverPermutations(fixedPermutation, activeMembers, reducedDuration, 0)

                reductionResult.onSuccess { sched ->
                    return sched.copy(scheduleMessage = ScheduleMessage.BreakfastReduced(reduceBreakfast))
                }

                // Kombiniere Frühstücksverkürzung mit Zeit-Verschiebung
                for (shiftMinutes in 5..15 step 5) {
                    val flexibleReductionResult = findBestScheduleOverPermutations(fixedPermutation, activeMembers, reducedDuration, shiftMinutes)
                    flexibleReductionResult.onSuccess { flexibleSchedule ->
                        return flexibleSchedule.copy(scheduleMessage = ScheduleMessage.BreakfastAndTimeAdjusted(reduceBreakfast, shiftMinutes))
                    }
                }
            }
        }

        // Letzten Fehler aus dem initialen Versuch extrahieren
        val conflictException = result.exceptionOrNull()
        val lastMessage = if (conflictException != null) {
            // Versuche den Mitgliedsnamen aus der Exception-Message zu extrahieren
            val memberName = conflictException.message
                ?.removePrefix("CONFLICT:")
                ?.substringBefore(":")
                ?.trim()
            if (!memberName.isNullOrBlank()) {
                ScheduleMessage.MemberConflict(memberName)
            } else {
                ScheduleMessage.NoValidScheduleFound
            }
        } else {
            ScheduleMessage.NoValidScheduleFound
        }

        return FamilySchedule(
            emptyList(), null, false,
            lastMessage
        )
    }

    private fun findBestScheduleOverPermutations(
        permutations: List<List<FamilyMember>>,
        members: List<FamilyMember>,
        breakfastDurationMinutes: Long,
        shiftToleranceMinutes: Int
    ): Result<FamilySchedule> {
        var bestSchedule: FamilySchedule? = null
        var bestScore = -1L
        var lastError: Exception? = null

        for (perm in permutations) {
            val result = evaluatePermutation(perm, breakfastDurationMinutes, shiftToleranceMinutes)
            result.onSuccess { scheduleOpt ->
                val score = scheduleOpt.memberSchedules.sumOf { it.wakeUpTime.toSecondOfDay().toLong() }
                if (score > bestScore) {
                    bestScore = score
                    bestSchedule = scheduleOpt
                }
            }.onFailure { exception ->
                lastError = exception as? Exception ?: Exception(exception.message)
            }
        }

        return if (bestSchedule != null) {
            Result.success(bestSchedule!!)
        } else {
            Result.failure(lastError ?: Exception())
        }
    }

    private fun evaluatePermutation(
        orderedMembers: List<FamilyMember>,
        breakfastDurationMinutes: Long,
        shiftToleranceMinutes: Int = 0
    ): Result<FamilySchedule> {
        val breakfastEaters = orderedMembers.filter { it.wantsBreakfast }
        var breakfastTime: LocalTime? = null

        if (breakfastEaters.isNotEmpty()) {
            var minLeaveForBreakfastEaters = LocalTime.of(23, 59)
            for (m in breakfastEaters) {
                // Ist kein leaveHomeTime gesetzt, nutzen wir latestWakeUp + bathroomDuration
                // als natürliche "Bad fertig"-Obergrenze (verhindert Fallback auf 23:59)
                val naturalBathEnd = m.latestWakeUp.plusMinutes(m.bathroomDurationMinutes)
                val leave = m.leaveHomeTime ?: naturalBathEnd
                if (leave.isBefore(minLeaveForBreakfastEaters)) {
                    minLeaveForBreakfastEaters = leave
                }
            }
            // Limit to a reasonable start time (not before 04:00)
            val startTime = if (minLeaveForBreakfastEaters.isBefore(LocalTime.of(4, 0)))
                LocalTime.of(4, 0) else minLeaveForBreakfastEaters

            breakfastTime = startTime.minusMinutes(breakfastDurationMinutes)
        }

        val schedules = mutableListOf<ScheduleResult>()
        var currentLatestBathroomEndTime = LocalTime.of(23, 59)

        for (member in orderedMembers.reversed()) {
            val allowedLatestWakeUp = member.latestWakeUp.plusMinutes(shiftToleranceMinutes.toLong())
            val allowedEarliestWakeUp = member.earliestWakeUp.minusMinutes(shiftToleranceMinutes.toLong())

            var maxAllowedBathroomEnd = allowedLatestWakeUp.plusMinutes(member.bathroomDurationMinutes)

            if (currentLatestBathroomEndTime.isBefore(maxAllowedBathroomEnd)) {
                maxAllowedBathroomEnd = currentLatestBathroomEndTime
            }

            // Fix: !isAfter statt isBefore – deckt auch den ==Fall ab (0 Min Frühstückszeit)
            if (member.wantsBreakfast && breakfastTime != null && !breakfastTime.isAfter(maxAllowedBathroomEnd)) {
                maxAllowedBathroomEnd = breakfastTime
            }

            val leaveTime = member.leaveHomeTime
            if (leaveTime != null && leaveTime.isBefore(maxAllowedBathroomEnd)) {
                maxAllowedBathroomEnd = leaveTime
            }

            val wakeUpTime = maxAllowedBathroomEnd.minusMinutes(member.bathroomDurationMinutes)

            if (wakeUpTime.isBefore(allowedEarliestWakeUp)) {
                // Prefix mit "CONFLICT:{memberName}:" für die Exception-Extraktion in calculateIdealSchedule
                return Result.failure(Exception("CONFLICT:${member.name}:${wakeUpTime}:${allowedEarliestWakeUp}"))
            }

            // Guard gegen Mitternacht-Overflow: Errechnete Weckzeit liegt vor 03:00 → unrealistisch
            // einen LocalTime-Wrap-Around hin (z. B. 90 Min Badzeit mit leaveHomeTime 02:00).
            if (wakeUpTime.isBefore(LocalTime.of(3, 0))) {
                return Result.failure(Exception("CONFLICT:${member.name}:${wakeUpTime}:midnight-guard"))
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

        // Post-Validation: Sicherheitsnetz fuer Edge Cases bei der Fruehstueck-Constraint-Berechnung.
        // Pruefe explizit dass kein Fruehstuecker sein Bad nach Fruehstücksbeginn beendet.
        if (breakfastTime != null) {
            for (s in schedules) {
                if (s.member.wantsBreakfast && s.bathroomEndTime.isAfter(breakfastTime)) {
                    return Result.failure(
                        Exception("CONFLICT:${s.member.name}:Bad endet nach Fruehstueck (${s.bathroomEndTime} > $breakfastTime)")
                    )
                }
            }
        }

        return Result.success(
            FamilySchedule(
                memberSchedules = schedules.reversed(),
                breakfastTime = breakfastTime,
                isValid = true,
                scheduleMessage = ScheduleMessage.OptimalPlan
            )
        )
    }
}
