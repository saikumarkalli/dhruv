# Feature Specification: Money Tab (Phase 3)

**Feature Branch**: `002-money-tab`

**Created**: 2026-08-16

**Status**: Draft

**Input**: User description: "Design-v1 Phase 3 — Money tab (screens D1–D9): ledger, quick add, full
transaction form, transaction detail with audit trail, filter sheet, accounts, account detail with
reconciliation, categories, recurring. Business rules BR-D1..BR-D5, QA catalog module MNY (20 rows)."

**Source of truth**: `apps/finance/docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md`
§5 Group D (screens D1–D9), business rules BR-D1..BR-D5, flow F-3, and NFR-1/3/4/8. QA rows:
`MNY-*` (20) in `2026-08-09-qa-test-scenario-catalog.md` §4. This document restates that material as
spec-kit's `spec.md` (what/why only) — schema, module topology, networking and component work are
`plan.md`, written separately.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Record a spend in seconds (Priority: P1)

A user who just paid for something opens the app, types the amount, accepts the guessed category and
account, and saves — the entry appears in today's list and the month's totals update.

**Why this priority**: Day-to-day capture is the entire reason the Money tab exists. Every other
screen in this feature reads what this story writes. A user who can only do this already has a
working expense tracker.

**Independent Test**: From the ledger, tap add, enter an amount, confirm the pre-filled category and
account, save — confirm the row appears under today with the correct sign and the month summary
changes by exactly that amount.

**Acceptance Scenarios**:

1. **Given** the ledger is open, **When** the user taps add, enters an amount, and accepts the
   pre-guessed category and account, **Then** the entry is saved in no more than three taps after
   the amount is entered, and appears immediately in today's group.
2. **Given** an entry is being added, **When** the user chooses the type Expense, Income or
   Transfer, **Then** the amount's sign and its treatment in totals follow that type, and Transfer
   requires both a source and a destination account.
3. **Given** an entry was just saved, **When** the ledger's pinned month summary is read, **Then**
   income, expense and saved-percentage reflect the new entry without a manual refresh.
4. **Given** the quick-entry surface is open, **When** the user needs a field it does not offer
   (receipt, split, recurring schedule, goal link), **Then** a "more options" path opens the full
   form carrying over everything already entered.

---

### User Story 2 - Understand where the month went (Priority: P2)

A user opens the ledger for a chosen month and reads what happened: grouped by day, summarised at
the top, searchable, and narrowable to exactly the slice they care about.

**Why this priority**: Capture without review is a diary nobody reads. Depends on Story 1 having
data but is independently valuable and testable the moment any transactions exist.

**Independent Test**: With transactions across several days and categories, open the ledger, confirm
day grouping and month totals, then apply a filter and confirm the result count and list match.

**Acceptance Scenarios**:

1. **Given** a month with transactions on multiple days, **When** the ledger opens, **Then** rows
   are grouped by day with a per-day net, and each row shows payee/description, category, account
   and a signed amount.
2. **Given** the ledger is open, **When** the user changes the month selector, **Then** the list and
   the pinned summary both switch to that month.
3. **Given** the filter surface is open, **When** any filter is changed, **Then** the number of
   matching results updates before the filter is applied, so the user sees the size of the result
   set before committing to it.
4. **Given** a filter combination the user expects to reuse, **When** they save it as a named view,
   **Then** that view can be re-applied later without re-selecting each filter.
5. **Given** a search term, **When** it is entered, **Then** matching transactions are found by
   payee, description or note.

---

### User Story 3 - Know what is actually spendable (Priority: P3)

A user checks their accounts and sees one honest number for money they can actually spend — with
credit-card balances shown separately as money owed, not money held.

**Why this priority**: The single most common financial mis-read is counting a credit limit as
available money. Independently testable with accounts alone, before any transaction exists.

**Independent Test**: Create one bank, one cash and one credit-card account with balances, open the
accounts screen, confirm "spendable now" equals bank + cash only and that the card appears under a
separate owed grouping.

**Acceptance Scenarios**:

