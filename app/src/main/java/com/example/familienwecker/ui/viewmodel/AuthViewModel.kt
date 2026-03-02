package com.example.familienwecker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.familienwecker.data.AuthRepository
import com.example.familienwecker.data.FirebaseRepository
import com.example.familienwecker.data.PreferencesRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository: AuthRepository = AuthRepository()
    private val prefsRepository: PreferencesRepository = PreferencesRepository(application)
    private val dbRepository: FirebaseRepository = FirebaseRepository()

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Authenticated(val user: FirebaseUser) : AuthState()
        data class Error(val message: String) : AuthState()
        object PasswordResetSuccess : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isRestoringFamily = MutableStateFlow(false)
    val isRestoringFamily: StateFlow<Boolean> = _isRestoringFamily.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val user = authRepository.currentUser
        if (user != null) {
            _authState.value = AuthState.Authenticated(user)
            restoreUserFamily(user.uid)
        }
    }

    private fun restoreUserFamily(uid: String) {
        _isRestoringFamily.value = true
        viewModelScope.launch {
            val result = dbRepository.getUserFamily(uid)
            result.onSuccess { triple ->
                if (triple != null) {
                    val familyExists = dbRepository.checkFamilyExists(triple.first)
                    if (familyExists) {
                        prefsRepository.setFamilyId(triple.first)
                        prefsRepository.setJoinCode(triple.second)
                        prefsRepository.setAlarmEnabled(triple.third) // Correct: use fetched value
                        
                        // Fetch and cache family name
                        val familyName = dbRepository.getFamilyName(triple.first)
                        prefsRepository.setFamilyName(familyName)
                        
                        // Automatically restore member claim if exists
                        val claimedMember = dbRepository.getClaimedMember(triple.first, uid)
                        if (claimedMember != null) {
                            prefsRepository.setMyMemberId(claimedMember.id)
                        }
                    } else {
                        // Family was deleted by someone else, clean up this user
                        dbRepository.removeUserFamily(uid)
                        prefsRepository.clearAll()
                    }
                }
                _isRestoringFamily.value = false
            }.onFailure {
                _isRestoringFamily.value = false
            }
        }
    }

    fun login(email: String, pass: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.login(email, pass)
            result.onSuccess { user ->
                _authState.value = AuthState.Authenticated(user)
                restoreUserFamily(user.uid)
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.localizedMessage ?: "Login fehlgeschlagen")
            }
        }
    }

    fun register(email: String, pass: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.register(email, pass)
            result.onSuccess { user ->
                _authState.value = AuthState.Authenticated(user)
                restoreUserFamily(user.uid)
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.localizedMessage ?: "Registrierung fehlgeschlagen")
            }
        }
    }

    fun signInWithGoogle(credential: AuthCredential) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.signInWithGoogleCredential(credential)
            result.onSuccess { user ->
                _authState.value = AuthState.Authenticated(user)
                restoreUserFamily(user.uid)
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.localizedMessage ?: "Google Sign-In fehlgeschlagen")
            }
        }
    }

    fun logout() {
        authRepository.logout()
        prefsRepository.clearAll()
        _authState.value = AuthState.Idle
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Bitte gib eine E-Mail-Adresse ein.")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            result.onSuccess {
                _authState.value = AuthState.PasswordResetSuccess
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.localizedMessage ?: "Passwort-Reset fehlgeschlagen")
            }
        }
    }

    fun setError(message: String) {
        _authState.value = AuthState.Error(message)
    }
}
