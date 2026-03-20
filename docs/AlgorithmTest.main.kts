import java.time.LocalTime

/**
 * 1. DATENMODELLE
 */
data class FamilyMember(
    val id: String,
    val name: String,
    val earliestWakeUp: LocalTime,
    val latestWakeUp: LocalTime,
    val bathroomDurationMinutes: Long,
    val wantsBreakfast: Boolean,
    val leaveHomeTime: LocalTime? = null
)

data class ScheduleResult(
    val member: FamilyMember,
    val wakeUpTime: LocalTime,
    val bathroomStartTime: LocalTime,
    val bathroomEndTime: LocalTime
)

sealed class ScheduleMessage {
    object OptimalPlan : ScheduleMessage()
    object NoActiveMembers : ScheduleMessage()
    object NoValidScheduleFound : ScheduleMessage()
    data class TimeAdjusted(val minutes: Int) : ScheduleMessage()
    data class BreakfastReduced(val minutes: Int) : ScheduleMessage()
    data class BreakfastAndTimeAdjusted(val breakfast: Int, val shift: Int) : ScheduleMessage()
    data class MemberConflict(val memberName: String) : ScheduleMessage()
    
    fun asString(): String = when(this) {
        is OptimalPlan -> "Optimaler Plan berechnet"
        is NoActiveMembers -> "Keine aktiven Mitglieder"
        is NoValidScheduleFound -> "Kein gültiger Plan gefunden"
        is TimeAdjusted -> "Zeit angepasst um ${minutes} Min"
        is BreakfastReduced -> "Frühstück verkürzt um ${minutes} Min"
        is BreakfastAndTimeAdjusted -> "Frühstück -${breakfast} Min, Zeit +${shift} Min"
        is MemberConflict -> "Konflikt bei $memberName"
    }
}

data class FamilySchedule(
    val memberSchedules: List<ScheduleResult>,
    val breakfastTime: LocalTime?,
    val isValid: Boolean,
    val scheduleMessage: ScheduleMessage,
    val message: String = scheduleMessage.asString()
)

/**
 * 2. ALGORITHMUS LOGIK
 */
class Scheduler {
    fun calculateIdealSchedule(
        members: List<FamilyMember>,
        breakfastDurationMinutes: Long = 30
    ): FamilySchedule {
        val activeMembers = members.take(6)
        if (activeMembers.isEmpty()) return FamilySchedule(emptyList(), null, true, ScheduleMessage.NoActiveMembers)

        // 1. Erster Versuch (0 Shift, inkl. Invalid-Daten für Fallback)
        val initialResult = evaluatePermutation(activeMembers, breakfastDurationMinutes, 0, includeInvalid = true)
        if (initialResult.isValid) return initialResult

        // 2. Zeit-Anpassung
        for (shiftMinutes in 5..15 step 5) {
            val flexibleSchedule = evaluatePermutation(activeMembers, breakfastDurationMinutes, shiftMinutes)
            if (flexibleSchedule.isValid) return flexibleSchedule.copy(scheduleMessage = ScheduleMessage.TimeAdjusted(shiftMinutes))
        }

        // 3. Frühstück-Verkürzung
        if (breakfastDurationMinutes >= 15) {
            for (reduceBreakfast in 5..10 step 5) {
                val reducedDuration = breakfastDurationMinutes - reduceBreakfast
                val reductionSchedule = evaluatePermutation(activeMembers, reducedDuration, 0)
                if (reductionSchedule.isValid) return reductionSchedule.copy(scheduleMessage = ScheduleMessage.BreakfastReduced(reduceBreakfast))

                for (shiftMinutes in 5..15 step 5) {
                    val flexibleReductionSchedule = evaluatePermutation(activeMembers, reducedDuration, shiftMinutes)
                    if (flexibleReductionSchedule.isValid) return flexibleReductionSchedule.copy(scheduleMessage = ScheduleMessage.BreakfastAndTimeAdjusted(reduceBreakfast, shiftMinutes))
                }
            }
        }

        // Fallback: Best-Effort Plan vom ersten Versuch
        return initialResult.copy(isValid = false, scheduleMessage = extractConflictMessage(initialResult))
    }

