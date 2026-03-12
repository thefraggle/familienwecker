# To-Do

## Offen (langfristig)
- [ ] **H-6:** Hilt DI – **BLOCKIERT** durch AGP 9.x Inkompatibilität. `BaseExtension` entfernt in AGP 8+; Hilt 2.55 (letztes Release) schlägt fehl mit "Android BaseExtension not found". Warten auf Hilt-Release mit AGP-9-Support.

## Recently Completed
- [x] **Audit-Findings 0.9.2–0.9.5:** Alle Findings umgesetzt – BootReceiver, Routes, BatteryWarnung, LocalDarkTheme, FamilyMemberMapper, getSyncStatusFlow, Passwort-Validierung, SharedPreferences-Listener, SyncStatus, N-2, M-5.
- [x] **Alarm-Status Sync (0.9.1):** `deviceAlarmEnabled` in Firestore. Live-Sync fremder geclaimter User.
- [x] **Release 0.9.0:** Consolidation Release (Security, Multi-Device Sync, Localization). GitHub Push & Tag v0.9.0.
