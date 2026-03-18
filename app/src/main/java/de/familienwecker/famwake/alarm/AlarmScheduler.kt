package de.familienwecker.famwake.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {
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
    fun scheduleWakeUp(
        wakeUpTime: LocalDateTime,
        memberId: String,
        memberName: String,
        soundUri: String? = null,
        isSnooze: Boolean = false,
        onPermissionDenied: (() -> Unit)? = null
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
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeInMillis = wakeUpTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val alarmClockInfo = AlarmManager.AlarmClockInfo(timeInMillis, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        // Backup in plain SharedPreferences – lesbar auch vor erstem Unlock nach Reboot
        AlarmBackupPrefs.save(context, memberId, memberName, soundUri, timeInMillis)
    }

    fun cancelWakeUp(memberId: String, isSnooze: Boolean = false) {
        val intent = Intent(context, AlarmReceiver::class.java)
        // Gleiche Bitmask wie in scheduleWakeUp – damit stimmen schedule und cancel überein
        val idForHash = if (isSnooze) memberId + "_snooze" else memberId
        val requestCode = idForHash.hashCode().and(0x7fffffff)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        // Backup-Eintrag ebenfalls löschen
        AlarmBackupPrefs.clear(context)
    }
}

