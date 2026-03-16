# Changelog

Das Format basiert auf [Keep a Changelog](https://keepachangelog.com/de/1.0.0/).

*[🇺🇸 English Version](CHANGELOG.en.md)*


## [1.1.1] - 2026-03-16

### Behoben
- E-Mail-Versand (Passwort-Reset, Opt-In-Bestätigung) war nicht möglich: Rate-Limit-Dokument wurde bei der ersten Anfrage nicht korrekt angelegt.
- Fehlermeldung beim Passwort-Reset: Fehlercode wurde als Zahl ausgewertet, `includes()` schlug fehl.
- „Familie verlassen" trennte irrtümlich auch andere Geräte (Self-Healing ohne Existenzprüfung).
- Beim Verlassen der Familie blieb der eigene Account-Claim im Firestore-Profil bestehen (Ghost-Claim).

---

## [1.1.0] - 2026-03-15

### Neu
- Scroll-Indicator (↓) auf dem Hauptscreen solange keine Mitglieder vorhanden.

### Behoben
- Wecker klingelt nach Geräteneustart (auch vor PIN-Eingabe).
- Wecker-Screen auf Sperrbildschirm (Samsung, Xiaomi u.a.).
- Google OAuth in selbst signierten APKs.
- Firebase Cleanup-Job löschte irrtümlich neue Familien.
- Akku-Kachel verschwindet sofort nach Bestätigung.

---

## [1.0.0] - 2026-03-12

### Sicherheit
- HTTP-Einladungslinks werden abgewiesen – nur HTTPS erlaubt.
- Familie löschen nur durch den Ersteller; andere erhalten eine Fehlermeldung.
- Profil-Auswahl offline gesperrt – verhindert irreführende Timeout-Fehler.

### Behoben
- Join-Link öffnet sofort den Konflikt-Dialog, auch wenn die App im Hintergrund läuft.
- Familie löschen klappt nun auch wenn andere User aktive Profile haben.
- Offline-Icon erscheint korrekt, auch wenn Schreiboperationen ausstehen.
- „Was ist neu"-Dialog zeigt konfigurierbaren Button-Text statt hardkodiertem „OK".

---

## [0.9.x] - 2026-03-12

### Sicherheit
- `createFamily` vollständig serverseitig; kein direkter Client-Schreibzugriff möglich.
- E-Mail-Versand via Cloud Functions: Rate-Limit 3/Stunde pro Adresse.
- Join-Code-Generierung: `crypto.randomInt()` statt `Math.random()`.
- Deprecated `FLAG_SHOW_WHEN_LOCKED`/`FLAG_TURN_SCREEN_ON` entfernt.
- Alle `Log.e()`-Aufrufe mit `BuildConfig.DEBUG` abgesichert.
- Wecker werden nach Gerät-Neustart automatisch neu geplant.

### Neu
- Alarm-Status geclaimter Mitglieder wird live synchronisiert.
- Admin-Erkennung: `createdByUserId` aus Firestore, `isAdmin` im ViewModel.
- Akku-Warnung in den Einstellungen wenn Optimierung aktiv ist.
- Passwort-Validierung: min. 8 Zeichen clientseitig.
- Typsichere Navigation via zentralem `Routes`-Objekt.

---

## [0.9.0] - 2026-03-12

### Sicherheit
- Familienbeitritt über Cloud Function mit serverseitigem Rate-Limiting.
- Firestore Security Rules überarbeitet; Zugriff auf verifizierte Mitglieder begrenzt.
- Lokale Einstellungen auf `EncryptedSharedPreferences` (AES-256) migriert.
- `joinCode` nicht mehr im Benutzerprofil gespeichert.

### Behoben
- Alarm-Switch gerätespezifisch – kein ungewollter Sync auf andere Geräte.
- UI-Freezes und Race-Conditions im Join-Dialog behoben.

---

## [0.8.x] - 2026-03-12
- Offline-Erkennung via `NET_CAPABILITY_VALIDATED` (kein Falsch-Positiv bei Captive Portals).
- App-Start ohne Netz: max. 2 Sekunden bis zum Dashboard.
- Scheduler-Guard gegen Mitternachts-Overflows.
- `SingleTask` Intent-Handling für Deep Links gefixt.

## [0.7.x] - März 2026
- `ImmutableList` für effizienteres Compose-Rendering.
- Material You (Dynamic Colors) ab Android 12; AMOLED Black Mode (`#000000`).
- Race-Conditions beim Profil-Freigeben und Familien-Löschen behoben.
- Vollständige DE/EN-Lokalisierung für alle Auth-Fehler und UI-Texte.

## [0.6.x] - März 2026
- Drag & Drop Sortierung der Familienmitglieder mit Spring-Animationen.
- Offline-Indikator und Sync-Icon in der Top-Bar.
- Deep Linking: `familienwecker.de/join/[CODE]` vollständig unterstützt.

## [0.5.x] - März 2026
- Design 2.0: OLED Dark Mode, Glasmorphismus, verbesserte Typografie.
- „Was ist neu"-Popup nach Updates.
- Datenschutz, Impressum und Support-Mail direkt aus der App.

## [0.4.x] - März 2026
- Glasmorphismus, Lottie-Animationen für Leerzustände, AMOLED Dark Mode.
- Automatisches Löschen inaktiver Familien (180 Tage), Limit: 6 Mitglieder.
- Verwechslungsfreie Einladungscodes; Auto-Reset pausierter Profile.

## [0.3.x] - Februar 2026
- Profil-Claiming zum Schutz eigener Weckzeiten.
- Neues Alarmsystem (Android 14, Fullscreen-Weckscreen).
- Firestore Rechte-Management und Validierung von Badezimmerzeiten.

## [0.2.5] - 2026-02-24
Erster öffentlicher Release. Weck-Algorithmus, DE/EN-Support, intuitive Bedienung.
