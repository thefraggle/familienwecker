package de.familienwecker.famwake

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

@RunWith(AndroidJUnit4::class)
class ScreenshotsTest {

    companion object {
        init {
            // Set flag immediately on class load, before any test rules launch the activity
            FamWakeApplication.isScreenshotMode = true
        }
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val localeTestRule = LocaleTestRule()

    @Test
    fun captureScreenshots() {
        val appSettings = (composeTestRule.activity.application as FamWakeApplication).appSettings

        // Wait for main dashboard to fully load and settle
        Thread.sleep(3000)

        // --- PART 1: LIGHT MODE ---
        appSettings.setTheme("light")
        Thread.sleep(1500)
        captureAllScreensForMode("Light")

        // --- PART 2: DARK MODE ---
        appSettings.setTheme("dark")
        Thread.sleep(1500)
        captureAllScreensForMode("Dark")
    }

    private fun captureAllScreensForMode(appearance: String) {
        // --- SCREENSHOT 1: Dashboard (Wecker AUS - Mond) ---
        // Tap on the main alarm toggle to switch it off
        composeTestRule.onNodeWithTag("main_alarm_toggle").performClick()
        Thread.sleep(2000)
        Screengrab.screenshot("01_MainDashboard_Empty_${appearance}")

        // Switch it back on for the next screenshots
        composeTestRule.onNodeWithTag("main_alarm_toggle").performClick()
        Thread.sleep(2000)

        // --- SCREENSHOT 2: Dashboard (Main Weckplan aktiv) ---
        Screengrab.screenshot("02_MainDashboard_Active_${appearance}")

        // --- SCREENSHOT 3: Settings eines Members (Weckzeit) ---
        // Tap on the dad's member list card (test tag "member_list_card_mock_dad")
        composeTestRule.onNodeWithTag("member_list_card_mock_dad").performClick()
        Thread.sleep(2000)
        Screengrab.screenshot("03_MemberSettings_${appearance}")

        // Go back to Main
        androidx.test.espresso.Espresso.pressBack()
        Thread.sleep(2000)

        // --- SCREENSHOT 4: Settings Screen (with Share Code) ---
        // Tap on gear icon to open settings (test tag "settings_button")
        composeTestRule.onNodeWithTag("settings_button").performClick()
        Thread.sleep(2000)
        Screengrab.screenshot("04_ShareFamily_${appearance}")

        // Go back to Main to reset state for the next mode
        androidx.test.espresso.Espresso.pressBack()
        Thread.sleep(2000)
    }
}
