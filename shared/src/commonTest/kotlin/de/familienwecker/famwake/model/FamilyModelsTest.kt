package de.familienwecker.famwake.model

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FamilyModelsTest {

    @Test
    fun snoozeConfig_hasExpectedConstants() {
        assertEquals(5, SnoozeConfig.SNOOZE_DURATION_MINUTES)
        assertEquals(2, SnoozeConfig.MAX_SNOOZE_COUNT)
        assertEquals(5L, SnoozeConfig.MIN_BATHROOM_MINUTES)
    }

    @Test
    fun dayProfile_defaultValues_areValid() {
        val profile = DayProfile()
        assertTrue(profile.isActive)
        assertEquals(LocalTime(6, 0), profile.earliestWakeUp)
        assertEquals(LocalTime(7, 30), profile.latestWakeUp)
        assertEquals(20L, profile.bathroomDurationMinutes)
        assertTrue(profile.wantsBreakfast)
        assertNull(profile.leaveHomeTime)
        assertNull(profile.bufferMinutes)
        assertFalse(profile.isSimpleMode)
    }

    @Test
    fun familyMember_customProperties_areRetained() {
        val member = FamilyMember(
            id = "m-123",
            name = "Papa",
            earliestWakeUp = LocalTime(5, 45),
            latestWakeUp = LocalTime(6, 30),
            bathroomDurationMinutes = 15L,
            wantsBreakfast = false,
            leaveHomeTime = LocalTime(7, 15),
            isPaused = false,
            claimedByUserId = "user-abc",
            snoozeCount = 1
        )

        assertEquals("m-123", member.id)
        assertEquals("Papa", member.name)
        assertEquals(LocalTime(5, 45), member.earliestWakeUp)
        assertEquals(LocalTime(6, 30), member.latestWakeUp)
        assertEquals(15L, member.bathroomDurationMinutes)
        assertFalse(member.wantsBreakfast)
        assertEquals(LocalTime(7, 15), member.leaveHomeTime)
        assertEquals("user-abc", member.claimedByUserId)
        assertEquals(1, member.snoozeCount)
    }
}
