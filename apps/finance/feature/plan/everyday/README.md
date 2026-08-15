# everyday

Everyday money tools: simple/compound interest, discount/markup, tip & bill split, inflation.

- **Gradle module:** `:apps:finance:feature:everyday`
- **Owner tab:** Plan (calculator strip, below the E1 live modules once Phase 4 rewrites the root)
- **Flag:** `everyday` — enabled

## Screens
- `EverydayScreen` (tabs: Interest, Discount, Tip Split, Inflation)

## ViewModels
- `EverydayViewModel` (`calculateSimpleCompound`, `calculateDiscountMarkup`, `calculateTipSplit`,
  `calculateInflation`)

## Data dependencies
- None — pure calculation. Uses `CurrencyFormatter` from `:apps:finance:data` for display only.
