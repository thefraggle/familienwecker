package de.familienwecker.famwake.ui.viewmodel

import de.familienwecker.famwake.R
import de.familienwecker.famwake.model.FamilySchedule
import de.familienwecker.famwake.model.ScheduleMessage
import de.familienwecker.famwake.model.toJavaLocalTime
import de.familienwecker.famwake.model.toKmpLocalTime
import de.familienwecker.famwake.model.toJavaLocalDateTime
import de.familienwecker.famwake.model.toKmpLocalDateTime
import de.familienwecker.famwake.ui.util.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ─── Alarm-Logik ──────────────────────────────────────────────────────────────

/**
 * Berechnet den optimalen Weckplan für alle Mitglieder und plant Alarme.
 * Wird aufgerufen wenn sich Members, isAlarmEnabled oder myMemberId ändern.
 */
internal fun FamilyViewModel.recalculateSchedule() {
    val currentMembers = _members.value
    val alarmsOn = isAlarmEnabled.value

    if (currentMembers.isNotEmpty()) {
        scope.launch {
            try {
                val currentMyMemberId = myMemberId.value
                val rawMembers = if (alarmsOn) {
                    currentMembers
                } else {
                    currentMembers.filter { it.id != currentMyMemberId }
                }

                val calculationMembers = rawMembers.map { resolveEffectiveMember(it) }

                if (calculationMembers.none { !it.isPaused }) {
                    if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                        android.util.Log.w("FamWake_Alarm", "recalculate: all members paused – checking grace period before cancel")
                    }
                    _schedule.value = FamilySchedule(emptyList(), null, true, ScheduleMessage.NoActiveSchedule)

                    val myMember = currentMembers.find { it.id == currentMyMemberId }
                    val myProfile = myMember?.dayProfiles?.get(java.time.LocalDate.now().dayOfWeek.value)
                    val myWakeUpToday = myProfile?.latestWakeUp?.toJavaLocalTime()
                    val inGrace = if (myWakeUpToday != null) {
                        val todayAlarmMillis = java.time.LocalDateTime.of(java.time.LocalDate.now(), myWakeUpToday)
                            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val millisSince = System.currentTimeMillis() - todayAlarmMillis
                        millisSince in 0..300_000
                    } else false

                    if (inGrace) {
                        if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                            android.util.Log.d("FamWake_Alarm", "recalculate: GRACE PERIOD – skipping cancel (alarm just fired)")
                        }
                    } else {
                        if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                            android.util.Log.w("FamWake_Alarm", "recalculate: cancelling all alarms (all paused, outside grace)")
                        }
                        currentMembers.forEach { alarmScheduler.cancelWakeUp(it.id) }
                    }
                    return@launch
                }

                val result = withContext(Dispatchers.Default) {
                    scheduler.calculateIdealSchedule(calculationMembers)
                }
                _schedule.value = result

                if (alarmsOn && result.memberSchedules.isNotEmpty()) {
                    if (!result.isValid) {
                        if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                            android.util.Log.w("FamWake_Alarm", "recalculate: Applying FALLBACK alarms for invalid schedule")
                        }
                    }
                    applyAlarms(result)
                } else {
                    if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                        android.util.Log.w("FamWake_Alarm", "recalculate: cancelling alarms – alarmsOn=$alarmsOn, hasSchedules=${result.memberSchedules.isNotEmpty()}")
                    }
                    currentMembers.forEach { alarmScheduler.cancelWakeUp(it.id) }
                }
            } catch (e: Exception) {
                _errorMessage.value = UiText.StringResource(R.string.error_calculate_schedule, e.localizedMessage ?: getApplication<android.app.Application>().getString(R.string.add_member_unknown))
                _schedule.value = null
            }
        }
    } else {
        cancelAlarmForCurrentUser()
        _schedule.value = null
    }
}

/**
 * Plant den konkreten AlarmManager-Eintrag für den eingeloggten User.
 */
