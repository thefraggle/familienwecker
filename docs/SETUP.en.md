# 🛠️ Technical Setup Guide (Android)

This guide explains how to get FamWake up and running locally. Since the app relies on Firebase, you need to configure your own backend project.

*Current Version: v0.4.9*

## 1. Create a Firebase Project
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Create a new project (e.g., "FamWake-Dev").
3. Add an **Android App**:
   - Package Name: `com.example.familienwecker`
   - Add your **SHA-1** and **SHA-256** fingerprints (required for Google Login).
4. Download the `google-services.json` file and move it into the `app/` directory.

## 2. Authentication & Branded Pages
1. In the Firebase menu under **Authentication**, enable the **Google** provider.
2. Copy the **Web Client ID** from the Google provider settings.
3. Open `app/src/main/res/values/strings.xml` in Android Studio.
4. Replace the value of `default_web_client_id` with your new Web Client ID.
5. **Branding**: The source templates for the password reset pages are located in the `auth/` directory of this project. These must be hosted and reachable on a web server (e.g., Firebase Hosting). Configure the Firebase Action URL to the corresponding address (e.g., `https://your-project.web.app/auth/reset-password.html`).

## 3. Cloud Firestore
1. Enable **Cloud Firestore** in the Firebase Console.
2. Choose a server location (e.g., `eur3` for Europe).
3. Start in "Test Mode" or directly with safety rules.
4. **IMPORTANT**: Copy the content of the `firestore.rules` file from this repository into the "Rules" tab in the Firebase Console and click "Publish".

## 4. Cloud Functions (Emails)
The app uses Cloud Functions for branded emails via **Resend**.
1. Install the Firebase CLI and log in.
2. Navigate to the `functions/` directory and run `npm install`.
3. Set the secret for the Resend API Key:
   `firebase functions:secrets:set RESEND_API_KEY`
4. Deploy the functions: `firebase deploy --only functions`
   *(This also deploys periodic garbage collection tasks).*


## 4. Build & Run
1. Open the project in **Android Studio** (Koala or newer recommended).
2. Let Gradle sync all dependencies.
3. Run the app on an emulator or a real device with Google Play Services.

## 5. Automated Tests
To ensure the correctness of the scheduling logic, unit tests have been integrated.
- **In Android Studio**: Right-click on the `app/src/test/java` folder -> "Run 'Tests in...'".
- **Via Command Line**: Run `./gradlew test` in the root directory.

---
*Note: Without a correctly configured `google-services.json` and the matching `default_web_client_id`, the login will fail.*

