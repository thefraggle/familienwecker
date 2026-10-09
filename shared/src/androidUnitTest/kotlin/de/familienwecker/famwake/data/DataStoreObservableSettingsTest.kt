package de.familienwecker.famwake.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreObservableSettingsTest {

    private class FakeDataStore(
        initialPrefs: Preferences = preferencesOf()
    ) : DataStore<Preferences> {
        private val state = MutableStateFlow(initialPrefs)
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private fun createSettings(fakeStore: FakeDataStore): DataStoreObservableSettings {
        return DataStoreObservableSettings(fakeStore, CoroutineScope(Dispatchers.Unconfined))
    }

    @Test
    fun initialLoad_populatesCacheSynchronously() {
        val initialKey = stringPreferencesKey("initial_key")
        val initial = preferencesOf(initialKey to "initial_value")
        val fakeStore = FakeDataStore(initial)

        val settings = createSettings(fakeStore)

        assertTrue(settings.hasKey("initial_key"))
        assertEquals("initial_value", settings.getString("initial_key", "default"))
    }

    @Test
    fun putAndGet_allTypesPreservedInCache() {
        val fakeStore = FakeDataStore()
        val settings = createSettings(fakeStore)

        settings.putString("str", "hello")
        assertEquals("hello", settings.getString("str", ""))
        assertEquals("hello", settings.getStringOrNull("str"))

        settings.putInt("num", 42)
        assertEquals(42, settings.getInt("num", 0))
        assertEquals(42, settings.getIntOrNull("num"))

        settings.putBoolean("flag", true)
        assertTrue(settings.getBoolean("flag", false))
        assertEquals(true, settings.getBooleanOrNull("flag"))

        settings.putLong("big_num", 1000L)
        assertEquals(1000L, settings.getLong("big_num", 0L))

        settings.putFloat("float_num", 3.14f)
        assertEquals(3.14f, settings.getFloat("float_num", 0f))

        settings.putDouble("double_num", 2.71828)
        assertEquals(2.71828, settings.getDouble("double_num", 0.0), 0.00001)
    }

    @Test
    fun remove_clearsKeyFromCache() {
        val fakeStore = FakeDataStore()
        val settings = createSettings(fakeStore)

        settings.putString("temp", "value")
        assertTrue(settings.hasKey("temp"))

        settings.remove("temp")
        assertFalse("Key should not be in cache", settings.hasKey("temp"))
        assertNull(settings.getStringOrNull("temp"))
    }

    @Test
    fun clear_removesAllKeysFromCache() {
        val fakeStore = FakeDataStore()
        val settings = createSettings(fakeStore)

        settings.putString("k1", "v1")
        settings.putInt("k2", 2)
        assertTrue(settings.size >= 2)

        settings.clear()
        assertEquals(0, settings.size)
        assertFalse(settings.hasKey("k1"))
        assertFalse(settings.hasKey("k2"))
    }

    @Test
    fun listeners_notifiedOnValueChange() {
        val fakeStore = FakeDataStore()
        val settings = createSettings(fakeStore)

        var notifiedValue: String? = null
        val listener = settings.addStringListener("observe_key", "default") {
            notifiedValue = it
        }

        settings.putString("observe_key", "updated_value")
        assertEquals("updated_value", notifiedValue)

        // Nach Deaktivierung darf keine Benachrichtigung mehr erfolgen
        listener.deactivate()
        settings.putString("observe_key", "another_value")
        assertEquals("updated_value", notifiedValue)
    }
}
