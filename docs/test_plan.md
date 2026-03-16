# 🧪 Testplan - FamWake

*[🇬🇧 English version](test_plan.en.md)*

---

## 📋 Übersicht & Strategie

Tests validieren nicht nur die UI, sondern insbesondere die mathematische Korrektheit des Planungsalgorithmus.

### Testbereiche
1.  **Onboarding & Account:** Registrierung, Login, Familienbeitritt.
2.  **Familienmanagement:** Mitglieder hinzufügen/entfernen, Rollen.
3.  **Planungs-Logik (Kern):** Bad-Taktung, Frühstücksplanung, Pufferzeiten.
4.  **Wecker-Funktion:** Sound, Fullscreen-Notification, Snooze.
5.  **Edge Cases & Robustheit:** Offline-Status, Zeitzonen, Konfliktsituationen.
6.  **Assets & Ressourcen:** Icon-Scaling, Splash-Screen Integrität.

---

## 🛠 Testfälle (Normalbetrieb)

### 1. Account & Onboarding
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-01 | Erst-Registrierung mit E-Mail | Account wird erstellt, Bestätigungs-E-Mail versendet. |
| TC-02 | Familie gründen | Benutzer wird "Admin" einer neuen Familie, Einladungscode wird generiert. |
| TC-03 | Familie beitreten | Benutzer tritt per Code einer bestehenden Familie bei. |
| TC-20 | Passwort vergessen (Reset) | Branded E-Mail wird versendet, Link führt zur branded HTML-Seite, Passwortänderung funktioniert. |
| TC-21 | **Einladungs-Sharing** | Klick auf "Teilen" öffnet den Android-Dialog mit dem Familien-Code und Link. |
| TC-22 | **Deep Link (App-Start)** | Klick auf einen `/join/` Link öffnet direkt die App (statt Browser). |
| TC-23 | **Auto-Join via Link** | App erkennt den Code aus der URL und zeigt den "Beitreten"-Dialog an. |
| TC-24 | **Re-Join Robustheit** | Beitritt zur eigenen Familie via Link löscht die Profil-Zuordnung nicht mehr. |
| TC-25 | **Self-Healing (Deep Link)** | Login via Link führt auch bei existierendem Alt-Profil zum neuen Beitritt. |
| TC-26 | **Single Instance Check** | Mehrfacher Klick auf Deep Links öffnet keine neuen App-Instanzen im Task-Manager. |
| TC-27 | **Selbst-Beitritt Guard** | Eingabe des eigenen Familien-Codes im Beitritt-Dialog führt **nicht** zu einem erneuten Join (keine Profilzuordnung verloren). |
| TC-28 | **Deep Link Conflict Dialog** | Klick auf Deep Link während man in einer Familie ist, öffnet MainScreen mit Warn-Dialog zum Wechseln. |
| TC-29 | **Join Code Validation Guard** | Ungültiger Deep/Join Code führt nach Bestätigung nur zu einer Fehlermeldung, alte Familie wird nicht verlassen. |
| TC-30 | **Join Code Sicherheit** | Ein generierter 6-stelliger Code ist weiterhin rein alphanumerisch ohne 0, O, 1, I und wird via SecureRandom erzeugt. |
| TC-31 | **Verschlüsselung (H-5)** | Nach Update von Altversion: Daten (familyId, joinCode) werden automatisch in EncryptedPrefs migriert; Original-Datei wird gelöscht. |
| TC-32 | **Rate-Limiting (H-1)** | Mehrfalsche falsche Code-Eingaben (>5/min) führen zu einer serverseitigen Blockierung ("Resource Exhausted"). |
| TC-33 | **E-Mail Rate-Limiting** | Mehr als 3 Passwort-Reset- oder Verifikations-E-Mails für dieselbe Adresse innerhalb einer Stunde werden serverseitig blockiert ("Resource Exhausted"). |
| TC-34 | **HTTP-Link abgewiesen** | Aufruf von `http://familienwecker.de/join/CODE` darf keinen Join auslösen – App ignoriert das HTTP-Schema. |
| TC-35 | **Admin-only Delete** | Nicht-Ersteller öffnet Settings → Familie löschen → erhält Fehlermeldung statt Dialog. |
| TC-36 | **Offline Profil-Claim gesperrt** | Im Flugmodus Dropdown öffnen → Snackbar mit Offline-Fehlermeldung erscheint sofort. |
| TC-37 | **Deep-Link Sofort-Dialog (Hintergrund)** | App auf Settings-Screen, Join-Link öffnen → Conflict-Dialog erscheint sofort ohne Zurück-Navigation. |
| TC-38 | **Akku-Kachel Sofort-Reset** | Akku-Optimierung deaktivieren, zur App zurückkehren → Kachel verschwindet sofort ohne Screen-Wechsel. |
| TC-39 | **Scroll-Indicator** | Hauptscreen ohne Mitglieder: Bounce-Pfeil am unteren Rand sichtbar. Verschwindet beim ersten Scrollen oder sobald ein Mitglied vorhanden ist. |
| TC-40 | **Reboot-Alarm-Persistenz** | Alarm für 2 Min. stellen → Gerät neustarten → Wecker klingelt auch ohne PIN-Eingabe auf dem Sperrbildschirm. |
| TC-41 | **Snooze überlebt Reboot** | Snooze drücken → Gerät innerhalb 5 Min. neustarten → Wecker klingelt zum Snooze-Zeitpunkt. |
| TC-42 | **E-Mail-Rate-Limit erste Anfrage** | Passwort-Reset für neue (nie angeforderte) E-Mail → E-Mail wird korrekt versendet, kein interner Fehler. |


