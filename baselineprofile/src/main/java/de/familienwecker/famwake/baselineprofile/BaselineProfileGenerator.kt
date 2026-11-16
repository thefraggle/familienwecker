package de.familienwecker.famwake.baselineprofile

import android.content.Intent
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generiert das App-eigene Baseline Profile für kritische Startup- und Wecker-Pfade.
 *
 * Ausführung (Gradle Managed Device):
 *   ./gradlew :app:generateReleaseBaselineProfile
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    private val targetPackage: String
        get() = InstrumentationRegistry.getArguments().getString("targetAppId")
            ?: "de.familienwecker.famwake"

    @Test
    fun generateStartupAndMainProfile() {
        rule.collect(
            packageName = targetPackage,
            includeInStartupProfile = true
        ) {
            pressHome()
            startActivityAndWait()
            device.waitForIdle(3_000L)
        }
    }

    @Test
    fun generateRingingActivityProfile() {
        rule.collect(
            packageName = targetPackage,
            includeInStartupProfile = false
        ) {
            pressHome()
            val intent = Intent().apply {
                setClassName(targetPackage, "de.familienwecker.famwake.ui.screens.RingingActivity")
                putExtra("MEMBER_ID", "baseline_profile_member")
                putExtra("MEMBER_NAME", "Baseline")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivityAndWait(intent)
            device.waitForIdle(2_000L)
        }
    }
}
