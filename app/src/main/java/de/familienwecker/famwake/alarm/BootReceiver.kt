package de.familienwecker.famwake.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.familienwecker.famwake.FamWakeApplication
import de.familienwecker.famwake.data.PreferencesRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Stellt nach einem Geräteneustart den Wecker wieder her.
 * Android löscht alle AlarmManager-Einträge beim Reboot. Dieser Receiver
 * liest die gespeicherten Präferenzen und plant den Alarm für den nächsten Morgen.
 *
 * Hinweis: Reagiert nur auf BOOT_COMPLETED (nach vollständigem Unlock), da
 * EncryptedSharedPreferences vor dem ersten Unlock nicht lesbar sind.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = (context.applicationContext as FamWakeApplication).preferencesRepository
        val familyId = prefs.familyId.value ?: return
        val memberId = prefs.myMemberId.value ?: return
        val memberName = prefs.myMemberName.value ?: ""
        val isEnabled = prefs.isAlarmEnabled.value

        if (!isEnabled || familyId.isBlank() || memberId.isBlank()) return

        // Fallback-Zeit 06:00 – wird vom ViewModel übersteuert sobald die App startet
        val now = LocalTime.now()
        val alarmTime = LocalTime.of(6, 0)
        val targetDate = if (now.isAfter(alarmTime)) LocalDate.now().plusDays(1) else LocalDate.now()
        val targetDateTime = LocalDateTime.of(targetDate, alarmTime)

        val scheduler = AlarmScheduler(context)
        scheduler.scheduleWakeUp(
            wakeUpTime = targetDateTime,
            memberId = memberId,
            memberName = memberName,
            soundUri = prefs.alarmSoundUri.value
        )
    }
}

