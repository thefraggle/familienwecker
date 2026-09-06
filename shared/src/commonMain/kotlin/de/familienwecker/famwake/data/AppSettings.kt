package de.familienwecker.famwake.data

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

interface AppSettings {
    val deviceId: String

    val myMemberId: StateFlow<String?>
    fun setMyMemberId(id: String?)
    
    val myMemberName: StateFlow<String?>
    fun setMyMemberName(name: String?)

    val familyId: StateFlow<String?>
    fun setFamilyId(id: String?)

    val joinCode: StateFlow<String?>
    fun setJoinCode(code: String?)

    val familyName: StateFlow<String?>
    fun setFamilyName(name: String?)

    val isLocalOnlyFamily: StateFlow<Boolean>
    fun setLocalOnlyFamily(isLocal: Boolean)

    val language: StateFlow<String>
    fun setLanguage(lang: String)

    val theme: StateFlow<String>
    fun setTheme(theme: String)

    val isAlarmEnabled: StateFlow<Boolean>
    fun setAlarmEnabled(enabled: Boolean)

    val onboardingCompleted: StateFlow<Boolean>
    fun setOnboardingCompleted(completed: Boolean)

    val alarmSoundUri: StateFlow<String?>
    fun setAlarmSoundUri(uri: String?)

    val isGentleWakeEnabled: StateFlow<Boolean>
    fun setGentleWakeEnabled(enabled: Boolean)

    val isEveningReminderEnabled: StateFlow<Boolean>
    fun setEveningReminderEnabled(enabled: Boolean)

    val isAwakeToday: StateFlow<Boolean>
    fun setAwakeToday(awake: Boolean)
    /** Liest den Wach-Status direkt aus SharedPrefs inkl. Datumsprüfung.
     *  Sicherer als isAwakeToday.value wenn der Flow veraltet sein könnte
     *  (App-Prozess läuft über Nacht ohne Resume). */
    fun isAwakeTodayEffective(): Boolean

    val snoozeUntil: StateFlow<kotlinx.datetime.LocalDateTime?>
    fun setSnoozeUntil(time: kotlinx.datetime.LocalDateTime?)

    val snoozeCount: StateFlow<Int>
    fun setSnoozeCount(count: Int)

    val tooltipsEnabled: StateFlow<Boolean>
    fun setTooltipsEnabled(enabled: Boolean)

    val tooltipsSeen: StateFlow<Map<String, Boolean>>
    fun setTooltipSeen(key: String, seen: Boolean)

    val installTime: StateFlow<Long>
    fun setInstallTime(time: Long)

    val lastAlarmTime: StateFlow<Long>
    fun setLastAlarmTime(time: Long)

    val lastReviewPromptTime: StateFlow<Long>
    fun setLastReviewPromptTime(time: Long)

    val lastFeedbackSentAt: StateFlow<Long>
    fun setLastFeedbackSentAt(time: Long)

    val pushNotificationsEnabled: StateFlow<Boolean>
    fun setPushNotificationsEnabled(enabled: Boolean)

    /** UID des zuletzt eingeloggten Users – dient zur Erkennung eines User-Wechsels auf gleichem Gerät. */
    val lastLoggedInUid: StateFlow<String?>
    fun setLastLoggedInUid(uid: String)

    /** Urlaubsmodus: Letzter Urlaubstag im Format YYYY-MM-DD (null = kein Urlaub aktiv). */
    val vacationUntil: StateFlow<String?>
    fun setVacationUntil(date: String?)

    fun clearAll()
}

class AppSettingsImpl(private val settings: ObservableSettings) : AppSettings {

    companion object {
        /** All valid language codes the app supports. "en" is the fallback for unknown values. */
        val SUPPORTED_LANGUAGE_CODES = setOf(
            "system", "en", "da", "de", "es", "fr", "it", "ja", "ko", "nl",
            "no", "pl", "pt", "ru", "sv", "tr", "uk", "zh",
            "id", "vi", "bn", "mr", "hi",
            "gsw", "ksh", "swg"
        )
    }



