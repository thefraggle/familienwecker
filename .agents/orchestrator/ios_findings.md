# iOS Findings

### Kritisch
**R4. Fehlerhandling (Force Unwrapping)**
Es gibt potenziell gefährliche Force Unwraps (`!`), die zu App-Abstürzen führen können:
- `AddEditMemberView.swift` (Zeile 515):
```swift
// Falsch:
get: { profile.leaveHomeTime?.asTime ?? Calendar.current.date(bySettingHour: 8, minute: 0, second: 0, of: Date())! }

// Richtig (Sicherer Fallback):
get: { profile.leaveHomeTime?.asTime ?? Calendar.current.date(bySettingHour: 8, minute: 0, second: 0, of: Date()) ?? Date() }
```
- `AlarmService.swift` (Zeile 112):
```swift
// Falsch:
sound: finalSoundNameToUse == nil ? .default : .named(finalSoundNameToUse!)

// Richtig:
sound: let soundName = finalSoundNameToUse; soundName != nil ? .named(soundName!) : .default // Besser direkt mit if-let vorher auflösen.
```

**R5. Strings & Übersetzungen (Hardcoded Texts)**
In `SettingsView.swift` existieren nicht lokalisierte, hartkodierte Strings:
- `Text("Es konnte keine Mail-App gefunden werden.")`
- `Text("⏰ Test-Wecker (2 Min)")`

### Wichtig
**R5. String Catalogs (.xcstrings)**
Die App verwendet noch das veraltete `.lproj/Localizable.strings` System (gefunden in `Resources/de.lproj`, etc.).
*Vorschlag*: Migration auf `.xcstrings` (String Catalogs), um Pluralisierungen und Lokalisierungs-Updates (z. B. fehlende Keys) in Xcode direkt zu visualisieren.

**R3. Offline-First Ansatz**
`isPersistenceEnabled = true` ist aktiv (gut!). Aber in `FamilyViewModel.swift` (`togglePauseMember` / `setAwake`) gibt es den Ansatz: `// NO ROLLBACK: Vertraue auf Firestores Offline-Queue.`. Wenn die App jedoch den Cache leert oder offline eine Berechtigung fehlt, schlägt die UI stumm fehl.
*Vorschlag*: Optimistic UI-Updates mit Rollback im `catch`-Block implementieren:
```swift
do {
    try await FamilyFirestoreService.shared.setAwake(familyId: fid, memberId: memberId, awake: awake)
} catch {
    self.isAwakeTodayLocal = !awake // Rollback
    self.errorMessage = L.errorGeneric // Graceful Degradation
}
```

### Best Practice
**R2. UI, UX & Onboarding (SwiftUI Best Practices)**
- In `OnboardingView.swift` werden Farben hardcodiert via Hex genutzt (z. B. `Color(hex: "#0D1B2A")`). Diese sollten idealerweise im Asset Catalog hinterlegt werden oder auf das bestehende `FamWakeTheme` zugreifen.
- Das Paging via `TabView(.page)` ist nativ und sehr gut gelöst.

**R1. Parität & Plattform-Optimierung**
- `NavigationStack`, `.sheet` und `.alert` werden durchgehend HIG-konform genutzt. Keine unerwünschten Android-Muster (`BottomSheet`).
- Der FAB für neue Mitglieder wurde explizit via Ausnahme beibehalten.
- Das Routing (`AppRouter.swift`) ahmt Androids `Routes.kt` etwas nach, was für simple States (Login -> Main) ausreicht. Für tiefere App-Strukturen sollte vermehrt auf `.navigationDestination(for:)` gesetzt werden.

**R6. Wecker-Logik**
Das Setup über `AlarmManager.shared` und `OpenFamWakeIntent` (AppIntents) ist sauber gelöst und läuft verlässlich offline. 
