# DhruvNext — UI/UX Design Reference (imported from Claude Design)

> Status: **REFERENCE ONLY — not binding.** This document records what an external design
> exploration specifies. It does **not** amend `2026-07-12-app-design-standard.md` (the
> currently BINDING nav/IA/token contract) or any ADR in `platform/DECISIONS.md`. Where the two
> disagree, the binding standard wins until someone writes an ADR reconciling them — see §8.
> No code was changed to produce this document; it is documentation only, per request.

## 1. Source

- Claude Design project: `https://claude.ai/design/p/9d9cb5fc-f839-47ac-a7a3-a8ca3af3761e`
- File read: `DhruvNext.dc.html` (a `.dc.html` "design canvas" — an interactive HTML/React
  prototype format, not shippable code).
- Imports read: `support.js` (the prototype's runtime — see §7), `assets/brand/ic_dhruv_logo.webp`.
- Also present in the same project but **out of scope for this pass** (not opened): `Dhruv -
  Current UI Recreation.dc.html`, `Dhruv - Redesign & SDD Plan.dc.html`, `DhruvPhone.dc.html`,
  `android-frame.jsx`, `assets/brand/ic_dhruv_wordmark.webp` (listed as a project asset but not
  referenced anywhere inside `DhruvNext.dc.html`), and the project's own `CLAUDE.md` (its design
  vocabulary is folded into §4–§6 below).
- The project's `CLAUDE.md` states the intended source of truth mapping: *"Android source of
  truth: `apps/finance/**` Compose screens; brand values in `DhruvBrand.kt`."* — i.e. the design
  project itself expects this HTML to be translated into Compose, not consumed directly.

## 2. What this file is

A single self-contained `.dc.html` prototype rendering a 428×908 phone frame (status bar +
19px-rounded screen + home-indicator bar) with a `screen` enum prop that swaps between **23
mutually-exclusive screen states**, plus `dark` and `full` (auto-height) boolean props. It is a
static-data mock (hardcoded numbers, no real backend) meant for visual/interaction review, driven
by a small embedded `Component` class (`renderVals()`) that computes derived flags from `screen`.

## 3. Relationship to the current locked architecture — read this first

`DhruvNext` proposes a **4-tab bottom nav: Home · Calc · Plan · Insights**, single global orange
accent, and a calculator-suite-first IA. The currently BINDING
`2026-07-12-app-design-standard.md` §3.1 locks a **3-tab pager: Home · Tools · Settings**, with
calculators demoted into a Tools launcher grid and per-domain `SectionTheme` accents (tracker =
green, calculators/converters = user-chosen per-section color). These are two different
navigation models for the same app and **cannot both be implemented as-is**.

Concretely, things that conflict:

| Aspect | `DhruvNext.dc.html` (this design) | Binding standard (`app-design-standard.md`) |
|---|---|---|
| Tab bar | 4 tabs: Home, Calc, Plan, Insights | 3 tabs: Home, Tools, Settings |
| Settings | reached via icon button, not a tab | is tab 2 |
| Accent | one global accent (`--acc`, orange by default, user-pickable in Settings) | per-domain `SectionTheme` (tracker always green; calculators use `financeColor`, etc.) |
| Home content | financial-health ring score, net-worth card, quick actions, cash-flow, category bars, upcoming bills, AI insight cards, goals, recent txns — all on one tab | net-worth bento grid only (§3.2's fixed 8-card registry); cash flow/spend/goals are separate cards in that same registry, not a different tab's content |
| Calculator | dedicated `Calc` tab (basic calculator keypad, always reachable) | calculator is one Tools-grid tile, not tab-level |
| Assistant | `Ask Dhruv` — full chat screen (`isAsk`) + a floating pill CTA on Home/Plan/Insights | assistant exists per `platform/PLATFORM.md` §6 but has no chat-screen or floating-pill spec in the binding standard |

Things that **do** already agree (safe to treat as confirmed, not just proposed):
- The accent orange (`--acc` `#F05A28` light / `#FF6D3B` dark) is pixel-identical to the existing
  `PrimaryLight`/`PrimaryDark` tokens in `libs/core/.../theme/Color.kt` — already shipped.
- The 4-swatch accent picker on the design's Settings screen (`#F05A28`, `#00796B`, `#0061A4`,
  `#4A148C`) matches the `light` colors of the `orange`/`green`/`blue`/`purple` entries in
  `ThemeColorConfig.kt`'s `ColorOptions` — the palette values are already in the codebase, only
  wired today as a per-section picker, not this design's single global picker.
