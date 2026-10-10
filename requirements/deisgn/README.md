# Handoff: SauberZauber – App-Redesign

## Overview
SauberZauber tracks which of two household members (sample users: Alex, Sam) did which chore and how often. It is **not a scheduler**: no due dates and no reminders. This handoff covers the full redesign: login, "Aufgaben" (main screen), "Statistik & Aufgaben" (statistics + CRUD), the empty state, the desktop layout and the PWA assets. All UI text is German.

Target repo: `florianheger/SauberZauber`, Angular frontend in `Frontend/src`. Today only `login/` and `home/` exist, with browser-default styling.

## About the Design Files
The `.dc.html` files are **design references built in HTML**. They are working prototypes of the intended look and behavior, not production code. Recreate them in the existing **Angular** app using its patterns (standalone components, signals, router, the existing `AuthService`). Don't ship the HTML. Open `SauberZauber Entwurf.dc.html` in a browser (it needs `support.js` next to it) to click through every state.

- `SauberZauber App.dc.html`: the complete app, one component. Template = markup, `class Component` = all logic (state, German icon search, fairness text, long-press).
- `SauberZauber Entwurf.dc.html`: canvas with phone frames (1a–1d), desktop (1e), PWA assets (1f) and current state (0a/0b).

## Fidelity
**High-fidelity.** Final colors, type, spacing, radii and interactions. Recreate pixel-accurately.

## Global layout
- Mobile-first at 390 px; content column `max-width: 720px; margin: 0 auto`.
- **Phone (< ~900 px):** column layout. Content padding is `16px` horizontal. Header top padding is the status bar plus `env(safe-area-inset-top)`, and the bottom padding of about 140 px clears the nav bar. A fixed **bottom nav** sits at the bottom with padding `10px 24px calc(10px + env(safe-area-inset-bottom))`.
- **Desktop (≥ ~900 px):** a fixed **sidebar** 256 px wide (surface bg, 1px right border, padding `28px 16px`), main padding `40px 48px 64px`. Sheets become centered dialogs (560 px wide, top 6 %, radius 28 px, padding 28 px). The who-did-it chooser is 400 px wide, top 24 %.
- Body font: **Figtree** 400–700. Headings: **Bricolage Grotesque** 700/800. Icons: **Material Symbols Rounded** (variable: FILL 0/1, wght 300–600).

## Screens

### Login
- Full-screen `--bg`. A vertically centered block with: app icon 104×104, radius 30, `#5A3FC0`, white `auto_awesome` symbol 56 px FILL 1, shadow `0 16px 40px rgba(90,63,192,.35)`. Below it: H1 "SauberZauber" in Bricolage 800 38 px, letter-spacing −0.02em. Tagline "Haushalt fair geteilt – ein Tipp, und schon ist’s notiert." in 17 px/1.45, `--muted`, max-width 280.
- Bottom (max-width 360, padding `70px 24px 56px` + safe areas): primary button height 58, radius 29, `--brand` bg, `--on-brand` text 600 17 px, icon `passkey` 24 px, label „Mit Passkey anmelden“. While busy: label „Einen Moment …“, opacity .7, disabled.
- Error (existing `errorMessage` signal): a box above the button with radius 14, padding `12px 14px`, `--surface2` bg, `--danger` text 15 px and the `error` icon.

### Aufgaben (Start)
- Header: „Hallo {Name}“ (15 px, muted) above H1 „Aufgaben“ (Bricolage 700 34 px, line-height 1.05). Padding `16px 4px 18px`. On phone, an avatar button sits on the right (44 px circle, `--me` bg, initial 700 17 px). Tapping it opens a menu (220 px, radius 18) with „Angemeldet als …“ and a „Abmelden“ button (48 px tall, `logout` icon).
- List: a column with gap 10. **Task card**: flex row, gap 14, padding 14, `--surface` bg, 1px `--line` border, radius 22, `--shadow`.
  - Icon tile: 48×48, radius 15, `--brand-soft` bg, `--brand` symbol 26 px.
  - Text column: title 600 17 px/1.25. Description 14 px/1.35 muted (only if set). "Zuletzt" line 13 px muted with an 8 px dot in the person's colour: „Zuletzt du · vor 3 Std.“ / „Zuletzt Sam · gestern“.
  - **Secondary button** (mark done for the other person): 44 px circle, 1.5px border `--other`, transparent, initial in `--other` 700 16 px. `aria-label` „Von {Other} erledigt“.
  - **Primary button** (mark done for me): 56 px circle, `--me-soft` bg, `check` symbol 30 px wght 600 in `--me`.
