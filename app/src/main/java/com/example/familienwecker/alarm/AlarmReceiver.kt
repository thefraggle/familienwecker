package com.example.familienwecker.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.familienwecker.ui.screens.RingingActivity

// WICHTIG: Channel-ID erhöhen wenn Sound-Einstellungen des Kanals geändert werden.
// Android cached Kanal-Einstellungen nach erstmaliger Erstellung dauerhaft.
private const val ALARM_CHANNEL_ID = "ALARM_CHANNEL_V2"

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val memberName = intent.getStringExtra("MEMBER_NAME") ?: "Familienmitglied"
        val memberId = intent.getStringExtra("MEMBER_ID") ?: memberName

        val ringingIntent = Intent(context, RingingActivity::class.java).apply {
            putExtra("MEMBER_NAME", memberName)
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
            val channel = NotificationChannel(ALARM_CHANNEL_ID, "Wecker", NotificationManager.IMPORTANCE_HIGH).apply {
                setBypassDnd(true)
                description = "Familienwecker Alarm Channel"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                // Kein Sound im Channel – Ton kommt ausschließlich vom MediaPlayer in RingingActivity
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Familienwecker klingelt")
            .setContentText("Guten Morgen, $memberName!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setSound(null)  // Kein Notification-Sound – Ton kommt vom MediaPlayer in RingingActivity
            .setVibrate(longArrayOf(0, 500, 500, 500))
            .setAutoCancel(true)

        notificationManager.notify(memberId.hashCode(), notificationBuilder.build())
    }
}
