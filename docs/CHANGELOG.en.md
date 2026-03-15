# Changelog

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

*[🇩🇪 Deutsche Version](CHANGELOG.md)*


## [1.0.4] - 2026-03-15
### Fixed
- Google OAuth failed on self-signed APKs with "No account found" on first use (no previously authorized account).

### Technical
- Two-step Credential Manager flow: check authorized accounts first; on `NoCredentialException` show the account picker.

---

## [1.0.3] - 2026-03-15
### Fixed
- Alarm no longer rings after device reboot (`AlarmManager` entries are wiped on reboot; `EncryptedSharedPreferences` are unreadable before first unlock).
- Alarm screen failed to appear over the lock screen on some devices (Samsung, Xiaomi).

### Technical
- New `AlarmBackupPrefs` store (plain, unencrypted) mirrors every active alarm – readable even before first unlock.
- `BootReceiver` now listens to `LOCKED_BOOT_COMPLETED` (before PIN entry) and restores the exact alarm timestamp.
- `RingingActivity`: added legacy window flags for OEM compatibility.

---

## [1.0.2] - 2026-03-15
### Fixed
- Firebase cleanup job incorrectly deleted newly created families (missing `createdAt` field was interpreted as Unix epoch 1970).
- Scroll indicator on main screen now bounces correctly (wrong animation API replaced).

### Added
- Scroll indicator (↓) on main screen while no family members have been added yet – fades out on first scroll or after the first member is added.

---

## [1.0.1] - 2026-03-12
### Fixed
- Battery optimization card now disappears immediately after confirmation (was: required screen navigation).
- Card layout aligned with other settings cards.
- Battery warning removed from main screen – now only shown in Settings.

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
