package de.familienwecker.famwake.ui.viewmodel

import android.app.Activity
import de.familienwecker.famwake.R
import de.familienwecker.famwake.ui.util.UiText
import de.familienwecker.famwake.util.ReviewHelper
import kotlinx.coroutines.launch

// ─── Settings & Admin ─────────────────────────────────────────────────────────

fun FamilyViewModel.setLanguage(lang: String) {
    appSettings.setLanguage(lang)
}

fun FamilyViewModel.setThemePreference(theme: String) {
    appSettings.setTheme(theme)
}

fun FamilyViewModel.checkAndShowReview(activity: Activity, ignoreConstraints: Boolean = false) {
    ReviewHelper.launchReview(activity, appSettings, ignoreConstraints)
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
        } else {
            _feedbackError.value = UiText.StringResource(R.string.error_unknown)
        }
        _isSendingFeedback.value = false
    }
}

fun FamilyViewModel.resetFeedbackState() {
    _feedbackSubmitted.value = false
    _feedbackError.value = null
}

/** Zeigt einen Offline-Hinweis wenn der Schreibvorgang ohne Verbindung ausgelöst wurde. */
internal fun FamilyViewModel.checkOfflineAndHint() {
    if (_isOffline.value) {
        _offlineWriteHint.value = UiText.StringResource(R.string.offline_write_hint)
    }
}