internal fun FamilyViewModel.applyAlarms(schedule: FamilySchedule) {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    val currentMyMemberId = myMemberId.value ?: run {
        if (de.familienwecker.famwake.BuildConfig.DEBUG) {
            android.util.Log.w("FamWake_Alarm", "applyAlarms: myMemberId is null, skipping")
        }
        return
    }
    if (schedule.memberSchedules.isEmpty()) return

    for (memberSchedule in schedule.memberSchedules) {
        if (memberSchedule.member.id == currentMyMemberId) {
            val wakeUpTime = memberSchedule.wakeUpTime.toJavaLocalTime()
            val targetDate = if (LocalTime.now().isAfter(wakeUpTime)) tomorrow else today

            if (targetDate == tomorrow) {
                val todayAlarmMillis = LocalDateTime.of(today, wakeUpTime)
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                val millisSinceTodayAlarm = System.currentTimeMillis() - todayAlarmMillis
                if (millisSinceTodayAlarm in 0..300_000) return
            }

            val dayOfWeek = targetDate.dayOfWeek.value
            val dayProfile = memberSchedule.member.dayProfiles?.get(dayOfWeek)

            if (dayProfile != null && !dayProfile.isActive) {
                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.w("FamWake_Alarm", "applyAlarms: day $dayOfWeek is inactive, cancelling alarm")
                }
                alarmScheduler.cancelWakeUp(currentMyMemberId)
                lastScheduledAlarmMillis = null
                _schedule.value = FamilySchedule(emptyList(), null, true, ScheduleMessage.NoActiveSchedule)
                return
            }

            val targetDateTime = LocalDateTime.of(targetDate, wakeUpTime)
            val newAlarmMillis = targetDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (newAlarmMillis == lastScheduledAlarmMillis) return

            if (isAwakeTodayLocal.value && targetDate == today) {
                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.w("FamWake_Alarm", "applyAlarms: isAwakeToday=true for today, cancelling alarm")
                }
                alarmScheduler.cancelWakeUp(currentMyMemberId)
                lastScheduledAlarmMillis = null
                return
            }

            alarmScheduler.scheduleWakeUp(
                wakeUpTime = targetDateTime,
                memberId = memberSchedule.member.id,
                memberName = memberSchedule.member.name,
                soundUri = alarmSoundUri.value,
                onPermissionDenied = {
                    if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                        android.util.Log.e("FamWake_Alarm", "applyAlarms: SCHEDULE_EXACT_ALARM permission denied!")
                    }
                    _errorMessage.value = UiText.StringResource(R.string.error_alarm_permission)
                }
            )
            lastScheduledAlarmMillis = newAlarmMillis
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.i("FamWake_Alarm", "applyAlarms: alarm SET for $targetDateTime")
            }
        }
    }
}

/**
 * Löst das effektive DayProfile für den nächsten Alarm-Tag auf.
 */
internal fun FamilyViewModel.resolveEffectiveMember(member: de.familienwecker.famwake.model.FamilyMember): de.familienwecker.famwake.model.FamilyMember {
    val profiles = member.dayProfiles ?: return member
    val now = LocalTime.now()
    val today = LocalDate.now()
    val todayDow = today.dayOfWeek.value
    val todayProfile = profiles[todayDow]

    val targetDate = if (todayProfile != null && todayProfile.isActive && now.isBefore(todayProfile.latestWakeUp.toJavaLocalTime())) {
        today
    } else {
        today.plusDays(1)
    }

    val targetDow = targetDate.dayOfWeek.value
    val profile = profiles[targetDow] ?: return member.copy(isPaused = true)
    if (!profile.isActive) return member.copy(isPaused = true)

    return member.copy(
        earliestWakeUp          = profile.earliestWakeUp,
        latestWakeUp            = profile.latestWakeUp,
        bathroomDurationMinutes = profile.bathroomDurationMinutes,
        wantsBreakfast          = profile.wantsBreakfast,
        leaveHomeTime           = profile.leaveHomeTime
    )
}

/** Cancelt den Alarm des aktuell eingeloggten Users. */
internal fun FamilyViewModel.cancelAlarmForCurrentUser() {
    myMemberId.value?.let { alarmScheduler.cancelWakeUp(it) }
}