### 2. Familien-Konfiguration
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-04 | Weck-Präferenzen ändern | Späteste Weckzeit und Bad-Dauer werden gespeichert. |
| TC-05 | Frühstücks-Wunsch umschalten | Algorithmus berücksichtigt das Mitglied bei der Frühstückszeit-Berechnung. |
| TC-06 | Haus-Verlassen Zeit setzen | Plan wird angepasst, damit die Person rechtzeitig fertig ist. |
| TC-10 | **Mitglieder-Limit** | Bei 6 Mitgliedern wird der „Hinzufügen"-Button gesperrt. |
| TC-11 | **Rechte-Schutz** | Profile von anderen (geclaimt) zeigen kein Edit-Icon und reagieren nicht auf Klick. |
| TC-12 | **Drag & Drop Reihung** | Long-Press auf eine Zeitplan-Kachel erlaubt das Verschieben. Andere Kacheln gleiten via Spring-Animation beiseite (Gap-Preview). |
| TC-13 | **Drag & Drop Persistence** | Tausch und Firestore-Sync erst beim Loslassen. Neue Reihenfolge bleibt auch nach App-Neustart und auf anderen Geräten erhalten. |
| TC-14 | **Offline-Indikator** | Im Flugmodus erscheint Cloud-Off Icon; bei Änderung Sync-Icon; Reset nach Re-connect. |

### 3. Planungs-Logik (Algorithmische Tests)
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-07 | Standard-Szenario (4 Personen) | Ein Zeitplan ohne Bad-Überschneidungen wird erstellt. |
| TC-08 | Frühstücks-Koordination | Alle "Frühstücker" sind vor der gemeinsamen Zeit mit dem Bad fertig. |
| TC-09 | Masterplan-Update | Wenn ein Mitglied früher aufsteht, wird der Plan für die restliche Familie optimiert (späteres Wecken). |

---

## ⚠️ Edge Cases (Grenzfälle)

### 1. Konfliktsituationen (Stress-Tests)
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| EC-01 | **Unmöglicher Plan** | Alle wollen gleichzeitig ins Bad. | App zeigt Konflikt-Warnung und Kompromissvorschläge. |
| EC-02 | **Extreme Bad-Dauer** | Mitglied mit 120 Min. | Plan verschiebt andere massiv; ggf. Warnung. |
| EC-03 | **Kurze Zeitfenster** | Wecken 7:00, Abfahrt 7:05. | App warnt vor zu knappem Zeitfenster. |

