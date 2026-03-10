package de.familienwecker.famwake.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.familienwecker.famwake.algorithm.Scheduler
import de.familienwecker.famwake.alarm.AlarmScheduler
import de.familienwecker.famwake.data.FirebaseRepository
import de.familienwecker.famwake.data.PreferencesRepository
import de.familienwecker.famwake.model.FamilyMember
import de.familienwecker.famwake.model.FamilySchedule
import de.familienwecker.famwake.model.ScheduleMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

class FamilyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FirebaseRepository()
    private val scheduler = Scheduler()
    private val alarmScheduler = AlarmScheduler(application)
    private val prefsRepo = PreferencesRepository(application)
    private val auth = FirebaseAuth.getInstance()
    private val whatsNewManager = WhatsNewManager(application)

    val myMemberId: StateFlow<String?> = prefsRepo.myMemberId
    val alarmSoundUri: StateFlow<String?> = prefsRepo.alarmSoundUri
    val familyId: StateFlow<String?> = prefsRepo.familyId
    val joinCode: StateFlow<String?> = prefsRepo.joinCode
    val familyName: StateFlow<String?> = prefsRepo.familyName
    val language: StateFlow<String> = prefsRepo.language
    val themePreference: StateFlow<String> = prefsRepo.themePreference
    val isAlarmEnabled: StateFlow<Boolean> = prefsRepo.isAlarmEnabled

    private val _members = MutableStateFlow<List<FamilyMember>>(emptyList())
    val members: StateFlow<List<FamilyMember>> = _members.asStateFlow()

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

    // O5: Offline-Debounce – CloudOff-Icon erst nach 3s ohne Verbindung zeigen
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()
    private var offlineDebounceJob: Job? = null

    private var membersJob: Job? = null
    private var syncStatusJob: Job? = null
    private var alarmEnabledJob: Job? = null

    // O4: Zuletzt gesetzten Alarm-Zeitstempel merken
    private var lastScheduledAlarmMillis: Long? = null

    init {
        // 1. Observe FamilyId and load members accordingly
        viewModelScope.launch {
            try {
                familyId.collect { currentFamilyId ->
                    membersJob?.cancel()
                    syncStatusJob?.cancel()
                    alarmEnabledJob?.cancel()
                    if (!currentFamilyId.isNullOrBlank()) {
                        // O6: refreshData nur aufrufen wenn familyId ein echter Wert ist (kein leerer Dummy)
                        refreshData()

                        syncStatusJob = launch {
                            try {
                                repository.getSyncStatusFlow(currentFamilyId).collect { status ->
                                    _syncStatus.value = status
                                    // O5: Offline-Status mit Debounce setzen
                                    if (status.isFromCache && !status.hasPendingWrites) {
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
                                    _members.value = checkedMembers

                                    // Auto-Sync MyMemberId from Cloud (multi-device resilience)
                                    val uid = auth.currentUser?.uid
                                    if (uid != null) {
                                        val claimedByMe = checkedMembers.find { it.claimedByUserId == uid }
                                        if (claimedByMe != null && claimedByMe.id != myMemberId.value) {
                                            prefsRepo.setMyMemberId(claimedByMe.id)
                                        } else if (claimedByMe == null && myMemberId.value != null) {
                                            prefsRepo.setMyMemberId(null)
                                        }
                                    }

                                    recalculateSchedule()
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                // Self-Healing: Bei Permission Denied (veraltete FamilyId) lokal aufräumen
                                if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                                    android.util.Log.w("FamilyViewModel", "Permission Denied beim Laden der Mitglieder - lösche lokalen Zustand")
                                    leaveFamily()
                                    _members.value = emptyList()
                                } else {
                                    _errorMessage.value = UiText.StringResource(R.string.error_load_members, e.localizedMessage ?: "Unknown")
                                }
                            }
                        }
                        alarmEnabledJob = launch {
                            try {
                                repository.getFamilyAlarmEnabledFlow(currentFamilyId).collect { enabled ->
                                    prefsRepo.setAlarmEnabled(enabled)
                                }
                            } catch (e: Exception) {
                                // Silent error for alarm sync
                            }
                        }
                    } else {
                        _members.value = emptyList()
                        recalculateSchedule()
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = UiText.StringResource(R.string.error_system, e.localizedMessage ?: "Unknown")
            }
        }

        // 2. Observer MyMemberId
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

        // 3. Observer Global Alarm Toggle
        viewModelScope.launch {
            try {
                isAlarmEnabled.collect {
                    recalculateSchedule()
                }
            } catch (e: Exception) {
                // Ignore silent errors in alarm toggle
            }
        }

        checkWhatsNew()
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
            val result = repository.createFamily(familyName, uid)
            result.onSuccess { pair ->
                repository.saveUserFamily(uid, pair.first, pair.second)
                prefsRepo.setFamilyId(pair.first)
                prefsRepo.setJoinCode(pair.second)
                prefsRepo.setFamilyName(familyName)
                onComplete(true)
            }.onFailure { error ->
                _errorMessage.value = UiText.StringResource(R.string.error_create_family, error.localizedMessage ?: "Unknown")
                onComplete(false)
            }
        }
    }

    fun joinFamily(code: String, onComplete: (Boolean) -> Unit) {
        _errorMessage.value = null

        // O7: Nicht beitreten wenn bereits in dieser Familie
        if (code.equals(joinCode.value, ignoreCase = true)) {
            onComplete(true)
            return
        }

        viewModelScope.launch {
            val result = repository.joinFamilyByCode(code)
            result.onSuccess { pair ->
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    repository.saveUserFamily(uid, pair.first, pair.second)
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
                _errorMessage.value = UiText.StringResource(R.string.error_invalid_code, error.localizedMessage ?: "Unknown")
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

        // O7: Nicht beitreten wenn bereits in dieser Familie
        if (code.equals(joinCode.value, ignoreCase = true)) {
            _pendingJoinCode.value = null
            onComplete(true)
            return
        }

        val uid = auth.currentUser?.uid

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                if (uid != null) {
                    repository.removeUserFamily(uid)
                }
                val result = repository.joinFamilyByCode(code)
                result.onSuccess { pair ->
                    if (uid != null) {
                        repository.saveUserFamily(uid, pair.first, pair.second)
                    }
                    val fetchedName = repository.getFamilyName(pair.first)
                    val oldFamilyId = familyId.value
                    prefsRepo.setFamilyId(pair.first)
                    prefsRepo.setJoinCode(pair.second)
                    prefsRepo.setFamilyName(fetchedName)

                    if (oldFamilyId != pair.first) {
                        prefsRepo.setMyMemberId(null)
                    }

                    _pendingJoinCode.value = null
                    _showJoinSuccess.value = true
                    onComplete(true)
                }.onFailure { error ->
                    _errorMessage.value = UiText.StringResource(R.string.error_invalid_code, error.localizedMessage ?: "Unknown")
                    _pendingJoinCode.value = null
                    prefsRepo.setFamilyId(null)
                    prefsRepo.setJoinCode(null)
                    prefsRepo.setFamilyName(null)
                    prefsRepo.setMyMemberId(null)
                    onComplete(false)
                }
            } catch (e: Exception) {
                _errorMessage.value = UiText.StringResource(R.string.error_system, e.localizedMessage ?: "Unknown")
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
        // B10: Fallback-Name aus strings.xml statt hardcodiertem "Papa/Mama"
        val userName = auth.currentUser?.displayName
            ?: getApplication<Application>().getString(R.string.settings_fallback_username)

        viewModelScope.launch {
            if (currentMyMemberId != null && currentMyMemberId != id) {
                repository.unclaimMember(currentFamilyId, currentMyMemberId, userId)
            }

            if (id != null) {
                val success = repository.claimMember(currentFamilyId, id, userId, userName)
                if (success) {
                    prefsRepo.setMyMemberId(id)
                    prefsRepo.setAlarmEnabled(true)
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } else {
                prefsRepo.setMyMemberId(null)
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

    fun setAlarmEnabled(enabled: Boolean) {
        if (enabled && myMemberId.value == null) return

        val currentFamilyId = familyId.value
        if (currentFamilyId != null) {
            viewModelScope.launch {
                repository.updateFamilyAlarmEnabled(currentFamilyId, enabled)
            }
        }
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
        _members.value = updatedMembers
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

        // B7: Toter Code (isInWindow-Block) entfernt. Nur die korrekte Duration-Berechnung.
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
        alarmScheduler.scheduleWakeUp(snoozeTime, memberId, memberName) {
            _errorMessage.value = UiText.StringResource(R.string.error_alarm_permission)
        }
    }

    fun refreshData() {
        val uid = auth.currentUser?.uid ?: return
        _isSyncing.value = true
        viewModelScope.launch {
            try {
                val result = repository.getUserFamily(uid)
                result.onSuccess { triple ->
                    if (triple != null) {
                        prefsRepo.setFamilyId(triple.first)
                        prefsRepo.setJoinCode(triple.second)
                        prefsRepo.setAlarmEnabled(triple.third)

                        val fetchedFamilyName = repository.getFamilyName(triple.first)
                        prefsRepo.setFamilyName(fetchedFamilyName)

                        val claimedMember = repository.getClaimedMember(triple.first, uid)
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
                _members.value = checkedMembers
                recalculateSchedule()
            }
        }
    }

    private fun checkAndResetMembers(members: List<FamilyMember>): List<FamilyMember> {
        val today = LocalDate.now().toString()
        val now = LocalTime.now()

        return members.map { member ->
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
                val familyIdVal = familyId.value
                if (familyIdVal != null) {
                    viewModelScope.launch {
                        try {
                            repository.addOrUpdateMember(familyIdVal, updated)
                        } catch (e: Exception) {
                            android.util.Log.e("FamilyViewModel", "Failed to reset member status: ${e.message}")
                        }
                    }
                }
                updated
            } else {
                member
            }
        }
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
        // B6: Alarm canceln bevor die Familie verlassen wird
        cancelAlarmForCurrentUser()
        viewModelScope.launch {
            repository.removeUserFamily(uid)
            prefsRepo.setFamilyId(null)
            prefsRepo.setJoinCode(null)
            prefsRepo.setFamilyName(null)
            prefsRepo.setMyMemberId(null)
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

                // O4: Alarm nur neu setzen wenn die Zeit sich geändert hat
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
}
