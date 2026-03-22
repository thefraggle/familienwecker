# 🧪 Test Plan - FamWake (v1.5.8)

This documentation describes the testing strategy and test cases for the FamWake app to ensure high reliability of the wake-up logic and a smooth user experience.

*[🇩🇪 Deutsche Version](test_plan.md)*

---

## 📋 Overview & Strategy

The FamWake app is based on a dynamic scheduling algorithm. Tests must therefore validate not only the UI but especially the mathematical correctness and stability of the schedule calculation under various constraints.

### Test Areas
1.  **Onboarding & Account:** Registration, Login, joining a family.
2.  **Family Management:** Adding/removing members, roles.
3.  **Scheduling Logic (Core):** Bathroom timing, breakfast planning, buffer times.
4.  **Alarm Function:** Sound, full-screen notification, snooze.
5.  **Edge Cases & Robustness:** Offline status, time zones, conflict situations.
6.  **Assets & Resources:** Icon scaling, splash screen integrity.

---

## 🛠 Test Cases (Standard Operation)

### 1. Account & Onboarding
| ID | Test Case | Expected Result |
|:---|:---|:---|
| TC-01 | Initial registration with email | Account created, confirmation email sent. |
| TC-02 | Found a family | User becomes "Admin" of a new family, invitation code generated. |
| TC-03 | Join a family | User joins an existing family via code. |
| TC-20 | Forgot Password (Reset) | Branded email sent, link leads to branded HTML page, password change works. |
| TC-21 | **Invitation Sharing** | Clicking "Share" opens the Android dialog with the family code and link. |
| TC-22 | **Deep Link (App Start)** | Clicking a `/join/` link opens the app directly (instead of browser). |
| TC-23 | **Auto-Join via Link** | App recognizes the code from the URL and shows the "Join" dialog. |
| TC-24 | **Re-Join Resilience** | Re-joining the own family via link no longer clears the profile assignment. |
| TC-25 | **Self-Healing (Deep Link)** | Login via link triggers new join even if an old profile exists. |
| TC-26 | **Single Instance Check** | Repeatedly clicking deep links does not open new app instances in task manager. |
| TC-27 | **Self-Join Guard** | Entering the own family code in the join dialog does **not** trigger a re-join (no profile assignment lost). |
| TC-28 | **Deep Link Conflict Dialog** | Clicking a deep link while already in a family opens MainScreen showing a warning dialog to switch. |
| TC-29 | **Join Code Validation Guard** | Invalid deep/join code only shows an error message after confirmation, without leaving the old family. |
| TC-30 | **Join Code Security** | A generated 6-digit code remains purely alphanumeric without 0, O, 1, I and is generated via SecureRandom. |
| TC-32 | **Rate-Limiting (H-1)** | Repeated wrong code entries (>5/min) trigger server-side blocking ("Resource Exhausted"). |
| TC-33 | **Email Rate-Limiting** | More than 5 password-reset or verification emails for the same address within one hour are blocked server-side ("Resource Exhausted"). |
| TC-34 | **HTTP link rejected** | Calling `http://familienwecker.de/join/CODE` must not trigger a join – app ignores the HTTP scheme. |
| TC-35 | **Admin-only delete** | Non-creator opens Settings → delete family → receives error message instead of confirmation dialog. |
| TC-36 | **Offline profile claim blocked** | Open dropdown in airplane mode → Snackbar with offline error appears immediately. |
| TC-37 | **Deep link instant dialog (background)** | App is on Settings screen, open join link → Conflict dialog appears immediately without back navigation. |
| TC-38 | **Battery card instant reset** | Disable battery optimization, return to app → card disappears immediately without screen navigation. |
| TC-39 | **Scroll indicator** | Main screen without members: bouncing arrow visible at bottom. Disappears on first scroll or once a member is added. |
| TC-40 | **Reboot alarm persistence** | Set alarm for 2 min → reboot device → alarm rings even before PIN entry on the lock screen. |
| TC-41 | **Snooze survives reboot** | Press snooze → reboot within 5 min → alarm rings at the snooze time. |
| TC-42 | **Email rate-limit first request** | Password reset for a new (never requested) email address → email is sent correctly, no internal error. |
| TC-43 | **Leave family – member deleted** | Leave family → own member profile deleted from Firestore. On rejoin: no old profile visible, new one must be created and claimed. |
| TC-44 | **Breakfast-bath conflict** | Member with bath duration ≥ time until breakfast → scheduler reports conflict, no silent failure. |
| TC-45 | **Offline indicator** | Disconnect network: offline icon appears after >3s. Reconnect: icon disappears immediately. On WiFi, no offline icon shown even if Firestore briefly serves cached data. |
| TC-51 | **New account – create family** | New email account, create family → app stays on main screen, no redirect back. |
| TC-52 | **Alarm restore after reinstall** | Alarm ON → uninstall app → reinstall → login → alarm is ON again. |
| TC-53 | **Multi-account on one device** | Log out User A, log in User B → B sees only their own family, no data from A. |
| TC-54 | **Alarm logout isolation** | User A has alarm ON → logout → `deviceAlarmEnabled` of member stays unchanged in Firestore (no false-write). |
| TC-55 | **Rate app button** | Tap „⭐ Rate App“ → In-app review dialog opens (or fallback to Play Store if unavailable). No crash. |
| TC-56 | **Daily rate limit** | After reaching the hourly limit, wait for the hour to pass and try again: the daily limit (2× hourly) kicks in after the second hour and blocks further attempts for 24h. |
| TC-71 | **Onboarding – first launch** | After fresh install/login: onboarding slides appear (5 screens with pager). Slide 0 shows the animated panda. |
| TC-72 | **Onboarding – app tour** | Settings → "Show app tour" → onboarding slides open completely. |
| TC-73 | **Paste in login screen** | Long-press on email or password field → native context menu with "Paste" appears (despite simplification). |
| TC-74 | **Context Menu in Family Setup** | Long-press on family name or join code field → native context menu appears. |
| TC-75 | **Autofill Support (manual)** | Focus on email field → keyboard suggests saved addresses. Password manager offers autofill via `AutofillNode`. |
| TC-76 | **Debouncing Toggles** | Rapidly clicking Awake/Pause multiple times → Logcat shows only one Firestore write after 2s. |
| TC-77 | **Master-Switch Debounce** | Rapidly toggling global alarm switch → sync of the status icon to others occurs only once with a delay. |
| TC-78 | **Batch-Reset Performance** | Manual reset trigger (via debug) → all members are updated in a single batch transaction. |
| TC-79 | **Lazy-Refresh** | UI must immediately reset when returning to foreground if threshold > 2h. |
| TC-80 | **Cloud-Reset** | Status must disappear from Firestore after 2h even without app interaction. |
| TC-81 | **New Member Creation** | Create new member -> Appears immediately in list (verifies Firestore Timestamp mapping). |
| TC-82 | **Deep Link Auto-Join** | Click join link without family -> Joins immediately and opens MainScreen. |
| TC-83 | **Deep Link Nav Fix** | Click join link while in family -> Confirmation switches family without Setup screen loop. |
| TC-84 | **Family Deletion** | Delete family as creator -> All members and family deleted successfully. |
| TC-85 | **Settings UI Feedback** | Error during leave/delete (e.g. offline) -> Snackbar with error message appears. |
| TC-86 | Login (Autofill Position) | Tap email field. | Autofill dropdown appears directly below the field (not shifted). |
| TC-87 | Login (Password Manager) | Open login screen. | Password manager (e.g. Google) actively offers saved credentials. |
| TC-88 | Security (XSS Feedback) | Send feedback with `<script>alert(1)</script>`. | Email shows the code as text, no execution or layout break. |


