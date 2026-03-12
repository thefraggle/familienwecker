## Recently Completed
- [x] **Release 0.9.5 (Audit-Abschluss):** Alle Security-Audit-Findings (H-1 bis N-6) umgesetzt. LocalDarkTheme, SyncStatus, Passwort-Validierung, FamilyMemberMapper, BootReceiver, Routes u.v.m. GitHub Push & Tag v0.9.5.
- [x] **Alarm-Status Sync (0.9.1):** `deviceAlarmEnabled` in Firestore. Live-Sync fremder geclaimter User (nur Anzeige).
- [x] **Release 0.9.0 (Consolidation):** Security-Audit, Multi-Device Bugfixes, Lokalisierung. Firebase & GitHub Push. Tag v0.9.0.

# Brain Context
Android App (FamWake).
- **Core Architecture:** Firebase Firestore (Cloud Sync), Jetpack Compose (UI), Android AlarmManager, Kotlin Coroutines (Flows).
- **Major Design Decisions:**
  - Offline-first (Firestore Cache).
  - Sicherheits-Audit: Beitritt via Cloud Function & EncryptedSharedPreferences.
  - Alarme sind gerätespezifisch (kein Sync von 'isAlarmEnabled' via Firestore).
  - `deviceAlarmEnabled` (Firestore, members-Dokument): Nur-Anzeige-Feld. Jedes Gerät schreibt eigenen Status für Fremdanzeige.
- **Aktueller Stand:** Version 0.9.5 released. Nächste: 0.9.6-dev.
- **⚠️ Hilt (H-6) INKOMPATIBEL mit AGP 9.x:** Hilt-Gradle-Plugin nutzt `BaseExtension` (entfernt in AGP 8+). Alle Versionen bis Hilt 2.55 schlagen mit "Android BaseExtension not found" fehl. Kein Release verfügbar das AGP 9.1.0 unterstützt (Stand: März 2026). Workaround: Manuelle Factory-Pattern (FamilyViewModelFactory, AuthViewModelFactory) beibehalten bis Hilt AGP-9-Support liefert.
