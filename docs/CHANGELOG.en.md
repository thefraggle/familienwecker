# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

*[🇩🇪 Deutsche Version](CHANGELOG.md)*

## [0.4.8] - TBD

### Added
- 

### Changed
- 

### Fixed
- 

## [0.4.7] - 2026-03-04

### Added
- **Visual Feedback:** Bounce effect when clicking buttons and interactive elements for a more responsive feel.
- **Iconography Enhancements:** New icons for external links (Privacy, Imprint, Delete Account) and email support for better visual guidance.
- **Join Code Security:** Implemented a uniqueness check for family join codes in `FirebaseRepository`.

### Changed
- **Settings Structure:** Logical reordering of sections (Profile, Alarm Sound, Family, Language, Help, Support).
- **Localization:** Localized the copyright footer in settings for German users ("Alle Rechte vorbehalten.").
- **Icon Refinement:** Adjusted section icons (Groups, Description) in Settings for clearer symbolism.

## [0.4.6] - 2026-03-04

### UI & UX Overhaul (Dark Mode 2.0)
- **Extreme Dark Mode:** Switched the dark theme to near-black backgrounds and deep-dark cards for an ultra-modern look and better battery life (OLED).
- **Contrast Optimization:** All text and icon colors refined for the new dark design to ensure maximum readability at night.
- **Dynamic App Branding:** The TopAppBar now displays a branded logo app-wide: **FamWake** (ExtraBold) + localized suffix (normal), e.g., "Family Alarm".
- **Light Theme Polishing:** Transparent (seamless) headers in the light design. Fixed "muddy" shadow artifacts on tiles by avoiding alpha transparency in Light Mode.
- **Font Strategy:** Switched to a more precise integration of the "Nunito" font with 7 individual weights, now correctly utilized throughout the project (Compose Typography).
- **Layout Refinement:** Reduced internal card padding from 16.dp to 12.dp for a more compact and professional appearance.

### Fixed
- **Compilation Errors:** Resolved syntax errors and missing imports in member management and login screens.
- **Shadow Artifacts:** Fixed a rendering issue with error cards and paused members in the light design.

## [0.4.5] - 2026-03-04

### UI & UX Modernization
- **Glassmorphism:** Introduction of semi-transparent cards with a subtle border (1.dp) and reduced elevation (0.dp) to avoid Android shadow artifacts.
- **Background Gradients:** Soft, vertical color gradient depending on the theme.
- **Rounded Corners:** CornerRadius increased to softer 24.dp - 32.dp.
- **New Typography:** Integration of the "Nunito" Google Font for a softer, more geometric appearance. Stronger font weights (ExtraBold) for headers and large titles.
- **Dark Mode Support:** Theme selection ("Light", "Dark", "System") added to settings, including automatic system adoption. Color scheme adjustments (e.g. to Night Blue and soft Mint).
- **Animations:** "Bouncy" spring animations on interactive objects (buttons) as well as smooth transitions for family member lists.
- **Clean-Up:** Removed the "Sync" icon in the TopBar, as synchronization happens in the background and at startup.

## [0.4.4] - 2026-03-03

### Added
- **Automated Unit Tests:** The core algorithm (`Scheduler`) is now automatically tested for correctness and conflict resolution using unit tests.


## [0.4.3] - 2026-03-03

### Cleaned & Improved
- **Icon Cleanup & Optimization:** 
    - Removed unused assets (`ic_splash_logo.png`, `splash_screen_full.jpg`) and the unused `SplashScreen.kt` to reduce app size.
    - Unified adaptive icon references (`mipmap-anydpi`) to use density-specific mipmaps instead of a single image in `drawable/`, preventing scaling issues on high-density displays.
    - Updated the app icon in `drawable/` to the current design.
    - Ensured support for Monochrome (Themed) Icons starting with Android 13.


## [0.4.2] - 2026-03-03

### Added
- **Password Change Confirmation Email:** After successfully saving a new password on the reset page, a branded confirmation email (DE/EN) is now automatically sent.

