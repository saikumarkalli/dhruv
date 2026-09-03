# Dhruv Finance — Design v1.0 FINAL · Functional Specification (BSA)

> **Source of truth for this doc:** Claude Design project `Dhruv brand & UI/UX finalization`
> (`20503754-e522-4b72-9601-ed1ea0f5801b`), imported 2026-08-08 via the `claude_design` MCP.
> Files read: `Dhruv Android Screens.dc.html`, `Dhruv Brand & Theme.dc.html`,
> `Dhruv Component Library.dc.html`, `Dhruv Launch & Logo.dc.html`, `Dhruv Web App.dc.html`,
> `support.js` (generated dc-runtime — no design content, ignored).
>
> **Status:** SPEC — Finance functional definition only. Technical build order lives in
> `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md`.
> Design labels itself **v1.0 · FINAL · Finance 1.2.5 · Android · Compose M3**.
>
> **Scope (narrowed 2026-08-09, ADR-0030).** The cross-app half of this document — tokens,
> typography, brand chrome, logo, component library, navigation law, screen-state matrix, motion,
> accessibility, copy — was promoted to **`platform/DESIGN-SYSTEM.md`**, the single design contract
> for every Dhruv app. What remains here is Finance's own product spec: its 61 screens (groups A–G),
> business rules (`BR-*`), and user flows (`F-*`). Per-route/notification/intent/settings indexes
> live in `2026-08-09-finance-surface-registries.md`. Sections that were promoted now carry a
> one-line pointer instead of a copy — the values are stated in exactly one place on purpose.

---

## 1. Executive summary

The finalized design is no longer "a calculator app with a dashboard". It is a **complete personal
finance system**: 5 tab roots, **61 screens**, 43 of which are drawn as full screen states.

| | Designed | Implemented today | Delta |
|---|---|---|---|
| Tab roots | 5 (Home · Money · Calc · Plan · Insights) | 4 (Home · Calc · Plan · Insights) | **Money tab missing** |
| Screens | 61 | ~14 real + 2 placeholders | **~47 screens** |
| Design tokens | full light/dark set + brand chrome | DhruvNext tokens 1:1 ✅, brand chrome ✗ | brand-chrome layer missing |
| Component library | 11 sections | 55 composables in `:libs:core` | ~12 components missing |
| Tracker data layer | Supabase (auth + 12 tables) | **none** | entire domain missing |

**The single largest gap is not UI — it is that no tracker data exists at all.** `:apps:finance:data`
today holds only calculator concerns (history, currency cache, Gemini). There is no auth, no
Supabase client, no holdings, no transactions. Every screen in groups A, B, C, D, E (live modules),
F and G depends on that layer.

---

## 2. As-is audit — what is actually implemented

### 2.1 Green — matches the final design already

| Area | Evidence |
|---|---|
| **Colour tokens** | [DhruvNextTokens.kt](../../../../../libs/core/src/main/kotlin/com/dhruv/core/ui/theme/DhruvNextTokens.kt) light/dark values are byte-identical to the design's `--c-*` CSS variables (`#F9F9F9/#FFFFFF/#F3F4F6/#E5E7EB/#111827…` light, `#0A0A0A/#1E1E1E/#2C2C2C/#3A3A3A/#F5F5F5…` dark), including `chart1–chart6`, `pos/neg/warn` and their soft variants. |
| **Accent** | `#F05A28` light / `#FF6D3B` dark — matches, and `resolveDhruvNextColors` already supports the Settings accent override. |
| **Typography** | Space Grotesk (4 weights), Inter (3), JetBrains Mono (2) are on disk and wired in `DhruvFont.kt` — exactly the design's three-family system. |
| **Responsive scale** | `calculateDhruvNextResponsiveTokens` gives small/phone/tablet tiers for spacing, radii (card 16 / listGroup 18 / innerTile 14 / pill 26), type and keypad. |
| **Splash** | `SplashScreen.kt` implements the logo-spin → wordmark-zoom → hand-off-to-app-bar sequence; the design's motion spec (`cubic-bezier(.16,1,.3,1)`, ~2.2 s, never > 2.5 s) is the target to verify against. |
| **Component library** | 55 composables in `libs/core/.../ui/components` — `NxButton`, `NxCard`, `NxTextField`, `NxTopBar`, `BottomBar`, `SegmentedRow`, `Stepper`, `SwitchRow`, `SearchField`, `ProgressRing`, `TrendSparkline`, `BarChart`, `AllocationStackedBar`, `ConsentGateScaffold`, `EmptyStateCard`, `NumericKeypad`, `AskPill`, brand marks, etc. |
| **Fault isolation** | Every feature route already goes through `FeatureHost`; flags in `platform/feature-flags/dhruv-finance.json`. |
| **Web parity** | `web/src/shared/components/` mirrors the same token names and component set (NxButton/NxCard/DonutChart/AreaChart/FeatureHost) — the design's "same tokens, desktop expression" holds. |

