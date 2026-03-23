# Changelog

Das Format basiert auf [Keep a Changelog](https://keepachangelog.com/de/1.0.0/).

*[🇺🇸 English Version](CHANGELOG.en.md)*

## 1.6.1 - 2026-03-23

### Neu
- **Support-Bereich für alle:** Die Option, die App zu unterstützen oder zu bewerten, ist nun für alle Nutzer in den Einstellungen sichtbar.

### Optimiert
- **Spenden-Dialog:** Aktualisierte Tier-Preise (1,79€, 4,79€, 9,49€) und verbesserte Lade-Anzeigen während der Kaufabwicklung.
- **Vollständige Lokalisation:** Alle Spenden-Texte und Status-Meldungen sind nun in Deutsch, Englisch, Französisch, Italienisch und Spanisch verfügbar.
- **Play Store Release-Prozess:** Optimierte Bereitstellung der Versionshinweise für eine schnellere Veröffentlichung.

## 1.6.0 - 2026-03-23

### Neu
- **Play Store Optimierung:** Optimierungen für den offiziellen Play Store Release und verbesserte Metadaten.
- **Vollständige Lokalisation:** Unterstützung für Französisch, Spanisch und Italienisch inkl. automatischer Erkennung hinzugefügt.
- **E-Mail-System:** Passwort-Reset, Bestätigung und Verifizierung werden nun vollständig in der gewählten App-Sprache versendet.
- **Deep-Link UX:** Visuelles Feedback beim Beitritt zu einer Familie via Link verbessert.

### Optimiert
- **Zuverlässigkeit:** Regelmäßige Updates zur Verbesserung der Stabilität und Performance sowie optimierte Synchronisation.
- **Sicherheits-Check:** Zugriffsberechtigungen für Familiendaten und Eigentumsrechte verschärft; Schutz privater Daten verbessert.
- **Onboarding:** Modernisierte Einführungstour mit Panda-Animationen und Dark Mode als Standard für Erstnutzer.

### Behoben
- **Einladungscodes:** Codes bleiben nun auch dann gültig, wenn eine Familie vorübergehend leer ist.
- **E-Mail-Zustellung:** Fehler bei den Übersetzungen behoben, die zuvor zu einem deutschen Fallback führten.
- **Zeitplanung:** Korrektur der Zeitberechnung für Mitternacht und sehr frühe Weckzeiten.

## 1.5.0 - 2026-03-21

### Neu
- **Onboarding:** Komplett neue Einführungstour mit Panda-Animationen für einen leichteren Start.
- **App-Design:** Neues, modernes App-Icon; Dark Mode als Standard für Erstnutzer; verbessertes Design der Wecker-Ansicht.
- **Admin-Konsole & Statistik:** Sicherer Zugriff auf App-Statistiken für Administratoren; wöchentliche Berichte per E-Mail.
- **Admin-Bereich:** Verwaltungsfunktionen für Administratoren in ein neues Menü verschoben.
- **Sicherheit:** Schutz privater Familiendaten vor unbefugtem Zugriff verbessert.
- **Datenschutz:** Personenbezogene Daten wurden aus internen Berichten entfernt (Minimierung von Nutzerdaten).
- **Stabilität:** Zuverlässigeres Beitreten und Verlassen von Familien.
- **Datensynchronisation:** Automatisierte Aktualisierung der Status-Anzeigen im Hintergrund für stets aktuelle Daten.

### Geändert
- **Datenschutz:** Erhöhter Schutz von Benutzerprofilen vor unbefugten Zugriffen.
- **Stabilität:** Sichereres Löschen und Verlassen von Familien durch neue Hintergrund-Logik.
- **Versionierung:** Vereinheitlichung der Versionsnummern für eine bessere Übersicht.
- **Konflikt-Lösung:** Verbesserte Unterstützung und Hinweise bei Zeitüberschneidungen im Familienplan.
- **System-Updates:** Optimierte App-Aktualisierungen und Tag-basierte Versionierung.

### Behoben
- **Zeitplanung:** Korrektur der Zeitberechnung für Mitternacht; sehr frühe Weckzeiten funktionieren nun korrekt.
- **Familienlöschung:** Fehler beim ID-Mapping korrigiert; Ersteller und globale Admins können Familien nun wieder zuverlässig löschen.
- **Sicherheit:** Unterstützung für Passwort-Manager verbessert und Schutz vor schädlichen Eingaben aktiviert.
- **Stabilität:** Fehler beim Anzeigen von Mitgliedern und doppelte Wecktöne behoben; verbesserter Ablauf beim Beitreten via Link.
- **Interne Optimierungen:** Verbesserte App-Erstellungsprozesse und Dateibenennungen.

## 1.4.0 - 2026-03-19

### Neu
- **Synchronisation:** Automatische Status-Aktualisierung im Hintergrund für stets aktuelle Daten beim App-Start.
- **Design:** Neue Einführungstour und Panda-Animationen für einen freundlichen Empfang.
- **Sicherheit:** Interner Audit erfolgreich abgeschlossen und Schutz vor schädlichen Skripten verstärkt.

### Behoben
- **Login:** Massive Verbesserung der Passwort-Manager-Kompatibilität.
- **Stabilität:** Fehler beim Anzeigen von Mitgliedern und doppelte Wecktöne behoben.
- **UI/UX:** Wochentag-Chips mit besserer Verteilung, Rot-Markierung bei Fehlern und klickbare Disclaimer/Footer.


---

## [1.3.0] - 2026-03-17

### Neu
- **⭐ App bewerten:** Direktes Bewerten im Play Store jetzt ganz einfach aus der App heraus möglich.

### Geändert
- **Wochentage:** Einheitliche Abkürzungen für eine übersichtlichere Anzeige.
- **Inaktive Tage:** Deaktivierte Tage werden nun deutlicher optisch hervorgehoben.
- **Sicherheit:** Schutz vor Spam bei E-Mails und Familienbeitritten aktiviert.

### Behoben
- **Anzeige:** Lesbarkeit der Wochentage verbessert.
- **Stabilität:** Fehlerbehebung bei der Familienerstellung und verbesserte Fehlermeldungen bei zu vielen Anfragen.

---

## [1.2.0] - 2026-03-17

### Neu
- **Alarm-Status-Persistenz:** Nach Neuinstall + Login wird der letzte bekannte Alarm-Status (An/Aus) automatisch aus Firestore wiederhergestellt.

### Behoben
- **Beitritts-Flow:** Fehler beim Beitreten der eigenen Familie behoben.
- **Stabilität:** Korrektur eines Fehlers bei der Familienerstellung, der zum App-Absturz führen konnte.
- **Hintergrund-Synchronisation:** Verlässlicherer Datenabgleich beim App-Start.
- **Fehlerbehebungen:** Mehrere Synchronisations-Fehler beim App-Start korrigiert, um den Alarm-Status korrekt beizubehalten.
- **Zeitplanung:** Korrektur der Frühstücks-Berechnung und Berücksichtigung individueller Wochentags-Einstellungen.

### Entfernt
- **„Was ist neu?"-Dialog** vollständig entfernt (Dialog, Logik, JSON-Datei, Strings).

---

## [1.1.5] - 2026-03-17

### Neu
- **Wochentags-Planung:** Weckzeiten und Badezimmer-Dauer können nun pro Wochentag individuell eingestellt werden.
- **Feedback:** Direktes Senden von Feedback-Nachrichten aus der App heraus.
- **Feedback UX:** Formular wird nach dem Absenden geleert; Screen schließt sich automatisch nach 2,5 Sekunden.
- **Settings-Footer:** Versionsnummer, klickbare Rechtlinks (Nutzungsbedingungen, Datenschutz, Impressum) und Copyright jetzt als Footer sichtbar.
- **Account löschen:** Externer Link zu `familienwecker.de/account-deletion.html` (DE) bzw. `/account-deletion-en.html` (EN) statt Info-Dialog.
- **Einstellungen restrukturiert:** Sprache und Erscheinungsbild in einer gemeinsamen Karte zusammengefasst; Hilfe & Feedback in eigener Karte.
- Nutzungsbedingungen (Terms of Use) direkt in den Einstellungen verlinkt.
- Disclaimer im Registrierungs-Screen mit klickbaren Links zu Nutzungsbedingungen und Datenschutz.

### Behoben
- **Zeitplan:** Zuverlässigere Prüfung auf Konflikte bei Bad- und Frühstückszeiten.
- **Netzwerk:** Verbesserte Anzeige des Offline-Status.
- **Sicherheit:** Zuverlässigerer Schutz vor Missbrauch bei zu vielen Anfragen.
- **Stabilität:** Mehrere Fehler beim Verlassen von Familien und Löschen von Mitgliederprofilen behoben.
- **Lokalisation:** Bereinigung ungenutzter Texte und Abgleich aller Sprachen.

---

## [1.1.0] - 2026-03-15

### Neu
- Scroll-Indicator (↓) auf dem Hauptscreen solange keine Mitglieder vorhanden.
- **Snooze:** Snooze-Button im Wecker-Screen (5 Min). Im Hauptscreen wird während eines aktiven Snooze ein Banner mit Endzeit und Abbruch-Button angezeigt.

### Behoben
- Wecker klingelt nach Geräteneustart (auch vor PIN-Eingabe).
- Wecker-Screen auf Sperrbildschirm (Samsung, Xiaomi u.a.).
- Google OAuth in selbst signierten APKs.
- Firebase Cleanup-Job löschte irrtümlich neue Familien.
- Akku-Kachel verschwindet sofort nach Bestätigung.

---

## [1.0.0] - 2026-03-12

### Sicherheit
- HTTP-Einladungslinks werden abgewiesen – nur HTTPS erlaubt.
- Familie löschen nur durch den Ersteller; andere erhalten eine Fehlermeldung.
- Profil-Auswahl offline gesperrt – verhindert irreführende Timeout-Fehler.

### Behoben
- Join-Link öffnet sofort den Konflikt-Dialog, auch wenn die App im Hintergrund läuft.
- Familie löschen klappt nun auch wenn andere User aktive Profile haben.
- Mitglieder-Limit erst bei 6 statt 5 aktiven Personen.
- Direkter Beitritt anderer User ohne Konflikt-Dialog behoben.
- Familienname-Anzeige in der Kopfzeile nach Neustart korrekt.
- Optimierter Ladevorgang: kein Flackern mehr beim App-Start.
- Vollständige Lokalisierung (DE/EN) aller neuer Strings.

---