1. **Given** accounts of every type exist, **When** the accounts screen opens, **Then** "spendable
   now" sums bank, cash and wallet balances only, and credit-card balances are excluded from it.
2. **Given** a credit-card account with spend on it, **When** it is displayed, **Then** its balance
   is negative, grouped under a heading that states it is owed and not held, and shows limit,
   utilisation percentage and due date.
3. **Given** an account whose balance was last confirmed beyond the staleness threshold, **When**
   the accounts or account-detail screen opens, **Then** the account is flagged as needing a check
   and offers a way to fix it.
4. **Given** an account's detail screen, **When** it opens, **Then** it shows the balance, a balance
   trend, money in and out for the month, and recent activity with a running balance after each row.
5. **Given** a stale account, **When** the user reconciles it by entering the real current balance,
   **Then** the staleness flag clears and any difference is recorded as an explainable adjustment
   rather than silently overwriting history.

---

### User Story 4 - See a single entry in full, with its history (Priority: P4)

A user opens one transaction and reads everything about it — including every change ever made to it
and who or what made the change.

**Why this priority**: Trust in the ledger comes from being able to answer "why does this say
Groceries when I remember Shopping?". Depends on Story 1's data; independently testable per entry.

**Independent Test**: Create a transaction, change its category, open its detail — confirm both the
creation and the category change appear in an ordered history with old and new values.

**Acceptance Scenarios**:

1. **Given** a transaction that has been created and later edited, **When** its detail opens,
   **Then** a history section lists every change in order, in plain language, naming what changed.
2. **Given** a transaction's detail, **When** it opens, **Then** amount, payee, date and time,
   cleared state, category, account, note and any attached receipt are readable without entering an
   edit mode first.
3. **Given** a transaction's detail, **When** the user chooses Duplicate, **Then** a new unsaved
   entry pre-filled from the original opens for review — nothing is written until it is saved.
4. **Given** a transaction's detail, **When** the user chooses "make recurring", **Then** the
   recurring setup opens pre-filled from that transaction.
5. **Given** any transaction is created, edited, re-categorised or deleted anywhere in the app,
   **When** the change completes, **Then** a history entry describing it is appended and can never
   be edited or removed.

---

### User Story 5 - Keep categories meaningful over time (Priority: P5)

A user tidies their categories — renaming ones that no longer read well, merging duplicates, and
marking categories that should never count as spending.

**Why this priority**: Category hygiene decides whether every downstream total is meaningful.
Independently testable against existing categories without touching transactions.

**Independent Test**: Rename a category and confirm its transactions are untouched; merge two
categories and confirm the confirmation states the exact number of transactions that will move and
that the total count is preserved afterwards.

**Acceptance Scenarios**:

1. **Given** a category with linked transactions, **When** it is renamed, **Then** every linked
   transaction remains linked and only the displayed label changes.
2. **Given** two categories holding N and M transactions, **When** the user merges them, **Then** a
   confirmation states that N+M transactions will move and that the action cannot be undone, and
   after confirming, no transaction is lost or unlinked.
3. **Given** the categories screen, **When** it opens, **Then** income and expense categories are
   separated with counts, and each row shows its spend and share of the total.
4. **Given** a category marked as excluded from spend (e.g. investment contributions), **When** any
   spend total or category share is computed, **Then** that category contributes nothing to it.
5. **Given** transactions with no category, **When** the categories screen opens, **Then** the count
   needing a category is stated with a path to fix them.

---

### User Story 6 - Let repeating money repeat, without losing control (Priority: P6)

A user sets up entries that repeat — rent, salary, SIP, subscriptions — sees what is coming in the
next 30 days, and approves each generated entry before it becomes part of their records.

**Why this priority**: Highest-effort slice and the only one that writes on the user's behalf, so it
ships last and behind an explicit approval step. Independently testable once transactions exist.

**Independent Test**: Create a recurring entry due today, confirm no ledger row is written, confirm
it appears as pending for review, accept it, and confirm the resulting ledger row's history states
it came from a recurring entry.

**Acceptance Scenarios**:

1. **Given** the full form with "make it recurring" enabled and a schedule set, **When** it is
   saved, **Then** a recurring definition is created and no duplicate immediate transaction is
   written alongside it.
