# 🧪 Testplan: FamWake
**Version:** 1.9.2
**Datum:** 2026-05-13

---

## 📋 Strategie
Tests validieren die Korrektheit des Planungsalgorithmus, die Wecker-Zuverlässigkeit und die Datensicherheit.

---

## 🛠 Normalbetrieb

### 1. Account, Onboarding & Familien
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-01 | Registrierung & Login | E-Mail & Google Login klappt. Passwort-Reset verschickt Mail (Enumeration-Schutz aktiv). |
| TC-02 | Lazy Registration & Onboarding | Tour mit 4 Slides läuft fehlerfrei. "Los geht's" erstellt anonymen Nutzer (Double-Click geschützt). Tooltip-Toggle am Ende speichert Präferenz korrekt. |
| TC-03 | Familien-Lifecycle | Gründen + Beitreten per Code/Link. Nur Creator darf löschen. Verlassen/Löschen der Familie wirft Mitglieder sofort auf den Startbildschirm zurück und deaktiviert lokale Wecker. Testdaten werden bei Registrierung übernommen. |
| TC-04 | Tour-Replay (Settings) | Eingeloggt: Letzter Slide (Login/Anonym) wird übersprungen. Button zeigt "Schließen". Tooltip-Checkbox übernimmt gespeicherte Einstellung. Nicht eingeloggt: Alle 4 Slides, originale Buttons. |

### 2. Mitglieder & Konfiguration
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-20 | Profil-Verwaltung | Erstes Mitglied auto-geclaimt (Wecker-Schalter sofort an). Ein fremdes Profil kann übernommen werden (Claim Stealing). Beim Claim wird `deviceAlarmEnabled` in Room + Firestore sofort auf `true` gesetzt, damit der Zeitplan direkt berechnet wird. Bearbeiten und Pausieren funktioniert fehlerfrei. |
| TC-20b | Profil nach Neuinstallation | App deinstallieren → neu installieren → einloggen. Profil wird NICHT automatisch übernommen (neue Device-ID). Nach manuellem Claim: Weckplan erscheint sofort, kein manuelles Toggle nötig. |
| TC-21 | Wochentag-Validierung | latestWakeUp ≤ earliestWakeUp blockiert Speichern. Zügiges Sliden erzeugt keine Sync-Fehler. |
| TC-22 | Listen-Organisation | Drag & Drop speichert Reihenfolge. Warnbanner erscheint, wenn Position 1 ungeclaimt ist. |

### 3. Wecker, Alarm & Berechtigungen
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-30 | Alarm-Zyklus | Wecker klingelt zuverlässig (auch Background/Lockscreen). Snooze (5 Min) plant neuen Alarm korrekt. |
| TC-31 | Statuswechsel ("Ich bin wach") | Stoppt System-Wecker, aktualisiert UI (Two-Pass-Logik für "Heute/Morgen"). Setzt sich am nächsten Tag automatisch zurück. |
| TC-32 | Android 14+ Warnungen | Fehlen `SCHEDULE_EXACT_ALARM` oder `USE_FULL_SCREEN_INTENT`, erscheinen entsprechende Banner/Warnungen. |

### 4. Benachrichtigungen & Sync (Multi-Device)
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-40 | Push-Events | Status-Änderungen (An/Aus, Reorder) senden Push an geclaimte Mitbewohner (kein Self-Push, kein Doppel-Piepsen). Toggle deaktiviert Push. |
| TC-41 | Multi-Device Sync | Claiming, Alarm-Status und Listen-Reihenfolge synchronisieren sich sofort auf Zweitgeräten. |

---

## ⚠️ Edge Cases & Sicherheit

### Konflikte & Systemgrenzen
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| EC-01 | Zeit-Konflikte | Unmögliche Pläne oder zu knappe Fenster zeigen Kompromissvorschläge. Zeitumstellung und Mitternachts-Alarme korrekt berechnet. AutoFix dehnt die Zeiten intelligent aus und berechnet sofort neu (Optimistic UI). |
| EC-02 | Offline-Betrieb | CloudOff-Icon bei Disconnect. Kein Absturz bei SSL/Netzwerk-Fehlern (korrekte Fehlermeldung). Re-Sync nach Reconnect. |
| EC-03 | Backend-Schutz | Zugriff auf fremde Profile oder Feedback ohne Auth liefert `PERMISSION_DENIED`. |

### UI & Lokalisation
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| UI-01 | Layout-Responsivität | Tastatur verdeckt keine Buttons. App ist auf Tablets strikt im Hochformat, Animationen verdecken keinen Text. |
| UI-02 | Sprachen & Themes | 25 Sprachen funktionieren absturzfrei. Dark/Light-Mode wechselt sofort, AMOLED Black greift. |

---

## 📈 Validierung
- **Automatisiert:** 9 Unit-Tests für `Scheduler` laufen in GitHub Actions.
- **Manuell:** Vor jedem Release ein Live-Test über eine Nacht.