fun FamilyViewModel.setAlarmEnabled(enabled: Boolean) {
    if (enabled && myMemberId.value == null) return
    checkOfflineAndHint()
    if (!enabled) {
        appSettings.setAwakeToday(false)
    }
    appSettings.setAlarmEnabled(enabled)
    val currentFamilyId = familyId.value
    val currentMemberId = myMemberId.value
    if (currentFamilyId != null && currentMemberId != null) {
        alarmToggleJob?.cancel()
        alarmToggleJob = scope.launch {
            try {
                kotlinx.coroutines.delay(2000)
                // NonCancellable: delay ist unterbrechbar (Entprellung), Write nicht
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    repository.updateDeviceAlarmEnabled(currentFamilyId, currentMemberId, enabled)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Job wurde durch neuen setAlarmEnabled-Aufruf abgebrochen → kein Error, korrekt
                throw e
            }
        }
    }
}

fun FamilyViewModel.setAlarmSoundUri(uri: String) {
    appSettings.setAlarmSoundUri(uri)
}

/**
 * ADMIN/DEBUG: Setzt das DayProfile des heutigen Wochentags so,
 * dass der Wecker in ~2 Minuten klingelt.
 */
fun FamilyViewModel.setDebugAlarmIn5Minutes() {
    val memberId = myMemberId.value ?: return
    val member   = _members.value.find { it.id == memberId } ?: return
    val now      = LocalTime.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES)
    val target   = now.plusMinutes(2)
    val earliest = target.minusMinutes(1)
    val latest   = target.plusMinutes(1)
    val todayKey = LocalDate.now().dayOfWeek.value
    val leaveHomeTime = latest.plusMinutes(30)

    val debugProfile = de.familienwecker.famwake.model.DayProfile(
        isActive = true,
        earliestWakeUp = earliest.toKmpLocalTime(),
        latestWakeUp = latest.toKmpLocalTime(),
        bathroomDurationMinutes = 1L,
        wantsBreakfast = false,
        leaveHomeTime = leaveHomeTime.toKmpLocalTime()
    )
    val updatedDayProfiles = (member.dayProfiles ?: mapOf()).toMutableMap()
    updatedDayProfiles[todayKey] = debugProfile

    val updatedMember = member.copy(
        dayProfiles = updatedDayProfiles,
        isPaused = false
    )
    addOrUpdateMember(updatedMember)
    appSettings.setAlarmEnabled(true)
}

fun FamilyViewModel.snooze(memberId: String, memberName: String) {
    val snoozeTime = java.time.LocalDateTime.now().plusMinutes(5)
    appSettings.setSnoozeUntil(snoozeTime.toKmpLocalDateTime())
    alarmScheduler.scheduleWakeUp(
        wakeUpTime = snoozeTime,
        memberId = memberId,
        memberName = memberName,
        soundUri = alarmSoundUri.value,
        isSnooze = true,
        onPermissionDenied = {
            _errorMessage.value = UiText.StringResource(R.string.error_alarm_permission)
        }
    )
}

fun FamilyViewModel.cancelSnooze(memberId: String) {
    appSettings.setSnoozeUntil(null)
    alarmScheduler.cancelWakeUp(memberId, isSnooze = true)
    lastScheduledAlarmMillis = null
    recalculateSchedule()
}

/** Übersetzt eine ScheduleMessage in lokalisierbares UiText. */
fun FamilyViewModel.scheduleMessageToUiText(msg: ScheduleMessage): UiText = when (msg) {
    is ScheduleMessage.OptimalPlan              -> UiText.StringResource(R.string.schedule_message_optimal)
    is ScheduleMessage.NoActiveMembers          -> UiText.StringResource(R.string.schedule_message_no_members)
    is ScheduleMessage.NoValidScheduleFound     -> UiText.StringResource(R.string.schedule_message_no_valid)
    is ScheduleMessage.TimeAdjusted             -> UiText.StringResource(R.string.schedule_message_time_adjusted, msg.minutes)
    is ScheduleMessage.BreakfastReduced         -> UiText.StringResource(R.string.schedule_message_breakfast_reduced, msg.minutes)
    is ScheduleMessage.BreakfastAndTimeAdjusted -> UiText.StringResource(R.string.schedule_message_breakfast_and_time_adjusted, msg.breakfast, msg.shift)
    is ScheduleMessage.MemberConflict           -> UiText.StringResource(R.string.schedule_message_member_conflict, msg.memberName)
    is ScheduleMessage.NoActiveSchedule         -> UiText.StringResource(R.string.main_no_active_schedule)
}
