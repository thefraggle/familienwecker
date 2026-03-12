# Changelog

Alle wichtigen Änderungen an diesem Projekt werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog](https://keepachangelog.com/de/1.0.0/),
und dieses Projekt hält sich an [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

*[🇺🇸 English Version](CHANGELOG.en.md)*


## [1.0.0] - 2026-03-12
### 🎉 Erster stabiler Release
Die erste vollständige, produktionsreife Version von FamWake.

### Sicherheit
- **HTTP-Links abgewiesen:** Einladungslinks werden nur noch über HTTPS akzeptiert.
- **Admin-only Löschen:** Familie kann nur noch vom Ersteller gelöscht werden. Andere Mitglieder erhalten eine klare Fehlermeldung.
- **Offline-Claim-Sperre:** Profil-Auswahl ist offline deaktiviert – verhindert irreführende Fehlermeldungen.

### UX & Bugfixes
- **Deep-Link Sofort-Dialog:** Join-Link öffnet jetzt direkt den Konflikt-Dialog, auch wenn die App bereits im Hintergrund läuft.
- **Familie löschen mit geclaimten Membern:** Familien können nun auch dann gelöscht werden, wenn andere User aktive Profile haben.
- **Offline-Icon:** Wird nun auch bei ausstehenden Schreiboperationen korrekt angezeigt (statt endlosem Sync-Spinner).
- **WhatsNew Button-Text:** „Was ist neu"-Dialog verwendet den konfigurierbaren Button-Text aus der JSON-Konfiguration.

---

## [0.9.x] - 2026-03-12
### Zusammenfassung (Pre-Release Stabilisierung)
Bündelt alle Verbesserungen, Sicherheits-Fixes und Code-Qualitäts-Arbeiten seit 0.9.0.

### Sicherheit & Code-Qualität (Security-Audit)
- **Cloud Function `createFamily` (H-5):** Familie-Erstellung läuft vollständig serverseitig – kein direkter Client-Schreibzugriff möglich (`allow create: if false`).
- **App-Singleton durchgängig (H-1/H-2):** `RingingActivity` und `BootReceiver` nutzen den `FamWakeApplication`-Singleton.
- **E-Mail Rate-Limiting (H-3):** Alle Cloud Functions für E-Mail-Versand haben ein serverseitiges Rate-Limit (max. 3/Std. pro Adresse).
- **Kryptografisch sicherer PRNG (M-1):** Join-Code-Generierung nutzt `crypto.randomInt()` statt `Math.random()`.
- **Deprecated Window-Flags entfernt (M-2):** `FLAG_SHOW_WHEN_LOCKED` / `FLAG_TURN_SCREEN_ON` aus `RingingActivity` entfernt.
- **Debug-Guards:** Alle `Log.e()`-Aufrufe mit `BuildConfig.DEBUG`-Guard abgesichert.
- **Boot-Sicherheit:** Wecker werden nach Neustart automatisch neu geplant (`BootReceiver`).
- **Navigations-Typsicherheit:** Zentrales `Routes`-Objekt verhindert Tippfehler-Crashes.
- **Passwort-Validierung:** Min. 8 Zeichen clientseitig geprüft.

### Neue Features
- **Alarm-Status Sync:** Der Alarm-Status geclaimter Mitglieder wird live synchronisiert – ohne App-Neustart.
- **CompositionLocal Dark Theme:** `isSystemInDarkTheme()` einmalig am Root ausgelesen (`LocalDarkTheme`).
- **Akku-Warnung:** Settings zeigen Hinweis wenn Akku-Optimierung aktiv ist.
- **Admin-Erkennung:** `createdByUserId` wird geladen, `isAdmin`-Property im ViewModel exponiert.

---

## [0.9.0] - 2026-03-12
### Zusammenfassung (Consolidation Release)
Bündelt alle kritischen Sicherheitsverbesserungen, Bugfixes und Lokalisierungs-Updates seit 0.8.0.

### Sicherheit & Audit
- **Sicherer Join-Flow:** Familienbeitritt über gesicherte Cloud Function mit serverseitigem Rate-Limiting.
- **Daten-Integrität:** Überarbeitung der Firestore Security Rules.
- **Verschlüsselung:** Lokale Einstellungen auf `EncryptedSharedPreferences` (AES-256) migriert.
- **Privacy:** `joinCode` nicht mehr im Benutzerprofil gespeichert.

### Lokalisierung & UX
- **Fehler-Mapping:** Firebase-Auth-Fehler vollständig lokalisiert (DE/EN).

### Behoben
- **Multi-Device Sync:** Alarm-Switch ist jetzt gerätespezifisch.
- **Stabilität & Offline:** UI-Freezes und Race-Conditions behoben.

---

## [0.8.x] - 2026-03-12
### Highlights
- **Offline-Erkennung:** Präzisere Prüfung via `NET_CAPABILITY_VALIDATED`.
- **Stabilität:** Scheduler-Guard gegen Mitternachts-Overflows; robusterer Löschvorgang für Familien.
- **Offline-Robustheit:** App-Start max. 2 Sekunden ohne Netz. Endlose Spinner behoben.
- **Deep Links:** `SingleTask` Intent-Handling gefixt; Einladungslinks funktionieren auch bei laufender App.

## [0.7.x] - Zusammengefasst (März 2026)
### Neu & Optimiert
- **Performance & Architektur:** `ImmutableList` für effizienteres Compose-Rendering.
- **Design:** Material You (Dynamic Colors) ab Android 12 und AMOLED Black Mode (`#000000`).
- **Barrierefreiheit (A11y):** Vergrößerte Touch-Targets in den Einstellungen.

### Behoben
- **Lokalisierung (I18n):** Alle hardcodierten Fehler und UI-Elemente vollständig (DE/EN) übersetzt.
- **Deep Links:** Fixes für Konflikte, Validierung und Endlosschleifen.
- **Stabilität:** Race-Conditions beim Profil-Freigeben und Familien-Löschen behoben.

## [0.6.x] - Zusammengefasst (März 2026)
### Hinzugefügt
- **Drag & Drop Reordering:** Mitglieder per Drag & Drop sortieren mit Spring-Animationen.
- **Offline-UI & Sync:** Indikatoren in der Top-Bar für Offline-Modus und Cloud-Sync.
- **Deep Linking:** Volle Unterstützung für `familienwecker.de/join/[CODE]`.

### Behoben
- Crash bei erstem Mitglied, Race-Conditions beim Deep Link Join, fehlerhafte Profil-Claims.

## [0.5.x] - Zusammengefasst (März 2026)
### Veröffentlichung im Play Store (Update)
- **Design 2.0:** OLED Dark Mode, Glasmorphismus, verbesserte Typografie.
- **Was ist neu Popup:** Intelligente News-Box nach Updates.
- **Support-Links:** Datenschutz, Impressum und E-Mail-Hilfe direkt aus der App.

## [0.4.x] - Zusammengefasst (März 2026)
- **Design & UX:** Glasmorphismus, Lottie-Animationen, AMOLED Dark Mode.
- **Stabilität:** Automatisches Löschen verwaister Familien (180 Tage), Limit 6 Mitglieder.
- **UX:** Verwechslungsfreie Einladungscodes, Auto-Reset pausierter Profile.

## [0.3.x] - Zusammengefasst (Februar 2026)
- **Profil-System:** Einführung des „Beanspruchens" von Profilen.
- **Wecker-Präzision:** Neues Alarmsystem (Android 14 Support, Fullscreen-Weckscreen).
- **Sicherheits-Update:** Striktes Rechte-Management und sichere Cloud-Speicherung.

## [0.2.5] - 2026-02-24
Erster öffentlicher Release.
- Fokus auf Weck-Algorithmus, Mehrsprachigkeit (DE/EN) und intuitive Bedienung.
