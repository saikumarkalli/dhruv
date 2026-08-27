# Research: Automation (Phase 7)

Phase 0 output. Every decision that shapes `plan.md`, `data-model.md` and the contracts, each with
the alternative it beat. Written against the clarified `spec.md` (7 clarifications, 2026-08-23).

The spec deliberately left storage, scheduling and module topology to this phase. Two of the
decisions below (**R1**, **R3**) resolve a genuine contradiction between documents that both shipped
before this phase existed — those are called out as contradictions, not preferences.

---

## R1 — SMS proposals are **device-local**, not rows in Phase 3's Supabase store

**The contradiction.** Two shipped documents disagree:

- Phase 3's `data-model.md` gives `finance.suggestions` a `raw_text` column, annotated *"Phase 7's
  SMS source; unused this phase"* — i.e. Phase 3 expected SMS proposals to be Supabase rows carrying
  their original message text.
- `AUT-BR-002` (QA catalog §9) requires that **"no raw SMS text field is present in any outbound
  Supabase payload"**, and BR-G2 / FR-024 / FR-025 say interpretation happens on the device and the
  original text never leaves it.

Meanwhile FR-005 requires the queue to **show** the original source text on the row, so the text
cannot simply be discarded either.

**Decision.** Every proposal this phase produces from bank messages lives in a **device-local Room
table**, `automation_proposal`, never in Supabase. `finance.suggestions.raw_text` is left permanently
unused and is documented as such rather than removed (Phase 3 owns that table; dropping a column is
its change to make, and an unused nullable column costs nothing).

**Rationale.** A split — proposal row in Supabase, text in Room — was the obvious middle path and is
worse than either end: it produces a half-synced entity where the row a second device sees is missing
the one field FR-005 says the user needs to check the parse, and it leaves `raw_text` sitting in the
schema as a loaded gun for a future engineer who does not know BR-G2. Keeping the whole proposal
local makes AUT-BR-002 **structurally true** — there is no outbound payload to audit, because the
entity never has a network path — which is the same reasoning ADR-0029 used for putting consent in an
interceptor rather than in screen discipline.

**Consequences (accepted, specified downstream).**
- SMS proposals are **per-device**. A message read on the phone does not appear on a tablet. This is
  correct rather than a limitation: the message itself only ever arrived on one device.
- The spec's edge case *"the same proposal is accepted on two devices"* applies only to
  Supabase-backed recurring proposals, not to SMS ones. `data-model.md` states this.
- Erasure needs two arms, not one (**R11**).

**Alternatives rejected**

| Alternative | Why not |
|---|---|
| Proposal row in Supabase, `raw_text` in Room | A half-synced entity; second device shows a row it cannot verify; `raw_text` stays in the schema inviting misuse |
| Whole proposal in Supabase, text discarded after parse | Violates FR-005 — the user cannot check what the parser read, which is the whole reason the design puts source text on the row |
| Whole proposal in Supabase with text encrypted | Still an outbound payload containing message content; AUT-BR-002 is about the data leaving, not its readability |

---

## R2 — The review queue reads **two stores** and merges them

**Decision.** `ProposalRepository` returns one list assembled from:

- **`finance.suggestions`** (Supabase, Phase 3's) — recurring-generated proposals. Cross-device, as
  Phase 3 designed.
- **`automation_proposal`** (Room, this phase's) — bank-message proposals (R1) and price proposals
  (R3). Device-local.

The merged model carries an `origin` discriminator so a row knows which store owns it, and every
mutation routes back to the store it came from.

**Rationale.** Phase 3 already ships recurring proposals into Supabase and D9 already reads them; the
shared queue's job is to be the one screen over everything waiting, not to relocate another phase's
data. Moving recurring proposals into Room would break their cross-device behaviour and would edit a
table this phase does not own.

**Consequence.** The queue's loading and error states are per-origin: the local store always loads,
the remote one can fail. A remote failure shows the local rows plus a retry affordance for the remote
half, rather than an all-or-nothing error — specified in `contracts/review-queue.md`.

**Alternative rejected.** Mirroring `finance.suggestions` into Room and reading only Room. It buys a
single-source read at the cost of a sync problem this project has explicitly avoided everywhere else
(ADR-0014: the server is the single source of truth for tracker data), and Phase 3's `(recurring_id,
due_on)` idempotency key already solves the duplicate-materialisation problem a mirror would recreate.

---

## R3 — Price proposals are device-local too; accepting one is an ordinary valuation insert

**Decision.** A proposed value update is a row in the same device-local `automation_proposal` table,
of kind `VALUE_UPDATE`. Accepting it **inserts a `finance.valuations` row** — the ordinary append-only
write Phase 2 already exposes. No RPC, no new server-side writer.

**Rationale.** Three things make local the right home. The proposal is derived from public market data
plus a value the user already has, so it is not a financial record until accepted. Keeping it local
means **no background job ever writes into tracker data** — a property worth protecting on its own,
and the same one 006 protected by keeping its alert log local. And `finance.valuations` is append-only
by construction (SELECT + INSERT policies only, ADR-0029 decision 4 / Article IX), so acceptance is a
plain insert that needs no privileged path.

**The correction case is explicitly *not* ours.** If a user accepts a price proposal and it was wrong,
that is a valuation correction — Phase 2's `finance.correct_valuation()` RPC, already authored. This
phase does not add a second correction path.

**Alternative rejected.** A server-side price cache with a `finance.price_proposals` table. It adds a
table, an RLS policy, a grant, an erasure obligation and a background writer into the user's
financial data, to store something that is ephemeral by nature and identical on every device anyway.

---

## R4 — Opting a holding into auto-valuation: one nullable column, not a boolean plus a series

**Decision.** Add one column to `finance.holdings`:

```sql
auto_value_series text  -- null = not auto-valued; else a frozen enum: GOLD_24K, GOLD_22K, SILVER
```

Presence **is** the opt-in (FR-044's "holdings the user marks as auto-valued"). The value names which
price series the holding tracks.

**Rationale.** A `boolean is_auto_valued` plus a nullable `series` encodes one fact in two columns and
makes an impossible state representable — `is_auto_valued = true, series = null` means "auto-value
this against nothing", which the code would then have to defend against forever. One nullable column
has no invalid state to defend.

**Frozen enum, append-only from birth** (Article IX). The initial set is metals only. Currency is
deliberately excluded: the tracker is INR-only by ADR-0029 decision 4 (currency-less `value_paise`),
so a "currency price feed" has no holding to value — the existing converter's rate cache is a
different concern and stays where it is.

**Alternative rejected.** Deriving eligibility from `sector = 'GOLD'`. It conflates *what a holding
is* with *whether the user wants the app touching its value*, and gives no way to hold gold whose
value the user maintains by hand — which the spec's own assumption requires ("a holding the user
values themselves is never included").

---

## R5 — The move threshold is a **device-local setting**; learned rules are **Supabase**

Two preferences, two different homes, for one reason each.

| Preference | Home | Why |
|---|---|---|
| Price move threshold (default 5%, FR-044b) | Device-local settings store (004's control plane) | It governs a device-local evaluator (R3). Putting it in Postgres would mean a network read before a background job can decide whether to bother the user |
| Learned rules (FR-032–035) | `finance.automation_rules` (Supabase) | "Teach it once" is worth nothing if the lesson is stuck on one device. A rule is a categorisation preference, not message content |

**A rule stores a matcher token, never message text.** The matcher is the extracted merchant or
counterparty (`SWIGGY`), not the message it came from — so AUT-BR-002 holds and this table has
nothing to redact. `data-model.md` fixes the matcher's shape.

`automation_rules` is an ordinary mutable table (rules are disabled, removed, and their applied count
increments), so it takes SELECT/INSERT/UPDATE — **not** the append-only shape. Article IX's
append-only rule governs history tables; a rule is current state.

---

## R6 — "Never proposed twice" (FR-008c) is a per-origin stable key

Each origin gets a key that is stable across rescans, and the store rejects a second proposal
carrying a key it has already seen in any terminal state.

| Origin | Key | Note |
|---|---|---|
| Recurring | `(recurring_id, due_on)` | Phase 3 already enforces this as a unique constraint — inherited, not rebuilt |
| Bank message | `hash(sender ‖ body ‖ message_timestamp)` | A hash, not the text. Stable across rescans of the same inbox row; distinct for two genuinely identical messages seconds apart because the timestamp differs |
| Price | `(holding_id, recorded_value_paise)` | See below — the price case needs more than a key |

**The price case, and why FR-045 and FR-045a are not in conflict.** FR-045 says ignoring must not stop
a *later, genuinely different* move; FR-045a says the same move must not come back. Reconciled by
recording the price at which the user ignored, and requiring the next proposal to clear the threshold
**twice over**:

```
propose(holding) when:
    |fetched − recorded| ≥ threshold × recorded          // FR-044a: a real move from what's recorded
AND (last_ignored_price is null
     OR |fetched − last_ignored_price| ≥ threshold × recorded)   // FR-045a: not the move already refused
```

This is a **pure function over four numbers** and is the single most valuable RED test in the phase —
it encodes two requirements that read as contradictory and are not. `contracts/price-feed.md` gives
its table of cases.

**Accepting resets it.** Acceptance changes `recorded`, so both terms re-measure from the new value
and `last_ignored_price` is cleared.

---

## R7 — Freezing is **derived state**, never a stored status

FR-026a freezes a source's outstanding proposals when consent is withdrawn; FR-026c requires
re-granting to unfreeze them *exactly as they were, with no duplicates*.

**Decision.** `frozen` is computed at read time as `origin == BANK_MESSAGE && !consent.readSms`. It is
never written to a row.

**Rationale.** A stored status needs a migration on withdrawal, a reverse migration on re-grant, and a
recovery path for a process killed between the two — three ways to produce exactly the duplicates and
lost rows FR-026c forbids. Derived state makes re-granting free and correct by construction: the flag
flips, the same rows re-render as actionable, and nothing was ever rewritten.

**Consequence.** `pending / accepted / ignored` remain the only persisted states; the spec's Key
Entities list of "pending / frozen / accepted / ignored" describes what the **user sees**, and
`data-model.md` states the distinction so nobody adds a `FROZEN` constant to an append-only enum.

---

## R8 — Reading messages: `READ_SMS` + a periodic content-provider query, not a broadcast

**Decision.** Declare `READ_SMS`. On each scan, query the system SMS inbox for rows newer than the
last scan watermark, filtered to allowlisted senders. Do **not** declare `RECEIVE_SMS` and do not
register a broadcast receiver.

**Rationale.** Clarification 5 chose periodic scanning over per-message wake-ups, and the permission
should match: `RECEIVE_SMS` exists to wake an app per message, which is precisely the behaviour that
was rejected. A watermarked query is also inherently correct across the cases a broadcast is worst
at — device off, app force-stopped, permission granted late — because it reads what is there rather
than depending on having been listening (FR-027d).

**Scheduling.** A second `androidx.work` periodic request (~1h), separate from 006's daily alert
worker. `androidx.work` is added and AGP-9-verified by 006; if 006 has not landed, 7c adds and
verifies it, per that phase's own note. Separate workers because the cadences, the flags and the
failure modes differ — an hourly SMS scan misbehaving must be switchable off without stopping alerts.

**Backfill.** On first enable, the watermark is set to 30 days ago (spec assumption), then advances.

**Play policy.** `READ_SMS` is a Play restricted permission. Distribution is a signed APK outside the
store (ADR-0008), the flag ships off, and the spec records the store obligation as attaching when
store distribution is taken up. This is a **stated risk**, matching implementation plan risk R7.

---

## R9 — Parsing is a pure Kotlin function with a two-stage gate

```
sender allowlist  →  transaction shape  →  field extraction  →  Parsed | Unparseable(missing)
```

**Sender first, content second** (spec assumption: "bank senders are identified by sender identity,
not by message content"). Indian bank alerts arrive from alphanumeric sender IDs of the form
`XX-YYYYYY` (a two-character circle/operator prefix, a hyphen, then a six-character issuer code). The
allowlist matches the issuer code, so a promotional message containing `₹` from a non-bank sender is
excluded **structurally** rather than by trying to judge its content — which is what makes SC-004's
"zero non-bank messages produce a proposal" achievable rather than aspirational.

Extraction yields amount (paise, integer — Article VII, no float anywhere in the path), date,
account last-4, direction (debit/credit) and merchant. Any missing required field produces
`Unparseable` naming what is missing, which FR-010 renders as *"Could not tell what this was — pick a
category"*. **Unparseable is a first-class outcome, never a dropped message** — SC-003's remaining 10%
must be visible.

**All of this is testable without Android**: a table of representative messages in, a sealed result
out. This is where the phase's RED tests concentrate.

**Alternative rejected.** A remote or on-device-model parser. It contradicts FR-024 (a remote parser
sends message content off-device, ending the phase's central privacy claim), and Gemini Nano is a
progressive enhancement that ADR-0007 forbids assuming present.

---

## R10 — Duplicate detection: one batched RPC, not N queries and not client-side money math

**Decision.** `finance.find_possible_duplicates(candidates jsonb) returns table(...)` — takes the
whole visible queue's (amount, account, date) triples in one call and returns which ones match an
existing transaction. `security invoker`, so RLS scopes it to the caller.

**Matching rule** (spec assumption, now stated to the user per FR-031): **exact amount, same account,
within 3 days**. Exact amount because a bank message quotes the posted amount to the paise — a
tolerance would create false matches between genuinely different purchases far more often than it
would catch a real duplicate.

**Rationale.** One round trip for the screen; the comparison happens in SQL where the money is already
`bigint` paise, satisfying Article VII's "no client-side money arithmetic" the same way 005 and 006
did. Per-row queries would mean up to N network calls to render one list.

**Alternative rejected.** Detecting at accept time only. It removes the callout FR-029 requires on the
row, so the user learns about the duplicate after creating it.

---

## R11 — Erasure has two arms, because the data has two homes

FR-050 says *no store this feature introduces may survive an erasure request*, which is stricter than
006's alert log (delivery state, exempted because it never left the device). Proposals are not
delivery state — they carry amounts, merchants and message text.

| Store | Cleared by |
|---|---|
| `finance.automation_rules` | `public.delete_my_data()` — extended by this phase, FK-safe order |
| `holdings.auto_value_series` | Rides `finance.holdings`, already deleted |
| `automation_proposal` (Room) | The **in-app** "Delete my data" action, which must clear it in the same user action |

**This is a new obligation on an existing flow** and is called out rather than assumed: before this
phase, "Delete my data" was a single RPC call. It now has a device-local arm, and a test asserts both
run — the failure mode otherwise is an erasure that reports success while leaving parsed bank
messages on the device.

---

## R12 — Flags: `automation` is added here, and it is genuinely absent today

Implementation plan §5.5 reserves `automation`, but
`platform/feature-flags/dhruv-finance.json` **does not contain it** — verified 2026-08-23. The file
holds eleven keys, ending at `networth`. This phase adds it:

```json
"automation": { "enabled": false, "minVersion": "1.0.0", "requiresConsent": true }
```

`enabled: false` until this phase's checkpoint passes, per surface registry §1 and implementation plan
§5.5. One flag, not two: unlike 006's search-vs-alerts split, every surface here is one feature and
turning it off should stop all of it, including the scan worker.

**Consent mapping** is already defined by implementation plan §5.5's table and is not re-decided:
*Sync my financial records* gates the flag like every tracker flag, and *Read transaction SMS* is
required **additionally** before the message source may parse anything.

---

## R13 — Zero new `NavTarget` cases; the `REVIEW_INBOX` intent dispatches at the shell

**Verified against the code, not assumed** (2026-08-23): `NavTarget` currently has exactly two cases,
`SelectTab` and `OpenPlanTool`. Every case Phases 2–6 plan to add is still unshipped.

**Decision.** G1, G2, G3 and the Ignored list are **shell detail routes**, reached from Settings ›
Modules › Automation and from the alert's intent — not drill-ins under any tab. `NavTarget`'s own
doc comment already draws this line: Settings, Ask and the converters "belong to no tab — they're
shell-level detail routes with their own back-top-bar, not part of any tab's nested back stack, so
they don't need cross-tab dispatch." G1–G3 are the same shape.

The `REVIEW_INBOX` intent extra (surface registry §3, already registered) is handled in the shell's
intent dispatcher and passes through 004's app-lock hold-and-dispatch, so FR-037's "honouring the app
lock rather than bypassing it" is inherited rather than re-implemented.

**Registry rows still change**: §1's Automation row gains the queue, the Ignored list and the flag
key; §3's `REVIEW_INBOX` row gains this phase as a producer alongside the recurring notification.

---

## R14 — Components: two owed by an earlier batch, one extension, zero new parallel components

Checked against `platform/DESIGN-SYSTEM.md` §5.1/§5.2/§5.3 and by symbol search on
`libs/core/src/main` (2026-08-23).

| Need | Component | Status |
|---|---|---|
| The dashed not-yet-accepted row (FR-004) | **`SuggestedRow`** | §5.2 **batch B4**, owed by Phase 3's ledger. Inherited if Phase 3 landed it; built once in `:libs:core` by 7a if not |
| Day grouping in the queue | **`DayGroupHeader`** | Same batch, same rule — and 006 makes the same call, so whichever phase lands first owns it |
| Frozen-source banner (FR-026a) | **`InfoBanner`** | §5.2 **batch B7** — the design draws SNACKBAR · INFO · OFFLINE as three; only offline and snackbar exist |
| Duplicate callout (FR-029) | **`StatusBadge`** | §5.2 batch B7 — §5.3 records `CountBadge` as counts-only. Closed by **extending `CountBadge`**, never a second badge component |
| Threshold setting (FR-044b) | `SliderWithPresets` | **Built** — verified |
| Everything else | `NxCard`, `ListGroup`, `ListGroupRow`, `SwitchRow`, `SectionLabel`, `NxButton`, `NxTopBar`, `MoneyText`, `EmptyStateCard`, `SignedOutCard`, `OfflineStateCard`, `NotConfiguredCard`, `RetryErrorCard`, `SkeletonBlock`, `UndoSnackbarHost`, `ConfirmDangerDialog`, `DhruvModalSheet`, `Chip` | **Built** — verified by symbol search |

Article VI is satisfied by extension in every gap. No component in the built list above was assumed
from a design file — the failure ADR-0030 diagnosed.

---

## R15 — Accept-all is a pure partition, decided before anything is written

Clarification 6 fixed the rule; the shape follows from it. `partitionForAcceptAll(rows)` is a **pure
function** returning the accepted set plus the three skip buckets (missing field · possible duplicate
· value update). The screen renders its result; the repository writes only the accepted set.

**Why pure and why first.** FR-009's original wording would have let one tap double-count a payment
and revalue holdings. Making the partition a function with its own test table means SC-006a ("zero
duplicate-flagged transactions recorded by accept-all") is proven by a unit test over every queue
composition, not by clicking through one.

---

## R16 — What this phase does **not** touch

Recorded because each was considered and each has an owner elsewhere.

| Not here | Owner |
|---|---|
| The proposal store's creation | Phase 3 (002) |
| Trash / recently deleted | Phase 0b Settings (004), per readiness §5.2 — this phase only fixes registry §4's stale line |
| CSV import | Deferred, spec clarification 2; the A4 CTA stays present and disabled |
| A live account-aggregator connection | Nobody — G3 ships the consent statement, the source row is marked unavailable |
| Valuation correction | Phase 2's `finance.correct_valuation()` |
| Alert delivery, channels, the notification centre | Phase 6 — this phase adds one alert **type**, not a mechanism |
| Editing recurring definitions | Phase 3 (D9) |