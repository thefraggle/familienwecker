# Changelog

Das Format basiert auf [Keep a Changelog](https://keepachangelog.com/de/1.0.0/).

*[🇺🇸 English Version](CHANGELOG.en.md)*
 
## 1.4.1 - 2026-03-19

### Neu
- **Onboarding & Design:** Hintergrundgrafik für Onboarding-Screens mit dunklem Scrim für bessere Lesbarkeit hinzugefügt.
- **App-Icon:** Neues App-Icon mit Unterstützung für Adaptive Icons und Legacy-Geräte implementiert.
- **Theme:** Dark Mode ist nun der Standard-Modus beim ersten App-Start.

### Behoben
- **Login & Sicherheit:** Absturz bei ungültigen Anmeldedaten behoben; Passwort-Validierung (min. 8 Zeichen) direkt im Login/Register-Flow hinzugefügt.
- **Deep-Link & Join-Flow:**
  - Doppeltes Popup beim Wechseln von Familien behoben.
  - Verifizierung des Join-Codes erfolgt nun vor dem Verlassen der alten Familie.
  - Verbesserte Fehlermeldungen für nicht existierende Familien (kein irreführender "Verlassen"-Button mehr).
- **Input-Validierung:** Umfassender Audit und Absicherung aller Felder (Familienname, Join-Code, Member-Name) gegen leere oder ungültige Eingaben.
- **Asset-Cleanup:** Entfernung unbenutzter Bildressourcen zur Reduzierung der App-Größe.

---

## 1.4.0 - 2026-03-19

### Neu
- **Cloud-Reset-Logik & Performance:** Neuer stündlicher Cron-Job für Status-Resets (2h Threshold) und effizienterer Daten-Refresh beim App-Start.
- **Onboarding & Design:** Neuer Onboarding-Tour (5 Screens), Panda-Lottie-Animationen und Redesign des RingingScreens.
- **Sicherheits-Audit & Fixes:** Behebung einer XSS-Lücke in Feedback-E-Mails und Verifizierung der IDOR-Sicherheit.

### Behoben
- **Autofill & Login:** Massive Verbesserung der Passort-Manager-Kompatibilität (`AutofillNode` + `Username`-Metadaten). Behebt Blockaden des Kontextmenüs.
- **Stabilität:** Korrektur von Mitglieder-Mapping (Timestamp-Fix), Deep-Link-Flows und Doppel-Alarmen (Notification + Activity).
- **UI/UX:** Wochentag-Chips mit besserer Verteilung, Rot-Markierung bei Fehlern und klickbare Disclaimer/Footer.


---

## [1.3.0] - 2026-03-17

### Neu
- **⭐ App bewerten:** Neuer Button im Hilfe & Feedback Block. Öffnet In-App-Bewertungsfenster (Play In-App Review API); Fallback auf Play Store Seite wenn nicht verfügbar.

### Geändert
- **Wochentag-Chips:** DE und EN nutzen jetzt einheitliche 2-Buchstaben-Kürzel (Mo Di Mi Do Fr Sa So / Mo Tu We Th Fr Sa Su), damit alle 7 Chips in der Breite passen.
- **Inaktive Tage:** Chips für deaktivierte Wochentage werden deutlich ausgeblendet (Text, Rahmen und Hintergrund auf ~30 % Deckkraft).
- **Settings-Footer:** Neue Reihenfolge: Version → Copyright → All rights reserved → Links.
- **Rate-Limits (Cloud Functions):**
  - E-Mail Reset/Verify: max. 5 pro Stunde + max. 10 pro Tag
  - Familie beitreten: max. 5 pro Minute + max. 10 pro Tag
  - Familie erstellen: max. 3 pro Stunde + max. 6 pro Tag

### Behoben
- **Chip-Text unsichtbar:** Selektierter inaktiver Wochentag-Chip: Text war auf ausgefülltem Hintergrund nicht lesbar. Fix: gedämpfter grauer Container statt Primary-Farbe.
- **Crash beim Rate-Limit „Familie erstellen":** `RESOURCE_EXHAUSTED`-Exception der Cloud Function wird jetzt korrekt abgefangen.
- **Rate-Limit Fehlermeldungen komplett:** Alle drei Rate-Limits (Familie erstellen, beitreten, E-Mail) zeigen spezifische Fehlermeldungen. `resendVerificationEmail` wertet Ergebnis jetzt aus.

---

## [1.2.0] - 2026-03-17

### Neu
- **Alarm-Status-Persistenz:** Nach Neuinstall + Login wird der letzte bekannte Alarm-Status (An/Aus) automatisch aus Firestore wiederhergestellt.

### Behoben
- **Join-Flow Doppel-Dialog:** War der Join-Link die eigene Familie, wurde `onLeaveFamily()` aufgerufen → User falsch auf Setup-Screen weitergeleitet. Fix: Same-Family-Guard gibt jetzt `false` zurück.
- **Familie erstellen – Loop + Crash (neuer Account):** `refreshData()` rief `leaveFamily()` auf wenn Firestore nach `createFamily()` kurz `null` zurückgab (Race Condition). Das zerstörte die neu erstellte Familie und ließ die App beim zweiten Versuch crashen. Fix: `leaveFamily()` aus `refreshData()` entfernt – Self-Healing läuft über Members-Flow-Collector.
- **Familie erstellen – Redirect zu Setup:** `LaunchedEffect` in `MainScreen` rief `onLeaveFamily()` auf wenn `familyId` während eines aktiven Syncs kurz als `null` erschien. Fix: Guard `familyId == null && !isSyncing`.
- **Alarm-Restore nach Neuinstall (mehrere Race Conditions):**
  - `myMemberId`-Observer rief `setAlarmEnabled(false)` beim App-Start auf (initial `null`).
  - `initialAlarmPushDone`-Block schrieb `false` nach Firestore vor dem Restore.
  - `isAlarmEnabled`-Observer schrieb nach Logout `false` in Firestore und überschrieb den Member-Status.
  - Alle drei Race Conditions behoben; Reihenfolge: Restore zuerst, dann proaktiver Firestore-Sync.
- **Frühstückszeit falsch berechnet:** War kein Abfahrtszeitpunkt gesetzt, wurde fälschlicherweise 23:59 als Basis genommen → unrealistische Zeiten (z.B. 23:29). Fallback ist jetzt `späteste Weckzeit + Badezimmer-Dauer`.
- **Wochentag-Konfiguration ignoriert:** Tagesspezifische Zeiten aus Tagesprofilen wurden beim Schedule ignoriert. Fix: Effektive Felder werden vor Scheduler-Aufruf korrekt aufgelöst.

### Entfernt
- **„Was ist neu?"-Dialog** vollständig entfernt (Dialog, Logik, JSON-Datei, Strings).

---

## [1.1.5] - 2026-03-17

### Neu
- **Wochentag-Konfiguration:** Weckzeiten und Badezimmer-Dauer können pro Wochentag individuell eingestellt werden.
- **Feedback-Screen:** Dedizierter Feedback-Screen mit Kategorie-Auswahl, Nachricht, optionaler E-Mail und automatisch mitgesendeten Gerätedaten.
- **Firebase Feedback-Versand:** Feedback wird direkt über eine Firebase Cloud Function (Resend) als E-Mail versendet – kein klassischer Mail-Client-Intent mehr. Zusätzlich wird jede Einsendung in Firestore archiviert.
- **Feedback UX:** Formular wird nach dem Absenden geleert; Screen schließt sich automatisch nach 2,5 Sekunden.
- **Settings-Footer:** Versionsnummer, klickbare Rechtlinks (Nutzungsbedingungen, Datenschutz, Impressum) und Copyright jetzt als Footer sichtbar.
- **Account löschen:** Externer Link zu `familienwecker.de/account-deletion.html` (DE) bzw. `/account-deletion-en.html` (EN) statt Info-Dialog.
- **Einstellungen restrukturiert:** Sprache und Erscheinungsbild in einer gemeinsamen Karte zusammengefasst; Hilfe & Feedback in eigener Karte.
- Nutzungsbedingungen (Terms of Use) direkt in den Einstellungen verlinkt.
- Disclaimer im Registrierungs-Screen mit klickbaren Links zu Nutzungsbedingungen und Datenschutz.

### Behoben
- Algorithmus: Frühstücks-Konflikt wird jetzt auch erkannt wenn Bad-Ende = Frühstücksbeginn (0 Min Puffer).
- Algorithmus: Post-Validierung stellt sicher dass kein Frühstücker sein Bad nach Frühstücksbeginn beendet.
- Offline-Anzeige: Falsches „Offline"-Icon nach App-Start behoben (nur noch bei echtem Netzwerkausfall).
- Cloud Functions: Rate-Limit-Zähler wurde beim ersten Aufruf nicht korrekt gespeichert (tx.update → tx.set).
- Cloud Functions: Join-Versuchslimit von 5 auf 10 pro Minute erhöht.
- CI: AAB-Dateiname in manuellen GitHub-Builds war fehlerhaft.
- Beim Verlassen der Familie wird das eigene Mitglieds-Profil nun vollständig aus Firestore gelöscht.
- E-Mail-Versand (Passwort-Reset, Opt-In-Bestätigung) war bei erster Anfrage durch fehlerhaftes Rate-Limit-Dokument blockiert.
- „Familie verlassen" trennte irrtümlich auch andere Geräte (Self-Healing ohne Existenzprüfung).
- Beim Verlassen der Familie blieb der eigene Account-Claim im Firestore-Profil bestehen (Ghost-Claim).
- String-Audit: Veraltete und unbenutzte Strings (Help-Section, `ok_button`, etc.) entfernt; beide Sprachen vollständig synchronisiert.

---

## [1.1.0] - 2026-03-15

### Neu
- Scroll-Indicator (↓) auf dem Hauptscreen solange keine Mitglieder vorhanden.
- **Snooze:** Snooze-Button im Wecker-Screen (5 Min). Im Hauptscreen wird während eines aktiven Snooze ein Banner mit Endzeit und Abbruch-Button angezeigt.

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
- Mitglieder-Limit erst bei 6 statt 5 aktiven Personen.
- Direkter Beitritt anderer User ohne Konflikt-Dialog behoben.
- Familienname-Anzeige in der Kopfzeile nach Neustart korrekt.
- Optimierter Ladevorgang: kein Flackern mehr beim App-Start.
- Vollständige Lokalisierung (DE/EN) aller neuer Strings.

---
