package de.familienwecker.famwake.model

/**
 * Typsichere Repräsentation von Scheduler-Ausgaben.
 * Ersetzt rohe Strings in FamilySchedule.message – das ViewModel übersetzt
 * diese Codes in lokalisierte UiText-Ressourcen.
 */
sealed class ScheduleMessage {
    /** Plan erfolgreich und optimal berechnet. */
    object OptimalPlan : ScheduleMessage()

    /** Keine aktiven Mitglieder vorhanden (alle pausiert). */
    object NoActiveMembers : ScheduleMessage()

    /** Kein gültiger Zeitplan gefunden (zu viele Konflikte). */
    object NoValidScheduleFound : ScheduleMessage()

    /** Zeiten wurden angepasst, um die Reihenfolge einzuhalten. */
    data class TimeAdjusted(val minutes: Int) : ScheduleMessage()

    /** Frühstück wurde verkürzt, um die Reihenfolge zu ermöglichen. */
    data class BreakfastReduced(val minutes: Int) : ScheduleMessage()

    /** Frühstück verkürzt UND Zeiten angepasst. */
    data class BreakfastAndTimeAdjusted(val breakfast: Int, val shift: Int) : ScheduleMessage()

    /** Konflikt bei einem konkreten Mitglied. */
    data class MemberConflict(val memberName: String) : ScheduleMessage()

    /**
     * Kein aktiver Weckplan (Wecker ausgeschaltet oder alle Mitglieder herausgefiltert).
     * Ersetzt den ehemaligen Magic-String "no_active_schedule".
     */
    object NoActiveSchedule : ScheduleMessage()
}
