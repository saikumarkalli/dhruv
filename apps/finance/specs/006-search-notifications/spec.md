# Feature Specification: Search & Notifications (Phase 6)

**Feature Branch**: `006-search-notifications`

**Created**: 2026-08-22

**Status**: Draft

**Input**: User description: "Phase 6" — design-v1 Phase 6, Search & notifications (screens B2, B3):
the notification centre and cross-entity global search, plus delivery of the alerts that Phases 3, 4
and 5 stored preferences for but deliberately did not send. QA catalog module `SRC` (5 rows).

**Source of truth**: `apps/finance/docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md`
§5 Group B (screens B2, B3), navigation contract §4 (N5, N6), offline posture D-6, and
NFR-1/2/4/5/6/8. QA rows: `SRC-*` (5) §10 of `2026-08-09-qa-test-scenario-catalog.md`. Route,
channel, intent and settings rows: `2026-08-09-finance-surface-registries.md` §1 (the Notifications
and Search rows, both marked Phase 6), §2 (the notification channel registry), §3 (the intent action
registry), §4 (the Modules tier, where each alert control lives with its owning module). Settings
behaviour is inherited from `apps/finance/specs/004-settings/` (notification master switch,
permission state, app lock hold-and-dispatch, hide-amounts masking). The deferred obligations this
feature discharges are recorded in `apps/finance/specs/003-plan-live-modules/research.md` R8 and in
`apps/finance/specs/005-insights/spec.md` (Clarifications, monthly summary). This document restates
that material as spec-kit's `spec.md` (what and why only) — storage, scheduling, module topology and
component work are `plan.md`, written separately.

**Position in the build**: Phase 6 is the first phase that makes the app *speak first*. Every phase
before it is something the user opens; this one reaches out. It adds no new user-entered data of its
own — search reads what Phases 2–4 recorded, and every alert is driven by a preference an earlier
phase already captured and left unconsumed on purpose (budget threshold, renewal days-before,
value-update staleness, monthly summary). It also completes two screens the shell has carried as
placeholders since Phase 0: the notification centre currently renders an honest empty state, and
there is no global search entry point at all.

## Clarifications

### Session 2026-08-22

- Q: Which alert types does Phase 6 actually deliver — every channel in the registry, or only those
  whose owning module and destination screen have both shipped by Phase 5? → A: The five that are
  ready — budget threshold breach, instalment due, policy renewal due, holding value overdue, monthly
  summary. Transactions-to-review is not delivered here: its review queue is a Phase 7 screen, so the
  alert would point at nothing. Daily rates and app updates stay with the currency module and app
  details, which own those channels; this feature does not absorb another module's alert.
- Q: What populates the notification centre — every alert the app generates, or only those the
  operating system was actually permitted to display? → A: Every alert raised. The centre is the
  app's own durable record and the system notification is its delivery, not its source — so a user
  who denied notification permission, or who has the app-wide master switch off, still finds a
  complete history in the app rather than a permanently empty screen. A consequence follows and is
  recorded as an assumption: granting permission later does not replay past alerts into the
  notification shade, because they were never pending there.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Find anything by name, from one field (Priority: P1)

A user who has been recording money for months remembers a thing but not where they put it — an
insurer's name, a fund, a goal, a payment to a particular shop. They open one search field, type a
few letters, and see everything that matches across all four kinds of record at once, with a count
per kind so they know immediately whether what they want is a holding or a transaction.

**Why this priority**: It is the only user story here that stands entirely alone. It needs no alert
pipeline, no scheduling, no permission, and no preference from an earlier phase — only records that
already exist. Shipped by itself it is a complete, useful feature, and it is the half of Phase 6 the
user reaches for deliberately rather than the half that reaches for them.

**Independent Test**: Record at least one holding, transaction, goal and policy sharing a common
substring, open search, type it, and confirm the chip counts match the grouped results and that each
result opens the right detail screen.

**Acceptance Scenarios**:

1. **Given** records of all four kinds exist and several match a query, **When** the user types that
   query, **Then** results appear grouped by kind, and the filter chips show a total plus a per-kind
   count that equals the number of results actually listed in each group.
2. **Given** results are showing, **When** the user selects a single-kind chip, **Then** only that
   kind's results remain and the total chip restores all of them.
3. **Given** a result of each kind, **When** each is tapped in turn, **Then** the app opens that
   entity's own detail screen — a holding to holding detail, a transaction to its record, a goal to
   goal detail, a policy to policy detail.
4. **Given** a query that matches nothing, **When** the results resolve, **Then** the screen states
   that nothing matched, echoes the query, and states which kinds of record were searched.
5. **Given** the user has no session or no connectivity, **When** search is opened, **Then** the
   designed signed-out or offline state appears rather than an empty result list or a spinner.

---

### User Story 2 - Be told a budget broke, without opening the app (Priority: P2)

A user set "alert me at 80%" on a category budget in Phase 4. Spending crosses that line mid-month.
The phone tells them, in one line naming the category and how far through the budget they are, and
tapping it opens that budget.

**Why this priority**: This is the deferred promise with the most user value and the clearest stored
preference behind it — the control has been on screen since Phase 4 doing nothing. It is also the
first alert to exercise the whole chain end to end: a stored preference, a periodic evaluation, a
posted alert, a centre entry, and a deep link to a real destination.

**Independent Test**: With a budget carrying a threshold, push recorded spending past that threshold,
wait for the next evaluation, and confirm one alert arrives, appears in the centre, and opens the
budget when tapped.

**Acceptance Scenarios**:

1. **Given** a budget with a stored threshold and spending below it, **When** spending crosses the
   threshold, **Then** exactly one alert is generated naming the category and its position against
   the budget.
2. **Given** that alert has been generated, **When** further spending is recorded in the same period
   without crossing a further stated threshold, **Then** no duplicate alert is generated for the same
   condition in the same period.
3. **Given** a new budget period begins, **When** the threshold is crossed again, **Then** a fresh
   alert is generated — the condition has recurred, not repeated.
4. **Given** the alert is showing, **When** it is tapped, **Then** the app opens that budget's detail
   screen, not a generic list.
5. **Given** the budget's alert control is switched off, **When** the threshold is crossed, **Then**
   no alert is generated at all.

---

### User Story 3 - See everything the app told me, in one place (Priority: P3)

A user who dismissed a notification, or who was away from the phone, opens the notification centre
and finds every alert the app has raised, newest first, split into what happened today and what
happened earlier, with unread ones distinguishable and a single action to clear that distinction.

**Why this priority**: The centre is the durable record; the system notification is the transient
one. Without it, a dismissed alert is lost. It depends on at least one alert type existing, which is
why it follows US2 rather than leading.

**Independent Test**: Generate alerts dated today and earlier, open the centre, confirm the grouping
and the unread marks, tap "Mark all read", restart the app, and confirm all are still read.

**Acceptance Scenarios**:

1. **Given** alerts exist dated today and on earlier days, **When** the centre is opened, **Then**
   they are grouped into a today group and an earlier group by the device's local calendar date, each
   group newest first.
2. **Given** unread alerts exist, **When** "Mark all read" is used and the app is restarted, **Then**
   every one of them is still marked read.
3. **Given** an unread alert, **When** it is opened from the centre, **Then** it becomes read and the
   app navigates to its subject.
4. **Given** no alert has ever been generated, **When** the centre is opened, **Then** the designed
   empty state appears with no fabricated sample content.
5. **Given** unread alerts exist, **When** the user is anywhere in the app, **Then** the unread count
   is visible on the entry point that leads to the centre.

---

### User Story 4 - Tap an alert and land on exactly the thing it is about (Priority: P4)

Every alert the app raises is about one specific record. Tapping it — from the notification shade or
from the centre — opens that record, even if the app was closed, even if the app is locked, and
without ever landing the user on a blank screen because the record has since been deleted.