### Fixed & Improved
- **Email Verification (Login Bypass):** Fixed a bug where users could bypass the verification screen after an app restart or explicit login. The app now strictly checks the `isEmailVerified` status in `AuthViewModel`.
- **Resend Reliability & Fallback:** Cloud Function errors during email dispatch are now caught client-side, triggering an automatic fallback to the native Firebase system (`sendEmailVerification`) to ensure the email is always delivered.
- **Routing Fix:** Verification links in emails now reliably point to the correct confirmation page (`verify-email.html`) instead of the password reset page by dynamically rewriting the global action URL in the Cloud Function.


## [0.4.1] - 2026-03-03

### Added
- **Branded Password Reset Pages:** Custom HTML pages for password reset were created in the `/auth` directory of this project, utilizing the app branding (Outfit font, colors).
- **Confirmation Email:** After a successful password reset, a branded confirmation email (DE/EN) is now sent via Cloud Function and Resend.

### Changed & Improved
- **Error Mapping for Password Reset:** Improved error mapping in the Cloud Function to ensure "Account not found" (`auth/user-not-found`) is reliably reported as `USER_NOT_FOUND` to the Android client.
- **Documentation:** SETUP guides (DE/EN) and test plan updated to v0.4.1, including branding information.
- **Version Bump:** App version increased to 0.4.1.

## [0.4.0] - 2026-03-02


### Added
- **Branded Password Reset Email:** Password resets are now sent via a Firebase Cloud Function (Node.js 22, Gen2, `europe-west3`) and the **Resend** email provider. Sender is `no-reply@familienwecker.de` with a custom HTML template (button, security notice, footer).
- **Bilingual Emails:** The email template (subject, content, sender name, footer) automatically adapts to the device language (German / English).
- **Password Reset Error Messages:** Invalid email, unknown account, and rate limit are now shown with clear messages (DE/EN).

### Changed & Improved
- **Code Hardening:** Safe `LocalTime` parsing fallbacks in `FirebaseRepository`. `addOrUpdateMember()` with try-catch and logging. `email.trim()` on login, register, and reset.
- **Cleanup:** Removed duplicate import in `MainActivity`.

## [0.3.11] - 2026-03-02

### Changed & Improved
- **Data Resilience:** Users are now offered an option to leave a family when data is missing or deleted, allowing a smooth return to the setup screen.
- **Claim Synchronization:** Claimed profiles are now synchronized more reliably across devices without requiring a manual refresh.

## [0.3.10] - 2026-03-02

### Added
- **Forgot Password:** Integrated password reset functionality into the login screen.

### Changed & Improved
- **Sync & Stability:** The "Alarm On/Off" master switch is now synced via Firestore. The app performs an automatic "Force Refresh" on startup to prevent data inconsistencies (especially after re-installs).
- **Error Transparency:** System errors (e.g., "Permission Denied" or network issues) are now displayed directly on the dashboard.
- **Database Hardening:** Firestore security rules optimized for family creators to resolve access issues during profile synchronization.
- **Robust Logic:** Improved determination of the most recent state (`lastUpdatedAt`) for concurrent changes and "ultra-robust" member mapping (safety default values).

### Fixed
- **Critical Fixes:** Resolved a `NullPointerException` on app startup and fixed a potential data loss issue during network instability.
- **UI Fixes:** Corrected the pause button logic on unclaimed cards for schedule calculation.
- **Version Bump:** App version increased to 0.3.10.

## [0.3.9] - 2026-03-02

### Added
- **Automatic Status Reset:** "Already Awake" and "Paused" statuses are now automatically reset once the calculated wake-up time for the member is reached or exceeded. This ensures the alarm is active as usual the next day.

### Changed & Improved
- **Scheduler Integration:** The reset check is integrated directly into the schedule calculation to ensure immediate UI and alarm updates for the following day.
- **Safety Reset:** Introduced an additional safety check on app startup that resets paused profiles if the app was inactive beyond the wake-up period.

### Fixed
- Fixed an issue where the "Paused" status could incorrectly remain active for several days.

## [0.3.8] - 2026-03-01

### Added
- **Android 14 Full-Screen Protection:** Automatic check and request for "Full-Screen Intent permission" on Android 14+ during app startup.

