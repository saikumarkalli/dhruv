# Dhruv Finance — Feature Modules

Reference for all Finance feature modules after the Phase 4 split. Each is a `dhruv.android.library`
+ `dhruv.android.compose` Koin module under `apps/finance/feature/`, namespace `com.dhruv.finance.<name>`,
depending on `:apps:finance:data`, `:libs:core`, `:libs:settings`. Flags live in
`platform/feature-flags/dhruv-finance.json`.

---

### calculator — `:apps:finance:feature:calculator`
Standard + scientific calculator with history (favourites, tags, notes, recycle bin), live preview,
locale-aware number formatting, and an in-screen AI "explain this calculation" action.
- **Screens:** `CalculatorScreen`
- **ViewModels:** `CalculatorViewModel`
- **Data deps:** `HistoryRepository` (Room `HistoryEntity`), `GeminiRepository`, `SettingsRepository`; own `engine/CalculatorEngine`.
- **Flag:** `calculator` — enabled.

### loans — `:apps:finance:feature:loans`
Loan EMI calculator and a side-by-side two-loan comparison (comparison reuses the EMI engine).
- **Screens:** `LoansScreen` (tabs: Loan EMI, Loan Comparison)
- **ViewModels:** `LoansViewModel` (`calculateEmi` + `EmiResult`)
- **Data deps:** none (pure calc); `CurrencyFormatter` from `:data`.
- **Flag:** `loans` — enabled.

### investments — `:apps:finance:feature:investments`
SIP growth, ROI/CAGR, and FD/RD maturity calculators.
- **Screens:** `InvestmentsScreen` (tabs: SIP Growth, ROI / CAGR, FD / RD)
- **ViewModels:** `InvestmentsViewModel` (`calculateSip`, `calculateRoiCagr`, `calculateFdRd`)
- **Data deps:** none (pure calc); `CurrencyFormatter`.
- **Flag:** `investments` — enabled.

### tax — `:apps:finance:feature:tax`
GST/tax add-or-extract and CTC salary breakup (gross, PF, estimated tax, take-home).
- **Screens:** `TaxScreen` (tabs: GST / Tax, Salary Breakup)
- **ViewModels:** `TaxViewModel` (`calculateGst`, `calculateSalaryBreakup`)
- **Data deps:** none (pure calc); `CurrencyFormatter`.
- **Flag:** `tax` — enabled.

### everyday — `:apps:finance:feature:everyday`
Everyday money tools: simple/compound interest, discount/markup, tip & bill split, inflation.
- **Screens:** `EverydayScreen` (tabs: Interest, Discount, Tip Split, Inflation)
- **ViewModels:** `EverydayViewModel` (`calculateSimpleCompound`, `calculateDiscountMarkup`, `calculateTipSplit`, `calculateInflation`)
- **Data deps:** none (pure calc); `CurrencyFormatter`.
- **Flag:** `everyday` — enabled.

### currency — `:apps:finance:feature:currency`
Live currency converter with cached-rate offline fallback and a sync status banner.
- **Screens:** `CurrencyScreen`
- **ViewModels:** `CurrencyViewModel` (`CurrencyStatus` sealed state)
- **Data deps:** `ICurrencyRepository`/`CurrencyRepository` → `CurrencyApi` + Room `CurrencyRateEntity` (external exchange-rate APIs).
- **Flag:** `currency` — enabled.

### unit — `:apps:finance:feature:unit`
Length and mass unit conversions.
- **Screens:** `UnitScreen` (tabs: Length, Mass)
- **ViewModels:** `UnitViewModel`; own `UnitConverter`/`LengthUnit`/`MassUnit`.
- **Data deps:** `CurrencyFormatter` (formatting only).
- **Flag:** `unit` — enabled.

### date — `:apps:finance:feature:date`
Date toolkit: difference, add/subtract days, age, business days, timezone, unix epoch.
- **Screens:** `DateScreen`
- **ViewModels:** `DateViewModel` (pure date math; `AgeResult`, `BusinessDaysResult`, `DateDifferenceResult`)
- **Data deps:** `SettingsRepository` (sub-tool enable prefs).
- **Flag:** `date` — **disabled** (code preserved, hidden; re-enable via flag).

### time — `:apps:finance:feature:time`
Stopwatch and countdown timer.
- **Screens:** `TimeScreen` (+ `stopwatch/`, `timer/` sub-screens)
- **ViewModels:** `TimeViewModel`, `StopwatchViewModel`, `TimerViewModel`
- **Data deps:** none (in-memory state only).
- **Flag:** `time` — **disabled** (code preserved, hidden).

### assistant — `:apps:finance:feature:assistant`
Standalone online AI assistant. Shows a DPDP consent gate before any Gemini call.
- **Screens:** `AssistantScreen` (+ `AssistantUiState`)
- **ViewModels:** `AssistantViewModel`
- **Data deps:** `GeminiRepository` (from `:data`; online Gemini API).
- **Flag:** `assistant` — `enabled = true` but **gated to `minVersion 1.2.0`** (current `versionName` is `2.0.2`, so already visible) and `requiresConsent: true` (consent gate in `AssistantScreen` before any Gemini call).

### networth — `:apps:finance:feature:networth`
Net worth tracker: assets, liabilities, valuations. Supabase-primary (ADR-0014). Google sign-in
via Credential Manager. DPDP consent gate before any network call.
- **Screens:** planned per P1 spec (`2026-07-03-p1-networth-tracker-design.md`)
- **ViewModels:** planned
- **Data deps:** Supabase REST (PostgREST + GoTrue via Retrofit/Moshi/OkHttp); no Room for tracker data.
- **Flag:** `networth` — `enabled = true`, `requiresConsent: true`. Module scaffolded but screens not yet implemented (R2 in the master roadmap).

---

## Design system

All feature screens use the **DhruvNext design system** (ADR-0024). Token usage:
- Colors: `LocalDhruvNextColors.current` (`acc`, `surf`, `tx`, `tx2`, `tx3`, `line`, `neg`, `pos`, etc.)
- Typography: `DhruvNextType.*` (`hero`, `title`, `cardTitle`, `body`, `meta`, `sectionLabel`)
- Spacing: `DhruvNextSpacing.*` (`screenGutter`, `interCardGap`, `sectionGap`, `cardPadding`, `inputGroupGap`)
- Radii: `DhruvNextRadii.*` (`card`, `listGroup`, `innerTile`, `pill`)
- Components: `NxCard`, `NxButton`, `NxTextField`, `SegmentedRow`, `SectionLabel`, `ListGroup`, `StatDeltaChip`, etc.

Zero `MaterialTheme.colorScheme` / `MaterialTheme.typography` refs remain in any screen file.
`CardDefaults` usage in date sub-views is intentional (accent-tinted `accSoft` result cards).
