# Feature Specification: Automation (Phase 7)

**Feature Branch**: `007-automation`

**Created**: 2026-08-23

**Status**: Draft

**Input**: User description: "Phase 7" — design-v1 Phase 7, Automation (screens G1–G3): the
automation hub, the shared review queue every automated source feeds, and the account-linking
consent screen. QA catalog module `AUT` (9 rows).

**Source of truth**: `apps/finance/docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md`
§5 Group G (screens G1, G2, G3), business rules BR-G1/BR-G2/BR-G3, flow F-7 (automation approval),
§5 Group A row **A3** (the *Read transaction SMS* consent switch), and NFR-1/2/4/5/6. QA rows:
`AUT-*` (9) §9 of `2026-08-09-qa-test-scenario-catalog.md`. Route, intent and settings rows:
`2026-08-09-finance-surface-registries.md` §1 (the Automation route row, marked Phase 7, `automation`
flag, ships `enabled: false` until its checkpoint), §3 (the `REVIEW_INBOX` intent, whose destination
is G2), §4 (Settings › Modules › **Automation**, the entry this feature ships with its module).
Consent-switch-to-flag mapping: implementation plan §5.5 (*Read transaction SMS* is required **on
top of** *Sync my financial records* before the SMS source may parse anything). Scope corrections
this feature must carry: implementation plan §7's Phase 7 row and
`apps/finance/docs/superpowers/specs/2026-08-23-phase-readiness-architecture-decisions.md` §5.2/§5.4.
Upstream deferrals this feature discharges: `apps/finance/specs/002-money-tab/spec.md` (review queue
is recurring-only in Phase 3) and `apps/finance/specs/006-search-notifications/spec.md`
(transactions-to-review alert has no destination until Phase 7). This document is spec-kit's
`spec.md` — the *what* and *why* only; storage, parsing implementation, module topology and
component work are `plan.md`, written separately.

**Position in the build**: Phase 7 is the last phase, and the only one where a record can begin to
exist without the user typing it. Every phase before it is a place the user puts information in;
this one proposes information and asks for a decision. That inversion is why the whole phase is
built around a single rule — **nothing an automated source produces is ever part of the user's
records until the user says so** — and why the phase ships behind a flag that stays off until its
own checkpoint passes.

It is also a dependency, not an epilogue. Two already-specified phases defer work into this phase's
review queue: Phase 3 ships D9's review surface limited to recurring entries because the shared
queue is here, and Phase 6 declines to deliver the transactions-to-review alert because its
destination screen does not exist yet. Both are discharged here.

**Corrections folded in from the plan and the readiness record** (stated up front so this spec is not
read against a stale plan row):

1. **The store proposals live in is owned by Phase 3**, not this phase. The implementation plan's
   Phase 7 step 1 lists it as new work; it is not, and building it again would collide with a
   shipped one. This feature adds further producers into a store that already exists, plus the shared
   consumer screen for it. (`plan.md` names the object; this document deliberately does not.)
2. **Trash / Recently deleted does not belong to this feature.** Surface registry §4 lists
   *Recently deleted* under the Automation settings entry; the readiness record §5.2 moved it to
   Phase 0b's Settings control plane, where it ships before Phase 2. That registry line is stale and
   is corrected as part of this feature's closure tasks.
