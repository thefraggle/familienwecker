# 🧪 Testplan: FamWake
**Version:** 2.0.0
**Datum:** 2026-06-17

---

## 📋 Strategie
Tests validieren die Korrektheit des Planungsalgorithmus, die Wecker-Zuverlässigkeit, die Datensicherheit und den geräteübergreifenden Sync.

---

## 🛠 Normalbetrieb

### 1. Account, Onboarding & Familien
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-01 | Registrierung & Login | E-Mail & Google Login klappt. Passwort-Reset verschickt Mail. Premium-Layout für Buttons und Eingabefelder mit einheitlichen Eckradien und optimalen Kontrasten. Inaktive Buttons sind klar lesbar und erst nach vollständiger Eingabe klickbar. Markenkonformer Google-Button in allen Modi. |
| TC-02 | Lazy Registration & Onboarding | Tour mit 4 Slides läuft fehlerfrei. "Los geht's" erstellt anonymen Nutzer (Double-Click geschützt). Tooltip-Toggle am Ende speichert Präferenz korrekt. |
| TC-03 | Familien-Lifecycle | Gründen + Beitreten per Code/Link. Nur Creator darf löschen. Verlassen/Löschen der Familie wirft Mitglieder sofort auf den Startbildschirm zurück und deaktiviert lokale Wecker. Testdaten werden bei Registrierung übernommen. |
| TC-04 | Tour-Replay (Settings) | Eingeloggt: Letzter Slide (Login/Anonym) wird übersprungen. Button zeigt "Schließen". Tooltip-Checkbox übernimmt gespeicherte Einstellung. Nicht eingeloggt: Alle 4 Slides, originale Buttons. |
| TC-05 | DSGVO-Datenlöschung | Löschen eines Kontos löscht `users/{uid}` inkl. `fcmTokens`/`pushMeta` und entclaimt alle zugeordneten Profile in der Familie. |
| TC-06 | Anonyme User-Bereinigung | Wöchentliche Löschung von unlinked anonymen Accounts (>30 Tage) über Cloud Functions. |

### 2. Mitglieder & Konfiguration
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-20 | Profil-Verwaltung & Neuinstallation | Erstes Mitglied auto-geclaimt (Wecker-Schalter sofort an). Ein fremdes Profil kann übernommen werden (Claim Stealing). Bei Neuinstallation wird das Profil wegen neuer Device-ID nicht auto-geclaimt; nach manuellem Claim erscheint der Weckplan sofort. |
| TC-21 | Wochentag-Validierung | latestWakeUp ≤ earliestWakeUp blockiert Speichern. Zügiges Sliden erzeugt keine Sync-Fehler. |
| TC-22 | Listen-Organisation & Reorder | Drag & Drop öffnet Bestätigungsdialog. „Nur heute" ändert die Reihenfolge nur für den ausgewählten Wochentag. „Ganze Woche" setzt sie global und löscht tagesabhängige Overrides. Abbrechen setzt die Liste visuell zurück. Warnbanner erscheint, wenn Position 1 ungeclaimt ist. |
| TC-23 | Puffer nach Bad (global & individuell) | Stepper unter "Familienmitglieder" ändert globalen Puffer (0–15 Min) in 5er-Schritten. Im Mitglieder-Editor wird der globale Wert kursiv angezeigt. Ein persönlicher Override setzt den eigenen Puffer (fett), das Zurücksetzen stellt die Vererbung wieder her (kursiv). Individueller Puffer an 1. Stelle wird auch bei globalem Puffer = 0m visualisiert. Manuelle 0m-Overrides überschreiben größere globale Puffer. |
| TC-24 | Opt-In Wecker für neue Profile | Neues Profil hat initial alle Tage inaktiv (Schalter aus). Heutiger Tag ist vorausgewählt. Beim Aktivieren eines Tages werden sofort Standardwerte geladen. |
| TC-25 | Familie einladen | Share-Button öffnet System-Share-Dialog mit Familien-Link. Nur für eingeloggte Nutzer sichtbar. |
| TC-26 | Zeitformat 12h/24h | Uhrzeiten folgen der Geräteeinstellung. Wechsel in den Systemeinstellungen wirkt sofort nach Rückkehr zur App. |
| TC-27 | Einfacher Modus | Aktivieren blendet erweiterte Optionen aus (Baddauer, Puffer, Frühstück). Scheduler weist feste Weckzeit zu, ohne das Zeitfenster zu verschieben. Speichern funktioniert und Zeitplan aktualisiert sich. |
| TC-28 | Profil-Löschung auf Fremdgerät | Ein geclaimtes Profil wird von einem anderen Gerät aus gelöscht. Das betroffene Gerät entkoppelt sich sofort (Wecker aus, kein geclaimtes Profil) und kehrt in den ungeclaimten Zustand zurück. |
| TC-29 | In-App Review Workflow | Erstmalige Aufforderung nach 3 Tagen und nur nach Verlassen der Einstellungen oder Speichern eines Mitglieds, nicht zur Weckzeit (6-9 Uhr). Zweite Aufforderung nach 9 Tagen (mit mind. 5 Tagen Abstand). |

