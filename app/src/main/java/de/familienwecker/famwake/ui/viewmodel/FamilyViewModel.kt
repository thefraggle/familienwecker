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

    private val _whatsNewContent = MutableStateFlow<WhatsNewContent?>(null)
    val whatsNewContent: StateFlow<WhatsNewContent?> = _whatsNewContent.asStateFlow()

    private val _pendingJoinCode = MutableStateFlow<String?>(null)
    val pendingJoinCode: StateFlow<String?> = _pendingJoinCode.asStateFlow()

    private val _showJoinSuccess = MutableStateFlow(false)
    val showJoinSuccess: StateFlow<Boolean> = _showJoinSuccess.asStateFlow()

    private var membersJob: Job? = null
    private var alarmEnabledJob: Job? = null

    init {
        // 1. Observe FamilyId and load members accordingly
        viewModelScope.launch {
            try {
                familyId.collect { currentFamilyId ->
                    membersJob?.cancel()
                    alarmEnabledJob?.cancel()
                    if (!currentFamilyId.isNullOrBlank()) {
                        // Startup Sync: Force refresh from Firebase once to ensure consistency
                        // We do this BEFORE starting the member flow to ensure user/family doc link is there
                        refreshData()
                        
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
                                            // Handle case where claim was removed on another device
                                            prefsRepo.setMyMemberId(null)
                                        }
                                    }
                                    
                                    recalculateSchedule()
                                }
                            } catch (e: CancellationException) {
                                // Ignore cancellation exceptions caused by rapid familyId re-emits (e.g. during auth restore)
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
                // User-Dokument zuerst in Firestore schreiben – Firestore Security Rules
                // prüfen isFamilyMember() über /users/{uid}.familyId. Erst danach darf
                // navigiert werden, sonst schlägt der erste Members-Write mit Permission-Denied fehl.
                repository.saveUserFamily(uid, pair.first, pair.second)

                // SharedPrefs NACH dem Cloud-Write setzen, damit Flow-Observer
                // getFamilyMembersFlow erst startet wenn die Berechtigung gesichert ist.
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
        viewModelScope.launch {
            val result = repository.joinFamilyByCode(code)
            result.onSuccess { pair ->
                val uid = auth.currentUser?.uid

                // User-Dokument zuerst schreiben (gleicher Grund wie createFamily)
                if (uid != null) {
                    repository.saveUserFamily(uid, pair.first, pair.second)
                }

                // Familienname aus der Cloud holen
                val fetchedName = repository.getFamilyName(pair.first)

                // SharedPrefs erst setzen wenn Cloud-Write abgeschlossen
                prefsRepo.setFamilyId(pair.first)
                prefsRepo.setJoinCode(pair.second)
                prefsRepo.setFamilyName(fetchedName)

                // Clear pending code if it matches successfully
                if (_pendingJoinCode.value == code) {
                    _pendingJoinCode.value = null
                }
                
                _showJoinSuccess.value = true
                onComplete(true)
            }.onFailure { error ->
                _errorMessage.value = UiText.StringResource(R.string.error_invalid_code, error.localizedMessage ?: "Unknown")
                onComplete(false)
            }
        }
    }

    fun setPendingJoinCode(code: String?) {
        _pendingJoinCode.value = code
    }

    fun clearPendingJoinCode() {
        _pendingJoinCode.value = null
    }

    fun handlePendingJoin(onComplete: (Boolean) -> Unit) {
        val code = _pendingJoinCode.value ?: return
        
        // Sicherstellen, dass wir einen sauberen Zustand haben bevor wir joinen
        // (Verhindert Race Conditions mit veralteten familyIds)
        prefsRepo.setFamilyId(null)
        
        joinFamily(code, onComplete)
    }

    /**
     * Verlässt die aktuelle Familie und tritt der neuen (aus pendingJoinCode) atomar bei.
     * Dies verhindert, dass der familyId Flow zwischendurch null wird und die UI zum SetupScreen springt.
     */
    fun leaveAndJoinPendingCode(onComplete: (Boolean) -> Unit) {
        val code = _pendingJoinCode.value ?: return
        val uid = auth.currentUser?.uid
        
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                // 1. Cloud-Link zur alten Familie entfernen
                if (uid != null) {
                    repository.removeUserFamily(uid)
                }
                
                // 2. Der neuen Familie beitreten
                val result = repository.joinFamilyByCode(code)
                result.onSuccess { pair ->
                    // 3. Neuen Cloud-Link speichern
                    if (uid != null) {
                        repository.saveUserFamily(uid, pair.first, pair.second)
                    }
                    
                    // 4. Familiennamen holen
                    val fetchedName = repository.getFamilyName(pair.first)
                    
                    // 5. SharedPrefs AKTUALISIEREN (dies triggert die Flow-Emissionen)
                    // Wir setzen hier direkt die neue ID, sodass sie nie null wird
                    val oldFamilyId = familyId.value
                    prefsRepo.setFamilyId(pair.first)
                    prefsRepo.setJoinCode(pair.second)
                    prefsRepo.setFamilyName(fetchedName)
                    
                    if (oldFamilyId != pair.first) {
                        prefsRepo.setMyMemberId(null) // Profil nur bei echtem Familienwechsel neu wählen
                    }
                    
                    _pendingJoinCode.value = null
                    _showJoinSuccess.value = true
                    onComplete(true)
                }.onFailure { error ->
                    _errorMessage.value = UiText.StringResource(R.string.error_invalid_code, error.localizedMessage ?: "Unknown")
                    _pendingJoinCode.value = null // Dialog schließen
                    
                    // Lokalen Zustand ebenfalls nullen, da Cloud-Link bereits gelöscht wurde.
                    // Dies triggert die Navigation zurück zum SetupScreen.
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
        // Alarm für dieses Mitglied abbrechen (unabhängig ob es myMemberId ist)
        alarmScheduler.cancelWakeUp(id)
        viewModelScope.launch {
            val result = repository.removeMember(currentFamilyId, id)
            if (result.isFailure) {
                _errorMessage.value = UiText.StringResource(R.string.error_delete_member, result.exceptionOrNull()?.localizedMessage ?: "Unknown")
            }
        }
        // Setze MyMemberId zurück falls der eigene Nutzer gelöscht wird
        if (myMemberId.value == id) {
            setMyMemberId(null)
        }
    }

    fun setMyMemberId(id: String?, onComplete: (Boolean) -> Unit = {}) {
        val currentFamilyId = familyId.value ?: return
        val currentMyMemberId = myMemberId.value
        val userId = auth.currentUser?.uid ?: return
        val userName = auth.currentUser?.displayName ?: "Papa/Mama"

        viewModelScope.launch {
            // 1. Unclaim previous member if exists
            if (currentMyMemberId != null && currentMyMemberId != id) {
                repository.unclaimMember(currentFamilyId, currentMyMemberId, userId)
            }

            // 2. Claim new member if id is not null
            if (id != null) {
                val success = repository.claimMember(currentFamilyId, id, userId, userName)
                if (success) {
                    prefsRepo.setMyMemberId(id)
                    // Alarm automatisch einschalten wenn ein Profil gewählt wird
                    prefsRepo.setAlarmEnabled(true)
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } else {
                prefsRepo.setMyMemberId(null)
                // Alarm ausschalten wenn kein Profil mehr gewählt ist
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
        // Wecker kann nur eingeschaltet werden, wenn ein Profil belegt ist
        if (enabled && myMemberId.value == null) return

        val currentFamilyId = familyId.value
        if (currentFamilyId != null) {
            viewModelScope.launch {
                repository.updateFamilyAlarmEnabled(currentFamilyId, enabled)
            }
        }
        
        // Fallback for local UI (flow will update it anyway)
        prefsRepo.setAlarmEnabled(enabled)
    }

    fun togglePauseMember(memberId: String) {
        val member = _members.value.find { it.id == memberId } ?: return
        
        // Pause only allowed for unclaimed members or own profile
        if (member.claimedByUserId != null && member.id != myMemberId.value) return
        
        val updatedMember = member.copy(isPaused = !member.isPaused)
        addOrUpdateMember(updatedMember)
    }

    /**
     * Verschiebt ein Mitglied lokal in der Liste für sofortiges UI-Feedback.
     */
    fun moveMemberOrder(fromIndex: Int, toIndex: Int) {
        val currentMembers = _members.value.toMutableList()
        if (fromIndex !in currentMembers.indices || toIndex !in currentMembers.indices) return

        // 1. Lokale Liste aktualisieren
        val member = currentMembers.removeAt(fromIndex)
        currentMembers.add(toIndex, member)
        
        // 2. Neue Indizes vergeben
        val updatedMembers = currentMembers.mapIndexed { index, m ->
            m.copy(sequenceOrder = index)
        }
        _members.value = updatedMembers
        
        // 3. Sofort neu berechnen für "Ghost-Times" (während des Drags)
        recalculateSchedule()
    }

    /**
     * Persistiert die aktuelle lokale Reihenfolge in Firestore.
     * Wird am Ende eines Drag & Drop Vorgangs aufgerufen.
     */
    fun saveMemberOrder() {
        val currentFamilyId = familyId.value ?: return
        val updatedMembers = _members.value
        val orderMap = updatedMembers.associate { it.id to it.sequenceOrder }
        
        viewModelScope.launch {
            repository.updateMemberOrders(currentFamilyId, orderMap)
        }
    }

    fun toggleAwakeMember(memberId: String) {
        // "Bin wach" can only be toggled for the own profile (requested feature)
        if (memberId != myMemberId.value) return
        
        val member = _members.value.find { it.id == memberId } ?: return
        
        // 4h window restriction: only allow "Already Awake" within 4h before wake-up
        val now = LocalTime.now()
        val wakeUpTime = member.latestWakeUp
        val fourHoursBefore = wakeUpTime.minusHours(4)
        
        // Check if now is within the 4h window before wake-up
        // or during the wake-up window itself (before it resets)
        val isInWindow = if (fourHoursBefore.isBefore(wakeUpTime)) {
            now.isAfter(fourHoursBefore) || now.isBefore(LocalTime.of(0, 5)) // spans midnight or just before
        } else {
            // Case where 4h before crosses midnight (e.g. wake up at 02:00 -> 22:00)
            now.isAfter(fourHoursBefore) || now.isBefore(wakeUpTime)
        }
        
        // Simpler check for now: actually just check if duration to wakeUp is <= 4h
        val targetDate = if (now.isAfter(wakeUpTime)) LocalDate.now().plusDays(1) else LocalDate.now()
        val targetDateTime = LocalDateTime.of(targetDate, wakeUpTime)
        val duration = java.time.Duration.between(LocalDateTime.now(), targetDateTime)
        val hoursUntil = duration.toHours()
        
        if (hoursUntil > 4 && !member.isAwakeToday) {
            // Too early to set "Awake", only allowed if already awake (to toggle back) or within 4h
            return 
        }

        val updatedMember = member.copy(isAwakeToday = !member.isAwakeToday)
        addOrUpdateMember(updatedMember)
    }


    fun snooze(memberId: String, memberName: String) {
        val snoozeTime = LocalDateTime.now().plusMinutes(5)
        alarmScheduler.scheduleWakeUp(snoozeTime, memberId, memberName)
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
                        
                        val familyName = repository.getFamilyName(triple.first)
                        prefsRepo.setFamilyName(familyName)
                        
                        val claimedMember = repository.getClaimedMember(triple.first, uid)
                        if (claimedMember != null) {
                            prefsRepo.setMyMemberId(claimedMember.id)
                        }
                    } else {
                        // Kein Profil in der Cloud -> Lokal ebenfalls säubern (Self-Healing)
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
            // Reset logic: Only reset if we are past the latest wake-up time (+ buffer)
            // AND we haven't reset for the current date yet.
            // This allows manual toggles (e.g. Pause) to persist until the next wake-up cycle.
            val resetThreshold = member.latestWakeUp.plusHours(4)
            val isPastResetThreshold = now.isAfter(resetThreshold)
            
            if (isPastResetThreshold && member.lastResetDate != today) {
                // Feature: Unclaimed fields get their 'paused' state reset. Claimed members keep their manual 'paused' state.
                val isUnclaimed = member.claimedByUserId == null
                val newIsPaused = if (isUnclaimed) false else member.isPaused
                
                val updated = member.copy(
                    isPaused = newIsPaused,
                    isAwakeToday = false,
                    lastResetDate = today
                )
                // Inside collect flow, we are already in a coroutine scope
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
        // 1. Cancel alarm first
        cancelAlarmForCurrentUser()
        // 2. Clear all local preferences
        prefsRepo.clearAll()
        // 3. Explicitly sign out from Firebase to be absolute sure
        auth.signOut()
    }

    /** Cancelt den System-Alarm des aktuell eingeloggten Nutzers (falls vorhanden). */
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
                // Don't logout, just clear family state
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
                    // Filter members for calculation based on Master Switch
                    // "switch off: keine berechnung meiner angaben"
                    val currentMyMemberId = myMemberId.value
                    val calculationMembers = if (alarmsOn) {
                        currentMembers
                    } else {
                        currentMembers.filter { it.id != currentMyMemberId }
                    }

                    if (calculationMembers.none { !it.isPaused }) {
                        _schedule.value = FamilySchedule(emptyList(), null, true, "no_active_schedule")
                        currentMembers.forEach { alarmScheduler.cancelWakeUp(it.id) }
                        return@launch
                    }

                    // Scheduler runs n! permutations – off main thread to avoid ANR
                    val result = withContext(Dispatchers.Default) {
                        scheduler.calculateIdealSchedule(calculationMembers)
                    }
                    _schedule.value = result

                    // Auto-reset logic moved entirely to checkAndResetMembers for robustness
                    
                    if (alarmsOn && result.isValid && result.memberSchedules.isNotEmpty()) {
                        applyAlarms(result)
                    } else {
                        // Cancel alarms if master switch is off or schedule invalid
                        currentMembers.forEach { alarmScheduler.cancelWakeUp(it.id) }
                    }
                } catch (e: Exception) {
                    _errorMessage.value = UiText.StringResource(R.string.error_calculate_schedule, e.localizedMessage ?: "Unknown")
                    _schedule.value = null
                }
            }
        } else {
            // Keine Mitglieder mehr – laufenden Alarm des eigenen Profils canceln
            cancelAlarmForCurrentUser()
            _schedule.value = null
        }
    }

    private fun applyAlarms(schedule: FamilySchedule) {
        val tomorrow = LocalDate.now().plusDays(1)
        val today = LocalDate.now()

        val currentMyMemberId = myMemberId.value ?: return
        
        // Safety: ensure we only schedule if we have member schedules
        if (schedule.memberSchedules.isEmpty()) return

        // Always cancel existing alarms first for the current user to avoid duplicates or old times
        alarmScheduler.cancelWakeUp(currentMyMemberId)

        for (memberSchedule in schedule.memberSchedules) {
            if (memberSchedule.member.id == currentMyMemberId) {
                val wakeUpTime = memberSchedule.wakeUpTime
                val targetDate = if (LocalTime.now().isAfter(wakeUpTime)) tomorrow else today
                val targetDateTime = LocalDateTime.of(targetDate, wakeUpTime)

                // Feature "Already Awake": Only skip alarm if we are within the 4h window before/during wake-up (requested)
                if (memberSchedule.member.isAwakeToday) {
                    val hoursUntilAlarm = java.time.Duration.between(LocalDateTime.now(), targetDateTime).toHours()
                    // If alarm is in the future and less than 4h away, or already in the past (e.g. within the snooze window)
                    if (hoursUntilAlarm in 0..4) {
                        continue
                    }
                }

                alarmScheduler.scheduleWakeUp(targetDateTime, memberSchedule.member.id, memberSchedule.member.name)
            }
        }
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
