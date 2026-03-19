# Brain Context - FamWake (v1.4.0)

## Current State
- **Current Version:** 1.4.0
- **Recent Release (v1.4.0):** 2026-03-19 (Consolidated 1.3.x fixes: XSS, Autofill, Cloud Reset, UI Redesign)
- **Recent Release (v1.3.0):** 2026-03-17 (In-App Review, weekday distribution fixes)
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
- `FirebaseFunctionsException` muss VOR `Exception` gecatcht werden (ist eine Unterklasse)
- `isFromCache`: allein kein zuverlässiger Offline-Indikator, immer mit NetworkUtils.isOnline() kombinieren
- FeedbackScreen: FirebaseFunctions.getInstance("europe-west3") explizit angeben (Region!)
- **Anti-Pattern:** `SelectionContainer` NIEMALS um editierbare `TextField`/`OutlinedTextField` legen. Ebenso verschachtelte `Scaffold`-Instanzen vermeiden, da sie die Event-Propagation für das native Android-Kontextmenü (Copy/Paste) stören können.
