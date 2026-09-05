# Phase 1 Data Model: Net Worth Tracker (Phase 2)

Maps spec.md's Key Entities to concrete storage.

> **Updated 2026-08-23 to match the authored schema.** The 2026-08-22 audit found two requirements
> in this phase that were impossible against the committed schema, and the
> [readiness architecture decisions](../../docs/superpowers/specs/2026-08-23-phase-readiness-architecture-decisions.md)
> resolve them. `holdings` and `valuations` **are** modified by this feature now (they were
> previously described as untouched), and the phase adds one table, **three** views and **two**
> functions. Migration is `supabase/migrations/20260823094500_networth_phase2.sql`, not the
> `0002_networth_phase2.sql` earlier drafts named.
>
> Declarative source of truth is `supabase/schemas/finance/` (ADR-0032 decision 4); the tables,
> views and functions below are authored there. **Open DB gaps and maintenance obligations are in
> §"DB readiness" at the foot of this file** — read it before running the migration.

## Modified entities (Phase 1 tables, extended by this phase)

### Holding → `finance.holdings`
| Column | Type | Notes |
|---|---|---|
| `id` | uuid, PK | |
| `user_id` | uuid, FK → `auth.users` | RLS: `user_id = auth.uid()` |
| `name` | text, `CHECK length(btrim(name)) BETWEEN 1 AND 120` | |
| `kind` | text, `CHECK IN ('ASSET','LIABILITY')` | maps spec's "ownership direction" |
| `sector` | text, **`CHECK IN` the 10 frozen values** | `BANK · MUTUAL_FUND · STOCKS · PROPERTY · GOLD · EPF_PPF · CASH · VEHICLE · CRYPTO · OTHER`. **Changed 2026-08-23**: previously free TEXT "enum-validated at the Kotlin repository boundary… so adding a category never needs a migration". The audit found the value set existed only in the functional spec's prose while T011 tested rejection against it. BR-C3 makes these append-only *forever*, so a migration to add one is the correct cost, and the repository still validates too (fail fast, better message) |
| `invested_paise` | bigint, nullable, `CHECK >= 0` | **New.** Cost basis for C3's `INVESTED`/`GAIN`. Nullable because a holding whose cost the user does not know is normal (inherited gold, an old EPF balance) — C3 omits the stat rather than showing a wrong zero. Funds a **simple** return, not XIRR (see §"Simple return, not XIRR") |
| `request_id` | uuid, nullable, **UNIQUE** | **New.** Client-generated when the user commits, so a retry after a timeout collides here instead of creating a second holding |
| `notes` | text, nullable | |
| `created_at` | timestamptz | |
| `deleted_at` | timestamptz, nullable | soft-delete slot. Writable — `holdings` has an UPDATE policy — so holding soft-delete is mechanically possible; FR coverage for edit/delete is tracked as gap-remediation work, not a schema gap |

RLS: SELECT/INSERT/UPDATE own rows (no client DELETE — erasure only via `delete_my_data()`).

### Recorded value → `finance.valuations`
| Column | Type | Notes |
|---|---|---|
| `id` | uuid, PK | |
| `holding_id` | uuid, FK → `holdings` | ownership transitive through parent (no `user_id` column) |
| `value_paise` | bigint, `CHECK >= 0` | **FR-005/constitution Article VII** — paise, never numeric/float |
| `as_of` | date, **`CHECK as_of <= current_date`** | **New guard.** `v_latest_valuation` orders `as_of DESC`, so a mistyped 2030 date would become permanently "latest" and — before `correct_valuation()` existed — could never be superseded. `current_date` is STABLE not IMMUTABLE, which Postgres permits in a CHECK; safe here **only** because the predicate is monotonic (a row valid on insert stays valid on restore). Do not copy the pattern to a lower-bound check |
| `source` | text, **`CHECK IN` 4 frozen values** | `MANUAL · STATEMENT · IMPORT · CORRECTION`. Previously documented as "e.g. `MANUAL`, `STATEMENT`" — never frozen, while Phase 5's `has_self_valued` needs the exact partition. **That partition is now defined: `MANUAL` and `CORRECTION` are self-valued; `STATEMENT` and `IMPORT` are not.** `CORRECTION` is written by `correct_valuation()` so C3's history can label a corrected entry |
| `request_id` | uuid, nullable, **UNIQUE** | **New.** Retry idempotency, same role as on `holdings` |
| `created_at` | timestamptz | |
| `deleted_at` | timestamptz, nullable | Written by **exactly one thing**: `finance.correct_valuation()` |

