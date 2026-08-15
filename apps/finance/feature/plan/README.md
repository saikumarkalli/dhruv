# Plan tab

Modules reached from the **Plan** tab (ADR-0027) — the tab with the most modules, because it hosts
two distinct generations of feature: the original four calculators, and the design-v1 live
planning modules layered on top of them (E1's rewrite puts the live modules first, calculators as
a strip below — see the implementation plan's Phase 4).

| Module | Status |
|---|---|
| [loans](loans/README.md) | live — Loan EMI + comparison |
| [investments](investments/README.md) | live — SIP / ROI-CAGR / FD-RD |
| [tax](tax/README.md) | live — GST + salary breakup |
| [everyday](everyday/README.md) | live — interest / discount / tip / inflation |
| [planning](planning/README.md) | not yet created — Phase 4 (budgets, goals, debt payoff) |
| [insurance](insurance/README.md) | not yet created — Phase 4 |
| [retirement](retirement/README.md) | not yet created — Phase 4 |

**E1 (the Plan tab's own root screen) is shell-owned**, not a feature module — it lives in
`apps/finance/app/.../ui/plan/PlanLauncher.kt`. This was a real point of confusion caught during
the 2026-08-09 doc re-validation (`apps/finance/docs/superpowers/specs/2026-08-09-module-standard-and-tdd-process.md`
§2.2 has the corrected module-code table) — don't create a `plan/plan/` module for E1, it doesn't
belong here.
