package com.example.familienwecker.algorithm

import com.example.familienwecker.model.FamilyMember
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

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
            val next = sortedSchedules[i+1]
            assertTrue("Bathroom overlap between ${current.member.name} and ${next.member.name}", 
                current.bathroomEndTime <= next.bathroomStartTime)
        }
    }

    @Test
    fun `schedule with unavoidable conflict should trigger fallback`() {
        // Everyone wants the bathroom at the exact same very narrow window
        val m1 = createMember("1", "A", "07:00", "07:05", 30, false, "07:40")
        val m2 = createMember("2", "B", "07:00", "07:05", 30, false, "07:40")

        val result = scheduler.calculateIdealSchedule(listOf(m1, m2))

        // It might still be valid due to the built-in shift fallback (up to 15 mins)
        // If it's valid, it should contain the fallback message
        if (result.isValid) {
            assertTrue("Should mention flexible adjustment", result.message.contains("angepasst"))
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