### Changed & Improved
- **Alarm Sound & System Tones:** Stabilized the selection of system alarm sounds. Introduced dynamic notification channels with sound fallback. The alarm now reliably plays the selected system tone even if the full-screen activity is delayed.
- **Optimized Ringing Logic:** `RingingActivity` now cancels the notification upon launch to prevent audio overlaps.
- **Modernized Screen Flags:** Improved flags for accessing the locked screen on current Android versions.

### Fixed
- **Sound Reset on Logout:** Fixed `PreferencesRepository` to preserve selected ringtone and language settings after logout and re-login.
- **Compilation Errors:** Added missing imports in `AlarmReceiver` and `RingingActivity`.

## [0.3.7] - 2026-02-27

### Added
- **Google Login Icon:** The login screen now displays the official Google icon on the sign-in button.
- **Member Deletion Confirmation:** Deleting a family member now requires explicit confirmation to prevent accidental removal.
- **Refined Deletion Safety:** Deleting a family now requires double confirmation if other members (claimed or unclaimed) are still present. A single confirmation suffices for empty families or if you are the only member.

### Changed & Improved
- **Centralizing "I'm Awake":** The "Already Awake" function has been moved from individual member cards to a prominent global button in the Master Switch area.
- **Compact Master Switch:** Shortened and clarified the labels on the main switch ("Alarm: ON" / "Alarm: OFF").
- **Permission Management:** Restricted editing and deletion of profiles claimed by other family members. Edit buttons are automatically hidden for these profiles.
- **Delete Own Profile:** You can now delete your own profile directly from the main dashboard (unassigns the profile and deactivates the alarm).
- **Pause Logic:** The pause button on member tiles (reset at midnight) is now only available for unclaimed members to avoid redundancy with the Master Switch.

### Fixed
- **Resolved Infinity Loading:** Fixed a bug where the app could hang in an endless loading loop after deleting a family and immediately creating a new one.
- **Login Persistence:** Deleting a family no longer triggers a full logout; the user remains logged in and is redirected to the setup screen.
- **Logic Audit:** Conducted a comprehensive consistency check of app logic regarding claimed, unclaimed, and personal profiles, closing security gaps in multi-user data editing.

## [0.3.6] - 2026-02-27

### Added
- **"Already Awake" Button:** A new sun icon (☀️) on member cards allows users to suppress their alarm for today without disrupting the bathroom schedule of others.
- **"Pause for Today":** A new pause icon (⏸️/▶️) removes a member entirely from today's schedule, potentially allowing others to sleep in longer.
- **Snooze Functionality:** A "Snooze (5 Min)" button in the ringing screen for a short alarm delay.
- **Member Limit:** Family size is now capped at 6 members to ensure stability and clarity.
- **Automatic Reset:** "Already Awake" and "Paused" statuses are automatically cleared at midnight (or upon the first app launch of the day).

### Fixed & Improved
- **Data Persistence:** Fixed an issue where "Already Awake" and "Paused" states were not saved correctly in the cloud.
- **Logout & Deletion:** Robust local state clearing (SharedPreferences) and explicit sign-out during logout or family deletion. 
- **Auto-Backup disabled:** To prevent the restoration of old session data after re-installation, Android's Auto-Backup has been disabled for the app.
- **UI Layout:** Optimized member card header to better handle long names and multiple status icons without text wrapping.
- **Battery Optimization:** Fixed a bug where clicking the battery warning card would not open the system settings (requires new permission).
- **Compilation:** Fixed a nullability mismatch in the repository.

### Changed
- **Scheduler Robustness:** Removed internal `LocalTime.MAX` sentinel values and introduced a 04:00 AM lower bound for backward planning to prevent wrap-around bugs with extremely early departure times.
- **Version Bump:** App version updated to 0.3.6.

## [0.3.5] - 2026-02-27

