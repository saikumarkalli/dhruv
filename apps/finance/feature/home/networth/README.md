# networth

Net worth tracker: assets, liabilities, valuations. Supabase-primary (ADR-0014), not Room.

- **Gradle module:** `:apps:finance:feature:networth`
- **Owner tab:** Home — reached from the Home tab's "View details" button (`DetailRoute.NetWorth`,
  a shell-level route hoisted in `MainActivity.kt`, not a pager tab of its own)
- **Flag:** `networth` — enabled, `requiresConsent: true` (`platform/feature-flags/dhruv-finance.json`)
- **Built in:** design-v1 Phase 2 (001-net-worth-tracker) — all 7 sub-phases shipped.

## Screens
- `NetWorthOverviewScreen` (C1) — net worth by sector, donut + sector rows
- `AssetsScreen` (C2) — sector-grouped asset list with filter chips
- `HoldingDetailScreen` (C3) — value history, trend sparkline, delta
- `AddEditHoldingScreen` (C4) — add a holding (asset or liability), collects liability terms
  (type/rate/EMI/tenure) inline when kind = LIABILITY
- `AddValuationSheet` (C5) — record or correct a value
- `LiabilitiesScreen` (C6) — grouped by type, outstanding/monthly-outgo/debt-free-by stats
- `LiabilityDetailScreen` (C7) — amortisation split donut, loan terms, prepay-savings projection

`NetWorthNavHost.kt`'s `NetWorthFeatureRoot` owns C1-C7's own nested `NavHostController` — see its
doc comment for how that integrates with `MainActivity`'s hardware back button. Home's own screen
(01) is shell-owned (`apps/finance/app/.../ui/home/`), not part of this module.

## ViewModels
`NetWorthOverviewViewModel`, `AssetsViewModel`, `HoldingDetailViewModel`, `AddEditHoldingViewModel`,
`AddValuationViewModel`, `LiabilitiesViewModel`, `LiabilityDetailViewModel` — registered in
`di/NetWorthModule.kt`. `AmortisationMath.kt` holds the pure payoff/prepay-projection math shared by
`LiabilitiesViewModel` and `LiabilityDetailViewModel`.

## Data dependencies
`:apps:finance:data`'s tracker layer: `HoldingRepository`, `ValuationRepository`,
`LiabilityRepository`, `NetWorthRepository` (all Supabase REST, no Room) — registered in
`:apps:finance:app`'s `PlatformModule.kt`.

## QA scenarios
`apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §3 (`NW-*`, 14 rows,
closed or explicitly deferred as of the Phase 8 checkpoint).

## Business rules implemented
Valuations are **append-only** (BR-C1) — no UPDATE path, ever (enforced at the RLS layer, not just
the repository). A holding's first valuation is written atomically with the holding itself (BR-C2,
`finance.create_holding_with_value`). Sector and liability-type are closed enums, never free text
(BR-C3). Net worth is read from `v_net_worth_by_sector`, never summed client-side over the whole
ledger (BR-C4, NFR-8). A liability's amortisation split sums to its total obligation (principal
paid + interest paid + remaining).

## Known gaps
- C2 (`AssetsScreen`) and C6 (`LiabilitiesScreen`) don't gate on signed-out/consent-off state the
  way C1 does (NW-UI-005) — found during the Phase 8 QA pass, not fixed there.
- No live Supabase verification this session (no credentials available) — every repository is
  tested against fakes only.
- No edit-liability screen — `LiabilityRepository.updateMeta()` exists and is tested, but nothing
  in the UI calls it yet.
