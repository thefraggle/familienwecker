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
| TC-32 | **Rate-Limiting (H-1)** | Mehrfalsche falsche Code-Eingaben (>5/min) führen zu einer serverseitigen Blockierung ("Resource Exhausted"). |
| TC-33 | **E-Mail Rate-Limiting** | Mehr als 5 Passwort-Reset- oder Verifikations-E-Mails für dieselbe Adresse innerhalb einer Stunde werden serverseitig blockiert ("Resource Exhausted"). |
| TC-34 | **HTTP-Link abgewiesen** | Aufruf von `http://familienwecker.de/join/CODE` darf keinen Join auslösen – App ignoriert das HTTP-Schema. |
| TC-35 | **Admin-only Delete** | Nicht-Ersteller öffnet Settings → Familie löschen → erhält Fehlermeldung statt Dialog. |
| TC-36 | **Offline Profil-Claim gesperrt** | Im Flugmodus Dropdown öffnen → Snackbar mit Offline-Fehlermeldung erscheint sofort. |
| TC-37 | **Deep-Link Sofort-Dialog (Hintergrund)** | App auf Settings-Screen, Join-Link öffnen → Conflict-Dialog erscheint sofort ohne Zurück-Navigation. |
| TC-38 | **Akku-Kachel Sofort-Reset** | Akku-Optimierung deaktivieren, zur App zurückkehren → Kachel verschwindet sofort ohne Screen-Wechsel. |
| TC-39 | **Scroll-Indicator** | Hauptscreen ohne Mitglieder: Bounce-Pfeil am unteren Rand sichtbar. Verschwindet beim ersten Scrollen oder sobald ein Mitglied vorhanden ist. |
| TC-40 | **Reboot-Alarm-Persistenz** | Alarm für 2 Min. stellen → Gerät neustarten → Wecker klingelt auch ohne PIN-Eingabe auf dem Sperrbildschirm. |
| TC-41 | **Snooze überlebt Reboot** | Snooze drücken → Gerät innerhalb 5 Min. neustarten → Wecker klingelt zum Snooze-Zeitpunkt. |
| TC-42 | **E-Mail-Rate-Limit erste Anfrage** | Passwort-Reset für neue (nie angeforderte) E-Mail → E-Mail wird korrekt versendet, kein interner Fehler. |
| TC-43 | **Familie verlassen – Member gelöscht** | Familie verlassen → eigenes Mitglieds-Profil in Firestore gelöscht. Erneuter Beitritt: kein altes Profil sichtbar, neues Anlegen nötig. |
| TC-44 | **Frühstück-Bad-Konflikt** | Mitglied mit Badzeit ≥ Zeit bis Frühstück → Scheduler meldet Konflikt, kein stiller Fehler. |
| TC-45 | **Offline-Anzeige** | Netz trennen: Offline-Icon erscheint nach >3s. Netz wieder verbinden: Icon verschwindet sofort. Im WLAN wird kein Offline-Icon angezeigt, auch wenn Firestore kurz aus Cache liefert. |
| TC-51 | **Neuer Account – Familie erstellen** | Neuer E-Mail-Account, Familie erstellen → App bleibt im Hauptscreen, kein Redirect zurück. |
| TC-52 | **Alarm-Restore nach Neuinstall** | Alarm AN → App deinstallieren → neu installieren → Login → Alarm ist wieder AN. |
| TC-53 | **Multi-Account auf einem Gerät** | User A ausloggen, User B einloggen → B sieht nur seine Familie, keine Daten von A. |
| TC-54 | **Alarm-Logout-Isolation** | User A hat Alarm AN → Logout → `deviceAlarmEnabled` des Members bleibt in Firestore unverändert (kein false-Write). |
| TC-55 | **App bewerten Button** | Klick auf „⭐ App bewerten“ → In-App-Review-Dialog öffnet sich (oder Fallback auf Play Store, wenn nicht verfügbar). Kein Absturz. |
| TC-56 | **Tageslimit Rate-Limiting** | Nach Erreichen des stündlichen Limits, nach Ablauf der Stunde erneut versuchen: Tageslimit (2× stündliches Limit) greift nach der zweiten Stunde und blockiert weitere Versuche bis 24h abgelaufen. |
| TC-71 | **Onboarding – Erststart** | Nach Erstinstall/Login: Onboarding-Slides erscheinen (5 Screens mit Pager). Slide 0 zeigt animierten Panda. |
| TC-72 | **Onboarding – App-Tour** | Einstellungen → „App-Tour anzeigen" → Onboarding-Slides öffnen sich vollständig. |
| TC-73 | **Paste im Login-Screen** | Long-Press auf E-Mail- oder Passwort-Feld → natives Kontextmenü mit „Einfügen" erscheint (trotz Vereinfachung). |
| TC-74 | **Context Menu im Family Setup** | Long-Press auf Familiennamen- oder Beitrittscode-Feld → natives Kontextmenü erscheint. |
| TC-75 | **Autofill-Support (manuell)** | Fokus auf E-Mail Feld → Tastatur schlägt gespeicherte Adressen vor. Passwort-Manager bietet Autofill via `AutofillNode` an. |
| TC-76 | **Debouncing Toggles** | Mehrfaches schnelles Klicken auf Awake/Pause → Logcat zeigt nur einen Firestore-Write nach 2s. |
| TC-77 | **Master-Switch Debounce** | Globalen Wecker-Switch schnell umschalten → Sync des Status-Icons zu anderen erfolgt nur einmal verzögert. |
| TC-78 | **Batch-Reset Performance** | Manueller Reset-Trigger (via Debug) → Alle Mitglieder werden in einer einzigen Batch-Transaktion aktualisiert. |
| TC-79 | **Lazy-Refresh** | UI muss beim Zurückkehren in den Vordergrund sofort resetten falls Threshold > 2h. |
| TC-80 | **Cloud-Reset** | Status muss auch ohne App-Interaktion nach 2h in Firestore verschwinden. |
| TC-81 | **Neuanlage Mitglieder** | Neues Mitglied anlegen -> Erscheint sofort in der Liste (Prüfung Firestore Timestamp Mapping). |
| TC-82 | **Deep Link Auto-Join** | Klick auf Join-Link ohne Familie -> Tritt sofort bei und landet im MainScreen. |
| TC-83 | **Deep Link Nav-Fix** | Klick auf Join-Link in bestehender Familie -> Bestätigung führt zum Switch ohne Setup-Screen-Schleife. |
| TC-84 | **Familien-Löschung** | Als Creator die Familie löschen -> Alle Mitglieder und Familie werden erfolgreich entfernt. |
| TC-85 | **Settings UI Feedback** | Fehler beim Verlassen/Löschen (z.B. Offline) -> Snackbar mit Fehlermeldung erscheint. |
| TC-86 | Login (Autofill Position) | Email-Feld antippen. | Autofill-Dropdown erscheint direkt unter dem Feld (nicht verschoben). |
| TC-87 | Login (Password Manager) | Login-Screen öffnen. | Passwort-Manager (z.B. Google) bietet gespeicherte Zugangsdaten aktiv an. |
| TC-88 | Security (XSS Feedback) | Feedback mit `<script>alert(1)</script>` senden. | E-Mail zeigt den Code als Text an, keine Ausführung oder Layout-Bruch. |
| TC-89 | **Login (Validierung)** | Login mit < 8 Zeichen Passwort oder ungültiger E-Mail versuchen. | Fehlermeldung erscheint sofort; kein App-Absturz. |
| TC-90 | **Feld-Validierung (Family)** | Neue Familie mit leerem Namen erstellen. | „Erstellen"-Button bleibt deaktiviert (Validierung im ViewModel). |
| TC-91 | **Feld-Validierung (Join)** | Join-Code mit < 6 Zeichen eingeben. | „Beitreten"-Button bleibt deaktiviert; Fehlermeldung bei falschem Code. |
| TC-92 | **Validierung (Member-Name)** | Mitglied ohne Namen speichern. | Speichern-Button gesperrt; Fehlermeldung bei leeren Eingaben. |


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
| TC-57 | **Chip-Layout (7 Tage sichtbar)** | Profil-Editor öffnen → alle 7 Wochentag-Chips (Mo–So) auf schmalem Screen vollständig sichtbar und gleich breit. |
| TC-58 | **Chip-Fehlermarkierung** | Ungültige Zeitkombination setzen → betroffener Chip wird rot markiert (Rahmen + Text). |
| TC-59 | **Validierung – Späteste Weckzeit** | `latestWakeUp` ≤ `earliestWakeUp` setzen → roter Fehlertext erscheint; Speichern-Button gesperrt. |
| TC-60 | **Validierung – Abfahrtszeit (explizit)** | Abfahrtszeit ≤ `latestWakeUp + Baddauer` setzen → roter Fehlertext erscheint; Speichern-Button gesperrt. |
| TC-61 | **Validierung – Abfahrtszeit (Default)** | `latestWakeUp` auf 21:00 setzen, Abfahrtszeit nicht anfassen (Default 08:00) → Fehlertext erscheint sofort. |
| TC-62 | **Next-Alarm – heutiger Tag deaktiviert** | Heutigen Wochentag deaktivieren, morgigen aktivieren → Hauptscreen zeigt den Wecker für morgen (nicht „kein aktiver Wecker"). |
| TC-63 | **Next-Alarm – kein aktiver Tag heute/morgen** | Heutigen und morgigen Wochentag deaktivieren → Hauptscreen zeigt „kein aktiver Wecker". |
| TC-64 | **Alarm klingelt zuverlässig (Hintergrund)** | App in den Hintergrund → Alarm-Zeit abwarten → Wecker klingelt und `RingingActivity` öffnet sich. |
| TC-65 | **Alarm nach Firebase-Sync** | Alarm setzen → kurz nach der Weckzeit Mitglied-Daten ändern → Alarm wurde dennoch korrekt ausgelöst (Grace-Period schützt). |
| TC-66 | **Alle Wochentage inaktiv** | Alle Tage deaktivieren → Member-Kachel zeigt „no alarm", keine Weckzeit-Details; Zeitplan zeigt NoActiveSchedule. |
| TC-67 | **Nächster aktiver Tag (übermorgen)** | Nur Freitag aktiv, heute ist Mittwoch → Member-Kachel zeigt „Freitag" + Freitag-Zeiten; Zeitplan-Karte zeigt Datum „Freitag, XX. März". |
| TC-68 | **Snooze + regulärer Alarm kein Konflikt** | Snooze drücken → Snooze klingelt → Wecker beenden → kein regulärer Alarm überschreibt den laufenden Snooze. |
| TC-69 | **Periodischer Refresh** | App offen, keine Änderungen → nach max. 5 Minuten wird der Zeitplan automatisch neu berechnet (Logcat: `applyAlarms: alarm SET` oder `day X is inactive`). |
| TC-70 | **Veralteter Zeitplan verschwindet** | Alarm klingelt und wird gestoppt → nach max. 5 Minuten zeigt der Hauptscreen keinen alten Zeitplan mehr, sondern den korrekten Stand (nächster Tag / NoActiveSchedule). |

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
| EC-35 | **Member-Löschung beim Verlassen** | Nach dem Verlassen ist das eigene Mitglieds-Profil vollständig aus Firestore gelöscht. Nach erneutem Beitreten ist kein altes Profil mehr vorhanden. |


### 3. Benutzer-Verhalten
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| EC-08 | **Pausieren für heute** | ⏸️ Icon entfernt Mitglied komplett aus dem Plan; andere schlafen ggf. länger. |
| EC-13 | **Bin schon wach (☀️)** | Sonnen-Icon unterdrückt nur den Alarm; Badezimmer-Slot bleibt für andere reserviert. |
| EC-36 | **Reboot-Alarm-Wiederherstellung** | Alarme werden nach Reboot (Locked Boot) korrekt wiederhergestellt (Direct Boot). |
| EC-37 | **Nutzungsbedingungen Link** | Klick auf "Nutzungsbedingungen" in den Einstellungen öffnet den korrekten Link. |
| EC-38 | **Registrierungs-Disclaimer Link** | Klick auf Disclaimer-Links bei der Registrierung öffnet die korrekten Seiten. |
| TC-46 | **Feedback-Screen öffnen** | Klick auf "Feedback geben / Fehler melden" öffnet den FeedbackScreen. |
| TC-47 | **Feedback senden (Firebase)** | Nachricht eingeben → Absenden → Erfolgsmeldung erscheint, Formular wird geleert, Screen schließt nach 2,5s. |
| TC-48 | **Feedback ohne Nachricht** | "Senden"-Button ist deaktiviert wenn Nachricht leer ist. |
| TC-49 | **Footer-Links öffnen** | Klick auf Nutzungsbedingungen, Datenschutz oder Impressum im Settings-Footer öffnet jeweils die externe Seite im Browser. |
| TC-50 | **Account löschen Link** | Klick auf "Account löschen (Info)" öffnet die externe Seite (de/en sprachkorrekt) im Browser. |
| TC-51 | **„Schon wach" – Stopp** | Klick stoppt geplanten System-Wecker sofort | Wecker im Android System gecancelt. |
| TC-52 | **„Schon wach" – Optik** | Klick schaltet Farbe auf Grün und Text auf „Du bist wach ✅“ | Dashboard zeigt aktiven Status. |
| TC-53 | **„Schon wach" – Reset** | Globalen Wecker-Switch ausschalten | „Schon wach"-Status wird auf false zurückgesetzt. |
| TC-54 | **„Schon wach" – Sichtbar** | Globalen Wecker-Switch ein/aus | Button erscheint/verschwindet mit Animation. |
| TC-55 | **RingingScreen – Panda** | Wecker klingelt | Lottie-Panda, Gradient und Zufallstext sichtbar. |
| TC-56 | **Admin: Statistik-Report (E-Mail)** | Klick in Settings -> Toast erscheint -> E-Mail mit Nutzer/Familien-Daten kommt an. |
| TC-57 | **Admin: Sicherheit (Sichtbarkeit)** | Login mit Nicht-Admin-User -> Admin-Buttons in Settings sind ausgeblendet. |
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
