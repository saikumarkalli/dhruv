# date

Date toolkit: difference, add/subtract days, age, business days, timezone, unix epoch.

- **Gradle module:** `:apps:finance:feature:date`
- **Owner tab:** none — shell-level detail route
- **Flag:** `date` — **disabled** (code preserved, hidden; re-enable via
  `platform/feature-flags/dhruv-finance.json`)

## Screens
- `DateScreen`

## ViewModels
- `DateViewModel` — pure date math; `AgeResult`, `BusinessDaysResult`, `DateDifferenceResult`

## Data dependencies
- `SettingsRepository` (sub-tool enable preferences) from `:apps:finance:data`.
