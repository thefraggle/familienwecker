# ⏰ **FamWake** – Smart Family Alarm & Morning Scheduler

[![Website: familienwecker.de](https://img.shields.io/badge/Website-familienwecker.de-blue)](https://familienwecker.de)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL%203.0-purple.svg)](LICENSE)

Stop the morning chaos! **FamWake** is the smart family alarm & bathroom scheduler that organizes your morning routine as a synchronized team.

👉 **Available now on the App Store and Google Play Store:**  
🌐 [familienwecker.de](https://familienwecker.de)

---

## ✨ Highlights

* **Smart Morning Schedule:** Intelligent scheduling coordinates bathroom slots, breakfast, and wake-up times for the whole family.
* **In Sync & Real-Time:** Live status updates for everyone – no more bathroom queues and zero morning stress.
* **No Account Required:** Start immediately without registration or create an optional account (Google / Email) to sync seamlessly across multiple devices.
* **Maximum Flexibility:** "Already awake" feature for early birds and intuitive routine adjustments.
* **Privacy-First:** Strictly privacy-focused, ad-free, and no data tracking.

---

## 🛠️ Developer Setup

FamWake is built as a Kotlin Multiplatform (KMP) project for Android and iOS with a Firebase backend.

### Prerequisites
* **OS**: macOS (required for iOS builds)
* **JDK**: Java 17+ (e.g. Azul Zulu)
* **Android**: Android Studio & Android SDK (Target SDK 36)
* **iOS**: Xcode 16+
* **Backend**: Node.js & Firebase CLI (`npx firebase-tools`)

### Project Structure
* `/app`: Android Application (Jetpack Compose, Room, Kotlin)
* `/ios`: iOS Application (SwiftUI, AlarmKit)
* `/shared`: Kotlin Multiplatform shared module (scheduler logic, shared models)
* `/functions`: Firebase Cloud Functions (Node.js backend)

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

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for details.

* [Website](https://familienwecker.de)
* [Changelog](docs/CHANGELOG.en.md)
* [Privacy Policy](https://familienwecker.de/privacy-policy.html)
* [Imprint](https://familienwecker.de/imprint-en.html)
* [Account Deletion](https://familienwecker.de/account-deletion-en.html)
