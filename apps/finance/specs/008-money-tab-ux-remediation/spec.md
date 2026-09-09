# Feature Specification: Money tab — usability remediation

**Feature Branch**: `feat/money-tab-ux-remediation`

**Created**: 2026-09-06

**Status**: Draft — 2 clarifications open

**Input**: User description: "feedback for 002-money-tab spec"

---

## Why this is a spec and not a `002` amendment

`002-money-tab` shipped 2026-09-05. A post-ship review — read across all nine screens (D1–D9),
their view models, the shell's navigation wiring, and the database constraints — found **twenty-one
defects, four of which stop a real user from completing the module's primary task**, and **six of
that spec's own requirements delivered in name only**. That is more than a defect row: it re-opens
shipped behaviour, adds surfaces that were never built, and needs its own scenarios and acceptance
criteria.

Most of the list shares one shape: **the data layer is built and tested, and no screen reaches it.**
Editing a transaction, editing an account, editing a recurring rule, saving a filter view,
splitting a payment and attaching a receipt are all stored, mapped and — in four cases — already
unit-tested. What is missing in each is the affordance. That is why this reads as a large spec for
what is mostly reconnection work.

Per `apps/finance/CLAUDE.md`'s tracking rule and constitution Article Xa, this spec is the *vehicle*;
**`002-money-tab`'s Implementation record remains the destination.** Every item closed here also
lands a dated row in `apps/finance/specs/002-money-tab/spec.md` § "Implementation record" naming the
FR whose stated behaviour was not actually delivered. This spec does not replace 002 or renumber its
requirements — it references them.

Findings are traced to their originating requirement in § Traceability. Where this spec introduces a
new requirement, it is numbered `FR-2xx` so it can never be confused with an `002` requirement.

---

## Clarifications

### Session 2026-09-06

- Q: When someone picks "Transfer" in the quick-add sheet, should the sheet grow a destination-account field, or should transfers only be available in the full form? → A: Keep Transfer visible but disabled, with helper text pointing at "More options"
- Q: Should splitting one transaction across several categories be built as part of this remediation, or formally deferred to a later phase? → A: Build it fully in this remediation (User Story 8, FR-224 to FR-227)
- Q: How should accounts, categories and recurring entries be reached from the ledger? → A: Move them into a Money top bar's overflow menu, leaving only search + filter in the content row
- Q: When someone edits a transaction, should they be allowed to change its type — turning a recorded expense into a transfer, or the reverse? → A: Any type change is allowed; the form swaps its required fields as the type changes (FR-228)
- Q: Where should accounts and categories be created and managed? → A: Settings for both (create, edit, delete); recurring stays in the Money tab because its review queue is an active surface, not configuration. **This supersedes the third clarification above** for accounts and categories — see the supersession note below.
- Q: What should a brand-new user get seeded, given they currently start with zero accounts and cannot save anything? → A: A starter set of common categories plus one "Cash" account, all behaving as ordinary rows (User Story 10, FR-238 to FR-241)

**Supersession note — clarification 3, same session.** Clarification 3 placed accounts, categories
and recurring together in the Money tab's top-bar overflow. Clarification 5 splits that group: the
two that are **configuration** (accounts, categories) move to Settings, and the one that is an
**active queue** (recurring) stays in the Money overflow. The reasoning behind clarification 3 —
that unlabelled icons crowding the search row is the defect, and that the tab owes itself a top bar
either way — is unchanged and still holds; only the destination of two of the three items moved.
Recorded rather than edited away, so the reasoning trail stays readable.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Recording money never dead-ends (Priority: P1)

Someone opens the Money tab, taps the add button, keys in an amount, and saves. Whatever happens
next, they are told: it saved, or it needs one more field, or it could not reach the server. They are
never left looking at a button that does nothing.

Today all three of those outcomes are silent. An amount with no account chosen produces no message
and no visible change. A network failure produces no message and no visible change. The two are
indistinguishable from a slow tap, and both leave the person unsure whether their money was recorded.

**Why this priority**: This is the module's single most-used flow and the app's whole reason to
exist. A person who cannot tell whether their spending was recorded stops trusting every number the
app shows them, including the ones that are correct.

**Independent Test**: Open quick add, attempt to save with a missing field, then with the network
disabled, then successfully. Confirm each produces a distinct, visible, actionable result.

**Acceptance Scenarios**:

1. **Given** an amount is entered but no account is chosen, **When** the person taps Save, **Then**
   the field that is missing is named on screen and the entry is preserved.
2. **Given** every required field is complete but the request cannot reach the server, **When** the
   person taps Save, **Then** a message says the entry could not be saved and offers to retry, and
   the entry is preserved exactly as typed.
3. **Given** a retry after a failed save, **When** it succeeds, **Then** exactly one transaction
   exists — a retry never produces a duplicate.
4. **Given** all required fields are complete and the network is available, **When** the person taps
   Save, **Then** the entry closes and the new row is visible in the ledger without a manual refresh.
5. **Given** the quick-entry sheet, **When** the transfer type is shown, **Then** it appears
   visibly unavailable, states that transfers are recorded in the full form, and cannot be
   selected — so no path reaches Save with a transfer that cannot be recorded.

---

### User Story 2 — A signed-out person can sign in (Priority: P1)

Someone who is not signed in opens the Money tab. They are told they need to sign in, and the
button that says "Sign in" signs them in.

