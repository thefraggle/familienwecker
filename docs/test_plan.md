# 🧪 Testplan: FamWake
**Version:** 1.7.3
**Datum:** 2026-04-10
*[🇬🇧 English version](test_plan.en.md)*

---

## 📋 Strategie

Tests validieren nicht nur die UI, sondern insbesondere die Korrektheit des Planungsalgorithmus.

**Bereiche:** Onboarding & Account · Familienmanagement · Planungs-Logik · Wecker · Edge Cases

---

## 🛠 Normalbetrieb

### 1. Account & Onboarding
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-01 | Erst-Registrierung mit E-Mail | Account wird erstellt, Bestätigungs-E-Mail versendet. |
| TC-02 | Familie gründen | Benutzer wird Admin, Einladungscode generiert. |
| TC-03 | Familie beitreten (Code & Deep Link) | Beitritt per Code und per `/join/`-Link funktioniert; App öffnet sich statt Browser. |
| TC-04 | Selbst-Beitritt Guard | Eigener Code führt nicht zu erneutem Join; keine Profilzuordnung verloren. |
| TC-05 | Join Code Sicherheit | 6-stelliger alphanumerischer Code (ohne 0/O/1/I), via SecureRandom. Ungültiger Code → Fehlermeldung, alte Familie bleibt. |
| TC-06 | Passwort-Reset | E-Mail wird versendet. Unbekannte E-Mail: immer gleiche Erfolgsmeldung (User Enumeration Prevention). |
| TC-07 | Autofill & Passwort-Manager | E-Mail-Feld schlägt gespeicherte Adressen vor; Passwort-Manager bietet Autofill an. |
| TC-08 | Login-Validierung | Zu kurzes Passwort / ungültige E-Mail → Fehlermeldung sofort, kein Absturz. |
| TC-09 | Familie verlassen / löschen | Eigenes Profil gelöscht. Admin-only Löschen: Nicht-Creator erhält Fehlermeldung. Bei leerer Familie: einfache Bestätigung. |
| TC-10 | Multi-Account Isolation | User A ausloggen, B einloggen → B sieht nur seine Familie. |
| TC-11 | Onboarding-Tour | Erststart zeigt 5 Slides mit Panda. Tour aus Einstellungen wieder aufrufbar. Abschluss landet korrekt im Hauptscreen. |
| TC-12 | Rate-Limiting | >5 falsche Codes/min bzw. >5 Reset-Mails/h → serverseitige Blockierung ("Resource Exhausted"). |

### 2. Mitglieder & Familienkonfiguration
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-20 | Auto-Claim erstes Profil | Erstes Mitglied auto-geclaimt, Wecker aktiviert, kein "Kein Profil"-Flash. |
| TC-21 | Profil bearbeiten (2. User) | Zweites Familienmitglied kann Profil anlegen, bearbeiten, speichern – kein Sync-Fehler. |
| TC-22 | Kachel-Status nach Claim | Nach Auto-Claim zeigt Kachel korrekt "Wecker aktiv/inaktiv" – kein dauerhaftes Fehlen. |
| TC-23 | Rechte-Schutz fremde Profile | Geclaimte Profile anderer: kein Edit-Icon, kein Klick-Effekt. |
| TC-24 | Mitglieder-Limit | Bei 6 Mitgliedern ist "Hinzufügen" gesperrt. |
| TC-25 | Pausieren | Unclaimed Member pausieren → korrekt pausiert, kein Fehler. Eigenes Profil: kein Pausieren-Knopf sichtbar. |
| TC-26 | Wochentag-Validierung | latestWakeUp ≤ earliestWakeUp oder Abfahrtszeit zu früh → Fehlertext, Speichern gesperrt. |
| TC-27 | Drag & Drop Reihenfolge | Long-Press → verschieben mit Gap-Preview. Reihenfolge bleibt nach Neustart und auf anderen Geräten. |
| TC-28 | Nächster aktiver Tag | Heutigen Tag deaktiviert, morgen aktiv → Hauptscreen zeigt morgigen Wecker korrekt. Kein aktiver Tag → NoActiveSchedule. |
| TC-29 | Zeitauswahl (Profil bearbeiten) | Tippen auf Uhrzeit öffnet Tastatur-Dialog (kein Uhrzeiger). Tipp außerhalb des Namensfelds schließt Tastatur. Badzeit-Buttons (−/+) immer sichtbar. |

