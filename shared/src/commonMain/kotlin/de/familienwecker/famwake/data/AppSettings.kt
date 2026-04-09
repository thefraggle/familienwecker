package de.familienwecker.famwake.data

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AppSettings {
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

    val language: StateFlow<String>
    fun setLanguage(lang: String)

    val theme: StateFlow<String>
    fun setTheme(theme: String)

    val isAlarmEnabled: StateFlow<Boolean>
    fun setAlarmEnabled(enabled: Boolean)

    // Speichert den Alarm-State vor dem Logout, damit er nach dem Login wiederhergestellt werden kann.
    val alarmStateBeforeLogout: StateFlow<Boolean>
    fun setAlarmStateBeforeLogout(enabled: Boolean)

    val onboardingCompleted: StateFlow<Boolean>
    fun setOnboardingCompleted(completed: Boolean)

    val alarmSoundUri: StateFlow<String?>
    fun setAlarmSoundUri(uri: String?)

    val isAwakeToday: StateFlow<Boolean>
    fun setAwakeToday(awake: Boolean)
    /** Liest den Wach-Status direkt aus SharedPrefs inkl. Datumsprüfung.
     *  Sicherer als isAwakeToday.value wenn der Flow veraltet sein könnte
     *  (App-Prozess läuft über Nacht ohne Resume). */
    fun isAwakeTodayEffective(): Boolean

    val snoozeUntil: StateFlow<kotlinx.datetime.LocalDateTime?>
    fun setSnoozeUntil(time: kotlinx.datetime.LocalDateTime?)

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

    fun clearAll()
}

class AppSettingsImpl(private val settings: ObservableSettings) : AppSettings {


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

    private val _language = MutableStateFlow(settings.getString("APP_LANGUAGE", "system"))
    override val language = _language.asStateFlow()

    override fun setLanguage(lang: String) {
        _language.value = lang
        settings["APP_LANGUAGE"] = lang
    }

    private val _theme = MutableStateFlow(settings.getString("APP_THEME", "dark"))
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

    private val _alarmStateBeforeLogout = MutableStateFlow(settings.getBoolean("ALARM_STATE_BEFORE_LOGOUT", false))
    override val alarmStateBeforeLogout = _alarmStateBeforeLogout.asStateFlow()

    override fun setAlarmStateBeforeLogout(enabled: Boolean) {
        _alarmStateBeforeLogout.value = enabled
        settings["ALARM_STATE_BEFORE_LOGOUT"] = enabled
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

    private fun computeAwakeTodayEffective(): Boolean {
        if (!settings.getBoolean("AWAKE_TODAY", false)) return false
        val storedDate = settings.getStringOrNull("AWAKE_TODAY_DATE") ?: return false
        // java.time ist in AppSettingsImpl (Android-only) verfügbar
        val today = java.time.LocalDate.now().toString()
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
            settings["AWAKE_TODAY_DATE"] = java.time.LocalDate.now().toString()
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

    private val _tooltipsEnabled = MutableStateFlow(settings.getBoolean("TOOLTIPS_ENABLED", true))
    override val tooltipsEnabled = _tooltipsEnabled.asStateFlow()

    override fun setTooltipsEnabled(enabled: Boolean) {
        _tooltipsEnabled.value = enabled
        settings["TOOLTIPS_ENABLED"] = enabled
    }

    private val tooltipKeys = listOf(
        "TOOLTIP_SEEN_AWAKE", "TOOLTIP_SEEN_DRAG", "TOOLTIP_SEEN_WAKE_WINDOW",
        "TOOLTIP_SEEN_BATHROOM", "TOOLTIP_SEEN_INVITE", "TOOLTIP_SEEN_SWITCH", "TOOLTIP_SEEN_WEEKDAYS"
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

    override fun clearAll() {
        setMyMemberId(null)
        setMyMemberName(null)
        setFamilyId(null)
        setJoinCode(null)
        setFamilyName(null)
        setAlarmEnabled(false)
        setAwakeToday(false)
        settings.remove("AWAKE_TODAY_DATE")
        setSnoozeUntil(null)
        // Note: language and theme persist
    }
}
