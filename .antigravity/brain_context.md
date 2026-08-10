# FamWake – Brain Context

## Projekt
- Familienwecker-App (iOS + Android), Repo: `familienwecker`
- iOS: Swift/SwiftUI, AlarmKit (iOS 26+), Firebase, RevenueCat
- Android: Kotlin/Jetpack Compose, Firebase, RevenueCat
- Letzter Push & Tag: `v2.1.1` (2026-08-08) – iOS v2.1.1 mit neuen Grafiken und Metadaten online.
- Letzte lokale Version: v2.1.1-dev (Android) / v2.1.1 (iOS)

## Neu in Version 2.1.1 (August 2026)
- **iOS Bengali & Marathi Support**:
  - Reaktiviert `bn-BD` (Bangla) und `mr-IN` (Marathi) in den iOS-Generierungsskripten (`generate_screenshots_ios.py` und `generate_ios_metadata.py`).
  - Fastlane auf `2.237.0` (in `Gemfile`s gelockt) aktualisiert, da erst ab `2.234.0` nativer Support für diese beiden Locales vorhanden ist.
  - In `ios/fastlane/Fastfile` die Option `ignore_language_directory_validation: true` und `overwrite_screenshots: true` aktiviert. Bypasst veraltete lokale Validierungen und bereinigt alte Screenshot-Duplikate im Store.
  - **CI-Runner-Fix**: In `ios-metadata.yml` and `ios-screenshots.yml` das automatische `bundler-cache: true` entfernt und durch `bundle update fastlane` ersetzt, damit der Runner (Ruby 3.2) alle Abhängigkeiten (inklusive `CFPropertyList 3.0.8`) sauber auflöst.

## ASO Screenshot-Optimierung & Parität (August 2026)
- **Android Screenshots (1080x1920)**:
  - Vollständiger Nachbau der originalen 16:9-Layouts. Skalierung des Pixel-Geräts vergrößert (Breite `880px`), sodass es links/rechts ausfüllt und oben/unten (je nach Text-Layout) über den Rand ragt.
  - Entfernung des Top-Crops bei iOS-Rohdaten, um App-Navigations-Header (z. B. *"Mitglied bearbeiten"*) voll funktionsfähig zu erhalten. Android-Statusbar und Punch-Hole werden auf Y=0-100 gezeichnet und überdecken Simulator-Icons perfekt.
- **iOS Screenshots (1242x2688 & 1290x2796)**:
  - iPhone-Mockup-Breite auf `1010px` skaliert (mehr Präsenz, weniger Freiraum an den Seiten).
  - Schriftgrößen proportional vergrößert (Headline `120`, Description `50`).
  - Textabstände nach oben bei Slide 1, 2, 4, 5 verringert (`text_y = 140 * scale`), um jegliche Überlappungen mit dem Telefon-Mockup zu verhindern.
  - Slide 3 und 6: Gerät tiefer gesetzt (`paste_y = -220 * scale`) und Text tiefer gesetzt (`text_y = 1980 * scale`), um Abstand zum Gerät zu vergrößern.
- **Lesbarkeits-Vignette (Android & iOS)**:
  - Bei textuntenliegenden Slides (3 & 6) wird am unteren Rand ein sanfter, dunkelblauer Farbverlauf (Vignette) gerendert, um den Kontrast des weißen Beschreibungstextes auf hellgelbem/orangefarbenem Hintergrund massiv zu erhöhen.
- **Git-Tracking**:
  - Da `docs/internal/` per `.gitignore` ausgeschlossen ist, müssen alle Rohdateien und gerenderten Vorschaubilder zwingend mit `git add -f docs/internal/images/screenshots/` hinzugefügt werden, um sie im Repository zu sichern.

## Principle of Action
- Bei Unsicherheit: konservativ ändern statt kaputtmachen
- AlarmKit cancel() is nötig für Screen-Dismissal – niemals weglassen
- recalculateSchedule() darf aktiven Snooze nicht überschreiben
