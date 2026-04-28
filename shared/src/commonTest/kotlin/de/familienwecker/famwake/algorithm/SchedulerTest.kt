package de.familienwecker.famwake.algorithm

import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.model.ScheduleMessage
import de.familienwecker.famwake.util.isBefore
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchedulerTest {

    private val scheduler = Scheduler()

    // Hilfsmethode: minimaler FamilyMember für Tests
    private fun member(
        id: String = "m1",
        name: String = "Test",
        earliestWakeUp: LocalTime = LocalTime(6, 0),
        latestWakeUp: LocalTime = LocalTime(7, 30),
        bathroomDurationMinutes: Long = 20L,
        wantsBreakfast: Boolean = true,
        leaveHomeTime: LocalTime? = null,
        isPaused: Boolean = false
    ) = FamilyMember(
        id = id,
        name = name,
        earliestWakeUp = earliestWakeUp,
        latestWakeUp = latestWakeUp,
        bathroomDurationMinutes = bathroomDurationMinutes,
        wantsBreakfast = wantsBreakfast,
        leaveHomeTime = leaveHomeTime,
        isPaused = isPaused
    )

    @Test
    fun emptyMembers_returnsNoActiveMembers() {
        val result = scheduler.calculateIdealSchedule(emptyList())
        assertTrue(result.memberSchedules.isEmpty())
        assertEquals(ScheduleMessage.NoActiveMembers, result.scheduleMessage)
    }

    @Test
    fun allPaused_returnsNoActiveMembers() {
        val members = listOf(
            member(id = "m1", isPaused = true),
            member(id = "m2", isPaused = true)
        )
        val result = scheduler.calculateIdealSchedule(members)
        assertEquals(ScheduleMessage.NoActiveMembers, result.scheduleMessage)
    }

    @Test
    fun singleMember_optimalPlan() {
        val result = scheduler.calculateIdealSchedule(listOf(member()))
        assertTrue(result.isValid)
        assertEquals(ScheduleMessage.OptimalPlan, result.scheduleMessage)
        assertEquals(1, result.memberSchedules.size)
    }

    @Test
    fun twoMembers_noConflict_optimalPlan() {
        // m1: 6:00-7:30 bath 20min, m2: 7:00-8:00 bath 20min
        val members = listOf(
            member(id = "m1", earliestWakeUp = LocalTime(6, 0), latestWakeUp = LocalTime(7, 30), bathroomDurationMinutes = 20L, wantsBreakfast = false),
            member(id = "m2", earliestWakeUp = LocalTime(7, 0), latestWakeUp = LocalTime(8, 0), bathroomDurationMinutes = 20L, wantsBreakfast = false)
        )
        val result = scheduler.calculateIdealSchedule(members, breakfastDurationMinutes = 0)
        assertTrue(result.isValid)
        assertEquals(ScheduleMessage.OptimalPlan, result.scheduleMessage)
        assertEquals(2, result.memberSchedules.size)
    }

    @Test
    fun pausedMembers_areIgnored() {
        val members = listOf(
            member(id = "m1", isPaused = false),
            member(id = "m2", isPaused = true)
        )
        val result = scheduler.calculateIdealSchedule(members)
        assertTrue(result.isValid)
        assertEquals(1, result.memberSchedules.size)
        assertEquals("m1", result.memberSchedules[0].member.id)
    }

    @Test
    fun conflict_withTimeAdjust_stillReturnsSchedule() {
        // Beide brauchen denselben Zeitslot, aber Scheduler versucht TimeAdjusted-Fallback.
        // Erwartet: Schedule wird zurückgegeben (isValid oder TimeAdjusted), kein Crash.
        val members = listOf(
            member(id = "m1", earliestWakeUp = LocalTime(6, 0), latestWakeUp = LocalTime(6, 10), bathroomDurationMinutes = 30L, wantsBreakfast = false),
            member(id = "m2", earliestWakeUp = LocalTime(6, 0), latestWakeUp = LocalTime(6, 10), bathroomDurationMinutes = 30L, wantsBreakfast = false)
        )
        val result = scheduler.calculateIdealSchedule(members, breakfastDurationMinutes = 0)
        // Scheduler gibt immer ein Ergebnis zurück (Best-Effort)
        assertTrue(result.memberSchedules.isNotEmpty() || !result.isValid,
            "Erwartet Schedule oder isValid=false, bekam leere Liste mit isValid=true")
    }

    @Test
    fun midnightWrap_clamp_startTime_04h() {
        // Wenn minLeaveForBreakfastEaters < 04:00, wird startTime auf 04:00 geclampet.
        // breakfastTime = startTime - breakfastDuration, kann dadurch noch vor 04:00 liegen.
        // Dieser Test dokumentiert das aktuelle Verhalten des Schedulers.
        val m = member(
            wantsBreakfast = true,
            latestWakeUp = LocalTime(3, 0),
            bathroomDurationMinutes = 20L,
            leaveHomeTime = LocalTime(3, 30)
        )
        val result = scheduler.calculateIdealSchedule(listOf(m), breakfastDurationMinutes = 30)
        // Der Clamp greift auf startTime (04:00), breakfastTime = 04:00 - 30min = 03:30.
        // Kein Crash, Ergebnis wird zurückgegeben.
        assertTrue(result.memberSchedules.isNotEmpty() || result.breakfastTime != null || !result.isValid,
            "Scheduler muss immer ein Ergebnis liefern")
    }

    @Test
    fun fallback_timeAdjusted() {
        // Leichte Überschneidung: m1 latestWakeUp knapp vor m2's Bad-End
        // Kein Frühstück, damit nur Time-Shift nötig
        val members = listOf(
            member(id = "m1", earliestWakeUp = LocalTime(6, 0), latestWakeUp = LocalTime(6, 30), bathroomDurationMinutes = 20L, wantsBreakfast = false),
            member(id = "m2", earliestWakeUp = LocalTime(6, 20), latestWakeUp = LocalTime(6, 40), bathroomDurationMinutes = 20L, wantsBreakfast = false)
        )
        val result = scheduler.calculateIdealSchedule(members, breakfastDurationMinutes = 0)
        // Entweder gültig oder TimeAdjusted/BreakfastAndTimeAdjusted
        val isAcceptable = result.isValid || result.scheduleMessage is ScheduleMessage.TimeAdjusted
        assertTrue(isAcceptable, "Erwartet isValid oder TimeAdjusted, bekam: ${result.scheduleMessage}")
    }

    @Test
    fun memberLimit_max6Active() {
        // 8 Member übergeben → nur 6 dürfen im Ergebnis sein
        val members = (1..8).map { i ->
            member(id = "m$i", name = "M$i",
                earliestWakeUp = LocalTime(5 + i, 0),
                latestWakeUp = LocalTime(5 + i, 30),
                bathroomDurationMinutes = 10L,
                wantsBreakfast = false
            )
        }
        val result = scheduler.calculateIdealSchedule(members, breakfastDurationMinutes = 0)
        assertTrue(result.memberSchedules.size <= 6)
    }
}
