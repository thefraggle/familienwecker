# Changelog

This format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

*[🇩🇪 Deutsche Version](CHANGELOG.md)*

## 1.6.12 - 2026-03-26

### Optimized
- **Audit:** Fixed minor audit findings in Alarm Receiver.
- **KMP:** Migrated internal time logic from java.time to multiplatform kotlinx.datetime.
- **Performance:** Drastically reduced startup jank by moving heavy initialization to background threads (Coroutines Dispatchers.IO).

## 1.6.11 - 2026-03-26

### Optimized
- **Code Quality:** Internal cleanup for improved stability and fewer unnecessary cloud requests.
- **Security:** Firestore rules refined – member profiles are now better protected against unauthorized changes.

## 1.6.10 - 2026-03-25

### Optimized
- **Stability:** Core architecture significantly refactored for better reliability and future iOS support.
- **Data Management:** Faster and more robust synchronization between the app and the cloud.
- **Settings:** Personal preferences are now stored more securely and reliably.

## 1.6.7 - 2026-03-25


### New
- **Data Loss Protection:** Added confirmation dialog for unsaved changes in the edit screen (8 languages).
- **UI Cleanup:** Time displays across the app are now uniformly formatted to `HH:mm` (seconds/milliseconds removed).

### Fixed
- **State Stability:** Fixed a bug where the edit screen lost its state during background synchronizations.

## 1.6.6 - 2026-03-25

### Fixed
- **Login Resilience:** Fixed critical crash (`MissingFormatArgumentException`) in German localization.
- **Permission Hardening:** Fixed family creation issues for non-admin users (Firestore Rules & Repository optimization).
- **Stability:** All asynchronous background tasks in `FamilyViewModel` are now wrapped in safety guards.
- **Self-Healing:** Implemented silent re-sync logic for Firestore listeners to handle transient permission delays.

### Optimized
- **System:** Completed Android 15 Edge-to-Edge support across all screens.
- **Completeness:** All 8 languages (DE, EN, ES, FR, IT, NL, PL, PT) are now 100% synchronized (271 keys).

## 1.6.5 - 2026-03-25

### New
- **Languages:** Added full support for Portuguese (pt), Polish (pl), and Dutch (nl). The app now supports a total of 8 languages.
- **Legal:** Integrated language-specific links for Terms of Use, Privacy Policy, Imprint, and Account Deletion (e.g., `terms-en.html`).

### Optimized
- **Localization:** Ensured 100% synchronization of all 265 resource keys across all 8 languages.
- **Onboarding:** Improved fallback logic for screenshots in newly supported languages.

### System
- **Build:** Fixed JVM toolchain resolution errors and improved build stability.

## 1.6.4 - 2026-03-24

### New
- **System:** Enabled support for Predictive Back Gestures (Android 13+).
- **Security:** Admin status is now validated purely on the server side via Firestore (preparing for multi-admin support).
- **Firestore:** Refined Security Rules for admin access.

### Optimized
- **RevenueCat:** Preparations for improved language synchronization and optimized data fetching (on-demand).
- **SDK Update:** Updated RevenueCat SDK to version 9.23.1.

## 1.6.3 - 2026-03-24

### Fixed
- **Navigation:** Fixed a bug where a duplicate instance of the main screen remained in the background after completing the onboarding tour.

## 1.6.2 - 2026-03-23

### Optimized
- Bug fixes and optimizations

## 1.6.1 - 2026-03-23

### New
- **Support for everyone:** The option to support or rate the app is now visible to all users in the settings.

### Optimized
- **Donation Dialog:** Updated tier prices (1.79€, 4.79€, 9.49€) and improved loading indicators during checkout.
- **Full Localization:** All donation texts and status messages are now available in German, English, French, Italian, and Spanish.
- **Play Store Release Process:** Optimized delivery of release notes for faster publication.

## 1.6.0 - 2026-03-23

### New
- **Play Store Optimization:** Enhanced metadata and optimizations for the official Play Store release.
- **Full Localization:** Added support for French, Spanish, and Italian with automatic language detection.
- **Email System:** Password reset, confirmation, and verification emails are now fully translated into the app's selected language.
- **Deep-Link UX:** Improved visual feedback when joining a family via an invitation link.

### Optimized
- **Reliability:** Continuous improvements to data synchronization and overall app stability.
- **Security Audit:** Re-enforced access permissions for family data and strengthened privacy rules.
- **Onboarding:** Modernized introduction tour with panda animations and Dark Mode as the default for new users.

### Fixed
- **Invitation Codes:** Fixed an issue where codes would become invalid if a family was temporarily empty.
- **Email Delivery:** Resolved translation issues that previously caused an unwanted fallback to German.
- **Scheduling:** Fixed time calculations for midnight and very early alarm times.

## 1.5.0 - 2026-03-21

