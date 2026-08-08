package de.familienwecker.famwake

import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.locale.LocaleTestRule
import java.io.File
import java.io.FileOutputStream

/**
 * Screenshot test that avoids ALL Espresso/Compose idling resource registration.
 *
 * Root cause of previous hangs: createAndroidComposeRule registers Espresso IdlingResources.
 * Espresso waits for the app to be "idle" before running any test code.
 * The app is NEVER idle because Firestore/Coroutines run permanently in the background.
 *
 * Fix: Use ActivityScenario.launch() which does NOT register IdlingResources,
 * then capture via UiAutomation.takeScreenshot() which also needs no idle state.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotsTest {

    companion object {
        init {
            // Set flag before any test rules launch the activity
            FamWakeApplication.isScreenshotMode = true
        }
    }

    @get:Rule
    val localeTestRule = LocaleTestRule()

    @Test
    fun captureScreenshots() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        // Launch activity WITHOUT Espresso idling registration
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Give the UI time to fully render (no idle sync needed)
        Thread.sleep(4000)

        var appSettings: de.familienwecker.famwake.data.AppSettings? = null
        scenario.onActivity { activity ->
            appSettings = (activity.application as FamWakeApplication).appSettings
            appSettings!!.setTheme("light")
            appSettings!!.setAlarmEnabled(false)
        }

        // Wait for UI to react to state changes
        Thread.sleep(2000)

        // Take screenshot via UiAutomation (no Espresso idling involved)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()

        if (bitmap != null) {
            // Write to the path Screengrab/Fastlane expects:
            // /sdcard/Android/data/<package>/files/screengrab/<locale>/images/screenshots/
            val targetDir = File(
                context.getExternalFilesDir(null),
                "screengrab/de-DE/images/screenshots"
            )
            targetDir.mkdirs()
            val targetFile = File(targetDir, "01_MainDashboard_Empty_Light.png")
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            android.util.Log.i("ScreenshotsTest", "✅ Screenshot saved: ${targetFile.absolutePath}")
        } else {
            android.util.Log.e("ScreenshotsTest", "❌ UiAutomation.takeScreenshot() returned null")
        }

        scenario.close()
    }
}
