# 🧪 Testplan: FamWake
**Version:** 2.0.2
**Datum:** 2026-07-12

---

## 📋 Strategie
Scheduler-Korrektheit, Wecker-Zuverlässigkeit, Navigation, Datensicherheit und geräteübergreifender Sync.

---

## 🛠 Normalbetrieb

### 1. Account & Familien
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-01 | Registrierung & Login | E-Mail, Google, Apple, Anonym. Passwort-Reset. Buttons erst nach Eingabe aktiv. |
| TC-02 | Onboarding | 4 Slides, anonymer Nutzer erstellt (Double-Click-Schutz). Gast-Warnung auf Setup-Screen. |
| TC-03 | Familien-Lifecycle | Gründen + Beitreten (Code/Link). Nur Creator löscht. Verlassen deaktiviert Wecker sofort. Fehlermeldung bei Netzproblemen. |
| TC-04 | DSGVO-Löschung | Konto löschen entfernt alle Daten inkl. Push-Tokens und entclaimt Profile. |

### 2. Mitglieder & Konfiguration
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-20 | Profil-Verwaltung | Erstes Mitglied auto-geclaimt. Neuinstallation: manueller Claim → Weckplan sofort da. |
| TC-21 | Zeitvalidierung | latestWakeUp ≤ earliestWakeUp blockiert Speichern. Nachtschicht-Zeiten korrekt akzeptiert. |
| TC-22 | Reorder & Wochentage | Drag & Drop mit Bestätigungsdialog. „Ganze Woche"-Warnung bei Unterschieden. Abbrechen setzt zurück. |
| TC-23 | Puffer | Global (0–15 Min). Kursiv = geerbt, fett = Override. Manuelles 0m überschreibt global. |
| TC-24 | Einfacher Modus | Erweiterte Optionen ausgeblendet. Feste Weckzeit zugewiesen. |
| TC-25 | Offline-Erstellung | Flugmodus: Familie → Mitglieder → Claim → Wecker stellt sich. App-Kill + Neustart: Daten erhalten. |

### 3. Wecker, Alarm & Snooze
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-30 | Alarm-Zyklus | Wecker klingelt (Hintergrund, Sperrbildschirm, Lock-Screen). Stop → Begrüßung. Kein Doppelton. |
| TC-31 | Snooze (1/2 → 2/2) | 1. Snooze: +5 Min, Badzeit absorbiert. 2. Snooze: volle Badzeit, Nachfolgende verschoben. Banner + Zähler korrekt. |
| TC-32 | Snooze-Maximum | 3. Klick: Alarm endet, Begrüßung erscheint. Lock-Screen: „Snooze nicht möglich". |
| TC-33 | Snooze nach Reboot | Gerät neu starten während Snooze → Alarm aus Backup wiederhergestellt. |
| TC-34 | Ghost-Alarm-Schutz | Kein Wecker bei: globalem Schalter OFF, pausiertem Member, nach Neustart oder Bearbeitung. Snooze-Count am Folgetag zurückgesetzt. |

### 4. Sync & Push
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-40 | Push-Events | Manuelle Änderungen → stumme Pushes an Mitbewohner (kein Self-Push). Auto-Resets gefiltert. Toggle OFF → keine Pushes. |
| TC-41 | Multi-Device Sync | Claim, Alarm-Status, Reihenfolge, Snooze synchron. 💤-Symbol bei Snooze. |
| TC-42 | Pause-Toggle | Sofortiger lokaler State, kein Flackern durch Snapshots. |
| TC-43 | Offline→Online Sync | Offline-Familie → Flugmodus aus → echte ID + Join-Code. Firestore-Listener aktiv. |

### 5. Navigation & UI (neu in v2.0.2)
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-50 | NavigationStack (iOS) | Schneller Wechsel Hauptbildschirm ↔ Einstellungen ↔ Mitglied-Editor: kein Crash. Zurück-Button-Text nicht abgeschnitten. |
| TC-51 | Modal-Close-Buttons | Alle Dialoge/Sheets: einheitlicher X-Button oben rechts. Schließen per ESC/Backdrop funktioniert. |
| TC-52 | Donations deaktiviert | iOS: Kein Donation-Button in Einstellungen sichtbar. Android: Donation funktionsfähig. |

---

## ⚠️ Edge Cases

| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| EC-01 | Zeitkonflikte | Kompromissvorschläge, AutoFix nur für Ziel-Wochentag. Puffer-Reduktion vor Zeitverschiebung. Warnung bei >6 Mitgliedern. |
| EC-02 | Offline (4+ Tage) | Alarm funktioniert, kein Absturz. Bei Reconnect: Auto-Sync. Family-Join einzige Ausnahme. |
| EC-03 | Backend-Schutz | Zugriff auf fremde Profile → PERMISSION_DENIED. |
| EC-04 | Bildschirm-Timeout | Bildschirm nur im aktiven Vordergrund wach, nicht in Sheets oder Hintergrund. |

---

## 🎨 UI & Barrierefreiheit

| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| UI-01 | Layout | Tastatur verdeckt keine Buttons. iPhone SE Chips scrollbar. |
| UI-02 | Sprachen & Themes | 25 Sprachen absturzfrei. Dark/Light sofort. |
| UI-03 | Fehlerhinweise | Sync-/Netzfehler → verständliche Meldung. |
| UI-04 | Barrierefreiheit | VoiceOver/TalkBack: alle Buttons, Toggles, Picker gelabelt. Initialen-Avatare lesbar. Dynamic Type: kein Clipping. |

---

## 📈 Validierung
- **Automatisiert:** 13 Unit-Tests für Scheduler (inkl. Buffer) in GitHub Actions.
- **Manuell:** Vor Release: Live-Test auf mind. 2 Geräten (Snooze-Sync, Ghost-Alarm, Multi-Device, Navigation).