2. **Given** a recurring entry whose next date has arrived, **When** the schedule is processed,
   **Then** a pending entry awaiting review is produced — never a ledger entry.
3. **Given** pending entries exist, **When** the recurring screen opens, **Then** a banner states
   how many need review and leads to them; accepting one writes the transaction with a history entry
   naming the recurring source, and dismissing one writes nothing.
4. **Given** recurring entries exist, **When** the recurring screen opens, **Then** it shows monthly
   money-in and money-out totals, a dated list of the next 30 days marking auto-debit versus
   variable-amount entries, and a separate paused section.
5. **Given** a recurring entry, **When** the user pauses it, **Then** it stops producing pending
   entries until resumed and moves to the paused section.

### Edge Cases

- A transfer between the user's own accounts must never appear in an expense total, a category
  share, or a budget's consumption — it moves money, it does not spend it.
- A credit-card account's balance is money owed; it must never increase "spendable now", and paying
  the card is a transfer, not an expense.
- Two transactions on the same day, or a transaction back-dated into a closed month: both are kept
  and land in the day and month their own date says, not the day they were entered.
- Deleting an account that has transactions: the user must be told what happens to those
  transactions before the deletion is confirmed, and no transaction may end up pointing at nothing.
- A month with no transactions must show an empty state inviting the first entry, never a screen of
  zeros with no explanation.
- Merging a category into itself, or merging while a filter is active, must not silently move a
  different set than the confirmation named.
- Editing a transaction that a recurring entry produced must not retroactively change the recurring
  definition, and vice versa.
- A pending recurring entry for a paused or deleted recurring definition must not remain actionable.
- Signed-out, offline with nothing cached, or a failed load on any Money screen must render a
  designed state — never a blank screen or a spinner that never resolves.
- A duplicate of an entry that has already been duplicated must not chain history from the original —
  the copy starts its own history at "created".

## Requirements *(mandatory)*

### Functional Requirements

**Recording**

- **FR-001**: Users MUST be able to record a transaction of exactly one of three types — expense,
  income, or transfer — with an amount, a category, an account, and a date.
- **FR-002**: Quick entry MUST reach a saved transaction in no more than three taps after the amount
  is entered, by pre-guessing category and account; both guesses MUST remain editable before saving.
- **FR-003**: A transfer MUST name both a source and a destination account, and MUST be excluded
  from every expense total, category share, and budget consumption figure in the app.
- **FR-004**: Users MUST be able to record every remaining detail through a full form: date and
  time, payee, note, receipt attachment, split across categories, a repeating schedule, and a link
  to a goal.
- **FR-005**: The full form MUST confirm before discarding unsaved changes.
- **FR-006**: Users MUST be able to edit and delete a transaction they recorded.

**History and trust**

- **FR-007**: Every transaction mutation — creation, edit, re-categorisation, deletion, or
  acceptance from a suggested source — MUST append an entry to that transaction's history stating
  what changed, in plain language.
- **FR-008**: History entries MUST NOT be editable or removable by any path in the app.
- **FR-009**: A transaction's detail MUST be readable without entering an edit mode, showing amount,
  payee, date and time, cleared state, category, account, note, receipt, and its full history.
- **FR-010**: Users MUST be able to duplicate a transaction into a new unsaved entry, and to turn an
  existing transaction into a recurring one, both pre-filled from the original.

**Reading the month**

- **FR-011**: The ledger MUST be scoped to a user-selectable month and show that month's income,
  expense and saved percentage in a summary that stays visible while scrolling.
- **FR-012**: Ledger rows MUST be grouped by day, with a per-day net, and each row MUST identify its
  payee/description, category, account and signed amount.
- **FR-013**: Users MUST be able to search the ledger by payee, description or note.
- **FR-014**: Users MUST be able to filter by type, one or more categories, an amount range, and
  account; the count of matching results MUST update as filters change, before the filter is
  applied.
- **FR-015**: Users MUST be able to reset all filters, and to save a filter combination as a named
  view that can be re-applied later.

**Accounts**

