# currency

Live currency converter with cached-rate offline fallback and a sync status banner.

- **Gradle module:** `:apps:finance:feature:currency`
- **Owner tab:** none — shell-level detail route (reached from Calc's title bar / Home quick action)
- **Flag:** `currency` — enabled

## Screens
- `CurrencyScreen`

## ViewModels
- `CurrencyViewModel` (`CurrencyStatus` sealed state)

## Data dependencies
- `ICurrencyRepository`/`CurrencyRepository` → `CurrencyApi` (external exchange-rate API) + Room
  `CurrencyRateEntity` (offline cache), both in `:apps:finance:data`.