### New
- **Onboarding:** New introduction tour with Panda animations for an easier start.
- **App Design:** New, modern app icon; Dark Mode as default for new users; improved design of the alarm view.
- **Admin Console & Statistics:** Secure access to app statistics for administrators; weekly reports via email.
- **Admin Area:** Management tools for administrators moved to a new menu.
- **Security:** Improved protection of private family data from unauthorized access.
- **Privacy:** Individual user data removed from internal reports.
- **Stability:** More reliable joining and leaving of families.
- **Data Sync:** Automatic background status updates for accurate information.

### Changed
- **Privacy:** Increased protection for user profiles against unauthorized changes.
- **Stability:** More secure deleting and leaving of families through new background logic.
- **Versioning:** Standardized version numbers for better clarity.
- **Conflict Resolution:** Improved support and guidance for schedule overlaps.
- **System Updates:** Optimized app update process.

### Fixed
- **Scheduling:** Fixed midnight time calculation; early alarms now work correctly.
- **Family Deletion:** Corrected ID mapping error; creators and global admins can now reliably delete families again.
- **Security:** Improved password manager support and protection against malicious inputs.
- **Stability:** Fixed member display issues and duplicate alarm sounds; improved joining process via link.
- **Internal Optimizations:** Improved app build processes and file naming.

## 1.4.0 - 2026-03-19

### New
- **Data Sync:** Automatic background status updates for accurate information on app start.
- **Design:** New introduction tour and Panda animations for a friendly welcome.
- **Security:** Internal audit successfully completed and protection against malicious scripts reinforced.

### Fixed
- **Login:** Massive improvement in password manager compatibility.
- **Stability:** Fixed member display issues and duplicate alarm sounds.
- **UI/UX:** Better distribution of weekday chips, red marking for errors, and clickable disclaimers/footers.


---

## [1.3.0] - 2026-03-17

### Added
- **⭐ Rate app:** Rate directly in the Play Store from within the app.

### Changed
- **Weekday chips:** Uniform abbreviations for a clearer display.
- **Inactive days:** Disabled days are now clearly visually highlighted.
- **Security:** Anti-spam protection for emails and family invites enabled.

### Fixed
- **Display:** Improved readability of weekdays.
- **Stability:** Fixed bugs in family creation and improved error messages for request limits.

---

## [1.2.0] - 2026-03-17

### Added
- **Alarm state persistence:** After reinstall + login, the last known alarm state (on/off) is automatically restored from Firestore.

### Fixed
- **Join flow:** Fixed various background issues related to joining families.
- **Stability:** Fixed a bug in family creation that could lead to an app crash.
- **Background Sync:** Improved data comparison on app start.
- **Bug fixes:** Fixed several synchronization errors on app start to correctly maintain the alarm status.
- **Scheduling:** Corrected breakfast calculation and considered individual weekday settings.

### Removed
- **"What's new" dialog** completely removed (dialog, logic, JSON file, strings).

---

## [1.1.5] - 2026-03-17

### Added
- **Weekday Planning:** Wake times and bathroom duration can now be set individually per weekday.
- **Feedback:** Direct sending of feedback messages from within the app.
- **Feedback UX:** Form is cleared after submission; screen closes automatically after 2.5 seconds.
- **Settings footer:** Version number, clickable legal links (terms of use, privacy, imprint) and copyright now visible as a footer.
- **Delete account:** External link to `familienwecker.de/account-deletion.html` (DE) or `/account-deletion-en.html` (EN) instead of an info dialog.
- **Settings restructured:** Language and appearance combined in one card; Help & Feedback in its own card.
- Terms of Use linked directly in settings.
- Disclaimer on the registration screen with clickable links to terms of use and privacy policy.

### Fixed
- **Schedule:** More reliable conflict detection for bathroom and breakfast times.
- **Network:** Improved display of offline status.
- **Security:** Improved anti-spam protection for too many requests.
- **Stability:** Fixed several bugs related to leaving families and deleting member profiles.
- **Localization:** Cleaned up unused texts and synchronized all languages.

---

## [1.1.0] - 2026-03-15

### Added
- Scroll indicator (↓) on the main screen while no members are present.
- **Snooze:** Snooze button on the alarm screen (5 min). A banner with end time and cancel button is shown on the main screen during an active snooze.

### Fixed
- Alarm rings after device reboot (even before PIN entry).
- Alarm screen on lock screen (Samsung, Xiaomi, etc.).
- Google OAuth in self-signed APKs.
- Firebase cleanup job incorrectly deleted new families.
- Battery tile disappears immediately after confirmation.

---

## [1.0.0] - 2026-03-12

### Security
- HTTP invitation links are rejected – only HTTPS allowed.
- Only the family creator can delete the family; others receive an error message.
- Profile selection blocked offline – prevents misleading timeout errors.

### Fixed
- Join link opens the conflict dialog immediately, even when the app is in the background.
- Deleting a family now works even when other users have active profiles.
- Member limit at 6 instead of 5 active persons.
- Direct joining by other users without conflict dialog fixed.
- Family name now displays correctly in the header after restart.
- Optimized loading: no more flickering on app start.
- Complete localization (DE/EN) of all new strings.

---
