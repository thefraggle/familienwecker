package de.familienwecker.famwake.model

import kotlinx.serialization.Serializable

/**
 * Typsichere Repräsentation von Scheduler-Ausgaben.
 * Ersetzt rohe Strings in FamilySchedule.message – das ViewModel übersetzt
 * diese Codes in lokalisierte UiText-Ressourcen.
 */
@Serializable
sealed class ScheduleMessage {
    /** Plan erfolgreich und optimal berechnet. */
    @Serializable
    object OptimalPlan : ScheduleMessage()

    /** Keine aktiven Mitglieder vorhanden (alle pausiert). */
    @Serializable
    object NoActiveMembers : ScheduleMessage()

    /** Kein gültiger Zeitplan gefunden (zu viele Konflikte). */
    @Serializable
    object NoValidScheduleFound : ScheduleMessage()

    /** Zeiten wurden angepasst, um die Reihenfolge einzuhalten. */
    @Serializable
    data class TimeAdjusted(val minutes: Int) : ScheduleMessage()

    /** Frühstück wurde verkürzt, um die Reihenfolge zu ermöglichen. */
    @Serializable
    data class BreakfastReduced(val minutes: Int) : ScheduleMessage()

    /** Frühstück verkürzt UND Zeiten angepasst. */
    @Serializable
    data class BreakfastAndTimeAdjusted(val breakfast: Int, val shift: Int) : ScheduleMessage()

    /** Konflikt bei einem konkreten Mitglied. */
    @Serializable
    data class MemberConflict(val memberName: String) : ScheduleMessage()

    /**
     * Kein aktiver Weckplan (Wecker ausgeschaltet oder alle Mitglieder herausgefiltert).
     * Ersetzt den ehemaligen Magic-String "no_active_schedule".
     */
    @Serializable
    object NoActiveSchedule : ScheduleMessage()
}
