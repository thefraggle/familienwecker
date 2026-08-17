# FamWake – Tech Stack & Guidelines

## Projekt-Regeln (Cross-Platform)
- **1:1 Parität:** Behandle die iOS- und Android-Codebasen mit strikter 1:1 Parität. Jede angeforderte Änderung gilt grundsätzlich für beide Plattformen.
- **Umsetzung & Rückfragen:** Setze unkritische oder eindeutige Änderungen direkt und spezifisch für beide Plattformen (iOS und Android) um. Bei Unsicherheiten, ob eine Anpassung nur für eine Plattform gedacht ist, musst du vorher nachfragen.
- **Push & Deployment:** Push zu GitHub nur nach ausdrücklicher Aufforderung. Tags setzen ebenfalls nur nach Aufforderung. **Tags IMMER mit `v`-Prefix** (z.B. `v1.9.16`), sonst triggert GitHub Actions nicht.
- **Commits:** Lokaler Commit nach jeder (erfolgreich umgesetzten) Änderung.
- **Kein Halluzinieren:** Bei Unsicherheit zwingend nachfragen, keine ungeprüften Annahmen treffen.
- **Issue-basiertes Arbeiten:** Es werden keine lokalen Feature-Backlogs oder ungepflegten Roadmap-Listen geführt. Die gesamte Weiterentwicklung wird ab sofort über GitHub Issues nachverfolgt.

## Server / Infrastruktur
- Firebase Hosting + Firestore + Auth + Cloud Functions (Node.js 22)
- Firebase CLI Deploy: `npx --package firebase-tools firebase deploy` (aus Projekt-Root, kein globales Install nötig)
- Android: Kotlin, Jetpack Compose
- iOS: Swift, SwiftUI, Branch `dev/ios-port`
- iOS Info.plist: MUST be at `/ios/Info.plist` (outside FamWake/ folder – PBXFileSystemSynchronizedRootGroup auto-copies everything inside → "Multiple commands produce Info.plist")

## iOS Key Files
| Datei | Zweck |
|---|---|
| `L.swift` | Zentrale Lokalisierungsschnittstelle |
| `LanguageManager.swift` | Bundle-Switching bei Sprachwechsel |
| `AppState.swift` | App-weiter State (Route, Theme, Sprache) |
| `AppRouter.swift` | Navigation via `appState.route` |
| `FamilyViewModel.swift` | Haupt-VM für Firestore-Daten |
| `Scheduler.swift` | KMP-Weckplan-Logik |

## Lokalisierung
- Android XML → iOS `.strings`: 1:1 Mapping
- Keys immer identisch in allen 20 Sprachen (17 main + 3 dialects)
- Neue Keys: erst in `values/strings.xml` anlegen, dann alle anderen Sprachen
- CJK Fonts in Generatoren: JA=Hiragino, KO=AppleSDGothicNeo, ZH=STHeiti

## Naming Conventions
- L-Keys: `snake_case`, Prefix = Bereich (`settings_`, `main_`, `onboarding_`, ...)
- ViewModels: `FamilyViewModel`, `AuthViewModel`, `DonationViewModel`
- Views: `SettingsView`, `MainView`, `MemberCardView`, `OnboardingView`

