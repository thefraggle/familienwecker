package de.familienwecker.famwake.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dev.gitlive.firebase.auth.FirebaseUser
import de.familienwecker.famwake.BuildConfig
import de.familienwecker.famwake.FamWakeApplication
import de.familienwecker.famwake.R
import de.familienwecker.famwake.data.AppError
import de.familienwecker.famwake.data.appErrorFromException
import de.familienwecker.famwake.data.toUiText
import de.familienwecker.famwake.data.AuthRepository
import de.familienwecker.famwake.data.FirebaseRepository
import de.familienwecker.famwake.data.GoogleSignInFailedException
import de.familienwecker.famwake.data.LoginFailedException
import de.familienwecker.famwake.data.AppSettings
import de.familienwecker.famwake.data.RegistrationFailedException
import de.familienwecker.famwake.ui.util.UiText
import de.familienwecker.famwake.util.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest
import java.util.UUID

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository: AuthRepository = AuthRepository()
    private val appSettings: AppSettings =
        (application as FamWakeApplication).appSettings
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
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            checkCurrentUser()
        }
    }

    private fun checkCurrentUser() {
        val user = authRepository.currentUser
        if (user != null) {
            if (user.isEmailVerified) {
                _isRestoringFamily.value = true
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
            try {
                if (!NetworkUtils.isOnline(getApplication())) {
                    _isRestoringFamily.value = false
                    return@launch
                }

                // Primärpfad: getUserContext() via Cloud Function (1 Call statt 3 Reads)
                val result = withTimeoutOrNull(3000) {
                    dbRepository.getUserContext(uid)
                } ?: run {
                    // Timeout → Fallback auf direkten Firestore-Pfad mit Cache
                    if (BuildConfig.DEBUG) {
                        android.util.Log.w("AuthViewModel", "getUserContext timed out, falling back to getUserFamily")
                    }
                    withTimeoutOrNull(2000) {
                        dbRepository.getUserFamily(uid, cachedJoinCode = appSettings.joinCode.value)
                    }
                }

                if (result == null) {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.w("AuthViewModel", "User family fetch timed out entirely")
                    }
                    _isRestoringFamily.value = false
                    return@launch
                }

                // Fallback bei CF-Fehler (z.B. noch nicht deployed)
                val finalResult = if (result.isFailure) {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.w("AuthViewModel", "getUserContext failed, falling back: ${result.exceptionOrNull()?.message}")
                    }
                    withTimeoutOrNull(2000) {
                        dbRepository.getUserFamily(uid, cachedJoinCode = appSettings.joinCode.value)
                    } ?: result
                } else {
                    result
                }

                finalResult.onSuccess { pair ->
                    if (pair != null) {
                        val familyExistsResult = kotlin.runCatching {
                            withTimeoutOrNull(2000) { dbRepository.checkFamilyExists(pair.first) }
                        }
                        if (familyExistsResult.getOrNull() == true) {
                            appSettings.setFamilyId(pair.first)
                            appSettings.setJoinCode(pair.second)

                            val familyName = withTimeoutOrNull(2000) { dbRepository.getFamilyName(pair.first) }
                            appSettings.setFamilyName(familyName)

                            val claimedMember = withTimeoutOrNull(2000) { dbRepository.getClaimedMember(pair.first, uid) }
                            if (claimedMember != null) {
                                appSettings.setMyMemberId(claimedMember.id)
                                appSettings.setMyMemberName(claimedMember.name)
                            }
                        } else if (familyExistsResult.getOrNull() == false) {
                            dbRepository.removeUserFamily(uid, pair.first)
                            appSettings.clearAll()
                        }
                    } else {
                        appSettings.clearAll()
                    }
                    _isRestoringFamily.value = false
                }.onFailure { error ->
                    if (BuildConfig.DEBUG) {
                        android.util.Log.e("AuthViewModel", "Restoration failed: ${error.message}")
                    }
                    _isRestoringFamily.value = false
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    android.util.Log.e("AuthViewModel", "Error during restoreUserFamily: ${e.message}")
                }
                _isRestoringFamily.value = false
            }
        }
    }


    fun login(email: String, pass: String) {
        if (!isValidEmail(email)) {
            _authState.value = AuthState.Error(UiText.StringResource(R.string.error_invalid_email))
            return
        }
        if (pass.length < 8) {
            _authState.value = AuthState.Error(UiText.StringResource(R.string.error_password_too_short))
            return
        }
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            val result = authRepository.login(email, pass)
            result.onSuccess { user ->
                if (user.isEmailVerified) {
                    if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                        android.util.Log.d("AuthViewModel", "User logged in and verified. Starting restoration...")
                    }
                    _isRestoringFamily.value = true
                    _authState.value = AuthState.Authenticated(user)
                    restoreUserFamily(user.uid)
                } else {
                    _authState.value = AuthState.AwaitingEmailVerification
                }
            }.onFailure { error ->
                _authState.value = AuthState.Error(appErrorFromException((error as? Exception) ?: Exception(error.message)).toUiText())
            }
        }
    }

    fun register(email: String, pass: String) {
        if (!isValidEmail(email)) {
            _authState.value = AuthState.Error(UiText.StringResource(R.string.error_invalid_email))
            return
        }
        if (pass.length < 8) {
            _authState.value = AuthState.Error(UiText.StringResource(R.string.error_password_too_short))
            return
        }
        _authState.value = AuthState.Loading
        val language = appSettings.language.value
        viewModelScope.launch {
            val result = authRepository.register(email, pass)
            result.onSuccess {
                val sendResult = authRepository.sendVerificationEmail(email, language)
                if (sendResult.isFailure && de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.e("AuthViewModel", "Failed to send verification email: ${sendResult.exceptionOrNull()}")
                }
                _authState.value = AuthState.AwaitingEmailVerification
            }.onFailure { error ->
                _authState.value = AuthState.Error(appErrorFromException((error as? Exception) ?: Exception(error.message)).toUiText())
            }
        }
    }

    /**
     * M-5: Kompletter Google Sign-In Flow – von Nonce-Generierung bis Firebase-Auth.
     * Zwei-Stufen-Ansatz:
     * 1. Versuch mit bereits autorisierten Accounts (schnell, kein Dialog)
     * 2. Fallback: Account-Picker anzeigen (filterByAuthorizedAccounts=false, autoSelect=false)
     *    → behebt "kein Konto gefunden" bei Erstnutzung / auf nicht-Play-Store-APKs
     */
    fun signInWithGoogle(context: Context) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)

                val rawNonce = UUID.randomUUID().toString()
                val hashedNonce = MessageDigest.getInstance("SHA-256")
                    .digest(rawNonce.toByteArray())
                    .fold("") { str, it -> str + "%02x".format(it) }

                val webClientId = context.getString(R.string.default_web_client_id)

                // Stufe 1: Nur bereits autorisierte Accounts (kein Dialog, schnell)
                val authorizedOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(webClientId)
                    .setNonce(hashedNonce)
                    .setAutoSelectEnabled(true)
                    .build()

                val credential = try {
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(authorizedOption)
                        .build()
                    credentialManager.getCredential(context, request).credential
                } catch (_: NoCredentialException) {
                    // Stufe 2: Kein autorisierter Account → Account-Picker anzeigen
                    val allAccountsOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .setNonce(hashedNonce)
                        .setAutoSelectEnabled(false)
                        .build()

                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(allAccountsOption)
                        .build()
                    credentialManager.getCredential(context, fallbackRequest).credential
                }

                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    // GoogleAuthProvider aus com.google.firebase.auth ist weiterhin nötig für den Credential-Typ
                    val firebaseCredential = com.google.firebase.auth.GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    // GitLive erwartet dev.gitlive.firebase.auth.AuthCredential
                    val gitliveCredential = dev.gitlive.firebase.auth.GoogleAuthProvider.credential(googleIdTokenCredential.idToken, null)
                    val authResult = authRepository.signInWithGoogleCredential(gitliveCredential)
                    authResult.onSuccess { user ->
                        _authState.value = AuthState.Authenticated(user)
                        restoreUserFamily(user.uid)
                    }.onFailure { error ->
                        _authState.value = AuthState.Error(appErrorFromException((error as? Exception) ?: Exception(error.message)).toUiText())
                    }
                } else {
                    _authState.value = AuthState.Error(UiText.StringResource(R.string.error_google_sign_in_failed_unknown))
                }
            } catch (_: NoCredentialException) {
                _authState.value = AuthState.Error(UiText.StringResource(R.string.login_google_error_no_account))
            } catch (e: GetCredentialException) {
                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.w("AuthViewModel", "Google Sign-In failed: ${e.message}")
                }
                _authState.value = AuthState.Error(UiText.StringResource(R.string.error_google_sign_in_failed_unknown))
            } catch (e: Exception) {
                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.e("AuthViewModel", "Unexpected Google Sign-In error: ${e.message}")
                }
                _authState.value = AuthState.Error(UiText.StringResource(R.string.error_google_sign_in_failed_unknown))
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
        appSettings.clearAll()
        _authState.value = AuthState.Idle
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error(UiText.StringResource(R.string.error_empty_email))
            return
        }
        if (!isValidEmail(email)) {
            _authState.value = AuthState.Error(UiText.StringResource(R.string.error_invalid_email))
            return
        }
        _authState.value = AuthState.Loading
        val language = appSettings.language.value
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email, language)
            // OWASP Best Practice: Immer Erfolg zurückgeben, unabhängig davon ob die
            // E-Mail-Adresse registriert ist. Verhindert User Enumeration Attacks.
            // Firebase-seitig: "Email Enumeration Protection" in der Console aktivieren.
            if (result.isFailure && BuildConfig.DEBUG) {
                android.util.Log.d("AuthViewModel", "Password reset silenced: ${result.exceptionOrNull()?.message}")
            }
            _authState.value = AuthState.PasswordResetSuccess
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
                _authState.value = AuthState.Error(UiText.StringResource(R.string.login_verify_email_not_verified))
            }
        }
    }

    fun resendVerificationEmail() {
        val email = authRepository.currentUser?.email ?: return
        val language = appSettings.language.value
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.sendVerificationEmail(email, language)
            result.onSuccess {
                _authState.value = AuthState.AwaitingEmailVerification
            }.onFailure { error ->
                _authState.value = AuthState.Error(appErrorFromException((error as? Exception) ?: Exception(error.message)).toUiText())
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }
}
