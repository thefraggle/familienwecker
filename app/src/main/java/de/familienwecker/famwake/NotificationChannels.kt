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
    const val EVENING_REMINDER = "evening_reminder"

    fun register(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Push-Channels bewusst IMPORTANCE_LOW: still (kein Ton/Vibration), aber sichtbar im Tray.
        // Der Alarm-Channel (ALARM_CHANNEL_S_*) in AlarmReceiver bleibt IMPORTANCE_HIGH + USAGE_ALARM.
        manager.createNotificationChannel(
            NotificationChannel(
                SCHEDULE_CHANGE,
                context.getString(R.string.notif_channel_schedule_change_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_schedule_change_desc)
            }
        )

        // Familien-Events (join/leave) – ebenfalls still
        manager.createNotificationChannel(
            NotificationChannel(
                FAMILY_EVENTS,
                context.getString(R.string.notif_channel_family_events_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_family_events_desc)
            }
        )

        // Abendliche Erinnerung (20:30 Uhr)
        manager.createNotificationChannel(
            NotificationChannel(
                EVENING_REMINDER,
                context.getString(R.string.notif_channel_evening_reminder_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_evening_reminder_desc)
            }
        )
    }
}