Today that button exists on the ledger, the accounts list and the account detail, and on all three
it does nothing at all. A signed-out person cannot get past the first screen of the module by any
route inside it.

**Why this priority**: It is a total block, not a degradation — the entire module is unreachable, and
the failure is silent, so the person concludes the app is broken rather than that they are logged
out.

**Independent Test**: Sign out, open Money, tap "Sign in" from each of the three surfaces that offer
it, and confirm each reaches the real sign-in flow.

**Acceptance Scenarios**:

1. **Given** no active session, **When** the Money tab is opened, **Then** a signed-out state
   explains the situation and offers a sign-in action.
2. **Given** the signed-out state is showing, **When** the sign-in action is used, **Then** the
   app's real sign-in flow starts.
3. **Given** sign-in completes, **When** the person returns, **Then** the screen they started from
   shows their data without a manual refresh or an app restart.

---

### User Story 10 — A brand-new account can record its first expense immediately (Priority: P1)

Someone signs in for the first time, taps add, keys in an amount, and saves. It works, with no setup
first.

Today it cannot. A new person is given exactly two categories — "Uncategorised" and "Adjustment" —
both of which are internal plumbing rather than anything anyone would choose, and **zero accounts**.
Because an account is required to save anything at all, the very first thing a new person tries is
guaranteed to fail. They must find the accounts screen, create an account, then come back.

They start instead with a set of everyday categories and one "Cash" account, all ordinary rows they
can rename, edit or delete like anything they created themselves.

**Why this priority**: It is a total block on first use, sitting directly behind the app's primary
action. Every other first-run problem in this spec is something a person could work around; this one
they cannot.

**Independent Test**: Sign in as a person who has never used the app, tap add, enter an amount, and
save — without visiting any other screen first.

**Acceptance Scenarios**:

1. **Given** a person using the app for the first time, **When** they open quick entry, **Then** the
   category and account pickers both offer real, usable choices.
2. **Given** a person using the app for the first time, **When** they enter an amount and save,
   **Then** the transaction is recorded without them having visited any setup screen.
3. **Given** seeded rows, **When** the person renames, edits or deletes one, **Then** it behaves
   exactly as a row they created themselves — nothing is protected or special-cased.
4. **Given** a person who has deleted a seeded row, **When** they return to the app later, **Then**
   it is not silently recreated.
5. **Given** an existing person who already has their own accounts and categories, **When** they use
   the app, **Then** nothing is seeded and nothing they have is changed.
6. **Given** seeding is interrupted part-way, **When** the person returns, **Then** it completes
   without producing duplicates.

---

### User Story 3 — Every screen says where you are and how to leave (Priority: P1)

Someone drills from the ledger into accounts, categories or recurring entries. Each screen names
itself and shows one way back to where they came from.

Today five drill-in screens — accounts, account detail, categories, recurring, and the recurring
review queue — have no title and no back control of any kind. **Confirmed on a device: the only way
back is the phone's own back button or gesture.** Nothing on screen says which screen this is, and
nothing offers a way out of it.

That turns out to matter for more than tidiness. Because the system gesture is the *only* route
back, it is also the route people use on the forms — and the system gesture does not go through the
"you have unsaved changes" confirmation at all. Type an amount into the full form, press back, and
the entry is gone without a word. The confirmation exists, but it is wired only to a close button
that these screens do not have. The one affordance that works is the one that skips the safeguard.

**Why this priority**: The missing icon on its own is a polish failure against navigation law N2 —
on a gesture-navigation phone a first-time user has no signal that these screens are exits rather
than dead ends. The silent data loss behind it is not polish, and it is only reachable *because* the
visible control is missing.

**Independent Test**: Navigate into each of the five screens and confirm each shows a title and a
single back control. Then, on each form, type something and leave using the phone's back gesture —
confirm the same "discard changes?" question appears as when the on-screen control is used.

**Acceptance Scenarios**:

1. **Given** any drill-in screen inside Money, **When** it is shown, **Then** it displays a title
   naming the screen and exactly one back control.
2. **Given** the back control is used, **When** it completes, **Then** the person is on the screen
   they came from, with its content current.
3. **Given** any Money screen, **When** the person leaves it using the phone's back button or
   gesture, **Then** the outcome is identical to using the on-screen back control — the same
   destination and the same confirmations.
4. **Given** a form with unsaved changes, **When** the person leaves by the phone's back gesture,
   **Then** they are asked to confirm before anything is discarded, exactly as they are when using
   the on-screen control.
5. **Given** a tab root, **When** it is shown, **Then** it shows no back control.

---

### User Story 4 — Setup lives in Settings; the ledger stays about money (Priority: P2)

Someone setting up their accounts and categories does it in Settings, in one place, once. Someone
recording and reading their spending stays in the Money tab and is not asked to think about
configuration.

Today all three of accounts, categories and recurring are unlabelled icons crowded into the same row
as the search field, alongside a fourth icon that is a filter control rather than a destination. On a
narrow screen the search field is squeezed below its own placeholder width. A price-tag glyph is the
only affordance for "Categories". Meanwhile a Settings entry for Money already exists and its
Categories row is a dead count — it displays a number and goes nowhere.

The split is by what the thing *is*. Accounts and categories are **configuration**: set up rarely,
changed rarely, and they belong in Settings, where the existing dead row becomes a real one.
Recurring entries are an **active queue** — things arrive in it and are acted on — so they stay in
the Money tab. The ledger's content row is left to search and filter, the two controls that act on
the transactions in front of you.

