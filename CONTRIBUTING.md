# Contributing to FamWake

Thank you for your interest in contributing to FamWake! We welcome bug reports, feature suggestions, and pull requests.

## Code of Conduct

This project adheres to the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## Development Setup

### Prerequisites
* **macOS** (required for building the iOS application)
* **JDK 17+** (e.g. Azul Zulu or Eclipse Temurin)
* **Android Studio** & Android SDK (Compile SDK 36)
* **Xcode 16+** & Swift 5 / Swift 6 toolchain

### Project Architecture
* `/app`: Android app written in Kotlin and Jetpack Compose.
* `/ios`: iOS app written natively in Swift and SwiftUI (using AlarmKit, ActivityKit).
* `/shared`: Internal Android library module (Room DB, scheduler algorithm).
* `/functions`: Firebase Cloud Functions backend (Node.js).

### Building the Apps

#### Android
```bash
# Run unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug
```

#### iOS
Open `ios/FamWake.xcodeproj` in Xcode or run:
```bash
xcodebuild -project ios/FamWake.xcodeproj -scheme FamWake -destination "generic/platform=iOS Simulator" build CODE_SIGNING_ALLOWED=NO
```

## Pull Request Guidelines

1. **Keep it focused**: One bugfix or feature per PR.
2. **Test your code**: Ensure all unit tests pass before submitting.
3. **No secrets**: Never commit private keys, keystores, or configuration files (`local.properties`, `google-services.json`, `GoogleService-Info.plist`, `Secrets.xcconfig`).
4. **Commit messages**: Use [Conventional Commits](https://www.conventionalcommits.org/) (e.g., `fix: ...`, `feat: ...`, `docs: ...`).

## Questions & Contact

For questions, support, or feedback, please reach out to **famwake@goork.de** or open an issue on GitHub.

