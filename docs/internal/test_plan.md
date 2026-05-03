# 🧪 Testplan: FamWake
**Version:** 1.8.6
**Datum:** 2026-05-03

---

## 📋 Strategie

Tests validieren die Korrektheit des Planungsalgorithmus, die Wecker-Zuverlässigkeit und die Datensicherheit.

**Bereiche:** Account & Onboarding · Familien & Mitglieder · Wecker & Alarm · Sicherheit · Lokalisation · Edge Cases · UI/UX

---

## 🛠 Normalbetrieb

### 1. Account & Onboarding
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-01 | Erst-Registrierung (E-Mail) + Login-Validierung | Account erstellt, Bestätigungs-E-Mail versendet. Ungültige E-Mail / Passwort <8 Zeichen → Fehlermeldung, kein Absturz. |
| TC-02 | Google Sign-In | Login erfolgreich, Familienkontext korrekt geladen. |
| TC-03 | Passwort-Reset | E-Mail wird versendet. Unbekannte E-Mail: immer gleiche Erfolgsmeldung (User Enumeration Prevention). |
| TC-04 | Familie gründen + beitreten | Familie mit Code erstellt. Zweites Gerät tritt per Code und per `/join/`-Deep-Link bei. Eigenen Code eingeben → kein erneuter Join. |
| TC-05 | Familie verlassen / löschen | Eigenes Profil gelöscht, Alarm gecancelt. Nur Creator darf Familie löschen. Fehlermeldung für Nicht-Creator. Keine falsche „Synchronisierung fehlgeschlagen"-Meldung (auch auf Fresh Install). |
| TC-06 | Logout + Re-Login | Alarm-State bleibt erhalten (kein Race-Condition-Reset). Zweiter User einloggen → sieht nur eigene Familie. |
| TC-07 | Onboarding-Tour | 5 Slides mit Panda. Tour aus Einstellungen wieder aufrufbar. Abschluss landet im Hauptscreen. |
| TC-08 | Rate-Limiting | >5 falsche Codes/min oder >5 Reset-Mails/h → serverseitige Blockierung. |

### 2. Mitglieder & Konfiguration
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-20 | Auto-Claim + Kachel-Status | Erstes Mitglied auto-geclaimt, Wecker aktiviert. Kachel zeigt korrekt „Wecker aktiv/inaktiv" – kein Flash. Kein „Rechenfehler" nach Anlegen. |
| TC-21 | Profil bearbeiten (2. User) | Zweites Mitglied kann Profil anlegen, bearbeiten, speichern. Geclaimte Profile anderer: kein Edit-Icon. |
| TC-25 | Schnelles Weckzeit-Bearbeiten | Weckzeit-Slider mehrfach schnell hin-/herschieben → kein falscher Sync-Fehler, letzter Wert wird korrekt gespeichert. |
| TC-22 | Wochentag-Profile + Validierung | latestWakeUp ≤ earliestWakeUp oder Abfahrt zu früh → Fehlertext, Speichern gesperrt. Heutigen Tag deaktiviert, morgen aktiv → Hauptscreen zeigt morgigen Wecker. |
| TC-23 | Pausieren + Eigentums-Schutz | Unclaimed Member pausieren → korrekt aus Plan entfernt. Eigenes Profil: kein Pausieren-Button. Member löschen → myMemberId erst NACH Firestore-Bestätigung null. |
| TC-24 | Drag & Drop Reihenfolge | Long-Press → verschieben mit Gap-Preview. Reihenfolge bleibt nach Neustart und auf anderen Geräten. Limit: max 6 Mitglieder. |
| TC-24b | Unclaimed-Member-Warnung | Ungeclaimtes Mitglied an Position 1 → Amber-Banner erscheint (unterhalb der Kein-Profil-Card). Kein-Profil-Banner aktiv → kein Amber-Banner. Mitglied auf Position 2 verschoben → Banner verschwindet. |

