# Spec carve — 5 module specs → 32 feature specs

> **Status: DESIGN, approved 2026-09-07.** Supersedes nothing; it restructures how the remaining
> Finance work is specified, not what that work is. The design-v1 functional spec
> (`2026-08-08-design-v1-final-functional-spec.md`) remains the product definition and is
> unchanged by this document.
>
> **Scope**: `apps/finance/specs/003`, `005`, `006`, `007`, `008` are carved into 24 feature specs;
> 8 further specs are authored fresh to own gaps that today have no owner. Shipped specs `001`,
> `002` and `004` are not carved — but `004`'s 14 unchecked tasks move out of it (see 015).

---

## 1. The problem

Five spec directories hold 778 unbuilt tasks between them:

| spec | total lines | tasks | built |
|---|---|---|---|
| 003 plan-live-modules | 2428 | 169 | 0 |
| 005 insights | 2691 | 199 | 0 |
| 006 search-notifications | 2508 | 146 | 0 |
| 007 automation | 3087 | 145 | 0 |
| 008 money-tab-ux-remediation | 2477 | 119 | 0 |

Each is a *module* spec, not a feature spec. Three consequences:

1. **Nothing ships until the whole module ships.** 003 has no releasable state between task 1 and
   task 169 — its Plan root (US7) is deliberately sequenced last, so the five modules it summarises
   have nowhere to be reached from until the very end.
2. **A spec set at this size does not fit one implementation session.** An agent picking up 007
   loads ~3100 lines before writing a line of code, and re-loads on every context reset.
3. **Incompleteness stays invisible.** 002 shipped with six requirements delivered in name only;
   that only surfaced in a post-ship review, which is why 008 exists at all. Smaller units make the
   gap visible at the gate rather than after release.

---

## 2. Decisions

### D-1 · One feature spec = one vertical slice
A feature spec owns its schema, backend, screens, tests, branch, PR, `regressionCheck` run and
coverage checkpoint. It is not a document slice — it is a shippable slice. Where a slice cannot ship
alone, it is explicitly paired with the sibling it releases with (§4 marks these).

### D-2 · Cut lines follow the FR groups the parents already declare
Each parent's `### Functional Requirements` section is already grouped under bold sub-headings
(`**Budgets**`, `**Review queue (G2)**`, …). Those groups *are* the features; the carve does not
invent boundaries. 005, 006 and 007 additionally declare sub-phases (5a–5f, 6a–6f, 7a–7g) already
described in their own plans as "independently shippable and green on `regressionCheck`" — the carve
materialises seams that were already reasoned about.

### D-3 · Children are carved, never regenerated
`/speckit-specify` run fresh would discard accepted clarification sessions — 007 alone carries 7 Q&A,
five further shaping clarifications and three verified corrections; 005 and 006 carry their own.
Children are produced by slicing the parent's existing text, not by re-deriving it.

### D-4 · One extraction: the alerts pipeline
Alerts today are owned by nobody and implemented by five specs: 006 delivers, 003 stores the
budget-breach preference, 005 stores the monthly-summary preference, 007 builds the entries-waiting
alert, 004 holds the controls. 006's `Raising alerts` / `Opening the app from an alert` / `Controls`
groups plus those scattered preferences become one spec (021). Every other module then *declares an
alert type* and owns nothing else about alerting.

Deliberately **not** extracted, after review: soft-delete/undo (owned instead by 012), cross-cutting
state/precision requirements (restated per child — see D-6), and consent classes (004 already ships
the mechanism).

### D-5 · Flat renumber; parents retired to a pointer index
Children are ordinary `NNN-slug` directories under `apps/finance/specs/`, so branch names,
`SPECIFY_FEATURE_DIRECTORY` and the constitution's Spec-Kit Directory Rule all keep working
unchanged. Each parent's `spec.md` is replaced by a short index naming its children and is otherwise
retired, per the ADR-0030 retirement pattern.

**Numbers equal ship order.** Nothing is built against 009–040 yet, so making the number carry
meaning is free. 009 ships first; 040 ships last.

### D-6 · Strict carve — every parent item is assigned, shared, or explicitly dropped
Research items, data-model rows, contract files and checklist rows go to the child that cites them.
Nothing is duplicated "just in case". Three classes, and the audit gate (§5) enforces that every
parent item lands in exactly one:

| class | meaning |
|---|---|
| **assigned** | exactly one child owns it |
| **shared** | genuinely cross-cutting (state matrix, paise precision, access rules) — replicated verbatim into every sibling, and the sibling list is written down |
| **dropped** | no child cites it; must appear on a reviewed drop list before the parent is deleted |

This is the highest-risk decision in the document: a mis-assignment loses a decision permanently.
§5 exists specifically to make dropping a listed act rather than a silent one.

