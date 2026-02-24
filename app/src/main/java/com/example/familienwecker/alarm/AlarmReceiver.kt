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

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val memberName = intent.getStringExtra("MEMBER_NAME") ?: "Familienmitglied"
        
        val ringingIntent = Intent(context, RingingActivity::class.java).apply {
            putExtra("MEMBER_NAME", memberName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            memberName.hashCode(),
            ringingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("ALARM_CHANNEL", "Wecker", NotificationManager.IMPORTANCE_HIGH)
            channel.setBypassDnd(true)
            channel.description = "Familienwecker Alarm Channel"
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, "ALARM_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Familienwecker klingelt")
            .setContentText("Guten Morgen, $memberName!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)

        notificationManager.notify(memberName.hashCode(), notificationBuilder.build())
    }
}
