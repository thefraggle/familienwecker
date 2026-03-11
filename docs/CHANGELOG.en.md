# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

*[🇩🇪 Deutsche Version](CHANGELOG.md)*

## [0.6.5] - 2026-03-11
### Fixed
- **Missing Translations:** Replaced hardcoded German error strings (e.g., "Family not found") when joining a family with localized string resources, ensuring these messages are properly translated into English.
- **Missing UI Translations:** Localized hardcoded texts in the Settings screen (Profile Claiming, Appearance) and Authentication system (Login, Registration) for better DE/EN support.

## [0.6.4] - 2026-03-11
### Security & Architecture
- **Family Code (Join Code):** Generation now uses `SecureRandom` instead of `Random` for true cryptographic randomness and protection against brute-force predictions.
- **Dependency Injection:** `FirebaseRepository` is now injected via `Factory` into the `FamilyViewModel` instead of hardcoded instantiation (improves testability).

## [0.6.3] - 2026-03-10
### Fixed
- **Deep Link Join Conflict:** Fixed a bug where a user already in a family wouldn't see a conflict dialog (warning) when trying to join a new family via deep link.
- **Deep Link Join Validation:** An invalid code now correctly shows an error message instead of accidentally leaving the user with no family.

## [0.6.2] - 2026-03-10
### Fixed
- **Deep Link Navigation:** Fixed a bug where clicking an invitation link would falsely trigger a global logout.
- **Deep Link Loop:** Prevents infinite loading loops if joining a family via link fails.

## [0.6.1] - 2026-03-10
### Fixed
- **Alarm Localization:** Notifications (title, text, channel name) now correctly appear in the device language instead of always in German.
- **unclaimMember Race Condition:** Releasing a profile now uses an atomic Firestore transaction (same as `claimMember`).
- **deleteFamily Consistency:** Members are now deleted via `WriteBatch` – no inconsistent state on network interruption.
- **Alarm Cancel on Leave:** `leaveFamily()` now correctly cancels the system alarm before leaving.
- **Dead Code Removed:** Unused `isInWindow` block in `toggleAwakeMember` and orphaned `generatePermutations()` removed.
- **Toast Replaced by UiText:** Missing alarm permission is now shown as a localized UI error message instead of a hardcoded Toast.
- **PreferencesRepository Double-Emit:** Listener guards prevent double StateFlow emissions on each setter call.
- **Fallback Username:** `"Papa/Mama"` hardcoded replaced by `R.string.settings_fallback_username` (DE: "Elternteil", EN: "Parent").

### Optimized
- **ScheduleMessage:** New type-safe `sealed class ScheduleMessage` replaces raw strings in Scheduler – messages are now fully localized.
- **Drag & Drop Height:** `itemHeightPx` measured dynamically via `LazyListState` instead of hardcoded 110 dp.
- **Duplicate State Collect:** Redundant `themePreference` collect in `MainScreen` removed.
- **Alarm Debounce:** Alarm is only rescheduled when the target time actually changed.
- **Offline Indicator:** CloudOff icon appears only after 3 seconds offline (no flickering on brief disconnects).
- **refreshData Guard:** No unnecessary Firestore call on temporary empty `""` familyId.
- **Self-Join Guard:** `joinFamily()` skips rejoining if the entered code is already the current one.

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