### D-7 · Carve by explicit FR id, never by id range
Found during extraction: **008's FR ids are non-contiguous and interleaved.** `Correction (US5)`
spans FR-212..FR-228, `Saved views (US6)` is FR-217..FR-218 *inside that span*, and
`Splitting (US8)` is FR-224..FR-227, also inside it. A range-based carve would mis-assign four
requirements without any error. Every child's source slice is therefore an explicit id list.

Two FR groups additionally **split across two children** and must be assigned requirement by
requirement:

- **008 `Navigation (US3, US4)` (FR-208..FR-211)** — US3's requirements go to 011, US4's to 013.
- **005 `Settings and alerts` (FR-046..FR-050)** — the Settings row goes to 018, the
  monthly-summary alert preference to 021.

---

## 3. Gap register

The 32 include 8 specs that are not carved from anything. They exist because a review of the 24
carved features against the design-v1 screen inventory (§5 groups A–G), the navigation contract
(§4), the functional spec's open items (§8) and the phase-readiness record (§5.5, §7) found 16 items
with no owner — several inside specs marked *shipped*.

Screen coverage itself is complete: every ID (`00 A2 A3 A4 01 B2 B3 C1–C7 D1–D9 E1–E9 F1–F5 G1–G3`)
maps to a shipped spec or one of the 32. The gaps are all non-screen.

| id | gap | severity | owner |
|---|---|---|---|
| **G1** | 001's migration and its 3 RLS/RPC verification scripts have never run against live `dhruv-dev`; `dhruv-prod`'s Data API `db_schema` was never patched to include `finance` (ADR-0033 correction, open in `scripts/env/README.md` §1a) | P0 | **010** |
| **G2** | After "Delete my data", device-local Room `alert_log.payload_json` still holds readable rupee amounts for deleted records. Server-side erasure FRs do not touch it | P0 | **021** |
| **G3** | Trash / Recently deleted has no owner. Nav contract lists `Security·Privacy·Trash`; DESIGN-SYSTEM §8 mandates soft-delete **plus a recoverable location**; 001/002/003 all soft-delete rows; 007 disclaims it; readiness §5.2 assigned it to Phase 0b and 0b shipped without it | P1 | **012** |
| **G4** | `daily_rates` notification channel has a real channel, a toggle and a delivery-time picker — and nothing posts to it. 006 disclaims it; the currency module has no such plan | P1 | **021** |
| **G5** | BYO AI key is stored encrypted but never used. `GeminiRepository` is a Koin singleton built from `BuildConfig.GEMINI_API_KEY`; `currentSnapshot()` deliberately skips the encrypted read | P1 | **024** |
| **G6** | No edit-liability screen. C4's I OWN / I OWE toggle covers add only | P1 | **014** |
| **G7** | Profile — nav contract lists it; `ProfileScreen.kt`'s stub route was to retire in 0b and did not | P2 | **015** |
| **G8** | Credit cards / Card detail / Card statement — route map §4 lists them under Money; only D-prefixed screens got IDs. 002 mentions cards 5×, 008 zero times. Folding into D6/D7 is likely but unrecorded | P2 | **032** |
| **G9** | Custom fields (Settings) — nav contract lists it, never drawn, no spec | P2 | **015** |
| **G10** | `font_family` is live but unreachable — `DhruvTheme` branches on it, the preference persists, no row exists anywhere. An FR-003 violation | P2 | decision **009**, row **015** |
| **G11** | Launcher-entry hiding for a disabled module — Calc converter tiles and the Ask pill stay visible | P2 | **024** |
| **G12** | CSV import mapper (A4) — functional spec open item 1, "needs its own spec"; 007 leaves the CTA present-and-disabled | P3 | **040** |
| **G13** | XIRR ADR — and the holding↔transaction link no phase models | blocking | **009** |
| **G14** | Tracker write-semantics ADR — idempotency keys, single-retry, write-then-refetch invalidation, concurrency. "Genuinely new architecture" | blocking | **009** |
| **G15** | Design-v1 scope-reduction ADR — D-3/D-4/D-6 remove drawn surfaces from a finalized design | blocking | **009** |
| **G16** | Cross-device consent sync — consent is device-local; a second device re-asks despite shared `auth.users`. "Do not build ad hoc when a future phase touches Settings/A3" | blocking | **009** |

**Downgraded on review:** recurring entries are not stranded. 002 shipped accept/dismiss for pending
entries (FR-028..FR-031b), so Waves 1–5 are coherent; 033 replaces a working local surface with the
shared queue rather than filling a hole.

---

## 4. The 32 features

Each entry below is written for an agent picking the feature up cold. **Source** is the exact carve
slice. **Depends on** is a hard prerequisite. **Ships with** means the two release together — one
alone is a dead end.

---

### Wave 0 — Unblock

No feature work is safe until both land.

#### `009-decisions-record`
*~12 tasks · authored fresh · depends on nothing*

Four decisions and one design call are blocking downstream work and nobody is assigned to write
them. This spec writes them and nothing else — it ships documents, not screens.

