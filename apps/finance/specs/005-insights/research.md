# Phase 0 Research: Insights (Phase 5)

**Feature**: `apps/finance/specs/005-insights/` | **Date**: 2026-08-22

Resolves every unknown in `plan.md`'s Technical Context. Four entries (R1, R4, R11, R12) **correct or
extend** `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` and
must be written back into its Phase 5 section as a dated note when this feature is implemented — the
same discipline Phase 4's plan applied to its five corrections.

---

## R1 — Reporting is parameterised SQL functions, not the views the implementation plan named

**Decision.** Six `security invoker` SQL functions in the `finance` schema, called over PostgREST's
`rpc/` path: `report_period_summary(p_from, p_to)`, `report_cashflow(p_from, p_to)`,
`report_pnl(p_from, p_to, p_prior_from, p_prior_to)`, `report_balance_sheet(p_as_of)`,
`report_category_breakdown(p_from, p_to)`, and — in 5f only — `report_investment_returns` and
`report_tax_summary`. **Not** `v_cashflow` / `v_pnl` / `v_balance_sheet`.

**Rationale.** The implementation plan §5.4 lists those three as views, written 2026-08-08. A
Postgres view takes no parameters. That was survivable while every Insights figure was month-grained,
which is what the design drew — a month selector on the root. The 2026-08-22 clarification session
changed the shape underneath it: the period model now spans month, quarter, financial year **and an
arbitrary day-grained custom range** (spec FR-023, FR-025), and the balance sheet takes an **arbitrary
as-at date** (FR-017). Neither is expressible as a parameterless view. The intent behind naming views
— *keep aggregation on the server, keep the client dumb* (NFR-8) — is fully preserved by functions and
would have been **violated** by the alternative below.

**Alternatives considered.**
- *Month-grain views summed client-side.* Reuses `v_month_summary`/`v_category_spend` and needs no new
  SQL. Rejected on two counts: a custom range starting on the 12th cannot be assembled from monthly
  rows at all, so the feature would silently be wrong rather than merely limited; and summing twelve
  rows on the client to draw a financial-year statement is the client-side reduction NFR-8 exists to
  prevent, whatever its size.
- *One view per period kind* (`v_cashflow_month`, `v_cashflow_quarter`, `v_cashflow_fy`). Rejected:
  still cannot express a custom range, and triples the surface that must agree with itself.
- *Client-side computation from raw transactions.* Rejected outright — NFR-8, and it would make
  reconciliation a client bug rather than a server guarantee.

---

## R2 — `PeriodResolver` lives at `data/reporting/`, a sibling of `tracker/`

**Decision.** Period arithmetic — month/quarter boundaries, the April–March financial year, custom
range validation, and the prior-year shift — is one pure Kotlin object at
`apps/finance/data/src/main/java/com/dhruv/finance/data/reporting/PeriodResolver.kt`, outside
`tracker/`.

**Rationale.** Exactly the placement Phase 4 chose for its four engines, for the same reason: it is
not a tracker write path, and keeping non-write logic out of `tracker/` keeps
`checkTrackerMoneyPrecision`'s deliberately blunt regex blunt. One resolver, not one per screen, is
what makes "the period carries" (FR-039) a property of the code rather than a convention five
ViewModels have to honour independently. Placing it in `:data` rather than `:libs:core` keeps
`:libs:core` internally dependency-free and free of Finance domain concepts (Article III).

**Alternatives considered.**
- *In `:libs:core`.* Rejected: an Indian financial year is Finance domain knowledge, not a design
  primitive, and `:libs:core` is shared with apps that will never have one.
- *In the feature module.* Rejected: the repository needs it too, to build the `rpc` arguments, and a
  feature module cannot be a dependency of `:data`.