### 2.2 Amber — exists but is a placeholder

| Screen | File | Design expects |
|---|---|---|
| Home tab | [HomeScreen.kt](../../../app/src/main/java/com/dhruv/finance/app/ui/home/HomeScreen.kt) — built in 001-net-worth-tracker Phase 7, superseding the placeholder `DashboardScreen.kt` this row originally named | **01 HOME**: greeting + date line, net-worth hero with trend sparkline and ▲/▼% delta, 4 quick-action tiles, "Upcoming" list (EMI auto-debit, card bill), one-line status (Phase 10) |
| Insights tab | `EmptyStateCard("Insights lands once expense tracking ships")` in `MainActivity` | **F1 Monthly summary** root + F2–F5 |
| Plan tab | `PlanLauncher` — 4 calculator tiles only | **E1 PLAN ROOT (REVISED)**: *live modules on top* (Budgets, Goals, Debt payoff, Insurance, Retirement), calculators demoted to a strip **below** |
| Notifications | `NotifScreen.kt` stub | **B2**: grouped Today/Earlier, budget overrun · EMI due · renewal · rate alert, "Mark all read" |
| Profile | `ProfileScreen.kt` stub | Settings sub-tree per route map |

### 2.3 Red — designed, entirely absent

| Group | Screens | Notes |
|---|---|---|
| **A · Onboarding** | A2 Sign-in · A3 DPDP consent · A4 Empty start | No auth of any kind exists. |
| **B · Home extras** | B3 Global search | Cross-entity search (holdings/txns/goals/policies). |
| **C · Net worth** | C1–C7 (7 screens) | The tracker core. `networth` flag exists, nothing behind it. |
| **D · Money** | D1–D9 (9 screens) | **Entire 5th tab.** |
| **E · Plan (live)** | E2–E9 (8 screens) | Budgets, goals, debt payoff, insurance, retirement. |
| **F · Insights** | F1–F5 (5 screens) | Statements: cashflow, P&L, balance sheet, reports/export. |
| **G · Automation** | G1–G3 (3 screens) | SMS parsing, review queue, AA consent. |

### 2.4 Component gap → **moved to the global design system**

The component library is cross-app and now lives in `platform/DESIGN-SYSTEM.md` §5 (ADR-0030):
§5.1 built · §5.2 planned batches · §5.3 built-but-narrower-than-the-design.

Since this spec was first written: **B1** (`NxFab`, `SkeletonBlock`, `SyncStatusChip`, `NxTopBar`)
and **B5** (signed-out/offline/not-configured trio) shipped. The **2026-08-09 reconciliation** —
which re-read the Claude Design Component Library card-by-card, where this spec's original list came
from a headings-only pass — found the gap set was undercounted and added **B6** (form: select,
textarea, removable chip), **B7** (feedback: status badge, spinner, info banner), **B8** (`NxTabs`,
distinct from `SegmentedRow`), **B9** (`SelectionSheet`), plus §5.3 extensions to `NxButton`
(sizes/loading/block), `NxTextField` (**error state + helper text** — blocks every validated form
here: C4, C5, D2, D3), `CountBadge` and `Chip`/`Pill`.

Per-batch Finance screen mapping is in the implementation plan §3.2.

---

## 3. Decisions this design forces (must be resolved before build)

### D-1 · Navigation: **5 tab roots, not 4** — supersedes ADR-0024 §1
The route map states **"5 ROOTS · 61 SCREENS"**: `Home · Money · Calc · Plan · Insights`.
ADR-0024 (accepted, and the shape currently in `MainActivity`/`TabKey`) draws **4**: Home · Calc ·
Plan · Insights, with money movement unowned. The design also **revises the Plan root** — live
planning modules first, calculators demoted to a strip.
**Recommendation:** adopt the design as-drawn; write **ADR-0027** superseding ADR-0024 §1.
`TabKey` gains `MONEY` between `HOME` and `CALC`.

