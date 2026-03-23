# Changelog

This format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

*[🇩🇪 Deutsche Version](CHANGELOG.md)*

## 1.5.12 - 2026-03-23

### Optimized
- **Reliability:** Regular updates to improve stability and performance.

## 1.5.11 - 2026-03-23
    
### Optimized
- **Reliability:** Internal optimizations for a more stable app performance.

## 1.5.10 - 2026-03-23
    
### Optimized
- **Stability:** Bug fixes and background improvements for better reliability.

## 1.5.9 - 2026-03-23
    
### New
- **Internal Optimizations:** Background preparations for upcoming features and stability improvements.

## 1.5.8 - 2026-03-22

### Optimized
- **Performance:** Optimized resources for a smoother user experience.
- **System Updates:** Updated to the latest Android standards for maximum compatibility.
- **Localization:** Completed support for Spanish, French, and Italian.

## 1.5.7 - 2026-03-21

### New
- **Email Localization:** Password reset, confirmation, and verification emails are now fully sent in the language selected in the app (DE, EN, ES, FR, IT).
- **Password Reset UI:** Added visual feedback (checkmark icon) after successfully sending the reset email.
- **Sender Localization:** The sender name in emails now adapts to the target language (e.g., "Sveglia Famiglia" for Italian).

### Fixed
- **Email Delivery:** Fixed translation errors for automated emails.

## 1.5.6 - 2026-03-21

### New
- **Deep-Link UX:** Added visual feedback (Snackbar "Joining family...") during the join process.
- **Security Hardening:** 
    - Enhanced protection for family data and internal system access.
    - Restricted access to internal system areas.
    - Feedback section protected by improved security rules.
- **Full Localization:** Added support for French, Spanish, and Italian including automatic detection.

### Optimized
- **Localization:** Improved stability for translated texts.
- **Onboarding:** Uses English screenshots as fallback for FR, ES, and IT for now.

### Fixed
- **Invitation Codes:** Codes now remain valid even if a family is temporarily empty.

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
