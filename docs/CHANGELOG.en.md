# Changelog

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

*[🇩🇪 Deutsche Version](CHANGELOG.md)*


## [1.1.1] - 2026-03-16

### Fixed
- Email sending (password reset, opt-in verification) was broken: rate-limit document was not created correctly on the first request.
- Password reset error: error code was evaluated as a number, causing `includes()` to fail.
- "Leave family" incorrectly terminated sessions on other devices (self-healing triggered without existence check).
- Leaving the family left the user's account claim in the Firestore member profile (ghost-claim).

---

## [1.1.0] - 2026-03-15

### New
- Scroll indicator (↓) on main screen while no members are added yet.

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
