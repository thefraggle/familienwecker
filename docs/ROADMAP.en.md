# 🗺️ FamWake Roadmap

*[🇩🇪 Deutsche Version](ROADMAP.md)*

---
## 🎯 Vision
Stress-free mornings – smart, dynamic planning for the whole family.

---

## ✅ Implemented in 1.1.5
- **Feedback Screen:** Dedicated feedback screen with category, message, optional email and device info.
- **Firebase Feedback:** Sent via Cloud Function (Resend); archived in Firestore.
- **Settings Restructured:** Language & Appearance merged; Help & Feedback in its own card.
- **Settings Footer:** Version, legal links (Terms of Use, Privacy Policy, Imprint) and copyright.
- **Delete Account:** External link instead of info dialog.
- **Bug Fixes 1.1.1–1.1.4:** Rate-limit fixes, scheduler corrections, ghost-claim fix, member deletion on leave.
- **String Audit:** Removed obsolete strings; DE/EN fully synchronized.

---

## ✅ Implemented in 1.0.0
- **Smart family wake-up schedule:** Automatic calculation of who gets woken up when.
- **Family invitations via link:** Deep-link join with conflict dialog.
- **Multi-device sync:** Real-time synchronization of all changes.
- **Profile claiming:** Each family member can claim their profile.
- **Offline handling:** Offline icon, offline guards for claim and join.
- **Admin protection:** Family deletion restricted to the creator.
- **Code / Security audit:** HTTP guard, rate-limiting, encrypted preferences.

---

## 🛠️ Backlog / Open Ideas

### Priority: High (Integration & Core Features)
- [ ] **Weekday configuration:** Separate wake times for weekdays and weekends.

### Priority: Medium (Usability & UI)
- [ ] **Snooze sync:** Others' plans adjust "live" when someone needs more time.
- [ ] **2-bathroom support:** Parallel slot calculation.
- [ ] **Individual breakfast duration:** Each member can set their own times.
- [ ] **Haptic profiles:** Different vibration patterns for pre-alarm and main alarm.
- [ ] **Smart time warnings:** Alert when time windows are too tight.
- [ ] **Input validation:** Warning for unrealistic values (e.g. 2h bathroom time).
- [ ] **Home screen widget:** See your wake time without opening the app.
- [ ] **Wake confirmation push:** "I'm awake!" button sends a push to everyone.
- [ ] **Vacation date:** Alarm re-activates automatically after the set date.
- [ ] **Weekly plan overview:** Compact table of all members × weekdays.

### Priority: Low (Nice-to-have)
- [ ] **Evening check-in:** Push reminder at 9 PM to confirm tomorrow's times.
- [ ] **Custom playlists:** Spotify integration for the wake-up screen.
- [ ] **Bathroom duration per weekday:** Configurable per day.
- [ ] **DST protection:** Guard schedule calculations against daylight saving time changes.
- [ ] **Alarm watchdog:** Protect background service against system kills.

---
