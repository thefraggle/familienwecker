## Recently Completed
- [x] **Lokalisierung (v0.8.4-dev):** Firebase-Auth-Fehler (Länge, Kollision) lokalisiert, hardcodierte Strings im Login-Bereich entfernt.
- [x] **Release 0.8.3:** Kritischer Bugfix: Alarm-Switch gerätespezifisch gemacht (Firestore-Sync entfernt). Tag `v0.8.3`.
- [x] **Release 0.8.2:** Sicherheits-Audit Fixes (K+H). EncryptedSharedPreferences, Cloud Function Join mit Rate-Limiting, restriktive Firestore Rules. Tag `v0.8.2`.
- [x] **Release 0.8.1:** Kritische Bugfixes für Offline-UI und Join-Konflikt-Dialog. Tag `v0.8.1`.
- [x] **Firebase Cleanup:** Korrektur der Cloud Functions (Timestamp-Handling & Secrets).
- [x] **Release 0.8.0:** Konsolidierung von 0.7.x und Implementierung aller Audit-Fixes. Tagging und Push zu GitHub erfolgreich.

# Brain Context
Android App (FamWake).
- **Core Architecture:** Firebase Firestore (Cloud Sync), Jetpack Compose (UI), Android AlarmManager, Kotlin Coroutines (Flows).
- **Major Design Decisions:** 
  - Offline-first Ansatz (Firestore-Cache mit Sync-Indikatoren).
  - Algorithmus-basiertes Scheduling (Rückwärtsrechnung von Weck- und Badzeiten mit Midnight-Guard).
  - Deep-Linking für Familien-Beitritt (`familienwecker.de/join`).
  - Strict Local Development: Release-Zyklen werden über semantische Tags (vX.Y.Z) konsolidiert und via GitHub deployt.
- **Aktueller Stand:** Version 0.8.4-dev. Release v0.8.3 (Bugfix Alarm-Sync) durchgeführt.
