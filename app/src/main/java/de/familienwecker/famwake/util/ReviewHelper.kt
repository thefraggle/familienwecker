package de.familienwecker.famwake.util

import android.app.Activity
import android.util.Log
import de.familienwecker.famwake.BuildConfig
import com.google.android.play.core.review.ReviewManagerFactory
import de.familienwecker.famwake.data.AppSettings

object ReviewHelper {
    private const val TAG = "ReviewHelper"
    private const val SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000L
    private const val THIRTY_DAYS_MS = 30 * 24 * 60 * 60 * 1000L

    /**
     * Prüft ob die Bedingungen für den automatischen Review-Prompt erfüllt sind:
     * - Mindestens 7 Tage seit Erstinstallation (User hat die App genug kennengelernt)
     * - Nicht zwischen 6–9 Uhr morgens (User will nicht gestört werden nach dem Aufstehen)
     * - Nicht erneut innerhalb von 30 Tagen (Spam-Schutz; Play API limitiert zusätzlich)
     */
    fun shouldShowReview(prefs: AppSettings): Boolean {
        val now = System.currentTimeMillis()
        val installTime = prefs.installTime.value

        // Ersten Installationszeitpunkt setzen, falls noch nicht geschehen
        if (installTime == 0L) {
            prefs.setInstallTime(now)
            return false
        }

        val longEnoughInstalled = (now - installTime) >= SEVEN_DAYS_MS

        // Morgensperre: zwischen 6:00 und 9:00 Uhr kein Review-Prompt
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val notMorning = currentHour < 6 || currentHour >= 9

        val lastPromptTime = prefs.lastReviewPromptTime.value
        val notRecentlyPrompted = (now - lastPromptTime) >= THIRTY_DAYS_MS

        val shouldShow = longEnoughInstalled && notMorning && notRecentlyPrompted
        
        if (BuildConfig.DEBUG) Log.d(TAG, "shouldShowReview: installed7d=$longEnoughInstalled, notMorning=$notMorning(h=$currentHour), cooldown30d=$notRecentlyPrompted -> $shouldShow")
        return shouldShow
    }

    /**
     * Startet den In-App Review Flow nach einer Settings-/Member-Änderung.
     * Wird automatisch von AddMemberScreen (nach Speichern) und MainScreen
     * (nach Alarm-Toggle) aufgerufen – kein manueller Button nötig.
     */
    fun launchReview(activity: Activity, prefs: AppSettings) {
        if (!shouldShowReview(prefs)) return

        if (BuildConfig.DEBUG) Log.d(TAG, "Launching Review Flow...")
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    prefs.setLastReviewPromptTime(System.currentTimeMillis())
                    if (BuildConfig.DEBUG) Log.d(TAG, "Review Flow finished, timestamp updated.")
                }
            } else {
                if (BuildConfig.DEBUG) Log.e(TAG, "Review request failed: ${task.exception?.message}")
            }
        }
    }
}
