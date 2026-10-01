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
 * Reagiert auf Systemzeit- und Zeitzonenänderungen (inkl. Sommer-/Winterzeit-Umstellung / DST).
 * Stellt sicher, dass der Wecker immer zur korrekten lokalen Wanduhr-Zeit des Geräts klingelt.
 */
class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_DATE_CHANGED) return

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
            val alarmTime: LocalTime = savedDateTime.toLocalTime()

            // Wenn der geplante Alarm noch in der Zukunft liegt, behalten wir das Datum bei (z.B. Wecker für übermorgen).
            // Nur wenn er in der Vergangenheit liegt, wird auf heute bzw. morgen korrigiert.
            val targetDateTime = if (savedDateTime.isAfter(now)) {
                savedDateTime
            } else if (now.toLocalTime().isBefore(alarmTime)) {
                LocalDateTime.of(now.toLocalDate(), alarmTime)
            } else {
                LocalDateTime.of(now.toLocalDate().plusDays(1), alarmTime)
            }

            scheduler.scheduleWakeUp(
                wakeUpTime = targetDateTime.toKmpLocalDateTime(),
                memberId = backup.memberId,
                memberName = backup.memberName,
                soundUri = backup.soundUri
            )
        }
    }
}
