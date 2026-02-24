package com.example.familienwecker.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleWakeUp(wakeUpTime: LocalDateTime, memberId: String, memberName: String) {
        // Exakte Alarme benötigen ab Android 12 (API 31) die Berechtigung SCHEDULE_EXACT_ALARM
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(context, "Bitte erlaube exakte Wecker in den Einstellungen!", Toast.LENGTH_LONG).show()
                return
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("MEMBER_ID", memberId)
            putExtra("MEMBER_NAME", memberName)
        }

        // Nutze einen eindeutigen RequestCode (z.B. Hash vom MemberId), damit nicht alle Wecker denselben überschreiben
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            memberId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Konvertiere die Zielzeit in Millisekunden (UTC)
        val timeInMillis = wakeUpTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Setzt den Wecker als echten System-Wecker (AlarmClock)
        // Das umgeht aggressive Doze-Modes von Herstellern (wie Samsung) am besten
        val alarmClockInfo = AlarmManager.AlarmClockInfo(
            timeInMillis,
            pendingIntent
        )
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    fun cancelWakeUp(memberId: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            memberId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
