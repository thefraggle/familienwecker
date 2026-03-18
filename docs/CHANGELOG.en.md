# Changelog

This format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

*[🇩🇪 Deutsche Version](CHANGELOG.md)*


## [1.3.2] - 2026-03-18

### Added
- **All weekdays can be deactivated:** A member can now disable all weekdays (no alarm) without the save button being blocked.
- **Next active day in member tile:** When not all days are active, the member tile shows the next active day (e.g. "Friday") and its day-specific wake times.
- **Alarm date in schedule card:** When the next alarm is not today, the schedule card shows a subtitle with weekday and date (e.g. "Thursday, 19 March").
- **Periodic refresh:** `recalculateSchedule` is called automatically every 5 minutes – schedule display no longer freezes when no Firestore update arrives.

### Fixed
- **Alarm not ringing (root cause):** `AlarmClockInfo` received a `getBroadcast` PendingIntent as the show intent instead of `getActivity`. On some Android versions this prevented `AlarmReceiver` from ever being called.
- **`FLAG_UPDATE_CURRENT` + `FLAG_IMMUTABLE` conflict:** Replaced with `FLAG_CANCEL_CURRENT` for clean PendingIntent recreation.
- **Race condition – Firebase sync after alarm time:** A Firestore update shortly after the wake time could trigger `recalculateSchedule` → `cancelWakeUp`. Fix: 5-minute grace period in `applyAlarms`.
- **Silent cancel in `recalculateSchedule`:** All alarms were silently cancelled when `now > todayProfile.latestWakeUp`. Fix: grace period also in "all paused" branch + W-level logs for all cancel paths.
- **Stale schedule after inactive next day:** UI kept showing old schedule when `applyAlarms` cancelled due to an inactive day. Fix: `_schedule` is now set to `NoActiveSchedule`.
- **Race condition – second ViewModel instance:** `RingingActivity` created a second `FamilyViewModel` that overwrote the running alarm. Fix: direct use of `PreferencesRepository` + `AlarmScheduler`.
- **`RingingActivity` not reliably launched:** `AlarmReceiver` now starts `RingingActivity` directly via `context.startActivity()` (in addition to the full-screen intent).
- **Member tile shows wrong alarm status:** "Alarm active" shown despite all days inactive. Fix: `allDaysInactive` check in the tile.
- **Wake time details visible when profile inactive:** Now hidden when all days are inactive.
- **Snooze slot conflict:** Snooze and regular alarms now use separate request codes (`_snooze` suffix).

---

## [1.3.1] - 2026-03-17

### Changed
- **Weekday chips:** All 7 chips (`Mo Tu We Th Fr Sa Su`) now use `weight(1f)` and distribute evenly across the full width – Sunday was previously cut off on narrow screens.
- **Chip error highlight:** Chips with invalid time settings are highlighted in red (border, text, and background).

### Fixed
- **Next-alarm logic:** `resolveEffectiveMember` now checks today's DayProfile first (is it active AND before `latestWakeUp`?), otherwise falls back to tomorrow. Previously, the legacy root field `member.latestWakeUp` was used as reference → wrong result when today's profile was inactive.
- **"No alarm" shown incorrectly:** If today's profile was disabled but tomorrow's is active, the app now correctly shows tomorrow's alarm instead of "no active alarm".
- **Alarm rings for disabled days:** `applyAlarms` now cancels the alarm if the DayProfile for the target date has `isActive = false`.
- **Validation – latest wake time:** Error message shown when `latestWakeUp ≤ earliestWakeUp`; save button is disabled.
- **Validation – leave home time:** Error message shown when leave time ≤ `latestWakeUp + bathroom duration`. Also validates against the displayed default value (08:00), not just explicitly set values.

---

## [1.3.0] - 2026-03-17

### Added
- **⭐ Rate app:** New button in the Help & Feedback section. Opens the in-app review dialog (Play In-App Review API); falls back to the Play Store page if unavailable.

### Changed
- **Weekday chips:** DE and EN now use uniform 2-letter abbreviations (Mo Di Mi Do Fr Sa So / Mo Tu We Th Fr Sa Su) so all 7 chips fit in the available width.
- **Inactive days:** Chips for disabled weekdays are now clearly dimmed (text, border and background at ~30% opacity).
- **Settings footer:** New order: Version → Copyright → All rights reserved → Links.
- **Rate limits (Cloud Functions):**
  - Email Reset/Verify: max. 5 per hour + max. 10 per day
  - Join family: max. 5 per minute + max. 10 per day
  - Create family: max. 3 per hour + max. 6 per day

### Fixed
- **Chip text invisible:** Selecting an inactive weekday chip showed unreadable text on the filled background. Fix: muted grey container instead of primary color.
- **Crash on "create family" rate limit:** `RESOURCE_EXHAUSTED` exception from the Cloud Function is now correctly caught.
- **Rate-limit errors fully covered:** All three rate limits (create, join, email) now show specific error messages. `resendVerificationEmail` result is now evaluated.