- Tabular numerals, Indian digit grouping, "answer first" hero-then-inputs layout, sentence-case
  copy, soft-delete/7-day-erasure consent language — all consistent with `PLATFORM.md`/
  `DECISIONS.md`/the binding standard already.
- The brand mark is the exact production asset: `assets/brand/ic_dhruv_logo.webp` in the design
  project is the same file as `libs/core/src/main/res/drawable-nodpi/ic_dhruv_logo.webp`
  (`DhruvLogo`/`DhruvCrest` composables) — a navy/silver compass-star crest. No new asset exists.

**Recommendation for future work:** before building any of §5, someone must either (a) write an
ADR reconciling the two nav models (e.g. "Calc/Plan/Insights become the Tools-tab content and
sub-routes, not new top-level tabs" — the *content* of Plan and Insights maps cleanly onto
existing calculator feature modules and the tracker's spend/insights cards, so a reconciliation
looks feasible), or (b) explicitly supersede `app-design-standard.md` §3 with a new one. Until
then this document is a reference, not a build target.

## 4. Design tokens as specified

All tokens are CSS custom properties on `[data-nx="light"|"dark"]`; Roboto 400/500/700/900 +
Material Icons ligatures throughout; `font-variant-numeric: tabular-nums` set app-wide.

| Role | Light | Dark |
|---|---|---|
| `--bg` (page) | `#F7F7F5` | `#0B0B0C` |
| `--surf` (card) | `#FFFFFF` | `#16171A` |
| `--surf2` (inset/chip) | `#F1F1EE` | `#1E2024` |
| `--line` / `--line2` (border/hairline) | `#E5E4E0` / `#EFEEEB` | `#2A2C31` / `#222428` |
| `--tx` / `--tx2` / `--tx3` (text primary/secondary/tertiary) | `#14161A` / `#6B6F76` / `#9AA0A6` | `#F2F3F5` / `#9BA1A9` / `#6E747C` |
| `--acc` / `--acc-soft` / `--acc-line` / `--on-acc` | `#F05A28` / `#FFF1ED` / `#F9CDBC` / `#FFFFFF` | `#FF6D3B` / 14%-alpha accent / 32%-alpha accent / `#231007` |
| `--pos` / `--neg` / `--warn` | `#00796B` / `#B3261E` / `#E65100` | `#4CAF50` / `#F2B8B5` / `#FFB300` |
| `--c1`…`--c6` (chart series, never reuse `--acc` for a 2nd series) | `F05A28 00796B 0061A4 4A148C E65100 455A64` | `FF6D3B 4CAF50 80D8FF B388FF FFB300 CFD8DC` |
| `--sh` (the only elevation) | `0 1px 2px rgba(20,22,26,.04), 0 1px 3px rgba(20,22,26,.03)` | `0 1px 2px rgba(0,0,0,.4)` |

Type/shape rules (from the design project's own `CLAUDE.md`, i.e. treat as the design's stated
intent, not something re-derived by this document):
- Screen title 17/700, card title 15/700, body 13.5, meta 11–12, section label 10/700 uppercase
  1.1px letter-spacing. Hero numbers (net worth, EMI, corpus, GST total) 30–46px/700,
  letter-spacing −1 to −2px.
- Radii: card 20dp, list-group 18dp, inner tile 13–16dp, chip/pill = half height (fully rounded),
  primary button 26dp (pill).
- Spacing: card padding 18dp, screen gutter 16dp, inter-card gap 12dp. Hit targets ≥ 40dp (icon
  buttons are 40×40 circles).
- Icons: Material Icons ligatures exclusively — **no emoji anywhere**. Currency/merchant/person
  avatars without a real image use an initials tile (32–34dp, `--surf2` rounded square, 11–12px/
  700 `--tx2` text), never an emoji or generic icon.

## 5. App shell & navigation model

Computed in the embedded `Component.renderVals()`:

```
TABS = [[home,'Home','home'], [calc,'Calc','calculate'], [plan,'Plan','donut_small'], [insights,'Insights','bar_chart']]

OWNER (which tab highlights for a given screen; null = no tab owns it):
  home→home, calc→calc, plan→plan, loan→plan, everyday→plan, invest→plan, tax→plan,
  insights→insights,
  history, consent, ask, shell, splash, onboard, settings, currency, unit, date,
  stopwatch, timer, addtxn, notif, profile → null

BARE (fills the frame edge-to-edge, no status bar / no home-indicator): splash, onboard
showAskPill = screen is one of: home, plan, insights   (floating "Ask Dhruv" pill, bottom-right)
showNav = OWNER[screen] !== null   (renders the 4-tab bar)
noNav   = OWNER[screen] === null AND not BARE            (renders just the home-indicator bar)
```

