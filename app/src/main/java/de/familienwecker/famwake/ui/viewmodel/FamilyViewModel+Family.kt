package de.familienwecker.famwake.ui.viewmodel

import de.familienwecker.famwake.R
import de.familienwecker.famwake.data.FamilyNotFoundException
import de.familienwecker.famwake.data.CodeGenerationFailedException
import de.familienwecker.famwake.ui.util.UiText
import de.familienwecker.famwake.util.NetworkUtils
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
        if (!NetworkUtils.isOnline(getApplication())) {
            _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, getApplication<android.app.Application>().getString(R.string.error_offline))
            onComplete(false)
            return@launch
        }
        val result = repository.createFamily(familyName, uid)
        result.onSuccess { pair ->
            appSettings.setFamilyId(pair.first)
            appSettings.setJoinCode(pair.second)
            appSettings.setFamilyName(familyName)
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
        if (!NetworkUtils.isOnline(getApplication())) {
            _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, getApplication<android.app.Application>().getString(R.string.error_offline))
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
        if (!success) _pendingJoinCode.value = null
        else _pendingJoinCode.value = null
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
            if (!NetworkUtils.isOnline(getApplication())) {
                _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, getApplication<android.app.Application>().getString(R.string.error_offline))
                _pendingJoinCode.value = null
                onComplete(false)
                return@launch
            }
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
        try {
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
        val uid = auth.currentUser?.uid ?: return@launch
        val result = repository.deleteFamily(currentFamilyId, uid)
        if (result.isSuccess) {
            appSettings.setFamilyId(null)
            appSettings.setJoinCode(null)
            appSettings.setFamilyName(null)
            appSettings.setMyMemberId(null)
            appSettings.setMyMemberName(null)
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
        if (!NetworkUtils.isOnline(getApplication())) {
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
            _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, e.localizedMessage ?: getApplication<android.app.Application>().getString(R.string.add_member_unknown))
        } finally {
            _isSyncing.value = false
        }
    }
}

fun FamilyViewModel.triggerRefresh() {
    refreshData()
    triggerMemberReset()
    recalculateSchedule()
}

fun FamilyViewModel.logout() {
    _errorMessage.value = null
    cancelAlarmForCurrentUser()
    appSettings.clearAll()
    scope.launch { auth.signOut() }
}
