package de.familienwecker.famwake.ui.viewmodel

import com.telemetrydeck.sdk.TelemetryDeck
import de.familienwecker.famwake.R
import de.familienwecker.famwake.data.FamilyNotFoundException
import de.familienwecker.famwake.data.CodeGenerationFailedException
import de.familienwecker.famwake.ui.util.UiText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// ─── Family-Logik ─────────────────────────────────────────────────────────────

fun FamilyViewModel.createFamily(familyName: String, onComplete: (Boolean) -> Unit) {
    _errorMessage.value = null
    val uid = auth.currentUser?.uid
    if (uid == null) {
        _errorMessage.value = UiText.StringResource(R.string.error_not_logged_in)
        onComplete(false)
        return
    }
    if (familyName.isBlank()) {
        _errorMessage.value = UiText.StringResource(R.string.error_create_family, "")
        onComplete(false)
        return
    }
    scope.launch {
        if (isOffline.value) {
            // Netzwerk kann beim Cold Start kurz als offline erscheinen (~1-3s Delay bis
            // NET_CAPABILITY_VALIDATED gesetzt ist). Einmalig warten, dann definitiv prüfen.
            delay(2000)
            if (isOffline.value) {
                _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, getApplication<android.app.Application>().getString(R.string.error_offline))
                onComplete(false)
                return@launch
            }
        }
        if (isSyncBlocked.value) {
            _errorMessage.value = UiText.StringResource(R.string.error_sync_blocked_device)
            onComplete(false)
            return@launch
        }
        val result = repository.createFamily(familyName, uid)
        result.onSuccess { pair ->
            appSettings.setFamilyId(pair.first)
            appSettings.setJoinCode(pair.second)
            appSettings.setFamilyName(familyName)
            // Etwaige Race-Condition-Fehlermeldung aus dem Snapshot-Listener clearen.
            // Der familyId-Change startet sofort einen Listener, der u.U. scheitert weil
            // die Security Rules den frischen users/{uid}.familyId noch nicht sehen.
            _errorMessage.value = null
            // Tracking: Nutzer hat erfolgreich eine neue Familie angelegt
            TelemetryDeck.signal("family.created")
            onComplete(true)
        }.onFailure { error ->
            when {
                error is CodeGenerationFailedException ->
                    _errorMessage.value = UiText.StringResource(R.string.error_code_generation_failed)
                error.message?.contains("TOO_MANY_REQUESTS", ignoreCase = true) == true ->
                    _errorMessage.value = UiText.StringResource(R.string.error_create_family_rate_limit)
                else ->
                    _errorMessage.value = UiText.StringResource(R.string.error_create_family)
            }
            onComplete(false)
        }
    }
}

fun FamilyViewModel.joinFamily(code: String, onComplete: (Boolean) -> Unit) {
    _errorMessage.value = null
    if (code.equals(joinCode.value, ignoreCase = true)) { onComplete(true); return }
    if (code.length != 6) {
        _errorMessage.value = UiText.StringResource(R.string.error_invalid_code, code)
        onComplete(false)
        return
    }
    scope.launch {
        _isJoiningFamily.value = true
        if (isOffline.value) {
            _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, getApplication<android.app.Application>().getString(R.string.error_offline))
            _isJoiningFamily.value = false
            onComplete(false)
            return@launch
        }
        if (isSyncBlocked.value) {
            _errorMessage.value = UiText.StringResource(R.string.error_sync_blocked_device)
            _isJoiningFamily.value = false
            onComplete(false)
            return@launch
        }
        val result = repository.joinFamilyByCode(code)
        result.onSuccess { pair ->
            val fetchedName = repository.getFamilyName(pair.first)
            appSettings.setFamilyId(pair.first)
            appSettings.setJoinCode(pair.second)
            appSettings.setFamilyName(fetchedName)
            if (_pendingJoinCode.value == code) _pendingJoinCode.value = null
            _isJoiningFamily.value = false
            // Tracking: Nutzer ist erfolgreich einer bestehenden Familie beigetreten
            TelemetryDeck.signal("family.joined")
            onComplete(true)
        }.onFailure { error ->
            when {
                error is FamilyNotFoundException ->
                    _errorMessage.value = UiText.StringResource(R.string.error_family_not_found)
                error.message?.contains("TOO_MANY_REQUESTS", ignoreCase = true) == true ->
                    _errorMessage.value = UiText.StringResource(R.string.error_join_family_rate_limit)
                else ->
                    _errorMessage.value = UiText.StringResource(R.string.error_invalid_code)
            }
            _pendingJoinCode.value = null
            _isJoiningFamily.value = false
            onComplete(false)
        }
    }
}

