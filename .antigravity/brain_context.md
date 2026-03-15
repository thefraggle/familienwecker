# FamWake Brain Context

## Current State
- **Version:** 1.0.1 released, next: 1.0.2-dev (nicht gesetzt)
- **Repo:** public @ github.com/thefraggle/familienwecker
- **Last Tag:** v1.0.1 (force-updated to bf05145)
- **Active Tags:** v1.0.0, v1.0.1

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
- `workflow_dispatch` mit `force_aab: true` → AAB als Artefakt `FamWake-v{version}-release` (14 Tage)
- Version wird aus `build.gradle.kts` gelesen

## 1.0.1 Fixes
- Battery-Kachel Settings: sofortiges Verschwinden via ON_RESUME-Observer
- Battery-Kachel Settings: Style auf 32dp/outline/surfaceVariant angeglichen
- Battery-Hinweis aus MainScreen entfernt → nur noch in Settings

## 1.0.0 Features (major)
- Sicherheit: HTTPS Deep Links, Admin-only Familienlöschung, Offline-Claim gesperrt
- Rate-Limiting für Cloud Functions (email)
- EncryptedSharedPreferences, CloudOff-Icon, Alarm-Status Sync

## Docs
- CHANGELOG.md/.en.md: 1.0.0 + 1.0.1 vollständig
- ROADMAP.md/.en.md: gestrafft, kein Footer/Intro
- test_plan.md/.en.md: TC-38 (Battery-Kachel), EC-Tabellen kompakt
- Alle Docs: Füllwörter entfernt

## Firebase
- Functions: zuletzt deployed, alle aktuell (kein Re-Deploy nötig)
- Firestore Rules: aktuell

## Known
- GitHub Releases-Counter zeigt vorübergehend "11" (Cache-Artefakt, wird automatisch korrigiert)
- Alte Tags (0.6.0, v0.5.0-v0.9.0) lokal + remote gelöscht