## Anti-Pattern Log (Fixed Critical Bugs)
| Date | Issue | Solution |
|---|---|---|
| 2026-03-31 | Play Console warnt unter Android 15 vor `setStatusBarColor` / Edge-To-Edge | Veraltete `WindowCompat`-Aufrufe entfernt. `enableEdgeToEdge()` dynamisch in Compose `Theme.kt` via `SystemBarStyle` aufrufen. |
| 2026-03-27 | Unicode-Escape `\u00e7` in Swift-String verursacht compile-error | Echte UTF-8-Zeichen direkt in Datei schreiben (ç statt \u00e7) |
| 2026-03-27 | Language-Picker zeigte Sprachnamen in App-Sprache statt Eigensprache | Native Names hart kodiert als String-Literale im languagePicker |
| 2026-04-01 | Android-App erzwingt deutsche Sprache auf nicht-unterstützten Systemen (z.B. Türkisch) | KMP AppSettings-Default von `de` auf `system` geändert und in `MainActivity` bei `system` den `LocaleListCompat` geleert, um den natürlichen Android-Fallback (`strings.xml` = Englisch) zu triggern. |
| 2026-04-06 | `feedback`-Collection erlaubte `update` + `delete` für alle eingeloggten Nutzer | Firestore-Rule auf `create: if signedIn()` eingeschränkt; `update/delete` nur noch für Admins. |
| 2026-04-06 | `sendFeedbackEmail` CF hatte keinen Auth-Check (unauthentifizierte Aufrufe möglich) | `if (!request.auth) throw HttpsError("unauthenticated")` als erstes Statement hinzugefügt. |
| 2026-04-06 | `leaveFamily` akzeptierte beliebige Client-`memberId` ohne Eigentumsprüfung | `claimedByUserId`-Check vor dem Delete: nur eigene Member-Dokumente dürfen gelöscht werden. |
| 2026-04-06 | Hardcodierte Admin-E-Mail im Rate-Limit-Bypass (`checkEmailRateLimit`) | Zeilen entfernt; `_admins`-Collection-Bypass ist ausreichend und sicher. |
| 2026-04-17 | TelemetryDeck `donutChart` blendet sich aus wenn nur 1 Datenpunkt vorhanden oder Signal noch keine Daten hat | Immer `barChart` verwenden für topN-Insights mit Breakdown — zuverlässig auch bei leerem/einzelnem Wert. |
| 2026-04-17 | `alarm.snoozed` + `alarm.dismissed` nie gesendet: RingingActivity ruft direkt `alarmScheduler` auf, bypassed ViewModel | Beide Signals direkt in `RingingActivity.onStopClicked` / `onSnoozeClicked` senden — nicht im ViewModel verlassen. |
| 2026-04-18 | "Schon wach" Button disabled obwohl Alarm in <2h: Day-Filter prüfte `earliestWakeUp` (z.B. 06:00), obwohl Alarm erst um 08:30. Inkonsistent mit `resolveEffectiveMember()` | Filter auf `latestWakeUp` umgestellt (konsistent mit resolveEffectiveMember). 2h-Fenster nutzt tatsächliche Schedule-Zeit statt earliestWakeUp. |
| 2026-04-18 | Nach Logout+Login fehlte eigener Member im Weckplan: parallele `scope.launch` Coroutinen in `recalculateSchedule()` mit veraltetem `alarmsOn`-State überlebten und überschrieben korrekte Ergebnisse | Cancel-and-replace via `scheduleJob?.cancel()` vor neuem `scheduleJob = scope.launch { }`. Zusätzlich: `alarmStateBeforeLogout` VOR `clearAll()` sichern. |
| 2026-04-18 | Schedule zeigte "morgen" (inkl. Frühstück) obwohl Members heute noch geweckt werden: `resolveEffectiveMember` wechselte per-Member zu morgen wenn `now > latestWakeUp`, auch wenn Bathroom-Scheduling den Alarm auf eine spätere Zeit geschoben hat | Two-Pass in `recalculateSchedule`: erst alle Members für heute auflösen, Schedule berechnen, nur zu morgen wechseln wenn ALLE geplanten Alarme verstrichen. `FamilySchedule.targetDate` steuert UI-Datum statt Heuristik. |
| 2026-04-18 | `removeMember()` setzte `setMyMemberId(null)` synchron außerhalb des Coroutine-Scopes – bei fehlgeschlagener Firestore-Deletion wurde lokaler State inkonsistent | `wasMyMember` vor `scope.launch` lesen, `setMyMemberId(null)` nur INNERHALB des Coroutine nach `result.isSuccess` aufrufen. |
| 2026-04-18 | `ReviewHelper.kt` hatte 4 `Log.*`-Aufrufe ohne `BuildConfig.DEBUG`-Guard – Logs landeten in Release-Builds | Alle 4 Aufrufe mit `if (BuildConfig.DEBUG)` umwickelt. Pattern: JEDE neue Log-Stelle MUSS einen Guard haben. |
| 2026-04-18 | ISO 639 `ksh` = Kölsch/Ripuarisch, nicht Ruhrpott. Strings hatten Kölsch-Spillover ("fählen", "d'n", "nit", "müjjelesch") | Alle ksh-Strings auf echtes Ruhrpott korrigiert ("nich", "dat", "wa", "auffe"). Bei Dialekten immer gegen ISO-Code prüfen! |
| 2026-04-18 | 5 Strings in bis zu 10 Sprachen waren nach Hinzufügen auf Englisch verblieben (error_offline_feedback, error_network etc.) | Python-Script für Batch-Übersetzung + xmllint-Validierung als QA-Gate. Immer `grep -rn "Please\|Network error" app/src/main/res/` nach String-Änderungen laufen lassen. |
| 2026-04-18 | Onboarding-Invite-Mockup zeigte hardcoded "Mustermann" statt lokalisiertem Familiennamen | Neuen String `onboarding_mock_family_name` in allen 18 Sprachen angelegt (Smith/García/Rossi/田中 etc.). Pattern: KEIN hardcoded Text in Mockup-Composables! |
| 2026-04-19 | `deleteFamily` + `createFamily` zeigten falsch `error_sync_failed`: Firestore Snapshot-Listener (membersJob) bekam `PERMISSION_DENIED` BEVOR `familyId=null` den Job canceln konnte (Race Condition). Bei delete: Docs weg → Listener crasht. Bei create: Security Rules sehen `users/{uid}.familyId` noch nicht. | 1) `stopSyncJobs()` VOR dem `deleteFamily` Cloud-Function-Call. 2) `_errorMessage.value = null` nach Erfolg in beiden Methoden. 3) PERMISSION_DENIED im membersJob-Catch unterdrücken (ist reguläres Family-Transition-Event, kein echter Fehler). |
| 2026-04-19 | `leaveAndJoinPendingCode` hatte gleiche Race Condition: `joinFamilyByCode` ändert `users/{uid}.familyId` serverseitig auf neue Familie → Security Rules verweigern alten Listener sofort PERMISSION_DENIED | `stopSyncJobs()` VOR dem CF-Call einfügen. Pattern: JEDE Methode die eine Cloud Function aufruft die `users/{uid}.familyId` ändert, MUSS vorher `stopSyncJobs()` aufrufen. |
| 2026-04-19 | `retryWhen { true }` in getFamilyMembersFlow/getSyncStatusFlow/checkIsGlobalAdminFlow retried endlos bei PERMISSION_DENIED (permanenter Fehler bei Familien-Löschung/-Wechsel) | PERMISSION_DENIED → sofort `false` (kein Retry). Max 5 Retries für transiente Fehler. Pattern: Endlos-Retries NIE bei Auth/Permission-Fehlern! |
| 2026-04-20 | „Rechenfehler: ed4" nach Member-Anlegen: `scheduleJob?.cancel()` in `recalculateSchedule()` warf `CancellationException`, die im generischen `catch (e: Exception)` als User-Fehler angezeigt wurde | `catch (e: CancellationException) { throw e }` VOR `catch (e: Exception)`. Pattern: In JEDER Coroutine mit `catch(Exception)` muss `CancellationException` separat rethrown werden! |
| 2026-04-20 | `android.builtInKotlin=false` + `android.newDsl=false` entfernt weil als \"deprecated\" fehlinterpretiert → Kotlin-Extension-Konflikt + BaseExtension-Cast-Fehler | Beide Flags NICHT entfernen ohne vorherige DSL-Migration. \"deprecated\" = \"muss vor AGP 10.0 migriert werden\", nicht \"jetzt entfernen\"! |
| 2026-04-21 | Notification permission Toggle (`Switch` + `RequestPermission`) funktioniert nicht auf Samsung/Android 13+: System zeigt Permission-Dialog nur EINMAL, danach ist `launch(permission)` wirkungslos | Toggle NIE für POST_NOTIFICATIONS verwenden. Stattdessen Warning Card mit `ACTION_APP_NOTIFICATION_SETTINGS` Intent → funktioniert immer. Lifecycle-Observer zum Re-Check nach Rückkehr aus System-Settings. |
| 2026-04-22 | Shortcut zu System-Notification-Settings erlaubt User "Ton und Vibration" zu deaktivieren → Samsung deaktiviert Full-Screen-Intent → Alarm klingelt NICHT MEHR, nur stille Notification im Tray | KEINEN Shortcut zu ACTION_APP_NOTIFICATION_SETTINGS anbieten. Der Alarm ist die Kernfunktion – Risiko dass User versehentlich Wecker killt ist zu hoch. |
| 2026-04-22 | `requestDismissKeyguard()` + `FLAG_DISMISS_KEYGUARD` zeigen PIN/Fingerprint-Dialog ÜBER der RingingActivity auf Samsung-Geräten | NUR `setShowWhenLocked(true)` + `setTurnScreenOn(true)` + `FLAG_SHOW_WHEN_LOCKED` + `FLAG_TURN_SCREEN_ON` + `FLAG_KEEP_SCREEN_ON` verwenden. Wecker muss OHNE Entsperren bedienbar sein. |
| 2026-04-22 | Push-Notification-Channels (schedule_change, family_events) mit IMPORTANCE_HIGH/DEFAULT spielen Ton → kann User bei häufigen Änderungen nerven und zur Deaktivierung aller Notifications motivieren | Push-Channels auf IMPORTANCE_LOW setzen (still, aber sichtbar). Nur der Alarm-Channel (ALARM_CHANNEL_S_*) bleibt IMPORTANCE_HIGH. ACHTUNG: Android cached Channel-Importance nach erster Erstellung → bestehende Installs brauchen App-Reinstall. |
| 2026-04-22 | Push-Reorder-Filter `[min,max]` benachrichtigte nur Members im betroffenen Positions-Bereich. Aber: Familien-Schedule wird rückwärts berechnet → JEDE Änderung betrifft alle. User wollen auch bei nicht-eigener Betroffenheit eingreifen können | KEINEN Positions-Filter bei Push-Benachrichtigungen verwenden. Immer ALLE geclaimten Members benachrichtigen (außer Sender). Sender-Erkennung via `pushMeta/reorder` (15s Window) oder `claimedByUserId` (Status-Änderungen) beibehalten. |
| 2026-04-22 | Fresh Install vs. Logout/Login bei Alarm-Wiederherstellung nicht unterscheidbar: `alarmStateBeforeLogout` Default `false` = identisch mit "User hatte bewusst OFF" | `hasAlarmStateBeenSaved()` prüft ob SharedPrefs-Key `ALARM_STATE_BEFORE_LOGOUT` existiert. Key fehlt = Fresh Install → Default ON. Key vorhanden = Logout → gespeicherten Wert exakt übernehmen. |
| 2026-04-23 | Email Rate-Limit: reset/verify/confirm auf demselben Counter → Verifikations-Mails fressen das Reset-Limit | `checkEmailRateLimit(email, type)` mit Typ-Prefix: `reset_`, `verify_`, `confirm_`. Je unabhängig 5/h + 10/d. |
| 2026-04-23 | Firebase compat SDK `httpsCallable()` schlägt still fehl wenn Nginx CSP `connect-src` fehlt für `cloudfunctions.net` | CSP um `https://*.cloudfunctions.net` ergänzen. Im HTML `fetch()` direkt statt compat SDK (robuster). |
| 2026-04-23 | Fix-Script hat `pageLang` injiziert, aber Server-HTML nutzt bereits `authLang` → undefined → silenter ReferenceError | Immer existierende Sprachvariable in Zieldateien prüfen bevor Fixes scripten. Live-HTML von Server fetchen, nicht nur lokale Kopie prüfen. |
| 2026-04-25 | `locales_config.xml` hatte nur 5 von 18 Sprachen → Android 13+ filtert `setApplicationLocales()` gegen diese Liste → alle nicht gelisteten Sprachen fielen auf EN zurück | `locales_config.xml` MUSS alle Sprachen aus `SUPPORTED_LANGUAGE_CODES` enthalten. Bei neuen Sprachen immer beide Stellen synchron aktualisieren. |
| 2026-04-25 | `git push origin main --tags` sendet Branch + Tag zusammen → GitHub Actions Workflow-Trigger für Tag geht verloren | Tag-Push IMMER separat: erst `git push origin main`, dann `git push origin vX.Y.Z` |
| 2026-04-28 | GitHub Actions: `actions/cache@v4` etc. laufen auf Node.js 20 (deprecated), Warning trotz `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true` | Die Env-Variable erzwingt nur die Runtime, nicht die Action selbst. Warning verschwindet wenn Action-Anbieter auf Node 24 updaten. Kein Handlungsbedarf. |
| 2026-04-28 | `@StringRes` auf Constructor-Parameter ohne `@param:` Prefix → Kotlin warnt über zukünftige Annotation-Target-Änderung | Immer `@param:StringRes` statt `@StringRes` bei Value-Parametern in Klassen. |
| 2026-05-01 | Play Store Grafiken zeigten "Tofu" (Rechtecke) für `hi, mr, bn` weil Standard-Font `InterTight` keine Devanagari/Bengali-Zeichen enthält | Native macOS-Systemschriften (`Devanagari Sangam MN`, `Bangla Sangam MN`) als Fallback via Font-Index in Generator-Skripten laden. |
| 2026-05-01 | Text in Screenshots (z.B. Vietnamesisch) war rechts abgeschnitten, da `draw.text` in PIL keinen automatischen Zeilenumbruch mit Textskalierung macht | Dynamische Skalierungslogik in `generate_screenshots.py` implementiert: `scale = max_w / max_text_w`, Font-Größe und Zeilenabstand proportional reduzieren wenn `scale < 1.0`. |
| 2026-05-01 | `deep-translator` crasht bei `zh` (für `zh-CN`), da es exakt `zh-CN` erwartet, aber das Skript `split('-')[0]` nutzt | In `generate_metadata.py` ein explizites Fallback-Mapping in `LANG_CODE_MAP` hinzugefügt (`'zh': 'zh-CN'`), analog zu Norwegisch `nb` -> `no`. |
| 2026-05-04 | App stürzt ab (MissingFormatArgumentException), weil in Fehler-Flows (z.B. INTERNAL SSL-Error) der Platzhalter %1$s nicht befüllt wurde | Bei `UiText.StringResource` immer sicherstellen, dass Argumente (`error.localizedMessage ?: ""`) für Strings mit Platzhaltern übergeben werden. |
| 2026-05-04 | Onboarding-Lottie-Animationen eskalieren auf Tablets / Querformat und verdecken Buttons | Maximalhöhe via `Modifier.heightIn(max = ...)` erzwingen und globale App-Orientierung in `AndroidManifest.xml` auf `portrait` fixieren. |
| 2026-05-04 | Mehrfaches Klicken auf Onboarding-Start-Button löst unkontrolliertes Verhalten aus | Lokalen `isStarting` State verwenden, um UI-Events (wie Firebase Auth Calls) zu blockieren. |
| 2026-05-04 | Löscht der Admin die Familie, laufen andere Clients offline weiter und Alarme klingeln weiterhin. | `PERMISSION_DENIED` im Snapshot-Listener abfangen (wenn `membersJob` nicht explizit gestoppt wurde) und sofort lokales Cleanup erzwingen (`appSettings.clear`, Alarme canceln). |
| 2026-05-04 | Wecker-Schalter geht sofort nach Auto-Claim wieder aus: `addOrUpdateMember` und `claimMember` erzeugten zwei Writes (unclaimed -> claimed), was lokal den "Gestohlen"-Schutz auslöste. | Auto-Claim direkt in `addOrUpdateMember` integrieren (Atomic Write: Profil sofort als claimed speichern). Verhindert Race Condition durch Zwischen-Snapshots. |
| 2026-05-04 | `catch (e: Exception)` in `AuthViewModel` verschluckte `CancellationException` bei Coroutine Cancel | Immer `catch (e: CancellationException) { throw e }` VOR `catch (e: Exception)` einfügen (Projektrichtlinie bekräftigt). |
| 2026-05-04 | `settings_anonymous_login_button` war in einigen XMLs doppelt definiert und brach den Build | Nach manuellen Übersetzungs-Updates immer Compile ausführen und bei Duplicate-Fehlern XMLs bereinigen. |
| 2026-05-06 | Nach Neuinstall+Login wurde Profil automatisch geclaimt obwohl neues Gerät (neue deviceId): `AuthViewModel.restoreUserFamily` prüfte nicht gegen deviceId | `claimedByDeviceId == appSettings.deviceId \|\| claimedByDeviceId == null` als Guard in restoreUserFamily. Pattern: Profil-Auto-Assign IMMER gegen aktuelle deviceId prüfen! |
| 2026-05-06 | Weckplan nach manuellem Claim leer ("Keine aktiven Wecker") obwohl Schalter AN: `claimMember()` setzte `deviceAlarmEnabled` nicht → `recalculateSchedule` filterte Member raus | `deviceAlarmEnabled = true` explizit in Room-Upsert, Firestore-Transaction UND claimMemberOffline setzen. Pattern: Claim muss IMMER alle alarm-relevanten Felder atomar mitsetzen! |
| 2026-05-13 | iOS: Info.plist in FamWake/ Ordner → "Multiple commands produce Info.plist" weil PBXFileSystemSynchronizedRootGroup es als Resource UND als INFOPLIST_FILE verarbeitet | Info.plist MUSS außerhalb des synchronisierten Ordners liegen (`/ios/Info.plist`). GENERATE_INFOPLIST_FILE=NO + alle Bundle-Keys manuell. |
| 2026-05-13 | iOS: Google OAuth crash "missing URL scheme" | REVERSED_CLIENT_ID aus GoogleService-Info.plist MUSS als CFBundleURLSchemes in Info.plist registriert sein. |
| 2026-05-13 | iOS: "credential already associated with different user" bei Login mit existierendem Account von Android | `link(with:)` fängt `credentialAlreadyInUse` ab → Fallback auf `signIn(with:)`. Pattern: Lazy Registration MUSS immer credentialAlreadyInUse behandeln! |
| 2026-05-13 | iOS: Nach Login alte Daten vom anonymen Account sichtbar | `reloadForNewUser()`: stopSyncJobs + clearFamilyLocally + getUserContext CF aufrufen. Pattern: JEDER Auth-Wechsel braucht Data-Reload! |
| 2026-05-18 | iOS: TestFlight Export Compliance Dialog bei jedem Upload blockiert Freigabe | `ITSAppUsesNonExemptEncryption = false` in `ios/Runner/Info.plist` (bzw. `/ios/Info.plist`) eintragen, da die App nur Standard-HTTPS nutzt. |
| 2026-05-19 | Android/iOS: Leave time validation error when set exactly to bathroom end time due to strict `<` / `!isAfter` checks. | Update validation checks to `<` / `isBefore` to allow the time to leave the house to be exactly the same as the end of the bathroom time. |
| 2026-05-20 | Android/iOS: Ghost alarms and unclaim sync issues when a member profile was deleted, stolen, or unclaimed. | Added reactive `myMemberId` observers to trigger `cancelWakeUp` on change/clear. Corrected list sync handler to clear claim locally if member no longer exists. Aligned UI "Kein Profil" / "No Profile" unclaim transaction on both platforms. |
| 2026-05-20 | iOS: Onboarding mockup card bottom fade overlay height stretch bug. | Limited gradient height to 48 instead of using minHeight(48) which stretched card content. |
| 2026-05-20 | iOS: Empty schedule card on main screen after first member add/edit. | Added explicit recalculateSchedule() call inside the MainActor block of addOrUpdateMember() in FamilyViewModel.swift. |
| 2026-05-20 | iOS: Type-casting crash / failure parsing dayProfiles from Firestore. | Refactored parseDayProfiles in FirestoreMapper.swift to use [String: Any] instead of [String: [String: Any]] for safer deserialization. |
| 2026-05-20 | iOS CI/CD: compile-time symbol mismatch error 'Function use can not be called: No function found for symbol androidx.sqlite/use' | Downgraded sqlite version from 2.5.0-alpha13 to 2.5.0-alpha11 in gradle/libs.versions.toml to align it with room version 2.7.0-alpha11, resolving the binary compatibility conflict at link-time. |
| 2026-05-20 | iOS CI/CD: xcodebuild -showBuildSettings timed out after 4 retries | Set FASTLANE_XCODEBUILD_SETTINGS_TIMEOUT=120 environment variable in GitHub Actions workflow to give Xcode ample time to resolve and load build settings (especially with heavy Swift Package dependencies). |
| 2026-05-20 | iOS CI/CD: "No profile matching 'FamWake TestFlight' found" due to incomplete keychain list & missing codesign tool in partition list | Appended temporary keychain to existing user keychains instead of overwriting, and added codesign: to set-key-partition-list to allow xcodebuild to access the private key without prompt. |
| 2026-05-21 | iOS: Kaltstart-Absturz (Boot-Loop) durch Notification-Klick | Alarm-Events werden in `AppState` gepuffert, falls die Route noch `.loading` ist, und erst 300ms nach Laden der stabilen Ziel-Route per MainActor abgespielt. |
| 2026-05-22 | iOS: Start-Absturz durch RevenueCat/Firebase init timing | `donationViewModel` (@StateObject in `FamWakeApp`) greift beim App-Start auf `Purchases.shared` zu. Da SwiftUI `@StateObject`s *vor* `AppDelegate.didFinishLaunchingWithOptions` initialisiert, führt dies zum Absturz. Lösung: SDK-Initialisierung (`FirebaseApp.configure()`, `RevenueCatService.configure()`) direkt in `FamWakeApp.init()` verschoben. |
| 2026-05-22 | Android: In-App Review Timing & Triggers | Prompts störten morgens oder erschienen unkontrolliert. Lösung: Timing auf 3/9 Tage angepasst (mit 5 Tagen Mindestabstand), Time-Guard für 06:00-09:00 Uhr eingebaut und Trigger auf Back-Aktionen in Settings sowie Member-Speichern eingeschränkt. |
| 2026-05-22 | iOS: Modal sheet close buttons inconsistency | Replaced text back buttons / circle xmarks with standard top-left "xmark" (without circle) in SettingsView, FeedbackView, and LoginView to conform to iOS HIG. |
| 2026-05-22 | iOS: Button contrast issues in dark/light mode | Replaced `.borderedProminent` buttons with custom styled `Button` using explicit label foreground (`theme.onPrimary`) and background (`theme.primary`) combined with `BounceButtonStyle()` in FamilySetup, Login, Feedback, Settings. |
| 2026-05-23 | Android: Auto-Fix deleted other weekday profiles | `applyAutoFix()` used `s.member` which only had target day profile due to `resolveEffectiveMember` mapping, instead of originalMember from `_members.value`, thereby overwriting other profiles in database. Fixed by fetching `originalMember` first. |
| 2026-05-23 | iOS: Invalid schedule tiles rendered & bounds fix | Checked `sched.isValid` before rendering tiles, warning tooltips, and unclaimed warnings in MainView.swift to match Android. Also added missing `newLatest < newEarliest` bounds safety in Swift `applyAutoFix()`. |
| 2026-05-25 | Cross-Platform: Buffer, reordering & iOS push delivery | Centered iOS main buffer display by grouping each card and buffer inside a `VStack` as a single List Row (avoiding default List spacing). Persisted global `sequenceOrder` in Firestore update payloads to fix weekly reorder resets. Modified functions `index.js` to send native high-priority APNs alerts to prevent Apple from dropping iOS silent pushes, and added duplicate banner checks in `FamWakeApp.swift`. |
| 2026-05-25 | Android: Centering buffer line and atomic reorder batch | Centered Android buffer divider line using Box instead of HorizontalDivider in `MainScreen.kt`, and set `top = 16.dp` and `bottom = 0.dp` padding on the buffer Row to offset the LazyColumn's `verticalArrangement = spacedBy(16.dp)` gap. Grouped `dayProfiles` and `sequenceOrder` into a single batch update to prevent snapshot listener race conditions from resetting UI. |
| 2026-05-26 | Cross-Platform: Bathroom buffer logic bug when global=0m | Fixed bug where individual bathroom buffers were applied to the successor rather than the predecessor in the Scheduler backward loop. Refactored the loop to index and subtract predecessor's buffer (`prevBuffer`) from `wakeUpTime`, and set `bufferAfter` to the current member's buffer (0 if last member). |
| 2026-08-08 | iOS: Lokalisierungs-Helper `L.s` bei String Catalogs fehlerhaft | Umstellung auf `String(localized:table:bundle:locale:comment:)`. Behebt den Fallback auf Englisch bei Simulator-Screenshots, da Xcode 15 String Catalogs keine physischen `.lproj`-Verzeichnisse im Bundle mehr anlegen. |
| 2026-08-08 | Cross-Platform: Pillow Textüberlappungen bei komplexen Schriften (TTC) | Pillow's `draw.text` mit `spacing` liefert falsche Metriken für Systemschriften (Hiragino, Devanagari). Textrendering in `generate_screenshots_*.py` und `generate_feature_graphics.py` auf manuellen, zeilenweisen Zeilenumbruch mit sprachspezifischem Faktor (`1.35` / `1.45` für CJK/Hindi/Bengali, `1.20` / `1.25` für Latein) umgestellt. |
| 2026-08-08 | Play Store: Indonesische Screenshots in Metadaten veraltet | Play-Store-Verzeichnis `"id"` (ohne `-ID`) wurde in `generate_screenshots_android.py` nicht aktualisiert. `LOCALE_MAP` um `"id"` erweitert, damit beide Ordner korrekt indonesische Bilder erhalten. |




