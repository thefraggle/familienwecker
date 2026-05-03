# Changelog

*[🇺🇸 English Version](CHANGELOG.en.md)*

## 1.8.7 – 2026-05-03
### Neu
- **Sofort starten** – App kann jetzt auch ohne Account genutzt werden (Lazy Registration). Man kann direkt eine Familie erstellen und den Wecker nutzen. Ein Account wird erst benötigt, um Codes zu teilen oder einer anderen Familie beizutreten.

### Fehlerbehebungen
- **Onboarding & Navigation:** Nach der Onboarding-Tour landest du nicht mehr in einer Sackgasse, und fehlgeleitete Logout-Dialoge im Testmodus wurden korrigiert.
- **Account-Übernahme:** Wenn du dich aus dem Testmodus heraus registrierst, werden deine Testdaten nun verlässlich in deinen neuen Account übernommen.

---

## 1.8.6 – 2026-05-03
### Verbessert
- **Angenehmerer Einstieg** – FamWake fragt nun nicht mehr direkt beim ersten Start nach der Berechtigung für den Sperrbildschirm. Ein dezenter Hinweis erscheint erst, wenn auch wirklich ein Wecker gestellt wurde.
- **Bessere Lesbarkeit** – Wir haben den Farbkontrast von Fehlermeldungen erhöht, damit diese nun auf allen Bildschirmen deutlich besser lesbar sind.

---

## 1.8.5 – 2026-05-01
### Neu
- **Weltweit verfügbar** – FamWake unterstützt nun 7 weitere Sprachen: Indonesisch, Vietnamesisch, Bengalisch, Marathi, Hindi, Chinesisch (Vereinfacht) und Koreanisch. Damit ist die App nun in 22 Sprachen verfügbar.
- **Lokalisierte E-Mails** – System-E-Mails (wie zum Zurücksetzen des Passworts) werden nun ebenfalls in der eingestellten App-Sprache versendet.

---

## 1.8.4 – 2026-04-28

### Verbessert
- **Stabilere Weckplan-Berechnung** – Randfälle bei sehr frühen Frühstückszeiten werden jetzt korrekt abgefangen.
- **Übersetzungen vervollständigt** – Benachrichtigungs-Einstellungen sind jetzt in allen 15 App-Sprachen korrekt beschriftet.

---

## 1.8.3 – 2026-04-28

### Behoben
- **Doppelter Alarm bei unterschiedlichen Wochentag-Zeiten** – Hatte z.B. Montag/Dienstag 6:30 Uhr und der Rest der Woche 7:30 Uhr, klingelte der Wecker nach dem Di-Alarm nochmal um 7:30 Uhr am selben Tag. Behoben.

---

## 1.8.2 – 2026-04-25

### Behoben
- **Sprachauswahl funktioniert wieder zuverlässig** – Wechsel zu Dänisch, Japanisch, Niederländisch, Polnisch, Türkisch und allen anderen Sprachen war auf manchen Geräten wirkungslos (Anzeige blieb auf Englisch). Behoben.

---

## 1.8.1 – 2026-04-24

### Behoben
- **Wecker-Schalter bleibt erhalten** – An/Aus-Zustand wird nach Abmelden und erneutem Anmelden korrekt beibehalten.
- **Familienwechsel** – Beim Wechsel in eine neue Familie via Einladungslink wird der Wecker-Schalter sicher deaktiviert, bis ein Profil gewählt wird.
- **Passwort-Reset** – Zurücksetzen funktioniert wieder zuverlässig für alle Konten.
- **Anderer Account auf gleichem Gerät** – Login mit einem anderen Konto setzt den Wecker-Status korrekt auf den Zustand dieses Accounts.

---

## 1.8.0 – 2026-04-21

### Neu
- **Push-Benachrichtigungen 🔔** – Du wirst automatisch informiert, wenn sich der Familienplan ändert – z.B. bei Reihenfolge, Wecker ein/aus oder Pause. Alle Familienmitglieder werden benachrichtigt.
- **Push-Toggle** – Push-Benachrichtigungen können in den App-Einstellungen ein- und ausgeschaltet werden.
- **Familien-Events** – Du erfährst sofort, wenn jemand deiner Familie beitritt oder sie verlässt.
- **Smartes Review-System** – Die App fragt nach positivem Erlebnis unaufdringlich nach einer Bewertung.
- **20 Weck-Sprüche** – Abwechslung auf dem Weck-Screen.

