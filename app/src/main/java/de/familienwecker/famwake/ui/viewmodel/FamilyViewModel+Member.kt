package de.familienwecker.famwake.ui.viewmodel

import android.util.Log
import com.telemetrydeck.sdk.TelemetryDeck
import de.familienwecker.famwake.BuildConfig
import de.familienwecker.famwake.R
import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.model.toJavaLocalTime
import de.familienwecker.famwake.ui.util.UiText
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

// ─── Member-Logik ─────────────────────────────────────────────────────────────

fun FamilyViewModel.addOrUpdateMember(member: FamilyMember) {
    checkOfflineAndHint()
    val currentFamilyId = familyId.value ?: return
    if (member.name.isBlank()) {
        if (de.familienwecker.famwake.BuildConfig.DEBUG) {
            android.util.Log.e("FamilyViewModel", "Abbruch: Member Name ist leer")
        }
        return
    }
    val isNewMember = _members.value.none { it.id == member.id }

    // Flag SYNCHRON setzen, bevor der Coroutine startet – verhindert den "Kein Profil"-Flash,
    // da Firestore-Listener schneller feuern können als der Coroutine-Scheduler.
    val willAutoClaim = isNewMember && myMemberId.value == null && member.claimedByUserId == null && auth.currentUser?.uid != null
    if (willAutoClaim) _isAutoClaimInProgress.value = true

    scope.launch {
        try {
            val finalMember = if (willAutoClaim) {
                val userId = auth.currentUser?.uid
                val userName = auth.currentUser?.displayName
                    ?: app.getString(de.familienwecker.famwake.R.string.settings_fallback_username)
                if (userId != null) {
                    member.copy(
                        claimedByUserId = userId,
                        claimedByUserName = userName,
                        claimedByDeviceId = appSettings.deviceId
                    )
                } else member
            } else member

            val currentUid = auth.currentUser?.uid
            if (currentUid != null) {
                repository.setUserActionMeta(currentUid, currentFamilyId)
            }
            repository.addOrUpdateMember(currentFamilyId, finalMember)
            
            if (willAutoClaim && finalMember.claimedByUserId != null) {
                appSettings.setMyMemberId(finalMember.id)
                appSettings.setMyMemberName(finalMember.name)
                appSettings.setAlarmEnabled(true)
                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.i("FamilyViewModel", "Auto-Claim: ${finalMember.name} (${finalMember.id}) automatisch geclaimt")
                }
                _isAutoClaimInProgress.value = false
            } else if (isNewMember) {
                TelemetryDeck.signal("member.created")
            } else {
                // Removed member.updated noise
            }
        } catch (e: Exception) {
            _isAutoClaimInProgress.value = false
            if (BuildConfig.DEBUG) {
                Log.e("FamilyViewModel", "Fehler beim Speichern von Member ${member.id}: ${e.message}")
            }
            _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, e.localizedMessage ?: app.getString(R.string.add_member_unknown))
        }
    }
}

/**
 * Schreibt ein Mitglied mit 2s Debounce nach Firebase.
 * Jedes Mitglied hat einen eigenen Debounce-Job – kein gegenseitiges Cancel (#4).
 */
internal fun FamilyViewModel.addOrUpdateMemberDebounced(member: FamilyMember, onComplete: (() -> Unit)? = null) {
    val currentFamilyId = familyId.value ?: return
    memberDebounceJobs[member.id]?.cancel()
    memberDebounceJobs[member.id] = scope.launch {
        delay(2000)
        try {
            val currentUid = auth.currentUser?.uid
            if (currentUid != null) {
                repository.setUserActionMeta(currentUid, currentFamilyId)
            }
            repository.addOrUpdateMember(currentFamilyId, member)
            onComplete?.invoke()
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Debounce-Cancel: neuer Aufruf hat diesen Job abgelöst – kein Fehler
            throw e
        } catch (e: Exception) {
            // Debounced write failed – surface error so the user knows the change wasn't saved
            if (BuildConfig.DEBUG) {
                Log.e("FamilyViewModel", "addOrUpdateMemberDebounced failed for ${member.id}: ${e.message}")
            }
            _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, e.localizedMessage ?: app.getString(R.string.add_member_unknown))
        } finally {
            memberDebounceJobs.remove(member.id)
        }
    }
}

