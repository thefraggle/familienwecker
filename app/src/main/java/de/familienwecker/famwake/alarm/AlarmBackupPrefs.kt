package de.familienwecker.famwake.alarm

import android.content.Context

/**
 * Unverschlüsselte Sicherung des aktiven Alarms.
 *
 * EncryptedSharedPreferences sind vor dem ersten Geräte-Unlock nach einem Reboot
 * nicht lesbar (KeyStore-Key noch nicht verfügbar). Dieser Store verwendet
 * reguläre SharedPreferences und ist daher auch im "Before-First-Unlock"-Zustand
 * (LOCKED_BOOT_COMPLETED) aus dem BootReceiver heraus lesbar.
 *
 * Enthält keine sensiblen Daten (nur memberName, memberId, Weckzeit, Sound-URI).
 */
object AlarmBackupPrefs {

    private const val PREFS_FILE = "alarm_backup"
    private const val KEY_MEMBER_ID    = "alarm_member_id"
    private const val KEY_MEMBER_NAME  = "alarm_member_name"
    private const val KEY_SOUND_URI    = "alarm_sound_uri"
    private const val KEY_WAKE_MILLIS  = "alarm_wake_millis"
    private const val KEY_ENABLED      = "alarm_enabled"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    /** Wird von AlarmScheduler.scheduleWakeUp() aufgerufen. */
    fun save(
        context: Context,
        memberId: String,
        memberName: String,
        soundUri: String?,
        wakeUpMillis: Long
    ) {
        prefs(context).edit().apply {
            putString(KEY_MEMBER_ID,   memberId)
            putString(KEY_MEMBER_NAME, memberName)
            putString(KEY_SOUND_URI,   soundUri)
            putLong(KEY_WAKE_MILLIS,   wakeUpMillis)
            putBoolean(KEY_ENABLED,    true)
            apply()
        }
    }

    /** Wird von AlarmScheduler.cancelWakeUp() aufgerufen. */
    fun clear(context: Context) {
        prefs(context).edit().apply {
            putBoolean(KEY_ENABLED, false)
            apply()
        }
    }

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun getMemberId(context: Context): String? =
        prefs(context).getString(KEY_MEMBER_ID, null)

    fun getMemberName(context: Context): String? =
        prefs(context).getString(KEY_MEMBER_NAME, null)

    fun getSoundUri(context: Context): String? =
        prefs(context).getString(KEY_SOUND_URI, null)

    fun getWakeUpMillis(context: Context): Long =
        prefs(context).getLong(KEY_WAKE_MILLIS, 0L)
}
