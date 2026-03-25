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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import de.familienwecker.famwake.R
import de.familienwecker.famwake.ui.util.UiText
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import de.familienwecker.famwake.util.NetworkUtils
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FamilyViewModel(
    application: Application,
    private val repository: FirebaseRepository = FirebaseRepository(),
    prefsRepo: PreferencesRepository = PreferencesRepository(application)
) : AndroidViewModel(application) {

    private val scheduler = Scheduler()
    private val alarmScheduler = AlarmScheduler(application)
    private val prefsRepo: PreferencesRepository = prefsRepo
    private val auth = FirebaseAuth.getInstance()

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
    val isAwakeTodayLocal: StateFlow<Boolean> = prefsRepo.isAwakeToday
    val onboardingCompleted: StateFlow<Boolean> = prefsRepo.onboardingCompleted

    // Tooltips
    val tooltipsEnabled: StateFlow<Boolean>       = prefsRepo.tooltipsEnabled
    val tooltipAwakeSeen: StateFlow<Boolean>      = prefsRepo.tooltipAwakeSeen
    val tooltipDragSeen: StateFlow<Boolean>       = prefsRepo.tooltipDragSeen
    val tooltipWakeWindowSeen: StateFlow<Boolean> = prefsRepo.tooltipWakeWindowSeen
    val tooltipBathroomSeen: StateFlow<Boolean>   = prefsRepo.tooltipBathroomSeen
    val tooltipInviteSeen: StateFlow<Boolean>     = prefsRepo.tooltipInviteSeen
    val tooltipSwitchSeen: StateFlow<Boolean>     = prefsRepo.tooltipSwitchSeen
    val tooltipWeekdaysSeen: StateFlow<Boolean>   = prefsRepo.tooltipWeekdaysSeen

    fun setOnboardingCompleted(completed: Boolean) = prefsRepo.setOnboardingCompleted(completed)
    fun setTooltipsEnabled(enabled: Boolean)        = prefsRepo.setTooltipsEnabled(enabled)
    fun markTooltipSeen(key: String)                = prefsRepo.setTooltipSeen(key)
    fun resetAllTooltips()                          = prefsRepo.resetAllTooltips()

    // Schlüssel-Accessoren für Composables
    val tooltipKeyAwake      get() = prefsRepo.tooltipKeyAwake
    val tooltipKeyDrag       get() = prefsRepo.tooltipKeyDrag
    val tooltipKeyWakeWindow get() = prefsRepo.tooltipKeyWakeWindow
    val tooltipKeyBathroom   get() = prefsRepo.tooltipKeyBathroom
    val tooltipKeyInvite     get() = prefsRepo.tooltipKeyInvite
    val tooltipKeySwitch     get() = prefsRepo.tooltipKeySwitch
    val tooltipKeyWeekdays   get() = prefsRepo.tooltipKeyWeekdays

    private val _members = MutableStateFlow<PersistentList<FamilyMember>>(persistentListOf())
    val members: StateFlow<PersistentList<FamilyMember>> = _members.asStateFlow()

    private val _schedule = MutableStateFlow<FamilySchedule?>(null)
    val schedule: StateFlow<FamilySchedule?> = _schedule.asStateFlow()

    private val _errorMessage = MutableStateFlow<UiText?>(null)
    val errorMessage: StateFlow<UiText?> = _errorMessage.asStateFlow()

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow(de.familienwecker.famwake.model.SyncStatus())
    val syncStatus: StateFlow<de.familienwecker.famwake.model.SyncStatus> = _syncStatus.asStateFlow()

    private val _pendingJoinCode = MutableStateFlow<String?>(null)
    val pendingJoinCode: StateFlow<String?> = _pendingJoinCode.asStateFlow()

    private val _isJoiningFamily = MutableStateFlow(false)
    val isJoiningFamily: StateFlow<Boolean> = _isJoiningFamily.asStateFlow()


    // Offline-Debounce – CloudOff-Icon erst nach 3s ohne Verbindung zeigen
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()
    private var offlineDebounceJob: Job? = null

    private val _familyCreatorId = MutableStateFlow<String?>(null)
    val familyCreatorId: StateFlow<String?> = _familyCreatorId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isGlobalAdmin: StateFlow<Boolean> = repository.getAuthStateFlow()
        .flatMapLatest { user ->
            user?.uid?.let { uid ->
                repository.checkIsGlobalAdminFlow(uid)
            } ?: flowOf(false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isAdmin: StateFlow<Boolean> = combine(isGlobalAdmin, _familyCreatorId) { isGlobal, creatorId ->
        isGlobal || (auth.currentUser?.uid != null && auth.currentUser?.uid == creatorId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var membersJob: Job? = null
    private var syncStatusJob: Job? = null

    // Zuletzt gesetzten Alarm-Zeitstempel merken
    private var lastScheduledAlarmMillis: Long? = null

    // Debounce Jobs für Firebase-Writes
    private var memberUpdateJob: Job? = null
    private var alarmToggleJob: Job? = null

    // Snooze-Status: wenn nicht null ist ein Snooze aktiv
    val snoozeUntil: StateFlow<java.time.LocalDateTime?> = prefsRepo.snoozeUntil

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
                                    // Nur offline zeigen wenn isFromCache UND kein validiertes Netz –
                                    // verhindert False-Positives direkt nach App-Start (Firestore liefert kurz aus Cache)
                                    if (status.isFromCache && !NetworkUtils.isOnline(getApplication())) {
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
                                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                                    android.util.Log.e("FamilyViewModel", "SyncStatus Flow Error: ${e.message}")
                                }
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
                                        // Race-Condition-Guard: myMemberId nur überschreiben wenn Cloud-Wert
                                        // wirklich anders ist als lokal bereits gesetzt
                                        if (claimedByMe != null && claimedByMe.id != myMemberId.value) {
                                            prefsRepo.setMyMemberId(claimedByMe.id)
                                            prefsRepo.setMyMemberName(claimedByMe.name)
                                        } else if (claimedByMe == null && myMemberId.value != null) {
                                            // Kein Profil mehr geclaimt – aber nur räumen wenn nicht gerade
                                            // ein frischer Join-Vorgang läuft (myMemberId wäre dann noch null)
                                            prefsRepo.setMyMemberId(null)
                                            prefsRepo.setMyMemberName(null)
                                        }
                                    }

                                    recalculateSchedule()
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                                    android.util.Log.e("FamilyViewModel", "Members Flow Error: ${e.message}")
                                }
                                val errorMsg = e.localizedMessage ?: getApplication<Application>().getString(R.string.error_permission_denied)
                                _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, errorMsg)

                                // Self-Healing: Bei Permission Denied (veraltete FamilyId) lokal aufräumen.
                                if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
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
                _errorMessage.value = UiText.StringResource(R.string.error_system, e.localizedMessage ?: getApplication<Application>().getString(R.string.add_member_unknown))
            }
        }

        // Observer MyMemberId
        viewModelScope.launch {
            try {
                var isFirstEmission = true
                myMemberId.collect { id ->
                    if (id == null && isAlarmEnabled.value && !isFirstEmission) {
                        // Alarm nur deaktivieren wenn myMemberId aktiv verloren geht
                        // (nicht beim initialen null-Wert nach Neuinstall/Neustart)
                        setAlarmEnabled(false)
                    }
                    isFirstEmission = false
                    recalculateSchedule()
                }
            } catch (e: Exception) {
                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.w("FamilyViewModel", "myMemberId observer error: ${e.message}")
                }
            }
        }

        // Observer Global Alarm Toggle
        viewModelScope.launch {
            try {
                isAlarmEnabled.collect { enabled ->
                    recalculateSchedule()
                    // Sync zu Firestore (deviceAlarmEnabled) entfernt, da nun rein lokal.
                }
            } catch (e: Exception) {
                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.w("FamilyViewModel", "isAlarmEnabled observer error: ${e.message}")
                }
            }
        }

        // Snooze-Cleanup: Veraltete Snoozes beim Start entfernen
        viewModelScope.launch {
            val currentSnooze = snoozeUntil.value
            if (currentSnooze != null && currentSnooze.isBefore(java.time.LocalDateTime.now().minusMinutes(30))) {
                prefsRepo.setSnoozeUntil(null)
            }
        }

