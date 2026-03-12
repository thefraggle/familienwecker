# To-Do

## Offen (langfristig)
- [ ] **H-6:** Hilt DI – **BLOCKIERT** durch AGP 9.x Inkompatibilität. Warten auf Hilt-Release mit AGP-9-Support.

## Recently Completed
- [x] **Release 0.9.9:** Security-Audit vollständig abgeschlossen. GitHub Push + Tag v0.9.9. Firebase deployed. Docs aktualisiert.
- [x] **Security Hardening:** E-Mail Rate-Limit in allen public Cloud Functions (3/Std/Mail). crypto.randomInt() in createFamily. Firestore allow create: if false.
- [x] **Re-Audit (2./3./4.):** RingingActivity+BootReceiver App-Singleton, SettingsScreen currentUserId, deprecated Flags, DEBUG-Guards, Tracking-Tags.
- [x] **Audit-Findings 0.9.2–0.9.5:** Alle Findings umgesetzt – BootReceiver, Routes, BatteryWarnung, LocalDarkTheme, FamilyMemberMapper, SyncStatus, Passwort-Validierung.
- [x] **Alarm-Status Sync (0.9.1):** `deviceAlarmEnabled` in Firestore. Live-Sync fremder geclaimter User.
- [x] **Release 0.9.0:** Consolidation Release. GitHub Push & Tag v0.9.0.
