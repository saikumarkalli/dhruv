# Feature Specification: Insights (Phase 5)

**Feature Branch**: `005-insights`

**Created**: 2026-08-21

**Status**: Draft

**Input**: User description: "Design-v1 Phase 5 — Insights (screens F1–F5): monthly summary as the
Insights tab root, cashflow statement, profit & loss, balance sheet, reports & export. QA catalog
module SIG (7 rows). Flow F-6 month-end review."

**Source of truth**: `apps/finance/docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md`
§5 Group F (screens F1–F5), flow F-6, offline posture D-6, navigation contract §4 (Insights row), and
NFR-1/3/4/6/8. QA rows: `SIG-*` (7) §8 of `2026-08-09-qa-test-scenario-catalog.md`. Route, channel,
intent and settings rows: `2026-08-09-finance-surface-registries.md` §1 (Insights row, Phase 5), §2
(`monthly_digest`), §3 (`OPEN_REPORTS(month)`), §4 (Modules › Insights). Settings entries follow the
control-plane model of `apps/finance/specs/004-settings/`. This document restates that material as
spec-kit's `spec.md` (what and why only) — schema, views, period arithmetic, module topology,
file-writing and component work are `plan.md`, written separately.

**Position in the build**: Insights is the last tab root without screens. It reads what Phase 2
(holdings, valuations, liabilities) and Phase 3 (accounts, transactions, categories, transfers)
record and adds no new user-entered data of its own. It cannot be built before those phases ship.

## Clarifications

### Session 2026-08-22

- Q: Should Phase 5 build the investment-returns (XIRR) and tax-summary reports that F5's list draws
  under "More", or ship the four core statements and leave those two for later? → A: Build both in
  Phase 5, but only after a decision record defining the investment-returns calculation exists. That
  decision is a prerequisite of this feature's plan, not of its specification.
- Q: Should the monthly-summary alert be delivered by Phase 5, or should Insights store only the
  user's preference and let the notifications phase deliver it, the way budgets and renewals already
  do? → A: Preference only. Insights ships a persisting monthly-summary preference; the channel,
  scheduling and deep-link dispatch are the notifications phase's work, matching the precedent the
  Plan phase already set for budget thresholds and renewal reminders.
- Q: Does the Insights root use the same month/quarter/financial-year/custom picker as Reports, or
  are the root and its three statements month-scoped? → A: One period model across the whole tab.
  Every Insights screen accepts month, quarter, financial year and custom range, and the selected
  period carries between them unchanged. This deliberately extends the design, which drew a month
  selector on the root and a period picker only on Reports. Two consequences follow and are recorded
  as assumptions: profit & loss compares the selected period against the same period one year
  earlier (a month against that month last year, a quarter against that quarter last year), and the
  root's comparative insight stays anchored to a trailing twelve-month window expressed per
  period-equivalent.
