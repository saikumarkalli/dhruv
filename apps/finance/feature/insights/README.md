# Insights tab

Modules reached from the **Insights** tab.

| Module | Status |
|---|---|
| [insights](insights/README.md) | not yet created — Phase 5 |

Insights owns the statement/reporting surface (F1–F5: monthly summary, cashflow, P&L, balance
sheet, reports & export) — read-only views over Money/Home's data, never a second source of truth
for it. All its numbers come from server-side SQL views (`v_cashflow`, `v_pnl`, `v_balance_sheet`),
never a client-side reduction over the full ledger (NFR-8).
