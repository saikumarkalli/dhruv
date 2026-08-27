# Feature Specification: Plan Live Modules (Phase 4)

**Feature Branch**: `003-plan-live-modules`

**Created**: 2026-08-19

**Status**: Draft

**Input**: User description: "Design-v1 Phase 4 — Plan live modules (screens E1 revised, E2–E9):
Plan root leading with live planning, budgets and budget detail, goals and goal detail, debt payoff,
insurance and policy detail, retirement projection. Business rules BR-E1..BR-E4, QA catalog modules
PLN (13 rows), INS (4 rows), RET (4 rows)."

**Source of truth**: `apps/finance/docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md`
§5 Group E (screens E1–E9), business rules BR-E1..BR-E4, flows F-4 and F-5, and NFR-1/3/4/8. QA
rows: `PLN-*` (13) §5, `INS-*` (4) §6, `RET-*` (4) §7 in `2026-08-09-qa-test-scenario-catalog.md`.
Navigation shape for the Plan root is fixed by ADR-0027. This document restates that material as
spec-kit's `spec.md` (what/why only) — schema, module topology, calculation engines, networking and
component work are `plan.md`, written separately.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Know whether this month's spending is on pace (Priority: P1)

A user who has been recording spending opens Budgets and reads, in one screen, how much of the month
they have used, how much of the budget they have used, and whether those two are in step — stated in
words, not left to be inferred from a colour.

**Why this priority**: A budget that only shows a percentage tells the user nothing they could not
count themselves. The pace comparison is the whole product idea, it is the module the user will open
most often, and it is the only Plan module that reads data the user already produces daily.

**Independent Test**: With one month of recorded transactions and a budget per category, open
Budgets — confirm the pace statement's number is reproducible by hand from spend fraction versus
elapsed-day fraction, and that a category over budget states the overage amount and the days left.

**Acceptance Scenarios**:

1. **Given** a calendar month budget and transactions in it, **When** Budgets opens on day N of a
   month of M days, **Then** the pace comparison uses N/M as the elapsed fraction and the stated
   "spending X% faster/slower than the month" figure matches that comparison exactly.
2. **Given** the budget overview, **When** it opens, **Then** it shows the percentage used, the
   amount left, the total, and the days remaining in the period.
3. **Given** a category whose spend fraction exceeds the elapsed-day fraction, **When** its bar is
   rendered against the month-position marker, **Then** it is shown as ahead of pace; a category at
   or below the marker is not.
4. **Given** a category that is over its budget, **When** it is displayed, **Then** the overage is
   stated in words and money ("Over by ₹1,000 with 9 days left") rather than implied by styling
   alone.
5. **Given** a category marked as excluded from spend, **When** any budget total or category figure
   is computed, **Then** that category contributes nothing to it.
6. **Given** a transfer between the user's own accounts, **When** budget consumption is computed,
   **Then** the transfer consumes no budget.

---

### User Story 2 - Recover from a breached budget (Priority: P2)

A user who has gone over on a category opens that budget's detail and is told, in their own numbers,
what got them there and what would happen if they carried on — then either raises the budget or asks
to be warned earlier next time.

**Why this priority**: Being told you overspent after the fact is a complaint, not a feature. This
story is what converts the budget from a report into something that changes a decision. It depends
on Story 1's data but is separately testable on any single category.

**Independent Test**: Open a category over its budget, confirm the recovery insight's stated amounts
recompute exactly from that category's own recent transactions, then raise the budget and confirm
the overage figure updates.

**Acceptance Scenarios**:

1. **Given** a category with recent transactions and a current overage, **When** its detail opens,
   **Then** the recovery insight's stated average and projected overage recompute exactly from those
   same transactions.
2. **Given** the recovery insight, **When** it is displayed, **Then** it is visually labelled as a
   derived insight, not presented as plain fact.
3. **Given** a budget detail, **When** it opens, **Then** it shows spend versus budget, the over or
   under amount, days left, the last six months as bars, and that category's recent transactions.
4. **Given** a budget detail, **When** the user chooses "Raise budget", **Then** the new amount
   applies to the current period and every derived figure on the screen updates.
