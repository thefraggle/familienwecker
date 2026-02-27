# 🧪 Testplan - FamWake (Familienwecker)

Diese Dokumentation beschreibt die Teststrategie und die Testfälle für die FamWake-App, um eine hohe Zuverlässigkeit der Weck-Logik und eine reibungslose Benutzererfahrung sicherzustellen.

---

## 📋 Übersicht & Strategie

Die FamWake-App basiert auf einem dynamischen Planungsalgorithmus. Tests müssen daher nicht nur die UI validieren, sondern insbesondere die mathematische Korrektheit und Stabilität der Zeitplanberechnung unter verschiedenen Randbedingungen.

### Testbereiche
1.  **Onboarding & Account:** Registrierung, Login, Familienbeitritt.
2.  **Familienmanagement:** Mitglieder hinzufügen/entfernen, Rollen.
3.  **Planungs-Logik (Kern):** Bad-Taktung, Frühstücksplanung, Pufferzeiten.
4.  **Wecker-Funktion:** Sound, Fullscreen-Notification, Snooze.
5.  **Edge Cases & Robustheit:** Offline-Status, Zeitzonen, Konfliktsituationen.

---

## 🛠 Testfälle (Normalbetrieb)

### 1. Account & Onboarding
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-01 | Erst-Registrierung mit E-Mail | Account wird erstellt, Bestätigungs-E-Mail versendet. |
| TC-02 | Familie gründen | Benutzer wird "Admin" einer neuen Familie, Einladungscode wird generiert. |
| TC-03 | Familie beitreten | Benutzer tritt per Code einer bestehenden Familie bei. |

### 2. Familien-Konfiguration
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-04 | Weck-Präferenzen ändern | Späteste Weckzeit und Bad-Dauer werden gespeichert. |
| TC-05 | Frühstücks-Wunsch umschalten | Algorithmus berücksichtigt das Mitglied bei der Frühstückszeit-Berechnung. |
| TC-06 | Haus-Verlassen Zeit setzen | Plan wird angepasst, damit die Person rechtzeitig fertig ist. |
| TC-10 | **Mitglieder-Limit** | Bei 6 Mitgliedern wird der „Hinzufügen"-Button gesperrt. |

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
| EC-01 | **Unmöglicher Plan** (Alle wollen zur selben Zeit ins Bad) | App zeigt Warnung "Konflikt gefunden" und bietet Kompromissvorschläge (z.B. Frühstück verkürzen). |
| EC-02 | **Extreme Bad-Dauer** (Mitglied mit 120 Min. Badzeit) | Der Plan verschiebt andere Mitglieder massiv; ggf. Warnung bei unrealistischen Eingaben. |
| EC-03 | **Kurze Zeitfenster** (Wecken 7:00, Haus verlassen 7:05) | App warnt vor zu knappem Zeitmanagement. |

### 2. Technische Grenzfälle
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| EC-04 | **Offline-Berechnung** | Der letzte gültige Plan bleibt lokal gespeichert. Wecker klingelt auch ohne Internet. |
| EC-05 | **Zeitumstellung** (Sommer/Winter) | Weckzeiten werden korrekt an die neue Zeit angepasst, keine doppelten Alarme. |
| EC-06 | **App-Absturz während Alarm** | Alarm-Dienst startet automatisch neu und setzt den Weckvorgang fort. |
| EC-07 | **Akku-Optimierung (Android)** | App ist als "Nicht optimiert" markiert, damit der Background-Service zuverlässig weckt. |
| EC-11 | **Snooze-Funktion** | Klick auf Snooze (5 Min) im Weckscreen plant einen neuen Alarm exakt 5 Min später. |
| EC-12 | **Mitternachts-Reset** | Status „Heute pausieren" und „Bin schon wach" werden automatisch am nächsten Tag zurückgesetzt. |
| EC-14 | **Persistenz & Logout** | Nach Logout oder Neu-Installation sind keine alten Login-Daten oder Familien-IDs mehr vorhanden (Auto-Backup deaktiviert). |

### 3. Benutzer-Verhalten
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| EC-08 | **Pausieren für heute** | ⏸️ Icon entfernt Mitglied komplett aus dem Plan; andere schlafen ggf. länger. |
| EC-13 | **Bin schon wach (☀️)** | Sonnen-Icon unterdrückt nur den Alarm; Badezimmer-Slot bleibt für andere reserviert. |
| EC-09 | Nachträgliche Änderung (2 Uhr nachts) | Plan wird neu berechnet und asynchron an alle synchronisiert. |
| EC-10 | Mehrere Admins | Beide ändern gleichzeitig die Badzeit eines Kindes -> Last-Write-Wins oder Konfliktmeldung. |

---

## 📱 UI/UX & Barrierefreiheit

- **Dark Mode:** Alle Kontraste müssen auch im dunklen Thema (für nachts/morgens) augenfreundlich sein.
- **Haptik:** Vibrationsmuster unterscheiden sich zwischen "Voralarm" und "Hauptalarm".
- **Echtzeit-Feedback:** Wenn der Plan neu berechnet wird, sieht der User eine kurze Animation/Bestätigung.

---

## 📈 Validierung & Reporting

- **Automatisierung:** Die Kern-Logik (`Scheduler`) wird über JUnit-Tests mit den Szenarien aus TC-07 bis TC-09 und EC-01 abgedeckt.
- **Manuelle Abnahme:** Vor jedem Release erfolgt ein "Live-Test" über eine Nacht in einer Test-Familie.
