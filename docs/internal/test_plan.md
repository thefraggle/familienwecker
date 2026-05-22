# 🧪 Testplan: FamWake
**Version:** 1.9.8
**Datum:** 2026-05-22

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
| TC-20 | Profil-Verwaltung & Neuinstallation | Erstes Mitglied auto-geclaimt (Wecker-Schalter sofort an). Ein fremdes Profil kann übernommen werden (Claim Stealing). Beim Claim wird `deviceAlarmEnabled` in Room + Firestore sofort auf `true` gesetzt. Bei Neuinstallation wird das Profil wegen neuer Device-ID nicht auto-geclaimt; nach manuellem Claim erscheint der Weckplan sofort ohne manuelles Wecker-Togglen. Bearbeiten und Pausieren funktioniert fehlerfrei. |
| TC-21 | Wochentag-Validierung | latestWakeUp ≤ earliestWakeUp blockiert Speichern. Zügiges Sliden erzeugt keine Sync-Fehler. |
| TC-22 | Listen-Organisation | Drag & Drop speichert Reihenfolge. Warnbanner erscheint, wenn Position 1 ungeclaimt ist. |
| TC-23 | Puffer nach Bad (global & individuell) | Stepper unter "Familienmitglieder" ändert globalen Puffer (0–15 Min) in 5er-Schritten. Im Mitglieder-Editor wird der globale Wert kursiv angezeigt. Ein persönlicher Override setzt den eigenen Puffer (fett), das Zurücksetzen stellt die Vererbung wieder her (kursiv). Werte synchronisieren und persistieren nach App-Neustart. |
| TC-25 | Familie einladen | Share-Button über dem Hinzufügen-Button öffnet System-Share-Dialog mit Familien-Link. Nur für eingeloggte Nutzer sichtbar. |
| TC-26 | Zeitformat 12h/24h | Uhrzeiten folgen der Geräteeinstellung. Wechsel in den Systemeinstellungen wirkt sofort nach Rückkehr zur App. |
| TC-27 | Einfacher Modus | Im Mitglieder-Editor: Aktivieren blendet alle erweiterten Optionen (Baddauer, Puffer, Frühstück etc.) aus und zeigt nur noch die Aufstehzeit. Scheduler weist feste Weckzeit zu, ohne das Zeitfenster zu verschieben oder zu jonglieren. Speichern funktioniert und Zeitplan aktualisiert sich. |
| TC-28 | Profil-Löschung auf Fremdgerät | Ein geclaimtes Profil wird von einem anderen Gerät aus gelöscht. Das betroffene Gerät entkoppelt sich sofort (Wecker aus, myMemberId = nil) und kehrt in den ungeclaimten Zustand zurück. |
| TC-29 | In-App Review Workflow | Erstmalige Aufforderung nach 3 Tagen und nur nach Verlassen der Einstellungen oder Speichern eines Mitglieds, nicht zur Weckzeit (6-9 Uhr). Zweite Aufforderung nach 9 Tagen (mit mind. 5 Tagen Abstand zur ersten), falls noch keine Bewertung abgegeben wurde. |

### 3. Wecker, Alarm & Berechtigungen
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-30 | Alarm-Zyklus | Wecker klingelt zuverlässig (auch Background/Lockscreen). Snooze (5 Min) plant neuen Alarm korrekt. |
| TC-31 | Statuswechsel ("Ich bin wach") | Stoppt System-Wecker, aktualisiert UI (Two-Pass-Logik für "Heute/Morgen"). Setzt sich am nächsten Tag automatisch zurück. |
| TC-32 | Android 14+ Warnungen | Fehlen `SCHEDULE_EXACT_ALARM` oder `USE_FULL_SCREEN_INTENT`, erscheinen entsprechende Banner/Warnungen. |
| TC-33 | Kaltstart iOS | App-Start per Alarm-Klick (Kaltstart) löst keinen Boot-Loop/Absturz aus. |
| TC-34 | DataStore Migration Android | Update von v1.9.5: Alte EncryptedSharedPreferences-Werte werden atomar in Jetpack DataStore migriert, Altdatei wird gelöscht, Einstellungen bleiben erhalten. |

### 4. Benachrichtigungen & Sync (Multi-Device)
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-40 | Push-Events | Status-Änderungen (An/Aus, Reorder) senden Push an geclaimte Mitbewohner (kein Self-Push, kein Doppel-Piepsen). Toggle deaktiviert Push. |
| TC-41 | Multi-Device Sync | Claiming, Alarm-Status und Listen-Reihenfolge synchronisieren sich sofort auf Zweitgeräten. |

---

## ⚠️ Edge Cases & Sicherheit

### Konflikte & Systemgrenzen
| ID | Testfall | Erwartetes Ergebnis |
| EC-01 | Zeit- & Puffer-Konflikte | Unmögliche Pläne oder zu knappe Fenster zeigen Kompromissvorschläge. AutoFix dehnt die Zeiten intelligent aus und berechnet sofort neu (Optimistic UI). Bei knappen Zeitfenstern reduziert der Scheduler den Puffer automatisch (BufferReduced-Meldung), bevor Zeitverschiebung/Frühstücksreduktion greifen. Zeitumstellung und Mitternachts-Alarme korrekt berechnet. |
| EC-02 | Offline-Betrieb | CloudOff-Icon bei Disconnect. Kein Absturz bei SSL/Netzwerk-Fehlern (korrekte Fehlermeldung). Re-Sync nach Reconnect. |
| EC-03 | Backend-Schutz | Zugriff auf fremde Profile oder Feedback ohne Auth liefert `PERMISSION_DENIED`. |

### UI & Lokalisation
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| UI-01 | Layout-Responsivität | Tastatur verdeckt keine Buttons. App ist auf Tablets strikt im Hochformat, Animationen verdecken keinen Text. |
| UI-02 | Sprachen & Themes | 25 Sprachen funktionieren absturzfrei. Dark/Light-Mode wechselt sofort, AMOLED Black greift. |
| UI-03 | Icon-Konsistenz | Material Icons (Wecker, Badewanne, Frühstück, Laufen) erscheinen korrekt auf MemberCard, MainScreen-Zeitplan, AddMember-Formular und Settings-Buttons. Keine Emoji-Reste. Dark/Light einheitlich. |

---

## 📈 Validierung
- **Automatisiert:** 12 Unit-Tests für `Scheduler` (inkl. 4 Buffer-Tests) laufen in GitHub Actions.
- **Manuell:** Vor jedem Release ein Live-Test über eine Nacht.
