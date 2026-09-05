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
  (type/rate/EMI/tenure) inline when kind = LIABILITY. Also handles **edit** (Phase 9): name,
  sector, invested amount and notes only — liability terms and the current-value field are
  add-only, not editable from this screen (see Known gaps)
- `AddValuationSheet` (C5) — record or correct a value
- `LiabilitiesScreen` (C6) — grouped by type, outstanding/monthly-outgo/debt-free-by stats
- `LiabilityDetailScreen` (C7) — amortisation split donut, loan terms, prepay-savings projection

`HoldingDetailScreen` (C3) also owns **soft-delete + undo** (Phase 9): Edit/Delete actions, a
5-second `UndoSnackbarHost` window as the only recoverable location (no Trash screen exists), and
the header now shows the holding's sector + last-valued date (Phase 10).

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
`:apps:finance:app`'s `PlatformModule.kt`. `HoldingRepository`/`ValuationRepository` also call two
security-definer RPCs rather than plain PostgREST inserts where atomicity/idempotency matters:
`finance.create_holding_with_value()` (holding + first valuation in one transaction, replay-safe on
`request_id`) and `finance.correct_valuation()` (the only path that amends a valuation — soft-delete
the wrong row + append the corrected one, since `valuations` has no UPDATE policy at all).

## Settings
`NetWorthSettingsContribution.kt` (`settings/` package, Phase 9) registers this module's Settings
entry — two `Info` rows stating the frozen sector/liability-type category counts (no toggle exists
to invent, per the settings-contribution convention's "one real static fact per row" rule).

## QA scenarios
`apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §3 (`NW-*`, 14 rows,
closed or explicitly deferred as of the Phase 8 checkpoint).

## Business rules implemented
Valuations are **append-only** (BR-C1) — no UPDATE path, ever (enforced at the RLS layer: no UPDATE
policy or grant exists on `finance.valuations`, not just repository discipline). A holding's first
valuation is written atomically with the holding itself (BR-C2, `finance.create_holding_with_value`).
Sector and liability-type are closed enums, never free text (BR-C3). Net worth is read from
`v_net_worth_by_sector`, never summed client-side over the whole ledger (BR-C4, NFR-8). A liability's
amortisation split sums to its total obligation (principal paid + interest paid + remaining).

## Known gaps
- No edit-liability screen — `LiabilityRepository.updateMeta()` exists and is tested, but nothing
  in the UI calls it yet (`AddEditHoldingScreen`'s edit mode deliberately excludes liability terms —
  see its own `startEditing()` doc comment).
- No live Supabase verification this session (no credentials available) — every repository is
  tested against fakes only. Phase 11 authored ready-to-run manual verification scripts for RLS on
  the tracker views and both RPCs (`supabase/verification/`), not yet executed for the same reason.
- The migration adding this module's schema objects
  (`supabase/migrations/20260823094500_networth_phase2.sql`) has itself never been executed against
  `dhruv-dev` — same credential gap. See `apps/finance/specs/001-net-worth-tracker/data-model.md`
  § "DB readiness" for the exact unblock steps.

C2/C6's signed-out/consent-off gating gap (found in the Phase 8 QA pass) and C3's missing
soft-delete were both closed in Phase 9; C1's legend/header fidelity gaps and accessibility/
`strings.xml` coverage were closed in Phase 10 — see `specs/001-net-worth-tracker/tasks.md` for the
full per-phase history.