### D-2 · Brand chrome is a second, theme-invariant palette → **resolved, now global**
Accepted as **ADR-0028** and implemented as `DhruvBrand` (`libs/core/.../ui/theme/DhruvBrandColors.kt`).
Values and usage rules now live in `platform/DESIGN-SYSTEM.md` §1 — not restated here.

Finance-specific consequence only: the deliberately-dark hero screens are **C3, D2, D7, E5, E9, F3,
G3** — those render on brand chrome regardless of the user's theme.

### D-3 · Logo directions are locked → **resolved, now global**
Accepted and shipped (1a monoline = monochrome/notification layer · 1b solid duotone = app mark ·
1c orbit tile = launcher). Full spec in `platform/DESIGN-SYSTEM.md` §4; the adaptive-icon and
notification assets were regenerated from 1c/1a in the Phase 0 pass. No Finance-specific residue.

### D-4 · The Home "financial health score" is dropped
DhruvNext §8 left "score out of 100 has no data spec" open. The final **01 HOME** has no score —
it shows greeting, net-worth hero, quick actions, upcoming. `FinancialHealthRing` in `:libs:core`
is therefore **repurposed**, not deleted: it becomes E2's budget pace ring and F1's savings-rate ring.

### D-5 · `history` disambiguation
`history` = **calculator result history** (Calc tab top bar, Room-local, offline). Transaction
history is **D1 Ledger**, and per-record audit trail is **D4**. Route ids must not collide:
keep `calc/history`, use `money/ledger` and `money/txn/{id}`.

### D-6 · Offline posture per tab
Route map: **"Calc › Offline · Room-local"**. Calc + Converters keep working with no session.
Everything under Home/Money/Plan-live/Insights requires a session and network — signed-out,
offline and not-configured are **first-class UI states** (ADR-0014), not error dialogs. A2 offers
**"Use offline — calculators only"** as an explicit path.

---

## 4. Navigation contract (binding)

```
Home        › Net worth › Assets by sector › Holding detail › Add valuation › Add/edit holding
            › Liabilities › Liability detail › Notifications › Search › Settings
            › Categories·Accounts·Cards › Custom fields › Currency·FY·Alerts
            › Security·Privacy·Trash › Automation › Profile
Money       › Ledger (day-grouped) › Quick add (sheet) › Full transaction form › Transaction detail
            › Filter (sheet) › Accounts › Account detail › Credit cards › Card detail
            › Card statement › Categories › Recurring
Calc        › Keypad › History (top bar)                                   [offline · Room-local]
Plan        › Budgets › Budget detail › Goals › Goal detail › Debt payoff › Insurance
            › Policy detail › Retirement › Calculators (Loan · SIP · Tax · Everyday)
Insights    › Monthly summary › Cashflow statement › Profit & loss › Balance sheet
            › Reports & export › Ask (Gemini)
```

**Rules N1–N7** (tab roots have no back arrow, one parent per screen, sheets dismiss down, forms
confirm on discard, …) are **global navigation law** — they apply to any Dhruv app's shell and now
live in `platform/DESIGN-SYSTEM.md` §6. They remain acceptance criteria here; the QA catalog's
`NAV-*` rows cite those same ids.

**Finance presentation classes:** bottom sheets = C5 add valuation, D2 quick add, D5 filter,
consent, app switcher. Full-screen modal (close ✕, not back ←) = C4 add/edit holding, D3 full form,
G3 AA consent. Bare full-frame, no chrome = splash, A2, A4-entry.

**Per-route index** (FeatureHost key · presentation · consent · phase) lives in
`2026-08-09-finance-surface-registries.md` §1 — this section is the shape, that is the checklist.

---

## 5. Functional specification by group

Money is **integer paise** (`Long`) everywhere in the tracker (ADR-0014 §4). `BigDecimal` stays in
the calculator/projection engines only.

### Group A — Launch, sign-in, consent, empty start

