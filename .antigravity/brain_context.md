# FamWake – Brain Context

## Projekt
- Familienwecker-App (iOS + Android), Repo: `familienwecker`
- iOS: Swift/SwiftUI, AlarmKit (iOS 26+), Firebase, RevenueCat
- Android: Kotlin/Jetpack Compose, Firebase, RevenueCat
- Letzter Push & Tag: `v2.1.1` (2026-08-08) – iOS v2.1.1 mit neuen Grafiken und Metadaten online.
- Letzte lokale Version: v2.1.1-dev (Android) / v2.1.1 (iOS)

## ASO & Applyra Status (KW 34 / August 2026)
- **Wöchentlicher Report**: `.antigravity/applyra_status_report.md` erstellt.
- **Top-Chancen (Traffic > 10, Diff < 40)**:
  - DE: *wecker kostenlos* (Traffic 63, Diff 33), *wecker laut* (Traffic 39, Diff 35), *badplaner* (Traffic 67), *Zeitmanagement* (Traffic 52)
  - US: *my morning routine* (Traffic 25, Diff 17), *kids morning routine* (Traffic 25, Diff 16), *morning routine free* (Traffic 22, Diff 11)
- **Wettbewerber-Benchmark (Alarmy)**: FamWake Scores: iOS bis 62 (ID/PL/GB 52-62, DE/US 47); Android 11-58 (FR/GB/US 54-58, DE 36).
- **Metadaten-Hebel**:
  - iOS: Redundanzen zwischen Subtitle und `keywords.txt` entfernen, um Platz für *kostenlos*, *laut*, *badplaner*, *shared*, *tracker* zu gewinnen.
  - Android: Keyword-Dichte von Kernbegriffen (*Familienwecker*, *Kinderwecker*, *shared alarm clock*) auf 1.2–1.5% anheben.

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
