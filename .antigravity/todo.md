# ToDo – FamWake

## Nächste Schritte

- [ ] Nächste Features planen
- [x] Recently Completed (v1.3.9): OutlinedTextField Simplification & AutofillNode (v1.3.9 local)
- [x] Recently Completed (v1.3.8): Login Screen Autofill & Context Menu Fix (RELEASED)
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
- [x] Autofill-Hints: Für E-Mail und Passwort im Login hinzugefügt

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

## Recently Completed
- [x] Play Store Listing: Restored and optimized (DE/EN) with HTML tags and user-focused messaging.
- [x] v1.3.1 Release: Chip-Layout, Validierungen, Next-Alarm-Logik, Alarm für deaktivierte Tage
- [x] v1.3.0 Release: In-App Review, Chip-Fixes, Rate-Limits (Dual), Fehlerbehandlung
- [x] Rate-Limit Tageslimit (2× stündliches Limit) für Email, Join, Create
- [x] Rate-Limit Fehlermeldungen vollständig (createFamily, joinFamily, resendVerificationEmail)
- [x] Crash RESOURCE_EXHAUSTED bei createFamily behoben
- [x] GitHub Actions: APK immer bei Tag, AAB nur manuell
- [x] Wochentag-Chips 2-Buchstaben DE+EN
- [x] Roadmap bereinigt (Smarte Zeit-Warnungen, Eingabe-Validierung, Wochentag-Bad, Alarm-Watchdog entfernt)
- [x] Tags v1.2.1–v1.2.7 gelöscht, konsolidiert in v1.3.0
- [x] v1.2.0 Release: Alarm-Status-Persistenz, Join-Flow-Fixes
- [x] v1.1.5 Release: Feedback via Firebase, Settings-Footer, String-Audit
