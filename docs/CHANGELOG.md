# Changelog

Alle wichtigen Änderungen an diesem Projekt werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog](https://keepachangelog.com/de/1.0.0/),
und dieses Projekt hält sich an [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

*[🇺🇸 English Version](CHANGELOG.en.md)*

 ## [0.5.0] - Unreleased
 
 ### Hinzugefügt
 - 
 
 ### Geändert
 - 
 
 ### Behoben
 - 
 
 ## [0.4.16] - 2026-03-06
 
 ### Hinzugefügt
- **Einladungs-Sharing:** Neues System zum Teilen des Familien-Codes via Android `ACTION_SEND` (Link: `https://familienwecker.de`).
- **Setup-UI:** Verfeinerung des Familien-Gründen Screens (Abstände, Logout-Position).
 
 ### Geändert
- **Texte:** Überarbeitung der Einladungs-Nachrichten (DE/EN) für eine persönlichere Note.
- **Navigation:** Bereinigung des Setup-Flows (entfernen experimenteller Animationen).
 
 ## [0.4.15] - 2026-03-05
 
 ### Hinzugefügt
- **"Was ist neu" Popup:** Einführung eines dynamischen News-Dialogs nach App-Updates (JSON-basiert).
- **Willkommens-Nachricht:** Erstmaliger Dialog für neue Nutzer und Rückkehrer nach dem Lottie-Update.
 
 ### Geändert
- **Infrastruktur:** Erweiterung des PreferencesRepository um Version-Tracking für News.

## [0.4.14] - 2026-03-05
 
 ### Hinzugefügt
- **Lottie-Animationen:** Integration von dynamischen Animationen für Empty-States ("Kein Wecker gestellt" und "Keine Mitglieder hinzugefügt").
- **Layout-Optimierung:** Verbesserung der Dashboard-Abstände für ein kompakteres Erscheinungsbild.
 
 ### Geändert
- **EmptyState:** Support für Lottie-Ressourcen inklusive Fallback auf statische Bilder.

## [0.4.13] - 2026-03-05

### Hinzugefügt
- **Empty States:** Schöne Illustrationen und neue `EmptyState`-Komponente für leere Listen.
- **UI-Refinement:** App-Name in Header und Footer ohne Bindestriche; modernisierte Settings-Struktur (3-Zeilen-Stil).

### Geändert
- **Strings & Lokalisierung:** Bereinigung und Update der Sprach-Ressourcen für eine professionellere Wirkung.
- **UX:** Automatischer Daten-Refresh beim App-Start im Vordergrund.

## [0.4.12] - 2026-03-04

### Geändert
- **Sprach-Optimierung:** Fehlermeldungen wurden komplett überarbeitet und werden nun intelligent in der korrekten Gerätesprache (Deutsch/Englisch) angezeigt. Technische Fehlermeldungen vom Server werden für den Nutzer verständlich übersetzt.

## [0.4.11] - 2026-03-04
- **Fehlerbehandlung:** Korrektur eines fälschlicherweise angezeigten Fehlers beim App-Start nach einer Daten-Wiederherstellung.

## [0.4.10] - 2026-03-04
- **Stabilitäts-Update:** Behebung eines Absturzes, der kurzzeitig bei der Verbindung zum Server auftreten konnte.

## 🎯 State of the App (2026-03-06) - v0.5.0 (Internal Dev)
v0.4.16 erfolgreich veröffentlicht. Nächster Meilenstein: Hero-Sektion und Tab-Navigation für v0.5.0.

## [0.4.9] - 2026-03-04
- **Fehlerbehandlung:** Behebung eines Problems, bei dem eine Neuinstallation (aus einem Backup) kurzzeitig einen Zugriffsfehler verursachen konnte.

## [0.4.8] - 2026-03-04

### Hinzugefügt
- **Hausputz:** Ein automatisches System räumt nun alle 180 Tage verwaiste Familiendaten auf dem Server auf, um die Performance hochzuhalten.

### Geändert & Verbessert
- **Akku- & Performance:** Massive Optimierung der App-Hintergrundprozesse. Die App schont nun spürbar den Akku, während der Wecker weiterhin absolut präzise arbeitet.
- **Dark Mode:** Der dunkle Modus wurde weiter verfeinert und bietet nun perfekte Kontraste auf allen Bildschirmen.
- **Besseres Planungs-Limit:** Um Abstürze bei extrem großen Familien zu verhindern, wurde das Limit auf maximal 6 aktive Mitglieder pro Weckplan festgelegt.
- **Sichere Einladungscodes:** Die generierten Codes verzichten nun auf verwechselbare Zeichen wie „0“ und „O“, um Tippfehler beim Beitreten zu vermeiden.

### Behoben
- **Auto-Reset:** Mitglieder, die manuell pausiert wurden, werden jetzt zuverlässig am nächsten Tag wieder aktiviert.

## [0.4.7] - 2026-03-04

### Hinzugefügt
- **Visuelles Feedback:** Bounce-Effekt beim Klicken von Buttons und interaktiven Elementen für ein reaktionsschnelles Gefühl.
- **Icons & Hilfe:** Neue Icons für externe Links (Datenschutz, Impressum) und E-Mail-Support zur besseren Orientierung.
- **Sicherheit:** Prüfung auf Eindeutigkeit der Familien-Codes beim Erstellen.

### Geändert
- **Settings-Struktur:** Logische Neuordnung der Sektionen (Profil, Weckton, Familie, Sprache, Hilfe & Support).

## [0.4.6] - 2026-03-04

### UI & UX Overhaul (Dark Mode 2.0)
- **Deep Dark Mode:** Umstellung des dunklen Designs auf extrem dunkle Hintergründe für einen ultra-modernen Look und akkuschonende Darstellung (OLED).
- **Kontrast-Optimierung:** Verfeinerung aller Text- und Icon-Farben für das neue Design.
- **Branding:** Die App zeigt nun konsistent das **FamWake** Logo im Header an.
- **Font-Strategie:** Wechsel auf die "Nunito" Schriftart in verschiedenen Stärken für eine modernere Typografie.

## [0.4.5] - 2026-03-04

### UI & UX Modernisierung
- **Glassmorphism:** Einführung von halb-transparenten Karten mit subtilen Rahmen.
- **Gradients:** Sanfte vertikale Farbverläufe je nach Theme.
- **CornerRadius:** Erhöhung auf weichere 24dp - 32dp.
- **Theme-Auswahl:** Unterstützung für "Hell", "Dunkel" und "System" inklusive manueller Auswahl in den Einstellungen.

## [0.4.x] - Weitere Verbesserungen
- **Unit Tests:** Automatisierte Tests für den Kern-Algorithmus (`Scheduler`).
- **Icon-Cleanup:** Entfernung ungenutzter Grafiken zur Reduzierung der App-Größe.
- **E-Mail-Branding:** Designte HTML-E-Mails für Passwort-Reset und Verifizierung (DE/EN).

## [0.3.x] - Zusammengefasst (Februar 2026)
In dieser Phase wurden die Grundlagen für die Familien-Planung gelegt:
- **Profil-System:** Einführung des "Beanspruchens" von Profilen, um eigene Weckzeiten zu schützen.
- **Wecker-Präzision:** Komplette Überarbeitung des Alarmsystems (Android 14 Support, neue Klingeltöne, Fullscreen-Weckscreen).
- **Design-Evolution:** Einführung von Glasmorphismus, sanften Übergängen und modernem Dark Mode.
- **Sicherheits-Update:** Striktes Rechte-Management und sichere Cloud-Speicherung (Firestore).
- **Validierung:** Prüfung von Badezimmerzeiten und Abfahrtsterminen zur Vermeidung unmöglicher Pläne.

## [0.2.5] - 2026-02-24
Erster öffentlicher Release.
- Fokus auf Weck-Algorithmus, Mehrsprachigkeit (DE/EN) und intuitive Bedienung.
