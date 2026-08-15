# money (not yet created)

Day-to-day ledger: transactions, quick add, accounts, credit cards, categories, recurring.

- **Gradle module:** `:apps:finance:feature:money` — **does not exist yet.** Not in
  `settings.gradle.kts`. This folder holds only this README until Phase 3 creates the module.
- **Owner tab:** Money (ADR-0027 — the 5th tab, inserted between Home and Calc)
- **Flag:** `money` — not yet added to `platform/feature-flags/dhruv-finance.json`; add it as part
  of Phase 3's SA step.
- **Builds in:** design-v1 Phase 3 —
  `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §7.

## Screens (functional spec §5 Group D)
D1 Ledger (tab root, day-grouped) · D2 Quick add (sheet) · D3 Full transaction form · D4
Transaction detail · D5 Filter (sheet) · D6 Accounts · D7 Account detail · D8 Categories · D9
Recurring.

## QA scenarios
`apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §4 (`MNY-*`, 20 rows) — write the
failing tests against those scenario IDs before implementing (module-standard doc §2, the TDD gate).

## Business rules to implement against
`TRANSFER` transactions are never spend — excluded from budgets/category totals (BR-D1). Credit
cards hold negative balances, excluded from "spendable now" (BR-D2). Category rename keeps history;
merge is irreversible and must confirm the exact transaction count moved (BR-D3). Recurring
templates never post directly to the ledger — always through the review queue first (BR-D4). Every
mutation appends a `transaction_events` audit row (BR-D5). Money is `Long` paise end-to-end (NFR-3).
