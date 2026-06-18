# 🧪 Testplan: FamWake
**Version:** 2.0.0
**Datum:** 2026-06-18

---

## 📋 Strategie
Scheduler-Korrektheit, Wecker-Zuverlässigkeit, Datensicherheit und geräteübergreifender Sync.

---

## 🛠 Normalbetrieb

### 1. Account & Familien
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-01 | Registrierung & Login | E-Mail & Google Login. Passwort-Reset. Inaktive Buttons erst nach vollständiger Eingabe klickbar. |
| TC-02 | Lazy Registration | Onboarding-Tour (4 Slides), anonymer Nutzer erstellt (Double-Click-Schutz). Prominente Warnung auf dem Setup-Screen für Gast-Accounts. |
| TC-03 | Familien-Lifecycle | Gründen + Beitreten per Code/Link. Nur Creator darf löschen. Verlassen/Löschen deaktiviert lokale Wecker sofort. Fehlermeldung bei Netzwerkproblemen. |
| TC-04 | DSGVO-Datenlöschung | Konto löschen entfernt alle Daten inkl. Push-Tokens und entclaimt Profile. |

### 2. Mitglieder & Konfiguration
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-20 | Profil-Verwaltung | Erstes Mitglied auto-geclaimt. Bei Neuinstallation: manueller Claim → Weckplan sofort da. |
| TC-21 | Zeitvalidierung | latestWakeUp ≤ earliestWakeUp blockiert Speichern. Nachtschicht-Zeiten (z.B. Wake 22:00, Leave 00:15) werden korrekt akzeptiert. |
| TC-22 | Reorder & Wochentage | Drag & Drop → Bestätigungsdialog. „Ganze Woche" zeigt Warnung bei tagesspezifischen Unterschieden. Abbrechen setzt visuell zurück. |
| TC-23 | Puffer (global & individuell) | Globaler Puffer (0–15 Min). Im Editor: kursiv = geerbt, fett = Override. Manuelle 0m überschreiben globalen Wert. |
| TC-24 | Einfacher Modus | Blendet erweiterte Optionen aus. Scheduler weist feste Weckzeit zu. |
| TC-25 | Offline-Schutz | Mitglieder löschen bei Offline → Fehlermeldung statt stille Pufferung. |

### 3. Wecker, Alarm & Snooze
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-30 | Alarm-Zyklus | Wecker klingelt zuverlässig (Hintergrund, Sperrbildschirm). Stop → App öffnet mit Begrüßung. Kein doppelter Ton. |
| TC-31 | Snooze (1/2) | Eigene Weckzeit +5 Min, Badzeit absorbiert Verschiebung. Andere nicht verschoben. Banner: „Schlummern bis HH:MM (1/2)". Zähler zeigt korrekt 1/2. |
| TC-32 | Snooze (2/2) | Eigene Weckzeit +5 Min, volle Badzeit. Nachfolgende Mitglieder werden verschoben. Banner zeigt korrekt (2/2). |
| TC-33 | Snooze-Maximum | 3. Snooze-Klick: Alarm endet, Begrüßung erscheint beim Entsperren, Lock-Screen-Hinweis „Snooze nicht möglich". |
| TC-34 | Snooze abbrechen | „Abbrechen" im Banner → Snooze-State reset, Plan neu berechnet, Sync auf alle Geräte. |
| TC-35 | Ghost-Alarm-Schutz | Kein Wecker wenn globaler Schalter OFF, wenn eigener Member pausiert wird (bei aktiven anderen), nach Neustart oder Mitglieder-Bearbeitung. |
| TC-35b | Snooze-Reset Folgetag | Nach 2× Snooze gestern: Snooze-Button heute wieder aktiv (Count automatisch zurückgesetzt). |
| TC-35c | Lock-Screen Snooze | Snooze-Button auf AlarmKit-Sperrbildschirm: Alarm stoppt, neuer Alarm in 5 Min geplant, Banner erscheint. |
| TC-36 | „Ich bin wach" | Stoppt Wecker, Button bleibt am Weckertag sichtbar (oder 4h vorher). Reset am Folgetag. |
| TC-37 | Snooze nach Reboot | Gerät neustarten während Snooze aktiv → Snooze-Alarm wird aus AlarmBackupPrefs wiederhergestellt. |

### 4. Sync & Benachrichtigungen
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-40 | Push-Events | Manuelle Änderungen senden stumme Pushes an Mitbewohner (kein Self-Push). Auto-Resets werden gefiltert. Toggle OFF → keine Pushes. |
| TC-41 | Multi-Device Sync | Claim, Alarm-Status, Reihenfolge und Snooze synchronisieren sofort. 💤-Symbol bei snoozendem Mitglied. |
| TC-42 | Pause-Toggle | Sofortiger lokaler State, kein Flackern durch eingehende Snapshots. |

---

## ⚠️ Edge Cases

| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| EC-01 | Zeitkonflikte | Kompromissvorschläge, AutoFix nur für Ziel-Wochentag. Puffer-Reduktion vor Zeitverschiebung. Warnung bei >6 aktiven Mitgliedern. |
| EC-02 | Offline-Betrieb | CloudOff-Icon (inkl. Firestore Cache Status). Kein Absturz. Re-Sync nach Reconnect. Optimistic UI mit Rollback. |
| EC-03 | Backend-Schutz | Zugriff auf fremde Profile → PERMISSION_DENIED. |
| EC-04 | Bildschirm-Timeout | Bildschirm bleibt nur im aktiven Vordergrund wach, nicht in Sheets oder im Hintergrund. |
| EC-05 | Dynamic Type | Große Systemschrift (Accessibility): Buttons und Chips skalieren mit, kein Clipping. |

---

## 🎨 UI & Lokalisation

| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| UI-01 | Layout | Tastatur verdeckt keine Buttons. Tablet-Hochformat. iPhone SE Weekday-Chips scrollbar. |
| UI-02 | Sprachen & Themes | 25 Sprachen absturzfrei. Dark/Light sofort. |
| UI-03 | Fehlerhinweise | Sync-Fehler (Reihenfolge, Pause, Snooze, Puffer) → verständliche Meldung. |
| UI-04 | Barrierefreiheit | VoiceOver (iOS) / TalkBack (Android): alle Buttons, Toggles, Picker mit Label. Initialen-Avatare lesbar. |
| UI-05 | Initialen-Avatare | Farbiger Kreis mit 2 Buchstaben pro Mitglied. Deterministische Farbe basierend auf Name. |

---

## 📈 Validierung
- **Automatisiert:** 13 Unit-Tests für Scheduler (inkl. Buffer-Tests) in GitHub Actions.
- **Manuell:** Vor jedem Release Live-Test auf mind. 2 Geräten (Snooze-Sync, Ghost-Alarm, Multi-Device).