    override val deviceId: String = settings.getStringOrNull("DEVICE_ID") ?: run {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val newId = (1..16).map { chars.random() }.joinToString("")
        settings["DEVICE_ID"] = newId
        newId
    }

    private val _myMemberId = MutableStateFlow(settings.getStringOrNull("MY_MEMBER_ID"))
    override val myMemberId = _myMemberId.asStateFlow()

    override fun setMyMemberId(id: String?) {
        _myMemberId.value = id
        if (id == null) settings.remove("MY_MEMBER_ID") else settings["MY_MEMBER_ID"] = id
    }

    private val _myMemberName = MutableStateFlow(settings.getStringOrNull("MY_MEMBER_NAME"))
    override val myMemberName = _myMemberName.asStateFlow()

    override fun setMyMemberName(name: String?) {
        _myMemberName.value = name
        if (name == null) settings.remove("MY_MEMBER_NAME") else settings["MY_MEMBER_NAME"] = name
    }

    private val _familyId = MutableStateFlow(settings.getStringOrNull("FAMILY_ID"))
    override val familyId = _familyId.asStateFlow()

    override fun setFamilyId(id: String?) {
        _familyId.value = id
        if (id == null) settings.remove("FAMILY_ID") else settings["FAMILY_ID"] = id
    }

    private val _joinCode = MutableStateFlow(settings.getStringOrNull("JOIN_CODE"))
    override val joinCode = _joinCode.asStateFlow()

    override fun setJoinCode(code: String?) {
        _joinCode.value = code
        if (code == null) settings.remove("JOIN_CODE") else settings["JOIN_CODE"] = code
    }

    private val _familyName = MutableStateFlow(settings.getStringOrNull("FAMILY_NAME"))
    override val familyName = _familyName.asStateFlow()

    override fun setFamilyName(name: String?) {
        _familyName.value = name
        if (name == null) settings.remove("FAMILY_NAME") else settings["FAMILY_NAME"] = name
    }

    private val _isLocalOnlyFamily = MutableStateFlow(settings.getBoolean("IS_LOCAL_ONLY_FAMILY", false))
    override val isLocalOnlyFamily = _isLocalOnlyFamily.asStateFlow()

    override fun setLocalOnlyFamily(isLocal: Boolean) {
        _isLocalOnlyFamily.value = isLocal
        settings["IS_LOCAL_ONLY_FAMILY"] = isLocal
    }

    private val _language = MutableStateFlow(
        // Normalize stored value: treat unknown codes as "en" on load
        settings.getString("APP_LANGUAGE", "system").let { stored ->
            if (stored in SUPPORTED_LANGUAGE_CODES) stored else "en"
        }
    )
    override val language = _language.asStateFlow()

    override fun setLanguage(lang: String) {
        // EN is the default/fallback for any unsupported language code
        val normalized = if (lang in SUPPORTED_LANGUAGE_CODES) lang else "en"
        _language.value = normalized
        settings["APP_LANGUAGE"] = normalized
    }

    private val _theme = MutableStateFlow(settings.getString("APP_THEME", "system"))
    override val theme = _theme.asStateFlow()

    override fun setTheme(theme: String) {
        _theme.value = theme
        settings["APP_THEME"] = theme
    }

    private val _isAlarmEnabled = MutableStateFlow(settings.getBoolean("ALARM_ENABLED", false))
    override val isAlarmEnabled = _isAlarmEnabled.asStateFlow()

    override fun setAlarmEnabled(enabled: Boolean) {
        _isAlarmEnabled.value = enabled
        settings["ALARM_ENABLED"] = enabled
    }

    private val _onboardingCompleted = MutableStateFlow(settings.getBoolean("ONBOARDING_COMPLETED", false))
    override val onboardingCompleted = _onboardingCompleted.asStateFlow()

