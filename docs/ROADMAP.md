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
- [ ] **"Bin schon wach" Button (#2):** Wecker für heute deaktivieren, ohne den Plan der anderen zu stören.
- [ ] **"Was ist neu?" Dialog (#3):** Übersichtliche Highlights nach einem App-Update anzeigen.
- [ ] **Wochentag-Konfiguration:** Weckzeiten für Werktage und Wochenende getrennt einstellen – z. B. Kinder schlafen samstags länger.
- [ ] **Mitglied für heute pausieren:** Ein nicht-geclaimed Mitglied (oder das eigene Profil) kann für heute aus dem Weck-Plan genommen werden (z. B. krank, auswärts). Zurücksetzen automatisch um Mitternacht. Geclaimte Fremd-Profile kann niemand anderes pausieren. Pause-Icon neben Edit/Delete in der Mitglied-Card ergänzen. Hinweis: Der Master-Switch auf der Hauptseite pausiert bereits den eigenen geclaimten User; nicht-geclaimte Mitglieder fließen immer in die Berechnung ein, werden aber nicht geweckt.
- [ ] **Basisfunktion Snooze:** Implementierung der Schlummerfunktion (z. B. 5/10 Min. Verzögerung) im Weckscreen.

### Prio: Mittel (Usability & UI)
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
- [ ] **Urlaubs-Datum statt manuellem Schalter:** Urlaub bis Datum X eintragen; Wecker schaltet sich danach automatisch wieder ein.
- [ ] **Plan-Übersicht als Wochentabelle:** Kompakte Tabelle aller Mitglieder × Wochentage in einer Übersicht.

### Prio: Niedrig (Nice-to-have)
- [ ] **Abendlicher Check-In:** Push-Reminder um 21 Uhr, um die Zeiten für morgen zu bestätigen.
- [ ] **Eigene Playlists:** Spotify-Integration für den Weck-Screen.
- [ ] **Badezimmer-Dauer nach Wochentag:** Freitag braucht Papa länger, Montag die Kinder mehr Zeit – pro Wochentag konfigurierbar.

### Prio: Technische Verbesserungen (Robustheit)
- [ ] **UI-Feedback bei Firestore-Fehler:** `addOrUpdateMember()` ist aktuell fire-and-forget – bei einem Schreib-Fehler bekommt der User nichts mit. Snackbar oder Retry-Dialog einbauen.
- [ ] **Scheduler-Limit absichern:** Maximale Mitgliederzahl für den Permutations-Algorithmus auf z. B. 7 begrenzen und oberhalb davon einen Greedy-Fallback einsetzen (aktuell sind n! Permutationen theoretisch unbegrenzt, auch wenn der Scheduler nun asynchron läuft).
- [ ] **Klingelton-Vorschau in den Einstellungen:** Kurzes Abspielen des gewählten Tons nach der Auswahl direkt im Settings-Screen, damit der User weiß, was er gewählt hat.
- [ ] **Scheduler: Shift-Richtung im Plan-Text ausweisen:** Der Fallback-Text "Zeiten wurden um X Minuten angepasst" zeigt nicht, bei wem und in welche Richtung abgewichen wurde. Idealerweise pro Mitglied anzeigen ob früher oder später.
- [ ] **Scheduler: `LocalTime.MAX`-Sentinel absichern:** Interner Startwert `currentLatestBathroomEndTime = LocalTime.MAX` kann bei sehr frühen Abfahrtszeiten mathematisch einmal durch Mitternacht wrappen. Absicherung durch expliziten Frühst-Zeitwert (z. B. 04:00) als praktische Untergrenze.
- [ ] **Zeitumstellungs-Schutz (DST):** Absicherung der Berechnung bei Wechsel Sommer-/Winterzeit (EC-05).
- [ ] **Alarm-Watchdog:** Maximale Belastbarkeit des Hintergrunddienstes gegen System-Kills/Abstürze (EC-06).
- [ ] **Deep Offline Resilience:** Explizite lokale Datenbank als Primary-Source bei fehlendem Internet (EC-04).
- [ ] **Multi-Admin Konfliktlösung:** Strategie für gleichzeitige Änderungen an denselben Mitgliedern (EC-10).

---

## ✅ Erledigt
- [x] Scheduler-Diagnose: Präzise Fehlermeldungen bei Konflikten (V 0.3.5)
- [x] Akku-Optimierung: System-Check & Warnhinweis (V 0.3.5)
- [x] Mitglied-Reihenfolge: stabile Sortierung nach Anlege-Zeitstempel (V 0.3.4)
- [x] Member anlegen nach Familien-Erstellung fehlgeschlagen (saveUserFamily-Race) (V 0.3.4)
- [x] Phantom-Alarm bei Logout/LeaveFamily/DeleteFamily (V 0.3.4)
- [x] isPaused/Claim beim Bearbeiten eines Mitglieds verloren gegangen (V 0.3.4)
- [x] Badezimmer-Dauer-Validierung (1–120 Min.) (V 0.3.4)
- [x] Fehlermeldungs-Flash beim Anlegen einer Familie behoben (V 0.3.4)
- [x] Alarm-Ton: Ausgewählter Klingelton wird korrekt gespielt (Notification-Channel V2, USAGE_ALARM-Attribute) (V 0.3.3)
- [x] Abfahrtszeit-Validierung: Fehler wenn Zeit vor Weckzeit liegt (V 0.3.3)
- [x] Scheduler auf Background-Thread ausgelagert (V 0.3.3)
- [x] Modernes App-Icon (V 0.3.2)
- [x] Optimierter Dark Mode & Kontraste (V 0.3.2)
- [x] Erweiterte Lösch-Logik & Firestore Security Rules (V 0.3.2)
- [x] Profil-Besitz, automatische Wiederherstellung & Sicherheit (#5) (V 0.3.1)
- [x] Robuster Lösch-Schutz & Navigations-Sync (V 0.3.1)
- [x] Farblich visualisierte Mitglieder-Stati (V 0.3.1)
- [x] Anzeige des Familiennamens in den Einstellungen (#4) (V 0.3.0)
- [x] Smarter Algorithmus für Bad-Konflikte (V 0.2.5)
- [x] Flexibles Frühstück (V 0.2.6)
- [x] Cloud-Sync & Urlaubs-Modus (V 0.2.5)
- [x] Mehrsprachigkeit (DE/EN) (V 0.2.5)

---
*Vorschläge können jederzeit ergänzt werden! Einfach dieses File bearbeiten.*
