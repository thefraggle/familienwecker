package de.familienwecker.famwake.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SettingsListener
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap

class DataStoreObservableSettings(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : ObservableSettings {

    private val cache = ConcurrentHashMap<String, Any>()
    private val listeners = ConcurrentHashMap<String, MutableList<Pair<Class<*>, (Any?) -> Unit>>>()

    init {
        // Synchroner Initial-Load über runBlocking
        runBlocking {
            try {
                val prefs = dataStore.data.first()
                prefs.asMap().forEach { (key, value) ->
                    cache[key.name] = value
                }
            } catch (e: Exception) {
                // Falls Laden fehlschlägt
            }
        }

        // Asynchrones Beobachten von Hintergrund-Änderungen
        scope.launch {
            dataStore.data.collect { prefs ->
                val newKeys = prefs.asMap().mapKeys { it.key.name }
                
                // Aktualisiere Cache und benachrichtige Listener bei geänderten Werten
                val allKeys = cache.keys + newKeys.keys
                for (keyName in allKeys) {
                    val oldValue = cache[keyName]
                    val newValue = newKeys[keyName]
                    if (oldValue != newValue) {
                        if (newValue == null) {
                            cache.remove(keyName)
                        } else {
                            cache[keyName] = newValue
                        }
                        notifyListeners(keyName, newValue)
                    }
                }
            }
        }
    }

    private fun notifyListeners(key: String, value: Any?) {
        listeners[key]?.forEach { (type, callback) ->
            try {
                callback(value)
            } catch (e: Exception) {
                // Ignoriere Fehler bei Callbacks
            }
        }
    }

    override val keys: Set<String> get() = cache.keys
    override val size: Int get() = cache.size

    override fun clear() {
        cache.clear()
        runBlocking {
            dataStore.edit { it.clear() }
        }
        listeners.forEach { (key, list) ->
            list.forEach { (_, callback) -> callback(null) }
        }
    }

    override fun hasKey(key: String): Boolean = cache.containsKey(key)

    override fun remove(key: String) {
        cache.remove(key)
        runBlocking {
            dataStore.edit { prefs ->
                val prefKey = prefs.asMap().keys.find { it.name == key }
                if (prefKey != null) {
                    prefs.remove(prefKey)
                }
            }
        }
        notifyListeners(key, null)
    }

    // Generic put helper
    private fun <T : Any> putValue(keyName: String, value: T, prefKey: Preferences.Key<T>) {
        cache[keyName] = value
        runBlocking {
            dataStore.edit { prefs ->
                prefs[prefKey] = value
            }
        }
        notifyListeners(keyName, value)
    }

    // Custom helper to get value or default
    @Suppress("UNCHECKED_CAST")
    private fun <T> getValue(key: String, defaultValue: T): T {
        return (cache[key] as? T) ?: defaultValue
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getValueOrNull(key: String): T? {
        return cache[key] as? T
    }

    override fun putBoolean(key: String, value: Boolean) = putValue(key, value, booleanPreferencesKey(key))
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = getValue(key, defaultValue)
    override fun getBooleanOrNull(key: String): Boolean? = getValueOrNull(key)

    override fun putDouble(key: String, value: Double) = putValue(key, value, doublePreferencesKey(key))
    override fun getDouble(key: String, defaultValue: Double): Double = getValue(key, defaultValue)
    override fun getDoubleOrNull(key: String): Double? = getValueOrNull(key)

    override fun putFloat(key: String, value: Float) = putValue(key, value, floatPreferencesKey(key))
    override fun getFloat(key: String, defaultValue: Float): Float = getValue(key, defaultValue)
    override fun getFloatOrNull(key: String): Float? = getValueOrNull(key)

    override fun putInt(key: String, value: Int) = putValue(key, value, intPreferencesKey(key))
    override fun getInt(key: String, defaultValue: Int): Int = getValue(key, defaultValue)
    override fun getIntOrNull(key: String): Int? = getValueOrNull(key)

    override fun putLong(key: String, value: Long) = putValue(key, value, longPreferencesKey(key))
    override fun getLong(key: String, defaultValue: Long): Long = getValue(key, defaultValue)
    override fun getLongOrNull(key: String): Long? = getValueOrNull(key)

    override fun putString(key: String, value: String) = putValue(key, value, stringPreferencesKey(key))
    override fun getString(key: String, defaultValue: String): String = getValue(key, defaultValue)
    override fun getStringOrNull(key: String): String? = getValueOrNull(key)

    private fun addGenericListener(key: String, type: Class<*>, callback: (Any?) -> Unit): SettingsListener {
        val list = listeners.getOrPut(key) { mutableListOf() }
        val pair = type to callback
        list.add(pair)
        return object : SettingsListener {
            override fun deactivate() {
                listeners[key]?.remove(pair)
            }
        }
    }

    override fun addBooleanListener(key: String, defaultValue: Boolean, callback: (Boolean) -> Unit): SettingsListener {
        return addGenericListener(key, Boolean::class.java) { callback(it as? Boolean ?: defaultValue) }
    }

    override fun addBooleanOrNullListener(key: String, callback: (Boolean?) -> Unit): SettingsListener {
        return addGenericListener(key, Boolean::class.java) { callback(it as? Boolean) }
    }

    override fun addDoubleListener(key: String, defaultValue: Double, callback: (Double) -> Unit): SettingsListener {
        return addGenericListener(key, Double::class.java) { callback(it as? Double ?: defaultValue) }
    }

    override fun addDoubleOrNullListener(key: String, callback: (Double?) -> Unit): SettingsListener {
        return addGenericListener(key, Double::class.java) { callback(it as? Double) }
    }

    override fun addFloatListener(key: String, defaultValue: Float, callback: (Float) -> Unit): SettingsListener {
        return addGenericListener(key, Float::class.java) { callback(it as? Float ?: defaultValue) }
    }

    override fun addFloatOrNullListener(key: String, callback: (Float?) -> Unit): SettingsListener {
        return addGenericListener(key, Float::class.java) { callback(it as? Float) }
    }

    override fun addIntListener(key: String, defaultValue: Int, callback: (Int) -> Unit): SettingsListener {
        return addGenericListener(key, Int::class.java) { callback(it as? Int ?: defaultValue) }
    }

    override fun addIntOrNullListener(key: String, callback: (Int?) -> Unit): SettingsListener {
        return addGenericListener(key, Int::class.java) { callback(it as? Int) }
    }

    override fun addLongListener(key: String, defaultValue: Long, callback: (Long) -> Unit): SettingsListener {
        return addGenericListener(key, Long::class.java) { callback(it as? Long ?: defaultValue) }
    }

    override fun addLongOrNullListener(key: String, callback: (Long?) -> Unit): SettingsListener {
        return addGenericListener(key, Long::class.java) { callback(it as? Long) }
    }

    override fun addStringListener(key: String, defaultValue: String, callback: (String) -> Unit): SettingsListener {
        return addGenericListener(key, String::class.java) { callback(it as? String ?: defaultValue) }
    }

    override fun addStringOrNullListener(key: String, callback: (String?) -> Unit): SettingsListener {
        return addGenericListener(key, String::class.java) { callback(it as? String) }
    }
}
