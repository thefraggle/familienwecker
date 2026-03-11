package de.familienwecker.famwake.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.familienwecker.famwake.data.AuthRepository
import de.familienwecker.famwake.data.LoginFailedException
import de.familienwecker.famwake.data.RegistrationFailedException
import de.familienwecker.famwake.data.GoogleSignInFailedException
import de.familienwecker.famwake.data.FirebaseRepository
import de.familienwecker.famwake.data.PreferencesRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import de.familienwecker.famwake.R
import de.familienwecker.famwake.ui.util.UiText

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository: AuthRepository = AuthRepository()
    private val prefsRepository: PreferencesRepository = PreferencesRepository(application)
    private val dbRepository: FirebaseRepository = FirebaseRepository()

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Authenticated(val user: FirebaseUser) : AuthState()
        data class Error(val message: UiText) : AuthState()
        object PasswordResetSuccess : AuthState()
        object AwaitingEmailVerification : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** E-Mail des aktuell eingeloggten (ggf. noch unverifiziert) Firebase-Users. */
    val currentUserEmail: String?
        get() = authRepository.currentUser?.email

    private val _isRestoringFamily = MutableStateFlow(false)
    val isRestoringFamily: StateFlow<Boolean> = _isRestoringFamily.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val user = authRepository.currentUser
        if (user != null) {
            if (user.isEmailVerified) {
                _authState.value = AuthState.Authenticated(user)
                restoreUserFamily(user.uid)
            } else {
                _authState.value = AuthState.AwaitingEmailVerification
            }
        }
    }

    private fun restoreUserFamily(uid: String) {
        _isRestoringFamily.value = true
        viewModelScope.launch {
            val result = dbRepository.getUserFamily(uid)
            result.onSuccess { triple ->
                if (triple != null) {
                        val familyExistsResult = kotlin.runCatching { dbRepository.checkFamilyExists(triple.first) }
                        if (familyExistsResult.getOrNull() == true) {
                            // Bugfix: Nach einer Neu-Installation via Backup-Restore kann es sein,
                            // dass preferencesRepository.familyId.value bereits "triple.first" ist,
                            // der SnapshotListener aber aufgrund fehlender Authentifizierung vorab
                            // mit PERMISSION_DENIED gecrasht ist.
                            // Um den Flow in FamilyViewModel zwingend neu zu starten, erzwingen wir ein Emit.
                            if (prefsRepository.familyId.value == triple.first) {
                                prefsRepository.setFamilyId("") // Temporärer Dummy-Wert
                            }
                            prefsRepository.setFamilyId(triple.first)
                            prefsRepository.setJoinCode(triple.second)
                            prefsRepository.setAlarmEnabled(triple.third)
                            
                            val familyName = dbRepository.getFamilyName(triple.first)
                            prefsRepository.setFamilyName(familyName)
                            
                            val claimedMember = dbRepository.getClaimedMember(triple.first, uid)
                            if (claimedMember != null) {
                                prefsRepository.setMyMemberId(claimedMember.id)
                            }
                        } else if (familyExistsResult.getOrNull() == false) {
                            // Only clear IF we definitely know it doesn't exist (404)
                            dbRepository.removeUserFamily(uid)
                            prefsRepository.clearAll()
                        }
                        // If it's a network error (exception), we keep what we have locally
                } else {
                    // Kein Profil in der Cloud -> Lokal ebenfalls säubern (Self-Healing)
                    prefsRepository.clearAll()
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
                if (user.isEmailVerified) {
                    _authState.value = AuthState.Authenticated(user)
                    restoreUserFamily(user.uid)
                } else {
                    _authState.value = AuthState.AwaitingEmailVerification
                }
            }.onFailure { error ->
                val uiMessage = if (error is LoginFailedException) {
                    UiText.StringResource(R.string.error_login_failed_unknown)
                } else {
                    UiText.StringResource(R.string.error_login_failed, error.localizedMessage ?: "Unknown")
                }
                _authState.value = AuthState.Error(uiMessage)
            }
        }
    }

    fun register(email: String, pass: String) {
        _authState.value = AuthState.Loading
        val language = java.util.Locale.getDefault().language
        viewModelScope.launch {
            val result = authRepository.register(email, pass)
            result.onSuccess {
                // Double Opt-In: Verifikations-Mail senden, NICHT direkt einloggen
                val sendResult = authRepository.sendVerificationEmail(email, language)
                if (sendResult.isFailure) {
                    // Log the failure or set an appropriate error state if desired,
                    // but we still want to show the awaiting verification screen.
                    android.util.Log.e("AuthViewModel", "Failed to send verification email: \${sendResult.exceptionOrNull()}")
                }
                _authState.value = AuthState.AwaitingEmailVerification
            }.onFailure { error ->
                val uiMessage = if (error is RegistrationFailedException) {
                    UiText.StringResource(R.string.error_registration_failed_unknown)
                } else {
                    UiText.StringResource(R.string.error_registration_failed, error.localizedMessage ?: "Unknown")
                }
                _authState.value = AuthState.Error(uiMessage)
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
                val uiMessage = if (error is GoogleSignInFailedException) {
                    UiText.StringResource(R.string.error_google_sign_in_failed_unknown)
                } else {
                    UiText.StringResource(R.string.error_google_sign_in_failed, error.localizedMessage ?: "Unknown")
                }
                _authState.value = AuthState.Error(uiMessage)
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
            _authState.value = AuthState.Error(UiText.StringResource(R.string.error_empty_email))
            return
        }
        _authState.value = AuthState.Loading
        val language = java.util.Locale.getDefault().language // "de", "en", etc.
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email, language)
            result.onSuccess {
                _authState.value = AuthState.PasswordResetSuccess
            }.onFailure { error ->
                val message = when (error.message) {
                    "INVALID_EMAIL" -> UiText.StringResource(R.string.error_invalid_email)
                    "USER_NOT_FOUND" -> UiText.StringResource(R.string.error_user_not_found)
                    "TOO_MANY_REQUESTS" -> UiText.StringResource(R.string.error_too_many_requests)
                    else -> UiText.StringResource(R.string.error_password_reset_failed)
                }
                _authState.value = AuthState.Error(message)
            }
        }
    }

    fun setError(message: UiText) {
        _authState.value = AuthState.Error(message)
    }

    fun checkEmailVerified() {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val verified = authRepository.reloadUser()
            if (verified) {
                val user = authRepository.currentUser
                if (user != null) {
                    _authState.value = AuthState.Authenticated(user)
                    restoreUserFamily(user.uid)
                } else {
                    _authState.value = AuthState.AwaitingEmailVerification
                }
            } else {
                // Hier könnten wir auch einen speziellen Error-String definieren, 
                // wird aber intern anscheinend nur zur Screen-Steuerung genutzt.
                _authState.value = AuthState.Error(UiText.DynamicString("EMAIL_NOT_VERIFIED"))
            }
        }
    }

    fun resendVerificationEmail() {
        val email = authRepository.currentUser?.email ?: return
        val language = java.util.Locale.getDefault().language
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            authRepository.sendVerificationEmail(email, language)
            _authState.value = AuthState.AwaitingEmailVerification
        }
    }
}