So: **Home/Calc/Plan/Insights** show the bottom tab bar; **Loan/Everyday/Invest/Tax** are
sub-routes reachable from Plan and keep the Plan tab highlighted but don't show the Plan list
underneath (standard drill-in). **History/Consent/Ask/Shell/Settings/Currency/Unit/Date/
Stopwatch/Timer/AddTxn/Notif/Profile** are all "detail/utility" routes — back-button top bars,
no tab bar, just the home-indicator pill at the bottom. **Splash/Onboard** are first-run, no
chrome at all. `Consent`, `Shell`, and `AddTxn` render as **bottom sheets** (dark 42%-alpha scrim
+ 26px-top-radius sheet with a 38×4 drag handle) rather than full-screen routes — the design
project's own vocabulary calls these `DhruvModalSheet`-style overlays.

Global status bar mock: time, a black pill (camera cutout), signal/wifi/battery icons — cosmetic
only, not part of the app's own UI.

## 6. Screen-by-screen breakdown

### 6.1 First run
- **`splash`** — centered logo (96dp), "dhruv" wordmark text + "finance" uppercase subtitle, a
  46%-filled 3px progress bar, footer line "Everything stays on this device". Bare frame.
- **`onboard`** — one onboarding page (of what a 3-dot indicator implies is 3): a preview card of
  the financial-health ring (78/100) + Saved/Rate mini-stats, headline "Your money, answered
  before you ask", body copy about 11 planners + a calculator that remembers + no accounts to
  link. Skip (top-right) + Continue (bottom, full-width pill) affordances.

### 6.2 Home tab (`isHome`)
Top bar: logo + "Good evening, Sai" / date, notification bell (unread dot) + settings icon.
Scrollable column of cards, in order:
1. **Financial health** — 88dp ring (conic-gradient donut, 78%) with "78 / of 100" center, label
   "Strong, one gap", "+4 points this month" delta, then an **AI insight strip** (accent-soft,
   `auto_awesome` icon) with two pill actions ("Plan the gap", "Why 78?").
2. **Net worth** — hero amount (₹18,42,600) + trend delta, D/M/Y/All segmented toggle, an SVG
   area-line sparkline, then an allocation stacked bar (Equity/Cash/Debt/Gold/Other %) with a
   legend row using the `--c1`…`--c6` chart colors.
3. **Quick actions** — 5 equal square tiles, horizontally scrollable: Expense, Income, Pay bill,
   Scan, Invest (Invest shown at 55% opacity — implies "coming soon"/disabled state).
4. **July cash flow** — In/Out/Saved 3-up stat row (Saved tile accent-highlighted) + a savings-
   rate progress bar with a target-tick marker (36% actual vs 20% target).
5. **Where it went** — top-4 category rows, each an icon + label + amount + a proportional bar.
6. **Next 7 days** — upcoming bills/SIPs list (date chip, name, cadence caption, amount);
   header shows total due.
7. **Smart insights** — up to 3 free-standing insight cards (warn/pos/neg icon + headline +
   explanation), e.g. "Food spend is up 38%", "₹42,000 idle in savings", "Shopping budget
   breached".
8. **Goals** — per-goal: label, "₹X of ₹Y", progress bar (color = on-track green / behind
   amber), status caption with a projected completion date.
9. **Recent** — last 3 transactions (icon tile, merchant, category+time, signed amount).

### 6.3 Calc tab (`isCalc`)
Title bar "Calculator" + history/copy icons. Mode chip row: **Basic** (selected) / Scientific /
Currency / Units — i.e. Currency and Unit converters are reachable as calculator *modes* here,
distinct from their own detail screens (§6.6). Body: two faded "recent result" lines, a divider,
then the live expression + huge `=` result, and 3 AI/utility pill actions (Explain, Tag, Save).
Bottom: a standard 4×5 calculator keypad (C, backspace, %, ÷, 7-9, ×, 4-6, −, 1-3, +, "( )", 0,
".", accent `=`).

### 6.4 Plan tab (`isPlan`) — planner launcher
Title bar "Plan" + settings icon; search field ("Search 11 planners — EMI, SIP, GST…"). Sections:
- **Pick up where you left** — 2 horizontally-scrolling resume cards (icon, name, input summary,
  result).
- **Borrowing** — Loan EMI, Compare two loans.
- **Growing** — SIP growth, Returns (ROI & CAGR), FD & RD maturity.
- **Tax & salary** — GST add/remove, CTC to take-home.
- **Everyday** — Simple & compound interest, Discount & markup, Tip & bill split, Inflation
  impact.
