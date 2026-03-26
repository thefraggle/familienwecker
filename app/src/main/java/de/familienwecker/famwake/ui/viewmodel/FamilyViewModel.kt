package de.familienwecker.famwake.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.familienwecker.famwake.algorithm.Scheduler
import de.familienwecker.famwake.alarm.AlarmScheduler
import de.familienwecker.famwake.data.AppError
import de.familienwecker.famwake.data.FirebaseRepository
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
    internal val alarmScheduler = AlarmScheduler(application)
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
    val tooltipAwakeSeen: StateFlow<Boolean>      = _tooltipsSeen.map { it["TOOLTIP_SEEN_AWAKE"] ?: false }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val tooltipDragSeen: StateFlow<Boolean>       = _tooltipsSeen.map { it["TOOLTIP_SEEN_DRAG"] ?: false }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val tooltipWakeWindowSeen: StateFlow<Boolean> = _tooltipsSeen.map { it["TOOLTIP_SEEN_WAKE_WINDOW"] ?: false }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val tooltipBathroomSeen: StateFlow<Boolean>   = _tooltipsSeen.map { it["TOOLTIP_SEEN_BATHROOM"] ?: false }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val tooltipInviteSeen: StateFlow<Boolean>     = _tooltipsSeen.map { it["TOOLTIP_SEEN_INVITE"] ?: false }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val tooltipSwitchSeen: StateFlow<Boolean>     = _tooltipsSeen.map { it["TOOLTIP_SEEN_SWITCH"] ?: false }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val tooltipWeekdaysSeen: StateFlow<Boolean>   = _tooltipsSeen.map { it["TOOLTIP_SEEN_WEEKDAYS"] ?: false }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setOnboardingCompleted(completed: Boolean) = appSettings.setOnboardingCompleted(completed)
    fun setTooltipsEnabled(enabled: Boolean)        = appSettings.setTooltipsEnabled(enabled)
    fun markTooltipSeen(key: String)                = appSettings.setTooltipSeen(key, true)
    fun resetAllTooltips() {
        listOf("TOOLTIP_SEEN_AWAKE", "TOOLTIP_SEEN_DRAG", "TOOLTIP_SEEN_WAKE_WINDOW",
               "TOOLTIP_SEEN_BATHROOM", "TOOLTIP_SEEN_INVITE", "TOOLTIP_SEEN_SWITCH", "TOOLTIP_SEEN_WEEKDAYS").forEach {
            appSettings.setTooltipSeen(it, false)
        }
    }

    val tooltipKeyAwake      get() = "TOOLTIP_SEEN_AWAKE"
    val tooltipKeyDrag       get() = "TOOLTIP_SEEN_DRAG"
    val tooltipKeyWakeWindow get() = "TOOLTIP_SEEN_WAKE_WINDOW"
    val tooltipKeyBathroom   get() = "TOOLTIP_SEEN_BATHROOM"
    val tooltipKeyInvite     get() = "TOOLTIP_SEEN_INVITE"
    val tooltipKeySwitch     get() = "TOOLTIP_SEEN_SWITCH"
    val tooltipKeyWeekdays   get() = "TOOLTIP_SEEN_WEEKDAYS"

    // ── UI-State ──────────────────────────────────────────────────────────────

    internal val _members = MutableStateFlow<PersistentList<FamilyMember>>(persistentListOf())
    val members: StateFlow<PersistentList<FamilyMember>> = _members.asStateFlow()

    internal val _schedule = MutableStateFlow<FamilySchedule?>(null)
    val schedule: StateFlow<FamilySchedule?> = _schedule.asStateFlow()

    internal val _errorMessage = MutableStateFlow<UiText?>(null)
    val errorMessage: StateFlow<UiText?> = _errorMessage.asStateFlow()

    fun clearErrorMessage() { _errorMessage.value = null }
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

    internal val _pendingPauseIds = MutableStateFlow<Set<String>>(emptySet())
    val pendingPauseIds: StateFlow<Set<String>> = _pendingPauseIds.asStateFlow()

    internal val _isSendingFeedback = MutableStateFlow(false)
    val isSendingFeedback = _isSendingFeedback.asStateFlow()

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

    // ── Connectivity ──────────────────────────────────────────────────────────

    private val connectivityManager = application.getSystemService(ConnectivityManager::class.java)
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            offlineDebounceJob?.cancel()
            _isOffline.value = false
        }
        override fun onLost(network: Network) {
            offlineDebounceJob?.cancel()
            offlineDebounceJob = viewModelScope.launch {
                try { delay(3000) } catch (_: Exception) {}
                _isOffline.value = true
            }
        }
    }

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

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try { connectivityManager.registerNetworkCallback(networkRequest, networkCallback) } catch (_: Exception) {}

        // Offline-Status beim Start einmalig initialisieren (bevor der erste NetworkCallback kommt)
        _isOffline.value = !NetworkUtils.isOnline(getApplication())

        viewModelScope.launch {
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
                    } else if (claimedByMe == null && myMemberId.value != null) {
                        appSettings.setMyMemberId(null)
                        appSettings.setMyMemberName(null)
                    }
                }
                recalculateSchedule()
            }
        }

        // Sync-Datenfluss: Firestore → Room
        viewModelScope.launch {
            try {
                familyId.collect { currentFamilyId ->
                    membersJob?.cancel()
                    syncStatusJob?.cancel()
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
                        memberRepository.clearCache()
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
                myMemberId.collect { id ->
                    if (id == null && isAlarmEnabled.value && !isFirstEmission) setAlarmEnabled(false)
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
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (_: Exception) {}
        membersJob?.cancel()
        syncStatusJob?.cancel()
        offlineDebounceJob?.cancel()
    }
}
