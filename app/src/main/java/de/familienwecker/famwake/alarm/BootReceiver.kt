package de.familienwecker.famwake.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
            // Bereits vergangen – selbe Uhrzeit, nächster Tag
            val nextDay = now.toLocalDate().plusDays(1)
            LocalDateTime.of(nextDay, alarmTime)
        }

        val scheduler = AlarmScheduler(context)
        scheduler.scheduleWakeUp(
            wakeUpTime = targetDateTime,
            memberId   = memberId,
            memberName = memberName,
            soundUri   = soundUri
        )
    }
}