5. **Given** a budget detail, **When** the user chooses "Alert me at 80%", **Then** that threshold is
   stored against the budget and is visible as set when the screen is reopened.
6. **Given** an over-budget category, **When** the user follows the path from the alert into the
   detail and on to that category's transactions, **Then** the chain works end to end and the
   transactions shown are exactly that category's, in that period.

---

### User Story 3 - Track goals from what you already own, without moving money (Priority: P3)

A user names a goal, links holdings they already own to it — whole, or a stated fraction of one —
and sees progress that is simply the current value of those holdings. Nothing is transferred, locked,
or duplicated.

**Why this priority**: This is the design's most distinctive claim and the one most likely to be
misunderstood as "the app moved my money". Getting it right early sets the trust model for every
later planning feature. Independently testable with holdings alone, before any budget exists.

**Independent Test**: Create a goal, link one holding whole and one by an earmarked quantity, confirm
progress equals the sum of those two contributions, and confirm no transaction was created by the
linking.

**Acceptance Scenarios**:

1. **Given** a goal linked to one whole holding and one earmarked fraction of another, **When**
   progress is computed, **Then** it equals the sum of the current value of the whole holding and the
   earmarked fraction's current value.
2. **Given** any goal link is created or removed, **When** the operation completes, **Then** no
   transaction is written and no holding's own value or quantity changes.
3. **Given** the goals list, **When** it opens, **Then** it shows total saved towards goals against
   total target, the count of active goals, and per goal the percentage, saved of target, target
   date and status.
4. **Given** three goals — one on track, one short, one with nothing linked — **When** the list
   opens, **Then** each shows its own status text: on track, the exact monthly amount needed to hit
   its target date, or that no funding is linked yet.
5. **Given** the goals list, **When** it opens, **Then** the rule is stated on screen: a goal's
   progress is the value of the holdings linked to it, and nothing is moved or locked.
6. **Given** a goal's detail, **When** it opens, **Then** it shows the progress ring, saved of
   target, the on-track date, the still-needed amount, the per-month amount, the months left, every
   funding link with its earmarked quantity, and a projection.
7. **Given** a holding earmarked to a goal, **When** it appears under "funded by", **Then** the
   earmarked quantity is shown and the contribution reflects that fraction, not the holding's full
   value.
8. **Given** a goal's detail, **When** the user links another holding and confirms, **Then** progress
   recomputes immediately from the new link set.

---

### User Story 4 - Choose a debt payoff order and see the trade-off honestly (Priority: P4)

A user with more than one debt compares paying highest-interest-first against paying
smallest-balance-first, and is shown what each choice actually costs — in months and in interest —
rather than being steered to one.

**Why this priority**: The two strategies genuinely trade money against motivation, and the design's
requirement is that the trade-off is shown, not hidden. Independently testable from the liabilities
the user already tracks, with no dependency on budgets or goals.

**Independent Test**: With three debts of differing rate and balance, toggle the two strategies and
confirm the ordering rule for each, and that the stated "N months slower, ₹X more interest" figures
agree with both projections.

**Acceptance Scenarios**:

1. **Given** three or more debts with differing interest rates and balances, **When** the
   highest-interest-first strategy is selected, **Then** the pay order is by interest rate
   descending; **When** smallest-balance-first is selected, **Then** it is by balance ascending.
2. **Given** either strategy is selected, **When** the summary is displayed, **Then** it states the
   debt-free date, the interest saved, the months saved, and the extra-per-month amount the
   projection assumes.
3. **Given** both strategies are projected over the same debts, **When** the trade-off statement is
   shown, **Then** its months and interest figures are internally consistent with both projections.
4. **Given** the trade-off statement, **When** it is displayed, **Then** it is labelled as a derived
   insight.
5. **Given** the pay-order list, **When** it is displayed, **Then** each debt shows its interest rate
   and its projected clear date under the selected strategy.
6. **Given** the extra-per-month amount is changed, **When** the projection recomputes, **Then** the
   debt-free date, interest saved and months saved all update together and stay mutually consistent.

---

### User Story 5 - Know what is insured, what renews when, and what is missing (Priority: P5)

A user records their policies and sees, in one place, what renews soon, whether their life cover
meets a stated rule of thumb, and which risks they carry no cover for at all.