fun FamilyViewModel.removeMember(id: String) {
    checkOfflineAndHint()
    val currentFamilyId = familyId.value ?: return
    alarmScheduler.cancelWakeUp(id)
    TelemetryDeck.signal("member.deleted")
    val wasMyMember = myMemberId.value == id
    scope.launch {
        val result = repository.removeMember(currentFamilyId, id)
        if (result.isSuccess) {
            memberRepository.deleteMember(id)
            // Eigenes Profil erst NACH erfolgreicher Firestore-Löschung zurücksetzen,
            // damit der lokale State bei einem Netzwerkfehler nicht inkonsistent wird.
            if (wasMyMember) {
                setMyMemberId(null)
            }
        } else {
            _errorMessage.value = UiText.StringResource(R.string.error_delete_member, result.exceptionOrNull()?.localizedMessage ?: app.getString(R.string.add_member_unknown))
        }
    }
}

fun FamilyViewModel.setMyMemberId(id: String?, force: Boolean = false, onComplete: (Boolean) -> Unit = {}) {
    val currentFamilyId = familyId.value ?: return
    val currentMyMemberId = myMemberId.value
    val userId = auth.currentUser?.uid ?: return
    val userName = auth.currentUser?.displayName
        ?: app.getString(R.string.settings_fallback_username)

    if (_isOffline.value) {
        onComplete(false)
        return
    }

    scope.launch {
        _isAutoClaimInProgress.value = true
        try {
            if (currentMyMemberId != null && currentMyMemberId != id) {
                repository.unclaimMember(currentFamilyId, currentMyMemberId, userId, appSettings.deviceId)
            }
            if (id != null) {
                val success = repository.claimMember(currentFamilyId, id, userId, userName, appSettings.deviceId, force)
                if (success) {
                    // Sofortiges lokales Update in Room, um Race-Conditions mit langsamen Snapshots zu verhindern.
                    val currentMember = _members.value.find { it.id == id }
                    if (currentMember != null) {
                        val updatedMember = currentMember.copy(
                            claimedByDeviceId = appSettings.deviceId,
                            claimedByUserId = userId,
                            claimedByUserName = userName,
                            deviceAlarmEnabled = true
                        )
                        memberRepository.upsertMember(updatedMember)
                    }

                    appSettings.setMyMemberId(id)
                    val memberName = currentMember?.name ?: _members.value.find { it.id == id }?.name
                    appSettings.setMyMemberName(memberName)
                    appSettings.setAlarmEnabled(true)
                    TelemetryDeck.signal("member.claimed")
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } else {
                appSettings.setMyMemberId(null)
                appSettings.setMyMemberName(null)
                appSettings.setAlarmEnabled(false)
                onComplete(true)
            }
        } finally {
            // Ein minimaler Delay, falls Flow-Emissions sich überschneiden.
            kotlinx.coroutines.delay(500)
            _isAutoClaimInProgress.value = false
        }
    }
}

fun FamilyViewModel.togglePauseMember(memberId: String) {
    val member = _members.value.find { it.id == memberId } ?: return
    // Nur unclaimed Member dürfen pausiert werden.
    // Geclaimte Member (auch das eigene Profil) sind nicht pausierbar.
    if (member.claimedByUserId != null) return
    val currentFamilyId = familyId.value ?: return
    val newPausedState = !member.isPaused
    val updatedMember = member.copy(isPaused = newPausedState)
    _pendingPauseIds.value = _pendingPauseIds.value + memberId
    // Tracking removed für Paused/Unpaused
    scope.launch {
        try {
            // Gezieltes Update nur für isPaused – kein volles .set() das name enthält
            // und die Security Rule für nicht-Admin-Nutzer verletzt.
            memberRepository.upsertMember(updatedMember)
            val currentUid = auth.currentUser?.uid
            if (currentUid != null) {
                repository.setUserActionMeta(currentUid, currentFamilyId)
            }
            repository.updateMemberPauseState(currentFamilyId, memberId, newPausedState)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("FamilyViewModel", "togglePauseMember failed for $memberId: ${e.message}")
            }
            _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, e.localizedMessage ?: app.getString(R.string.add_member_unknown))
        } finally {
            _pendingPauseIds.value = _pendingPauseIds.value - memberId
        }
    }
}

