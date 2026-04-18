package de.familienwecker.famwake.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import de.familienwecker.famwake.MainActivity
import de.familienwecker.famwake.alarm.AlarmPlatformScheduler
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class AlarmScheduler(private val context: Context) : AlarmPlatformScheduler {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Plant einen exakten Systemwecker. Verwendet [AlarmManager.setAlarmClock] um
     * aggressive Doze-Modes von Herstellern zu umgehen.
     * Speichert die Alarm-Daten zusätzlich in [AlarmBackupPrefs] (plain SharedPreferences),
     * damit der [BootReceiver] sie nach einem Reboot lesen kann – noch bevor der erste
     * Unlock erfolgt ist (EncryptedSharedPreferences wären da nicht verfügbar).
     *
     * @param onPermissionDenied Callback wenn SCHEDULE_EXACT_ALARM fehlt (Android 12+).
     */
    override fun scheduleWakeUp(
        wakeUpTime: LocalDateTime,
        memberId: String,
        memberName: String,
        soundUri: String?,
        isSnooze: Boolean,
        onPermissionDenied: (() -> Unit)?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                onPermissionDenied?.invoke()
                return
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("MEMBER_ID", memberId)
            putExtra("MEMBER_NAME", memberName)
            soundUri?.let { putExtra("SOUND_URI", it) }
        }

        // Bitmask verhindert Int.MIN_VALUE-Kollision bei hashCode()
        // Snooze-Alarme erhalten einen eigenen Slot (Suffix)
        val idForHash = if (isSnooze) memberId + "_snooze" else memberId
        val requestCode = idForHash.hashCode().and(0x7fffffff)

        // FLAG_CANCEL_CURRENT: PendingIntent immer neu erstellen (clean slate).
        // Kombinieren von FLAG_UPDATE_CURRENT + FLAG_IMMUTABLE kann auf manchen Geräten
        // dazu führen, dass AlarmManager den Receiver nie aufruft.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // AlarmClockInfo benötigt als Show-Intent eine getActivity-Intent (nicht getBroadcast).
        // Fehler hier führen auf manchen Android-Versionen dazu, dass der Receiver nie aufgerufen wird.
        val showIntent = PendingIntent.getActivity(
            context,
            requestCode + 1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeInMillis = wakeUpTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val alarmClockInfo = AlarmManager.AlarmClockInfo(timeInMillis, showIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        // Backup in plain SharedPreferences – lesbar auch vor erstem Unlock nach Reboot
        AlarmBackupPrefs.save(context, memberId, memberName, soundUri, timeInMillis)
    }

    override fun cancelWakeUp(memberId: String, isSnooze: Boolean) {
        val intent = Intent(context, AlarmReceiver::class.java)
        // Gleiche Bitmask wie in scheduleWakeUp – damit stimmen schedule und cancel überein
        val idForHash = if (isSnooze) memberId + "_snooze" else memberId
        val requestCode = idForHash.hashCode().and(0x7fffffff)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        // Backup-Eintrag ebenfalls löschen, aber nur wenn er diesem Member gehört
        AlarmBackupPrefs.clear(context, memberId)
    }
}