- *In SQL, as a helper function.* Rejected: it would put period semantics in two languages, and the
  client must be able to state the resolved range on screen (FR-016's "state the comparison range")
  without a round trip.

---

## R3 — Every reporting function is `security invoker`

**Decision.** All six (later eight) functions are declared `security invoker`. None is
`security definer`.

**Rationale.** They read RLS-protected tables. A definer function runs as its owner and bypasses RLS
entirely — every user would receive every user's statements. The failure is invisible to the tests
this feature otherwise has: a cross-user statement still reconciles perfectly, so `SIG-BR-001..003`
would all pass. The two definer functions already in this repo are definer for an argued reason
(ADR-0029 §5 — `delete_my_account()` deletes the caller's own `auth.users` row, which needs privilege
the caller lacks). Reporting needs no privilege the caller lacks, so it gets none.

**Recorded because it is a plausible wrong fix.** If a reporting function ever returns empty or
permission-denied during development, the tempting one-word change is `definer`. It is the wrong fix
and it is a data breach. The right fix is a missing `grant execute` or a missing `grant usage on
schema finance` (ADR-0033 decision 4 — custom schemas need explicit grants; `public`'s legacy
auto-exposure does not apply here). The Sec step for each SQL-bearing sub-phase asserts a second
user's rows never appear.

---

## R4 — As-at position derives from "latest valuation on or before the date"; agreement with net worth is a test, not a refactor

**Decision.** `report_balance_sheet(p_as_of)` selects, per holding, the most recent non-deleted
`valuations` row with `as_of <= p_as_of` (`DISTINCT ON (holding_id) … ORDER BY holding_id, as_of DESC,
created_at DESC`), joins `holdings` and `liabilities_meta`, and groups assets by `sector` and
liabilities by `liability_type`. Phase 2's `v_latest_valuation` is **not** refactored to share this
logic. Instead, an equivalence test asserts that `report_balance_sheet(today)` produces the same net
worth as `v_net_worth_by_sector`.

**Rationale.** The new function is `v_latest_valuation` generalised with a date bound, so the
tempting move is to rewrite the shipped view on top of a shared helper. That edits a Phase 2 object
that Home and the net-worth screen already depend on, to serve a phase that reads it — the wrong
direction of risk. The spec already **requires** the two to agree (FR-020, SC-002), so the duplication
is guarded by an assertion that must hold anyway. If the two ever diverge, the test names it
immediately, which is what a refactor would have been trying to guarantee.

**Consequence worth stating.** Append-only valuations are what make an arbitrary past date derivable
at all — there is no stored snapshot and none is added. A correction (soft-delete plus append,
Phase 2's FR-004) therefore changes a *past* balance sheet when it is re-read. That is correct and is
what the spec's append-only assumption already says; it is recorded here because "last month's balance
sheet changed" reads like a bug to anyone who has not read that assumption.

**Alternatives considered.**
- *Refactor `v_latest_valuation` onto a shared date-bounded helper.* Rejected as above — a Phase 5
  change to a Phase 2 object with live consumers, for tidiness.
- *Store a monthly position snapshot.* Rejected: a stored derived figure goes stale on the next
  correction and is later read as a recorded fact. Same reasoning Phase 4 used to refuse storing a
  projected corpus.

---

## R5 — Export serialises the already-rendered statement model; CSV and PDF need no dependency

**Decision.** Both formats are produced from the **same in-memory `Statement` model the screen is
currently displaying** — never from a fresh query. CSV is string building. PDF is
`android.graphics.pdf.PdfDocument` (platform API, no dependency), typeset against the design system's
§11 mapping of the type hierarchy to PDF text sizes. The destination is the platform's
`ACTION_CREATE_DOCUMENT` picker; the app writes to the returned URI and manages no export library of
its own. Generation runs off the main thread.

**Rationale.** FR-029 requires the file to equal the screen. Re-querying at export time makes that a
property to test for and a race to lose — the period could have changed, a correction could have
landed mid-flow. Serialising the object already on screen makes it structurally true, so the test
confirms a guarantee rather than sampling for a defect. No dependency is needed for either format,
which keeps Article XI clean and adds nothing to the APK.

**Alternatives considered.**
- *Re-query at export.* Rejected as above; it is the direct cause of the "export covered a different
  period" edge case the spec had to enumerate.
- *A PDF library (iText, PdfBox-Android).* Rejected: a new dependency on an AGP 9 toolchain that has
  already rejected three libraries (Hilt, Kover, supabase-kt), to typeset a table the platform API
  can typeset.
- *Writing to app-private storage and offering a share sheet.* Rejected: the spec's assumption is a
  user-chosen destination, and the artifact viewer's own sandbox rules aside, a private-then-share
  flow leaves copies the user did not ask for.

---

## R6 — The balance-sheet date override is local ViewModel state, not part of the shared period

**Decision.** `InsightsPeriodStore` (one Koin single, feature-scoped) holds the `ReportingPeriod` and
nothing else. `BalanceSheetViewModel` holds `dateOverride: LocalDate?` privately, collects the period
flow, and clears the override whenever the period changes. The rendered date is
`dateOverride ?: period.end`.

**Rationale.** FR-039 names the override as the **sole** exception to period carry. Putting it in the
shared store would make the exception structural and expose it to four screens that must never see
it — and the first bug would be a cashflow statement quietly rendering at an overridden date. Keeping
it private to the one ViewModel that owns the concept means the exception cannot leak, and the reset
rule (FR-017: override survives while the period is unchanged, discarded when it changes) is one
`collect` in one place with one test.

**Alternatives considered.**
- *A second field on the shared store.* Rejected as above.
- *Nav-argument-only, no retained state.* Rejected: FR-017 requires the override to survive leaving
  and returning to the screen while the period is unchanged, which an argument-only design loses.

---

## R7 — Zero genuinely new `:libs:core` components

**Decision.** This feature adds no component to `:libs:core`. Verified by symbol search against
`libs/core/src/main`: `PeriodChipRow`, `ProgressRing`, `MoneyText`, `StatDeltaChip`, `TrendSparkline`,
`CategoryBarRow`, `SectionLabel`, `ListGroup`, `ListGroupRow`, `NxCard`, `NxTopBar`, `DhruvModalSheet`,
`SkeletonBlock` and the full NFR-4 state trio all exist today. A statement is `ListGroup` +
`ListGroupRow` with a subtotal row — not a new table component.

**Rationale.** Article VI: extend before adding, and nothing enters the design system's built list
before the code exists. `PeriodChipRow`'s own KDoc already names Insights month/quarter/FY as its
intended consumer, so the period selector is the component being used as designed rather than
re-invented.

**Owed by earlier phases, not by this one.** `DonutChart` + ranked legend (batch B3), `NxTabs` (B8)
and `DateRangeSheet` (B2) are unbuilt and are needed here — by the category breakdown, the statement
tabs and the custom range picker respectively. They belong to the phases that already owe them
(design system §5.2). If Insights somehow reaches implementation before they land, they move into 5a
and 5e rather than being duplicated locally, which is what the plan's dependency table records.

---

## R8 — What the gating decision record must settle before 5f can start

**Decision.** Sub-phase 5f does not begin until an accepted ADR fixes the investment-returns
calculation. This research entry does **not** decide it — it states what the record has to answer, so
writing it is cheap rather than open-ended.

The record must settle, at minimum:

1. **The cashflow set.** Which movements count as contributions and withdrawals: transactions
   categorised as investment, transfers into accounts holding investments, valuations themselves, or
   an explicit link between a transaction and a holding. Phase 3's `transactions.goal_id` exists but
   there is no holding link, so today the data may not support every candidate answer — that is part
   of what the record must confirm.
2. **The terminal value and its date.** The latest valuation on or before the period end, presumably,
   but stated.
3. **Scope.** Per holding, per sector, or whole portfolio — and whether the screen offers more than
   one.
4. **Unrealised value.** Whether an unsold holding's appreciation is a return, and if so how a holding
   with no purchase transaction recorded is treated.
5. **Sign convention and the no-solution case.** An irregular cashflow set need not admit a unique
   rate; FR-036 already requires the report to say so, but the record must state which conditions
   count as no-solution rather than leaving it to whatever the solver does.

**Rationale.** Functional spec open item §8.6 has recorded XIRR's definition as unspecified since the
design import, with an ADR reserved but never written. The spec's Q1 clarification made building both
"More" reports conditional on that record existing. Enumerating the questions here is the difference
between a blocker and a task.

**Numbering.** The record takes its number from `platform/DECISIONS.md` **at the moment it is
written**, never reserved in advance — three collisions caused by advance reservation are already
recorded in that file's numbering-hygiene notes, the most recent involving documents that sat dormant
while the register advanced. This plan therefore names no number.

---

## R9 — Privacy masking is applied at render; the export path reads the unmasked model

**Decision.** The `Statement` model always carries exact paise. Masking is a rendering decision made
by the money-formatting layer at display time. The export writers consume the model, so an export is
unmasked by construction, and the export sheet states that before the file is written.

**Rationale.** Settings FR (004) requires privacy mode to mask on screen while counts, percentages and
dates stay readable, and explicitly exempts a user-initiated export provided it says so. Masking the
model instead of the render would make the exemption a special case to remember at every call site —
and the failure mode is a user exporting a statement full of dots.

---

## R10 — Six sub-phases, split by the spec's own story priorities

**Decision.** 5a foundation + F1 · 5b cashflow · 5c balance sheet · 5d profit & loss · 5e reports and
export · 5f the two gated reports. Each ends green on `regressionCheck` and ratchets the floor once.

**Rationale.** Three independent reasons, any one of which would justify the split. **Reviewability**:
five screens, six report types, two export formats and a settings entry behind a single checkpoint
produces a diff nobody reviews properly. **The gate**: one of the six report types is blocked on a
decision record that does not exist, and a single-checkpoint phase would hold the other five hostage
to it. **Shippability**: the tab is honest after every sub-phase — after 5a it is a working monthly
summary, not a half-drawn reports screen — which is the property the implementation plan asks of a
phase and which a monolithic Phase 5 would not have.

The boundaries are the spec's user stories in the spec's own priority order, not a fresh
decomposition, so nothing is invented and the traceability from story to sub-phase to QA row is
direct.

**Alternatives considered.**
- *One checkpoint for the whole phase* (as every prior phase did). Rejected for the three reasons
  above; prior phases were not gated on a missing decision and did not carry six report types.
- *Split by layer* (all SQL, then all repositories, then all screens). Rejected: no intermediate state
  ships, which defeats the point — and it is the shape that produces four weeks of work with nothing
  demonstrable.

---

## R11 — Finding: net worth and cashflow measure two different money universes

**Decision.** The balance sheet reports **holdings only**, matching `v_net_worth_by_sector`, because
FR-020 requires its net worth to equal the net-worth screen's. Account balances are not added to it.
This is recorded as a **cross-phase finding, not resolved here.**

**What was found.** Phase 2 defines net worth as the sum of latest asset values minus latest liability
values over `holdings` (001 FR-005) — accounts are not part of it. Phase 3 introduces `accounts` with
`opening_balance_paise` and a derived `v_account_balances`, and 002's spec never mentions net worth at
all. So a user's bank balance contributes to the cashflow statement's opening and closing balance and
to nothing on the balance sheet, unless they have separately recorded that cash as a holding — in
which case the same money is tracked in two places that are never reconciled against each other.

**Consequence for this feature.** F2's closing balance and F4's position are **not the same quantity**
and are not required to agree; each reconciles internally (SC-001, SC-002) and that is all the spec
asks. Stating it here so nobody later reads their disagreement as a defect in Insights, and so the
statements' own copy does not imply an equivalence that does not hold.

**Why it is not fixed here.** Making accounts part of net worth is a change to Phase 2's definition
with consequences for Home, the net-worth screen and every figure derived from it. That belongs to
whoever owns that definition, as its own decision. Insights surfaces the gap because it is the first
feature to display both universes side by side — surfacing it is the correct contribution.

---

## R12 — Tax relevance is a user-set column, deferred into 5f

**Decision.** The tax summary groups by a new nullable `finance.categories.tax_section text` column,
set by the user, default null. The column and the report ship together in 5f.

**Rationale.** FR-037 requires the report to state which categories it treats as tax-relevant **and on
what basis**. A basis the user set is stateable and stays correct when they rename a category, because
Phase 3 made category identity survive renames by design (BR-D3). Deferring it into 5f keeps 5a–5e
free of any alteration to a Phase 3 table, so five of six sub-phases add no schema risk to tables
another feature owns.

**Alternatives considered.**
- *A hardcoded list of category names.* Rejected: unstated, and wrong the first time a user renames or
  translates a category — the exact failure Phase 3's identity-survives-rename decision was designed
  to prevent.
- *An Insights-local mapping table.* Rejected: it would be a second place category meaning lives, and
  the two would drift the first time a category is merged (Phase 3 has a `merge_categories` function
  that would not know about it).
- *Inferring from `categories.kind`.* Rejected: income versus expense says nothing about tax
  treatment.

---

## R13 — Reconciliation is exact by construction; the client divides only for display

**Decision.** Every figure crossing the wire is `bigint`/`Long` paise. The reporting functions return
already-summed paise and the client performs no aggregation. The only client arithmetic is the
savings-rate division and the share/movement percentages, all computed at render from paise and never
stored, transported or round-tripped.

**Rationale.** Article VII, and FR-039's requirement that no rounding drift may make a statement fail
to reconcile. Because sums happen once, in Postgres, over `bigint`, there is no place for drift to
enter — the reconciliation identity holds by integer arithmetic rather than by tolerance. Percentages
are the only lossy values and they are display-only, so a rounded percentage can never feed back into
a total.

**Zero-income case.** The savings rate is a share of income and is undefined when income is zero
(FR-003). It is modelled as an explicit absent value in the domain model, not as `0`, not as a
sentinel, and not as a `Double` NaN — so the screen is forced to handle it and cannot accidentally
render "0%".

---

## R14 — Coverage registration happens in 5a, and the standing `_FEATURES` gap is not silently patched

**Decision.** `:apps:finance:feature:insights` is added to `coveredModules` (root `build.gradle.kts`)
and `"insights"` to `_FEATURES` (`scripts/ci/regression_summary.py`) as **5a setup tasks**. The
`globalLineFloor` is ratcheted once per sub-phase checkpoint, only to the newly measured value.

**Rationale.** Both lists are hand-maintained, so a module absent from them contributes nothing to the
gate and reports as `(other)`. Registering in 5a rather than at the end means five sub-phases of work
are measured as they land instead of arriving as one unmeasured lump.

**Standing gap, re-confirmed and deliberately not fixed here.**
`:apps:finance:feature:onboarding` is in `coveredModules` but missing from `_FEATURES`, so its
coverage has reported as `(other)` since Phase 1. Phase 3's plan reported it, Phase 4's plan reported
it, and this is the third. It is a one-line fix that belongs to nobody in particular, which is exactly
why it has survived three plans — worth raising as its own small change rather than being smuggled
into a reporting feature's diff.