RLS: SELECT/INSERT only — **no UPDATE policy and no UPDATE grant exists**. This is what makes FR-004
("preserve every previously recorded value... never altering or removing the original record")
true at the database layer, not just by client discipline.

> **Correction (2026-08-23).** This file previously described FR-004's correction as "mark the wrong
> row `deleted_at = now()`" and cited the missing UPDATE policy as the guarantee that made it work.
> Setting `deleted_at` **is** an UPDATE, so the correction was impossible: the RLS cited as the
> guarantee was what forbade it. Resolved by `finance.correct_valuation()` (below), which is the RPC
> ADR-0029 decision 4 already named and assigned to this phase's SA step.
> **Do not "fix" this by adding an UPDATE policy** — that makes the table ordinarily mutable and
> destroys BR-C1's database-level append-only guarantee, which is the entire reason it has none.

**Validation rules** (from FR-002): a new holding and its first valuation are written in one
transaction. This is **not** achievable from the client — it is two PostgREST inserts over HTTP, and
a failed second insert leaves an orphan holding that no client DELETE policy can clean up. It is
therefore done server-side by `finance.create_holding_with_value()` (below).

## New entities (this phase)

### Liability detail → `finance.liabilities_meta`
1:1 extension of a `holdings` row where `kind = 'LIABILITY'`.

> **Schema corrected 2026-08-23: `finance`, not `public`.** This file previously declared
> `public.liabilities_meta` while `holdings`/`valuations` in the same file were `finance.*`. Under
> ADR-0033 a `public` table is unreachable — every tracker call sends `Accept-Profile: finance` and
> would 404 against `public`, silently rather than loudly. 002 and 003 each flagged this as an
> unresolved carry-over and 005 silently assumed `finance.`; all four now agree.

| Column | Type | Notes |
|---|---|---|
| `holding_id` | uuid, PK, FK → `holdings` | one row per liability holding |
| `liability_type` | text, `CHECK IN` 4 frozen values | `HOME_LOAN · CAR_LOAN · CREDIT_CARD · BNPL` — append-only (BR-C3) |
| `rate_bps` | integer, `CHECK 0–10000` | basis points, not a float percentage |
| `emi_paise` | bigint, nullable, `CHECK >= 0` | **constitution Article VII** — paise |
| `debit_day` | smallint, nullable, `CHECK 1–31` | day-of-month, feeds Home's UPCOMING list (FR-010) |
| `tenure_months` | integer, nullable, `CHECK > 0` | |
| `paid_months` | integer, default 0, `CHECK >= 0` and `<= tenure_months` | drives payoff-progress display (C6 "84 of 180 paid") |
| `original_principal_paise` | bigint, nullable, `CHECK >= 0` | **New.** Required to derive C7's amortisation split (principal paid / interest paid / left) — the audit found T028 asserting that split "sums to total obligation" against a computation with no stored inputs. Nullable for a card or BNPL line, which has no sanctioned principal |
| `collateral` | text, nullable | **Deviation to confirm**: earlier drafts had `collateral_holding_id uuid FK → holdings`. Authored as free text because the design's C7 shows collateral as a descriptive line ("collateral"), and modelling it as a holding FK asserts the collateral is itself a tracked holding — often false (a hypothecated vehicle, a pledged deposit outside the tracker). Reversible while no rows exist |
| `linked_account_id` | uuid, nullable | forward-compatible; **no FK constraint this phase** — `finance.accounts` does not exist until Phase 3, which adds the constraint in its own migration so Phase 2 never depends on a table it cannot create |
| `request_id` | uuid, nullable, UNIQUE | retry idempotency |
| `created_at` / `updated_at` / `deleted_at` | timestamptz | mutable state, unlike `valuations` |

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

