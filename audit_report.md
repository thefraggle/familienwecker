# Audit Report: FamWake (iOS vs. Android)

Dieser Bericht vergleicht die Implementierungen der iOS- und Android-Apps. Die Befunde sind nach den sechs Hauptanforderungen strukturiert und in **[Kritisch]**, **[Wichtig]** und **[Best Practice]** priorisiert.

## R1. Parität & Plattform-Optimierung
**Vergleich:** Beide Plattformen halten sich an ihre jeweiligen nativen Design-Richtlinien. Android glänzt mit sauberen Material 3 Composables, während iOS konsequent auf HIG-konforme Elemente (`NavigationStack`, `.sheet`, `.alert`) setzt und unerwünschte Android-Muster (wie BottomSheets) vermeidet.

**Befunde:**
- **[Best Practice] (iOS):** Der FAB für neue Mitglieder wurde explizit wie gewünscht beibehalten. Das Routing (`AppRouter.swift`) orientiert sich an Androids `Routes.kt`, was für einfache States ausreicht. Für tiefere App-Strukturen sollte vermehrt auf `.navigationDestination(for:)` gesetzt werden.

## R2. UI, UX, Layout & Onboarding
**Vergleich:** Das Onboarding ist auf beiden Plattformen ressourcenschonend und nativ gelöst (Android nutzt Lottie & Compose Mockups, iOS nutzt Paging via `TabView(.page)`).

**Befunde:**
- **[Best Practice] (Android):** Exzellente, ressourcenschonende Nutzung von `collectAsStateWithLifecycle` und pure Compose UIs.
- **[Best Practice] (iOS):** Das Paging ist nativ und sehr gut gelöst. In `OnboardingView.swift` werden allerdings Farben teilweise hartkodiert (z. B. `Color(hex: "#0D1B2A")`). Diese sollten idealerweise im Asset Catalog hinterlegt werden oder auf das bestehende `FamWakeTheme` zugreifen.

## R3. Offline-First Ansatz
**Vergleich:** Beide Apps implementieren den Offline-First-Ansatz (`isPersistenceEnabled = true` bei iOS, `PersistentCacheSettings` bei Android). Android geht mit zusätzlichem lokalem Caching via Room noch einen Schritt weiter.

**Befunde:**
- **[Best Practice] (Android):** Vorbildliche Kombination von Firebase Offline-Support und lokalem Caching via Room (`MemberRepository.upsertMember`).
- **[Wichtig] (iOS):** In `FamilyViewModel.swift` (`togglePauseMember` / `setAwake`) fehlt ein UI-Rollback. Wenn die Offline-Operation fehlschlägt, schlägt die UI stumm fehl. Optimistische UI-Updates mit Rollback im `catch`-Block sollten implementiert werden:
```swift
do {
    try await FamilyFirestoreService.shared.setAwake(familyId: fid, memberId: memberId, awake: awake)
} catch {
    self.isAwakeTodayLocal = !awake // Rollback
    self.errorMessage = L.errorGeneric // Graceful Degradation
}
```

## R4. Fehlerhandling
**Vergleich:** Beide Plattformen weisen kleinere Schwächen im Fehlerhandling in Form von potenziellen NullPointerExceptions oder Force Unwraps auf, die behoben werden müssen.

