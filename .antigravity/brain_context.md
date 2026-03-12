## Recently Completed
- [x] **Release 1.0.0 (Erster stabiler Release):** Bug-Fixes (HTTP-Guard, Admin-Delete, Offline-Claim-Sperre, Offline-Icon, Deep-Link Sofort-Dialog, Familie löschen mit anderen Membern). WhatsNew Button-Text-Fix. CHANGELOG konsolidiert (0.9.x). ROADMAP aktualisiert. Test Plan erweitert (TC-34–37, EC-32/33). GitHub Push & Tag v1.0.0. Firebase Functions (keine Änderungen seit letztem Deploy).
- [x] **Release 0.9.9 (Security-Audit abgeschlossen):** Alle Hoch/Mittel-Findings aus 4 Audit-Durchläufen umgesetzt. GitHub Push & Tag v0.9.9. Firebase deployed.

# Brain Context
Android App (FamWake).
- **Core Architecture:** Firebase Firestore (Cloud Sync), Jetpack Compose (UI), Android AlarmManager, Kotlin Coroutines (Flows).
- **Major Design Decisions:**
  - Offline-first (Firestore Cache).
  - Familie erstellen/beitreten vollständig via Cloud Functions (createFamily, joinFamilyByCode) – kein direkter Client-Write.
  - Firestore Rule: `allow create: if false` für families – nur Admin SDK.
  - Member delete Rule: nur owner oder unclaimed – `deleteFamily` unclaimed fremde Member vor Batch-Delete.
  - `PreferencesRepository`-Singleton via `FamWakeApplication` überall konsistent.
  - Alarme sind gerätespezifisch (kein Sync von 'isAlarmEnabled' via Firestore).
  - `deviceAlarmEnabled` (Firestore, members-Dokument): Nur-Anzeige-Feld.
  - E-Mail Rate-Limiting: max. 3/Stunde pro Mail-Adresse in allen public Cloud Functions.
  - `isAdmin` = `auth.currentUser?.uid == _familyCreatorId.value` (loaded from Firestore on family load).
  - `isOffline` Debounce: 3s nach `isFromCache = true` (unabhängig von hasPendingWrites).
  - GitHub Actions baut und deployed AAB automatisch zum Play Store bei Tag-Push.
- **Aktueller Stand:** Version 1.0.0 released. Nächste: 1.0.1-dev.
- **⚠️ Hilt (AGP 9.x INKOMPATIBEL):** Workaround: Manuelle Factory-Pattern (FamilyViewModelFactory, AuthViewModelFactory) beibehalten bis Hilt AGP-9-Support liefert.
- **Git Tags:** v1.0.0, v0.9.0, v0.8.0, v0.7.0, v0.5.0, 0.6.0
