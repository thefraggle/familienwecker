package de.familienwecker.famwake.data

import android.content.Context
import android.content.SharedPreferences
import android.media.RingtoneManager
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_FILE = "FamilienweckerPrefs"
private const val ENCRYPTED_PREFS_FILE = "FamilienweckerPrefs_enc"
private const val MIGRATION_DONE_KEY = "enc_migration_v1"
private const val KEY_MY_MEMBER_ID = "MY_MEMBER_ID"
private const val KEY_MY_MEMBER_NAME = "MY_MEMBER_NAME"
private const val KEY_ALARM_SOUND_URI = "ALARM_SOUND_URI"
private const val KEY_FAMILY_ID = "FAMILY_ID"
private const val KEY_JOIN_CODE = "JOIN_CODE"
private const val KEY_FAMILY_NAME = "FAMILY_NAME"
private const val KEY_LANGUAGE = "APP_LANGUAGE"
private const val KEY_THEME = "APP_THEME"
private const val KEY_ALARM_ENABLED = "ALARM_ENABLED"
private const val KEY_SNOOZE_UNTIL = "SNOOZE_UNTIL"
private const val KEY_ONBOARDING_COMPLETED = "ONBOARDING_COMPLETED"
private const val KEY_TOOLTIPS_COMPLETED = "TOOLTIPS_COMPLETED"

class PreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = createEncryptedPrefs(context).also {
        migrateIfNeeded(context, it)
    }

    companion object {
        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    ENCRYPTED_PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                    android.util.Log.e("PreferencesRepository", "EncryptedSharedPreferences nicht verfügbar, Fallback: ${e.message}")
                }
                context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            }
        }

        private fun migrateIfNeeded(context: Context, encrypted: SharedPreferences) {
            val legacy = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            if (legacy === encrypted) return
            if (encrypted.contains(MIGRATION_DONE_KEY)) return
            if (legacy.all.isEmpty()) {
                encrypted.edit { putBoolean(MIGRATION_DONE_KEY, true) }
                return
            }
            encrypted.edit {
                legacy.getString(KEY_MY_MEMBER_ID, null)?.let { putString(KEY_MY_MEMBER_ID, it) }
                legacy.getString(KEY_MY_MEMBER_NAME, null)?.let { putString(KEY_MY_MEMBER_NAME, it) }
                legacy.getString(KEY_ALARM_SOUND_URI, null)?.let { putString(KEY_ALARM_SOUND_URI, it) }
                legacy.getString(KEY_FAMILY_ID, null)?.let { putString(KEY_FAMILY_ID, it) }
                legacy.getString(KEY_JOIN_CODE, null)?.let { putString(KEY_JOIN_CODE, it) }
                legacy.getString(KEY_FAMILY_NAME, null)?.let { putString(KEY_FAMILY_NAME, it) }
                legacy.getString(KEY_LANGUAGE, null)?.let { putString(KEY_LANGUAGE, it) }
                legacy.getString(KEY_THEME, null)?.let { putString(KEY_THEME, it) }
                putBoolean(KEY_ALARM_ENABLED, legacy.getBoolean(KEY_ALARM_ENABLED, false))
                legacy.getString(KEY_SNOOZE_UNTIL, null)?.let { putString(KEY_SNOOZE_UNTIL, it) }
                putBoolean(MIGRATION_DONE_KEY, true)
            }
            legacy.edit { clear() }
        }
    }

    private val _myMemberId = MutableStateFlow<String?>(prefs.getString(KEY_MY_MEMBER_ID, null))
    val myMemberId: StateFlow<String?> = _myMemberId.asStateFlow()

    private val _myMemberName = MutableStateFlow<String?>(prefs.getString(KEY_MY_MEMBER_NAME, null))
    val myMemberName: StateFlow<String?> = _myMemberName.asStateFlow()

    private val _alarmSoundUri = MutableStateFlow<String?>(
        try {
            prefs.getString(KEY_ALARM_SOUND_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString())
        } catch (e: Exception) {
            prefs.getString(KEY_ALARM_SOUND_URI, null)
        }
    )
    val alarmSoundUri: StateFlow<String?> = _alarmSoundUri.asStateFlow()

    private val _familyId = MutableStateFlow<String?>(prefs.getString(KEY_FAMILY_ID, null))
    val familyId: StateFlow<String?> = _familyId.asStateFlow()

    private val _joinCode = MutableStateFlow<String?>(prefs.getString(KEY_JOIN_CODE, null))
    val joinCode: StateFlow<String?> = _joinCode.asStateFlow()

    private val _familyName = MutableStateFlow<String?>(prefs.getString(KEY_FAMILY_NAME, null))
    val familyName: StateFlow<String?> = _familyName.asStateFlow()

    private val defaultLang = if (java.util.Locale.getDefault().language == "de") "de" else "en"
    private val _language = MutableStateFlow<String>(prefs.getString(KEY_LANGUAGE, defaultLang) ?: defaultLang)
    val language: StateFlow<String> = _language.asStateFlow()

    private val _themePreference = MutableStateFlow<String>(prefs.getString(KEY_THEME, "system") ?: "system")
    val themePreference: StateFlow<String> = _themePreference.asStateFlow()

    private val _isAlarmEnabled = MutableStateFlow<Boolean>(prefs.getBoolean(KEY_ALARM_ENABLED, false))
    val isAlarmEnabled: StateFlow<Boolean> = _isAlarmEnabled.asStateFlow()

    private val _snoozeUntil = MutableStateFlow<java.time.LocalDateTime?>(
        prefs.getString(KEY_SNOOZE_UNTIL, null)?.let {
            try { java.time.LocalDateTime.parse(it) } catch (e: Exception) { null }
        }
    )
    val snoozeUntil: StateFlow<java.time.LocalDateTime?> = _snoozeUntil.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow<Boolean>(prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false))
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _tooltipsCompleted = MutableStateFlow<Boolean>(prefs.getBoolean(KEY_TOOLTIPS_COMPLETED, false))
    val tooltipsCompleted: StateFlow<Boolean> = _tooltipsCompleted.asStateFlow()


    /**
     * Reagiert nur auf externe Schreiber (z.B. andere Prozesse).
     * Eigene Setters setzen den StateFlow direkt – kein Doppel-Emit.
     */
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            KEY_MY_MEMBER_ID -> {
                val v = sharedPreferences.getString(key, null)
                if (v != _myMemberId.value) _myMemberId.value = v
            }
            KEY_MY_MEMBER_NAME -> {
                val v = sharedPreferences.getString(key, null)
                if (v != _myMemberName.value) _myMemberName.value = v
            }
            KEY_ALARM_SOUND_URI -> {
                val v = sharedPreferences.getString(key, null)
                if (v != _alarmSoundUri.value) _alarmSoundUri.value = v
            }
            KEY_FAMILY_ID -> {
                val v = sharedPreferences.getString(key, null)
                if (v != _familyId.value) _familyId.value = v
            }
            KEY_JOIN_CODE -> {
                val v = sharedPreferences.getString(key, null)
                if (v != _joinCode.value) _joinCode.value = v
            }
            KEY_FAMILY_NAME -> {
                val v = sharedPreferences.getString(key, null)
                if (v != _familyName.value) _familyName.value = v
            }
            KEY_LANGUAGE -> {
                val v = sharedPreferences.getString(key, defaultLang) ?: defaultLang
                if (v != _language.value) _language.value = v
            }
            KEY_THEME -> {
                val v = sharedPreferences.getString(key, "system") ?: "system"
                if (v != _themePreference.value) _themePreference.value = v
            }
            KEY_ALARM_ENABLED -> {
                val v = sharedPreferences.getBoolean(key, false)
                if (v != _isAlarmEnabled.value) _isAlarmEnabled.value = v
            }
            KEY_SNOOZE_UNTIL -> {
                val v = sharedPreferences.getString(key, null)?.let {
                    try { java.time.LocalDateTime.parse(it) } catch (e: Exception) { null }
                }
                if (v != _snoozeUntil.value) _snoozeUntil.value = v
            }
            KEY_ONBOARDING_COMPLETED -> {
                val v = sharedPreferences.getBoolean(key, false)
                if (v != _onboardingCompleted.value) _onboardingCompleted.value = v
            }
            KEY_TOOLTIPS_COMPLETED -> {
                val v = sharedPreferences.getBoolean(key, false)
                if (v != _tooltipsCompleted.value) _tooltipsCompleted.value = v
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    /** Listener deregistrieren – wird von FamilyViewModel.onCleared() aufgerufen. */
    fun unregisterListener() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun setMyMemberId(id: String?) {
        _myMemberId.value = id
        prefs.edit { putString(KEY_MY_MEMBER_ID, id) }
    }

    fun setMyMemberName(name: String?) {
        _myMemberName.value = name
        prefs.edit { putString(KEY_MY_MEMBER_NAME, name) }
    }

    fun setAlarmSoundUri(uri: String) {
        _alarmSoundUri.value = uri
        prefs.edit { putString(KEY_ALARM_SOUND_URI, uri) }
    }

    fun setFamilyId(id: String?) {
        _familyId.value = id
        prefs.edit { putString(KEY_FAMILY_ID, id) }
    }

    fun setJoinCode(code: String?) {
        _joinCode.value = code
        prefs.edit { putString(KEY_JOIN_CODE, code) }
    }

    fun setFamilyName(name: String?) {
        _familyName.value = name
        prefs.edit { putString(KEY_FAMILY_NAME, name) }
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        prefs.edit { putString(KEY_LANGUAGE, lang) }
    }

    fun setThemePreference(theme: String) {
        _themePreference.value = theme
        prefs.edit { putString(KEY_THEME, theme) }
    }

    fun setAlarmEnabled(enabled: Boolean) {
        _isAlarmEnabled.value = enabled
        prefs.edit { putBoolean(KEY_ALARM_ENABLED, enabled) }
    }

    fun setSnoozeUntil(time: java.time.LocalDateTime?) {
        _snoozeUntil.value = time
        prefs.edit { putString(KEY_SNOOZE_UNTIL, time?.toString()) }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        _onboardingCompleted.value = completed
        prefs.edit { putBoolean(KEY_ONBOARDING_COMPLETED, completed) }
    }

    fun setTooltipsCompleted(completed: Boolean) {
        _tooltipsCompleted.value = completed
        prefs.edit { putBoolean(KEY_TOOLTIPS_COMPLETED, completed) }
    }

    fun clearAll() {
        _myMemberId.value = null
        _myMemberName.value = null
        _familyId.value = null
        _joinCode.value = null
        _familyName.value = null
        _isAlarmEnabled.value = false
        prefs.edit {
            remove(KEY_MY_MEMBER_ID)
            remove(KEY_MY_MEMBER_NAME)
            remove(KEY_FAMILY_ID)
            remove(KEY_JOIN_CODE)
            remove(KEY_FAMILY_NAME)
            remove(KEY_ALARM_ENABLED)
        }
        // Sprache und Sound-URI bleiben erhalten (bessere UX nach Logout/Login)
    }
}