// Periodischer Timer entfernt zugunsten von Lazy-Refresh (onResume) und Cloud-Reset.
    }

    fun setError(message: UiText) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun createFamily(familyName: String, onComplete: (Boolean) -> Unit) {
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
        viewModelScope.launch {
            if (!NetworkUtils.isOnline(getApplication())) {
                    _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, getApplication<Application>().getString(R.string.error_offline))
                onComplete(false)
                return@launch
            }
            val result = repository.createFamily(familyName, uid)
            result.onSuccess { pair ->
                // Security Fix: saveUserFamily erfolgt jetzt serverseitig in der Cloud Function
                prefsRepo.setFamilyId(pair.first)
                prefsRepo.setJoinCode(pair.second)
                prefsRepo.setFamilyName(familyName)
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

    fun joinFamily(code: String, onComplete: (Boolean) -> Unit) {
        _errorMessage.value = null

        if (code.equals(joinCode.value, ignoreCase = true)) {
            onComplete(true)
            return
        }

        if (code.length != 6) {
            _errorMessage.value = UiText.StringResource(R.string.error_invalid_code, code)
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _isJoiningFamily.value = true
            if (!NetworkUtils.isOnline(getApplication())) {
                    _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, getApplication<Application>().getString(R.string.error_offline))
                onComplete(false)
                return@launch
            }
            val result = repository.joinFamilyByCode(code)
            result.onSuccess { pair ->
                // Security Fix: saveUserFamily erfolgt jetzt serverseitig in der Cloud Function
                val fetchedName = repository.getFamilyName(pair.first)
                prefsRepo.setFamilyId(pair.first)
                prefsRepo.setJoinCode(pair.second)
                prefsRepo.setFamilyName(fetchedName)

                if (_pendingJoinCode.value == code) {
                    _pendingJoinCode.value = null
                }
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
        // Auch Fehlermeldung löschen, wenn der Code verworfen wird
        _errorMessage.value = null
    }

    fun handlePendingJoin(onComplete: (Boolean) -> Unit) {
        val code = _pendingJoinCode.value ?: return
        if (code.equals(joinCode.value, ignoreCase = true)) {
            _pendingJoinCode.value = null
            onComplete(true)
            return
        }
        // WICHTIG: NICHT familyId(null) setzen bevor wir wissen ob der Join klappt!
        // LoadingScreen navigiert bei Erfolg sowieso zu Main.
        joinFamily(code) { success ->
            if (success) {
                _pendingJoinCode.value = null
            } else {
                // Bei Fehler Code löschen damit kein Loop im SetupScreen entsteht
                _pendingJoinCode.value = null
            }
            onComplete(success)
        }
    }

    fun leaveAndJoinPendingCode(onComplete: (Boolean) -> Unit) {
        val code = _pendingJoinCode.value ?: return

        // Nicht beitreten wenn bereits in dieser Familie – kein onLeaveFamily-Trigger
        if (code.equals(joinCode.value, ignoreCase = true)) {
            _pendingJoinCode.value = null
            onComplete(false)  // false = kein Navigation-Trigger im MainScreen
            return
        }

        val uid = auth.currentUser?.uid

        viewModelScope.launch {
            _isSyncing.value = true
            _isJoiningFamily.value = true
            try {
                // Netzwerk-Check vor Join-Versuch
                if (!NetworkUtils.isOnline(getApplication())) {
                    _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, getApplication<Application>().getString(R.string.error_offline))
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
                    
                    // Security Fix: saveUserFamily erfolgt jetzt serverseitig in der Cloud Function
                    val fetchedName = repository.getFamilyName(newFamilyId)
                    
                    // ERST den pendingJoinCode auf null setzen, dann die ID in den Prefs ändern!
                    _pendingJoinCode.value = null
                    _errorMessage.value = null
                    
                    prefsRepo.setFamilyId(newFamilyId)
                    prefsRepo.setJoinCode(newJoinCode)
                    prefsRepo.setFamilyName(fetchedName)
                    prefsRepo.setMyMemberId(null)

                    onComplete(true)
                }.onFailure { error ->
                    // Code ist ungültig → Fehler anzeigen, aber in der ALTEN Familie bleiben!
                    when {
                        error is FamilyNotFoundException ->
                            _errorMessage.value = UiText.StringResource(R.string.error_family_not_found)
                        error.message?.contains("TOO_MANY_REQUESTS", ignoreCase = true) == true ->
                            _errorMessage.value = UiText.StringResource(R.string.error_join_family_rate_limit)
                        else ->
                            _errorMessage.value = UiText.StringResource(R.string.error_invalid_code, error.localizedMessage ?: getApplication<Application>().getString(R.string.add_member_unknown))
                    }
                    _pendingJoinCode.value = null
                    onComplete(false)
                }
            } catch (e: Exception) {
                _errorMessage.value = UiText.StringResource(R.string.error_system, e.localizedMessage ?: getApplication<Application>().getString(R.string.add_member_unknown))
                _pendingJoinCode.value = null
                onComplete(false)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun addOrUpdateMember(member: FamilyMember) {
        val currentFamilyId = familyId.value ?: return
        if (member.name.isBlank()) {
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.e("FamilyViewModel", "Abbruch: Member Name ist leer")
            }
            return
        }
        viewModelScope.launch {
            try {
                repository.addOrUpdateMember(currentFamilyId, member)
            } catch (e: Exception) {
                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.e("FamilyViewModel", "Fehler beim Speichern von Member ${member.id}: ${e.message}")
                }
                _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, e.localizedMessage ?: getApplication<Application>().getString(R.string.add_member_unknown))
            }
        }
    }

    /**
     * Schreibt ein Mitglied mit 2s Debounce nach Firebase.
     * Nützlich für Toggles im MainScreen um Schreib-Spam zu vermeiden.
     */
    private fun addOrUpdateMemberDebounced(member: FamilyMember) {
        val currentFamilyId = familyId.value ?: return
        memberUpdateJob?.cancel()
        memberUpdateJob = viewModelScope.launch {
            delay(2000)
            repository.addOrUpdateMember(currentFamilyId, member)
        }
    }

    fun removeMember(id: String) {
        val currentFamilyId = familyId.value ?: return
        alarmScheduler.cancelWakeUp(id)
        viewModelScope.launch {
            val result = repository.removeMember(currentFamilyId, id)
            if (result.isFailure) {
                _errorMessage.value = UiText.StringResource(R.string.error_delete_member, result.exceptionOrNull()?.localizedMessage ?: getApplication<Application>().getString(R.string.add_member_unknown))
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

    // isAlarmEnabled ist gerätespezifisch. Firestore-Sync (deviceAlarmEnabled)
    // erfolgt debounced (2s) für die Anzeige bei anderen Familienmitgliedern.
    fun setAlarmEnabled(enabled: Boolean) {
        if (enabled && myMemberId.value == null) return

        // Wenn der Wecker ausgeschaltet wird, den "Schon wach"-Status zurücksetzen.
        // Das stellt sicher, dass der Wecker beim Wiedereinschalten normal klingelt.
        if (!enabled) {
            prefsRepo.setAwakeToday(false)
        }

        prefsRepo.setAlarmEnabled(enabled)
        
        // Sync zu Firestore debounced (Icon-Status für andere)
        val currentFamilyId = familyId.value
        val currentMemberId = myMemberId.value
        if (currentFamilyId != null && currentMemberId != null) {
            alarmToggleJob?.cancel()
            alarmToggleJob = viewModelScope.launch {
                delay(2000)
                repository.updateDeviceAlarmEnabled(currentFamilyId, currentMemberId, enabled)
            }
        }
    }

    /**
     * ADMIN/DEBUG: Setzt das DayProfile des heutigen Wochentags so,
     * dass der Wecker in ~5 Minuten klingelt.
     * DayOfWeek.value: 1=Mo ... 7=So (java.time)
     */
    fun setDebugAlarmIn5Minutes() {
        val memberId = myMemberId.value ?: return
        val member   = _members.value.find { it.id == memberId } ?: return
        val now      = java.time.LocalTime.now()
        val target   = now.plusMinutes(3)
        val earliest = target.minusMinutes(1)   // 2 Minuten ab jetzt
        val latest   = target.plusMinutes(1)    // 4 Minuten ab jetzt
        val todayKey = java.time.LocalDate.now().dayOfWeek.value // 1=Mo…7=So

        val debugProfile = de.familienwecker.famwake.model.DayProfile(
            isActive = true,
            earliestWakeUp = earliest,
            latestWakeUp = latest,
            bathroomDurationMinutes = 1L,
            wantsBreakfast = false
        )
        // Nur den heutigen Tag überschreiben; alle anderen Tage unverändert lassen
        val updatedDayProfiles = (member.dayProfiles ?: mapOf()).toMutableMap()
        updatedDayProfiles[todayKey] = debugProfile

        val updatedMember = member.copy(
            dayProfiles = updatedDayProfiles,
            isPaused = false
        )
        addOrUpdateMember(updatedMember)
        // Wecker global einschalten
        prefsRepo.setAlarmEnabled(true)
    }

    fun togglePauseMember(memberId: String) {
        val member = _members.value.find { it.id == memberId } ?: return
        if (member.claimedByUserId != null && member.id != myMemberId.value) return
        val updatedMember = member.copy(isPaused = !member.isPaused)
        addOrUpdateMemberDebounced(updatedMember)
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

        // 4-Stunden-Sperre entfernt, damit der Button jederzeit am Tag des Weckers funktioniert.

        val newAwakeState = !isAwakeTodayLocal.value
        prefsRepo.setAwakeToday(newAwakeState)

        // Status-Sync für andere (Sonnen-Icon)
        val updatedMember = member.copy(isAwakeToday = newAwakeState)
        addOrUpdateMemberDebounced(updatedMember)

        if (newAwakeState) {
            // Wecker sofort aus dem System entfernen – nicht auf applyAlarms() warten.
            // Der globale Switch bleibt dabei unverändert.
            cancelAlarmForCurrentUser()
            lastScheduledAlarmMillis = null
        } else {
            // Wieder aktiviert → nächsten Alarm neu berechnen und planen
            recalculateSchedule()
        }
    }

    fun snooze(memberId: String, memberName: String) {
        val snoozeTime = LocalDateTime.now().plusMinutes(5)
        prefsRepo.setSnoozeUntil(snoozeTime)
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

    fun cancelSnooze(memberId: String) {
        prefsRepo.setSnoozeUntil(null)
        alarmScheduler.cancelWakeUp(memberId, isSnooze = true)
        lastScheduledAlarmMillis = null
        recalculateSchedule()
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
                            // Restore von deviceAlarmEnabled aus Firestore entfernt (jetzt rein lokal).
                        }
                    } else {
                        // Keine Familie in Firestore gefunden – kein leaveFamily() hier!
                        // Firestore kann kurz null zurückgeben (Race nach createFamily).
                        // Self-Healing läuft über den Members-Flow-Collector (PERMISSION_DENIED-Guard).
                        if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                            android.util.Log.w("FamilyViewModel", "refreshData: getUserFamily returned null, keeping local state")
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, e.localizedMessage ?: getApplication<Application>().getString(R.string.add_member_unknown))
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * ADMIN/DEBUG: Setzt das DayProfile des heutigen Wochentags so,
     * dass der Wecker in ~5 Minuten klingelt.
     * DayOfWeek.value: 1=Mo ... 7=So (java.time)
     */
    fun triggerRefresh() {
        refreshData()
        triggerMemberReset()
        recalculateSchedule()
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
            val resetThreshold = member.latestWakeUp.plusHours(2)
            val isPastResetThreshold = now.isAfter(resetThreshold)

            if (isPastResetThreshold && member.lastResetDate != today) {
                val isUnclaimed = member.claimedByUserId == null
                val newIsPaused = if (isUnclaimed) false else member.isPaused

                val updated = member.copy(
                    isPaused = newIsPaused,
                    isAwakeToday = false,
                    lastResetDate = today
                )
                if (member.id == myMemberId.value) {
                    prefsRepo.setAwakeToday(false)
                }
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
     * Löst das korrekte DayProfile für den nächsten Alarm-Tag auf.
     *
     * Logik:
     * - Wenn das heutige DayProfile aktiv ist UND die latestWakeUp noch nicht erreicht wurde
     *   → verwende das heutige Profil.
     * - Sonst: nächsten Tag prüfen (morgen).
     * - Ist das Profil des Zieltags inaktiv → member.isPaused = true (kein Wecker).
     * - Sind keine dayProfiles vorhanden → unveranderter Member (Fallback auf Root-Felder).
     */
    private fun resolveEffectiveMember(member: FamilyMember): FamilyMember {
        val profiles = member.dayProfiles ?: return member
        val now = LocalTime.now()
        val today = LocalDate.now()

        // Heutiges Profil prüfen
        val todayDow = today.dayOfWeek.value // 1=Mo … 7=So
        val todayProfile = profiles[todayDow]

        val targetDate = if (todayProfile != null && todayProfile.isActive && now.isBefore(todayProfile.latestWakeUp)) {
            // Heute ist noch Zeit für den Wecker
            today
        } else {
            // Heute vorbei oder deaktiviert → morgen prüfen
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
            val uid = auth.currentUser?.uid ?: return@launch
            val result = repository.deleteFamily(currentFamilyId, uid)
            if (result.isSuccess) {
                prefsRepo.setFamilyId(null)
                prefsRepo.setJoinCode(null)
                prefsRepo.setFamilyName(null)
                prefsRepo.setMyMemberId(null)
                onComplete(true)
            } else {
                _errorMessage.value = UiText.StringResource(R.string.error_delete_family, result.exceptionOrNull()?.localizedMessage ?: getApplication<Application>().getString(R.string.add_member_unknown))
                onComplete(false)
            }
        }
    }

    fun requestAdminStatsReport(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.requestAdminStatsReport()
            if (result.isSuccess) {
                onComplete(true)
            } else {
                _errorMessage.value = UiText.StringResource(R.string.error_report_failed, result.exceptionOrNull()?.localizedMessage ?: getApplication<Application>().getString(R.string.error_label))
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
            _isSyncing.value = true
            try {
                // Eigenen Member-Datensatz UND User-Mapping atomar löschen.
                val result = if (currentFamilyId != null && currentMemberId != null) {
                    repository.leaveFamilyBatch(uid, currentFamilyId, currentMemberId)
                } else {
                    // Security Fix
                    Result.success(Unit)
                }
                
                if (result.isSuccess) {
                    prefsRepo.setFamilyId(null)
                    prefsRepo.setJoinCode(null)
                    prefsRepo.setFamilyName(null)
                    prefsRepo.setMyMemberId(null)
                    prefsRepo.setMyMemberName(null)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: getApplication<Application>().getString(R.string.error_leave_failed)
                    _errorMessage.value = UiText.StringResource(R.string.error_sync_failed, errorMsg)
                }
            } catch (e: Exception) {
                _errorMessage.value = UiText.StringResource(R.string.error_system, e.localizedMessage ?: getApplication<Application>().getString(R.string.add_member_unknown))
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun recalculateSchedule() {
        val currentMembers = _members.value
        val alarmsOn = isAlarmEnabled.value

        if (currentMembers.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    val currentMyMemberId = myMemberId.value
                    val rawMembers = if (alarmsOn) {
                        currentMembers
                    } else {
                        currentMembers.filter { it.id != currentMyMemberId }
                    }

                    // Wochentag-spezifische Felder aus dayProfiles auflösen (nächster Alarm-Tag)
                    val calculationMembers = rawMembers.map { resolveEffectiveMember(it) }

                    if (calculationMembers.none { !it.isPaused }) {
                        android.util.Log.w("FamWake_Alarm", "recalculate: all members paused – checking grace period before cancel")
                        _schedule.value = FamilySchedule(emptyList(), null, true, ScheduleMessage.NoActiveSchedule)
                        // Grace-Period: Keinen Alarm abbrechen, wenn der aktuelle User's Alarm
                        // in den letzten 5 Minuten hätte klingeln sollen.
                        // (resolveEffectiveMember sieht ihn als "paused", weil todayProfile.latestWakeUp
                        // bereits vorbei ist – aber der echte Alarm könnte noch feuern.)
                        val myMember = currentMembers.find { it.id == currentMyMemberId }
                        val myProfile = myMember?.dayProfiles?.get(LocalDate.now().dayOfWeek.value)
                        val myWakeUpToday = myProfile?.latestWakeUp
                        val inGrace = if (myWakeUpToday != null) {
                            val todayAlarmMillis = LocalDateTime.of(LocalDate.now(), myWakeUpToday)
                                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                            val millisSince = System.currentTimeMillis() - todayAlarmMillis
                            millisSince in 0..300_000
                        } else false

                        if (inGrace) {
                            android.util.Log.d("FamWake_Alarm", "recalculate: GRACE PERIOD – skipping cancel (alarm just fired)")
                        } else {
                            android.util.Log.w("FamWake_Alarm", "recalculate: cancelling all alarms (all paused, outside grace)")
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
                            android.util.Log.w("FamWake_Alarm", "recalculate: Applying FALLBACK alarms for invalid schedule")
                        }
                        applyAlarms(result)
                    } else {
                        android.util.Log.w("FamWake_Alarm", "recalculate: cancelling alarms – alarmsOn=$alarmsOn, hasSchedules=${result.memberSchedules.isNotEmpty()}")
                        currentMembers.forEach { alarmScheduler.cancelWakeUp(it.id) }
                    }
                } catch (e: Exception) {
                    _errorMessage.value = UiText.StringResource(R.string.error_calculate_schedule, e.localizedMessage ?: getApplication<Application>().getString(R.string.add_member_unknown))
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

        val currentMyMemberId = myMemberId.value ?: run {
            android.util.Log.w("FamWake_Alarm", "applyAlarms: myMemberId is null, skipping")
            return
        }
        if (schedule.memberSchedules.isEmpty()) return

        for (memberSchedule in schedule.memberSchedules) {
            if (memberSchedule.member.id == currentMyMemberId) {
                val wakeUpTime = memberSchedule.wakeUpTime
                val targetDate = if (LocalTime.now().isAfter(wakeUpTime)) tomorrow else today

                // RACE-CONDITION-GUARD (müss als ALLERERSTER Check laufen):
                // Wenn targetDate == morgen, bedeutet das: die heutige Weckzeit ist gerade eben vorbei.
                // In diesem Moment könnte AlarmManager noch dabei sein, den Alarm zu feuern.
                // Jeder cancelWakeUp oder scheduleWakeUp-Aufruf mit demselben requestCode würde
                // den startenden Alarm abwürgen. Deshalb: 5 Minuten Grace Period – in dieser Zeit
                // wird NICHTS verändert (weder Cancel noch Reschedule).
                if (targetDate == tomorrow) {
                    val todayAlarmMillis = LocalDateTime.of(today, wakeUpTime)
                        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val millisSinceTodayAlarm = System.currentTimeMillis() - todayAlarmMillis
                    if (millisSinceTodayAlarm in 0..300_000) { // 0..5 Minuten
                        return
                    }
                }

                val dayOfWeek = targetDate.dayOfWeek.value
                val dayProfile = memberSchedule.member.dayProfiles?.get(dayOfWeek)

                if (dayProfile != null && !dayProfile.isActive) {
                    android.util.Log.w("FamWake_Alarm", "applyAlarms: day $dayOfWeek is inactive, cancelling alarm")
                    alarmScheduler.cancelWakeUp(currentMyMemberId)
                    lastScheduledAlarmMillis = null
                    // Zeitplan-Anzeige ebenfalls zurücksetzen, damit die UI keinen
                    // veralteten Plan zeigt wenn der nächste Tag inaktiv ist.
                    _schedule.value = FamilySchedule(emptyList(), null, true, ScheduleMessage.NoActiveSchedule)
                    return
                }

                val targetDateTime = LocalDateTime.of(targetDate, wakeUpTime)
                val newAlarmMillis = targetDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

                if (newAlarmMillis == lastScheduledAlarmMillis) return
                
                // Nutze lokalen "Bereits wach"-Status
                if (isAwakeTodayLocal.value && targetDate == today) {
                    android.util.Log.w("FamWake_Alarm", "applyAlarms: isAwakeToday=true for today, cancelling alarm")
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
                        android.util.Log.e("FamWake_Alarm", "applyAlarms: SCHEDULE_EXACT_ALARM permission denied!")
                        _errorMessage.value = UiText.StringResource(R.string.error_alarm_permission)
                    }
                )
                lastScheduledAlarmMillis = newAlarmMillis
                android.util.Log.i("FamWake_Alarm", "applyAlarms: alarm SET for $targetDateTime")
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

    override fun onCleared() {
        super.onCleared()
        // SharedPreferences-Listener deregistrieren um Memory Leaks zu vermeiden
        prefsRepo.unregisterListener()
    }
}