- **FR-016**: Users MUST be able to create and edit accounts of type bank, cash, wallet, or credit
  card, each with a display name, an optional masked identifier, and one account marked primary.
- **FR-017**: "Spendable now" MUST be the sum of bank, cash and wallet balances only; credit-card
  balances MUST be excluded from it.
- **FR-018**: Credit-card accounts MUST hold negative balances, be grouped under a heading that
  states the balance is owed and not held, and show credit limit, utilisation percentage and due
  date.
- **FR-019**: An account's detail MUST show current balance, a balance trend over time, money in and
  out for the selected month, and recent activity with a running balance after each row.
- **FR-020**: The system MUST flag an account whose balance has not been confirmed within the
  staleness threshold, on both the accounts list and the account detail, with an action to resolve
  it.
- **FR-021**: Reconciling an account MUST record the user-stated real balance, clear the staleness
  flag, and record any difference as an explainable adjustment with its own history entry — never as
  a silent overwrite.

**Categories**

- **FR-022**: Categories MUST be separated into income and expense sets, each with its count, and
  each row MUST show its spend and share of the total.
- **FR-023**: Renaming a category MUST preserve its identity and every transaction linked to it —
  only the label changes.
- **FR-024**: Merging two categories MUST move every transaction from one to the other, MUST be
  confirmed with a dialog stating the exact number of transactions that will move and that the
  action is irreversible, and MUST NOT be undoable afterwards.
- **FR-025**: A category MUST be markable as excluded from spend, and an excluded category MUST
  contribute nothing to any spend total, category share, or budget.
- **FR-026**: The system MUST surface how many transactions have no category, with a path to
  categorise them.

**Recurring**

- **FR-027**: Users MUST be able to define a recurring entry with a schedule, from the full form or
  from an existing transaction; creating one MUST NOT write an immediate duplicate transaction.
- **FR-028**: A recurring entry that comes due MUST produce a pending entry awaiting the user's
  review. No automated path may write directly into the ledger.
- **FR-029**: Users MUST be able to accept a pending entry — which writes the transaction with a
  history entry naming the recurring source — or dismiss it, which writes nothing.
- **FR-030**: The recurring screen MUST state how many entries need review, show monthly money-in
  and money-out totals, list the next 30 days by date marking auto-debit versus variable-amount
  entries, and list paused entries separately.
- **FR-031**: Users MUST be able to pause and resume a recurring entry; a paused entry produces no
  pending entries.

**Cross-cutting**

- **FR-032**: Every Money screen MUST define and render distinct empty, loading, error, offline and
  signed-out states — never a blank screen or an unresolving spinner.
- **FR-033**: Amounts MUST be recorded and totalled exactly, with no rounding drift between a
  transaction's amount and any total that includes it.
- **FR-034**: Once accounts exist, the home screen's upcoming-obligations list MUST include
  credit-card bill due dates alongside the loan/EMI obligations it already shows (the item deferred
  from the net-worth phase).
- **FR-035**: Category and account type values, once shipped, MUST NOT be renamed or removed; new
  ones may be added.

### Key Entities

- **Account**: A place money sits or is owed — bank, cash, wallet or credit card. Has a name, type,
  optional masked identifier, current balance, a primary flag, and (for credit) a limit and a due
  day. Carries the date its balance was last confirmed.
- **Transaction**: A single dated money movement of one type (expense, income, transfer) with an
  amount, a category, one account (two for a transfer), and optional payee, note, receipt, split
  allocation, goal link and cleared state.
- **Category**: A user-facing grouping for transactions, belonging to either the income or expense
  set, optionally excluded from spend, optionally holding sub-categories. Identity survives renaming.
- **Transaction history entry**: An append-only, human-readable record of one change to one
  transaction — what changed, from what, to what, when, and from which source (manual, recurring,
  imported).
- **Recurring definition**: A template plus a schedule that produces pending entries, never ledger
  entries. Can be paused, resumed and deleted; knows its next due date and whether its amount is
  fixed (auto-debit) or variable.
- **Pending entry**: A proposed transaction awaiting the user's accept or dismiss. Not part of any
  total until accepted.