### 3. Wecker & Alarm
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-30 | Alarm klingelt (Hintergrund + Lockscreen) | App im Hintergrund → Alarm klingelt, RingingActivity öffnet sich. Nach Geräterestart korrekt wiederhergestellt (auch vor PIN-Eingabe via Direct Boot). |
| TC-31 | Wecker an/aus/an | Wecker aus → an → Alarm klingelt zum geplanten Zeitpunkt. Status bleibt nach App-Neustart erhalten. |
| TC-32 | „Ich bin wach" – Kompletttest | Button aktiv 2h vor Alarm, inaktiv außerhalb. Stoppt System-Wecker sofort, Farbe/Icon wechselt. Status-Reset jederzeit möglich. App über Nacht im Hintergrund → Ausgangszustand am nächsten Morgen. |
| TC-33 | Two-Pass Datumslogik | Mama geweckt, Kind+Papa schlafen noch → Anzeige „Frühstück heute um …" (nicht „morgen"). Erst wenn ALLE heutigen Alarme verstrichen → UI wechselt zu „morgen". |
| TC-34 | Snooze | Snooze (5 Min) plant neuen Alarm exakt 5 Min später. Banner mit Endzeit + Abbruch-Button. Kein Konflikt mit regulärem Alarm. |
| TC-35 | Alarm-Sync Multi-Device | Wecker auf Gerät A aus → Gerät B sieht Status sofort. „Schon wach" auf A → Schedule aktualisiert sich auf B. |
| TC-36 | Exact Alarm Warnung (Android 14+) | Kein SCHEDULE_EXACT_ALARM → rotes Banner auf Hauptscreen und in Settings, klickbar zur System-Einstellung. |
| TC-37 | DayProfile-Datumssprung | Mo+Di: 6:30, Rest: 7:30. Di-6:30 hat geklingelt → UI zeigt "Mi 7:30". Kein zweiter Alarm mehr am Di um 7:30. Nächster Alarm klingelt am Mi um 7:30. |
| TC-38 | Lockscreen Alarm Warnung (Android 14+) | Kein USE_FULL_SCREEN_INTENT → **Kein** Popup mehr beim App-Start. Banner im MainScreen erscheint **nur**, wenn Exakter Alarm erlaubt, Wecker aktiv und Zeitplan vorhanden ist. Warnkarte in den Settings ist immer sichtbar, wenn Berechtigung fehlt. |

### 6. Push-Benachrichtigungen
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-40 | Push bei Wecker ein/aus | Gerät A schaltet Wecker aus → Gerät B erhält Push und Kachel verschwindet sofort. |
| TC-41 | Push bei Reorder | Gerät A verschiebt Reihenfolge → alle geclaimten Mitglieder (außer Auslöser) bekommen eine Benachrichtigung. |
| TC-42 | Kein Self-Push | Eigene Änderungen lösen keine Push auf dem eigenen Gerät aus. |
| TC-43 | Ungeclaimte Member | Reorder mit ungeclaimtem Mitglied → kein unnötiger Push an unbeteiligte Geräte. |
| TC-44 | Pause/Unpause Push | Mitglied pausieren → Push an alle. Schnell wieder unpausen → auch Push (kein Rate-Limit-Block). |
| TC-45 | Kein Doppel-Piepsen | Bei Reorder mit 3 Mitgliedern kommt genau 1 Benachrichtigung, nicht 2 oder 3. |
| TC-46 | Familie beitreten/verlassen | Beitritt → alle bestehenden Mitglieder erhalten Push. Austritt → Push an verbleibende. |
| TC-47 | Push-Toggle in App-Settings | Toggle AUS → kein Push wird angezeigt (Gerät empfängt still). Toggle AN → Push erscheint wieder. Alarm-Kanal unberührt. |
| TC-48 | Alarm-Default bei Fresh Install | Neuinstallation + Login mit bestehendem geclaimtem Profil → Wecker automatisch AN. Logout/Login (gleiche UID) → An/Aus-Zustand exakt beibehalten. Anderer Account auf gleichem Gerät → Wecker-Status auf den Zustand dieses Accounts gesetzt (AN wenn Profil geclaimt, sonst AUS). Familienwechsel via Einladungslink → Wecker-Schalter auf AUS bis Profil geclaimt. |

### 4. Sicherheit
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-50 | Firestore Member-Schutz | Versuch, name/claimedByUserId eines fremden Members zu schreiben oder zu löschen → PERMISSION_DENIED. |
| TC-51 | Feedback-Schutz | Versuch, Feedback ohne Auth → unauthenticated-Fehler. XSS-Payload → Code als Text in E-Mail, keine Ausführung. |
| TC-52 | Device Trust (Monitoring) | Integritäts-Check läuft lautlos im Hintergrund. Kein sichtbarer Effekt für den User. |
| TC-53 | In-App Review | Review-Prompt erscheint frühestens 7 Tage nach Install, nicht vor 06:00–09:00, max. alle 30 Tage. |

