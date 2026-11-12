package de.familienwecker.famwake.algorithm

import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.model.FamilySchedule
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime

/**
 * Zentraler Rechner für die nächste Weckzeit zur Vermeidung von Berechnungs-Inkonsistenzen
 * an verschiedenen Aufrufstellen (ViewModel, RingingActivity, BootReceiver, TileService, TimeChangeReceiver).
 */
object NextAlarmCalculator {

    /**
     * Löst das effektive Mitglieds-Profil für ein Ziel-Datum auf.
     */
    fun resolveEffectiveMember(
        member: FamilyMember,
        forDate: LocalDate? = null,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): FamilyMember {
        val profiles = member.dayProfiles ?: return member
        val today = Clock.System.todayIn(timeZone)
        val nowTime = Clock.System.now().toLocalDateTime(timeZone).time

        val targetDate = if (forDate != null) {
            forDate
        } else {
            val todayDow = today.dayOfWeek.value
            val todayProfile = profiles[todayDow]
            if (todayProfile != null && todayProfile.isActive && nowTime < todayProfile.latestWakeUp) {
                today
            } else {
                today.plus(1, DateTimeUnit.DAY)
            }
        }

        val targetDow = targetDate.dayOfWeek.value
        val profile = profiles[targetDow] ?: return member.copy(isPaused = true)
        if (!profile.isActive) return member.copy(isPaused = true)

        val resolved = member.copy(
            earliestWakeUp = profile.earliestWakeUp,
            latestWakeUp = profile.latestWakeUp,
            bathroomDurationMinutes = profile.bathroomDurationMinutes,
            wantsBreakfast = profile.wantsBreakfast,
            leaveHomeTime = profile.leaveHomeTime,
            breakfastDurationMinutes = profile.breakfastDurationMinutes ?: member.breakfastDurationMinutes,
            sequenceOrder = profile.sequenceOrder ?: member.sequenceOrder,
            dayProfiles = mapOf(targetDow to profile),
            isSimpleMode = profile.isSimpleMode,
            snoozeUntil = if (targetDate == today) member.snoozeUntil else null,
            snoozeCount = if (targetDate == today) member.snoozeCount else 0
        )

        // Snooze-Constraint: Wenn dieser Member heute aktiv snoozed, fixiere Weckzeit
        val snoozeUntil = resolved.snoozeUntil
        if (snoozeUntil != null) {
            val nowDateTime = Clock.System.now().toLocalDateTime(timeZone)
            if (nowDateTime < snoozeUntil && snoozeUntil.date == targetDate) {
                val snoozeTime = snoozeUntil.time
                val absorbableMinutes = if (resolved.snoozeCount <= 1) {
                    minOf(
                        de.familienwecker.famwake.model.SnoozeConfig.SNOOZE_DURATION_MINUTES.toLong(),
                        maxOf(0L, resolved.bathroomDurationMinutes - de.familienwecker.famwake.model.SnoozeConfig.MIN_BATHROOM_MINUTES)
                    )
                } else {
                    0L
                }
                val reducedBathroom = resolved.bathroomDurationMinutes - absorbableMinutes
                return resolved.copy(
                    earliestWakeUp = snoozeTime,
                    latestWakeUp = snoozeTime,
                    bathroomDurationMinutes = reducedBathroom
                )
            } else {
                return resolved.copy(snoozeUntil = null, snoozeCount = 0)
            }
        }
        return resolved
    }

    /**
     * Berechnet die nächste Weckzeit für ein bestimmtes Mitglied an einem Zieldatum unter Berücksichtigung
     * aller Familienmitglieder, Bad-Slots und Puffer.
     */
    fun calculateNextWakeUpForDate(
        members: List<FamilyMember>,
        targetMemberId: String,
        targetDate: LocalDate,
        globalBufferMinutes: Long = 0,
        vacationUntil: String? = null,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): LocalDateTime? {
        if (vacationUntil != null && targetDate.toString() <= vacationUntil) {
            return null
        }

        // Alle Mitglieder für dieses Zieldatum auflösen
        val resolvedMembers = members.map { resolveEffectiveMember(it, targetDate, timeZone) }
            .sortedBy { it.sequenceOrder }

        val schedule = Scheduler().calculateIdealSchedule(
            members = resolvedMembers,
            globalBufferMinutes = globalBufferMinutes
        )

        val mySchedule = schedule.memberSchedules.find { it.member.id == targetMemberId }
        return if (mySchedule != null && !mySchedule.member.isPaused && mySchedule.member.deviceAlarmEnabled != false) {
            LocalDateTime(targetDate, mySchedule.wakeUpTime)
        } else {
            null
        }
    }

    /**
     * Bestimmt den nächsten Alarm-Termin ausgehend von einer gespeicherten Weckzeit (z.B. Backup-Prefs).
     */
    fun calculateNextAlarmFromSavedTime(
        alarmTime: LocalTime,
        now: LocalDateTime
    ): LocalDateTime {
        return if (now.time < alarmTime) {
            LocalDateTime(now.date, alarmTime)
        } else {
            LocalDateTime(now.date.plus(1, DateTimeUnit.DAY), alarmTime)
        }
    }

    /**
     * Prüft, ob der "Schon wach"-Status zurückgesetzt werden muss, wenn ein Profil für heute
     * geändert wurde und noch ein zukünftiger Alarm ansteht.
     */
    fun shouldResetAwakeToday(
        oldMember: FamilyMember?,
        newMember: FamilyMember,
        todayDow: Int,
        currentTime: LocalTime
    ): Boolean {
        val oldProfile = oldMember?.dayProfiles?.get(todayDow)
        val newProfile = newMember.dayProfiles?.get(todayDow)

        val oldEarliest = oldProfile?.earliestWakeUp ?: oldMember?.earliestWakeUp
        val newEarliest = newProfile?.earliestWakeUp ?: newMember.earliestWakeUp
        val oldLatest = oldProfile?.latestWakeUp ?: oldMember?.latestWakeUp
        val newLatest = newProfile?.latestWakeUp ?: newMember.latestWakeUp

        val oldIsActive = oldProfile?.isActive ?: !(oldMember?.isPaused ?: false)
        val newIsActive = newProfile?.isActive ?: !newMember.isPaused

        val todayProfileChanged = oldProfile != newProfile || oldEarliest != newEarliest || oldLatest != newLatest || oldIsActive != newIsActive
        val effectiveNewTime = maxOf(newEarliest, newLatest)
        val hasFutureAlarmToday = newIsActive && effectiveNewTime > currentTime

        return todayProfileChanged && hasFutureAlarmToday
    }

    /**
     * Prüft, ob das "Schon wach"-Symbol (☀️) in der Mitgliederliste angezeigt werden soll.
     * Nur sichtbar, wenn der Member heute wach ist UND der angezeigte Weckplan für heute gilt
     * (verhindert das ☀️-Symbol neben dem Namen, sobald abends die Weckzeit für morgen angezeigt wird).
     */
    fun shouldShowAwakeBadge(
        isAwakeToday: Boolean,
        scheduleTargetDate: LocalDate?,
        today: LocalDate
    ): Boolean {
        if (!isAwakeToday) return false
        return scheduleTargetDate == null || scheduleTargetDate == today
    }
}