### 3. Wecker & Alarm
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-30 | Wecker klingelt (Hintergrund) | App im Hintergrund → Alarm klingelt, RingingActivity öffnet sich. |
| TC-31 | Wecker nach Neustart | Alarm nach Geräterestart korrekt wiederhergestellt (auch vor PIN-Eingabe). |
| TC-32 | Wecker nach Deaktivieren + Aktivieren | Wecker aus → Wecker an → Alarm klingelt zum eingeplanten Zeitpunkt. |
| TC-33 | „Ich bin wach"-Button – Zeitfenster | Weckzeit morgen 07:00, aktuelle Zeit 15:00 → Button inaktiv. Bei 05:30 → aktiv (2h-Fenster). |
| TC-34 | „Ich bin wach"-Button nach Profilneuanlage | Member löschen, neu anlegen → Button korrekt aktiv/inaktiv je nach Profil-Weckzeit. |
| TC-35 | „Ich bin wach" – Zurücksetzen | Wacher Status → Status-Reset jederzeit möglich (Button bleibt klickbar). |
| TC-36 | „Ich bin wach" – Toggle-Effekt | Button stoppt System-Wecker sofort; Farbe/Icon wechselt. Globaler Switch aus → Status zurückgesetzt. |
| TC-37 | „Ich bin wach" – Tageswechsel | App über Nacht im Hintergrund → Button zeigt am nächsten Morgen wieder Ausgangszustand. |
| TC-38 | Snooze | Snooze (5 Min) plant neuen Alarm exakt 5 Min später. Banner mit Endzeit + Abbruch-Button. Kein Konflikt mit regulärem Alarm. |
| TC-39 | Alarm-Status Sync | Wecker auf Gerät A aus → Gerät B sieht Status sofort (kein eigener Status beeinflusst). |
| TC-40 | Alarm-Berechtigungs-Warnung (Android 14+) | Kein SCHEDULE_EXACT_ALARM → rote Kachel auf Hauptscreen, klickbar zur System-Einstellung. |
| TC-41 | Globaler Wecker nach Neustart AN | Wecker einschalten → App neu starten → Wecker bleibt AN (kein Race-Condition-Reset). |

### 4. Sicherheit & Admin
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-50 | Firestore Member-Schutz | Versuch, name/claimedByUserId eines anderen Members zu schreiben → PERMISSION_DENIED. |
| TC-51 | Admin-Sichtbarkeit | Nicht-Admin sieht keine Admin-Buttons. Globaler Admin sieht Admin-Modal; 2-Min-Wecker auslösbar. |
| TC-52 | Feedback nur eingeloggt | Feedback via API ohne Auth → unauthenticated-Fehler, kein Eintrag. |
| TC-53 | Member-Löschung Eigentumsschutz | Als Member versuchen, fremdes Profil zu löschen → permission-denied. |
| TC-54 | XSS im Feedback | `<script>alert(1)</script>` senden → E-Mail zeigt Code als Text, keine Ausführung. |

### 5. Lokalisation
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-60 | Sprach-Fallback (unbekannter Code) | Unbekannter oder ungültiger Sprachcode → App fällt auf Englisch zurück, nicht auf Deutsch. Auch beim Laden aus dem Speicher. |
| TC-61 | Dialektsprachen | Schwäbsch / Schwiizerdeutsch / Ruhrpott auswählen → korrekte Texte,  kein Absturz. |
| TC-62 | Weitere Sprachen | FR/ES/IT/SV/TR/UK/RU/NO/DA → UI-Texte korrekt, keine Fallbacks auf DE. |
| TC-63 | Sprachpicker – alphabetisch | Öffnet BottomSheet, Sprachen in korrekter Reihenfolge, System immer zuerst. |
| TC-64 | Sprachpicker kleine Screens | Auf kleinem Gerät (360dp Breite): Grid scrollt, kein Abschneiden. |

