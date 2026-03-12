# To-Do

## Security Audit – Offen für später (Mittel/Niedrig)
- [ ] **M-1:** `toFamilyMember()` Extension-Funktion extrahieren (Duplikat-Mapping in `FirebaseRepository`)
- [ ] **M-2:** `saveUserFamily()` ist erledigt; `updateFamilyAlarmEnabled` könnte Result<Unit> rückkgeben (optional)
- [ ] **M-3:** Navigation via `sealed class` statt String-Literals in `MainActivity.kt`
- [ ] **M-5:** `SyncStatus` auch auf `families/{id}` Dokument ausweiten (kosmetisch)
- [ ] **M-7:** Clientseitige Passwort-Validierung (min. 8 Zeichen) im Register-Flow
- [ ] **H-6:** Dependency Injection für Repositories einführen (Hilt oder manuell, langfristig)
- [ ] **N-1:** Hardcodierten String "Unbekannt" in `FirebaseRepository` durch `UiText` ersetzen
- [ ] **N-2:** `isSystemInDarkTheme()` via CompositionLocal durchreichen statt mehrfach abfragen
- [ ] **N-4:** Doppelten `graphicsLayer`-Import in `MainScreen.kt` entfernen
- [ ] **N-5:** GitHub Actions Workflow: `commitHash`/`commitDate` Properties automatisch setzen
- [ ] **N-6:** `SharedPreferences.OnSharedPreferenceChangeListener` in `clearup()`-Methode deregistrieren

## Recently Completed
- [x] **Alarm-Status Sync (0.9.1):** `deviceAlarmEnabled` in Firestore members-Dokument. Live-Sync des Alarm-Status fremder geclaimter User (nur Anzeige).
- [x] **Release 0.9.0:** Consolidation Release (Security, Multi-Device Sync, Localization). GitHub Push & Tag v0.9.0.
- [x] **Release 0.8.x:** Bugfixes (Offline-UI & Join-Konflikt) und Sicherheits-Audit (v0.8.2).
- [x] **Release 0.8.1:** Kritische Bugfixes (Offline-UI & Join-Konflikt). Tag v0.8.1 erstellt.
- [x] **Release Cycle 0.8.0:** Konsolidierung 0.7.1-0.7.6 + Audit Fixes (NetworkUtils, Scheduler Guard, Log.e). Tagging & Push durchgeführt.
- [x] **Release Cycle 0.7.6:** Offline-Optimierung (Join-Flow): Endloser Lade-Spinner bei fehlendem Netzwerk verhindert. Deployment & Tagging durchgeführt.
- [x] **Release Cycle 0.7.5:** NDK-Symbole fixiert (debugSymbolLevel FULL), FLAG_SECURE entfernt.
- [x] **Release Cycle 0.7.3:** Implemented massive offline robustness (timeouts/checks), `FLAG_SECURE` for join codes, fixed NDK debug symbols in CI, and restored compact icon sizes in Settings.
- [x] **Security Enhancement:** Protected join codes from screenshots via `FLAG_SECURE`.
- [x] **Release Cycle 0.7.x:** Launched 0.7.2 (Deep Link intent fix + NDK Symbols), implemented R8 minification, GitHub Actions cache optimizations, and consolidated 0.4.x changelogs.
- [x] **Release Cycle 0.6.x:** Launched 0.6.0 with Drag & Drop member sorting, Offline-UI indicators, and Deep Linking (`familienwecker.de/join`).
- [x] **Security & Perf Audit:** Migrated to `ImmutableList`, improved Material 3 contrasts, fixed SecureRandom generation, and extracted ViewModels via Factory.
- [x] **Localization Audit:** Completed DE/EN translations for all errors and edge-cases (Join Flow, Settings, Firebase).