fun FamilyViewModel.toggleAwakeMember(memberId: String) {
    if (memberId != myMemberId.value) return
    val member = _members.value.find { it.id == memberId } ?: return
    // O3: Offline-Hinweis – lokale Änderung wird gespeichert, nach Reconnect sync'd
    checkOfflineAndHint()
    val newAwakeState = !isAwakeTodayLocal.value

    appSettings.setAwakeToday(newAwakeState)
    TelemetryDeck.signal(if (newAwakeState) "awake.markedAwake" else "awake.reset")
    val updatedMember = member.copy(isAwakeToday = newAwakeState)
    scope.launch {
        try {
            memberRepository.upsertMember(updatedMember)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w("FamilyViewModel", "toggleAwakeMember: Room write failed: ${e.message}")
            }
        }
    }
    addOrUpdateMemberDebounced(updatedMember)

    if (newAwakeState) {
        cancelAlarmForCurrentUser()
        lastScheduledAlarmMillis = null
    } else {
        recalculateSchedule()
    }
}

fun FamilyViewModel.moveMemberOrder(fromIndex: Int, toIndex: Int, wholeWeek: Boolean = false) {
    val sched = _schedule.value ?: return
    val targetDate = sched.targetDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault())
    val dayOfWeek = selectedDayOfWeek.value ?: targetDate.dayOfWeek.value
    
    val visibleIds = sched.memberSchedules.map { it.member.id }
    if (fromIndex !in visibleIds.indices || toIndex !in visibleIds.indices) return
    val targetIds = visibleIds.toMutableList()
    val movedId = targetIds.removeAt(fromIndex)
    targetIds.add(toIndex, movedId)
    
    val updatedMembers = _members.value.map { m ->
        val indexInTarget = targetIds.indexOf(m.id)
        if (indexInTarget != -1) {
            if (wholeWeek) {
                val currentProfiles = m.dayProfiles ?: emptyMap()
                val updatedProfiles = currentProfiles.mapValues { (_, profile) ->
                    profile.copy(sequenceOrder = null)
                }
                m.copy(
                    sequenceOrder = indexInTarget,
                    dayProfiles = updatedProfiles
                )
            } else {
                val currentProfiles = m.dayProfiles ?: emptyMap()
                val profile = currentProfiles[dayOfWeek] ?: de.familienwecker.famwake.model.DayProfile(
                    isActive = !m.isPaused,
                    earliestWakeUp = m.earliestWakeUp,
                    latestWakeUp = m.latestWakeUp,
                    bathroomDurationMinutes = m.bathroomDurationMinutes,
                    wantsBreakfast = m.wantsBreakfast,
                    leaveHomeTime = m.leaveHomeTime,
                    isSimpleMode = m.isSimpleMode
                )
                val updatedProfile = profile.copy(sequenceOrder = indexInTarget)
                m.copy(dayProfiles = currentProfiles + (dayOfWeek to updatedProfile))
            }
        } else {
            m
        }
    }
    _members.value = updatedMembers.toPersistentList()
    recalculateSchedule()
}