| ID | Screen | Functional requirements |
|---|---|---|
| **00** | Splash | Brand sequence ≤ 2.5 s; shows app version + build. Hands off the moment the shell is ready. |
| **A2** | Sign-in | **Google only** ("one account, every device"). Copy: *"Every rupee you own, in one place."* Secondary action **"Use offline — calculators only"** enters the app with Calc + converters and every tracker surface in a signed-out state. Terms/Privacy links. Consent is asked **separately, next** — never bundled into sign-in. |
| **A3** | DPDP consent | **Itemised, granular, revocable.** Four independent switches, each with a plain-language scope statement: (1) *Sync my financial records* — required for tracker; (2) *Read transaction SMS* — parsed on device, user approves each; (3) *Ask Dhruv about my money* — anonymised summaries only, never account numbers; (4) data-retention/erasure block. Header: *"Nothing syncs until you switch it on. Calculators always work offline."* Every switch must be **persisted and revocable from Settings** — not in-memory. |
| **A4** | Empty start | Day one, exactly two tasks: **1 Add your first account**, **2 Record what you own**. Plus an escape hatch: **Import a CSV** (bank statement or old spreadsheet). No dashboard chrome, no zeros-everywhere dashboard. |

**Acceptance:** no network call of any kind may precede A3's relevant switch being on. Declining
sync leaves the app fully usable in calculators-only mode.

### Group B — Home

| ID | Screen | Functional requirements |
|---|---|---|
| **01** | Home (tab root) | Time-of-day greeting + name; date line + one-line state ("everything on track"). **NET WORTH** hero: value in lakh-crore short form (`₹18.42L`), ▲/▼ % delta, area sparkline. Four quick actions (Loan EMI · SIP · Currency · GST) that cross-navigate. **UPCOMING**: dated obligations with amount and source (Home loan EMI · Auto-debit · 5 Aug · ₹42,110; Credit card bill · Due · 12 Aug). Floating **Ask** pill. |
| **B2** | Notifications | Grouped `TODAY` / `EARLIER`. Types: budget overrun (`Dining is 112% of budget — ₹9,400 of ₹8,400 with 9 days left`), EMI due, policy renewal, price/rate alert, recurring-posted digest. "Mark all read". Each row deep-links to its subject. |
| **B3** | Global search | One field across **transactions, holdings, policies, goals**. Result counts per type as filter chips (`All 7 · Holdings 2 · Transactions 4 · Goals 1`), grouped results with entity-appropriate secondary lines. |

### Group C — Net worth

| ID | Screen | Functional requirements |
|---|---|---|
| **C1** | Net worth by sector | Donut + **ranked** legend. Centre: `NET`, value, delta. Two sub-totals: `ASSETS`, `LIABILITIES`. Legend rows: sector name, enum tag, count/qty, value, share %. Tapping a sector filters holdings. Primary action: **Add holding**. |
| **C2** | Assets | Grouped by sector with per-sector subtotal; per-holding sparkline, last-updated date, value and % change. Filter chips (All/Funds/Stocks/Gold). Search + filter in top bar. FAB add. |
| **C3** | Holding detail | Value + % + sector + `LAST VALUED <date>`. Trend chart with 3M/6M/1Y/All range chips. Three stats: **INVESTED · GAIN · XIRR**. **VALUATION HISTORY** list — every entry dated, sourced (Manual / statement), with delta vs previous. Actions: *Link to goal*, *Update value*. |
| **C4** | Add / edit holding | **I OWN THIS / I OWE THIS** toggle (Asset ↔ Liability). Name; **SECTOR is an enum picker, never free text**: `BANK · MUTUAL_FUND · STOCKS · PROPERTY · GOLD · EPF_PPF · CASH · VEHICLE · CRYPTO · OTHER`. Current value; as-of date; optional notes. Footer states the rule verbatim: *"Saving writes the first valuation entry. Later changes are new entries — history is never overwritten."* |
| **C5** | Add valuation (sheet) | Shows last recorded value + date. Enter value today → **live delta preview** (`Up ₹16,400 (4.8%) since 1 Aug`) before commit. Date + source picker. |
| **C6** | Liabilities | **Outstanding, not original.** Three stats: `TOTAL OUTSTANDING · MONTHLY OUTGO · DEBT-FREE BY`. Grouped by type (`HOME_LOAN`, `CAR_LOAN`, `CREDIT_CARD`, `BNPL`) with rate, EMI, payoff progress (`84 of 180 paid`). |
| **C7** | Liability detail | Amortisation donut (principal paid vs interest paid vs left). Rate, EMI + debit day, tenure remaining, linked account, collateral. **Prepay insight**: *"Prepaying ₹2,00,000 now would save ₹4.86L in interest and end the loan 31 months early"* — hands off to the EMI calculator. Recent payments with principal/interest split. Actions: *Record payment*, *Plan prepayment*. |

**Business rules:**
- **BR-C1** Valuations are **append-only**. No update path exists for a valuation row. Corrections =
  soft-delete + append (ADR-0014 §5).
