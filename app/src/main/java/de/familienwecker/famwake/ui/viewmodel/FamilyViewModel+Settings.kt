package de.familienwecker.famwake.ui.viewmodel

import android.app.Activity
import com.telemetrydeck.sdk.TelemetryDeck
import de.familienwecker.famwake.R
import de.familienwecker.famwake.ui.util.UiText
import de.familienwecker.famwake.util.ReviewHelper
import kotlinx.coroutines.launch

// ─── Settings & Admin ─────────────────────────────────────────────────────────

fun FamilyViewModel.setLanguage(lang: String) {
    appSettings.setLanguage(lang)
    TelemetryDeck.signal("settings.languageChanged", mapOf("language" to lang))
}

fun FamilyViewModel.setThemePreference(theme: String) {
    appSettings.setTheme(theme)
    TelemetryDeck.signal("settings.themeChanged", mapOf("theme" to theme))
}

fun FamilyViewModel.setTooltipsEnabled(enabled: Boolean) {
    appSettings.setTooltipsEnabled(enabled)
    // Konsolidiert: ein Event mit Payload statt zwei separater Events
    TelemetryDeck.signal("settings.tipsChanged", mapOf("enabled" to enabled.toString()))
}

fun FamilyViewModel.checkAndShowReview(activity: Activity) {
    ReviewHelper.launchReview(activity, appSettings)
}

fun FamilyViewModel.requestAdminStatsReport(onComplete: (Boolean) -> Unit) {
    scope.launch {
        val result = repository.requestAdminStatsReport()
        if (result.isSuccess) {
            onComplete(true)
        } else {
            _errorMessage.value = UiText.StringResource(R.string.error_report_failed, result.exceptionOrNull()?.localizedMessage ?: getApplication<android.app.Application>().getString(R.string.error_label))
            onComplete(false)
        }
    }
}

fun FamilyViewModel.sendFeedback(
    category: String,
    message: String,
    email: String,
    appVersion: String,
    device: String
) {
    // O1: Offline-Check vor dem Senden
    if (_isOffline.value) {
        _feedbackError.value = UiText.StringResource(R.string.error_offline_feedback)
        return
    }
    // S3: Client-seitiges Rate-Limiting – max. 1 Feedback pro 60 Sekunden
    val now = System.currentTimeMillis()
    val lastSent = appSettings.lastFeedbackSentAt.value
    if (lastSent > 0L && now - lastSent < 60_000L) {
        val secondsLeft = ((60_000L - (now - lastSent)) / 1000L).coerceAtLeast(1L)
        _feedbackError.value = UiText.StringResource(R.string.error_feedback_rate_limit, secondsLeft.toString())
        return
    }
    scope.launch {
        _isSendingFeedback.value = true
        _feedbackError.value = null
        val result = repository.sendFeedback(
            category = category,
            message = message.trim(),
            email = email.trim(),
            appVersion = appVersion,
            device = device
        )
        if (result.isSuccess) {
            _feedbackSubmitted.value = true
            appSettings.setLastFeedbackSentAt(System.currentTimeMillis())
            TelemetryDeck.signal("feedback.sent", mapOf("category" to category))
        } else {
            // F3: Fehlertyp differenzieren – Netzwerk vs. Rate-Limit vs. Server
            val ex = result.exceptionOrNull()
            _feedbackError.value = when {
                ex?.message?.contains("TOO_MANY_REQUESTS") == true ->
                    UiText.StringResource(R.string.error_feedback_rate_limit_server)
                ex?.message?.contains("INVALID_EMAIL") == true ->
                    UiText.StringResource(R.string.error_invalid_email)
                ex?.message?.contains("INVALID_MESSAGE") == true ->
                    UiText.StringResource(R.string.error_feedback_empty_message)
                else -> UiText.StringResource(R.string.error_unknown)
            }
        }
        _isSendingFeedback.value = false
    }
}

fun FamilyViewModel.resetFeedbackState() {
    _feedbackSubmitted.value = false
    _feedbackError.value = null
}

/**
 * Zeigt einen Offline-Hinweis wenn der Schreibvorgang ohne Verbindung ausgelöst wurde.
 * Kein harter Guard – der Schreibvorgang läuft durch (Firestore puffert offline).
 * Destruktive Operationen (leave/delete/join) haben zusätzliche eigene Guards.
 */
internal fun FamilyViewModel.checkOfflineAndHint() {
    if (_isOffline.value) {
        _offlineWriteHint.value = UiText.StringResource(R.string.offline_write_hint)
    }
}
