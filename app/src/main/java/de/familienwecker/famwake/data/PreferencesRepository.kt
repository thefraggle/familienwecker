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

class PreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = createEncryptedPrefs(context).also {
        // H-5: Einmalige Migration von alten unverschlüsselten Prefs
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
                // Fallback auf unverschlüsselte Prefs wenn EncryptedSharedPreferences nicht verfügbar
                android.util.Log.e("PreferencesRepository", "EncryptedSharedPreferences nicht verfügbar, Fallback: ${e.message}")
                context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            }
        }

        private fun migrateIfNeeded(context: Context, encrypted: SharedPreferences) {
            val legacy = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            // Prüfen ob Migration noch nicht durchgeführt oder ob encrypted auf legacy zeigt (Fallback-Fall)
            if (legacy === encrypted) return
            if (encrypted.contains(MIGRATION_DONE_KEY)) return
            if (legacy.all.isEmpty()) {
                encrypted.edit { putBoolean(MIGRATION_DONE_KEY, true) }
                return
            }
            // Werte übertragen
            encrypted.edit {
                legacy.getString("MY_MEMBER_ID", null)?.let { putString("MY_MEMBER_ID", it) }
                legacy.getString("ALARM_SOUND_URI", null)?.let { putString("ALARM_SOUND_URI", it) }
                legacy.getString("FAMILY_ID", null)?.let { putString("FAMILY_ID", it) }
                legacy.getString("JOIN_CODE", null)?.let { putString("JOIN_CODE", it) }
                legacy.getString("FAMILY_NAME", null)?.let { putString("FAMILY_NAME", it) }
                legacy.getString("APP_LANGUAGE", null)?.let { putString("APP_LANGUAGE", it) }
                legacy.getString("APP_THEME", null)?.let { putString("APP_THEME", it) }
                putBoolean("ALARM_ENABLED", legacy.getBoolean("ALARM_ENABLED", false))
                putInt("LAST_SEEN_WHATS_NEW_VERSION", legacy.getInt("LAST_SEEN_WHATS_NEW_VERSION", 0))
                putBoolean(MIGRATION_DONE_KEY, true)
            }
            // Alte Prefs löschen
            legacy.edit { clear() }
            android.util.Log.i("PreferencesRepository", "Migration zu EncryptedSharedPreferences abgeschlossen.")
        }
    }

    private val _myMemberId = MutableStateFlow<String?>(prefs.getString("MY_MEMBER_ID", null))
    val myMemberId: StateFlow<String?> = _myMemberId.asStateFlow()

    private val _alarmSoundUri = MutableStateFlow<String?>(
        try {
            prefs.getString("ALARM_SOUND_URI", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString())
        } catch (e: Exception) {
            prefs.getString("ALARM_SOUND_URI", null)
        }
    )
    val alarmSoundUri: StateFlow<String?> = _alarmSoundUri.asStateFlow()

    private val _familyId = MutableStateFlow<String?>(prefs.getString("FAMILY_ID", null))
    val familyId: StateFlow<String?> = _familyId.asStateFlow()

    private val _joinCode = MutableStateFlow<String?>(prefs.getString("JOIN_CODE", null))
    val joinCode: StateFlow<String?> = _joinCode.asStateFlow()

    private val _familyName = MutableStateFlow<String?>(prefs.getString("FAMILY_NAME", null))
    val familyName: StateFlow<String?> = _familyName.asStateFlow()

    private val defaultLang = if (java.util.Locale.getDefault().language == "de") "de" else "en"
    private val _language = MutableStateFlow<String>(prefs.getString("APP_LANGUAGE", defaultLang) ?: defaultLang)
    val language: StateFlow<String> = _language.asStateFlow()

    private val _themePreference = MutableStateFlow<String>(prefs.getString("APP_THEME", "system") ?: "system")
    val themePreference: StateFlow<String> = _themePreference.asStateFlow()

    private val _isAlarmEnabled = MutableStateFlow<Boolean>(prefs.getBoolean("ALARM_ENABLED", false))
    val isAlarmEnabled: StateFlow<Boolean> = _isAlarmEnabled.asStateFlow()

    private val _lastSeenWhatsNewVersion = MutableStateFlow<Int>(prefs.getInt("LAST_SEEN_WHATS_NEW_VERSION", 0))
    val lastSeenWhatsNewVersion: StateFlow<Int> = _lastSeenWhatsNewVersion.asStateFlow()

    // B9: Der SharedPreferences-Listener reagiert nur auf externe Schreiber (z.B. andere Prozesse).
    // Alle eigenen Setters setzen den StateFlow direkt – kein Doppel-Emit.
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        // Guard: nur updaten wenn der Wert sich tatsächlich von dem aktuellen StateFlow-Wert unterscheidet
        when (key) {
            "MY_MEMBER_ID" -> {
                val v = sharedPreferences.getString(key, null)
                if (v != _myMemberId.value) _myMemberId.value = v
            }
            "ALARM_SOUND_URI" -> {
                val v = sharedPreferences.getString(key, null)
                if (v != _alarmSoundUri.value) _alarmSoundUri.value = v
            }
            "FAMILY_ID" -> {
                val v = sharedPreferences.getString(key, null)
                if (v != _familyId.value) _familyId.value = v
            }
            "JOIN_CODE" -> {
                val v = sharedPreferences.getString(key, null)
                if (v != _joinCode.value) _joinCode.value = v
            }
            "FAMILY_NAME" -> {
                val v = sharedPreferences.getString(key, null)
                if (v != _familyName.value) _familyName.value = v
            }
            "APP_LANGUAGE" -> {
                val v = sharedPreferences.getString(key, defaultLang) ?: defaultLang
                if (v != _language.value) _language.value = v
            }
            "APP_THEME" -> {
                val v = sharedPreferences.getString(key, "system") ?: "system"
                if (v != _themePreference.value) _themePreference.value = v
            }
            "ALARM_ENABLED" -> {
                val v = sharedPreferences.getBoolean(key, false)
                if (v != _isAlarmEnabled.value) _isAlarmEnabled.value = v
            }
            "LAST_SEEN_WHATS_NEW_VERSION" -> {
                val v = sharedPreferences.getInt(key, 0)
                if (v != _lastSeenWhatsNewVersion.value) _lastSeenWhatsNewVersion.value = v
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setMyMemberId(id: String?) {
        _myMemberId.value = id
        prefs.edit { putString("MY_MEMBER_ID", id) }
    }

    fun setAlarmSoundUri(uri: String) {
        _alarmSoundUri.value = uri
        prefs.edit { putString("ALARM_SOUND_URI", uri) }
    }

    fun setFamilyId(id: String?) {
        _familyId.value = id
        prefs.edit { putString("FAMILY_ID", id) }
    }

    fun setJoinCode(code: String?) {
        _joinCode.value = code
        prefs.edit { putString("JOIN_CODE", code) }
    }

    fun setFamilyName(name: String?) {
        _familyName.value = name
        prefs.edit { putString("FAMILY_NAME", name) }
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        prefs.edit { putString("APP_LANGUAGE", lang) }
    }

    fun setThemePreference(theme: String) {
        _themePreference.value = theme
        prefs.edit { putString("APP_THEME", theme) }
    }

    fun setAlarmEnabled(enabled: Boolean) {
        _isAlarmEnabled.value = enabled
        prefs.edit { putBoolean("ALARM_ENABLED", enabled) }
    }

    fun setLastSeenWhatsNewVersion(version: Int) {
        _lastSeenWhatsNewVersion.value = version
        prefs.edit { putInt("LAST_SEEN_WHATS_NEW_VERSION", version) }
    }

    fun clearAll() {
        _myMemberId.value = null
        _familyId.value = null
        _joinCode.value = null
        _familyName.value = null
        _isAlarmEnabled.value = false
        prefs.edit {
            remove("MY_MEMBER_ID")
            remove("FAMILY_ID")
            remove("JOIN_CODE")
            remove("FAMILY_NAME")
            remove("ALARM_ENABLED")
        }
        // Hinweis: Sprache und Sound-URI bleiben erhalten (User-Experience nach Logout/Login)
    }
}
