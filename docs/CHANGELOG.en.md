# Changelog

*[🇩🇪 Deutsche Version](CHANGELOG.md)*

## 1.9.17 – 2026-06-03
### Improved
- **Reliable Snooze Alarm** – The snooze function has been fixed and now rings completely reliably. Your phone remains locked while snoozing so you can continue sleeping peacefully.
- **Smoother Scrolling** – We made major background optimizations, making the app react even faster and smoother when scrolling through the alarm schedule.
- **More Privacy** – After stopping the alarm or hitting snooze, there is no more accidental unlocking of the screen. Everything remains secure.
- **Better Accessibility** – For people using screen readers, we improved some button descriptions to be easier to understand and fixed minor translation errors.

## 1.9.16 – 2026-06-03
- **Reliable Snooze** – The snooze feature has been completely overhauled and now reliably rings again after 5 minutes. A banner in the app shows the remaining snooze time and can be cancelled at any time.
- **Seamless Alarm Stop** – When you stop the alarm, the app now opens directly instead of leaving you on the phone's home screen.
- **Clearer Alarm Sounds** – Fixed an issue where alarm sounds could play twice or overlap. Custom sounds also load more reliably now.
- **Stable Offline Mode** – If you lose your internet connection, the app will now safely revert changes (e.g., when pausing an alarm) if they couldn't be saved.
- **Safer Wake-Ups** – A background optimization ensures that scheduled alarms are never "swallowed" or missed under any circumstances.

## 1.9.15 – 2026-06-01
### Improved
- **More intentional alarms** – When adding new family members, alarms are now turned off by default for all weekdays (Opt-In). You simply activate the specific days you need, preventing any unwanted wake-ups.

## 1.9.14 – 2026-05-29
### Improved
- **Better readability** – Inactive buttons (e.g., before entering a name) are now much easier to read in both light and dark modes, and will only react once all required information is entered.
- **Clearer messages** – If in-app purchases or donations are ever unavailable on your device, you will now see a helpful message instead of a technical error.

## 1.9.13 – 2026-05-28
### New
- **Help tooltips restored** – We brought back all helpful explanations and tips (e.g., regarding weekdays, bathroom times, and wake windows) to make using the app even easier and more intuitive.

### Improved
- **Unified app design** – We harmonized the design of all buttons and input fields across the entire app. With consistent rounded corners, optimized contrasts, and uniform heights, the app now looks even more polished and coherent.
- **Polished Google Sign-In** – The Google login button has been visually enhanced to integrate seamlessly and remain highly visible in both light and dark modes.
- **Optimized alarm screen** – The active alarm screen now adapts even more flexibly to modern displays with camera notches, ensuring no buttons are ever covered.

### Fixed
- **Registration issue resolved** – Fixed an issue during registration where users could occasionally access the app before verifying their email address.


## 1.9.12 – 2026-05-26
### Fixed
- **Bathroom Buffer Fix** – Individual bathroom buffers are now correctly applied to the subsequent family member even when the global buffer is set to 0 minutes. Additionally, individual buffers are reliably displayed when a member is positioned first in the bathroom order.
- **Buffer Settings Polished** – Manual 0-minute buffer overrides now reliably override larger global buffers. In the member editor, the buffer value is displayed in italics whenever it matches the global value to clearly indicate inheritance.

## 1.9.11 – 2026-05-26
### Improved
- **Better Privacy & Data Protection** – User data and profiles are now completely and thoroughly wiped from our database when an account is deleted.
- **Automated Cleanup** – Unlinked temporary accounts with no registered login are now automatically cleaned up after 30 days of inactivity to keep the system clean.
- **Cleaner View** – We reduced help tooltips to a minimum to keep screens simple and highlight only the most essential information.

---

## 1.9.10 – 2026-05-25
### New
- **Reordering Confirmation Dialog** – When reordering members on the schedule, you will now be asked if the new order should apply only to today or to the entire week.

### Improved
- **Smarter "I'm Awake" Button** – The "I'm already awake" button is now permanently visible on the day of your alarm (or starting 4 hours before) and no longer disappears when previewing schedules for other weekdays.
- **Optimized Push Notifications** – You will only receive family schedule updates when other members actively make changes. Automatic background resets will no longer trigger unnecessary notifications.
- **Bugfixes & Design Polish** – More reliable dialog closing behavior on all devices and visual polishing of the schedule cards.

---

## 1.9.9 – 2026-05-23
### Fixed
- **Automatic time adjustment corrected** – Resolving overlapping times now reliably updates the selected day without modifying settings for other weekdays.
- **Startup crash resolved** – Fixed an issue that could prevent the app from opening under certain conditions.
- **Overlap display improved** – Details of the wake schedule are hidden when time conflicts cannot be resolved, until they are corrected, to avoid confusion.

---

## 1.9.8 – 2026-05-22
### New
- **Weekday schedules** – Customize bathroom order and wake-up times for each day of the week.
- **New Empty State** – Refreshed design and clearer help texts for days without scheduled alarms.

### Improved
- **Warning banner relocated** – The notice for unregistered profiles/missing alarms has been moved directly below the optimal plan card.

---

## 1.9.7 – 2026-05-22
### Improved
- **App startup stability** – Fixed a rare crash that could occur under certain conditions when opening the app.
- **Optimized review requests** – Requests for app feedback are now scheduled at more appropriate times without disrupting the morning routine.
- **Better theme scaling** – The app design now syncs even more reliably with your system-wide light/dark preferences.
- **Clearer error alerts** – Improved readability and contrast of error cards in settings and on the home screen.

---

## 1.9.6 – 2026-05-21
### New
- **Buffer explanation (Tooltip)** – A new help tooltip explains in a simple way how the family bathroom buffer works.

### Improved
- **Crash-free app startup** – Fixed a cold startup crash when launching the app by tapping an alarm notification.
- **Secured app settings** – Migrated local settings storage to a modern system to ensure your preferences are saved even more reliably.
- **Visual polish** – Harmonized tooltip colors to match the sleeping theme. Fixed potential button truncations on some screens and improved general color contrast in light mode.

---

## 1.9.5 – 2026-05-20
### New
- **Simple Mode** – For family members who do not need a coordinated bathroom slot or wake up outside the usual routine. Simple mode hides all advanced options and triggers the alarm exactly at the chosen wake-up time.
- **Full Feature Set** – FamWake now offers the same feature set on all supported platforms, including full push notifications, deep links for sharing the family, and synchronous UI scaling for accessibility.
- **Automatic Restore** – The family status is now automatically restored in the background after a reinstall, so you no longer have to join manually.

### Improved
- **Faster Sharing** – The family code share button now reacts instantly and without delay to login status updates.
- **Reliable Profile Deletion** – If your member profile is deleted from another device, the app now instantly and reliably disables alarms on your device.

---

## 1.9.4 – 2026-05-16
### New
- **Buffer after bathroom** – Add a time buffer between family members’ bathroom slots. Set it globally for everyone or individually per person.
- **Invite family** – New share button on the home screen to invite family members via WhatsApp, SMS, or any other app.

### Improved
- **Time format adapts** – The app now automatically shows times in 12h or 24h format based on your device setting.
- **Compact cards** – Member cards display info more efficiently with icons. No more text wrapping, even in longer languages.
- **Less clutter** – Removed redundant labels. Icons speak for themselves.

---

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
- **⭐ Rate the app** – Rate directly from within the app.

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
