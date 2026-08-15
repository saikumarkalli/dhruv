# insurance (not yet created)

Policy registry: cover vs need, renewals before they lapse.

- **Gradle module:** `:apps:finance:feature:insurance` — **does not exist yet.** Not in
  `settings.gradle.kts`. This folder holds only this README until Phase 4 creates the module.
- **Owner tab:** Plan (screens E7–E8)
- **Flag:** `insurance` — not yet added to `platform/feature-flags/dhruv-finance.json`; add as part
  of Phase 4's SA step.
- **Builds in:** design-v1 Phase 4 —
  `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §7.

## Screens (functional spec §5 Group E, subset)
E7 Insurance (list, cover-vs-rule-of-thumb, renewal banner, GAPS section) · E8 Policy detail
(nominee, documents, premium history).

## QA scenarios
`apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §6 (`INS-*`, 4 rows) — write the
failing tests against those scenario IDs before implementing (module-standard doc §2, the TDD gate).

## Business rules to implement against
Rule-of-thumb cover = 10× annual income + outstanding loans; shortfall = rule-of-thumb minus actual
cover (E7). Renewal banner logic and the GAPS section's uncovered-risk detection both need explicit
test fixtures, not eyeballing.
