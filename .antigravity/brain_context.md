## Recently Completed
- [x] **Release 0.9.0 (Consolidation):** Alle Sicherheits-Audit Fixes, Multi-Device Bugfixes und Lokalisierungserweiterungen seit 0.8.0 zusammengefasst. Firebase & GitHub Push abgeschlossen. Tag `v0.9.0`.
- [x] **Kritische Fixes:** Alarm-Switch ist nun gerätespezifisch; Firebase-Auth-Fehler sind lokalisiert (DE/EN).
- [x] **Git Hygiene:** Alte Zwischen-Tags (0.8.1-0.8.3) gelöscht.
- [x] **Alarm-Status Sync:** Neues Feld `deviceAlarmEnabled` in Firestore (members). Andere Geräte sehen den Alarm-Status geclaimter User live (reine Anzeige, kein Einfluss auf lokalen Switch).

# Brain Context
Android App (FamWake).
- **Core Architecture:** Firebase Firestore (Cloud Sync), Jetpack Compose (UI), Android AlarmManager, Kotlin Coroutines (Flows).
- **Major Design Decisions:** 
  - Offline-first (Firestore Cache).
  - Sicherheits-Audit: Beitritt via Cloud Function & EncryptedSharedPreferences.
  - Alarme sind gerätespezifisch (kein Sync von 'isAlarmEnabled' via Firestore).
  - `deviceAlarmEnabled` (Firestore, members-Dokument): Nur-Anzeige-Feld. Jedes Gerät schreibt eigenen Status für Fremdanzeige.
- **Aktueller Stand:** Version 0.9.1-dev. Feature: Alarm-Status anderer geclaimter User wird live synchronisiert.
