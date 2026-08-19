# Phase 0 Research: Plan Live Modules (Phase 4)

Resolves every unknown in `plan.md`'s Technical Context before design begins. Five entries (R1, R2,
R3, R4, R9) **add to or correct** the design-v1 implementation plan §5.4/§7 rather than merely
restating it, and must be written back there as a dated note when this feature is implemented — the
same discipline Phase 3's research applied to its own two deltas.

---

## R1 — Earmarks are basis points of a holding, not a quantity

**Decision.** `goal_links.earmark_bps int not null` (1–10000, where 10000 means the whole holding),
replacing the implementation plan §5.4's sketched `earmark_qty numeric NULL`.

**Rationale.** The plan's `earmark_qty` and the design's E5 wording ("`56 g earmarked`") both assume
a holding has a **quantity and a unit**. It does not: §5.4's `holdings` row is
`id · user_id · name · kind · sector · notes · created_at · deleted_at`, and `valuations` carries a
`value_paise` only. There is no gram, no unit count, no per-unit price anywhere in the Phase 2
schema. So a quantity earmark cannot be computed against anything today, and inventing a quantity
column here would be silently redesigning Phase 2's table from Phase 4 — exactly the kind of drift
this repo has already had to retire documents over.

A fraction is also what the binding rule actually says. BR-E1 is *"Σ current value of linked
holdings (whole or **earmarked fraction**)"*, and `PLN-UI-005` accepts *"the earmark
quantity/fraction"*. Basis points satisfy both, keep Article VII's integer-money discipline (no
`numeric`, no `Double`), make FR-024's cap a trivial integer comparison (`Σ bps ≤ 10000`), and
survive a holding's value changing — which a quantity earmark also does, but a paise-amount earmark
does not.

**Consequence, stated rather than hidden.** E5 renders "62% of SGB 2029 · ₹1,84,000" where the
design drew "56 g earmarked". The rupee value is exact and live; the gram figure is not renderable
until holdings carry a quantity. When and if Phase 2 (or a later phase) adds `quantity` + `unit` to
`holdings`, the display becomes derivable with **no schema change here** — bps × quantity is the
gram figure. This is a deliberate, reversible narrowing of one label, not a change to the rule.

**Alternatives considered.**
- *Paise-amount earmark* — rejected: a fixed rupee earmark against a holding whose value moves means
  the goal either over-claims after a fall or under-claims after a rise, and BR-E1's whole point is
  that progress tracks the holding.
- *Add `quantity`/`unit` to `holdings` now* — rejected: it is Phase 2's table, Phase 2 is spec'd and
  unimplemented, and a cross-phase schema edit made from a later phase's plan is how two specs start
  disagreeing.
- *Boolean whole-or-nothing links* — rejected: E5 explicitly draws partial earmarks.

---

## R2 — The calculation engines live outside `tracker/`, on purpose

**Decision.** `BudgetPaceEngine`, `DebtPayoffEngine`, `RetirementProjectionEngine` and
`InsuranceCoverEngine` live at `apps/finance/data/src/main/java/com/dhruv/finance/data/planning/` —
a **sibling** of `tracker/`, not a package inside it. They take paise `Long` in and return paise
`Long` out; `BigDecimal` appears only between those boundaries.

**Rationale.** Two constraints meet here and only this placement satisfies both.

1. `checkTrackerMoneyPrecision` (root `build.gradle.kts`, wired into `regressionCheck`) scans
   `apps/finance/data/src/main/**/tracker/**/*.kt` for the bare words `Double` and `Float` and fails
   the build on any hit. It is a regex, not a type check, and its bluntness is the feature — it
   catches a `Double` the moment anyone writes one on a tracker path.
2. A retirement projection needs fractional arithmetic. Inflation, pre- and post-retirement returns,
   and monthly compounding over ~40 years are rates, not amounts. Constitution Article VII already
   anticipates this: *"`BigDecimal` is reserved for pure calculation engines (calculators, retirement
   projection) and never appears on a tracker write path."*

Putting the engines under `tracker/` would have forced either weakening that regex for every future
tracker file, or contorting engine code to avoid a word. Placement costs nothing and keeps both the
guard and the engine honest.

