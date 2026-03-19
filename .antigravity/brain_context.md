# Brain Context - FamWake (v1.3.10-dev)

## Current State
- **Current Version:** 1.3.10
- **Recent Release (v1.3.9):** 2026-03-19 (Manual Autofill & Context Menu Fix)
- **Planned for v1.3.10:** Hotfix for Password Manager (AutofillType.Username) and confirmation of Context Menu fix.
- v1.3.8 (2026-03-19) (RELEASED) — Login Autofill & Context Menu Fix.
- v1.3.7 (2026-03-19) (RELEASED) — Cloud Reset, Lazy Refresh, mapping/deep-link/rules fixes.
- **Awake-Button:** Design angepasst (Sonnen-Icon, Text "Bereits wach", grüner Haken bei Aktivierung).
- **Local-First Logik:** Wecker-Switch (`isAlarmEnabled`) und die funktionale Wirkung von "Bereits wach" sind rein lokal pro Gerät.
- **Icon-Sync:** Der "Bereits wach"-Status wird weiterhin an Firestore gesendet (Sonnen-Icon für andere).
- **Dark Mode:** Fix der System-Theme-Erkennung in `MainActivity`.
- **Context Menu Fix:** Verschachtelte `Scaffold`-Instanzen in Login/Setup-Screens entfernt, um die native Android-Event-Propagation für das Selektionsmenü (Copy/Paste) zu gewährleisten.
- **Autofill Hints:** E-Mail und Passwort Felder im Login-Screen unterstützen nun native Autofill-Identifier für Passwort-Manager.
- **RingingScreen:** Redesign mit Lottie-Panda, Gradient (Lila/Peachy) und randomisierten Begrüßungen.
- **Alarm-Sound:** System-Sound in Notification entfernt, um Dopplung mit RingingActivity-MediaPlayer zu vermeiden.
- **Awake-Button:** Logik fixiert (cancelt Systemwecker sofort), visuelles Feedback (Farbe/Text toggle), bedingte Sichtbarkeit.
- **Tooltips:** 5 kontextuelle Erstnutzer-Hinweise. System basiert auf granularen PreferencesRepository-Keys.
- **Optimierung (v1.3.7 - RELEASED):**
    - Cloud-Reset via `scheduledMemberReset` (stündlich, 2h Threshold nach Weckzeit).
    - Lazy-Refresh in `MainActivity.onResume` für sofortige UI-Aktualisierung.
    - **Mitglieder-Mapping Fix:** Robuste Konvertierung von Firestore `Timestamp` zu `Long` im `FamilyMemberMapper`. (Behebt leere Listen nach Neuanlage).
    - **Firestore Rules:** `isFamilyMember` nutzt nun `exists()`. Ersteller einer Familie darf Mitglieder-Dokumente löschen.
    - **Settings UI:** Snackbar-Feedback bei Fehlern beim Verlassen/Löschen einer Familie.
    - **Deep-Link-Fix:** Redundante State-Löschung in `MainActivity` bei Familien-Wechsel entfernt. Auto-Join Logik im `FamilySetupScreen` hinzugefügt.
    - Entfernung des 5-Minuten-Hintergrund-Timers im ViewModel.
- **Firebase (v1.3.6 - RELEASED):** 2s Debouncing für Toggles, Batch-Updates für Resets/Delete, ServerTimestamp, Login-Autofill.

## Architecture
- Android (Kotlin/Compose), Firebase (Firestore + Functions), Cloud Functions (europe-west3)
- `build.gradle.kts`: appVersion string, versionCode int
- `functions/index.js`: Cloud Functions (email, cleanup, join, create, sendFeedbackEmail)
- `firestore.rules`: Security rules

## GitHub Actions Workflow
- `.github/workflows/android-release-v2.yml`
- Tag-Push → immer APK (prerelease: true falls x.x.1+, false falls x.x.0)
- AAB nur über manuelles `workflow_dispatch`
- Version wird aus `build.gradle.kts` gelesen

## v1.3.2 (2026-03-18)
- **Snooze-Banner:** Persistenz via `PreferencesRepository`. Design: 32.dp Ecken, Pastell-Grün. Snooze-Alarm stoppt korrekt wenn Banner-X gedrückt wird.
- **Chip-Layout:** Padding reduziert, kein Text-Truncation mehr.
- **Eingabefelder:** Paste-Support via `SelectionContainer`.
- **Alarm-System komplett überarbeitet (mehrere kritische Bugs):**
  - `AlarmScheduler`: Getrennte Request-Codes für Snooze vs. reguläre Alarme (`_snooze`-Suffix). Korrekte `getActivity`-Show-Intent in `AlarmClockInfo` statt `getBroadcast`. `FLAG_CANCEL_CURRENT` statt `FLAG_UPDATE_CURRENT + FLAG_IMMUTABLE`.
  - `FamilyViewModel.applyAlarms`: Grace-Period-Guard (5 Min) verhindert dass Firebase-Sync kurz nach der Weckzeit den Alarm überschreibt oder cancelt. `isAwakeToday`-Logik korrigiert (nur für heute, nicht morgen). Stille Cancel-Pfade in `recalculateSchedule` erhalten ebenfalls Grace-Period-Guard.
  - `RingingActivity`: Kein `FamilyViewModel` mehr – verwendet direkt `PreferencesRepository` und `AlarmScheduler`, um Race Conditions durch zwei konkurrierende ViewModel-Instanzen zu vermeiden. Snooze-Status wird beim Stop-Button korrekt gelöscht.
  - `AlarmReceiver`: Startet `RingingActivity` direkt via `context.startActivity()` (nicht nur über Notification-Full-Screen-Intent).
