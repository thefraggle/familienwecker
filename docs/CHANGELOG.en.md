# Changelog

This format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

*[🇩🇪 Deutsche Version](CHANGELOG.md)*

## 1.7.3 - 2026-04-10

### Improved
- **Language fallback** – Unknown or invalid language codes now automatically fall back to English instead of German.

---

## 1.7.2 - 2026-04-10

### New
- **Norsk 🇳🇴 and Dansk 🇩🇰** – FamWake now speaks Norwegian and Danish. Welcome on board!
- **14 languages** – The language picker now covers 14 world languages plus 3 regional dialects.

### Improved
- **Language picker** – Cleaner sheet-style selection menu, sorted alphabetically.
- **Appearance switcher** – Sun/Auto/Moon icons instead of a dropdown – works correctly on narrow devices too.
- **Weekly schedule** – The "Copy to other days" link is now always visible right below the day tabs.
- **Schedule field order** – Optimised layout: wake times and bathroom first, then leave time, breakfast last.
- **Small screens** – All screens are now fully usable; the keyboard no longer pushes content off-screen.

---

## 1.7.1 - 2026-04-08

### Improved
- **Time picker** – Time fields now open a clean keyboard-style dialog – no more auto-jumping from hours to minutes.
- **Keyboard** – Tapping outside the name field now dismisses the keyboard.
- **Bathroom buttons** – The "−" and "+" buttons remain fully visible on narrow screens and in all languages.

---

## 1.7.0 - 2026-04-06

### New
- **Swedish 🇸🇪** – FamWake now speaks Swedish. Välkommen!
- **Dialect languages 🎉** – Swabian, Swiss German, and Ruhr Valley slang. Wake up in your home dialect. (Not an April Fools' joke.)
- **Reliable alarms** – If alarm permissions are missing, a large red warning tile appears immediately on the home screen – no more unexpected silent mornings.
- **Auto-claim profile** – When creating your first member profile, it is automatically linked to your account. No manual "This is me" step required.

### Fixed
- **Alarm silent after toggling off and on** – An internal error caused the alarm to stay silent after being re-enabled.
- **"I'm already awake" button** – The button remained permanently grayed out after re-creating a profile.
- **Stability and sync improvements** – Various bugs fixed during startup, family joining, sync, and profile management.
- **Second family member** – Users who joined as the second member were sometimes unable to edit or pause their profile.
- **Global alarm reset on restart** – A startup race condition incorrectly turned the active alarm off after a reboot.
- **"Already awake" status persisted overnight** – Status is now date-bound and resets correctly on the next morning.
- **Login screen with large font sizes** – At 125% or 150% system font size, the login button was out of reach.

### Improved
- **Security** – Access rules tightened, feedback form restricted to logged-in users, password reset no longer reveals whether an email is registered.
- **Weekday scheduling** – Wake times and bathroom duration can be set individually per weekday.
- **11 languages** – Full localization for Portuguese, Polish, and Dutch. Device language detection now works correctly for all system languages.

---

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
