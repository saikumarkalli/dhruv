# unit

Length and mass unit conversions.

- **Gradle module:** `:apps:finance:feature:unit`
- **Owner tab:** none — shell-level detail route (reached from Calc's title bar)
- **Flag:** `unit` — enabled

## Screens
- `UnitScreen` (tabs: Length, Mass)

## ViewModels
- `UnitViewModel`; own `UnitConverter`/`LengthUnit`/`MassUnit`

## Data dependencies
- `CurrencyFormatter` from `:apps:finance:data` (display formatting only).