## GitHub Actions CI-Gate (ab v1.8.4)
- Unit-Tests laufen vor bundleRelease: `./gradlew :shared:testDebugUnitTest --build-cache`
- Schlägt ein Test fehl → kein Build, kein Deploy in Play Store
- **Play Store Metadata / Release Notes:** Automatically generated by GitHub Actions from the main changelogs. For future releases, only update `docs/CHANGELOG.md` (DE), `docs/CHANGELOG.en.md` (EN), and `docs/internal/test_plan.md` (if useful). Never manually update or translate store-specific `release-notes/whatsnew-*` files.
- **iOS-Screenshot-Laufzeit (CI)**: Ein kompletter Durchlauf von `fastlane snapshot` für alle 22 Sprachen auf dem GitHub Actions macOS-Runner dauert ca. **1 Stunde und 52 Minuten**. Dies liegt an der sequentiellen Ausführung der Xcode UI-Tests pro Simulator-Locale.

## TODO: AGP DSL-Migration (vor AGP 10.0)
Wenn `android.builtInKotlin=false` + `android.newDsl=false` entfernt werden sollen:
1. `build.gradle.kts`: `fun Project.android(configure: Action<BaseAppModuleExtension>)` → `ApplicationExtension`
2. Plugin `org.jetbrains.kotlin.android` prüfen ob durch built-in Kotlin ersetzbar
3. Beide Flags danach entfernen

