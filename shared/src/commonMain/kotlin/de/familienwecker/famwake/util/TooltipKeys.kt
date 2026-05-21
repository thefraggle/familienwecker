package de.familienwecker.famwake.util

/**
 * Zentrale Konstanten für alle Tooltip-Keys.
 * KMP-ready: Kein Android-Import, nutzbar in commonMain.
 */
object TooltipKeys {
    const val AWAKE       = "TOOLTIP_SEEN_AWAKE"
    const val DRAG        = "TOOLTIP_SEEN_DRAG"
    const val WAKE_WINDOW = "TOOLTIP_SEEN_WAKE_WINDOW"
    const val BATHROOM    = "TOOLTIP_SEEN_BATHROOM"
    const val INVITE      = "TOOLTIP_SEEN_INVITE"
    const val SWITCH      = "TOOLTIP_SEEN_SWITCH"
    const val WEEKDAYS    = "TOOLTIP_SEEN_WEEKDAYS"
    const val ALARM_SOUND = "TOOLTIP_SEEN_ALARM_SOUND"
    const val BUFFER      = "TOOLTIP_SEEN_BUFFER"

    val ALL = listOf(AWAKE, DRAG, WAKE_WINDOW, BATHROOM, INVITE, SWITCH, WEEKDAYS, ALARM_SOUND, BUFFER)
}
