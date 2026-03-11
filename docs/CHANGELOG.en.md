# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

*[🇩🇪 Deutsche Version](CHANGELOG.md)*

## [0.7.1] - 2026-03-11
### Optimized & Fixed
- **Build (R8):** The app is now properly minified and obfuscated (via R8) in release mode. The corresponding `mapping.txt` file for crash symbolication is seamlessly embedded into the `.aab` file for the Google Play Console.
- **CI/CD (GitHub Actions):** Massive speedup of the GitHub Release Action (from ~9 back to ~3 minutes). The workflow now correctly utilizes isolated Kotlin & Gradle cache warming on the `main` branch.

## [0.7.0] - 2026-03-11
### Added & Optimized
- **Performance & Architecture:** Implemented `ImmutableList` for more efficient Compose rendering and improved Dependency Injection (`FirebaseRepository`).
- **Security:** `SecureRandom` used for family code generation to prevent predictability.
- **Design:** Added support for Material You (Dynamic Colors) on Android 12+ and a true AMOLED Black Mode (`#000000`).
- **Accessibility:** Increased Touch Targets in Settings to align with safe interaction minimums.

### Fixed
- **Localization (I18n):** Resolved missing translations for errors and UI elements in email/join flows and Settings (full DE/EN support). Alarms now correctly respect the configured system locale.
- **Deep Links:** Extensive fixes for conflicts, validation, and infinite loops when joining via invitation links.
- **Stability:** Resolved race conditions during profile unclaiming (Atomic Transaction) and family deletion (WriteBatch). `leaveFamily()` now reliably cancels the underlying system alarm.

## [0.6.0] - 2026-03-09
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

## [0.5.0] - 2026-03-06
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

## [0.4.14] - 2026-03-05
 
 ### Added
- **Lottie Animations:** Integrated dynamic animations for Empty States ("No alarm set" and "No members added").
- **Layout Optimization:** Improved dashboard spacing for a more compact appearance.
 
 ### Changed
- **EmptyState:** Support for Lottie resources including fallback to static images.

## [0.4.13] - 2026-03-05

### Added
- **Empty States:** Beautiful illustrations and new `EmptyState` component for empty lists.
- **UI Refinement:** App name in header and footer without hyphens; modernized settings structure (3-line style).

### Changed
- **Strings & Localization:** Cleaned and updated language resources for a more professional impact.
- **UX:** Automatic data refresh when app starts in the foreground.

## [0.4.12] - 2026-03-04

### Changed
- **Language Optimization:** Refined error messages to be user-friendly and intelligently translated (EN/DE), replacing technical server exceptions.

## [0.4.11] - 2026-03-04
- **Error Handling:** Fixed a visual issue on startup during data recovery.

## [0.4.10] - 2026-03-04
- **Stability Update:** Fixed a crash during server connection.

## [0.4.9] - 2026-03-04
- **Error Handling:** Fixed an issue where a fresh installation (from a backup) could briefly trigger a access error.

## [0.4.8] - 2026-03-04

### Added
- **Housekeeping:** Automated system cleans up orphaned families after 180 days to maintain high performance.

### Changed & Improved
- **Battery & Performance:** Massive optimization of background processes. The app is now battery-friendly while maintaining 100% alarm precision.
- **Dark Mode:** Refined dark theme with perfect contrasts across all screens.
- **Better Planning Limit:** Set a limit of 6 active members per schedule to ensure stability on all devices.
- **Safe Codes:** Invitation codes now exclude ambiguous characters (like 0 vs O) to prevent typing errors.

### Fixed
- **Auto-Reset:** Paused members are now reliably reactivated for the upcoming morning.

## [0.4.7] - 2026-03-04

### Added
- **Visual Feedback:** Bounce effect when clicking buttons and interactive elements for a more responsive feel.
- **Icons & Help:** New icons for external links (Privacy, Imprint) and email support for better orientation.
- **Security:** Uniqueness check for family codes upon creation.

### Changed
- **Settings Structure:** Logical reordering of sections (Profile, Alarm Sound, Family, Language, Help & Support).

## [0.4.6] - 2026-03-04

### UI & UX Overhaul (Dark Mode 2.0)
- **Deep Dark Mode:** Switched the dark theme to near-black backgrounds for an ultra-modern look and battery-friendly display (OLED).
- **Contrast Optimization:** Refinement of all text and icon colors for the new design.
- **Branding:** Consistent **FamWake** logo in the header.
- **Font Strategy:** Switched to "Nunito" font in various weights for modern typography.

## [0.4.5] - 2026-03-04

### UI & UX Modernization
- **Glassmorphism:** Introduction of semi-transparent cards with subtle borders.
- **Gradients:** Soft vertical color gradients depending on the theme.
- **CornerRadius:** Increased to softer 24dp - 32dp.
- **Theme Selection:** Support for "Light", "Dark", and "System" including manual selection in settings.

## [0.4.x] - Further Improvements
- **Unit Tests:** Automated tests for the core algorithm (`Scheduler`).
- **Icon Cleanup:** Removed unused graphics to reduce app size.
- **Email Branding:** Designed HTML emails for password reset and verification (DE/EN).

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
