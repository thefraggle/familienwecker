# Brain Context
Android App (FamWake).
- **Core Architecture:** Firebase Firestore (Cloud Sync), Jetpack Compose (UI), Android AlarmManager, Kotlin Coroutines (Flows).
- **Major Design Decisions:** 
  - Offline-first Ansatz (Firestore-Cache mit Sync-Indikatoren).
  - Algorithmus-basiertes Scheduling (Rückwärtsrechnung von Weck- und Badzeiten).
  - Deep-Linking für Familien-Beitritt (`familienwecker.de/join`).
- **Aktueller Stand:** Version 0.7.2 (dev). Release v0.7.1 durchgeführt mit aktiviertem R8 Build (und automatischer mapping.txt) plus GitHub Actions Cache Warming auf dem main-Branch.
