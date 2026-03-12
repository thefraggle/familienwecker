## Recently Completed
- [x] **NDK Symbol Fix (Versuch 3):** Re-aktivierte 'FULL' Debug Symbole und stellte NDK-Installation im CI sicher, um Play Console Warnungen zu eliminieren.
- [x] **Security Revert:** `FLAG_SECURE` für Join-Codes entfernt (Benutzer-Feedback: funktioniert nicht/nicht gewünscht).
# Brain Context
Android App (FamWake).
- **Core Architecture:** Firebase Firestore (Cloud Sync), Jetpack Compose (UI), Android AlarmManager, Kotlin Coroutines (Flows).
- **Major Design Decisions:** 
  - Offline-first Ansatz (Firestore-Cache mit Sync-Indikatoren).
  - Algorithmus-basiertes Scheduling (Rückwärtsrechnung von Weck- und Badzeiten).
  - Deep-Linking für Familien-Beitritt (`familienwecker.de/join`).
  - Strict Local Development: Commits und CI/CD-Pipelines (APK/AAB) werden ausschließlich über Git Tags in GitHub Releases gebaut.
- [x] Post-Release: Lokale Version auf `0.7.6-dev` angehoben.
- **Aktueller Stand:** Version 0.7.6-dev. Release v0.7.5 durchgeführt. NDK Symbole via `FULL` eingebettet, CI-Workflow angepasst, `FLAG_SECURE` Revert durchgeführt.