fun FamilyViewModel.setPendingJoinCode(code: String?) {
    if (code != null && code.equals(joinCode.value, ignoreCase = true)) {
        _pendingJoinCode.value = null
        return
    }
    _pendingJoinCode.value = code
}

fun FamilyViewModel.clearPendingJoinCode() {
    _pendingJoinCode.value = null
    _errorMessage.value = null
}

fun FamilyViewModel.handlePendingJoin(onComplete: (Boolean) -> Unit) {
    val code = _pendingJoinCode.value ?: return
    if (code.equals(joinCode.value, ignoreCase = true)) {
        _pendingJoinCode.value = null
        onComplete(true)
        return
    }
    joinFamily(code) { success ->
        _pendingJoinCode.value = null
        onComplete(success)
    }
}

fun FamilyViewModel.leaveAndJoinPendingCode(onComplete: (Boolean) -> Unit) {
    val code = _pendingJoinCode.value ?: return
    if (code.equals(joinCode.value, ignoreCase = true)) {
        _pendingJoinCode.value = null
        onComplete(false)
        return
    }
    scope.launch {
        _isSyncing.value = true
        _isJoiningFamily.value = true
        try {
            if (isOffline.value) {
                _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, getApplication<android.app.Application>().getString(R.string.error_offline))
                _pendingJoinCode.value = null
                onComplete(false)
                return@launch
            }
            // Snapshot-Listener VOR dem Join stoppen – joinFamilyByCode ändert
            // users/{uid}.familyId serverseitig auf die neue Familie, wodurch die
            // Security Rules den alten Listener mit PERMISSION_DENIED abweisen.
            stopSyncJobs()
            val result = repository.joinFamilyByCode(code)
            result.onSuccess { pair ->
                cancelAlarmForCurrentUser()
                val fetchedName = repository.getFamilyName(pair.first)
                _pendingJoinCode.value = null
                _errorMessage.value = null
                appSettings.setFamilyId(pair.first)
                appSettings.setJoinCode(pair.second)
                appSettings.setFamilyName(fetchedName)
                appSettings.setMyMemberId(null)
                appSettings.setMyMemberName(null)
                // Alarm deaktivieren: kein Member mehr geclaimt – Switch würde sonst ON zeigen
                // obwohl kein Alarm gesetzt werden kann (myMemberId == null).
                appSettings.setAlarmEnabled(false)
                onComplete(true)
            }.onFailure { error ->
                when {
                    error is FamilyNotFoundException ->
                        _errorMessage.value = UiText.StringResource(R.string.error_family_not_found)
                    error.message?.contains("TOO_MANY_REQUESTS", ignoreCase = true) == true ->
                        _errorMessage.value = UiText.StringResource(R.string.error_join_family_rate_limit)
                    else ->
                        _errorMessage.value = UiText.StringResource(R.string.error_invalid_code, error.localizedMessage ?: getApplication<android.app.Application>().getString(R.string.add_member_unknown))
                }
                _pendingJoinCode.value = null
                onComplete(false)
            }
        } catch (e: Exception) {
            _errorMessage.value = UiText.StringResource(R.string.error_system, e.localizedMessage ?: getApplication<android.app.Application>().getString(R.string.add_member_unknown))
            _pendingJoinCode.value = null
            onComplete(false)
        } finally {
            _isSyncing.value = false
            // Ensure loader is always reset – even on unexpected exceptions
            _isJoiningFamily.value = false
        }
    }
}

fun FamilyViewModel.leaveFamily(onComplete: (Boolean) -> Unit = {}) {
    checkOfflineAndHint()
    _errorMessage.value = null
    val uid = auth.currentUser?.uid ?: return
    val currentFamilyId = familyId.value
    val currentMemberId = myMemberId.value
    cancelAlarmForCurrentUser()
    scope.launch {
        _isSyncing.value = true
        // Leaving requires a Cloud Function call – not possible while offline
        if (isOffline.value) {
            _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, getApplication<android.app.Application>().getString(R.string.error_offline))
            _isSyncing.value = false
            onComplete(false)
            return@launch
        }
        try {
            // Snapshot-Listener VOR dem Leave stoppen – die Cloud Function entfernt
            // den User serverseitig, was PERMISSION_DENIED im Listener auslöst.
            stopSyncJobs()
            val result = if (currentFamilyId != null && currentMemberId != null) {
                repository.leaveFamilyBatch(uid, currentFamilyId, currentMemberId)
            } else {
                Result.success(Unit)
            }
            if (result.isSuccess) {
                appSettings.setFamilyId(null)
                appSettings.setJoinCode(null)
                appSettings.setFamilyName(null)
                appSettings.setMyMemberId(null)
                appSettings.setMyMemberName(null)
                // Etwaige Race-Condition-Fehlermeldung clearen
                _errorMessage.value = null
                TelemetryDeck.signal("family.left")
                onComplete(true)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: getApplication<android.app.Application>().getString(R.string.error_leave_failed)
                _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, errorMsg)
                onComplete(false)
            }
        } catch (e: Exception) {
            _errorMessage.value = UiText.StringResource(R.string.error_system, e.localizedMessage ?: getApplication<android.app.Application>().getString(R.string.add_member_unknown))
            onComplete(false)
        } finally {
            _isSyncing.value = false
        }
    }
}

