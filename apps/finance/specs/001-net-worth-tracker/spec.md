# Feature Specification: Net Worth Tracker (Phase 2)

**Feature Branch**: `001-net-worth-tracker`

**Created**: 2026-08-16

**Status**: Draft

**Input**: User description: "Phase 2 — Net worth (C1-C7) + real Home. Source: design-v1 functional
spec Group C (C1-C7) + Group B '01' Home + BR-C1..C4; QA rows NW-* (14) and HOM-* (5)."

**Source of truth**: `apps/finance/docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md`
§5 Group C (screens C1–C7) and Group B (screen 01); business rules BR-C1..BR-C4; NFR-4, NFR-8.
This document restates that material as spec-kit's `spec.md` (what/why only) — technical design
(schema, module topology, networking) is `plan.md`, written separately.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Record what I own or owe, and see my net worth (Priority: P1)

A user adds something they own (a bank balance, a mutual fund, gold) or owe (a loan, a credit
card balance) with its current value, and immediately sees it reflected in a single net-worth
total broken down by category.

**Why this priority**: This is the entire point of the tracker — without it there is nothing to
show on any other screen. It is the smallest possible slice that delivers real value: a user who
can only do this already has a working (if minimal) net-worth tracker.

**Independent Test**: Sign in, add one asset and one liability with a value each, and confirm the
net-worth total on the overview screen equals asset value minus liability value.

**Acceptance Scenarios**:

1. **Given** a signed-in user with no holdings, **When** they add a holding (owned or owed),
   choose a category from the fixed list, and enter a current value, **Then** the holding appears
   immediately in the relevant list and the net-worth total updates to include it.
2. **Given** a holding is being added, **When** the user tries to type a free-text category instead
   of choosing one of the fixed categories, **Then** the entry is rejected and only the fixed
   category list is accepted.
3. **Given** a user has added at least one asset and one liability, **When** they open the net-worth
   overview, **Then** they see one net total plus separate asset and liability subtotals, and a
   ranked breakdown by category with each category's share of the total.

---

### User Story 2 - Review a holding's detail and value history (Priority: P2)

A user opens a single holding to see its current value, how it has changed over time, and the full
list of every value ever recorded for it.

**Why this priority**: Builds directly on Story 1 (a holding must exist first) and is the second
most common action — checking on something you already track. Independently demoable once Story 1
exists.

**Independent Test**: With one holding already recorded, open it and confirm the value, a trend
view over selectable time ranges, and a dated history list all render correctly.

**Acceptance Scenarios**:

1. **Given** a holding has three or more recorded values over time, **When** its detail screen is
   opened, **Then** the value history is shown newest-first, and each entry shows how much it
   changed compared to the entry before it.
2. **Given** a holding's detail screen, **When** the user selects a different time range (e.g. 3
   months vs 1 year), **Then** the trend view updates to that range.
3. **Given** an asset holding with a purchase/invested amount known, **When** its detail is viewed,
   **Then** the amount invested, the absolute gain (₹), and a simple return percentage
   (`(current − invested) / invested`) are shown alongside the current value. This percentage is an
   interim figure — see the Assumptions note on returns calculation.

---

### User Story 3 - Update a value without losing history (Priority: P3)

A user records a new current value for something they already track (e.g. this month's mutual fund
value) and sees exactly how much it changed, while every previous value they ever entered stays
visible and unchanged.

**Why this priority**: This is how the tracker stays accurate over time. It depends on Stories 1–2
(a holding and its history view must already exist) but is independently valuable and testable on
its own.

**Independent Test**: Record a second value for an existing holding and confirm the previous value
is still visible in its history, unaltered, with the new value shown as the latest.

**Acceptance Scenarios**:

1. **Given** a holding's last recorded value is known, **When** the user enters a new value,
   **Then** the change (amount and percent) is shown immediately, before the new value is saved.
2. **Given** a value was entered incorrectly, **When** the user corrects it, **Then** the correction
   appears as a new entry and the incorrect entry is removed from view — but no existing entry is
   ever directly altered or deleted from the underlying record.
