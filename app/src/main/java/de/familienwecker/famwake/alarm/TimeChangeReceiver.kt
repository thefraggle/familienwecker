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

        val memberId = AlarmBackupPrefs.getMemberId(context) ?: return
        val memberName = AlarmBackupPrefs.getMemberName(context) ?: ""
        val soundUri = AlarmBackupPrefs.getSoundUri(context)
        val savedMillis = AlarmBackupPrefs.getWakeUpMillis(context)

        if (savedMillis == 0L) return

        val zone = ZoneId.systemDefault()
        val savedDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(savedMillis), zone)
        val alarmTime: LocalTime = savedDateTime.toLocalTime()

        val now = LocalDateTime.now(zone)
        val targetDateTime = if (now.toLocalTime().isBefore(alarmTime)) {
            LocalDateTime.of(now.toLocalDate(), alarmTime)
        } else {
            LocalDateTime.of(now.toLocalDate().plusDays(1), alarmTime)
        }

        val scheduler = AlarmScheduler(context)
        scheduler.scheduleWakeUp(
            wakeUpTime = targetDateTime.toKmpLocalDateTime(),
            memberId = memberId,
            memberName = memberName,
            soundUri = soundUri
        )
    }
}
