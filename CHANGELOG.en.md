# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

*[🇩🇪 Deutsche Version](CHANGELOG.md)*

## [0.3.1] - 2026-02-26
### Added
- **Profile Ownership (Claiming):** Introduced a new system where users "claim" a family member in the settings. This prevents other users from using the same profile or modifying someone else's wake-up times.
- **Security Hardening (Firestore):** New database rules enforce profile ownership at a technical level. Only the "owner" of a profile can modify or delete its data.
- **Strict Alarm Logic & Auto-Enable:** Removed automatic fallback to the first profile. The global alarm is now OFF by default and automatically switches ON only after a profile has been successfully claimed. Alarms cannot be enabled without a profile assignment.
- **UI Navigation:** The "No profile selected" warning on the dashboard is now clickable and navigates directly to the settings screen.

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
