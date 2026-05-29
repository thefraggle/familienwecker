# 🧪 Testplan: FamWake
**Version:** 1.9.14
**Datum:** 2026-05-29

---

## 📋 Strategie
Tests validieren die Korrektheit des Planungsalgorithmus, die Wecker-Zuverlässigkeit und die Datensicherheit.

---

## 🛠 Normalbetrieb

### 1. Account, Onboarding & Familien
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-01 | Registrierung & Login | E-Mail & Google Login klappt. Passwort-Reset verschickt Mail. Premium-Layout für Buttons und Eingabefelder mit einheitlichen Eckradien (12.dp) und optimalen Kontrasten (inaktive Buttons sind klar lesbar und erst nach vollständiger Eingabe klickbar) verifiziert. Markenkonformer Google-Button in allen Modi. |
| TC-02 | Lazy Registration & Onboarding | Tour mit 4 Slides läuft fehlerfrei. "Los geht's" erstellt anonymen Nutzer (Double-Click geschützt). Tooltip-Toggle am Ende speichert Präferenz korrekt. |
| TC-03 | Familien-Lifecycle | Gründen + Beitreten per Code/Link. Nur Creator darf löschen. Verlassen/Löschen der Familie wirft Mitglieder sofort auf den Startbildschirm zurück und deaktiviert lokale Wecker. Testdaten werden bei Registrierung übernommen. |
| TC-04 | Tour-Replay (Settings) | Eingeloggt: Letzter Slide (Login/Anonym) wird übersprungen. Button zeigt "Schließen". Tooltip-Checkbox übernimmt gespeicherte Einstellung. Nicht eingeloggt: Alle 4 Slides, originale Buttons. |
| TC-05 | DSGVO-Datenlöschung | Löschen eines Kontos löscht `users/{uid}` inkl. `fcmTokens`/`pushMeta` und entclaimt alle zugeordneten Profile in der Familie. |
| TC-06 | Anonyme User-Bereinigung | Wöchentliche Löschung von unlinked anonymen Accounts (>30 Tage) über Cloud Functions. |

### 2. Mitglieder & Konfiguration
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-20 | Profil-Verwaltung & Neuinstallation | Erstes Mitglied auto-geclaimt (Wecker-Schalter sofort an). Ein fremdes Profil kann übernommen werden (Claim Stealing). Beim Claim wird `deviceAlarmEnabled` in Room + Firestore sofort auf `true` gesetzt. Bei Neuinstallation wird das Profil wegen neuer Device-ID nicht auto-geclaimt; nach manuellem Claim erscheint der Weckplan sofort ohne manuelles Wecker-Togglen. Bearbeiten und Pausieren funktioniert fehlerfrei. |
| TC-21 | Wochentag-Validierung | latestWakeUp ≤ earliestWakeUp blockiert Speichern. Zügiges Sliden erzeugt keine Sync-Fehler. |
| TC-22 | Listen-Organisation & Reorder-Bestätigung | Drag & Drop öffnet Bestätigungsdialog. „Nur heute“ ändert die Reihenfolge nur für den ausgewählten Wochentag. „Ganze Woche“ setzt sie global und löscht tagesabhängige Overrides. Abbrechen (oder Tippen außerhalb) setzt die Liste visuell zurück. Warnbanner erscheint, wenn Position 1 ungeclaimt ist. |
| TC-23 | Puffer nach Bad (global & individuell) | Stepper unter "Familienmitglieder" ändert globalen Puffer (0–15 Min) in 5er-Schritten. Im Mitglieder-Editor wird der globale Wert kursiv angezeigt. Ein persönlicher Override setzt den eigenen Puffer (fett), das Zurücksetzen stellt die Vererbung wieder her (kursiv). Werte synchronisieren und persistieren. Individueller Puffer an 1. Stelle wird auch bei globalem Puffer = 0m im Weckplan visualisiert und schränkt den Nachfolger ein. Zudem überschreiben manuelle 0m-Overrides größere globale Puffer und werden bei Übereinstimmung mit dem globalen Wert kursiv dargestellt. |
| TC-25 | Familie einladen | Share-Button über dem Hinzufügen-Button öffnet System-Share-Dialog mit Familien-Link. Nur für eingeloggte Nutzer sichtbar. |
| TC-26 | Zeitformat 12h/24h | Uhrzeiten folgen der Geräteeinstellung. Wechsel in den Systemeinstellungen wirkt sofort nach Rückkehr zur App. |
| TC-27 | Einfacher Modus | Im Mitglieder-Editor: Aktivieren blendet alle erweiterten Optionen (Baddauer, Puffer, Frühstück etc.) aus und zeigt nur noch die Aufstehzeit. Scheduler weist feste Weckzeit zu, ohne das Zeitfenster zu verschieben oder zu jonglieren. Speichern funktioniert und Zeitplan aktualisiert sich. |
| TC-28 | Profil-Löschung auf Fremdgerät | Ein geclaimtes Profil wird von einem anderen Gerät aus gelöscht. Das betroffene Gerät entkoppelt sich sofort (Wecker aus, myMemberId = nil) und kehrt in den ungeclaimten Zustand zurück. |
| TC-29 | In-App Review Workflow | Erstmalige Aufforderung nach 3 Tagen und nur nach Verlassen der Einstellungen oder Speichern eines Mitglieds, nicht zur Weckzeit (6-9 Uhr). Zweite Aufforderung nach 9 Tagen (mit mind. 5 Tagen Abstand zur ersten), falls noch keine Bewertung abgegeben wurde. |

