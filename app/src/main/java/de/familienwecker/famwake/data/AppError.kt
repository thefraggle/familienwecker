package de.familienwecker.famwake.data

import de.familienwecker.famwake.R
import de.familienwecker.famwake.ui.util.UiText

/**
 * S-2: Repräsentiert App-spezifische Fehler ohne Magic Strings.
 */
sealed class AppError(val uiText: UiText) {
    // Auth Fehler
    object EmailOrPasswordEmpty : AppError(UiText.StringResource(R.string.error_invalid_credentials))
    object LoginFailed : AppError(UiText.StringResource(R.string.error_login_failed))
    object RegistrationFailed : AppError(UiText.StringResource(R.string.error_registration_failed))
    object GoogleSignInFailed : AppError(UiText.StringResource(R.string.error_login_failed))
    object UserNotFound : AppError(UiText.StringResource(R.string.error_user_not_found))
    object InvalidEmail : AppError(UiText.StringResource(R.string.error_invalid_email))
    object TooManyRequests : AppError(UiText.StringResource(R.string.error_too_many_requests))
    object ResetFailed : AppError(UiText.StringResource(R.string.error_password_reset_failed))
    
    // Family Fehler
    object FamilyNotFound : AppError(UiText.StringResource(R.string.error_family_not_found))
    object CodeGenerationFailed : AppError(UiText.StringResource(R.string.error_code_generation_failed))
    object PermissionDenied : AppError(UiText.StringResource(R.string.error_permission_denied))
    object LoadMembersFailed : AppError(UiText.StringResource(R.string.error_load_members))
    
    // Fallback
    data class Unknown(val message: String?) : AppError(
        UiText.DynamicString(message ?: "Ein unbekannter Fehler ist aufgetreten")
    )

    companion object {
        fun fromException(e: Exception): AppError {
            val msg = e.message?.uppercase() ?: ""
            return when {
                msg.contains("USER_NOT_FOUND") || msg.contains("NOT_FOUND") -> UserNotFound
                msg.contains("INVALID_EMAIL") || msg.contains("INVALID_ARGUMENT") -> InvalidEmail
                msg.contains("RESOURCE_EXHAUSTED") || msg.contains("TOO_MANY") -> TooManyRequests
                msg.contains("PERMISSION_DENIED") -> PermissionDenied
                else -> Unknown(e.localizedMessage)
            }
        }
    }
}
