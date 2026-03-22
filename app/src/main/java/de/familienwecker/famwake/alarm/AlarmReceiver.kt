package de.familienwecker.famwake.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import de.familienwecker.famwake.R
import de.familienwecker.famwake.ui.screens.RingingActivity
import androidx.core.net.toUri

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val defaultMemberName = context.getString(R.string.alarm_default_member)
        val memberName = intent.getStringExtra("MEMBER_NAME") ?: defaultMemberName
        val memberId = intent.getStringExtra("MEMBER_ID") ?: memberName

        val soundUriString = intent.getStringExtra("SOUND_URI")
        val soundUri = soundUriString?.let { it.toUri() }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        // Dynamische Channel-ID basierend auf dem Sound-URI.
        // Android cached Channel-Einstellungen (inkl. Sound) – neuer Sound benötigt neuen Channel.
        val soundHash = soundUri.toString().hashCode().coerceAtLeast(0)
        val dynamicChannelId = "ALARM_CHANNEL_S_$soundHash"

        val ringingIntent = Intent(context, RingingActivity::class.java).apply {
            putExtra("MEMBER_NAME", memberName)
            putExtra("MEMBER_ID", memberId)
            putExtra("FROM_NOTIFICATION", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // PRIMÄR: Activity direkt starten. BroadcastReceiver der durch setAlarmClock ausgelöst
        // wird, ist von den Background-Activity-Start-Einschränkungen ausgenommen.
        try {
            context.startActivity(ringingIntent)
        } catch (e: Exception) {
            android.util.Log.e("FamWake_Alarm", "AlarmReceiver: startActivity failed: ${e.message}")
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            memberId.hashCode(),
            ringingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelName = context.getString(R.string.alarm_channel_name)
        val channel = NotificationChannel(dynamicChannelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
            setBypassDnd(true)
            description = channelName
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(soundUri, audioAttributes)
        }
        notificationManager.createNotificationChannel(channel)

        val notificationTitle = context.getString(R.string.alarm_notification_title)
        val notificationText = context.getString(R.string.alarm_notification_text, memberName)

        // FALLBACK: Notification mit Full-Screen-Intent (z.B. wenn Activity-Start fehlschlägt)
        val notificationBuilder = NotificationCompat.Builder(context, dynamicChannelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            // Kein .setSound() hier: Ton wird ausschließlich von RingingActivity's
            // MediaPlayer mit USAGE_ALARM gespielt. Doppelton vermeiden.
            .setVibrate(longArrayOf(0, 500, 500, 500))
            .setAutoCancel(true)

        notificationManager.notify(memberId.hashCode().and(0x7fffffff), notificationBuilder.build())
    }
}