## Technische Schulden (aus v1.8.0 Release-Logs, 2026-04-22)
| Komponente | Problem | Aktion |
|---|---|---|
| `SettingsFactory.kt` | `EncryptedSharedPreferences` + `MasterKey` deprecated | ✅ Vollständig erledigt: Migrations-Code + `security-crypto` Dependency entfernt. Nur noch reines DataStore in SettingsFactory.kt. |
| `gradle.properties` | `android.builtInKotlin=false` + `android.newDsl=false` deprecated | ⛔ Beibehalten: AGP 9.x erfordert `com.android.kotlin.multiplatform.library` statt `com.android.library` im shared-Modul. Flags bleiben bis zur KMP-Plugin-Migration. Kommentar in gradle.properties aktualisiert. |
| GitHub Actions | Veraltete Action-Versionen | ✅ Aktualisiert in v2.0.4: setup-gradle@v6, cache@v5, upload-artifact@v7, gh-release@v3, cleanup.yml auf gh CLI migriert |
| jarsigner | Self-signed Cert Warning | ✅ Normal – Google Play App Signing aktiv, kein Handlungsbedarf |
| 2026-06-03 | iOS 26 AlarmKit: `openAppWhenRun=false` blockiert AlarmKit-Scheduling lautlos | In LiveActivityIntents mit `openAppWhenRun=false` scheitert `AlarmManager.shared.schedule()` lautlos – UserDefaults funktioniert, AlarmKit nicht. **Lösung:** `openAppWhenRun = true` ist ZWINGEND für Snooze-Intents die AlarmKit aufrufen. Die App öffnet sich kurz, plant den Alarm, zeigt den Banner. |
| 2026-06-03 | iOS 26 AlarmKit: Snooze-Alarm nicht geplant trotz `openAppWhenRun=true` | `scheduleWakeUp()` ist eine fire-and-forget Wrapper-Methode (startet internen `Task`). Wenn der Intent `.result()` zurückgibt, suspendiert iOS die App und der Task wird abgebrochen. **Lösung:** Direkt `try await scheduleWakeUpAsync()` im Intent aufrufen, damit der Alarm garantiert registriert ist bevor der Intent endet. |
| 2026-06-03 | iOS 26 AlarmKit: Race Condition killt Snooze Alarm & verhindert Klingeln | Wenn User Snooze drückt, führt iOS oft auch den `stopIntent` aus. Dies löschte den gerade vom `secondaryIntent` neu geplanten Wecker. **Lösung:** `SnoozeNotifyIntent` setzt SOFORT synchron `snooze_until` in UserDefaults. `OpenFamWakeIntent` prüft `hasActiveSnooze` und bricht das Canceln ab. Zudem plant nicht der Intent den neuen Alarm (wegen iOS-Suspension), sondern er sendet eine Notification an `AppRouter` -> `familyViewModel.snooze`, welches den Wecker mit `soundUri` zuverlässig plant. |
| 2026-06-02 | iOS 26 AlarmKit: Custom Sound auf echten Geräten stumm (Fallback auf Default) | Simulator stürzt ab, wenn bei `AlertSound.named("sound.caf")` die Endung mitgegeben wird. Auf einem echten iPhone MUSS die Endung `.caf` jedoch zwingend dabei sein, sonst findet er die Datei im Bundle nicht. **Lösung:** `#if targetEnvironment(simulator)` für saubere Trennung des Dateinamens. |
| 2026-06-02 | iOS 26 AlarmKit: Doppel-Klingeln durch Fallback-Push | Wenn der Wecker per AlarmKit Intents beendet wird, klingelt oft noch die klassische iOS Push-Benachrichtigung als Fallback. **Lösung:** Im `OpenFamWakeIntent` (Dismiss) zwingend `AlarmService.shared.cancelWakeUp` aufrufen, um die pending `UNUserNotification` zu löschen. Da `AlarmService` `@MainActor` ist, `await` nicht vergessen! |
| 2026-06-09 | iOS 26 AlarmKit: Snooze-Alarm im Background-Intent schlägt fehl | **Lösung:** `SnoozeNotifyIntent` verwendet `openAppWhenRun = false` und plant Wecker direkt im Background-Thread über statische Methode `scheduleWakeUpDirect(...)` (ohne `@MainActor`-Wrapper). `Task.sleep(1s)` verhindert Prozess-Abbruch, `UserDefaults` schützt vor Race Conditions. |
| 2026-07-20 | Android: Play Store Warnungen (R8/Edge-to-Edge/Bilder) für v2.0.4 | **Lösung:** (1) `enableEdgeToEdge()` in `RingingActivity.kt` integriert. (2) `proguard-rules.pro` bereinigt und Google/Firebase Wildcard-Rules gelöscht (R8 schrumpft nun ungenutzten Firebase-Code korrekt). (3) Hintergrundbild `onboarding_bg.jpg` (272 KB) zu WebP (74 KB) konvertiert (73% Einsparung). |
| 2026-07-20 | iOS: Impressum Link-Crash im Simulator (`NSOSStatusErrorDomain Code=-50` bei URLs) | **Lösung:** Die URL-Lokalisierungsschlüssel (`settings_privacy_policy_url`, etc.) fehlten in `Localizable.xcstrings`, wodurch der rohe Schlüsselname als URL an `openURL` übergeben wurde. In `L.swift` wurden sprachsensitive Fallback-Properties implementiert, und in `SettingsView.swift` ehemals rohe Lookups darauf umgestellt. |
| 2026-07-20 | Android: Metadaten-Upload schlägt fehl mit `bn-IN - Invalid request` | **Lösung:** Google Play verlangt für Bengali die Angabe unter `bn-BD`. Da `bn-IN` (Bengali - India) in der Play Console nicht als separate Sprache aktiv war, lehnte die API den Upload ab. `bn-IN` aus `generate_android_metadata.py` entfernt (Bengali bleibt komplett über `bn-BD` abgedeckt). |