> **Every view carries `with (security_invoker = on)`, and this is not optional.** A Postgres 15+
> view executes as its **owner** by default, bypassing RLS on the underlying tables — and PostgREST
> exposes these views. Without the clause each of them returns **every user's rows to every
> signed-in caller**. The audit found all 8 planned views across Phases 2–4 missing it.
> `supabase db diff` cannot express security-invoker views (ADR-0032 decision 4's documented caveat
> list), so the clause is hand-verified in the generated migration and each phase's RLS test asserts
> a second user reads zero rows from every view it adds.

### `v_latest_valuation`
One row per holding: its most recent non-deleted `valuations` row (`DISTINCT ON (holding_id) ...
ORDER BY holding_id, as_of DESC, created_at DESC`). Feeds C1's totals, C2's per-holding current
value, and `v_net_worth_by_sector` below.

### `v_net_worth_by_sector`
Aggregates `v_latest_valuation` joined to `holdings`, grouped by `kind` + `sector`: sector name,
count of holdings, summed `value_paise`, and each sector's share of its `kind`'s total. Backs C1's
ranked legend and the net/asset/liability subtotals (FR-005, FR-007).

### `v_net_worth_history` *(new — resolves FR-010's undefined trend)*
Trailing **24 month-ends**, clamped so the newest point is today rather than a future month-end.
Columns: `user_id`, `as_of`, `assets_paise`, `liabilities_paise`, `net_paise`.

Added because FR-010 mandates a `▲/▼ %` delta and an area sparkline on Home, and C1 a delta in its
donut centre, while this phase previously defined only current-state views — the delta had no source
and "delta vs *when*" was undefined. Home's delta compares the latest point against the same holding
set **30 days prior**; C2's per-holding sparkline reads that holding's own valuation rows directly.

**T046 closure note (2026-09-02):** the source above (`v_net_worth_history`, feeding Home's delta
and sparkline) shipped as designed. C2's own per-holding sparkline/last-updated-date/%-change,
described in the line above, did **not** ship — `AssetsScreen`'s row only ever showed name/sector/
current value. This is a deliberate scope deferral, not an oversight rediscovered here: building it
means either an N+1 per-row valuation-history fetch or a new aggregation view, and no `NW-UI-*` row
requires it (C2's own rows in the catalog only ever tested the sector filter/list, never a
sparkline). Tracked as a follow-up for whichever phase next touches C2.

Derivation is **"latest valuation ≤ date"** — deliberately the same rule Phase 5's
`report_balance_sheet(p_as_of)` uses, so this is not a competing mechanism and 005 may read this view
rather than re-derive. Cost is O(months × holdings) index lookups served by
`valuations_holding_id_as_of_idx`, which is fine at personal-finance scale.

**The 30-day window is a product choice, not a fact** — chosen because the design's copy says "this
month". Calendar-month-to-date changes only the view's window.

## Functions (this phase)

### `finance.correct_valuation(p_valuation_id, p_value_paise, p_as_of, p_note)` → `uuid`
`security definer`. The **only** path by which a valuation row is ever amended, and the resolution
of FR-004's impossibility (above). Asserts the caller owns the parent holding, soft-deletes the
target row and inserts the corrected one with `source = 'CORRECTION'` — in one transaction, so a
holding can never be left with both rows live or neither. Because `security definer` bypasses RLS,
the ownership check is explicit and is the same statement that resolves the holding, leaving no
window between the two.

### `finance.create_holding_with_value(...)` → `uuid`
`security definer`. Creation only — an ordinary later valuation append stays a plain PostgREST
insert against the existing INSERT policy. Delivers FR-002's atomicity server-side. Replays
idempotently: a retry carrying the same `p_request_id` returns the already-created holding instead of
duplicating it.

## Field validation rules (Phase 10, T064) and post-write invalidation (T075)

**T064 — future-dated valuations and C4/C5 field rules.** Verified already fully guarded, both at
the DB layer and the client, before this note was added:
- `finance.valuations.as_of` carries `CHECK (as_of <= current_date)` (line above) — a future date is
  rejected by Postgres regardless of what the client sends.