### Added
- **Precise Scheduler Diagnostics:** If no schedule can be found, the app now identifies the specific member and constraint causing the conflict (e.g., "Conflict at Mom: wake up would need to be 06:15, but earliest wake up is 06:30").
- **Battery Optimization Warning:** New warning card on the main screen appears if system battery optimizations might delay the alarm. Clicking the card opens the relevant system settings.
- **Test Plan:** Initial comprehensive test plan in `docs/test_plan.md` covering functional tests, edge cases (EC), and UI/UX scenarios.

### Changed
- **Robust Scheduler:** The algorithm now uses Kotlin `Result` types for better error propagation and more stable calculation chains.
- **Roadmap Synchronization:** Updated roadmaps (DE/EN) with all gaps identified in the test plan (e.g., Snooze, DST protection, Watchdog) for future versions.
- **Version Bump:** App version updated to 0.3.5.

## [0.3.4] - 2026-02-27

### Added
- **Bathroom duration validation:** Input must be between 1 and 120 minutes. The Save button is disabled for invalid values and an error card is shown.

### Fixed
- **Member creation failing after family setup:** `saveUserFamily()` was called asynchronously _after_ navigation to the main screen. Firestore security rules check `isFamilyMember()` via the user document — which didn't exist yet when the first member write occurred → silent Permission Denied. Fix: user document is now awaited before navigation; SharedPrefs are set afterwards. Side effect: also eliminates the brief red error flash during family creation.
- **isPaused / claim status reset after editing a member:** `AddMemberScreen` constructed a new `FamilyMember` without `isPaused`, `claimedByUserId`, `claimedByUserName`, or `createdAt` — all fields were reset to defaults on every save. Fix: non-editable fields are now copied from the existing member.
- **Stable member list order:** Firestore returns documents in non-deterministic order (UUID-based document IDs). New `createdAt` field (epoch millis) is written on creation and preserved on update. The list is sorted client-side by `createdAt` ascending.
- **Phantom alarm after logout / leave family / delete family:** `logout()` did not cancel a pending system alarm. New `cancelAlarmForCurrentUser()` helper is now called in `logout()`, `leaveFamily()`, `deleteFamily()`, and `recalculateSchedule()` (when the member list becomes empty).

## [0.3.3] - 2026-02-27

### Added
- **Leave-home-time validation:** When adding or editing a member, the app now validates that the "leave home" time is after the latest wake time. An error card is shown and the Save button is disabled if invalid.
- **Edit icon on member cards:** A small pencil icon now signals that member entries can be tapped to edit.

### Changed
- **Ringtone fix (complete rewrite):** The alarm sound selected in Settings is now played correctly.
  - Notification channel bumped to `ALARM_CHANNEL_V2` — bypasses Android's cached sound settings from the old channel.
  - `RingingActivity.playRingtone()` rewritten: uses `setDataSource + prepare()` with `USAGE_ALARM` audio attributes instead of `MediaPlayer.create()`. Three-stage fallback chain (saved URI → system alarm → ringtone). The alarm now runs on the correct audio stream and is not blocked by DND.
- **Ringtone no longer runs on after dismiss:** `onDestroy()` in `RingingActivity` now calls `stop()` before `release()`; `mediaPlayer` is set to `null` afterwards.
- **Scheduler moved to background thread:** The n! permutation computation now runs on `Dispatchers.Default` and no longer blocks the main thread (ANR protection for ~7+ members).
- **Color palette refined:** Improved contrast and quality feel in both Light and Dark Mode.
  - Light: Background `#F3F7FB` (Night Blue tint), cards `#E8F0F8` (clearly elevated), error container clearly red instead of warm grey.
  - Dark: Background `#0F1923` (deeper blue-black), primary `#8DAFC8` (brand-aligned, 5.5:1), accent SunriseOrange `#FFB37A` (5.2:1).
- **`compileSdk` / `targetSdk` set to 35:** Avoids Java 21 dependency introduced by the android-36.1 extension platform.

### Fixed
- **App crashes after login:** `FamilyViewModel` and `PreferencesRepository` no longer crash on first launch when no `familyId` is available yet.
- **Alarm not cancelled on member delete:** `cancelWakeUp()` is now called for every deleted member, not only for the current user's profile.
- **Race condition when claiming a profile:** `claimMember()` now uses an atomic Firestore transaction instead of `get() + update()`. Two users can no longer simultaneously claim the same profile.
- **Double MediaPlayer on screen rotation:** `RingingActivity` is now locked to `screenOrientation="portrait"` in the manifest, preventing Android from recreating the activity and starting a second audio track.

