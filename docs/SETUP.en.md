# 🛠️ Technical Setup Guide (Android)

This guide explains how to get FamWake up and running locally. Since the app relies on Firebase, you need to configure your own backend project.

## 1. Create a Firebase Project
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Create a new project (e.g., "FamWake-Dev").
3. Add an **Android App**:
   - Package Name: `com.example.familienwecker`
   - Add your **SHA-1** and **SHA-256** fingerprints (required for Google Login).
4. Download the `google-services.json` file and move it into the `app/` directory.

## 2. Authentication (Google Login)
1. In the Firebase menu under **Authentication**, enable the **Google** provider.
2. Copy the **Web Client ID** from the Google provider settings.
3. Open `app/src/main/res/values/strings.xml` in Android Studio.
4. Replace the value of `default_web_client_id` with your new Web Client ID.

## 3. Cloud Firestore
1. Enable **Cloud Firestore** in the Firebase Console.
2. Choose a server location (e.g., `eur3` for Europe).
3. Start in "Test Mode" or directly with safety rules.
4. **IMPORTANT**: Copy the content of the `firestore.rules` file from this repository into the "Rules" tab in the Firebase Console and click "Publish".

## 4. Build & Run
1. Open the project in **Android Studio** (Koala or newer recommended).
2. Let Gradle sync all dependencies.
3. Run the app on an emulator or a real device with Google Play Services.

---
*Note: Without a correctly configured `google-services.json` and the matching `default_web_client_id`, the login will fail.*