### 2. Family Configuration
| ID | Test Case | Expected Result |
|:---|:---|:---|
| TC-04 | Change wake-up preferences | Latest wake-up time and bathroom duration are saved. |
| TC-05 | Toggle breakfast request | Algorithm considers the member for breakfast time calculation. |
| TC-06 | Set leave-home time | Schedule is adjusted so the person is ready on time. |
| TC-10 | **Member Limit** | At 6 members, the "Add" button is disabled. |
| TC-11 | **Permission Protection** | Profiles claimed by others do not show an edit icon and do not respond to clicks. |
| TC-12 | **Drag & Drop Reordering** | Long-press on a schedule tile allows moving it. Swap occurs at >50% overlap. |
| TC-13 | **Order Persistence** | After reordering, the new sequence is preserved after app restart and across multiple devices. |
| TC-57 | **Chip layout (all 7 days visible)** | Open profile editor → all 7 weekday chips (Mo–Su) are fully visible and equally wide on a narrow screen. |
| TC-58 | **Chip error highlight** | Set an invalid time combination → affected chip is highlighted in red (border + text). |
| TC-59 | **Validation – latest wake time** | Set `latestWakeUp` ≤ `earliestWakeUp` → red error text appears; save button is disabled. |
| TC-60 | **Validation – leave home time (explicit)** | Set leave time ≤ `latestWakeUp + bathroom duration` → red error text; save button disabled. |
| TC-61 | **Validation – leave home time (default)** | Set `latestWakeUp` to 21:00 without touching leave time (default 08:00) → error text appears immediately. |
| TC-62 | **Next alarm – today disabled** | Disable today's weekday, enable tomorrow's → main screen shows tomorrow's alarm (not "no active alarm"). |
| TC-63 | **Next alarm – today & tomorrow disabled** | Disable both today's and tomorrow's weekday → main screen shows "no active alarm". |
| TC-64 | **Alarm rings reliably (background)** | Send app to background → wait for alarm time → alarm rings and `RingingActivity` opens. |
| TC-65 | **Alarm after Firebase sync** | Set alarm → change member data shortly after the wake time → alarm was still triggered correctly (grace period protects it). |
| TC-66 | **All weekdays inactive** | Disable all days → member tile shows "no alarm" with no wake time details; schedule shows NoActiveSchedule. |
| TC-67 | **Next active day (day after tomorrow)** | Only Friday active, today is Wednesday → member tile shows "Friday" + Friday times; schedule card shows date "Friday, XX March". |
| TC-68 | **Snooze + regular alarm no conflict** | Press snooze → snooze rings → stop alarm → no regular alarm overwrites the running snooze. |
| TC-69 | **Periodic refresh** | App open, no changes → after max. 5 minutes the schedule is automatically recalculated (Logcat: `applyAlarms: alarm SET` or `day X is inactive`). |
| TC-70 | **Stale schedule disappears** | Alarm rings and is stopped → after max. 5 minutes the main screen no longer shows the old schedule but the correct state (next day / NoActiveSchedule). |