    private fun extractConflictMessage(schedule: FamilySchedule): ScheduleMessage {
        for (s in schedule.memberSchedules) {
            if (s.wakeUpTime.isBefore(s.member.earliestWakeUp)) return ScheduleMessage.MemberConflict(s.member.name)
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
            var minLeave = LocalTime.of(23, 59)
            for (m in breakfastEaters) {
                val naturalBathEnd = m.latestWakeUp.plusMinutes(m.bathroomDurationMinutes)
                val leave = m.leaveHomeTime ?: naturalBathEnd
                if (leave.isBefore(minLeave)) minLeave = leave
            }
            val startTime = if (minLeave.isBefore(LocalTime.of(4, 0))) LocalTime.of(4, 0) else minLeave
            breakfastTime = startTime.minusMinutes(breakfastDurationMinutes)
        }

        val schedules = mutableListOf<ScheduleResult>()
        var currentLatestBathroomEndTime = LocalTime.of(23, 59)
        var isValid = true

        for (member in orderedMembers.reversed()) {
            val allowedLatestWakeUp = member.latestWakeUp.plusMinutes(shiftToleranceMinutes.toLong())
            val allowedEarliestWakeUp = member.earliestWakeUp.minusMinutes(shiftToleranceMinutes.toLong())
            var maxAllowedBathroomEnd = allowedLatestWakeUp.plusMinutes(member.bathroomDurationMinutes)
            if (currentLatestBathroomEndTime.isBefore(maxAllowedBathroomEnd)) maxAllowedBathroomEnd = currentLatestBathroomEndTime
            if (member.wantsBreakfast && breakfastTime != null && !breakfastTime.isAfter(maxAllowedBathroomEnd)) maxAllowedBathroomEnd = breakfastTime
            val leaveTime = member.leaveHomeTime
            if (leaveTime != null && leaveTime.isBefore(maxAllowedBathroomEnd)) maxAllowedBathroomEnd = leaveTime
            val wakeUpTime = maxAllowedBathroomEnd.minusMinutes(member.bathroomDurationMinutes)

            if (wakeUpTime.isBefore(allowedEarliestWakeUp)) {
                if (!includeInvalid) return FamilySchedule(emptyList(), null, false, ScheduleMessage.NoValidScheduleFound)
                isValid = false
            }

            schedules.add(ScheduleResult(member, wakeUpTime, wakeUpTime, maxAllowedBathroomEnd))
            currentLatestBathroomEndTime = wakeUpTime
        }
        return FamilySchedule(schedules.reversed(), breakfastTime, isValid, if (isValid) ScheduleMessage.OptimalPlan else ScheduleMessage.NoValidScheduleFound)
    }
}

/**
 * 3. TEST SZENARIO
 */
fun runTestSzenario() {
    val m1 = FamilyMember("1", "Papa", LocalTime.of(5, 30), LocalTime.of(7, 30), 20, true, LocalTime.of(8, 0))
    val m2 = FamilyMember("2", "Mama", LocalTime.of(6, 0), LocalTime.of(7, 45), 30, true, LocalTime.of(8, 30))
    val m3 = FamilyMember("3", "Sohn", LocalTime.of(6, 30), LocalTime.of(7, 0), 15, true, LocalTime.of(7, 45))
    val m4 = FamilyMember("4", "Tochter", LocalTime.of(6, 15), LocalTime.of(8, 0), 25, false, LocalTime.of(8, 30))

    val family = listOf(m1, m2, m3, m4)
    val scheduler = Scheduler()

    println("Test: Normaler Zeitplan...")
    val result = scheduler.calculateIdealSchedule(family, breakfastDurationMinutes = 25)
    println("Status: ${result.message}, Valid: ${result.isValid}, Mitglieder im Plan: ${result.memberSchedules.size}")

    println("--------------------------------------------------")
    println("Test: Best-Effort bei Konflikt...")
    // Erzeuge künstlich Konflikt (Sohn muss früher wach sein als erlaubt durch Papa/Mama-Kette)
    val tightFamily = family.map { if (it.name == "Sohn") it.copy(earliestWakeUp = LocalTime.of(7, 10), latestWakeUp = LocalTime.of(7, 15)) else it }
    val resultConflict = scheduler.calculateIdealSchedule(tightFamily, breakfastDurationMinutes = 30)
    println("Status: ${resultConflict.message}, Valid: ${resultConflict.isValid}, Mitglieder im Plan: ${resultConflict.memberSchedules.size}")
    if (resultConflict.memberSchedules.isNotEmpty()) {
        println("Best-effort Weckzeit Sohn: ${resultConflict.memberSchedules.find { it.member.name == "Sohn" }?.wakeUpTime}")
    }

    println("--------------------------------------------------")
    println("Test: Mitternachts-Wecker (00:15)...")
    val mMidnight = FamilyMember("5", "Nachtschicht", LocalTime.of(0, 5), LocalTime.of(0, 15), 10, false, LocalTime.of(1, 0))
    val resultMidnight = scheduler.calculateIdealSchedule(listOf(mMidnight))
    if (resultMidnight.isValid) {
        println("✅ Mitternacht erfolgreich: ${resultMidnight.memberSchedules.first().wakeUpTime}")
    } else {
        println("❌ Mitternacht fehlgeschlagen: ${resultMidnight.message}")
    }
}

runTestSzenario()
