# Contract: the review queue (G2) and the Ignored list

Covers FR-003…FR-012, FR-008a–c, FR-009a–b and the two-store merge. The queue is the one screen every
automated source feeds, and the one place FR-001's rule is visible to the user.

## 1. The merged read

`ProposalRepository.observePending(): Flow<QueueState>` assembles one list from two stores (R2).

| Origin | Store | Failure mode |
|---|---|---|
| `RECURRING` | `finance.suggestions` (Supabase, Phase 3's) | Network. Can fail independently |
| `BANK_MESSAGE`, `PRICE_FEED` | `automation_proposal` (Room) | Local. Effectively cannot fail |

```
QueueState(
    rows: List<ProposalRow>,       // merged, newest first, grouped by local calendar day
    remoteStatus: Loaded | Loading | Failed(retryable),
    frozenBannerVisible: Boolean   // derived, §4
)
```

**Partial failure is a first-class state, not an error screen.** If the remote half fails, the local
rows still render and the remote half shows a retry affordance in place of its own rows. An
all-or-nothing error would hide device-local proposals behind a network problem they have nothing to
do with — the queue's whole value is that it is one list, and it should degrade to a shorter list, not
to nothing.

**Ordering**: newest `created_at` first, grouped under `DayGroupHeader` (TODAY / YESTERDAY / date).
Backlog rows from a missed scan window (FR-027d) land in their own date groups, not all under today —
that is what "in date order" means in FR-027d.

## 2. Row contract

Every row renders in the **not-yet-accepted treatment** (`SuggestedRow`, dashed) — FR-004 — and states
its kind unmistakably (FR-003a, SC-014).

### `kind = TRANSACTION`

| Shown | Source | Missing ⇒ |
|---|---|---|
| Amount | `amount_paise`, `MoneyText`, masked per FR-051 | `UNPARSEABLE` |
| Date | `occurred_on` | `UNPARSEABLE` |
| Account | `account_id` resolved, else `account_hint` ("card ••4432") | Row asks the user to pick |
| Category | rule match, else null | Row asks the user to pick (FR-010) |
| Original text | `raw_text`, `BANK_MESSAGE` only (FR-005) | — |
| Duplicate callout | see [sms-source.md](./sms-source.md) §5 and 7d | — |

### `kind = VALUE_UPDATE`

| Shown | Source |
|---|---|
| What is being revalued | holding name + how many holdings the price affects |
| From → to | `recorded_value_paise` → `proposed_value_paise` |
| Why now | how far the price moved, and from what (FR-044c) |
| As at | `price_taken_at`, so a stale proposal is visibly stale |

### Unparseable rows (FR-010)

`parse_confidence = UNPARSEABLE` renders the design's copy — *"Could not tell what this was — pick a
category"* — generalised over `missing_fields`, so a row missing an account says so rather than
naming a category. **Never dropped, never guessed**: SC-003's remaining 10% must be visible.

## 3. Actions

| Action | Effect | Requirement |
|---|---|---|
| **Accept** | `TRANSACTION` → insert `finance.transactions`; `VALUE_UPDATE` → insert `finance.valuations`. Status → `ACCEPTED` | FR-007 |
| **Correct then accept** | Category (or account) changed first; the user's value wins over the proposal's | FR-006, US1-3 |
| **Ignore** | Status → `IGNORED`. Nothing written to tracker data. For `VALUE_UPDATE`, sets `last_ignored_price_paise` | FR-008 |
| **Accept all** | See §5 | FR-009, FR-009a, FR-009b |
| **Restore** (Ignored list) | Status → `PENDING`, `decided_at` cleared | FR-008b |

Accept and Ignore are reachable **from the row itself** — swipe or inline buttons — without opening a
detail screen (FR-012).

**The audit trail is not new code.** FR-002's "came from an automated source" entry is written by
Phase 3's existing `trg_transaction_audit` trigger when the row is inserted. This phase supplies the
source on the inserted row; it does not write history itself.

## 4. Frozen rows — derived, never stored (R7)

```
frozen(row) = row.origin == BANK_MESSAGE && !consent.readTransactionSms
```

| Behaviour | Requirement |
|---|---|
| Frozen rows stay **visible**, rendered non-actionable | FR-026a |
| A banner (`InfoBanner`) states message reading is off and offers exactly two actions: **delete them**, **turn it back on** | FR-026a |
| The banner names which rows it governs; other origins in the same queue stay fully actionable | FR-026b |
| Re-granting consent restores them exactly as they were, with no duplicates and no migration | FR-026c |
| No interpretation happens while frozen; the rows are inert | FR-026d |
| Accept-all skips frozen rows — they are not acceptable at all | §5 |

**There is no `FROZEN` status constant.** Adding one would need a migration on withdrawal and a
reverse migration on re-grant — the exact mechanism that produces the duplicates FR-026c forbids.

## 5. Accept-all — a pure partition (R15)

```kotlin
fun partitionForAcceptAll(rows: List<ProposalRow>): AcceptAllResult
```

```
AcceptAllResult(
    accept:            List<ProposalRow>,   // written
    skippedIncomplete: List<ProposalRow>,   // missing a required field
    skippedDuplicate:  List<ProposalRow>,   // carries a duplicate callout
    skippedValueUpdate:List<ProposalRow>    // kind = VALUE_UPDATE
)
```

| Row | Accepted by accept-all? | Why |
|---|---|---|
| `TRANSACTION`, parsed, no duplicate callout, not frozen | **yes** | The boring majority this action exists for |
| `TRANSACTION`, `UNPARSEABLE` or missing account/category | no → `skippedIncomplete` | FR-009a |
| `TRANSACTION` with a duplicate callout | no → `skippedDuplicate` | FR-009a. Confidently parsed but probably wrong |
| `VALUE_UPDATE` | no → `skippedValueUpdate` | FR-009a. Accepting changes something entirely different |
| Frozen | no | Not acceptable by any route (FR-026d) |

**The result is always reported** (FR-009b): how many were accepted, and how many left under each of
the three reasons. When nothing qualifies, it says so rather than appearing to do nothing (edge case).

**Why pure and why it lands first.** SC-006a ("accept-all records zero duplicate-flagged transactions
and changes zero holding values") is proven by a table-driven unit test over every queue composition.
Filtering inline in the ViewModel would make the same claim provable only by clicking, and this is the
one action in the feature that can corrupt data in a single tap.

## 6. Never proposed twice (FR-008c)

A source checks its `source_key` against `automation_proposal` **and** `automation_seen_key` before
inserting. A hit means skip — whatever the earlier row's terminal state, and regardless of a rescan, a
source re-enable, or the FR-027d backlog.

Recurring proposals inherit Phase 3's `(recurring_id, due_on)` unique constraint rather than
re-implementing it. Price proposals are the documented special case — see
[price-feed.md](./price-feed.md).

## 7. States

Both the queue and the Ignored list define all six (FR-011).

| State | Component | When |
|---|---|---|
| default | — | rows present |
| loading | `SkeletonBlock` | first load only |
| empty | `EmptyStateCard` | "Nothing waiting." Ignored list: "Nothing ignored." |
| error | `RetryErrorCard` | the **remote half** only — local rows still render (§1) |
| offline | `OfflineBanner` over local rows | local proposals remain fully usable offline; accepting queues or fails visibly (edge case) |
| signed-out | `SignedOutCard` | no session |
| disabled | `FeatureDisabledCard` via `FeatureHost` | `automation` flag off |

**Offline accepting**: an accept either completes when connectivity returns or fails visibly with the
proposal still `PENDING`. Never a silent loss — the row must not disappear on a write that did not
land.