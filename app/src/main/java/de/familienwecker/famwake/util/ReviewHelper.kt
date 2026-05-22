package de.familienwecker.famwake.util

import android.app.Activity
import android.util.Log
import de.familienwecker.famwake.BuildConfig
import com.google.android.play.core.review.ReviewManagerFactory
import de.familienwecker.famwake.data.AppSettings

object ReviewHelper {
    private const val TAG = "ReviewHelper"
    private const val THREE_DAYS_MS = 3 * 24 * 60 * 60 * 1000L
    private const val NINE_DAYS_MS = 9 * 24 * 60 * 60 * 1000L
    private const val FIVE_DAYS_MS = 5 * 24 * 60 * 60 * 1000L

    /**
     * Prüft ob die Bedingungen für den automatischen Review-Prompt erfüllt sind:
     * - Mindestens 3 Tage seit Erstinstallation für den ersten Prompt.
     * - Mindestens 9 Tage seit Erstinstallation für den zweiten Prompt (falls der erste Prompt vor Tag 9 lag und mindestens 5 Tage vergangen sind).
     * - Nicht zwischen 6–9 Uhr morgens (User will nach dem Aufstehen nicht gestört werden).
     */
    fun shouldShowReview(prefs: AppSettings): Boolean {
        val now = System.currentTimeMillis()
        val installTime = prefs.installTime.value

        // Ersten Installationszeitpunkt setzen, falls noch nicht geschehen
        if (installTime == 0L) {
            prefs.setInstallTime(now)
            return false
        }

        // Morgensperre: zwischen 6:00 und 9:00 Uhr kein Review-Prompt
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val notMorning = currentHour < 6 || currentHour >= 9
        if (!notMorning) {
            if (BuildConfig.DEBUG) Log.d(TAG, "shouldShowReview: Blocked during morning hours (6-9)")
            return false
        }

        val lastPromptTime = prefs.lastReviewPromptTime.value
        val timeSinceInstall = now - installTime

        val shouldShow = if (lastPromptTime == 0L) {
            timeSinceInstall >= THREE_DAYS_MS
        } else {
            val firstPromptTimeSinceInstall = lastPromptTime - installTime
            val timeSinceLastPrompt = now - lastPromptTime
            timeSinceInstall >= NINE_DAYS_MS && 
                    firstPromptTimeSinceInstall < NINE_DAYS_MS && 
                    timeSinceLastPrompt >= FIVE_DAYS_MS
        }
        
        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "shouldShowReview: timeSinceInstall=${timeSinceInstall / 1000 / 60}m, " +
                        "lastPromptTime=$lastPromptTime -> shouldShow=$shouldShow"
            )
        }
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