- **UI-Verbesserungen:** Alle Wochentage deaktivierbar. Member-Kachel zeigt nächsten aktiven Tag + tagespezifische Zeiten. Zeitplan-Karte zeigt Datum wenn Alarm nicht heute ist. Periodischer 5-Min-Refresh in ViewModel.
- **Anti-Pattern:** `FLAG_UPDATE_CURRENT + FLAG_IMMUTABLE` nie zusammen für AlarmManager-PendingIntents → `FLAG_CANCEL_CURRENT + FLAG_IMMUTABLE` verwenden.
- **Anti-Pattern:** `getBroadcast`-Intent NIEMALS als Show-Intent in `AlarmClockInfo` verwenden → muss `getActivity`-Intent sein.

## v1.3.1 (2026-03-17)
- **Chip-Layout:** `weight(1f)` → alle 7 Chips gleichmäßig, Sonntag sichtbar
- **Chip-Fehler-Markierung:** rot wenn Zeitkonfiguration ungültig
- **resolveEffectiveMember:** prüft heute's dayProfile.latestWakeUp (nicht Root-Feld)
  - Heute aktiv+vor Weckzeit → Heute; sonst Morgen; morgen inaktiv → isPaused=true
- **applyAlarms:** Alarm gecancelt wenn DayProfile des Ziel-Tages `isActive=false`
- **Validierung:** latestWakeUp>earliestWakeUp, leaveHome>weckEnd (inkl. Default 08:00)
- **Firebase:** kein Deploy nötig (keine Änderungen an Functions/Rules)

## v1.3.0 (2026-03-17) – Konsolidiert aus v1.2.1–v1.2.7
- **In-App Review:** Button „⭐ App bewerten" in Hilfe & Feedback. Play In-App Review API + Fallback auf Play Store.
- **Wochentag-Chips:** 2-Buchstaben-Kürzel DE+EN (Mo Di Mi Do Fr Sa So / Mo Tu We Th Fr Sa Su)
- **Inaktive Tage:** Chips ~30% Deckkraft (Text, Rahmen, Hintergrund)
- **Settings-Footer:** Version → Copyright → All rights → Links
- **Rate-Limits (Cloud Functions):** Email 5/h+10d, Join 5/min+10d, Create 3/h+6d
  - Dual Rate-Limit: generische `checkSingleRateLimit`-Hilfsfunktion in index.js
  - Schlüssel: `email_*_h`, `email_*_d`, `join_*_m`, `join_*_d`, `create_*_h`, `create_*_d`
- **Fix:** Chip-Text unsichtbar bei selektiert+inaktiv (gedämpfter grauer Container)
- **Fix:** Crash `RESOURCE_EXHAUSTED` bei createFamily → abgefangen in `FirebaseRepository`
- **Fix:** Rate-Limit-Fehlermeldungen überall (joinFamily, leaveAndJoinPendingCode, resendVerificationEmail)
- **Dependency:** `com.google.android.play:review-ktx:2.0.1`

## v1.2.0 (2026-03-17) – Alarm-Status-Persistenz
- Alarm-Status-Restore nach Neuinstall+Login aus Firestore
- Join-Flow Fixes, Race Condition Fixes, „Was ist neu?"-Dialog entfernt

## Signing & Deployment
- Release-Keystore: app/famwake-release.jks, Alias: famwake
- SHA-1:   3D:3C:D2:0A:E6:4F:AE:D8:27:70:FE:87:6F:C4:11:E9:7B:8C:C9:B2
- SHA-256: 38:89:77:FC:E1:E8:A9:E9:C4:F6:6E:25:D8:65:7C:18:68:81:BF:DB:50:8E:81:39:0D:81:08:F1:F5:6B:AD:46
- Play Store SHA-256: BE:37:A9:A3:90:94:A0:F1:ED:7F:5E:6C:A4:18:4E:79:2D:46:3B:2A:A3:96:A7:97:35:4B:26:AD:D6:D7:7E:4B
- assetlinks.json (familienwecker-web): alle 3 SHA-256 eingetragen

## Firebase
- Cloud Functions: alle deployed, europe-west3
- Rate-Limit-Daten in Firestore Collection `_rate_limits`
- Feedback-Archiv in Collection `feedback`

## Deep Links
- Schema: `https://familienwecker.de/join/{code}`
- `autoVerify=true` im AndroidManifest

## Known Patterns & Gotchas
- `tx.update()` auf nicht-existierendem Doc → immer `tx.set({merge:true})`
- `err.code` aus Firestore/Admin SDK ist numeric gRPC → immer `String(err.code)` casten
- Rate-Limit-Schlüssel geändert: alte Keys (`join_${uid}`, etc.) sind orphaned in Firestore (harmlos)
- `FirebaseFunctionsException` muss VOR `Exception` gecatcht werden (ist eine Unterklasse)
- `isFromCache`: allein kein zuverlässiger Offline-Indikator, immer mit NetworkUtils.isOnline() kombinieren
- FeedbackScreen: FirebaseFunctions.getInstance("europe-west3") explizit angeben (Region!)
- **Anti-Pattern:** `SelectionContainer` NIEMALS um editierbare `TextField`/`OutlinedTextField` legen. Ebenso verschachtelte `Scaffold`-Instanzen vermeiden, da sie die Event-Propagation für das native Android-Kontextmenü (Copy/Paste) stören können.
