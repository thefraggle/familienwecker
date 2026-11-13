# ⏰ **FamWake** – Smart Family Alarm & Morning Scheduler

[![Website: familienwecker.de](https://img.shields.io/badge/Website-familienwecker.de-blue)](https://familienwecker.de)
[![License: GPL-3.0 with Exceptions](https://img.shields.io/badge/License-GPL%203.0%20w%2F%20exceptions-purple.svg)](LICENSE)

Stop the morning chaos! **FamWake** is the smart family alarm & bathroom scheduler that organizes your morning routine as a synchronized team.

👉 **Available now on the App Store and Google Play Store:**  
🌐 [familienwecker.de](https://familienwecker.de)

---

## ✨ Highlights

* **Smart Morning Schedule:** Intelligent scheduling coordinates bathroom slots, breakfast, and wake-up times for the whole family.
* **In Sync & Real-Time:** Live status updates for everyone – no more bathroom queues and zero morning stress.
* **No Account Required:** Start immediately without registration, or use one-tap clipboard join. Optional account (Google / Email) syncs seamlessly across multiple devices.
* **Maximum Flexibility:** Weekday routines, vacation/holiday mode, "Already awake" feature, 30s crescendo gentle alarm, and evening routine check (20:30).
* **Privacy-First:** Strictly privacy-focused, ad-free, and privacy-friendly anonymous analytics (self-hosted Aptabase, no third-party tracking, no cross-app tracking).

---

## 🛠️ Developer Setup

FamWake uses a shared Kotlin Multiplatform (`:shared`) module for algorithms and models on Android, paired with a 100% native SwiftUI implementation on iOS and a Firebase backend.

### Prerequisites
* **OS**: macOS (required for iOS builds)
* **JDK**: Java 17+ (Java 21+ required for Firebase Local Emulators)
* **Android**: Android Studio & Android SDK (Target SDK 36)
* **iOS**: Xcode 16+ & Swift 5.0+
* **Backend**: Node.js 22 & Firebase CLI (`npx firebase-tools`)

### Configuration & Secrets
For local builds, template files are provided and must be populated:
* **Android**: 
  * Place your Firebase configuration in `app/google-services.json`.
  * Create `local.properties` in root with:
    ```properties
    DEFAULT_WEB_CLIENT_ID=your_google_web_client_id
    REVENUECAT_PUBLIC_API_KEY=your_revenuecat_key
    # Optional (only initialized in non-debug builds when both key and host are set):
    APTABASE_APP_KEY=your_aptabase_key
    APTABASE_HOST=https://your-aptabase-host
    ```
* **iOS**: 
  * Copy `ios/FamWake/Config/Secrets.xcconfig.template` to `ios/FamWake/Config/Secrets.xcconfig` and insert your API keys (`APTABASE_APP_KEY` and `APTABASE_HOST` remain empty by default so local builds and forks do not emit telemetry).
  * Place your iOS Firebase configuration in `ios/FamWake/GoogleService-Info.plist`.
* **Cloud Functions**:
  * Copy `functions/.env.example` to `functions/.env` and configure `OPERATOR_NAME`, `OPERATOR_ADDRESS`, and `NOTIFY_EMAIL` for notification emails and feedback forwarding.

### Project Structure
* `/app`: Android Application (Jetpack Compose, Room, Kotlin)
* `/ios`: iOS Application (SwiftUI, AlarmKit)
* `/shared`: Kotlin Multiplatform shared module (scheduler logic, shared models, settings, Room DB)
* `/functions`: Firebase Cloud Functions (Node.js 22 backend)
* `/distribution`: App store assets, listings across 29 locales, and release automation scripts
* `/docs`: Documentation and changelogs (German & English)
* `/internal`: Internal tools, marketing material, and dashboards (git-ignored)

### Core Build Commands

#### Android
```bash
# Run unit tests
./gradlew testDebugUnitTest

# Assemble debug build
./gradlew assembleDebug
```

#### iOS
Open `ios/FamWake.xcodeproj` in Xcode or build via terminal:
```bash
xcodebuild -project ios/FamWake.xcodeproj -scheme FamWake -destination "generic/platform=iOS Simulator" build
```

#### Firebase Backend
```bash
npx firebase-tools deploy
```

---

## ⚖️ License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)** with **GPLv3 Section 7 Additional Permissions** for digital application store distribution (Apple App Store, Google Play Store) and proprietary SDK linking (Firebase, RevenueCat, Aptabase, Apple AlarmKit, Play Integrity). See the [LICENSE](LICENSE) file for the full text.

* [Website](https://familienwecker.de)
* [Architecture](ARCHITECTURE.md)
* [Contributing](CONTRIBUTING.md)
* [Changelog](docs/CHANGELOG.en.md)
* [Security Policy](SECURITY.md)
* [Privacy Policy](https://familienwecker.de/privacy-policy.html)
* [Imprint](https://familienwecker.de/imprint.html)
* [Account Deletion](https://familienwecker.de/account-deletion.html)
* [Contact / Support](mailto:famwake@goork.de)

