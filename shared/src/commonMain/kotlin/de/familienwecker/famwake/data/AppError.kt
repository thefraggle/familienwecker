package de.familienwecker.famwake.data

/**
 * Plattform-unabhängige App-Fehler (KMP-ready).
 * Enthält keine Android-Abhängigkeiten (kein R.string, kein UiText).
 * Die UI-Darstellung erfolgt über AppError.toUiText() im app-Modul.
 */
sealed class AppError {
    // Auth
    object EmailOrPasswordEmpty : AppError()
    object LoginFailed : AppError()
    object RegistrationFailed : AppError()
    object GoogleSignInFailed : AppError()
    object UserNotFound : AppError()
    object InvalidEmail : AppError()
    object TooManyRequests : AppError()
    object ResetFailed : AppError()
    object EmailAlreadyInUse : AppError()
    object WeakPassword : AppError()

    // Family
    object FamilyNotFound : AppError()
    object CodeGenerationFailed : AppError()
    data class PermissionDenied(val message: String? = null) : AppError()
    object LoadMembersFailed : AppError()

    // Fallback
    data class Unknown(val message: String?) : AppError()
}