## iOS In-App-Käufe (Donations) – Freigabe-Anleitung
**Status:** Deaktiviert. UI auskommentiert in `SettingsView.swift:87`. Produkt-IDs: `donation_small`, `donation_medium`, `donation_big`.

### Reihenfolge (Henne-Ei-Problem vermeiden):
1. **ASC Agreements** prüfen: „Paid Apps" muss grün/aktiv sein (sonst liefert Sandbox keine Produkte)
2. **ASC → In-App Purchases**: Alle 3 Produkte brauchen Preis + Screenshot + Review Notes → Status „Ready to Submit"
3. **Sandbox testen**: Sandbox-Tester in ASC erstellen → iPhone Einstellungen → App Store → Sandbox-Account einloggen → App via Xcode auf echtem Gerät starten → Produkte müssen mit Preisen erscheinen → Testkauf
4. **Code aktivieren**: `SettingsView.swift:87` einkommentieren (`donationCard`)
5. **Build hochladen**: Version/Build bumpen, via Fastlane/Xcode hochladen
6. **Produkte anhängen**: ASC → App → App Store → Version → Sektion „In-App Purchases" → **alle 3 Produkte per „+" verknüpfen**
7. **Review Info**: Sandbox-Credentials + Testanleitung eintragen, z.B. *"Settings → Support Us → Select any tier"*
8. **Submit for Review** → Apple reviewed App + Produkte zusammen → nach Approval sind sie live

