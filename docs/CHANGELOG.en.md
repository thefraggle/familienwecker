# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

*[🇩🇪 Deutsche Version](CHANGELOG.md)*


## [1.0.0] - 2026-03-12
### 🎉 First Stable Release
The first complete, production-ready version of FamWake.

### Security
- **HTTP links rejected:** Invitation links are now only accepted via HTTPS.
- **Admin-only deletion:** The family can only be deleted by its creator. Other members receive a clear error message.
- **Offline claim guard:** Profile selection is disabled when offline – prevents misleading timeout errors.

### UX & Bug Fixes
- **Deep link instant dialog:** Join links now immediately show the conflict dialog, even when the app is already running in the background.
- **Delete family with claimed members:** Families can now be deleted even when other users have active profiles.
- **Offline icon:** Now displayed correctly even when writes are pending (instead of an infinite sync spinner).
- **WhatsNew button text:** The "What's new" dialog now uses the configurable button text from the JSON configuration.

---

## [0.9.x] - 2026-03-12
### Summary (Pre-Release Stabilization)
Consolidates all improvements, security fixes, and code quality work since 0.9.0.

### Security & Code Quality (Security Audit)
- **Cloud Function `createFamily` (H-5):** Family creation runs entirely server-side – no direct client write access possible (`allow create: if false`).
- **App singleton used consistently (H-1/H-2):** `RingingActivity` and `BootReceiver` use the `FamWakeApplication` singleton.
- **Email rate-limiting (H-3):** All Cloud Functions for email sending enforce server-side rate-limiting (max. 3/hour per address).
- **Cryptographically secure PRNG (M-1):** Join code generation uses `crypto.randomInt()` instead of `Math.random()`.
- **Deprecated window flags removed (M-2):** `FLAG_SHOW_WHEN_LOCKED` / `FLAG_TURN_SCREEN_ON` removed from `RingingActivity`.
- **Debug guards:** All `Log.e()` calls protected with `BuildConfig.DEBUG`.
- **Boot resilience:** Alarms are rescheduled automatically after device restart.
- **Type-safe navigation:** Central `Routes` object prevents typo-related crashes.
- **Password validation:** Minimum 8 characters checked client-side.

### New Features
- **Alarm status sync:** The alarm status of claimed members is live-synced – no app restart required.
- **CompositionLocal dark theme:** `isSystemInDarkTheme()` called once at theme root (`LocalDarkTheme`).
- **Battery warning:** Settings show a warning when battery optimization is active.
- **Admin detection:** `createdByUserId` is loaded, `isAdmin` property exposed in ViewModel.

---

## [0.9.0] - 2026-03-12
### Summary (Consolidation Release)
Bundles all critical security improvements, bug fixes, and localization updates since 0.8.0.

### Security & Audit
- **Secure Join Flow:** Family joining via secure Cloud Function with server-side rate-limiting.
- **Data Integrity:** Overhaul of Firestore Security Rules.
- **Encryption:** Local settings migrated to `EncryptedSharedPreferences` (AES-256).
- **Privacy:** `joinCode` no longer stored in the user profile.

### Localization & UX
- **Error mapping:** Firebase auth errors fully localized (DE/EN).

### Fixed
- **Multi-Device Sync:** Alarm switch is now device-specific.
- **Stability & Offline:** UI freezes and race conditions fixed.

---

## [0.8.x] - 2026-03-12
### Highlights
- **Offline Detection:** More reliable via `NET_CAPABILITY_VALIDATED`.
- **Stability:** Scheduler guard for midnight-overflow; robust family deletion.
- **Offline Robustness:** App startup max. 2 seconds without internet.
- **Deep Links:** Fixed `SingleTask` intent handling; links work with app already running.

## [0.7.x] - Summary (March 2026)
### Added & Optimized
- **Performance & Architecture:** `ImmutableList` for more efficient Compose rendering.
- **Design:** Material You (Dynamic Colors) on Android 12+ and true AMOLED Black Mode.
- **Accessibility:** Increased touch targets in Settings.

### Fixed
- **Localization (I18n):** All hardcoded strings fully translated (DE/EN).
- **Deep Links:** Fixes for conflicts, validation, and infinite loops.
- **Stability:** Race conditions in profile unclaiming and family deletion.

## [0.6.x] - Summary (March 2026)
### Added
- **Drag & Drop Reordering:** Sort members via long-press with spring animations.
- **Offline UI & Sync:** Status icons in the top bar for offline mode and cloud sync.
- **Deep Linking:** Full support for `familienwecker.de/join/[CODE]`.

### Fixed
- Crash on first member, race conditions in deep link joins, incorrect profile claims.

## [0.5.x] - Summary (March 2026)
### Play Store Release (Update)
- **Design 2.0:** OLED Dark Mode, glassmorphism, improved typography.
- **What's New Popup:** Intelligent news box shown after updates.
- **Support Links:** Privacy policy, imprint, and email support from within the app.

## [0.4.x] - Summary (March 2026)
- **Design & UX:** Glassmorphism, Lottie animations, AMOLED Dark Mode.
- **Stability:** Auto-deletion of orphaned families (180 days), 6-member limit.
- **UX:** Unambiguous invitation codes, auto-reset of paused profiles.

## [0.3.x] - Summary (February 2026)
- **Profile Ownership:** Introduced profile "claiming".
- **Alarm Precision:** New alarm system (Android 14 support, fullscreen wake screen).
- **Security Update:** Strict permission management and secure cloud storage.

## [0.2.5] - 2026-02-24
Initial public release.
- Focused on wake-up algorithm, multilingual support (DE/EN), and intuitive handling.
