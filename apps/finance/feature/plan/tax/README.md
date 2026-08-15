# tax

GST/tax add-or-extract and CTC salary breakup (gross, PF, estimated tax, take-home).

- **Gradle module:** `:apps:finance:feature:tax`
- **Owner tab:** Plan (calculator strip, below the E1 live modules once Phase 4 rewrites the root)
- **Flag:** `tax` — enabled

## Screens
- `TaxScreen` (tabs: GST / Tax, Salary Breakup)

## ViewModels
- `TaxViewModel` (`calculateGst`, `calculateSalaryBreakup`)

## Data dependencies
- None — pure calculation. Uses `CurrencyFormatter` from `:apps:finance:data` for display only.
