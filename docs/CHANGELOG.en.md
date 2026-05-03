# Changelog

*[🇩🇪 Deutsche Version](CHANGELOG.md)*

## 1.8.7 – 2026-05-03
### New
- **Start instantly** – The app can now be used without an account (Lazy Registration). You can instantly create a family and use the alarm clock. An account is only required to share codes or join another family.

---

## 1.8.6 – 2026-05-03
### Improved
- **Smoother onboarding** – FamWake no longer asks for lock screen permissions right at launch. Instead, you'll see a gentle reminder only when you actually set up an alarm.
- **Better readability** – We improved the color contrast of error messages and warnings, making them much easier to read on all screens.

---

## 1.8.5 – 2026-05-01
### New
- **Globally available** – FamWake now speaks 7 new languages: Indonesian, Vietnamese, Bengali, Marathi, Hindi, Simplified Chinese, and Korean. We now support 22 languages for families worldwide.
- **Localized emails** – System emails (such as password reset requests) are now sent in the language of your app.

---

## 1.8.4 – 2026-04-28

### Improved
- **More reliable schedule calculation** – Edge cases with very early breakfast times are now handled correctly.
- **Translations completed** – Notification settings are now correctly labelled in all 15 app languages.

---

## 1.8.3 – 2026-04-28

### Fixed
- **Duplicate alarm with different weekday times** – If e.g. Monday/Tuesday were set to 6:30 and the rest of the week to 7:30, the alarm fired again at 7:30 on Tuesday after the first 6:30 alarm. Fixed.

---

## 1.8.2 – 2026-04-25


### Fixed
- **Language switching works reliably again** – Switching to Danish, Japanese, Dutch, Polish, Turkish, and other languages had no effect on some devices (app stayed in English). Fixed.

---

## 1.8.1 – 2026-04-24

### Fixed
- **Alarm switch state preserved** – On/off state is correctly retained after signing out and back in.
- **Family switch** – When switching to a new family via invitation link, the alarm is safely turned off until a profile is selected.
- **Password reset** – Reset emails are sent reliably again for all accounts.
- **Different account on same device** – Signing in with a different account correctly resets the alarm state to match that account.

---

## 1.8.0 – 2026-04-21

### New
- **Push notifications 🔔** – Get notified automatically when the family plan changes – e.g. reorder, alarm on/off, or pause. All family members are notified.
- **Push toggle** – Push notifications can be enabled or disabled in the app settings.
- **Family events** – Instantly know when someone joins or leaves your family.
- **Smart review prompts** – The app gently asks for a rating after a positive experience.
- **20 wake-up messages** – More variety on the alarm screen.

### Improved
- **Live sync** – Toggling the alarm on or off now updates the schedule instantly on all devices.
- **More reliable alarm** – Improved lock screen behavior, clearer warning for missing permissions.
- **Fresh install** – The alarm is automatically active after reinstallation if a profile already exists.
- **Silent push notifications** – Info notifications are silent and non-disruptive.
- **Onboarding refreshed** – Clearer introduction, country-specific examples.
- **Fresher design** – Softer corners, scroll hint, collapsible title.
- **Better time picker** – Clean keyboard-style dialog instead of a clock face.
- **Reorder warning** – A notice appears when a member without their own profile is first in the schedule.

### Fixed
- Alarm screen appears over the lock screen without requiring PIN entry.
- Various stability, sync, and reliability improvements.

---

## 1.7.0 – 2026-04-06

### New
- **Permission warning** – A red tile appears if alarm permissions are missing.
- **Auto-claim profile** – Your first member profile is automatically linked to your account.
- **Weekday scheduling** – Wake times and bathroom duration can be set individually per day.

### Fixed
- Alarm rings reliably after being toggled off and on again.
- "I'm awake" button no longer stays grayed out after re-creating a profile.
- Second family member can now reliably edit their profile.
- "Already awake" status resets correctly the next morning.
- Login screen usable at large system font sizes (125%+).
- Various sync and stability improvements.

---

## 1.6.0 – 2026-03-23

### New
- **Invitation links** – Better feedback when joining via a link.

### Fixed
- Invitation codes stay valid even if a family is temporarily empty.
- Fixed time calculations for midnight and very early alarm times.

---

## 1.5.0 – 2026-03-21

### New
- **Onboarding** – New introduction tour with panda animations.
- **New app icon** – Modern design, dark mode as default.
- **Feedback** – Send feedback directly from within the app.

### Fixed
- More reliable joining and leaving of families.
- Fixed member display issues and duplicate alarm sounds.

---

## 1.4.0 – 2026-03-19

### New
- **Onboarding tour** – 5-screen introduction with panda animations and redesigned alarm screen.

### Improved
- **Autofill & login** – Much better password manager compatibility.
- **Security** – XSS protection for feedback emails, cloud reset for status resets.

### Fixed
- Double alarms, deep link flows, and member mapping corrected.

---

## 1.3.0 – 2026-03-17

### New
- **⭐ Rate the app** – Rate directly in the Play Store from within the app.

### Improved
- Disabled days are now more clearly highlighted.
- Anti-spam protection for emails and family invites.

---

## 1.2.0 – 2026-03-17

### New
- **Feedback screen** – Send feedback directly from within the app.
- **Weekday configuration** – Individual times per weekday.

### Fixed
- Alarm state restored after reinstallation.
- Breakfast time calculation and family creation stabilized.

---

## 1.1.0 – 2026-03-15

### New
- **Snooze** – 5-minute snooze with banner and cancel button.

### Fixed
- Alarm rings after device reboot (even before PIN entry).
- Alarm screen on lock screen (Samsung, Xiaomi, etc.).

---

## 1.0.0 – 2026-03-12

### First Release 🎉
- Family alarm clock with coordinated bathroom scheduling.
- Invite codes for joining, Google Sign-In.
