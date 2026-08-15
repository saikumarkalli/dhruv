# automation (not yet created)

SMS parsing, account-aggregator groundwork, review queue for auto-suggested transactions.

- **Gradle module:** `:apps:finance:feature:automation` — **does not exist yet.** Not in
  `settings.gradle.kts`. This folder holds only this README until Phase 7 creates the module.
- **Owner tab:** none — reached from Settings, same as the other `shell/` modules.
- **Flag:** `automation` — not yet added to `platform/feature-flags/dhruv-finance.json`; ships
  `enabled: false` until this module's full checkpoint (including its DPDP entry) passes.
- **Builds in:** design-v1 Phase 7 (last) —
  `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §7.

## Screens (functional spec §5 Group G)
G1 Automation hub (one switch per source) · G2 Review queue (dashed rows until accepted, duplicate
detection) · G3 Account-aggregator consent.

## QA scenarios
`apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §9 (`AUT-*`, 9 rows) — write the
failing tests against those scenario IDs before implementing (module-standard doc §2, the TDD gate).

## Business rules to implement against
No automated source (SMS/AA/recurring) ever writes directly to `transactions` — everything lands in
`suggestions` first and needs an explicit user accept (BR-G1). Raw SMS text is parsed on-device and
never leaves the device (BR-G2) — verify by asserting the network DTOs sent to Supabase never
contain a raw-SMS field, not by inspection. A learned rule is visible with its applied count and can
be disabled (BR-G3). This is deliberately the last phase — it's the highest blast-radius module
(requests SMS/AA permissions) and depends on every other tracker surface already existing.
