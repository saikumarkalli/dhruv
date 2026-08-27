# Quickstart: Insights (Phase 5)

How to run and validate this feature end to end. Structured by sub-phase — each section is the exit
check for that sub-phase's checkpoint, so a reviewer can confirm one slice without running the whole
phase.

Shapes and column names are not repeated here: see [`data-model.md`](data-model.md),
[`contracts/reporting-functions.md`](contracts/reporting-functions.md) and
[`contracts/routes.md`](contracts/routes.md).

---

## Prerequisites

| Requirement | Why |
|---|---|
| **Phases 2 and 3 shipped** | Insights reads holdings, valuations, liabilities, accounts, categories and transactions and adds none of them. Nothing below runs against an empty schema |
| `JAVA_HOME` = Android Studio JBR (JDK 17+) | Standing project requirement, AGP 9 |
| Supabase CLI linked to `dhruv-dev` | The reporting functions and every equivalence assertion run there. Never the dashboard SQL editor (ADR-0032 decision 3 — an uncommitted statement voids the drift guard) |
| A signed-in test account with consent granted | Every route is `requiresConsent`; with consent off the correct result is a designed state, not data |
| A **second** test account with its own data | Required for the RLS assertion at every Sec step. Without it, `security invoker` is a claim rather than a verified property |

**Fixture data.** Each sub-phase needs at least three independent period fixtures (SC-001, SC-002,
SC-004 all say "at least three"). Seed them through `supabase/seed.sql` — dev-only, never part of a
`db push` (ADR-0032 decision 3). One fixture must include a transfer between the user's own accounts,
one must include a split transaction, and one must span a financial-year boundary.

---

## Commands

```bash
# whole gate — what CI runs, and the exit condition for every sub-phase checkpoint
./gradlew regressionCheck

# this feature's unit tests only, while iterating
./gradlew :apps:finance:feature:insights:testDebugUnitTest
./gradlew :apps:finance:data:testDebugUnitTest --tests "com.dhruv.finance.data.reporting.*"

# one scenario
./gradlew :apps:finance:data:testDebugUnitTest --tests "*PeriodResolverTest*"

# build and install
./gradlew :apps:finance:app:assembleDebug

# schema: author declaratively, generate the migration, never hand-write it
supabase db diff -f insights_phase5
supabase db reset            # local; loads seed.sql
python scripts/db/gen_schema_docs.py --check
```

---

## 5a — Foundation + monthly summary (F1)

**Setup**: apply the migration adding `report_period_summary` and `report_category_breakdown`; add the
`insights` flag; register the module in `coveredModules` and `_FEATURES`.

| Check | Expected |
|---|---|
| Open the Insights tab | The monthly summary renders. `NotConfiguredCard` no longer appears when the backend is reachable |
| Read the savings rate | Equals surplus ÷ income for the period, recomputed by hand from the three headline figures |
| Set up a fixture with **zero income** | The rate states it cannot be computed. **Not** "0%", not a dash, not a blank ring |
| Include a transfer between own accounts in the period | It moves none of income, expense, surplus, savings rate, or any category figure |
| Open "where it went" across two months | Each category's movement recomputes from the two periods' totals; a near-zero movement reads "flat", not "+0.3%" |
| Read the comparative insight with **fewer than 12 months** of data | It names how many months it actually averaged, and is visibly labelled as derived |
| Change the period | Every figure on the screen recomputes |
| Sign out, then go offline, then turn consent off | Three distinct designed states. No spinner that never resolves in any of them |
| Turn the `insights` flag off | `FeatureDisabledCard`, whole tab |
| Open Settings | One Insights entry, holding the monthly-summary preference. Set it, restart the app, reopen — still set |
| Disable the module, reopen Settings | No Insights entry at all. Not greyed out |
| **Equivalence** (Sec step) | `report_period_summary(month)` equals `v_month_summary`; `report_category_breakdown(month)` equals `v_category_spend` |
| **RLS** (Sec step) | Signed in as the second account, no row from the first appears in either function |

**Exit**: `regressionCheck` green, floor ratcheted once, `SIG-UI-001` and `SIG-UI-002` closed, the new
settings QA rows written and closed.

---

## 5b — Cashflow statement (F2)

| Check | Expected |
|---|---|
| Open cashflow for a fixture period | Six sections in order: opening, money in, money out, moved-not-spent, net change, closing |
| Add the displayed figures by hand | opening + money in − money out − net(moved-not-spent) = closing, **exactly**, on all three fixtures |
| Confirm the identity is not a tautology | Verify in SQL that opening and closing are each computed from the ledger independently — not closing = opening + net (see `contracts/reporting-functions.md`) |
| Check a transfer and an excluded-category row | Both appear in moved-not-spent; neither appears in money out |
| Read each section | Its lines sum to its stated subtotal |
| Read the footnote | States that transfers between the user's own accounts are listed separately so they never inflate spend |
| Open a period with no transactions | Empty state, not a statement of zeroes |
| **RLS** (Sec step) | Second account sees only its own |

**Exit**: `SIG-BR-001` closed on ≥3 period fixtures.

---

## 5c — Balance sheet (F4)