### 3. Scheduling Logic (Algorithmic Tests)
| ID | Test Case | Expected Result |
|:---|:---|:---|
| TC-07 | Standard scenario (4 people) | A schedule without bathroom overlaps is created. |
| TC-08 | Breakfast coordination | All "breakfasters" finish with the bathroom before the shared time. |
| TC-09 | Masterplan Update | If one member wakes up earlier, the plan for the rest of the family is optimized (later wake-up). |

---

## ⚠️ Edge Cases

### 1. Conflict Situations (Stress Tests)
| ID | Test Case | Expected Result |
|:---|:---|:---|
| EC-01 | **Impossible Plan** | Everyone wants the bathroom at the same time. | App shows a conflict warning and compromise suggestions. |
| EC-02 | **Extreme Bathroom Duration** | Member with 120 min. | Plan shifts others significantly; warning shown if applicable. |
| EC-03 | **Short Time Windows** | Wake 7:00, leave 7:05. | App warns about the tight time window. |

### 2. Technical Edge Cases
| ID | Test Case | Expected Result |
|:---|:---|:---|
| EC-04 | **Offline Calculation** | Last valid plan stored locally; alarm rings without internet. |
| EC-05 | **Daylight Saving Time** | Wake times correctly adjusted, no duplicate alarms. |
| EC-06 | **App Crash during Alarm** | Alarm service restarts automatically. Alarm also survives device reboot (AlarmBackupPrefs + LOCKED_BOOT_COMPLETED). |
| EC-07 | **Battery Optimization** | App marked as "Not optimized". |
| EC-11 | **Snooze** | Snooze (5 min) schedules new alarm exactly 5 min later. |
| EC-12 | **Midnight Reset** | "Pause for Today" and "Already Awake" reset the next day. |
| EC-14 | **Persistence & Logout** | No old login data or family IDs after logout/reinstall. |
| EC-15 | **Delete Family** | Double confirmation if other members exist; single for empty family. |
| EC-16 | **Delete Member** | Confirmation dialog before deletion. |
| EC-17 | **New Setup after Deletion** | New family can be created immediately, no infinite spinner. |
| EC-18 | **Data Resilience** | Family deleted on another device → automatic reset to setup. |
| EC-19 | **Multi-Device Claim Sync** | Profile claimed on Device A → Device B recognizes without refresh. |
| EC-20 | **Icon Scaling** | Icons and splash screen correct on xhdpi to xxxhdpi. |
| EC-21 | **Calc Limit** | Plan for >6 members capped at 6. |
| EC-22 | **Server Cleanup** | Families without updates for 180 days deleted on Sundays. New families (< 180 days old) are never deleted. |
| EC-23 | **Missing Alarm Permission** | Localized error in UI; no Toast, no crash. |
| EC-24 | **Touch Targets (A11y)** | At least 24dp. |
| EC-25 | **Offline Startup** | Loading screen transitions to dashboard within max. 2s. |
| EC-26 | **Offline Join** | Immediate error message; no infinite spinner. |
| EC-27 | **Captive Portal** | Offline status detected via `NET_CAPABILITY_VALIDATED`. |
| EC-28 | **Midnight Guard** | Early departure + long bathroom time → conflict instead of invalid times. |
| EC-29 | **Device-specific Alarm Toggle** | Alarm off on Device A → Device B unchanged. |
| EC-30 | **Localized Auth Errors** | Error in system language, not English. |
| EC-31 | **Alarm Status Sync** | User A disables alarm → Device B sees it immediately, own status unchanged. |
| EC-32 | **Delete Family with Other Users** | Admin deletes → all removed, family deleted. |
| EC-33 | **Offline Icon with Pending Writes** | After 3s offline → CloudOff icon instead of sync spinner. |
| EC-34 | **Leave Family (isolation)** | Papa leaves family on Device A → Mama's session on Device B remains unchanged; no unintended leaveFamily triggered. |
| EC-35 | **Member deletion on leave** | After leaving, the own member profile is completely deleted from Firestore. After rejoining, no old profile is present; a new one must be created. |
| EC-39 | **Best-effort Fallback Alarm** | Impossible plan -> Active user still receives an alarm at the best-effort time. |
| EC-40 | **Midnight Alarm (00:15)** | Set alarm to 00:15 -> Correctly scheduled and triggered (no midnight-guard blocking). |
| EC-36 | **Reboot Alarm Restore** | Alarms are correctly restored after reboot (Locked Boot) via Direct Boot. |
| EC-37 | **Terms of Use Link** | Terms of Use in Settings open correctly in a browser or webview. |
| EC-38 | **Registration Disclaimer Link** | Registration disclaimer links open correctly in a browser or webview. |
| TC-46 | **Open Feedback Screen** | Tapping "Give Feedback / Report Issue" opens the FeedbackScreen. |
| TC-47 | **Send Feedback (Firebase)** | Enter message → submit → success message appears, form is cleared, screen closes after 2.5s. |
| TC-48 | **Feedback without message** | "Send" button is disabled when message field is empty. |
| TC-49 | **Footer links open** | Tapping Terms of Use, Privacy Policy, or Imprint in the Settings footer each open the correct external page in a browser. |
| TC-50 | **Delete account link** | Tapping "Delete Account (Info)" opens the correct external page (language-aware: DE/EN) in a browser. |
| TC-51 | **Login (Validation)** | Try login with < 8 chars password or invalid email. | Error message appears immediately; no app crash. |
| TC-52 | **Field Validation (Family)** | Create a new family with an empty name. | "Create" button remains disabled (ViewModel validation). |
| TC-53 | **Field Validation (Join)** | Enter join code with < 6 characters. | "Join" button remains disabled; error message on invalid code. |
| TC-54 | **Validation (Member Name)** | Save member without a name. | Save button disabled; error message on empty inputs. |
| TC-93 | **Security (familyId Write Lock)** | Attempt to modify `users/{uid}/familyId` via prototype code. | Firestore Rules reject write (PERMISSION_DENIED). |
| TC-94 | **Admin Modal (Visibility)** | Regular user opens Help & Feedback card. | Admin button is not visible. |
| TC-95 | **Admin Modal (Functional)** | Global admin opens admin modal and taps 3min alarm. | Modal opens, alarm is triggered after 3 min as expected. |
| TC-96 | **Cloud Mapping (Join)** | Join family via code. | `familyId` appears in user profile (server-side written), even though client has no write access. |
| TC-97 | **Admin Deletion** | Global admin deletes any family. | Family and all members successfully removed. |
| TC-98 | **Localization (FR/ES/IT)** | Set app language to French/Spanish/Italian. | UI strings are correctly displayed in the selected language. |
| TC-99 | **Onboarding Image Fallback** | Start onboarding in FR/ES/IT. | English screenshots are displayed as fallback. |
| TC-100| **Join-Code Persistence** | Last user leaves the family. | Family persists in Firestore; code remains valid for new users. |

