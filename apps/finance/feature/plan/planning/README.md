# planning (not yet created)

Budgets, goals, and debt payoff — one module because they share repositories and cross-link
heavily (a budget overrun notification opens a budget detail screen that references the same
category totals a goal's funding calc reads; splitting them into three modules would force either
a `feature → feature` edge, which is forbidden, or triplicated view-model logic).

- **Gradle module:** `:apps:finance:feature:planning` — **does not exist yet.** Not in
  `settings.gradle.kts`. This folder holds only this README until Phase 4 creates the module.
- **Owner tab:** Plan (screens E2–E6 — **not** E1, which is shell-owned; see `plan/README.md`)
- **Flags:** `budgets`, `goals`, `debtpayoff` — not yet added to
  `platform/feature-flags/dhruv-finance.json`; add them as part of Phase 4's SA step.
- **Builds in:** design-v1 Phase 4 —
  `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §7.

## Screens (functional spec §5 Group E, subset)
E2 Budgets · E3 Budget detail · E4 Goals · E5 Goal detail · E6 Debt payoff.

## QA scenarios
`apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §5 (`PLN-*`, 14 rows) — write the
failing tests against those scenario IDs before implementing (module-standard doc §2, the TDD gate).

## Business rules to implement against
Goal progress = Σ current value of linked holdings (whole or earmarked) — linking never moves,
locks, or duplicates money (BR-E1). Budget pace = elapsed-day fraction of the calendar month
(BR-E2). Categories excluded from spend are excluded from budgets too (BR-E3). Every derived/AI
insight (recovery suggestions, avalanche/snowball trade-off text) must be visually labelled as such
(BR-E4). Budget-pace math and avalanche/snowball ordering are pure-Kotlin engines, **tested first**
like the existing calculators — see the implementation plan's Phase 4 Backend step.
