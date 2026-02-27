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
- [ ] **"Already Awake" Button (#2):** Disable alarm for today without disrupting others' plans.
- [ ] **"What's New" Dialog (#3):** Clear highlights shown after an app update.
- [ ] **Weekday configuration:** Set different wake times for weekdays vs. weekends – e.g. children sleep in on Saturdays.
- [ ] **Pause member for today:** An unclaimed member (or your own profile) can be removed from today's schedule (e.g. sick, sleeping elsewhere). Resets automatically at midnight. Other people's claimed profiles can never be paused by someone else. Add a pause icon next to Edit/Delete on member cards. Note: the master switch on the main screen already pauses your own claimed profile; unclaimed members always stay in the calculation but are never alarmed.

### Priority: Medium (Usability & UI)
- [ ] **Support for 2 Bathrooms:** Parallel slot calculation.
- [ ] **Snooze Sync:** If someone takes longer, the plan for others adjusts "live."
- [ ] **Individual Breakfast Duration:** Each member can set their own duration (e.g., kids eat for 30 mins, dad joins only for 10 mins).
- [ ] **Home Screen Widget:** Small 2×1 widget showing today's wake time for your own profile – without opening the app.
- [ ] **Wake confirmation with family push:** A "I'm awake!" button on the ringing screen sends a brief push notification to the other family members.
- [ ] **Vacation end date instead of manual toggle:** Enter vacation until date X; the alarm re-enables itself automatically afterwards.
- [ ] **Weekly schedule overview:** Compact table of all members × weekdays in one view.

### Priority: Low (Nice-to-have)
- [ ] **Evening Check-In:** Push reminder at 9 PM to confirm times for tomorrow.
- [ ] **Custom Playlists:** Spotify integration for the ringing screen.
- [ ] **Bathroom duration by weekday:** Dad needs more time on Fridays, kids on Mondays – configurable per weekday.

### Priority: Technical Improvements (Robustness)
- [ ] **UI feedback on Firestore errors:** `addOrUpdateMember()` is currently fire-and-forget — if a write fails, the user sees nothing. Add a Snackbar or retry dialog.
- [ ] **Scheduler upper limit:** Cap the number of members fed into the permutation algorithm (e.g., at 7) and use a greedy fallback above that threshold (currently n! permutations are theoretically unbounded, even though the scheduler is now async).
- [ ] **Ringtone preview in Settings:** Play the selected tone briefly after picking it, so the user hears what they chose before the next alarm fires.

---

## ✅ Completed
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
