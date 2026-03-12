## Recently Completed
- [x] **NDK Symbol Fix (Versuch 3):** Re-aktivierte 'FULL' Debug Symbole und stellte NDK-Installation im CI sicher, um Play Console Warnungen zu eliminieren.
# Brain Context
Android App (FamWake).
- **Core Architecture:** Firebase Firestore (Cloud Sync), Jetpack Compose (UI), Android AlarmManager, Kotlin Coroutines (Flows).
- **Major Design Decisions:** 
  - Offline-first Ansatz (Firestore-Cache mit Sync-Indikatoren).
  - Algorithmus-basiertes Scheduling (Rückwärtsrechnung von Weck- und Badzeiten).
  - Deep-Linking für Familien-Beitritt (`familienwecker.de/join`).
  - Strict Local Development: Commits und CI/CD-Pipelines (APK/AAB) werden ausschließlich über Git Tags in GitHub Releases gebaut.
- **Aktueller Stand:** Version 0.7.5 (dev). Release v0.7.4 durchgeführt. NDK Symbol Fix (Versuch 3) implementiert: Symbole werden nun via 'FULL' eingebettet und NDK im CI installiert.
