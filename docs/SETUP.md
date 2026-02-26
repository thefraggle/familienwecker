# 🛠️ Technischer Setup-Guide (Android)

Diese Anleitung erklärt, wie du FamWake lokal zum Laufen bekommst. Da die App auf Firebase basiert, musst du dein eigenes Backend-Projekt konfigurieren.

## 1. Firebase Projekt erstellen
1. Gehe zur [Firebase Console](https://console.firebase.google.com/).
2. Erstelle ein neues Projekt (z. B. "FamWake-Dev").
3. Füge eine **Android-App** hinzu:
   - Package-Name: `com.example.familienwecker`
   - Füge deinen **SHA-1** und **SHA-256** Fingerabdruck hinzu (wichtig für Google Login).
4. Lade die `google-services.json` herunter und verschiebe sie in den Ordner `app/`.

## 2. Authentifizierung (Google Login)
1. Aktiviere im Firebase Menü unter **Authentication** die Methode **Google**.
2. Kopiere die **Web-Client-ID** aus den Einstellungen des Google-Providers.
3. Öffne `app/src/main/res/values/strings.xml` in Android Studio.
4. Ersetze den Wert von `default_web_client_id` durch deine neue ID.

## 3. Cloud Firestore
1. Aktiviere **Cloud Firestore** in der Firebase Console.
2. Wähle einen Server-Standort (z. B. `eur3` für Europa).
3. Starte im "Testmodus" oder direkt mit den Sicherheitsregeln.
4. **WICHTIG**: Kopiere den Inhalt der Datei `firestore.rules` aus diesem Repository in den Reiter "Rules" in der Firebase Console und klicke auf "Publish".

## 4. App bauen & starten
1. Öffne das Projekt in **Android Studio** (Koala oder neuer empfohlen).
2. Lass Gradle alle Abhängigkeiten synchronisieren.
3. Starte die App auf einem Emulator oder einem echten Gerät mit Google Play Services.

---
*Hinweis: Ohne eine korrekt konfigurierte `google-services.json` und die passende `default_web_client_id` wird der Login fehlschlagen.*