3. **Given** a holding with a full value history, **When** net worth is computed anywhere in the
   app, **Then** it always uses each holding's single latest value, never a stale or cached total.

---

### User Story 4 - Track liabilities and payoff progress (Priority: P4)

A user tracks something they owe (a home loan, car loan, credit card, buy-now-pay-later balance),
sees the outstanding amount (not the original amount), and understands how paying extra now would
change when it's paid off.

**Why this priority**: Parallel to asset tracking (Story 1 covers the mechanics) but liabilities
have their own screens and payoff-specific value — independently testable and independently
valuable to a user with debt to track.

**Independent Test**: Add a loan-type liability with rate and monthly payment, open its detail, and
confirm outstanding balance, payoff progress, and a "pay extra" projection all render.

**Acceptance Scenarios**:

1. **Given** liabilities of different types exist, **When** the liabilities overview is opened,
   **Then** they are grouped by type with total outstanding, total monthly outgo, and a projected
   debt-free date shown.
2. **Given** a loan-type liability, **When** its detail is opened, **Then** the split between
   principal already paid, interest already paid, and amount remaining is shown, and it sums to the
   total obligation.
3. **Given** a loan-type liability's detail, **When** the user asks what paying a specific extra
   amount now would do, **Then** the app shows the interest saved and how much earlier the loan
   would be paid off.

---

### User Story 5 - See net worth at a glance on the home screen (Priority: P5)

A user opens the app and immediately sees their net-worth total, its recent trend, and any upcoming
loan/EMI obligations, without navigating anywhere.

**Why this priority**: Ties Stories 1–4 together into the app's front door. Depends on net worth
already being computable (Story 1) but is its own independently testable surface.

**Independent Test**: With holdings and at least one liability with a due date already recorded,
open the home screen and confirm the net-worth figure, trend indicator, and an upcoming-obligations
list all render without navigating elsewhere.

**Acceptance Scenarios**:

1. **Given** net-worth data is available, **When** the home screen opens, **Then** it shows the
   net-worth total, a percentage change indicator, and a short trend visualization.
2. **Given** a liability has an upcoming due date, **When** the home screen opens, **Then** that
   obligation appears in an upcoming-obligations list with its amount and date.
3. **Given** the user is signed out, or offline with nothing previously loaded, **When** the home
   screen opens, **Then** it shows a clear signed-out or offline state instead of a blank screen or
   a stuck loading spinner.

### Edge Cases

- What happens when a user has zero holdings? The net-worth overview must show an empty state that
  invites them to add their first holding, not a zero value with no explanation.
- What happens when the user is signed out, or offline with no data previously loaded, on any net
  worth or home screen? A clear signed-out/offline state must render — never a blank screen, a
  crash, or a spinner that never resolves.
- What happens when a user tries to directly edit or delete a past recorded value? This must not be
  possible through any path — only "add a new value" and "remove this value from view" (which
  preserves the underlying record) are available.
- What happens when two values are recorded for the same holding on the same day? Both are kept;
  the most recently recorded one is treated as latest.
- What happens when a category (for holdings or liability types) needs a new option in the future?
  New categories can be added; an existing category already in use is never renamed or removed.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to record something they own or owe, choosing its category from a
  fixed list (never free text), and entering its current value.
- **FR-002**: The system MUST atomically create both the holding and its first recorded value when
  a new holding is saved — a holding never exists without at least one value.
- **FR-003**: Users MUST be able to record a new value for an existing holding at any time.
- **FR-004**: The system MUST preserve every previously recorded value for a holding permanently.
  Correcting a value MUST be done by recording a new value and hiding the incorrect one from view,
  never by altering or removing the original record.
- **FR-005**: The system MUST compute net worth as the sum of the latest value of every asset minus
  the sum of the latest value of every liability, always from each holding's current latest value —
  never from a total calculated and cached earlier.