- **Empty state:** centered, padding `56px 24px`. Tile 96×96 radius 32 with `auto_awesome` 48 FILL 1. H2 „Noch keine Aufgaben“ in Bricolage 700 24. Text „Legt unter „Statistik & Aufgaben“ eure erste Aufgabe an. Danach reicht ein Tipp, um sie abzuhaken.“ Button „Erste Aufgabe anlegen“ (52 tall, radius 26, `add` icon) switches to the Statistik tab and opens the create sheet.

### Statistik & Aufgaben
- H1 „Statistik & Aufgaben“.
- **Summary card** (radius 24, padding 20, gap 14):
  - Label „Insgesamt erledigt“ 13 px 600 muted.
  - Two columns, left/right: person name with a 10 px dot („Alex (du)“) and the count in Bricolage 700 44 px in the person's colour.
  - Split bar: 14 px tall, two flex segments, gap 4, radius 7, `flex-grow` = count (1:1 if both are 0).
  - Fairness sentence (15 px/1.45):
    - Total = 0 → „Sobald ihr Aufgaben erledigt, seht ihr hier, wie ihr euch die Arbeit teilt.“
    - |a−b|/total ≤ 10 % → „Ihr seid fast gleichauf – schön, wie ihr euch das teilt.“
    - I'm behind → „{Other} hat zuletzt etwas mehr übernommen. Wie wär’s als Nächstes mit „{Chore with the largest gap}“?“
    - I'm ahead → „Du hast zuletzt etwas mehr übernommen – danke dir! Kleine Unterschiede gleichen sich mit der Zeit aus.“
- Section header: H2 „Pro Aufgabe“ (Bricolage 700 22) and the button „Neue Aufgabe“ (44 tall, radius 22, brand, `add` icon). Padding `28px 4px 12px`.
- **Per-chore list:** one card (radius 24) with rows separated by a 1px `--line` top border. Row padding `14px 8px 14px 16px`, gap 14. Each row has:
  - an icon tile 40×40, radius 13;
  - the title 600 16;
  - a stats row: Alex count (700 14, `--alex`, tabular-nums) · split bar 8 px tall, radius 4, gap 3 (`--surface2` track if both counts are 0) · Sam count;
  - a 44 px `edit` icon button.
  - The whole row is clickable and opens the edit sheet.

### Create / edit sheet
- Phone: bottom sheet (radius `28px 28px 0 0`, padding `10px 20px 40px` + safe-area bottom, max-height 92 %) with a grab handle 40×5. Desktop: centered dialog.
- Title „Neue Aufgabe“ / „Aufgabe bearbeiten“ (Bricolage 700 24). Gap between fields 18.
- **Titel** (required): input 52 tall, radius 14, 1.5px border `--line` (focus `--brand`, error `--danger`), bg `--bg`, 16 px. Placeholder „z. B. Wäsche waschen“. Error text „Bitte gib einen Titel ein.“ on save with an empty title.
- **Beschreibung (optional):** textarea with 2 rows and the same styling. Placeholder „z. B. Inklusive Aufhängen“.
- **Icon:**
  - Search pill 48 tall, radius 24, `--surface2` bg, `search` icon. Placeholder „Suchen, z. B. Wäsche, Müll, Staubsaugen“.
  - Below it a 6-column grid, gap 8, square cells (min 44), radius 14, max-height about 200 px with scroll. The selected cell has a 2px `--brand` border and `--brand-soft` bg.
  - No results → „Keine Treffer für „…“. Versuch einen anderen Begriff.“
