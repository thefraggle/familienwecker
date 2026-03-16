package de.familienwecker.famwake.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.familienwecker.famwake.algorithm.Scheduler
import de.familienwecker.famwake.alarm.AlarmScheduler
import de.familienwecker.famwake.data.FirebaseRepository
import de.familienwecker.famwake.data.PreferencesRepository
import de.familienwecker.famwake.data.FamilyNotFoundException
import de.familienwecker.famwake.data.CodeGenerationFailedException
import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.model.FamilySchedule
import de.familienwecker.famwake.model.ScheduleMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import de.familienwecker.famwake.R
import de.familienwecker.famwake.ui.util.UiText
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import de.familienwecker.famwake.util.WhatsNewManager
import de.familienwecker.famwake.util.WhatsNewContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import de.familienwecker.famwake.util.NetworkUtils
import kotlinx.coroutines.withTimeoutOrNull

class FamilyViewModel(
    application: Application,
    private val repository: FirebaseRepository = FirebaseRepository(),
    prefsRepo: PreferencesRepository = PreferencesRepository(application)
) : AndroidViewModel(application) {

    private val scheduler = Scheduler()
    private val alarmScheduler = AlarmScheduler(application)
    private val prefsRepo: PreferencesRepository = prefsRepo
    private val auth = FirebaseAuth.getInstance()
    private val whatsNewManager = WhatsNewManager(application)

    /** UID des aktuell eingeloggten Users (null wenn nicht eingeloggt). */
    val currentUserId: String?
        get() = auth.currentUser?.uid

    val myMemberId: StateFlow<String?> = prefsRepo.myMemberId
    val alarmSoundUri: StateFlow<String?> = prefsRepo.alarmSoundUri
    val familyId: StateFlow<String?> = prefsRepo.familyId
    val joinCode: StateFlow<String?> = prefsRepo.joinCode
    val familyName: StateFlow<String?> = prefsRepo.familyName
    val language: StateFlow<String> = prefsRepo.language
    val themePreference: StateFlow<String> = prefsRepo.themePreference
    val isAlarmEnabled: StateFlow<Boolean> = prefsRepo.isAlarmEnabled

    private val _members = MutableStateFlow<PersistentList<FamilyMember>>(persistentListOf())
    val members: StateFlow<PersistentList<FamilyMember>> = _members.asStateFlow()

    private val _schedule = MutableStateFlow<FamilySchedule?>(null)
    val schedule: StateFlow<FamilySchedule?> = _schedule.asStateFlow()

    private val _errorMessage = MutableStateFlow<UiText?>(null)
    val errorMessage: StateFlow<UiText?> = _errorMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow(de.familienwecker.famwake.model.SyncStatus())
    val syncStatus: StateFlow<de.familienwecker.famwake.model.SyncStatus> = _syncStatus.asStateFlow()

    private val _whatsNewContent = MutableStateFlow<WhatsNewContent?>(null)
    val whatsNewContent: StateFlow<WhatsNewContent?> = _whatsNewContent.asStateFlow()

    private val _pendingJoinCode = MutableStateFlow<String?>(null)
    val pendingJoinCode: StateFlow<String?> = _pendingJoinCode.asStateFlow()

    private val _showJoinSuccess = MutableStateFlow(false)
    val showJoinSuccess: StateFlow<Boolean> = _showJoinSuccess.asStateFlow()

    // Offline-Debounce – CloudOff-Icon erst nach 3s ohne Verbindung zeigen
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()
    private var offlineDebounceJob: Job? = null

    private val _familyCreatorId = MutableStateFlow<String?>(null)
    val familyCreatorId: StateFlow<String?> = _familyCreatorId.asStateFlow()

    /** True wenn der eingeloggte User der Ersteller (Admin) der aktuellen Familie ist. */
    val isAdmin: Boolean
        get() = auth.currentUser?.uid != null && auth.currentUser?.uid == _familyCreatorId.value

    private var membersJob: Job? = null
    private var syncStatusJob: Job? = null

    // Zuletzt gesetzten Alarm-Zeitstempel merken
    private var lastScheduledAlarmMillis: Long? = null

    init {
        // Observe FamilyId and load members accordingly
        viewModelScope.launch {
            try {
                familyId.collect { currentFamilyId ->
                    membersJob?.cancel()
                    syncStatusJob?.cancel()
                    if (!currentFamilyId.isNullOrBlank()) {
                        refreshData()
                        // Admin-Status laden
                        launch {
                            val data = repository.getFamilyData(currentFamilyId)
                            _familyCreatorId.value = data?.createdByUserId
                        }

                        syncStatusJob = launch {
                            try {
                                repository.getSyncStatusFlow(currentFamilyId).collect { status ->
                                    _syncStatus.value = status
                                    // Offline-Status mit Debounce setzen (3s Wartezeit)
                                    // isFromCache reicht als Signal – hasPendingWrites ist egal
                                    if (status.isFromCache) {
                                        offlineDebounceJob?.cancel()
                                        offlineDebounceJob = launch {
                                            delay(3000)
                                            _isOffline.value = true
                                        }
                                    } else {
                                        offlineDebounceJob?.cancel()
                                        _isOffline.value = false
                                    }
                                }
                            } catch (e: Exception) {
                                // Silent error for sync status
                            }
                        }

                        membersJob = launch {
                            try {
                                repository.getFamilyMembersFlow(currentFamilyId).collect { membersList ->
                                    val checkedMembers = checkAndResetMembers(membersList)
                                    _members.value = checkedMembers.toPersistentList()

                                    // Auto-Sync MyMemberId aus Cloud (multi-device resilience)
                                    val uid = auth.currentUser?.uid
                                    if (uid != null) {
                                        val claimedByMe = checkedMembers.find { it.claimedByUserId == uid }
                                        if (claimedByMe != null && claimedByMe.id != myMemberId.value) {
                                            prefsRepo.setMyMemberId(claimedByMe.id)
                                            prefsRepo.setMyMemberName(claimedByMe.name)
                                        } else if (claimedByMe == null && myMemberId.value != null) {
                                            prefsRepo.setMyMemberId(null)
                                            prefsRepo.setMyMemberName(null)
                                        }
                                        // Initial-Push: lokalen Alarm-Status nach Firestore übertragen
                                        val myId = myMemberId.value
                                        val fId = currentFamilyId
                                        if (myId != null) {
                                            launch {
                                                repository.updateDeviceAlarmEnabled(fId, myId, isAlarmEnabled.value)
                                            }
                                        }
                                    }

                                    recalculateSchedule()
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                // Self-Healing: Bei Permission Denied (veraltete FamilyId) lokal aufräumen.
                                // Nur wenn die Familie wirklich nicht mehr existiert – verhindert false-positives
                                // z.B. wenn ein anderer User die Familie verlässt und kurz PERMISSION_DENIED
                                // auf diesem Gerät ausgelöst wird.
                                if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                                    val fId = currentFamilyId
                                    val stillExists = if (fId != null) repository.checkFamilyExists(fId) else false
                                    if (!stillExists) {
                                        leaveFamily()
                                        _members.value = persistentListOf()
                                    }
                                } else {
                                    _errorMessage.value = UiText.StringResource(R.string.error_load_members)
                                }
                            }
                        }
                    } else {
                        _members.value = persistentListOf()
                        recalculateSchedule()
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = UiText.StringResource(R.string.error_system, e.localizedMessage ?: "Unknown")
            }
        }

        // Observer MyMemberId
        viewModelScope.launch {
            try {
                myMemberId.collect { id ->
                    if (id == null && isAlarmEnabled.value) {
                        setAlarmEnabled(false)
                    }
                    recalculateSchedule()
                }
            } catch (e: Exception) {
                // Ignore silent errors in member ID update
            }
        }

        // Observer Global Alarm Toggle → nach Firestore pushen (nur Anzeige für andere Geräte)
        viewModelScope.launch {
            try {
                isAlarmEnabled.collect { enabled ->
                    recalculateSchedule()
                    val fId = familyId.value
                    val myId = myMemberId.value
                    if (fId != null && myId != null) {
                        launch {
                            repository.updateDeviceAlarmEnabled(fId, myId, enabled)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore silent errors in alarm toggle
            }
        }

        checkWhatsNew()
    }

    fun setError(message: UiText) {
        _errorMessage.value = message
    }

    fun createFamily(familyName: String, onComplete: (Boolean) -> Unit) {
        _errorMessage.value = null
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _errorMessage.value = UiText.StringResource(R.string.error_not_logged_in)
            onComplete(false)
            return
        }
        viewModelScope.launch {
            if (!NetworkUtils.isOnline(getApplication())) {
                _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, "Offline")
                onComplete(false)
                return@launch
            }
            val result = repository.createFamily(familyName, uid)
            result.onSuccess { pair ->
                val saveResult = repository.saveUserFamily(uid, pair.first)
                if (saveResult.isFailure && de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.e("FamilyViewModel", "saveUserFamily fehlgeschlagen: ${saveResult.exceptionOrNull()?.message}")
                }
                prefsRepo.setFamilyId(pair.first)
                prefsRepo.setJoinCode(pair.second)
                prefsRepo.setFamilyName(familyName)
                onComplete(true)
            }.onFailure { error ->
                if (error is CodeGenerationFailedException) {
                    _errorMessage.value = UiText.StringResource(R.string.error_code_generation_failed)
                } else {
                    _errorMessage.value = UiText.StringResource(R.string.error_create_family)
                }
                onComplete(false)
            }
        }
    }

    fun joinFamily(code: String, onComplete: (Boolean) -> Unit) {
        _errorMessage.value = null

        // Nicht beitreten wenn bereits in dieser Familie
        if (code.equals(joinCode.value, ignoreCase = true)) {
            onComplete(true)
            return
        }

        viewModelScope.launch {
            if (!NetworkUtils.isOnline(getApplication())) {
                _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, "Offline")
                onComplete(false)
                return@launch
            }
            val result = repository.joinFamilyByCode(code)
            result.onSuccess { pair ->
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val saveResult = repository.saveUserFamily(uid, pair.first)
                    if (saveResult.isFailure && de.familienwecker.famwake.BuildConfig.DEBUG) {
                        android.util.Log.e("FamilyViewModel", "saveUserFamily fehlgeschlagen: ${saveResult.exceptionOrNull()?.message}")
                    }
                }
                val fetchedName = repository.getFamilyName(pair.first)
                prefsRepo.setFamilyId(pair.first)
                prefsRepo.setJoinCode(pair.second)
                prefsRepo.setFamilyName(fetchedName)

                if (_pendingJoinCode.value == code) {
                    _pendingJoinCode.value = null
                }
                _showJoinSuccess.value = true
                onComplete(true)
            }.onFailure { error ->
                if (error is FamilyNotFoundException) {
                    _errorMessage.value = UiText.StringResource(R.string.error_family_not_found)
                } else {
                    _errorMessage.value = UiText.StringResource(R.string.error_invalid_code)
                }
                _pendingJoinCode.value = null
                onComplete(false)
            }
        }
    }

    fun setPendingJoinCode(code: String?) {
        if (code != null && code.equals(joinCode.value, ignoreCase = true)) {
            // Ignorieren, falls Benutzer schon in dieser Familie ist
            _pendingJoinCode.value = null
            return
        }
        _pendingJoinCode.value = code
    }

    fun clearPendingJoinCode() {
        _pendingJoinCode.value = null
    }

    fun handlePendingJoin(onComplete: (Boolean) -> Unit) {
        val code = _pendingJoinCode.value ?: return
        if (code.equals(joinCode.value, ignoreCase = true)) {
            _pendingJoinCode.value = null
            onComplete(true)
            return
        }
        prefsRepo.setFamilyId(null)
        joinFamily(code, onComplete)
    }

    fun leaveAndJoinPendingCode(onComplete: (Boolean) -> Unit) {
        val code = _pendingJoinCode.value ?: return

        // Nicht beitreten wenn bereits in dieser Familie
        if (code.equals(joinCode.value, ignoreCase = true)) {
            _pendingJoinCode.value = null
            onComplete(true)
            return
        }

        val uid = auth.currentUser?.uid

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                // Netzwerk-Check vor Join-Versuch
                if (!NetworkUtils.isOnline(getApplication())) {
                    _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, "Offline")
                    _pendingJoinCode.value = null
                    onComplete(false)
                    return@launch
                }

                // Versuche zuerst der neuen Familie beizutreten, BEVOR die alte verlassen wird!
                val result = repository.joinFamilyByCode(code)
                result.onSuccess { pair ->
                    val newFamilyId = pair.first
                    val newJoinCode = pair.second
                    
                    // Code ist gültig -> Alte Familie verlassen (Alarm canceln)
                    cancelAlarmForCurrentUser()
                    
                    // Neues Mapping speichern (kein joinCode im User-Profil)
                    if (uid != null) {
                        val saveResult = repository.saveUserFamily(uid, newFamilyId)
                        if (saveResult.isFailure) {
                            android.util.Log.e("FamilyViewModel", "saveUserFamily fehlgeschlagen: ${saveResult.exceptionOrNull()?.message}")
                        }
                    }
                    val fetchedName = repository.getFamilyName(newFamilyId)
                    val oldFamilyId = familyId.value
                    
                    prefsRepo.setFamilyId(newFamilyId)
                    prefsRepo.setJoinCode(newJoinCode)
                    prefsRepo.setFamilyName(fetchedName)

                    if (oldFamilyId != newFamilyId) {
                        prefsRepo.setMyMemberId(null)
                    }

                    _pendingJoinCode.value = null
                    _showJoinSuccess.value = true
                    onComplete(true)
                }.onFailure { error ->
                    // Code ist ungültig -> Fehler anzeigen, aber in der ALTEN Familie bleiben!
                    if (error is FamilyNotFoundException) {
                        _errorMessage.value = UiText.StringResource(R.string.error_family_not_found)
                    } else {
                        _errorMessage.value = UiText.StringResource(R.string.error_invalid_code, error.localizedMessage ?: "Unknown")
                    }
                    _pendingJoinCode.value = null
                    onComplete(false)
                }
            } catch (e: Exception) {
                _errorMessage.value = UiText.StringResource(R.string.error_system, e.localizedMessage ?: "Unknown")
                _pendingJoinCode.value = null
                onComplete(false)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun addOrUpdateMember(member: FamilyMember) {
        val currentFamilyId = familyId.value ?: return
        viewModelScope.launch {
            repository.addOrUpdateMember(currentFamilyId, member)
        }
    }

    fun removeMember(id: String) {
        val currentFamilyId = familyId.value ?: return
        alarmScheduler.cancelWakeUp(id)
        viewModelScope.launch {
            val result = repository.removeMember(currentFamilyId, id)
            if (result.isFailure) {
                _errorMessage.value = UiText.StringResource(R.string.error_delete_member, result.exceptionOrNull()?.localizedMessage ?: "Unknown")
            }
        }
        if (myMemberId.value == id) {
            setMyMemberId(null)
        }
    }

    fun setMyMemberId(id: String?, onComplete: (Boolean) -> Unit = {}) {
        val currentFamilyId = familyId.value ?: return
        val currentMyMemberId = myMemberId.value
        val userId = auth.currentUser?.uid ?: return
        val userName = auth.currentUser?.displayName
            ?: getApplication<Application>().getString(R.string.settings_fallback_username)

        // Offline: Profil-Claim erfordert Netzwerk (Firestore-Transaktion)
        if (_isOffline.value) {
            onComplete(false)
            return
        }

        viewModelScope.launch {
            if (currentMyMemberId != null && currentMyMemberId != id) {
                repository.unclaimMember(currentFamilyId, currentMyMemberId, userId)
            }

            if (id != null) {
                val success = repository.claimMember(currentFamilyId, id, userId, userName)
                if (success) {
                    prefsRepo.setMyMemberId(id)
                    // Namen des geclaimten Mitglieds persistieren für BootReceiver
                    val memberName = _members.value.find { it.id == id }?.name
                    prefsRepo.setMyMemberName(memberName)
                    prefsRepo.setAlarmEnabled(true)
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } else {
                prefsRepo.setMyMemberId(null)
                prefsRepo.setMyMemberName(null)
                prefsRepo.setAlarmEnabled(false)
                onComplete(true)
            }
        }
    }

    fun setAlarmSoundUri(uri: String) {
        prefsRepo.setAlarmSoundUri(uri)
    }

    fun setLanguage(lang: String) {
        prefsRepo.setLanguage(lang)
    }

    fun setThemePreference(theme: String) {
        prefsRepo.setThemePreference(theme)
    }

    // isAlarmEnabled ist gerätespezifisch und darf NICHT in Firestore geschrieben werden.
    fun setAlarmEnabled(enabled: Boolean) {
        if (enabled && myMemberId.value == null) return
        prefsRepo.setAlarmEnabled(enabled)
    }

    fun togglePauseMember(memberId: String) {
        val member = _members.value.find { it.id == memberId } ?: return
        if (member.claimedByUserId != null && member.id != myMemberId.value) return
        val updatedMember = member.copy(isPaused = !member.isPaused)
        addOrUpdateMember(updatedMember)
    }

    fun moveMemberOrder(fromIndex: Int, toIndex: Int) {
        val currentMembers = _members.value.toMutableList()
        if (fromIndex !in currentMembers.indices || toIndex !in currentMembers.indices) return

        val member = currentMembers.removeAt(fromIndex)
        currentMembers.add(toIndex, member)

        val updatedMembers = currentMembers.mapIndexed { index, m ->
            m.copy(sequenceOrder = index)
        }
        _members.value = updatedMembers.toPersistentList()
        recalculateSchedule()
    }

    fun saveMemberOrder() {
        val currentFamilyId = familyId.value ?: return
        val updatedMembers = _members.value
        val orderMap = updatedMembers.associate { it.id to it.sequenceOrder }

        viewModelScope.launch {
            repository.updateMemberOrders(currentFamilyId, orderMap)
        }
    }

    fun toggleAwakeMember(memberId: String) {
        if (memberId != myMemberId.value) return

        val member = _members.value.find { it.id == memberId } ?: return

        // Dauer-Berechnung
        val now = LocalTime.now()
        val wakeUpTime = member.latestWakeUp
        val targetDate = if (now.isAfter(wakeUpTime)) LocalDate.now().plusDays(1) else LocalDate.now()
        val targetDateTime = LocalDateTime.of(targetDate, wakeUpTime)
        val hoursUntil = java.time.Duration.between(LocalDateTime.now(), targetDateTime).toHours()

        if (hoursUntil > 4 && !member.isAwakeToday) {
            return
        }

        val updatedMember = member.copy(isAwakeToday = !member.isAwakeToday)
        addOrUpdateMember(updatedMember)
    }

    fun snooze(memberId: String, memberName: String) {
        val snoozeTime = LocalDateTime.now().plusMinutes(5)
        alarmScheduler.scheduleWakeUp(
            wakeUpTime = snoozeTime,
            memberId = memberId,
            memberName = memberName,
            soundUri = alarmSoundUri.value,
            onPermissionDenied = {
                _errorMessage.value = UiText.StringResource(R.string.error_alarm_permission)
            }
        )
    }

    fun refreshData() {
        val uid = auth.currentUser?.uid ?: return
        _isSyncing.value = true
        viewModelScope.launch {
            if (!NetworkUtils.isOnline(getApplication())) {
                _isSyncing.value = false
                return@launch
            }
            try {
                val result = withTimeoutOrNull(3000) {
                    repository.getUserFamily(uid, cachedJoinCode = prefsRepo.joinCode.value)
                }
                if (result == null) {
                    _isSyncing.value = false
                    return@launch
                }
                result.onSuccess { pair ->
                    if (pair != null) {
                        prefsRepo.setFamilyId(pair.first)
                        prefsRepo.setJoinCode(pair.second)
                        // isAlarmEnabled wird NICHT aus Firestore geladen (gerätespezifisch)

                        val fetchedFamilyName = repository.getFamilyName(pair.first)
                        prefsRepo.setFamilyName(fetchedFamilyName)

                        val claimedMember = repository.getClaimedMember(pair.first, uid)
                        if (claimedMember != null) {
                            prefsRepo.setMyMemberId(claimedMember.id)
                        }
                    } else {
                        leaveFamily()
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, e.localizedMessage ?: "Unknown")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun triggerMemberReset() {
        val currentMembers = _members.value
        if (currentMembers.isNotEmpty()) {
            val checkedMembers = checkAndResetMembers(currentMembers)
            if (checkedMembers != currentMembers) {
                _members.value = checkedMembers.toPersistentList()
                recalculateSchedule()
            }
        }
    }

    private fun checkAndResetMembers(members: List<FamilyMember>): List<FamilyMember> {
        val today = LocalDate.now().toString()
        val now = LocalTime.now()
        val toUpdate = mutableListOf<FamilyMember>()

        val result = members.map { member ->
            val resetThreshold = member.latestWakeUp.plusHours(4)
            val isPastResetThreshold = now.isAfter(resetThreshold)

            if (isPastResetThreshold && member.lastResetDate != today) {
                val isUnclaimed = member.claimedByUserId == null
                val newIsPaused = if (isUnclaimed) false else member.isPaused

                val updated = member.copy(
                    isPaused = newIsPaused,
                    isAwakeToday = false,
                    lastResetDate = today
                )
                toUpdate.add(updated)
                updated
            } else {
                member
            }
        }

        // Alle geänderten Members in einem einzigen Batch schreiben
        val familyIdVal = familyId.value
        if (familyIdVal != null && toUpdate.isNotEmpty()) {
            viewModelScope.launch {
                toUpdate.forEach { updated ->
                    try {
                        repository.addOrUpdateMember(familyIdVal, updated)
                    } catch (e: Exception) {
                        if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                            android.util.Log.e("FamilyViewModel", "Failed to reset member status: ${e.message}")
                        }
                    }
                }
            }
        }

        return result
    }

    fun logout() {
        _errorMessage.value = null
        cancelAlarmForCurrentUser()
        prefsRepo.clearAll()
        auth.signOut()
    }

    private fun cancelAlarmForCurrentUser() {
        myMemberId.value?.let { alarmScheduler.cancelWakeUp(it) }
    }

    fun deleteFamily(onComplete: (Boolean) -> Unit) {
        _errorMessage.value = null
        val currentFamilyId = familyId.value ?: return
        viewModelScope.launch {
            val result = repository.deleteFamily(currentFamilyId)
            if (result.isSuccess) {
                auth.currentUser?.uid?.let { uid ->
                    repository.removeUserFamily(uid)
                }
                prefsRepo.setFamilyId(null)
                prefsRepo.setJoinCode(null)
                prefsRepo.setFamilyName(null)
                prefsRepo.setMyMemberId(null)
                onComplete(true)
            } else {
                _errorMessage.value = UiText.StringResource(R.string.error_delete_family, result.exceptionOrNull()?.localizedMessage ?: "Unknown")
                onComplete(false)
            }
        }
    }

    fun leaveFamily() {
        _errorMessage.value = null
        val uid = auth.currentUser?.uid ?: return
        val currentFamilyId = familyId.value
        val currentMemberId = myMemberId.value
        cancelAlarmForCurrentUser()
        viewModelScope.launch {
            // Eigenen Member-Datensatz komplett löschen (nicht nur unclaimen).
            // Die Firestore-Rule erlaubt delete wenn claimedByUserId == request.auth.uid.
            // So ist kein verwaistes Profil mehr in der Familie nach dem Verlassen.
            if (currentFamilyId != null && currentMemberId != null) {
                repository.removeMember(currentFamilyId, currentMemberId)
            }
            repository.removeUserFamily(uid)
            prefsRepo.setFamilyId(null)
            prefsRepo.setJoinCode(null)
            prefsRepo.setFamilyName(null)
            prefsRepo.setMyMemberId(null)
            prefsRepo.setMyMemberName(null)
        }
    }

    private fun recalculateSchedule() {
        val currentMembers = _members.value
        val alarmsOn = isAlarmEnabled.value

        if (currentMembers.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    val currentMyMemberId = myMemberId.value
                    val calculationMembers = if (alarmsOn) {
                        currentMembers
                    } else {
                        currentMembers.filter { it.id != currentMyMemberId }
                    }

                    if (calculationMembers.none { !it.isPaused }) {
                        _schedule.value = FamilySchedule(emptyList(), null, true, ScheduleMessage.NoActiveSchedule)
                        currentMembers.forEach { alarmScheduler.cancelWakeUp(it.id) }
                        return@launch
                    }

                    val result = withContext(Dispatchers.Default) {
                        scheduler.calculateIdealSchedule(calculationMembers)
                    }
                    _schedule.value = result

                    if (alarmsOn && result.isValid && result.memberSchedules.isNotEmpty()) {
                        applyAlarms(result)
                    } else {
                        currentMembers.forEach { alarmScheduler.cancelWakeUp(it.id) }
                    }
                } catch (e: Exception) {
                    _errorMessage.value = UiText.StringResource(R.string.error_calculate_schedule, e.localizedMessage ?: "Unknown")
                    _schedule.value = null
                }
            }
        } else {
            cancelAlarmForCurrentUser()
            _schedule.value = null
        }
    }

    private fun applyAlarms(schedule: FamilySchedule) {
        val tomorrow = LocalDate.now().plusDays(1)
        val today = LocalDate.now()

        val currentMyMemberId = myMemberId.value ?: return
        if (schedule.memberSchedules.isEmpty()) return

        for (memberSchedule in schedule.memberSchedules) {
            if (memberSchedule.member.id == currentMyMemberId) {
                val wakeUpTime = memberSchedule.wakeUpTime
                val targetDate = if (LocalTime.now().isAfter(wakeUpTime)) tomorrow else today
                val targetDateTime = LocalDateTime.of(targetDate, wakeUpTime)

                // Alarm nur neu setzen wenn die Zeit sich geändert hat
                val newAlarmMillis = targetDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                if (newAlarmMillis == lastScheduledAlarmMillis) return

                alarmScheduler.cancelWakeUp(currentMyMemberId)

                if (memberSchedule.member.isAwakeToday) {
                    val hoursUntilAlarm = java.time.Duration.between(LocalDateTime.now(), targetDateTime).toHours()
                    if (hoursUntilAlarm in 0..4) {
                        lastScheduledAlarmMillis = null
                        return
                    }
                }

                alarmScheduler.scheduleWakeUp(
                    wakeUpTime = targetDateTime,
                    memberId = memberSchedule.member.id,
                    memberName = memberSchedule.member.name,
                    soundUri = alarmSoundUri.value,
                    onPermissionDenied = {
                        _errorMessage.value = UiText.StringResource(R.string.error_alarm_permission)
                    }
                )
                lastScheduledAlarmMillis = newAlarmMillis
            }
        }
    }

    /** Übersetzt eine ScheduleMessage in lokalisiertes UiText für die Darstellung im UI. */
    fun scheduleMessageToUiText(msg: ScheduleMessage): UiText = when (msg) {
        is ScheduleMessage.OptimalPlan -> UiText.StringResource(R.string.schedule_message_optimal)
        is ScheduleMessage.NoActiveMembers -> UiText.StringResource(R.string.schedule_message_no_members)
        is ScheduleMessage.NoValidScheduleFound -> UiText.StringResource(R.string.schedule_message_no_valid)
        is ScheduleMessage.TimeAdjusted -> UiText.StringResource(R.string.schedule_message_time_adjusted, msg.minutes)
        is ScheduleMessage.BreakfastReduced -> UiText.StringResource(R.string.schedule_message_breakfast_reduced, msg.minutes)
        is ScheduleMessage.BreakfastAndTimeAdjusted -> UiText.StringResource(R.string.schedule_message_breakfast_and_time_adjusted, msg.breakfast, msg.shift)
        is ScheduleMessage.MemberConflict -> UiText.StringResource(R.string.schedule_message_member_conflict, msg.memberName)
        is ScheduleMessage.NoActiveSchedule -> UiText.StringResource(R.string.main_no_active_schedule)
    }

    private fun checkWhatsNew() {
        viewModelScope.launch {
            val content = whatsNewManager.getWhatsNewContent() ?: return@launch
            val lastSeen = prefsRepo.lastSeenWhatsNewVersion.value
            if (content.versionCode > lastSeen) {
                _whatsNewContent.value = content
            }
        }
    }

    fun dismissWhatsNew() {
        _whatsNewContent.value?.let {
            prefsRepo.setLastSeenWhatsNewVersion(it.versionCode)
            _whatsNewContent.value = null
        }
    }

    fun dismissJoinSuccess() {
        _showJoinSuccess.value = false
    }

    override fun onCleared() {
        super.onCleared()
        // SharedPreferences-Listener deregistrieren um Memory Leaks zu vermeiden
        prefsRepo.unregisterListener()
    }
}