## [0.3.2] - 2026-02-26
### Added
- **New App Icon:** The icon has been updated to match the modern design of the website favicon.
- **Security Rules (Firestore):** Added a new `firestore.rules` file to secure access to families and members and enforce deletion logic on the server side.

### Changed
- **Color Concept & Contrast:** Comprehensive overhaul of the color palette (Deep Night Blue, Sunrise Orange). Dark Mode now features significantly satterer contrasts and a consistently dark header (TopAppBar) across all screens.
- **Optimized Deletion Logic:** 
    - Unclaimed members can now be deleted by any family member.
    - Your own profile can be deleted/unclaimed at any time.
    - The alarm is automatically deactivated when deleting your own profile.
- **Resilient Family Deletion:** Deleting a family is now more robust against individual member deletion failures. The family document is securely removed in any case.
- **Error Handling:** Error messages (e.g., after failed actions) are now reliably reset upon logout or leaving the family and no longer appear on the login screen.

### Fixed
- **Build Error:** Added missing `Color` references in the UI to ensure error-free compilation.

## [0.3.1] - 2026-02-26
### Added
- **Profile Ownership (Claiming):** Introduced a new system where users "claim" a family member in the settings. This prevents other users from using the same profile or modifying someone else's wake-up times.
- **Security Hardening (Firestore):** New database rules enforce profile ownership at a technical level. Only the "owner" of a profile can modify or delete its data.
- **Strict Alarm Logic & Auto-Enable:** Removed automatic fallback to the first profile. The global alarm is now OFF by default and automatically switches ON only after a profile has been successfully claimed. Alarms cannot be enabled without a profile assignment.
- **UI Navigation:** The "No profile selected" warning on the dashboard is now clickable and navigates directly to the settings screen.
- **Improved Button Placement:** Moved the "+" button for adding members from a Floating Action Button (FAB) directly into the Family Members section.
- **Prioritized Layout:** The calculated wake-up plan is now positioned at the top of the dashboard, followed by the member list, as it provides the most important daily information.
- **Automatic Profile Restoration:** After re-installation or re-login, a previously claimed profile is automatically recognized and selected.
- **Robust Deletion & Navigation Sync (V 0.3.1):** Deleting a family now checks for other active members and reliably deletes all member documents in a batch process before removing the family itself.
- **Synchronized Navigation (Fix):** All app components now use synchronized data streams for user preferences, guaranteeing absolutely reliable redirection to the dashboard after login.
- **State Restoration:** The status of the master alarm switch (On/Off) is now saved across sessions and automatically restored upon next login.
- **Visualized Member Status (3-state):** The member list now distinguishes: "(Alarm active)" [Green] or "(no alarm)" [Red], but only if a profile has been claimed. Unclaimed profiles show no status text (Purely for calculation).
- **Settings UI Polish:** Renamed the logout button and added a horizontal divider for better visual separation.

### Changed
- **Improved Profile Selection:** The dropdown menu in the settings is now disabled as long as no family members have been created.
- **Dynamic Warnings:** The warning for missing profile assignment only appears if there are already members in the family.

### Fixed
- **Navigation Issue (Backstack):** Fixed a bug where the login screen remained in the background after successful login. A "back" swipe now correctly exits the app.

## [0.3.0] - 2026-02-26
### Added
- Display of the family name in the settings: The name of the family is now displayed next to the invitation code to avoid confusion when joining or managing.
- Optimized Navigation: A new Loading Screen prevents the "double swipe" issue when exiting and ensures a clean app startup.
- Logout on Setup Screen: A new button allows users to sign out directly from the initial setup flow, in case they want to use a different account.
- Robust Versioning: The internal version code is now based on a timestamp, ensuring reliable APK updates.