- Actions (right-aligned): „Abbrechen“ (text button) and „Speichern“ (brand, 48 tall, radius 24). In edit mode, „Löschen“ sits on the left (danger text, `delete` icon).
- **Delete confirmation:** a dialog of 320 px, radius 28, padding 24, with the `delete` icon in danger colour, H2 „„{Titel}“ löschen?“ and the text „Die Aufgabe und ihre Statistik ({n}× erledigt) werden dauerhaft entfernt.“ Buttons: „Abbrechen“ and „Löschen“ (danger bg).

### Who-did-it chooser (long-press / right-click on a task card)
- Bottom sheet titled „Wer hat „{Titel}“ erledigt?“ with two 60 px option buttons, radius 18:
  - „Ich ({Me})“ on `--me-soft` with an avatar;
  - „{Other}“ on `--other-soft`.
- Below them a „Abbrechen“ button.

### Snackbar
- Phone: left/right 16 px, bottom 112 px (above the nav). Desktop: bottom-left of the content area, max-width 440.
- Styling: `--snack` bg, radius 16, min-height 52, padding `6px 6px 6px 18px`, `check_circle` icon in `--snack-action`, text 15/500.
- Optional „Rückgängig“ action (44 tall, 700 15, `--snack-action`).
- Auto-hides after 5 s. Messages: „Erledigt von {Name}“ (with undo), „Aufgabe angelegt“, „Änderungen gespeichert“, „Aufgabe gelöscht“.

### Navigation
- **Bottom nav** (phone): two items, „Aufgaben“ (`checklist`) and „Statistik“ (`bar_chart`). Each item is at least 56 tall: a 64×32 pill (radius 16; active = `--brand-soft` and icon FILL 1) above a 600 12 px label.
- **Sidebar** (desktop):
  - Logo row: 40 px icon, radius 12, plus „SauberZauber“ in Bricolage 700 20.
  - Nav items 48 tall, radius 14; the active one is `--brand-soft`.
  - Bottom: avatar 36, name and a logout icon button.

## Interactions & Behavior
- **Mark done (me):** one tap on the check adds a log entry `{choreId, by: me, at: now}`. The button turns solid `--me` with `--on-person` text and scales to 1.08 (`transform .3s cubic-bezier(.3,1.6,.5,1)`, bg `.2s`), then reverts after 1.3 s. It also triggers `navigator.vibrate(12)` and the snackbar with undo.
- **Mark done (other):** the initial button does the same for the other person and the snack reads „Erledigt von {Other}“.
- **Long-press** (480 ms, pointerdown/up/leave; `contextmenu` on desktop) opens the chooser with `vibrate(20)`. The card scales to .98 while it's open. Suppress the click that follows a long-press. There's an alternative variant (prototype tweak `otherAction=longpress`): hide the secondary button and show the hint „Lange drücken, um für {Other} einzutragen“ above the list.
- **Undo:** removes that log entry.
- **Sheets:** phone slides up (`translateY(105%)` → 0, `.32s cubic-bezier(.2,.8,.2,1)`); desktop scales .96 → 1 and fades. Scrim fades `.25s`, and tapping the scrim closes the sheet.
- **Relative time:** „gerade eben“, „vor N Min.“, „vor N Std.“, „gestern“, „vor N Tagen“.
- **German icon search:** case- and umlaut-insensitive (ä→a, ö→o, ü→u, ß→ss; ae/oe/ue are folded too). Multiple words are combined with AND. The match runs against the symbol name plus German keywords. The full keyword list is the `ICONS` array in `SauberZauber App.dc.html`; port it to e.g. `icon-catalog.ts` and extend as needed.
- Hit targets are ≥ 44 px everywhere.

## State Management
- `chores: {id, title, description?, icon}[]`
- `completions: {id, choreId, by: 'Alex'|'Sam' (userId), at}[]`. Derive the counts per chore and person, the totals, and the last completion per chore from this list (or have the backend aggregate them).
- UI state: active tab (router: `/` → Aufgaben, `/statistik`), snackbar `{text, undoId}`, sheet `{mode, id, title, desc, icon, query, touched}`, chooser choreId, confirm-delete flag.
- Backend needs:
  - CRUD for chores;
  - POST a completion (with `by`);
  - DELETE a completion (undo);
  - stats per chore;
  - deleting a chore cascades to its completions.
