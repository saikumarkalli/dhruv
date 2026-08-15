# R5 Pre-Work — Accounts Entity & Multi-Currency Stance (Decision Spec)

> Status: **SPECCED** (must be decided BEFORE P2 implementation starts — P2's
> `transactions.account_ref` and every tracker table's `currency` column both dangle without
> these). Master sequence: `../plans/2026-07-12-master-roadmap-personal-app.md` (R5; gaps N6/N14).
> Output = two ADR entries in `platform/DECISIONS.md`: **ADR-0017 (accounts are assets)** and
> **ADR-0018 (INR-only, validated)**. Minimal code ships with P2 itself; this spec exists so P2
> starts with zero schema ambiguity.

## Problem 1 (N6): `account_ref` points at nothing

P2's schema has `transactions.account_ref uuid` — "nullable → assets.id (which account it hit)" —
but no accounts feature is specced anywhere. Left undecided, P2 data is permanently unlinkable and
P6 balance reconciliation / R9 XIRR lose their join key.

### Options

| Option | Shape | Verdict |
|--------|-------|---------|
| **A. Accounts ARE assets** (recommended) | A bank account is already representable as an asset with `category = BANK` (or `CASH`). `account_ref` formally becomes an FK to `assets.id`; the QuickAdd sheet's optional "Account" picker lists the user's BANK/CASH assets. No new table, no new screens — account management IS asset management (P1 screens, unchanged) | **ACCEPT** |
| B. Dedicated `accounts` table | New table, new CRUD screens, new repository | Rejected: duplicates the assets domain, doubles entry for the user (a bank balance would exist twice), violates the code-move/minimal-surface instinct |
| C. Formally defer (declare `account_ref` unused until P6) | Zero work now | Rejected: costs nothing to accept A now, and every unlinked transaction is data the user must re-attribute later |

### Decision A — execution detail (ships inside the P2 PR)

- SQL (additive, in P2's migration): `alter table transactions add constraint transactions_account_ref_fkey
  foreign key (account_ref) references assets(id) on delete set null;`
  `on delete set null` — deleting an asset must never delete transaction history.
- Soft-deleted assets: picker hides them; existing `account_ref` values keep pointing (history
  stays attributable); R8 trash-restore brings the link back to life for free.
- QuickAddSheet: optional "Account" chip row (BANK/CASH assets, most-used first, skippable —
  `account_ref` stays nullable). Zero new validation.
- `TransactionListScreen`: account name as a secondary chip on rows that have one; account filter
  added to the existing filter chips.
- **Explicit non-goal:** no balance reconciliation (asset value ≠ sum of transactions) — asset
  valuations remain manual/append-only per P1; reconciliation is P6 automation territory.
- R9 dependency satisfied: XIRR cashflows join `transactions (category = INVESTMENT, account_ref =
  asset.id)` against that asset's valuation series.

## Problem 2 (N14): `currency` column with no semantics

Every tracker table carries `currency text not null default 'INR'`, but no conversion logic is
specced. A USD brokerage asset entered today silently sums into net worth as if paise —
**wrong by ~85×**. Silence is the only unacceptable option.

### Options

| Option | Shape | Verdict |
|--------|-------|---------|
| **A. INR-only, validated** (recommended) | Client never exposes a currency field (always 'INR'); server adds `check (currency = 'INR')` on all tracker tables; editors label amounts "₹" | **ACCEPT for now** |
| B. FX-convert at read time | Non-INR holdings converted via the existing currency-rates cache into net worth | Rejected for now: rate staleness makes net worth non-deterministic; historical valuations need historical rates (new data domain); the maintainer's actual holdings are INR — cost with no current benefit |
| C. Display-only currency tag | Store foreign amounts, exclude from totals, badge them | Rejected: a net worth that excludes known assets is a lie in the other direction |

### Decision A — execution detail (ships inside the P2 PR)

- SQL (additive migration, all tracker tables): `alter table assets add constraint assets_inr_only
  check (currency = 'INR');` (same for liabilities, valuation_entries, transactions, budgets, and
  future P3/P4/P5 tables — the constraint joins each table's create script going forward).
- Client: DTOs keep the `currency` field (schema honesty); editors hard-code 'INR'; a defensive
  mapper check logs to `CrashReporter` if a non-INR row is ever read (should be impossible).
- **Upgrade path stays open by design:** the column exists, the check constraint is one `drop
  constraint` away, and Option B's requirements (historical FX rates per valuation date) are
  recorded here as the acceptance bar for a future ADR. Trigger to revisit: the first real
  foreign-currency holding.

## Problem 3 (PG2, PO review): no TRANSFER type — self-transfers corrupt the books

P2's `type` = INCOME | EXPENSE only. Savings→FD moves and ATM withdrawals would log as fake
expense + fake income, inflating both sides and corrupting the savings rate that feeds P3 ETAs
and P5 defaults.

**Decision (reserve ADR-0022):** at P2 table creation, `type` gains `TRANSFER` (append-only-safe
since the enum ships with it), schema gains `transfer_to_ref uuid references assets(id) on delete
set null` (destination; `account_ref` = source), and **every income/expense rollup excludes
TRANSFER** — `MonthlyRollupCalculator` test cases for this are mandatory. QuickAdd type toggle
becomes three-way; category chip row hidden for transfers (category stored as `OTHER`).

## Problem 4 (PG1, PO review): P2 CRUD is capture-only

P2 never states editing/deleting an existing transaction. **Decision:** P2 build requirement —
row tap → the same QuickAdd sheet pre-filled (edit); delete = soft + undo snackbar (R8 pattern);
"duplicate" row action for repeat purchases. No schema impact.

## Tests

FK behavior: repository test — delete linked asset → transaction survives with `account_ref` null
(fake remote asserts PATCH semantics). INR guard: editor ViewModel test (currency always 'INR' in
outbound DTO); mapper defensive-check test. Both ride P2's test suite.

## Dependencies / sequencing

Blocks P2 implementation start. No standalone PR — ADR entries land with the P2 PR that
implements them. R9 (XIRR) and P6 (reconciliation) consume Decision A-N6; nothing consumes N14
beyond the constraint itself.

## Risks / open questions

- User later wants foreign assets → revisit ADR-0018 with the recorded acceptance bar (historical
  rates). Not a schema migration risk — column already exists.
- BANK/CASH asset used as an account gets soft-deleted with years of linked transactions → picker
  hides it but history remains readable; document in helper text ("archive, don't delete, accounts
  you've transacted against").
