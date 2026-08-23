# ⏰ **FamWake** Familienwecker / Family Alarm

[![Website: familienwecker.de](https://img.shields.io/badge/Website-familienwecker.de/en-blue)](https://www.familienwecker.de/index-en.html)

*[🇩🇪 Deutsche Version](README.md)*

Stop the morning chaos! **FamWake** is the smart family alarm & bathroom scheduler that organizes your morning as a perfectly synced team.

👉 **Find all information, features and early access at:**  
🌐 [familienwecker.de/en](https://familienwecker.de/index-en.html)

---

## ✨ The Highlights

* **Smart Roadmap:** An intelligent algorithm coordinates bathroom, breakfast, and wake-up for your entire family.
* **In Sync:** Live status for everyone – scheduled bathroom slots for the whole family. No more queues, no stress.
* **Maximum Flexibility:** "Already awake" button for early birds and intuitive drag & drop planning.
* **Secure & Private:** Sign in via Google or Email. No ads, no data sales – your morning belongs to you.

---

## 🛠️ Developer Setup

This project is a Kotlin Multiplatform (KMP) App for iOS and Android with a Firebase backend.

### Prerequisites
* **OS**: macOS (required for iOS builds)
* **JDK**: Java 17+ (e.g. Azul Zulu)
* **Android**: Android Studio & Android SDK
* **iOS**: Xcode 15+ & CocoaPods
* **Backend**: Node.js & Firebase CLI (`npx firebase-tools`)

### Project Structure
* `/app`: Android App (Jetpack Compose, targetSdk 36)
* `/ios`: iOS App (SwiftUI, iOS 16+)
* `/shared`: KMP shared module (scheduling logic, shared preferences, databases)
* `/functions`: Firebase Cloud Functions (Node.js)
* `/scripts`: Python helper scripts for ASO metadata and screenshot framing

### Core Commands

#### 1. Deploy Firebase Backend
From the project root directory:
```bash
npx firebase-tools deploy
```

#### 2. Capture Raw Simulator Screenshots (Takes ~2h)
Switch to the `/ios/` directory and run:
```bash
bundle exec fastlane generate_screenshots
```
Raw screenshots will be stored in `docs/internal/images/screenshots/devices/{lang}/`.

#### 3. Frame App Store Screenshots (iPhone Mockup + Text Overlay)
From the project root directory:
```bash
python3 scripts/generate_screenshots_ios.py
python3 scripts/generate_html.py
```
Framed images are saved to `/ios/fastlane/screenshots/`, and the HTML preview gallery is located at `/ios/fastlane/screenshots/screenshots.html`.

#### 4. Frame Play Store Screenshots (Pixel Mockup + Text Overlay)
From the project root directory:
```bash
python3 scripts/generate_screenshots_android.py
```
Framed Play Store graphics are saved to `/android/fastlane/metadata/android/`.

#### 5. Generate Play Store Feature Graphics
From the project root directory:
```bash
python3 scripts/generate_feature_graphics.py
```
This generates the `1024x500` feature graphics for all 22 languages in `/docs/internal/images/feature_graphics/` and copies them straight to the fastlane directories.

#### 6. Git Tracking for ASO Graphics
Since `/docs/internal/` is added to `.gitignore`, newly generated raw or framed images must be added using `force`:
```bash
git add -f docs/internal/images/screenshots/ docs/internal/images/feature_graphics/
```

---

## ⚖️ License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for details.

* [Website & Landing Page](https://familienwecker.de/index-en.html)
* [Changelog (Version History)](docs/CHANGELOG.en.md)
* [Privacy Policy](https://familienwecker.de/privacy-policy.html)
* [Imprint](https://familienwecker.de/imprint-en.html)
* [Account Deletion](https://familienwecker.de/account-deletion-en.html)
