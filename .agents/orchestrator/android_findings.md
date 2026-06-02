# Android Findings

### 🔴 Kritisch
**R6. Wecker-Logik (Doppelter Alarm-Ton):**
In `AlarmReceiver.kt` wird der `NotificationChannel` mit einem Sound konfiguriert (`setSound(soundUri, ...)`). Gleichzeitig startet die `RingingActivity` ihren eigenen `MediaPlayer`. Dadurch spielen beide parallel ab. Der Code-Kommentar suggeriert, dass `.setSound()` auf dem Builder weggelassen wurde, aber Android spielt den Ton des Channels ab!
*Lösung in `AlarmReceiver.kt`:*
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

### 🟡 Wichtig
**R4. Fehlerhandling (NullPointerException-Risiko):**
In `FamilyViewModel.kt` (Zeile 262) wird `lastMemberId!!` verwendet. Da `lastMemberId` eine veränderbare Variable in einer Coroutine ist, kann der Compiler keinen Smart-Cast anwenden. Dies kann crashen.
*Lösung in `FamilyViewModel.kt`:*
```kotlin
val oldId = lastMemberId
if (oldId != null && oldId != newId) {
    alarmScheduler.cancelWakeUp(oldId)
}
lastMemberId = newId
```

**R6. PendingIntent Flags (`AlarmScheduler.kt`):**
Das Verwenden von `FLAG_CANCEL_CURRENT` beim Setzen des Alarms kann auf manchen Geräten zu verlorenen Alarmen führen, wenn das System überlastet ist. Bei `cancelWakeUp` sollte man den Intent nur abrufen, nicht neu kreieren.
*Lösung:* Nutze `PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE` in beiden Funktionen (`scheduleWakeUp` und `cancelWakeUp`).

### 🟢 Best Practice
**R1 & R2. Parität & Compose UI:**
Sehr saubere Implementierung! Nutzt `collectAsStateWithLifecycle`, Material 3, und intelligente Composables. Das Onboarding (Lottie & pure Compose Mockups) ist ressourcenschonend und exzellent umgesetzt.

**R3. Offline-First Ansatz:**
Vorbildlich implementiert über `PersistentCacheSettings` in `FirebaseRepository` und das lokale Caching via Room `MemberRepository.upsertMember`. Einzige kleine Auffälligkeit: `claimMemberOffline` existiert im Repo, wird aber im ViewModel anscheinend nicht genutzt (die Transaktion via Firestore schlägt im Offline-Modus fehl, was beim "Claiming" aber meistens auch gewollt ist, um Race-Conditions zu vermeiden).

**R5. Strings & Übersetzungen:**
`strings.xml` ist sauber getrennt. UI Components enthalten keine hardcodierten Texte. Perfekte Nutzung von `UiText` für ViewModels. 

**R6. Direct Boot (Reboot Resistenz):**
Hervorragend gelöst! `BootReceiver` nutzt `directBootAware` und das `AlarmBackupPrefs` schreibt sicher in den `DeviceProtectedStorageContext()`. Der Wecker überlebt also einen Neustart ohne Entsperrung.
