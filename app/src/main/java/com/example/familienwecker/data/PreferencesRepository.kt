package com.example.familienwecker.data

import android.content.Context
import android.content.SharedPreferences
import android.media.RingtoneManager
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("FamilienweckerPrefs", Context.MODE_PRIVATE)

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

    private val _isAlarmEnabled = MutableStateFlow<Boolean>(prefs.getBoolean("ALARM_ENABLED", false))
    val isAlarmEnabled: StateFlow<Boolean> = _isAlarmEnabled.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            "MY_MEMBER_ID" -> _myMemberId.value = sharedPreferences.getString(key, null)
            "ALARM_SOUND_URI" -> _alarmSoundUri.value = sharedPreferences.getString(key, null)
            "FAMILY_ID" -> _familyId.value = sharedPreferences.getString(key, null)
            "JOIN_CODE" -> _joinCode.value = sharedPreferences.getString(key, null)
            "FAMILY_NAME" -> _familyName.value = sharedPreferences.getString(key, null)
            "APP_LANGUAGE" -> _language.value = sharedPreferences.getString(key, defaultLang) ?: defaultLang
            "ALARM_ENABLED" -> _isAlarmEnabled.value = sharedPreferences.getBoolean(key, false)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setMyMemberId(id: String?) {
        prefs.edit { putString("MY_MEMBER_ID", id) }
        _myMemberId.value = id
    }

    fun setAlarmSoundUri(uri: String) {
        prefs.edit { putString("ALARM_SOUND_URI", uri) }
        _alarmSoundUri.value = uri
    }

    fun setFamilyId(id: String?) {
        prefs.edit { putString("FAMILY_ID", id) }
        _familyId.value = id
    }

    fun setJoinCode(code: String?) {
        prefs.edit { putString("JOIN_CODE", code) }
        _joinCode.value = code
    }

    fun setFamilyName(name: String?) {
        prefs.edit { putString("FAMILY_NAME", name) }
        _familyName.value = name
    }

    fun setLanguage(lang: String) {
        prefs.edit { putString("APP_LANGUAGE", lang) }
        _language.value = lang
    }

    fun setAlarmEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("ALARM_ENABLED", enabled) }
        _isAlarmEnabled.value = enabled
    }

    fun clearAll() {
        prefs.edit { clear() }
        _myMemberId.value = null
        _familyId.value = null
        _joinCode.value = null
        _familyName.value = null
        _isAlarmEnabled.value = false
        // Note: Language and Sound URI are kept at default or last set 
        // as they are typically user-level app settings, not session-level.
    }
}
