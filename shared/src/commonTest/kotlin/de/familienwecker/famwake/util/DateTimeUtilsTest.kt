package de.familienwecker.famwake.util

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DateTimeUtilsTest {

    @Test
    fun plusMinutes_normalAddition_returnsCorrectTime() {
        val time = LocalTime(7, 15)
        val result = time.plusMinutes(45)
        assertEquals(LocalTime(8, 0), result)
    }

    @Test
    fun plusMinutes_midnightWrap_wrapsAroundDay() {
        val time = LocalTime(23, 45)
        val result = time.plusMinutes(30)
        assertEquals(LocalTime(0, 15), result)
    }

    @Test
    fun minusMinutes_normalSubtraction_returnsCorrectTime() {
        val time = LocalTime(8, 30)
        val result = time.minusMinutes(45)
        assertEquals(LocalTime(7, 45), result)
    }

    @Test
    fun minusMinutes_midnightWrap_wrapsAroundPreviousDay() {
        val time = LocalTime(0, 15)
        val result = time.minusMinutes(30)
        assertEquals(LocalTime(23, 45), result)
    }

    @Test
    fun isBefore_and_isAfter_behaveCorrectly() {
        val morning = LocalTime(6, 30)
        val noon = LocalTime(12, 0)

        assertTrue(morning.isBefore(noon))
        assertFalse(noon.isBefore(morning))

        assertTrue(noon.isAfter(morning))
        assertFalse(morning.isAfter(noon))
    }
}