**Why this priority**: Insurance is low-frequency but high-consequence — a lapsed policy is a real
loss. Independently testable from policy records alone, needing no transactions, budgets or goals.

**Independent Test**: Record one policy renewing inside the window and one outside it, confirm the
banner appears only for the first and states the correct days remaining and the lapse consequence.

**Acceptance Scenarios**:

1. **Given** a policy whose renewal date falls inside the renewal window, **When** the insurance
   screen opens, **Then** a banner states the days remaining and what happens if it lapses; a policy
   outside the window produces no banner.
2. **Given** an annual income and outstanding loans, **When** the rule-of-thumb cover is computed,
   **Then** it equals ten times annual income plus outstanding loans, the formula is stated on
   screen, and the shortfall against actual cover is named.
3. **Given** policies covering some risk categories but not others, **When** the gaps section is
   shown, **Then** it names the specific uncovered categories rather than a generic warning.
4. **Given** policies exist, **When** the screen opens, **Then** they are grouped into life and
   health, each showing renewal date, premium, sum assured, and whether cover is floater or
   per-member.
5. **Given** a policy's detail, **When** it opens, **Then** it shows sum assured, premium due date,
   type, policy number, premium and its frequency, cover-until date with the age it corresponds to,
   nominee name, relation and share, riders, attached documents, and the premiums-paid history.
6. **Given** a policy with a premium due, **When** the user marks it paid, **Then** a premium payment
   is recorded in that policy's history and that policy's renewal banner clears.
7. **Given** a policy detail, **When** the user chooses "Remind me", **Then** the reminder preference
   is stored against the policy and shows as set when the screen is reopened.

---

### User Story 6 - See a retirement projection with its assumptions in plain sight (Priority: P6)

A user sets how they expect to retire — age, today's monthly spend, inflation, returns before and
after retiring, life expectancy — and sees the projected corpus against a target, with every one of
those assumptions visible on the same screen as the number they produced.

**Why this priority**: The largest, longest-horizon and least verifiable calculation in the app. It
ships last because its credibility depends entirely on the assumptions being visible and adjustable,
which is only worth building once the rest of the Plan tab is real.

**Independent Test**: Vary one assumption at a time and confirm the projected corpus moves in the
expected direction each time, with all five assumptions readable on the same screen as the result.

**Acceptance Scenarios**:

1. **Given** one base input set, **When** the base, optimistic and cautious scenarios are toggled,
   **Then** three distinct corpus figures are shown — no two scenarios produce the same number from
   different assumptions.
2. **Given** the retirement screen, **When** it opens, **Then** all five assumptions — retire-at age,
   monthly spend today, inflation, pre-retirement return, post-retirement return, life expectancy —
   are visible without navigating away from the projected corpus.
3. **Given** a fixed assumption set, **When** each assumption is varied one at a time, **Then** the
   projected corpus changes in the expected direction for that assumption.
4. **Given** the screen, **When** it displays the result, **Then** it shows the projected corpus at
   the retirement age, the target, the percentage of target reached, and the shortfall.
5. **Given** a shortfall exists, **When** the gap insight is shown, **Then** it names the monthly
   amount required to close it and is labelled as a derived insight.
6. **Given** assumptions set to non-default values, **When** the user saves the scenario and reopens
   the screen later, **Then** those saved values reappear.

---

### User Story 7 - Open the Plan tab and see planning first, calculators second (Priority: P7)

A user opens the Plan tab and lands on live planning — this month's budgets and goals at the top,
long-run debt, insurance and retirement below — with the four calculators available underneath
rather than being the whole tab.

**Why this priority**: A hub is only worth rewriting once the modules it summarises exist and can
show real numbers; until then it would summarise nothing. It ships last in this phase, and its live
summary rows appear as each preceding story lands.

**Independent Test**: With budgets, goals and the long-run modules in place, open the Plan tab and
confirm the two live groups appear above the calculator strip, and that each row reaches its module
with the Plan tab still selected.

**Acceptance Scenarios**:

1. **Given** the Plan tab, **When** it opens, **Then** live planning modules appear first and the
   four calculators appear as a strip below them.
2. **Given** the Plan root, **When** it opens, **Then** the this-month group states spend of budget
   total with the number of budgets over, and the active goal count with total saved of total target.
