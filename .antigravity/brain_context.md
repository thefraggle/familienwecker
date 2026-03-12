## Recently Completed
- [x] **Release 0.8.0:** Konsolidierung von 0.7.x und Implementierung aller Audit-Fixes (Security & Code Quality). Tagging und Push zu GitHub erfolgreich.
- [x] **Firebase Cleanup:** Korrektur der Cloud Functions (Secrets & Timestamp-Validierung) für unbestätigte User (48h) und inaktive Familien (180d).
- [x] **Security Audit:** Algorithmus-Scheduling mit Midnight-Guard abgesichert; Offline-Erkennung via `NET_CAPABILITY_VALIDATED` präzisiert.
- [x] **Offline-Bug-Fixes (0.8.1):** Header zeigt nun korrekt CloudOff-Icon (isOffline hat Vorrang über hasPendingWrites). Join-Konflikt-Dialog schliessen sich korrekt auch bei Offline-Fehler (Button disabled + Spinner während Coroutine läuft, kein Mehrfachklick).

# Brain Context
Android App (FamWake).
- **Core Architecture:** Firebase Firestore (Cloud Sync), Jetpack Compose (UI), Android AlarmManager, Kotlin Coroutines (Flows).
- **Major Design Decisions:** 
  - Offline-first Ansatz (Firestore-Cache mit Sync-Indikatoren).
  - Algorithmus-basiertes Scheduling (Rückwärtsrechnung von Weck- und Badzeiten mit Midnight-Guard).
  - Deep-Linking für Familien-Beitritt (`familienwecker.de/join`).
  - Strict Local Development: Release-Zyklen werden über semantische Tags (vX.Y.Z) konsolidiert und via GitHub deployt.
- **Aktueller Stand:** Version 0.8.1-dev. Release v0.8.0 durchgeführt (Audit & Cleanup).
