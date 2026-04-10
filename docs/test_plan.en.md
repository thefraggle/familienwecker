# 🧪 Test Plan: FamWake
**Version:** 1.7.4
**Date:** 2026-04-10
*[🇩🇪 Deutsche Version](test_plan.md)*

---

## 📋 Strategy

Tests validate not only the UI but especially the correctness of the scheduling algorithm.

**Areas:** Onboarding & Account · Family Management · Scheduling Logic · Alarm · Edge Cases

---

## 🛠 Normal Operation

### 1. Account & Onboarding
| ID | Test Case | Expected Result |
|:---|:---|:---|
| TC-01 | First-time registration (email) | Account created, confirmation email sent. |
| TC-02 | Create family | User becomes admin; invite code generated. |
| TC-03 | Join family (code & deep link) | Joining via code and `/join/` link works; app opens instead of browser. |
| TC-04 | Self-join guard | Entering own code does not trigger a new join; profile assignment is preserved. |
| TC-05 | Join code security | 6-character alphanumeric (no 0/O/1/I) via SecureRandom. Invalid code → error message; old family retained. |
| TC-06 | Password reset | Email is sent. Unknown email: always the same success message (User Enumeration Prevention). |
| TC-07 | Autofill & password manager | Email field suggests saved addresses; password manager offers autofill. |
| TC-08 | Login validation | Short password / invalid email → immediate error message, no crash. |
| TC-09 | Leave / delete family | Own profile deleted. Admin-only delete: non-creator sees error message. Empty family: single confirmation. |
| TC-10 | Multi-account isolation | Logout user A, login B → B sees only their family. |
| TC-11 | Onboarding tour | First launch shows 5 slides with panda. Tour restartable from settings. Finish lands correctly on main screen. |
| TC-12 | Rate limiting | >5 wrong codes/min or >5 reset emails/h → server-side block ("Resource Exhausted"). |

### 2. Members & Family Configuration
| ID | Test Case | Expected Result |
|:---|:---|:---|
| TC-20 | Auto-claim first profile | First member auto-claimed, alarm enabled, no "no profile" flash. |
| TC-21 | Edit profile (2nd user) | Second member can create, edit, and save profile – no sync error. |
| TC-22 | Tile status after auto-claim | Tile shows correct "alarm active/inactive" after auto-claim – no permanent missing badge. |
| TC-23 | Permissions for other profiles | Claimed profiles of others: no edit icon, no tap response. |
| TC-24 | Member limit | At 6 members, "Add" is disabled. |
| TC-25 | Pause | Pause unclaimed member → correctly paused, no error. Own profile: no pause button visible. |
| TC-26 | Weekday validation | latestWakeUp ≤ earliestWakeUp or departure too early → error text, save blocked. |
| TC-27 | Drag & drop order | Long-press → drag with gap preview. Order persists after restart and on other devices. |
| TC-28 | Next active day | Today disabled, tomorrow active → main screen shows tomorrow's alarm. No active day → NoActiveSchedule. |
| TC-29 | Time picker (edit profile) | Tapping a time opens a keyboard-style dialog (no clock). Tapping outside the name field dismisses the keyboard. Bathroom buttons (−/+) always visible. |

### 3. Alarm
| ID | Test Case | Expected Result |
|:---|:---|:---|
| TC-30 | Alarm rings (background) | App in background → alarm rings, RingingActivity opens. |
| TC-31 | Alarm after reboot | Alarm correctly restored after reboot (even before PIN entry). |
| TC-32 | Alarm after disable + re-enable | Alarm off → alarm on → alarm rings at scheduled time. |
| TC-33 | "I'm awake" button – time window | Alarm tomorrow 07:00, current time 15:00 → button inactive. At 05:30 → active (2h window). |
| TC-34 | "I'm awake" button after profile re-creation | Delete member, create new → button correctly active/inactive based on new profile. |
| TC-35 | "I'm awake" – reset always possible | Awake state → reset always possible (button stays clickable). |
| TC-36 | "I'm awake" – toggle effect | Button cancels system alarm immediately; color/icon changes. Global switch off → status reset. |
| TC-37 | "I'm awake" – day change | App in background overnight → button shows initial state the next morning. |
| TC-38 | Snooze | Snooze (5 min) schedules new alarm exactly 5 min later. Banner with end time + cancel. No conflict with regular alarm. |
| TC-39 | Alarm status sync | Alarm off on device A → device B sees status immediately (own status unaffected). |
| TC-40 | Alarm permission warning (Android 14+) | No SCHEDULE_EXACT_ALARM → red tile on main screen, clickable to system settings. |
| TC-41 | Global alarm ON after restart | Enable alarm → restart app → alarm stays ON (no race-condition reset). |

