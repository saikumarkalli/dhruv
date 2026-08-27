# Contract: the bank message source

Covers FR-020…FR-028 and FR-029…FR-031. This is the phase's only sensitive read and its only runtime
permission, so every gate is stated explicitly rather than left to wiring.

## 1. Two gates, both required (FR-020)

```
read allowed  ⇔  androidPermission(READ_SMS) granted
              ∧  consent.readTransactionSms == true
```

Either missing ⇒ **nothing is read**, and the hub states *which* one is missing rather than showing a
generic "off" (FR-020, edge case).

**Why this is not redundant with `ConsentInterceptor`.** Everywhere else in this app the sensitive
operation is a network call, and the interceptor makes the gate structural. Reading the SMS inbox is a
local `ContentResolver` query — **no interceptor sits in front of it**. So the message source carries
its own gate, checked in the scanner's single entry point rather than in a screen, and a test asserts
there is no path from anywhere to `SmsInboxReader` that skips it.

**Before granting anything** (FR-021), the app states: messages are read **on this device**, only
**bank senders** are considered, and **nothing is recorded without approval**. This precedes the
system permission dialog — the OS prompt is not an explanation.

**Withdrawal** (FR-026, FR-026a–d): reading stops, the hub shows the source off, and outstanding
proposals **freeze** — see [review-queue.md](./review-queue.md) §4.

## 2. Reading: watermarked query, not a broadcast (R8)

| Decision | Value |
|---|---|
| Permission | `READ_SMS` — **not** `RECEIVE_SMS` |
| Mechanism | `ContentResolver` query over the system SMS inbox |
| Trigger | Periodic `androidx.work` request, ~1 hour (FR-027, FR-027a) |
| Filter | `date > watermark`, sender in allowlist |
| Watermark | Device-local setting; advances to the newest message read |
| First enable | Watermark set to **30 days ago**, then advances (spec assumption) |
| App open | Also triggers a read (FR-027c) |

`RECEIVE_SMS` exists to wake an app per message — the behaviour clarification 5 explicitly rejected.
A watermarked query is also correct in the cases a broadcast handles worst: device off, app
force-stopped, permission granted late. It reads what is there rather than depending on having been
listening (FR-027d).

**Best-effort cadence** (FR-027a): the device may delay or drop a scan. The next scan that runs picks
up everything since the watermark, so nothing is lost — the feature never assumes an exact interval.

**The queue is not live** (FR-027b): it states that it refreshes periodically, so a purchase made
moments ago being absent reads as expected rather than broken.

**No telephony** (FR-028): no SMS provider ⇒ the source row states it is unavailable on this device
rather than offering an inoperable switch.

## 3. Sender allowlist — identity, not content (FR-022)

Indian bank alerts arrive from alphanumeric sender IDs shaped `XX-YYYYYY`: a two-character
circle/operator prefix, a hyphen, then a six-character issuer code (`VM-HDFCBK`, `AD-ICICIB`). The
allowlist matches the **issuer code**, ignoring the prefix, which varies by circle for the same bank.

```kotlin
fun isBankSender(senderId: String): Boolean   // pure
```

**Content is never used to decide whether to consider a message.** A promotional SMS containing `₹` is
excluded because of who sent it, not because of what it says — which is what makes SC-004's *"zero
messages from non-bank senders produce a proposal"* achievable rather than aspirational.

The allowlist lives in `AutomationConfig.kt` (Article V — screen-level data is never inline).

## 4. Parsing — pure, two-stage, `Long` paise (R9)

```kotlin
fun parse(message: RawMessage): ParseResult    // pure, no Android types

sealed interface ParseResult {
    data class Parsed(
        val amountPaise: Long,        // never Double — Article VII
        val direction: Direction,     // DEBIT | CREDIT
        val occurredOn: LocalDate,
        val accountHint: String?,     // the last-4 the message quoted
        val merchant: String?,
    ) : ParseResult

    data class Unparseable(val missing: Set<Field>) : ParseResult
}
```

| Stage | Rejects |
|---|---|
| Transaction shape | Anything with no amount **and** no debit/credit signal — OTPs, balance summaries, marketing from a bank's own sender |
| Field extraction | Nothing. A missing field yields `Unparseable(missing)`, never a drop |

**`Unparseable` is a first-class outcome** (FR-010): it becomes a visible row naming what it needs.
Dropping it would make SC-003's remaining 10% invisible, which is the failure the design's *"Could not
tell what this was"* copy exists to prevent.

**Amount goes straight to paise.** The parser reads the digit groups and produces `Long` paise without
a `Double` intermediate — this is the single place in the phase where a float could plausibly have
been introduced, and `checkTrackerMoneyPrecision` scans for exactly that.

**Account resolution**: `accountHint` (last-4) is matched against `finance.accounts`. No match ⇒
`account_id` stays null and the row asks the user to pick — never a default account (spec assumption).

**Rule application**: if an active `automation_rules` row matches the extracted merchant, its category
is proposed and `applied_count` increments (FR-033, FR-034). See 7e.

## 5. Duplicate detection (FR-029…FR-031, R10)

**One batched RPC for the whole visible queue**, never one call per row:

```sql
finance.find_possible_duplicates(candidates jsonb)
  returns table (candidate_key text, transaction_id uuid,
                 amount_paise bigint, occurred_on date, account_id uuid, description text)
security invoker
```

`security invoker` is load-bearing: a `security definer` here would let a caller probe **another
user's** transactions by amount. RLS scopes it to the caller's own rows (Article IXa).

**Matching rule, stated to the user** (FR-031): **exact amount, same account, within 3 days.**

- *Exact* amount because a bank message quotes the posted amount to the paise. A tolerance would
  produce false matches between genuinely different purchases far more often than it would catch a
  real duplicate.
- The comparison happens in SQL on `bigint` paise — no client-side money arithmetic (Article VII).

| Requirement | Behaviour |
|---|---|
| FR-029 | Matching rows carry a callout **identifying** the matched record (amount, date, description) |
| FR-030 | Advisory — the user can still accept |
| FR-009a | But **accept-all never does** — flagged rows go to `skippedDuplicate` |
| US4-3 | Two genuinely identical same-day payments remain recordable; the callout informs, never blocks |

The callout **shows** the matched record inline rather than navigating to it — `OpenTransaction` does
not exist yet and this phase does not add it speculatively (see [routes.md](./routes.md)).

## 6. What never leaves the device (FR-024, FR-025, AUT-BR-002)

| Data | Leaves? |
|---|---|
| Original message text | **Never.** Lives only in Room's `automation_proposal.raw_text` (R1) |
| Sender identity | **Never** |
| The parsed proposal, while pending | **Never** — the whole row is device-local |
| The accepted transaction | Yes — it is an ordinary ledger row the user chose to create |
| The duplicate-check candidates | Amount, account id and date only — never text |
| A rule's matcher | The extracted merchant token only, never the message (R5) |

**This is structural, not procedural.** There is no outbound payload containing message text to audit,
because the entity carrying it has no network path at all. `finance.suggestions.raw_text` — the column
Phase 3 reserved for this — stays permanently unwritten, and its schema file carries a comment saying
why, so nobody later "finishes" the wiring and breaks AUT-BR-002.

## 7. Play policy — a stated risk, not a solved problem

`READ_SMS` is a Play restricted permission. Distribution is a signed APK outside the store (ADR-0008),
the flag ships off, and the store declaration obligation attaches when store distribution is taken up
(spec assumption; implementation plan risk R7). Recorded here so the next engineer meets it in the
contract rather than in a rejection email.