# Changelog

Alle wichtigen Änderungen an diesem Projekt werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog](https://keepachangelog.com/de/1.0.0/),
und dieses Projekt folgt der [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

*[🇬🇧 English version](CHANGELOG.en.md)*

## [0.4.11] - 2026-03-04

### Behoben
- **Coroutine Cancellation Popup:** Ein visueller Fehler wurde behoben. Wenn der Datenstrom beim App-Start (nach einer Backup-Wiederherstellung) sauber neu gestartet wird, löste der Abbruch der alten Coroutine fälschlicherweise eine rote Fehlermeldung ("StandaloneCoroutine was cancelled") im UI aus. Diese erwartete System-Exception wird nun korrekt ignoriert.


## [0.4.10] - 2026-03-04

### Behoben
- **Invalid document reference Crash:** Ein Folgefehler des vorherigen Updates wurde behandelt. Der kurzzeitige Workaround-Status für den App-Reload führt nun nicht mehr zu fehlerhaften Firestore-Anfragen mit leerer ID (`""`).


## [0.4.9] - 2026-03-04

### Behoben
- **Fehlende Berechtigungen nach Neuinstallation:** Ein Fehler wurde behoben, durch den nach einer Neuinstallation (aus einem Backup) kurzzeitig eine `PERMISSION_DENIED`-Fehlermeldung wegen veralteter Authentifizierungs-Status auftreten konnte. Das Profil lädt nun den Wecker reibungslos direkt nach dem erneuten Login bzw. App-Start.


## [0.4.8] - 2026-03-04

### Hinzugefügt
- **Datenbank & Speicher-Hygiene (Garbage Collection):** Automatisierte Firebase Cloud Function (`cleanupInactiveFamilies`), die jeden Sonntag verwaiste Familien (180 Tage Inaktivität) restlos vom Server löscht. Inklusive E-Mail-Bericht an den Administrator.

### Geändert & Verbessert
- **Akku- & Performance-Optimierung:** Umstellung auf `collectAsStateWithLifecycle` in der gesamten Android App. Die App pausiert nun vollständig im Hintergrund und spart merklich Akkulaufzeit, während der Systemwecker unberührt weiterarbeitet.
- **Dark Mode Konsistenz:** Entfernung letzter hartkodierter Weiß-Töne. Das Theme verlässt sich nun nahtlos auf das offizielle `MaterialTheme.colorScheme.onSurface` Token für perfekten Dark/Light-Mode Kontrast.
- **Memory-Schutz im Scheduler:** Ein hartes Limit (max. 6 aktive Mitglieder) verhindert nun OutOfMemory (OOM) Crashes auf Android Geräten bei der komplexen O(n!) Zeitgeist-Berechnung.
- **Fehlerfreie Familiencodes:** Der Generator für Einladungscodes nutzt nun einen Base32-Zeichensatz ohne verwechselbare Buchstaben (`0`, `O`, `1`, `I`), was Fehler beim Abtippen drastisch reduziert.

### Behoben
- **Auto-Reset "Pausiert":** Ein Fehler wurde behoben, durch den manuell pausierte unbeanspruchte Profile (z.B. Kleinkinder) nicht automatisch für den nächsten Tag reaktiviert wurden. Zudem resettet der Status "Bin schon wach" nun ebenfalls zuverlässig.


## [0.4.7] - 2026-03-04

### Hinzugefügt
- **Visuelles Feedback:** Bounce-Effekt beim Klicken auf Buttons und interaktive Elemente für eine reaktionsschnellere Bedienung.
- **Icon-Erweiterungen:** Neue Icons für externe Links (Datenschutz, Impressum, Account löschen) und E-Mail-Support zur besseren visuellen Führung.
- **Join-Code Sicherheit:** Implementierung einer Eindeutigkeitsprüfung für Familien-Join-Codes im `FirebaseRepository`.

### Geändert
- **Einstellungen-Struktur:** Logische Neusortierung der Sektionen (Profil, Weckton, Familie, Sprache, Hilfe, Support).
- **Lokalisierung:** Copyright-Footer in den Einstellungen auf Deutsch („Alle Rechte vorbehalten.“) lokalisiert.
- **Icon-Refinement:** Anpassung der Sektions-Icons (Groups, Description) in den Einstellungen für klarere Symbolik.

## [0.4.6] - 2026-03-04

### UI & UX Überarbeitung (Dark Mode 2.0)
- **Extreme Dark Mode:** Das dunkle Thema wurde auf fast schwarze Hintergründe und tiefdunkle Karten umgestellt für einen extrem modernen Look und bessere Akkulaufzeit (OLED).
- **Kontrast-Optimierung:** Alle Text- und Icon-Farben wurden für das neue dunkle Design geschärft, um maximale Lesbarkeit bei Nacht zu garantieren.
- **Dynamic App Branding:** Die TopAppBar zeigt nun app-weit ein gebrandetes Logo: **FamWake** (ExtraBold) + lokaler Zusatz (normal), z.B. „Familienwecker“.
- **Light Theme Polishing:** Transparente (nahtlose) Header im hellen Design. Fix für „matschige“ Schatten-Artefakte an Kacheln durch Verzicht auf Alpha-Transparenz im Light Mode.
- **Font-Strategie:** Wechsel auf eine präzisere Font-Einbindung von "Nunito" mit 7 individuellen Gewichtungen, die nun korrekt über das gesamte Projekt hinweg (Compose Typography) genutzt werden.
- **Layout-Verfeinerung:** Reduzierung der inneren Karten-Abstände von 16.dp auf 12.dp für ein kompakteres, professionelleres Erscheinungsbild.

### Behoben
- **Kompilierungsfehler:** Behebung von Syntaxfehlern und fehlenden Imports in den Screens für Mitgliederverwaltung und Login.
- **Shadow Artefakte:** Behebung eines Darstellungsfehlers bei Fehlerkarten und pausierten Mitgliedern im hellen Design.

## [0.4.5] - 2026-03-04

### UI & UX Modernisierung
- **Glasmorphismus:** Einführung von halbtransparenten Karten mit leichtem Rand (1.dp) und reduzierter Elevation (0.dp) zur Vermeidung von Android-Shadow-Artefakten.
- **Hintergrund-Gradients:** Sanfter, vertikaler Farbverlauf je nach Theme.
- **Abgerundete Ecken:** CornerRadius auf weichere 24.dp - 32.dp angehoben.
- **Neue Typografie:** Einbindung der "Nunito" Google Font für ein weicheres, geometrischeres Schriftbild. Stärkere Font-Gewichtungen (ExtraBold) für Header und große Überschriften.
- **Dark Mode Support:** Theme-Auswahl ("Light", "Dark", "System") in den Einstellungen hinzugefügt, inklusive automatischer Systemübernahme. Anpassung des Color-Schemes (z.B. auf Night Blue und weiches Mint).
- **Animationen:** "Bouncy" Spring-Animationen auf interaktiven Objekten (Buttons) sowie sanfte Transitions für Familienmitglieder-Listen.
- **Clean-Up:** Das "Sync"-Icon in der TopBar wurde entfernt, da die Synchronisierung im Hintergrund und beim Start erfolgt.

## [0.4.4] - 2026-03-03

### Hinzugefügt
- **Automatisierte Unit-Tests:** Der Kern-Algorithmus (`Scheduler`) wird nun automatisch durch Unit-Tests auf Korrektheit und Konfliktlösung geprüft.


## [0.4.3] - 2026-03-03

### Bereinigt & Verbessert
- **Icon-Bereinigung & Optimierung:** 
    - Ungenutzte Assets (`ic_splash_logo.png`, `splash_screen_full.jpg`) und die ungenutzte `SplashScreen.kt` wurden entfernt, um die App-Größe zu reduzieren.
    - Die Verweise in den adaptiven Icons (`mipmap-anydpi`) wurden vereinheitlicht und nutzen nun dichte-spezifische Mipmaps statt eines einzelnen Bildes in `drawable/`, um Skalierungsfehler auf hochauflösenden Displays zu vermeiden.
    - Das App-Icon in `drawable/` wurde auf das aktuelle Design aktualisiert.
    - Unterstützung für Monochrome-Icons (Themed Icons) ab Android 13 sichergestellt.


## [0.4.2] - 2026-03-03

### Hinzugefügt
- **Bestätigungs-E-Mail nach Passwortänderung:** Nach dem erfolgreichen Speichern eines neuen Passworts über die Reset-Seite wird nun automatisch eine gebrandete Bestätigungs-E-Mail (DE/EN) versendet.

### Behoben & Verbessert
- **E-Mail-Verifizierung (Login-Bypass):** Ein Fehler wurde behoben, durch den Nutzer nach einem App-Restart oder explizitem Login den Verifizierungs-Screen umgehen konnten. Die App prüft nun strikt den `isEmailVerified` Status (`AuthViewModel`).
- **Resend-Zuverlässigkeit & Fallback:** Cloud Function Fehler beim E-Mail-Versand werden nun clientseitig abgefangen und lösen einen automatischen Fallback auf das native Firebase-System (`sendEmailVerification`) aus, um sicherzustellen, dass die Mail immer ankommt.
- **Link-Routing-Fix:** Die Verifizierungs-Links in den E-Mails führen nun zuverlässig zur korrekten Bestätigungsseite (`verify-email.html`) statt zur Passwort-Reset-Seite, indem die globale Action-URL in der Cloud Function dynamisch angepasst wird.


## [0.4.1] - 2026-03-03

### Hinzugefügt
- **Gebrandete Passwort-Reset-Seiten:** In diesem Projekt unter `/auth` wurden eigene HTML-Seiten für das Zurücksetzen des Passworts erstellt, die das App-Branding (Outfit-Font, Farben) nutzen.
- **Bestätigungs-E-Mail:** Nach erfolgreichem Passwort-Reset wird nun eine gebrandete Bestätigungs-E-Mail (DE/EN) via Cloud Function und Resend versendet.

### Behoben & Verbessert
- **Fehler-Mapping bei Password-Reset:** Das Error-Mapping in der Cloud Function wurde verbessert, sodass „Account nicht vorhanden" (`auth/user-not-found`) nun zuverlässig als `USER_NOT_FOUND` an den Android-Client gemeldet wird.
- **Dokumentation:** SETUP-Guides (DE/EN) und Testplan auf v0.4.1 aktualisiert und um Branding-Infos ergänzt.
- **Version Bump:** App-Version auf 0.4.1 angehoben.

## [0.4.0] - 2026-03-02


### Hinzugefügt
- **Gebrandete Passwort-Reset-Mail:** Die Passwortzurücksetzung läuft nun über eine Firebase Cloud Function (Node.js 22, Gen2, `europe-west3`) und den Mail-Provider **Resend**. Absender ist `no-reply@familienwecker.de` mit eigenem HTML-Template (Button, Sicherheitshinweis, Footer).
- **Zweisprachige E-Mails:** E-Mail-Template (Betreff, Inhalt, Absender, Footer) passt sich automatisch an die Gerätesprache an (Deutsch / Englisch).
- **Fehlermeldungen bei Password-Reset:** Ungültige E-Mail, unbekannter Account und Ratenlimit werden nun mit klaren Meldungen (DE/EN) angezeigt.

### Geändert & Verbessert
- **Code-Härtung:** Sichere `LocalTime`-Parsing-Fallbacks in `FirebaseRepository`. `addOrUpdateMember()` mit Try-Catch und Logging. `email.trim()` bei Login, Register und Reset.
- **Bereinigung:** Doppelter Import in `MainActivity` entfernt.

## [0.3.11] - 2026-03-02

### Geändert & Verbessert
- **Daten-Resilienz:** Bei gelöschten oder fehlenden Familiendaten wird nun eine Option zum Verlassen der Familie angeboten, um zum Setup-Screen zurückzukehren.
- **Claim-Synchronisierung:** Geclaimte Profile werden nun zuverlässiger über Geräte hinweg synchronisiert, ohne dass ein manueller Refresh nötig ist.

## [0.3.10] - 2026-03-02

### Hinzugefügt
- **Passwort vergessen:** Funktion zum Zurücksetzen des Passworts auf dem Login-Screen integriert.

### Geändert & Verbessert
- **Synchronisierung & Stabilität:** Der "Wecker Ein/Aus"-Schalter wird nun via Firestore synchronisiert. Die App führt beim Start einen automatischen "Force Refresh" durch, um Daten-Inkonsistenzen (z. B. nach Neuinstallation) zu vermeiden.
- **Transparenz bei Fehlern:** Systemfehler (z. B. "Permission Denied" oder Netzwerkfehler) werden nun direkt auf dem Dashboard angezeigt.
- **Datenbank-Härtung:** Die Firestore-Regeln wurden für den Familien-Ersteller optimiert, um Zugriffsprobleme während der Profil-Synchronisierung zu beheben.
- **Robustere Logik:** Verbesserte Bestimmung des aktuellsten Stands (`lastUpdatedAt`) bei gleichzeitigen Änderungen und "ultra-robuste" Mitglieder-Mapping-Logik (Sicherheits-Standardwerte).

### Behoben
- **Kritische Fixes:** Behebung eines `NullPointerException` beim App-Start sowie eines Fehlers, der bei Netzwerkproblemen zu lokalem Datenverlust führen konnte.
- **UI-Fixes:** Der Pause-Button auf ungeclaimten Karten funktioniert nun korrekt für die Zeitplanberechnung.
- **Version Bump:** App-Version auf 0.3.10 angehoben.

## [0.3.9] - 2026-03-02

### Hinzugefügt
- **Automatischer Status-Reset:** Die Status „Bin schon wach" und „Pausiert" werden nun automatisch zurückgesetzt, sobald die berechnete Weckzeit für das jeweilige Mitglied erreicht oder überschritten wurde. Dies stellt sicher, dass der Wecker am nächsten Tag wieder regulär aktiv ist.

### Geändert & Verbessert
- **Scheduler-Integration:** Der Reset-Check wurde direkt in die Zeitplan-Berechnung integriert, um eine sofortige Aktualisierung der UI und der Alarme für den Folgetag zu gewährleisten.
- **Safety-Reset:** Zusätzliche Sicherheitsprüfung beim App-Start eingeführt, die pausierte Profile zurücksetzt, falls die App über den Weckzeitraum hinaus inaktiv war.

### Behoben
- Ein potenzielles Problem wurde behoben, bei dem der "Pausiert"-Status fälschlicherweise über mehrere Tage aktiv bleiben konnte.

## [0.3.8] - 2026-03-01

### Hinzugefügt
- **Android 14 Vollbild-Schutz:** Automatischer Check und Request der "Vollbild-Intent-Berechtigung" unter Android 14+ beim App-Start.

### Geändert & Verbessert
- **Wecker-Sound & System-Töne:** Die Auswahl von System-Wecktönen wurde stabilisiert. Einführung von dynamischen Notification-Channels mit Sound-Fallback. Der Wecker nutzt nun zuverlässig den gewählten System-Ton, auch wenn die Fullscreen-Activity verzögert startet.
- **Optimierte Ringing-Logik:** Die `RingingActivity` bricht nun beim Start die Benachrichtigung ab, um Sound-Überschneidungen zu vermeiden.
- **Modernisierte Screen-Flags:** Verbesserte Flags für das Erreichen des gesperrten Bildschirms auf aktuellen Android-Versionen.

### Behoben
- **Sound-Reset beim Logout:** `PreferencesRepository` korrigiert; der gewählte Weckton und die Spracheinstellungen bleiben nun nach dem Abmelden und erneuten Anmelden dauerhaft erhalten.
- **Kompilierungsfehler:** Fehlende Imports in `AlarmReceiver` und `RingingActivity` ergänzt.

## [0.3.7] - 2026-02-27

### Hinzugefügt
- **Google-Login Icon:** Die Login-Seite zeigt nun das offizielle Google-Icon auf dem Login-Button.
- **Lösch-Bestätigung für Mitglieder:** Das Löschen eines Familienmitglieds erfordert nun eine explizite Bestätigung, um versehentliches Entfernen zu verhindern.
- **Präzisierte Deletions-Sicherheit:** Das Löschen einer Familie erfordert nun eine doppelte Bestätigung, wenn sich noch andere Mitglieder (egal ob geclaimt oder nicht) in der Familie befinden. Bei einer leeren Familie oder wenn man nur selbst Mitglied ist, genügt eine einfache Bestätigung.

### Geändert & Verbessert
- **Zentraler "Bin wach"-Button:** Die Funktion „Ich bin wach" wurde aus den einzelnen Mitgliederkacheln entfernt und als globaler Button prominent in den Bereich des Master-Schalters verschoben.
- **Kompakter Master-Schalter:** Die Texte am Hauptschalter wurden gekürzt und präzisiert ("Wecker an" / "Wecker aus").
- **Rechte-Management:** Profile, die von anderen Familienmitgliedern beansprucht wurden, können nun nicht mehr von Dritten bearbeitet oder gelöscht werden. Die Bearbeitungs-Buttons werden für diese Profile automatisch ausgeblendet.
- **Besitzer-Profil löschen:** Das eigene Profil kann nun direkt vom Hauptbildschirm aus gelöscht werden (führt zur Freigabe des Profils und Deaktivierung des Weckers).
- **Pause-Logik:** Der Pausieren-Knopf (der um mitternacht resettet wird) ist nun nur noch bei nicht geclaimten Membern aktiv und anwählbar, um Redundanz zum Master-Schalter zu vermeiden.

### Behoben
- **Kein Infinity-Loading nach Löschen:** Ein Fehler wurde behoben, durch den die App nach dem Löschen einer Familie beim Erstellen einer neuen Familie in einer Endlosschleife hängen blieb.
- **Login-Persistenz:** Das Löschen einer Familie führt nicht mehr zum kompletten Logout; der Nutzer bleibt eingeloggt und landet direkt im Setup-Screen für eine neue Familie.
- **Logik-Audit:** Konsistenzprüfung der gesamten App-Logik bezüglich geclaimter, ungeclaimter und eigener Profile durchgeführt und Sicherheitslücken beim Bearbeiten fremder Daten geschlossen.

## [0.3.6] - 2026-02-27

### Hinzugefügt
- **"Bin schon wach" (Already Awake) Button:** Ein neues Sonnen-Icon (☀️) auf den Mitgliedskarten ermöglicht es, den eigenen Wecker für heute zu unterdrücken, ohne die Badezimmer-Reihenfolge der anderen zu stören.
- **"Pause für heute":** Über ein neues Pause-Icon (⏸️/▶️) kann ein Mitglied komplett aus dem heutigen Plan genommen werden, wodurch andere ggf. länger ausschlafen können.
- **Snooze-Funktion:** Ein "Snooze (5 Min)" Button im Weck-Screen ermöglicht eine kurze Verzögerung des Alarms.
- **Mitglieder-Limit:** Die Familiengröße ist nun auf maximal 6 Mitglieder begrenzt, um Stabilität und Übersichtlichkeit zu gewährleisten.
- **Automatischer Reset:** Die Status „Bin schon wach" und „Pausiert" werden automatisch um Mitternacht (bzw. beim ersten App-Start des Tages) zurückgesetzt.

### Behoben & Verbessert
- **Daten-Persistenz:** Ein Fehler wurde behoben, durch den die Stati "Bereits wach" und "Pausiert" nicht korrekt in der Cloud gespeichert wurden.
- **Logout & Lösch-Logik:** Lokaler App-Zustand (SharedPreferences) wird beim Logout oder Löschen einer Familie nun restlos bereinigt; zudem erfolgt ein expliziter Firebase-Signout.
- **Auto-Backup deaktiviert:** Um die Wiederherstellung alter Sitzungsdaten nach einer Neuinstallation zu verhindern, wurde das Android-Auto-Backup für die App deaktiviert.
- **UI-Layout:** Die Kopfzeile der Mitgliedskarten wurde optimiert, um lange Namen und mehrere Status-Icons ohne unschöne Zeilenumbrüche darzustellen.
- **Akku-Optimierung:** Fehler behoben, bei dem der Klick auf die Warnkarte die Systemeinstellungen nicht öffnete (erfordert neue Berechtigung).
- **Kompilierung:** Ein Typ-Konflikt (Nullability) im Repository wurde behoben.

### Geändert
- **Scheduler-Robustheit:** Interne `LocalTime.MAX` Sentinel-Werte entfernt und eine Untergrenze von 04:00 Uhr für die Rückwärts-Planung eingeführt, um Fehler bei extrem frühen Abfahrtszeiten zu vermeiden.
- **Version Bump:** App-Version auf 0.3.6 angehoben.

## [0.3.5] - 2026-02-27

### Hinzugefügt
- **Präzise Scheduler-Diagnose:** Wenn kein Zeitplan gefunden wird, nennt die App nun das Mitglied und die Einschränkung, die den Konflikt verursacht (z.B. „Konflikt bei Mama: Wecken müsste um 06:15 Uhr sein, aber frühestes Wecken ist 06:30 Uhr“).
- **Akku-Optimierung Warnhinweis:** Neue Warnkarte auf dem Hauptbildschirm, falls die Akku-Optimierung des Systems den Wecker verzögern könnte. Klick auf die Karte führt direkt zur entsprechenden System-Einstellung.
- **Testplan:** Erstmaliger umfassender Testplan in `docs/test_plan.md`, der funktionale Tests, Randbedingungen (EC) und UI/UX-Szenarien abdeckt.

### Geändert
- **Robuster Scheduler:** Der Algorithmus nutzt nun Kotlin's `Result`-Typen für eine bessere Fehlerweitergabe und stabilere Berechnungsketten.
- **Roadmap-Synchronisation:** Die Roadmaps (DE/EN) wurden um alle Lücken aus dem Testplan (z.B. Snooze, DST-Schutz, Watchdog) für zukünftige Versionen ergänzt.
- **Version Bump:** App-Version auf 0.3.5 angehoben.

## [0.3.4] - 2026-02-27

### Hinzugefügt
- **Badezimmer-Dauer-Validierung:** Eingabe muss zwischen 1 und 120 Minuten liegen. Speichern-Button deaktiviert bei ungültigem Wert, Fehlermeldung wird angezeigt.

### Behoben
- **Member-Anlegen nach Familien-Erstellung:** `saveUserFamily()` wurde asynchron aufgerufen _nach_ der Navigation zur Hauptseite. Firestore-Security-Rules prüfen `isFamilyMember()` über das User-Dokument — dieses fehlte noch beim ersten Members-Write → Permission Denied (lautlos). Fix: User-Dokument wird jetzt vor der Navigation awaitet; SharedPrefs werden danach gesetzt. Nebeneffekt: Fehlermeldungs-Flash beim Anlegen einer Familie behoben.
- **isPaused / Claim-Status nach Bearbeiten eines Mitglieds zurückgesetzt:** `AddMemberScreen` hat beim Speichern ein neues `FamilyMember`-Objekt ohne `isPaused`, `claimedByUserId`, `claimedByUserName` und `createdAt` erstellt — alle Felder wurden auf Default-Werte zurückgesetzt. Fix: nicht-editierbare Felder werden jetzt aus dem bestehenden Mitglied übernommen.
- **Stabile Mitglieder-Reihenfolge:** Firestore liefert Dokumente in nicht-deterministischer Reihenfolge (UUID-IDs). Neues Feld `createdAt` (Epoch-Millis) wird beim Anlegen gesetzt und beim Bearbeiten bewahrt. Liste wird client-seitig nach `createdAt` sortiert.
- **Phantom-Alarm nach Logout / Familie verlassen / Familie löschen:** `logout()` cancelte keinen laufenden System-Alarm. Neuer Helper `cancelAlarmForCurrentUser()` wird jetzt in `logout()`, `leaveFamily()`, `deleteFamily()` und `recalculateSchedule()` (bei leerer Mitgliederliste) aufgerufen.

## [0.3.3] - 2026-02-27

### Hinzugefügt
- **Abfahrtszeit-Validierung:** Beim Anlegen/Bearbeiten eines Mitglieds wird nun geprüft, ob die „Haus verlassen"-Zeit nach der spätesten Weckzeit liegt. Bei Fehler wird eine Fehlermeldung angezeigt und der Speichern-Button deaktiviert.
- **Edit-Icon in der Mitglieder-Card:** Kleines Stift-Icon signalisiert die Bearbeitbarkeit von Mitgliedseinträgen per Antippen.

### Geändert
- **Klingelton-Fix (komplett überarbeitet):** Der ausgewählte Klingelton aus den Einstellungen wird nun korrekt abgespielt.
  - Notification-Channel auf `ALARM_CHANNEL_V2` erhöht – Android's gecachte Sound-Einstellungen des alten Kanals werden dadurch umgangen.
  - `RingingActivity.playRingtone()` komplett neu implementiert: `setDataSource + prepare()` mit `USAGE_ALARM`-Audio-Attributen statt `MediaPlayer.create()`. Dreistufige Fallback-Kette (gespeicherter Ton → System-Alarm → System-Ringtone). Ton läuft nun garantiert über den Alarm-Audio-Stream und wird nicht durch DND geblockt.
- **Klingelton läuft nicht mehr nach:** `onDestroy()` der `RingingActivity` ruft nun `stop()` vor `release()` auf; `mediaPlayer` wird danach auf `null` gesetzt.
- **Scheduler auf Background-Thread:** Die n!-Permutations-Berechnung des Schedulers wird nun auf `Dispatchers.Default` ausgeführt und blockiert nicht mehr den Main-Thread (ANR-Schutz ab ~7 Mitgliedern).
- **Farbpalette überarbeitet:** Verbesserter Kontrast und Hochwertigkeitsgefühl in Light und Dark Mode.
  - Light: Hintergrund `#F3F7FB` (NightBlue-Stich), Cards `#E8F0F8` (deutlich abgehoben), Fehler-Container klar Rot statt Warmgrau.
  - Dark: Hintergrund `#0F1923` (tieferes Blau-Schwarz), Primary `#8DAFC8` (Brand-nah, 5.5:1 Kontrast), Akzent SunriseOrange `#FFB37A` (5.2:1).
- **`compileSdk` / `targetSdk` auf 35:** Verhindert Java-21-Abhängigkeit des android-36.1 Extension-Platforms.

### Behoben
- **App-Abstürze nach Login:** `FamilyViewModel` und `PreferencesRepository` stürzen nicht mehr ab, wenn beim ersten Hochfahren noch kein `familyId` vorhanden ist.
- **Alarm-Cancel beim Mitglied-Löschen:** `cancelWakeUp()` wird nun für jedes gelöschte Mitglied aufgerufen, nicht nur für das eigene Profil.
- **Race Condition beim Profil-Beanspruchen:** `claimMember()` nutzt nun eine atomare Firestore-Transaktion statt `get() + update()`. Zwei Nutzer können dasselbe Profil nicht mehr gleichzeitig beanspruchen.
- **Doppelter MediaPlayer bei Bildschirm-Rotation:** `RingingActivity` ist nun als `screenOrientation="portrait"` im Manifest fixiert. Android recreated die Activity nicht mehr, ein doppeltes Abspielen ist ausgeschlossen.

## [0.3.2] - 2026-02-26
### Hinzugefügt
- **Neues App-Icon:** Das Icon wurde an das moderne Design des Web-Favicons angepasst.
- **Sicherheits-Regeln (Firestore):** Eine neue `firestore.rules` Datei wurde hinzugefügt, die den Zugriff auf Familien und Mitglieder absichert und die Lösch-Logik serverseitig erzwingt.

### Geändert
- **Farbkonzept & Kontraste:** Umfassende Überarbeitung der Farben (Deep Night Blue, Sunrise Orange). Der Dark Mode bietet nun deutlich sattere Kontraste und einen konsistent dunklen Header (TopAppBar) über alle Screens hinweg.
- **Optimierte Lösch-Logik:** 
    - Ungeclaimte Mitglieder können nun von jedem Familienmitglied gelöscht werden. 
    - Das eigene Profil kann jederzeit gelöscht/freigegeben werden.
    - Beim Löschen des eigenen Profils wird der Wecker automatisch deaktiviert.
- **Resiliente Familien-Löschung:** Das Löschen einer Familie ist nun robuster gegen Einzelfehler bei der Mitglieder-Löschung. Das Familiendokument wird in jedem Fall sicher entfernt.
- **Fehlerbehandlung:** Fehlermeldungen (z.B. nach fehlgeschlagenen Aktionen) werden nun beim Logout oder Verlassen der Familie zuverlässig zurückgesetzt und erscheinen nicht mehr auf dem Login-Screen.

### Behoben
- **Build-Fehler:** Fehlende `Color`-Referenzen in der UI wurden ergänzt, um eine fehlerfreie Kompilierung sicherzustellen.

## [0.3.1] - 2026-02-26
### Hinzugefügt
- **Profil-Besitz (Claiming):** Ein neues System wurde eingeführt, bei dem Nutzer in den Einstellungen ein Familienmitglied "beanspruchen" (claimen). Dies verhindert, dass andere Nutzer dasselbe Profil nutzen oder die Weckzeiten anderer Familienmitglieder verändern.
- **Sicherheits-Härtung (Firestore):** Neue Datenbank-Regeln erzwingen den Profil-Besitz auf technischer Ebene. Nur der "Besitzer" eines Profils kann dessen Daten ändern oder löschen.
- **Striktes Wecken & Auto-Alarm:** Der automatische Fallback auf das erste Profil wurde entfernt. Der globale Wecker ist nun standardmäßig AUS und schaltet sich erst automatisch EIN, wenn ein Profil erfolgreich geclaimt wurde. Ohne Profil-Zuweisung kann der Wecker nicht aktiviert werden.
- **UI-Navigation:** Die Warnung "Kein Profil ausgewählt" auf dem Dashboard ist nun anklickbar und führt direkt zu den Einstellungen.
- **Verbesserte Button-Platzierung:** Der "+"-Button zum Hinzufügen von Mitgliedern wurde von einem schwebenden Button (FAB) direkt in den Bereich der Familienmitglieder verschoben.
- **Priorisiertes Layout:** Der berechnete Weck-Plan steht nun an oberster Stelle auf dem Dashboard, gefolgt von der Mitgliederliste, da dieser im Alltag die wichtigste Information darstellt.
- **Automatische Profil-Wiederherstellung:** Nach einer Neuinstallation oder einem erneuten Login wird ein bereits beanspruchtes Profil automatisch wiedererkannt und ausgewählt.
- **Robuster Lösch-Schutz & Navigations-Sync (V 0.3.1):** Das Löschen einer Familie prüft nun auf andere aktive Mitglieder und löscht alle Mitglieder-Dokumente zuverlässig in einem Batch-Vorgang, bevor die Familie und der Benutzer-Bezug entfernt werden.
- **Synchronisierte Navigation (Fix):** Alle App-Komponenten nutzen nun synchronisierte Datenströme für die Benutzer-Präferenzen, was eine absolut zuverlässige Weiterleitung zum Dashboard nach dem Login garantiert.
- **Zustands-Wiederherstellung:** Der Status des Haupt-Weckschalters (An/Aus) wird nun sitzungsübergreifend gespeichert und beim nächsten Login automatisch wiederhergestellt.
- **Visualisierte Mitglieder-Stati (3-stufig):** Die Mitgliederliste zeigt nun differenziert an: "(Wecker aktiviert)" [Grün] oder "(kein Alarm)" [Rot], aber nur wenn ein Profil aktiv beansprucht wurde. Unbeanspruchte Profile zeigen keinen Status-Text (Rein für die Berechnung).
- **UI-Feinschliff Settings:** Die Schaltfläche "Abmelden" wurde in "Ausloggen" umbenannt und optisch durch eine Trennlinie abgesetzt.

### Geändert
- **Profil-Auswahl verbessert:** Das Dropdown-Menü in den Einstellungen ist nun deaktiviert, solange noch keine Familienmitglieder angelegt wurden.
- **Dynamische Warnungen:** Der Warnhinweis zur fehlenden Profil-Zuweisung erscheint nur noch, wenn bereits Mitglieder in der Familie vorhanden sind.

### Behoben
- **Navigations-Fehler (Backstack):** Ein Problem wurde behoben, bei dem der Login-Screen nach erfolgreicher Anmeldung im Hintergrund offen blieb. Ein "Zurück"-Wischen beendet die App nun wie erwartet.

## [0.3.0] - 2026-02-26
### Hinzugefügt
- Anzeige des Familiennamens in den Einstellungen: Neben dem Einladungscode wird nun auch der Name der Familie angezeigt, um Verwechslungen beim Beitritt oder bei der Verwaltung zu vermeiden.
- Navigation optimiert: Ein neuer Lade-Screen verhindert "doppeltes Wischen" zum Beenden der App und sorgt für einen sauberen App-Start.
- Logout im Setup-Screen: Ein neuer Button ermöglicht das Abmelden direkt aus dem Start-Setup, falls man ein anderes Konto nutzen möchte.
- Versionsverwaltung verbessert: Der interne Version-Code basiert nun auf einem Zeitstempel, was reibungslose Updates beim Drüber-Installieren der APK garantiert.

## [0.2.9] - 2026-02-25
### Behoben
- Abstürze beim Google Login (`NoCredentialException`) behoben, die auftraten, wenn auf dem Gerät noch gar kein Google Konto eingerichtet war.
- Die Konto-Auswahl beim Google Login (`setAutoSelectEnabled(true)`) optimiert, um systembedingte Abbrüche des Android `CredentialManager` zu verhindern.
- Code-Bereinigung: Ungenutzte Imports und ungenutzte Variablen entfernt.

## [0.2.8] - 2026-02-25
### Hinzugefügt
- Vollständige "Familie löschen"-Funktion in den Einstellungen hinzugefügt, welche die Familie inkl. aller Mitglieder sicher aus der Datenbank entfernt.
- Nutzer anderer Geräte werden nun automatisch auf den Startbildschirm zurückgeleitet, wenn ihre Familie gelöscht wurde.
- Automatisierte sprechende Dateinamen (inkl. Version und Build-Nummer) für die kompilierte APK eingeführt.

### Geändert
- Das Layout der Einstellungsseite trennt nun den Support-E-Mail-Button optisch mit einer zarten Trennlinie (HorizontalDivider) von den restlichen Weblinks ab.

### Behoben
- Google Sign-In Fehler auf dem Login-Screen werden jetzt rot auf dem Bildschirm ausgegeben (für einfacheres Debugging des SHA-256 Fingerabdrucks).
- Die URLs für Impressum, Datenschutz und Account-löschen wurden in der deutschen Sprachausgabe korrigiert, da versehentlich ein `-de` Suffix mitkopiert wurde.
- Die Konstante `default_web_client_id` wurde fest in die `strings.xml` integriert, um lästige "Unresolved reference"-Fehler in Android Studio nach einem Clean Build zu vermeiden.

## [0.2.7] - 2026-02-25

### Hinzugefügt (Added)
- **Account Löschen:** Ein Button "Konto löschen" wurde im Support-Bereich der Einstellungen hinzugefügt, der zur entsprechenden Löschungs-Seite verlinkt (Richtlinien-Anforderung).

## [0.2.6] - 2026-02-25

### Hinzugefügt (Added)
- **Flexibles Frühstück:** Die Frühstückszeit passt sich nun an enge Zeitpläne an und verkürzt sich automatisch um 5 bis 10 Minuten, wenn der gemeinsame Familienkalender ansonsten nicht aufgeht.
- **Englische Dokumentation:** Sowohl die README als auch das Changelog sind nun vollständig auf Englisch verfügbar und miteinander verlinkt.

### Geändert (Changed)
- **Rechtliche Dokumente:** Die Links zu Impressum und Datenschutzerklärung in der README und in den Einstellungen verweisen nun auf die korrekten, nativen Live-Webseiten-Pfade.

## [0.2.5] - 2026-02-24

Dies ist der initiale öffentliche Release auf GitHub nach einer umfassenden Codebereinigung und UI-Politur. 

### Hinzugefügt (Added)
- **Mehrsprachigkeit:** Die App ist nun vollständig in Deutsch und Englisch verfügbar. Die Sprache kann manuell in den Einstellungen umgeschaltet werden oder richtet sich nach den Systemeinstellungen.
- **Cloud-Synchronisierter Wecker-Schalter (Urlaubs-Modus):** Auf der Startseite gibt es nun einen Schalter ("Wecker Aktiviert/Pausiert"). Dieser deaktiviert nicht nur deinen lokalen Wecker, sondern meldet der Cloud, dass du heute kein Bad benötigst. Der Rest der Familie kann dadurch vollautomatisch länger ausschlafen. Pausierte Mitglieder werden im Dashboard mit einem "(Pausiert)" Label und blasserer Farbe hervorgehoben.
- **Support-Bereich:** In den App-Einstellungen gibt es nun einen Support-Bereich mit direktem E-Mail-Kontakt zum Entwickler.
- **Rechtliche Dokumente:** Links zu Datenschutzerklärung und Impressum in den App-Einstellungen (für App Store / Play Store Kompatibilität).
- **Adaptives App-Icon:** Ein neues, hochauflösendes und responsives App-Icon für alle gängigen Android-Launcher-Formate.
- **System Splash Screen:** Nativ unterstützter Android 12+ (API 31+) Splash Screen, der nahtlos in den ersten Screen der App übergeht.
- **README Dokumentation:** Screenshots und erklärende Feature-Übersicht für Besucher des GitHub-Repositories.

### Geändert (Changed)
- **Flexibler Alarm-Algorithmus:** Der Berechnungs-Algorithmus weicht Konflikten (z.B. überschneidende Badzeiten) nun intelligent aus und passt die Weckzeiten automatisch in 5-Minuten-Schritten (bis zu +/- 15 Minuten) an, um einen gangbaren Plan zu finden.
- **Dark Mode Anpassungen:** Verbessertes Farb-Thema (Theme.kt) für eine augenfreundlichere, kontrastreichere Darstellung bei aktiviertem Nachtmodus ("Dark Mode").
- **Settings Layout:** Die Einstellungen wurden logisch neu gruppiert. Version und Copyright-Infos sind jetzt deutlich als Footer am unteren Rand platziert.
- **Weck-Screen (Ringing Screen):** Das Layout des roten Aufwach-Screens wurde optimiert. Padding und zentrierter Text verhindern nun abgeschnittene Namen bei kleinen Displays oder langen Strings.
- **Projektname:** Der Anzeigename auf dem Homescreen und in der App wurde auf `FamWake - Familienwecker` (bzw. `Family Alarm`) vereinheitlicht.

### Behoben (Fixed)
- **Doppelte Splash Screens:** Ein Bug wurde behoben, bei dem auf neueren Android Versionen erst das Android System-Logo und danach nochmal ein ladebildschirm-ähnlicher Activity-Splash angezeigt wurde.
- **Platzhalter-Texte:** Hardcodierte Entwicklernamen ("Familie Notthoff" / "Smith Family") wurden aus den Textfeldern bei der Familien-Gründung entfernt und durch neutrale Beispiele ("Musterfamilie") ersetzt.

---
*Ältere, interne Entwicklungsstände (vor Version 0.2.5) sind in diesem öffentlichen Repository nicht dokumentiert.*
