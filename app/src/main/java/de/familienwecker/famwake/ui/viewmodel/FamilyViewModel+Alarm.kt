package de.familienwecker.famwake.ui.viewmodel

import com.telemetrydeck.sdk.TelemetryDeck
import de.familienwecker.famwake.R
import de.familienwecker.famwake.model.FamilySchedule
import de.familienwecker.famwake.model.ScheduleMessage
import de.familienwecker.famwake.model.toJavaLocalDateTime
import de.familienwecker.famwake.model.toKmpLocalDateTime
import de.familienwecker.famwake.ui.util.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

// ─── Alarm-Logik ──────────────────────────────────────────────────────────────

/**
 * Berechnet den optimalen Weckplan für alle Mitglieder und plant Alarme.
 * Wird aufgerufen wenn sich Members, isAlarmEnabled oder myMemberId ändern.
 */
internal fun FamilyViewModel.recalculateSchedule() {
    val currentMembers = _members.value
    val alarmsOn = isAlarmEnabled.value

    if (currentMembers.isNotEmpty()) {
        // Cancel-and-replace: verhindert dass eine ältere Berechnung (mit veraltetem
        // alarmsOn/myMemberId) eine neuere überschreibt – Race Condition bei Login-Flow.
        scheduleJob?.cancel()
        scheduleJob = scope.launch {
            try {
                val currentMyMemberId = myMemberId.value
                val rawMembers = if (alarmsOn) {
                    // Members ohne aktiven Gerätealarm ausschließen (z.B. anderes Gerät hat global AUS)
                    currentMembers.filter { it.deviceAlarmEnabled != false }
                } else {
                    currentMembers.filter { it.id != currentMyMemberId && it.deviceAlarmEnabled != false }
                }

                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val tomorrow = today.plus(1, DateTimeUnit.DAY)
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time

                // Two-Pass: Erst heute berechnen, nur auf morgen wechseln wenn ALLE
                // geplanten Weckzeiten von heute bereits verstrichen sind.
                // Verhindert den Sprung auf "Sonntag" wenn Kind/Papa noch geweckt werden müssen.
                val todayMembers = rawMembers.map { resolveEffectiveMember(it, forDate = today) }
                val todayHasActive = todayMembers.any { !it.isPaused }

                val (calculationMembers, targetDate) = if (todayHasActive) {
                    val todaySchedule = withContext(Dispatchers.Default) {
                        scheduler.calculateIdealSchedule(todayMembers)
                    }
                    val latestAlarm = todaySchedule.memberSchedules.maxByOrNull { it.wakeUpTime }?.wakeUpTime
                    if (latestAlarm != null && now < latestAlarm) {
                        // Heute hat noch anstehende Alarme – heute verwenden
                        todayMembers to today
                    } else {
                        // Alle Alarme von heute sind vorbei – morgen berechnen
                        rawMembers.map { resolveEffectiveMember(it, forDate = tomorrow) } to tomorrow
                    }
                } else {
                    // Heute kein aktives Profil – morgen berechnen
                    rawMembers.map { resolveEffectiveMember(it, forDate = tomorrow) } to tomorrow
                }

                if (calculationMembers.none { !it.isPaused }) {
                    if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                        android.util.Log.w("FamWake_Alarm", "recalculate: all members paused – checking grace period before cancel")
                    }
                    _schedule.value = FamilySchedule(emptyList(), null, true, ScheduleMessage.NoActiveSchedule)

                    val myMember = currentMembers.find { it.id == currentMyMemberId }
                    val currentDao = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    val myProfile = myMember?.dayProfiles?.get(currentDao.dayOfWeek.value)
                    val myWakeUpToday = myProfile?.latestWakeUp
                    val inGrace = if (myWakeUpToday != null) {
                        val todayAlarmMillis = LocalDateTime(currentDao, myWakeUpToday)
                            .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
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
                }.copy(targetDate = targetDate)
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
            } catch (e: CancellationException) {
                // Cancel-and-replace: alter Job wurde durch neuen recalculateSchedule() abgelöst – kein Fehler
                throw e
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
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val tomorrow = today.plus(1, DateTimeUnit.DAY)
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time

    val currentMyMemberId = myMemberId.value ?: run {
        if (de.familienwecker.famwake.BuildConfig.DEBUG) {
            android.util.Log.w("FamWake_Alarm", "applyAlarms: myMemberId is null, skipping")
        }
        return
    }
    if (schedule.memberSchedules.isEmpty()) return

    for (memberSchedule in schedule.memberSchedules) {
        if (memberSchedule.member.id == currentMyMemberId) {
            val wakeUpTime = memberSchedule.wakeUpTime

            // Autoritatives Zieldatum aus dem Two-Pass in recalculateSchedule() übernehmen.
            // Früher wurde targetDate hier nochmal anhand "now > wakeUpTime" bestimmt –
            // das ignorierte den Wochentag und führte z.B. dazu, dass nach Di-6:30 der
            // Mi-7:30-Alarm fälschlicherweise noch am Di-7:30 gesetzt wurde.
            val targetDate = schedule.targetDate
                ?: if (now > wakeUpTime) tomorrow else today

            // Grace-Period: verhindert, dass ein soeben gefeuerter Alarm den nächsten
            // Tag überspringt, weil targetDate nun bereits auf morgen zeigt.
            if (targetDate != today) {
                val todayAlarmMillis = LocalDateTime(today, wakeUpTime)
                    .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
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

            val targetDateTime = LocalDateTime(targetDate, wakeUpTime)
            val newAlarmMillis = targetDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

            if (newAlarmMillis == lastScheduledAlarmMillis) return

            // isAwakeTodayLocal.value könnte veraltet sein, wenn der App-Prozess über Nacht
            // im Hintergrund läuft und kein Resume stattfand. Daher appSettings direkt befragen –
            // isAwakeTodayEffective() prüft das gespeicherte Datum live gegen heute.
            if (appSettings.isAwakeTodayEffective() && targetDate == today) {
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
 * Löst das effektive DayProfile für ein bestimmtes Datum auf.
 * @param forDate Zieldatum. null = Auto-Detect (heute wenn latestWakeUp nicht vorbei, sonst morgen).
 */
internal fun FamilyViewModel.resolveEffectiveMember(
    member: de.familienwecker.famwake.model.FamilyMember,
    forDate: kotlinx.datetime.LocalDate? = null
): de.familienwecker.famwake.model.FamilyMember {
    val profiles = member.dayProfiles ?: return member
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    val targetDate = if (forDate != null) {
        forDate
    } else {
        val todayDow = today.dayOfWeek.value
        val todayProfile = profiles[todayDow]
        if (todayProfile != null && todayProfile.isActive && now < todayProfile.latestWakeUp) {
            today
        } else {
            today.plus(1, DateTimeUnit.DAY)
        }
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
    // Beim Aus- UND Einschalten wird „Schon wach" zurückgesetzt.
    // Bewusster Toggle = expliziter Neustart – unabhängig vom vorherigen Zustand.
    appSettings.setAwakeToday(false)
    // Cache zurücksetzen damit applyAlarms() beim erneuten Einschalten nicht
    // durch den Duplikat-Guard (newAlarmMillis == lastScheduledAlarmMillis) überspringt.
    if (!enabled) lastScheduledAlarmMillis = null
    TelemetryDeck.signal(if (enabled) "alarm.globalEnabled" else "alarm.globalDisabled")
    // Auch member.isAwakeToday in Room + Firestore zurücksetzen, damit das
    // ☀️-Icon in der MemberCard sofort verschwindet (liest Firestore, nicht AppSettings).
    val memberId = myMemberId.value
    if (memberId != null) {
        val member = _members.value.find { it.id == memberId }
        if (member != null && member.isAwakeToday) {
            val resetMember = member.copy(isAwakeToday = false)
            scope.launch {
                try {
                    memberRepository.upsertMember(resetMember)
                } catch (e: Exception) {
                    if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                        android.util.Log.w("FamWake_Alarm", "setAlarmEnabled: Room reset failed: ${e.message}")
                    }
                }
            }
            addOrUpdateMemberDebounced(resetMember)
        }
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
    // "title"-Query-Parameter enthält den lesbaren Namen (z.B. "Morning Strum").
    // Fallback auf letztes Pfadsegment für ältere/custom URIs ohne title-Parameter.
    val parsedUri = android.net.Uri.parse(uri)
    val soundName = parsedUri.getQueryParameter("title")
        ?: uri.substringAfterLast("/").substringBeforeLast(".")
    TelemetryDeck.signal("alarm.soundChanged", mapOf("soundName" to soundName))
}

/**
 * ADMIN/DEBUG: Setzt das DayProfile des heutigen Wochentags so,
 * dass der Wecker in ~2 Minuten klingelt.
 */
fun FamilyViewModel.setDebugAlarmIn5Minutes() {
    val memberId = myMemberId.value ?: return
    val member   = _members.value.find { it.id == memberId } ?: return
    
    val currentInstant = Clock.System.now()
    val nowLocal = currentInstant.toLocalDateTime(TimeZone.currentSystemDefault())
    // Truncate to minutes (by zeroing seconds and nanoseconds)
    val now = LocalDateTime(nowLocal.year, nowLocal.monthNumber, nowLocal.dayOfMonth, nowLocal.hour, nowLocal.minute, 0, 0)
    
    // Kotlinx.datetime has no direct plusMinutes, we add to Instant and convert back, or use DateTimePeriod?
    // Using Instant plus:
    val targetInstant = currentInstant.plus(2, DateTimeUnit.MINUTE)
    val earliestInstant = currentInstant.plus(1, DateTimeUnit.MINUTE)
    val latestInstant = currentInstant.plus(3, DateTimeUnit.MINUTE)
    val leaveInstant = currentInstant.plus(33, DateTimeUnit.MINUTE)

    val earliest = earliestInstant.toLocalDateTime(TimeZone.currentSystemDefault()).time
    val latest = latestInstant.toLocalDateTime(TimeZone.currentSystemDefault()).time
    val leaveHomeTime = leaveInstant.toLocalDateTime(TimeZone.currentSystemDefault()).time
    
    val todayKey = Clock.System.todayIn(TimeZone.currentSystemDefault()).dayOfWeek.value

    val debugProfile = de.familienwecker.famwake.model.DayProfile(
        isActive = true,
        earliestWakeUp = earliest,
        latestWakeUp = latest,
        bathroomDurationMinutes = 1L,
        wantsBreakfast = false,
        leaveHomeTime = leaveHomeTime
    )
    val updatedDayProfiles = (member.dayProfiles ?: mapOf()).toMutableMap()
    updatedDayProfiles[todayKey] = debugProfile

    val updatedMember = member.copy(
        dayProfiles = updatedDayProfiles,
        isPaused = false
    )
    addOrUpdateMember(updatedMember)
    // Öffentliche Funktion nutzen (nicht appSettings direkt) –
    // setzt lastScheduledAlarmMillis zurück, damit applyAlarms() nach
    // Deaktivieren + erneut Aktivieren den Alarm nicht als Duplikat überspringt.
    setAlarmEnabled(true)
}

fun FamilyViewModel.snooze(memberId: String, memberName: String) {
    val snoozeInstant = Clock.System.now().plus(5, DateTimeUnit.MINUTE)
    val snoozeTime = snoozeInstant.toLocalDateTime(TimeZone.currentSystemDefault())
    appSettings.setSnoozeUntil(snoozeTime)
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
    TelemetryDeck.signal("alarm.snoozed")
}

fun FamilyViewModel.cancelSnooze(memberId: String) {
    appSettings.setSnoozeUntil(null)
    alarmScheduler.cancelWakeUp(memberId, isSnooze = true)
    lastScheduledAlarmMillis = null
    // Tracking: Nutzer hat Snooze manuell abgebrochen (Gegenstück zu alarm.snoozed)
    TelemetryDeck.signal("alarm.snoozeCancelled")
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
