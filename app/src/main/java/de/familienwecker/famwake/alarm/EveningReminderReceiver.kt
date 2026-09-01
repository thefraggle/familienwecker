package de.familienwecker.famwake.alarm

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import de.familienwecker.famwake.FamWakeApplication
import de.familienwecker.famwake.MainActivity
import de.familienwecker.famwake.NotificationChannels
import de.familienwecker.famwake.R

class EveningReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        // Zunächst für den nächsten Tag (20:30) neu einplanen
        EveningReminderScheduler.schedule(context)

        val app = context.applicationContext as? FamWakeApplication ?: return
        val appSettings = app.appSettings

        // Prüfen, ob der Switch aktiv und Onboarding abgeschlossen ist
        if (!appSettings.isEveningReminderEnabled.value || !appSettings.onboardingCompleted.value) {
            return
        }

        // Berechtigung auf Android 13+ prüfen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.EVENING_REMINDER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notif_evening_reminder_title))
            .setContentText(context.getString(R.string.notif_evening_reminder_desc))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notif_evening_reminder_desc)))
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(102030, notification)
        } catch (_: SecurityException) {
            // Fehlende Permission auf OS-Ebene abfangen
        }
    }
}
