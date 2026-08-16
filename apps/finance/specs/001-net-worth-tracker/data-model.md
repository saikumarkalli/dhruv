# Phase 1 Data Model: Net Worth Tracker (Phase 2)

Maps spec.md's Key Entities to concrete storage. `holdings`/`valuations` already exist (Phase 1,
`supabase/migrations/0001_init.sql`) and are **not modified** by this feature — only extended with
one new table and two views, migration `0002_networth_phase2.sql`.

## Existing entities (Phase 1, referenced not changed)

### Holding → `public.holdings`
| Column | Type | Notes |
|---|---|---|
| `id` | uuid, PK | |
| `user_id` | uuid, FK → `auth.users` | RLS: `user_id = auth.uid()` |
| `name` | text | |
| `kind` | text, `CHECK IN ('ASSET','LIABILITY')` | maps spec's "ownership direction" |
| `sector` | text | free TEXT at the DB layer; **enum-validated at the Kotlin repository boundary** per FR-001/FR-012 — not a DB CHECK constraint, so adding a category never needs a migration (BR-C3, NW-BR-004/005) |
| `notes` | text, nullable | |
| `created_at` | timestamptz | |
| `deleted_at` | timestamptz, nullable | soft-delete slot; not written by this phase |

RLS: SELECT/INSERT/UPDATE own rows (no client DELETE — erasure only via `delete_my_data()`).

### Recorded value → `public.valuations`
| Column | Type | Notes |
|---|---|---|
| `id` | uuid, PK | |
| `holding_id` | uuid, FK → `holdings` | ownership transitive through parent (no `user_id` column) |
| `value_paise` | bigint | **FR-005/constitution Article VII** — paise, never numeric/float |
| `as_of` | date | |
| `source` | text | e.g. `MANUAL`, `STATEMENT` |
| `created_at` | timestamptz | |
| `deleted_at` | timestamptz, nullable | used by FR-004's "hide, never alter" correction path |

RLS: SELECT/INSERT only — **no UPDATE policy exists at all**. This is what makes FR-004
("preserve every previously recorded value... never altering or removing the original record")
true at the database layer, not just by client discipline.

**Validation rules** (from FR-002): a new holding and its first valuation are written in one
transaction — the repository layer either writes both or neither (atomicity requirement, not
expressible as a single-table constraint since they're two inserts).

## New entities (this phase)

### Liability detail → `public.liabilities_meta`
1:1 extension of a `holdings` row where `kind = 'LIABILITY'`.

| Column | Type | Notes |
|---|---|---|
| `holding_id` | uuid, PK, FK → `holdings` | one row per liability holding |
| `liability_type` | text | `HOME_LOAN \| CAR_LOAN \| CREDIT_CARD \| BNPL` — same append-only-TEXT convention as `sector` |
| `rate_bps` | integer | basis points, not a float percentage |
| `emi_paise` | bigint | **constitution Article VII** — paise |
| `debit_day` | integer | day-of-month, feeds Home's UPCOMING list (FR-010) |
| `tenure_months` | integer | |
| `paid_months` | integer | drives payoff-progress display (C6 "84 of 180 paid") |
| `linked_account_id` | uuid, nullable | forward-compatible; no `accounts` table exists until Phase 3, so this stays null this phase |
| `collateral_holding_id` | uuid, nullable, FK → `holdings` | e.g. a home loan's collateral property |

RLS: ownership transitive through `holding_id → holdings.user_id`, same pattern as `valuations`.
SELECT/INSERT/UPDATE own rows (unlike `valuations`, this is mutable state — EMI/tenure legitimately
change, e.g. after a part-payment — not an append-only history).

**Validation rule** (FR-008/FR-009): `liability_type` is enum-validated at the repository boundary,
same as `sector`.

### Net worth (derived, not stored)
Not a table. Computed by `v_net_worth_by_sector` — spec.md's FR-005 definition ("sum of latest
asset values minus sum of latest liability outstandings") is exactly this view's output, never
computed by summing raw rows client-side (R4, NFR-8).

## Views (server-side aggregation, this phase)

### `v_latest_valuation`
One row per holding: its most recent non-deleted `valuations` row (`DISTINCT ON (holding_id) ...
ORDER BY holding_id, as_of DESC, created_at DESC`). Feeds C1's totals, C2's per-holding current
value, and `v_net_worth_by_sector` below.

### `v_net_worth_by_sector`
Aggregates `v_latest_valuation` joined to `holdings`, grouped by `kind` + `sector`: sector name,
count of holdings, summed `value_paise`, and each sector's share of its `kind`'s total. Backs C1's
ranked legend and the net/asset/liability subtotals (FR-005, FR-007).

## State transitions

- **Holding**: created → (exists indefinitely; no state machine — a holding either exists with ≥1
  valuation, or doesn't exist at all, per FR-002's atomicity rule).
- **Valuation**: created → (immutable). A "correction" (FR-004) is: mark the wrong row
  `deleted_at = now()` (excluded from `v_latest_valuation`'s view logic) + insert a new row. Never
  an UPDATE — the RLS policy set makes this the only possible path, not just the intended one.
- **Liability meta**: created alongside its holding → updatable in place (EMI/tenure/paid_months
  change over the life of the loan) — the one entity in this phase that is *not* append-only, since
  it represents current loan terms, not a value history.
