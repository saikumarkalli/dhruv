# Data model: Automation (Phase 7)

Phase 1 output. Four parts: what this phase **adds** (one Room table, one Postgres table, one column,
three enum sets), what it **reads** from earlier phases, what it **deliberately leaves alone**, and
the state transitions.

Storage rationale lives in [research.md](./research.md) R1–R6 and R11; this file fixes the shapes.

---

## 0. Where each thing lives, and why

| Data | Store | Synced? | Reason |
|---|---|---|---|
| Recurring proposals | `finance.suggestions` (Supabase) | yes | Phase 3 owns it; unchanged by this phase |
| Bank-message proposals | `automation_proposal` (Room) | **no** | Raw message text must never leave the device (R1) |
| Price proposals | `automation_proposal` (Room) | **no** | Ephemeral, derived from public data; keeps background jobs out of tracker writes (R3) |
| Learned rules | `finance.automation_rules` (Supabase) | yes | "Teach once" is worthless if stuck on one device (R5) |
| Auto-value opt-in | `finance.holdings.auto_value_series` | yes | It is a property of the holding |
| Move threshold, scan watermark | Device settings (004) | no | They govern device-local evaluators (R5, R8) |

---

## 1. `automation_proposal` — device-local, Room, new (v6 → v7)

Holds every proposal this phase produces. **Never synced, never exported, never in a PDF or CSV.**

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | INTEGER PK autoincrement | no | Room surrogate |
| `origin` | TEXT | no | `BANK_MESSAGE` \| `PRICE_FEED` — see §4. (`RECURRING` never appears here; those rows live in Supabase) |
| `kind` | TEXT | no | `TRANSACTION` \| `VALUE_UPDATE` — FR-003a's two kinds |
| `source_key` | TEXT | no | **UNIQUE.** The never-propose-twice key (R6). Survives the row reaching a terminal state |
| `status` | TEXT | no | `PENDING` \| `ACCEPTED` \| `IGNORED`. **No `FROZEN`** — that is derived (§5) |
| `created_at` | INTEGER (epoch ms) | no | Device clock; drives day grouping and ordering |
| `decided_at` | INTEGER | yes | Set when status leaves `PENDING` |
| `raw_text` | TEXT | yes | The original message, `BANK_MESSAGE` only. **The reason this table is local.** Rendered on the row (FR-005); never transmitted |
| `parse_confidence` | TEXT | no | `PARSED` \| `UNPARSEABLE` — FR-010 |
| `missing_fields` | TEXT | yes | Comma-separated field names when `UNPARSEABLE`; what the row asks the user to supply |
| **Transaction fields** (`kind = TRANSACTION`) | | | |
| `amount_paise` | INTEGER | yes | `Long` paise. Never a `Double` anywhere in the path (Article VII) |
| `direction` | TEXT | yes | `DEBIT` \| `CREDIT` |
| `occurred_on` | INTEGER (epoch day) | yes | Date only — a bank message states a date, not an instant |
| `account_id` | TEXT | yes | Resolved `finance.accounts` id; null when the last-4 matched nothing (FR-023 → row asks) |
| `account_hint` | TEXT | yes | The last-4 the message quoted, kept so an unmatched row can still say which card |
| `merchant` | TEXT | yes | Extracted counterparty. Also the rule matcher's subject (§3) |
| `category_id` | TEXT | yes | Proposed category — from a rule if one matched, else null |
| **Value-update fields** (`kind = VALUE_UPDATE`) | | | |
| `holding_id` | TEXT | yes | The holding being revalued |
| `recorded_value_paise` | INTEGER | yes | What is on file now — shown as the "from" (FR-005) |
| `proposed_value_paise` | INTEGER | yes | What the fetched price implies |
| `price_taken_at` | INTEGER (epoch ms) | yes | The moment the price was read. Shown on the row, so a stale proposal is visibly stale (edge case: price moved back before the user acted) |
| `last_ignored_price_paise` | INTEGER | yes | Set on ignore; the second term of the move predicate (R6) |

