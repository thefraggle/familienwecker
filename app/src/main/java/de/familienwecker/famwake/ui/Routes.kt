package de.familienwecker.famwake.ui

/**
 * M-1: Typsichere Navigationskonstanten statt Magic Strings in NavHost und navigate()-Aufrufen.
 * Typos führen jetzt zu Compile-Fehlern statt Laufzeit-Crashes.
 */
object Routes {
    const val LOADING = "loading"
    const val LOGIN = "login"
    const val SETUP = "setup"
    const val ONBOARDING = "onboarding"
    const val ONBOARDING_WELCOME = "onboarding_welcome"
    const val MAIN = "main"
    const val ADD_MEMBER = "addMember"
    const val SETTINGS = "settings"
    const val FEEDBACK = "feedback"
    /** Edit-Route mit Argument – via [editMember] erzeugen */
    const val EDIT_MEMBER = "editMember/{memberId}"

    fun editMember(memberId: String) = "editMember/$memberId"
}
