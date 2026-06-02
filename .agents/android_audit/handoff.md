# Android Audit Report

## 1. Observation
- `app/src/main/java/de/familienwecker/famwake/alarm/AlarmReceiver.kt` (Z. 70): NotificationChannel wird mit `.setSound(soundUri, ...)` konfiguriert, obwohl `RingingActivity` einen eigenen `MediaPlayer` startet.
- `app/src/main/java/de/familienwecker/famwake/alarm/AlarmScheduler.kt`: Setzt PendingIntents mit `FLAG_CANCEL_CURRENT` sowohl bei `schedule` als auch bei `cancelWakeUp`.
- `app/src/main/java/de/familienwecker/famwake/alarm/BootReceiver.kt` und `AlarmBackupPrefs.kt`: Nutzen korrekt `createDeviceProtectedStorageContext()` und `directBootAware`.
- `app/src/main/java/de/familienwecker/famwake/ui/viewmodel/FamilyViewModel.kt` (Z. 262): Verwendet `lastMemberId!!`, was zu NullPointerExceptions führen könnte.
- UI verwendet `collectAsStateWithLifecycle()` und Lottie/Mockups im Onboarding.

## 2. Logic Chain
- Wenn die Notification einen Sound hat und die `RingingActivity` gleichzeitig startet, überschneiden sich die Töne (Doppelton), was den Wecker fehlerhaft klingen lässt. Der Kommentar besagt, dass Doppelton vermieden werden soll, aber der Channel wird trotzdem mit Sound erstellt.
- `lastMemberId!!` in einer Coroutine ist unsicher, da die `var` von einem anderen Thread in der Zwischenzeit modifiziert werden könnte. Besser ist eine lokale Konstante.
- Die Nutzung von Device Protected Storage stellt sicher, dass der Wecker auch nach einem nächtlichen Neustart ohne PIN-Eingabe funktioniert (Offline/Direct Boot).

## 3. Caveats
- Die Funktionalität von `BootReceiver` kann auf bestimmten Hersteller-ROMs (Xiaomi, Huawei) durch agressive Battery-Saver trotzdem blockiert werden, auch wenn die Implementation korrekt ist.

## 4. Conclusion
Die Android-App ist architektonisch sehr solide gebaut (KMP, Compose, Offline-First). Es gibt einen kritischen Bug in der Audio-Wiedergabe des Weckers und kleinere Verbesserungen bei der Nutzung von Kotlin (`!!`) und PendingIntent-Flags.

## 5. Verification Method
- Code-Inspektion der bemängelten Zeilen (`AlarmReceiver.kt`, `FamilyViewModel.kt`).
- Kompilieren der App und Testen des Alarms bei gesperrtem Bildschirm.
