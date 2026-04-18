package de.familienwecker.famwake.util

import android.app.Activity
import android.util.Log
import de.familienwecker.famwake.BuildConfig
import android.widget.Toast
import com.google.android.play.core.review.ReviewManagerFactory
import de.familienwecker.famwake.data.AppSettings

object ReviewHelper {
    private const val TAG = "ReviewHelper"
    private const val SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000L
    private const val TWO_HOURS_MS = 2 * 60 * 60 * 1000L

    /**
     * Prüft, ob die Bedingungen für ein Review-Prompt erfüllt sind:
     * - Mindestens 7 Tage seit Installation
     * - Mindestens 2 Stunden seit dem letzten Alarm
     * - Optional: Nicht zu häufig (z.B. nur alle 30 Tage, falls abgelehnt - wird autom. von Play API gehandhabt)
     */
    fun shouldShowReview(prefs: AppSettings): Boolean {
        val now = System.currentTimeMillis()
        val installTime = prefs.installTime.value
        val lastAlarmTime = prefs.lastAlarmTime.value
        val lastPromptTime = prefs.lastReviewPromptTime.value

        val longEnoughInstalled = (now - installTime) >= SEVEN_DAYS_MS
        val notTooCloseToAlarm = (now - lastAlarmTime) >= TWO_HOURS_MS
        // Zusätzliche Sperre von 1 Woche zwischen unseren eigenen Checks, um die API nicht unnötig zu stressen
        val notRecentlyPrompted = (now - lastPromptTime) >= SEVEN_DAYS_MS

        val shouldShow = longEnoughInstalled && notTooCloseToAlarm && notRecentlyPrompted
        
        if (BuildConfig.DEBUG) Log.d(TAG, "shouldShowReview checking: Installed=$longEnoughInstalled, SafeFromAlarm=$notTooCloseToAlarm, NotRecent=$notRecentlyPrompted -> Result=$shouldShow")
        return shouldShow
    }

    /**
     * Startet den In-App Review Flow.
     * @param ignoreConstraints Wenn true (z.B. für Admins), werden die Zeitchecks ignoriert.
     */
    fun launchReview(activity: Activity, prefs: AppSettings, ignoreConstraints: Boolean = false) {
        if (!ignoreConstraints && !shouldShowReview(prefs)) {
            return
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "Launching Review Flow...")
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    // Flow abgeschlossen (egal ob erfolgreich oder nicht)
                    prefs.setLastReviewPromptTime(System.currentTimeMillis())
                    if (BuildConfig.DEBUG) Log.d(TAG, "Review Flow finished and timestamp updated.")
                }
            } else {
                if (BuildConfig.DEBUG) Log.e(TAG, "Review request failed: ${task.exception?.message}")
                if (ignoreConstraints) {
                    Toast.makeText(activity, "Review Flow Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
