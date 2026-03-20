# Changelog

This format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

*[🇩🇪 Deutsche Version](CHANGELOG.md)*

## 1.5.1 - 2026-03-20

### Fixed
- **Midnight Conflict:** Removed the restrictive 03:00 AM guard in the algorithm, allowing early alarms (e.g., 00:15) to work correctly.

### Changed
- **Conflict UX:** Implemented "best-effort" fallback alarms for the active user when a valid schedule cannot be found.
- **Error Messages:** More detailed tips for resolving conflicts and visual indicators for active fallback alarms on the main screen.
- **Versioning:** Switched to tag-driven versioning to sync Play Store and codebase.

---

### New
- **Admin Console & Statistics:** Secure, reactive admin status via Firestore (`_admins` collection); manual and weekly statistics report via email for admins.
- **Onboarding Refactor:** New onboarding tour (5 screens) with Panda Lottie animations and background visuals (dark scrim for improved readability).
- **App Design:** New adaptive & legacy app icon; Dark Mode as default for new users; redesigned RingingScreen.
- **Cloud Reset Logic:** Hourly cron job for status resets (2h threshold) and more efficient data refresh on app start.

### Fixed
- **Security & Validation:** Password manager support (`AutofillNode` + `Username` metadata); password validation (min. 8 chars); comprehensive field audit; XSS protection in feedback emails; migration to Firebase Secrets (Resend API).
- **Stability:** Fixed member mapping (timestamp fix); improved deep link flows (verification before switching); fixed double alarms (notification + activity) and redundant join dialogs.
- **Build & CI:** Fixed versioning and automated AAB file naming in GitHub Actions.

---

## 1.4.0 - 2026-03-19

### New
- **Cloud Reset Logic & Performance:** New hourly cron job for status resets (2h threshold) and more efficient data refresh on app start.
- **Onboarding & Design:** New onboarding tour (5 screens), Panda Lottie animations, and redesign of the RingingScreen.
- **Security Audit & Fixes:** Fixed an XSS vulnerability in feedback emails and verified IDOR security.

### Fixed
- **Autofill & Login:** Massive improvement in password manager compatibility (`AutofillNode` + `Username` metadata). Fixes context menu blockages.
- **Stability:** Fixed member mapping (timestamp fix), deep link flows, and double alarms (notification + activity).
- **UI/UX:** Better distribution of weekday chips, red marking for errors, and clickable disclaimers/footers.


---

## [1.3.0] - 2026-03-17

### Added
- **⭐ Rate app:** New button in the Help & Feedback section. Opens the in-app review dialog (Play In-App Review API); falls back to the Play Store page if unavailable.

### Changed
- **Weekday chips:** DE and EN now use uniform 2-letter abbreviations (Mo Di Mi Do Fr Sa So / Mo Tu We Th Fr Sa Su) so all 7 chips fit in the available width.
- **Inactive days:** Chips for disabled weekdays are now clearly dimmed (text, border and background at ~30% opacity).
- **Settings footer:** New order: Version → Copyright → All rights reserved → Links.
- **Rate limits (Cloud Functions):**
  - Email Reset/Verify: max. 5 per hour + max. 10 per day
  - Join family: max. 5 per minute + max. 10 per day
  - Create family: max. 3 per hour + max. 6 per day

### Fixed
- **Chip text invisible:** Selecting an inactive weekday chip showed unreadable text on the filled background. Fix: muted grey container instead of primary color.
- **Crash on "create family" rate limit:** `RESOURCE_EXHAUSTED` exception from the Cloud Function is now correctly caught.
- **Rate-limit errors fully covered:** All three rate limits (create, join, email) now show specific error messages. `resendVerificationEmail` result is now evaluated.

---

## [1.2.0] - 2026-03-17

### Added
- **Alarm state persistence:** After reinstall + login, the last known alarm state (on/off) is automatically restored from Firestore.

### Fixed
- **Join flow double dialog:** When the join link was for the user's own family, `onLeaveFamily()` was called, incorrectly redirecting to the setup screen. Fix: same-family guard now returns `false`.
- **Create family – loop + crash (new account):** `refreshData()` called `leaveFamily()` when Firestore returned `null` briefly after `createFamily()` (propagation race condition). This destroyed the newly created family and crashed the app on the second attempt. Fix: removed `leaveFamily()` from `refreshData()` – self-healing runs via the members-flow collector.
- **Create family – redirect to setup:** `LaunchedEffect` in `MainScreen` called `onLeaveFamily()` when `familyId` briefly appeared as `null` during an active sync. Fix: guard `familyId == null && !isSyncing`.
- **Alarm restore after reinstall (multiple race conditions):**
  - `myMemberId` observer called `setAlarmEnabled(false)` on app start (initial `null`).
  - `initialAlarmPushDone` block wrote `false` to Firestore before restore could run.
  - `isAlarmEnabled` observer wrote `false` to Firestore on logout, overwriting the member's alarm state.
  - All three race conditions fixed; order: restore first, then proactive Firestore sync.
- **Breakfast time incorrect:** When no departure time was set, 23:59 was incorrectly used as the base → unrealistic times (e.g. 23:29). Fallback is now `latest wake time + bathroom duration`.
- **Weekday configuration ignored:** Day-specific times from day profiles were ignored when calculating the schedule. Fix: effective fields are now correctly resolved before the scheduler is called.

### Removed
- **"What's new" dialog** completely removed (dialog, logic, JSON file, strings).

---

## [1.1.5] - 2026-03-17

### Added
- **Weekday configuration:** Wake times and bathroom duration can be set individually per weekday.
- **Feedback screen:** Dedicated feedback screen with category selection, message, optional email, and automatically included device data.
- **Firebase feedback sending:** Feedback is sent directly via a Firebase Cloud Function (Resend) as an email – no more classic mail client intent. Every submission is also archived in Firestore.
- **Feedback UX:** Form is cleared after submission; screen closes automatically after 2.5 seconds.
- **Settings footer:** Version number, clickable legal links (terms of use, privacy, imprint) and copyright now visible as a footer.
- **Delete account:** External link to `familienwecker.de/account-deletion.html` (DE) or `/account-deletion-en.html` (EN) instead of an info dialog.
- **Settings restructured:** Language and appearance combined in one card; Help & Feedback in its own card.
- Terms of Use linked directly in settings.
- Disclaimer on the registration screen with clickable links to terms of use and privacy policy.

### Fixed
- Algorithm: breakfast conflict now detected when bathroom end = breakfast start (0 min buffer).
- Algorithm: post-validation ensures no breakfast member finishes their bathroom after breakfast starts.
- Offline display: incorrect "offline" icon after app start fixed (only shown on real network failure).
- Cloud Functions: rate limit counter not saved correctly on first call (tx.update → tx.set).
- Cloud Functions: join attempt limit increased from 5 to 10 per minute.
- CI: AAB filename in manual GitHub builds was incorrect.
- When leaving a family, the member's profile is now fully deleted from Firestore.
- Email sending (password reset, opt-in confirmation) was blocked on first request due to incorrect rate limit document.
- "Leave family" incorrectly disconnected other devices (self-healing without existence check).
- After leaving, the account claim remained in the Firestore profile (ghost claim).
- String audit: obsolete and unused strings removed; both languages fully synchronized.

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
