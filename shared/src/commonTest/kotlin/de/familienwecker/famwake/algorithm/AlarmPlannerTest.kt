package de.familienwecker.famwake.algorithm

import de.familienwecker.famwake.model.DayProfile
import de.familienwecker.famwake.model.FamilyMember
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AlarmPlannerTest {

    private val tz = TimeZone.UTC

    private fun member(
        id: String = "m1",
        name: String = "Mama",
        earliest: LocalTime = LocalTime(6, 30),
        latest: LocalTime = LocalTime(7, 30),
        leave: LocalTime = LocalTime(8, 0),
        activeDays: Set<Int> = (1..7).toSet()
    ): FamilyMember {
        val profiles = (1..7).associateWith { dow ->
            DayProfile(
                isActive = dow in activeDays,
                earliestWakeUp = earliest,
                latestWakeUp = latest,
                bathroomDurationMinutes = 20,
                wantsBreakfast = false,
                leaveHomeTime = leave
            )
        }
        return FamilyMember(
            id = id,
            name = name,
            earliestWakeUp = earliest,
            latestWakeUp = latest,
            bathroomDurationMinutes = 20,
            wantsBreakfast = false,
            leaveHomeTime = leave,
            dayProfiles = profiles
        )
    }

    @Test
    fun planSchedules_vacationActive_skipsToFirstActiveDayAfterVacation() {
        // 2026-09-10 = Donnerstag (4). Urlaub bis 2026-09-17 (Donnerstag).
        // Erster Tag nach Urlaub: 2026-09-18 (Freitag, 5). Nur Montag (1) aktiv -> 2026-09-21.
        val m = member(activeDays = setOf(1))
        val now = LocalDateTime(2026, 9, 10, 6, 0)

        val result = AlarmPlanner.planSchedules(
            members = listOf(m),
            myMemberId = "m1",
            selectedDate = null,
            globalBufferMinutes = 0,
            vacationUntil = "2026-09-17",
            isAwakeToday = false,
            now = now,
            timeZone = tz
        )

        assertEquals(LocalDate(2026, 9, 21), result.deviceSchedule.targetDate)
    }

    @Test
    fun planSchedules_invalidVacationString_fallsBackToTomorrow() {
        val m = member()
        val now = LocalDateTime(2026, 9, 10, 6, 0)

        val result = AlarmPlanner.planSchedules(
            members = listOf(m),
            myMemberId = "m1",
            selectedDate = null,
            globalBufferMinutes = 0,
            vacationUntil = "not-a-date",
            isAwakeToday = false,
            now = now,
            timeZone = tz
        )

        // invalid date string compares lexicographically: "2026-09-10" <= "not-a-date" is true,
        // parsing fails -> fallback to tomorrow (2026-09-11)
        assertEquals(LocalDate(2026, 9, 11), result.deviceSchedule.targetDate)
    }

    @Test
    fun planSchedules_isAwakeToday_movesDeviceScheduleToTomorrow() {
        val m = member(earliest = LocalTime(6, 30), latest = LocalTime(7, 30))
        val now = LocalDateTime(2026, 9, 10, 6, 0)

        val result = AlarmPlanner.planSchedules(
            members = listOf(m),
            myMemberId = "m1",
            selectedDate = null,
            globalBufferMinutes = 0,
            vacationUntil = null,
            isAwakeToday = true,
            now = now,
            timeZone = tz
        )

        assertEquals(LocalDate(2026, 9, 11), result.deviceSchedule.targetDate)
    }

    @Test
    fun decideAlarmAction_activeSnoozeTakesPrecedenceOverRegularSchedule() {
        val m = member()
        val now = LocalDateTime(2026, 9, 10, 7, 1)
        val plan = AlarmPlanner.planSchedules(
            members = listOf(m),
            myMemberId = "m1",
            selectedDate = null,
            globalBufferMinutes = 0,
            vacationUntil = null,
            isAwakeToday = false,
            now = now,
            timeZone = tz
        )
        val snoozeUntil = LocalDateTime(2026, 9, 10, 7, 6)

        val decision = AlarmPlanner.decideAlarmAction(
            schedule = plan.deviceSchedule,
            myMemberId = "m1",
            isAwakeToday = false,
            vacationUntil = null,
            snoozeUntil = snoozeUntil,
            now = now,
            timeZone = tz
        )

        val snooze = assertIs<AlarmDecision.ScheduleSnooze>(decision)
        assertEquals(snoozeUntil, snooze.wakeUpTime)
        assertEquals("m1", snooze.memberId)
    }

    @Test
    fun decideAlarmAction_withinGracePeriod_keepsExistingAlarm() {
        // 2026-09-10 is Thursday (4). WakeUp is 07:30. At 07:32 (2 min after), deviceTargetDate is tomorrow,
        // but we are inside the 5-minute grace period and not awake yet -> KeepGracePeriod.
        val m = member(earliest = LocalTime(7, 30), latest = LocalTime(7, 30), leave = LocalTime(8, 0))
        val now = LocalDateTime(2026, 9, 10, 7, 32)
        val plan = AlarmPlanner.planSchedules(
            members = listOf(m),
            myMemberId = "m1",
            selectedDate = null,
            globalBufferMinutes = 0,
            vacationUntil = null,
            isAwakeToday = false,
            now = now,
            timeZone = tz
        )

        val decision = AlarmPlanner.decideAlarmAction(
            schedule = plan.deviceSchedule,
            myMemberId = "m1",
            isAwakeToday = false,
            vacationUntil = null,
            snoozeUntil = null,
            now = now,
            timeZone = tz
        )

        assertIs<AlarmDecision.KeepGracePeriod>(decision)
    }

    @Test
    fun decideRebootAlarm_missedWithin30Min_triggersImmediate() {
        val saved = LocalDateTime(2026, 9, 10, 7, 0)
        val now = LocalDateTime(2026, 9, 10, 7, 15)

        val decision = AlarmPlanner.decideRebootAlarm(savedDateTime = saved, now = now)
        assertTrue(decision.isImmediateMissed)
        assertEquals(LocalDateTime(2026, 9, 10, 7, 15, 10), decision.targetDateTime)
    }

    @Test
    fun decideRebootAlarm_missedOver30Min_notifiesAndSchedulesNextDay() {
        val saved = LocalDateTime(2026, 9, 10, 7, 0)
        val now = LocalDateTime(2026, 9, 10, 8, 0)

        val decision = AlarmPlanner.decideRebootAlarm(savedDateTime = saved, now = now)
        assertTrue(!decision.isImmediateMissed)
        assertTrue(decision.notifyMissed)
        assertEquals(LocalDateTime(2026, 9, 11, 7, 0), decision.targetDateTime)
    }
}
