package com.example.familienwecker.alarm

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
import com.example.familienwecker.ui.screens.RingingActivity

// WICHTIG: Channel-ID erhöhen wenn Sound-Einstellungen des Kanals geändert werden.
// Android cached Kanal-Einstellungen nach erstmaliger Erstellung dauerhaft.
private const val ALARM_CHANNEL_ID = "ALARM_CHANNEL_V2"


class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val memberName = intent.getStringExtra("MEMBER_NAME") ?: "Familienmitglied"
        val memberId = intent.getStringExtra("MEMBER_ID") ?: memberName

        // Sound URI aus Preferences holen für Fallback im Notification-Channel
        val prefsRepo = com.example.familienwecker.data.PreferencesRepository(context)
        val soundUriString = prefsRepo.alarmSoundUri.value
        val soundUri = soundUriString?.let { Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        // Dynamische Channel-ID basierend auf dem Sound-URI
        // Da Android Channel-Einstellungen (Sound) cached, brauchen wir einen neuen Channel bei neuem Sound.
        val soundHash = soundUri.toString().hashCode().coerceAtLeast(0)
        val dynamicChannelId = "ALARM_CHANNEL_S_$soundHash"

        val ringingIntent = Intent(context, RingingActivity::class.java).apply {
            putExtra("MEMBER_NAME", memberName)
            putExtra("MEMBER_ID", memberId)
            putExtra("FROM_NOTIFICATION", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            memberId.hashCode(),
            ringingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Lösche alte Kanäle (optional, um "Müll" zu vermeiden, aber vorsichtig sein)
            // Hier erstellen wir einfach den neuen.
            val channel = NotificationChannel(dynamicChannelId, "Wecker", NotificationManager.IMPORTANCE_HIGH).apply {
                setBypassDnd(true)
                description = "Familienwecker Alarm Channel"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                // Sound im Channel setzen als Fallback
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, dynamicChannelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Familienwecker klingelt")
            .setContentText("Guten Morgen, $memberName!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setSound(soundUri) // Sound auch hier explizit setzen
            .setVibrate(longArrayOf(0, 500, 500, 500))
            .setAutoCancel(true)
            .setOngoing(true) // Schwerer wegzuswipen

        notificationManager.notify(memberId.hashCode(), notificationBuilder.build())
    }
}
