# Phase 0 Research: Money Tab (Phase 3)

Nothing in Technical Context was left as NEEDS CLARIFICATION. This is brownfield work on an already
decided architecture, so research here means (a) confirming reuse, and (b) settling the handful of
questions Phase 3 is genuinely the first to hit. R1 and R7 **add** to the design-v1 implementation
plan's Phase 3 section; both were written back into that document as a dated note rather than left
only here.

## R1 — Phase 3's table list is larger than the implementation plan's §7 row says

**Decision**: Phase 3's migration ships `accounts`, `categories`, `transactions`,
`transaction_events` **plus `recurring_templates` and `suggestions`**, and the three views.

**Rationale**: the impl plan §7 Phase 3 step 1 lists only the first four tables and the three views,
but the same phase owns screen **D9 (Recurring)** and QA rows `MNY-BR-005` ("a `suggestions` row is
created, not a `transactions` row") and `MNY-FLOW-002` ("a `recurring_templates` row is created").
Those two rows cannot pass without both tables. The omission is a gap in the step-1 list, not a
scope decision — §5.4 already specifies both tables' shape, and §7's own Phase 7 row is about the
SMS/AA *sources* and the shared review-queue screen (G1–G3), not about recurring existing at all.

**Alternatives considered**: defer D9 to Phase 7 so the two tables move with it — rejected: D9 is
listed in Phase 3's own screen set (D1–D9) and its checkpoint, and recurring entries are the single
most common reason a manual ledger goes stale. Deferring would ship a Money tab that cannot express
rent or salary.

## R2 — Networking: extend Phase 1's stack, add nothing

**Decision**: all new repositories use the same Retrofit + Moshi + OkHttp `dataClient`
(`SupabaseClientFactory`) that Phase 1 built and Phase 2 extended.

**Rationale**: ADR-0029 settled this for the whole tracker domain. Nothing about transactions
changes the calculus, and constitution Article XI forbids re-opening a settled stack choice without
new information.

**Consequence to not forget**: calls to `finance.*` tables must send `Accept-Profile: finance`
(reads) / `Content-Profile: finance` (writes), per ADR-0033's consequences — omitting the header
does not error, it silently 404s against the empty `public` schema. If Phase 2 has already added
this to the client, Phase 3 inherits it; if Phase 3 lands first, it adds it.

## R3 — Transfer exclusion (BR-D1) is enforced in the view definitions, not only in Kotlin

**Decision**: `v_month_summary` and `v_category_spend` filter `type <> 'TRANSFER'` in SQL. The
repository layer also excludes transfers when it aggregates anything locally, but the view is the
authority.

**Rationale**: `MNY-BR-001` asserts a transfer is absent from expense totals, budgets **and**
category shares — three consumers, two of them (budgets, Phase 4; insights, Phase 5) not written
yet. Encoding the rule once in the view means those later phases inherit it instead of each
re-implementing an exclusion they could forget. Same reasoning as ADR-0029's consent interceptor:
make the rule structural, not a thing every call site remembers.

**Alternatives considered**: a `is_spend` generated column on `transactions` — equivalent, but it
duplicates a fact already derivable from `type`, and would need backfilling if the rule ever grows
(e.g. excluded categories, which are already a separate flag).

## R4 — Audit trail (BR-D5) is a database trigger, not two client calls

**Decision**: an `AFTER INSERT OR UPDATE OR DELETE` trigger on `finance.transactions` calls
`finance.fn_transaction_audit()`, which appends the `transaction_events` row describing the change.
`transaction_events` has SELECT + INSERT policies only — no UPDATE, no DELETE — mirroring
`valuations`.

**Rationale**: `MNY-BR-006` says *any* mutation appends an event. A client that writes the
transaction and then writes the event has two failure modes the rule forbids: a crash between the
two calls, and any future write path (import, recurring acceptance, a Phase 7 SMS accept) that
forgets the second call. PostgREST cannot wrap two requests in one transaction, so client-side
pairing cannot be made atomic at all. The trigger makes the guarantee structural.

**Alternatives considered**: (a) client writes both — rejected above; (b) a security-definer RPC per
mutation (`create_transaction()`, `update_transaction()`, …) — rejected as a larger surface that
replaces PostgREST's whole CRUD surface for one guarantee a trigger already gives; (c) Postgres
logical-decoding/audit extension — not available on the free tier and far past what BR-D5 asks for.

## R5 — Components: build the genuinely-missing batches, compose rather than duplicate

**Decision**: build B4 (`DayGroupHeader`, `LedgerRow`, `SuggestedRow`, `ReconcileBanner`), the B6
remainder (`NxTextArea`, `InputChip`) and B7 (`StatusBadge`, `InfoBanner`) as new `:libs:core`
components; build `DateRangeSheet` (B2 batch) for D5; build `AmountKeypadSheet` as a **composition**
of the existing `NumericKeypad` inside `DhruvModalSheet`, not a new keypad.

**Rationale**: verified by symbol search over `libs/core/src/main` (design-system §13 method) —
none of the above resolve today, so the gap is real. `NumericKeypad` and `DhruvModalSheet` both
exist, so D2's amount pad is an arrangement of built parts; adding a second keypad component would
be exactly the parallel-component fragmentation Article VI forbids.

**Depends on Phase 2**: `NxTextField`'s error/helper state, `NxButton` sizes/loading,
`SelectionSheet` (B9, D5's multi-category picker) and `NxSelect` (B6) are Phase 2 deliverables that
D3/D5 consume. Not rebuilt here.

**Alternatives considered**: hand-rolling ledger rows inside `:feature:money` — rejected outright,
that is the feature-local styling the micro-frontend rule (ADR-0014 §8) exists to prevent, and D1's
row is the single most-reused visual in the app.

## R6 — Receipts are device-local this phase

**Decision**: `transactions.receipt_path` stores a **device-local** URI into app-private storage.
The attachment is captured, stored and viewed on the device that recorded it. Receipt binaries are
not uploaded to Supabase Storage this phase, and the UI says so where a receipt is attached.

**Rationale**: no ADR covers object storage. Doing it properly means a private Storage bucket,
path-scoped RLS policies, upload retry/backoff, a size/quota policy against the free tier, and an
erasure path that `delete_my_data()` also has to cover — a meaningful new surface, on the critical
path of a phase whose actual subject is the ledger. Device-local is honest and cheap, and the
column shape does not change when cloud receipts land later (the value becomes a storage key).

**Alternatives considered**: (a) Supabase Storage now — rejected on scope, as above, and it deserves
its own ADR rather than being decided inside a phase plan; (b) drop receipts from D3 entirely —
rejected, the design draws them and capture-now/sync-later keeps the data.

**Consequence**: a receipt does not follow the user to a new device. That is a stated limitation,
not a silent one — same treatment as the "automatic balance refresh arrives with account linking"
footnote the design already requires on D6.

## R7 — Recurring entries are materialised client-side on open, idempotently

**Decision**: there is no server scheduler. When the Money tab (or the app) opens with consent
active, `RecurringRepository` asks for templates whose `next_run <= today` and not paused, and
inserts one `suggestions` row per due occurrence. A unique constraint on
`(recurring_id, due_on)` makes the operation idempotent, so opening the app twice — or on two
devices — cannot produce duplicate pending entries.

**Rationale**: `pg_cron` is not available/appropriate on the free tier and would run without a user
session, so it cannot satisfy RLS's `user_id = auth.uid()` without a service-role path — which
ADR-0014 §7 keeps well away from this design. Android `WorkManager` would work but adds a background
execution path for something whose only consumer is a screen the user is already looking at.
Materialise-on-open is the smallest mechanism that satisfies BR-D4 ("never posts silently") because
the produced row is a *suggestion*, never a ledger entry, regardless of when it is produced.

**Alternatives considered**: (a) `pg_cron` + service role — rejected on both availability and the
service-role rule; (b) `WorkManager` periodic job — deferred, not rejected: if Phase 7's automation
work introduces a background worker anyway, moving materialisation into it is a small change and
the idempotency key already makes double-production safe; (c) computing due entries purely in the
UI without persisting them — rejected, the user's accept/dismiss decision needs somewhere to live,
and `MNY-BR-005` explicitly asserts a persisted `suggestions` row.

## R8 — Reconciliation records an adjustment; it never rewrites the opening balance

**Decision**: reconciling an account writes (a) `accounts.reconciled_at = now()` and (b) if the
stated real balance differs from the computed one, an adjustment `transactions` row with
`source = 'RECONCILE'` against a reserved, `excluded_from_spend` category. Balance stays
`opening_balance_paise + Σ signed transactions`, from `v_account_balances`, always.

**Rationale**: FR-021 requires the difference be explainable rather than silently absorbed. Editing
`opening_balance_paise` to make the number match would rewrite history invisibly and break every
past-dated balance in D7's trend chart. An adjustment transaction is visible, auditable (it goes
through the same trigger as everything else), and keeps one balance formula in one place.

**Alternatives considered**: a separate `reconciliations` table holding stated balances — more
faithful bookkeeping, but it introduces a second source for "what is this account's balance" and a
reconciliation UI far beyond what D7 draws. Revisit if statement import (Phase 7) needs it.

## R9 — Category merge is one SQL function, not a client loop

**Decision**: `finance.merge_categories(p_source uuid, p_target uuid) returns integer` re-points
every transaction, soft-deletes the source category, and returns the number of transactions moved.
Runs with invoker rights — RLS already restricts it to the caller's own rows.

**Rationale**: `MNY-BR-004` requires the confirmation to state the exact count that will move and
the merge to be irreversible-but-complete. A client-side loop over pages of transactions can fail
half-way, leaving a partial merge with no undo — the worst possible outcome for an operation the
dialog just told the user cannot be undone. One function call is atomic. Invoker (not definer)
rights keep RLS in force, unlike the erasure functions, which need definer rights precisely because
they touch `auth.users`.

**Alternatives considered**: PostgREST bulk `PATCH` with a filter — atomic for the re-point, but it
cannot also delete the source category or return the moved count in the same transaction.

## R10 — Test strategy: fakes at the HTTP boundary; SQL rules verified at the Sec step

**Decision**: repository/ViewModel tests run against in-memory fakes or MockWebServer. The trigger,
the RLS policy set, the view-level transfer exclusion and `merge_categories`' atomicity are
verified against the dev Supabase project at the phase's Sec step, and recorded there.

**Rationale**: the standing project constraint (Robolectric-SQLite unreliable on Windows) plus the
fact that tracker data is not in Room at all — the fake boundary is HTTP. But R3/R4/R9 deliberately
put three correctness rules *in the database*, so a JVM-only test suite would assert them nowhere;
naming the Sec step as their verification home keeps them covered instead of assumed.

**Alternatives considered**: a Postgres test container in CI — the honest long-term answer, but it
adds container infrastructure to a pipeline with a hard runner-minute budget (ADR-0026) and is a
decision bigger than this phase.
