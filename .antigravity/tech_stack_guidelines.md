# Tech Stack Guidelines
## Known Persistent Configs
- Android (Kotlin)
- Jetpack Compose expected
- Firebase (Auth, Firestore)

## Anti-Pattern Log
- **Fixed Critical Bugs:**
  - [2026-03-11] | Insecure Randomness | Standard 'kotlin.random.Random' used for auth/join codes. Solution: Migrated to 'java.security.SecureRandom()'.
  - [2026-03-11] | Hardcoded Dependencies | FirebaseRepository initialized inside ViewModel breaking testability. Solution: Extracted to ViewModelProvider.Factory.
  - [2026-03-11] | CI Cache Conflicts | GitHub Action ran on main and tags simultaneously, duplicating builds. Solution: Guard cache warming against simultaneous tag pipelines.
  - [2026-03-11] | Compose Deep Link Silence | Warm start 'onNewIntent' didn't trigger handling. Solution: Call handler in both 'onCreate' and 'onNewIntent'.
  - [2026-03-12] | Missing Native Symbols | Play Console warning. Solution: Explicit 'ndkVersion' and forced installation.
  - [2026-03-12] | Offline Startup Hang | App hung indefinitely. Solution: Added 2s emergency timeout.
  - [2026-03-12] | Silent Exceptions | Usage of 'printStackTrace()' in production. Solution: Replaced with 'Log.e()'.
  - [2026-03-12] | Captive Portal False-Positives | Hotel Wi-Fi check. Solution: Added 'NET_CAPABILITY_VALIDATED'.
  - [2026-03-12] | Cloud Function Secrets | Missing 'secrets' declaration. Solution: Added 'secrets' to 'onSchedule'.
  - [2026-03-12] | Firestore Timing Comparison | Unreliable direct comparison. Solution: Used '.toMillis()'.
  - [2026-03-12] | Plain SharedPreferences | Sensitive data stored in plain XML. Solution: Migrated to 'EncryptedSharedPreferences'.
  - [2026-03-12] | Global Collection Read | 'families' collection was readable by any user. Solution: Restricted Rules and moved to Cloud Function.
  - [2026-03-12] | Fragmented ViewModel Init | 'lateinit' caused race conditions. Solution: Switched to 'by viewModels()' delegate.
  - [2026-03-12] | Global Alarm Sync | Global 'isAlarmEnabled' affected all devices. Solution: Migrated to local PreferenceRepository only.
  - [2026-03-12] | Raw Firebase Errors | Registration errors shown in English. Solution: Mapped exceptions to localized string resources.
  - [2026-03-12] | Missing Member Status Sync | Alarm status of other claimed users not updated live. Solution: Added 'deviceAlarmEnabled' to Firestore (members) written by each device for its own user.
  - [2026-03-15] | False-Positive Family Deletion | cleanupInactiveFamilies deleted new families because (1) isStale defaulted to true and (2) missing createdAt fell back to 0 (unix epoch 1970). Solution: Default isStale=false, fallback Date.now() for missing timestamps, two-step check: createdAt old AND (no members OR lastUpdatedAt old).
  - [2026-03-15] | Alarm Lost After Reboot | AlarmManager entries are cleared on reboot. BootReceiver read EncryptedSharedPreferences which are unreadable before first unlock. Solution: New AlarmBackupPrefs (plain SharedPrefs), directBootAware BootReceiver listening to LOCKED_BOOT_COMPLETED, restores exact alarm time.  - [2026-03-16] | Firestore tx.update on missing doc | `tx.update()` throws "No document to update" when _rate_limits doc doesn't exist yet (first request per email). Solution: Use `tx.set({merge:true})` instead.
  - [2026-03-16] | gRPC error code is a number | `err.code` from Firestore/Admin SDK is a numeric gRPC code, not a string. Calling `.includes()` causes `TypeError`. Solution: Always cast with `String(err.code)` before string checks.
  - [2026-03-16] | tx.update pattern in ALL rate-limit checks | Same tx.update bug existed also in joinFamilyByCode and createFamily rate-limit transactions. Solution: Use tx.set({merge:true}) consistently everywhere. Also: joinFamilyByCode maxAttempts 5→10 (5 was too low for join/leave test cycles).
  - [2026-03-18] | Double Alarm Sound | Paralllel notifications and Activity both playing sound. Solution: Notification sound removed, `MediaPlayer` in `RingingActivity` used for all alarm audio. Use `USAGE_ALARM` in `AudioAttributes`.
  - [2026-03-18] | Notification Cancel Failure | Activity couldn't cancel notification because of differing ID calculation. Solution: Unified ID with `memberId.hashCode().and(0x7fffffff)` across Receiver and Activity.
  - [2026-03-18] | Awake Status Persistence | User clicked "awake" but UI didn't update or alarm still came. Solution: Added `cancelAlarmForCurrentUser()` and `recalculateSchedule()` to `toggleAwakeMember`.
