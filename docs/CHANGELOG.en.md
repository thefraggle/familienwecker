# Changelog

*[🇩🇪 Deutsche Version](CHANGELOG.md)*

## 1.7.12 – 2026-04-21

### Improved
- **App name in short display** – Now correctly aligned with the official store title for English, Turkish, Swedish, Norwegian, and Japanese.
- **Ruhr Valley dialect** – App name now displays correctly without a hyphen.

---

## 1.7.11 – 2026-04-20

### Fixed
- **Phantom "calculation error"** – Adding a new family member could incorrectly show a calculation error, even though everything worked correctly.
- **Spinner when joining a family** – Attempting to join a family while offline left the loading indicator stuck.
- **Error when editing quickly** – Rapidly changing wake times could briefly show a false sync error.

### Improved
- **Emails for dialect users** – Password reset and account verification emails now arrive in German when Swabian, Swiss German, or Ruhr Valley dialect is selected (instead of English).

---

## 1.7.10 – 2026-04-19

### Fixed
- **Create/delete/leave family** – Creating, deleting, or leaving a family could incorrectly show “Synchronization failed” even though the action succeeded. Especially on fresh installations.

### Improved
- **More accurate alarm sound detection** – The selected alarm sound name is now correctly displayed and tracked.
- Background improvements for device security checks.

---

## 1.7.9 – 2026-04-18

### New
- **Smart review prompts** – The app gently asks for a rating after a positive experience – never in the morning, never too often.
- **More wake-up messages** – 20 different messages on the alarm screen, in all 18 languages.

### Improved
- **Onboarding refreshed** – The invite screen now shows country-specific example names (e.g. “Smith Family” instead of “Mustermann”).
- **Translations improved** – Missing texts added in several languages. Dialects (Swabian, Ruhr Valley, Swiss German) refined.
- **Smaller app size** – Removed unused images.

---

## 1.7.8 – 2026-04-18

### Fixed
- **Schedule shows correct date** – "Breakfast today at …" is now displayed correctly as long as family members are still being woken up. Only then does the display switch to the next day.
- **"I'm awake" button** – The button is now reliably active within the correct time window.
- **Schedule after login** – After logging out and back in, all members now appear correctly in the schedule immediately.

### Improved
- General stability and reliability improvements.

---

## 1.7.7 – 2026-04-17

### New
- **Onboarding reimagined** – The intro tour now explains more clearly what FamWake is really about.

### Improved
- Background improvements for better data security.

---

## 1.7.6 – 2026-04-15

### Fixed
- **Times always readable** – On narrow devices and in languages with long labels, times were sometimes cut off.

---

## 1.7.5 – 2026-04-10

### Fixed
- **Back button** – The back arrow in the header now responds correctly on all screens.
- **Stability** – Removed several potential crash sources in rare scenarios.

### Improved
- **Emails** – Password reset and account verification emails now arrive in the correct language.

---

## 1.7.4 – 2026-04-10

### Improved
- **Look & Feel** – Fresher, more modern design: softer corners, cleaner cards.
- **Scroll hint** – A gentle bouncing arrow shows when more content is available below.
- **Main screen** – Title is now large when opened and collapses smoothly when scrolling.

---

## 1.7.3 – 2026-04-10

### Improved
- **Language fallback** – Unknown language codes now fall back to English instead of German.

---

## 1.7.2 – 2026-04-10

### New
- **Norsk 🇳🇴 and Dansk 🇩🇰** – FamWake now speaks Norwegian and Danish.
- **14 languages** – The language picker now covers 14 world languages plus 3 regional dialects.

### Improved
- **Language picker** – Cleaner selection menu, sorted alphabetically.
- **Appearance** – Sun/Auto/Moon icons instead of a dropdown.
- **Weekly schedule** – "Copy to other days" always visible. Better field order.
- **Small screens** – All screens are now fully usable.

---

## 1.7.1 – 2026-04-08

### Improved
- **Time picker** – Clean keyboard-style dialog instead of a clock face.
- **Keyboard** – Dismisses when tapping outside the input field.
- **Bathroom buttons** – "−" and "+" always stay visible.

---

## 1.7.0 – 2026-04-06

### New
- **Swedish 🇸🇪** – Välkommen!
- **Dialects 🎉** – Swabian, Swiss German, and Ruhr Valley slang.
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
- **French, Spanish, and Italian** – Three new languages with automatic detection.
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

## 1.3.0 – 2026-03-17

### New
- **⭐ Rate the app** – Rate directly in the Play Store from within the app.

### Improved
- Disabled days are now more clearly highlighted.
- Anti-spam protection for emails and family invites.

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
- Support for German and English.
- Invite codes for joining, Google Sign-In.