**Indexes**: `status`, `created_at`, and the unique index on `source_key`.

**Retention**: `ACCEPTED` rows are purged 30 days after `decided_at` — they have served their purpose
and the real record lives in `transactions`/`valuations`. `IGNORED` rows are **kept**: they are the
Ignored list (FR-008a) and their `source_key` is what makes FR-008c true. A purged `ACCEPTED` row's
key is retained in a compact `automation_seen_key` table so purging never resurrects a proposal.

> **Why a separate key table rather than never purging.** Keeping accepted rows forever would grow a
> local table containing bank message text indefinitely — a privacy cost for no user benefit, since
> the accepted record already exists in the ledger. Retaining only the hash keeps FR-008c true at a
> few bytes per message.

### `automation_seen_key` — Room, new

| Column | Type | Notes |
|---|---|---|
| `source_key` | TEXT PK | The key of a proposal that reached a terminal state and was later purged |
| `seen_at` | INTEGER | For diagnostics only |

Never purged. Contains no message content — a hash and a timestamp.

---

## 2. `finance.automation_rules` — Supabase, new

A learned rule (BR-G3, FR-032–035). **Deliberately mutable, not append-only** — see the plan's
Article IX note.

| Column | Type | Notes |
|---|---|---|
| `id` | uuid PK | |
| `user_id` | uuid not null → `auth.users` | RLS subject |
| `match_kind` | text not null | Frozen enum, §4. `MERCHANT_EXACT` at birth |
| `match_value` | text not null | The extracted merchant token — **never message text** (R5) |
| `category_id` | uuid not null → `finance.categories` | What the rule proposes |
| `applied_count` | integer not null default 0 | FR-034; incremented at propose time |
| `active` | boolean not null default true | FR-035's disable |
| `created_at` | timestamptz not null default now() | |
| `deleted_at` | timestamptz | FR-035's remove — soft, so it reaches Phase 0b's Trash like every other soft delete |

**Unique** `(user_id, match_kind, match_value)` where `deleted_at is null` — one active rule per
matcher, so "teach it again" updates rather than stacking duplicates.

**RLS**: select / insert / update, all `user_id = auth.uid()`. **No delete policy** — rows leave only
through `public.delete_my_data()`, consistent with every other tracker table (ADR-0029 decision 5).

**Grant**: `grant select, insert, update on finance.automation_rules to authenticated;` — custom
schemas have no implicit exposure (ADR-0033 decision 4), and `supabase db diff` emits neither the
grant nor RLS, so both are hand-appended and verified by reading the generated migration.

**Removing a rule leaves history alone** (FR-035): accepted transactions are ordinary
`finance.transactions` rows with no reference back to the rule that suggested their category.

---

## 3. `finance.holdings.auto_value_series` — new column

```sql
auto_value_series text check (auto_value_series is null or auto_value_series in
    ('GOLD_24K', 'GOLD_22K', 'SILVER'))
```

Null = the user maintains this value themselves and the price feed never touches it. Non-null = opted
in, naming the series tracked. One column, not a boolean plus a series — R4.

Added by `ALTER TABLE … ADD COLUMN`, which the CI equivalence guard understands as of Phase 2's
`gen_schema_docs.py` work (001 T079).

---

## 4. Enum sets — TEXT-persisted, append-only from birth (Article IX)

Never rename a shipped constant. Adding a value is fine; changing one silently reinterprets stored
rows.

| Set | Values at birth | Extends when |
|---|---|---|
| `ProposalOrigin` | `RECURRING`, `BANK_MESSAGE`, `PRICE_FEED` | Account aggregator goes live — `ACCOUNT_AGGREGATOR` is the reserved name, deliberately **not** added now (nothing produces it) |
| `ProposalKind` | `TRANSACTION`, `VALUE_UPDATE` | A source proposes something that is neither |
| `AutoValueSeries` | `GOLD_24K`, `GOLD_22K`, `SILVER` | Another metal. Currency is excluded by design — the tracker is INR-only (R4) |
| `RuleMatchKind` | `MERCHANT_EXACT` | A second matching strategy (contains, sender-based) is wanted. One value at birth is honest: the spec only requires teaching from a corrected merchant |

