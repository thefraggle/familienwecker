package de.familienwecker.famwake.data

import de.familienwecker.famwake.model.DayProfile
import de.familienwecker.famwake.model.FamilyMember
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FamilyMemberMapperTest {

    @Test
    fun parseBreakfastDuration_nullReturnsNull() {
        assertNull(parseBreakfastDuration(null))
    }

    @Test
    fun parseBreakfastDuration_longValuePreserved() {
        assertEquals(15L, parseBreakfastDuration(15L))
        assertEquals(45L, parseBreakfastDuration(45L))
    }

    @Test
    fun parseBreakfastDuration_numberConvertedToLong() {
        // Firestore can deserialize numbers as Int, Double, or other Numbers
        assertEquals(20L, parseBreakfastDuration(20))
        assertEquals(30L, parseBreakfastDuration(30.0))
        assertEquals(25L, parseBreakfastDuration(25.0f))
        assertEquals(10L, parseBreakfastDuration(10.toShort()))
        assertEquals(5L, parseBreakfastDuration(5.toByte()))
    }

    @Test
    fun parseBreakfastDuration_invalidTypesReturnNull() {
        assertNull(parseBreakfastDuration("30"))
        assertNull(parseBreakfastDuration(true))
        assertNull(parseBreakfastDuration(listOf(30)))
        assertNull(parseBreakfastDuration(mapOf("duration" to 30)))
    }

    @Test
    fun toFirestoreMap_includesBreakfastDurationMinutes() {
        val memberWithBreakfast = FamilyMember(
            id = "m-1",
            name = "Test",
            earliestWakeUp = LocalTime(6, 0),
            latestWakeUp = LocalTime(7, 0),
            bathroomDurationMinutes = 15L,
            wantsBreakfast = true,
            breakfastDurationMinutes = 25L
        )
        val mapWith = memberWithBreakfast.toFirestoreMap()
        assertEquals(25L, mapWith["breakfastDurationMinutes"])

        val memberWithoutBreakfast = FamilyMember(
            id = "m-2",
            name = "Test 2",
            earliestWakeUp = LocalTime(6, 0),
            latestWakeUp = LocalTime(7, 0),
            bathroomDurationMinutes = 15L,
            wantsBreakfast = false,
            breakfastDurationMinutes = null
        )
        val mapWithout = memberWithoutBreakfast.toFirestoreMap()
        assertNull(mapWithout["breakfastDurationMinutes"])
    }

    @Test
    fun toFirestoreMap_dayProfilesIncludeBreakfastDurationMinutes() {
        val profile = DayProfile(breakfastDurationMinutes = 35L)
        val member = FamilyMember(
            id = "m-3",
            name = "Test 3",
            earliestWakeUp = LocalTime(6, 0),
            latestWakeUp = LocalTime(7, 0),
            bathroomDurationMinutes = 15L,
            wantsBreakfast = true,
            dayProfiles = mapOf(1 to profile)
        )
        val map = member.toFirestoreMap()
        @Suppress("UNCHECKED_CAST")
        val dayProfilesMap = map["dayProfiles"] as? Map<String, Map<String, Any?>>
        assertEquals(35L, dayProfilesMap?.get("1")?.get("breakfastDurationMinutes"))
    }
}