Each is a grouped-list row (icon, title, subtitle, `chevron_right`) — this section's taxonomy
(Borrowing/Growing/Tax & salary/Everyday) reads as the design's version of the app's existing
loans/investments/tax/everyday calculator-module split.

Sub-routes opened from Plan (still `owner = plan`, no visible tab-bar list underneath):
- **`loan`** ("Loan EMI") — hero EMI amount, principal-vs-interest stacked bar + legend, Total
  payable / Interest share / Ends-date 3-up row; then 3 sliders (Loan amount w/ preset chips 5L/
  10L/25L/50L/1Cr, Interest rate, Tenure) each rendered as a track + preset-filled portion + a
  draggable thumb; a 6-bar principal-vs-interest-by-year chart; an AI prepayment-suggestion card
  with a "Model a prepayment" CTA.
- **`invest`** ("SIP growth") — hero corpus amount, invested-vs-gains stacked bar, XIRR/Gain
  multiple/Matures 3-up row; sliders for Monthly instalment/Expected return/Duration; a "Step up
  10% a year" switch; a growth-by-year stacked bar chart (gains vs principal); an AI lump-sum
  suggestion card tied to the Home screen's "idle savings" insight (cross-screen continuity).
- **`tax`** ("GST") — Add GST / Remove GST segmented toggle; hero invoice total; a taxable-
  value/CGST/SGST/total breakdown; a numeric-entry field + rate chips (5/12/18/28%); an
  "Inter-state (IGST)" switch; a "Tax & salary planners" jump-list (CTC, HRA exemption, advance
  tax dates).
- **`everyday`** ("Tip & bill split", representative of the Everyday group) — hero "each person
  pays" amount, Bill/Tip/Total 3-up row; Bill-amount slider; Tip-% chip row (5/10/15/20);
  people-count stepper; "Round up per person" switch; a "More everyday maths" jump-list (interest,
  discount & markup, inflation).

### 6.5 Insights tab (`isInsights`)
Title bar + calendar/share icons. Period chip row (Jul 2026 selected / Jun / May / Quarter / FY).
- **Spent in July** hero + delta vs June + a 6-month bar chart (current month accent-highlighted).
- **Versus last month** — per-category rows with a %-delta pill (red = up/bad, green = down/good,
  neutral = flat).
- **Budgets** — per-category progress bars (amber near cap, red over cap, green under).
- **Top merchants** — initials-tile + name + order count + total spent.

### 6.6 Utilities (no tab, `owner = null`, reached from Calc/Plan or elsewhere)
- **`currency`** — two-row converter card (INR ↔ USD by default, tap to change currency) with a
  central swap FAB overlapping both rows; quick-amount chips (1,000/10,000/85,000 selected/
  1,00,000); a rate/staleness caption; a "₹85,000 in your currencies" list (EUR/AED/GBP/JPY) with
  an offline/cached-rates disclosure banner at the bottom.
- **`unit`** — category chip row (Length selected / Mass / Temp / Area); From/To rows with
  expandable unit pickers and a live result; a "12.5 km is also…" multi-unit list; a numeric
  keypad with a dedicated swap key spanning 2 rows.
- **`date`** — mode chip row (Difference selected / Add or subtract / Age); a difference hero
  ("4y 2m 18d" + days/weeks) with working-days/weekends/hours 3-up row; From/To date rows; a full
  month calendar grid with the selected day accent-filled.
- **`stopwatch`** — Stopwatch/Timer segmented toggle; huge running time display + last-lap
  caption; Lap/Pause/Reset controls; a lap list with FASTEST (green) / SLOWEST (red) tags.
- **`timer`** — 250dp circular countdown ring (conic-gradient progress) with time + "of 15:00" +
  end-clock-time in the center; duration presets (1/5/15/30 min, Custom); Stop/Pause/Add controls.

### 6.7 Transactions
- **`addtxn`** — bottom sheet. Expense/Income segmented toggle; huge amount entry with a blinking
  caret; category chip row (Food selected, Transport/Shopping/Bills); a 3-row grouped list
  (account picker, date/time picker, free-text note); a 4×4 numeric keypad with a full-width
  "Save" key spanning 2 columns.
- **`history`** (of calculator results, not transactions) — back + title + share/overflow;
  search field (prefilled "gst") with mic icon; filter chip row (All 142 / Saved 9 / Tagged /
  Bin 4); date-range chip row (This month selected / Today / Last 7 days / Anytime); date-grouped
  result cards (expression, result, optional tags, time, star/starred toggle).