`ProposalOrigin` includes `RECURRING` even though no Room row ever carries it — the merged model
(R2) needs the value to describe a Supabase-sourced row.

---

## 5. State transitions

```
                    ┌──────────── restore (FR-008b) ─────────────┐
                    ▼                                            │
   (source runs) ──▶ PENDING ──── accept ────▶ ACCEPTED          │
                       │                        │                │
                       └──── ignore ────▶ IGNORED ───────────────┘
                                             │
                              (kept — this is the Ignored list)
```

- **PENDING → ACCEPTED**: writes a `finance.transactions` row (`TRANSACTION`) or a
  `finance.valuations` row (`VALUE_UPDATE`). The transaction's "from an automated source" history
  entry (FR-002) is written by Phase 3's existing audit trigger, not by new code.
- **PENDING → IGNORED**: writes nothing to tracker data. For a `VALUE_UPDATE`, sets
  `last_ignored_price_paise` (R6).
- **IGNORED → PENDING** (restore, FR-008b): the only backward edge. Clears `decided_at`.
- **ACCEPTED is terminal.** Undoing an acceptance means editing or deleting the created record on its
  own screen — there is no un-accept, because the record now belongs to the ledger.
- **No `FROZEN` state exists.** Frozen is `origin == BANK_MESSAGE && !consent.readSms`, computed at
  read time (R7). A `PENDING` row simply renders as non-actionable while that holds. **Do not add a
  `FROZEN` constant** — it would need a migration on withdrawal and a reverse one on re-grant, which
  is the exact mechanism FR-026c's "no duplicates" forbids.

### Never-propose-twice, precisely (FR-008c)

Before inserting, a source checks its `source_key` against both `automation_proposal` and
`automation_seen_key`. A hit means skip — regardless of whether the earlier row was accepted or
ignored, and regardless of a rescan, a source re-enable, or the FR-027d backlog.

**The one exception, and it is not an exception to this rule**: a price proposal's key is
`(holding_id, recorded_value_paise)`, so accepting one changes the recorded value and therefore
produces a *different* key next time. FR-045's "a later genuinely different move still proposes"
falls out of the key's shape, plus the predicate's second term. See
[contracts/price-feed.md](./contracts/price-feed.md).

---

## 6. What this phase reads and does not change

| Reads | From | Used for |
|---|---|---|
| `finance.suggestions` (all columns except `raw_text`) | Phase 3 | the queue's recurring origin |
| `finance.transactions`, `accounts`, `categories` | Phase 3 | accepting; duplicate matching; account resolution from a last-4 |
| `finance.holdings`, `v_latest_valuation` | Phase 2 | the price feed's recorded value |
| `finance.valuations` | Phase 2 | **insert only** — append-only holds |
| A3 consent flags, app lock, hide-amounts | Phases 1, 004 | gates, dispatch, masking |

**`finance.suggestions.raw_text` stays permanently unused.** Phase 3 reserved it for this phase's SMS
source; R1 puts that data in Room instead. The column is not dropped — Phase 3 owns the table, and an
unused nullable column costs nothing — but it must never be written to. A comment on that file
records why, so a future engineer does not "finish" the wiring and break AUT-BR-002.

---

## 7. Erasure (FR-050) — two arms

| Store | Cleared by |
|---|---|
| `finance.automation_rules` | `public.delete_my_data()`, extended by 7g in FK-safe order |
| `finance.holdings.auto_value_series` | Rides `finance.holdings`, already deleted |
| `automation_proposal`, `automation_seen_key` | The **in-app** "Delete my data" action's device-local arm |

Before this phase, "Delete my data" was one RPC call. It now has a second arm, and a test asserts both
run. Unlike Phase 6's alert log — delivery state, explicitly exempted because it never left the
device — these rows carry amounts, merchants and message text, so FR-050 covers them without
exception (R11).