**Why this priority**: The features exist and work; people simply will not find them. That is
indistinguishable from not shipping them, but far cheaper to fix. Consolidating setup also removes
the current situation where the same concept has two half-built homes.

**Independent Test**: Give someone who has not seen the app the goal "add a bank account" and observe
whether they reach the right screen without help. Separately, render the ledger at the narrowest
supported width and confirm no control is clipped or truncated.

**Acceptance Scenarios**:

1. **Given** Settings, **When** someone opens the Money entry, **Then** accounts and categories are
   each listed as a real destination they can open, add to, edit within, and remove from.
2. **Given** the Settings Money entry, **When** its categories row is shown, **Then** it opens the
   categories screen — it is never a number that goes nowhere.
3. **Given** the Money tab, **When** someone opens its overflow menu, **Then** recurring entries are
   listed with a text label and open their screen.
4. **Given** the Money tab, **When** its content row is read, **Then** it holds only the search field
   and the filter control.
5. **Given** the Money tab with no accounts or no categories yet, **When** the empty state is shown,
   **Then** it names where setup happens and takes the person there directly.
6. **Given** the narrowest supported screen width, **When** the ledger renders, **Then** the search
   field shows its full placeholder and no control is clipped.
7. **Given** the Money tab root, **When** its top bar is shown, **Then** it names the tab and shows
   no back control.

---

### User Story 5 — Fixing a mistake does not mean destroying the record (Priority: P2)

Someone records a transaction against the wrong category, mistypes an account name, or needs to
raise a recurring rent amount. In each case they correct the thing itself.

Today none of the three can be edited from any screen. The transaction's correction path is to delete
it — which discards the audit trail the module exists to keep — or to duplicate it, which leaves the
wrong row in place forever. The account's name is permanent once created. A changed rent means
deleting the recurring rule and rebuilding it.

**Why this priority**: Correcting an entry is a daily task in any money tracker, and every current
workaround damages data. The capability is already promised by three shipped requirements
(`002` FR-006, FR-016, FR-031a) and the supporting data operations already exist and are tested —
only the screens are missing.

**Independent Test**: Create a transaction, an account and a recurring rule; edit each; confirm the
change is applied, recorded in history where applicable, and that no duplicate or orphan is created.

**Acceptance Scenarios**:

1. **Given** a recorded transaction, **When** the person edits its amount, category, account, payee
   or note, **Then** the change is saved and a plain-language history entry describes what changed.
2. **Given** an edit is in progress with unsaved changes, **When** the person tries to leave,
   **Then** they are asked to confirm before the changes are discarded.
3. **Given** an existing account, **When** the person edits its name, type, masked identifier or
   primary flag, **Then** the change is applied everywhere the account appears.
4. **Given** a recurring rule, **When** the person edits its amount, category, account or schedule,
   **Then** future entries use the new values and entries it already produced are unchanged.
5. **Given** an editable item, **When** its screen is shown, **Then** the edit affordance is present
   and discoverable without entering a menu of unrelated destructive actions.
6. **Given** a recorded expense being edited, **When** the person changes its type to transfer,
   **Then** the form asks for a destination account, stops asking for a category, and cannot be
   saved until the new type's own fields are complete.
7. **Given** a recorded transfer being edited, **When** the person changes its type to expense or
   income, **Then** the form asks for a category, stops asking for a destination account, and the
   change is saved with a history entry naming the type change.

---

### User Story 6 — A filter you use often can be kept (Priority: P3)

Someone who checks "restaurants over ₹1,000 this month" every week saves that combination once and
re-applies it by name.

Today filters can be built and applied but never named or kept. The capability's data layer is built
and tested; no screen reaches it.

**Why this priority**: A genuine convenience with a real audience, but nobody is blocked without it,
and everything it does can be redone manually in a few taps.

**Independent Test**: Build a filter, save it under a name, reset, re-apply it by name, and confirm
the same result set.

**Acceptance Scenarios**:

1. **Given** a filter combination that has been built, **When** the person saves it with a name,
   **Then** it is listed for later re-use.
2. **Given** a saved view, **When** it is applied, **Then** the ledger shows exactly what the filter
   would have shown if rebuilt by hand.
3. **Given** a saved view, **When** it is renamed or removed, **Then** the change persists and no
   transaction is affected.

---

### User Story 7 — The module tells the truth about its own state (Priority: P3)

Someone offline, or looking at a due date, or about to delete an account, gets an honest and
consistent answer.

Today the ledger has no offline state at all while the accounts screen it leads to does, so losing
connectivity produces a generic failure on one screen and a correct offline treatment one tap later.
The recurring review queue shows raw machine-format dates ("Due 2026-09-06"). Deleting an account is
a full-width destructive button placed directly beneath the everyday "Add transaction" action, with
no separation.

**Why this priority**: Each item is small on its own; together they are the difference between a
module that feels finished and one that feels assembled. None blocks a task.

**Independent Test**: Disable connectivity and visit every Money screen; check every user-visible
date; attempt an account deletion and observe the placement and confirmation of the action.

**Acceptance Scenarios**:

1. **Given** no connectivity, **When** any Money screen is opened, **Then** it shows the offline
   state, not a generic error.
2. **Given** any date shown to a person, **When** it is read, **Then** it is in the app's stated
   human date format, never a raw machine format.
3. **Given** an account detail screen, **When** it is shown, **Then** the destructive action is
   visually separated from the everyday actions and requires an explicit confirmation naming its
   consequence.