3. **CSV import** was parked under this phase by readiness §5.4 ("deferred, not cancelled — Phase 7
   owns it alongside the other ingestion paths"), but the functional spec's own open item §8 records
   that **no design exists for the column-mapping step**. It is resolved as out of scope in the
   Clarifications below — deferred deliberately and visibly, not silently dropped.

## Clarifications

### Session 2026-08-23

- Q: What does an enabled price feed do with a fetched gold/silver/currency price — write the
  holding's value directly, or propose it? → A: **Propose it.** A fetched price arrives in the review
  queue as a proposal the user accepts, exactly like every other source. The hub's header rule
  ("every source below only suggests — you approve each entry") is therefore true without an
  exception, which was the deciding argument: BR-G1 only forbids automated writes to the *ledger*, so
  a direct valuation write would have been legal but would have made the hub's own stated promise
  false for one row. The consequence is accepted and specified: **the review queue carries two kinds
  of proposal** — a proposed transaction and a proposed value update — and must present, accept and
  ignore both. This is a real shape change to G2 and is spec'd as such rather than discovered during
  build.
- Q: Is CSV import in scope for Phase 7? → A: **No.** The onboarding screen's "Import a CSV" call to
  action stays disabled, with copy naming it as arriving later. The functional spec's own open item
  records that the column-mapping step has no design at all, so including it here would mean
  designing a new subsystem inside the largest phase in the plan. It remains deferred and visible —
  the CTA is not removed — and gets its own spec when it is taken up.
- Q: When the user ignores a proposal, is it remembered so the same message is never proposed again,
  or forgotten? → A: **Remembered, and shown.** An ignored proposal moves to a separate **Ignored**
  list the user can review and restore from, and no source ever proposes that message or occurrence
  again. Forgetting it outright was rejected because a rescan, a source re-enable, or FR-027's
  backlog replay would resurrect the same row and the user would dismiss it forever. Keeping it
  invisible was rejected because ignore is a one-tap action on a swipeable row — the easiest action
  in the feature to trigger by accident, and the only one with no undo. The cost is accepted and
  specified: a fourth surface, inside the erasure path and under privacy masking like every other.
- Q: When *Read transaction SMS* consent is withdrawn, what happens to proposals that source already
  created and the user has not reviewed? → A: **Freeze them, do not delete them.** The rows stay
  visible but become read-only under a banner stating that SMS reading is off, offering exactly two
  ways out: delete them, or turn the source back on. Purging on withdrawal was rejected because
  withdrawal is a one-tap toggle and silently destroying a queue of unreviewed entries makes an
  easily-reversed action unrecoverable. Doing nothing was rejected because a queue that keeps
  offering to record message-derived data after the user said stop is the weakest possible reading of
  "withdraw". **This is a deliberate retention choice and is stated as one**: frozen proposals are
  retained, but no longer processed into anything, cannot be accepted, and remain covered by the
  erasure path (FR-050) and by privacy masking. The edge case that previously said only that these
  proposals "must have a stated fate" is replaced by the fate.
- Q: Does the app read a bank message when it arrives with the app closed, or only when the user next
  opens the app? → A: **Periodically, in the background** — a scheduled scan, not a per-message
  wake-up and not open-only. Open-only was rejected outright because it silently kills User Story 6:
  an entries-waiting alert cannot announce anything to a user who must already be in the app for the
  reading to have happened. Per-message reading was rejected as more machinery and more battery than
  the value justifies for a review queue that is drained daily at best. The consequence is accepted
  and stated rather than hidden: **the queue lags reality by up to one interval**, so a purchase made
  now may not appear for up to an hour, and the app says the queue is periodically refreshed rather
  than implying it is live. This matches the posture Phase 6 already set for alert evaluation —
  when the device allows it, not at an exact instant.
- Q: Does *Accept all* also accept duplicate-warned rows and proposed value updates? → A: **No —
  confidently-parsed transaction rows with no duplicate warning only.** FR-009's original wording
  ("all confidently-parsed proposals") read literally would let one tap double-count a payment and
  revalue holdings, which is precisely what SC-006 and SC-013 exist to prevent; a duplicate-flagged
  row is confidently parsed, it is just probably wrong. Accept-all is a speed tool for the boring
  majority, and every row it skips is one the app has a specific reason to think needs a human look.
  The skip is never silent: the result states how many were left and under which of the three
  reasons — missing field, possible duplicate, or value update.
- Q: How often does the price feed propose, and what stops it becoming daily noise? → A: **On a
  meaningful move** — it proposes when the market price has moved more than a stated percentage away
  from the value currently recorded for that holding, not on a clock. A daily proposal was rejected
  because it refills the queue faster than anyone drains it and buries real transactions under price
  rows. Manual-only was rejected as not being automation at all — it leaves the user with the
  remembering, which is the problem the source exists to remove. Tying it to the existing 60/90-day
  staleness threshold was considered and rejected as too slow: it would let a recorded value sit
  visibly wrong for two months while the app knew better. The cost is accepted and specified: **a new
  threshold the user can see and change**, defaulting to 5%, living in this module's own Settings
  entry — and, in a volatile period, more than one proposal for the same holding, which is correct
  behaviour rather than noise because each one represents a genuinely different price.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - One queue holds everything waiting to be recorded (Priority: P1)

A user has recurring entries the app generated for them — a rent payment due, an insurance premium,
a monthly SIP — and no single place to deal with them. They open the review queue and see every
proposed entry in one list, each visibly *not yet part of my records*: the amount, the date, the
account it belongs to, and a category the app has guessed. They accept the ones that are right,
correct a category and accept, and dismiss the one that did not actually happen. Their ledger
changes only where they said so.

**Why this priority**: this is the screen two other phases are waiting on, and it is the physical
embodiment of the rule the entire feature exists to guarantee. It also delivers value with **zero**
new sources — Phase 3 already produces recurring proposals with nowhere shared to review them — so
it can ship, be tested and be useful before any SMS is ever read.

**Independent Test**: with the SMS source never enabled, generate recurring proposals from Phase
3's templates, open the queue, accept one and ignore another, and confirm exactly one new
transaction exists, carrying the accepted values and an audit trail naming where it came from.

**Acceptance Scenarios**:

1. **Given** three pending proposals exist, **When** the review queue is opened, **Then** all three
   are listed, each rendered in the not-yet-accepted treatment that visually distinguishes it from a
   real record, and each showing its amount, date, account and proposed category.
2. **Given** a pending proposal with a proposed category, **When** the user accepts it, **Then** a
   real transaction is created with those values, the proposal leaves the pending list, and the
   transaction's history states it came from an automated source rather than manual entry.
3. **Given** a pending proposal, **When** the user changes its category and then accepts,
   **Then** the transaction is created with the user's category, not the proposed one.
4. **Given** a pending proposal, **When** the user ignores it, **Then** it leaves the pending list,
   no transaction is created anywhere, and it appears in the Ignored list.
4a. **Given** an ignored proposal, **When** the user restores it, **Then** it returns to the
   pending list with the same details it had, ready to be accepted or ignored again.
4b. **Given** a proposal that was ignored, **When** its source runs again over the same message or
   occurrence, **Then** no new proposal is produced for it.
5. **Given** a queue mixing confident entries, one missing a category, one flagged as a possible
   duplicate and one proposed value update, **When** the user chooses to accept them all, **Then**
   only the confident transaction entries become transactions, the other three stay pending, and the
   user is told how many were left and under which reason.
6. **Given** the queue is empty, **When** it is opened, **Then** an empty state states plainly that
   nothing is waiting, rather than an ambiguous blank screen.

---

### User Story 2 - See every source, what it reads, and switch it off (Priority: P2)

A user wants to know exactly what the app is allowed to look at. They open the automation hub and
find one row per source, each stating in plain language what that source reads and where its output
goes. A header states the rule once: every source only suggests, and the user approves each entry.
They can turn any source on or off from here, and turning one off stops it producing anything new.

**Why this priority**: the hub is the control surface that gates every other source, so it must
exist before any source can be enabled. It is independently testable and useful with only the
sources that already exist.

**Independent Test**: open the hub with no permissions granted and confirm every source row states
its scope and its current state, that at least one source can be switched on and off, and that a
source switched off produces nothing new.

**Acceptance Scenarios**:

1. **Given** the automation hub, **When** it is opened, **Then** the header states that every source
   only suggests and that the user approves each entry before it becomes part of their records.
2. **Given** a source row, **When** it is read, **Then** it states what that source reads, in the
   user's terms, without requiring the user to open a separate explanation.
3. **Given** a source that is switched on, **When** the user switches it off, **Then** it stops
   producing new proposals, and proposals it already produced remain in the queue for the user to
   decide on rather than silently vanishing.
4. **Given** a source that cannot be offered yet, **When** the hub is opened, **Then** its row is
   present and clearly marked as unavailable rather than absent or presented as working.
5. **Given** the automation feature is switched off entirely, **When** the user navigates to it,
   **Then** the disabled state is rendered rather than a failure.

---

### User Story 3 - Bank alerts become proposed entries, without typing (Priority: P3)

A user who is tired of typing every card swipe turns on bank SMS reading. They are told, before
granting anything, that messages are read on the device, that only bank senders are looked at, and
that nothing is recorded without their approval. From then on, the transaction alerts their bank
sends turn into proposed entries waiting in the review queue, each showing the original message text
so the user can check the app read it correctly.

**Why this priority**: this is the feature's headline capability, and the one that makes the queue
worth opening daily — but it depends on both the queue and the hub, and it is the only part carrying
a device permission and a distinct consent class.

**Independent Test**: with the source enabled and consent granted, present a representative bank
transaction message and confirm a pending proposal appears carrying the correct amount, date and
account, with the original message text visible on the row — and that no transaction was created.

**Acceptance Scenarios**:

1. **Given** the SMS source is off, **When** the user switches it on, **Then** they are shown what
   will be read and what will not, and both the device permission and the *Read transaction SMS*
   consent must be granted before anything is read.
2. **Given** consent and permission are granted, **When** a bank transaction message arrives and the
   next background scan runs, **Then** a pending proposal is produced carrying the amount, the
   date, the account it names and a proposed category — and no transaction is created.
2a. **Given** a bank message arrived while the app was closed, **When** the background scan runs,
   **Then** the proposal exists and the entries-waiting alert can fire, without the user having
   opened the app.
2b. **Given** a message arrived moments ago and no scan has run yet, **When** the user opens the
   queue, **Then** a read is triggered on open, and the queue states that it refreshes periodically
   rather than presenting itself as live.
3. **Given** a message the app cannot confidently interpret, **When** it appears in the queue,
   **Then** the row says so in plain language and asks the user for the missing piece, rather than
   guessing or being dropped without trace.
4. **Given** a message from a sender that is not a bank, or a message that is not about a
   transaction, **When** it arrives, **Then** no proposal is produced from it.
5. **Given** any message has been read, **When** anything at all is sent off the device,
   **Then** the original message text is never part of it.
6. **Given** the user withdraws *Read transaction SMS* consent, or revokes the device permission,
   **When** further messages arrive, **Then** nothing further is read, and the hub reflects the
   source as off rather than showing it as on and silently doing nothing.
7. **Given** unreviewed proposals from messages exist, **When** the user withdraws that consent,
   **Then** those proposals remain visible but can no longer be accepted, under a banner naming why
   and offering to delete them or turn the source back on — and proposals from other sources in the
   same queue stay fully actionable.
8. **Given** frozen proposals exist, **When** the user re-grants the consent, **Then** they become
   acceptable again exactly as they were, with no duplicates produced.

---

### User Story 4 - The same spend is not recorded twice (Priority: P4)

A user pays by card and the app proposes an entry from the bank's message — but they had already
typed that payment in themselves an hour earlier. The queue tells them, on that row, that this looks
like something already in their records, and shows them what it matched, so they can dismiss the
duplicate instead of ending up with the same ₹2,400 counted twice.

**Why this priority**: without it, the SMS source actively damages data quality for exactly the
users who were already diligent about manual entry. It depends on US3 producing proposals, but it
is separately testable and separately valuable.

**Independent Test**: create a transaction manually, then produce a proposal with a closely
matching amount, date and account, and confirm the row carries a duplicate callout naming the
existing record.

**Acceptance Scenarios**:

1. **Given** a transaction already exists, **When** a proposal closely matching its amount, date
   and account appears in the queue, **Then** that row carries a duplicate callout identifying the
   existing record.
2. **Given** a duplicate-flagged proposal, **When** the user accepts it anyway, **Then** the
   transaction is created — the callout informs, it does not block.
3. **Given** two genuinely separate payments of the same amount to the same place on the same day,
   **When** both are recorded, **Then** the user can complete that without the app preventing it.
4. **Given** a proposal with no close match, **When** the queue is opened, **Then** no duplicate
   callout appears on it.

---

### User Story 5 - Teach it once, stop correcting it (Priority: P5)

A user keeps re-categorising the same merchant. When they correct a proposal's category, they can
turn that correction into a standing rule. From then on the app proposes that category itself. The
hub lists every rule they have taught it, how many times each has been applied, and lets them switch
one off or delete it when it stops being right.

**Why this priority**: it converts the queue from a chore into something that gets lighter over
time, and it is the subject of a written business rule (BR-G3) and QA row. It depends on US1 and
US3 existing, and the feature is usable without it.

**Independent Test**: create a rule from a corrected proposal, produce a second matching
proposal, confirm the proposed category now matches the rule and the rule's applied count
increased, then remove the rule and confirm it stops applying.

**Acceptance Scenarios**:

1. **Given** the user corrects a proposal's category, **When** they accept it, **Then** they are
   offered the option of making that a standing rule, and declining is a normal outcome that leaves
   no rule behind.
2. **Given** a rule exists, **When** a matching proposal is produced, **Then** the rule's category
   is proposed and the rule's applied count increases.
3. **Given** rules exist, **When** the hub is opened, **Then** each rule is listed with what it
   matches, how many times it has been applied, and a working control to disable or remove it.
4. **Given** a rule is removed, **When** a previously matching proposal is produced, **Then** the
   rule's category is no longer proposed and previously accepted transactions are unchanged.

---

### User Story 6 - Told that entries are waiting (Priority: P6)

A user does not have to remember to check the queue. When entries are waiting, the app tells them,
and tapping that alert takes them straight to the queue rather than to the home screen. The alert is
governed by the same single control as every other alert in the app, in the section that owns it.

**Why this priority**: it closes an obligation Phase 6 explicitly deferred, and it is what makes the
queue reliably drained rather than discovered weeks later. It depends on the queue existing and on
Phase 6's delivery machinery.

**Independent Test**: with entries pending, confirm the alert is raised, appears in the notification
centre, opens the review queue when tapped, and stops being raised when the queue is empty.

**Acceptance Scenarios**:

1. **Given** proposals are pending, **When** the alert condition is evaluated, **Then** an alert is
   raised stating how many entries need review.
2. **Given** the alert is shown, **When** the user opens it, **Then** the review queue opens
   directly.
3. **Given** the queue has been emptied, **When** the condition is next evaluated, **Then** no alert
   is raised.
4. **Given** the user switches this alert off in the Automation settings entry, **When** entries are
   pending, **Then** no alert is raised, and the entries still wait in the queue.

---

### User Story 7 - Account linking is explained before it is agreed to (Priority: P7)

A user considering linking a bank account is shown, before any consent action is available, exactly
what would be shared, for how long, and what it would be used for. They can close the screen without
agreeing to anything.

**Why this priority**: the screen is a drawn deliverable with a written QA row and a compliance
purpose, but the underlying linking capability is marked as not yet available in the design itself,
so it delivers a stated commitment rather than a working integration.

**Independent Test**: open the account-linking screen and confirm scope, duration and purpose are all
stated and readable before any control that grants consent is reachable, and that dismissing it
grants nothing.

**Acceptance Scenarios**:

1. **Given** the user starts account linking, **When** the screen opens, **Then** the scope of what
   would be shared, the duration of that permission, and the purpose it serves are stated before any
   consent control is available.
2. **Given** the screen is open, **When** the user dismisses it, **Then** nothing is granted and no
   source is enabled.
3. **Given** the linking capability is not yet available, **When** the screen is reached from the
   hub, **Then** its unavailability is stated plainly rather than presented as a working flow that
   fails later.

---

### User Story 8 - Metal and currency values refresh themselves, with approval (Priority: P8)

A user holding gold and silver has to look up the rate and retype a value every time they want their
net worth to be current. They turn on the price feed, and from then on the app brings them the new
rate as something to approve: *"Gold 24k is ₹7,412/g today — update your 3 gold holdings?"*. They
accept, and the values move. They ignore, and nothing moves — the same promise the hub makes about
every other source.

**Why this priority**: it removes real recurring friction, but it is the only source whose output is
not a transaction, so it depends on the queue already carrying a second proposal kind. The app is
fully usable without it — Phase 6's value-update-overdue alert already nudges the user to refresh by
hand.

**Independent Test**: with the price feed enabled and holdings that track a metal or currency,
confirm a fetched rate appears as a proposed value update naming the affected holdings and their old
and new values, that accepting moves those values, and that ignoring moves nothing.

**Acceptance Scenarios**:

1. **Given** the price feed is enabled and the user holds an auto-valued asset, **When** a fetched
   price has moved further than the threshold from that holding's recorded value, **Then** a proposed
   value update appears in the queue naming the affected holdings, their currently recorded values,
   the proposed ones, and how far the price has moved.
1a. **Given** the price feed is enabled, **When** a fetched price has moved less than the threshold,
   **Then** no proposal is produced at all.
2. **Given** a proposed value update, **When** the user accepts it, **Then** each affected holding's
   recorded value moves to the proposed one, and the change is recorded the same way any value change
   is recorded.
3. **Given** a proposed value update, **When** the user ignores it, **Then** no holding's value
   changes, and no proposal reappears until the price has moved past the threshold again from the
   same recorded value.
3a. **Given** the user changes the threshold, **When** prices are next checked, **Then** proposals
   are produced against the new threshold, and the setting states what it controls in plain language.
4. **Given** the price source cannot be reached, **When** the hub is opened, **Then** the row states
   the source is unreachable, and no proposal carrying an out-of-date price is presented as current.
5. **Given** the price feed is switched off, **When** prices change, **Then** no proposal is produced
   and no recorded value moves.

---

### Edge Cases

- **The app has not been opened for weeks.** Background scans have been accumulating proposals the
  whole time, so the queue is already current on open; anything the device prevented from scanning
  appears in date order once scanning resumes, rather than being lost or arriving as one
  undifferentiated blob.
- **The device delays or drops a scheduled scan** (battery saver, doze, force-stop). Nothing is lost;
  the next scan that runs picks up everything since the last one, and the feature never assumes an
  exact cadence.
- **A message arrives seconds before the user opens the queue.** The open-triggered read catches it,
  or the queue's own "refreshes periodically" statement makes its absence expected rather than a bug.
- **The device cannot receive messages at all** (tablet, no SIM). The SMS source states it is
  unavailable on this device rather than offering a switch that can never produce anything.
- **Permission granted, consent withheld** (or the reverse). Nothing is read until both are present,
  and the hub states which one is missing rather than showing a generic failure.
- **Consent withdrawn while proposals are pending.** Those proposals freeze: still visible, no
  longer acceptable, under a banner naming why and offering delete-them or turn-it-back-on. Nothing
  is silently lost and nothing is silently still working.
- **Consent re-granted while frozen proposals exist.** They unfreeze exactly as they were, rather
  than being re-produced from scratch or duplicated by a rescan.
- **Only some sources are frozen.** Withdrawal freezes the message source's proposals only; proposals
  from recurring definitions and the price feed stay fully actionable in the same queue, and the
  banner says which rows it applies to.
- **A proposal's account or category was deleted before the user got to it.** Accepting must
  either resolve to a valid target or tell the user what to pick, never create a transaction pointing
  at nothing.
- **Accept-all with a mixed queue.** Confident transaction rows are accepted; rows missing a required
  field, rows flagged as possible duplicates, and value updates all stay pending, and the user is
  told how many were left under each reason.
- **Accept-all when nothing qualifies** (every row is duplicate-flagged, incomplete or a value
  update). The action reports that nothing was accepted and why, rather than appearing to do nothing.
- **The same proposal is accepted on two devices.** Only one transaction results.
- **Duplicate detection fires on a genuine repeat purchase.** The user can proceed; the callout is
  advisory.
- **A promotional message contains a rupee amount.** No proposal is produced from it.
- **A rule matches nothing for a long period.** It remains listed with an applied count of zero
  rather than being silently pruned.
- **The alert deep link fires while the app lock is engaged.** The user unlocks first and then lands
  on the queue, never bypassing the lock.
- **Signed out, or offline, while accepting.** Acceptance either completes when connectivity returns
  or fails visibly with the proposal still pending — never a silent loss.
- **A price proposal is pending when the user manually updates that same holding.** The proposal must
  reflect that, or accepting it must not silently overwrite a value the user just set by hand. The
  threshold is re-measured from whatever the recorded value now is.
- **A volatile period moves the price past the threshold repeatedly.** More than one proposal for the
  same holding is correct — each represents a genuinely different price — but they must not stack up
  as near-identical rows for a move that has already been proposed.
- **The price moves past the threshold and back before the user acts.** The pending proposal is
  visibly out of date; it must state the moment its price was taken so the user is not accepting a
  number the app knows has moved.
- **The threshold is set so high nothing ever triggers.** The hub row says the source is on but has
  produced nothing, rather than looking broken.
- **A price proposal covers several holdings and one of them was deleted.** The surviving holdings
  update; the deleted one is not resurrected.
- **The feature flag is off.** Every route renders the disabled state, and no source runs in the
  background.

## Requirements *(mandatory)*

### Functional Requirements

**The rule that governs everything (BR-G1)**

- **FR-001**: No automated source MUST ever create a record in the user's ledger, or change a
  recorded value, directly. Every automated output MUST enter the review queue as a proposal and
  require an explicit user action before it changes anything the user's records show. BR-G1 states
  this for the ledger; this feature holds itself to it for valuations too, so the hub's "every source
  only suggests" promise is true without exception.
- **FR-002**: Every record created from a proposal MUST carry, in its own history, the fact that it
  originated from an automated source and which source that was.

**Review queue (G2)**

- **FR-003**: The review queue MUST show every pending proposal from every enabled source in one
  list, regardless of which source produced it.
- **FR-003a**: The queue MUST carry **two kinds** of proposal — a proposed **transaction** (from
  messages and recurring definitions) and a proposed **value update** (from a price feed) — and MUST
  make which kind a row is unmistakable, because accepting them changes different things.
- **FR-004**: Each row MUST render in a visually distinct not-yet-accepted treatment that
  distinguishes a proposal from a real record.
- **FR-005**: A proposed transaction row MUST show the proposed amount, the date, the account, the
  proposed category, and — where the source has one — the original source text it was derived from.
  A proposed value update row MUST show what is being revalued, the currently recorded value and the
  proposed one.
- **FR-006**: Users MUST be able to change a proposed transaction's category before accepting it.
- **FR-007**: Users MUST be able to accept a proposal — creating a record for a proposed
  transaction, or updating the recorded value for a proposed value update — with the values shown.
- **FR-008**: Users MUST be able to ignore a proposal, removing it from the pending queue and
  changing nothing in the user's records.
- **FR-008a**: An ignored proposal MUST be retained in an **Ignored** list reachable from the review
  queue, showing the same details it showed while pending.
- **FR-008b**: Users MUST be able to restore an ignored proposal to the pending queue, from which it
  can be accepted or ignored again.
- **FR-008c**: Once a proposal has been ignored or accepted, **no source MUST ever propose that same
  message or occurrence again** — not on a rescan, not when a source is re-enabled, and not through
  the backlog replay of FR-027.
- **FR-009**: Users MUST be able to accept, in one action, every **proposed transaction** that is
  confidently parsed and carries no duplicate warning.
- **FR-009a**: That action MUST leave pending — never accept — any proposal that is missing a
  required field, carries a duplicate warning, or is a proposed value update.
- **FR-009b**: After the action, the user MUST be told how many proposals were accepted and how many
  were left, broken down by which of those three reasons applied.
- **FR-010**: A proposal the source could not confidently interpret MUST say so in plain language and
  name what the user needs to supply, rather than being dropped or silently guessed.
- **FR-011**: The queue **and the Ignored list** MUST each define their empty, loading, error,
  offline, signed-out and disabled states; an empty queue MUST state that nothing is waiting.
- **FR-012**: Accepting or ignoring MUST be reachable from the row itself, without opening a
  separate screen for the common case.

**Automation hub (G1)**

- **FR-013**: The hub MUST state, once and prominently, that every source only suggests and that the
  user approves each entry before it becomes part of their records.
- **FR-014**: The hub MUST list one row per source, each stating what that source reads in the
  user's own terms.
- **FR-015**: Each available source MUST be independently switchable on and off from the hub.
- **FR-016**: A source that is not yet available MUST be shown and marked unavailable, not hidden and
  not presented as working.
- **FR-017**: Switching a source off MUST stop it producing new proposals and MUST leave already
  produced proposals in the queue for the user to decide on.
- **FR-018**: The hub MUST show an activity indication per source sufficient for the user to tell
  whether it is doing anything (for example, how many proposals it produced this month).
- **FR-019**: The hub MUST be reachable from the Settings Automation entry, which ships with this
  module rather than being added to a central list.

**Bank message source**

- **FR-020**: The message source MUST require both the device permission and the *Read transaction
  SMS* consent before reading anything; either one missing MUST mean nothing is read, and the hub
  MUST state which is missing.
- **FR-021**: Before the user grants anything, the app MUST state that messages are read on the
  device, that only bank senders are considered, and that nothing is recorded without approval.
- **FR-022**: The source MUST consider only messages from senders it identifies as banks or card
  issuers, and only messages describing a transaction.
- **FR-023**: A considered message MUST produce a proposal carrying the amount, the date, the account
  it identifies, and a proposed category.
- **FR-024**: All interpretation of message content MUST happen on the device.
- **FR-025**: The original message text MUST never be included in anything the app sends off the
  device.
- **FR-026**: Withdrawing the consent, or losing the permission, MUST stop the source, and the hub
  MUST reflect it as off.
- **FR-026a**: Proposals the message source already produced MUST NOT be deleted on withdrawal. They
  MUST become **frozen** — still visible, no longer acceptable — under a banner stating that message
  reading is off and offering exactly two actions: delete them, or turn the source back on.
- **FR-026b**: Freezing MUST apply only to the message source's proposals. Proposals from other
  sources in the same queue MUST remain fully actionable, and the banner MUST make clear which rows
  it governs.
- **FR-026c**: Re-granting the consent MUST unfreeze the existing proposals as they were, and MUST
  NOT re-produce or duplicate them.
- **FR-026d**: Frozen proposals MUST remain covered by the erasure path and by privacy masking. No
  further interpretation of message content MUST occur while frozen.
- **FR-027**: The source MUST read messages on a **periodic background scan** that runs whether or
  not the app is open, so proposals accumulate and the entries-waiting alert can fire without the
  user opening the app first.
- **FR-027a**: The scan interval MUST be approximately one hour, and MUST be treated as
  best-effort — the device may delay it, and the feature MUST behave correctly when it does rather
  than assuming an exact cadence.
- **FR-027b**: The queue MUST NOT present itself as live. It MUST state that it refreshes
  periodically, so a purchase made moments ago being absent reads as expected rather than broken.
- **FR-027c**: Opening the app MUST also trigger a read, so a user who comes looking is never shown a
  queue staler than the moment they arrived.
- **FR-027d**: Messages that accumulated while the device was off, or while the app was force-stopped
  and no scan could run, MUST appear in the queue in date order once scanning resumes.
- **FR-028**: On a device that cannot receive messages, the source MUST state it is unavailable
  rather than offering an inoperable switch.

**Duplicate detection**

- **FR-029**: A proposal closely matching an existing record on amount, date and account MUST carry a
  duplicate callout on its row, identifying the record it matched.
- **FR-030**: The callout MUST be advisory — the user MUST still be able to accept the proposal.
- **FR-031**: The matching criteria MUST be stated to the user rather than being an opaque judgement.

**Learned rules (BR-G3)**

- **FR-032**: When a user corrects a proposal's category, the app MUST offer to make that correction
  a standing rule; declining MUST leave no rule behind.
- **FR-033**: A standing rule MUST cause its category to be proposed on subsequent matching
  proposals.
- **FR-034**: Every rule MUST be listed to the user with what it matches and how many times it has
  been applied.
- **FR-035**: Every rule MUST be individually disableable and removable, and removing one MUST NOT
  alter records already created under it.

**Entries-waiting alert**

- **FR-036**: When proposals are pending, an alert MUST be raised stating how many entries need
  review, using the app's existing alert delivery and notification-centre machinery rather than a
  parallel mechanism.
- **FR-037**: Opening that alert MUST land the user directly on the review queue, honouring the app
  lock rather than bypassing it.
- **FR-038**: The alert MUST have exactly one control, in the Automation settings entry, and MUST
  also be governed by the app-wide alert master switch.
- **FR-039**: No alert MUST be raised when nothing is pending.

**Account-linking consent (G3)**

- **FR-040**: The account-linking screen MUST state the scope of what would be shared, the duration
  of the permission, and the purpose it serves, before any consent-granting control is reachable.
- **FR-041**: Dismissing the screen MUST grant nothing and enable nothing.
- **FR-042**: While the linking capability is unavailable, the screen MUST state that plainly rather
  than presenting a flow that fails downstream.

**Other sources**

- **FR-043**: The hub MUST include a price-feed source row for the metals and currency values the app
  already tracks, switchable like any other source.
- **FR-044**: An enabled price feed MUST produce its fetched price as a **proposed value update** in
  the review queue, naming the holdings it would affect and the old and new value of each. It MUST
  NOT change a holding's recorded value without an accept action.
- **FR-044a**: A proposal MUST be produced **only when the fetched price has moved further than a
  stated percentage from the value currently recorded** for that holding — never on a fixed schedule
  and never when the price has barely moved.
- **FR-044b**: That percentage MUST be a user-visible, user-changeable setting in this module's
  Settings entry, defaulting to 5%.
- **FR-044c**: A proposed value update MUST state **why it is being proposed** — how far the price has
  moved and from what — so it reads as a reason rather than an unexplained interruption.
- **FR-044d**: Price checking MUST run on the same periodic background schedule as the message
  source, rather than introducing a second cadence.
- **FR-045**: Ignoring a proposed value update MUST leave every affected holding's recorded value
  untouched. It MUST NOT stop a **later, genuinely different** move from producing a new proposal —
  this is the one deliberate exception to FR-008c, because each price proposal represents a distinct
  market state rather than a repeat of the same one.
- **FR-045a**: Ignoring a proposal MUST NOT cause the same move to be re-proposed. The next proposal
  for that holding MUST require a fresh move past the threshold, measured from the same recorded
  value.
- **FR-046**: A price feed that cannot reach its source MUST state that on the hub row rather than
  failing silently or producing a stale proposal presented as current.
- **FR-047**: The hub MUST include a recurring-templates source row reflecting the recurring
  definitions Phase 3 already owns, and proposals it generates MUST appear in the same queue as every
  other source.
- **FR-048**: CSV import is **not** delivered by this feature. The onboarding screen's import call to
  action MUST remain present and disabled, with copy naming it as arriving later — it MUST NOT be
  removed, and MUST NOT be made to look available.

**Privacy, consent and erasure**

- **FR-049**: This feature MUST introduce exactly one new consent class — reading transaction
  messages — and it MUST be persisted, revocable from Settings, and additional to the existing sync
  consent rather than replacing it.
- **FR-050**: Every proposal and every rule MUST be removed by the app's existing "delete my data"
  path; no store this feature introduces may survive an erasure request.
- **FR-051**: Amount values shown in the queue, in the Ignored list and in any alert MUST honour the
  app's existing privacy masking.

**Cross-cutting**

- **FR-052**: Every route this feature adds MUST be fault-isolated behind the `automation` feature
  flag, rendering the disabled state rather than failing the shell, and that flag MUST remain off
  until this feature's own checkpoint passes.
- **FR-053**: Every deferred scenario this feature discharges — Phase 3's shared review queue and
  Phase 6's transactions-to-review alert — MUST be closed in the QA catalog, and anything it cannot
  discharge MUST be re-deferred with a stated reason rather than left unticked.
- **FR-054**: The stale registry line placing *Recently deleted* under this feature's settings entry
  MUST be corrected, since that surface is owned by the Settings control plane.

### Key Entities

> **Canonical term.** This document says **proposal** throughout. The design and the QA catalog say
> *suggestion* (and Phase 3's store is named for it) — the same thing, renamed here only because the
> queue now carries proposed value updates as well as transactions, and "suggestion" reads oddly for
> a price. A `plan.md` or test citing `AUT-BR-001`'s wording is talking about this entity.

- **Proposal**: something an automated source believes is true but which is not yet part of the
  user's records — carrying the source that produced it, the original source text where one exists,
  its pending / frozen / accepted / ignored state, the identity of the message or occurrence it came
  from (so it is never proposed twice), and **which of two kinds** it is:
  - a **proposed transaction** — amount, date, account, proposed category;
  - a **proposed value update** — the holdings affected, their currently recorded values and the
    proposed ones.
- **Source**: an origin of proposals — bank messages, recurring definitions, price feeds, account
  linking — each with a user-facing statement of what it reads, an on/off state, an availability
  state (including unreachable), and an activity count.
- **Price move threshold**: how far a market price must move from a holding's recorded value before
  the price feed proposes an update — a user-visible, user-changeable setting, not a hidden constant.
- **Standing rule**: a user-taught association between something recognisable in a proposal and the
  category to propose for it — with what it matches, how many times it has applied, and whether it is
  active.
- **Duplicate match**: the relationship between a proposal and an existing record that resembles it,
  carrying enough of the existing record to let the user judge.
- **Account-link consent**: the scope, duration and purpose statement a user would be agreeing to,
  and whether they have agreed.
- **Message-reading consent**: the persisted, revocable permission to read transaction messages,
  distinct from and additional to the sync consent. Its withdrawn state is a real state with
  behaviour attached — it freezes that source's outstanding proposals rather than merely stopping
  future reads.

### Scope Boundaries

**In scope**

- The shared review queue: every source's proposals in one list — both proposed transactions and
  proposed value updates — accept / correct-and-accept / ignore / accept-all, unparseable rows,
  duplicate callouts, and all its screen states.
- The **Ignored** list reached from the queue: what was dismissed, restorable, and the guarantee that
  nothing already decided is ever proposed again.
- The price feed for the metals and currency values the app already tracks, producing proposed value
  updates — only when the price has moved past a user-set threshold — rather than writing values
  directly.
- The automation hub: per-source switches, per-source scope statements, activity counts, and the
  learned-rules list with counts and removal.
- On-device reading and interpretation of bank transaction messages behind both a device permission
  and its own consent class.
- Duplicate detection between a proposal and existing records.
- Standing rules taught from a corrected proposal.
- The entries-waiting alert and its deep link into the queue, discharging Phase 6's deferral.
- The account-linking consent screen, stating scope, duration and purpose.
- This module's Settings entry, feature flag, and the closure of the `AUT-*` catalog rows.

**Out of scope**

- **Creating the proposal store.** Phase 3 owns it; this feature adds producers and the shared
  consumer screen.
- **Trash / recently deleted.** Owned by the Settings control plane (Phase 0b); this feature only
  corrects the registry line that points at it here.
- **A working account-aggregator integration.** The design itself marks account linking as not yet
  available; this feature ships the consent statement, not the connection.
- **Reading anything other than bank transaction messages** — no one-time passwords, no promotional
  messages, no email, no notification scraping, no screen reading.
- **Editing recurring definitions.** Phase 3 owns them; this feature only surfaces the source and
  consumes its output.
- **CSV import.** Resolved out of scope in the Clarifications. The onboarding call to action stays
  present and disabled with honest copy; the column-mapping step has no design and gets its own spec
  when it is taken up.
- **Market price alerts.** A value going stale is Phase 6's existing alert; a price that moved is a
  different thing. The price feed here proposes a *value update in the queue*, it does not notify the
  user that a price moved.
- **Auto-valuing anything other than the metals and currencies the app already tracks.** No equity,
  fund or property price source is in scope.
- **Bulk editing or re-categorising already-accepted records** — that is the ledger's own surface.
- **Any server-side scheduling, server-side parsing, or cross-device coordination of proposals
  beyond what already-shared data gives for free.**
- **Play Store distribution obligations** arising from a message-reading permission. Distribution is
  a signed package outside the store for now, and the store declaration is a separate, already
  deferred piece of work.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Across every source and every path, **zero** records are created in the user's ledger
  and **zero** recorded values change without an explicit accept action — verified end to end, not by
  inspection of one path.
- **SC-002**: A user with pending proposals can clear a queue of ten in under 60 seconds when the
  proposals are correct, using accept-all plus at most two individual decisions.
- **SC-003**: A representative set of bank transaction messages yields a correct amount, date and
  account on at least 90% of them, and every remaining message is presented as needing input rather
  than being dropped or silently mis-parsed.
- **SC-003a**: A bank message that arrives while the app is closed becomes a pending proposal within
  roughly an hour on a device that is not restricting background work, and **zero** such messages are
  lost when the device does restrict it.
- **SC-004**: **Zero** messages from non-bank senders and zero non-transaction messages produce a
  proposal.
- **SC-005**: **Zero** occurrences of original message text in anything leaving the device, verified
  against every outbound payload the feature can produce.
- **SC-006**: A proposal matching an existing record on amount, date and account is flagged on 100%
  of such cases, and a user can still record two genuinely identical same-day payments.
- **SC-006a**: Accept-all records **zero** duplicate-flagged transactions and changes **zero**
  holding values, across every queue composition it is offered on.
- **SC-007**: After teaching one rule, a second matching proposal arrives with the taught category
  already proposed, with no further user action.
- **SC-008**: Every rule the user has taught is visible with an accurate applied count, and removing
  one takes a single action and leaves prior records unchanged.
- **SC-009**: Tapping the entries-waiting alert lands on the review queue on 100% of attempts,
  including when the app was closed and when the app lock was engaged.
- **SC-010**: The account-linking screen states scope, duration and purpose before any consent
  control is reachable — verified by a user being able to read all three without granting anything.
- **SC-011**: Withdrawing message-reading consent stops all reading within the same session, the hub
  reflects the change immediately, and **zero** unreviewed proposals are destroyed by the withdrawal
  itself.
- **SC-011a**: While consent is withdrawn, **zero** frozen proposals can be accepted by any route,
  and the user can delete all of them in a single action.
- **SC-012**: An erasure request removes 100% of proposals and rules this feature stores, with none
  surviving.
- **SC-013**: **Zero** holding values change from a price feed without an accept action, and
  ignoring a proposed value update leaves 100% of affected values untouched.
- **SC-013a**: **Zero** proposals are produced for a price move smaller than the threshold, and a
  price that stays flat produces **zero** proposals over any length of time.
- **SC-013b**: A user can find the move threshold, understand from its own copy what it controls, and
  change it — without leaving the Automation settings entry.
- **SC-014**: A user can tell which kind a queue row is — a proposed transaction or a proposed value
  update — without opening it, on 100% of rows.
- **SC-015**: Every one of the feature's screens renders correctly in its empty, loading, error,
  offline, signed-out and disabled states.
- **SC-016**: A message or occurrence already decided on — accepted or ignored — is re-proposed
  **zero** times across a rescan, a source re-enable and a backlog replay.
- **SC-017**: An accidentally ignored proposal can be restored to the pending queue in a single
  action, with none of its details lost.
- **SC-018**: All nine `AUT-*` catalog rows plus the two upstream deferrals are closed or explicitly
  re-deferred with a stated reason at the checkpoint.

## Assumptions

- **Prerequisites**: Phases 1–6 have shipped. Sign-in, sync consent, the ledger and its accounts and
  categories, the proposal store and recurring definitions, the Settings control plane, and the alert
  delivery plus notification centre all exist. This feature assumes them and specifies only its own
  fallbacks.
- **The proposal store already exists.** Phase 3 created it with the shape this feature needs,
  including a field reserved for a message source's original text. This feature adds producers and
  the shared consumer, not the store.
- **Account linking is not live.** The design marks it as not yet available, so the consent screen
  ships as a stated commitment and the source row is marked unavailable. Making it live requires a
  licensed aggregator relationship, which is a commercial dependency outside this feature.
- **Message reading is forward-looking, with a short backfill.** Enabling the source reads messages
  from roughly the last 30 days and then continues forward on the periodic scan. The design states a
  per-month count, which implies a recent window rather than an entire inbox history; a full
  historical scan is not assumed.
- **The one-hour scan interval is a starting value, not a contract.** It is stated so the queue's
  staleness is bounded and testable; changing it later is a tuning decision, not a spec change,
  provided the "not live, refreshes periodically" statement stays true to whatever it becomes.
- **Bank senders are identified by sender identity, not by message content**, so a promotional
  message that happens to contain an amount is excluded structurally rather than by guesswork.
- **An account is matched to a proposal by the partial account identifier the message contains**, and
  a proposal that cannot be matched to a known account is presented as needing input rather than
  being assigned to a default.
- **Duplicate matching is a same-amount, same-account, near-in-time comparison.** The exact tolerance
  is a stated number rather than a hidden heuristic; the default assumption is an exact amount match
  on the same account within three days.
- **Freezing on consent withdrawal is a deliberate retention choice, not an oversight.** The app
  keeps message-derived proposals the user has not reviewed, rather than destroying them, because
  withdrawal is a one-tap toggle and losing a queue of unreviewed entries to a mistap is worse than
  the retention. What makes it defensible is stated in the requirements rather than assumed: the
  rows are inert (no further interpretation, no acceptance possible), the user is told they exist and
  offered one action to delete them all, and they stay inside the erasure path.
- **A standing rule is taught explicitly from a correction, never inferred silently.** The app does
  not learn from behaviour the user did not choose to turn into a rule; this keeps BR-G3's
  "user-visible, counted, revocable" honest.
- **The entries-waiting alert reuses Phase 6's delivery and notification centre wholesale.** It is a
  new alert type, not a new alert mechanism, and it lands in the same durable record as every other
  alert.
- **Only holdings the user marks as auto-valued are covered by the price feed.** A holding the user
  values themselves is never included in a proposed value update; opting a holding in is a user
  action, not a default applied to everything of a matching kind.
- **A price feed fetch is a read of public market data, not of the user's records**, so it introduces
  no new consent class. It runs under the sync consent already granted, and the holdings it names
  never leave the device as part of fetching a price.
- **The price feed checks on a cadence but proposes on a move.** The app is not a live ticker: it
  looks on the same periodic schedule as the message source, and a proposal appears only when the
  price has moved past the threshold. A proposal represents the price at a stated moment, shown with
  that moment.
- **The 5% default threshold is a starting value.** It is stated so the behaviour is testable and so
  the setting has a sensible day-one value, not because 5% is a researched number; the user can
  change it, and changing the default later is a tuning decision.
- **CSV import stays a visible, disabled promise.** Removing the call to action would be a silent
  descope; leaving it enabled-but-broken would be worse. It stays, disabled, with copy naming that it
  arrives later.
- **The feature ships flag-off** and stays off until its own checkpoint passes, per the route
  registry. The flag also carries the consent requirement every tracker route carries.
- **Distribution remains outside an app store for now**, so a message-reading permission does not
  block release; the store declaration obligations attach when store distribution is taken up, which
  is separately deferred.
- **Single currency** (Indian rupee) for all amounts, consistent with the tracker's existing shape.
- **Money is exact.** Every amount this feature reads, proposes and records is handled at the same
  exactness as manually entered money, with no rounding introduced by parsing.

## Dependencies

- **Phase 2** — holdings and the append-only valuation history the price feed's accepted proposals
  write into, and the correction path that history requires.
- **Phase 3** — the ledger, accounts, categories, the proposal store, recurring definitions, and the
  audit trail this feature writes into. **This feature discharges Phase 3's deferral** of the shared
  review queue.
- **Phase 0b Settings** — the control plane this feature's entry plugs into, the app lock the alert
  deep link honours, and the privacy masking amounts obey.
- **Phase 1** — the consent framework the message-reading switch extends, and the erasure path
  proposals and rules must be covered by.
- **Phase 6** — alert raising, delivery, and the notification centre. **This feature discharges Phase
  6's deferral** of the transactions-to-review alert.
- **Phase 5** — nothing directly; reporting reads the ledger, and records created here are ordinary
  records.

## Implementation record

> Constitution Article Xa: this section is empty until the feature ships. From the checkpoint onward
> this spec describes what *was* built, and any later change to shipped behaviour updates this
> section in the same change that alters the behaviour.

### As built

_Not yet implemented._

### Deviations from this spec

_None recorded._

### Deferred

_None recorded._

### Change log for this feature

_None recorded._