### Verbessert
- **Live-Sync** – Wecker ein/aus aktualisiert den Weckplan sofort auf allen Geräten.
- **Zuverlässigerer Wecker** – Verbessertes Verhalten auf dem Sperrbildschirm, deutlichere Warnung bei fehlenden Berechtigungen.
- **Fresh Install** – Der Wecker ist nach Neuinstallation automatisch aktiv, wenn bereits ein Profil existiert.
- **Stille Push-Nachrichten** – Info-Benachrichtigungen sind lautlos und stören nicht.
- **Onboarding überarbeitet** – Klarere Einführung, landesspezifische Beispiele.
- **Frischeres Design** – Weichere Ecken, Scroll-Hinweis, kollabierbarer Titel.
- **Übersichtlichere Zeitauswahl** – Tastatur-Dialog statt Uhrzeiger.
- **Reorder-Warnung** – Hinweis wenn ein Mitglied ohne eigenes Profil an erster Stelle im Plan steht.

### Behoben
- Wecker-Bildschirm erscheint ohne PIN-Abfrage über dem Sperrbildschirm.
- Diverse Stabilitäts-, Sync- und Zuverlässigkeitsverbesserungen.

---


## 1.7.0 – 2026-04-06

### Neu
- **Berechtigungs-Warnung** – Fehlt die Wecker-Berechtigung, erscheint eine rote Kachel auf dem Hauptscreen.
- **Automatische Profilverknüpfung** – Beim Anlegen des ersten Profils wirst du automatisch damit verbunden.
- **Wochentags-Planung** – Weckzeiten und Badezimmer-Dauer pro Wochentag individuell einstellbar.

### Behoben
- Wecker klingelt nach Aus/Ein wieder zuverlässig.
- „Ich bin wach"-Button bleibt nach Profilneuanlage nicht mehr dauerhaft ausgegraut.
- Zweites Familienmitglied kann Profil jetzt zuverlässig bearbeiten.
- „Schon wach"-Status wird am nächsten Morgen korrekt zurückgesetzt.
- Login-Screen bei großer Systemschrift (125 %+) nutzbar.
- Diverse Sync- und Stabilitätsverbesserungen.

---

## 1.6.0 – 2026-03-23

### Neu
- **Einladungslinks** – Verbessertes Feedback beim Beitritt via Link.

### Behoben
- Einladungscodes bleiben gültig, auch wenn eine Familie vorübergehend leer ist.
- Korrektur der Zeitberechnung für Mitternacht und sehr frühe Weckzeiten.

---

## 1.5.0 – 2026-03-21

### Neu
- **Onboarding** – Neue Einführungstour mit Panda-Animationen.
- **Neues App-Icon** – Modernes Design, Dark Mode als Standard.
- **Feedback** – Direktes Senden von Feedback-Nachrichten aus der App heraus.

### Behoben
- Zuverlässigeres Beitreten und Verlassen von Familien.
- Fehlerbehebung bei Mitglieder-Anzeige und doppelten Wecktönen.

---

## 1.4.0 – 2026-03-19

### Neu
- **Onboarding-Tour** – 5-Screen-Einführung mit Panda-Animationen und neuem Weck-Screen-Design.

### Verbessert
- **Autofill & Login** – Deutlich bessere Passwort-Manager-Kompatibilität.
- **Sicherheit** – XSS-Schutz bei Feedback-E-Mails, Cloud-Reset für Status-Resets.

### Behoben
- Doppel-Alarme, Deep-Link-Flows und Mitglieder-Mapping korrigiert.

---

## 1.3.0 – 2026-03-17

### Neu
- **⭐ App bewerten** – Direktes Bewerten im Play Store aus der App heraus.

### Verbessert
- Deaktivierte Tage werden deutlicher optisch hervorgehoben.
- Schutz vor Spam bei E-Mails und Familienbeitritten.

---

## 1.2.0 – 2026-03-17

### Neu
- **Feedback-Screen** – Feedback direkt aus der App senden.
- **Wochentag-Konfiguration** – Individuelle Zeiten pro Wochentag.

### Behoben
- Alarm-Status wird nach Neuinstallation wiederhergestellt.
- Frühstückszeit-Berechnung und Familien-Erstellung stabilisiert.

---

## 1.1.0 – 2026-03-15

### Neu
- **Snooze** – 5-Minuten-Snooze mit Banner und Abbruch-Button.

### Behoben
- Wecker klingelt nach Geräteneustart (auch vor PIN-Eingabe).
- Wecker-Screen auf Sperrbildschirm (Samsung, Xiaomi u. a.).

---

## 1.0.0 – 2026-03-12

### Erster Release 🎉
- Familien-Wecker mit koordinierter Badezimmer-Planung.
- Einladungscodes zum Beitritt, Google Sign-In.