## ASO Screenshots: Erstellung & Regeneration

### 0. Rohe iOS-Screenshots über Simulatoren neu erstellen (Laufzeit ~2h)
* **Befehl (aus Ordner `/ios/`)**: `bundle exec fastlane generate_screenshots`
* **Arbeitsweise**:
  1. Startet Simulatoren für alle konfigurierten iOS-Locales.
  2. Führt UI-Tests aus, um die App-Zustände vollautomatisch abzufotografieren (`capture_screenshots`).
  3. Legt die rohen PNGs unter `ios/fastlane/screenshots/` ab.
  4. Die rohen Bilder werden nach dem anschließenden Ausführen des Framing-Skripts automatisch im Git-Verzeichnis `docs/internal/images/screenshots/devices/{lang}/` für die dauerhafte Persistenz abgelegt.

### 1. iOS-Screenshots neu generieren
* **Skript**: `python3 scripts/generate_screenshots_ios.py`
* **Arbeitsweise**:
  1. Liest die rohen Simulator-Screenshots aus `docs/internal/images/screenshots/devices/{lang}/` (oder temporär aus `ios/fastlane/screenshots_temp`).
  2. Baut das iPhone 17 Pro Max Mockup drumherum (Breite `1010px`), legt die iOS Statusbar (`9:41`) und Dynamic Island darüber.
  3. Prüft den Kontrast an den Rändern und färbt Statusbar-Elemente sowie Home-Indicator passend ein (Schwarz/Weiß).
  4. Wendet bei Slide 3 und 6 am unteren Rand eine dunkelblaue Lesbarkeits-Vignette an (`start_y = 1730 * scale`), um weißen Text auf hellem Grund kontrastreich zu machen.
  5. Speichert PNGs für Fastlane unter `ios/fastlane/screenshots/{locale}/` und JPEGs für die Vorschau unter `docs/internal/images/screenshots/ios/{lang}/`.