**Owns**
- **G14** Tracker write-semantics ADR: idempotency keys, single-retry policy, write-then-refetch
  invalidation, concurrency posture. Every write path from Wave 1 onward depends on it; ADR-0014
  only ever said "no client-side conflict resolution", which is not a write contract.
- **G13** XIRR ADR: the cashflow set XIRR is computed over, *and* the holding↔transaction link that
  no phase currently models. Unblocks C3's XIRR stat (NW-BR-007) and 039.
- **G15** Design-v1 scope-reduction ADR: D-3/D-4/D-6 removed drawn surfaces from a finalized design;
  amending a finalized design deserves a record.
- **G16** Cross-device consent sync design pass: is consent a synced `user_consent` table under
  HLC-LWW, or intentionally device-local (a shared device should arguably not inherit consent)?
- **G10** The `font_family` DESIGN-SYSTEM call: add a picker, or drop `DhruvFont`'s non-default
  variants. DESIGN-SYSTEM §2 owns this; 015 implements whichever answer lands.

**Done when** each record is written into `platform/DECISIONS.md` (re-checking the highest existing
ADR number at that moment — the register has had three numbering collisions from exactly this gap)
or into `platform/DESIGN-SYSTEM.md`, and every downstream spec citing it resolves.

---

#### `010-db-live-verification`
*~10 tasks · authored fresh · depends on nothing*

The tracker database has never been executed. Everything above it is untested against a real
Postgres, and prod is misconfigured in a way that fails silently rather than loudly.

**Owns**
- **G1** Run 001's Phase 2 migration against live `dhruv-dev`, then the three RLS/RPC verification
  scripts in `supabase/verification/` that have never had credentials.
- Patch `dhruv-prod`'s Data API `db_schema` to include `finance`. ADR-0033's 2026-09-03 correction
  found `dhruv-dev` returning `"db_schema":"public,graphql_public"` — every tracker call 406'd
  despite correct migrations and a correct `Accept-Profile` header. `dhruv-prod` has the same
  default and has never been patched.
- Close the standing item in `scripts/env/README.md` §1a, and add the scripted Management-API check
  ADR-0033's correction names as "a real follow-up, not yet built".

**Done when** a live authenticated request against `finance.holdings` succeeds on both projects and
the drift guard passes.

---

### Wave 1 — Unbreak shipped surfaces

Money tab is live with four P0 usability defects; net worth and settings each carry an open hole. No
new tab is built while a shipped one dead-ends.

**Wave 1 exit: every live surface tells the truth.**

#### `011-money-tab-ux-p1`
*~50 tasks · carved from 008 (US1, US2, US3, US10) · depends on 009*

The four P1 defects found in the post-ship review of 002. Recording money currently dead-ends, a
signed-out person cannot get back in, screens have no back path, and a brand-new account cannot
record anything.

