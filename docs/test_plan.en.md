# 🧪 Test Plan - FamWake (Family Alarm)

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
| TC-31 | **Encryption (H-5)** | After update: Data (familyId, joinCode) is automatically migrated to EncryptedPrefs; legacy file is cleared. |
| TC-32 | **Rate-Limiting (H-1)** | Repeated wrong code entries (>5/min) trigger server-side blocking ("Resource Exhausted"). |
| TC-33 | **Email Rate-Limiting** | More than 3 password-reset or verification emails for the same address within one hour are blocked server-side ("Resource Exhausted"). |
| TC-34 | **HTTP link rejected** | Calling `http://familienwecker.de/join/CODE` must not trigger a join – app ignores the HTTP scheme. |
| TC-35 | **Admin-only delete** | Non-creator opens Settings → delete family → receives error message instead of confirmation dialog. |
| TC-36 | **Offline profile claim blocked** | Open dropdown in airplane mode → Snackbar with offline error appears immediately. |
| TC-37 | **Deep link instant dialog (background)** | App is on Settings screen, open join link → Conflict dialog appears immediately without back navigation. |
| TC-38 | **Battery card instant reset** | Disable battery optimization, return to app → card disappears immediately without screen navigation. |


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
| EC-06 | **App Crash during Alarm** | Alarm service restarts automatically. |
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
| EC-22 | **Server Cleanup** | Families without updates for 180 days deleted on Sundays. |
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

### 3. User Behavior
| ID | Test Case | Expected Result |
|:---|:---|:---|
| EC-08 | **Pause for Today** | ⏸️ icon removes member entirely from the plan; others may sleep in longer. |
| EC-13 | **Already Awake (☀️)** | Sun icon suppresses the alarm only; bathroom slot remains reserved for others. |
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
