# Changelog

Alle wichtigen Änderungen an diesem Projekt werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog](https://keepachangelog.com/de/1.0.0/),
und dieses Projekt hält sich an [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

*[🇺🇸 English Version](CHANGELOG.en.md)*

## [0.7.1] - 2026-03-11
### Optimiert & Behoben
- **Build (R8):** Die App wird im Release-Modus nun sauber via R8 minifiziert (obfuscated). Die entsprechende `mapping.txt` Datei für Crash-Reports wird vollautomatisch ins Android App Bundle (.aab) integriert.

## [0.7.0] - 2026-03-11
### Neu & Optimiert
- **Performance & Architektur:** Einführung von `ImmutableList` für effizienteres Compose-Rendering und verbesserte Dependency Injection für Repositories.
- **Sicherheit:** Verwendung von `SecureRandom` für die Generierung von Familiencodes.
- **Design:** Unterstützung für Material You (Dynamic Colors) ab Android 12 und echter AMOLED Black Mode (`#000000`) für Akkuschonung und bessere Kontraste.
- **Barrierefreiheit (A11y):** Vergrößerte Touch-Targets in den Einstellungen für bessere Nutzbarkeit.

### Behoben
- **Lokalisierung (I18n):** Alle ehemals hardcodierten Fehler und UI-Elemente in E-Mail/Join-Flows und Settings sind nun vollständig (DE/EN) übersetzt. Alarme respektieren die eingestellte System-Sprache.
- **Deep Links:** Umfangreiche Fixes für Konflikte, Validierung und Endlosschleifen beim Beitreten über Einladungslinks.
- **Stabilität:** Race-Conditions beim Profil-Freigeben (Atomic Transaction) und Familien-Löschen (WriteBatch) behoben. `leaveFamily()` bricht nun zuverlässig den eigenen Wecker ab.

## [0.6.0] - 2026-03-09
### Hinzugefügt
- **Drag & Drop Reordering:** Mitglieder können nun per Drag & Drop sortiert werden. Inklusive Spring-Animationen für Gap-Preview und haptischem Feedback.
- **Offline-UI & Sync:** Neue Indikatoren in der Top-Bar für Offline-Modus und ausstehende Cloud-Synchronisierung.
- **Deep Linking:** Volle Unterstützung für `familienwecker.de/join/[CODE]`. Automatischer Beitritt nach Login inkl. Konflikt-Dialogen.
- **App-Beschreibung:** Hilfetexte um die neue Sortierfunktion erweitert.

### Verbessert
- **Lokalisierung:** Volle Unterstützung für Deutsch und Englisch (DE/EN) in der gesamten App.
- **UX & Robustheit:** Sanitisierung von Join-Codes, Join-Success Popups auf dem Dashboard und automatisches Self-Healing bei Berechtigungsfehlern.
- **Performance:** Optimierter Cloud-Sync (Batch-Updates nach Drag-Ende) und Akku-Optimierungen.
- **Navigation:** Einheitliches Back-Handler Verhalten und Single-Instance LaunchMode für Deep Links.

### Behoben
- Crash bei erstem Mitglied (Duplicate Key).
- Race-Conditions beim Deep Link Join.
- Fehlerhafter Profil-Claim Status nach Re-Join.
- Gradle Build-Cache & CI Stabilitäts-Fixes.

## [0.5.0] - 2026-03-06
 ### Veröffentlichung im Play Store (Update)
 Eine komplett überarbeitete Version mit Fokus auf Familien-Organisation, Design und Stabilität.
 *Hinweis: Der Paketname wurde zu `de.familienwecker.famwake` geändert.*
 
 ### Hinzugefügt
- **Einladungs-Sharing:** Neues System zum Teilen des Familien-Codes via Android `ACTION_SEND` (Link: `https://familienwecker.de`).
- **Was ist neu Popup:** Intelligente News-Box nach Updates zur Vorstellung neuer Funktionen.
- **Lottie-Animationen:** Hochwertige, dynamische Animationen für einen modernen Look.
- **Support-Links:** Direkter Zugriff auf Datenschutz, Impressum und E-Mail-Hilfe direkt aus der App.
 
 ### Geändert & Verbessert
- **Design 2.0:** Moderner "OLED" Dark Mode, Glasmorphismus-Effekte und verbesserte Typografie (Nunito).
- **Setup-UI:** Optimierter Prozess zum Gründen und Beitreten von Familien.
- **Sprach-System:** Unterstützung für Deutsch und Englisch in der gesamten App inklusive smarter Fehlermeldungen.
- **Performance:** Massive Optimierung der Akkulaufzeit und Hintergrund-Stabilität.
 
 ### Behoben
- Viele kleine Fixes für die Synchronisierung und das Rechte-Management.

## [0.4.x] - Zusammengefasst (März 2026)
In dieser Phase wurde die App für eine wachsende Nutzerbasis poliert und stabiler gemacht:
- **Design & UX:** Einführung von Glasmorphismus, Lottie-Animationen für Leerzustände und ein verfeinerter AMOLED Dark Mode. Interaktives Bounce-Feedback für Buttons.
- **Stabilität & Hausputz:** Automatisches Löschen verwaister Familien (nach 180 Tagen), Begrenzung auf 6 aktive Mitglieder pro Schlafanalyse für Crash-Prävention.
- **Lokalisierung:** Smarte, mehrsprachige Fehlermeldungen (DE/EN) statt technischer Server-Exceptions.
- **UX & Skalierung:** Verwechslungsfreie Einladungscodes (ohne `O` und `0`), automatische Aktualisierung beim App-Start und Auto-Reset von pausierten Profilen am Folgetag.

## [0.3.x] - Zusammengefasst (Februar 2026)
In dieser Phase wurden die Grundlagen für die Familien-Planung gelegt:
- **Profil-System:** Einführung des "Beanspruchens" von Profilen, um eigene Weckzeiten zu schützen.
- **Wecker-Präzision:** Komplette Überarbeitung des Alarmsystems (Android 14 Support, neue Klingeltöne, Fullscreen-Weckscreen).
- **Design-Evolution:** Einführung von Glasmorphismus, sanften Übergängen und modernem Dark Mode.
- **Sicherheits-Update:** Striktes Rechte-Management und sichere Cloud-Speicherung (Firestore).
- **Validierung:** Prüfung von Badezimmerzeiten und Abfahrtsterminen zur Vermeidung unmöglicher Pläne.

## [0.2.5] - 2026-02-24
Erster öffentlicher Release.
- Fokus auf Weck-Algorithmus, Mehrsprachigkeit (DE/EN) und intuitive Bedienung.
