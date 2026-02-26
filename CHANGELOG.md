# Changelog

Alle nennenswerten Änderungen an diesem Projekt werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog](https://keepachangelog.com/de/1.0.0/),
und dieses Projekt folgt der [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

*[🇬🇧 English version](CHANGELOG.en.md)*

## [0.3.1] - 2026-02-26
### Hinzugefügt
- **Profil-Besitz (Claiming):** Ein neues System wurde eingeführt, bei dem Nutzer in den Einstellungen ein Familienmitglied "beanspruchen" (claimen). Dies verhindert, dass andere Nutzer dasselbe Profil nutzen oder die Weckzeiten anderer Familienmitglieder verändern.
- **Sicherheits-Härtung (Firestore):** Neue Datenbank-Regeln erzwingen den Profil-Besitz auf technischer Ebene. Nur der "Besitzer" eines Profils kann dessen Daten ändern oder löschen.
- **Striktes Wecken & Auto-Alarm:** Der automatische Fallback auf das erste Profil wurde entfernt. Der globale Wecker ist nun standardmäßig AUS und schaltet sich erst automatisch EIN, wenn ein Profil erfolgreich geclaimt wurde. Ohne Profil-Zuweisung kann der Wecker nicht aktiviert werden.
- **UI-Navigation:** Die Warnung "Kein Profil ausgewählt" auf dem Dashboard ist nun anklickbar und führt direkt zu den Einstellungen.
- **Verbesserte Button-Platzierung:** Der "+"-Button zum Hinzufügen von Mitgliedern wurde von einem schwebenden Button (FAB) direkt in den Bereich der Familienmitglieder verschoben.
- **Priorisiertes Layout:** Der berechnete Weck-Plan steht nun an oberster Stelle auf dem Dashboard, gefolgt von der Mitgliederliste, da dieser im Alltag die wichtigste Information darstellt.
- **Automatische Profil-Wiederherstellung:** Nach einer Neuinstallation oder einem erneuten Login wird ein bereits beanspruchtes Profil automatisch wiedererkannt und ausgewählt.
- **Robustes Löschen:** Das Löschen einer Familie prüft nun auf andere aktive Mitglieder und zeigt eine explizite Warnung an, bevor Daten unwiderruflich entfernt werden.
- **Synchronisierte Navigation:** Nach dem Login wird nun explizit gewartet, bis alle Familiendaten und Profil-Zuordnungen geladen sind, um eine falsche Weiterleitung auf den Einrichtungs-Bildschirm zu verhindern.

### Geändert
- **Profil-Auswahl verbessert:** Das Dropdown-Menü in den Einstellungen ist nun deaktiviert, solange noch keine Familienmitglieder angelegt wurden.
- **Dynamische Warnungen:** Der Warnhinweis zur fehlenden Profil-Zuweisung erscheint nur noch, wenn bereits Mitglieder in der Familie vorhanden sind.

### Behoben
- **Navigations-Fehler (Backstack):** Ein Problem wurde behoben, bei dem der Login-Screen nach erfolgreicher Anmeldung im Hintergrund offen blieb. Ein "Zurück"-Wischen beendet die App nun wie erwartet.

## [0.3.0] - 2026-02-26
### Hinzugefügt
- Anzeige des Familiennamens in den Einstellungen: Neben dem Einladungscode wird nun auch der Name der Familie angezeigt, um Verwechslungen beim Beitritt oder bei der Verwaltung zu vermeiden.
- Navigation optimiert: Ein neuer Lade-Screen verhindert "doppeltes Wischen" zum Beenden der App und sorgt für einen sauberen App-Start.
- Logout im Setup-Screen: Ein neuer Button ermöglicht das Abmelden direkt aus dem Start-Setup, falls man ein anderes Konto nutzen möchte.
- Versionsverwaltung verbessert: Der interne Version-Code basiert nun auf einem Zeitstempel, was reibungslose Updates beim Drüber-Installieren der APK garantiert.

## [0.2.9] - 2026-02-25
### Behoben
- Abstürze beim Google Login (`NoCredentialException`) behoben, die auftraten, wenn auf dem Gerät noch gar kein Google Konto eingerichtet war.
- Die Konto-Auswahl beim Google Login (`setAutoSelectEnabled(true)`) optimiert, um systembedingte Abbrüche des Android `CredentialManager` zu verhindern.
- Code-Bereinigung: Ungenutzte Imports und ungenutzte Variablen entfernt.

## [0.2.8] - 2026-02-25
### Hinzugefügt
- Vollständige "Familie löschen"-Funktion in den Einstellungen hinzugefügt, welche die Familie inkl. aller Mitglieder sicher aus der Datenbank entfernt.
- Nutzer anderer Geräte werden nun automatisch auf den Startbildschirm zurückgeleitet, wenn ihre Familie gelöscht wurde.
- Automatisierte sprechende Dateinamen (inkl. Version und Build-Nummer) für die kompilierte APK eingeführt.

### Geändert
- Das Layout der Einstellungsseite trennt nun den Support-E-Mail-Button optisch mit einer zarten Trennlinie (HorizontalDivider) von den restlichen Weblinks ab.

### Behoben
- Google Sign-In Fehler auf dem Login-Screen werden jetzt rot auf dem Bildschirm ausgegeben (für einfacheres Debugging des SHA-256 Fingerabdrucks).
- Die URLs für Impressum, Datenschutz und Account-löschen wurden in der deutschen Sprachausgabe korrigiert, da versehentlich ein `-de` Suffix mitkopiert wurde.
- Die Konstante `default_web_client_id` wurde fest in die `strings.xml` integriert, um lästige "Unresolved reference"-Fehler in Android Studio nach einem Clean Build zu vermeiden.

## [0.2.7] - 2026-02-25

### Hinzugefügt (Added)
- **Account Löschen:** Ein Button "Konto löschen" wurde im Support-Bereich der Einstellungen hinzugefügt, der zur entsprechenden Löschungs-Seite verlinkt (Richtlinien-Anforderung).

## [0.2.6] - 2026-02-25

### Hinzugefügt (Added)
- **Flexibles Frühstück:** Die Frühstückszeit passt sich nun an enge Zeitpläne an und verkürzt sich automatisch um 5 bis 10 Minuten, wenn der gemeinsame Familienkalender ansonsten nicht aufgeht.
- **Englische Dokumentation:** Sowohl die README als auch das Changelog sind nun vollständig auf Englisch verfügbar und miteinander verlinkt.

### Geändert (Changed)
- **Rechtliche Dokumente:** Die Links zu Impressum und Datenschutzerklärung in der README und in den Einstellungen verweisen nun auf die korrekten, nativen Live-Webseiten-Pfade.

## [0.2.5] - 2026-02-24

Dies ist der initiale öffentliche Release auf GitHub nach einer umfassenden Codebereinigung und UI-Politur. 

### Hinzugefügt (Added)
- **Mehrsprachigkeit:** Die App ist nun vollständig in Deutsch und Englisch verfügbar. Die Sprache kann manuell in den Einstellungen umgeschaltet werden oder richtet sich nach den Systemeinstellungen.
- **Cloud-Synchronisierter Wecker-Schalter (Urlaubs-Modus):** Auf der Startseite gibt es nun einen Schalter ("Wecker Aktiviert/Pausiert"). Dieser deaktiviert nicht nur deinen lokalen Wecker, sondern meldet der Cloud, dass du heute kein Bad benötigst. Der Rest der Familie kann dadurch vollautomatisch länger ausschlafen. Pausierte Mitglieder werden im Dashboard mit einem "(Pausiert)" Label und blasserer Farbe hervorgehoben.
- **Support-Bereich:** In den App-Einstellungen gibt es nun einen Support-Bereich mit direktem E-Mail-Kontakt zum Entwickler.
- **Rechtliche Dokumente:** Links zu Datenschutzerklärung und Impressum in den App-Einstellungen (für App Store / Play Store Kompatibilität).
- **Adaptives App-Icon:** Ein neues, hochauflösendes und responsives App-Icon für alle gängigen Android-Launcher-Formate.
- **System Splash Screen:** Nativ unterstützter Android 12+ (API 31+) Splash Screen, der nahtlos in den ersten Screen der App übergeht.
- **README Dokumentation:** Screenshots und erklärende Feature-Übersicht für Besucher des GitHub-Repositories.

### Geändert (Changed)
- **Flexibler Alarm-Algorithmus:** Der Berechnungs-Algorithmus weicht Konflikten (z.B. überschneidende Badzeiten) nun intelligent aus und passt die Weckzeiten automatisch in 5-Minuten-Schritten (bis zu +/- 15 Minuten) an, um einen gangbaren Plan zu finden.
- **Dark Mode Anpassungen:** Verbessertes Farb-Thema (Theme.kt) für eine augenfreundlichere, kontrastreichere Darstellung bei aktiviertem Nachtmodus ("Dark Mode").
- **Settings Layout:** Die Einstellungen wurden logisch neu gruppiert. Version und Copyright-Infos sind jetzt deutlich als Footer am unteren Rand platziert.
- **Weck-Screen (Ringing Screen):** Das Layout des roten Aufwach-Screens wurde optimiert. Padding und zentrierter Text verhindern nun abgeschnittene Namen bei kleinen Displays oder langen Strings.
- **Projektname:** Der Anzeigename auf dem Homescreen und in der App wurde auf `FamWake - Familienwecker` (bzw. `Family Alarm`) vereinheitlicht.

### Behoben (Fixed)
- **Doppelte Splash Screens:** Ein Bug wurde behoben, bei dem auf neueren Android Versionen erst das Android System-Logo und danach nochmal ein ladebildschirm-ähnlicher Activity-Splash angezeigt wurde.
- **Platzhalter-Texte:** Hardcodierte Entwicklernamen ("Familie Notthoff" / "Smith Family") wurden aus den Textfeldern bei der Familien-Gründung entfernt und durch neutrale Beispiele ("Musterfamilie") ersetzt.

---
*Ältere, interne Entwicklungsstände (vor Version 0.2.5) sind in diesem öffentlichen Repository nicht dokumentiert.*
