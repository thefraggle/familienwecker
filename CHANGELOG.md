# Changelog

Alle nennenswerten Änderungen an diesem Projekt werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog](https://keepachangelog.com/de/1.0.0/),
und dieses Projekt folgt der [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

*[🇬🇧 English version](CHANGELOG.en.md)*

## [0.2.6] - 2026-02-25

### Hinzugefügt (Added)
- **Flexibles Frühstück:** Die Frühstückszeit passt sich nun an enge Zeitpläne an und verkürzt sich automatisch um 5 bis 10 Minuten, wenn der gemeinsame Familienkalender ansonsten nicht aufgeht.

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
