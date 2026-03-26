package de.familienwecker.famwake.ui.viewmodel

import de.familienwecker.famwake.R
import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.model.toJavaLocalTime
import de.familienwecker.famwake.ui.util.UiText
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    scope.launch {
        try {
            repository.addOrUpdateMember(currentFamilyId, member)
        } catch (e: Exception) {
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.e("FamilyViewModel", "Fehler beim Speichern von Member ${member.id}: ${e.message}")
            }
            _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, e.localizedMessage ?: getApplication<android.app.Application>().getString(R.string.add_member_unknown))
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
            repository.addOrUpdateMember(currentFamilyId, member)
            onComplete?.invoke()
        } finally {
            memberDebounceJobs.remove(member.id)
        }
    }
}

fun FamilyViewModel.removeMember(id: String) {
    checkOfflineAndHint()
    val currentFamilyId = familyId.value ?: return
    alarmScheduler.cancelWakeUp(id)
    scope.launch {
        val result = repository.removeMember(currentFamilyId, id)
        if (result.isSuccess) {
            memberRepository.deleteMember(id)
        } else {
            _errorMessage.value = UiText.StringResource(R.string.error_delete_member, result.exceptionOrNull()?.localizedMessage ?: getApplication<android.app.Application>().getString(R.string.add_member_unknown))
        }
    }
    if (myMemberId.value == id) {
        setMyMemberId(null)
    }
}

fun FamilyViewModel.setMyMemberId(id: String?, onComplete: (Boolean) -> Unit = {}) {
    val currentFamilyId = familyId.value ?: return
    val currentMyMemberId = myMemberId.value
    val userId = auth.currentUser?.uid ?: return
    val userName = auth.currentUser?.displayName
        ?: getApplication<android.app.Application>().getString(R.string.settings_fallback_username)

    if (_isOffline.value) {
        onComplete(false)
        return
    }

    scope.launch {
        if (currentMyMemberId != null && currentMyMemberId != id) {
            repository.unclaimMember(currentFamilyId, currentMyMemberId, userId)
        }
        if (id != null) {
            val success = repository.claimMember(currentFamilyId, id, userId, userName)
            if (success) {
                appSettings.setMyMemberId(id)
                val memberName = _members.value.find { it.id == id }?.name
                appSettings.setMyMemberName(memberName)
                appSettings.setAlarmEnabled(true)
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
    }
}

fun FamilyViewModel.togglePauseMember(memberId: String) {
    val member = _members.value.find { it.id == memberId } ?: return
    if (member.claimedByUserId != null && member.id != myMemberId.value) return
    val updatedMember = member.copy(isPaused = !member.isPaused)
    _pendingPauseIds.value = _pendingPauseIds.value + memberId
    scope.launch {
        memberRepository.upsertMember(updatedMember)
        _pendingPauseIds.value = _pendingPauseIds.value - memberId
        addOrUpdateMemberDebounced(updatedMember)
    }
}

fun FamilyViewModel.toggleAwakeMember(memberId: String) {
    if (memberId != myMemberId.value) return
    val member = _members.value.find { it.id == memberId } ?: return
    val newAwakeState = !isAwakeTodayLocal.value

    appSettings.setAwakeToday(newAwakeState)
    val updatedMember = member.copy(isAwakeToday = newAwakeState)
    scope.launch {
        try {
            memberRepository.upsertMember(updatedMember)
        } catch (e: Exception) {
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.w("FamilyViewModel", "toggleAwakeMember: Room write failed: ${e.message}")
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

fun FamilyViewModel.moveMemberOrder(fromIndex: Int, toIndex: Int) {
    val currentMembers = _members.value.toMutableList()
    if (fromIndex !in currentMembers.indices || toIndex !in currentMembers.indices) return
    val member = currentMembers.removeAt(fromIndex)
    currentMembers.add(toIndex, member)
    val updatedMembers = currentMembers.mapIndexed { index, m -> m.copy(sequenceOrder = index) }
    _members.value = updatedMembers.toPersistentList()
    recalculateSchedule()
}

fun FamilyViewModel.saveMemberOrder() {
    checkOfflineAndHint()
    val currentFamilyId = familyId.value ?: return
    val orderMap = _members.value.associate { it.id to it.sequenceOrder }
    scope.launch {
        repository.updateMemberOrders(currentFamilyId, orderMap)
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
                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.e("FamilyViewModel", "Failed to reset member status batch: ${e.message}")
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