    override fun setOnboardingCompleted(completed: Boolean) {
        _onboardingCompleted.value = completed
        settings["ONBOARDING_COMPLETED"] = completed
    }

    private val _alarmSoundUri = MutableStateFlow(settings.getStringOrNull("ALARM_SOUND_URI"))
    override val alarmSoundUri = _alarmSoundUri.asStateFlow()

    override fun setAlarmSoundUri(uri: String?) {
        _alarmSoundUri.value = uri
        if (uri == null) settings.remove("ALARM_SOUND_URI") else settings["ALARM_SOUND_URI"] = uri
    }

    private val _isGentleWakeEnabled = MutableStateFlow(settings.getBoolean("GENTLE_WAKE_ENABLED", true))
    override val isGentleWakeEnabled = _isGentleWakeEnabled.asStateFlow()

    override fun setGentleWakeEnabled(enabled: Boolean) {
        _isGentleWakeEnabled.value = enabled
        settings["GENTLE_WAKE_ENABLED"] = enabled
    }

    private val _isEveningReminderEnabled = MutableStateFlow(settings.getBoolean("EVENING_REMINDER_ENABLED", true))
    override val isEveningReminderEnabled = _isEveningReminderEnabled.asStateFlow()

    override fun setEveningReminderEnabled(enabled: Boolean) {
        _isEveningReminderEnabled.value = enabled
        settings["EVENING_REMINDER_ENABLED"] = enabled
    }

    private fun computeAwakeTodayEffective(): Boolean {
        if (!settings.getBoolean("AWAKE_TODAY", false)) return false
        val storedDate = settings.getStringOrNull("AWAKE_TODAY_DATE") ?: return false
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date.toString()
        return storedDate == today
    }

    private val _isAwakeToday = MutableStateFlow(computeAwakeTodayEffective())
    override val isAwakeToday = _isAwakeToday.asStateFlow()

    /** Live-Abfrage direkt aus SharedPrefs inkl. Datumsprüfung – unabhängig vom Flow-Wert. */
    override fun isAwakeTodayEffective(): Boolean = computeAwakeTodayEffective()

    override fun setAwakeToday(awake: Boolean) {
        _isAwakeToday.value = awake
        settings["AWAKE_TODAY"] = awake
        if (awake) {
            settings["AWAKE_TODAY_DATE"] = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date.toString()
        } else {
            settings.remove("AWAKE_TODAY_DATE")
        }
    }

    private val _snoozeUntil = MutableStateFlow(settings.getStringOrNull("SNOOZE_UNTIL")?.let {
        try { kotlinx.datetime.LocalDateTime.parse(it) } catch (e: Exception) { null }
    })
    override val snoozeUntil = _snoozeUntil.asStateFlow()

    override fun setSnoozeUntil(time: kotlinx.datetime.LocalDateTime?) {
        _snoozeUntil.value = time
        if (time == null) settings.remove("SNOOZE_UNTIL") else settings["SNOOZE_UNTIL"] = time.toString()
    }

    private val _snoozeCount = MutableStateFlow(settings.getInt("SNOOZE_COUNT", 0))
    override val snoozeCount = _snoozeCount.asStateFlow()

    override fun setSnoozeCount(count: Int) {
        _snoozeCount.value = count
        settings["SNOOZE_COUNT"] = count
    }

    private val _tooltipsEnabled = MutableStateFlow(settings.getBoolean("TOOLTIPS_ENABLED", true))
    override val tooltipsEnabled = _tooltipsEnabled.asStateFlow()

    override fun setTooltipsEnabled(enabled: Boolean) {
        _tooltipsEnabled.value = enabled
        settings["TOOLTIPS_ENABLED"] = enabled
    }

    private val tooltipKeys = listOf(
        "TOOLTIP_SEEN_AWAKE", "TOOLTIP_SEEN_DRAG", "TOOLTIP_SEEN_WAKE_WINDOW",
        "TOOLTIP_SEEN_BATHROOM", "TOOLTIP_SEEN_INVITE", "TOOLTIP_SEEN_SWITCH",
        "TOOLTIP_SEEN_WEEKDAYS", "TOOLTIP_SEEN_ALARM_SOUND", "TOOLTIP_SEEN_BUFFER"
    )