* **HTML-Report**: Anschließend `python3 scripts/generate_html.py` ausführen.

### 2. Android-Screenshots neu generieren
* **Skript**: `python3 scripts/generate_screenshots_android.py`
* **Arbeitsweise**:
  1. Nutzt die gleichen iOS-Rohdaten aus `docs/internal/images/screenshots/devices/{lang}/`.
  2. Baut das Pixel-Mockup im 16:9 Format (`1080x1920`) drumherum (Gerätebreite `880px`), das stilvoll über den Rand hinausragt.
  3. Legt eine native Android-Statusbar (`10:00`) und Punch-Hole Kamera über den unberührten App-Inhalt (kein Top-Crop, um Header-Texte nicht abzuschneiden).
  4. Wendet bei Slide 3 und 6 die dunkelblaue Lesbarkeits-Vignette ab `Y=1150` an.
  5. Speichert die PNGs direkt im Play Store Verzeichnis `android/fastlane/metadata/android/{locale}/images/phoneScreenshots/` und JPEGs unter `docs/internal/images/screenshots/android/{lang}/`.

### 3. Git-Tracking & Gitignore-Bypass
* **WICHTIG**: Da der Pfad `docs/internal/` in der `.gitignore` eingetragen ist, ignoriert Git alle dortigen Roh- und Vorschaubilder standardmäßig.
* Nach jeder Neugenerierung müssen geänderte oder neue Bilder explizit mit force hinzugefügt werden:
  `git add -f docs/internal/images/screenshots/ docs/internal/images/feature_graphics/`
