package de.familienwecker.famwake.util

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri

object AlarmPermissionUtils {
    /**
     * Prüft, ob ab Android 12 (S) die Berechtigung für exakte Alarme vergeben ist.
     * Vor Android 12 oder wenn AlarmManager fehlt, wird immer true zurückgegeben.
     */
    fun hasExactAlarmPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            return alarmManager?.canScheduleExactAlarms() ?: true
        }
        return true
    }

    /**
     * Öffnet die Systemeinstellungen, damit der Nutzer die Berechtigung
     * "Alarme & Erinnerungen" vergeben kann.
     */
    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = "package:${context.packageName}".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Falls der Vendor/OEM den Intent entfernt hat, fallback auf App-Details
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${context.packageName}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(fallbackIntent)
                } catch (ex: Exception) {
                    if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                        android.util.Log.e("AlarmPermissionUtils", "Cannot open exact alarm permission settings", ex)
                    }
                }
            }
        }
    }

    /**
     * Prüft, ob ab Android 14 (UPSIDE_DOWN_CAKE) die Berechtigung für Full-Screen Intents
     * (Alarme auf dem Lockscreen) vergeben ist.
     */
    fun hasFullScreenIntentPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            return nm?.canUseFullScreenIntent() ?: true
        }
        return true
    }

    /**
     * Öffnet die Systemeinstellungen für die Full-Screen Intent Berechtigung (Android 14+).
     */
    fun requestFullScreenIntentPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = "package:${context.packageName}".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.e("AlarmPermissionUtils", "Cannot open full screen intent permission settings", e)
                }
            }
        }
    }
}