3. **Given** the Plan root, **When** it opens, **Then** the long-run group offers debt payoff,
   insurance and retirement.
4. **Given** any Plan root row, **When** it is opened, **Then** the module opens as a drill-in with
   the Plan tab still selected and a single back path to the root.
5. **Given** a module with no data yet, **When** its root row is displayed, **Then** it states that
   rather than showing a zero that reads as a real figure.

### Edge Cases

- A budget created part-way through a month: the pace comparison must state which period it is
  measuring, and must not read as though the budget covered days it did not exist for.
- The last day of the month, and a month with zero days remaining: the pace statement and the
  "days left" figure must remain truthful rather than dividing by zero or reading "0% of the month".
- A budget on a category that is later marked excluded from spend: that budget must stop consuming
  and must say why, not silently show zero spend forever.
- A category with a budget but no transactions in the period, and a category with transactions but
  no budget: both must be represented honestly rather than omitted.
- A goal whose linked holding is sold, deleted, or falls to zero value: progress must fall
  accordingly and the goal must not keep claiming money that no longer exists.
- Earmarking more of a holding than exists, or earmarking the same holding to several goals until the
  total exceeds it: must be prevented and explained, never accepted silently.
- A goal with a target date already in the past, or already fully funded: status text must handle
  both without producing a negative monthly requirement.
- A goal with no linked holdings must read as unfunded, not as 0% on track.
- Debt payoff with no debts, with exactly one debt, or with an extra-payment amount of zero: each
  must produce a truthful screen rather than an empty projection.
- Two debts with an identical interest rate (highest-interest-first) or an identical balance
  (smallest-balance-first): the ordering must be deterministic and stable between openings.
- A debt whose minimum payment does not cover its own interest: it must be named as not clearing
  under the current plan, never silently dropped from the projection.
- A policy whose renewal date has already passed: it must be shown as lapsed or overdue rather than
  as renewing in a negative number of days.
- A policy with no nominee recorded, or a nominee split that does not total 100%: must be surfaced as
  incomplete rather than displayed as if complete.
- Rule-of-thumb cover when annual income is not recorded: the formula must state what it could not
  use instead of computing against zero.
- Retirement assumptions that are self-contradictory — retire-at age at or below current age, life
  expectancy below retirement age, or inflation above both return rates: each must be handled with a
  stated result, not a blank chart or a negative corpus presented as fact.
- Any Plan screen while signed out, offline with nothing cached, or after a failed load must render a
  designed state — never a blank screen or a spinner that never resolves.
- Every derived figure on every screen in this feature — recovery insight, payoff trade-off, goal
  status, retirement gap — must be labelled as derived, including when it happens to be exactly right.

## Requirements *(mandatory)*

### Functional Requirements

**Plan root**

- **FR-001**: The Plan root MUST present live planning modules above the calculators: a this-month
  group (budgets, goals) and a long-run group (debt payoff, insurance, retirement), with the four
  existing calculators as a strip below.
- **FR-002**: The budgets summary row MUST state spend against budget total and how many budgets are
  over; the goals summary row MUST state the active goal count and total saved against total target.
- **FR-003**: Opening any module from the root MUST keep the Plan tab selected and offer exactly one
  back path to the root.
- **FR-004**: A module with no data yet MUST say so on the root rather than presenting a zero that
  reads as a real figure.

**Budgets**

- **FR-005**: Users MUST be able to set a budget amount per category for a calendar-month period.
- **FR-006**: The budget overview MUST state percentage used, amount left, budget total, days
  remaining, and an explicit pace statement comparing spend fraction against elapsed-day fraction.
- **FR-007**: Pace MUST be the elapsed-day fraction of the calendar-month period, and every
  ahead/behind determination MUST be made against that fraction.
- **FR-008**: Each category MUST render a bar against a month-position marker, shown as ahead of pace
  if and only if its spend fraction exceeds the elapsed-day fraction.
- **FR-009**: An over-budget category MUST state the overage amount and the days remaining in words
  and money; colour alone MUST NOT be the only carrier of that meaning.
- **FR-010**: A category excluded from spend MUST contribute nothing to any budget figure, and a
  transfer between the user's own accounts MUST consume no budget.
