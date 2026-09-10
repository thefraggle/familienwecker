package de.familienwecker.famwake.data

import de.familienwecker.famwake.model.FamilyMember
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FamilyMemberDeserializationTest {

    @Test
    fun parseBreakfastDuration_withNull_returnsNull() {
        val result = parseBreakfastDuration(null)
        assertNull("Null input should deserialize to null (using global default)", result)
    }

    @Test
    fun parseBreakfastDuration_withLong_returnsExactLong() {
        val result = parseBreakfastDuration(25L)
        assertEquals(25L, result)
    }

    @Test
    fun parseBreakfastDuration_withNumber_convertsToLong() {
        // Test various Number types that could be returned by Firestore deserialization
        val intResult = parseBreakfastDuration(30)
        assertEquals(30L, intResult)

        val doubleResult = parseBreakfastDuration(45.0)
        assertEquals(45L, doubleResult)

        val floatResult = parseBreakfastDuration(15.0f)
        assertEquals(15L, floatResult)

        val javaLongResult = parseBreakfastDuration(java.lang.Long.valueOf(20L))
        assertEquals(20L, javaLongResult)

        val javaIntegerResult = parseBreakfastDuration(java.lang.Integer.valueOf(35))
        assertEquals(35L, javaIntegerResult)

        val javaDoubleResult = parseBreakfastDuration(java.lang.Double.valueOf(40.0))
        assertEquals(40L, javaDoubleResult)
    }

    @Test
    fun parseBreakfastDuration_withUnsupportedTypes_returnsNullSafely() {
        assertNull(parseBreakfastDuration("not a number"))
        assertNull(parseBreakfastDuration(true))
        assertNull(parseBreakfastDuration(listOf("invalid")))
    }

    @Test
    fun familyMember_modelRetainsBreakfastDurationMinutes() {
        val memberWithCustomBreakfast = FamilyMember(
            id = "test-1",
            name = "Test Member",
            earliestWakeUp = LocalTime(6, 30),
            latestWakeUp = LocalTime(7, 30),
            bathroomDurationMinutes = 20L,
            wantsBreakfast = true,
            breakfastDurationMinutes = parseBreakfastDuration(25L)
        )
        assertEquals(25L, memberWithCustomBreakfast.breakfastDurationMinutes)

        val memberWithDefaultBreakfast = FamilyMember(
            id = "test-2",
            name = "Test Member 2",
            earliestWakeUp = LocalTime(6, 30),
            latestWakeUp = LocalTime(7, 30),
            bathroomDurationMinutes = 20L,
            wantsBreakfast = true,
            breakfastDurationMinutes = parseBreakfastDuration(null)
        )
        assertNull(memberWithDefaultBreakfast.breakfastDurationMinutes)
    }
}