### 2. Technische Grenzfälle
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| EC-04 | **Offline-Berechnung** | Letzter Plan lokal gespeichert; Wecker klingelt auch ohne Internet. |
| EC-05 | **Zeitumstellung** | Weckzeiten korrekt angepasst, keine doppelten Alarme. |
| EC-06 | **App-Absturz während Alarm** | Alarm-Dienst startet automatisch neu. Alarm überlebt auch Geräteneustart (AlarmBackupPrefs + LOCKED_BOOT_COMPLETED). |
| EC-07 | **Akku-Optimierung** | App als „Nicht optimiert" markiert. |
| EC-11 | **Snooze** | Snooze (5 Min) plant neuen Alarm exakt 5 Min später. |
| EC-12 | **Mitternachts-Reset** | „Heute pausieren" und „Bin wach" am nächsten Tag zurückgesetzt. |
| EC-14 | **Persistenz & Logout** | Keine alten Login-Daten oder Familien-IDs nach Logout/Neuinstallation. |
| EC-15 | **Familien-Löschen** | Doppelte Bestätigung bei anderen Mitgliedern; einfach bei leerer Familie. |
| EC-16 | **Mitglied löschen** | Bestätigungs-Dialog vor dem Löschen. |
| EC-17 | **Neu-Anlage nach Löschung** | Neue Familie direkt anlegbar, kein Infinity-Spinner. |
| EC-18 | **Daten-Resilienz** | Familie auf anderem Gerät gelöscht → automatischer Reset zum Setup. |
| EC-19 | **Multi-Device Claim Sync** | Profil auf Gerät A geclaimt → Gerät B erkennt es ohne Refresh. |
| EC-20 | **Icon-Skalierung** | Icons und Splash-Screen korrekt auf xhdpi bis xxxhdpi. |
| EC-21 | **Calc-Limit** | Plan für >6 aktive Mitglieder wird auf 6 begrenzt. |
| EC-22 | **Server-Cleanup** | Familien ohne Update seit 180 Tagen werden sonntags gelöscht. Neue Familien (< 180 Tage alt) werden nie gelöscht. |
| EC-23 | **Fehlende Alarm-Berechtigung** | Lokalisierte Fehlermeldung im UI; kein Toast, kein Absturz. |
| EC-24 | **Touch-Targets (A11y)** | Mindestens 24dp groß. |
| EC-25 | **Offline-Start** | Lade-Screen wechselt nach max. 2s zum Dashboard. |
| EC-26 | **Offline-Join** | Sofortige Fehlermeldung; kein endloser Spinner. |
| EC-27 | **Captive Portal** | Offline-Status via `NET_CAPABILITY_VALIDATED` erkannt. |
| EC-28 | **Mitternachts-Guard** | Frühes Abfahrtsziel + lange Badzeit → Konflikt statt fehlerhafte Zeiten. |
| EC-29 | **Gerätespezifischer Alarm-Switch** | Alarm auf Gerät A aus → Gerät B unverändert. |
| EC-30 | **Lokalisierte Auth-Fehler** | Fehlermeldung in Systemsprache, nicht Englisch. |
| EC-31 | **Alarm-Status Sync** | User A deaktiviert Wecker → Gerät B sieht es sofort, eigener Status bleibt. |
| EC-32 | **Familie löschen mit anderen Usern** | Admin löscht → alle herausgeworfen, Familie gelöscht. |
| EC-33 | **Offline-Icon bei Writes** | Nach 3s Offline → CloudOff-Icon statt Sync-Spinner. |
| EC-34 | **Familie-Verlassen (Isolation)** | Papa verlässt Familie auf Gerät A → Mamas Session auf Gerät B bleibt unverändert; kein ungewolltes leaveFamily. |
| EC-35 | **Ghost-Claim nach Verlassen** | Nach Papa's Verlassen ist Papas Mitglieds-Profil in Firestore nicht mehr geclamet (claimedByUserId = null). |


### 3. Benutzer-Verhalten
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| EC-08 | **Pausieren für heute** | ⏸️ Icon entfernt Mitglied komplett aus dem Plan; andere schlafen ggf. länger. |
| EC-13 | **Bin schon wach (☀️)** | Sonnen-Icon unterdrückt nur den Alarm; Badezimmer-Slot bleibt für andere reserviert. |
| EC-09 | Nachträgliche Änderung (2 Uhr nachts) | Plan wird neu berechnet und asynchron an alle synchronisiert. |
| EC-10 | Mehrere Admins | Beide ändern gleichzeitig die Badzeit eines Kindes -> Last-Write-Wins oder Konfliktmeldung. |

---

## 📱 UI/UX & Barrierefreiheit

- **Dark Mode:** Kontraste augenfreundlich; AMOLED Black (`#000000`) für Akku-Effizienz.
- **Material You:** Dynamische Farben ab Android 12.
- **Haptik:** Unterschiedliche Vibrationsmuster für Voralarm und Hauptalarm.
- **Echtzeit-Feedback:** Kurze Animation bei Plan-Neuberechnung.

---

## 📈 Validierung & Reporting

- **Automatisierung:** Die Kern-Logik (`Scheduler`) wird über JUnit-Tests (zu finden in `app/src/test`) mit den Szenarien aus TC-07 bis TC-09, EC-01 und dem neuen `NoActiveMembers`-Test abgedeckt. Alle Tests prüfen jetzt typsichere `ScheduleMessage`-Codes statt Rohstrings.
- **Manuelle Abnahme:** Vor jedem Release erfolgt ein "Live-Test" über eine Nacht in einer Test-Familie.
