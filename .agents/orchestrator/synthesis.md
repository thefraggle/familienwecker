# FamWake Audit Synthesis

This document synthesizes the findings from the iOS and Android audits.

## R1. Parität & Plattform-Optimierung
- **Android**: Sehr saubere Implementierung mit Material 3 und intelligenten Composables.
- **iOS**: Durchgehend HIG-konform (nutzt `NavigationStack`, `.sheet`, `.alert`). Keine unerwünschten Android-Muster (wie BottomSheet). Das Routing ahmt Androids `Routes.kt` etwas nach, was für simple States okay ist, für tieferes Routing sollte `.navigationDestination(for:)` verwendet werden. Der FAB für neue Mitglieder wurde wie explizit gewünscht beibehalten.

## R2. UI, UX, Layout & Onboarding
- **Android**: Sehr gut. Nutzt `collectAsStateWithLifecycle` und ressourcenschonendes Onboarding (Lottie & Compose Mockups).
- **iOS**: Das Paging im Onboarding via `TabView(.page)` ist nativ und gut gelöst. *Best Practice*: In `OnboardingView.swift` werden Farben hardcodiert via Hex genutzt. Diese sollten im Asset Catalog hinterlegt oder auf das `FamWakeTheme` ausgelagert werden.

## R3. Offline-First Ansatz
- **Android**: Vorbildlich implementiert über `PersistentCacheSettings` in `FirebaseRepository` und lokales Caching via Room (`MemberRepository.upsertMember`).
- **iOS**: `isPersistenceEnabled = true` ist aktiv. *Wichtig*: In `FamilyViewModel.swift` (`togglePauseMember` / `setAwake`) fehlt ein UI-Rollback. Wenn die Offline-Operation fehlschlägt, schlägt die UI stumm fehl. Optimistic UI-Updates mit Rollback im `catch`-Block sollten implementiert werden.

## R4. Fehlerhandling
- **Android**: *Wichtig*: In `FamilyViewModel.kt` (Z. 262) wird `lastMemberId!!` verwendet. Das kann in einer Coroutine crashen, da kein Smart-Cast möglich ist. Ein sicherer Fallback (`if (oldId != null)`) wird benötigt.
- **iOS**: *Kritisch*: Gefährliche Force Unwraps (`!`) in `AddEditMemberView.swift` (Z. 515) und `AlarmService.swift` (Z. 112), die zu Abstürzen führen können. Hier muss ein sicherer Fallback integriert werden.

## R5. Strings & Übersetzungen
- **Android**: Sehr sauber getrennt in `strings.xml`. Keine hartkodierten Texte.
- **iOS**: *Kritisch/Wichtig*: Hartkodierte Strings existieren in `SettingsView.swift` ("Es konnte keine Mail-App gefunden werden.", "⏰ Test-Wecker (2 Min)"). Zudem nutzt die App das veraltete `.lproj/Localizable.strings` System. Es wird die Migration auf String Catalogs (`.xcstrings`) empfohlen.

## R6. Wecker-Logik
- **Android**: *Kritisch*: In `AlarmReceiver.kt` spielen der `NotificationChannel` und die `RingingActivity` parallel Wecktöne ab. Bei der Erstellung des NotificationChannels muss der Sound explizit auf null gesetzt werden (`setSound(null, null)`). *Wichtig*: In `AlarmScheduler.kt` wird `FLAG_CANCEL_CURRENT` statt `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE` genutzt, was zu verlorenen Alarmen führen kann. *Best Practice*: Direct Boot (Reboot Resistenz) ist hervorragend gelöst.
- **iOS**: Sehr sauberes Setup über `AlarmManager.shared` und `OpenFamWakeIntent` (AppIntents), läuft verlässlich offline.