4. **Given** a screen reader, **When** the quick-entry amount is focused, **Then** it is announced
   with what the number means, not as a bare figure.
5. **Given** the quick-entry sheet, **When** the note field is focused, **Then** the amount keypad
   and the save action remain reachable.

---

### User Story 9 — Record what happened, when it happened (Priority: P2)

Someone forgot to log Saturday's dinner and enters it on Monday, dated Saturday. Someone else
photographs a bill and keeps it with the transaction.

Today neither is possible. Every transaction the app can produce is stamped with the moment it was
saved — there is no date field, no time field and no picker anywhere, so an entry made on Monday is
a Monday entry no matter when the money actually moved. Receipts are the same story from the other
end: the storage, the display and the "stays on this device" assurance are all built and working,
and there is no way to ever attach one.

**Why this priority**: Back-dating is a daily need in any money tracker — people log in batches,
after the fact. Without it the ledger's day grouping, the month summary and every category total are
quietly wrong for anyone who does not record at the instant of spending, and nothing tells them so.
It ranks below the four P0 blockers only because those fail silently; this one fails visibly, in the
sense that the wrong date is at least on screen.

**Independent Test**: Record a transaction dated three days ago and confirm it lands in that day's
group with the month summary adjusted. Attach a photo to a transaction, reopen it, and confirm the
image is there and never left the device.

**Acceptance Scenarios**:

1. **Given** the full form, **When** the person sets a date and time other than now, **Then** the
   transaction is recorded at that moment, not at the moment of saving.
2. **Given** a back-dated transaction, **When** the ledger is read, **Then** it appears under its own
   day's group, in its own month, and that month's summary includes it.
3. **Given** the full form, **When** a date after the present moment is chosen, **Then** saving is
   refused with a message saying why. A same-day earlier time is always accepted.
4. **Given** the full form, **When** the person attaches a photo of a receipt, **Then** it is stored
   on the device only, shown on the transaction, and the screen states it is not uploaded.
5. **Given** a transaction with a receipt, **When** the person replaces or removes it, **Then** the
   change is applied and the previous image is deleted from the device.
6. **Given** a transaction being edited, **When** its date is changed to a different month, **Then**
   both the old and new month's summaries reflect the move.

---

### User Story 8 — One payment, more than one category (Priority: P3)

Someone pays ₹2,400 at a supermarket: ₹1,800 of it was groceries and ₹600 was household supplies.
They record it once and divide it between the two categories, and each category's totals count only
its own share.

Today this cannot be expressed at all. The person must either pick one category and accept that the
other is wrong, or record two separate transactions that no longer look like the single payment they
actually made.

**Why this priority**: Real and recurring — supermarket runs, shared bills, one card swipe covering
two things — but nobody is blocked without it, and the current workaround (two transactions) at
least keeps the totals right. It is the only capability in this spec that is genuinely new rather
than reconnected, which is why it sits below the seven remediation stories.

**Independent Test**: Record one payment divided across two categories; confirm the ledger, each
category's totals, and the account balance all reflect the parts correctly and none double-counts.

**Acceptance Scenarios**:

1. **Given** a transaction being recorded in the full form, **When** the person divides it across two
   or more categories, **Then** each part carries its own amount and category and the parts are
   recorded together as one payment.
2. **Given** a split being edited, **When** the parts do not add up to the stated total, **Then** the
   remaining or excess amount is shown and the entry cannot be saved until it balances.
3. **Given** a saved split, **When** any part is opened, **Then** it identifies itself as part of a
   larger payment and offers to show the other parts.
4. **Given** a saved split, **When** the month's totals are read, **Then** each category is charged
   only its own part and the payment's full amount is counted exactly once against the account.
5. **Given** a saved split, **When** a part's amount or category is corrected, **Then** the balance
   rule is re-checked and history records the change on that part.
6. **Given** a saved split, **When** a part is deleted, **Then** the person is told what happens to
   the rest of the payment before it is applied.

---

### Edge Cases

- A half-typed quick entry is abandoned with the phone's back gesture — the person is told whether
  anything was kept.
- The phone's back gesture is used while a confirmation dialog or a picker is already open.
- The phone's back gesture is used on a form immediately after a save has begun but not finished.
- A person deletes every seeded category, then opens quick entry — the reserved rows still exist,
  but nothing usable does.
- A person deletes the seeded cash account after recording transactions against it.
- Two devices sign in to the same account for the first time at nearly the same moment — seeding
  must not run twice.
- Seeding is attempted while offline, or while consent has not been granted.
- A person renames a seeded category to match one they created themselves.
- A date is chosen that is valid but implausible — years in the past — the system accepts it rather
  than inventing a limit it never stated.
- A date is set to the future by exactly a few seconds because of a clock difference.
- A back-dated transaction is entered into a month the person is not currently viewing.
- A receipt photo is chosen and the person cancels the picker, or picks a file that is not an image.
- A receipt's underlying file is removed from the device by another app after being attached.
- A transaction carrying a receipt is deleted, then restored from the undo action.
- A save succeeds on the server but the response never arrives, and the person retries.
- A split is entered with only one part — it is an ordinary transaction, not a split, and must not
  be recorded as one.
- A split's parts are entered as zero or negative amounts.
- A split's parts add to more than the stated total, or leave a remainder.
- Two parts of the same split are assigned the same category.
- A category used by a split part is later merged into another, or deleted.
- A split part is opened from a category filter that matched only that one part.
- A save is attempted twice because the first appeared to do nothing — the system must produce one
  transaction, not two.