### 4. Security & Admin
| ID | Test Case | Expected Result |
|:---|:---|:---|
| TC-50 | Firestore member protection | Attempt to write name/claimedByUserId of another member → PERMISSION_DENIED. |
| TC-51 | Admin visibility | Non-admin sees no admin buttons. Global admin sees admin modal; 2-min alarm triggerable. |
| TC-52 | Feedback requires auth | Feedback via API without auth → unauthenticated error, no Firestore entry. |
| TC-53 | Member deletion protection | Attempting to delete another user's profile → permission-denied. |
| TC-54 | XSS in feedback | Send `<script>alert(1)</script>` → email shows code as plain text, no execution. |

### 5. Localization
| ID | Test Case | Expected Result |
|:---|:---|:---|
| TC-60 | Language fallback (unknown code) | Unknown or invalid language code → app falls back to English, not German. Also applies when loading from storage. |
| TC-61 | Dialect languages | Swabian / Swiss German / Ruhr Valley → correct texts, no crash. |
| TC-62 | Additional languages | DA/NO/FR/ES/IT/SV/TR/UK/RU/NL/PL/PT → UI texts correct, no DE fallbacks. |

---

## ⚠️ Edge Cases

### Conflict Scenarios
| ID | Test Case | Expected Result |
|:---|:---|:---|
| EC-01 | Impossible plan | Everyone wants the bathroom at the same time → conflict warning + compromise suggestion. |
| EC-02 | Best-effort fallback | Impossible plan → active user still receives an alarm at the best possible time. |
| EC-03 | Tight time window | Wake 07:00, leave 07:05 → app warns about insufficient time. |

### Technical Edge Cases
| ID | Test Case | Expected Result |
|:---|:---|:---|
| EC-10 | Offline calculation | Last plan cached locally; alarm rings without internet. |
| EC-11 | Offline indicator | Disconnect → CloudOff icon after >3s. Reconnect → icon gone immediately. |
| EC-12 | Offline profile claim | In airplane mode → snackbar immediately, no spinner. |
| EC-13 | Daylight saving time | Times adjusted correctly, no duplicate alarms. |
| EC-14 | Midnight alarm (00:15) | Correctly scheduled and triggered, no midnight guard block. |
| EC-15 | Multi-device claim sync | Profile claimed on device A → device B recognizes it without refresh. |
| EC-16 | No stale schedule after family switch | No old family members or schedule briefly visible. |
| EC-17 | Server cleanup | Families >180 days without update deleted on Sundays. New families never deleted. |

### User Behavior
| ID | Test Case | Expected Result |
|:---|:---|:---|
| EC-20 | Pause today | ⏸️ removes member from plan; others may sleep longer. |
| EC-21 | Midnight reset | "I'm awake" and "Pause today" reset at the start of the next day. |
| EC-22 | Reboot alarm (Direct Boot) | Alarms restored after reboot even before PIN entry. |
| EC-23 | Firestore self-healing | Airplane mode on/off → listener catches errors, re-syncs silently. |

---

## 📱 UI/UX & Accessibility

- **Dark Mode:** Eye-friendly contrasts; AMOLED Black (`#000000`).
- **Material You:** Dynamic colors from Android 12.
- **Touch Targets:** Minimum 24 dp for all interactive elements.
- **Haptics:** Vibration patterns for pre-alarm and main alarm.

| ID | Test Case | Expected Result |
|:---|:---|:---|
| UI-01 | Theme switcher | Sun/Auto/Moon icons switch immediately; no overflow on narrow devices. |
| UI-02 | Schedule copy link | After opening the day schedule, the "Copy to other days" link is visible without scrolling. |
| UI-03 | Small screen scrolling | On 360dp device: FamilySetupScreen, SettingsScreen, AddMemberScreen – keyboard pushes content up, button stays reachable. |
| UI-04 | Field order | Weekday card: earliest wake, latest wake, bathroom, leave home, breakfast. |
| UI-05 | Scroll hint | Open Settings/AddMember → bouncing arrow visible. After scrolling to the end: arrow disappears. |
| UI-06 | Collapsing title | Main screen: title large on open. Scrolling down collapses it cleanly into the compact top bar. |

---

## 📈 Validation

- **Automated:** JUnit tests for `Scheduler` in `app/src/test` (TC-07–TC-09, EC-01, NoActiveMembers, type-safe `ScheduleMessage` codes).
- **Manual:** A live overnight test in a test family before every release.