### 3. Wecker, Alarm & Berechtigungen
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-30 | Alarm-Zyklus | Wecker klingelt zuverlässig (auch Background/Lockscreen). Snooze (5 Min) plant neuen Alarm korrekt. |
| TC-31 | Statuswechsel („Ich bin wach“) | Stoppt System-Wecker, aktualisiert UI (Two-Pass-Logik). Button bleibt unabhängig vom ausgewählten Vorschau-Wochentag auf dem Screen sichtbar (an Weckplan gekoppelt). Zeigt sich dauerhaft am Tag des Weckers oder ab 4h vorher. Reset erfolgt am Folgetag automatisch. |
| TC-32 | Android 14+ Warnungen | Fehlen `SCHEDULE_EXACT_ALARM` oder `USE_FULL_SCREEN_INTENT`, erscheinen entsprechende Banner/Warnungen. |
| TC-33 | Kaltstart iOS | App-Start per Alarm-Klick (Kaltstart) löst keinen Boot-Loop/Absturz aus. |
| TC-34 | DataStore Migration Android | Update von v1.9.5: Alte EncryptedSharedPreferences-Werte werden atomar in Jetpack DataStore migriert, Altdatei wird gelöscht, Einstellungen bleiben erhalten. |

### 4. Benachrichtigungen & Sync (Multi-Device)
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| TC-40 | Push-Events & Filterung | Manuelle Änderungen (Pause, Alarm-Schalter, Reihenfolge, Profile) senden stumme Push-Benachrichtigungen an geclaimte Mitbewohner (kein Self-Push). Automatische Status-Resets (Daily Reset) werden gefiltert und senden KEINEN Push. App-Toggle deaktiviert den Empfang. |
| TC-41 | Multi-Device Sync | Claiming, Alarm-Status und Listen-Reihenfolge synchronisieren sich sofort auf Zweitgeräten. |

---

## ⚠️ Edge Cases & Sicherheit

### Konflikte & Systemgrenzen
| ID | Testfall | Erwartetes Ergebnis |
| EC-01 | Zeit- & Puffer-Konflikte | Unmögliche Pläne oder zu knappe Fenster zeigen Kompromissvorschläge. AutoFix dehnt die Zeiten intelligent aus und berechnet sofort neu (Optimistic UI). Der Auto-Fix passt nur den Ziel-Wochentag an und lässt andere Tage unberührt. Bei knappen Zeitfenstern reduziert der Scheduler den Puffer automatisch (BufferReduced-Meldung), bevor Zeitverschiebung/Frühstücksreduktion greifen. Zeitumstellung und Mitternachts-Alarme korrekt berechnet. |
| EC-02 | Offline-Betrieb | CloudOff-Icon bei Disconnect. Kein Absturz bei SSL/Netzwerk-Fehlern (korrekte Fehlermeldung). Re-Sync nach Reconnect. |
| EC-03 | Backend-Schutz | Zugriff auf fremde Profile oder Feedback ohne Auth liefert `PERMISSION_DENIED`. |

### UI & Lokalisation
| ID | Testfall | Erwartetes Ergebnis |
|:---|:---|:---|
| UI-01 | Layout-Responsivität | Tastatur verdeckt keine Buttons. App ist auf Tablets strikt im Hochformat, Animationen verdecken keinen Text. |
| UI-02 | Sprachen & Themes | 25 Sprachen funktionieren absturzfrei. Dark/Light-Mode wechselt sofort, AMOLED Black greift. |
| UI-03 | Icon-Konsistenz | Material Icons (Wecker, Badewanne, Frühstück, Laufen) erscheinen korrekt auf MemberCard, MainScreen-Zeitplan, AddMember-Formular und Settings-Buttons. Keine Emoji-Reste. Dark/Light einheitlich. |
| UI-04 | Hilfetexte & Tooltips | Alle 9 Tooltips (Wach-Button, Drag-Handle, Puffer, Wochentage, Weckzeitfenster, Baddauer, Wecker-Schalter, Einladungscode, Weckton) erscheinen korrekt an ihren Stellen und lassen sich dismissen. |

---

## 📈 Validierung
- **Automatisiert:** 13 Unit-Tests für `Scheduler` (inkl. 5 Buffer-Tests) laufen in GitHub Actions.
- **Manuell:** Vor jedem Release ein Live-Test über eine Nacht.