---

## ⚠️ Edge Cases

### Konfliktsituationen
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| EC-01 | Unmöglicher Plan | Alle wollen gleichzeitig ins Bad → Konflikt-Warnung + Kompromissvorschlag. |
| EC-02 | Best-effort Fallback | Unmöglicher Plan → aktiver Nutzer erhält dennoch einen Wecker zur bestmöglichen Zeit. |
| EC-03 | Kurzes Zeitfenster | Wecken 7:00, Abfahrt 7:05 → App warnt vor zu knappem Fenster. |

### Technische Grenzfälle
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| EC-10 | Offline-Berechnung | Letzter Plan lokal gespeichert; Wecker klingelt auch ohne Internet. |
| EC-11 | Offline-Anzeige | Netz trennen → CloudOff-Icon nach >3s. Nach Reconnect sofort weg. |
| EC-12 | Offline-Profil-Claim | Im Flugmodus → Snackbar sofort, kein Spinner. |
| EC-13 | Zeitumstellung | Weckzeiten korrekt, keine doppelten Alarme. |
| EC-14 | Mitternachts-Wecker (00:15) | Korrekt eingeplant und ausgelöst, kein Mitternachts-Guard-Block. |
| EC-15 | Multi-Device Claim Sync | Profil auf Gerät A geclaimt → Gerät B erkennt es ohne Refresh. |
| EC-16 | Kein Stale-Schedule nach Familienwechsel | Kein Zeitplan/Mitglied der alten Familie kurzzeitig sichtbar. |
| EC-17 | Server-Cleanup | Familien >180 Tage ohne Update werden sonntags gelöscht. Neue Familien nie. |

### Benutzerverhalten
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| EC-20 | Pausieren | ⏸️ entfernt Member aus Plan; andere schlafen ggf. länger. |
| EC-21 | Mitternachts-Reset | „Bin wach" und „Heute pausieren" am nächsten Tag zurückgesetzt. |
| EC-22 | Reboot-Alarm (Direct Boot) | Alarme nach Reboot auch vor PIN-Eingabe korrekt wiederhergestellt. |
| EC-23 | Selbst-Healing Firestore | Flugmodus an/aus → Listener fängt Fehler ab, re-sync lautlos. |

---

## 📱 UI/UX & Barrierefreiheit

- **Dark Mode:** Augenfreundliche Kontraste; AMOLED Black (`#000000`).
- **Material You:** Dynamische Farben ab Android 12.
- **Touch-Targets:** Mindestens 24 dp für alle Interaktionselemente.
- **Haptik:** Vibrationsmuster für Vor- und Hauptalarm.

| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| UI-01 | Theme-Switcher | Sonne/Auto/Mond-Icons wechseln sofort; kein Überlauf auf schmalen Geräten. |
| UI-02 | Wochenplan-Kopierlink | Nach Öffnen des Wochenplanbereichs ist der "Auf andere Tage kopieren"-Link ohne Scrollen sichtbar. |
| UI-03 | Kleine Screens scrollen | Auf 360dp-Gerät: FamilySetupScreen, SettingsScreen, AddMemberScreen – Tastatur schiebt Content hoch, Button bleibt erreichbar. |
| UI-04 | Feldreihenfolge | Wochenplan-Card: Frühste Weckzeit, Späteste Weckzeit, Badzeit, Haus verlassen, Frühstück. |

---

## 📈 Validierung

- **Automatisiert:** JUnit-Tests für `Scheduler` in `app/src/test` (TC-07–TC-09, EC-01, NoActiveMembers, typsichere `ScheduleMessage`-Codes).
- **Manuell:** Vor jedem Release ein Live-Test über eine Nacht in einer Test-Familie.
