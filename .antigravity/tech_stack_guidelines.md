# Tech Stack Guidelines
## Known Persistent Configs
- Android (Kotlin)
- Jetpack Compose
- Firebase (Auth, Firestore, Functions Node.js 22)

## Anti-Pattern Log
- **Fixed Critical Bugs:**
  - [2026-03-11] | Insecure Randomness | Standard 'kotlin.random.Random' used. Migrated to 'java.security.SecureRandom()'.
  - [2026-03-12] | Plain SharedPreferences | Sensitive data stored in plain XML. Migrated to 'EncryptedSharedPreferences'.
  - [2026-03-12] | Global Collection Read | 'families' collection was readable. Restricted Rules + Cloud Function.
  - [2026-03-16] | gRPC error code is a number | Calling `.includes()` on numeric code caused crash. Solution: String cast.
  - [2026-03-18] | Double Alarm Sound | Parallel notifications/Activity. Notification sound disabled.
  - [2026-03-18] | Context Menu Blockade | Nested Scaffolds/Modifiers blocked long-press. Simplified UI tree.
  - [2026-03-19] | Android Autofill Fail | Compose semantics inconsistent. Switched to manual 'AutofillNode' registration.
  - [2026-03-19] | XSS in Feedback Emails | User input unsanitized in HTML. Added 'escapeHtml()' helper.
  - [2026-03-19] | Resend SENDER format | False assumption about object properties (.name/.email) vs language keys (.de). Solution: Use pre-formatted strings for `from`.
  - [2026-03-19] | Auth State Race Condition | `auth.currentUser` can be null at ViewModel init. Solution: Use `flatMapLatest` on `getAuthStateFlow()`.
  - [2026-03-19] | Field Name Drift | Firestore used `name`, report used `familyName`. Solution: Unified access with fallback.

## 🛑 Critical & High Severity Issues
*Currently none open.*
