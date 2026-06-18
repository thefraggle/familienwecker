package de.familienwecker.famwake.ui.viewmodel

import com.telemetrydeck.sdk.TelemetryDeck
import de.familienwecker.famwake.R
import de.familienwecker.famwake.model.FamilySchedule
import de.familienwecker.famwake.model.ScheduleMessage
import de.familienwecker.famwake.model.toJavaLocalDateTime
import de.familienwecker.famwake.model.toKmpLocalDateTime
import de.familienwecker.famwake.ui.util.UiText
import kotlinx.collections.immutable.toPersistentList
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
import de.familienwecker.famwake.model.SnoozeConfig
import de.familienwecker.famwake.model.toJavaLocalTime
import de.familienwecker.famwake.NotificationChannels

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

                // 1. Berechne UI Schedule (basierend auf selectedDayOfWeek)
                val selectedDay = selectedDayOfWeek.value
                val uiTargetDate = if (selectedDay != null) {
                    var checkDate = today
                    while (checkDate.dayOfWeek.value != selectedDay) {
                        checkDate = checkDate.plus(1, DateTimeUnit.DAY)
                    }
                    if (checkDate == today) {
                        val todayMembers = rawMembers.map { resolveEffectiveMember(it, forDate = today) }
                        val todayHasActive = todayMembers.any { !it.isPaused }
                        if (todayHasActive) {
                            val todaySchedule = withContext(Dispatchers.Default) {
                                scheduler.calculateIdealSchedule(todayMembers, globalBufferMinutes = _globalBufferMinutes.value)
                            }
                            val latestAlarm = todaySchedule.memberSchedules.maxByOrNull { it.wakeUpTime }?.wakeUpTime
                            if (latestAlarm != null && now < latestAlarm) {
                                today
                            } else {
                                today.plus(7, DateTimeUnit.DAY)
                            }
                        } else {
                            today.plus(7, DateTimeUnit.DAY)
                        }
                    } else {
                        checkDate
                    }
                } else {
                    // Auto-Modus
                    val todayMembers = rawMembers.map { resolveEffectiveMember(it, forDate = today) }
                    val todayHasActive = todayMembers.any { !it.isPaused }
                    if (todayHasActive) {
                        val todaySchedule = withContext(Dispatchers.Default) {
                            scheduler.calculateIdealSchedule(todayMembers, globalBufferMinutes = _globalBufferMinutes.value)
                        }
                        val latestAlarm = todaySchedule.memberSchedules.maxByOrNull { it.wakeUpTime }?.wakeUpTime
                        if (latestAlarm != null && now < latestAlarm) {
                            today
                        } else {
                            // Morgen prüfen: Wenn morgen keine aktiven Profile hat,
                            // auf heute zurückfallen statt leere Liste anzuzeigen
                            val tomorrowMembers = rawMembers.map { resolveEffectiveMember(it, forDate = tomorrow) }
                            val tomorrowHasActive = tomorrowMembers.any { !it.isPaused }
                            if (tomorrowHasActive) tomorrow else today
                        }
                    } else {
                        // Heute keine aktiven Profile → morgen prüfen, sonst heute
                        val tomorrowMembers = rawMembers.map { resolveEffectiveMember(it, forDate = tomorrow) }
                        val tomorrowHasActive = tomorrowMembers.any { !it.isPaused }
                        if (tomorrowHasActive) tomorrow else today
                    }
                }

                val uiCalculationMembers = rawMembers.map { resolveEffectiveMember(it, forDate = uiTargetDate) }
                    .sortedWith(compareBy({ it.dayProfiles?.get(uiTargetDate.dayOfWeek.value)?.sequenceOrder ?: it.sequenceOrder }, { it.createdAt ?: 0L }))
                
                val uiResult = if (uiCalculationMembers.none { !it.isPaused }) {
                    FamilySchedule(emptyList(), null, true, ScheduleMessage.NoActiveSchedule).copy(targetDate = uiTargetDate)
                } else {
                    val res = withContext(Dispatchers.Default) {
                        scheduler.calculateIdealSchedule(uiCalculationMembers, globalBufferMinutes = _globalBufferMinutes.value)
                    }
                    res.copy(targetDate = uiTargetDate)
                }
                
                _schedule.value = uiResult

                // 2. Berechne Device Alarm Schedule (IMMER Auto-Modus)
                val deviceTargetDate = run {
                    val todayMembers = rawMembers.map { resolveEffectiveMember(it, forDate = today) }
                    val todayHasActive = todayMembers.any { !it.isPaused }
                    if (todayHasActive) {
                        val todaySchedule = withContext(Dispatchers.Default) {
                            scheduler.calculateIdealSchedule(todayMembers, globalBufferMinutes = _globalBufferMinutes.value)
                        }
                        val latestAlarm = todaySchedule.memberSchedules.maxByOrNull { it.wakeUpTime }?.wakeUpTime
                        if (latestAlarm != null && now < latestAlarm) {
                            today
                        } else {
                            tomorrow
                        }
                    } else {
                        tomorrow
                    }
                }

                val deviceCalculationMembers = rawMembers.map { resolveEffectiveMember(it, forDate = deviceTargetDate) }
                    .sortedWith(compareBy({ it.dayProfiles?.get(deviceTargetDate.dayOfWeek.value)?.sequenceOrder ?: it.sequenceOrder }, { it.createdAt ?: 0L }))
                
                val deviceResult = if (deviceCalculationMembers.none { !it.isPaused }) {
                    FamilySchedule(emptyList(), null, true, ScheduleMessage.NoActiveSchedule).copy(targetDate = deviceTargetDate)
                } else {
                    val res = withContext(Dispatchers.Default) {
                        scheduler.calculateIdealSchedule(deviceCalculationMembers, globalBufferMinutes = _globalBufferMinutes.value)
                    }
                    res.copy(targetDate = deviceTargetDate)
                }
                _deviceSchedule.value = deviceResult

                // 3. Alarme anwenden basierend auf deviceResult (inkl. Grace-Period Check)
                if (deviceResult.memberSchedules.isEmpty()) {
                    if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                        android.util.Log.w("FamWake_Alarm", "recalculate: all members paused – checking grace period before cancel")
                    }

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
                } else {
                    if (alarmsOn) {
                        if (!deviceResult.isValid) {
                            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                                android.util.Log.w("FamWake_Alarm", "recalculate: Applying FALLBACK alarms for invalid schedule")
                            }
                        }
                        applyAlarms(deviceResult)

                        // Shift-Notification: Informiere User wenn Weckzeit durch Snooze eines
                        // anderen Members verschoben wurde
                        val mySchedule = deviceResult.memberSchedules.find { it.member.id == currentMyMemberId }
                        if (mySchedule != null) {
                            val newWakeTime = mySchedule.wakeUpTime
                            val oldWakeTime = lastKnownWakeUpTime
                            if (oldWakeTime != null && newWakeTime != oldWakeTime) {
                                // Prüfe ob ein anderer Member gerade snoozed
                                val nowDt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                                val snoozingMember = currentMembers.firstOrNull { m ->
                                    m.id != currentMyMemberId &&
                                    m.snoozeUntil != null &&
                                    m.snoozeUntil!! > nowDt
                                }
                                if (snoozingMember != null) {
                                    val context = getApplication<android.app.Application>()
                                    val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                                    val timeStr = newWakeTime.toJavaLocalTime().format(formatter)
                                    sendSnoozeShiftNotification(context, snoozingMember.name, timeStr)
                                }
                            }
                            lastKnownWakeUpTime = newWakeTime
                        }
                    } else {
                        if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                            android.util.Log.w("FamWake_Alarm", "recalculate: cancelling alarms – alarmsOn=$alarmsOn, hasSchedules=true")
                        }
                        currentMembers.forEach { alarmScheduler.cancelWakeUp(it.id) }
                    }
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

    // Snooze-Guard: Während eines aktiven Snooze keine regulären Alarme planen
    // (der Snooze-Alarm läuft auf separatem PendingIntent-Slot)
    val snoozeUntilLocal = appSettings.snoozeUntil.value
    if (snoozeUntilLocal != null) {
        val snoozeDateTime = snoozeUntilLocal.toJavaLocalDateTime()
        if (snoozeDateTime.isAfter(java.time.LocalDateTime.now())) {
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.d("FamWake_Alarm", "applyAlarms: active snooze until $snoozeUntilLocal, skipping regular alarm")
            }
            return
        }
    }

    var foundMySchedule = false
    for (memberSchedule in schedule.memberSchedules) {
        if (memberSchedule.member.id == currentMyMemberId) {
            foundMySchedule = true
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
    // Eigener Member nicht im Plan (pausiert, deaktiviert, etc.) → alten Alarm canceln,
    // sonst bleibt der Geisterwecker stehen.
    if (!foundMySchedule) {
        alarmScheduler.cancelWakeUp(currentMyMemberId)
        lastScheduledAlarmMillis = null
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

    val resolved = member.copy(
        earliestWakeUp          = profile.earliestWakeUp,
        latestWakeUp            = profile.latestWakeUp,
        bathroomDurationMinutes = profile.bathroomDurationMinutes,
        wantsBreakfast          = profile.wantsBreakfast,
        leaveHomeTime           = profile.leaveHomeTime,
        dayProfiles             = mapOf(targetDow to profile),
        isSimpleMode            = profile.isSimpleMode
    )

    // Snooze-Constraint: Wenn dieser Member aktiv snoozed, fixiere seine Weckzeit
    val snoozeUntil = member.snoozeUntil
    if (snoozeUntil != null) {
        val nowDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        if (snoozeUntil > nowDateTime) {
            val snoozeTime = snoozeUntil.time
            // Beim 1. Snooze: Badzeit um SNOOZE_DURATION reduzieren → absorbiert die Verschiebung,
            // sodass nachfolgende Members NICHT verschoben werden.
            // Beim 2. Snooze: Badzeit bleibt gleich → Scheduler verschiebt nachfolgende Members.
            // Bei kurzer Badzeit (≤ MIN_BATHROOM): auch der 1. Snooze verschiebt.
            val absorbableMinutes = if (member.snoozeCount <= 1) {
                minOf(
                    SnoozeConfig.SNOOZE_DURATION_MINUTES.toLong(),
                    maxOf(0L, resolved.bathroomDurationMinutes - SnoozeConfig.MIN_BATHROOM_MINUTES)
                )
            } else {
                0L // 2. Snooze: keine Absorption → volle Verschiebung nachfolgender Members
            }
            val reducedBathroom = resolved.bathroomDurationMinutes - absorbableMinutes
            return resolved.copy(
                earliestWakeUp = snoozeTime,
                latestWakeUp = snoozeTime,
                bathroomDurationMinutes = reducedBathroom
            )
        }
    }

    return resolved
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
    if (!enabled) {
        lastScheduledAlarmMillis = null
        // ALLE Alarme + Snooze-State komplett aufräumen
        val mid = myMemberId.value
        if (mid != null) {
            alarmScheduler.cancelWakeUp(mid, isSnooze = false)
            alarmScheduler.cancelWakeUp(mid, isSnooze = true)
        }
        appSettings.setSnoozeCount(0)
    }
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

    // Lokalen _members-State SOFORT aktualisieren, damit nachfolgende
    // addOrUpdateMember-Calls den korrekten deviceAlarmEnabled-Wert schreiben.
    // Ohne dieses Update wird der stale Wert (null/true) nach Firestore geschrieben
    // und überschreibt das Partial-Update von updateDeviceAlarmEnabled.
    if (currentMemberId != null) {
        val currentList = _members.value.toMutableList()
        val idx = currentList.indexOfFirst { it.id == currentMemberId }
        if (idx != -1) {
            currentList[idx] = currentList[idx].copy(deviceAlarmEnabled = enabled)
            _members.value = currentList.toPersistentList()
        }
    }
    recalculateSchedule()

    if (currentFamilyId != null && currentMemberId != null) {
        // pushMeta SOFORT schreiben (vor dem 2s-Debounce), damit die CF
        // den Sender beim ersten Member-Write bereits kennt.
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            scope.launch {
                repository.setUserActionMeta(currentUid, currentFamilyId)
            }
        }
        alarmToggleJob?.cancel()
        alarmToggleJob = scope.launch {
            try {
                kotlinx.coroutines.delay(2000)
                // NonCancellable: delay ist unterbrechbar (Entprellung), Write nicht
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    kotlinx.coroutines.withTimeout(10_000) {
                        repository.updateDeviceAlarmEnabled(currentFamilyId, currentMemberId, enabled)
                    }
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
}

/**
 * ADMIN/DEBUG: Setzt das DayProfile des heutigen Wochentags so,
 * dass der Wecker in ~2 Minuten klingelt.
 */
fun FamilyViewModel.setDebugAlarmIn5Minutes() {
    if (!de.familienwecker.famwake.BuildConfig.DEBUG) return
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
    val currentCount = appSettings.snoozeCount.value
    if (currentCount >= SnoozeConfig.MAX_SNOOZE_COUNT) {
        // Max Snooze erreicht – nicht snoozen
        if (de.familienwecker.famwake.BuildConfig.DEBUG) {
            android.util.Log.w("FamWake_Alarm", "snooze: MAX_SNOOZE_COUNT reached ($currentCount), blocking")
        }
        return
    }

    val newCount = currentCount + 1
    val snoozeInstant = Clock.System.now().plus(SnoozeConfig.SNOOZE_DURATION_MINUTES, DateTimeUnit.MINUTE)
    val snoozeTime = snoozeInstant.toLocalDateTime(TimeZone.currentSystemDefault())

    // Lokal speichern
    appSettings.setSnoozeUntil(snoozeTime)
    appSettings.setSnoozeCount(newCount)

    // Lokalen _members-State SOFORT aktualisieren, damit resolveEffectiveMember
    // die snooze-fixierte Weckzeit berechnet und der Scheduler nachfolgende
    // Members korrekt verschiebt (ohne auf den Firestore-Roundtrip zu warten).
    val currentList = _members.value.toMutableList()
    val idx = currentList.indexOfFirst { it.id == memberId }
    if (idx != -1) {
        currentList[idx] = currentList[idx].copy(
            snoozeUntil = snoozeTime,
            snoozeCount = newCount
        )
        _members.value = currentList.toPersistentList()
    }
    recalculateSchedule()

    // Alarm planen
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

    // Firestore: Snooze-State synchronisieren (über native Firebase SDK, nicht GitLive)
    val currentFamilyId = familyId.value
    if (currentFamilyId != null) {
        scope.launch {
            try {
                // pushMeta setzen, damit CF den Sender erkennt und keine Self-Push schickt
                val currentUid = auth.currentUser?.uid
                if (currentUid != null) {
                    repository.setUserActionMeta(currentUid, currentFamilyId)
                }
                repository.updateMemberSnoozeState(currentFamilyId, memberId, snoozeTime, newCount)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.w("FamWake_Alarm", "snooze: Firestore write failed: ${e.message}")
                }
                // Nicht blockierend – lokaler Alarm funktioniert trotzdem
            }
        }
    }

    TelemetryDeck.signal("alarm.snoozed", mapOf("snoozeCount" to newCount.toString()))
}

fun FamilyViewModel.cancelSnooze(memberId: String) {
    appSettings.setSnoozeUntil(null)
    appSettings.setSnoozeCount(0)
    alarmScheduler.cancelWakeUp(memberId, isSnooze = true)
    lastScheduledAlarmMillis = null

    // Lokalen Member-State sofort aktualisieren
    val currentList = _members.value.toMutableList()
    val idx = currentList.indexOfFirst { it.id == memberId }
    if (idx != -1) {
        currentList[idx] = currentList[idx].copy(
            snoozeUntil = null,
            snoozeCount = 0
        )
        _members.value = currentList.toPersistentList()
    }

    // Firestore: Snooze-State löschen (über native Firebase SDK)
    val currentFamilyId = familyId.value
    if (currentFamilyId != null) {
        scope.launch {
            try {
                // pushMeta setzen, damit CF den Sender erkennt und keine Self-Push schickt
                val currentUid = auth.currentUser?.uid
                if (currentUid != null) {
                    repository.setUserActionMeta(currentUid, currentFamilyId)
                }
                repository.updateMemberSnoozeState(currentFamilyId, memberId, null, 0)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.w("FamWake_Alarm", "cancelSnooze: Firestore clear failed: ${e.message}")
                }
            }
        }
    }

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
    is ScheduleMessage.BufferReduced            -> UiText.StringResource(R.string.schedule_message_buffer_reduced, msg.originalMinutes, msg.reducedMinutes)
    // M11: Warnung bei Überschreitung des Member-Limits
    is ScheduleMessage.MemberLimitExceeded      -> UiText.StringResource(R.string.schedule_message_member_limit, msg.total, msg.limit)
    is ScheduleMessage.NoActiveSchedule         -> UiText.StringResource(R.string.main_no_active_schedule)
}

/** Sendet eine lokale Notification wenn die Weckzeit durch den Snooze eines anderen Members verschoben wurde. */
private fun sendSnoozeShiftNotification(context: android.content.Context, memberName: String, newTimeStr: String) {
    val notification = androidx.core.app.NotificationCompat.Builder(context, NotificationChannels.SCHEDULE_CHANGE)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(context.getString(R.string.notif_snooze_shift_title))
        .setContentText(context.getString(R.string.notif_snooze_shift_body, memberName, newTimeStr))
        .setAutoCancel(true)
        .build()

    try {
        val manager = androidx.core.app.NotificationManagerCompat.from(context)
        // Feste ID: überschreibt vorherige Shift-Notifications (kein Stacking)
        manager.notify(1002, notification)
    } catch (_: SecurityException) {
        // POST_NOTIFICATIONS nicht granted – stille Fehlschlag
    }
}
