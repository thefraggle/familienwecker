# 🧪 Testplan: FamWake
**Version:** 1.7.8
**Datum:** 2026-04-18

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
| TC-05 | Familie verlassen / löschen | Eigenes Profil gelöscht, Alarm gecancelt. Nur Creator darf Familie löschen. Fehlermeldung für Nicht-Creator. |
| TC-06 | Logout + Re-Login | Alarm-State bleibt erhalten (kein Race-Condition-Reset). Zweiter User einloggen → sieht nur eigene Familie. |
| TC-07 | Onboarding-Tour | 5 Slides mit Panda. Tour aus Einstellungen wieder aufrufbar. Abschluss landet im Hauptscreen. |
| TC-08 | Rate-Limiting | >5 falsche Codes/min oder >5 Reset-Mails/h → serverseitige Blockierung. |

### 2. Mitglieder & Konfiguration
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-20 | Auto-Claim + Kachel-Status | Erstes Mitglied auto-geclaimt, Wecker aktiviert. Kachel zeigt korrekt „Wecker aktiv/inaktiv" – kein Flash. |
| TC-21 | Profil bearbeiten (2. User) | Zweites Mitglied kann Profil anlegen, bearbeiten, speichern. Geclaimte Profile anderer: kein Edit-Icon. |
| TC-22 | Wochentag-Profile + Validierung | latestWakeUp ≤ earliestWakeUp oder Abfahrt zu früh → Fehlertext, Speichern gesperrt. Heutigen Tag deaktiviert, morgen aktiv → Hauptscreen zeigt morgigen Wecker. |
| TC-23 | Pausieren + Eigentums-Schutz | Unclaimed Member pausieren → korrekt aus Plan entfernt. Eigenes Profil: kein Pausieren-Button. Member löschen → myMemberId erst NACH Firestore-Bestätigung null. |
| TC-24 | Drag & Drop Reihenfolge | Long-Press → verschieben mit Gap-Preview. Reihenfolge bleibt nach Neustart und auf anderen Geräten. Limit: max 6 Mitglieder. |

### 3. Wecker & Alarm
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-30 | Alarm klingelt (Hintergrund + Lockscreen) | App im Hintergrund → Alarm klingelt, RingingActivity öffnet sich. Nach Geräterestart korrekt wiederhergestellt (auch vor PIN-Eingabe via Direct Boot). |
| TC-31 | Wecker an/aus/an | Wecker aus → an → Alarm klingelt zum geplanten Zeitpunkt. Status bleibt nach App-Neustart erhalten. |
| TC-32 | „Ich bin wach" – Kompletttest | Button aktiv 2h vor Alarm, inaktiv außerhalb. Stoppt System-Wecker sofort, Farbe/Icon wechselt. Status-Reset jederzeit möglich. App über Nacht im Hintergrund → Ausgangszustand am nächsten Morgen. |
| TC-33 | Two-Pass Datumslogik | Mama geweckt, Kind+Papa schlafen noch → Anzeige „Frühstück heute um …" (nicht „morgen"). Erst wenn ALLE heutigen Alarme verstrichen → UI wechselt zu „morgen". |
| TC-34 | Snooze | Snooze (5 Min) plant neuen Alarm exakt 5 Min später. Banner mit Endzeit + Abbruch-Button. Kein Konflikt mit regulärem Alarm. |
| TC-35 | Alarm-Sync Multi-Device | Wecker auf Gerät A aus → Gerät B sieht Status sofort. „Schon wach" auf A → Schedule aktualisiert sich auf B. |
| TC-36 | Berechtigungs-Warnung (Android 14+) | Kein SCHEDULE_EXACT_ALARM → rote Kachel auf Hauptscreen, klickbar zur System-Einstellung. |

### 4. Sicherheit
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-50 | Firestore Member-Schutz | Versuch, name/claimedByUserId eines fremden Members zu schreiben oder zu löschen → PERMISSION_DENIED. |
| TC-51 | Admin-Sichtbarkeit | Nicht-Admin sieht keine Admin-Buttons. Globaler Admin sieht Admin-Modal; 2-Min-Testwecker auslösbar. |
| TC-52 | Feedback-Schutz | Feedback ohne Auth → unauthenticated-Fehler. XSS-Payload → Code als Text in E-Mail, keine Ausführung. |
| TC-53 | Device Trust (Monitoring) | Integritäts-Check läuft lautlos im Hintergrund. Kein sichtbarer Effekt für den User. |

### 5. Lokalisation
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-60 | Sprach-Fallback | Unbekannter Sprachcode → App fällt auf Englisch zurück, nicht Deutsch. Auch beim Laden aus dem Speicher. |
| TC-61 | Alle Sprachen + Dialekte | DA/NO/FR/ES/IT/SV/TR/UK/RU/NL/PL/PT + Schwäbisch/Schweizerdeutsch/Ruhrpott → korrekte Texte, kein Absturz, kein DE-Fallback. |
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
| EC-10 | Offline-Betrieb | Netz trennen → CloudOff-Icon nach >3s. Wecker klingelt offline (lokaler Cache). Profil-Claim → sofortige Snackbar, kein Spinner. Reconnect → Icon weg, Re-Sync lautlos. |
| EC-11 | Zeitumstellung + Mitternachtswecker | Weckzeiten bei Sommer-/Winterzeit korrekt. Alarm 00:15 korrekt eingeplant, kein Mitternachts-Guard-Block. |
| EC-12 | Multi-Device Claim Sync | Profil auf Gerät A geclaimt → Gerät B erkennt es ohne manuellen Refresh. |
| EC-13 | Kein Stale-Schedule nach Familienwechsel | Wechsel in neue Familie → kein Zeitplan/Mitglied der alten Familie kurzzeitig sichtbar. |
| EC-14 | Tagesreset + Pause-Reset | „Bin wach" und „Heute pausieren" am nächsten Tag automatisch zurückgesetzt. |

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

- **Automatisiert:** 7 JUnit-Tests für `Scheduler` in `app/src/test` (Standard-4er-Familie, Konflikte, Pause, NoActiveMembers, Breakfast-Clamping, Awake-Member, Single-Member).
- **Manuell:** Vor jedem Release ein Live-Test über eine Nacht in einer Test-Familie.
