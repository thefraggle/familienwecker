# Changelog

Alle wichtigen Änderungen an diesem Projekt werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog](https://keepachangelog.com/de/1.0.0/),
und dieses Projekt hält sich an [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

*[🇺🇸 English Version](CHANGELOG.en.md)*


## [0.9.5] - 2026-03-12
### Verbesserungen
- **CompositionLocal für Dark Theme:** `isSystemInDarkTheme()` wird einmalig im Theme-Root ausgelesen und via `LocalDarkTheme` bereitgestellt – korrektere Architektur, kein mehrfaches Abfragen des System-States.
- **SyncStatus:** Überblick über Cloud-Synchronisierung überwacht jetzt sowohl das Familien-Dokument als auch die Mitglieder-Kollektion.
- **Passwort-Validierung:** Clientseitige Prüfung (min. 8 Zeichen) verhindert unnötige Firebase-Aufrufe bei zu kurzem Passwort.

## [0.9.2] - 2026-03-12
### Sicherheit & Code-Qualität (Audit-Fixes)
- **Boot-Sicherheit:** Wecker werden nach Geräte-Neustart automatisch neu geplant (`BootReceiver`).
- **Deep Links:** Nur noch HTTPS-Schema erlaubt – kein HTTP mehr in Einladungslinks.
- **Navigations-Typsicherheit:** Navigations-Routen über zentrales `Routes`-Objekt; Tippfehler führen jetzt zum Compile-Fehler statt Laufzeit-Crash.
- **Akku-Warnung:** Einstellungen zeigen Hinweis, wenn Akku-Optimierung aktiv ist (kann Wecker verzögern).
- **SharedPreferences-Listener:** Wird bei ViewModel-Destroy sauber deregistriert – verhindert Memory Leak.
- **Fallback:** `getUserFamily()` nutzt lokal gecachten Join-Code als Fallback bei Firestore-Fehler.
- **Code-Cleanup:** Duplikates Firestore-Mapping in `FamilyMemberMapper` zentralisiert; redundante Imports entfernt.

## [0.9.1] - 2026-03-12
### Neu
- **Alarm-Status Sync:** Der Alarm-Status geclaimter Familienmitglieder wird in der Liste automatisch live synchronisiert. Deaktiviert jemand seinen Wecker, sehen andere das Ändern sofort – ohne App-Neustart. Der eigene globale Alarm-Switch bleibt weiterhin gerätespezifisch.

## [0.9.0] - 2026-03-12
### Zusammenfassung (Consolidation Release)
Bündelt alle kritischen Sicherheitsverbesserungen, Bugfixes und Lokalisierungs-Updates seit 0.8.0.

### Sicherheit & Audit
- **Sicherer Join-Flow:** Familienbeitritt über gesicherte Cloud Function mit serverseitigem Rate-Limiting.
- **Daten-Integrität:** Überarbeitung der Firestore Security Rules; Zugriff strikt auf verifizierte Mitglieder begrenzt.
- **Verschlüsselung:** Lokale Einstellungen auf `EncryptedSharedPreferences` (AES-256) migriert.
- **Privacy:** `joinCode` nicht mehr im Benutzerprofil gespeichert.

### Lokalisierung & UX
- **Fehler-Mapping:** Firebase-Auth-Fehler (z. B. Passwort zu kurz, E-Mail vergeben) vollständig lokalisiert (DE/EN).
- **Bereinigung:** Hardcodierte Texte im Login entfernt.

### Behoben
- **Multi-Device Sync:** Alarm-Switch ist jetzt gerätespezifisch – Deaktivieren auf einem Gerät beeinflusst keine anderen Mitglieder.
- **Stabilität & Offline:** UI-Freezes, Race-Conditions im Join-Dialog und negative `PendingIntent` Request-Codes behoben.

## [0.8.x] - 2026-03-12
### Highlights
- **Offline-Erkennung:** Präzisere Prüfung via `NET_CAPABILITY_VALIDATED` (verhindert Falsch-Positiv bei Captive Portals).
- **Stabilität:** Scheduler-Guard gegen Mitternachts-Overflows; verbessertes Error-Logging; robusterer Löschvorgang für Familien.
- **Offline-Robustheit:** App-Start dauert auch ohne Netz max. 2 Sekunden. Endlose Lade-Spinner im Join-Flow behoben.
- **Deep Links:** `SingleTask` Intent-Handling gefixt; Einladungslinks funktionieren auch bei laufender App.
- **Release:** R8-Minifizierung und NDK-Debug-Symbole (`FULL`) fest integriert.


## [0.7.x] - Zusammengefasst (März 2026)
### Neu & Optimiert
- **Performance & Architektur:** Einführung von `ImmutableList` für effizienteres Compose-Rendering und verbesserte Dependency Injection für Repositories.
- **Sicherheit:** Verwendung von `SecureRandom` für die Generierung von Familiencodes.
- **Design:** Unterstützung für Material You (Dynamic Colors) ab Android 12 und echter AMOLED Black Mode (`#000000`) für Akkuschonung und bessere Kontraste.
- **Barrierefreiheit (A11y):** Vergrößerte Touch-Targets in den Einstellungen für bessere Nutzbarkeit.

### Behoben
- **Lokalisierung (I18n):** Alle ehemals hardcodierten Fehler und UI-Elemente in E-Mail/Join-Flows und Settings sind nun vollständig (DE/EN) übersetzt. Alarme respektieren die eingestellte System-Sprache.
- **Deep Links:** Umfangreiche Fixes für Konflikte, Validierung und Endlosschleifen beim Beitreten über Einladungslinks.
- **Stabilität:** Race-Conditions beim Profil-Freigeben (Atomic Transaction) und Familien-Löschen (WriteBatch) behoben. `leaveFamily()` bricht nun zuverlässig den eigenen Wecker ab.

## [0.6.x] - Zusammengefasst (März 2026)
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

## [0.5.x] - Zusammengefasst (März 2026)
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
