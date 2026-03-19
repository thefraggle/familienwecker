# Changelog

Das Format basiert auf [Keep a Changelog](https://keepachangelog.com/de/1.0.0/).

*[🇺🇸 English Version](CHANGELOG.en.md)*
 
## [1.3.7] - 2026-03-19
 
 ### Neu
- **Cloud-Reset-Logik**: Neuer stündlicher Cron-Job (`scheduledMemberReset`) setzt den "Bin wach"-Status und ungeclaimte Pausen zentral in der Cloud zurück.
- **Verkürzter Reset-Threshold**: Status-Reset erfolgt nun bereits **2 Stunden** (statt 4h) nach der geplanten Weckzeit.
- **Lazy-Refresh**: App aktualisiert Daten und Zeitplan nun effizient beim App-Start oder Zurückkehren in den Vordergrund (`onResume`).
- **Akku-Optimierung**: Der periodische 5-Minuten-Hintergrund-Timer im ViewModel wurde entfernt.
 
### Behoben
- **Mitglieder-Mapping**: Korrektur des Daten-Mappings für `lastUpdatedAt` und `createdAt` (Firestore `Timestamp` -> `Long`). Behebt das Problem, dass neu angelegte Mitglieder nicht in der Liste erschienen.
- **Deep Link Flow**: Bereinigung der Navigations-Race-Condition und doppelter Popups beim Beitritt via Deep Link.
- **Familien löschen**: Firestore-Regeln korrigiert, sodass der Ersteller einer Familie nun alle zugehörigen Mitglieder-Dokumente löschen darf.
- **UI-Feedback**: Snackbar-Feedback im Settings-Screen bei Fehlern (Verlassen/Löschen der Familie).
- **Firestore-Robustheit**: `isFamilyMember`-Regel optimiert (nutzt nun `exists()`).
 
 ## [1.3.6] - 2026-03-19

### Neu
- **Autofill-Support:** Felder für E-Mail und Passwort im Login wurden mit Autofill-Hints versehen, um die Erkennung durch Passwort-Manager (Z.B. Bitwarden, Google) zu verbessern.

### Geändert
- **Firebase-Optimierung (Performance):** 
    - 2s Debouncing für alle UI-Toggles (Awake, Pause, Master-Switch). Verhindert unnötige Schreibvorgänge bei schnellem Klicken.
    - Batch-Updates für tägliche Resets und Core-Löschvorgänge (`leaveFamily` / `deleteFamily`), was die Datenbank-Last reduziert und die Atomarität verbessert.
    - Nutzung von `FieldValue.serverTimestamp()` für konsistente `lastUpdatedAt` Zeitstempel über alle Geräte hinweg.

## [1.3.5] - 2026-03-18

### Behoben
- **Context Menu & Password Manager:** Blockiertes Kontextmenü (Copy/Paste) in Login- und Familien-Setup-Screens repariert durch Optimierung der UI-Hierarchie (Entfernung verschachtelter Scaffolds).
- **Autofill-Support:** Felder für E-Mail und Passwort im Login wurden mit Autofill-Hints versehen, um die Erkennung durch Passwort-Manager (Z.B. Bitwarden, Google) zu verbessern.

## [1.3.4] - 2026-03-18

### Neu
- **RingingScreen Redesign:** Hintergründe mit Gradient (Lila bis Pfirsich), Lottie-Panda Animation und randomisierten Begrüßungen (DE/EN).

### Geändert
- **Admin-Debug:** Wecker-Vorlauf auf 3 Minuten verkürzt.
- **Verbesserung „Ich bin wach":** Button-Logik repariert (sofortige Stornierung), bedingte Sichtbarkeit (nur wenn Wecker an) und verbessertes visuelles Feedback („Du bist wach ✅“).

### Behoben
- **Fehlerbehebung:** Doppelter Alarm-Ton durch parallele Notifications/Activity beseitigt.
- **Reset-Logik:** „Ich bin wach"-Status wird beim Ausschalten des globalen Weckers automatisch zurückgesetzt.

---

## [1.3.3] - 2026-03-18

### Neu
- **Onboarding-Tour:** Beim ersten App-Start erscheinen 5 animierte Intro-Screens (HorizontalPager) mit Benefit-fokussierten Texten und App-Screenshots (lokalisiert DE/EN). Kann über Einstellungen → „App-Tour anzeigen" jederzeit erneut gestartet werden.
- **Panda-Intro-Animation:** Slide 0 des Onboardings zeigt eine loopende Lottie-Animation (schlafender Panda) statt eines statischen Bildes.

### Behoben
- **Long-Press / Einfügen im Login- und Join-Screen:** `SelectionContainer` um editierbare Textfelder entfernt – blockierte das native Kontextmenü (Einfügen, Kopieren). Passwort-Manager und Zwischenablage funktionieren nun zuverlässig.

---

## [1.3.2] - 2026-03-18

### Neu
- **Alle Wochentage deaktivierbar:** Ein Mitglied kann nun alle Wochentage deaktivieren (kein Wecker aktiv) ohne Sperrung des Speichern-Buttons.
- **Nächster aktiver Tag in Member-Kachel:** Wenn nicht alle Tage aktiv sind, zeigt die Mitglieds-Kachel den nächsten aktiven Tag (z.B. „Freitag") und dessen tagespezifische Weckzeiten.
- **Alarm-Datum im Zeitplan:** Liegt der nächste Alarm nicht heute, erscheint in der Zeitplan-Karte ein Subtitle mit Wochentag und Datum (z.B. „Donnerstag, 19. März").
- **Periodischer Refresh:** `recalculateSchedule` wird alle 5 Minuten automatisch aufgerufen – kein Einfrieren der Zeitplan-Anzeige mehr wenn kein Firestore-Update kommt.

### Behoben
- **Wecker klingelt nicht (Hauptfehler):** `AlarmClockInfo` erhielt eine `getBroadcast`-PendingIntent als Show-Intent statt der korrekten `getActivity`-Intent. Auf manchen Android-Versionen verhinderte das, dass der `AlarmReceiver` aufgerufen wurde.
- **`FLAG_UPDATE_CURRENT` + `FLAG_IMMUTABLE` Konflikt:** Ersetzt durch `FLAG_CANCEL_CURRENT` für sauberes Neuerstellen des PendingIntents.
- **Race Condition – Firebase-Sync nach Alarmzeit:** Firestore-Update kurz nach der Weckzeit konnte `recalculateSchedule` → `cancelWakeUp` triggern. Fix: 5-Min-Grace-Period in `applyAlarms`.
- **Stilles Cancel in `recalculateSchedule`:** Alle Alarme wurden ohne Log gecancelt wenn `now > todayProfile.latestWakeUp`. Fix: Grace-Period auch im „alle pausiert"-Branch + W-Level-Logs.
- **Veralteter Zeitplan nach inaktivem Folgetag:** UI zeigte alten Zeitplan wenn `applyAlarms` wegen inaktivem Tag cancelte. Fix: `_schedule` wird auf `NoActiveSchedule` gesetzt.
- **Race Condition – zweite ViewModel-Instanz:** `RingingActivity` erzeugte zweiten `FamilyViewModel` → überschrieb laufenden Alarm. Fix: direkte Nutzung von `PreferencesRepository` + `AlarmScheduler`.
- **`RingingActivity` nicht zuverlässig gestartet:** `AlarmReceiver` startet `RingingActivity` jetzt direkt via `context.startActivity()` (zusätzlich zum Full-Screen-Intent).
- **Member-Kachel zeigt falschen Alarm-Status:** „Alarm active" trotz aller inaktiver Tage. Fix: `allDaysInactive`-Check in der Kachel.
- **Weckzeiten bei inaktivem Profil sichtbar:** Werden jetzt ausgeblendet wenn alle Tage inaktiv sind.
- **Snooze-Slot-Konflikt:** Snooze- und reguläre Alarme nutzen eigene Request-Codes (`_snooze`-Suffix).

---

## [1.3.1] - 2026-03-17

### Geändert
- **Wochentag-Chips:** Alle 7 Chips (`Mo Di Mi Do Fr Sa So`) haben jetzt `weight(1f)` und verteilen sich gleichmäßig auf die volle Breite – Sonntag war auf schmalen Screens abgeschnitten.
- **Chip-Fehler-Markierung:** Chips mit ungültigen Zeiteinstellungen werden rot hervorgehoben (Rahmen + Text + Hintergrund).

### Behoben
- **Next-Alarm-Logik:** `resolveEffectiveMember` prüft jetzt das heutige DayProfile zuerst (ist es aktiv UND vor `latestWakeUp`?), andernfalls morgen. Vorher wurde das veraltete Root-Feld `member.latestWakeUp` als Referenz genutzt → falsches Ergebnis wenn heutiges Profil deaktiviert war.
- **„Kein Wecker" falsch angezeigt:** Wenn der aktuelle Tag deaktiviert und der nächste aktiv ist, wurde trotzdem „kein aktiver Wecker" gezeigt.
- **Alarm klingelt für deaktivierte Tage:** `applyAlarms` wird abgebrochen wenn das DayProfile des Zieldatums `isActive = false` ist.
- **Validierung – Späteste Weckzeit:** Fehlertext erscheint wenn `latestWakeUp ≤ earliestWakeUp`; Speichern-Button gesperrt.
- **Validierung – Abfahrtszeit:** Fehlertext erscheint wenn Abfahrtszeit ≤ `latestWakeUp + Baddauer`. Prüft jetzt auch den angezeigten Default-Wert (08:00), nicht nur explizit gesetzte Werte.

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