* Danach normal committen und pushen.

### 4. Play Store Feature Graphics neu generieren
* **Skript**: `python3 scripts/generate_feature_graphics.py`
* **Arbeitsweise**:
  1. Verwendet das Querformat-Hintergrundbild `docs/internal/images/hintergrund_quer.png` als Canvas (`1024x500`).
  2. Übersetzt die Texte (Titel, Subtitle, Description) dynamisch für alle 22 Sprachen (mit verifizierten Fallbacks für Deutsch und Englisch).
  3. Baut ein Pixel-Mockup mit dem aktuellen Dashboard-Screenshot (`main_scrolled.png`), neigt es um `12` Grad gegen den Uhrzeigersinn und versieht es mit einem weichen Schlagschatten.
  4. Platziert das Mockup so auf der rechten Seite, dass es stilvoll über den oberen, unteren und rechten Rand hinausläuft (wie im Originaldesign).
  5. Speichert die PNGs direkt in den Play Store Fastlane-Pfaden `android/fastlane/metadata/android/{locale}/images/featureGraphic.png` sowie als PNG unter `docs/internal/images/feature_graphics/`.

## ASO & Metadaten Best Practices (Applyra)

### 1. iOS App Store Keyword-Synergie
- **Indexierungs-Kombination**: Apple indiziert Suchbegriffe kombiniert aus `App Title` (max. 30 Zeichen), `Subtitle` (max. 30 Zeichen) und `Keyword-Feld` (max. 100 Zeichen).
- **Keine Redundanzen**: Wörter, die bereits im Title oder Subtitle vorkommen (z.B. *Familienwecker*, *Morgenplaner*, *Family*, *Alarm*), dürfen **nicht** in `keywords.txt` wiederholt werden. Dies spart wertvolle Zeichen für zusätzliche High-Traffic Keywords (*kostenlos*, *laut*, *badplaner*, *shared*, *tracker*).
- **Formatierung**: Kommagetrennt ohne Leerzeichen (z.B. `kinderwecker,morgenroutine,zeitmanagement,...`), keine Wortwiederholungen, keine Plural-/Singular-Duplikate.

### 2. Google Play Store Keyword-Dichte
- **Indexierung**: Google Play indiziert `Title` (max. 30 Zeichen), `Short Description` (max. 80 Zeichen) und `Full Description` (max. 4000 Zeichen).
- **Optimale Keyword-Dichte**: Primäre Keywords (*Familienwecker*, *Kinderwecker*, *shared alarm clock*, *morning routine*) sollten eine natürliche Dichte von **1.2% – 1.5%** in der `full_description.txt` aufweisen (ca. 6–8 Erwähnungen bei ~500 Wörtern).
- **Kein Keyword-Stuffing**: Keywords sinnvoll in Fließtext und strukturierte Feature-Überschriften einbinden.

### 3. Applyra MCP Workflow
- **Wöchentliche Routine**: Keywords mit Traffic $< 5$ oder Duplikate (Case-Sensitive Einträge wie *Schlafplaner* vs. *schlafplaner*) via `untrack_keyword` bereinigen.
- **Potenzialsuche**: Regelmäßige Nischen- und Autocomplete-Abfragen nach Begriffen mit **Traffic $> 10$** und **Difficulty $< 40$** (KEI $\ge 1.0$).