**Why this priority**: This is what separates an alert from a nag. It is stated separately from US2
because it must hold for every alert type, including ones added later, and because the locked-app and
missing-record paths are the ones most likely to be skipped and most likely to crash.

**Independent Test**: With one alert of each delivered type, open each from a cold start and confirm
the destination; repeat with app lock on; repeat after deleting the subject record.

**Acceptance Scenarios**:

1. **Given** one alert of each delivered type, **When** each is opened from a cold app start,
   **Then** the app lands on that alert's own subject screen, with the owning tab selected beneath it
   so the back gesture returns to that tab's root.
2. **Given** app lock is on, **When** an alert is opened, **Then** the app requires authentication
   first and then navigates to the subject — the destination is not lost and is not reachable before
   unlocking.
3. **Given** an alert whose subject record has since been deleted, **When** it is opened, **Then**
   the app shows that record's normal not-found state and does not crash.
4. **Given** an alert carrying a quick action, **When** that action is used from the notification
   itself, **Then** it produces the same result as performing it inside the app, and never more than
   two such actions are offered.

---

### User Story 5 - Obligations tell me before they are due, not after (Priority: P5)

A loan instalment falls due; a policy comes up for renewal. Both were configured with a reminder
offset in Phase 4 that has never fired. The user is reminded ahead of the date, by the number of days
they chose, with the name and date but no amount on the lock screen.

**Why this priority**: It discharges the second and third of Phase 4's deferred preferences. It rides
entirely on the delivery pipeline US2 establishes, so it is additive rather than foundational.

**Independent Test**: Set a reminder offset on a liability and a policy, advance to the offset date,
and confirm one reminder each, carrying name and date only, deep-linking to the liability and the
policy respectively.

**Acceptance Scenarios**:

1. **Given** a liability with an instalment date and a reminder preference, **When** the offset is
   reached, **Then** one reminder is raised naming the liability and the due date.
2. **Given** a policy with a renewal date and a stored days-before offset, **When** that offset is
   reached, **Then** one reminder is raised naming the policy and the renewal date.
3. **Given** either reminder, **When** it is displayed, **Then** it shows no amount, per the stated
   masking rule for that alert type.
4. **Given** either reminder, **When** it is opened, **Then** the app opens that liability or that
   policy.
5. **Given** the obligation is settled before the date, **When** the offset is reached, **Then** no
   reminder is raised for it.

---

### User Story 6 - Periodic nudges: values going stale, and the month in summary (Priority: P6)

Holdings the user has not revalued for a long time are flagged; at the start of a month, a summary of
the one just closed arrives for users who asked for it in Phase 5.

**Why this priority**: Lowest-urgency alerts, and the monthly summary depends on Phase 5 having
shipped. Both are single-preference, single-destination cases once the pipeline exists.

**Independent Test**: Age a holding's last valuation past the configured staleness window and confirm
one flag; enable the monthly summary preference, roll into a new month, and confirm one summary
deep-linking to that month's report.

**Acceptance Scenarios**:

1. **Given** a holding whose last recorded value is older than the configured window, **When** the
   check runs, **Then** one alert names the holdings that are stale and their age, without amounts.
2. **Given** the monthly summary preference is on, **When** a month closes, **Then** one summary
   alert is raised for that month and opens the report for that same month.
3. **Given** the monthly summary preference is off, **When** a month closes, **Then** nothing is
   raised.
4. **Given** privacy mode is on, **When** any of these is displayed, **Then** amounts are masked while
   percentages, counts and dates stay readable.

---

### User Story 7 - Silence any of it, from the module that owns it (Priority: P7)

Every alert type is controllable in exactly one place — inside the module that raises it — plus one
app-wide switch that silences all of them, and an honest banner when the operating system has
withdrawn permission entirely.

**Why this priority**: The controls themselves already exist from Phase 4, Phase 5 and the Settings
control plane; what this story adds is that they now actually govern something. It is last because it
is verification of an inherited contract rather than new surface.

**Independent Test**: Turn each module's alert control off in turn and confirm that type stops; turn
the app-wide master off and confirm all stop; deny permission at the system level and confirm the
banner appears and no alert is displayed.