- **BR-C2** Creating a holding writes its first valuation row atomically.
- **BR-C3** Sector/liability-type enums persist as TEXT and are **append-only** — never rename a
  shipped constant.
- **BR-C4** Net worth = Σ latest asset valuations − Σ latest liability outstandings, computed
  server-side or from the latest-valuation view, never from a stale client cache.

### Group D — Money (new tab)

| ID | Screen | Functional requirements |
|---|---|---|
| **D1** | Ledger (tab root) | Month selector in the title. Pinned month summary: `INCOME · EXPENSE · SAVED %`. **Day-grouped** rows with a per-day net when it matters; each row = icon, payee/description, category, account, signed amount. Search + filter. FAB quick-add. |
| **D2** | Quick add (sheet) | **Amount first**, category and account **pre-guessed**, target **3 taps**. Type segmented (Expense/Income/Transfer). Inline numeric keypad with a date key. Optional note + camera. |
| **D3** | Full transaction form | Every field: type, amount, category, account, date+time, payee, note, receipt, split, **make it recurring** (with schedule), **link to a goal**. Delete in top bar. Confirm-on-discard (N4). |
| **D4** | Transaction detail | Read-first. Amount, payee, datetime, cleared state. Category, account, **budget impact** (`Groceries · 68% used`), note, receipt attachment. **HISTORY / audit trail** at the bottom (`Created manually on this device`, `Category changed Shopping → Groceries`). Actions: *Duplicate*, *Make recurring*. |
| **D5** | Filter (sheet) | Type, category (multi + "+14 more"), amount range, account. **Result count updates live** (`Show 23 results`). **Save as view**. Reset. |
| **D6** | Accounts | `SPENDABLE NOW` = bank + cash + wallet only. Groups: `BANK`, `CASH/WALLET`, and **`CREDIT — OWED, NOT HELD`** (negative balances, limit, due date, utilisation %). Cash shows reconciliation staleness (`Reconciled 28 Jul · needs check`). Footnote that automatic balance refresh arrives with account linking. |
| **D7** | Account detail | Balance + type + masked number + primary badge. Balance-trend area chart. `IN` / `OUT` for the month. **Reconciliation banner** when stale, with *Fix*. Recent activity showing running balance after each row. Actions: *Reconcile*, *Add transaction*. |
| **D8** | Categories | Tabs Expense/Income with counts. Rows: icon, name, sub-category count or budget, spend, share %. Special rows: `Investment · Excluded from spend`, `Uncategorised · N need a category`. Footnote states the rule: *"Renaming keeps history. Merging moves every transaction and cannot be undone."* |
| **D9** | Recurring | Banner: *N entries need review* → review queue. `MONTHLY IN` / `MONTHLY OUT`. **NEXT 30 DAYS** dated list (monthly/yearly, auto-debit vs variable amount). `PAUSED` section. |

**Business rules:**
- **BR-D1** Three types: `EXPENSE`, `INCOME`, `TRANSFER`. **Transfers are never spend** — excluded
  from expense totals, budgets and category shares, and reported separately (see F2 "MOVED, NOT SPENT").
- **BR-D2** Credit-card accounts hold **negative** balances and are excluded from "spendable now".
- **BR-D3** Category rename preserves history. **Merge is irreversible** and must be confirmed with
  a danger dialog stating the transaction count moved.
- **BR-D4** Recurring templates never post silently into the ledger — they post into the **review
  queue** (G2) and require acceptance.
- **BR-D5** Every mutation appends to the transaction's audit trail (D4 HISTORY).

### Group E — Plan

