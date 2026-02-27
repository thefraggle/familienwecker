package com.example.familienwecker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.familienwecker.algorithm.Scheduler
import com.example.familienwecker.alarm.AlarmScheduler
import com.example.familienwecker.data.FirebaseRepository
import com.example.familienwecker.data.PreferencesRepository
import com.example.familienwecker.model.FamilyMember
import com.example.familienwecker.model.FamilySchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class FamilyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FirebaseRepository()
    private val scheduler = Scheduler()
    private val alarmScheduler = AlarmScheduler(application)
    private val prefsRepo = PreferencesRepository(application)
    private val auth = FirebaseAuth.getInstance()

    val myMemberId: StateFlow<String?> = prefsRepo.myMemberId
    val alarmSoundUri: StateFlow<String?> = prefsRepo.alarmSoundUri
    val familyId: StateFlow<String?> = prefsRepo.familyId
    val joinCode: StateFlow<String?> = prefsRepo.joinCode
    val familyName: StateFlow<String?> = prefsRepo.familyName
    val language: StateFlow<String> = prefsRepo.language
    val isAlarmEnabled: StateFlow<Boolean> = prefsRepo.isAlarmEnabled

    private val _members = MutableStateFlow<List<FamilyMember>>(emptyList())
    val members: StateFlow<List<FamilyMember>> = _members.asStateFlow()

    private val _schedule = MutableStateFlow<FamilySchedule?>(null)
    val schedule: StateFlow<FamilySchedule?> = _schedule.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var membersJob: Job? = null

    init {
        // 1. Observe FamilyId and load members accordingly
        viewModelScope.launch {
            try {
                familyId.collect { currentFamilyId ->
                    membersJob?.cancel()
                    if (currentFamilyId != null) {
                        membersJob = launch {
                            try {
                                repository.getFamilyMembersFlow(currentFamilyId).collect { membersList ->
                                    val checkedMembers = checkAndResetMembers(membersList)
                                    _members.value = checkedMembers
                                    recalculateSchedule()
                                }
                            } catch (e: Exception) {
                                _errorMessage.value = "Fehler beim Laden der Mitglieder: ${e.localizedMessage}"
                            }
                        }
                    } else {
                        _members.value = emptyList()
                        recalculateSchedule()
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Systemfehler: ${e.localizedMessage}"
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
    }

    fun createFamily(familyName: String, onComplete: (Boolean) -> Unit) {
        _errorMessage.value = null
        val uid = auth.currentUser?.uid ?: return
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
                _errorMessage.value = error.localizedMessage ?: "Fehler beim Erstellen der Familie"
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

                onComplete(true)
            }.onFailure { error ->
                _errorMessage.value = error.localizedMessage ?: "Code ungültig"
                onComplete(false)
            }
        }
    }

    fun addOrUpdateMember(member: FamilyMember) {
        val currentFamilyId = familyId.value ?: return
        repository.addOrUpdateMember(currentFamilyId, member)
    }

    fun removeMember(id: String) {
        val currentFamilyId = familyId.value ?: return
        // Alarm für dieses Mitglied abbrechen (unabhängig ob es myMemberId ist)
        alarmScheduler.cancelWakeUp(id)
        viewModelScope.launch {
            val result = repository.removeMember(currentFamilyId, id)
            if (result.isFailure) {
                _errorMessage.value = "Mitglied konnte nicht gelöscht werden: ${result.exceptionOrNull()?.localizedMessage}"
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

    fun setAlarmEnabled(enabled: Boolean) {
        // Wecker kann nur eingeschaltet werden, wenn ein Profil belegt ist
        if (enabled && myMemberId.value == null) return

        prefsRepo.setAlarmEnabled(enabled)
        
        // Sync to cloud so other family members know this user's alarm is paused
        val currentMyMemberId = myMemberId.value ?: return
        val currentMembers = _members.value
        val myMemberProfile = currentMembers.find { it.id == currentMyMemberId }
        
        if (myMemberProfile != null) {
            val updatedProfile = myMemberProfile.copy(isPaused = !enabled)
            addOrUpdateMember(updatedProfile)
        }
    }

    fun togglePauseMember(memberId: String) {
        val member = _members.value.find { it.id == memberId } ?: return
        val updatedMember = member.copy(isPaused = !member.isPaused)
        addOrUpdateMember(updatedMember)
    }

    fun toggleAwakeMember(memberId: String) {
        val member = _members.value.find { it.id == memberId } ?: return
        val updatedMember = member.copy(isAwakeToday = !member.isAwakeToday)
        addOrUpdateMember(updatedMember)
    }

    fun snooze(memberId: String, memberName: String) {
        val snoozeTime = LocalDateTime.now().plusMinutes(5)
        alarmScheduler.scheduleWakeUp(snoozeTime, memberId, memberName)
    }

    private fun checkAndResetMembers(members: List<FamilyMember>): List<FamilyMember> {
        val today = LocalDate.now().toString()
        val updatedMembers = members.map { member ->
            if (member.lastResetDate != today) {
                val updated = member.copy(
                    isPaused = false,
                    isAwakeToday = false,
                    lastResetDate = today
                )
                addOrUpdateMember(updated)
                updated
            } else {
                member
            }
        }
        return updatedMembers
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

    fun leaveFamily() {
        _errorMessage.value = null
        auth.currentUser?.uid?.let { uid ->
            viewModelScope.launch {
                repository.removeUserFamily(uid)
            }
        }
        logout()
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
                logout()
                onComplete(true)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Fehler beim Löschen der Familie"
                onComplete(false)
            }
        }
    }

    private fun recalculateSchedule() {
        val currentMembers = _members.value
        val alarmsOn = isAlarmEnabled.value

        if (currentMembers.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    // Scheduler runs n! permutations – off main thread to avoid ANR
                    val result = withContext(Dispatchers.Default) {
                        scheduler.calculateIdealSchedule(currentMembers)
                    }
                    _schedule.value = result

                    if (alarmsOn && result.isValid && result.memberSchedules.isNotEmpty()) {
                        applyAlarms(result)
                    } else {
                        currentMembers.forEach { alarmScheduler.cancelWakeUp(it.id) }
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Fehler bei der Zeitplanberechnung: ${e.localizedMessage}"
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
                // Feature "Bin schon wach": Wenn der Nutzer bereits wach ist, keinen Alarm planen
                if (memberSchedule.member.isAwakeToday) {
                    continue
                }

                val wakeUpTime = memberSchedule.wakeUpTime
                val targetDate = if (LocalTime.now().isAfter(wakeUpTime)) tomorrow else today
                val targetDateTime = LocalDateTime.of(targetDate, wakeUpTime)

                alarmScheduler.scheduleWakeUp(targetDateTime, memberSchedule.member.id, memberSchedule.member.name)
            }
        }
    }
}