- **FR-006**: Users MUST be able to view a single holding's current value, a value trend over
  multiple selectable time ranges, and its full value history ordered newest-first with each
  entry's change from the one before it.
- **FR-006a**: For an asset holding with a known invested amount, users MUST see the absolute gain
  and a simple return percentage (`(current − invested) / invested`). This is an interim
  calculation — see Assumptions.
- **FR-007**: Users MUST be able to view a ranked breakdown of net worth by category, with each
  category's total value and share of the whole.
- **FR-008**: For liability-type holdings, users MUST be able to see outstanding balance (not
  original amount), rate, payment amount, and payoff progress.
- **FR-009**: For a loan-type liability, users MUST be able to enter a hypothetical extra payment
  and see the resulting interest saved and payoff-date change.
- **FR-010**: The home screen MUST display the net-worth total, its recent trend, and any upcoming
  liability obligations without requiring navigation to another screen.
- **FR-011**: Any screen showing net-worth or home data MUST show a distinct signed-out state and a
  distinct offline-with-no-cached-data state, never a blank screen or an unresolving spinner.
- **FR-012**: A category (holding category or liability type) already used by an existing holding
  MUST NOT be renamed or removed once shipped; new categories may be added.

### Key Entities

- **Holding**: Something a user owns (asset) or owes (liability) — has a name, a fixed category,
  and an ownership direction (owned/owed). Exists only alongside at least one recorded value.
- **Recorded value**: A single dated value for a holding, plus how it was determined (manually
  entered, from a statement, etc). Immutable once created — corrections are new records, never
  edits. The full ordered set of a holding's recorded values is its history.
- **Net worth**: A derived figure — the sum of the latest recorded value across every asset holding,
  minus the sum of the latest recorded value across every liability holding. Not stored on its own;
  always computed fresh from current holdings and their latest values.
- **Liability detail**: Additional attributes specific to a liability-type holding — interest rate,
  regular payment amount, remaining term, and (for loan types) principal-vs-interest paid so far.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can add a holding and see it reflected in their net-worth total within the
  same session, with no page reload or manual refresh needed.
- **SC-002**: A correction to a mistaken value never causes a previously recorded value to
  disappear from that holding's history — 100% of prior entries remain visible after any number of
  corrections.
- **SC-003**: Every screen that depends on net-worth or home data shows a correct signed-out,
  offline, or loaded state — 0% of sessions show a blank or permanently-loading screen under those
  conditions.
- **SC-004**: A user can go from "nothing tracked" to "seeing a complete net-worth total" in under
  2 minutes for a first holding.
- **SC-005**: A user evaluating a loan payoff can get an interest-saved answer for a hypothetical
  extra payment without leaving the liability's own detail view.

## Assumptions

- This feature is scoped to net worth (holdings, values, liabilities) and the home screen's use of
  that data — it does not cover day-to-day transactions/ledger, budgets, goals, insurance, or
  retirement (later phases in the design-v1 implementation plan).
- Users are already signed in and have completed onboarding/consent (Phase 1, already shipped);
  this feature assumes that state exists and only specifies its own signed-out/offline fallback
  behavior.
- Currency is a single fixed currency for all values in this feature (resolved for this phase by
  the implementation plan; not re-litigated here).
- "Upcoming obligations" on the home screen in this phase covers loan/EMI-type liabilities only;
  other obligation sources (e.g. credit card bills) depend on data introduced in a later phase and
  are out of scope here.
- **Returns calculation (resolved 2026-08-16)**: Story 2's return percentage ships this phase as a
  simple absolute return, `(current − invested) / invested`. This is a deliberate placeholder — it
  does not account for the timing of multiple contributions/withdrawals, so it is not comparable
  across holdings with irregular contribution history. The more accurate internal-rate-of-return
  figure (referenced as XIRR in the source design) is blocked on its own not-yet-written ADR
  (source catalog: NW-BR-007) and is a follow-up once that ADR lands — at that point the simple
  percentage is replaced, not shown alongside it.

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
