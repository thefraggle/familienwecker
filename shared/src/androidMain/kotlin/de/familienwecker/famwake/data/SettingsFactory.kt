package de.familienwecker.famwake.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.russhwolf.settings.ObservableSettings

private val Context.dataStore by preferencesDataStore(name = "famwake_settings")

class SettingsFactory(private val context: Context) {
    fun createSettings(): ObservableSettings {
        return DataStoreObservableSettings(context.dataStore)
    }
}
