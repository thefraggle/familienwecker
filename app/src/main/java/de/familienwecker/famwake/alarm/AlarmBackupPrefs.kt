package de.familienwecker.famwake.alarm

import android.content.Context
import android.os.Build

data class MemberAlarmBackup(
    val memberId: String,
    val memberName: String,
    val soundUri: String?,
    val wakeUpMillis: Long,
    val snoozeUntilMillis: Long = 0L,
    val snoozeCount: Int = 0
)

/**
 * Unverschlüsselte Sicherung der aktiven Alarme in Device-Protected Storage.
 *
 * Normales context.getSharedPreferences() schreibt in Credential-Encrypted Storage
 * (nur nach Unlock lesbar). Device-Protected Storage hingegen ist sowohl nach Unlock
 * als auch im Direct-Boot-Modus (LOCKED_BOOT_COMPLETED, vor PIN-Eingabe) zugänglich.
 *
 * Multi-Member-fähig: Jedes Mitglied erhält eigene Einträge mit Member-ID-Präfix,
 * sodass beim Neustart alle geplanten Alarme zuverlässig wiederhergestellt werden.
 *
 * → Schreiben: App nach Unlock via createDeviceProtectedStorageContext()
 * → Lesen: BootReceiver via LOCKED_BOOT_COMPLETED (Context ist bereits DEP)
 */
object AlarmBackupPrefs {

    private const val PREFS_FILE = "alarm_backup"

    // Set aller aktiven Member-IDs
    private const val KEY_ACTIVE_MEMBERS = "alarm_active_members_set"

    // Legacy Single-Member Keys (für Rückwärtskompatibilität und Migration)
    private const val KEY_MEMBER_ID    = "alarm_member_id"
    private const val KEY_MEMBER_NAME  = "alarm_member_name"
    private const val KEY_SOUND_URI    = "alarm_sound_uri"
    private const val KEY_WAKE_MILLIS  = "alarm_wake_millis"
    private const val KEY_ENABLED      = "alarm_enabled"
    private const val KEY_SNOOZE_UNTIL = "alarm_snooze_until"
    private const val KEY_SNOOZE_COUNT = "alarm_snooze_count"

    // Member-spezifische Key-Generatoren
    private fun keyMemberName(id: String) = "alarm_name_$id"
    private fun keySoundUri(id: String) = "alarm_sound_$id"
    private fun keyWakeMillis(id: String) = "alarm_millis_$id"
    private fun keySnoozeUntil(id: String) = "alarm_snooze_until_$id"
    private fun keySnoozeCount(id: String) = "alarm_snooze_count_$id"