| ID | Screen | Functional requirements |
|---|---|---|
| **E1** | Plan root (**revised**) | `THIS MONTH`: Budgets (spend of budget, N over), Goals (N active, saved of target). `LONG RUN`: Debt payoff, Insurance, Retirement. `CALCULATORS` strip **below**: Loan · SIP · Tax · Everyday. |
| **E2** | Budgets | Pace ring: `% used`, amount left, of total, days remaining, and an explicit pace statement (*"Spending 14% faster than the month"*). Per-category bars with a **month-position marker**; bars past it are ahead of pace. **Over-budget is stated, not implied** (*"Over by ₹1,000 with 9 days left"*). |
| **E3** | Budget detail | Spent vs budget with over/under amount and days left. **Recovery insight** (*"Two more meals out at your average of ₹780 would put you ₹2,560 over"*). Last-6-months bars. Recent transactions. Actions: *Raise budget*, *Alert me at 80%*. |
| **E4** | Goals | `SAVED TOWARDS GOALS` of target, N active. Per goal: %, saved of target, target date, status (`on track` / `needs ₹18,400/mo to hit Dec 2029` / `no funding linked yet`). Footnote states the rule: *"A goal's progress is the value of the holdings you link to it. Nothing is moved or locked."* |
| **E5** | Goal detail | Ring %, saved of target, on-track date. `STILL NEEDED · PER MONTH · MONTHS LEFT`. **FUNDED BY** — the linked holdings, with partial earmarks (`56 g earmarked`). Projection chart. Contribution insight. Action: *Link another holding*, *Add a contribution*. |
| **E6** | Debt payoff | **Avalanche / Snowball** toggle. `DEBT-FREE BY · INTEREST SAVED · MONTHS SAVED · EXTRA PER MONTH`. **PAY IN THIS ORDER** — ranked list with APR and projected clear date. The trade-off is **shown, not hidden**: *"Snowball clears the smallest balance first — 6 months slower here, ₹41,800 more interest, but two wins in year one."* |
| **E7** | Insurance | Renewal banner with days remaining and lapse consequence. `LIFE COVER` vs `RULE OF THUMB` with the formula stated (**10× annual income + outstanding loans**) and the shortfall named. Grouped LIFE / HEALTH with renewal date, premium, sum assured, floater/member info. `GAPS` section naming uncovered risks. |
| **E8** | Policy detail | Sum assured, premium due date. Type, policy number, premium + frequency, cover-until (+ age), **nominee** (name, relation, %), riders. `DOCUMENTS` (policy PDF, receipts) + Add. `PREMIUMS PAID` history. Actions: *Remind me*, *Mark as paid*. |
| **E9** | Retirement | Scenario segmented (**Base / Optimistic / Cautious**). `PROJECTED CORPUS AT 60`, % of target, target, shortfall. Corpus projection chart. Gap insight naming the required monthly figure. **ASSUMPTIONS on the same screen as the answer**: retire-at age, monthly spend today, inflation %, pre- and post-retirement return %, life expectancy. Action: *Save this scenario*. |

**Business rules:**
- **BR-E1** Goal progress = Σ current value of **linked holdings** (whole or earmarked fraction).
  Linking never moves, locks or duplicates money.
- **BR-E2** Budget period is a calendar month; pace = elapsed-days fraction of the period.
- **BR-E3** Categories excluded from spend (e.g. `Investment`) are excluded from budgets too.
- **BR-E4** Every projection screen must display its assumptions on the same screen (E9), and every
  AI/derived insight is labelled as such.

### Group F — Insights

| ID | Screen | Functional requirements |
|---|---|---|
| **F1** | Monthly summary (tab root) | **Savings rate first** (ring, %). `INCOME · EXPENSE · SURPLUS`. Three statement shortcuts (Cashflow · P&L · Balance sheet). `WHERE IT WENT` — top categories with **change vs last month** (`+31%`, `flat`). Comparative insight vs the 12-month average. Month selector + export. |
| **F2** | Cashflow statement | `OPENING BALANCE → MONEY IN (itemised) → MONEY OUT (itemised) → MOVED, NOT SPENT (transfers/SIP) → NET CHANGE → CLOSING BALANCE`, **reconciled on screen**. Footnote: transfers between your own accounts are listed separately so they never inflate spend. |
| **F3** | Profit & loss | Month vs **same month last year**. Columns: `LINE · <month> · % INC · YOY`. Income lines then expense lines, each with subtotal rows, ending at net surplus. |
| **F4** | Balance sheet | Position **as at a date**. Columns: `SECTOR · <date> · Δ 1 MO`. Assets by sector → total; liabilities by type → total; **Net worth**. Footnote flags self-valued items. |
| **F5** | Reports & export | Period picker: `Month / Quarter / FY / Custom` + date range. Statement list (Cashflow, P&L, Balance sheet, Category breakdown, More: Investment returns XIRR, tax summary). **Read on screen first, export second** (CSV/PDF). |

### Group G — Automation (last phase)

