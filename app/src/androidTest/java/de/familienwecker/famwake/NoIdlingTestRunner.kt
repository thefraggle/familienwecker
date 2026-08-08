package de.familienwecker.famwake

import android.os.Bundle
import androidx.test.espresso.IdlingRegistry
import androidx.test.runner.AndroidJUnitRunner

/**
 * Custom test runner that disables all Espresso IdlingResources before running tests.
 *
 * Why this is needed:
 * The default AndroidJUnitRunner lets Espresso's global IdlingRegistry block test execution.
 * Our app (Firebase/Firestore + Compose + Coroutines) registers IdlingResources that NEVER
 * become idle, causing any test that launches an Activity to hang forever.
 *
 * This runner clears the registry immediately at startup so tests run without waiting for idle.
 */
class NoIdlingTestRunner : AndroidJUnitRunner() {

    override fun onCreate(arguments: Bundle) {
        super.onCreate(arguments)
        // Unregister everything that may have been auto-registered
        IdlingRegistry.getInstance().resources.forEach {
            IdlingRegistry.getInstance().unregister(it)
        }
    }

    override fun onStart() {
        // Clear again just before tests start (some registration happens lazily)
        IdlingRegistry.getInstance().resources.toList().forEach {
            IdlingRegistry.getInstance().unregister(it)
        }
        super.onStart()
    }
}
