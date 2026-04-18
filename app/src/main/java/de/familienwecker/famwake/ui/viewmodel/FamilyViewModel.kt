package de.familienwecker.famwake.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.familienwecker.famwake.algorithm.Scheduler
import de.familienwecker.famwake.alarm.AlarmScheduler
import de.familienwecker.famwake.data.AppError
import de.familienwecker.famwake.data.FirebaseRepository
import de.familienwecker.famwake.util.TooltipKeys
import de.familienwecker.famwake.util.DeviceTrustLevel
import de.familienwecker.famwake.util.DeviceTrustManager
import de.familienwecker.famwake.data.IFirebaseRepository
import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.model.FamilySchedule
import de.familienwecker.famwake.model.ScheduleMessage
import de.familienwecker.famwake.model.toJavaLocalTime
import de.familienwecker.famwake.model.toJavaLocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
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
import de.familienwecker.famwake.FamWakeApplication
import de.familienwecker.famwake.ui.util.UiText
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import de.familienwecker.famwake.util.NetworkUtils
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Zentraler State-Halter für die FamWake-App.
 *
 * Die Geschäftslogik ist in folgende Extension-Dateien ausgelagert:
 *  - FamilyViewModel+Alarm.kt    → Alarm-Berechnung, applyAlarms, snooze
 *  - FamilyViewModel+Member.kt   → CRUD-Operationen für Familienmitglieder
 *  - FamilyViewModel+Family.kt   → Familie erstellen/beitreten/verlassen
 *  - FamilyViewModel+Settings.kt → Sprache, Theme, Feedback, Admin
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FamilyViewModel(
    application: Application,
    internal val repository: IFirebaseRepository = FirebaseRepository(),
    internal val appSettings: de.familienwecker.famwake.data.AppSettings = (application as FamWakeApplication).appSettings,
    internal val memberRepository: de.familienwecker.famwake.data.MemberRepository = (application as FamWakeApplication).memberRepository
) : AndroidViewModel(application) {

    // ── Interne Helfer ────────────────────────────────────────────────────────

    internal val scheduler = Scheduler()
    internal val alarmScheduler: de.familienwecker.famwake.alarm.AlarmPlatformScheduler = AlarmScheduler(application)
    internal val auth = Firebase.auth

    /** UID des aktuell eingeloggten Users (null wenn nicht eingeloggt). */
    val currentUserId: String?
        get() = auth.currentUser?.uid

    // ── Offline-Schreib-Hinweis ───────────────────────────────────────────────

    internal val _offlineWriteHint = MutableStateFlow<UiText?>(null)
    val offlineWriteHint: StateFlow<UiText?> = _offlineWriteHint.asStateFlow()
    fun clearOfflineWriteHint() { _offlineWriteHint.value = null }

    /** Interner Zugang zu viewModelScope für Extension-Funktionen. */
    internal val scope get() = viewModelScope

    // ── Settings-Flows (aus AppSettings) ─────────────────────────────────────

    val myMemberId: StateFlow<String?> = appSettings.myMemberId
    val alarmSoundUri: StateFlow<String?> = appSettings.alarmSoundUri
    val familyId: StateFlow<String?> = appSettings.familyId
    val joinCode: StateFlow<String?> = appSettings.joinCode
    val familyName: StateFlow<String?> = appSettings.familyName
    val language: StateFlow<String> = appSettings.language
    val themePreference: StateFlow<String> = appSettings.theme
    val isAlarmEnabled: StateFlow<Boolean> = appSettings.isAlarmEnabled
    val isAwakeTodayLocal: StateFlow<Boolean> = appSettings.isAwakeToday
    val onboardingCompleted: StateFlow<Boolean> = appSettings.onboardingCompleted

    // ── Tooltip-Flows ────────────────────────────────────────────────────────

    val tooltipsEnabled: StateFlow<Boolean> = appSettings.tooltipsEnabled
    private val _tooltipsSeen = appSettings.tooltipsSeen
    val tooltipAwakeSeen: StateFlow<Boolean>      = _tooltipsSeen.map { it[TooltipKeys.AWAKE]       ?: false }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val tooltipDragSeen: StateFlow<Boolean>       = _tooltipsSeen.map { it[TooltipKeys.DRAG]        ?: false }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val tooltipWakeWindowSeen: StateFlow<Boolean> = _tooltipsSeen.map { it[TooltipKeys.WAKE_WINDOW] ?: false }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val tooltipBathroomSeen: StateFlow<Boolean>   = _tooltipsSeen.map { it[TooltipKeys.BATHROOM]    ?: false }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val tooltipInviteSeen: StateFlow<Boolean>     = _tooltipsSeen.map { it[TooltipKeys.INVITE]      ?: false }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val tooltipSwitchSeen: StateFlow<Boolean>     = _tooltipsSeen.map { it[TooltipKeys.SWITCH]      ?: false }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val tooltipWeekdaysSeen: StateFlow<Boolean>   = _tooltipsSeen.map { it[TooltipKeys.WEEKDAYS]    ?: false }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val tooltipAlarmSoundSeen: StateFlow<Boolean> = _tooltipsSeen.map { it[TooltipKeys.ALARM_SOUND] ?: false }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setOnboardingCompleted(completed: Boolean) = appSettings.setOnboardingCompleted(completed)
    fun setTooltipsEnabled(enabled: Boolean)        = appSettings.setTooltipsEnabled(enabled)
    fun markTooltipSeen(key: String)                = appSettings.setTooltipSeen(key, true)
    fun resetAllTooltips() { TooltipKeys.ALL.forEach { appSettings.setTooltipSeen(it, false) } }

    val tooltipKeyAwake      get() = TooltipKeys.AWAKE
    val tooltipKeyDrag       get() = TooltipKeys.DRAG
    val tooltipKeyWakeWindow get() = TooltipKeys.WAKE_WINDOW
    val tooltipKeyBathroom   get() = TooltipKeys.BATHROOM
    val tooltipKeyInvite     get() = TooltipKeys.INVITE
    val tooltipKeySwitch     get() = TooltipKeys.SWITCH
    val tooltipKeyWeekdays   get() = TooltipKeys.WEEKDAYS
    val tooltipKeyAlarmSound get() = TooltipKeys.ALARM_SOUND


    // ── UI-State ──────────────────────────────────────────────────────────────

    internal val _members = MutableStateFlow<PersistentList<FamilyMember>>(persistentListOf())
    val members: StateFlow<PersistentList<FamilyMember>> = _members.asStateFlow()

    internal val _schedule = MutableStateFlow<FamilySchedule?>(null)
    val schedule: StateFlow<FamilySchedule?> = _schedule.asStateFlow()

    internal val _errorMessage = MutableStateFlow<UiText?>(null)
    val errorMessage: StateFlow<UiText?> = _errorMessage.asStateFlow()

    fun setError(message: UiText) { _errorMessage.value = message }
    fun clearError() { _errorMessage.value = null }



    internal val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    internal val _syncStatus = MutableStateFlow(de.familienwecker.famwake.model.SyncStatus())
    val syncStatus: StateFlow<de.familienwecker.famwake.model.SyncStatus> = _syncStatus.asStateFlow()

    internal val _pendingJoinCode = MutableStateFlow<String?>(null)
    val pendingJoinCode: StateFlow<String?> = _pendingJoinCode.asStateFlow()

    internal val _isJoiningFamily = MutableStateFlow(false)
    val isJoiningFamily: StateFlow<Boolean> = _isJoiningFamily.asStateFlow()

    internal val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    // ── Geräteintegrität (Play Integrity API) ─────────────────────────────────

    private val _deviceTrustLevel = MutableStateFlow(DeviceTrustLevel.UNKNOWN)
    val deviceTrustLevel: StateFlow<DeviceTrustLevel> = _deviceTrustLevel.asStateFlow()

    /** true = gerootetes/manipuliertes Gerät → Firebase-Sync deaktiviert. */
    val isSyncBlocked: StateFlow<Boolean> = _deviceTrustLevel
        .map { it == DeviceTrustLevel.UNTRUSTED }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    internal val _pendingPauseIds = MutableStateFlow<Set<String>>(emptySet())
    val pendingPauseIds: StateFlow<Set<String>> = _pendingPauseIds.asStateFlow()

    internal val _isSendingFeedback = MutableStateFlow(false)
    val isSendingFeedback = _isSendingFeedback.asStateFlow()

    // Verhindert den kurzen "Kein Profil"-Flash während der Auto-Claim läuft
    internal val _isAutoClaimInProgress = MutableStateFlow(false)
    val isAutoClaimInProgress: StateFlow<Boolean> = _isAutoClaimInProgress.asStateFlow()

    internal val _feedbackError = MutableStateFlow<UiText?>(null)
    val feedbackError = _feedbackError.asStateFlow()

    internal val _feedbackSubmitted = MutableStateFlow(false)
    val feedbackSubmitted = _feedbackSubmitted.asStateFlow()

    // ── Interne Jobs & Caches ─────────────────────────────────────────────────

    private var offlineDebounceJob: Job? = null
    private var membersJob: Job? = null
    private var syncStatusJob: Job? = null
    internal var lastScheduledAlarmMillis: Long? = null
    internal val memberDebounceJobs = mutableMapOf<String, Job>()
    internal var alarmToggleJob: Job? = null
    internal var scheduleJob: Job? = null

    // ── Connectivity ──────────────────────────────────────────────────────────

    private val networkMonitor = de.familienwecker.famwake.util.createNetworkMonitor(application)

    // ── Admin-Status ──────────────────────────────────────────────────────────

    private val _familyCreatorId = MutableStateFlow<String?>(null)
    val familyCreatorId: StateFlow<String?> = _familyCreatorId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isGlobalAdmin: StateFlow<Boolean> = repository.getAuthStateFlow()
        .flatMapLatest { user ->
            user?.uid?.let { uid ->
                repository.checkIsGlobalAdminFlow(uid)
                    .catch { e ->
                        if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                            android.util.Log.e("FamilyViewModel", "isGlobalAdmin Flow Error: ${e.message}")
                        }
                        emit(false)
                    }
            } ?: flowOf(false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)


    val isAdmin: StateFlow<Boolean> = combine(isGlobalAdmin, _familyCreatorId) { isGlobal, creatorId ->
        isGlobal || (auth.currentUser?.uid != null && auth.currentUser?.uid == creatorId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // ── Snooze ────────────────────────────────────────────────────────────────

    val snoozeUntil: StateFlow<java.time.LocalDateTime?> = appSettings.snoozeUntil
        .map { it?.toJavaLocalDateTime() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        networkMonitor.startMonitoring()
        viewModelScope.launch(Dispatchers.IO) {
            networkMonitor.isOnline.collect { online ->
                _isOffline.value = !online
            }
        }

        // Integritätscheck beim Start – asynchron, blockiert den Init nicht.
        // Ergebnis wird in TelemetryDeck geloggt ("integrity.check").
        // Im Monitoring-Modus (v1.7.7) ist isSyncBlocked immer false.
        viewModelScope.launch(Dispatchers.IO) {
            val trustManager = DeviceTrustManager(getApplication())
            _deviceTrustLevel.value = trustManager.checkTrust()
        }

        viewModelScope.launch(Dispatchers.IO) {
            memberRepository.members.collect { membersList ->
                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.d("FamilyViewModel", "UI Source: Received ${membersList.size} members from Room")
                }
                val checkedMembers = checkAndResetMembers(membersList)
                _members.value = checkedMembers.toPersistentList()

                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val claimedByMe = checkedMembers.find { it.claimedByUserId == uid }
                    if (claimedByMe != null && claimedByMe.id != myMemberId.value) {
                        appSettings.setMyMemberId(claimedByMe.id)
                        appSettings.setMyMemberName(claimedByMe.name)
                    } else if (claimedByMe == null && myMemberId.value != null && checkedMembers.isNotEmpty()) {
                        // Nur zurücksetzen wenn wir echte Daten haben – eine leere Liste bedeutet
                        // dass clearCache() gerade lief und der Firestore-Sync noch aussteht.
                        // Leere Liste → false positive: würde myMemberId fälschlich nullen und
                        // in Folge setAlarmEnabled(false) triggern (Startup-Race-Condition).
                        appSettings.setMyMemberId(null)
                        appSettings.setMyMemberName(null)
                    }
                }
                recalculateSchedule()
            }
        }

        // Sync-Datenfluss: Firestore → Room
        viewModelScope.launch(Dispatchers.IO) {
            try {
                familyId.collect { currentFamilyId ->
                    membersJob?.cancel()
                    syncStatusJob?.cancel()
                    // Sofort leeren: verhindert dass Mitglieder/Zeitplan der Vorgänger-Familie
                    // kurz angezeigt werden, während der neue Firestore-Sync noch läuft.
                    memberRepository.clearCache()
                    _schedule.value = null
                    if (!currentFamilyId.isNullOrBlank()) {
                        if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                            android.util.Log.d("FamilyViewModel", "Start sync for family: $currentFamilyId")
                        }
                        refreshData()
                        launch {
                            try {
                                val data = repository.getFamilyData(currentFamilyId)
                                _familyCreatorId.value = data?.createdByUserId
                            } catch (e: Exception) {
                                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                                    android.util.Log.e("FamilyViewModel", "Error loading family creator: ${e.message}")
                                }
                            }
                        }
                        syncStatusJob = launch {
                            try {
                                repository.getSyncStatusFlow(currentFamilyId).collect { status ->
                                    _syncStatus.value = status
                                    // SyncStatus dient nur als Info-Quelle; isOffline wird
                                    // ausschließlich vom NetworkCallback gesteuert (P4: zentralisiert).
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                                    android.util.Log.e("FamilyViewModel", "SyncStatus Flow Error: ${e.message}", e)
                                }
                                _isSyncing.value = false
                            }
                        }
                        membersJob = launch {
                            try {
                                repository.getFamilyMembersFlow(currentFamilyId).collect { membersList ->
                                    if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                                        android.util.Log.d("FamilyViewModel", "Sync: Received ${membersList.size} members from Firestore -> Caching in Room")
                                    }
                                    memberRepository.cacheMembers(membersList)
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                                    android.util.Log.e("FamilyViewModel", "Members Flow Error: ${e.message}", e)
                                }
                                _errorMessage.value = UiText.StringResource(de.familienwecker.famwake.R.string.error_sync_failed, e.localizedMessage ?: "")
                            }
                        }
                    } else {
                        // Cache bereits oben geleert – kein weiterer Action nötig
                    }
                }
            } catch (e: Exception) {
                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.e("FamilyViewModel", "Outer Init Error: ${e.message}", e)
                }
                _errorMessage.value = UiText.StringResource(R.string.error_system, e.localizedMessage ?: getApplication<Application>().getString(R.string.add_member_unknown))
            }
        }

        // Observer MyMemberId
        viewModelScope.launch {
            try {
                var isFirstEmission = true
                var previousMemberId: String? = null
                myMemberId.collect { id ->
                    if (!isFirstEmission) {
                        when {
                            // Logout / Unclaim: State sichern, Alarm abschalten
                            id == null && previousMemberId != null -> {
                                appSettings.setAlarmStateBeforeLogout(isAlarmEnabled.value)
                                if (isAlarmEnabled.value) setAlarmEnabled(false)
                            }
                            // Login / Claim: gesicherten State wiederherstellen
                            id != null && previousMemberId == null -> {
                                val savedState = appSettings.alarmStateBeforeLogout.value
                                if (savedState) {
                                    setAlarmEnabled(true)
                                    // State-Sicherung zurücksetzen nach Wiederherstellung
                                    appSettings.setAlarmStateBeforeLogout(false)
                                }
                            }
                        }
                    }
                    isFirstEmission = false
                    previousMemberId = id
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
                isAlarmEnabled.collect { recalculateSchedule() }
            } catch (e: Exception) {
                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.w("FamilyViewModel", "isAlarmEnabled observer error: ${e.message}")
                }
            }
        }

        // Snooze-Cleanup
        viewModelScope.launch {
            val currentSnooze = appSettings.snoozeUntil.value
            if (currentSnooze != null && currentSnooze.toJavaLocalDateTime().isBefore(java.time.LocalDateTime.now().minusMinutes(30))) {
                appSettings.setSnoozeUntil(null)
            }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        try { networkMonitor.stopMonitoring() } catch (_: Exception) {}
        membersJob?.cancel()
        syncStatusJob?.cancel()
        offlineDebounceJob?.cancel()
        scheduleJob?.cancel()
    }
}