**Acceptance Scenarios**:

1. **Given** a module's alert control is switched off, **When** its condition occurs, **Then** that
   alert type is not raised, and no other type is affected.
2. **Given** the app-wide notification master is off, **When** any condition occurs, **Then** nothing
   is displayed by the system for any type.
3. **Given** notification permission is denied at system level, **When** the user opens the
   notification area of Settings, **Then** the state is stated plainly with a route to system
   settings, and the app does not repeatedly re-prompt.
4. **Given** every delivered alert type, **When** Settings is inspected, **Then** each has exactly one
   control, located with its owning module, and appears nowhere else.

---

### Edge Cases

- **A query matching nothing but resembling an amount** — search states what it searches over, so a
  user typing `4500` is told amounts are not searched rather than being shown an empty list with no
  explanation.
- **A query matching hundreds of records** — counts stay accurate and the list stays responsive; the
  chip count is the true total, not the number currently rendered.
- **Records the user deleted** — excluded from search results and never the destination of an alert
  raised afterwards; an already-raised alert pointing at one lands on its not-found state.
- **A closed account, a matured policy, a completed goal** — still findable, and labelled as such, so
  a user searching historical records is not told they do not exist.
- **The device is off, or the app has not been opened for a week** — the conditions that came due in
  that window are evaluated when the device is next able to; the user is not silently skipped, and is
  not buried under a backlog of one alert per missed day for the same condition.
- **The same condition on two devices signed into the same account** — the user may be alerted twice
  for one condition; this is a stated consequence of on-device evaluation, not a defect (see
  Assumptions).
- **Clock or timezone changes** — today/earlier grouping and "due in N days" use the device's local
  calendar date at read time, so a timezone change re-groups rather than corrupting stored records.
- **A budget crossing several thresholds at once** (a single large expense taking a category from 40%
  to 130%) — this raises the alerts for the thresholds actually configured, not one per percentage
  point crossed.
- **Permission granted after alerts were already raised** — the centre already holds them; grant does
  not replay them into the notification shade.
- **A module disabled after its alerts were raised** — no new alerts of that type; existing centre
  entries remain readable, and opening one lands on the disabled-feature state rather than a crash.
- **An alert type whose destination screen does not exist yet** — that type is not delivered at all in
  this phase rather than delivered pointing at nothing.
- **The centre's retention boundary** — an alert older than the retention window is dropped rather
  than accumulating indefinitely; nothing else in the app depends on it still being there.

## Requirements *(mandatory)*

### Functional Requirements

**Global search (B3)**

- **FR-001**: The app MUST provide one global search entry point, reachable from the Home tab's top
  bar, opening a single search field. Scoped searches that already exist inside individual screens are
  not replaced by it and are not part of this feature.
- **FR-002**: Search MUST cover exactly four kinds of record — transactions, holdings, policies and
  goals — and MUST state that scope to the user when nothing matches.
- **FR-003**: Search MUST present a per-kind result count and a total count as selectable filters, and
  each count MUST equal the number of results actually listed for that kind.
- **FR-004**: Selecting a per-kind filter MUST narrow the results to that kind; selecting the total
  MUST restore all kinds.
- **FR-005**: Results MUST be grouped by kind, each result carrying a secondary line appropriate to
  its kind that distinguishes it from a similarly-named record.
- **FR-006**: Selecting any result MUST open that record's own detail screen.
- **FR-007**: Matching MUST be against the human-readable text the user themselves entered when
  recording — names, descriptions, notes and counterparties — case-insensitively and on partial words.
  Amounts and dates are not searched.
- **FR-008**: Search MUST distinguish "nothing typed yet" from "nothing matched", and the
  nothing-matched state MUST echo the query.
- **FR-009**: Search MUST exclude records the user has deleted, and MUST include closed, matured or
  completed records, labelled as such.
- **FR-010**: Search results MUST reflect the records as they currently stand, not a snapshot from an
  earlier visit to the screen.
