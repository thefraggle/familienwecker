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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
            familyId.collect { currentFamilyId ->
                membersJob?.cancel()
                if (currentFamilyId != null) {
                    membersJob = launch {
                        repository.getFamilyMembersFlow(currentFamilyId).collect { membersList ->
                            _members.value = membersList
                            recalculateSchedule()
                        }
                    }
                } else {
                    _members.value = emptyList()
                    recalculateSchedule()
                }
            }
        }

        // 2. Observer MyMemberId
        viewModelScope.launch {
            myMemberId.collect { id ->
                if (id == null && isAlarmEnabled.value) {
                    setAlarmEnabled(false)
                }
                recalculateSchedule()
            }
        }

        // 3. Observer Global Alarm Toggle
        viewModelScope.launch {
            isAlarmEnabled.collect {
                recalculateSchedule()
            }
        }
    }

    fun createFamily(familyName: String, onComplete: (Boolean) -> Unit) {
        _errorMessage.value = null
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = repository.createFamily(familyName, uid)
            result.onSuccess { pair ->
                prefsRepo.setFamilyId(pair.first)
                prefsRepo.setJoinCode(pair.second)
                prefsRepo.setFamilyName(familyName)
                
                auth.currentUser?.uid?.let { uid ->
                    repository.saveUserFamily(uid, pair.first, pair.second)
                }
                
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
                prefsRepo.setFamilyId(pair.first)
                prefsRepo.setJoinCode(pair.second)
                
                // Fetch name since we only have the code
                val fetchedName = repository.getFamilyName(pair.first)
                prefsRepo.setFamilyName(fetchedName)
                
                auth.currentUser?.uid?.let { uid ->
                    repository.saveUserFamily(uid, pair.first, pair.second)
                }
                
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
        repository.removeMember(currentFamilyId, id)
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

    fun logout() {
        prefsRepo.setFamilyId(null)
        prefsRepo.setJoinCode(null)
        prefsRepo.setFamilyName(null)
        prefsRepo.setMyMemberId(null)
    }

    fun leaveFamily() {
        auth.currentUser?.uid?.let { uid ->
            viewModelScope.launch {
                repository.removeUserFamily(uid)
            }
        }
        logout()
    }

    fun deleteFamily(onComplete: (Boolean) -> Unit) {
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
                onComplete(false)
            }
        }
    }

    private fun recalculateSchedule() {
        val currentMembers = _members.value
        val alarmsOn = isAlarmEnabled.value

        if (currentMembers.isNotEmpty()) {
            val result = scheduler.calculateIdealSchedule(currentMembers)
            _schedule.value = result
            
            // Apply alarms for active schedules if the global toggle is ON
            if (alarmsOn && result.isValid && result.memberSchedules.isNotEmpty()) {
                applyAlarms(result)
            } else {
                // Wipe local system alarms
                currentMembers.forEach { alarmScheduler.cancelWakeUp(it.id) }
            }
        } else {
            _schedule.value = null
        }
    }

    private fun applyAlarms(schedule: FamilySchedule) {
        val tomorrow = LocalDate.now().plusDays(1)
        val today = LocalDate.now()

        val currentMyMemberId = myMemberId.value
        
        schedule.memberSchedules.forEach { alarmScheduler.cancelWakeUp(it.member.id) }

        if (currentMyMemberId == null) return

        for (memberSchedule in schedule.memberSchedules) {
            if (memberSchedule.member.id == currentMyMemberId) {
                val wakeUpTime = memberSchedule.wakeUpTime
                val targetDate = if (LocalTime.now().isAfter(wakeUpTime)) tomorrow else today
                val targetDateTime = LocalDateTime.of(targetDate, wakeUpTime)

                alarmScheduler.scheduleWakeUp(targetDateTime, memberSchedule.member.id, memberSchedule.member.name)
            }
        }
    }
}