- Current user: from `AuthService.currentUser()`. The "other" person is the second household member.

## Design Tokens
Use CSS custom properties on `:root` / `[data-theme=dark]`, following `prefers-color-scheme`. Values (oklch, as used in the prototype):

| Token | Light | Dark |
|---|---|---|
| --bg | oklch(97.6% 0.007 80) | oklch(18% 0.018 285) |
| --surface | #fff | oklch(23% 0.02 285) |
| --surface2 | oklch(95.5% 0.008 80) | oklch(27.5% 0.02 285) |
| --ink | oklch(24% 0.025 285) | oklch(95% 0.008 80) |
| --muted | oklch(48% 0.02 285) | oklch(74% 0.02 285) |
| --line | oklch(91.5% 0.008 80) | oklch(31% 0.02 285) |
| --brand | #5A3FC0 | #B9A6FF |
| --brand-soft | oklch(94.5% 0.035 288) | oklch(32% 0.07 288) |
| --on-brand | #fff | oklch(20% 0.04 288) |
| --alex | oklch(52% 0.1 200) | oklch(76% 0.1 200) |
| --alex-soft | oklch(94.5% 0.03 200) | oklch(31% 0.05 200) |
| --sam | oklch(57% 0.15 40) | oklch(77% 0.12 45) |
| --sam-soft | oklch(95% 0.03 45) | oklch(32% 0.06 45) |
| --on-person | #fff | oklch(20% 0.03 285) |
| --danger | oklch(52% 0.19 25) | oklch(72% 0.15 25) |
| --snack / --on-snack | oklch(26% 0.02 285) / #fff | oklch(92% 0.01 80) / oklch(22% 0.02 285) |
| --snack-action | oklch(82% 0.1 288) | #5A3FC0 |
| --scrim | rgba(20,16,40,.38) | rgba(0,0,0,.55) |
| --shadow | 0 1px 2px rgba(30,20,60,.05), 0 8px 24px rgba(30,20,60,.06) | none |

`--me` / `--other` (+ `-soft`) map to alex/sam depending on the signed-in user.

- Radii: 12, 13, 14, 15, 16, 18, 22 (cards), 24 (sections/pills), 28 (sheets/dialogs), 50 % (round buttons).
- Spacing: 4, 6, 8, 10, 12, 14, 16, 18, 20, 24, 28.
- Type scale: 12 / 13 / 14 / 15 / 16 / 17 body, plus 22 / 24 / 34 / 38 / 44 display.

## PWA
- `manifest.webmanifest`:
  - `name`/`short_name` "SauberZauber", `lang: "de"`;
  - `display: "standalone"`;
  - `theme_color` and `background_color` `#5A3FC0`;
  - icons 192/512 plus maskable variants.
- `index.html`:
  - `<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">`;
  - `theme-color` metas per colour scheme: light `#F8F6F1`, dark `#17141F`;
  - `apple-mobile-web-app-capable`, `apple-touch-icon` 180 px;
  - `lang="de"`.
- Safe areas: use `env(safe-area-inset-*)` for the header top, bottom nav, sheets and login.
- Add `@angular/service-worker` (`ng add @angular/pwa`).

## Assets
- Fonts (Google Fonts): Bricolage Grotesque, Figtree, Material Symbols Rounded. Self-host them for offline PWA use.
- App icon: **placeholder**, the symbol `auto_awesome` (white, FILL 1) on `#5A3FC0`. Replace it with a real logo when one exists.
- No other images.

## Files
- `SauberZauber App.dc.html`: full interactive prototype (all screens and logic).
- `SauberZauber Entwurf.dc.html`: overview canvas (phone, dark, login, empty, desktop, PWA, current state).
- `support.js`: runtime needed only to open the prototypes in a browser. It is not part of the app.
