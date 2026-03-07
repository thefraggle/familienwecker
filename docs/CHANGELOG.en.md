# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

*[🇩🇪 Deutsche Version](CHANGELOG.md)*

## [0.5.11] - 2026-03-07
 ### Self-Healing & Deep Link Fix
 This version fixes critical issues with the login flow via deep links and introduces automatic error correction for permission-related problems.

 ### Fixed
 - **Self-Healing:** The app now detects `PERMISSION_DENIED` errors (e.g., for deleted families) and automatically clears the local state to lead back to the setup screen.
 - **Deep Link Race Condition:** Joining after login now always takes precedence over restoring old profiles. Fixes the issue where users landed on the wrong screen after logging in.
 - **State Synchronization:** Consistent cleanup of local data when a cloud profile no longer exists.

## [0.5.10] - 2026-03-07
 ### Deep Linking, Navigation & CI Optimization
 This version introduces comprehensive deep linking for family invitations, optimizes app navigation on errors, and accelerates the build pipeline.

 ### Added
- **Deep Linking:** Support for `familienwecker.de/join/[CODE]`. Links now open the app directly.
- **Automatic Join:** When clicking a link, the user is automatically added to the family (after login).
- **Conflict Handling:** Smart dialog when attempting to join a new family while already being a member.

 ### Improved & Optimized
- **App Link Verification:** Best-practice manifest structure to increase the probability of automatic verification on Android 12+.
- **Build Pipeline (CI/CD):** Enabled Gradle build cache, parallel execution, and configuration cache in GitHub Actions for significantly faster deployments.
- **CI Performance:** Increased heap memory to 4GB for more stable build runs.
- **Invitation Sharing:** Revised invitation text with a direct link for a seamless user experience.
- **Join Logic:** Profile selection is now preserved when re-joining a family the user is already a member of.

 ### Fixed
- **Profile Claim:** Fixed an issue where the user's own profile was shown as "occupied" after re-joining (e.g., via link).
- **Navigation Stability:** Automatic family switching is now atomic. In case of errors (e.g., invalid code), a clean backtrack to the setup screen occurs, including the error message.
- **Configuration Cache:** Build metadata is now passed via Gradle properties, fixing build failures in the CI process.
- **Multiple Instances:** App no longer opens twice when clicking deep links (`singleTop`).
- **Dialog UX:** The conflict dialog now closes reliably in all error scenarios.

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