| Check | Expected |
|---|---|
| Open the balance sheet with a period selected | Renders as at that period's **end date**, with no date chosen by the user |
| Add the figures by hand | assets total − liabilities total = displayed net worth, exactly, on ≥3 date fixtures |
| Compare against the net-worth screen for the same date | Identical (FR-020) |
| Override the date | Every figure and every change column re-anchors to the new date; the screen states both the inherited period and the date shown |
| Go to another Insights screen and back | The other screen is on the original period; the override is still in force |
| Change the period anywhere | The override is discarded; the balance sheet renders at the new period's end |
| Try a future date | Rejected or corrected with a stated reason |
| Try a date before the first record | States that no position existed. Not a net worth of zero |
| Check a user-valued holding | The self-valued footnote appears and identifies it |
| **Equivalence** (Sec step) | `report_balance_sheet(today)` net worth equals `v_net_worth_by_sector` net worth |
| **RLS** (Sec step) | Second account sees only its own |

**Exit**: `SIG-BR-003` closed, the new date-override QA rows written and closed.

**Known and expected**: this statement's position and the cashflow statement's closing balance are
different quantities and need not agree — see research R11. Do not raise it as a defect here.

---

## 5d — Profit & loss (F3)

| Check | Expected |
|---|---|
| Open P&L on a month with a prior-year counterpart | Comparison column is that same calendar month one year earlier — **not** the preceding month |
| Read the layout | Income lines first, then expense lines, each group with a subtotal, ending at net surplus |
| Sum each group's lines | Equals its displayed subtotal; income subtotal − expense subtotal = net surplus |
| Read any line | Name, amount, share of that period's income, year-on-year movement |
| Select a **quarter**, then a financial year | Renders that period; does not degrade to a month, does not disable itself |
| Select a custom range | The comparison range is stated on screen — especially across a leap day |
| Open a period with no prior-year data | Comparison states unavailable. Not zero, not a movement figure |
| Rename a category between the two periods | Still one line; its prior amount still appears |
| **RLS** (Sec step) | Second account sees only its own |

**Exit**: `SIG-BR-002` closed, verified on both a month and a non-month period.

---

## 5e — Reports & export (F5 core)

| Check | Expected |
|---|---|
| Open Reports, cycle the period picker | Month, quarter, financial year, custom all selectable; the selected period is stated |
| Select financial year | Runs April to March |
| Enter a custom range with end before start | Rejected or corrected with a stated reason |
| Open each report in the list | All present data for the same selected period |
| Try to export before a report has rendered | Export is not offered |
| Export a rendered report | File's totals and line items equal what was on screen |
| Export the **balance sheet with an override in force** | The file covers the date shown, not the period end |
| Read the exported file's header/footer | Names the period, the generation date and the app version |
| Turn privacy mode on, then export | On-screen amounts masked; **the file is unmasked**, and the export sheet said so before writing |
| Cancel the file picker mid-export | Reported as failed, no partial file presented as complete, action still available |
| Deny storage / pick an unwritable location | Same — a stated failure, retryable |
| Open Settings | The Insights entry now also carries the statement-export control |

**The chain walk** (`SIG-FLOW-003`, manual — this is the whole point of the tab):

1. Open the monthly summary and select a period.
2. Walk F1 → F2 → F3 → F4 → F5 without re-selecting anything.
3. At each step, confirm the period on screen is the one selected in step 1.
4. Export at F5. Confirm the file covers that period.
5. Walk it again with a balance-sheet date override in force. Confirm only F4 differs, that F4 says so,
   and that every other screen is untouched.

**Exit**: `SIG-FLOW-001`, `SIG-FLOW-002`, `SIG-FLOW-003` closed.

---

## 5f — Investment returns + tax summary (GATED)

**Do not start this sub-phase until an accepted decision record fixes the investment-returns
calculation.** Research R8 lists the five questions it must answer. Without it there is no correct
implementation to write and no test that could distinguish one from a wrong one.

| Check | Expected |
|---|---|
| Open the investment-returns report | States which money movements it computed over, over what date span, and by what method |
| Recompute by hand from exactly those movements | Matches, on ≥3 fixtures |
| Open a period with no investment movements | States that a return cannot be computed, and why. Not zero, not a dash |
| Open the tax summary | States which categories it treats as tax-relevant and on what basis; labelled a summary of the user's own records, never advice |
| Mark a category tax-relevant, then rename it | The line survives the rename (identity, not name) |
| Trace every tax-summary figure | Every one comes from a category the report named. None from outside that set |
| Export both | Same rules as 5e |

**Exit**: the new QA rows for both reports written and closed; the decision record referenced by id in
the closing note.

---

## Whole-phase exit

- `./gradlew regressionCheck` green.
- Coverage floor ratcheted at each sub-phase checkpoint, never ahead of landed tests.
- All 7 original `SIG-*` rows plus the rows added for the date override, the settings entry and the two
  "More" reports either **closed** or **explicitly deferred with a stated reason**.
- One row is expected to be deferred, not closed: the monthly-summary alert's master-switch
  suppression and privacy masking (FR-050) cannot be exercised until delivery ships in the
  notifications phase. Deferring it with that reason is the correct outcome — closing it would be
  false.
- The four research corrections (R1, R4, R11, R12) written back into the design-v1 implementation
  plan's Phase 5 section as a dated note.