    private val _tooltipsSeen = MutableStateFlow(tooltipKeys.associateWith { settings.getBoolean(it, false) })
    override val tooltipsSeen = _tooltipsSeen.asStateFlow()

    override fun setTooltipSeen(key: String, seen: Boolean) {
        val current = _tooltipsSeen.value.toMutableMap()
        current[key] = seen
        _tooltipsSeen.value = current
        settings[key] = seen
    }

    private val _installTime = MutableStateFlow(settings.getLong("INSTALL_TIME", 0L))
    override val installTime = _installTime.asStateFlow()

    override fun setInstallTime(time: Long) {
        _installTime.value = time
        settings["INSTALL_TIME"] = time
    }

    private val _lastAlarmTime = MutableStateFlow(settings.getLong("LAST_ALARM_TIME", 0L))
    override val lastAlarmTime = _lastAlarmTime.asStateFlow()

    override fun setLastAlarmTime(time: Long) {
        _lastAlarmTime.value = time
        settings["LAST_ALARM_TIME"] = time
    }

    private val _lastReviewPromptTime = MutableStateFlow(settings.getLong("LAST_REVIEW_PROMPT_TIME", 0L))
    override val lastReviewPromptTime = _lastReviewPromptTime.asStateFlow()

    override fun setLastReviewPromptTime(time: Long) {
        _lastReviewPromptTime.value = time
        settings["LAST_REVIEW_PROMPT_TIME"] = time
    }

    private val _lastFeedbackSentAt = MutableStateFlow(settings.getLong("LAST_FEEDBACK_SENT_AT", 0L))
    override val lastFeedbackSentAt = _lastFeedbackSentAt.asStateFlow()

    override fun setLastFeedbackSentAt(time: Long) {
        _lastFeedbackSentAt.value = time
        settings["LAST_FEEDBACK_SENT_AT"] = time
    }

    private val _pushNotificationsEnabled = MutableStateFlow(settings.getBoolean("PUSH_NOTIFICATIONS_ENABLED", true))
    override val pushNotificationsEnabled = _pushNotificationsEnabled.asStateFlow()

    override fun setPushNotificationsEnabled(enabled: Boolean) {
        _pushNotificationsEnabled.value = enabled
        settings["PUSH_NOTIFICATIONS_ENABLED"] = enabled
    }

    private val _lastLoggedInUid = MutableStateFlow(settings.getStringOrNull("LAST_LOGGED_IN_UID"))
    override val lastLoggedInUid = _lastLoggedInUid.asStateFlow()

    override fun setLastLoggedInUid(uid: String) {
        _lastLoggedInUid.value = uid
        settings["LAST_LOGGED_IN_UID"] = uid
    }

    private val _vacationUntil = MutableStateFlow(settings.getStringOrNull("VACATION_UNTIL"))
    override val vacationUntil = _vacationUntil.asStateFlow()

    override fun setVacationUntil(date: String?) {
        _vacationUntil.value = date
        if (date == null) settings.remove("VACATION_UNTIL") else settings["VACATION_UNTIL"] = date
    }

    override fun clearAll() {
        settings.clear()
        _isAlarmEnabled.value = true
        _isAwakeToday.value = false
        _snoozeUntil.value = null
        _snoozeCount.value = 0
        _onboardingCompleted.value = false
        _isLocalOnlyFamily.value = false
        _tooltipsEnabled.value = true
        _tooltipsSeen.value = tooltipKeys.associateWith { false }
        _installTime.value = 0L
        _lastAlarmTime.value = 0L
        _lastReviewPromptTime.value = 0L
        _lastFeedbackSentAt.value = 0L
        _lastLoggedInUid.value = null
        _vacationUntil.value = null
    }
}