- **FR-011**: Budget detail MUST show spend versus budget, the over or under amount, days left, the
  last six months as bars, and that category's recent transactions.
- **FR-012**: Budget detail MUST show a recovery insight whose stated figures are reproducible from
  that category's own recent transactions.
- **FR-013**: Users MUST be able to raise a budget from its detail, applying to the current period,
  with every derived figure on the screen updating.
- **FR-014**: Users MUST be able to set a percentage alert threshold on a budget; the threshold MUST
  persist and be visible as set when the screen is reopened.
- **FR-015**: Once budgets exist, a transaction's detail MUST show that transaction's impact on its
  category's budget — the line deferred from the Money phase.

**Goals**

- **FR-016**: Users MUST be able to create a goal with a name, a target amount and a target date, and
  to edit and delete it.
- **FR-017**: A goal's progress MUST equal the sum of the current value of the holdings linked to it,
  counting an earmarked link at its earmarked fraction only.
- **FR-018**: Linking or unlinking a holding MUST NOT write a transaction, move money, lock a
  holding, or alter that holding's own value or quantity.
- **FR-019**: The goals surface MUST state that rule on screen in the user's own terms.
- **FR-020**: Each goal MUST show a status of on track, the exact monthly amount needed to reach its
  target by its target date, or that no funding is linked yet.
- **FR-021**: The goals list MUST show total saved against total target and the count of active goals.
- **FR-022**: Goal detail MUST show progress percentage, saved of target, the on-track date, the
  still-needed amount, the required monthly amount, months left, every funding link with its
  earmarked quantity, a projection, and a contribution insight.
- **FR-023**: Users MUST be able to link a further holding to a goal, whole or by an earmarked
  quantity, and to unlink one; progress MUST recompute immediately.
- **FR-024**: The system MUST prevent the total earmarked across all goals for a single holding from
  exceeding that holding, and MUST explain the refusal.

**Debt payoff**

- **FR-025**: Debt payoff MUST read the outstanding debts the user already tracks as liabilities;
  this feature introduces no separate debt record.
- **FR-026**: Users MUST be able to switch between a highest-interest-first and a
  smallest-balance-first strategy; the first MUST order by interest rate descending and the second by
  balance ascending, with a deterministic, stable tie-break.
- **FR-027**: For the selected strategy the system MUST state the debt-free date, the interest saved,
  the months saved, and the extra-per-month amount the projection assumes.
- **FR-028**: Users MUST be able to change the extra-per-month amount, with the debt-free date,
  interest saved and months saved all recomputing consistently.
- **FR-029**: The pay-in-this-order list MUST rank every debt and show each one's interest rate and
  projected clear date under the selected strategy.
- **FR-030**: The trade-off between the two strategies MUST be stated numerically — months difference
  and interest difference — and MUST be consistent with both projections.
- **FR-031**: A debt whose minimum payment does not cover its own interest MUST be named as not
  clearing under the current plan rather than omitted from the projection.

**Insurance**

- **FR-032**: Users MUST be able to record a policy with its type, policy number, sum assured,
  premium and frequency, renewal date, cover-until date, nominee details and riders, and to edit and
  delete it.
- **FR-033**: Policies MUST be grouped into life and health, each showing renewal date, premium, sum
  assured, and whether cover is floater or per-member.
- **FR-034**: A renewal banner MUST appear only for policies inside the renewal window, stating the
  days remaining and the consequence of lapsing.
- **FR-035**: The system MUST compute rule-of-thumb life cover as ten times annual income plus
  outstanding loans, state the formula on screen, and name the shortfall against actual cover.
- **FR-036**: The gaps section MUST name the specific risk categories carrying no cover.
- **FR-037**: Policy detail MUST show sum assured, premium due date, type, policy number, premium and
  frequency, cover-until with the corresponding age, nominee name, relation and share, riders,
  attached documents, and the premiums-paid history.
- **FR-038**: Users MUST be able to attach and view documents against a policy.
- **FR-039**: Marking a premium paid MUST append a premium payment record and clear that policy's
  renewal banner.
- **FR-040**: Users MUST be able to set a reminder preference on a policy; it MUST persist and show
  as set when reopened.

**Retirement**

