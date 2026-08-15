# loans

Loan EMI calculator and a side-by-side two-loan comparison (comparison reuses the EMI engine).

- **Gradle module:** `:apps:finance:feature:loans`
- **Owner tab:** Plan (calculator strip, below the E1 live modules once Phase 4 rewrites the root)
- **Flag:** `loans` — enabled

## Screens
- `LoansScreen` (tabs: Loan EMI, Loan Comparison)

## ViewModels
- `LoansViewModel` (`calculateEmi` → `EmiResult`)

## Data dependencies
- None — pure calculation. Uses `CurrencyFormatter` from `:apps:finance:data` for display only.
