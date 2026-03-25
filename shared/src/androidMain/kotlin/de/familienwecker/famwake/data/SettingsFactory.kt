package de.familienwecker.famwake.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings

class SettingsFactory(private val context: Context) {
    fun createSettings(): ObservableSettings {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        val sharedPrefs = EncryptedSharedPreferences.create(
            context,
            "FamilienweckerPrefs_enc", // Name von PreferencesRepository übernommen
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        return SharedPreferencesSettings(sharedPrefs)
    }
}
