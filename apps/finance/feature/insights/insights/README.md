# insights (not yet created)

Statements: monthly summary, cashflow, P&L, balance sheet, reports & export.

- **Gradle module:** `:apps:finance:feature:insights` — **does not exist yet.** Not in
  `settings.gradle.kts`. This folder holds only this README until Phase 5 creates the module.
- **Owner tab:** Insights
- **Flag:** `insights` — not yet added to `platform/feature-flags/dhruv-finance.json`; add as part
  of Phase 5's SA step.
- **Builds in:** design-v1 Phase 5 —
  `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §7.

## Screens (functional spec §5 Group F)
F1 Monthly summary (tab root, savings rate) · F2 Cashflow statement · F3 Profit & loss (YoY) · F4
Balance sheet · F5 Reports & export.

## QA scenarios
`apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §8 (`SIG-*`, 8 rows) — write the
failing tests against those scenario IDs before implementing (module-standard doc §2, the TDD gate).

## Business rules to implement against
Every statement must reconcile: cashflow's opening + in − out − moved = closing (SIG-BR-001), P&L
line sums equal displayed totals with YoY compared to the same calendar month (SIG-BR-002), balance
sheet's assets − liabilities = displayed net worth (SIG-BR-003). All data comes from server-side
views (`v_cashflow`, `v_pnl`, `v_balance_sheet`), never a client-side full-ledger reduction (NFR-8).
FY period resolution uses the Indian financial year (Apr–Mar).
