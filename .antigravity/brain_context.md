## Recently Completed
- [x] **Release 0.9.9 (Security-Audit abgeschlossen):** Alle Hoch/Mittel-Findings aus 4 Audit-Durchläufen umgesetzt. GitHub Push & Tag v0.9.9. Firebase (Firestore Rules + 7 Cloud Functions) deployed. CHANGELOG, test_plan (TC-33) aktualisiert. whats_new.json: Willkommenstext, versionCode 499. Nächste: 0.9.10-dev.
- [x] **Security Hardening (Cloud Functions):** E-Mail Rate-Limiting (3/Std/Mail) für sendBrandedResetEmail, sendVerificationEmail, sendBrandedConfirmationEmail. crypto.randomInt() statt Math.random() in createFamily.
- [x] **Re-Audit (2./3./4. Durchlauf):** H-1 RingingActivity Singleton, H-2 BootReceiver Singleton, M-1 AuthRepository DEBUG-Guard, M-2 Firestore allow create: false, M-3 SettingsScreen currentUserId, M-4 deprecated Window-Flags. Alle remaning Niedrig-Findings (Log-Guards, Imports, Tags) behoben.
- [x] **Release 0.9.5 (Audit-Abschluss):** Alle Security-Audit-Findings (H-1 bis N-6) umgesetzt. LocalDarkTheme, SyncStatus, Passwort-Validierung, FamilyMemberMapper, BootReceiver, Routes u.v.m. GitHub Push & Tag v0.9.5.

# Brain Context
Android App (FamWake).
- **Core Architecture:** Firebase Firestore (Cloud Sync), Jetpack Compose (UI), Android AlarmManager, Kotlin Coroutines (Flows).
- **Major Design Decisions:**
  - Offline-first (Firestore Cache).
  - Familie erstellen/beitreten vollständig via Cloud Functions (createFamily, joinFamilyByCode) – kein direkter Client-Write.
  - Firestore Rule: `allow create: if false` für families – nur Admin SDK.
  - `PreferencesRepository`-Singleton via `FamWakeApplication` überall konsistent (inkl. RingingActivity, BootReceiver).
  - Alarme sind gerätespezifisch (kein Sync von 'isAlarmEnabled' via Firestore).
  - `deviceAlarmEnabled` (Firestore, members-Dokument): Nur-Anzeige-Feld.
  - E-Mail Rate-Limiting: max. 3/Stunde pro Mail-Adresse in allen public Cloud Functions.
  - firebase.json: Firestore-Rules jetzt via `firebase deploy --only firestore` deploybar.
- **Aktueller Stand:** Version 0.9.9 released. Nächste: 0.9.10-dev.
- **⚠️ Hilt (AGP 9.x INKOMPATIBEL):** Hilt-Gradle-Plugin nutzt `BaseExtension` (entfernt in AGP 8+). Alle Versionen bis Hilt 2.55 schlagen fehl. Workaround: Manuelle Factory-Pattern (FamilyViewModelFactory, AuthViewModelFactory) beibehalten bis Hilt AGP-9-Support liefert.
