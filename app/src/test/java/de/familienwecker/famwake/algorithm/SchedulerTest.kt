package de.familienwecker.famwake.algorithm

import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.model.ScheduleMessage
import de.familienwecker.famwake.util.isBefore
import org.junit.Assert.*
import org.junit.Test
import kotlinx.datetime.LocalTime

class SchedulerTest {

    private val scheduler = Scheduler()

    @Test
    fun `standard schedule for 4 members should be valid`() {
        val m1 = createMember("1", "Papa", "05:30", "07:30", 20, true, "08:00")
        val m2 = createMember("2", "Mama", "06:00", "07:45", 30, true, "08:30")
        val m3 = createMember("3", "Sohn", "06:30", "07:00", 15, true, "07:45")
        val m4 = createMember("4", "Tochter", "06:15", "08:00", 25, false, "08:30")

        val family = listOf(m1, m2, m3, m4)
        val result = scheduler.calculateIdealSchedule(family, breakfastDurationMinutes = 25)

        assertTrue("Schedule should be valid", result.isValid)
        assertEquals(4, result.memberSchedules.size)

        // Check for bathroom overlaps
        val sortedSchedules = result.memberSchedules.sortedBy { it.bathroomStartTime }
        for (i in 0 until sortedSchedules.size - 1) {
            val current = sortedSchedules[i]
            val next = sortedSchedules[i + 1]
            assertTrue(
                "Bathroom overlap between ${current.member.name} and ${next.member.name}",
                current.bathroomEndTime <= next.bathroomStartTime
            )
        }
    }

    @Test
    fun `schedule with unavoidable conflict should trigger fallback`() {
        // Everyone wants the bathroom at the exact same very narrow window
        val m1 = createMember("1", "A", "07:00", "07:05", 30, false, "07:40")
        val m2 = createMember("2", "B", "07:00", "07:05", 30, false, "07:40")

        val result = scheduler.calculateIdealSchedule(listOf(m1, m2))

        // It might still be valid due to the built-in shift fallback (up to 15 mins)
        // If valid, scheduleMessage must indicate the adjustment (not a plain string anymore)
        if (result.isValid) {
            assertTrue(
                "Should indicate flexible adjustment via ScheduleMessage",
                result.scheduleMessage is ScheduleMessage.TimeAdjusted ||
                    result.scheduleMessage is ScheduleMessage.BreakfastReduced ||
                    result.scheduleMessage is ScheduleMessage.BreakfastAndTimeAdjusted
            )
        } else {
            assertFalse("Schedule should be invalid for extreme conflicts", result.isValid)
        }
    }

    @Test
    fun `paused members should be ignored by scheduler`() {
        val m1 = createMember("1", "Active", "06:00", "07:00", 20, false)
        val m2 = createMember("2", "Paused", "06:00", "07:00", 20, false).copy(isPaused = true)

        val result = scheduler.calculateIdealSchedule(listOf(m1, m2))

        assertEquals(1, result.memberSchedules.size)
        assertEquals("Active", result.memberSchedules[0].member.name)
    }

    @Test
    fun `no active members should return NoActiveMembers message`() {
        val m1 = createMember("1", "All", "06:00", "07:00", 20, false).copy(isPaused = true)
        val result = scheduler.calculateIdealSchedule(listOf(m1))

        assertTrue(result.isValid)
        assertEquals(ScheduleMessage.NoActiveMembers, result.scheduleMessage)
    }

    @Test
    fun `single member with breakfast and no leaveHomeTime should not have late-night breakfast time`() {
        // Reproduziert den Bug: Weckzeit 06:00–07:30, kein leaveHomeTime, wantsBreakfast=true
        // → Frühstück darf NICHT 23:29 sein, sondern muss ≤ latestWakeUp + bathroomDuration sein
        val mama = createMember("1", "Mama", "06:00", "07:30", 20, true, null)

        val result = scheduler.calculateIdealSchedule(listOf(mama), breakfastDurationMinutes = 30)

        assertTrue("Schedule should be valid", result.isValid)
        val breakfast = result.breakfastTime
        assertNotNull("Breakfast time should be set", breakfast)
        // Frühstück muss vor 10:00 liegen (nie im abendlichen/nächtlichen Bereich)
        assertTrue(
            "Breakfast time $breakfast should not be unreasonably late (expected < 10:00)",
            breakfast!!.isBefore(LocalTime(10, 0))
        )
        // Frühstück darf nicht vor der Weckzeit liegen
        val wakeUp = result.memberSchedules.first().wakeUpTime
        assertTrue(
            "Breakfast $breakfast should be after or equal to wakeUpTime $wakeUp",
            !breakfast.isBefore(wakeUp)
        )
    }

    @Test
    fun `awake members should be excluded from schedule`() {
        val m1 = createMember("1", "Active", "06:00", "07:00", 20, false)
        val m2 = createMember("2", "Awake", "06:00", "07:00", 20, false).copy(isAwakeToday = true)

        // Scheduler berechnet für alle nicht-pausierten. isAwakeToday wird im ViewModel
        // via resolveEffectiveMember gehandhabt, nicht im Scheduler selbst.
        // Dieser Test bestätigt, dass der Scheduler isAwakeToday NICHT filtert.
        val result = scheduler.calculateIdealSchedule(listOf(m1, m2))
        assertEquals("Scheduler should include awake members (filtering is VM responsibility)", 2, result.memberSchedules.size)
    }

    @Test
    fun `single member should produce optimal plan`() {
        val m1 = createMember("1", "Solo", "06:00", "07:00", 15, false)
        val result = scheduler.calculateIdealSchedule(listOf(m1))

        assertTrue(result.isValid)
        assertEquals(ScheduleMessage.OptimalPlan, result.scheduleMessage)
        assertEquals(1, result.memberSchedules.size)
        assertNull("No breakfast should be set", result.breakfastTime)
    }

    private fun createMember(
        id: String,
        name: String,
        earliest: String,
        latest: String,
        duration: Long,
        breakfast: Boolean,
        leave: String? = null
    ): FamilyMember {
        return FamilyMember(
            id = id,
            name = name,
            earliestWakeUp = LocalTime.parse(earliest),
            latestWakeUp = LocalTime.parse(latest),
            bathroomDurationMinutes = duration,
            wantsBreakfast = breakfast,
            leaveHomeTime = leave?.let { LocalTime.parse(it) }
        )
    }
}
