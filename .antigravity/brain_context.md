# FamWake – Brain Context

## Projekt
- Familienwecker-App (iOS + Android), Repo: `familienwecker`
- iOS: Swift/SwiftUI, AlarmKit (iOS 26+), Firebase, RevenueCat
- Android: Kotlin/Jetpack Compose, Firebase, RevenueCat
- Letzter Push & Tag: `v2.1.1` (2026-08-08) – iOS v2.1.1 mit neuen Grafiken und Metadaten online.
- Letzte lokale Version: v2.1.1-dev (Android) / v2.1.1 (iOS)

## ASO & Store Listings Status (KW 34 / August 2026)
- **Applyra Analyse**: `.antigravity/applyra_status_report.md` erstellt.
- **Store Listings synchronisiert**:
  - iOS & Android für alle 29 Sprachen aktualisiert (DE/US mit High-Potential Keywords wie *kostenlos*, *laut*, *badplaner*, *kids morning routine*, *shared alarm clock*).
  - Zeichen- und Byte-Limits (Titel <=30, Kurzbeschreibung <=80, Volltext <=4000 Bytes) in allen 29 Sprachen zu 100% validiert.
  - Sammeldatei `STORE_LISTINGS.md` in Root, `android/fastlane/metadata/android/` und `docs/internal/play_store_listings/` bereitgestellt.
  - Generator-Fix: `generate_android_metadata.py` löscht `images/` nicht mehr. Quelltexte in `docs/internal/play_store_listings/` synchronisiert.

## Screenshot & Feature-Graphics (August 2026)
- **Norwegisch (no / nb)**: Mapping-Konflikt gelöst, Snapshot-Lauf stabilisiert.
- **Indonesisch (id)**: Play-Store-Verzeichnis `"id"` ergänzt.
- **Pillow Feature-Graphics**: TTC-Font-Fix für CJK/Hindi/Bengali (`factor = 1.45`). HTML-Übersicht (`screenshots.html`) mit Feature-Graphics-Tab aktiv.

## GitHub Issues & Features (August 2026)
- **Offen**:
  - **Issue #26**: UX: Vertikales Padding um Zahnrad & Header-Größe optimieren (iOS & Android)
  - **Issue #27**: UX: Wochentag-Auswahl vom Hauptbildschirm beim Bearbeiten eines Mitglieds übernehmen
  - **Issue #36**: Gamification & Kids Mode (Reward Timer & Panda Stars)
  - **Issue #39**: Flatshare / Co-Living Mode (Neutral UI Wording & Equal Admin Rights)
  - **Issue #40**: Feature: Shift Work & Flexible Schedule Profiles
  - **Issue #41**: Refactor: "Ich bin schon wach" Button-Logik auf relatives 4h-Zeitfenster anpassen
  - **Issue #42**: Bug (iOS): Settings-Icon (Zahnrad) glitched/springt wiederholt auf dem Screen

## Principle of Action
- Bei Unsicherheit: konservativ ändern statt kaputtmachen
- AlarmKit cancel() ist nötig für Screen-Dismissal – niemals weglassen
- recalculateSchedule() darf aktiven Snooze nicht überschreiben
