package de.familienwecker.famwake.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class VacationModeLogicTest {

    @Test
    fun vacationEndDate_plus7Days_calculatesCorrectTargetDate() {
        val today = LocalDate.of(2026, 9, 10)
        val target = today.plusDays(7)
        assertEquals(LocalDate.of(2026, 9, 17), target)
        assertEquals("2026-09-17", target.toString())
    }

    @Test
    fun vacationEndDate_plus14Days_calculatesCorrectTargetDate() {
        val today = LocalDate.of(2026, 9, 10)
        val target = today.plusDays(14)
        assertEquals(LocalDate.of(2026, 9, 24), target)
        assertEquals("2026-09-24", target.toString())
    }

    @Test
    fun vacationEndDate_endOfMonth_calculatesLastDayOfMonth() {
        val today = LocalDate.of(2026, 9, 10)
        val target = today.withDayOfMonth(today.lengthOfMonth())
        assertEquals(LocalDate.of(2026, 9, 30), target)
        assertEquals("2026-09-30", target.toString())
    }

    @Test
    fun vacationDateParsing_withInvalidInput_handlesGracefully() {
        val invalid = "not-a-date"
        val parsed = try {
            LocalDate.parse(invalid)
        } catch (_: Exception) {
            null
        }
        assertNull(parsed)
    }

    @Test
    fun firstDayAfterVacation_isDayImmediatelyFollowingEndDate() {
        val vacationEnd = LocalDate.of(2026, 9, 17)
        val firstDayAfter = vacationEnd.plusDays(1)
        assertEquals(LocalDate.of(2026, 9, 18), firstDayAfter)
    }
}
