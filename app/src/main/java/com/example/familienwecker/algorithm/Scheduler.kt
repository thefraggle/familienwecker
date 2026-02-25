package com.example.familienwecker.algorithm

import com.example.familienwecker.model.FamilyMember
import com.example.familienwecker.model.FamilySchedule
import com.example.familienwecker.model.ScheduleResult
import java.time.LocalTime

class Scheduler {

    fun calculateIdealSchedule(
        members: List<FamilyMember>,
        breakfastDurationMinutes: Long = 30
    ): FamilySchedule {
        val activeMembers = members.filter { !it.isPaused }
        
        if (activeMembers.isEmpty()) return FamilySchedule(emptyList(), null, true, "Keine aktiven Mitglieder vorhanden.")

        val permutations = generatePermutations(activeMembers)

        // Check strictly first
        var bestSchedule = findBestScheduleOverPermutations(permutations, activeMembers, breakfastDurationMinutes, 0)
        
        // Fallback 1: Iteratively allow shifts up to 15 minutes in 5-minute increments
        if (bestSchedule == null || !bestSchedule.isValid) {
            for (shiftMinutes in 5..15 step 5) {
                val flexibleSchedule = findBestScheduleOverPermutations(permutations, activeMembers, breakfastDurationMinutes, shiftMinutes)
                if (flexibleSchedule != null && flexibleSchedule.isValid) {
                    return flexibleSchedule.copy(message = "Zeiten wurden um $shiftMinutes Minuten flexibel angepasst, um Konflikte zu lösen.")
                }
            }
        }

        // Fallback 2: Reduce breakfast time by 5 or 10 minutes and try again with shifts
        if ((bestSchedule == null || !bestSchedule.isValid) && breakfastDurationMinutes >= 15) {
            for (reduceBreakfast in 5..10 step 5) {
                val reducedDuration = breakfastDurationMinutes - reduceBreakfast
                val sched = findBestScheduleOverPermutations(permutations, activeMembers, reducedDuration, 0)
                if (sched != null && sched.isValid) {
                    return sched.copy(message = "Frühstück wurde um $reduceBreakfast Minuten verkürzt, um Konflikte zu lösen.")
                }
                
                // Try shifts with reduced breakfast
                for (shiftMinutes in 5..15 step 5) {
                    val flexibleSchedule = findBestScheduleOverPermutations(permutations, activeMembers, reducedDuration, shiftMinutes)
                    if (flexibleSchedule != null && flexibleSchedule.isValid) {
                        return flexibleSchedule.copy(message = "Frühstück wurde um $reduceBreakfast Min. verkürzt & Zeiten um $shiftMinutes Min. angepasst.")
                    }
                }
            }
        }

        return bestSchedule ?: FamilySchedule(
            emptyList(), null, false, 
            "Kein gültiger Zeitplan gefunden! Zeiten überschneiden sich zu stark."
        )
    }

    private fun findBestScheduleOverPermutations(
        permutations: List<List<FamilyMember>>,
        members: List<FamilyMember>,
        breakfastDurationMinutes: Long,
        shiftToleranceMinutes: Int
    ): FamilySchedule? {
        var bestSchedule: FamilySchedule? = null
        var bestScore = -1L

        for (perm in permutations) {
            val scheduleOpt = evaluatePermutation(perm, breakfastDurationMinutes, shiftToleranceMinutes)
            if (scheduleOpt != null) {
                val score = scheduleOpt.memberSchedules.sumOf { it.wakeUpTime.toSecondOfDay().toLong() }
                if (score > bestScore) {
                    bestScore = score
                    bestSchedule = scheduleOpt
                }
            }
        }
        return bestSchedule
    }

    private fun evaluatePermutation(
        orderedMembers: List<FamilyMember>,
        breakfastDurationMinutes: Long,
        shiftToleranceMinutes: Int = 0
    ): FamilySchedule? {
        val breakfastEaters = orderedMembers.filter { it.wantsBreakfast }
        var breakfastTime: LocalTime? = null

        if (breakfastEaters.isNotEmpty()) {
            var minLeaveForBreakfastEaters = LocalTime.MAX
            for (m in breakfastEaters) {
                val leave = m.leaveHomeTime ?: LocalTime.of(23, 59)
                if (leave.isBefore(minLeaveForBreakfastEaters)) {
                    minLeaveForBreakfastEaters = leave
                }
            }
            if (minLeaveForBreakfastEaters != LocalTime.MAX) {
                breakfastTime = minLeaveForBreakfastEaters.minusMinutes(breakfastDurationMinutes)
            }
        }

        val schedules = mutableListOf<ScheduleResult>()
        var currentLatestBathroomEndTime = LocalTime.MAX

        for (member in orderedMembers.reversed()) {
            val allowedLatestWakeUp = member.latestWakeUp.plusMinutes(shiftToleranceMinutes.toLong())
            val allowedEarliestWakeUp = member.earliestWakeUp.minusMinutes(shiftToleranceMinutes.toLong())

            var maxAllowedBathroomEnd = allowedLatestWakeUp.plusMinutes(member.bathroomDurationMinutes)

            if (currentLatestBathroomEndTime.isBefore(maxAllowedBathroomEnd)) {
                maxAllowedBathroomEnd = currentLatestBathroomEndTime
            }

            if (member.wantsBreakfast && breakfastTime != null && breakfastTime.isBefore(maxAllowedBathroomEnd)) {
                maxAllowedBathroomEnd = breakfastTime
            }

            val leaveTime = member.leaveHomeTime
            if (leaveTime != null && leaveTime.isBefore(maxAllowedBathroomEnd)) {
                maxAllowedBathroomEnd = leaveTime
            }

            val wakeUpTime = maxAllowedBathroomEnd.minusMinutes(member.bathroomDurationMinutes)

            if (wakeUpTime.isBefore(allowedEarliestWakeUp)) {
                return null
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

        return FamilySchedule(
            memberSchedules = schedules.reversed(),
            breakfastTime = breakfastTime,
            isValid = true,
            message = "Optimaler Plan berechnet."
        )
    }

    private fun <T> generatePermutations(list: List<T>): List<List<T>> {
        if (list.isEmpty()) return listOf(emptyList())
        val result = mutableListOf<List<T>>()
        for (i in list.indices) {
            val current = list[i]
            val remaining = list.toMutableList().apply { removeAt(i) }
            for (perm in generatePermutations(remaining)) {
                result.add(listOf(current) + perm)
            }
        }
        return result
    }
}
