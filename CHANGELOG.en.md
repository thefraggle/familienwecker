# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

*[🇩🇪 Deutsche Version](CHANGELOG.md)*

## [0.2.6] - 2026-02-25

### Added
- **Flexible Breakfast:** The breakfast time now adapts to tight schedules and automatically shortens by 5 to 10 minutes if the shared family schedule otherwise wouldn't work out.

### Changed
- **Legal Documents:** The links to the Imprint and Privacy Policy in the README and in the app settings now point to the correct native live website URLs depending on the language.

## [0.2.5] - 2026-02-24

This is the initial public release on GitHub after comprehensive code cleanup and UI polish.

### Added
- **Multilingual Support:** The app is now fully available in German and English. The language can be toggled manually in the settings or adapts to system settings.
- **Cloud-Synchronized Alarm Switch (Vacation Mode):** The dashboard now features a switch ("Alarm Enabled/Paused"). This not only deactivates your local alarm but also notifies the cloud that you don't need the bathroom today. As a result, the rest of the family can sleep in longer entirely automatically. Paused members are highlighted in the dashboard with a "(Paused)" label and faded colors.
- **Support Section:** A new support section has been added to the app settings, featuring direct email contact to the developer.
- **Legal Documents:** Added links to the Privacy Policy and Imprint to the app settings (for App Store / Play Store compliance).
- **Adaptive App Icon:** A new, high-resolution, and responsive app icon supporting all common Android launcher formats.
- **System Splash Screen:** Natively supported Android 12+ (API 31+) Splash Screen that seamlessly transitions into the app's first screen.
- **README Documentation:** Screenshots and explanatory feature overview for visitors of the GitHub repository.

### Changed
- **Flexible Alarm Algorithm:** The calculation algorithm now intelligently works around conflicts (e.g., overlapping bathroom times) and automatically adjusts wake-up times in 5-minute increments (up to +/- 15 minutes) to find a viable schedule.
- **Dark Mode Adjustments:** Improved color scheme (Theme.kt) for a more eye-friendly, high-contrast display when Night Mode ("Dark Mode") is active.
- **Settings Layout:** The settings have been logically regrouped. Version and Copyright info are now prominently placed as a footer at the bottom.
- **Ringing Screen:** Optimized the layout of the red wake-up screen. Padding and centered text now prevent names from being cut off on small displays or with long strings.
- **Project Name:** The display name on the home screen and in the app has been standardized to `FamWake - Family Alarm` (or `Familienwecker`).

### Fixed
- **Duplicate Splash Screens:** Fixed a bug where newer Android versions displayed the Android system logo followed by an additional loading-style Activity splash screen.
- **Placeholder Texts:** Hardcoded developer names ("Familie Notthoff" / "Smith Family") have been removed from the text fields during family setup and replaced with neutral examples ("Musterfamilie" / "Sample Family").

---
*Older, internal development builds (prior to version 0.2.5) are not documented in this public repository.*
