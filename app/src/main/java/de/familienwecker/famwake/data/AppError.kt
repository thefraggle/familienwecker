package de.familienwecker.famwake.data

import de.familienwecker.famwake.R
import de.familienwecker.famwake.ui.util.UiText

/**
 * S-2: Repräsentiert App-spezifische Fehler ohne Magic Strings.
 */
sealed class AppError(val uiText: UiText) {
    // Auth Fehler
    object EmailOrPasswordEmpty : AppError(UiText.StringResource(R.string.error_invalid_credentials))
    object LoginFailed : AppError(UiText.StringResource(R.string.error_login_failed_unknown))
    object RegistrationFailed : AppError(UiText.StringResource(R.string.error_registration_failed_unknown))
    object GoogleSignInFailed : AppError(UiText.StringResource(R.string.error_login_failed_unknown))
    object UserNotFound : AppError(UiText.StringResource(R.string.error_user_not_found))
    object InvalidEmail : AppError(UiText.StringResource(R.string.error_invalid_email))
    object TooManyRequests : AppError(UiText.StringResource(R.string.error_too_many_requests))
    object ResetFailed : AppError(UiText.StringResource(R.string.error_password_reset_failed))
    object EmailAlreadyInUse : AppError(UiText.StringResource(R.string.error_email_already_in_use))
    object WeakPassword : AppError(UiText.StringResource(R.string.error_weak_password))
    
    // Family Fehler
    object FamilyNotFound : AppError(UiText.StringResource(R.string.error_family_not_found))
    object CodeGenerationFailed : AppError(UiText.StringResource(R.string.error_code_generation_failed))
    data class PermissionDenied(val message: String? = null) : AppError(UiText.StringResource(R.string.error_permission_denied))
    object LoadMembersFailed : AppError(UiText.StringResource(R.string.error_load_members, ""))
    
    // Fallback
    data class Unknown(val message: String?) : AppError(
        UiText.StringResource(R.string.error_unknown)
    )

    companion object {
        fun fromException(e: Exception): AppError {
            val msg = e.message?.uppercase() ?: ""
            return when {
                e is com.google.firebase.auth.FirebaseAuthInvalidUserException || msg.contains("USER_NOT_FOUND") -> UserNotFound
                e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException || msg.contains("INVALID_CREDENTIALS") -> EmailOrPasswordEmpty
                e is com.google.firebase.auth.FirebaseAuthUserCollisionException || msg.contains("EMAIL_EXISTS") -> EmailAlreadyInUse
                e is com.google.firebase.auth.FirebaseAuthWeakPasswordException || msg.contains("WEAK_PASSWORD") -> WeakPassword
                msg.contains("INVALID_EMAIL") || msg.contains("INVALID_ARGUMENT") -> InvalidEmail
                msg.contains("RESOURCE_EXHAUSTED") || msg.contains("TOO_MANY") -> TooManyRequests
                msg.contains("PERMISSION_DENIED") -> PermissionDenied(e.localizedMessage)
                else -> {
                    if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                        Unknown(e.localizedMessage ?: e.toString())
                    } else {
                        Unknown(null)
                    }
                }
            }
        }
    }
}