- **Saved view**: A named filter combination a user can re-apply to the ledger.
- **Reconciliation**: A user-stated real balance for an account at a point in time, plus any
  adjustment it produced.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can record a routine expense in three taps after entering the amount, and
  complete the whole entry in under 15 seconds.
- **SC-002**: Transfers contribute exactly zero to every expense total, category share and budget
  figure — verified across 100% of transfer scenarios.
- **SC-003**: 100% of transaction mutations produce a history entry; zero mutations are observable
  without one.
- **SC-004**: "Spendable now" equals the sum of bank, cash and wallet balances exactly, in 100% of
  account mixes including credit cards.
- **SC-005**: A category merge preserves every transaction — the transaction count across the two
  categories before the merge equals the count in the surviving category after it, every time.
- **SC-006**: Zero ledger entries are ever created by a recurring definition without an explicit
  user acceptance.
- **SC-007**: The result count shown before applying a filter matches the number of rows returned
  after applying it, in 100% of filter combinations.
- **SC-008**: Every Money screen renders a correct empty, offline, signed-out or loaded state — 0%
  of sessions show a blank or permanently-loading Money screen.
- **SC-009**: A ledger month containing 5,000 transactions opens and scrolls without visible stutter.
- **SC-010**: A user can go from opening the app to knowing this month's income, expense and savings
  rate without leaving the ledger's first screen.

## Assumptions

- **Scope**: this feature covers the Money tab (D1–D9) only — day-to-day transactions, accounts,
  categories and recurring definitions. Budgets, goals, insurance, retirement (Phase 4), insights
  and statements (Phase 5), global search and notifications (Phase 6), and SMS/account-aggregator
  automation (Phase 7) are out of scope and are specified with their own phases.
- **Prerequisites**: sign-in, DPDP consent and the net-worth data already shipped (Phases 1–2).
  This feature assumes that state exists and only specifies its own signed-out/offline fallbacks.
- **Budget impact deferred**: the transaction detail's "budget impact" line (e.g. `Groceries · 68%
  used`) requires budgets, which do not exist until Phase 4. Transaction detail ships this phase
  **without** that line; Phase 4 adds it back as an explicit follow-up (implementation plan §7,
  Phase 3/4 scoped-dependency notes). Everything else on the detail screen ships now.
- **Credit-card screens fold into accounts**: the design's route map lists `Credit cards › Card
  detail › Card statement` under Money, but only D1–D9 were drawn (functional spec §8 open item 2a).
  This spec assumes credit cards are represented as credit-type accounts in the accounts list and
  account detail — no separate credit-card screens, and no card-statement screen, ship this phase.
  If the maintainer wants dedicated card screens, that is a scope addition needing its own screen
  requirements, not a silent build.
- **Review queue is recurring-only this phase**: the shared review queue that also handles SMS and
  account-aggregator suggestions ships with automation (Phase 7). This phase ships review limited to
  recurring-generated pending entries, reached from the recurring screen's banner. The rule that
  nothing posts without acceptance holds identically in both.
- **Staleness threshold**: an account balance is treated as needing a check 30 days after it was
  last confirmed. Chosen as a reasonable default — the design states the behaviour ("Reconciled 28
  Jul · needs check") but not the number.
- **Split transactions** ship in their minimal useful form: one transaction allocated across two or
  more categories whose parts sum exactly to the transaction's amount. Nested splits, per-part
  payees and per-part receipts are out of scope.
- **Receipts** are attached to a transaction and viewable from its detail. They are user financial
  records and fall under the sync consent already granted in Phase 1 — this phase introduces no new
  consent class. Where and how they are stored is a `plan.md` decision.
- **Automatic balance refresh does not exist**: all balances are user-maintained through entry and
  reconciliation. The accounts screen states this, as the design requires. Account linking arrives
  with Phase 7.
- **Single currency** (Indian rupee) for all amounts, consistent with the schema decision already
  taken for the tracker domain.
- **Transfers reported separately** ("moved, not spent") is a reporting surface in Phase 5. This
  phase guarantees the underlying exclusion (FR-003); the separate reporting line is Phase 5's.
