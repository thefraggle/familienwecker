package de.familienwecker.famwake.data

import de.familienwecker.famwake.model.DayProfile
import de.familienwecker.famwake.model.FamilyMember
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AwakeStateResetLogicTest {

    private fun createMember(
        id: String = "m1",
        isAwakeToday: Boolean = false,
        dayProfiles: Map<Int, DayProfile>? = null
    ) = FamilyMember(
        id = id,
        name = "Test",
        earliestWakeUp = LocalTime(6, 0),
        latestWakeUp = LocalTime(7, 30),
        bathroomDurationMinutes = 15L,
        wantsBreakfast = false,
        isAwakeToday = isAwakeToday,
        dayProfiles = dayProfiles
    )

    private fun shouldResetAwakeToday(
        oldMember: FamilyMember?,
        newMember: FamilyMember,
        todayDow: Int,
        currentTime: LocalTime
    ): Boolean {
        val oldProfile = oldMember?.dayProfiles?.get(todayDow)
        val newProfile = newMember.dayProfiles?.get(todayDow)

        val oldWakeUp = oldProfile?.earliestWakeUp ?: oldMember?.earliestWakeUp
        val newWakeUp = newProfile?.earliestWakeUp ?: newMember.earliestWakeUp
        val oldIsActive = oldProfile?.isActive ?: !(oldMember?.isPaused ?: false)
        val newIsActive = newProfile?.isActive ?: !newMember.isPaused

        val todayProfileChanged = oldProfile != newProfile || oldWakeUp != newWakeUp || oldIsActive != newIsActive
        val hasFutureAlarmToday = newIsActive && newWakeUp > currentTime

        return todayProfileChanged && hasFutureAlarmToday
    }

    @Test
    fun futureAlarmToday_whenProfileChanged_shouldResetAwake() {
        val todayDow = 4 // Thursday
        val now = LocalTime(13, 0)

        // Member had 07:00 alarm in the morning
        val oldMember = createMember(
            isAwakeToday = true,
            dayProfiles = mapOf(
                todayDow to DayProfile(isActive = true, earliestWakeUp = LocalTime(7, 0))
            )
        )

        // User sets a new alarm for 15:00 today (in the future)
        val newMember = oldMember.copy(
            dayProfiles = mapOf(
                todayDow to DayProfile(isActive = true, earliestWakeUp = LocalTime(15, 0))
            )
        )

        val reset = shouldResetAwakeToday(oldMember, newMember, todayDow, now)
        assertTrue("isAwakeToday should be reset when a new future alarm is set for today", reset)
    }

    @Test
    fun pastAlarmToday_whenProfileChanged_shouldNotResetAwake() {
        val todayDow = 4
        val now = LocalTime(13, 0)

        // Old was 07:00
        val oldMember = createMember(
            isAwakeToday = true,
            dayProfiles = mapOf(
                todayDow to DayProfile(isActive = true, earliestWakeUp = LocalTime(7, 0))
            )
        )

        // New is 11:00 (still in the past)
        val newMember = oldMember.copy(
            dayProfiles = mapOf(
                todayDow to DayProfile(isActive = true, earliestWakeUp = LocalTime(11, 0))
            )
        )

        val reset = shouldResetAwakeToday(oldMember, newMember, todayDow, now)
        assertFalse("isAwakeToday should not be reset when the new alarm time is in the past", reset)
    }

    @Test
    fun anotherDayChanged_todayUnchanged_shouldNotResetAwake() {
        val todayDow = 4 // Thursday
        val tomorrowDow = 5 // Friday
        val now = LocalTime(13, 0)

        val oldMember = createMember(
            isAwakeToday = true,
            dayProfiles = mapOf(
                todayDow to DayProfile(isActive = true, earliestWakeUp = LocalTime(7, 0)),
                tomorrowDow to DayProfile(isActive = true, earliestWakeUp = LocalTime(7, 0))
            )
        )

        // User changes Friday's alarm, Thursday remains 07:00
        val newMember = oldMember.copy(
            dayProfiles = mapOf(
                todayDow to DayProfile(isActive = true, earliestWakeUp = LocalTime(7, 0)),
                tomorrowDow to DayProfile(isActive = true, earliestWakeUp = LocalTime(8, 0))
            )
        )

        val reset = shouldResetAwakeToday(oldMember, newMember, todayDow, now)
        assertFalse("isAwakeToday should not be reset when today was not changed", reset)
    }

    @Test
    fun todayActivatedFromInactive_withFutureTime_shouldResetAwake() {
        val todayDow = 4
        val now = LocalTime(13, 0)

        // Today was inactive
        val oldMember = createMember(
            isAwakeToday = true,
            dayProfiles = mapOf(
                todayDow to DayProfile(isActive = false, earliestWakeUp = LocalTime(7, 0))
            )
        )

        // Today is now activated with 16:00
        val newMember = oldMember.copy(
            dayProfiles = mapOf(
                todayDow to DayProfile(isActive = true, earliestWakeUp = LocalTime(16, 0))
            )
        )

        val reset = shouldResetAwakeToday(oldMember, newMember, todayDow, now)
        assertTrue("isAwakeToday should be reset when today is activated with future alarm", reset)
    }
}
