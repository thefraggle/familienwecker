# 🗺️ FamWake Roadmap & Ideen-Backlog

Dieses Dokument dient als zentraler Ort für alles, was wir an FamWake verbessern oder neu bauen wollen. 

*[🇬🇧 English version](ROADMAP.en.md)*

---
## 🎯 Vision
Morgenroutine ohne Stress – durch intelligente, dynamische Planung für die ganze Familie.

---

## 🛠️ Backlog / Offene Ideen

### Prio: Hoch (Integration & Kern-Features)
- [ ] **Individueller Klingelton (#1):** Auswahl eines eigenen Wecktons aus den lokalen Handy-Sounds.
- [ ] **"Was ist neu?" Dialog (#3):** Übersichtliche Highlights nach einem App-Update anzeigen.
- [ ] **Wochentag-Konfiguration:** Weckzeiten für Werktage und Wochenende getrennt einstellen – z. B. Kinder schlafen samstags länger.

### Prio: Mittel (Usability & UI)
### ✔️ Abgeschlossen (v0.3.10)
 - [x] **Passwort vergessen:** Passwort-Reset-Mail via Firebase Auth.
 - [x] **Fehler-Transparenz:** Systemfehler (z. B. "Permission Denied") werden direkt auf dem Dashboard angezeigt.
 - [x] **Master Switch Sync:** Synchronisierung des Hauptschalters über alle Geräte.
 - [x] **Startup Force-Sync:** Automatisches Neuladen der Cloud-Daten beim App-Start.
 - [x] **NPE Fix:** Behebung eines kritischen Start-Absturzes im ViewModel.
- [ ] **Unterstützung für 2 Badezimmer:** Parallele Slot-Berechnung.
- [ ] **Snooze-Synchronisation:** Wenn einer länger braucht, passt sich der Plan der anderen "live" an.
- [ ] **Individuelle Frühstücksdauer:** Jedes Mitglied kann eigene Zeiten setzen (z. B. Kinder frühstücken 30 Min., Papa kommt nur für 10 Min. dazu).
- [ ] **Echtzeit-Feedback:** Visuelle Bestätigung/Animation, wenn der Plan im Hintergrund neu berechnet wurde (Testplan UI).
- [ ] **Haptik-Profile:** Unterschiedliche Vibrationsmuster für Voralarm und Hauptalarm (Testplan UX).
- [ ] **Smarte Zeit-Warnungen:** Hinweis bei zu knappen Zeitfenstern (z. B. Wecken vs. Haus verlassen) (EC-03).
- [ ] **Smarte Konflikt-Vorschläge:** UI-Vorschläge zur Lösung von Bad-Engpässen (z. B. "Frühstück um 5 Min. kürzen?") (EC-01).
- [ ] **Eingabe-Validierung (Extreme):** Warnung bei unrealistischen Werten (z. B. 2h Bad-Dauer) (EC-02).
- [ ] **Homescreen-Widget:** Kleines 2×1 Widget zeigt die eigene heutige Weckzeit – ohne App öffnen.
- [ ] **Weck-Bestätigung mit Familien-Push:** "Bin wach!"-Button auf dem Weckscreen schickt eine kurze Push-Meldung an alle anderen Familienmitglieder.
- [x] **Automatischer Reset nach Weckzeit:** Status "Bin wach" und "Pause" werden nach dem täglichen Weck-Event automatisch für morgen resettet (v0.3.9).
- [ ] **Urlaubs-Datum statt manuellem Schalter:** Urlaub bis Datum X eintragen; Wecker schaltet sich danach automatisch wieder ein.
- [ ] **Plan-Übersicht als Wochentabelle:** Kompakte Tabelle aller Mitglieder × Wochentage in einer Übersicht.

### Prio: Niedrig (Nice-to-have)
- [ ] **Abendlicher Check-In:** Push-Reminder um 21 Uhr, um die Zeiten für morgen zu bestätigen.
- [ ] **Eigene Playlists:** Spotify-Integration für den Weck-Screen.
- [ ] **Badezimmer-Dauer nach Wochentag:** Freitag braucht Papa länger, Montag die Kinder mehr Zeit – pro Wochentag konfigurierbar.

- [ ] **Zeitumstellungs-Schutz (DST):** Absicherung der Berechnung bei Wechsel Sommer-/Winterzeit (EC-05).
- [ ] **Alarm-Watchdog:** Maximale Belastbarkeit des Hintergrunddienstes gegen System-Kills/Abstürze (EC-06).
- [ ] **Deep Offline Resilience:** Explizite lokale Datenbank als Primary-Source bei fehlendem Internet (EC-04).
- [ ] **Multi-Admin Konfliktlösung:** Strategie für gleichzeitige Änderungen an denselben Mitgliedern (EC-10).

---
*Vorschläge können jederzeit ergänzt werden! Einfach dieses File bearbeiten.*
