# SauberZauber: Design Brief

## Context
SauberZauber is a household chore tracker for **one household with exactly two people** (sample users: Alex and Sam). It records who did which chore and how often, so housework can be shared fairly. It is **not a scheduler**: there are no due dates, deadlines or reminders, and every chore repeats indefinitely.

## Platform
- **Angular** web app, used mainly as an installed **PWA** on Android and iOS phones, but it must also work on desktop.
- **Mobile-first:**
  - design for about 390px width first, then scale up
  - large touch targets, one-handed use
  - main navigation within thumb reach (e.g. a bottom nav bar)
- **Should feel like a native app** when installed:
  - standalone display mode, no browser chrome
  - respects the safe areas (iPhone notch and home indicator)
  - needs an app icon, a splash/theme colour, and light and dark modes
- **All UI text is in German.**
- Icons come from **Google Material Symbols**.
- Login already exists: a single screen with the app name and a „Mit Passkey anmelden“ button. Restyle it to match the new design.

## Pages

### 1. Start („Aufgaben“): the main screen, used daily
- A list of all existing chores. Each one shows its Material Symbol icon and title, plus the description if it has one.
- The main action is to **mark a chore as done**, and it should take one tap. The current user can mark a chore done:
  - **for themselves** (the primary action)
  - **for the other person** (a secondary action, e.g. a second button or a long-press or menu option)
- After marking, show short feedback, ideally with undo (e.g. a snackbar „Erledigt von Sam – Rückgängig“).
- Optional, if it fits without clutter: when each chore was last done and by whom.
- An empty state for when no chores exist yet, pointing to page 2 to create one.
- No editing on this page; keep it fast and simple.

### 2. Statistics and chore management („Statistik & Aufgaben“)
- **Statistics:** for each chore, how many times each person has done it.
  - The two people should be easy to compare side by side, e.g. per-chore rows with two counts or a split bar, in a distinct colour per person.
  - A total per person is a nice extra.
- **Manage chores:**
  - **Create:** title (required), description (optional), and an icon picked from Material Symbols. The icon picker needs a search field that works with **German terms** (e.g. „Wäsche“, „Müll“, „Staubsaugen“).
  - **Edit:** the same fields.
  - **Delete:** with a confirmation step.
- On mobile, create and edit can be a bottom sheet or a full-screen dialog.

## Tone
Friendly, light and a bit playful (the name is a pun on "sauber" = clean and "Zauber" = magic), but calm and uncluttered. It's a tool two people open several times a day, so speed beats decoration. Fairness should feel encouraging, not like a scoreboard.