- Q: When a period is selected, what date does the balance sheet render as at — the period's end
  date, or does it keep its own separate date control? → A: The period's end date by default,
  overridable with a date picker on the balance sheet itself. The override does not change the tab's
  period. Precedence is fixed: an override survives while the period is unchanged and is discarded
  when the period changes (the new period's end becomes the date again), the screen states both the
  inherited period and the date it is rendering so a divergence is visible rather than silent, and an
  export from this screen covers the date shown.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Answer "did I actually save this month" in one screen (Priority: P1)

A user who has been recording money for a month opens Insights and is told, first and largest, what
share of what came in is still there — then income, expense and surplus as three plain numbers, then
where the money went by category with how each moved against last month.

**Why this priority**: It is the tab root — nothing else in Insights is reachable without it — and
it is the one screen that answers the question the whole ledger exists to answer. Every other
statement is a drill-down that explains this number.

**Independent Test**: With one month of recorded transactions, open Insights and confirm the savings
rate recomputes by hand as surplus divided by income, that income minus expense equals the displayed
surplus, and that each category's change against last month recomputes from the two months' totals.

**Acceptance Scenarios**:

1. **Given** a period with recorded income and expense, **When** the monthly summary opens, **Then**
   the savings-rate figure equals surplus divided by income for that same period, and the income,
   expense and surplus figures are internally consistent (income − expense = surplus).
2. **Given** the monthly summary, **When** it opens, **Then** the savings rate is the first and most
   prominent element on the screen, ahead of the three amounts.
3. **Given** the monthly summary, **When** it opens, **Then** it offers a direct route to each of the
   three statements (cashflow, profit & loss, balance sheet) for the period currently selected.
4. **Given** two consecutive months of transactions, **When** the "where it went" section is read,
   **Then** each category shows its total for the selected period and its change against the previous
   period, and that change recomputes exactly from the two periods' category totals.
5. **Given** a category whose spend is unchanged within a stated tolerance, **When** it is displayed,
   **Then** the change is stated in words ("flat") rather than a misleadingly precise near-zero
   percentage.
6. **Given** at least two months of history, **When** the comparative insight is shown, **Then** it
   states the period's position against the trailing twelve-month average, is labelled as a derived
   insight, and names how many months it actually averaged over.
7. **Given** the monthly summary, **When** the user changes the period with the period selector,
   **Then** every figure on the screen recomputes for the newly selected period.
8. **Given** a transfer between the user's own accounts inside the period, **When** income, expense,
   surplus, savings rate and every category figure are computed, **Then** none of them is affected
   by that transfer.

---

### User Story 2 - Read a cashflow statement that reconciles in front of the user (Priority: P2)

A user opens the cashflow statement for a period and can follow it top to bottom — opening balance,
what came in itemised, what went out itemised, what merely moved between their own accounts, the net
change, the closing balance — and see the arithmetic close on screen without taking anything on
trust.

**Why this priority**: It is the statement that explains the summary's surplus figure, it uses the
data the user produces daily, and its on-screen reconciliation is the credibility test for every
other number in the tab. A report the user cannot check by eye is a report they will not believe.

**Independent Test**: With a period of fixture transactions including at least one transfer, open the
cashflow statement and confirm opening + money in − money out − net moved-not-spent equals the stated
closing balance, and that removing the transfer changes only the moved-not-spent section.

**Acceptance Scenarios**:

1. **Given** a period's transactions, **When** the cashflow statement is rendered, **Then** it shows,
   in order: opening balance, money in itemised, money out itemised, moved-not-spent, net change,
   closing balance.
2. **Given** the rendered statement, **When** the displayed figures are summed, **Then** opening
   balance plus money in, less money out, less the net of moved-not-spent, equals the displayed
   closing balance exactly.
3. **Given** transfers and investment contributions inside the period, **When** the statement is
   rendered, **Then** they appear in moved-not-spent and are excluded from money out.
4. **Given** the statement, **When** it is displayed, **Then** it carries a footnote stating that
   transfers between the user's own accounts are listed separately so they never inflate spend.
5. **Given** the statement, **When** each itemised section is read, **Then** every line names what it
   is and its amount, and the section's lines sum to that section's stated total.
6. **Given** a period with no transactions at all, **When** the statement opens, **Then** it renders
   a designed empty state rather than a statement of zeroes presented as a result.

---

### User Story 3 - See the position as at a date, and see it agree with net worth (Priority: P3)

A user picks a date and reads what they owned and owed as at that moment — assets grouped by sector,
liabilities grouped by type, each with its one-month change — ending at a net worth figure that
matches what the net-worth screen says for the same date.

**Why this priority**: It is the statement that ties the reporting tab to the tracker the user
already built in Phase 2, and it is readable from the first day a holding exists — unlike profit &
loss, which needs a year of history before it says anything. Its value does not depend on the ledger
being complete.

**Independent Test**: With holdings, valuations and liabilities recorded, open the balance sheet at a
date and confirm total assets minus total liabilities equals the displayed net worth, and that the
figure matches the net-worth screen for that same date.

**Acceptance Scenarios**:

1. **Given** recorded assets and liabilities, **When** the balance sheet is rendered as at a date,
   **Then** assets are grouped by sector with a total, liabilities by type with a total, and assets
   total minus liabilities total equals the displayed net worth.
2. **Given** the balance sheet, **When** each row is read, **Then** it shows the sector or liability
   type, its value as at the chosen date, and its change over the preceding month.
3. **Given** an asset whose value is user-supplied rather than derived from a price source, **When**
   the balance sheet is rendered, **Then** a footnote flags that self-valued items are included and
   which they are.
4. **Given** a date earlier than the user's first record, **When** the balance sheet is opened at it,
   **Then** it states that no position existed at that date rather than showing zeroes as a result.
5. **Given** the same date, **When** the balance sheet's net worth and the net-worth screen's figure
   are compared, **Then** they are identical.
6. **Given** a selected period, **When** the balance sheet opens, **Then** it renders as at that
   period's end date without the user choosing one.
7. **Given** the balance sheet open at a period's end date, **When** the user overrides the date,
   **Then** every figure and change column re-anchors to the new date, the screen states both the
   inherited period and the date shown, and the period the other screens use is unchanged.
8. **Given** an override is in force, **When** the user leaves the balance sheet and returns without
   changing the period, **Then** the override is still in force.
9. **Given** an override is in force, **When** the period is changed anywhere in the tab, **Then**
   the override is discarded and the balance sheet renders at the new period's end date.

---

### User Story 4 - Compare a period against the same period last year (Priority: P4)

A user reads income and expense lines for the selected period beside the same period a year earlier,
with each line's share of income and its year-on-year movement, ending at net surplus.

**Why this priority**: The most informative statement once a year of history exists, and the least
useful before that — so it is built after the statements that work from day one. It shares the
period machinery of the earlier stories, so it is cheap once they exist.

**Independent Test**: With two years of fixture transactions, open profit & loss on a month, then on
a quarter, and confirm each line's percentage of income and year-on-year movement recompute by hand,
and that the comparison is the same period one year prior — not the preceding period.

**Acceptance Scenarios**:

1. **Given** a selected period with a matching period one year earlier, **When** profit & loss is
   rendered, **Then** the comparison column is that same period one year earlier — the same calendar
   month, quarter, financial year or shifted custom range, never the immediately preceding period.
2. **Given** the rendered statement, **When** it is read, **Then** income lines appear first, then
   expense lines, each group with a subtotal row, ending at net surplus.
3. **Given** any line, **When** it is displayed, **Then** it shows the line name, its amount for the
   month, its share of that month's income, and its year-on-year movement.
4. **Given** the rendered statement, **When** the line amounts within a group are summed, **Then**
   they equal that group's displayed subtotal, and income subtotal minus expense subtotal equals the
   displayed net surplus.
5. **Given** a period with no matching period a year earlier, **When** profit & loss is rendered,
   **Then** the comparison column states that no prior-year data exists rather than showing zero or a
   misleading movement figure.
6. **Given** a category created, renamed or removed between the two periods, **When** the statement
   is rendered, **Then** its lines still reconcile to the displayed totals and no amount is silently
   dropped.
7. **Given** a quarter, financial year or custom range is selected, **When** profit & loss is
   rendered, **Then** it presents that period without degrading to a month and without disabling
   itself.

---

### User Story 5 - Choose a period, read the report, then export it (Priority: P5)

A user picks a period — month, quarter, financial year or a custom range — chooses a report from a
list, reads it on screen, and only then exports it as a file.

**Why this priority**: It generalises the fixed monthly view into arbitrary periods and is the only
route by which anything leaves the app as a file, so it must come after the statements it exports are
correct and readable. Export before reading is the failure mode the design explicitly forbids.

**Independent Test**: Select each period type in turn, confirm every report's figures change
consistently and that the financial-year option uses an April-to-March boundary; then export a report
and confirm the file's totals equal what was on screen for that same period.

**Acceptance Scenarios**:

1. **Given** the reports screen, **When** the period picker is used, **Then** month, quarter,
   financial year and a custom date range are all selectable, and the chosen period is stated on
   screen.
2. **Given** the financial-year option, **When** it is selected, **Then** the period runs April to
   March.
3. **Given** any selected period, **When** each report in the list is opened in turn, **Then** every
   report presents data for that same period.
4. **Given** a report, **When** the user attempts to export it, **Then** export is only offered once
   the report has been rendered on screen for the selected period.
5. **Given** a rendered report, **When** it is exported, **Then** the exported file's totals and line
   items equal what was displayed on screen for that same period.
6. **Given** an export, **When** the user chooses the format, **Then** both a data format and a
   formatted document format are offered, and the file states the period it covers, the date it was
   generated and the app version.
7. **Given** privacy mode is on and amounts are masked on screen, **When** the user performs an
   export, **Then** the export contains unmasked amounts and says so at the point of export.
8. **Given** an export that cannot complete (storage refused, cancelled, or interrupted), **When** it
   fails, **Then** the failure is reported as a failure, no partial file is presented as complete, and
   the action stays available to retry.
9. **Given** the investment-returns report, **When** it is opened, **Then** it states which money
   movements it computed over, over what date span and by what method, and its figure is reproducible
   by hand from those same movements.
10. **Given** the tax-summary report, **When** it is opened, **Then** it states which categories it
    treats as tax-relevant and on what basis, and is labelled as a summary of the user's own records
    rather than as tax advice.

---

### User Story 6 - Move between statements without losing the period (Priority: P6)

A user doing a month-end review walks from the summary through cashflow, profit & loss and the
balance sheet into reports, and every screen stays on the period they started from.

**Why this priority**: It is the flow the tab was designed around (flow F-6) and the one defect that
would make every individually-correct statement useless in combination. It is only testable once the
preceding stories exist, which is why it is last among the reading stories.

**Independent Test**: Open the summary at a period, navigate through each statement to reports in
sequence, and confirm at each step that the period on screen is the one selected at the start and
that an export taken at the end covers it.

**Acceptance Scenarios**:

1. **Given** a period selected on the monthly summary, **When** the user opens any statement from it,
   **Then** that statement opens on the same period without re-selection.
2. **Given** a statement opened at a period, **When** the user returns and opens another statement,
   **Then** the period is still the one originally selected.
3. **Given** the full chain walked to reports, **When** an export is taken there, **Then** it covers
   the period the chain began with.
3a. **Given** the chain is walked with a balance-sheet date override in force, **When** each screen
    is read, **Then** every other screen is still on the original period and the balance sheet states
    plainly that its date differs from the period end.
4. **Given** any statement, **When** the period is changed on that screen, **Then** the change is
   carried by the rest of the chain rather than reverting on the next screen.
5. **Given** any screen below the tab root, **When** it is open, **Then** it offers exactly one back
   route to its single parent, and the tab root itself offers none.

---

### User Story 7 - Control Insights from Settings, with the module (Priority: P7)

A user who has Insights opens Settings and finds one Insights entry holding that module's own
controls; a user who does not have it sees no such entry.

**Why this priority**: Required by the settings control-plane model — enabling a module enables its
entry, and no phase edits a central list — but it changes no reporting behaviour, so it ships with
the module rather than gating it.

**Independent Test**: With the module enabled, confirm an Insights entry appears in Settings holding
its controls and nothing else; disable the module and confirm the entry is absent, not greyed out.

**Acceptance Scenarios**:

1. **Given** the Insights module is present and enabled, **When** Settings is opened, **Then** an
   Insights entry appears in the modules tier holding that module's own controls.
2. **Given** the Insights module is absent, disabled or gated out for the running version, **When**
   Settings is opened, **Then** no Insights entry appears at all.
3. **Given** the Insights entry, **When** it is read, **Then** each control it holds belongs to
   Insights and no Insights control appears anywhere else in Settings.
4. **Given** the Insights entry, **When** the user sets the monthly-summary alert preference and
   reopens the entry after an app restart, **Then** it shows as set.
5. **Given** the monthly-summary preference, **When** Settings is searched end to end, **Then** it
   appears exactly once, in the Insights entry, and nowhere else.

---

### Edge Cases

- **Income is zero for the period.** Savings rate is a share of income and is undefined — the summary
  must say so rather than display 0%, a dash, or an infinite value.
- **Expense is zero, income is not.** Savings rate is 100%; the "where it went" section renders its
  own empty state rather than an empty list with a heading.
- **Period contains only transfers.** Income, expense and surplus are all zero and the cashflow
  statement's moved-not-spent section carries every line; the statement still reconciles.
- **Fewer than twelve months of history.** The comparative insight averages what exists and states
  the number of months it used; it is never presented as a twelve-month comparison when it is not.
- **No matching period one year earlier.** Profit & loss states the comparison is unavailable; it
  does not treat the absence as zero.
- **A custom range shifted back one year that lands on different month lengths or across a leap
  day.** The comparison range is stated on screen so the user can see exactly what was compared;
  it is never silently rounded to whole months.
- **A date before the first record.** The balance sheet states that no position existed rather than
  reporting a net worth of zero — whether that date arrived from a period end or from an override.
- **A balance-sheet date override set to a future date.** Rejected or corrected at selection with a
  stated reason; a position is never projected forward.
- **An override in force when the period changes on another screen.** The override is discarded
  silently in favour of the new period's end date, and the balance sheet shows the new date on the
  user's next visit rather than a stale one.
- **A correction issued against an already-reported period.** Records are append-only; a statement
  re-read after a correction reflects the corrected figures and still reconciles.
- **A category renamed or deleted between two compared periods.** Both periods' totals still
  reconcile, and no amount is silently dropped from either.
- **Custom range spanning a financial-year boundary.** The range is honoured as selected; nothing is
  silently snapped to a financial year.
- **Custom range whose end precedes its start, or which extends into the future.** Rejected or
  corrected at selection with a stated reason; never rendered as an empty result.
- **Very large period (a multi-year custom range).** The statement remains readable and scrollable
  and does not block interaction while it loads.
- **Signed out, offline, or consent withdrawn.** Every Insights screen renders its designed state —
  signed-out, offline or not-configured — never an unresolving spinner, and export is unavailable
  with the reason stated.
- **Export requested while the report on screen is stale relative to a period change.** The export
  covers the period displayed with the report; it never mixes two periods in one file.
- **A period with no investment movements, or movements that admit no return solution.** The
  investment-returns report states that a return cannot be computed for the period, naming why; it
  never falls back to zero or to a figure from a different period.
- **A period with no tax-relevant categories.** The tax summary renders its empty state and names
  what it looked for, rather than presenting an empty table as a completed summary.

## Requirements *(mandatory)*

### Functional Requirements

**Monthly summary (F1)**

- **FR-001**: The Insights tab root MUST present a savings-rate figure for the selected period as its
  first and most prominent element, computed as surplus divided by income.
- **FR-002**: The root MUST show income, expense and surplus for the selected period as three plain
  figures that are mutually consistent.
- **FR-003**: The root MUST state, rather than imply, when savings rate cannot be computed because
  income for the period is zero.
- **FR-004**: The root MUST offer a direct route to the cashflow statement, profit & loss and balance
  sheet for the currently selected period.
- **FR-005**: The root MUST show the period's largest spending categories with each category's change
  against the previous comparable period, expressed as a movement, and MUST state near-zero movement
  in words rather than as a precise percentage.
- **FR-006**: The root MUST show a comparative insight against the trailing twelve-month average,
  MUST label it as derived, and MUST state how many months it averaged when fewer than twelve exist.
- **FR-007**: The root MUST offer the same period selector the rest of the tab uses — month, quarter,
  financial year and custom range — and changing the period MUST recompute every figure on the
  screen.

**Cashflow statement (F2)**

- **FR-008**: The cashflow statement MUST present, in order: opening balance, itemised money in,
  itemised money out, moved-not-spent, net change, closing balance.
- **FR-009**: The displayed figures MUST reconcile on screen: opening balance plus money in, less
  money out, less the net of moved-not-spent, equals closing balance.
- **FR-010**: Transfers between the user's own accounts and contributions to the user's own
  investments MUST appear only in moved-not-spent and MUST be excluded from money out.
- **FR-011**: The statement MUST carry a footnote stating that transfers between the user's own
  accounts are listed separately so they never inflate spend.
- **FR-012**: Each itemised section's lines MUST sum to that section's displayed total.

**Profit & loss (F3)**

- **FR-013**: Profit & loss MUST compare the selected period against the same period one year
  earlier — a month against that calendar month last year, a quarter against that quarter last year,
  a financial year against the preceding one, a custom range against the same range shifted back one
  year. It MUST NOT compare against the immediately preceding period.
- **FR-014**: It MUST present income lines then expense lines, each group with a subtotal, ending at
  net surplus, with group subtotals reconciling to the displayed lines.
- **FR-015**: Each line MUST show its name, its amount for the period, its share of that period's
  income, and its year-on-year movement.
- **FR-016**: When no prior-year period exists, the comparison MUST be stated as unavailable and MUST
  NOT be rendered as zero or as a movement figure.

**Balance sheet (F4)**

- **FR-017**: The balance sheet MUST present the position as at a single date, resolved as follows:
  it opens at the selected period's end date; the user MAY override it with any date from a control
  on the balance sheet itself; the override MUST NOT change the period the rest of the tab is using;
  the override MUST survive while the period is unchanged and MUST be discarded when the period
  changes, reverting to the new period's end date.
- **FR-017a**: Whenever the rendered date is not the selected period's end date, the balance sheet
  MUST state both — the period it inherited and the date it is showing — so the divergence is visible
  rather than silent.
- **FR-018**: It MUST group assets by sector with a total, group liabilities by type with a total,
  and display net worth as assets total minus liabilities total.
- **FR-019**: Each row MUST show its value at the rendered date and its change over the month
  preceding that same date — the anchor is always the date on screen, never the period's end when an
  override is in force.
- **FR-020**: The net worth it displays for a date MUST equal what the net-worth screen displays for
  that same date.
- **FR-021**: It MUST flag, in a footnote, that self-valued items are included and identify them.
- **FR-022**: For a date preceding the user's first record it MUST state that no position existed
  rather than display zeroes as a result.

**Reports & export (F5)**

- **FR-023**: Reports MUST use the tab's period picker — month, quarter, financial year and a custom
  date range — and MUST state the selected period on screen. It MUST be the same period model every
  other Insights screen uses, not a second one.
- **FR-024**: The financial-year period MUST run April to March.
- **FR-025**: A custom range MUST be validated at selection; an end before its start MUST be
  rejected or corrected with a stated reason.
- **FR-026**: Reports MUST list the available statements — cashflow, profit & loss, balance sheet,
  category breakdown, investment returns and tax summary — and every listed report MUST present data
  for the selected period.
- **FR-027**: Every report MUST be readable in full on screen; export MUST NOT be the only way to
  read a report, and MUST be offered only after the report has rendered for the selected period.
- **FR-028**: Export MUST offer both a machine-readable data format and a formatted document format.
- **FR-029**: An exported file's totals and line items MUST equal what was displayed on screen for
  the same period — and, for the balance sheet, for the date actually rendered rather than the
  period's end date when the two differ.
- **FR-030**: An exported file MUST state the period it covers, the date it was generated, and the
  app version.
- **FR-031**: When amounts are masked on screen by privacy mode, an export MUST still contain
  unmasked amounts and MUST state that at the point of export.
- **FR-032**: A failed or cancelled export MUST be reported as failed, MUST NOT present a partial
  file as complete, and MUST leave the action available for retry.
- **FR-033**: Reports MUST be openable at a period supplied from outside the screen, so that a later
  monthly-summary alert can land on the exact period it refers to. Dispatching that entry point from
  a notification is the notifications phase's work, not this feature's; this feature is responsible
  only for accepting the period and honouring it.
**Investment returns and tax summary (F5 "More")**

- **FR-034**: The investment-returns report MUST state, on the screen that displays it, which set of
  money movements it computed over, over what date span, and by what method — so its figure can be
  reproduced by hand from the user's own records.
- **FR-035**: The investment-returns report MUST NOT be built to a definition invented during
  implementation. Its calculation MUST be fixed by an accepted decision record before this feature is
  planned; that record is a prerequisite of the plan, not of this specification.
- **FR-036**: When a return figure cannot be computed for the selected period — no investment
  movements, or movements that do not admit a solution — the report MUST state that plainly instead
  of showing zero, a dash, or an arbitrary value.
- **FR-037**: The tax-summary report MUST present the selected period's tax-relevant income and
  deduction lines, MUST state which categories it treats as tax-relevant and on what basis, and MUST
  be labelled as a summary of the user's own records rather than as tax advice or a filing.
- **FR-038**: Both reports MUST obey every rule that applies to the other statements: readable in
  full on screen before export, exportable in the same formats, and period-consistent with the rest
  of the tab.

**Period behaviour across the tab (flow F-6)**

- **FR-039**: A period selected on any Insights screen MUST carry to the other Insights screens the
  user navigates to, without re-selection. A balance-sheet date override (FR-017) is the sole
  exception: it is local to that screen, never propagates, and never mutates the carried period.
- **FR-040**: Every Insights screen below the tab root MUST offer exactly one back route to its
  single parent; the tab root MUST offer none.

**States, precision and access**

- **FR-041**: Every Insights screen MUST render a designed signed-out, offline, not-configured and
  disabled state, and MUST NOT show an unresolving spinner in any of them.
- **FR-042**: Insights MUST NOT cause any off-device call before the corresponding consent switch is
  on, and MUST degrade to its designed state when consent is withdrawn.
- **FR-043**: Every reported amount MUST be exact — no rounding drift may make a displayed statement
  fail to reconcile.
- **FR-044**: Every derived or inferred statement (the comparative insight, the investment-returns
  figure, any projection language) MUST be labelled as derived rather than presented as plain fact.
- **FR-045**: A period with no records MUST render a designed empty state, never a statement of
  zeroes presented as a result.

**Settings and alerts**

- **FR-046**: Insights MUST declare its own Settings entry, present only when the module is present
  and enabled, holding that module's controls and no others.
- **FR-047**: Insights MUST offer a net-worth statement export control from its own Settings entry.
- **FR-048**: Users MUST be able to set a monthly-summary alert preference from the Insights Settings
  entry; it MUST persist and show as set when the entry is reopened. Creating the channel, scheduling
  the alert and dispatching it are the notifications phase's work and are NOT part of this feature.
- **FR-049**: The monthly-summary preference MUST have exactly one control, in the Insights Settings
  entry, and MUST NOT be duplicated elsewhere in Settings.
- **FR-050**: When the monthly-summary alert is eventually delivered, it MUST be suppressed by the
  app-wide notification master switch and MUST NOT restate an amount while privacy mode is on. These
  are constraints this feature records on the preference it stores; they are verified when delivery
  ships, and that scenario is deferred with this reason stated rather than closed here.

### Key Entities

Insights introduces **no user-entered records**. Every entity below is a derived, read-only view over
what Phases 2 and 3 already store.

- **Reporting period**: a bounded span the user selects — a month, a quarter, a financial year
  (April–March) or a custom range. Carries across Insights screens; every figure in the tab is
  qualified by exactly one of these.
- **Statement**: a named, period-qualified report — cashflow, profit & loss, balance sheet, category
  breakdown. Has an ordered set of sections, each with lines and a subtotal, and a reconciliation
  relationship its displayed totals must satisfy.
- **Statement line**: one named amount inside a statement section, optionally with comparison figures
  (share of income, year-on-year movement, one-month change).
- **Category summary**: one spending category's total for a period plus its movement against the
  comparable previous period.
- **Position snapshot**: assets grouped by sector and liabilities grouped by type as at a chosen
  date, resolving to a single net worth figure that must agree with the net-worth screen.
- **Export artifact**: a file produced from an already-rendered statement, carrying that statement's
  period, generation date and app version, in either a data or a formatted-document form.

### Scope Boundaries

**In scope**: the five Insights screens (monthly summary, cashflow, profit & loss, balance sheet,
reports & export); the period model shared across them; on-screen reconciliation; the full report
list including investment returns and tax summary; export of a rendered statement to a data and a
document format; the Insights Settings entry; the designed signed-out, offline, not-configured,
disabled and empty states for each screen.

**Gating prerequisite**: the investment-returns calculation must be fixed by an accepted decision
record before this feature is planned. Functional spec open item §8.6 records that its cashflow set
was never specified and its decision record was reserved but never written. That record is written
first; this feature then builds both "More" reports against it. Writing it is a prerequisite of
`/speckit-plan`, not of this specification, and its number is taken from the register at the time it
is written — never reserved in advance here.

**Out of scope**:

- The **Account-tier "export my financial records"** row (settings spec FR-018). That is portability
  of the user's records under the data-protection obligation; this feature's export is formatted
  reporting for reading. The settings spec separates them explicitly. Which phase ships the
  portability export is not settled here.
- The **Ask (Gemini) route**. The navigation contract draws it under Insights, but the route registry
  already assigns it to the shell as a detail route under the assistant module, live today. Functional
  spec open item §8.4 concerns which tab owns its back stack and is not resolved by this feature.
- **Notification delivery of any kind.** Insights stores the monthly-summary preference (FR-048); the
  channel, the permission prompt, the scheduler and the deep-link dispatch that acts on it are the
  notifications phase's work, along with the notification centre and search screens. This matches the
  Plan phase, which stores budget thresholds and renewal reminders and defers their delivery the same
  way.
- Any change to **what the underlying records mean**: category semantics, transfer semantics,
  valuation history and the exclusion of investment categories from spend are Phase 2 and Phase 3
  decisions this feature reads and must not redefine.
- **Multi-currency reporting**. The tracker is a single-currency design; nothing here reopens it.
- **Editing records from a statement.** Statements are read surfaces; corrections happen where the
  record lives.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For at least three independent period fixtures, the cashflow statement's displayed
  figures reconcile exactly — opening plus money in, less money out, less net moved-not-spent, equals
  closing — with zero discrepancy.
- **SC-002**: For at least three independent date fixtures — including one reached by overriding the
  date rather than by the period's end — the balance sheet's assets total minus liabilities total
  equals its displayed net worth exactly, and equals the net-worth screen's figure for that date.
- **SC-003**: For any period, the savings rate shown on the summary and the surplus derivable from
  the cashflow statement for that same period agree exactly.
- **SC-004**: For a period with a prior-year counterpart — verified on at least a month and a
  non-month period — every profit & loss line's share-of-income and year-on-year figures reproduce by
  independent recomputation, and group subtotals equal the sum of their lines.
- **SC-005**: Selecting each of month, quarter, financial year and a custom range in turn updates
  every Insights screen consistently — root, all three statements and every report — and the
  financial year resolves to April–March in every one of them.
- **SC-006**: An exported file's totals and line items match the on-screen statement for the same
  period, verified by direct comparison of the file against the rendered report.
- **SC-007**: A user walking the month-end review chain — summary, cashflow, profit & loss, balance
  sheet, reports, export — finds the period unchanged at every step and covered by the resulting file.
  Walking it again with a balance-sheet date override in force changes only that screen, which states
  its own date, and leaves the period every other screen uses untouched.
- **SC-008**: A user can read every report in full without exporting it; no report requires a file to
  be legible.
- **SC-009**: Each of the five screens renders its signed-out, offline, not-configured, disabled and
  empty states as designed states, with no state producing an unresolving spinner.
- **SC-010**: Every stated edge case — zero income, transfers-only period, no prior-year month, date
  before first record, fewer than twelve months of history — produces a stated explanation rather than
  a zero, a dash, or a misleading figure.
- **SC-011**: Opening any statement for a period presents the report without blocking interaction, on
  a data set representing a year of daily recording.
- **SC-012**: Every money figure in the tab is exact to the smallest currency unit, with no rounding
  drift that would break any of SC-001 through SC-004.
- **SC-013**: With the module disabled, no Insights entry appears in Settings and no Insights route is
  reachable; with it enabled, its entry appears once and its controls appear nowhere else.
- **SC-014**: For at least three independent fixtures, the investment-returns figure reproduces by
  independent recomputation from exactly the money movements the report says it used, over exactly
  the date span it names.
- **SC-015**: Every figure in the tax summary traces to the records of the categories the report
  names as tax-relevant, with no line sourced from a category outside that stated set.
- **SC-016**: The monthly-summary preference survives an app restart and appears in exactly one place
  in Settings; no notification is emitted by this feature, and the deferred delivery scenarios are
  recorded as deferred rather than closed.

## Assumptions

- **Phases 2 and 3 have shipped.** Insights derives everything from holdings, valuations, liabilities,
  accounts, transactions, categories and transfers. It cannot be specified against, or built on,
  records that do not exist yet.
- **The investment-returns decision record exists before planning begins.** This feature builds both
  "More" reports, but the returns calculation is settled in that record first — the phase is
  deliberately blocked on it rather than proceeding with an implementation-invented definition. If
  that record is not written, the feature ships nothing under "More"; it does not guess.
- **Transfer and exclusion semantics are inherited, not redefined.** Which categories are excluded
  from spend, and what counts as a transfer between the user's own accounts, are Phase 3 decisions;
  this feature reads them and must produce the same answer the ledger does.
- **Corrections are append-only.** A reported period can change after the fact when a correction is
  appended; statements are read fresh rather than frozen at first view, so a re-read reflects
  corrections. Frozen or versioned statements are not part of this feature.
- **The financial year is April to March**, per the QA scenario for the period picker.
- **One period model spans the whole tab** — month, quarter, financial year, custom range — on every
  Insights screen, not just Reports. This extends the design, which drew a month selector on the root
  and a period picker only on Reports; the extension was chosen deliberately (see Clarifications).
- **The comparable previous period for a movement figure is the immediately preceding period of the
  same length** (previous month for a month, previous quarter for a quarter), except for profit &
  loss, whose comparison is the same period one year earlier.
- **Profit & loss generalises to non-month periods** as "the same period one year earlier". The
  design and the QA scenario state this only for months; the generalisation is the only reading
  consistent with a single period model, and is recorded here so it can be overturned cheaply if the
  maintainer prefers a different basis for quarters and financial years.
- **The root's comparative insight stays anchored to a trailing twelve-month window**, expressed per
  period-equivalent so it remains meaningful when the selected period is not a month, and it names
  the window it actually used.
- **The balance sheet's "as at" date is resolvable for any past date**, because valuations are
  append-only and transactions are dated — the position is derivable rather than needing a stored
  snapshot. The date override (FR-017) makes arbitrary past dates a first-class path rather than
  something reachable only by constructing a custom range, so this derivation is load-bearing, not
  an edge case.
- **Export destination is the user's own choice** through the platform's standard file handling; the
  feature produces a file and does not manage a private export library.
- **Insights is consent-gated and session-backed**, matching every other tracker surface — the
  offline posture decision makes signed-out, offline and not-configured first-class states here, and
  offline reading of previously fetched statements is not assumed.
- **Privacy-mode masking applies on screen only**; the settings spec already exempts a user-initiated
  export and requires it to say so, which this feature inherits rather than re-decides.
- **The Insights feature flag entry does not exist yet** and is added with this feature, consistent
  with every other tracker module.
- **The tab root exists but is empty today** — the Insights tab is present in the shell and renders a
  not-configured state; this feature replaces that state with the real root, it does not add the tab.

---

## Implementation record

> **Status: NOT YET IMPLEMENTED.** This section is filled in when {phase} ships, and is
> **maintained for the life of the feature** thereafter — see constitution Article Xa
> ("Documentation Tracks Reality"). Everything above this line describes what *will* be built;
> everything below describes what *was*.
>
> Module(s): {module}.

### As built

*(Fill on completion. What actually shipped, per user story. Keep it short — the tasks list the
work, this records the outcome.)*

| Story / FR | Shipped | Notes |
|---|---|---|
| | | |

### Deviations from this spec

*(Anything built differently from what is specified above, and **why**. A deviation recorded here is
a decision; a deviation left unrecorded is drift, and this repo has been burned by it — see
ADR-0030.)*

| Spec says | Built as | Reason |
|---|---|---|
| | | |

### Deferred

*(Scope named in this spec that did **not** ship, with a reason and an owner. Never silently drop
scope — an audit found several screens quietly reduced to a subset with no deferral recorded.)*

| Item | Deferred to | Reason |
|---|---|---|
| | | |

### Change log for this feature

Every later change to shipped behaviour lands a row here **in the same PR that changes the
behaviour** — defect fixes, functional changes, schema migrations, removals.

A defect row names the **FR whose stated behaviour was not actually delivered**. That is what
separates a bug fix from an undocumented behaviour change, and it is how the next reader learns the
spec was once wrong rather than assuming the code is.

| Date | Change | Type | FR affected | PR |
|---|---|---|---|---|
| | | fix / change / removal | | |
