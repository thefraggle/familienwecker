# ⏰ **FamWake** Familienwecker / Family Alarm

[![Website: familienwecker.de](https://img.shields.io/badge/Website-familienwecker.de-blue)](https://www.familienwecker.de)

*[🇬🇧 English version](README.en.md)*

Schluss mit dem morgendlichen Chaos! **FamWake** ist der intelligente Familienwecker & Bad-Planer, der euren Morgen als perfekt eingespieltes Team organisiert.

👉 **Alle Informationen, Features und Early Access auf:**  
🌐 [familienwecker.de](https://familienwecker.de)

---

## ✨ Die Highlights

* **Smarter Fahrplan:** Ein intelligenter Algorithmus koordiniert Bad, Frühstück und Aufstehen für die ganze Familie.
* **Synchronisierter Morgen:** Live-Status für alle – wer wann ins Bad darf, steht fest. Keine Schlange, kein Stress.
* **Maximale Flexibilität:** "Bin schon wach"-Button für Frühaussteher und intuitive Drag & Drop Planung.
* **Sicher & Privat:** Anmeldung via Google oder E-Mail. Keine Werbung, kein Datenverkauf – dein Morgen gehört dir.

---

## 🛠️ Entwickler-Dokumentation (Developer Setup)

Dieses Projekt ist eine Kotlin Multiplatform (KMP) App für iOS und Android mit einem Firebase-Backend.

### Voraussetzungen (Prerequisites)
* **OS**: macOS (erforderlich für iOS-Builds)
* **JDK**: Java 17+ (z. B. Azul Zulu)
* **Android**: Android Studio & Android SDK
* **iOS**: Xcode 15+ & CocoaPods
* **Backend**: Node.js & Firebase CLI (`npx firebase-tools`)

### Projektstruktur
* `/app`: Android App (Jetpack Compose, targetSdk 36)
* `/ios`: iOS App (SwiftUI, iOS 16+)
* `/shared`: KMP Modul (Weckplan-Logik, Shared Preferences, Datenbanken)
* `/functions`: Firebase Cloud Functions (Node.js)
* `/scripts`: Python-Hilfsskripte für ASO-Metadaten und Screenshot-Framing

### Wichtige Befehle (Core Commands)

#### 1. Firebase Backend deployen
Aus dem Projekt-Root-Verzeichnis:
```bash
npx firebase-tools deploy
```

#### 2. Rohe iOS-Screenshots über Simulatoren erstellen (Laufzeit ~2h)
Wechseln in das Verzeichnis `/ios/` und ausführen:
```bash
bundle exec fastlane generate_screenshots
```
Die Rohbilder werden unter `docs/internal/images/screenshots/devices/{lang}/` abgelegt.

#### 3. App Store Screenshots framen (iPhone Mockup + Texte)
Aus dem Projekt-Root-Verzeichnis:
```bash
python3 scripts/generate_screenshots_ios.py
python3 scripts/generate_html.py
```
Die fertigen Grafiken liegen unter `/ios/fastlane/screenshots/`, die HTML-Vorschaugalerie unter `/ios/fastlane/screenshots/screenshots.html`.

#### 4. Play Store Screenshots framen (Pixel Mockup + Texte)
Aus dem Projekt-Root-Verzeichnis:
```bash
python3 scripts/generate_screenshots_android.py
```
Die fertigen Play-Store-Grafiken liegen unter `/android/fastlane/metadata/android/`.

#### 5. Google Play Store Feature Graphics generieren
Aus dem Projekt-Root-Verzeichnis:
```bash
python3 scripts/generate_feature_graphics.py
```
Erstellt die `1024x500` Feature Graphics in allen 22 Sprachen unter `/docs/internal/images/feature_graphics/` und kopiert sie direkt in die Fastlane-Verzeichnisse.

#### 6. Git-Tracking für ASO-Bilder
Da `docs/internal/` in der `.gitignore` eingetragen ist, müssen neu generierte Roh- und Framed-Bilder mit `force` hinzugefügt werden:
```bash
git add -f docs/internal/images/screenshots/ docs/internal/images/feature_graphics/
```

---

## ⚖️ Copyright & License

Copyright (c) 2026 Daniel Notthoff. All rights reserved. 

The source code in this repository is provided for educational and review purposes only. It may not be copied, modified, or distributed without explicit permission from the author.

* [Changelog (Version History)](docs/CHANGELOG.md)
* [Roadmap (Geplante Features)](docs/ROADMAP.md)
* [Datenschutzerklärung (Privacy Policy)](https://familienwecker.de/privacy-policy.html)
* [Impressum (Imprint)](https://familienwecker.de/imprint.html)
* [Konto & Daten löschen (Account Deletion)](https://familienwecker.de/account-deletion.html)