fun FamilyViewModel.saveMemberOrder() {
    checkOfflineAndHint()
    val currentFamilyId = familyId.value ?: return
    val sched = _schedule.value ?: return
    val targetDate = sched.targetDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault())
    val dayOfWeek = selectedDayOfWeek.value ?: targetDate.dayOfWeek.value
    
    val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    
    scope.launch {
        try {
            if (currentUid != null) {
                repository.setReorderMeta(currentUid, currentFamilyId)
                repository.setUserActionMeta(currentUid, currentFamilyId)
            }
            _members.value.forEach { m ->
                memberRepository.upsertMember(m)
                repository.updateMemberDayProfiles(currentFamilyId, m.id, m.dayProfiles)
            }
            val orderMap = _members.value.associate { it.id to it.sequenceOrder }
            repository.updateMemberOrders(currentFamilyId, orderMap)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("FamilyViewModel", "saveMemberOrder failed: ${e.message}")
            }
            _errorMessage.value = UiText.StringResource(R.string.error_sync_failed,
                e.localizedMessage ?: app.getString(R.string.add_member_unknown))
        }
    }
}

/**
 * Prüft, ob das tägliche Reset-Datum für jeden Member abgelaufen ist und setzt
 * isAwakeToday / isPaused zurück. Schreibt Änderungen als Batch nach Firestore.
 */
internal fun FamilyViewModel.checkAndResetMembers(members: List<FamilyMember>): List<FamilyMember> {
    val today = java.time.LocalDate.now().toString()
    val now = java.time.LocalTime.now()
    val toUpdate = mutableListOf<FamilyMember>()

    val result = members.map { member ->
        val resetThreshold = member.latestWakeUp.toJavaLocalTime().plusHours(2)
        val isPastResetThreshold = now.isAfter(resetThreshold)

        if (isPastResetThreshold && member.lastResetDate != today) {
            val isUnclaimed = member.claimedByUserId == null
            val newIsPaused = if (isUnclaimed) false else member.isPaused
            val updated = member.copy(isPaused = newIsPaused, isAwakeToday = false, lastResetDate = today)
            if (member.id == myMemberId.value) {
                appSettings.setAwakeToday(false)
            }
            toUpdate.add(updated)
            updated
        } else {
            member
        }
    }

    val familyIdVal = familyId.value
    if (familyIdVal != null && toUpdate.isNotEmpty()) {
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                repository.updateMembersBatch(familyIdVal, toUpdate)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e("FamilyViewModel", "Failed to reset member status batch: ${e.message}")
                }
            }
        }
    }
    return result
}

/**
 * Admin-Hilfsfunktion: Löscht ALLE Member, legt "Daniel" an, claimt ihn und setzt 2-Min-Weckzeit.
 */
fun FamilyViewModel.setupTestAlarmAndMembers(onStatus: (String) -> Unit) {
    val currentFamilyId = familyId.value ?: run { onStatus("Fehler: keine FamilyId"); return }
    val userId = auth.currentUser?.uid ?: run { onStatus("Fehler: nicht eingeloggt"); return }
    val userName = auth.currentUser?.displayName ?: "Test User"

    scope.launch {
        val members = _members.value
        members.forEach { m ->
            alarmScheduler.cancelWakeUp(m.id)
            repository.removeMember(currentFamilyId, m.id)
            memberRepository.deleteMember(m.id)
        }

        val newId = java.util.UUID.randomUUID().toString()
        val newMember = FamilyMember(
            id = newId,
            name = userName,
            earliestWakeUp = kotlinx.datetime.LocalTime(6, 0),
            latestWakeUp = kotlinx.datetime.LocalTime(7, 0),
            bathroomDurationMinutes = 10L,
            wantsBreakfast = false,
            isPaused = false,
            claimedByUserId = userId,
            claimedByUserName = userName,
            claimedByDeviceId = appSettings.deviceId,
            sequenceOrder = 0,
            createdAt = System.currentTimeMillis()
        )
        repository.addOrUpdateMember(currentFamilyId, newMember)
        appSettings.setMyMemberId(newId)
        appSettings.setMyMemberName(userName)

        delay(500)
        setDebugAlarmIn5Minutes()
        onStatus("Reset & Test User angelegt")
    }
}

fun FamilyViewModel.triggerMemberReset() {
    val currentMembers = _members.value
    if (currentMembers.isNotEmpty()) {
        val checkedMembers = checkAndResetMembers(currentMembers)
        if (checkedMembers != currentMembers) {
            _members.value = checkedMembers.toPersistentList()
            recalculateSchedule()
        }
    }
}
