# 🗺️ FamWake Roadmap & Idea Backlog

This document serves as the central hub for everything we want to improve or build for FamWake.

*[🇩🇪 Deutsche Version](ROADMAP.md)*

---

## 🎯 Vision
Stress-free morning routines – through intelligent, dynamic planning for the whole family.

---

## 🛠️ Backlog / Open Ideas

### Priority: High (Integration & Core Features)
- [ ] **Custom Ringtone (#1):** Selection of an individual alarm sound from local device files.
- [ ] **"What's New" Dialog (#3):** Clear highlights shown after an app update.
- [ ] **Weekday configuration:** Set different wake times for weekdays vs. weekends – e.g. children sleep in on Saturdays.

### Priority: Medium (Usability & UI)
- [ ] **Support for 2 Bathrooms:** Parallel slot calculation.
- [ ] **Snooze Sync:** If someone takes longer, the plan for others adjusts "live."
- [ ] **Individual Breakfast Duration:** Each member can set their own duration (e.g., kids eat for 30 mins, dad joins only for 10 mins).
- [ ] **Real-time Feedback:** Visual confirmation/animation when the plan has been recalculated in the background (Testplan UI).
- [ ] **Haptic Feedback Profiles:** Different vibration patterns for pre-alarm and main alarm (Testplan UX).
- [ ] **Smart Time Warnings:** Alert if time windows are too tight (e.g., wake up vs. leave home) (EC-03).
- [ ] **Smart Conflict Proposals:** UI suggestions to resolve bathroom bottlenecks (e.g., "Shorten breakfast by 5 mins?") (EC-01).
- [ ] **Input Validation (Extreme):** Warning for unrealistic values (e.g., 2h bathroom duration) (EC-02).
- [ ] **Home Screen Widget:** Small 2×1 widget showing today's wake time for your own profile – without opening the app.
- [ ] **Wake confirmation with family push:** A "I'm awake!" button on the ringing screen sends a brief push notification to the other family members.
- [ ] **Vacation end date instead of manual toggle:** Enter vacation until date X; the alarm re-enables itself automatically afterwards.
- [ ] **Weekly schedule overview:** Compact table of all members × weekdays in one view.

### Priority: Low (Nice-to-have)
- [ ] **Evening Check-In:** Push reminder at 9 PM to confirm times for tomorrow.
- [ ] **Custom Playlists:** Spotify integration for the ringing screen.
- [ ] **Bathroom duration by weekday:** Dad needs more time on Fridays, kids on Mondays – configurable per weekday.

- [ ] **Daylight Saving Time (DST) Support:** Ensure calculations are robust against summer/winter time shifts (EC-05).
- [ ] **Alarm Watchdog:** Enhance background service resilience against system kills or crashes (EC-06).
- [ ] **Deep Offline Resilience:** Explicit local database as primary source when internet is unavailable (EC-04).
- [ ] **Multi-Admin Conflict Resolution:** Strategy for simultaneous edits to the same member profiles (EC-10).

---

## ✅ Completed
- [x] "Already Awake" Button (☀️) (V 0.3.6)
- [x] Pause for Today (⏸️) & Automatic Midnight Reset (V 0.3.6)
- [x] Snooze Functionality (5 Min) (V 0.3.6)
- [x] Member limit (max 6) and robustness against large family sizes (V 0.3.6)
- [x] Scheduler: `LocalTime.MAX` sentinel guard & 04:00 AM lower bound (V 0.3.6)
- [x] Robust Logout, Deletion & Auto-Backup disablement (V 0.3.6)
- [x] Scheduler Diagnostics: Precise error messages on conflicts (V 0.3.5)
- [x] Battery Optimization: System check & warning card (V 0.3.5)
- [x] Member order: stable sort by creation timestamp (V 0.3.4)
- [x] Member creation failing after family setup (saveUserFamily race condition) (V 0.3.4)
- [x] Phantom alarm on logout/leaveFamily/deleteFamily (V 0.3.4)
- [x] isPaused/claim lost when editing a member (V 0.3.4)
- [x] Bathroom duration validation (1–120 min) (V 0.3.4)
- [x] Error flash on family creation fixed (V 0.3.4)
- [x] Alarm sound: selected ringtone is now played correctly (Notification Channel V2, USAGE_ALARM attributes) (V 0.3.3)
- [x] Leave-home-time validation: error shown if time is before wake time (V 0.3.3)
- [x] Scheduler moved to background thread (V 0.3.3)
- [x] Modern App Icon (V 0.3.2)
- [x] Optimized Dark Mode & Contrast (V 0.3.2)
- [x] Extended Deletion Logic & Firestore Security Rules (V 0.3.2)
- [x] Profile Ownership, Automatic Restoration & Security (#5) (V 0.3.1)
- [x] Robust Deletion Protection & Navigation Sync (V 0.3.1)
- [x] Color-coded member alarm statuses (V 0.3.1)
- [x] Display of family name in settings (#4) (V 0.3.0)
- [x] Smart algorithm for bathroom conflicts (V 0.2.5)
- [x] Flexible breakfast (V 0.2.6)
- [x] Cloud Sync & Vacation Mode (V 0.2.5)
- [x] Multi-language support (DE/EN) (V 0.2.5)

---
*Suggestions can be added at any time! Just edit this file.*