### 5. Lokalisation
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-60 | Sprach-Fallback | Unbekannter Sprachcode → App fällt auf Englisch zurück, nicht Deutsch. Auch beim Laden aus dem Speicher. |
| TC-61 | Alle Sprachen + Dialekte | 22 Sprachen (inkl. ID, VI, BN, MR, HI, ZH, KO) + Schwäbisch/Schweizerdeutsch/Ruhrpott → korrekte Texte, kein Absturz. 20 Weck-Sprüche pro Sprache vorhanden. Jede Sprache auf echtem Gerät (Android 13+) testen – Wechsel muss sofort greifen, kein EN-Fallback. |
| TC-62 | Sprachpicker | BottomSheet, alphabetisch, „System" immer zuerst. Grid scrollt auf 360dp-Geräten ohne Abschneiden. |

---

## ⚠️ Edge Cases

### Konfliktsituationen
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| EC-01 | Unmöglicher Plan | Alle wollen gleichzeitig ins Bad → Konflikt-Warnung + Kompromissvorschlag (Verschiebung/Frühstücksverkürzung). Wenn unlösbar → Best-Effort-Wecker zur bestmöglichen Zeit. |
| EC-02 | Kurzes Zeitfenster | Wecken 7:00, Abfahrt 7:05 → Warnung vor zu knappem Fenster. |

### Technische Grenzfälle
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| EC-10 | Offline-Betrieb | Netz trennen → CloudOff-Icon nach >3s. Wecker klingelt offline (lokaler Cache). Profil-Claim → sofortige Snackbar, kein Spinner. Familie beitreten offline → Fehlermeldung, Spinner stoppt sofort. Reconnect → Icon weg, Re-Sync lautlos. |
| EC-11 | Zeitumstellung + Mitternachtswecker | Weckzeiten bei Sommer-/Winterzeit korrekt. Alarm 00:15 korrekt eingeplant, kein Mitternachts-Guard-Block. |
| EC-12 | Multi-Device Claim Sync | Profil auf Gerät A geclaimt → Gerät B erkennt es ohne manuellen Refresh. |
| EC-13 | Kein Stale-Schedule nach Familienwechsel | Wechsel in neue Familie → kein Zeitplan/Mitglied der alten Familie kurzzeitig sichtbar. |
| EC-14 | Tagesreset + Pause-Reset | „Bin wach" und „Heute pausieren" am nächsten Tag automatisch zurückgesetzt. |
| EC-15 | Push bei Schnellaktionen | Pause → sofort Unpause (< 5s Abstand) → beide Aktionen lösen jeweils einen Push aus. |
| EC-16 | Push nach App-Neuinstallation | FCM-Token wird beim Start automatisch registriert. Push kommt sofort auf dem neuen Gerät an. |

---

## 📱 UI/UX & Barrierefreiheit

- **Dark Mode:** Augenfreundliche Kontraste; AMOLED Black (`#000000`).
- **Material You:** Dynamische Farben ab Android 12.
- **Touch-Targets:** Mindestens 24 dp.
- **Haptik:** Vibrationsmuster für Vor- und Hauptalarm.

| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| UI-01 | Theme-Switcher | Sonne/Auto/Mond-Icons wechseln sofort; kein Überlauf auf schmalen Geräten. |
| UI-02 | Kleine Screens (360dp) | FamilySetupScreen, SettingsScreen, AddMemberScreen – Tastatur schiebt Content hoch, Button bleibt erreichbar. Uhrzeiten einzeilig lesbar; Baddauer-Label bricht ohne Abschneiden um. |
| UI-03 | Scroll-Verhalten | Bouncender Pfeil bei langen Seiten. Kollabierender Titel auf Hauptscreen. Wochenplan-„Kopierlink" ohne Scrollen sichtbar. |
| UI-04 | Navigation | Zurück-Button in Settings/Login/FamilySetup. Feldreihenfolge in Wochentag-Card: Frühste Weckzeit → Späteste → Badzeit → Abfahrt → Frühstück. |

---

## 📈 Validierung

- **Automatisiert:** 9 Unit-Tests für `Scheduler` in `shared/commonTest` (OptimalPlan, Konflikt, Paused, NoActiveMembers, Breakfast-Clamping, Midnight-Wrap, TimeAdjusted-Fallback, MemberLimit, AllPaused). Laufen automatisch als Gate im GitHub Actions Release-Workflow.
- **Manuell:** Vor jedem Release ein Live-Test über eine Nacht in einer Test-Familie.
