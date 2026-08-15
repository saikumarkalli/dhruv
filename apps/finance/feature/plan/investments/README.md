# investments

SIP growth, ROI/CAGR, and FD/RD maturity calculators.

- **Gradle module:** `:apps:finance:feature:investments`
- **Owner tab:** Plan (calculator strip, below the E1 live modules once Phase 4 rewrites the root)
- **Flag:** `investments` — enabled

## Screens
- `InvestmentsScreen` (tabs: SIP Growth, ROI / CAGR, FD / RD)

## ViewModels
- `InvestmentsViewModel` (`calculateSip`, `calculateRoiCagr`, `calculateFdRd`)

## Data dependencies
- None — pure calculation. Uses `CurrencyFormatter` from `:apps:finance:data` for display only.
