package de.familienwecker.famwake.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.familienwecker.famwake.model.toKmpLocalDateTime
import de.familienwecker.famwake.R
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

        val backups = AlarmBackupPrefs.getAllBackups(context)
        if (backups.isEmpty()) return

        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val scheduler = AlarmScheduler(context)

        for (backup in backups) {
            val savedMillis = backup.wakeUpMillis
            if (savedMillis == 0L) continue

            val savedDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(savedMillis), zone)
            val rebootDecision = de.familienwecker.famwake.algorithm.AlarmPlanner.decideRebootAlarm(
                savedDateTime = savedDateTime.toKmpLocalDateTime(),
                now = now.toKmpLocalDateTime()
            )
            val isMissedAndShouldRingNow = rebootDecision.isImmediateMissed
            if (rebootDecision.notifyMissed && rebootDecision.missedAlarmTime != null) {
                val alarmTime = rebootDecision.missedAlarmTime!!
                try {
                    val channelName = context.getString(R.string.notif_channel_missed_alarm)
                    val channel = android.app.NotificationChannel(
                        "missed_alarm", channelName,
                        android.app.NotificationManager.IMPORTANCE_HIGH
                    )
                    val nm = context.getSystemService(android.app.NotificationManager::class.java)
                    nm?.createNotificationChannel(channel)
                    val notificationText = context.getString(R.string.notif_alarm_missed, alarmTime.hour, alarmTime.minute)
                    val notification = android.app.Notification.Builder(context, "missed_alarm")
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle(backup.memberName.ifEmpty { "FamWake" })
                        .setContentText(notificationText)
                        .setAutoCancel(true)
                        .build()
                    nm?.notify(backup.memberId.hashCode().and(0x7fffffff), notification)
                } catch (e: Exception) {
                    Log.w("BootReceiver", "Missed alarm notification failed: ${e.message}", e)
                }
            }

            scheduler.scheduleWakeUp(
                wakeUpTime = rebootDecision.targetDateTime,
                memberId   = backup.memberId,
                memberName = backup.memberName,
                soundUri   = backup.soundUri,
                persistBackup = !isMissedAndShouldRingNow
            )

            // Snooze-Alarm wiederherstellen, falls einer aktiv war
            val snoozeMillis = backup.snoozeUntilMillis
            if (snoozeMillis > 0L) {
                val snoozeDateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(snoozeMillis), zone
                )
                if (snoozeDateTime.isAfter(now)) {
                    // Snooze liegt noch in der Zukunft → als Snooze-Alarm planen
                    scheduler.scheduleWakeUp(
                        wakeUpTime = snoozeDateTime.toKmpLocalDateTime(),
                        memberId   = backup.memberId,
                        memberName = backup.memberName,
                        soundUri   = backup.soundUri,
                        isSnooze   = true
                    )
                } else {
                    // Snooze abgelaufen → Backup aufräumen
                    AlarmBackupPrefs.clearSnooze(context, backup.memberId)
                }
            }
        }
    }
}
