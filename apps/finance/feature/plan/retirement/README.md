# retirement (not yet created)

Corpus projection with assumptions shown on the same screen as the answer.

- **Gradle module:** `:apps:finance:feature:retirement` — **does not exist yet.** Not in
  `settings.gradle.kts`. This folder holds only this README until Phase 4 creates the module.
- **Owner tab:** Plan (screen E9)
- **Flag:** `retirement` — not yet added to `platform/feature-flags/dhruv-finance.json`; add as
  part of Phase 4's SA step.
- **Builds in:** design-v1 Phase 4 —
  `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §7.

## Screens (functional spec §5 Group E, subset)
E9 Retirement — Base/Optimistic/Cautious scenarios, projected corpus, assumptions (retire age,
monthly spend, inflation, pre/post-retirement return, life expectancy).

## QA scenarios
`apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §7 (`RET-*`, 4 rows) — write the
failing tests against those scenario IDs before implementing (module-standard doc §2, the TDD gate).

## Business rules to implement against
The projection engine is pure Kotlin, **tested first** with golden-value fixtures per assumption
(RET-BR-001) — correctness-critical, same standard as the existing calculators. The three scenarios
must produce genuinely distinct corpus values for the same base inputs, not aliases of each other.