**Source** — 008 FR-201..FR-205 (`Feedback and completion`), FR-206..FR-207 (`Access`), the **US3
subset** of FR-208..FR-211 (`Navigation` — US4's requirements go to 013, see D-7), FR-238..FR-241
(`First run`).

**Owns** completion feedback on every write · sign-in reachable from a signed-out ledger · a back
path and unsaved-changes confirmation on every Money screen including the back gesture · first-run
seeding so a new account can record immediately.

**Done when** the four P1 user stories' acceptance scenarios pass and `regressionCheck` is green.

---

#### `012-trash-and-recovery`
*~25 tasks · authored fresh · depends on 009 · ships before 013*

**G3.** DESIGN-SYSTEM §8 requires soft-delete **plus a recoverable location**. Three specs
soft-delete rows and the recoverable location was never built — 004's T121 has sat unchecked inside
a spec the phase table calls shipped.

**Owns** a single `Settings › Trash` surface listing every soft-deleted entity across holdings,
transactions, accounts, categories and recurring definitions · restore · permanent delete · the
retention window · and telling 001 T053 / 002 T088 / 003 T142 where undo's durable half lives.

**Ships before 013** because 013's correction flow (US5) is what needs somewhere to recover from.

**Done when** every soft-delete in the app has a reachable restore path, and 004 T121 is closed.

---

#### `013-money-tab-ux-p2`
*~40 tasks · carved from 008 (US4, US5, US9) · depends on 012*

The six requirements 002 delivered in name only. There is no edit surface for a transaction, an
account or a recurring rule; correcting a mistake destroys the record; and there is no way to record
when something actually happened.

**Source** — 008 the **US4 subset** of FR-208..FR-211, FR-212..FR-216 and FR-228 (`Correction`,
excluding FR-217..FR-218 and FR-224..FR-227 which belong to 020 — see D-7), FR-229..FR-234
(`When it happened, and proof of it`).

**Owns** edit surfaces for transaction / account / recurring rule · correction that appends to the
audit trail rather than destroying it (BR-D5) · date and time entry · receipt capture · moving
Setup out of the ledger and into Settings.

**Done when** every entity the ledger creates can be edited and corrected without data loss.

---

#### `014-net-worth-residuals`
*~18 tasks · authored fresh · depends on 009 (XIRR ADR)*

Two open items 001 explicitly recorded as "still open, not silently dropped".

**Owns**
- **G6** The edit-liability screen. C4's I OWN / I OWE toggle covers creation; a liability cannot be
  edited afterwards.
- C3's XIRR stat (NW-BR-007), now that 009 has fixed the definition and the holding↔transaction
  link it needs.

**Done when** C3 renders a real XIRR and a liability can be edited.

---

#### `015-settings-residuals`
*~30 tasks · carved from 004's open tasks + fresh · depends on 009 (font call)*

004-settings is recorded as shipped but carries 14 unchecked tasks. One of them is load-bearing:
**T119 enforces the settings-contribution model** — the mechanism by which "every later phase ships
its module's settings entry with the module". Waves 2 onward each ship such an entry, so T119 lands
before the first one does.

**Owns**
- T119 contribution-model enforcement · T120 (**G7** Profile's fate: absorb into Account, keep as a
  route, or delete — the stub route 0b was meant to retire) · T123 `contracts/routes.md` ·
  T124–T126 (declined-item status, surface-registry source-phase column, the `PinEntry` claim from
  design-system batch B2 that the app-lock flow needs) · T130–T136.
- **G9** Custom fields, listed in the nav contract, never drawn.
- **G10** The `font_family` Settings row, implementing whichever answer 009 recorded.

**Excludes** T122 (`alert_log` purge), which belongs to 021 with the store it purges.

**Done when** 004 has no unchecked tasks and its phase-table row is corrected to say the residuals
moved here.

---

### Wave 2 — First planning value

**Wave 2 exit: all five tabs have real content.**

#### `016-plan-root` · **ships with 017**
*~15 tasks · carved from 003 (US7) · depends on 015*

E1 revised: the Plan tab leads with live planning modules and demotes the four calculators to a
strip below. Today the tab is calculators only, so there is no entry point to anything Wave 2–5
builds.

**Source** — 003 FR-001..FR-004 (`Plan root`).

**Owns** the `THIS MONTH` group (budgets, goals summary rows) · the `LONG RUN` group (debt payoff,
insurance, retirement) · the calculator strip · and FR-004's rule that a module with no data says so
rather than showing a zero that reads as real.

**Ships with 017.** A Plan root that says "no budgets yet" with no way to create one is the same
dead-end defect Wave 1 just fixed.

---

#### `017-budgets` · **ships with 016**
*~35 tasks · carved from 003 (US1, US2) · depends on 016, 009*

E2 and E3. The highest-value Plan module and the one that makes the Money tab's data mean something:
spend against an intent, not just a list.

**Source** — 003 FR-005..FR-015 (`Budgets`).

**Owns** a budget amount per category over a calendar month · the pace ring and the explicit pace
statement (spend fraction vs elapsed-day fraction, BR-E2) · per-category bars with a month-position
marker · over-budget stated in words and money, never colour alone · budget detail with the recovery
insight and last-6-months bars · `Raise budget` · the **budget-breach alert preference**, stored here
and *delivered* by 021.

**Note** BR-E3: categories excluded from spend (e.g. `Investment`) are excluded from budgets too.

---

#### `018-insights-monthly`
*~60 tasks · carved from 005 (5a) · depends on 009*

F1 plus the period model the entire Insights tab runs on. Answers "did I actually save this month"
in one screen, and makes the Insights tab non-empty for the first time.

**Source** — 005 FR-001..FR-007 (`Monthly summary (F1)`), FR-039..FR-040 (`Period behaviour across
the tab`), the Settings-row half of FR-046..FR-050 (see D-7). `States, precision and access`
(FR-041..FR-045) is **shared** into this and every other insights child.

**Owns** the one period model every Insights screen shares · savings-rate ring · `INCOME · EXPENSE ·
SURPLUS` · the three statement shortcuts · `WHERE IT WENT` with change vs last month · the
comparative insight against the 12-month average · month selector · the Insights `SettingsContribution`.

**Carries four corrections** the parent recorded and the child must fold in: reporting is
parameterised `finance` functions, not views (a view cannot take a period); as-at position derives
from "latest valuation ≤ date"; holdings and account balances are two different money universes and
are not required to agree; tax relevance is a user-set `categories.tax_section` column.

---

### Wave 3 — Daily-use completion

#### `019-goals`
*~28 tasks · carved from 003 (US3) · depends on 016*

E4 and E5. Goals funded from holdings the user already owns — nothing is moved, locked or
duplicated (BR-E1). Cheap because it reuses net-worth data rather than introducing a money-movement
concept.

**Source** — 003 FR-016..FR-024 (`Goals`).

**Owns** `SAVED TOWARDS GOALS` · per-goal status (`on track` / `needs ₹X/mo` / `no funding linked
yet`) · goal detail with the `FUNDED BY` linked-holdings list including partial earmarks · the
projection chart · `Link another holding`.

---

#### `020-money-tab-ux-p3`
*~29 tasks · carved from 008 (US6, US7, US8) · depends on 013*

The P3 remainder of the 002 post-ship review.

**Source** — 008 FR-217..FR-218 (`Saved views`), FR-219..FR-223 (`Honest states`),
FR-224..FR-227 (`Splitting a payment`), FR-235 (`Record honesty`). Interleaved with 013's ids — see
D-7.

**Owns** saving a filter as a reusable view (D5's `Save as view`, shipped in name only) · the module
reporting its own real state instead of a generic one · splitting one payment across categories
without inventing a parent record that holds a total.

---

#### `021-alerts-pipeline` · **ships with 022**
*~80 tasks · carved from 006 + 003/005/007 preferences · depends on 017, 018*

**D-4's extraction.** One owner for the whole alerting mechanism: the durable store, raising,
delivery, deep-linking and per-type control. Every other module declares an alert type against this
and owns nothing else.

Placed here, not earlier: before budgets and F1 exist there is nothing to alert about. By Wave 3 two
of its alert types have real producers.

**Source** — 006 FR-020..FR-027 (`Raising alerts`), FR-028..FR-031 (`Opening the app from an
alert`), FR-032..FR-035 (`Controls`); 007 FR-036..FR-039 (`Entries-waiting alert`); the
alert-preference half of 005 FR-046..FR-050; 003's budget-breach preference consumer.

**Owns**
- The device-local Room alert store (v5→v6) — an alert is recorded whether or not the OS displayed
  it.
- The five ready alert types: budget threshold, instalment due, policy renewal, value-update
  overdue, monthly summary. Each driven by the preference the producing module already stores.
- **G4** `daily_rates` as a sixth type. 004 shipped its channel, toggle and delivery-time picker;
  nothing posts to it. This is the only `androidx.work` scheduling in the app, so a second scheduler
  for one channel would be waste.
- **G2** Purging `alert_log` on "Delete my data" (004 T122). The payload stores rupee amounts as
  paise in device-local Room and is excluded from `delete_my_data()`, so after erasure the app still
  holds readable amounts for records that no longer exist. Either purge the table in the same action
  or stop storing amounts in the payload.
- Deep-link resolution to the exact subject of each alert. Adds **zero** new `NavTarget` cases — it
  is the first consumer of five existing ones, including the `OpenBudget` 003 added speculatively.

**Flags** `search` and `alerts` are both genuinely absent from
`platform/feature-flags/dhruv-finance.json` — the parent reserved the names but never applied them.

---

#### `022-notification-centre` · **ships with 021**
*~25 tasks · carved from 006 (6b) · depends on 021*

B2. The screen over 021's store — the app's own durable record of everything it has told the user.

**Source** — 006 FR-012..FR-019 (`Notification centre (B2)`).

**Owns** `TODAY` / `EARLIER` grouping · per-row deep-link to its subject · `Mark all read` · read
state.

---

### Wave 4 — Depth

#### `023-global-search`
*~33 tasks · carved from 006 (6a) · depends on 019, 026*

B3. One field across transactions, holdings, policies and goals. Placed here because it needs
content worth searching — ledger, goals and policies all exist by now.

**Source** — 006 FR-001..FR-011 (`Global search (B3)`). `Cross-cutting` (FR-036..FR-038) is
**shared** with 022.

**Owns** the search data seam · result counts per type as filter chips · grouped results with
entity-appropriate secondary lines · the search RPC contract.

**Note** this phase adds **no** Gradle module — B2/B3 belong to `:apps:finance:app`.

---

#### `024-ask-dhruv`
*~35 tasks · authored fresh · depends on 009, 015*

**G5 + G11.** ADR-0024 decision 4 promised Ask Dhruv as real surface area — a `FeatureHost`-wrapped
chat route, a floating pill on Home/Plan/Insights, a route-registry row and its own consent entry —
and no phase ever owned it. `DetailRoute.Ask` exists today as a bridge to the pre-existing
`AssistantScreen`.

**Owns**
- **G5** BYO AI-key consumption. 004 stores the key encrypted with masked display and one-action
  removal; `GeminiRepository` is a Koin singleton built once from `BuildConfig.GEMINI_API_KEY` and
  `currentSnapshot()` deliberately skips the encrypted read. Needs a key-provider indirection so a
  user key is honoured (ADR-0002's override). Until this ships, 004's row is honest only because it
  says "stored, not used".
- The real chat surface, its consent entry, and A3's "Ask Dhruv about my money — anonymised
  summaries only, never account numbers" switch being genuinely enforced.
- **G11** Hiding launcher entry points for a disabled module: the Calc-tab converter tiles and the
  Ask pill.
- Resolving functional-spec open item 4 — Ask sits under Insights in the route map but the pill
  floats on three tabs; the owner tab for the back stack must be stated.

**Constraint** no shared Gemini key is ever embedded in the APK (ADR-0002).

---

#### `025-debt-payoff`
*~22 tasks · carved from 003 (US4) · depends on 016*

E6. Avalanche vs snowball, with the trade-off shown rather than hidden.

**Source** — 003 FR-025..FR-031 (`Debt payoff`).

**Owns** the avalanche/snowball toggle · `DEBT-FREE BY · INTEREST SAVED · MONTHS SAVED · EXTRA PER
MONTH` · the ranked `PAY IN THIS ORDER` list with APR and projected clear date · the honest
comparison statement naming what the slower strategy costs and what it buys.

---

#### `026-insurance`
*~28 tasks · carved from 003 (US5) · depends on 016*

E7 and E8. What is insured, what renews when, and what is missing.

**Source** — 003 FR-032..FR-040 (`Insurance`).

**Owns** the renewal banner with days remaining and the lapse consequence · `LIFE COVER` vs the
stated rule of thumb (10× annual income + outstanding loans) with the shortfall named · LIFE/HEALTH
grouping · policy detail with nominee, riders, documents and premium history · the `GAPS` section ·
the policy-renewal alert preference, delivered by 021.

---

#### `027-insights-cashflow`
*~20 tasks · carved from 005 (5b) · depends on 018*

F2. A cashflow statement that reconciles on screen.

**Source** — 005 FR-008..FR-012 (`Cashflow statement (F2)`) + the **shared** states block.

**Owns** `OPENING BALANCE → MONEY IN → MONEY OUT → MOVED, NOT SPENT → NET CHANGE → CLOSING BALANCE`,
reconciled visibly. BR-D1 governs: transfers are never spend and are reported separately so they
cannot inflate expense totals.

---

#### `028-insights-balance-sheet`
*~22 tasks · carved from 005 (5c) · depends on 018*

F4. Position as at a date.

**Source** — 005 FR-017..FR-022 (`Balance sheet (F4)`) + the **shared** states block.

**Owns** the overridable as-at date · `SECTOR · <date> · Δ 1 MO` columns · assets by sector and
liabilities by type to a net worth line · the footnote flagging self-valued items. As-at position
derives from "latest valuation ≤ date" (parent correction).

---

### Wave 5 — Long-run, reporting, residual surfaces

#### `029-retirement`
*~22 tasks · carved from 003 (US6) · depends on 016*

E9. A projection with its assumptions in plain sight (BR-E4).

**Source** — 003 FR-041..FR-046 (`Retirement`).

**Owns** Base/Optimistic/Cautious scenarios · `PROJECTED CORPUS AT 60` against target with the
shortfall named · the corpus projection chart · the gap insight naming the required monthly figure ·
and the assumptions block on the same screen as the answer (retire-at age, monthly spend today,
inflation, pre- and post-retirement return, life expectancy) · `Save this scenario`.

**Note** `BigDecimal` is correct here — this is a projection engine, not tracker money (ADR-0014 §4).

---

#### `030-insights-pnl`
*~18 tasks · carved from 005 (5d) · depends on 018*

F3. Month against the same month last year.

**Source** — 005 FR-013..FR-016 (`Profit & loss (F3)`) + the **shared** states block.

**Owns** the `LINE · <month> · % INC · YOY` table · income lines then expense lines with subtotals,
ending at net surplus.

---

#### `031-insights-reports-export`
*~35 tasks · carved from 005 (5e) · depends on 027, 028, 030*

F5. Choose a period, read the report, then export it — and move between statements without losing
the period.

**Source** — 005 FR-023..FR-033 (`Reports & export (F5)`) + the **shared** states block. Includes
the parent's US6 (period preserved across statement navigation).

**Owns** the `Month / Quarter / FY / Custom` period picker · the statement list · read-on-screen-first
then export · CSV and PDF output. PDF follows DESIGN-SYSTEM §11: full money format never masked,
footer carries generated date and app version, wordmark only.

---

#### `032-credit-cards`
*~20 tasks · authored fresh · depends on 013*

**G8.** Route map §4 lists `Credit cards › Card detail › Card statement` under Money; only the
D-prefixed screens ever got IDs and requirements. 002 mentions cards five times (D6's `CREDIT —
OWED, NOT HELD` group); 008 mentions them zero times.

**Owns** either the three screens with real requirements, **or** a recorded, accepted descope
folding them into D6/D7's existing `CREDIT_CARD` account rows. Either outcome closes functional-spec
open items 2 and 2a — what is not acceptable is building them ad hoc when a phase happens to reach
them, which is exactly what the open item warns against.

**Constraint** BR-D2: credit-card accounts hold negative balances and are excluded from "spendable
now".

---

### Wave 6 — Automation

The largest block, dependent on everything above. G1–G3 are shell detail routes like Settings and
Ask, so this wave adds **zero** `NavTarget` cases and **one** Gradle module
(`:apps:finance:feature:automation`, in 034).

#### `033-review-queue`
*~35 tasks · carved from 007 (7a) · depends on 013, 021*

G2. The one queue every source feeds. Discharges 002's deferred D9 shared queue and 023's
transactions-to-review.

**Source** — 007 FR-003..FR-012 (`Review queue (G2)`).

**Owns** the queue carrying **two proposal kinds** — proposed transaction and proposed value update ·
suggested rows dashed until accepted · unparseable rows saying so · accept / ignore / accept-all
(which takes confidently-parsed transactions with no duplicate warning only, and reports what it
skipped and why) · the restorable **Ignored** list, never re-proposed.

**Terminology** the spec says *proposal* where the design and QA catalog say *suggestion* — same
entity.

**Note** the proposal store is owned by 002 (`plan.md:38`) and already exists. Only
`finance.automation_rules` is a new table, and it is deliberately mutable, not append-only.

---

#### `034-automation-hub`
*~20 tasks · carved from 007 (7b) · depends on 033*

G1 and G3, plus the Gradle module and the governing rule.

**Source** — 007 FR-001..FR-002 (`The rule that governs everything`, BR-G1 — **shared** into every
Wave 6 sibling), FR-013..FR-019 (`Automation hub (G1)`), FR-040..FR-042 (`Account-linking consent
(G3)`), and the hub's half of FR-049..FR-051 (`Privacy, consent and erasure`).

**Owns** one switch per source, each stating what it reads · the header rule *"Every source below
only suggests. You approve each entry before it becomes part of your records."* · `RULES YOU HAVE
TAUGHT IT` with applied counts · G3's scope/duration/purpose-before-consent modal · the
`automation` feature flag key, which is genuinely absent from `platform/feature-flags/dhruv-finance.json`
(the name was reserved and never applied) and ships here `enabled: false` · FR-054's correction of
the stale surface-registry line that parks *Recently deleted* under this module (it belongs to 012).

---

#### `035-bank-message-source`
*~30 tasks · carved from 007 (7c) · depends on 034*

The largest single FR group in the parent. On-device bank-SMS parsing behind its own consent class.

**Source** — 007 FR-020..FR-028 (`Bank message source`) + the SMS half of FR-049..FR-051.

**Owns** a periodic background scan (~1h, best-effort — open-only reading was rejected because it
would leave the entries-waiting alert with nothing to announce) · parsing bank senders only ·
proposals into 033's queue, never the ledger (BR-G1) · withdrawing consent **freezes** that source's
unreviewed proposals (read-only, delete-all or re-enable) rather than purging them.

**Hard constraints** BR-G2: parsing is on-device and raw SMS never leaves the device. AUT-BR-002
forbids raw message text in any outbound Supabase payload — which is why bank-message proposals stay
**entirely device-local** (Room v6→v7) and 002's reserved `finance.suggestions.raw_text` column stays
permanently unwritten. SMS access uses the platform `ContentResolver`; **zero new dependencies**.

---

#### `036-duplicate-detection`
*~15 tasks · carved from 007 (7d) · depends on 035*

The same spend must not be recorded twice.

**Source** — 007 FR-029..FR-031 (`Duplicate detection`).

**Owns** the match window and criteria · the duplicate callout in the queue · the interaction with
accept-all (which skips anything carrying a duplicate warning).

---

#### `037-learned-rules`
*~15 tasks · carved from 007 (7e) · depends on 033*

Teach it once, stop correcting it.

**Source** — 007 FR-032..FR-035 (`Learned rules`, BR-G3).

**Owns** rule capture from user corrections · applied counts · and BR-G3's requirement that a
learned rule is user-visible, counted and revocable.

---

#### `038-price-feed-sources`
*~20 tasks · carved from 007 (7f) · depends on 033, 021*

Metal and currency values that refresh themselves, with approval.

**Source** — 007 FR-043..FR-048 (`Other sources`).

**Owns** the gold/silver/currency feed · a fetch that arrives as a **proposed value update in the
review queue**, never a direct valuation write — BR-G1 only forbids automated writes to the
*ledger*, so a direct write would have been legal but would have made G1's own header promise false ·
a trigger on a **move past a user-set threshold (default 5%)**, not on a clock.

---

### Wave 7 — Gated / optional

#### `039-insights-more`
*~17 tasks · carved from 005 (5f) · **gated***

F5's "More" reports: investment returns (XIRR) and tax summary.

**Source** — 005 FR-034..FR-038 (`Investment returns and tax summary (F5 "More")`).

**Blocked on** 009's XIRR ADR *and* the holding↔transaction link it defines, which no phase models
today. `invested_paise` gives a simple return; XIRR needs the cashflow set. Do not start before that
record is accepted.

**Note** tax relevance is a user-set `categories.tax_section` column (parent correction).

---

#### `040-csv-import`
*~30 tasks · authored fresh · optional*

**G12.** A4's `Import a CSV` escape hatch. Functional-spec open item 1 states plainly that no design
exists for the column-mapping step and that it needs its own spec; 007 descoped it and leaves the CTA
present-and-disabled with copy naming that it arrives later.

**Owns** the file picker, the column-mapping UI that has no design yet, validation, and the import
landing in 033's review queue rather than writing to the ledger directly (BR-G1).

**Note** this is the one feature in the 32 whose *design* does not exist, not merely its
implementation. Expect a design pass before a spec.

---

## 5. Carve procedure and the audit gate

### 5.1 Procedure, per parent

1. Assign every FR/NFR to exactly one child **by explicit id list** (D-7 — never by range), or mark
   it `shared` with its sibling list.
2. Assign every research item (`R1`…`Rn`), data-model row, contract file and checklist row to the
   child that cites it.
3. Emit each child's `spec.md`, `plan.md`, `research.md`, `data-model.md`, `tasks.md`, `contracts/`
   and `checklists/` from the assigned slices. Task ids renumber `T001…` per child; the parent id is
   preserved in a `was:` column so history stays traceable.
4. Run the audit gate.
5. Retire the parent to a pointer index; run `scripts/ci/doc_link_check.py`.

### 5.2 Audit gate

`scripts/spec/carve_audit.py <parent>` prints four sets and exits non-zero if any is unreviewed:

| set | meaning |
|---|---|
| **unmapped** | parent items no child claimed — the drop list; must be explicitly acknowledged before deletion |
| **double-mapped** | claimed by two children without being declared `shared` |
| **dangling** | a child cites a research/contract id that was not carried over |
| **task delta** | Σ child tasks vs parent tasks |

The parent is not deleted until the unmapped list has been reviewed. This is what makes D-6's "drop
what no child cites" a listed act rather than silent loss.

---

## 6. Blast radius outside `apps/finance/specs/`

| file | change |
|---|---|
| `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §7 (rows 348–351) | phase→spec table repointed from one directory to N children per phase |
| the same table's `004-settings` row | corrected — it is **not** shipped clean; its 14 unchecked tasks moved to 015 |
| `.specify/memory/constitution.md` "Tracking" | still one line, now pointing at the rewritten table |
| `scripts/ci/doc_link_check.py` | retired parent paths become dangling-pointer patterns |
| `platform/feature-flags/dhruv-finance.json` | `search`, `alerts` and `automation` keys are reserved-but-absent; 021 and 034 apply them |
| `FEATURES.md`, `CHANGELOG` | 32 entries where there were 5 |
| branch names | `009-decisions-record` … `040-csv-import`, convention unchanged |

---

## 7. Two orders, deliberately different

**Ship order** is the numbering: 009 → 040, grouped into the waves above.

**Carve order** is dependency order for the documents, and differs:

1. **`021-alerts-pipeline` is carved first** — 017, 018 and 038 all cite it. Writing it later leaves
   dangling references that fail `doc_link_check.py`.
2. Then 003's children, 005's, 006's, 007's, 008's — one parent per reviewable commit.
3. The 8 gap specs (009, 010, 012, 014, 015, 024, 032, 040) are authored fresh, not carved, and can
   proceed in parallel with any of the above.

Each carve is one commit. Each feature is one branch.

---

## 8. How to pick up a feature

1. Read this document's entry for the feature — it names the source slice, dependencies and exit
   criteria.
2. Read `platform/AGENTS.md`'s session bootstrap in order, as always.
3. Read the feature's own `spec.md` and `plan.md`. Do **not** read the retired parent; its pointer
   index exists to send you here, not to be read as a second spec.
4. Follow the constitution's fixed step order: SA → QA → Backend (RED→GREEN→REFACTOR) → Android
   (RED→GREEN→REFACTOR) → QA closure → Sec → checkpoint.
5. Ship it: its own branch, its own PR to `develop`, `regressionCheck` green, coverage floor not
   regressed, every QA scenario row CLOSED or explicitly deferred with a stated reason.

---

## 9. Traceability

Design source: `2026-08-08-design-v1-final-functional-spec.md` (product definition, unchanged) and
`2026-08-08-design-v1-final-implementation-plan.md` §7 (phase table, rewritten by this change).
Gap evidence: `2026-08-23-phase-readiness-architecture-decisions.md` §5.2, §5.5, §7;
`apps/finance/specs/004-settings/tasks.md` T119–T136; `apps/finance/specs/001-net-worth-tracker/`
spec row; ADR-0033's 2026-09-03 correction; functional spec §8 open items 1, 2, 2a, 3, 4, 6, 7.
Governing decisions: ADR-0027 (5 tab roots), ADR-0029 (tracker architecture), ADR-0030 (one design
system; the retire-don't-fork precedent this carve follows), ADR-0032/0033 (schema authorship).