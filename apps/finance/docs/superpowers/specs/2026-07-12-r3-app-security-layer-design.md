# R3 — Personal-Data Security Layer (App Lock, Privacy Mode, FLAG_SECURE)

> Status: **SPECCED** (build immediately after P1 merges — first phase where real financial data
> renders on screen). Master sequence: `../plans/2026-07-12-master-roadmap-personal-app.md` (R3;
> gaps N1/N2/N3). Design system + playbook binding. Inherits shared invariants (FeatureHost, Koin,
> `:libs:core` components only, TDD, `regressionCheck` gate).
> Decisions proposed here become **ADR-0019 (app lock = system auth only, whole-app scope)** in
> `platform/DECISIONS.md` at implementation time.

## Goal

Protect on-screen financial data with three independent layers: (1) an app lock using system
authentication (biometric Class 3 with device-credential fallback), (2) a global privacy mode that
masks every money value, (3) `FLAG_SECURE` on tracker routes so screenshots and the app-switcher
preview never leak balances. No custom PIN anywhere — the plaintext history-PIN mistake (H7) is not
repeated.

## Non-goals

- No custom PIN/pattern implementation (system `BiometricPrompt` only).
- No per-feature locks (vault-style per-module locking stays a Vault-app concern).
- No change to Supabase session handling — app lock and auth session are independent layers;
  unlocking never refreshes tokens, token expiry never triggers the lock screen.
- History-lock PIN (legacy, calculator) is untouched here; R0 hashes it, this layer supersedes it
  long-term (deprecation note in Settings copy).

## Decisions (proposed)

| # | Decision | Rationale |
|---|----------|-----------|
| D1 | Lock scope = **whole app** once enabled | One gate at the activity level is simpler and safer than route-scoped locking; calculator-only users are unaffected because of D2 |
| D2 | Default **OFF**; after first successful tracker sign-in, one-time prompt suggests enabling | Users who never sign in (pure calculator use) never see a lock |
| D3 | Authenticators, **per API level (SEC1)**: API 30+ = `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` combo; API 26–29 = `BIOMETRIC_STRONG` prompt with negative button "Use screen lock" → `KeyguardManager.createConfirmDeviceCredentialIntent()` (the combo and credential-only modes are unsupported below API 30) | Device credential fallback means a biometric re-enrollment can never lock the user out (ADR-0003 lesson); lockout/backoff handled by the system; both paths in the manual device checklist |
| D4 | Neither biometric nor device credential enrolled → app lock setting disabled with explanation | Never build a custom secret to fill the gap |
| D5 | Privacy mode masks at a single choke point: `MoneyText` in `:libs:core` | Design-system rule already forces all money rendering through `formatPaise*` components; masking lives once |
| D6 | `FLAG_SECURE` is route-scoped via `FeatureHost(secure = true)` | Single existing wrapper touches every route; calculator screens stay screenshot-able |

## Threat model (SEC2 — explicit, so nobody "hardens" this into the re-enrollment trap)

The app lock is a **UI gate against casual device access and shoulder-surfing** — it does not
bind decryption to authentication (no `CryptoObject`). Data at rest is already protected by
platform encryption plus Keystore-wrapped stores; binding keys to biometrics would reintroduce
the ADR-0003 failure mode (re-enrollment invalidates keys → data loss). Consequences: user-facing
copy must never call the lock "encryption"; a process-level attacker (root/forensic) is an
explicit non-goal.

## Architecture

No server schema. No new tracker tables. New dependency: `androidx.biometric:biometric`
(stable AndroidX — allowed class per ADR-0014 §6 reasoning).

| Piece | Detail |
|-------|--------|
| `:libs:core` | `MoneyText` composable (renders `formatPaise`/`formatPaiseCompact` output; reads `LocalHideAmounts`; masked form `₹••••••` with stable width); `LocalHideAmounts` CompositionLocal; `MaskedMoney.mask(formatted: String): String` — pure string transform for **non-Compose surfaces** (notifications R6, Glance widget R8), which read `hideAmounts` themselves and apply it (Compose locals cannot reach them — F3 in the consistency review) |
| `:libs:settings` | `AppSettings` gains `appLockEnabled: Boolean` (default false), `appLockTimeout: LockTimeout` (IMMEDIATE / ONE_MIN default / FIVE_MIN), `hideAmounts: Boolean` (default false); Settings > Security section rows for both |
| `:apps:finance:app` | `AppLockController` (lifecycle observer: records `lastBackgroundedAt` on ON_STOP, exposes `isLocked: StateFlow<Boolean>`); `LockScreen` composable (full-screen scrim + app logo + "Unlock" button → `BiometricPrompt`); shown as overlay above NavHost content when locked (content never composes behind it) |
| Pure logic (TDD) | `LockTimeoutPolicy`: `(lastBackgroundedAt, now, timeout, enabled) -> Boolean` — all lock/no-lock decisions live here, controller stays thin |
| `FeatureHost` | gains `secure: Boolean = false`; when true, a DisposableEffect **increments a window-level secure ref-count** on enter, decrements on dispose — FLAG_SECURE is set while count ≥ 1 (NAV5: during a pager swipe two pages compose at once; last-writer-wins would clear the flag under visible tracker content). Tracker routes (networth, expenses, goals, insurance, retirement, reports, search) pass `secure = true` |