| ID | Screen | Functional requirements |
|---|---|---|
| **G1** | Automation hub | One switch per source, **each states what it reads**: Bank SMS alerts (bank senders only, parsed on device, N this month), Account aggregator (RBI-licensed — `COMING SOON`), Price feeds (gold/silver/currency), Recurring templates. Header rule: *"Every source below only suggests. You approve each entry before it becomes part of your records."* `RULES YOU HAVE TAUGHT IT` with applied counts. |
| **G2** | Review queue | Header: *"Read from bank SMS on this device. Nothing here is in your records yet."* **Suggested rows are dashed until accepted.** Per row: raw source text, timestamp, account, amount, suggested category (editable). Unparseable rows say so (*"Could not tell what this was — Pick a category"*). **Duplicate detection** callout. Swipe or tap to Accept/Ignore; *Accept all*. |
| **G3** | AA consent | **Scope, duration and purpose stated before consent**, per screen title *"Link an account"*. Modal with ✕. |

**Business rules:**
- **BR-G1** No automated source ever writes directly to the ledger. Every suggestion passes through
  G2 and an explicit user action.
- **BR-G2** SMS parsing happens **on device**; raw SMS never leaves the device.
- **BR-G3** A learned rule is user-visible, counted, and revocable.

---

## 6. Key user flows

### F-1 · First run (cold install → live net worth)
```
Splash (00) → Sign-in (A2)
   ├─ "Continue with Google"        → DPDP consent (A3) → Empty start (A4)
   │        ├─ Add first account    → D6 add-account form → back to A4
   │        ├─ Record what you own  → C4 add holding (writes first valuation) → C1
   │        └─ Import a CSV         → import mapper → C1/D1
   └─ "Use offline — calculators only" → shell with Calc + Plan-calculators live,
                                          Home/Money/Insights in signed-out state
```
Exit criterion for A4: at least one account **or** one holding exists → Home (01) becomes the landing tab.

### F-2 · Record what you own, then keep it current
```
Home (01) → Net worth (C1) → Assets (C2) → Holding detail (C3)
                                              → Update value (C5 sheet)
                                                   → live delta preview → Record valuation
                                                   → appends row, C3 chart + XIRR recompute
C1 → Add holding (C4 modal) → Save → first valuation written → C1 refreshed
```

### F-3 · Daily money entry
```
Money (D1) → FAB → Quick add (D2 sheet)
   amount → (category pre-guessed) → (account pre-guessed) → Save     [3 taps]
   → row appears in today's group, month summary + budget impact update
   → "More options" → Full form (D3) for receipt / split / recurring / goal link
```

### F-4 · Budget breach → recovery
```
Notification (B2 "Dining is 112% of budget")
   → Budget detail (E3) → recovery insight → "Raise budget" | "Alert me at 80%"
   → or Ledger filtered to that category (D1 + D5)
```

### F-5 · Goal funding
```
Plan (E1) → Goals (E4) → Goal detail (E5) → "Link another holding"
   → holding picker (whole | earmark qty) → progress recomputes from holding value
   (no money moved — BR-E1)
```

### F-6 · Month-end review
```
Insights (F1) → Cashflow (F2) → P&L (F3) → Balance sheet (F4)
   → Reports (F5) → period → export CSV/PDF
```

### F-7 · Automation approval
```
Settings → Automation (G1) → enable "Bank SMS alerts" (permission + consent gate)
   → SMS parsed on device → suggestions land in Review queue (G2)
   → Accept (with category) → becomes a real transaction in D1, audit trail says "from SMS"
   → Ignore → discarded, optionally teaches a rule
```

### F-8 · Consent withdrawal / erasure (DPDP)
```
Settings → Privacy → toggle any A3 consent off
   → dependent surfaces immediately degrade to signed-out/disabled state
Settings → Privacy → "Delete my data"    → hard-deletes all tracker rows
                   → "Delete my account" → delete_my_account() security-definer fn
```

---

## 7. Non-functional requirements

Kept here in full because the QA catalog's `NFR-00x` rows cite these ids as Finance acceptance
criteria. The **Source** column marks which are inherited design-system law (defined once in
`platform/DESIGN-SYSTEM.md`, restated here only as the acceptance handle) versus Finance-specific.

