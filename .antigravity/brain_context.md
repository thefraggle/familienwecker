# FamWake Brain Context

## Current State
- **Version:** 1.1.4 released, next: 1.1.5-dev
- **Repo:** public @ github.com/thefraggle/familienwecker
- **Last Tag:** v1.1.4
- **Active Tags:** v1.0.0, v1.1.0, v1.1.1, v1.1.2, v1.1.3, v1.1.4

## Architecture
- Android (Kotlin/Compose), Firebase (Firestore + Functions), Cloud Functions (europe-west3)
- `build.gradle.kts`: appVersion string, versionCode int
- `functions/index.js`: 7 Functions (email, cleanup, join, create)
- `firestore.rules`: Security rules
- `whats_new.json`: Release notes (de + en)

## GitHub Actions Workflow
- `.github/workflows/android-release-v2.yml`
- Tag endet auf `.0` → AAB (Play Store, prerelease: false)
- Tag endet auf `.1+` → APK (Beta, prerelease: true)
- Version wird aus `build.gradle.kts` gelesen
- AAB-Dateiname: immer `FamWake-v{app_version}-release.aab` (nicht github.ref_name)

## 1.1.4 (2026-03-16) – Legal Compliance
- `SettingsScreen.kt`: Nutzungsbedingungen ("Terms of Use") Link hinzugefügt.
- `LoginScreen.kt`: Disclaimer bei Registrierung mit klickbaren Links zu Nutzungsbedingungen und Datenschutz.
- `strings.xml`: Neue Strings für Nutzungsbedingungen und Disclaimer (DE/EN).
- Firebase: Functions & Rules deployed (2026-03-16).

## 1.1.3 (2026-03-16) – Audit-Release
- `Scheduler.kt`: `isBefore` → `!isAfter` für Frühstücks-Constraint (inkl. ==Fall)
- `Scheduler.kt`: Post-Validation nach Loop – kein Frühstücker darf Bad nach Frühstücksbeginn beenden
- `FamilyViewModel.kt`: Offline-Indikator nur bei isFromCache UND !NetworkUtils.isOnline()
- `FamilyViewModel.kt`: `initialAlarmPushDone`-Flag – deviceAlarmEnabled nur 1x schreiben
- `FamilyViewModel.kt`: Race-Condition-Guard für myMemberId Auto-Sync verbessert
- `FamilyViewModel.kt`: Stille catch-Blöcke durch BuildConfig.DEBUG-Log ersetzt
- `functions/index.js`: `tx.update` → `tx.set({merge:true})` in joinFamilyByCode + createFamily
- `functions/index.js`: Join-Limit 5 → 10 pro Minute
- CI: AAB-Name fix (app_version statt github.ref_name)
- Firebase: Functions deployed 2026-03-16

## 1.1.2 (2026-03-16)
- `leaveFamily()`: Member-Datensatz via `removeMember()` komplett gelöscht. Re-Join erfordert neues Profil.

## 1.1.1 (2026-03-16)
- Firebase Rate-Limit: `tx.update()` → `tx.set({merge:true})` in checkEmailRateLimit
- Fehlercode-Cast: `String(err.code)` vor `.includes()`
- Self-Healing PERMISSION_DENIED: `checkFamilyExists()` Guard

## 1.1.0 (konsolidiert aus 1.0.1–1.0.5)
- Reboot-Alarm, Google OAuth APK, Google Play OAuth, BootReceiver, RingingActivity OEM-Flags

## Signing & Deployment
- Release-Keystore: app/famwake-release.jks, Alias: famwake
- SHA-1:   3D:3C:D2:0A:E6:4F:AE:D8:27:70:FE:87:6F:C4:11:E9:7B:8C:C9:B2
- SHA-256: 38:89:77:FC:E1:E8:A9:E9:C4:F6:6E:25:D8:65:7C:18:68:81:BF:DB:50:8E:81:39:0D:81:08:F1:F5:6B:AD:46
- Debug SHA-256: 45:EE:50:A6:9A:11:DF:0B:63:21:55:E9:5D:CC:4B:28:1D:52:7D:1A:8C:B0:48:4C:0C:EE:0A:A6:20:1E:11:0A
- Play Store SHA-256: BE:37:A9:A3:90:94:A0:F1:ED:7F:5E:6C:A4:18:4E:79:2D:46:3B:2A:A3:96:A7:97:35:4B:26:AD:D6:D7:7E:4B
- assetlinks.json (familienwecker-web): alle 3 SHA-256 eingetragen (Release, Debug, Play Store)
- GitHub Secret GOOGLE_SERVICES_JSON muss bei JSON-Änderungen manuell aktualisiert werden
- APK (Beta) ≠ Play Store AAB: verschiedene Signing-Keys

## Firebase
- Functions: alle 7 Functions deployed (2026-03-16)
- Firestore Rules: aktuell

## Deep Links
- Schema: `https://familienwecker.de/join/{code}`
- `autoVerify=true` im AndroidManifest
- assetlinks.json muss SHA-256 des Keystores enthalten (SHA-1 reicht nicht)
- Bei Gerätewechsel/Neuinstall: Android verifiziert automatisch
- Bestehende Installs: Einstellungen → Apps → FamWake → Standard-Apps neu setzen

## Known Patterns & Gotchas
- google-services.json: manuell beide SHA-1 Clients eingetragen (GCP-generiert)
- `tx.update()` auf nicht-existierendem Doc → immer `tx.set({merge:true})` verwenden
- `err.code` aus Firestore/Admin SDK ist numeric gRPC → immer `String(err.code)` casten
- Join-Rate-Limit: jetzt 10/min (vorher 5 – zu niedrig für Join/Leave-Tests)
- Alarm-Backup: nur ein Member pro Gerät (by design – ein geclaimter User pro Gerät)
- `isFromCache`: allein kein zuverlässiger Offline-Indikator, immer mit NetworkUtils.isOnline() kombinieren