- **FR-041**: Users MUST be able to set five assumptions — retire-at age, monthly spend today,
  inflation rate, pre-retirement return, post-retirement return, life expectancy — and all of them
  MUST be visible on the same screen as the projected result.
- **FR-042**: The system MUST offer base, optimistic and cautious scenarios that produce distinct
  projections from distinct assumption sets.
- **FR-043**: The system MUST show the projected corpus at the retirement age, the target corpus, the
  percentage of target reached, the shortfall, and a corpus projection over time.
- **FR-044**: Varying any single assumption MUST move the projected corpus in that assumption's
  expected direction.
- **FR-045**: When a shortfall exists, the gap insight MUST name the monthly amount required to close
  it.
- **FR-046**: Users MUST be able to save a scenario; its assumption values MUST reappear when the
  screen is reopened later.

**Cross-cutting**

- **FR-047**: Every derived or AI-produced statement in this feature — recovery insight, payoff
  trade-off, goal status, contribution insight, retirement gap — MUST be visually labelled as derived
  rather than presented as plain fact.
- **FR-048**: Every screen in this feature MUST define and render distinct empty, loading, error,
  offline and signed-out states — never a blank screen or an unresolving spinner.
- **FR-049**: Stored amounts MUST be exact with no rounding drift between a recorded figure and any
  total containing it; projected figures MUST be presented as estimates and their rounding stated.
- **FR-050**: Persisted category, policy-type, strategy and scenario values, once shipped, MUST NOT
  be renamed or removed; new ones may be added.
- **FR-051**: Every budget, goal, policy, premium and saved scenario MUST be readable only by the
  user who created it.
- **FR-052**: No notification or reminder originating from this feature may contain a policy number
  or an account number.

### Key Entities

- **Budget**: A user-set amount for one category over one calendar month, optionally carrying an
  alert threshold percentage. Knows its period, its amount, and nothing about how spend is recorded.
- **Goal**: A named target amount with a target date. Holds no money of its own — its progress is
  entirely derived from the holdings linked to it.
- **Goal funding link**: The connection between a goal and a holding, either whole or for a stated
  earmarked quantity. Creating one moves nothing; removing one takes nothing away from the holding.
- **Debt**: An outstanding liability the user already tracks, with a balance, an interest rate and a
  minimum payment — read by this feature, owned by the net-worth records.
- **Payoff plan**: A strategy choice plus an extra-per-month amount, producing an order, a debt-free
  date, and interest and months figures. Derived, never stored as a fact about the debts.
- **Policy**: An insurance contract with a type, number, sum assured, premium and frequency, renewal
  and cover-until dates, nominee details and riders.
- **Premium payment**: An append-only record that a policy's premium was paid, with its date and
  amount.
- **Policy document**: A file the user attaches to a policy — a policy PDF, a receipt.
- **Retirement scenario**: A named, saved set of the five assumptions, from which a corpus projection
  is derived. The projection is never stored as if it were a recorded fact.
- **Derived insight**: Any figure or sentence the app computed rather than the user recorded, carrying
  a visible marker saying so.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The stated budget pace figure matches an independent hand-computation of spend fraction
  versus elapsed-day fraction in 100% of checked periods.
- **SC-002**: Categories excluded from spend and transfers between the user's own accounts contribute
  exactly zero to every budget figure, in 100% of cases.
- **SC-003**: Every over-budget category states its overage in words and money — 0% rely on colour
  alone to convey that they are over.
- **SC-004**: A budget detail's recovery insight is reproducible from that category's own recent
  transactions in 100% of checks.
- **SC-005**: Goal progress equals the sum of its linked holdings' current values, counting earmarked
  links at their earmarked fraction, in 100% of link combinations.
- **SC-006**: Zero transactions are created, and zero holding values or quantities change, as a result
  of linking or unlinking a goal — verified across every link operation.
- **SC-007**: The two payoff strategies order debts by interest rate descending and balance ascending
  respectively, in 100% of debt sets, and the stated trade-off agrees with both projections every
  time.
- **SC-008**: A renewal banner appears for exactly those policies inside the renewal window — no
  false positives and no missed renewals across the checked policy set.
