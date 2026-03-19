# Brain Context - FamWake (v1.4.4)

## Current State
- **Current Version:** 1.4.4 (LOCAL - NEXT)
- **Latest Release:** 1.4.3 (2026-03-19)
- **Changes in 1.4.3:** Admin Stats Report (E-Mail), Secure `_admins` Firestore collection, Reactive Admin Check (Auth-driven), Battery Optimization (WhileSubscribed).
- **Changes in 1.4.2:** Fixed duplicate join popup in `MainScreen` by removing redundant `AlertDialog` instances.
- **Key Features:** Admin Console, XSS Security Hotfix, Android Autofill transformation (AutofillNode), Context Menu repair, Cloud-Reset-Logic (2h), Onboarding Tour, RingingScreen redesign.
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
