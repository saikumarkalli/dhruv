# onboarding

Sign-in, DPDP consent, empty start — everything before the app has a session.

- **Gradle module:** `:apps:finance:feature:onboarding` (physical path
  `apps/finance/feature/onboarding/onboarding/`, remapped in `settings.gradle.kts`).
- **Owner tab:** none — pre-tab, bare full-frame screens shown before the shell exists.
- **Flag:** none — onboarding itself isn't feature-flagged (it gates everything else).
- **Builds in:** design-v1 Phase 1 —
  `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §7.
  This task (Task 2) built the Gradle module and `OnboardingViewModel`. **Screens are not yet
  built — Task 3** (Compose UI for A2/A3/A4, wired to Credential Manager for the Google ID
  token).

## Screens (functional spec §5 Group A)
A2 Sign-in (Google only, + "Use offline — calculators only") · A3 DPDP consent (4 itemised,
revocable switches, 3 of which are toggles) · A4 Empty start (add first account / record what
you own / import CSV). **Not yet built — Task 3.**

## ViewModels
- `OnboardingViewModel` (`src/main/java/com/dhruv/finance/onboarding/OnboardingViewModel.kt`) —
  owns the A2→A3→A4 state machine.
  - `uiState: StateFlow<OnboardingUiState>` — one case per screen (`SignIn`, `Consent`,
    `EmptyStart`).
  - `exitToShell: StateFlow<Boolean>` — separate signal for leaving onboarding entirely (offline
    path, or A3-continue when the user already has tracker data). Not a fourth `OnboardingUiState`
    case because it answers an orthogonal question ("should the host still be showing onboarding
    at all?"); Task 3's host composable is expected to observe it and swap to the shell.
  - `onGoogleIdTokenReceived(idToken: String)`, `onUseOfflineSelected()`,
    `onConsentSwitchToggled(switch: ConsentSwitch, value: Boolean)`, `onConsentContinue()`.
  - A4's exit criterion ("at least one account or one holding exists → Home becomes the landing
    tab") is stubbed `hasAccountOrHolding() = false` — **Phase 2 blocked**, no accounts/holdings
    repository exists yet.
- `OnboardingUiState.kt` — the sealed `OnboardingUiState` + the `ConsentSwitch` enum (the 3
  independently-persisted A3 toggles; the 4th A3 item, data retention/erasure, is informational
  copy only, not a flag).
- `OnboardingConfig.kt` — every A2/A3/A4 copy string (functional spec §5 Group A), so neither the
  ViewModel nor future screens hardcode copy.

## Data dependencies
- `SessionStore` / `ConsentRepository` (Task 1, `:apps:finance:data`) — read/observed directly.
- `AuthRepository` (added by this task, `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/auth/AuthRepository.kt`)
  — thin `GoTrueApi` + `SessionStore` composition (`signInWithGoogleIdToken`), Koin-wired as a
  singleton in `PlatformModule.kt`. Task 1 did not build this interface; it was added here because
  `OnboardingViewModel` needed it and no other consumer existed yet.

## QA scenarios
`apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §2 (`ONB-*`).
Test file: `src/test/java/com/dhruv/finance/onboarding/OnboardingViewModelTest.kt`.

| Row | Status | Test |
|---|---|---|
| ONB-FLOW-001 | 🟢 | `OnboardingViewModelTest.kt:63` `cold install with no session shows SignIn` |
| ONB-FLOW-002 | 🟢 | `OnboardingViewModelTest.kt:95` `sign-in success transitions SignIn to Consent…` |
| ONB-FLOW-003 | 🟢 | `OnboardingViewModelTest.kt:122` `use offline selected exits to shell…` |
| ONB-FLOW-004 | 🟢 (Phase 2 blocked, stub-only) | `OnboardingViewModelTest.kt:192` `EmptyStart reports hasAccountOrHolding false…` |
| ONB-FLOW-005 | ☐ | Task 4 scope (Settings › Privacy + erasure chain) |
| ONB-BR-001 | 🟢 | `OnboardingViewModelTest.kt:80` `sign-in touches only AuthRepository…` |
| ONB-BR-002 | 🟢 | `OnboardingViewModelTest.kt:133` `declining every consent switch still lets Continue proceed…` |
| ONB-BR-003 | 🟢 (Task 1) | `apps/finance/data/.../tracker/net/ConsentInterceptorTest.kt` — Backend/network-layer, nothing to re-test in this ViewModel |
| ONB-BR-004 | 🟢 (Task 1 persistence) + 🟢 (wiring) | Task 1: `ConsentRepositoryTest.kt`; here: `OnboardingViewModelTest.kt:150` / `:172` |
| ONB-BR-005 | 🟢 (Task 1 persistence) + 🟢 (wiring) | same as ONB-BR-004; Settings › Privacy entry point itself is Task 4 |
| ONB-BR-006 | ☐ | Owned by each tracker screen's own ViewModel (Home/Money/Plan-live/Insights), not onboarding |
| ONB-BR-007 | ☐ deferred | Open item — CSV import mapper has no design yet (functional spec §8.1) |
| ONB-BR-008 | ☐ | Task 4 scope (erasure) |
| ONB-BR-009 | ☐ | Task 4 scope (erasure) |

Also see §11 (`DAT-*`, 9 rows) — the auth/consent plumbing (`tracker/net`, `tracker/auth` in
`:apps:finance:data`) this module depends on, built in Task 1.

## Business rules to implement against
Zero tracker network calls before the relevant A3 consent switch is on (BR-001, NFR-1) — enforced
by `ConsentInterceptor`, not by screen-level discipline. Declining sync must leave Calc and the Plan
calculators fully usable. Every consent switch persists immediately via `ConsentRepository`
(`onConsentSwitchToggled` never buffers a value only in `OnboardingViewModel` state) and is
revocable later from Settings › Privacy.