### 6.8 AI (`ask`, `consent`)
- **`ask`** ("Ask Dhruv") — back + title + "Online · question only" status dot; a chat transcript
  (user bubble right-aligned accent-filled, assistant bubble left-aligned card with a structured
  numeric breakdown sub-card + a disclaimer line "Estimate from the numbers on this device. Not
  financial advice."); Copy/Open-in-planner/Retry actions under each assistant reply; a "Try next"
  suggested-follow-up chip list; bottom composer (text field + mic + send).
- **`consent`** — bottom sheet, drag handle, `auto_awesome` icon, "Let Dhruv think out loud?"
  headline. Copy is close to a direct restatement of `PLATFORM.md` §6 / `DECISIONS.md` ADR-0002:
  *"this sends your question to Google Gemini. Nothing else leaves your phone."* Three rows: what
  is sent (only the question text + the single tapped expression), what never is (history,
  balances, goals, transactions, tags, notes), and the right to withdraw ("Turn this off any time
  in Settings. Deletion completes within 7 days — DPDP Rules 2025." — matches ADR-0005's 7-day
  erasure guarantee verbatim in intent). Two actions: "Allow and continue" (filled) / "Stay fully
  offline" (outlined) + a footer clarifying offline still keeps every calculator working.

### 6.9 Shell / account (`shell`, `settings`, `profile`, `notif`)
- **`shell`** — bottom sheet, an app-switcher: "Your dhruv apps — one account, one sync, separate
  vaults." Rows for **finance** (OPEN, accent-outlined, current app), **tools** (SOON), **vault**
  (SOON, lock icon) — directly matches `PLATFORM.md` §1's app table and status column. Below a
  divider: Sync & devices, Privacy & data shortcuts.
- **`settings`** — back + title; scrollable grouped sections: account row (avatar-initial,
  name/email, sync caption); **Appearance** (Theme System/Light/Dark row, a 4-swatch global
  accent picker — see §3's conflict note, "Use wallpaper colours" Material You switch);
  **Money** (Currency & format, Month starts on, Decimal places); **AI & privacy** (Ask Dhruv
  toggle — on by default here, App lock/biometric toggle — off by default, Export my data,
  destructive "Delete everything" row in `--neg`); **App** (Notifications, Backup & restore,
  Home screen widgets, About + version).
- **`profile`** ("Account") — back + title; centered avatar/name/email + Edit profile pill; a
  142/9/"14 mo" stats strip (Calculations / Saved plans / With dhruv); **Sync** section (Synced
  4 min ago + "Sync now", this-device row, a second device with a "Remove" action); **Your dhruv
  apps** mini-list (finance in-use, tools coming-soon, greyed); a full-width outlined "Sign out"
  row in `--neg`.
- **`notif`** — back + title + "Mark all read"; date-grouped notification cards: an unread
  accent-highlighted "EMI in 2 days" card with an unread dot, then plain cards for budget breach,
  health-score-up, unused-subscription nudge, and a backup-completed confirmation. Same
  icon+headline+body shape as the Home "Smart insights" cards, reused as a notification template.

## 7. `support.js` — prototype runtime, not app code

`support.js` is a generated bundle ("`// GENERATED from dc-runtime/src/*.ts — do not edit`") that
parses the `<x-dc>`/`<sc-if>`/`<sc-for>`/`{{ }}` template syntax in the `.dc.html` file and renders
it via `window.React`/`window.ReactDOM` for the Claude Design canvas preview. It has no
relationship to the Kotlin/Compose codebase and is not something to port — it exists purely so
the design file is previewable/interactive inside the design tool.

## 8. Open questions for whoever picks this up

1. Reconcile the 4-tab (Home/Calc/Plan/Insights) vs 3-tab (Home/Tools/Settings) navigation models
   — needs an ADR per `CLAUDE.md`'s "do not redesign architecture, propose an ADR instead" rule.
2. Decide whether the single global accent picker (design's Settings) replaces or coexists with
   the existing per-`SectionTheme` accent system (`ThemeColorConfig.kt`) — binding standard §2
   currently mandates per-domain accents.
3. The AI "Ask Dhruv" chat screen and floating pill are new surface area not described in
   `app-design-standard.md` §5's component inventory or §3.3's route registry — would need a
   `FeatureHost` key, a route-registry row, and a notification-channel/consent entry (§4's AI
   Assistant settings section already has a slot: "Consent status + re-consent · BYO Gemini key").
4. `history` here means *calculator-result history*, distinct from the tracker's transaction
   history — naming should stay disambiguated if both ship (the binding standard's Tools grid
   already lists a calculator history preview under §4's Calculators settings section).
5. Financial-health "score out of 100" (Home, ring) is not a scored concept anywhere in
   `PLATFORM.md`/the phase specs today — would need its own design/data spec if pursued.
