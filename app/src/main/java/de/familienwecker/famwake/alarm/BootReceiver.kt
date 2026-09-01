package de.familienwecker.famwake.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.familienwecker.famwake.model.toKmpLocalDateTime
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Stellt nach einem Geräteneustart den Wecker wieder her.
 * Android löscht alle AlarmManager-Einträge beim Reboot.
 *
 * Reagiert auf zwei Broadcasts:
 * - [Intent.ACTION_BOOT_COMPLETED]: nach vollständigem Unlock (normale Geräte)
 * - [Intent.ACTION_LOCKED_BOOT_COMPLETED]: sofort nach Boot, vor PIN-Eingabe
 *
 * [AlarmBackupPrefs] (plain SharedPreferences) werden verwendet statt
 * EncryptedSharedPreferences, da diese vor dem ersten Unlock nicht lesbar sind.
 * Der Receiver ist directBootAware (siehe AndroidManifest).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            EveningReminderScheduler.schedule(context)
        }

        // Plain Prefs – immer lesbar, auch vor erstem Unlock
        if (!AlarmBackupPrefs.isEnabled(context)) return

        val memberId   = AlarmBackupPrefs.getMemberId(context)   ?: return
        val memberName = AlarmBackupPrefs.getMemberName(context) ?: ""
        val soundUri   = AlarmBackupPrefs.getSoundUri(context)
        val savedMillis = AlarmBackupPrefs.getWakeUpMillis(context)

        if (savedMillis == 0L) return

        // Gespeicherten Zeitstempel lesen und Datum anpassen:
        // - Liegt der Zeitpunkt in der Zukunft → exakt diesen Zeitpunkt verwenden
        // - Liegt er in der Vergangenheit → gleiche Uhrzeit am nächsten Tag
        val zone = ZoneId.systemDefault()
        val savedDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(savedMillis), zone)
        val alarmTime: LocalTime = savedDateTime.toLocalTime()

        val now = LocalDateTime.now(zone)
        val targetDateTime = if (savedDateTime.isAfter(now)) {
            // Noch in der Zukunft – exakt diesen Termin wiederherstellen
            savedDateTime
        } else {
            // Bereits vergangen – prüfe wie lange her
            val minutesMissed = java.time.Duration.between(savedDateTime, now).toMinutes()
            if (minutesMissed <= 30) {
                // Kurz verpasst → sofort klingeln (in 10 Sekunden)
                now.plusSeconds(10)
            } else {
                // Zu lange her → Notification + nächster Tag
                try {
                    val channel = android.app.NotificationChannel(
                        "missed_alarm", "Verpasste Wecker",
                        android.app.NotificationManager.IMPORTANCE_HIGH
                    )
                    val nm = context.getSystemService(android.app.NotificationManager::class.java)
                    nm.createNotificationChannel(channel)
                    val notification = android.app.Notification.Builder(context, "missed_alarm")
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle(memberName.ifEmpty { "FamWake" })
                        .setContentText("Wecker verpasst (${alarmTime.hour}:${"%02d".format(alarmTime.minute)})")
                        .setAutoCancel(true)
                        .build()
                    nm.notify(9999, notification)
                } catch (_: Exception) { /* Best-effort */ }
                val nextDay = now.toLocalDate().plusDays(1)
                LocalDateTime.of(nextDay, alarmTime)
            }
        }

        val scheduler = AlarmScheduler(context)
        scheduler.scheduleWakeUp(
            wakeUpTime = targetDateTime.toKmpLocalDateTime(),
            memberId   = memberId,
            memberName = memberName,
            soundUri   = soundUri
        )

        // Snooze-Alarm wiederherstellen, falls einer aktiv war
        val snoozeMillis = AlarmBackupPrefs.getSnoozeUntilMillis(context)
        if (snoozeMillis > 0L) {
            val snoozeDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(snoozeMillis), zone
            )
            if (snoozeDateTime.isAfter(now)) {
                // Snooze liegt noch in der Zukunft → als Snooze-Alarm planen
                scheduler.scheduleWakeUp(
                    wakeUpTime = snoozeDateTime.toKmpLocalDateTime(),
                    memberId   = memberId,
                    memberName = memberName,
                    soundUri   = soundUri,
                    isSnooze   = true
                )
            } else {
                // Snooze abgelaufen → Backup aufräumen
                AlarmBackupPrefs.clearSnooze(context)
            }
        }
    }
}