- `AddValuationViewModel` never offers a date picker at all — every `save()` path uses
  `LocalDate.now().toString()` for `asOf`, so a future date cannot be entered from C5 in the first
  place. `finance.correct_valuation()` independently re-asserts `p_as_of > current_date` as a guard
  for its own call site.
- Every other field rule C4/C5 leave unstated in prose is enforced by a named `CHECK` above:
  `holdings.name` 1–120 chars, `sector`/`kind` frozen enums, `invested_paise` nullable/`>= 0`,
  `valuations.value_paise` `>= 0`, `source` frozen enum, `liabilities_meta.rate_bps` 0–10000,
  `emi_paise` nullable/`>= 0`, `debit_day` 1–31, `tenure_months > 0`, `paid_months >= 0` and
  `<= tenure_months`. There is no field in C4/C5 with a rule that exists only in prose and not as a
  `CHECK` — the table above **is** the field-rule specification T064 asked this phase to write down.

**T075 — post-write invalidation model.** SC-001 promises the net-worth total updates "without a
manual refresh," and `platform/DESIGN-SYSTEM.md` §8 forbids per-feature pull-to-refresh. The model
this phase actually ships (Phase 8) is **navigation-triggered reload, not polling or a server push**:
every C1/C2/C3/C6/C7 route in `NetWorthNavHost.kt` wraps its content in
`LifecycleResumeEffect(key) { viewModel.load(...); onPauseOrDispose { } }`, so returning to a screen
(via `popBackStack()` from C4/C5's write, or backgrounding and resuming the app) re-fetches from the
server-side views synchronously before render. Between write-ack and the caller screen resuming,
C4/C5 themselves show their own submit-in-flight state (`NxButton`'s `loading` param disables the
button and shows a spinner); there is no intermediate "stale total" frame because the previous
screen simply hasn't re-rendered yet — it is still showing its last successfully loaded state,
which was correct at the time it loaded. There is no cross-device realtime sync in this phase
(ADR-0014: Supabase + RLS is the single source of truth, no client-side conflict resolution) — a
second device only sees the update on its own next navigation-triggered reload.

## Concurrency and write-retry semantics (Phase 10, T077)

**Two-device conflict**: out of scope by ADR-0014 design, not an oversight of this phase — the
tracker domain has no client-side conflict resolution at all; Supabase + RLS is the single source of
truth and the last write physically committed wins. Two devices editing the same holding's mutable
fields (name, sector, invested amount, or `liabilities_meta`'s EMI/tenure) is the same last-write-
wins outcome any single-row UPDATE has; `valuations` cannot conflict this way since it is
insert-only.

**Write-retry / duplicate-submission**: **already solved**, not an open gap — `p_request_id` on both
`finance.create_holding_with_value()` and (via the `holdings.request_id`/`valuations.request_id`
UNIQUE columns) is generated client-side once, at the moment `AddEditHoldingViewModel.save()`
commits (`UUID.randomUUID()`), and reused by the RPC's idempotent-replay check
(`create_holding_with_value.sql`'s comment: "generated client-side at the moment the user commits
… so an automatic retry after a timeout reuses it, collides on the UNIQUE column, and returns the
already-created holding instead of duplicating it"). This covers the case that actually matters — an
HTTP-level automatic retry of the *same* logical request. A user manually re-tapping "Save" after
perceiving no response generates a **new** UUID and is treated as a new, independent save — which is
correct: from the domain's perspective that is a second, deliberate user action, not a duplicate of
the first.

## Simple return, not XIRR

`invested_paise` funds `(current − invested) / invested`, which C3 must label **"Simple return"** —
**not** XIRR. The design's own text says XIRR and the two are not comparable. XIRR needs a dated
cashflow series; a single scalar cost basis cannot provide one, and 005's research R8 records that
no holding↔transaction link exists to build one from. XIRR stays deferred to its own ADR, and this
column narrows that ADR's scope rather than closing it.

## State transitions

- **Holding**: created → (exists indefinitely; no state machine — a holding either exists with ≥1
  valuation, or doesn't exist at all, per FR-002's atomicity rule).
- **Valuation**: created → (immutable to every client path). A "correction" (FR-004) is a single
  call to `finance.correct_valuation()`, which soft-deletes the wrong row (excluding it from
  `v_latest_valuation`) and appends the corrected one as `source = 'CORRECTION'`. The client never
  issues an UPDATE, and cannot: there is no UPDATE policy and no UPDATE grant. The RLS policy set
  makes the RPC the only possible path — which is the property the earlier draft claimed while
  describing a client-side `deleted_at` write that the same policy set forbade.
- **Liability meta**: created alongside its holding → updatable in place (EMI/tenure/paid_months
  change over the life of the loan) — the one entity in this phase that is *not* append-only, since
  it represents current loan terms, not a value history.

---

## DB readiness — what is authored, what is open

### Authored (on disk now)

| Artifact | Path |
|---|---|
| `liabilities_meta` table | `supabase/schemas/finance/10_tables/liabilities_meta.sql` |
| `holdings` extensions | `supabase/schemas/finance/10_tables/holdings.sql` |
| `valuations` extensions | `supabase/schemas/finance/10_tables/valuations.sql` |
| 3 views, all `security_invoker` | `supabase/schemas/finance/20_views/v_{latest_valuation,net_worth_by_sector,net_worth_history}.sql` |
| 2 functions | `supabase/schemas/finance/30_functions/{correct_valuation,create_holding_with_value}.sql` |
| Erasure extended | `supabase/schemas/public/30_functions/delete_my_data.sql` (+ `liabilities_meta`) |
| Migration | `supabase/migrations/20260823094500_networth_phase2.sql` |

`config.toml`'s `schema_paths` already globs `20_views/*.sql` and `30_functions/*.sql`, so no
config change was needed.

### Open — planned, not done

**1. The migration has never been executed.** It is **hand-authored, not `supabase db diff`-
generated**. `supabase db reset` locally, or the `develop` push that runs `supabase-migrate.yml`'s
`apply-dev` job, is its **first real execution and the point at which its correctness is actually
confirmed**. → T078.

*Toolchain, verified 2026-08-23 (an earlier draft of this section repeated ADR-0033's "CLI + Docker
not installed" without re-checking — that was true when that ADR was written on 2026-08-16 and is no
longer true of the CLI):* **`supabase` CLI v2.114.0 is installed. Docker is not.** So the local
stack (`supabase db reset`) cannot run, and the default `migra` diff engine is containerised.
`supabase db diff --linked`, which diffs local migration files against the linked project, is the
path to try first — confirm it works before relying on it. ADR-0032 decision 4's caveat list
requires hand-authorship for several statements here regardless (security-invoker views, grants,
`create or replace function` bodies), so a generated diff would still need hand-editing.

**Re-verified 2026-09-03 (Phase 11, T078) — still blocked, same shape of blocker as ADR-0033 named
for a different tool.** The CLI is present and the project is linked
(`supabase/.temp/linked-project.json` → `dsfnrtckgpnvyvscevxn`, name `dhruv`), confirmed by reading
that file directly. What's missing is an **authenticated session**: no `SUPABASE_ACCESS_TOKEN` in
the environment and no `supabase login` session on disk (`supabase projects list` fails with
`LegacyPlatformAuthRequiredError`) — `supabase db diff --linked`/`db push` both need one, and Docker
is still absent, so the local `db reset` path is equally closed. Both routes named above remain the
correct unblock; neither is executable from this session. **What *was* completed without execution**
— a static text review of the migration file against T078's own three named watch-items, all
confirmed present by direct inspection of `supabase/migrations/20260823094500_networth_phase2.sql`:
`security_invoker = on` on all three views (lines 173, 191, 209), the `as_of <= current_date` guard
as `add constraint valuations_as_of_check` (line 95), and both RPC functions
(`correct_valuation`/`create_holding_with_value`) present with bodies matching their declarative
source files verbatim. This is not a substitute for running it — a text match proves the SQL says
what it should, not that Postgres accepts it against `dhruv-dev`'s actual state — but it is real,
completable verification that needed no live access. Ready-to-run verification scripts for what
*does* need a live database (T081–T083) are authored at `supabase/verification/` — see that
directory's `README.md`.

**2. The ADR-0032 equivalence guard — RESOLVED 2026-08-23.** It reported
`finance.holdings` and `finance.valuations` as drifted when the schema was in fact
consistent, because `gen_schema_docs.py` had no rule for `ALTER TABLE … ADD COLUMN`, so a
column introduced by a migration was invisible on the executed side. The parser now
understands `ADD COLUMN` and `ADD CONSTRAINT`, compares columns **order-insensitively** on
name + type (a `db diff` migration appends columns; the declarative file declares them in
place), and compares CHECK expressions as a **normalized per-table set** so the same rule
spelled inline and spelled as a named constraint match. `equiv` is green, and it was
verified to still catch a column present on only one side and a changed CHECK, while
correctly ignoring a pure column reorder. → T079.

*Known remaining gap, deliberate:* a named `UNIQUE`/`PRIMARY KEY`/`FOREIGN KEY` added by
`ALTER` has no comparable inline spelling on the declarative side, so those are **not**
compared. Folding them in would reintroduce exactly the permanent false positives this
change removed, and a guard that cries wolf gets ignored.

**3. The Windows crash — RESOLVED 2026-08-23.** `gen_schema_docs.py` wrote `SCHEMA.md`
successfully and then died printing `✅` under cp1252, reporting failure for a run that had
succeeded. It now reconfigures stdout/stderr to UTF-8 itself; no `PYTHONIOENCODING` needed.
→ T080.

**4. Two reversible schema choices — CONFIRMED 2026-09-03 (Phase 11, T084), no change.**
`collateral text` (not a `collateral_holding_id uuid` FK): confirmed — the design's own C7 renders
collateral as a descriptive line, not a link to another tracked holding, and a hypothecated vehicle
or a pledged deposit "outside the tracker" (this file's original reasoning, § New entities above) is
a real and common case a FK cannot represent. No new information since 2026-08-23 argues otherwise.
`v_net_worth_history`'s 30-day comparison window (not calendar-month-to-date): confirmed — a
calendar-month comparison degrades badly on the 1st–2nd of a new month (comparing against an
almost-empty partial month produces a misleading, near-100% delta), which a rolling 30-day window
never does. The view's own grain is trailing month-end points regardless of which comparison a
client picks, so this choice lives entirely in how a client reads the view, not in the view's SQL —
changing it later, if ever needed, touches no schema and is not the migration-time decision the
"reversible only while no rows exist" framing implied. Both choices ship as originally authored;
this entry exists so a later phase does not re-open either without a genuinely new reason.

### Maintenance conventions for every later phase

These exist so phases 3–7 do not re-derive them. Each is a rule the audit found *missing*, not a
restatement of ADR-0032.

1. **Every view carries `with (security_invoker = on)`.** No exceptions. `db diff` cannot emit it —
   verify by hand in the generated migration, every time.
2. **Every new user-data table adds its `DELETE` to `public.delete_my_data()`** in the same
   migration. This is the whole DPDP 7-day erasure guarantee and a miss is silent — no test fails.
3. **Grants are hand-appended.** `db diff` emits no `GRANT`, and ADR-0033 decision 4 makes an
   explicit grant mandatory for custom-schema objects — without one the object is simply unreachable.
4. **One declarative file per object**, then `db diff`, then review, then commit both. Never
   hand-edit a migration that has already been applied to dev.
5. **Each phase's RLS test asserts a second user reads zero rows** from every table *and every view*
   it adds. Views were the gap: table policies were tested, view invoker-rights were not.
6. **Money is `bigint` paise; proportions are integer basis points.** No `numeric`, no float, no
   whole-percent columns (the audit found `nominee_share_pct` unable to represent three equal
   nominees).
7. **A frozen TEXT enum gets a `CHECK` constraint**, not prose. BR-C3 makes the values append-only
   forever, so the migration cost of adding one is correct and intended.