- **FR-011**: Search MUST render the signed-out, offline, not-configured and disabled states as
  designed states, never as an empty result list or an unresolving spinner.

**Notification centre (B2)**

- **FR-012**: The centre MUST list every alert this feature has raised for the signed-in user, newest
  first, grouped into today and earlier by the device's local calendar date.
- **FR-013**: Each entry MUST state its type, a single readable line describing the condition, its
  subject, and when it was raised.
- **FR-014**: The centre MUST distinguish read from unread, MUST offer a single action marking all
  read, and read state MUST survive an app restart.
- **FR-015**: Opening an entry MUST mark it read and navigate to its subject.
- **FR-016**: An alert MUST be recorded in the centre when it is raised, independently of whether the
  operating system was permitted to display it — a user who has denied notification permission, or
  who has the app-wide master switch off, still finds a complete record in the app.
- **FR-017**: The unread count MUST be visible on the entry point that leads to the centre.
- **FR-018**: Entries MUST be retained for 90 days and then dropped; the centre MUST NOT grow without
  bound.
- **FR-019**: With no alerts ever raised, the centre MUST show its designed empty state and MUST NOT
  present sample or illustrative content.

**Raising alerts**

- **FR-020**: The app MUST raise alerts for the conditions whose preferences earlier phases already
  store and whose destination screens exist: budget threshold breach, instalment due, policy renewal
  due, holding value overdue for update, and monthly summary available — and MUST NOT raise any other
  registered alert type. Transactions-to-review, daily rates and app updates are explicitly not this
  feature's to raise (see Assumptions).
- **FR-021**: Each alert type MUST be driven by the preference its owning module already stores. This
  feature MUST NOT introduce a second preference for the same thing, and a preference set months
  earlier MUST be honoured from this feature's first run without asking the user to set it again.
- **FR-022**: Conditions MUST be evaluated at least once per day without requiring the user to open
  the app.
- **FR-023**: A single condition MUST raise a single alert. Re-evaluating an unchanged condition MUST
  NOT raise a duplicate; the same condition recurring in a later period MUST raise a new one.
- **FR-024**: An alert MUST NOT be raised for a module that is disabled, absent or version-gated.
- **FR-025**: Each alert type MUST honour the money-masking rule stated for it, and privacy mode MUST
  mask amounts in every alert while leaving percentages, counts and dates readable.
- **FR-026**: An alert MUST carry at most two quick actions, and a quick action MUST produce exactly
  the same result as performing that action inside the app.
- **FR-027**: A missed evaluation window MUST NOT produce one alert per missed day for the same
  condition; the user is told the current state once, not the history of it.

**Opening the app from an alert**

- **FR-028**: Every alert MUST open its own subject: a budget alert to that budget, an instalment
  reminder to that liability, a renewal reminder to that policy, a value-update alert to that holding,
  a monthly summary to the report for that month.
- **FR-029**: An alert MUST land on the owning tab's root with the subject pushed above it, so the
  back gesture returns to that tab rather than exiting the app.
- **FR-030**: An unknown, foreign or deleted subject MUST resolve to that record's normal not-found
  state and MUST NOT crash the app or the shell.
- **FR-031**: When app lock is on, an alert's destination MUST be held until the user authenticates
  and then dispatched; content MUST NOT be reachable before authentication and the destination MUST
  NOT be lost.

**Controls**

- **FR-032**: Each delivered alert type MUST have exactly one control, in the Settings entry of the
  module that owns it, and MUST NOT appear in any central alerts list.
- **FR-033**: The app-wide notification master switch MUST suppress display of every type when off.
- **FR-034**: When system notification permission is denied, the app MUST state that plainly with a
  route to system settings and MUST NOT repeatedly re-prompt.
- **FR-035**: Turning one type off MUST NOT affect any other type.

**Cross-cutting**

- **FR-036**: Both screens MUST be fault-isolated behind their own feature flag entry, rendering the
  disabled state rather than failing the shell.
