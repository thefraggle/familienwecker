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
| EC-01 | **Impossible Plan** (Everyone wants the bathroom at the same time) | App shows "Conflict found" warning and offers compromise suggestions (e.g., shorten breakfast). |
| EC-02 | **Extreme Bathroom Duration** (Member with 120 min) | The plan shifts other members significantly; warning for unrealistic inputs shown if applicable. |
| EC-03 | **Short Time Windows** (Wake up 7:00, leave home 7:05) | App warns about tight time management. |

### 2. Technical Edge Cases
| ID | Test Case | Expected Result |
|:---|:---|:---|
| EC-04 | **Offline Calculation** | The last valid plan remains stored locally. Alarm rings even without internet. |
| EC-05 | **Daylight Saving Time** (Summer/Winter) | Wake-up times are correctly adjusted to the new time, no duplicate alarms. |
| EC-06 | **App Crash during Alarm** | Alarm service restarts automatically and continues the wake-up process. |
| EC-07 | **Battery Optimization (Android)** | App is marked as "Not optimized" so the background service wakes reliably. |
| EC-11 | **Snooze Function** | Clicking Snooze (5 min) on the ringing screen schedules a new alarm exactly 5 min later. |
| EC-12 | **Midnight Reset** | "Pause for Today" and "Already Awake" statuses are automatically reset the next day. |
| EC-14 | **Persistence & Logout** | No old login data or family IDs remain after logout or re-installation (Auto-Backup disabled). |
| EC-15 | **Delete Family (Safety)** | Double confirmation required if other members exist; single confirmation for "just me" or empty lists. |
| EC-16 | **Delete Member (Confirmation)** | Yes/No dialog appears before deleting a member. |
| EC-17 | **New Setup after Deletion** | A new family can be created immediately after deletion without hanging (Infinity Loading test). |
| EC-18 | **Data Resilience (Dashboard)** | If the family was deleted on another device, an automatic reset to setup occurs (Self-Healing). |
| EC-19 | **Multi-Device Claim Sync** | If a profile is claimed on Device A, Device B (same UID) recognizes this automatically without refresh. |
| EC-20 | **Resource Health** | Icons and Splash screen are rendered correctly across various pixel densities (xhdpi to xxxhdpi) without distortion. |
| EC-21 | **Max. Calculation Limit (OOM)** | Attempting to calculate a plan for >6 active members is limited to 6 for crash prevention. |
| EC-22 | **Garbage Collection (Server)** | Families without updates in the last 180 days are deleted by Cloud Functions on Sundays. |
| EC-23 | **Missing Alarm Permission (UI)** | If `SCHEDULE_EXACT_ALARM` is missing, a localized error message appears in the UI (no Toast, no crash). |
| EC-24 | **Accessibility (A11y)** | Touch targets (e.g., in Settings) are at least 24dp large for reliable usability. |
| EC-25 | **Offline Startup Timeout** | Launch app in Airplane mode. Loading screen must transition to Dashboard within max. 2s (given local data exists). |
| EC-26 | **Offline Join Error** | Attempting to join (code/link) in Airplane mode shows a brief spinner in the button and then an immediate error message; dialog closes. |
| EC-27 | **Captive Portal Detection** | Wi-Fi with a login page (no real internet access). | App must detect offline status (via `NET_CAPABILITY_VALIDATED`). |
| EC-28 | **Midnight Schedule Guard** | Departure extremely early + long bathroom duration (calc before 03:00). | Scheduler must report a conflict instead of generating invalid times. |

### 3. User Behavior
| ID | Test Case | Expected Result |
|:---|:---|:---|
| EC-08 | **Pause for Today** | ⏸️ icon removes member entirely from the plan; others may sleep in longer. |
| EC-13 | **Already Awake (☀️)** | Sun icon suppresses the alarm only; bathroom slot remains reserved for others. |
| EC-09 | Late-night change (2 AM) | Plan is recalculated and synchronized asynchronously to all. |
| EC-10 | Multiple Admins | Both simultaneously change a child's bathroom duration -> Last-write-wins or conflict message. |

---

## 📱 UI/UX & Accessibility

- **Dark Mode:** All contrasts must be eye-friendly in the dark theme (for night/morning use). The background now uses a true AMOLED Black (`#000000`) for improved battery efficiency.
- **Material You:** Dynamic application colors based on the system wallpaper (Android 12+).
- **Haptics:** Vibration patterns differ between "Pre-alarm" and "Main alarm."
- **Real-time Feedback:** When the plan is recalculated, the user sees a short animation/confirmation.

---

## 📈 Validation & Reporting

- **Automation:** The core logic (`Scheduler`) is covered by JUnit tests (found in `app/src/test`) with scenarios from TC-07 to TC-09, EC-01, and the new `NoActiveMembers` test. All tests now check type-safe `ScheduleMessage` codes instead of raw strings.
- **Manual Verification:** A "Live Test" over one night in a test family occurs before every release.