### Lock flow

1. Process start or ON_START: `LockTimeoutPolicy` evaluates → locked ⇒ `LockScreen` overlay
   (window also gets FLAG_SECURE while locked, so the switcher preview is blank).
2. Unlock button → `BiometricPrompt.authenticate()` with
   `setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)`.
3. Success → overlay removed. Error/cancel → stay locked; system handles retry/backoff.
4. Enabling the setting requires one successful authentication first (proves enrollment).
5. **Pending intent extras** (launcher shortcut `QUICK_ADD`, notification `REVIEW_INBOX` etc. —
   the R5b contract): held by `AppLockController` while locked, dispatched to the NavHost only
   after unlock. The lock never drops a navigation intent.

### Privacy mode flow

- Eye toggle in Home top bar + Settings row; persisted (`hideAmounts`).
- `DhruvTheme`-level provider exposes `LocalHideAmounts`; every money render in tracker features
  and Home bento cards uses `MoneyText`. Charts render shapes normally but suppress value labels
  when hidden. Widget (R8) reads the same flag.
- **Carve-out (used by R9):** percentages and ratios stay visible under privacy mode — a % alone
  cannot reconstruct balances; absolute ₹ values always mask.

## Tests

`LockTimeoutPolicyTest` (enabled/disabled, each timeout, clock edge: background→foreground same
instant, process death = locked); `MoneyTextTest` (masked/unmasked, compact + full variants,
stable-width mask); `AppLockControllerTest` (state machine with fake clock, Turbine);
FeatureHost secure-flag param unit-covered via Robolectric window-flag assertion; ArchUnit green.
Manual gate (biometric prompt cannot be automated): checklist — enroll/no-enroll, credential
fallback, re-enrollment survival, switcher preview blank when locked and on secure routes.

## Dependencies

P1 merged (tracker data exists; sign-in event drives D2 prompt). No dependency on R4+.

## UI/UX detail (states per design system)

| Surface | Layout & states |
|---|---|
| `LockScreen` | Full-surface scrim (no content visible), Dhruv logo, "Unlocked with fingerprint or screen lock" caption, primary Unlock button (auto-fires prompt on first show); error text on failed attempt; no back-dismiss |
| Settings > Security | "App lock" switch (disabled + explainer when no enrollment, per D4); timeout selector (segmented, visible only when enabled); "Hide amounts" switch; legacy history-PIN row gains "superseded by App lock" caption |
| Home top bar | Eye / eye-off icon toggling privacy mode; state persists across restarts |
| Enable prompt (one-time) | `DhruvModalSheet` after first tracker sign-in: "Protect your financial data" + Enable / Not now; never re-shown (DataStore flag) |

## Rollout & rollback

No feature flag (app-shell behavior, not a route); kill switch = the setting itself, default off.
Rollback = revert PR; no data migration, no server change. Ships as one PR after P1;
`regressionCheck` + `/dhruv-security` + manual biometric checklist are the merge gates.

## Risks / open questions

- Compose overlay approach must be verified against config change + process death (lock state must
  default to LOCKED on process recreation when enabled) — covered by `LockTimeoutPolicy` default.
- `FLAG_SECURE` also blocks the user's own screenshots on tracker screens — accepted; R7 export/
  reports is the sanctioned way to get data out.
- BiometricPrompt inside a single-activity Compose app requires `FragmentActivity` — MainActivity
  currently extends `ComponentActivity`; migration to `FragmentActivity` is a one-line change,
  verify no Compose interop regression (execution checklist item).

## Execution checklist

- [ ] `LockTimeoutPolicy` + tests (TDD) — `:apps:finance:app`
- [ ] Settings fields + Security section UI (`appLockEnabled`, `appLockTimeout`, `hideAmounts`)
- [ ] `MoneyText` + `LocalHideAmounts` in `:libs:core` + tests; migrate P1 money renders to it
- [ ] `AppLockController` + `LockScreen` + MainActivity→FragmentActivity + prompt wiring
- [ ] `FeatureHost(secure)` + apply to tracker routes + Robolectric flag test
- [ ] One-time enable prompt after sign-in
- [ ] Intent-extra hold/dispatch through lock (test: locked + shortcut → unlock → QuickAddSheet)
- [ ] `MaskedMoney` util + tests (non-Compose masking, F3)
- [ ] Manual biometric checklist on device; `/dhruv-security` + `/dhruv-pre-merge`

## TDD Mandate

> **Test-Driven Development (TDD) is strictly required for this phase.**
> All pure logic, calculators, reducers, and state machines MUST be written with failing tests first, followed by implementation. UI components must be tested for accessibility and rendering states on both Android and Web platforms.

