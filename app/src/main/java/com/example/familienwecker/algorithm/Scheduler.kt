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
        val result = findBestScheduleOverPermutations(permutations, activeMembers, breakfastDurationMinutes, 0)
        
        if (result.isSuccess) return result.getOrThrow()

        // Fallback 1: Iteratively allow shifts up to 15 minutes in 5-minute increments
        for (shiftMinutes in 5..15 step 5) {
            val flexibleResult = findBestScheduleOverPermutations(permutations, activeMembers, breakfastDurationMinutes, shiftMinutes)
            flexibleResult.onSuccess { flexibleSchedule ->
                return flexibleSchedule.copy(message = "Zeiten wurden um $shiftMinutes Minuten flexibel angepasst, um Konflikte zu lösen.")
            }
        }

        // Fallback 2: Reduce breakfast time by 5 or 10 minutes and try again with shifts
        if (breakfastDurationMinutes >= 15) {
            for (reduceBreakfast in 5..10 step 5) {
                val reducedDuration = breakfastDurationMinutes - reduceBreakfast
                val reductionResult = findBestScheduleOverPermutations(permutations, activeMembers, reducedDuration, 0)
                
                reductionResult.onSuccess { sched ->
                    return sched.copy(message = "Frühstück wurde um $reduceBreakfast Minuten verkürzt, um Konflikte zu lösen.")
                }
                
                // Try shifts with reduced breakfast
                for (shiftMinutes in 5..15 step 5) {
                    val flexibleReductionResult = findBestScheduleOverPermutations(permutations, activeMembers, reducedDuration, shiftMinutes)
                    flexibleReductionResult.onSuccess { flexibleSchedule ->
                        return flexibleSchedule.copy(message = "Frühstück wurde um $reduceBreakfast Min. verkürzt & Zeiten um $shiftMinutes Min. angepasst.")
                    }
                }
            }
        }

        val lastErrorMessage = result.exceptionOrNull()?.message ?: "Kein gültiger Zeitplan gefunden! Zeiten überschneiden sich zu stark."

        return FamilySchedule(
            emptyList(), null, false, 
            lastErrorMessage
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
        var lastError: String? = null

        for (perm in permutations) {
            val result = evaluatePermutation(perm, breakfastDurationMinutes, shiftToleranceMinutes)
            result.onSuccess { scheduleOpt ->
                val score = scheduleOpt.memberSchedules.sumOf { it.wakeUpTime.toSecondOfDay().toLong() }
                if (score > bestScore) {
                    bestScore = score
                    bestSchedule = scheduleOpt
                }
            }.onFailure { exception ->
                lastError = exception.message
            }
        }
        
        return if (bestSchedule != null) {
            Result.success(bestSchedule!!)
        } else {
            Result.failure(Exception(lastError ?: "Kein gültiger Zeitplan gefunden!"))
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
                val leave = m.leaveHomeTime ?: LocalTime.of(23, 59)
                if (leave.isBefore(minLeaveForBreakfastEaters)) {
                    minLeaveForBreakfastEaters = leave
                }
            }
            // Limit to a reasonable start time (e.g., not before 04:00)
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

            if (member.wantsBreakfast && breakfastTime != null && breakfastTime.isBefore(maxAllowedBathroomEnd)) {
                maxAllowedBathroomEnd = breakfastTime
            }

            val leaveTime = member.leaveHomeTime
            if (leaveTime != null && leaveTime.isBefore(maxAllowedBathroomEnd)) {
                maxAllowedBathroomEnd = leaveTime
            }

            val wakeUpTime = maxAllowedBathroomEnd.minusMinutes(member.bathroomDurationMinutes)

            if (wakeUpTime.isBefore(allowedEarliestWakeUp)) {
                return Result.failure(Exception("Konflikt bei ${member.name}: Wecken müsste um $wakeUpTime Uhr sein, um Bad/Frühstück einzuhalten, aber frühestes Wecken ist $allowedEarliestWakeUp Uhr."))
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

        return Result.success(FamilySchedule(
            memberSchedules = schedules.reversed(),
            breakfastTime = breakfastTime,
            isValid = true,
            message = "Optimaler Plan berechnet."
        ))
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