### 3. User Behavior
| ID | Test Case | Expected Result |
|:---|:---|:---|
| EC-08 | **Pause for Today** | ⏸️ icon removes member entirely from the plan; others may sleep in longer. |
| EC-13 | **Already Awake (☀️)** | Sun icon suppresses the alarm only; bathroom slot remains reserved for others. |
| EC-14 | **Awake – Cancel** | Clicking "Already Awake" cancels system alarm immediately | Alarm removed from Android AlarmManager. |
| EC-15 | **Awake – Feedback** | Button turns green and text to "You're awake ✅" | UI provides clear active state feedback. |
| EC-16 | **Awake – Reset** | Turn off global alarm switch | "Already Awake" status is reset to false. |
| EC-17 | **Awake – Visibility** | Toggle global alarm switch | Button shows/hides with smooth animation. |
| EC-18 | **RingingScreen** | Alarm goes off | Lottie Panda, gradient, and random greeting are shown. |
| TC-56 | **Admin: Statistics Report (Email)** | Click in Settings -> Toast appears -> Email with user/family data arrives. |
| TC-57 | **Admin: Security (Visibility)** | Login with non-admin user -> Admin buttons in Settings are hidden. |
| EC-09 | Late-night change (2 AM) | Plan is recalculated and synchronized asynchronously to all. |
| EC-10 | Multiple Admins | Both simultaneously change a child's bathroom duration -> Last-write-wins or conflict message. |

---

## 📱 UI/UX & Accessibility

- **Dark Mode:** Contrasts eye-friendly; AMOLED Black (`#000000`) for battery efficiency.
- **Material You:** Dynamic colors on Android 12+.
- **Haptics:** Different vibration patterns for pre-alarm and main alarm.
- **Real-time Feedback:** Short animation on schedule recalculation.

---

## 📈 Validation & Reporting

- **Automation:** Core logic (`Scheduler`) covered by JUnit tests in `app/src/test` (TC-07 to TC-09, EC-01, `NoActiveMembers`). All tests use type-safe `ScheduleMessage` codes.
- **Manual:** Live test over one night in a test family before every release.
