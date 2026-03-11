# Brain Context
Android App (FamWake).
- **Core Architecture:** Firebase Firestore (Cloud Sync), Jetpack Compose (UI), Android AlarmManager, Kotlin Coroutines (Flows).
- **Major Design Decisions:** 
  - Offline-first Ansatz (Firestore-Cache mit Sync-Indikatoren).
  - Algorithmus-basiertes Scheduling (Rückwärtsrechnung von Weck- und Badzeiten).
  - Deep-Linking für Familien-Beitritt (`familienwecker.de/join`).
  - Strict Local Development: Commits und CI/CD-Pipelines (APK/AAB) werden ausschließlich über Git Tags in GitHub Releases gebaut.
- **Aktueller Stand:** Version 0.7.2 (dev). Release v0.7.1 eingeführt (R8 Obfuscation inkl. auto `mapping.txt`). Alle antiken Changelogs (0.4.x) konsolidiert für saubere Historie.
