# TODO - FamWake (v1.4.0)

## Nächste Schritte
- [ ] Nächste Features planen

### Recently Completed
- [x] v1.4.0: Consolidated Major Release (since v1.3.0) <!-- id: 103 -->
- [x] v1.3.11: XSS Security Hotfix (escapeHtml in Functions) <!-- id: 102 -->
- [x] v1.3.10: Password Manager Support (AutofillType.Username) & Context Menu Confirmation <!-- id: 100 -->
- [x] v1.3.9: Login Autofill & Context Menu Fix (RELEASED) <!-- id: 101 -->
- [x] Recently Completed (v1.3.9): OutlinedTextField Simplification & AutofillNode (RELEASED)
- [x] Recently Completed (v1.3.7): Finales Release mit Mapping-Fix, Deep-Link-Flow & Firestore Rules (RELEASED)
- [x] Recently Completed (v1.3.7): Cloud Reset (2h), Lazy-Refresh & Background-Optimierung (RELEASED)
- [x] Recently Completed (v1.3.6): Awake Redesign, Theme-Fix, Local Logic & Firebase Perf (RELEASED)
- [x] Autofill-Hints: Für E-Mail und Passwort im Login hinzugefügt
- [x] Refactoring der Awake- & Alarm-Logik (Funktion lokal, Status gesynct)
- [x] 2s Debouncing für Toggles (Awake, Pause, Master-Switch) eingeführt
- [x] Batch-Updates & atomare Core-Prozesse (Reset, Leave, Delete) implementiert
- [x] Wecker-Switch (isAlarmEnabled) rein lokal gemacht
- [x] Awake-Button Design anpassen (Icon + Text "Bereits wach" + grüner Haken)
- [x] Dark Mode "System" Einstellung repariert (MainActivity Fix)
- [x] Version auf 1.3.6 gesetzt
- [x] Context Menu Fix: Verschachtelte Scaffolds entfernt (Login/Setup)

## Recently Completed (v1.3.4)
- [x] RingingScreen Redesign: Lottie-Panda, Gradient & Randomized Greetings
- [x] Admin-Debug: Wecker-Vorlauf auf 3 Minuten verkürzt
- [x] Fehlerbehebung: Doppelter Alarm-Ton beseitigt
- [x] Verbesserung „Ich bin wach": Button-Logik repariert (sofortige Stornierung), bedingte Sichtbarkeit
- [x] „Ich bin wach" Visuals: Farbe/Text toggle („Du bist wach ✅“)
- [x] Reset-Logik: Awake-Status wird bei globalem Switch-Off zurückgesetzt
- [x] Tooltip-System: 5 kontextuelle Erstnutzer-Hinweise (Awake, Drag, Weckfenster, Baddauer, Einladungscode)
- [x] GitHub Push + Tag v1.3.4 + Firebase Deploy Rules/Indexes

## Recently Completed (v1.3.2)
- [x] v1.3.2 Play Store Release (AAB)
- [x] Versionsnummer auf 1.3.3-dev setzen
- [x] Alarm klingelt nicht behoben (AlarmClockInfo Show-Intent-Typ)
- [x] FLAG_CANCEL_CURRENT für AlarmManager PendingIntent
- [x] Race Condition Grace-Period in applyAlarms + recalculateSchedule
- [x] Stilles Cancel-Logging hinzugefügt
- [x] Zweite ViewModel-Instanz in RingingActivity entfernt
- [x] Veralteter Zeitplan-Anzeige nach inaktivem Tag behoben
- [x] Periodischer 5-Min-Refresh-Timer
- [x] Alle Wochentage deaktivierbar
- [x] Member-Kachel: nächster aktiver Tag + richtige Zeiten
- [x] Zeitplan-Karte: Datum wenn Alarm nicht heute
- [x] Member-Kachel: korrekte Alarm-Status-Anzeige + Hide bei allen inaktiv
- [x] GitHub Push + Tag v1.3.2

## Backlog (Roadmap)
- [x] UI-Fixes & Snooze-Banner (Daniel)
- [ ] Snooze-Synchronisation zwischen Geräten
- [ ] Multi-Bad-Unterstützung
- [ ] Homescreen-Widget
- [ ] Individuelle Frühstücksdauer pro Mitglied
- [ ] Haptik-Profile (Voralarm vs. Hauptalarm)
- [ ] Weck-Bestätigung mit Push („Bin wach!")
- [ ] Urlaubs-Datum (Auto-Reaktivierung)
- [ ] Plan-Übersicht als Wochentabelle
- [ ] Abendlicher Check-In (Push 21 Uhr)
- [ ] Eigene Playlists (Spotify)
- [ ] DST-Schutz
