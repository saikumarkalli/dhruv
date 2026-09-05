# Quickstart: Validate Net Worth Tracker (Phase 2)

Validation guide only — implementation detail lives in `tasks.md` (generated separately by
`/speckit-tasks`) and the feature's own source. Entities: `data-model.md`. Routes: `contracts/routes.md`.

## Prerequisites

- Phase 1 already shipped and working: signed in via Google, consent granted for "Sync my financial
  records" (`networth`'s `requiresConsent` gate).
- `supabase/migrations/20260823094500_networth_phase2.sql` applied to the dev Supabase project
  (adds `finance.liabilities_meta`, `v_latest_valuation`, `v_net_worth_by_sector`,
  `v_net_worth_history`, `correct_valuation()`, `create_holding_with_value()` — see
  `data-model.md`). Phase 10, T065: this line previously named a placeholder
  `0002_networth_phase2.sql` filename this feature never authored.
- `:apps:finance:feature:networth` module registered in `settings.gradle.kts` and wired into
  `CalculatorApplication`'s Koin graph.

## Automated validation

```powershell
# Unit + ArchUnit + this module's tests, with the merged coverage gate
./gradlew regressionCheck

# This feature's module in isolation
./gradlew :apps:finance:feature:networth:testDebugUnitTest

# Money-precision guard (no Double/Float on the tracker write path)
./gradlew checkTrackerMoneyPrecision
```

Expected: all green. Every test in the module cites an `NW-*`/`HOM-*` scenario ID from
`apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §3/§12 — a test with no
citation is a review-blocking finding per constitution Article I.

## Manual end-to-end scenarios (mirrors spec.md's Acceptance Scenarios)

1. **Story 1 — record and see net worth**
   Sign in → Home (net worth shows ₹0 / empty state) → Net worth overview → Add holding → choose
   "I own this" → pick a sector from the picker (confirm free text is rejected) → enter a value →
   save.
   **Expect**: holding appears in Assets immediately; C1's total and the tapped sector's subtotal
   both include it; Home's hero figure updates on next open.

2. **Story 2 — holding detail and history**
   Open the holding just added → Add a second valuation with a different value (C5) → reopen the
   holding.
   **Expect**: C3 shows both entries newest-first, the newer one's delta vs the older one; the
   trend range chips (3M/6M/1Y/All) all render without error even with only 2 data points.

3. **Story 3 — correct a value without losing history**
   From C3, add a value → realize it's wrong → add a corrected value.
   **Expect**: the wrong value's row is no longer shown in the "current" position, but is still
   visible in the entry list marked as history (never physically missing); net worth reflects only
   the latest un-superseded value.

4. **Story 4 — liability payoff**
   Add a `HOME_LOAN` liability with a rate, EMI, and tenure (C4, "I owe this") → open its detail
   (C7) → request a hypothetical extra payment.
   **Expect**: C6 shows it grouped under `HOME_LOAN` with outstanding/EMI/progress; C7 shows the
   amortisation donut summing to the total obligation, and returns an interest-saved + months-
   earlier figure for the hypothetical payment.

5. **Story 5 — Home at a glance**
   With the liability above having a `debit_day` in the near future, open Home fresh.
   **Expect**: net-worth hero, trend sparkline, and an UPCOMING row for that liability's EMI all
   render without navigating anywhere else; Home's total matches C1's total exactly.

6. **Offline / signed-out contract (FR-011)**
   Sign out, then open Net worth overview. Re-sign-in, enable airplane mode, force-kill and reopen
   with nothing cached.
   **Expect**: `SignedOutCard` in the first case, `OfflineStateCard` in the second — never a blank
   screen or a spinner that never resolves, on any of C1–C7 or Home.

## Definition of done for this quickstart

Every numbered scenario above passes manually, `regressionCheck` is green, and every NW-*/HOM-* row
in the QA catalog is closed (✅) or explicitly deferred with a stated reason — per constitution's
Development Workflow step 7 (checkpoint gate).
