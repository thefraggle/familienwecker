package de.familienwecker.famwake.algorithm

import de.familienwecker.famwake.model.DayProfile
import de.familienwecker.famwake.model.FamilyMember
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NextAlarmCalculatorTest {

    private fun testMember(
        id: String = "m1",
        name: String = "Papa",
        earliestWakeUp: LocalTime = LocalTime(6, 30),
        latestWakeUp: LocalTime = LocalTime(7, 0),
        bathroomDurationMinutes: Long = 20L,
        dayProfiles: Map<Int, DayProfile>? = null,
        isPaused: Boolean = false,
        deviceAlarmEnabled: Boolean? = true
    ) = FamilyMember(
        id = id,
        name = name,
        earliestWakeUp = earliestWakeUp,
        latestWakeUp = latestWakeUp,
        bathroomDurationMinutes = bathroomDurationMinutes,
        wantsBreakfast = true,
        dayProfiles = dayProfiles,
        isPaused = isPaused,
        deviceAlarmEnabled = deviceAlarmEnabled
    )

    @Test
    fun calculateNextAlarmFromSavedTime_beforeAlarm_schedulesForToday() {
        val alarmTime = LocalTime(6, 30)
        val now = LocalDateTime(LocalDate(2026, 11, 3), LocalTime(5, 45))
        val next = NextAlarmCalculator.calculateNextAlarmFromSavedTime(alarmTime, now)

        assertEquals(LocalDate(2026, 11, 3), next.date)
        assertEquals(LocalTime(6, 30), next.time)
    }

    @Test
    fun calculateNextAlarmFromSavedTime_afterAlarm_schedulesForTomorrow() {
        val alarmTime = LocalTime(6, 30)
        val now = LocalDateTime(LocalDate(2026, 11, 3), LocalTime(7, 15))
        val next = NextAlarmCalculator.calculateNextAlarmFromSavedTime(alarmTime, now)

        assertEquals(LocalDate(2026, 11, 4), next.date)
        assertEquals(LocalTime(6, 30), next.time)
    }

    @Test
    fun calculateNextWakeUpForDate_respectsVacation() {
        val member = testMember()
        val targetDate = LocalDate(2026, 11, 4)
        val wakeUp = NextAlarmCalculator.calculateNextWakeUpForDate(
            members = listOf(member),
            targetMemberId = "m1",
            targetDate = targetDate,
            vacationUntil = "2026-11-05"
        )
        assertNull(wakeUp)
    }

    @Test
    fun calculateNextWakeUpForDate_respectsDayProfile() {
        // Mittwoch, 4. Nov 2026 ist DayOfWeek 3 (Wednesday)
        val targetDate = LocalDate(2026, 11, 4)
        assertEquals(3, targetDate.dayOfWeek.value)

        val wedProfile = DayProfile(
            isActive = true,
            earliestWakeUp = LocalTime(6, 45),
            latestWakeUp = LocalTime(7, 30),
            bathroomDurationMinutes = 15L
        )
        val member = testMember(dayProfiles = mapOf(3 to wedProfile))

        val wakeUp = NextAlarmCalculator.calculateNextWakeUpForDate(
            members = listOf(member),
            targetMemberId = "m1",
            targetDate = targetDate
        )

        assertNotNull(wakeUp)
        assertEquals(targetDate, wakeUp.date)
        assertEquals(LocalTime(7, 0), wakeUp.time)
    }

    @Test
    fun calculateNextWakeUpForDate_inactiveDayProfile_returnsNull() {
        val targetDate = LocalDate(2026, 11, 4)
        val wedProfile = DayProfile(isActive = false)
        val member = testMember(dayProfiles = mapOf(3 to wedProfile))

        val wakeUp = NextAlarmCalculator.calculateNextWakeUpForDate(
            members = listOf(member),
            targetMemberId = "m1",
            targetDate = targetDate
        )

        assertNull(wakeUp)
    }
}
