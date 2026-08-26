package de.familienwecker.famwake.tile

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import de.familienwecker.famwake.FamWakeApplication
import de.familienwecker.famwake.R
import de.familienwecker.famwake.alarm.AlarmBackupPrefs
import de.familienwecker.famwake.alarm.AlarmScheduler
import de.familienwecker.famwake.model.toKmpLocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Quick Settings Tile Service für Android.
 * Ermöglicht das Umschalten des Weckers (AN / AUS) direkt aus dem Systemmenü.
 */
@RequiresApi(Build.VERSION_CODES.N)
class FamWakeTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val app = application as? FamWakeApplication ?: return
        val currentEnabled = app.appSettings.isAlarmEnabled.value
        val newEnabled = !currentEnabled

        app.appSettings.setAlarmEnabled(newEnabled)

        val scheduler = AlarmScheduler(this)
        val memberId = app.appSettings.myMemberId.value

        if (!newEnabled) {
            if (memberId != null) {
                scheduler.cancelWakeUp(memberId)
            }
        } else {
            // Wenn Wecker wieder aktiviert wird: Backup-Alarm direkt reaktivieren
            val savedMillis = AlarmBackupPrefs.getWakeUpMillis(this)
            val memberName = AlarmBackupPrefs.getMemberName(this) ?: ""
            val soundUri = AlarmBackupPrefs.getSoundUri(this)
            if (memberId != null && savedMillis > 0L) {
                val zone = ZoneId.systemDefault()
                val savedDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(savedMillis), zone)
                val alarmTime = savedDateTime.toLocalTime()
                val now = LocalDateTime.now(zone)
                val targetDateTime = if (now.toLocalTime().isBefore(alarmTime)) {
                    LocalDateTime.of(now.toLocalDate(), alarmTime)
                } else {
                    LocalDateTime.of(now.toLocalDate().plusDays(1), alarmTime)
                }
                scheduler.scheduleWakeUp(
                    wakeUpTime = targetDateTime.toKmpLocalDateTime(),
                    memberId = memberId,
                    memberName = memberName,
                    soundUri = soundUri
                )
            }
        }

        // Firestore Update im Hintergrund (sofern online und familyId vorhanden)
        val familyId = app.appSettings.familyId.value
        if (familyId != null && memberId != null) {
            serviceScope.launch(Dispatchers.IO) {
                try {
                    app.firebaseRepository.updateDeviceAlarmEnabled(familyId, memberId, newEnabled)
                } catch (_: Exception) { /* Best-effort sync */ }
            }
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val app = application as? FamWakeApplication
        val isEnabled = app?.appSettings?.isAlarmEnabled?.value ?: true

        tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.app_name_short)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_alarm_tile)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isEnabled) getString(R.string.quick_tile_alarm_on) else getString(R.string.quick_tile_alarm_off)
        }
        tile.updateTile()
    }
}
