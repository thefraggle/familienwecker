# Brain Context - FamWake (v1.4.1)

## Current State
- **Current Version:** 1.4.1 (RELEASED 2026-03-19)
- **Changes in 1.4.1:** Fixed Login Crash (email/password/auth), Updated App Icon (adaptive + legacy), Onboarding Design Update (background image), Default Dark Mode, Extended Input Validation (Family, Code, Member), Robust Deep-Link/Join logic (fix duplicate popup & verification).
- **Consolidated v1.4.0:** Merged all v1.3.1-v1.3.11 fixes into one major update.
- **Key Features:** XSS Security Hotfix, Android Autofill transformation (AutofillNode), Context Menu repair, Cloud-Reset-Logic (2h), Onboarding Tour, RingingScreen redesign.
- **Local-First Logik:** Wecker-Switch (`isAlarmEnabled`) und "Bereits wach" Wirkung sind lokal pro Gerät; Status-Icon wird via Firestore gesynct.
- **Architecture:** Android (Kotlin/Compose), Firebase (Auth, Firestore, Functions Node.js europe-west3).
- **App Icon:** Updated locally with `ic_launcher_foreground.png` (Adaptive + Legacy). Primary color `#211954`.
- **Security:** Input Validation in ViewModels (email, pass, names), XSS protection in emails (`escapeHtml`), IDOR verified, EncryptedSharedPreferences for local data.

## GitHub Actions & Deployment
- Tag-Push (v1.x.0) → Production APK/AAB via `.github/workflows/android-release-v2.yml`.
- Firebase: Functions, Rules and Indexes deployed manually via `npx firebase-tools`.
- Versioning: Managed in `app/build.gradle.kts`.

## Deep Links
- `https://familienwecker.de/join/{code}` (autoVerify: true).

## Known Patterns & Gotchas
- `tx.update()` on missing doc → use `tx.set({merge:true})`.
- `err.code` from Admin SDK is numeric → cast to `String(err.code)`.
- **Anti-Pattern:** No `SelectionContainer` around editable TextFields. No nested `Scaffold` instances (blocks native context menu).
- **Autofill:** Use `AutofillNode` + `onGloballyPositioned` (boundsInWindow) for reliable password manager integration in Compose.