Three further reasons the `:data` module (rather than a feature module or `:libs:core`) is the right
home: `:feature:planning` and `:apps:finance:app` (E1's summary rows) both need `BudgetPaceEngine`,
and `feature → feature` is forbidden, so a feature module cannot host it; `:libs:core` must stay
free of Finance domain concepts (Article III, `core → anything internal` forbidden in spirit); and
`:data` is already where every feature legitimately depends.

**Consequence.** A future "tidy-up" that moves these four files into `tracker/` will break
`regressionCheck` on the retirement engine. That is the intended failure — this entry is the note
explaining why, so the fix is to move them back rather than to relax the guard.

**Alternatives considered.**
- *Keep `BigDecimal` but place under `tracker/`* — works today by accident (the regex matches
  `Double`, not `BigDecimal`), but the first `.toDouble()` or `Math.pow` call breaks it, and
  compounding over a 480-month horizon will reach for one.
- *A new `:apps:finance:engines` Gradle module* — rejected on the same reasoning §5.1 uses to reject
  `:tracker-data`: ArchUnit already gives the isolation, and a fifth module buys a second Koin graph
  and a dependency edge on three feature modules for nothing.

---

## R3 — Annual income and today's monthly spend are derived from the ledger, not stored

**Decision.** Two inputs the design names but never sources — the annual income behind FR-035's
rule-of-thumb cover, and the "monthly spend today" assumption in E9 — come from Phase 3's ledger via
one new view, `finance.v_annual_income` (trailing-12-month income and expense, per user). Insurance
consumes it read-only and states its basis on screen. Retirement seeds its assumption from it and
lets the user override; **the override is stored inside `retirement_scenarios.assumptions`**, which
already exists to hold exactly that.

**Rationale.** These are the only two figures in the feature with no obvious owner. Storing them as
profile fields would create a second source of truth that silently goes stale — the user's income
changes and the insurance shortfall keeps quoting last year's. Deriving them means the number is
always current and needs no new table, no settings screen, and no migration.

The honesty requirement in the spec ("the formula must state what it could not use") is what makes
this safe with sparse data: with fewer than 12 months of ledger the view still returns what it has,
annualised, and E7 says so in words rather than quietly computing against a partial year. This is
the same posture as the app's other derived figures — visible basis, labelled as derived.

Retirement differs deliberately: a projection's assumptions must be *editable*, because the user is
modelling a future they choose, not reporting a past they recorded. So retirement seeds from the
derived figure and stores the user's chosen value; insurance does not, because a rule of thumb about
your actual income is not a scenario.

**Alternatives considered.**
- *A `profile` table with `annual_income_paise`* — rejected: new table, new form, and it goes stale
  the moment income changes.
- *Ask the user on E7 each time* — rejected: a rule-of-thumb card that demands input before it can
  say anything is not a card, it is a form.

---

## R4 — The table list is the same six; three columns and three views are additions

**Decision.** The six tables in the implementation plan §7 Phase 4 step 1 are correct and complete:
`budgets`, `goals`, `goal_links`, `policies`, `policy_premiums`, `retirement_scenarios`. Three
column-level additions to §5.4's sketch, and three views §5.4 does not list, are needed:

| Addition | Where | Why |
|---|---|---|
| `earmark_bps int` replaces `earmark_qty numeric` | `goal_links` | R1 |
| `remind_days_before int null` | `policies` | FR-040's "Remind me" must persist something; §5.4's policy row has no reminder column |
| `documents jsonb` | `policies` | FR-038 attaches documents; §5.4's policy row has no document reference (see R6 for what goes in it) |
| `v_budget_status` | new view | budget consumption without the client summing a ledger (NFR-8) |
| `v_goal_progress` | new view | BR-E1's Σ across links × latest valuation, server-side |
| `v_annual_income` | new view | R3 |

No seventh table. `policy_documents` was considered and rejected in R6; a debt table was considered
and rejected because Phase 2's `liabilities_meta` already carries `rate_bps`, `emi_paise` and the
balance the payoff engine needs — creating a parallel debt record would let a user's net worth and
their payoff plan disagree about the same loan, which is precisely the failure the single-source
rule exists to prevent.

**Consequence.** The implementation plan §5.4's table needs these three cell corrections written
back as a dated note, so the next reader of that document does not re-derive them.

---

## R5 — One genuinely new component, three extensions, zero parallel components

**Decision.** Verified by symbol search against `libs/core/src/main` (the method
`platform/DESIGN-SYSTEM.md` §13 prescribes), not assumed from the design file:

| Need | Status today | This phase |
|---|---|---|
| Pace ring with month-position marker (E2) | `PaceRing` **absent**; `ProgressRing`, `CountdownRing`, `FinancialHealthRing` present in `Rings.kt` | **Build `PaceRing`** on `FinancialHealthRing`'s base, as impl plan §3.2 already directs |
| Per-category bar with a month-position marker (E2) | `CategoryBarRow` present, no marker | **Extend** it with an optional marker fraction |
| Goal ring (E4, E5) | `ProgressRing` present | reuse as-is |
| Last-6-months bars (E3) | `BarChart` present | reuse as-is |
| Goal and corpus projection charts (E5, E9) | `TrendSparkline` present | reuse as-is |
| Scenario selector (E9) | `SegmentedRow` present | reuse as-is — the design draws segmented under Actions and `NxTabs` under Navigation as two distinct controls; this is the segmented one |
| Renewal banner, lapse status (E7) | `StatusBadge`, `InfoBanner` **absent** (batch B7) | shared with Phase 3 — whichever phase lands first builds them |
| Derived-insight labelling (FR-047, `PLN-BR-005`) | `SmartInsightCard`, `AiInsightStrip` present | **Extend** `SmartInsightCard` to be the single carrier — see below |

**The derived-insight decision is the one that matters.** FR-047 and `PLN-BR-005` require *every*
derived statement in this feature to be visibly labelled: E3's recovery insight, E6's
avalanche/snowball trade-off, E4's goal status, E5's contribution insight, E9's gap insight. That is
five screens across three Gradle modules. Implemented per-screen it will be satisfied four times and
missed once, and the miss will be invisible in review because each screen looks fine in isolation.
So it gets one mechanism: `SmartInsightCard` carries the marker, every derived sentence renders
through it, and a single test asserts the marker is present rather than five tests each asserting
their own screen. Article VI decides *which* component: `SmartInsightCard` and `AiInsightStrip`
already exist for exactly this purpose, so this is an extension, not a new `DerivedInsightLabel`.

**Alternatives considered.** A new `DerivedInsightLabel` primitive — rejected under Article VI: two
components saying "this was computed" is the fragmentation the library exists to prevent. A
per-screen convention documented in the module standard — rejected: a convention is not a gate.

---

## R6 — Policy documents are device-local this phase

**Decision.** `policies.documents jsonb` holds an array of `{ label, path, added_at }` describing
files on the device. No blob storage, no Supabase Storage bucket, no `policy_documents` table.

**Rationale.** This is the same decision Phase 3 took for transaction receipts (that phase's R6), for
the same reasons, and taking a different one here would mean two attachment mechanisms in one app
before either has a user. Supabase Storage is a new surface with its own RLS model, its own consent
question (a policy PDF is materially more sensitive than a receipt), and its own erasure obligation
under NFR-1 — none of which this phase has budget to design properly, and all of which are cheaper
to design once, for both attachment kinds, than twice.

**Consequence, stated plainly.** A policy PDF does not follow the user to a new device this phase.
E8 says so where documents are listed, in the same voice the accounts screen uses to say balances
are user-maintained. When cloud attachments are designed, both receipts and policy documents migrate
together — and `documents jsonb` becomes the migration's read source, so nothing is lost.

**Alternatives considered.** A `policy_documents` table with a storage-object reference — rejected as
premature: it commits the schema to a storage design that does not exist yet.

---

## R7 — Budget pace is computed client-side from a server-aggregated spend figure

**Decision.** `v_budget_status` returns, per budget, the period, the budget amount and the spent
amount. The **pace comparison itself** — elapsed-day fraction, ahead/behind, the "X% faster than the
month" sentence — is computed in `BudgetPaceEngine` on the client.

**Rationale.** The NFR-8 rule is that the client never sums a ledger to draw a screen, and the view
honours it: one aggregated row per budget, however many transactions are behind it. But pace also
depends on **today's date in the user's timezone**, which the server does not reliably know, and on
presentation-tier decisions (rounding, phrasing thresholds) that belong with the sentence they
produce. Splitting it this way also makes `PLN-BR-002` — the row demanding the pace figure match
`N/M` across at least three fixtures — a pure JVM test with no database in it.

Two correctness properties come free from the view rather than from client discipline: transfers
never consume a budget (FR-010), and categories marked `excluded_from_spend` contribute nothing
(FR-010, BR-E3) — both are already guaranteed inside Phase 3's `v_category_spend`, which
`v_budget_status` builds on. Re-implementing either exclusion in Kotlin would create a second place
for them to be wrong.

**Alternatives considered.** Computing pace in SQL — rejected on the timezone and phrasing grounds
above. Summing transactions client-side — rejected, straight NFR-8 violation.

---

## R8 — Alert thresholds and reminders are stored now; delivery is Phase 6

**Decision.** `budgets.alert_pct` and `policies.remind_days_before` are written by this phase and
read by nothing in this phase. `PLN-FLOW-003`'s chained flow is tested from the budget detail
onward; its notification-initiated leg is **deferred with this reason recorded**, not closed.

**Rationale.** "Alert me at 80%" (FR-014) and "Remind me" (FR-040) are actions the design puts on
E3 and E8, but the surface that would deliver them is screen B2, scheduled for Phase 6. Two
honest options existed: ship the buttons storing a preference nothing consumes, or omit them. Storing
wins — the user's choice is captured from the moment the screen exists, Phase 6 reads a column that
is already populated with real preferences rather than launching against an empty table, and the
alternative (adding the column in Phase 6) means a migration plus a "set your alerts now" prompt
against budgets the user configured months earlier.

**Consequence for `/speckit-tasks`.** The deferral must appear as an explicit task outcome, not a
silently skipped row — constitution step 7 allows a scenario row to be *"CLOSED or explicitly
deferred with a stated reason"*, and this is the stated reason. A row quietly left unticked at the
checkpoint fails the gate; a row deferred with this citation does not.

---

## R9 — Both projections terminate by construction

**Decision.** `DebtPayoffEngine` simulates month by month with a hard cap of **600 months (50
years)**. A debt still outstanding at the cap is reported as *not clearing under the current plan*
(FR-031). `RetirementProjectionEngine` is bounded by life expectancy, which is a user input, and is
additionally clamped to a maximum horizon so a mistyped expectancy cannot produce an unbounded loop.

**Rationale.** An amortisation loop is `while (balance > 0)`, and that loop does not terminate when
the minimum payment is at or below the monthly interest accrual — a real situation (a maxed credit
card at a high rate with a minimum-due of 1%) that a user can genuinely be in. Without a cap, the
screen hangs. With a cap and no message, the debt silently vanishes from the plan, which is worse
than hanging because it is wrong quietly. So the cap produces a *statement*, and FR-031 exists
because of it.

Tie-breaks are part of the same determinism concern: FR-026 requires a stable order, so
highest-interest-first breaks a rate tie by balance descending then by id, and smallest-balance-first
breaks a balance tie by rate descending then by id. Falling back to id last makes the order
reproducible across openings, which is what makes `PLN-BR-004` a testable assertion rather than a
flaky one.

**Alternatives considered.** Detecting the non-amortising case analytically before simulating
(`min_payment ≤ balance × monthly_rate`) — this is *also* implemented, as the explanation the user
reads; the cap stays as the backstop, because the analytic check covers the standing case and not
every path through a multi-debt snowball where payments are reallocated as earlier debts clear.

---

## R10 — Test strategy: engines are the cheap half, and should be tested first

**Decision.** Same layering as Phase 3 — fakes and MockWebServer at the HTTP boundary for
repositories, Turbine for ViewModel state, SQL-layer rules verified against the dev Supabase project
at the Sec step. What is different here: the four engines are pure functions over plain data, so
**they get golden-value fixture tests with no Android, no coroutines and no fakes at all**, and those
tests are written before anything else in the phase.

**Rationale.** `RET-BR-001` already asks for exactly this ("matching golden-value fixtures"), and
`PLN-BR-002`/`PLN-BR-004`/`INS-BR-001` are the same shape. This is the highest-value RED step
available in the whole feature: the engines carry every correctness-critical rule (pace, ordering,
projection, rule-of-thumb), they are the code most likely to be wrong in a way no reviewer would
spot, and they are testable in milliseconds with no infrastructure. It is also the highest-yield
coverage in the phase — pure logic with no Compose in it is the only kind of code the JVM gate sees
completely.

**Consequence.** `tasks.md` should front-load the four engines' tests. If the phase runs short, the
engines are done and correct and only the screens are outstanding — which is the right thing to be
outstanding, because a screen over a wrong projection is worse than no screen.