    private fun prefs(context: Context) =
        context.createDeviceProtectedStorageContext()
            .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    /** Wird von AlarmScheduler.scheduleWakeUp() aufgerufen. */
    fun save(
        context: Context,
        memberId: String,
        memberName: String,
        soundUri: String?,
        wakeUpMillis: Long
    ) {
        try {
            val p = prefs(context)
            val currentMembers = p.getStringSet(KEY_ACTIVE_MEMBERS, emptySet())?.toMutableSet() ?: mutableSetOf()
            currentMembers.add(memberId)

            p.edit().apply {
                putStringSet(KEY_ACTIVE_MEMBERS, currentMembers)
                putString(keyMemberName(memberId), memberName)
                putString(keySoundUri(memberId), soundUri)
                putLong(keyWakeMillis(memberId), wakeUpMillis)
                // Legacy-Keys synchron halten
                putString(KEY_MEMBER_ID, memberId)
                putString(KEY_MEMBER_NAME, memberName)
                putString(KEY_SOUND_URI, soundUri)
                putLong(KEY_WAKE_MILLIS, wakeUpMillis)
                putBoolean(KEY_ENABLED, true)
                apply()
            }
        } catch (e: Exception) {
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.e("AlarmBackupPrefs", "Fehler beim Speichern des Backups: ${e.message}")
            }
        }
    }

    /** Wird von AlarmScheduler.cancelWakeUp() aufgerufen. Löscht gezielt nur dieses Mitglied. */
    fun clear(context: Context, memberId: String) {
        try {
            val p = prefs(context)
            val currentMembers = p.getStringSet(KEY_ACTIVE_MEMBERS, emptySet())?.toMutableSet() ?: mutableSetOf()
            currentMembers.remove(memberId)

            val editor = p.edit().apply {
                remove(keyMemberName(memberId))
                remove(keySoundUri(memberId))
                remove(keyWakeMillis(memberId))
                remove(keySnoozeUntil(memberId))
                remove(keySnoozeCount(memberId))
                putStringSet(KEY_ACTIVE_MEMBERS, currentMembers)
            }

            // Falls dies der aktive Legacy-Member war:
            if (p.getString(KEY_MEMBER_ID, null) == memberId) {
                if (currentMembers.isNotEmpty()) {
                    val nextId = currentMembers.first()
                    editor.putString(KEY_MEMBER_ID, nextId)
                    editor.putString(KEY_MEMBER_NAME, p.getString(keyMemberName(nextId), ""))
                    editor.putString(KEY_SOUND_URI, p.getString(keySoundUri(nextId), null))
                    editor.putLong(KEY_WAKE_MILLIS, p.getLong(keyWakeMillis(nextId), 0L))
                } else {
                    editor.remove(KEY_MEMBER_ID)
                    editor.remove(KEY_MEMBER_NAME)
                    editor.remove(KEY_SOUND_URI)
                    editor.remove(KEY_WAKE_MILLIS)
                    editor.remove(KEY_SNOOZE_UNTIL)
                    editor.remove(KEY_SNOOZE_COUNT)
                    editor.putBoolean(KEY_ENABLED, false)
                }
            }
            editor.apply()
        } catch (e: Exception) {
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.e("AlarmBackupPrefs", "Fehler beim Löschen des Backups: ${e.message}")
            }
        }
    }

    /** Liefert alle gespeicherten Backups für alle aktiven Familienmitglieder. */
    fun getAllBackups(context: Context): List<MemberAlarmBackup> {
        val p = prefs(context)
        val activeIds = p.getStringSet(KEY_ACTIVE_MEMBERS, emptySet())?.toMutableSet() ?: mutableSetOf()

        // Legacy-Migration prüfen
        val legacyId = p.getString(KEY_MEMBER_ID, null)
        if (legacyId != null && !activeIds.contains(legacyId)) {
            val legacyMillis = p.getLong(KEY_WAKE_MILLIS, 0L)
            if (legacyMillis > 0L) {
                activeIds.add(legacyId)
            }
        }

        return activeIds.mapNotNull { id ->
            val millis = if (p.contains(keyWakeMillis(id))) {
                p.getLong(keyWakeMillis(id), 0L)
            } else if (id == legacyId) {
                p.getLong(KEY_WAKE_MILLIS, 0L)
            } else 0L

            if (millis > 0L) {
                val name = p.getString(keyMemberName(id), null)
                    ?: if (id == legacyId) p.getString(KEY_MEMBER_NAME, "") else ""
                val sound = p.getString(keySoundUri(id), null)
                    ?: if (id == legacyId) p.getString(KEY_SOUND_URI, null) else null
                val snoozeUntil = p.getLong(keySnoozeUntil(id), 0L).let {
                    if (it == 0L && id == legacyId) p.getLong(KEY_SNOOZE_UNTIL, 0L) else it
                }
                val snoozeCount = p.getInt(keySnoozeCount(id), 0).let {
                    if (it == 0 && id == legacyId) p.getInt(KEY_SNOOZE_COUNT, 0) else it
                }
                MemberAlarmBackup(
                    memberId = id,
                    memberName = name ?: "",
                    soundUri = sound,
                    wakeUpMillis = millis,
                    snoozeUntilMillis = snoozeUntil,
                    snoozeCount = snoozeCount
                )
            } else null
        }
    }

    fun isEnabled(context: Context): Boolean {
        val p = prefs(context)
        val members = p.getStringSet(KEY_ACTIVE_MEMBERS, emptySet())
        return !members.isNullOrEmpty() || p.getBoolean(KEY_ENABLED, false)
    }

    fun getMemberId(context: Context): String? =
        prefs(context).getString(KEY_MEMBER_ID, null)

    fun getMemberName(context: Context): String? =
        prefs(context).getString(KEY_MEMBER_NAME, null)

    fun getSoundUri(context: Context): String? =
        prefs(context).getString(KEY_SOUND_URI, null)

    fun getWakeUpMillis(context: Context): Long =
        prefs(context).getLong(KEY_WAKE_MILLIS, 0L)

    /** Snooze-State sichern (optional mit memberId). */
    fun saveSnooze(context: Context, snoozeUntilMillis: Long, snoozeCount: Int, memberId: String? = null) {
        try {
            prefs(context).edit().apply {
                putLong(KEY_SNOOZE_UNTIL, snoozeUntilMillis)
                putInt(KEY_SNOOZE_COUNT, snoozeCount)
                if (memberId != null) {
                    putLong(keySnoozeUntil(memberId), snoozeUntilMillis)
                    putInt(keySnoozeCount(memberId), snoozeCount)
                }
                apply()
            }
        } catch (e: Exception) {
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.e("AlarmBackupPrefs", "Fehler beim Speichern des Snooze-Backups: ${e.message}")
            }
        }
    }

    /** Snooze-State löschen. */
    fun clearSnooze(context: Context, memberId: String? = null) {
        try {
            prefs(context).edit().apply {
                if (memberId != null) {
                    remove(keySnoozeUntil(memberId))
                    remove(keySnoozeCount(memberId))
                }
                remove(KEY_SNOOZE_UNTIL)
                remove(KEY_SNOOZE_COUNT)
                apply()
            }
        } catch (e: Exception) {
            if (de.familienwecker.famwake.BuildConfig.DEBUG) {
                android.util.Log.e("AlarmBackupPrefs", "Fehler beim Löschen des Snooze-Backups: ${e.message}")
            }
        }
    }

    fun getSnoozeUntilMillis(context: Context, memberId: String? = null): Long {
        val p = prefs(context)
        if (memberId != null && p.contains(keySnoozeUntil(memberId))) {
            return p.getLong(keySnoozeUntil(memberId), 0L)
        }
        return p.getLong(KEY_SNOOZE_UNTIL, 0L)
    }

    fun getSnoozeCount(context: Context, memberId: String? = null): Int {
        val p = prefs(context)
        if (memberId != null && p.contains(keySnoozeCount(memberId))) {
            return p.getInt(keySnoozeCount(memberId), 0)
        }
        return p.getInt(KEY_SNOOZE_COUNT, 0)
    }
}
