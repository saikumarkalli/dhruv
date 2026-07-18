# R9 — Investment Returns (XIRR & Growth Rate)

> Status: **SPECCED** (ships with the P5 retirement phase — same pure-math package, same
> disclaimer discipline). Master sequence: `../plans/2026-07-12-master-roadmap-personal-app.md`
> (R9; gap N15). Umbrella + design system + playbook binding; inherits all shared invariants
> (paise at boundaries, BigDecimal inside engines, TDD).

## Goal

P1 stores every asset's full valuation history and P2 (with ADR-0017) stores contribution flows —
but nothing ever computes a return from them. Add per-asset and portfolio-level annualized
returns: true **XIRR** where contribution data exists, honest **value-growth rate** where it
doesn't, never conflating the two.

## The correctness problem this spec exists to enforce

A valuation series alone cannot distinguish growth from contributions: an asset that went
₹1L → ₹2L might be +100% return or 0% return with a ₹1L top-up. Therefore:

| Data available | Metric shown | Label |
|---|---|---|
| Valuations + linked `INVESTMENT` transactions (`account_ref = asset.id`, ADR-0017) | **XIRR** over cashflows (each contribution = outflow at its date; terminal inflow = latest valuation) | "XIRR (money-weighted)" |
| Valuations only | Annualized value growth `(latest/first)^(365.25/days) − 1` | "Value growth — includes any money you added" |

The two are never mixed in one number; the UI labels which one it is showing. This split is the
core design decision (**ADR-0021** at implementation time).

## Non-goals

- No TWRR (needs per-period flows between every valuation pair — data too sparse for honesty).
- No benchmark comparison (no index data source in the app; would need its own keyless-API spec).
- No tax/LTCG math. No "advice"; the P5 disclaimer pattern applies verbatim.
- No liability-side APR inference (P3's payoff engine owns debt math).

## Architecture

No server schema changes. No new dependencies.

| Piece | Detail |
|-------|--------|
| Pure logic (TDD), `:data` `financemath` package (P3/P5 home) | `XirrCalculator`: Newton–Raphson on the XIRR NPV function, bisection fallback on non-convergence/oscillation, bounded to (−0.999, 10.0); returns `Converged(rateBps)` / `NoSolution`; BigDecimal internals, basis-point output (integer, house style). `GrowthRateCalculator`: annualized from first/latest valuation pair; guards: < 30 days span → absolute-only (no annualization — annualizing a week is noise), zero/negative first value → undefined. `CashflowAssembler`: (valuations, transactions, today) → dated flow list; rules: only `INVESTMENT`-category linked txns count as contributions; soft-deleted rows excluded; terminal value = latest non-deleted valuation |
| Repository | No new repository — `INetWorthRepository` (valuations) + `ITransactionRepository` (linked txns) already expose the inputs; a small `ReturnsUseCase` in the feature composes them |
| Surfaces | `HoldingDetailScreen` (P1, extended): "Returns" `BentoCard` — headline annualized % (`StatDeltaChip` semantics), absolute change ₹ (`MoneyText`), period ("since Mar 2024"), metric label per table above; info icon → bottom-sheet explaining XIRR vs growth. Portfolio: net-worth Home gains nothing (kept clean); the investments hub gets a "Portfolio returns" card = paise-weighted aggregate across assets with ≥ 2 valuations, labeled by the *weakest* metric present ("blended — some assets lack contribution data") |
| Flag | Rides `networth` flag (extends its screens; no new route) |
| Observability | trace `returns_compute`; `NoSolution` counted via CrashReporter log (non-fatal breadcrumb, no values) |

## Tests (the whole feature is its tests)

`XirrCalculatorTest`: known-fixture cases cross-checked against spreadsheet XIRR (single
contribution + terminal, monthly SIP year ≈ known rate, negative return, same-day flows,
non-convergent pathological series → bisection or `NoSolution`, sub-year span, huge gain bound);
`GrowthRateCalculatorTest` (annualization, <30-day guard, negative, undefined guards);
`CashflowAssemblerTest` (category filter, soft-delete exclusion, unlinked txns ignored, no
valuations → empty); paise↔BigDecimal boundary rounding (half-even, house rule); ViewModel test:
asset with/without linked txns selects correct metric + label. ArchUnit green.

## Dependencies

P1 (valuation history), ADR-0017 (account_ref join — the XIRR path is inert without it, growth
path works regardless), P2 (INVESTMENT transactions). Ships inside the P5 phase train as its own
PR (shares the `financemath` package P3 extracts).

## UI/UX detail

| Surface | Layout & states |
|---|---|
| Holding "Returns" card | Headline % with ▲/▼ role colors, metric label caption, absolute ₹ line, period line; states: needs-more-data ("Add a second valuation to see returns") when < 2 valuations; privacy mode masks ₹ but keeps % (percentages don't leak balances — deliberate R3 carve-out, documented there at implementation) |
| Info sheet | Two-paragraph plain-language XIRR vs growth explainer + the double-count warning ("link your SIP transactions to this asset to get true XIRR") |
| Portfolio card | Investments hub `BentoCard`: blended annualized %, asset count, weakest-metric label |

## Rollout & rollback

No new flag/route/table — revert PR is total rollback. Merge gates: standard + fixture
cross-check documented in the PR (spreadsheet screenshot or CSV committed to test resources).

## Risks / open questions

- Percentage-visible-under-privacy-mode carve-out: % alone can't reconstruct balances; if the
  maintainer disagrees at build time, masking %'s is a one-line change in the card.
- Users who log SIPs without linking `account_ref` silently get the weaker metric → the info
  sheet + a one-time hint chip on unlinked-but-INVESTMENT-heavy assets nudge linking.
- XIRR numerical edge cases are notorious → bisection fallback + `NoSolution` honesty (card shows
  "can't compute a stable rate for these flows") rather than a wrong number.

## TDD Mandate

> **Test-Driven Development (TDD) is strictly required for this phase.**
> All pure logic, calculators, reducers, and state machines MUST be written with failing tests first, followed by implementation. UI components must be tested for accessibility and rendering states on both Android and Web platforms.

