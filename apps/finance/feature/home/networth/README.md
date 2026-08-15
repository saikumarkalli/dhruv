# networth (not yet created)

Net worth tracker: assets, liabilities, valuations. Supabase-primary (ADR-0014), not Room.

- **Gradle module:** `:apps:finance:feature:networth` — **does not exist yet.** Not in
  `settings.gradle.kts`. This folder holds only this README until Phase 2 creates the module.
- **Owner tab:** Home
- **Flag:** `networth` already exists in `platform/feature-flags/dhruv-finance.json`
  (`enabled: true`, `requiresConsent: true`) — provisioned ahead of the module, same as `date`/
  `time` were provisioned ahead of their code.
- **Builds in:** design-v1 Phase 2 —
  `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §7.

## Screens (functional spec §5 Group C + the Home tab)
C1 Net worth by sector · C2 Assets · C3 Holding detail · C4 Add/edit holding · C5 Add valuation ·
C6 Liabilities · C7 Liability detail. The Home tab's own screen (01) is shell-owned
(`apps/finance/feature/home/README.md`), not part of this module.

## QA scenarios
`apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §3 (`NW-*`, 14 rows) — write the
failing tests against those scenario IDs before implementing (module-standard doc §2, the TDD gate).

## Business rules to implement against
Valuations are **append-only** (BR-C1) — no UPDATE path, ever. A holding's first valuation is
written atomically with the holding itself (BR-C2). Sector is a closed enum, never free text
(BR-C3, append-only once shipped). Net worth is read from `v_net_worth_by_sector`, never summed
client-side over the whole ledger (BR-C4, NFR-8).
