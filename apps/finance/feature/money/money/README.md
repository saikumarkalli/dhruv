# money

Day-to-day ledger: transactions, quick add, accounts, credit cards, categories, recurring.

- **Gradle module:** `:apps:finance:feature:money`
- **Owner tab:** Money (ADR-0027 — the 5th tab, inserted between Home and Calc)
- **Flag:** `money` — `platform/feature-flags/dhruv-finance.json`, `enabled = true`,
  `requiresConsent = true` (tracker data, same consent gate as `networth`)
- **Built in:** design-v1 Phase 3 —
  `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §7.

## Screens (functional spec §5 Group D)
- `LedgerScreen` (D1) — tab root, day-grouped rows, pinned month header, filter entry
- `QuickAddSheet` (D2) — amount-first entry sheet, category/account pre-guessed
- `TransactionFormScreen` (D3) — full form: amount, type, account(s), category, payee, note,
  "make it recurring" toggle + frequency
- `TransactionDetailScreen` (D4) — amount/payee/datetime/cleared state, HISTORY audit trail,
  Duplicate and Make-recurring actions (both hand off to D3 via `MainActivity`'s
  `pendingDuplicatePrefill`)
- `FilterSheet` (D5) — filter chips with a live preview count before applying
- `AccountsScreen` (D6) — account list, SPENDABLE NOW total
- `AccountDetailScreen` (D7) — account detail, reconciliation banner + Fix flow
- `CategoriesScreen` (D8) — Expense/Income tabs, rename, exclude-from-spend, merge (irreversible,
  confirmed with the exact transaction count that will move)
- `RecurringScreen` / `RecurringReviewScreen` (D9) — MONTHLY IN/OUT, NEXT 30 DAYS, PAUSED
  sections; review queue for materialised suggestions (accept/dismiss)

## ViewModels
`LedgerViewModel` · `QuickAddViewModel` · `TransactionFormViewModel` · `TransactionDetailViewModel`
· `AccountsViewModel` · `AccountDetailViewModel` (parametrized by `accountId`) ·
`AccountFormViewModel` · `CategoriesViewModel` · `RecurringViewModel` · `RecurringReviewViewModel`
— all extend `FeatureViewModel(crashReporter, "money")`, wired in `di/MoneyModule.kt`.

## Data dependencies
`:apps:finance:data`'s `tracker/repo` package, Repository-only (never Room, never a second
PostgREST client): `TransactionRepository`, `AccountRepository`, `CategoryRepository`,
`RecurringRepository`, `SuggestionRepository`. All PostgREST access is consent-gated
(`ConsentInterceptor` on `SupabaseClientFactory.dataClient`) and auth-gated (`AuthInterceptor`).

## Business rules implemented
`TRANSFER` transactions are never spend — excluded from budgets/category totals (BR-D1). Credit
cards hold negative balances, excluded from "spendable now" (BR-D2). Category rename keeps
identity; merge is irreversible and confirms the exact transaction count moved via
`CategoryRepository.mergeCategories` / the `merge_categories` RPC (BR-D3). Recurring templates
never post directly to the ledger — `RecurringRepository.materialiseDue` writes a `suggestions` row
via `SuggestionRepository`, never a `transactions` row, and accepting one is the only write path
(BR-D4). Every mutation appends a `transaction_events` audit row, read by `TransactionDetailScreen`
via `TransactionAuditTest`-covered mapping (BR-D5). Money is `Long` paise end-to-end (NFR-3,
enforced by the root `checkTrackerMoneyPrecision` Gradle task); the one non-money percentage
(`Category.sharePercentTenths`) is an integer tenths-of-a-percent for the same reason.

## QA scenarios
`apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §4 (`MNY-*`, 20 rows)
— closed 2026-09-05 (§14 Recount): 18 ✅ (7 explicitly partial — budgets/goals are a future Plan-
module dependent this module doesn't own), 2 🔴 deferred (`MNY-UI-001` manual tap-timing check and
`MNY-FLOW-003` goal-linking, both blocked outside this module's scope).