| ID | Requirement | Source |
|---|---|---|
| NFR-1 | **DPDP**: no off-device call before the corresponding A3 switch is on; consent persisted + revocable; erasure within 7 days (in-app, immediate). | Finance (ADR-0014 §7) |
| NFR-2 | **Fault isolation**: every route wrapped in `FeatureHost`; a feature crash never blanks the shell. | global — PLATFORM.md §4 |
| NFR-3 | **Money precision**: integer paise end-to-end; no float in any tracker path. | Finance (ADR-0014 §4) |
| NFR-4 | **Offline states**: signed-out / offline / not-configured are designed states on every network-backed screen — never a spinner that never resolves. | global — DESIGN-SYSTEM §7 |
| NFR-5 | **Theming**: every screen renders light and dark from the same tokens; zero feature-local hex, dp or sp literals (all through `DhruvNextColors` / `DhruvNextSpacing` / `DhruvNextType`). | global — DESIGN-SYSTEM §1–§3 |
| NFR-6 | **Accessibility**: 4.5:1 text contrast, ≥48 dp touch targets, content descriptions on every icon-only action, tabular numerals for money. | global — DESIGN-SYSTEM §9 |
| NFR-7 | **Motion**: `cubic-bezier(.16,1,.3,1)` as the standard easing; splash ≤ 2.5 s; charts animate in once, not on every recomposition. | global — DESIGN-SYSTEM §8 |
| NFR-8 | **Performance**: tab switch < 100 ms; ledger list virtualised; charts drawn from pre-aggregated data, never computed on the main thread. | Finance |
| NFR-9 | **Security**: no secrets in repo/APK; Supabase anon key via the existing `.env` secrets plugin; CA-level pinning (ISRG Root X1/X2); session tokens only in encrypted DataStore. | Finance (ADR-0014 §6) |
| NFR-10 | **Coverage**: `./gradlew regressionCheck` stays green; the global line floor ratchets up at each phase checkpoint, never ahead of landed tests. | repo-wide (ADR-0013) |

---

## 8. Open items (need the maintainer's call, do not block phase 0–1)

1. **CSV import mapper** (A4) — no design exists for the column-mapping step. Needs its own spec.
2. **Card statement** screen (route map lists it under Money; not drawn).
2a. **Credit cards (list) and Card detail** screens (route map §4 lists `Credit cards › Card detail
    › Card statement` under Money, alongside D1–D9; only the D-prefixed screens got IDs. Credit
    cards likely fold into D6/D7's existing CREDIT_CARD account rows — or need their own C-Dx IDs
    and requirements. Found missing during the 2026-08-09 doc re-validation pass; flagged here
    rather than silently built ad hoc when Phase 3 reaches it.
3. **Custom fields** (route map, under Settings; not drawn).
4. **Ask (Gemini)** sits under Insights in the route map but the pill floats on Home/Plan/Insights —
   confirm the owner tab for the back stack.
5. **Multi-currency** — the whole design is INR-only (`₹` hardcoded in every value). R5's
   multi-currency decision must state whether tracker values are INR-only by design.
6. **XIRR definition** (C3) — needs the cashflow set it is computed over specified (ADR reserved).
7. **Cross-device consent sync** (A3) — consent state (`requiresConsent` toggles) is per-device only
   today: `SettingsRepository`'s DataStore flag lives on-device, nothing pushes it to or reads it
   from Supabase. Sign in on a second device and A3 asks again / Settings shows no prior consent,
   even though `auth.users` and tracker rows are already shared via the Google SSO session (ADR-
   0031). Deliberately deferred — sign-in itself was the immediate priority (2026-08-15); needs its
   own design pass (a synced `user_consent` table? last-write-wins per §5 ADR-0004's HLC-LWW, or
   is consent intentionally device-local — e.g. a shared device shouldn't inherit another device's
   consent?) before building. Do not build ad hoc when a future phase touches Settings/A3.

---

## 9. Traceability

| Design artefact | Consumed as |
|---|---|
| `Dhruv Android Screens.dc.html` | §4 route shape, §5 screen requirements, §6 flows — **this doc** |
| `Dhruv Brand & Theme.dc.html` | brand chrome + tokens → promoted to `platform/DESIGN-SYSTEM.md` §1–§3 |
| `Dhruv Component Library.dc.html` | component library → promoted to `platform/DESIGN-SYSTEM.md` §5 |
| `Dhruv Launch & Logo.dc.html` | logo directions + motion spec → promoted to `platform/DESIGN-SYSTEM.md` §4, §8 |
| `Dhruv Web App.dc.html` | web parity → `platform/DESIGN-SYSTEM.md` §12 |

The promotion (ADR-0030) is why four of the five artefacts now resolve to the platform doc: they
described the design *system*, which is cross-app. Only the screens file is Finance product content.
| `support.js` | generated dc-runtime; no design content |