**Befunde:**
- **[Kritisch] (iOS):** Gefährliche Force Unwraps (`!`) in `AddEditMemberView.swift` (Zeile 515) und `AlarmService.swift` (Zeile 112) können zu Abstürzen führen. Ein sicherer Fallback ist erforderlich:
```swift
// AddEditMemberView.swift - Falsch:
get: { profile.leaveHomeTime?.asTime ?? Calendar.current.date(bySettingHour: 8, minute: 0, second: 0, of: Date())! }

// AddEditMemberView.swift - Richtig (Sicherer Fallback):
get: { profile.leaveHomeTime?.asTime ?? Calendar.current.date(bySettingHour: 8, minute: 0, second: 0, of: Date()) ?? Date() }
```
```swift
// AlarmService.swift - Falsch:
sound: finalSoundNameToUse == nil ? .default : .named(finalSoundNameToUse!)

// AlarmService.swift - Richtig:
sound: let soundName = finalSoundNameToUse; soundName != nil ? .named(soundName!) : .default // Besser direkt mit if-let vorher auflösen.
```
- **[Wichtig] (Android):** In `FamilyViewModel.kt` (Zeile 262) wird `lastMemberId!!` verwendet. Da dies in einer Coroutine geschieht, kann der Compiler keinen Smart-Cast anwenden, was crashen kann. Lösung:
```kotlin
val oldId = lastMemberId
if (oldId != null && oldId != newId) {
    alarmScheduler.cancelWakeUp(oldId)
}
lastMemberId = newId
```

## R5. Strings & Übersetzungen
**Vergleich:** Android trennt UI-Texte vorbildlich, während bei iOS noch Optimierungsbedarf bei der Lokalisierung besteht (hartkodierte Texte).

**Befunde:**
- **[Best Practice] (Android):** Sehr saubere und konsequente Auslagerung aller Texte in `strings.xml`. Keine hartkodierten UI-Texte.
- **[Kritisch] (iOS):** Hartkodierte, nicht lokalisierte Strings in `SettingsView.swift` (`Text("Es konnte keine Mail-App gefunden werden.")`, `Text("⏰ Test-Wecker (2 Min)")`) müssen ausgelagert werden:
```swift
// Falsch:
Text("Es konnte keine Mail-App gefunden werden.")
Text("⏰ Test-Wecker (2 Min)")

// Richtig:
Text(String(localized: "error_no_mail_app"))
Text(String(localized: "test_alarm_button"))
```
- **[Wichtig] (iOS):** Die App nutzt das veraltete `.lproj/Localizable.strings` System. Eine Migration auf String Catalogs (`.xcstrings`) wird empfohlen, um Lokalisierungs-Updates besser in Xcode zu visualisieren. Beispiel für die Nutzung:
```swift
// Zugriff über .xcstrings erfolgt exakt wie bei Localizable.strings, aber die Verwaltung ist visuell
Text(String(localized: "key_in_string_catalog"))
```

## R6. Wecker-Logik
**Vergleich:** iOS verfügt über ein äußerst stabiles Setup (`AlarmManager.shared` und AppIntents). Bei Android gibt es Konflikte bei der Audiowiedergabe, dafür ist die Reboot-Resistenz herausragend umgesetzt.

**Befunde:**
- **[Kritisch] (Android):** In `AlarmReceiver.kt` spielen der `NotificationChannel` und die `RingingActivity` parallel Wecktöne ab. Bei der Erstellung des NotificationChannels muss der Sound explizit deaktiviert werden:
```kotlin
val channel = NotificationChannel(dynamicChannelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
    setBypassDnd(true)
    description = channelName
    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
    enableVibration(true)
    // KRITISCH: Explizit auf null setzen, damit nur die RingingActivity den Ton macht!
    setSound(null, null) 
}
```
- **[Wichtig] (Android):** In `AlarmScheduler.kt` wird `FLAG_CANCEL_CURRENT` anstelle von `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE` genutzt, was auf einigen Geräten zu verlorenen Alarmen führen kann.
```kotlin
// Falsch:
val flags = PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE

// Richtig:
val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
```
- **[Best Practice] (Android):** Direct Boot (Reboot Resistenz) über den `BootReceiver` und `DeviceProtectedStorageContext` ist hervorragend gelöst. Der Wecker überlebt Neustarts ohne Entsperrung.
- **[Best Practice] (iOS):** Sehr sauberes, verlässliches Setup über `AlarmManager.shared` und `OpenFamWakeIntent` mit solider Offline-Unterstützung.
