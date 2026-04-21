package de.familienwecker.famwake

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * Zentrale Definition aller FCM Notification Channels.
 * Channels sind idempotent – mehrfaches Registrieren ist sicher.
 * User kann jeden Channel auf OS-Ebene individuell deaktivieren.
 */
object NotificationChannels {

    const val SCHEDULE_CHANGE = "schedule_change"
    const val FAMILY_EVENTS   = "family_events"

    fun register(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Reihenfolge/Zeit geändert – hohe Priorität (beeinflusst Morgenroutine direkt)
        manager.createNotificationChannel(
            NotificationChannel(
                SCHEDULE_CHANGE,
                context.getString(R.string.notif_channel_schedule_change_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_schedule_change_desc)
            }
        )

        // Familien-Events (join/leave) – normale Priorität
        manager.createNotificationChannel(
            NotificationChannel(
                FAMILY_EVENTS,
                context.getString(R.string.notif_channel_family_events_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_family_events_desc)
            }
        )
    }
}