- Connectivity is lost mid-save: the entry is preserved and retryable, never silently dropped.
- Sign-in is cancelled part-way: the person returns to the signed-out state, not to a blank or
  errored screen.
- An account is edited to a name already used by another account — the system states its rule rather
  than failing silently.
- An account's type is changed from a spendable type to credit (or back): "spendable now" must
  recompute, since the two are defined differently.
- A recurring rule is edited while entries from it are already awaiting review in the queue.
- A transaction produced by a recurring rule is edited directly — the rule itself must not change.
- A transaction is edited to a different account: both accounts' balances and any running balance
  must reflect the move.
- A transaction's type is changed to transfer while its chosen category is the only thing the
  ledger's active filter matched on.
- A part of a split is edited to become a transfer — a transfer holds no category, so it cannot
  remain part of a category split; the person must be told what happens to the rest of the payment.
- A type change is abandoned part-way, after the form has already swapped which fields it asks for.
- A saved view references a category that is later merged or deleted.
- A device set to its largest text size renders every screen without clipping or overlap.
- The narrowest supported screen width renders the ledger controls without truncation.

---

## Requirements *(mandatory)*

### Functional Requirements

**Feedback and completion (US1)**

- **FR-201**: Quick entry MUST name the specific missing field on screen when a save is attempted
  and cannot proceed. A save attempt MUST never be a silent no-op.
- **FR-202**: A save that fails MUST show a message stating the entry was not saved, offer a retry,
  and preserve every value already entered.
- **FR-203**: A retry of a failed save MUST NOT create a second transaction if the original in fact
  succeeded. When a retry is refused because the original attempt was already recorded, the person
  MUST be told the entry **was saved** — never that it failed. (Today this case is reported as an
  ordinary save failure, which is the one message guaranteed to be wrong; `002` FR-036 names it as
  an open follow-up.)
- **FR-203a**: The same single-attempt identity rule MUST apply to the "make it recurring" write and
  to accepting a pending entry, not only to a plain transaction create — a retry on either path
  MUST NOT produce a duplicate. *(Closes the residual `002` FR-036 names and leaves open.)*
- **FR-204**: A successful save MUST leave the ledger showing the new entry without a manual
  refresh.
- **FR-205**: Quick entry MUST NOT present a transaction type it cannot complete as if it were
  selectable. The transfer type MUST remain **visible but unavailable** in quick entry, carrying
  text that says transfers are recorded in the full form. It MUST NOT be selectable, and MUST NOT
  be silently hidden — a person who expects three types and sees two would reasonably conclude
  transfers are unsupported.

**Access (US2)**

- **FR-206**: Every signed-out state inside the Money tab MUST offer an action that starts the app's
  real sign-in flow.
- **FR-207**: After a successful sign-in, the originating screen MUST show the person's data without
  a manual refresh or an app restart.

**Navigation (US3, US4)**

- **FR-208**: Every non-root Money screen MUST display a title naming the screen and exactly one
  back control returning to its single parent. Confirmed absent on a device across all five drill-in
  screens, where the phone's own back button or gesture is currently the only way out.
- **FR-208a**: Leaving a screen by the phone's back button or gesture MUST produce the same result
  as using its on-screen back control — the same destination, and the same confirmations. Neither
  route may do something the other does not.
- **FR-208b**: A form with unsaved changes MUST ask before discarding them **by every route that
  leaves the form**, including the phone's back button and gesture. Today the confirmation is
  reachable only through a close button, so the back gesture discards silently — and on the screens
  in FR-208 the back gesture is the only route that exists.
- **FR-208c**: Every Money form that accepts input MUST use the same unsaved-changes confirmation.
  The account form currently has none at all, by any route.
- **FR-209**: Accounts and categories MUST be created, edited and removed from the Settings entry for
  Money. Recurring entries MUST be reachable from the Money tab's top-bar overflow, carrying a text
  label. No destination may be represented by an unlabelled icon alone.
- **FR-209a**: The Settings Money entry's categories row MUST open the categories screen. It MUST
  NOT remain an informational count that navigates nowhere, as it does today.
- **FR-209b**: A Money screen that cannot proceed for want of an account or a category MUST name
  where setup happens and take the person there directly, rather than describing it.
- **FR-210**: The ledger's content row MUST hold only the search field and the filter control. The
  Money tab MUST have a top bar that names the tab, stays within the platform's stated top-bar
  budget, and — being a tab root — MUST NOT show a back control.
- **FR-211**: At the narrowest supported screen width, no ledger control may be clipped and the
  search field MUST show its full placeholder.

**Correction (US5)**

- **FR-212**: Users MUST be able to edit a recorded transaction's amount, type, category, account,
  destination account, payee and note. Editing MUST append a plain-language history entry, and MUST
  NOT create a second transaction. *(Delivers `002` FR-006, which shipped without any edit surface.)*
- **FR-213**: An edit form with unsaved changes MUST confirm before discarding them, by every route
  that leaves the form (see FR-208b).