- **SC-009**: Rule-of-thumb cover equals ten times annual income plus outstanding loans in 100% of
  income and loan combinations, with the formula visible on screen.
- **SC-010**: All five retirement assumptions are readable on the same screen as the projected corpus
  without any navigation, and varying any one moves the corpus in its expected direction in 100% of
  single-variable checks.
- **SC-011**: The three retirement scenarios produce three distinct corpus figures — 0% of scenario
  sets collapse to the same number.
- **SC-012**: 100% of derived statements in this feature carry a visible derived-insight label.
- **SC-013**: Every screen in this feature renders a correct empty, offline, signed-out or loaded
  state — 0% of sessions show a blank or permanently-loading Plan screen.
- **SC-014**: A user can go from opening the Plan tab to knowing whether this month's spending is on
  pace within two taps.

## Assumptions

- **Scope**: this feature covers the Plan tab's live modules (E1 revised, E2–E9) — budgets, goals,
  debt payoff, insurance and retirement — plus the Plan root rewrite. Insights and statements
  (Phase 5), global search and notifications (Phase 6), and automation (Phase 7) are out of scope and
  are specified with their own phases. The four calculators are unchanged by this feature; only their
  position on the Plan root changes.
- **Prerequisites**: sign-in and consent (Phase 1), net-worth holdings and liabilities (Phase 2), and
  transactions, categories and accounts (Phase 3) already exist. Budgets read Phase 3's transactions
  and categories; goals read Phase 2's holdings; debt payoff reads Phase 2's liabilities. This
  feature adds no records of those kinds.
- **Deferred item delivered here**: the transaction detail's budget-impact line was explicitly
  deferred from Phase 3 because budgets did not exist. FR-015 delivers it as part of this feature.
- **Alert and reminder delivery is Phase 6**: "Alert me at 80%" (FR-014) and "Remind me" (FR-040)
  store the user's preference in this feature. Actually *delivering* the notification is the
  notifications phase. The chained breach-to-recovery flow is therefore exercised in this feature
  from the budget detail onward; the notification-initiated leg is verified when notifications ship,
  and that QA row is deferred with this reason stated rather than silently closed.
- **Debts are liabilities, not a new record type**: introducing a parallel debt table would let a
  user's net worth and their payoff plan disagree. Payoff figures are derived on demand and never
  written back onto a liability.
- **Rule-of-thumb inputs**: annual income is taken from what the user already records; where it is
  absent, the screen states what it could not use rather than computing against zero. The ten-times
  multiplier is fixed by the design and is not user-configurable in this feature.
- **Gap categories are a fixed checklist**: the insurance gaps section checks a defined set of risk
  categories — term life, health, personal accident, critical illness, and home or property. The
  design names the behaviour ("GAPS section naming uncovered risks") but not the list; this set is a
  reasonable default. Widening it later adds rows to a list, not a new mechanism.
- **Retirement target definition**: the target corpus is derived from today's monthly spend inflated
  to the retirement age and sustained to life expectancy at the post-retirement return. The design
  states the inputs and the outputs but not the bridge between them; this is the standard derivation
  and is stated on screen as an assumption alongside the other five.
- **Scenario differences**: base, optimistic and cautious differ by their return and inflation
  assumptions, not by a different calculation. The user may edit any scenario's assumptions.
- **Projections are estimates, records are exact**: recorded money — budget amounts, premiums, goal
  targets — is exact. Projected money — corpus, interest saved, months to clear — is an estimate,
  presented as such, and is never stored as though it were a recorded figure.
- **Insurance is manual**: policies, premiums and documents are user-entered. No insurer integration,
  no policy-document parsing, and no automatic premium detection ships in this feature.
- **Where policy documents are stored** is a `plan.md` decision, not a spec one. They are the user's
  financial records and fall under the sync consent already granted in Phase 1 — this feature
  introduces no new consent class.
- **Single currency** (Indian rupee) for all amounts, consistent with the tracker domain's existing
  schema decision.
- **Budget periods are calendar months only**. Weekly, fortnightly, custom and rolling periods are out
  of scope; the design specifies a calendar month and the pace rule depends on it.
- **One budget per category per month**. Sub-category budgets, envelope rollover of unspent amounts,
  and shared or household budgets are out of scope for this feature.

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