- **FR-037**: Neither screen MUST send anything off-device beyond what the existing consent switch
  already permits, and no alert content MUST leave the device.
- **FR-038**: Every deferred scenario this feature discharges MUST be closed in the QA catalog, and
  every one it cannot discharge MUST be re-deferred with a stated reason rather than left unticked.

### Key Entities

- **Search result**: one matched record, carrying its kind, its display name, a kind-appropriate
  secondary description, and the identity needed to open it.
- **Search scope**: the fixed set of four record kinds searched, and the fields within them that are
  matched — stated to the user, not implicit.
- **Alert**: one raised notification — its type, subject, the readable line describing the condition,
  the moment it was raised, whether it has been read, and its masking rule.
- **Alert condition**: the evaluable rule behind a type — a budget's threshold, a policy's
  days-before offset, a valuation's age, a month having closed — owned by the module that stores it.
- **Alert channel**: the user-facing grouping an alert belongs to, its importance, and its single
  Settings control, held by the owning module.
- **Alert destination**: the record an alert is about and the screen that shows it, resolvable to a
  not-found state when the record no longer exists.

### Scope Boundaries

**In scope**

- Global search across transactions, holdings, policies and goals, with per-kind counts and
  navigation to each result.
- The notification centre: grouping, read state, mark-all-read, retention, empty state, unread count.
- Raising, de-duplicating and displaying the alert types whose preferences and destinations already
  exist.
- Opening the app from an alert, including the locked and missing-record paths.
- Verifying that each delivered type is governed by its existing module control and by the app-wide
  master switch.

**Out of scope**

- Any new user-entered data. This feature stores preferences for nothing; it consumes preferences
  earlier phases already captured.
- Automation sources — bank SMS parsing, account aggregator, price feeds — and the review queue they
  feed. Those are Phase 7, and the transactions-to-review alert has no destination until then.
- Market price alerts on holdings. No price feed exists; the value-update alert is about a value the
  user has not refreshed, not about a price that moved.
- Server-pushed notifications and any cross-device coordination of them.
- Changing the Settings control plane, the alert channel registry's tiers, or where a class of control
  lives.
- Scoped searches inside individual screens, and search over calculator history, settings or help.
- Snoozing, per-alert muting, quiet hours, and notification grouping/summarisation beyond what the
  platform does by default.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For at least three independent query fixtures matching all four record kinds, every
  filter count equals the number of results listed under that kind, with no discrepancy.
- **SC-002**: For a result of each of the four kinds, selecting it opens that record's own detail
  screen — four for four, with no result reaching a list, a wrong record, or a dead end.
- **SC-003**: A user who knows only a record's name finds it through search in a single query, without
  first knowing which tab or kind of record it is.
- **SC-004**: Alerts dated today and on earlier days group correctly by the device's local calendar
  date for at least three date fixtures, including one spanning a midnight boundary.
- **SC-005**: After marking all read and restarting the app, zero previously-read alerts appear
  unread.
- **SC-006**: For one alert of every delivered type, opening it lands on its stated subject — with the
  owning tab beneath it — in 100% of attempts, from both a cold start and a warm resume.
- **SC-007**: With app lock on, no alert destination is reachable before authentication, and no
  destination is lost by locking; verified for every delivered type.
- **SC-008**: Opening an alert whose subject has been deleted produces that record's not-found state
  in 100% of attempts and zero crashes.
- **SC-009**: A condition that stays true across repeated evaluations produces exactly one alert; the
  same condition recurring in a later period produces exactly one more. Verified over at least three
  consecutive evaluation cycles per delivered type.
- **SC-010**: With a module's alert control off, zero alerts of that type are raised while every other
  type continues unaffected; verified for each delivered type in turn.
- **SC-011**: With the app-wide master switch off, zero alerts are displayed for any type.
- **SC-012**: With privacy mode on, no amount is legible in any alert, on screen or on the lock
  screen, while percentages, counts and dates remain readable.
- **SC-013**: Each alert type's masking rule is honoured exactly as stated for it — the reminders that
  are specified to carry no amount carry none, in 100% of samples.
