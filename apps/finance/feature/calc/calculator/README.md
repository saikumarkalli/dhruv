# calculator

Standard + scientific calculator with history (favourites, tags, notes, recycle bin), live
preview, locale-aware number formatting, and an in-screen AI "explain this calculation" action.

- **Gradle module:** `:apps:finance:feature:calculator`
- **Owner tab:** Calc (the tab's whole surface — keypad + history)
- **Flag:** `calculator` — enabled

## Screens
- `CalculatorScreen`

## ViewModels
- `CalculatorViewModel`

## Data dependencies
- `HistoryRepository` (Room `HistoryEntity`, via `:apps:finance:data`)
- `GeminiRepository` ("explain this calculation")
- `SettingsRepository`
- own `engine/CalculatorEngine` (pure calculation logic)
