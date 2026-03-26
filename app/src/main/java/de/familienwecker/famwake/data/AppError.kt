package de.familienwecker.famwake.data

import de.familienwecker.famwake.R
import de.familienwecker.famwake.ui.util.UiText

/**
 * Mappt einen plattform-unabhängigen AppError auf einen Android-spezifischen UiText.
 * Bleibt im app-Modul, da R.string Android-only ist.
 */
fun AppError.toUiText(): UiText = when (this) {
    is AppError.EmailOrPasswordEmpty  -> UiText.StringResource(R.string.error_invalid_credentials)
    is AppError.LoginFailed           -> UiText.StringResource(R.string.error_login_failed_unknown)
    is AppError.RegistrationFailed    -> UiText.StringResource(R.string.error_registration_failed_unknown)
    is AppError.GoogleSignInFailed    -> UiText.StringResource(R.string.error_login_failed_unknown)
    is AppError.UserNotFound          -> UiText.StringResource(R.string.error_user_not_found)
    is AppError.InvalidEmail          -> UiText.StringResource(R.string.error_invalid_email)
    is AppError.TooManyRequests       -> UiText.StringResource(R.string.error_too_many_requests)
    is AppError.ResetFailed           -> UiText.StringResource(R.string.error_password_reset_failed)
    is AppError.EmailAlreadyInUse     -> UiText.StringResource(R.string.error_email_already_in_use)
    is AppError.WeakPassword          -> UiText.StringResource(R.string.error_weak_password)
    is AppError.FamilyNotFound        -> UiText.StringResource(R.string.error_family_not_found)
    is AppError.CodeGenerationFailed  -> UiText.StringResource(R.string.error_code_generation_failed)
    is AppError.PermissionDenied      -> UiText.StringResource(R.string.error_permission_denied)
    is AppError.LoadMembersFailed     -> UiText.StringResource(R.string.error_load_members, "")
    is AppError.Unknown               -> UiText.StringResource(R.string.error_unknown)
}

/**
 * Mappt eine Exception auf den passenden AppError.
 * Bleibt im app-Modul, da com.google.firebase.auth Android-only ist.
 */
fun appErrorFromException(e: Exception): AppError {
    val msg = e.message?.uppercase() ?: ""
    return when {
        e is com.google.firebase.auth.FirebaseAuthInvalidUserException
                || msg.contains("USER_NOT_FOUND")          -> AppError.UserNotFound
        e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
                || msg.contains("INVALID_CREDENTIALS")     -> AppError.EmailOrPasswordEmpty
        e is com.google.firebase.auth.FirebaseAuthUserCollisionException
                || msg.contains("EMAIL_EXISTS")            -> AppError.EmailAlreadyInUse
        e is com.google.firebase.auth.FirebaseAuthWeakPasswordException
                || msg.contains("WEAK_PASSWORD")           -> AppError.WeakPassword
        msg.contains("INVALID_EMAIL")
                || msg.contains("INVALID_ARGUMENT")        -> AppError.InvalidEmail
        msg.contains("RESOURCE_EXHAUSTED")
                || msg.contains("TOO_MANY")                -> AppError.TooManyRequests
        msg.contains("PERMISSION_DENIED")                  -> AppError.PermissionDenied(e.localizedMessage)
        else -> {
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                AppError.Unknown(e.localizedMessage ?: e.toString())
            } else {
                AppError.Unknown(null)
            }
        }
    }
}
