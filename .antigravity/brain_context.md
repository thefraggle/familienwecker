# FamWake Brain Context

## Current State
- **Version:** 1.1.6 released, next: 1.1.7-dev
- **Repo:** public @ github.com/thefraggle/familienwecker
- **Last Tag:** v1.1.6
- **Active Tags:** v1.0.0, v1.1.0, v1.1.5, v1.1.6

## Architecture
- Android (Kotlin/Compose), Firebase (Firestore + Functions), Cloud Functions (europe-west3)
- `build.gradle.kts`: appVersion string, versionCode int
- `functions/index.js`: 8 Functions (email, cleanup, join, create, sendFeedbackEmail)
- `firestore.rules`: Security rules
- `whats_new.json`: Release notes (de + en)

## GitHub Actions Workflow
- `.github/workflows/android-release-v2.yml`
- Tag endet auf `.0` → AAB (Play Store, prerelease: false)
- Tag endet auf `.1+` → APK (Beta, prerelease: true)
- Version wird aus `build.gradle.kts` gelesen
- AAB-Dateiname: immer `FamWake-v{app_version}-release.aab`

## 1.1.6 (2026-03-17) – Scheduler Bugfixes
- **BUG FIX Frühstückszeit:** Scheduler-Fallback war `LocalTime.of(23,59)` wenn kein `leaveHomeTime` → jetzt `latestWakeUp + bathroomDuration`.
- **BUG FIX dayProfiles:** `resolveEffectiveMember()` löst Wochentag-Profil auf bevor der Scheduler aufgerufen wird; deaktivierte Tage setzen `isPaused=true`.
- **Regressionstest:** Neuer Unit-Test in `SchedulerTest.kt`.
- **Commits:** `00ae64a` fix + `fab1784` chore(release)

## 1.1.5 (2026-03-17) – Settings + Feedback
- **FeedbackScreen.kt (NEU):** Dedizierter Screen mit Kategorie, Nachricht, optionaler E-Mail, Gerätedaten.
- **Firebase Cloud Function `sendFeedbackEmail`:** Resend API, archiviert in Firestore `feedback`-Collection.
- **Feedback UX:** Formular leert sich nach Absenden, Screen schließt sich nach 2,5s automatisch.
- **SettingsScreen.kt:** Sprache & Erscheinungsbild in einer Karte; Hilfe & Feedback in eigener Karte.
- **Settings Footer:** Versionsnummer + Rechtlinks (Nutzungsbedingungen, Datenschutz, Impressum) + Copyright.
- **Account löschen:** Externer Link statt Info-Dialog (DE: account-deletion.html, EN: account-deletion-en.html).
- **Routes.kt:** `FEEDBACK`-Route ergänzt.
- **String-Audit:** Veraltete Strings entfernt (15 Keys); DE/EN vollständig synchron.
- **Wochentag-Konfiguration:** Im Changelog ergänzt (bereits in 1.1.x umgesetzt).
- **Firebase deployed:** `sendFeedbackEmail` live in europe-west3.
- **Docs:** Changelogs, Roadmap, Testplan (DE+EN) aktualisiert. Roadmap zeigt nur noch Backlog.
- **BUG FIX Frühstückszeit:** Scheduler-Fallback war `LocalTime.of(23,59)` wenn kein `leaveHomeTime` → jetzt `latestWakeUp + bathroomDuration`.
- **BUG FIX dayProfiles:** `resolveEffectiveMember()` löst Wochentag-Profil auf bevor der Scheduler aufgerufen wird; deaktivierte Tage setzen `isPaused=true`.
- **Commit:** `00ae64a` `fix(scheduler): breakfast time fallback + dayProfiles resolution`

## 1.1.4 (2026-03-16) – Legal Compliance
- Nutzungsbedingungen + Registrierungs-Disclaimer (LoginScreen, SettingsScreen).

## 1.1.3 (2026-03-16) – Audit-Release
- Scheduler, Offline-Indicator, Rate-Limit tx.set, Join-Limit 10/min, CI-Fix.

## 1.1.2 / 1.1.1 / 1.1.0 – Bugfixes
- Member-Deletion, Rate-Limit-Fix, Ghost-Claim, Reboot-Alarm, Google OAuth.

## Signing & Deployment
- Release-Keystore: app/famwake-release.jks, Alias: famwake
- SHA-1:   3D:3C:D2:0A:E6:4F:AE:D8:27:70:FE:87:6F:C4:11:E9:7B:8C:C9:B2
- SHA-256: 38:89:77:FC:E1:E8:A9:E9:C4:F6:6E:25:D8:65:7C:18:68:81:BF:DB:50:8E:81:39:0D:81:08:F1:F5:6B:AD:46
- Debug SHA-256: 45:EE:50:A6:9A:11:DF:0B:63:21:55:E9:5D:CC:4B:28:1D:52:7D:1A:8C:B0:48:4C:0C:EE:0A:A6:20:1E:11:0A
- Play Store SHA-256: BE:37:A9:A3:90:94:A0:F1:ED:7F:5E:6C:A4:18:4E:79:2D:46:3B:2A:A3:96:A7:97:35:4B:26:AD:D6:D7:7E:4B
- assetlinks.json (familienwecker-web): alle 3 SHA-256 eingetragen

## Firebase
- Functions: 8 Functions deployed (sendFeedbackEmail neu, 2026-03-17)
- Firestore Rules: aktuell
- Neue Collection: `feedback` (Archiv aller Feedback-Einsendungen)

## Deep Links
- Schema: `https://familienwecker.de/join/{code}`
- `autoVerify=true` im AndroidManifest
- assetlinks.json muss SHA-256 des Keystores enthalten

## Known Patterns & Gotchas
- `tx.update()` auf nicht-existierendem Doc → immer `tx.set({merge:true})` verwenden
- `err.code` aus Firestore/Admin SDK ist numeric gRPC → immer `String(err.code)` casten
- Join-Rate-Limit: 10/min
- `isFromCache`: allein kein zuverlässiger Offline-Indikator, immer mit NetworkUtils.isOnline() kombinieren
- FeedbackScreen: FirebaseFunctions.getInstance("europe-west3") explizit angeben (Region!)