### 3. Wecker, Alarm & Snooze
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-30 | Alarm-Zyklus (Klingeln → Stop) | Wecker klingelt zuverlässig (auch im Hintergrund und auf dem Sperrbildschirm). **Stop:** App öffnet sich direkt (Main Screen). Kein doppelter/überlappender Alarm-Ton. |
| TC-31 | Snooze (lokal) | **1. Snooze:** Eigene Weckzeit wird um 5 Min verschoben, Badzeit absorbiert die Verschiebung (wird kürzer). Andere Mitglieder werden NICHT verschoben. Banner zeigt „Schlummern bis HH:MM (1/2)". |
| TC-32 | Snooze (2. Snooze mit Verschiebung) | **2. Snooze:** Eigene Weckzeit +5 Min, volle Badzeit (keine Absorption mehr). Nachfolgende Mitglieder werden entsprechend nach hinten verschoben. |
| TC-33 | Snooze-Sync (Multi-Device) | Snooze-Zeiten werden sofort auf allen Geräten angezeigt. 💤-Symbol erscheint beim snoozenden Mitglied. Verschobene Zeiten nach 2. Snooze sind auf allen Geräten sichtbar. |
| TC-34 | Snooze abbrechen | „Abbrechen" im Banner setzt Snooze-State zurück. Weckplan wird neu berechnet. Änderung wird auf anderen Geräten synchronisiert. |
| TC-35 | Snooze-Maximum | Nach 2 Snoozes ist der Snooze-Button deaktiviert. Eine lokale Benachrichtigung erscheint mit Hinweis „Aufstehen!". |
| TC-36 | Ghost-Alarm-Schutz | Wecker klingelt NICHT, wenn der globale Schalter ausgeschaltet ist – auch nicht nach App-Neustart, nach Bearbeitung eines Mitglieds mit passender Weckzeit, oder aus dem Hintergrund heraus. |
| TC-37 | Statuswechsel („Ich bin wach") | Stoppt System-Wecker, aktualisiert UI. Button bleibt unabhängig vom Vorschau-Wochentag sichtbar (am Weckertag oder ab 4h vorher). Reset erfolgt am Folgetag automatisch. |
| TC-38 | Berechtigungswarnungen | Fehlen Wecker-Berechtigungen, erscheinen Banner/Warnungen auf dem Hauptbildschirm. |

### 4. Benachrichtigungen & Sync (Multi-Device)
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-40 | Push-Events & Filterung | Manuelle Änderungen (Pause, Alarm-Schalter, Reihenfolge, Profile) senden stumme Push-Benachrichtigungen an geclaimte Mitbewohner (kein Self-Push). Automatische Status-Resets werden gefiltert und senden KEINEN Push. App-Toggle deaktiviert den Empfang. |
| TC-41 | Multi-Device Sync | Claiming, Alarm-Status, Listen-Reihenfolge und Snooze-State synchronisieren sich sofort auf Zweitgeräten. Mitglieder mit globalem Schalter OFF erscheinen NICHT in der Liste anderer Geräte. |
| TC-42 | Pause-Toggle ohne Flackern | Pausieren/Entpausieren eines Mitglieds zeigt sofortigen lokalen State. Kein kurzes Zurückspringen durch eingehende Snapshots. |

---

## ⚠️ Edge Cases & Sicherheit

### Konflikte & Systemgrenzen
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| EC-01 | Zeit- & Puffer-Konflikte | Unmögliche Pläne zeigen Kompromissvorschläge. AutoFix dehnt die Zeiten intelligent aus (Optimistic UI), nur für den Ziel-Wochentag. Bei knappen Zeitfenstern reduziert der Scheduler den Puffer automatisch (BufferReduced-Meldung), bevor Zeitverschiebung/Frühstücksreduktion greifen. |
| EC-02 | Offline-Betrieb | CloudOff-Icon bei Disconnect. Kein Absturz bei SSL/Netzwerk-Fehlern. Re-Sync nach Reconnect. Optimistische UI-Updates werden bei Fehlschlag sauber zurückgesetzt. Destruktive Operationen (Löschen, Verlassen) werden bei Offline blockiert. |
| EC-03 | Backend-Schutz | Zugriff auf fremde Profile oder Feedback ohne Auth liefert `PERMISSION_DENIED`. |

### UI & Lokalisation
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| UI-01 | Layout-Responsivität | Tastatur verdeckt keine Buttons. Tablets im Hochformat, Animationen verdecken keinen Text. |
| UI-02 | Sprachen & Themes | 25 Sprachen funktionieren absturzfrei. Dark/Light-Mode wechselt sofort. |
| UI-03 | Icon-Konsistenz | Material Icons erscheinen korrekt auf MemberCard, Zeitplan, Formular und Settings. Keine Emoji-Reste. |
| UI-04 | Hilfetexte & Tooltips | Alle Tooltips erscheinen korrekt und lassen sich dismissen. |
| UI-05 | Fehlerhinweise | Bei fehlgeschlagenen Sync-Writes (Reihenfolge, Pause, Snooze) erscheint eine verständliche Fehlermeldung statt stiller Fehler. |

---

## 📈 Validierung
- **Automatisiert:** 13 Unit-Tests für `Scheduler` (inkl. 5 Buffer-Tests) laufen in GitHub Actions.
- **Manuell:** Vor jedem Release ein Live-Test auf mindestens 2 Geräten (Snooze-Sync, Ghost-Alarm-Guard, Multi-Device).
