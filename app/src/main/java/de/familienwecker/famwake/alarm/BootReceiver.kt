package de.familienwecker.famwake.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.familienwecker.famwake.data.PreferencesRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * H-2: Stellt nach einem Geräteneustart den Wecker wieder her.
 * Android löscht alle AlarmManager-Einträge beim Reboot. Dieser Receiver
 * liest die gespeicherten Präferenzen und plant den Alarm für den nächsten Morgen.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) return

        val prefs = PreferencesRepository(context)
        val familyId = prefs.familyId.value ?: return
        val memberId = prefs.myMemberId.value ?: return
        val isEnabled = prefs.isAlarmEnabled.value

        // Alarm nur planen wenn aktiv und eine Family + Member ID vorhanden ist
        if (!isEnabled || familyId.isBlank() || memberId.isBlank()) return

        android.util.Log.i("BootReceiver", "Gerät neu gestartet – Alarm für Member $memberId wird neu geplant")

        // Alarm für heute oder morgen (06:00 als Fallback – wird vom Scheduler übersteuert,
        // sobald die App und der ViewModel starten und recalculateSchedule aufrufen)
        val now = LocalTime.now()
        val alarmTime = LocalTime.of(6, 0)
        val targetDate = if (now.isAfter(alarmTime)) LocalDate.now().plusDays(1) else LocalDate.now()
        val targetDateTime = LocalDateTime.of(targetDate, alarmTime)

        val scheduler = AlarmScheduler(context)
        scheduler.scheduleWakeUp(
            wakeUpTime = targetDateTime,
            memberId = memberId,
            memberName = prefs.familyName.value ?: "",
            soundUri = prefs.alarmSoundUri.value
        )
    }
}
