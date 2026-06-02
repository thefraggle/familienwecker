## 2026-06-02T15:41:47Z

# Teamwork Project Prompt — Draft

Führe ein umfassendes Audit der "FamWake"-App für beide Plattformen durch: Die iOS-App (SwiftUI) und die Android-App (Kotlin/Compose). Das Audit soll architektonische, UX/UI- und plattformspezifische Verbesserungspotenziale identifizieren und in einem detaillierten, gemeinsamen Bericht zusammenfassen.

Working directory (Android): `/Users/daniel.notthoff/GIT_Repos/_privat/familienwecker`
Working directory (iOS): `/Users/daniel.notthoff/GIT_Repos/_privat/familienwecker/ios`
Integrity mode: development

## Requirements

### R1. Parität & Plattform-Optimierung (HIG & Material Design)
Prüfe die Feature-Parität beider Apps. Stelle sicher, dass die iOS-Version konsequent die Apple Human Interface Guidelines (HIG) einhält (z.B. NavigationStack, native Sheets, Alerts) und identifiziere versehentlich übernommene Android-Muster. Stelle gleichzeitig sicher, dass die Android-Version sauberes Material Design nutzt. 
Ausnahme: Der FAB-Button für neue Mitglieder ist bei iOS explizit erwünscht und soll bestehen bleiben.

### R2. UI, UX, Layout & Onboarding
Prüfe die Architektur (SwiftUI & Jetpack Compose) auf Best Practices (effiziente Container, Modifier, Safe Areas). Evaluiere zudem das Onboarding auf beiden Plattformen auf Intuitivität und Benutzerführung.

### R3. Offline-First Ansatz
Überprüfe die Datenhaltung und Netzwerk-Aufrufe beider Plattformen. Zeige auf, wo lokales Caching, Queues oder Offline-Speicherungs-Mechanismen fehlen, damit die Apps ohne Internetverbindung vollständig bedienbar bleiben.

### R4. Fehlerhandling
Suche nach unsauber abgefangenen Fehlern, potenziellen Absturzrisiken (z. B. Force Unwrapping in iOS, NullPointerExceptions in Android) und bewerte die Qualität der Fehlermeldungen für den Nutzer (Graceful Degradation).

### R5. Strings & Übersetzungen
Finde hartkodierte Texte und prüfe, ob durchgängig die nativen Lokalisierungssysteme (String Catalogs `.xcstrings` bei iOS, `strings.xml` bei Android) inklusive Plural-Regeln sauber verwendet werden.

### R6. Wecker-Logik
Analysiere die Implementierung der Wecker-Kernfunktionen auf beiden Systemen (AlarmManager/AlarmKit): das Klingeln, die "Snooze / Bin schon wach"-Logik und die Auswahl der Sounds.

## Acceptance Criteria

### Audit Report
- [ ] Es wird eine Datei `audit_report.md` erstellt.
- [ ] Der Report vergleicht iOS und Android aktiv miteinander und ist nach den 6 Requirements gegliedert.
- [ ] Jeder Kritikpunkt enthält konkreten, lauffähigen SwiftUI/Swift- (für iOS) bzw. Kotlin/Compose- (für Android) Beispielcode als Verbesserungsvorschlag.
- [ ] Das Feedback ist priorisiert in "Kritisch", "Wichtig" und "Best Practice".
- [ ] Der Report ist kurz und verständlich formuliert.
- [ ] Die Agents nehmen **keine** eigenmächtigen Änderungen am Quellcode der Apps vor, sondern dokumentieren die Vorschläge nur im Report.
