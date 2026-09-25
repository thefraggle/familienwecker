package de.familienwecker.famwake.data

/**
 * Plattform-unabhängige App-Fehler (KMP-ready).
 * Enthält keine Android-Abhängigkeiten (kein R.string, kein UiText).
 * Die UI-Darstellung erfolgt über AppError.toUiText() im app-Modul.
 */
sealed class AppError {
    // Auth
    data object EmailOrPasswordEmpty : AppError()
    data object LoginFailed : AppError()
    data object RegistrationFailed : AppError()
    data object GoogleSignInFailed : AppError()
    data object UserNotFound : AppError()
    data object InvalidEmail : AppError()
    data object TooManyRequests : AppError()
    data object ResetFailed : AppError()
    data object EmailAlreadyInUse : AppError()
    data object WeakPassword : AppError()

    // Family
    data object FamilyNotFound : AppError()
    data object CodeGenerationFailed : AppError()
    data class PermissionDenied(val message: String? = null) : AppError()
    data object LoadMembersFailed : AppError()

    // Fallback
    data class Unknown(val message: String?) : AppError()
}
