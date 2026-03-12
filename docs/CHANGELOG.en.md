# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

*[🇩🇪 Deutsche Version](CHANGELOG.md)*


## [0.9.5] - 2026-03-12
### Improvements
- **CompositionLocal for Dark Theme:** `isSystemInDarkTheme()` is now called once at the theme root and provided via `LocalDarkTheme` – cleaner architecture, no redundant system state queries.
- **SyncStatus expanded:** The cloud sync indicator now monitors both the family document and the members sub-collection.
- **Password validation:** Client-side check (min. 8 chars) prevents unnecessary Firebase calls for too-short passwords.

## [0.9.2] - 2026-03-12
### Security & Code Quality (Audit Fixes)
- **Boot resilience:** Alarms are automatically rescheduled after device restart (`BootReceiver`).
- **Deep Links:** Only HTTPS scheme allowed – HTTP removed from invitation links.
- **Type-safe navigation:** Navigation routes centralized in `Routes` object; typos now cause compile errors instead of runtime crashes.
- **Battery warning:** Settings screen shows a warning card when battery optimization is active (may delay alarm).
- **SharedPreferences listener:** Properly unregistered on ViewModel destroy – prevents memory leak.
- **Offline fallback:** `getUserFamily()` uses locally cached join code as fallback on Firestore errors.
- **Code cleanup:** Duplicate Firestore mapping centralized in `FamilyMemberMapper`; redundant imports removed.

## [0.9.1] - 2026-03-12
### Added
- **Alarm Status Sync:** The alarm status of claimed family members is now live-synced in the member list. When someone disables their alarm, others see it instantly – no app restart required. The global alarm switch remains device-specific.

## [0.9.0] - 2026-03-12
### Summary (Consolidation Release)
Bundles all critical security improvements, bug fixes, and localization updates since 0.8.0.

### Security & Audit
- **Secure Join Flow:** Family joining now uses a secure Cloud Function with server-side rate-limiting.
- **Data Integrity:** Overhaul of Firestore Security Rules; access strictly limited to verified members.
- **Encryption:** Local settings migrated to `EncryptedSharedPreferences` (AES-256).
- **Privacy:** `joinCode` no longer stored in the user profile.

### Localization & UX
- **Error Mapping:** Firebase auth errors (e.g., password too short, email in use) fully localized (DE/EN).
- **Cleanup:** Hardcoded strings in the login area removed.

### Fixed
- **Multi-Device Sync:** Alarm switch is now device-specific – disabling on one device no longer affects others.
- **Stability & Offline:** UI freezes, race conditions in join dialog, and negative `PendingIntent` codes fixed.

## [0.8.x] - 2026-03-12
### Highlights
- **Offline Detection:** More reliable via `NET_CAPABILITY_VALIDATED` (prevents false-positives with captive portals).
- **Stability:** Scheduler guard for midnight-overflow edge cases; improved error logging; robust family deletion.
- **Offline Robustness:** App startup takes max. 2 seconds even without internet. Infinite spinners in join flow fixed.
- **Deep Links:** Fixed `SingleTask` intent handling; invitation links work even when app is already running.
- **Release:** R8 minification and NDK debug symbols (`FULL`) fully integrated.


## [0.7.x] - Summary (March 2026)
### Added & Optimized
- **Performance & Architecture:** Implemented `ImmutableList` for more efficient Compose rendering and improved Dependency Injection (`FirebaseRepository`).
- **Security:** `SecureRandom` used for family code generation to prevent predictability.
- **Design:** Added support for Material You (Dynamic Colors) on Android 12+ and a true AMOLED Black Mode (`#000000`).
- **Accessibility:** Increased Touch Targets in Settings to align with safe interaction minimums.

### Fixed
- **Localization (I18n):** Resolved missing translations for errors and UI elements in email/join flows and Settings (full DE/EN support). Alarms now correctly respect the configured system locale.
- **Deep Links:** Extensive fixes for conflicts, validation, and infinite loops when joining via invitation links.
- **Stability:** Resolved race conditions during profile unclaiming (Atomic Transaction) and family deletion (WriteBatch). `leaveFamily()` now reliably cancels the underlying system alarm.

## [0.6.x] - Summary (March 2026)
### Added
- **Drag & Drop Reordering:** Sort family members via long-press. Includes spring animations for gap preview and haptic feedback.
- **Offline-UI & Sync:** New status icons in the top bar for offline mode and pending cloud synchronization.
- **Deep Linking:** Support for `familienwecker.de/join/[CODE]`. Automatic join after login with conflict handling dialogs.
- **App Description:** Expanded help text to explain the new manual reordering feature.

### Improved
- **Localization:** Full German and English (DE/EN) support across the entire app.
- **UX & Robustness:** Sanitized join codes, join-success popups on the dashboard, and automatic self-healing for permission issues.
- **Performance:** Optimized cloud sync (batch updates after drag-end) and battery optimizations.
- **Navigation:** Unified back-handler behavior and single-instance launch mode for deep links.

### Fixed
- Crash during first member addition (Duplicate Key).
- Race conditions during deep link joins.
- Incorrect profile claim status after re-joining.
- Gradle build-cache and CI stability fixes.

## [0.5.x] - Summary (March 2026)
 ### Release in the Play Store (Update)
 A fully redesigned and stabilized version with focus on family organization and modern UI.
 *Note: The package name has been changed to `de.familienwecker.famwake`.*
 
 ### Added
- **Invitation Sharing:** New system for sharing the family code via Android `ACTION_SEND` (Link: `https://familienwecker.de`).
- **What's New Popup:** Intelligent news box after updates to introduce new features.
- **Lottie Animations:** High-quality, dynamic animations for a modern look.
- **Support Links:** Direct access to privacy policy, imprint, and email support.
 
 ### Changed & Improved
- **Design 2.0:** Modern "OLED" Dark Mode, glassmorphism effects, and improved typography (Nunito).
- **Setup UI:** Optimized process for creating and joining families.
- **Language System:** Full support for German and English with smart, user-friendly error messages.
- **Performance:** Massive battery life improvements and background stability.
 
 ### Fixed
- Numerous small fixes for synchronization and permission management.

## [0.4.x] - Summary (March 2026)
This phase polished the app for a growing user base and enhanced stability:
- **Design & UX:** Introduction of glassmorphism edge effects, Lottie animations for empty states, and a refined AMOLED Deep Dark Mode. Interactive bounce feedback for buttons.
- **Stability & Housekeeping:** Automated deletion of orphaned families (after 180 days) and a hard limit of 6 active members per schedule to prevent out-of-memory crashes.
- **Localization:** Smart, multilingual error messages (DE/EN) instead of technical server exceptions.
- **UX & Scaling:** Unambiguous invitation codes (no `O` or `0`), automatic localized data refresh on app foreground, and auto-resetting of paused profiles for the next day.

## [0.3.x] - Summary (February 2026)
This phase established the core of family planning:
- **Profile Ownership:** Introduced profile "claiming" to protect personal wake-up times.
- **Alarm Precision:** Complete rewrite of the alarm system (Android 14 support, new ringtones, fullscreen wake-up screen).
- **Design Evolution:** Introduction of glassmorphism, smooth transitions, and a modern Dark Mode.
- **Security Update:** Strict permission management and secure cloud storage (Firestore).
- **Validation:** Validation of bathroom durations and departure times to prevent impossible schedules.

## [0.2.5] - 2026-02-24
Initial public release.
- Focused on wake-up algorithm, multilingual support (DE/EN), and intuitive handling.