fun FamilyViewModel.deleteFamily(onComplete: (Boolean) -> Unit) {
    _errorMessage.value = null
    val currentFamilyId = familyId.value ?: return
    scope.launch {
        if (isOffline.value) {
            _errorMessage.value = UiText.StringResource(R.string.error_sync_failed,
                getApplication<android.app.Application>().getString(R.string.error_offline))
            onComplete(false)
            return@launch
        }
        val uid = auth.currentUser?.uid ?: return@launch
        // Snapshot-Listener VOR dem Delete stoppen – sonst crashen sie mit PERMISSION_DENIED
        // sobald die Cloud Function die Docs löscht und die Error-Message wird gesetzt
        // bevor familyId=null den Job cancelt (Race Condition).
        stopSyncJobs()
        val result = repository.deleteFamily(currentFamilyId, uid)
        if (result.isSuccess) {
            appSettings.setFamilyId(null)
            appSettings.setJoinCode(null)
            appSettings.setFamilyName(null)
            appSettings.setMyMemberId(null)
            appSettings.setMyMemberName(null)
            // Etwaige Race-Condition-Fehlermeldung aus dem Listener clearen
            _errorMessage.value = null
            TelemetryDeck.signal("family.deleted")
            onComplete(true)
        } else {
            _errorMessage.value = UiText.StringResource(R.string.error_delete_family, result.exceptionOrNull()?.localizedMessage ?: getApplication<android.app.Application>().getString(R.string.add_member_unknown))
            onComplete(false)
        }
    }
}

fun FamilyViewModel.refreshData() {
    val uid = auth.currentUser?.uid ?: return
    _isSyncing.value = true
    scope.launch {
        if (isOffline.value) {
            _isSyncing.value = false
            return@launch
        }
        try {
            val result = withTimeoutOrNull(3000) {
                repository.getUserFamily(uid, cachedJoinCode = appSettings.joinCode.value)
            }
            if (result == null) {
                _isSyncing.value = false
                return@launch
            }
            result.onSuccess { pair ->
                if (pair != null) {
                    appSettings.setFamilyId(pair.first)
                    appSettings.setJoinCode(pair.second)
                    val fetchedFamilyName = repository.getFamilyName(pair.first)
                    appSettings.setFamilyName(fetchedFamilyName)
                    val claimedMember = repository.getClaimedMember(pair.first, uid)
                    if (claimedMember != null) {
                        appSettings.setMyMemberId(claimedMember.id)
                        appSettings.setMyMemberName(claimedMember.name)
                    }
                } else {
                    if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                        android.util.Log.w("FamilyViewModel", "refreshData: getUserFamily returned null, keeping local state")
                    }
                }
            }
        } catch (e: Exception) {
            // Für neue Accounts ohne users/{uid}-Dokument ist ein Fehler hier kein echter Fehler.
            // Fehlermeldung nur anzeigen wenn User tatsächlich eine Familie hatte.
            if (appSettings.familyId.value != null) {
                _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, e.localizedMessage ?: getApplication<android.app.Application>().getString(R.string.add_member_unknown))
            }
        } finally {
            _isSyncing.value = false
        }
    }
}

fun FamilyViewModel.triggerRefresh() {
    // StateFlow mit datumsgebundenem Wach-Status synchronisieren.
    // Nötig wenn der App-Prozess über Nacht im Hintergrund läuft:
    // der Flow-Wert könnte noch "true" sein, obwohl das Datum gewechselt hat.
    if (!appSettings.isAwakeTodayEffective()) {
        appSettings.setAwakeToday(false)
    }
    refreshData()
    triggerMemberReset()
    recalculateSchedule()
}

fun FamilyViewModel.logout() {
    _errorMessage.value = null
    // Tracking: Logout-Rate für Retention-Analyse (vor clearAll, da danach User-Kontext weg)
    TelemetryDeck.signal("auth.logout")
    // Alarm-State persistiert in SharedPrefs (ALARM_ENABLED ist kein Session-State).
    // clearAll() berührt ihn nicht – kein explizites Save nötig.
    cancelAlarmForCurrentUser()
    appSettings.clearAll()
    scope.launch { auth.signOut() }
}
