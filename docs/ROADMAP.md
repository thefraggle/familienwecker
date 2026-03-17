# 🗺️ FamWake Roadmap

*[🇬🇧 English version](ROADMAP.en.md)*

---
## 🎯 Vision
Morgenroutine ohne Stress – smarte, dynamische Planung für die ganze Familie.

---

## ✅ In 1.1.5 umgesetzt
- **Feedback-Screen:** Dedizierter Feedback-Screen mit Kategorie, Nachricht, optionaler E-Mail und Ger\u00e4teinfos.
- **Firebase Feedback:** Versand via Cloud Function (Resend); Archivierung in Firestore.
- **Settings restrukturiert:** Sprache & Erscheinungsbild in einer Karte; Hilfe & Feedback in eigener Karte.
- **Settings-Footer:** Versionsnummer, Rechtlinks (Nutzungsbedingungen, Datenschutz, Impressum) und Copyright.
- **Account l\u00f6schen:** Externer Link statt Info-Dialog.
- **Bug-Fixes 1.1.1\u20131.1.4:** Rate-Limit-Fixes, Algorithmus-Korrekturen, Ghost-Claim-Fix, Member-Deletion beim Verlassen.
- **String-Audit:** Veraltete Strings entfernt, DE/EN vollst\u00e4ndig synchronisiert.

---

## ✅ In 1.0.0 umgesetzt
- **Smarter Familien-Weckplan:** Automatische Berechnung, wer wann geweckt wird.
- **Familien-Einladungen per Link:** Deep-Link Join mit Conflict-Dialog.
- **Multi-Device Sync:** Echtzeit-Synchronisierung aller Änderungen.
- **Profil-Claiming:** Jedes Familienmitglied kann sein Profil beanspruchen.
- **Offline-Handling:** Offline-Icon, Offline-Guards für Claim und Join.
- **Admin-Schutz:** Familie löschen nur durch Ersteller möglich.
- **Code-/Security-Audit:** HTTP-Guard, Rate-Limiting, verschlüsselte Prefs.

---

## 🛠️ Backlog / Offene Ideen

### Prio: Hoch (Integration & Kern-Features)
- [ ] **Wochentag-Konfiguration:** Weckzeiten für Werktage und Wochenende getrennt einstellen.

### Prio: Mittel (Usability & UI)
- [ ] **Snooze-Sync:** Plan der anderen passt sich „live" an, wenn jemand länger braucht.
- [ ] **Unterstützung für 2 Badezimmer:** Parallele Slot-Berechnung.
- [ ] **Individuelle Frühstücksdauer:** Jedes Mitglied kann eigene Zeiten setzen.
- [ ] **Haptik-Profile:** Unterschiedliche Vibrationsmuster für Voralarm und Hauptalarm.
- [ ] **Smarte Zeit-Warnungen:** Hinweis bei zu knappen Zeitfenstern.
- [ ] **Eingabe-Validierung:** Warnung bei unrealistischen Werten (z. B. 2h Bad-Dauer).
- [ ] **Homescreen-Widget:** Eigene heutige Weckzeit ohne App öffnen.
- [ ] **Weck-Bestätigung mit Familien-Push:** „Bin wach!"-Button schickt Push an alle.
- [ ] **Urlaubs-Datum:** Wecker reaktiviert sich automatisch nach Ablauf.
- [ ] **Plan-Übersicht als Wochentabelle:** Kompakte Tabelle aller Mitglieder × Wochentage.

### Prio: Niedrig (Nice-to-have)
- [ ] **Abendlicher Check-In:** Push-Reminder um 21 Uhr für die Zeiten von morgen.
- [ ] **Eigene Playlists:** Spotify-Integration für den Weck-Screen.
- [ ] **Badezimmer-Dauer nach Wochentag:** Pro Wochentag konfigurierbar.
- [ ] **Zeitumstellungs-Schutz (DST):** Absicherung der Berechnung bei Sommer-/Winterzeit.
- [ ] **Alarm-Watchdog:** Hintergrunddienst gegen System-Kills absichern.

---