## [0.2.9] - 2026-02-25
### Fixed
- Fixed a crash during Google Login (`NoCredentialException`) that occurred when no Google accounts were registered on the device yet.
- Optimized account selection during Google Login (`setAutoSelectEnabled(true)`) to prevent system-level aborts in the Android `CredentialManager`.
- Code Cleanup: Removed unused imports and unutilized variables.

## [0.2.8] - 2026-02-25
### Added
- Complete "Delete Family" feature added to Settings, which safely removes the family including all members from the database.
- Users on other devices are now automatically redirected to the setup screen if their family was deleted.
- Introduced automated, descriptive file naming (incl. version and build number) for the compiled APK.

### Changed
- The layout of the settings page now visually separates the developer support email button from the remaining web links using a subtle horizontal divider.

### Fixed
- Google Sign-In errors on the Login screen are now displayed in red directly on screen (for easier SHA-256 fingerprint debugging).
- Fixed the URLs for the German Imprint, Privacy Policy, and Account Deletion pages, as they accidentally included a `-de` suffix.
- Added `default_web_client_id` directly to `strings.xml` to prevent annoying "Unresolved reference" errors in Android Studio following a clean build.
## [0.2.7] - 2026-02-25

### Added
- **Delete Account:** Added a "Delete Account" button to the support section in settings that links to the corresponding account deletion page (Play Store policy requirement).

## [0.2.6] - 2026-02-25

### Added
- **Flexible Breakfast:** The breakfast time now adapts to tight schedules and automatically shortens by 5 to 10 minutes if the shared family schedule otherwise wouldn't work out.
- **English Documentation:** Both the README and the Changelog are now fully available in English and properly linked.

### Changed
- **Legal Documents:** The links to the Imprint and Privacy Policy in the README and in the app settings now point to the correct native live website URLs depending on the language.

## [0.2.5] - 2026-02-24

This is the initial public release on GitHub after comprehensive code cleanup and UI polish.

### Added
- **Multilingual Support:** The app is now fully available in German and English. The language can be toggled manually in the settings or adapts to system settings.
- **Cloud-Synchronized Alarm Switch (Vacation Mode):** The dashboard now features a switch ("Alarm Enabled/Paused"). This not only deactivates your local alarm but also notifies the cloud that you don't need the bathroom today. As a result, the rest of the family can sleep in longer entirely automatically. Paused members are highlighted in the dashboard with a "(Paused)" label and faded colors.
- **Support Section:** A new support section has been added to the app settings, featuring direct email contact to the developer.
- **Legal Documents:** Added links to the Privacy Policy and Imprint to the app settings (for App Store / Play Store compliance).
- **Adaptive App Icon:** A new, high-resolution, and responsive app icon supporting all common Android launcher formats.
- **System Splash Screen:** Natively supported Android 12+ (API 31+) Splash Screen that seamlessly transitions into the app's first screen.
- **README Documentation:** Screenshots and explanatory feature overview for visitors of the GitHub repository.

### Changed
- **Flexible Alarm Algorithm:** The calculation algorithm now intelligently works around conflicts (e.g., overlapping bathroom times) and automatically adjusts wake-up times in 5-minute increments (up to +/- 15 minutes) to find a viable schedule.
- **Dark Mode Adjustments:** Improved color scheme (Theme.kt) for a more eye-friendly, high-contrast display when Night Mode ("Dark Mode") is active.
- **Settings Layout:** The settings have been logically regrouped. Version and Copyright info are now prominently placed as a footer at the bottom.
- **Ringing Screen:** Optimized the layout of the red wake-up screen. Padding and centered text now prevent names from being cut off on small displays or with long strings.
- **Project Name:** The display name on the home screen and in the app has been standardized to `FamWake - Family Alarm` (or `Familienwecker`).

### Fixed
- **Duplicate Splash Screens:** Fixed a bug where newer Android versions displayed the Android system logo followed by an additional loading-style Activity splash screen.
- **Placeholder Texts:** Hardcoded developer names ("Familie Notthoff" / "Smith Family") have been removed from the text fields during family setup and replaced with neutral examples ("Musterfamilie" / "Sample Family").

---
*Older, internal development builds (prior to version 0.2.5) are not documented in this public repository.*
