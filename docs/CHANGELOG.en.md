# Changelog

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

*[🇩🇪 Deutsche Version](CHANGELOG.md)*


## [1.1.9] - 2026-03-17

### Fixed
- **Alarm restore race condition:** The `myMemberId` observer called `setAlarmEnabled(false)` on app start (because `myMemberId` is initially `null`), immediately overwriting the alarm state restored from Firestore. Fix: The initial `null` emission at startup is now ignored.

---

## [1.1.8] - 2026-03-17

### Added
- **Alarm state persistence:** After reinstall + login, the last known alarm state (on/off) is restored from Firestore. If the alarm was on before reinstall, it will be on again.

---

## [1.1.7] - 2026-03-17

### Fixed
- **Join-flow double dialog:** When the join link pointed to the user's own family, `onLeaveFamily()` was triggered, redirecting them to the Setup screen. Fix: Same-family guard now returns `false` – dialog closes, user stays in their family.

### Removed
- **"What's New" dialog** fully removed (dialog, logic, JSON file, strings).

---

## [1.1.6] - 2026-03-17

### Fixed
- **Breakfast time:** When no departure time was set, 23:59 was incorrectly used as the base, resulting in unrealistic breakfast times (e.g. 23:29). Fix: Fallback is now `latest wake-up + bathroom duration`.
- **Weekday configuration:** Day-specific times from day profiles were ignored when calculating the schedule. Fix: Effective fields are now correctly resolved before passing members to the scheduler.

---

## [1.1.5] - 2026-03-17

### Added
- **Weekday Configuration:** Wake times and bathroom duration can be set individually per day of the week.
- **Feedback Screen:** Dedicated feedback screen with category selection, message, optional email, and automatically included device info.
- **Firebase Feedback Sending:** Feedback is sent via a Firebase Cloud Function (Resend) as an email — no system mail intent anymore. Each submission is also archived in Firestore.
- **Feedback UX:** Form is cleared after submission; screen closes automatically after 2.5 seconds.
- **Settings Footer:** Version, clickable legal links (Terms of Use, Privacy Policy, Imprint) and copyright are now visible as a footer.
- **Delete Account:** External link to `familienwecker.de/account-deletion-en.html` (EN) instead of info dialog.
- **Settings Restructured:** Language and Appearance merged into a single card; Help & Feedback in its own card.
- Terms of Use linked directly in Settings.
- Disclaimer in the registration screen with clickable links to Terms of Use and Privacy Policy.

### Fixed
- Scheduler: Breakfast conflict now detected when bath end equals breakfast start (0 min buffer).
- Scheduler: Post-validation ensures no breakfast eater finishes their bath after breakfast begins.
- Offline indicator: False 'offline' display on app start resolved (only shows on real network loss).
- Cloud Functions: Rate-limit counter was not saved correctly on first request (tx.update → tx.set).
- Cloud Functions: Join attempt limit increased from 5 to 10 per minute.
- CI: AAB filename in manual GitHub builds was incorrect.
- When leaving a family, the own member profile is now completely deleted from Firestore.
- Email sending (password reset, opt-in verification) was broken: rate-limit document was not created correctly on the first request.
- "Leave family" incorrectly terminated sessions on other devices (self-healing triggered without existence check).
- Leaving the family left the user's account claim in the Firestore member profile (ghost-claim).
- String audit: Removed obsolete unused strings (Help section, `ok_button`, etc.); both languages fully synchronized.

---

## [1.1.0] - 2026-03-15

### New
- Scroll indicator (↓) on main screen while no members are added yet.
- **Snooze:** Snooze button on the alarm screen (5 min). While a snooze is active, a banner with end time and cancel button is shown on the main screen.

### Fixed
- Alarm rings after device reboot (even before PIN entry).
- Alarm screen shows on lock screen (Samsung, Xiaomi etc.).
- Google OAuth on self-signed APKs.
- Firebase cleanup job incorrectly deleted new families.
- Battery card disappears immediately after confirmation.

---

## [1.0.0] - 2026-03-12

### Security
- HTTP invitation links rejected – only HTTPS accepted.
- Family deletion restricted to the creator; others receive an error message.
- Profile selection disabled offline – prevents misleading timeout errors.

### Fixed
- Join link immediately shows conflict dialog, even when app runs in the background.
- Family deletion works even when other users have active profiles.
- Offline icon displayed correctly when writes are pending.
- "What's new" dialog uses configurable button text instead of hardcoded "OK".

---

## [0.9.x] - 2026-03-12

### Security
- `createFamily` runs entirely server-side; no direct client write access possible.
- Email Cloud Functions: rate-limited to 3 per hour per address.
- Join code generation uses `crypto.randomInt()` instead of `Math.random()`.
- Deprecated `FLAG_SHOW_WHEN_LOCKED`/`FLAG_TURN_SCREEN_ON` removed.
- All `Log.e()` calls guarded with `BuildConfig.DEBUG`.
- Alarms rescheduled automatically after device restart.

### Added
- Alarm status of claimed members live-synced – no app restart needed.
- Admin detection: `createdByUserId` from Firestore, `isAdmin` in ViewModel.
- Battery optimization warning in Settings.
- Client-side password validation: min. 8 characters.
- Type-safe navigation via central `Routes` object.

---

## [0.9.0] - 2026-03-12

### Security
- Family join via Cloud Function with server-side rate-limiting.
- Firestore Security Rules overhauled; access restricted to verified members.
- Local settings migrated to `EncryptedSharedPreferences` (AES-256).
- `joinCode` no longer stored in the user profile.

### Fixed
- Alarm switch device-specific – no unintended sync to other devices.
- UI freezes and race conditions in join dialog resolved.

---

## [0.8.x] - 2026-03-12
- Offline detection via `NET_CAPABILITY_VALIDATED` (no false-positive with captive portals).
- App startup without internet: max. 2 seconds to dashboard.
- Scheduler guard against midnight-overflow edge cases.
- Fixed `SingleTask` intent handling for deep links.

## [0.7.x] - March 2026
- `ImmutableList` for more efficient Compose rendering.
- Material You (Dynamic Colors) on Android 12+; AMOLED Black Mode (`#000000`).
- Race conditions in profile unclaiming and family deletion fixed.
- Full DE/EN localization for all auth errors and UI strings.

## [0.6.x] - March 2026
- Drag & drop sorting of family members with spring animations.
- Offline indicator and sync icon in the top bar.
- Deep linking: `familienwecker.de/join/[CODE]` fully supported.

## [0.5.x] - March 2026
- Design 2.0: OLED Dark Mode, glassmorphism, improved typography.
- "What's new" popup after updates.
- Privacy policy, imprint, and support email accessible from within the app.

## [0.4.x] - March 2026
- Glassmorphism, Lottie animations for empty states, AMOLED Dark Mode.
- Auto-deletion of inactive families (180 days), 6-member limit.
- Unambiguous invitation codes; auto-reset of paused profiles.

## [0.3.x] - February 2026
- Profile claiming to protect personal wake-up times.
- New alarm system (Android 14, fullscreen wake screen).
- Firestore permission management and bathroom time validation.

## [0.2.5] - 2026-02-24
Initial public release. Wake-up algorithm, DE/EN support, intuitive UI.