- **FR-214**: Users MUST be able to edit an existing account's name, type, masked identifier and
  primary flag, under the same validation rules that govern creation.
  *(Delivers `002` FR-016's "and edit", which shipped create-only.)*
- **FR-215**: Users MUST be able to edit a recurring rule's amount, category, account and schedule.
  Entries the rule has already produced MUST NOT change.
  *(Delivers `002` FR-031a, which shipped with no edit surface.)*
- **FR-216**: Editing a transaction that a recurring rule produced MUST NOT alter that rule.
- **FR-228**: Changing a transaction's type during an edit MUST swap the fields the form requires —
  a transfer requires a destination account and holds no category; an expense or income requires a
  category and holds no destination account. The form MUST NOT be saveable until the new type's own
  fields are complete, and the discarded field's value MUST NOT be silently retained.
  *(Quick entry deliberately does not offer transfers at all — FR-205. The full form is where a
  transfer is created or corrected.)*

**Saved views (US6)**

- **FR-217**: Users MUST be able to save the current filter combination under a name, list saved
  views, apply one, rename it, and remove it. *(Delivers `002` FR-015's second half.)*
- **FR-218**: Applying a saved view MUST produce the same result set as rebuilding that filter by
  hand.

**Honest states (US7)**

- **FR-219**: Every network-backed Money screen MUST define an offline state and a signed-out state,
  consistently across the module.
- **FR-220**: Every date shown to a person MUST use the app's stated human date format.
- **FR-221**: A destructive action MUST be visually separated from everyday actions on the same
  screen and MUST require a confirmation that names its consequence.
- **FR-222**: The quick-entry amount MUST be announced by a screen reader with its meaning, not as a
  bare number.
- **FR-223**: While the quick-entry note field is focused, the amount keypad and the save action
  MUST remain reachable.

**When it happened, and proof of it (US9)**

- **FR-229**: Users MUST be able to set a transaction's date and time when recording it and when
  editing it. A transaction MUST be recorded at the moment the person states, not the moment they
  saved. *(Delivers `002` FR-004's date-and-time field, which shipped absent — every transaction the
  app can currently produce is stamped "now".)*
- **FR-230**: A transaction dated after the present moment MUST be refused at save time with a
  message saying why. A same-day, earlier time is always valid.
  *(Delivers `002` FR-004a, which is currently unreachable rather than merely untested, because
  there is no way to set a date at all.)*
- **FR-231**: A back-dated transaction MUST appear under its own day's group in its own month, and
  MUST be included in that month's summary. Moving a transaction to a different month MUST update
  both months' summaries.
- **FR-232**: Users MUST be able to attach a photo of a receipt to a transaction from the full form.
  *(Delivers `002` FR-004's receipt field. The storage, the display and the disclaimer all exist and
  work; nothing has ever been able to attach one.)*
- **FR-233**: A receipt MUST be replaceable and removable, and removing it MUST delete the stored
  image from the device.
- **FR-234**: A receipt MUST remain on the device. It MUST NOT be uploaded, and the screen MUST say
  so. Attaching a receipt MUST NOT introduce any new off-device data flow.

**Splitting a payment (US8)**

- **FR-224**: Users MUST be able to divide one recorded payment across two or more categories, each
  part carrying its own amount and category, recorded together as one payment.
  *(Delivers `002` FR-004's "split across categories", which shipped absent and unrecorded.)*
- **FR-225**: A split MUST NOT be saved unless its parts add up exactly to the stated total. While
  they do not, the outstanding or excess amount MUST be shown.
- **FR-226**: Each part of a split MUST identify itself as part of a larger payment wherever it is
  shown on its own, and MUST offer a way to see the other parts.
- **FR-227**: Every total in the app MUST count a split's full amount exactly once against its
  account, and charge each category only its own part. A split MUST never be double-counted, and a
  split with a single part MUST NOT be recorded as a split at all.

**First run (US10)**

- **FR-238**: A person using the app for the first time MUST be able to record a transaction without
  visiting any setup screen. Both the category and the account picker MUST offer real, usable
  choices from the outset.
- **FR-239**: A new person MUST be given a starter set of everyday categories and one cash account.
  These MUST be ordinary rows — renameable, editable and deletable exactly like rows the person
  creates — with no protected or special-cased behaviour. The two existing reserved rows keep their
  current, separate meaning and are not part of this set.
- **FR-240**: Seeding MUST happen once and MUST NOT recreate anything the person has since deleted
  or renamed. Repeating it MUST NOT produce duplicates, including after an interruption part-way.
- **FR-241**: Seeding MUST NOT alter anything belonging to a person who already has accounts or
  categories of their own.

**Record honesty**

- **FR-235**: On completion, `002-money-tab`'s Implementation record MUST state what actually
  shipped, what deviated, and what was deferred — including every gap this spec closes, each naming
  the `002` requirement whose stated behaviour was not delivered. That section is still the blank
  template it was created with, and it is the reason six of that spec's requirements
  (FR-004, FR-004a, FR-006, FR-015, FR-016, FR-031a) could go undelivered while the phase was
  recorded as shipped. Closing this spec without filling it in reproduces the same failure one
  level up.

### Key Entities

This feature introduces **no new stored entity**. It reaches entities `002-money-tab` already
defines — transaction, transaction history entry, account, category, recurring definition, pending
entry, saved view, split group — and adds the screens that were missing for four of them.

- **Saved view**: a named filter combination (type, categories, amount range, account) belonging to
  one person, re-applicable to the ledger. Already stored; not yet reachable.
- **Split group**: the identifier that binds the parts of one divided payment together. There is
  **no parent record holding a total** — each part is independently a full transaction, so nothing
  can double-count and every existing total already sums the parts correctly. The "parts must add
  up" rule of FR-225 is therefore an entry-time rule, not a stored constraint. Already defined and
  carried end to end; nothing has ever written it.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-201**: 100% of save attempts in quick entry produce a visible outcome within one second —
  saved, a named missing field, or a retryable failure. Zero silent no-ops.
- **SC-202**: A signed-out person reaches the sign-in flow from the Money tab in one tap, from every
  screen that offers the action.
- **SC-203**: Every non-root Money screen shows a title and one back control — 9 of 9 screens, zero
  exceptions.
- **SC-203a**: On every Money screen, leaving by the phone's back gesture and leaving by the
  on-screen control produce the identical outcome — verified screen by screen, with zero cases where
  one route discards unsaved input that the other would have asked about.
- **SC-204**: Someone who has not used the app before reaches the accounts screen from the ledger
  without assistance on the first attempt.
- **SC-205**: A wrongly-categorised transaction is corrected without deleting it and without
  creating a duplicate, and its history states what changed.
- **SC-206**: A saved filter re-applied by name returns exactly the same set of transactions as the
  hand-rebuilt filter — verified on at least three different filter combinations.
- **SC-207**: Every Money screen renders a correct loading, empty, offline, signed-out and loaded
  state — 0% blank screens across the state matrix.
- **SC-208**: Zero user-visible raw machine-format dates remain in the module.
- **SC-209**: Every Money screen passes a screen-reader pass: every icon-only action, the quick-entry
  amount, and every state card is announced meaningfully.
- **SC-210**: At the narrowest supported width and the largest supported text size, no Money screen
  clips or overlaps content.
- **SC-211**: A payment divided across categories is counted once against its account and once per
  category for that category's own part — verified across the ledger, the month summary, each
  category's totals and the account's running balance, with zero double-counting.
- **SC-212**: A transaction can be recorded for any past moment and lands in that moment's day and
  month everywhere it is counted. Zero transactions are stamped with their save time against the
  person's stated intent.
- **SC-213**: A receipt attached to a transaction is visible on that transaction and present in zero
  outbound network payloads.
- **SC-214**: A save retried after a lost response results in exactly one recorded item and a
  message that correctly says it was saved — verified on the plain create, the "make it recurring"
  write, and accepting a pending entry.
- **SC-215**: A person signing in for the first time records their first transaction without opening
  any setup screen — zero setup steps between first launch and first saved entry.
- **SC-216**: Accounts and categories have exactly one place they are managed from. Zero screens
  offer a second, competing route to create either, and no Settings row displays a value it cannot
  navigate to.

### Traceability

| Finding | Severity | Origin | Delivered by |
|---|---|---|---|
| Transfer offered in quick entry cannot ever be saved | P0 | `002` FR-003 | FR-205 (shown unavailable, points at the full form) |
| Save is a silent no-op on a missing field | P0 | `002` FR-002 | FR-201 |
| A failed save produces no feedback anywhere | P0 | `002` FR-036 | FR-202, FR-203 |
| All three "Sign in" actions do nothing | P0 | `002` FR-032 | FR-206, FR-207 |
| Five drill-in screens have no title and no back — device-confirmed, phone back is the only route | P1 | DESIGN-SYSTEM N2, §8 | FR-208 |
| The phone's back gesture bypasses the unsaved-changes confirmation entirely | P1 | `002` FR-005 | FR-208a, FR-208b |
| The account form has no unsaved-changes confirmation by any route | P2 | `002` FR-005 | FR-208c |
| Three destinations hidden as unlabelled icons; search truncates | P1 | DESIGN-SYSTEM §8, N5 | FR-209, FR-210, FR-211 |
| Settings › Money's categories row is a dead count that navigates nowhere | P2 | `004` FR-003 | FR-209a |
| A new user has zero accounts, so their first save cannot succeed | P1 | `002` FR-001 | FR-238, FR-239 |
| Seeded rows must not resurrect after deletion, or duplicate on retry | P2 | — *(follows from FR-239)* | FR-240, FR-241 |
| Destructive account delete adjacent to the primary action | P1 | DESIGN-SYSTEM §8 | FR-221 |
| No edit surface for a transaction | P2 | `002` FR-006 | FR-212, FR-213, FR-216, FR-228 |
| No edit surface for an account | P2 | `002` FR-016 | FR-214 |
| No edit surface for a recurring rule | P2 | `002` FR-031a | FR-215 |
| No surface to save or apply a named view | P2 | `002` FR-015 | FR-217, FR-218 |
| Ledger has no offline state; review queue lacks offline + signed-out | P2 | `002` FR-032 | FR-219 |
| Raw machine-format date in the review queue | P2 | DESIGN-SYSTEM §10 | FR-220 |
| Quick-entry amount unannounced to a screen reader | P2 | DESIGN-SYSTEM §9 | FR-222 |
| Note field's keyboard covers the keypad and save action | P2 | DESIGN-SYSTEM §8 | FR-223 |
| Split across categories never built, and the omission never recorded | P2 | `002` FR-004 | FR-224 – FR-227 |
| Date and time not settable — every transaction stamped "now" | P2 | `002` FR-004 | FR-229, FR-231 |
| Future-date rejection unreachable, because no date can be set at all | P2 | `002` FR-004a | FR-230 |
| Receipt attachment never built, though storage and display both work | P2 | `002` FR-004 | FR-232, FR-233, FR-234 |
| A retry after a lost response is reported as failed when it in fact saved | P2 | `002` FR-036 | FR-203 |
| Retry identity missing on the recurring write and pending-entry accept | P2 | `002` FR-036 | FR-203a |
| `002`'s Implementation record still holds its blank template after shipping | Process | Article Xa | FR-235 |

---

## Assumptions

- **Scope is the Money tab only.** Nothing here changes the net-worth, calculator, plan or insights
  surfaces, or any database schema. The transfer-shape rule that currently rejects an incomplete
  transfer at the database is correct and stays — this spec fixes the screen that lets someone
  build one, not the rule that catches it.
- **Back is one behaviour with two triggers, not two behaviours.** The app already resolves where a
  back press goes in one shared place, and that resolution is correct — what is missing is that a
  screen can currently have something to say about a back press (an unsaved form) and never be
  asked. Whatever mechanism lets a screen intervene must serve both the on-screen control and the
  phone's gesture from the same code, or the two will drift apart again the next time a form is
  added. Building the visible back control without this would fix the icon and leave the data loss.
- **"Managed in Settings" means the Settings entry owns the rows, not the screen code.** The
  Settings surface is assembled from what each module contributes; a Money screen moving *into*
  Settings means Money contributes a row that opens it, not that its code moves into the shared
  settings library. Moving screen code there would break the module-boundary rules the build
  enforces, and the Money contribution's own notes already record one such boundary it had to work
  around. This is stated because "move it to Settings" reads, wrongly, like a relocation.
- **Seeded content is a product decision with a maintenance cost.** The starter categories are
  user-visible text that must be translated alongside everything else, and adding to the list later
  does not retroactively reach people who have already been seeded. Keep the set small and
  genuinely everyday; it is a starting point people edit, not a taxonomy.
- **Two component-library gaps are real dependencies, not incidental.** There is no single-date
  picker in the shared library — only a date *range* sheet, and that one is planned rather than
  built. A photo picker is equally absent. Both must be added to the shared library and consumed
  from there; hand-rolling either inside the Money screens is exactly the fragmentation the
  component-reuse principle forbids. This is the main reason the date field was left undone in
  `002` and it does not disappear by being restated here.
- **Receipts stay device-local, deliberately.** Attaching one introduces no network call, no
  storage bucket and no new consent surface. Anything that would send a receipt off the device is a
  separate decision requiring its own consent design, and is out of scope.
- **No new stored entity; one migration, and not the kind you would expect.**
  *(Corrected 2026-09-06 by research R9 — this assumption originally read "no migration", which was
  wrong.)* No table and no column is added: every table involved already carries the
  `request_id` uniqueness this feature relies on. But the database trigger that writes a
  transaction's history does not currently track a transaction's **type** or its **destination
  account** — the two fields FR-228 newly makes editable — so a type change would append no history
  at all, quietly falsifying FR-212. Extending that trigger is the one migration this feature needs.
  Every other capability added here reaches data that already
  exists. Saved views, transaction updates and recurring edits all have working, tested data
  operations today; what is missing is the screens. Splitting is the same story one step earlier —
  the split-group identifier is defined and carried end to end, and the write pattern is already
  documented (each part is written as its own transaction sharing the identifier), but nothing has
  ever constructed one, so its entry screen, its balance rule and its sibling display are all new.
- **String extraction is out of scope and stays a recorded residual.** The module carries 113
  user-visible text literals against 6 extracted ones, already logged as a residual gap in `002`.
  Extracting them is a large mechanical sweep with no user-visible outcome on its own, and it would
  dominate this spec's diff. New and modified text added by this work follows the platform rule from
  birth; the existing backlog is carried forward unchanged and re-stated in `002`'s record so it is
  not mistaken for closed.
- **Scroll and render performance stays with the future performance spec.** The filter sheet
  recomputes its live result count synchronously as fields change, which is a structural concern
  worth noting, but measuring it belongs to the app-wide performance work already deferred in the
  implementation plan §7a — not to a one-off check here.
- **The reviewer is the sole approver.** SC-204 ("someone who has not used the app before") is
  validated by the maintainer walking the flow cold against a written script, not by a recruited
  usability study — consistent with how every prior phase's device pass was run.
- **Findings are code-verified, not inferred.** Each row in § Traceability was confirmed by reading
  the shipped screen, view model or navigation wiring, and by checking the database constraint where
  one is involved. None is derived from documentation alone — which is the failure mode
  ADR-0030 exists to prevent.

---

## Implementation record

> **Status: NOT YET IMPLEMENTED.** Filled in when this work ships, and maintained for the life of
> the feature thereafter — constitution Article Xa. Everything above this line describes what *will*
> be built; everything below describes what *was*.
>
> **On closure, every row below is also mirrored into
> `apps/finance/specs/002-money-tab/spec.md` § "Implementation record"**, each naming the `002`
> requirement whose stated behaviour was not actually delivered. That mirror is what makes `002`
> honest about its own shipped state; this spec alone does not.
>
> Module(s): `:apps:finance:feature:money`, `:apps:finance:app` (navigation wiring).

### As built

| Story / FR | Shipped | Notes |
|---|---|---|
| | | |

### Deviations from this spec

| Spec says | Built as | Reason |
|---|---|---|
| | | |

### Deferred

| Item | Deferred to | Reason |
|---|---|---|
| | | |

### Change log for this feature

| Date | Change | Type | FR affected | PR |
|---|---|---|---|---|
| | | fix / change / removal | | |