- **SC-014**: Every delivered alert type has exactly one control in Settings, located with its owning
  module; a full sweep of Settings finds zero duplicates and zero orphans.
- **SC-015**: Both screens render their signed-out, offline, not-configured, disabled and empty states
  as designed states, with no state producing an unresolving spinner.
- **SC-016**: Search results appear without blocking interaction on a data set representing a year of
  daily recording plus a full portfolio of holdings, goals and policies.
- **SC-017**: After a period with the device unavailable, each condition that came due produces at
  most one alert rather than one per elapsed day.
- **SC-018**: Every `SRC-*` scenario row is closed, or explicitly deferred with a stated reason;
  zero rows are left silently unticked at the checkpoint.

## Assumptions

- **Phases 2, 3, 4 and 5 have shipped.** Search reads holdings, transactions, goals and policies;
  every alert reads a preference or a destination those phases own. This feature cannot be built
  against records and screens that do not exist.
- **Alerts are evaluated on the device, from data the app already holds.** Nothing is pushed from a
  server. The consequences are accepted and stated rather than engineered away: evaluation happens
  when the device allows it rather than at an exact instant, and a user signed in on two devices may
  be alerted twice for one condition.
- **The preferences are already populated.** Phase 4 shipped the budget threshold and the renewal
  offset storing real user choices with nothing consuming them, precisely so this phase would launch
  against configured data rather than an empty table. Phase 5 did the same for the monthly summary.
  This feature reads them as they stand and adds no migration prompt.
- **"Rate alert" in the design means a value that has gone stale, not a market price that moved.**
  No price feed exists before Phase 7, and its stated destination is the holding itself, which matches
  the value-update-due channel. A genuine market-price alert is out of scope and would need its own
  source.
- **Transactions-to-review has no destination until Phase 7.** Its review queue is a Phase 7 screen,
  so delivering the alert now would point at nothing.
- **Daily rates and app updates belong to the modules that own them** — the currency module and app
  details — and are their work to raise, not this feature's, even though both channels are already
  named in the registry.
- **Notification permission, the app-wide master switch, app lock hold-and-dispatch, and privacy-mode
  masking are inherited from the Settings control plane**, already specified and shipped there. This
  feature consumes them and verifies them against real alerts; it does not re-specify or duplicate
  them.
- **The notification centre is a per-user record, not a per-device one in the user's mental model**,
  but is stored and retained on the device that raised the alert; alerts raised on another device are
  not expected to appear.
- **90 days is the retention window** for centre entries — long enough that a returning user finds
  their history, short enough that the list stays bounded. Nothing else depends on an entry surviving
  past it.
- **Search is session-backed and consent-gated**, like every other Home-tab surface, per the offline
  posture decision; offline search over previously fetched records is not assumed.
- **Search matches text the user wrote, not derived or computed text** — a category's name matches
  because the user named it, but a computed status or a formatted amount does not.
- **Both screens' feature-flag entries do not exist yet** and are added with this feature, consistent
  with every other module.
- **The notification centre screen exists today as an honest empty state** and the global search entry
  point does not exist at all; this feature replaces the former and adds the latter.

## Dependencies

- **Phase 2 (net worth)** — holdings and liabilities: search targets, and the subjects of value-update
  and instalment alerts.
- **Phase 3 (money)** — transactions: search targets, and the spending that budget evaluation reads.
- **Phase 4 (plan live modules)** — budgets, goals, policies: search targets, the stored budget
  threshold and renewal offset, and the budget, goal and policy destination screens.
- **Phase 5 (insights)** — the stored monthly-summary preference and the month report the summary
  alert opens.
- **Settings control plane (004)** — the notification permission state and banner, the app-wide master
  switch, app lock hold-and-dispatch, hide-amounts masking, and the module-owned contribution
  mechanism every alert control uses.
- **The channel, route and intent registries** — this feature adds destination rows for the alerts it
  delivers and MUST NOT introduce a deep-link target that is not registered.

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
