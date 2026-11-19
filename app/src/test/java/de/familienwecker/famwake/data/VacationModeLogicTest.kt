package de.familienwecker.famwake.data

import de.familienwecker.famwake.algorithm.AlarmCancelReason
import de.familienwecker.famwake.algorithm.AlarmDecision
import de.familienwecker.famwake.algorithm.AlarmPlanner
import de.familienwecker.famwake.model.DayProfile
import de.familienwecker.famwake.model.FamilyMember
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VacationModeLogicTest {

    private val tz = TimeZone.UTC

    private fun testMember(activeDays: Set<Int> = (1..7).toSet()): FamilyMember {
        val profiles = (1..7).associateWith { dow ->
            DayProfile(
                isActive = dow in activeDays,
                earliestWakeUp = LocalTime(6, 30),
                latestWakeUp = LocalTime(7, 30),
                bathroomDurationMinutes = 20,
                wantsBreakfast = false,
                leaveHomeTime = LocalTime(8, 0)
            )
        }
        return FamilyMember(
            id = "m1",
            name = "Mama",
            earliestWakeUp = LocalTime(6, 30),
            latestWakeUp = LocalTime(7, 30),
            bathroomDurationMinutes = 20,
            wantsBreakfast = false,
            leaveHomeTime = LocalTime(8, 0),
            dayProfiles = profiles
        )
    }

    @Test
    fun vacationActive_picksFirstActiveDayAfterVacation() {
        val now = LocalDateTime(2026, 9, 10, 6, 0)
        val plan = AlarmPlanner.planSchedules(
            members = listOf(testMember(activeDays = setOf(1, 2, 3, 4, 5))),
            myMemberId = "m1",
            selectedDate = null,
            globalBufferMinutes = 0,
            vacationUntil = "2026-09-18", // Freitag -> erster Tag nach Urlaub ist Sa 19.09 (inaktiv) -> Mo 2026-09-21
            isAwakeToday = false,
            now = now,
            timeZone = tz
        )
        assertEquals(LocalDate(2026, 9, 21), plan.deviceSchedule.targetDate)
    }

    @Test
    fun vacationActive_cancelsAlarmWhenTargetDateIsInsideVacation() {
        val now = LocalDateTime(2026, 9, 10, 6, 0)
        val plan = AlarmPlanner.planSchedules(
            members = listOf(testMember()),
            myMemberId = "m1",
            selectedDate = null,
            globalBufferMinutes = 0,
            vacationUntil = "2026-09-17",
            isAwakeToday = false,
            now = now,
            timeZone = tz
        )
        val decision = AlarmPlanner.decideAlarmAction(
            schedule = plan.uiSchedule,
            myMemberId = "m1",
            isAwakeToday = false,
            vacationUntil = "2026-09-17",
            snoozeUntil = null,
            now = now,
            timeZone = tz
        )
        assertTrue(decision is AlarmDecision.CancelAll && decision.reason == AlarmCancelReason.VACATION)
    }
}
