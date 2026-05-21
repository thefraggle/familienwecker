package de.familienwecker.famwake.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.runBlocking
import java.io.File

private val Context.dataStore by preferencesDataStore(name = "famwake_settings")

class SettingsFactory(private val context: Context) {
    fun createSettings(): ObservableSettings {
        val prefsFile = File(context.filesDir.parentFile, "shared_prefs/FamilienweckerPrefs_enc.xml")
        val dataStore = context.dataStore

        if (prefsFile.exists()) {
            try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                
                val sharedPrefs = EncryptedSharedPreferences.create(
                    context,
                    "FamilienweckerPrefs_enc",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )

                val allEntries = sharedPrefs.all
                if (allEntries.isNotEmpty()) {
                    runBlocking {
                        dataStore.edit { editPrefs ->
                            allEntries.forEach { (key, value) ->
                                when (value) {
                                    is Boolean -> editPrefs[booleanPreferencesKey(key)] = value
                                    is String -> editPrefs[stringPreferencesKey(key)] = value
                                    is Int -> editPrefs[intPreferencesKey(key)] = value
                                    is Long -> editPrefs[longPreferencesKey(key)] = value
                                    is Float -> editPrefs[floatPreferencesKey(key)] = value
                                    is Double -> editPrefs[doublePreferencesKey(key)] = value
                                }
                            }
                        }
                    }
                }
                
                sharedPrefs.edit().clear().commit()
                if (prefsFile.exists()) {
                    prefsFile.delete()
                }
                val backupFile = File(context.filesDir.parentFile, "shared_prefs/FamilienweckerPrefs_enc.bak")
                if (backupFile.exists()) {
                    backupFile.delete()
                }
            } catch (e: Exception) {
                // Bei Keystore- oder Decrypt-Fehlern: XML löschen, um Boot-Crash-Loops zu verhindern
                try {
                    if (prefsFile.exists()) {
                        prefsFile.delete()
                    }
                    val backupFile = File(context.filesDir.parentFile, "shared_prefs/FamilienweckerPrefs_enc.bak")
                    if (backupFile.exists()) {
                        backupFile.delete()
                    }
                } catch (inner: Exception) {
                    // Ignorieren
                }
            }
        }

        return DataStoreObservableSettings(dataStore)
    }
}
