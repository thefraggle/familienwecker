# Changelog

*[🇩🇪 Deutsche Version](CHANGELOG.md)*

## 1.9.3 – 2026-05-15
### Improved
- **Modern icons** – Emojis in the alarm schedule and on member cards have been replaced with crisp Material Icons for a consistent look across all devices.
- **Clearer settings** – Every menu item in the settings now has a matching icon for faster navigation.
- **Better readable form** – Wake times, bathroom duration, departure time, and breakfast in the member editor are now marked with icons.

---

## 1.9.2 – 2026-05-13
### Improved
- **Better translations** – FamWake now speaks all supported languages even more fluently so every family member can easily join in.
- **Spring cleaning** – We cleaned up behind the scenes. The app is now leaner and runs smoother than ever.

---

## 1.9.1 – 2026-05-10
### Improved
- **Faster onboarding** – The introduction has been shortened and focused so you can get started quicker.
- **Cleaner overview** – The "Add member" button is now always accessible at the bottom right.
- **Better readability** – Alarm times and status info on member cards stand out more with higher contrast.
- **Smarter "Awake" button** – The "I'm already awake" button now only appears 2 hours before your scheduled alarm.

---

## 1.9.0 – 2026-05-06
### New
- **Get started instantly – no account needed** – FamWake can now be used without signing up. Create a family or join one via code or link. You only need an account when you want to invite others.
- **7 new languages** – Indonesian, Vietnamese, Bengali, Marathi, Hindi, Chinese, and Korean. FamWake is now available in 22 languages.

### Improved
- **More reliable profile** – After reinstalling, your profile is recognized immediately and the alarm schedule is calculated right away. No more manual toggling.
- **Smarter Auto-Fix** – When times overlap, Auto-Fix now adjusts settings precisely and saves them directly.
- **Refreshed onboarding** – Clearer introduction, new mascot, and you can now toggle helpful tips on or off.
- **Better login flow** – Verification emails open the app directly and log you in automatically.

### Bugfixes
- Fixed duplicate alarms when different weekdays had different wake-up times.
- Language switching now works reliably on all devices.
- Alarm toggle state is correctly preserved after login/logout.
- Profile claiming on new devices works reliably again.
- Family deletion now immediately disables alarms on all devices.
- Various stability and sync improvements.

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