---

## [1.2.0] - 2026-03-17

### Added
- **Alarm state persistence:** After reinstall + login, the last known alarm state (on/off) is automatically restored from Firestore.

### Fixed
- **Join flow double dialog:** When the join link was for the user's own family, `onLeaveFamily()` was called, incorrectly redirecting to the setup screen. Fix: same-family guard now returns `false`.
- **Create family – loop + crash (new account):** `refreshData()` called `leaveFamily()` when Firestore returned `null` briefly after `createFamily()` (propagation race condition). This destroyed the newly created family and crashed the app on the second attempt. Fix: removed `leaveFamily()` from `refreshData()` – self-healing runs via the members-flow collector.
- **Create family – redirect to setup:** `LaunchedEffect` in `MainScreen` called `onLeaveFamily()` when `familyId` briefly appeared as `null` during an active sync. Fix: guard `familyId == null && !isSyncing`.
- **Alarm restore after reinstall (multiple race conditions):**
  - `myMemberId` observer called `setAlarmEnabled(false)` on app start (initial `null`).
  - `initialAlarmPushDone` block wrote `false` to Firestore before restore could run.
  - `isAlarmEnabled` observer wrote `false` to Firestore on logout, overwriting the member's alarm state.
  - All three race conditions fixed; order: restore first, then proactive Firestore sync.
- **Breakfast time incorrect:** When no departure time was set, 23:59 was incorrectly used as the base → unrealistic times (e.g. 23:29). Fallback is now `latest wake time + bathroom duration`.
- **Weekday configuration ignored:** Day-specific times from day profiles were ignored when calculating the schedule. Fix: effective fields are now correctly resolved before the scheduler is called.

### Removed
- **"What's new" dialog** completely removed (dialog, logic, JSON file, strings).

---

## [1.1.5] - 2026-03-17

### Added
- **Weekday configuration:** Wake times and bathroom duration can be set individually per weekday.
- **Feedback screen:** Dedicated feedback screen with category selection, message, optional email, and automatically included device data.
- **Firebase feedback sending:** Feedback is sent directly via a Firebase Cloud Function (Resend) as an email – no more classic mail client intent. Every submission is also archived in Firestore.
- **Feedback UX:** Form is cleared after submission; screen closes automatically after 2.5 seconds.
- **Settings footer:** Version number, clickable legal links (terms of use, privacy, imprint) and copyright now visible as a footer.
- **Delete account:** External link to `familienwecker.de/account-deletion.html` (DE) or `/account-deletion-en.html` (EN) instead of an info dialog.
- **Settings restructured:** Language and appearance combined in one card; Help & Feedback in its own card.
- Terms of Use linked directly in settings.
- Disclaimer on the registration screen with clickable links to terms of use and privacy policy.

### Fixed
- Algorithm: breakfast conflict now detected when bathroom end = breakfast start (0 min buffer).
- Algorithm: post-validation ensures no breakfast member finishes their bathroom after breakfast starts.
- Offline display: incorrect "offline" icon after app start fixed (only shown on real network failure).
- Cloud Functions: rate limit counter not saved correctly on first call (tx.update → tx.set).
- Cloud Functions: join attempt limit increased from 5 to 10 per minute.
- CI: AAB filename in manual GitHub builds was incorrect.
- When leaving a family, the member's profile is now fully deleted from Firestore.
- Email sending (password reset, opt-in confirmation) was blocked on first request due to incorrect rate limit document.
- "Leave family" incorrectly disconnected other devices (self-healing without existence check).
- After leaving, the account claim remained in the Firestore profile (ghost claim).
- String audit: obsolete and unused strings removed; both languages fully synchronized.

---

## [1.1.0] - 2026-03-15

### Added
- Scroll indicator (↓) on the main screen while no members are present.
- **Snooze:** Snooze button on the alarm screen (5 min). A banner with end time and cancel button is shown on the main screen during an active snooze.

### Fixed
- Alarm rings after device reboot (even before PIN entry).
- Alarm screen on lock screen (Samsung, Xiaomi, etc.).
- Google OAuth in self-signed APKs.
- Firebase cleanup job incorrectly deleted new families.
- Battery tile disappears immediately after confirmation.

---

## [1.0.0] - 2026-03-12

### Security
- HTTP invitation links are rejected – only HTTPS allowed.
- Only the family creator can delete the family; others receive an error message.
- Profile selection blocked offline – prevents misleading timeout errors.

### Fixed
- Join link opens the conflict dialog immediately, even when the app is in the background.
- Deleting a family now works even when other users have active profiles.
- Member limit at 6 instead of 5 active persons.
- Direct joining by other users without conflict dialog fixed.
- Family name now displays correctly in the header after restart.
- Optimized loading: no more flickering on app start.
- Complete localization (DE/EN) of all new strings.

---
