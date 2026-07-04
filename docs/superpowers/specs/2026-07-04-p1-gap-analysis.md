# P1 Net Worth Tracker — End-to-End Gap Analysis (audit trail)

> Status: register CLOSED — every gap has a decided fix folded into the P1 plan/spec before
> development started (one-go rule). Outcome column filled at PR time.
> Method: full-plan adversarial review + codebase fact-verification (2026-07-04).

## Critical (would have broken build/CI/feature)

| # | Gap (evidence) | Fix | Outcome |
|---|----------------|-----|---------|
| G1 | **minVersion trap**: `gradle.properties` `VERSION_NAME=1.2.7` (CI-owned, bumps only on merge); flag `networth.minVersion=1.3.0` would gate the feature OFF in every local/PR build | Flag set to `minVersion "1.0.0"` (keeps `requiresConsent`) | pending |
| G2 | **Coverage aggregation hardcoded**: root `build.gradle.kts` `coveredModules` list does not auto-discover modules — networth would be silently skipped by regressionCheck/JaCoCo | Add `":apps:finance:feature:networth"` to `coveredModules` | pending |
| G3 | **Assistant consent not persisted** (in-memory `MutableStateFlow(ConsentNeeded)`, `AssistantViewModel.kt:19,44` — re-asks every launch). "Copy assistant pattern" would have copied a broken pattern | Networth consent = persisted `SettingsRepository` DataStore boolean; revocable. Assistant fix = follow-up | pending |
| G4 | **Secrets mechanism is the `secrets` gradle plugin + `.env`** (gitignored; `.env.example` committed), not local.properties as first planned; CI release job injects only keystore vars — release APK would ship dead tracker config | `.env`/`.env.example` carry the 3 new keys; ci.yml release job writes `.env` from GitHub secrets; blank config → NotConfigured UI state (no crash) | pending |
| G5 | **R8**: release is minified; proguard has Moshi/Room/Koin/Gemini rules only | Moshi/Retrofit rules already cover the REST stack (post-R3 decision); add DTO package + credentials/googleid keeps; release assemble smoke in gate | pending |

## Functional (missing states/rules)

| # | Gap | Fix | Outcome |
|---|-----|-----|---------|
| G6 | Session tokens must not sit in plaintext prefs | Own session store over encrypted DataStore | pending |
| G7 | Token expiry/refresh: 401 must map to SignedOut, not error-loop | Interceptor refresh-rotation; refresh failure → SignedOut; tests | pending |
| G8 | Consent withdrawal missing (DPDP requires revocable) | "Withdraw consent" persists false + signs out + regates; test | pending |
| G9 | No offline/not-configured distinction | `DashboardUiState` gains Offline + NotConfigured; tests | pending |
| G10 | Future-dated valuations unvalidated | `recordedAt <= now` rule; editor + test | pending |
| G11 | Double-save race + refresh race | Save disabled in-flight; repo Mutex single-flight; tests | pending |
| G12 | Month-delta timezone undefined | Device-timezone calendar month; explicit test | pending |
| G13 | Compact INR display missing (hero needs ₹4.8L; no lakh/crore formatter in repo) | `formatPaiseCompact` + tests | pending |
| G14 | Client-side user_id/updated_at bookkeeping error-prone | SQL: `default auth.uid()` + `moddatetime` triggers — client never sends either | pending |

## Consistency (nav/settings/docs)

| # | Gap | Fix | Outcome |
|---|-----|-----|---------|
| G15 | Settings per-section colors + enable toggles reference sections that move into Tools | ToolsHub card visibility = flag AND settings toggle; Settings copy "shown in Tools"; colors keep working via per-tool `SectionTheme` | pending |
| G16 | Back-nav contract: page 0 becomes Home; `settings` reached via top bar only | Preserved; nested BackHandlers enabled only when own tab current + sub-screen open; testTags kept | pending |
| G17 | Screenshot tests not viable (Roborazzi `@Ignore`d — native graphics unavailable) | No screenshot tests in P1 gate; UX review covers visuals | pending |
| G18 | PRODUCTION_READINESS.md / IMPLEMENTATION.md unaware of pivot | Updated in docs task | pending |
| G19 | Version catalog lacked supabase-kt stack entries | Resolved by R3: supabase-kt dropped; only androidx credentials + googleid added | pending |
| G20 | Valuation fetch-all won't scale forever | Accepted P1 (personal dataset); windowed fetch = P6 revisit | pending |

## Risk resolutions (decided at plan time)

| # | Risk | Resolution |
|---|------|-----------|
| R1 | Online-only tracker | Accepted by design (U7); Offline = specced state; P6 cache revisit |
| R2 | Google OAuth SHA-1 friction | M0 blocking device-auth milestone + `signingReport` checklist — fails before any UI exists |
| R3 | supabase-kt/Ktor AGP-9 unknown | **Eliminated**: REST on existing Retrofit/Moshi/OkHttp; only stable AndroidX additions |
| R4 | Cert pinning vs rotation | CA-level pins (ISRG Root X1+X2), not leaf — survives Supabase rotations |
| R5 | Coverage floor dilution | Pure-logic tests land first; floor untouched (ADR-0013 ratchet) |
| R6 | DPDP full erasure | `delete_my_account()` security-definer RPC — rows + auth user, in-app, no service-role on device |
