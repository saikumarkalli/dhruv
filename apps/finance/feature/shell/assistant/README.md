# assistant

Standalone online AI assistant. Shows a DPDP consent gate before any Gemini call.

- **Gradle module:** `:apps:finance:feature:assistant`
- **Owner tab:** none — shell-level detail route (reached via the floating Ask pill on
  Home/Plan/Insights)
- **Flag:** `assistant` — `enabled = true` but **gated to `minVersion 1.2.0`** (current
  `versionName` is `2.0.2`, so already visible) and `requiresConsent: true` (consent gate lives in
  `AssistantScreen`, checked before any Gemini call).

## Screens
- `AssistantScreen` (+ `AssistantUiState`)

## ViewModels
- `AssistantViewModel`

## Data dependencies
- `GeminiRepository` from `:apps:finance:data` (online Gemini API).

## Known follow-up
The consent here is currently in-memory (forgets on restart) — ADR-0014 §7 tracks fixing this to
match the persisted, revocable `SettingsRepository`-backed pattern Phase 1's onboarding consent
switches will establish.
