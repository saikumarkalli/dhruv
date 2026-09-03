---

description: "Task list for 008 User Login — password + Google, email OTP, own profile storage"
---

# Tasks: User Login — Password + Google, Email OTP, Own Profile Storage

**Input**: Design documents from `apps/finance/specs/008-user-login/`

**Prerequisites**: [plan.md](plan.md) · [spec.md](spec.md) · [research.md](research.md) ·
[data-model.md](data-model.md) · [contracts/](contracts/) · [quickstart.md](quickstart.md)

**Tests**: **MANDATORY, not optional.** Constitution Article I (Test-First) is marked
NON-NEGOTIABLE and Article II requires QA scenario rows to exist *before* any Backend or Android
task starts. Every test cites its `AUTH-*` scenario ID. The template's "tests are optional" default
does not apply in this repo.

**Organization**: Grouped by user story. The plan's phase letters (A–G) are annotated per phase so
the two documents read together.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: parallelizable — different files, no dependency on an incomplete task
- **[Story]**: US1–US7 per spec.md
- Exact file paths in every description

## Path conventions

- Backend/SQL: `supabase/schemas/`, `supabase/migrations/`, `supabase/verification/`
- Data layer: `apps/finance/data/src/main/java/com/dhruv/finance/data/`
- Onboarding: `apps/finance/feature/onboarding/onboarding/src/main/java/com/dhruv/finance/onboarding/`
- Settings/shell: `apps/finance/app/src/main/java/com/dhruv/finance/app/`
- Design system: `libs/core/src/main/kotlin/com/dhruv/core/ui/components/`

---

## Phase 1: Setup — platform prerequisites *(plan Phase A)*

**Purpose**: credentialed, mostly-manual actions. Nothing downstream is testable until these land.

**⚠️ T001 is the hard blocker**: Supabase's default mail service sends **2 emails/hour**. Without a
custom SMTP provider the OTP feature is not degraded — it is inoperable, and the first validation run
will appear to pass while every later one silently fails.

- [ ] T001 Configure a custom SMTP provider on `dhruv-dev` and `dhruv-prod` (Supabase Auth settings), and record the choice in `scripts/env/README.md` as a standing runbook step
- [ ] T002 [P] Edit the **Confirm signup** and **Reset password** email templates to use `{{ .Token }}` instead of `{{ .ConfirmationURL }}` on both projects
- [ ] T003 [P] Set OTP expiry to 10 minutes (default is 1 hour) and confirm the 60s resend cooldown on both projects
- [ ] T004 [P] Enable manual identity linking (beta flag) on both projects — required by FR-036
- [ ] T005 Re-run `grep '^## ADR-' platform/DECISIONS.md` to confirm the next free ADR number, then write **ADR-0037** (`identity` Postgres schema for cross-app profile data) into `platform/DECISIONS.md`
- [ ] T006 Add `"identity"` to `[api] schemas` in `supabase/config.toml`
- [ ] T007 Patch the hosted Data API `db_schema` to include `identity` on **`dhruv-dev` and `dhruv-prod`** via the Management API — `config.toml` does not drive this (ADR-0033's 2026-09-03 correction). Also patch `dhruv-prod` for `finance`, still outstanding from that correction
- [ ] T008 [P] Create the private `avatars` Storage bucket on both projects
- [ ] T009 **Decide open item O2**: build Phase 10 (Edge Function for username sign-in + lockout), or narrow FR-002/FR-040. Record the decision in spec.md's Clarifications section — a narrowing is a spec amendment, never a silent drop

**Checkpoint**: an OTP email arrives in a real inbox; `curl` with `Accept-Profile: identity` returns
`401`, not `406` (see quickstart.md Step 0).

---

## Phase 2: Foundational — schema, scenarios, shared client wiring *(plan Phases B + C1/C3/C5)*

**Purpose**: everything every user story depends on.

**⚠️ CRITICAL**: No user story work begins until this phase completes. T010–T012 (QA rows) precede
all code per Article II.

### QA scenarios first (Article II)

- [ ] T010 Write `AUTH-*` scenario rows covering FR-001 through FR-040 in `apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md`
- [ ] T011 Amend the existing `ONB-FLOW-001`/`ONB-FLOW-002`/`ONB-BR-001` rows in the same catalog to describe the FR-023a flow order, not the current Google-only one
- [ ] T012 Create `apps/finance/specs/008-user-login/checklists/qa.md` indexing the `AUTH-*` rows with their status column

### Schema + RLS (Article IXa)

- [ ] T013 Create `supabase/schemas/identity/00_schema.sql` — `create schema identity` + `grant usage on schema identity to authenticated`
- [ ] T014 Create `supabase/schemas/identity/10_tables/profiles.sql` — table per data-model.md, RLS select/insert/update own, **no DELETE policy**, explicit `grant select, insert, update … to authenticated`, no `anon` grant
- [ ] T015 Generate the migration via `supabase db diff -f identity_profiles`, then **hand-append the grants** (`db diff` does not emit them) and read the generated SQL back before committing
- [ ] T016 [P] Add Storage RLS policies on the `avatars` bucket scoping read/write to the caller's own `{auth.uid()}/` path prefix
- [ ] T017 Amend `supabase/schemas/public/30_functions/delete_my_data.sql` and `delete_my_account.sql` to remove the profile row **and the stored avatar object** (FR-018), as `create or replace function` — never drop+recreate
- [ ] T018 **VERIFY-AT-RED (open item O4)**: write a failing check proving whether a Storage object can be deleted from SQL, or whether a client call must precede the RPC. Record the answer in research.md and adjust T017 accordingly
- [ ] T019 Write `supabase/verification/auth_identity_rls.sql` — a second signed-in user reads **zero** rows from `identity.profiles` and cannot fetch another person's avatar object
- [ ] T020 Apply the migration to `dhruv-dev` and run T019 against it
- [ ] T021 [P] Regenerate `supabase/SCHEMA.md` via `scripts/db/gen_schema_docs.py`
- [ ] T022 [P] Update the `supabase gen types` invocations in `supabase/migrations/README.md` and `web/src/shared/types/database.ts`'s freshness check to `--schema public,finance,identity`

### Shared client wiring

- [ ] T023 Generalise `FinanceSchemaInterceptor` into a schema-parameterised `SchemaInterceptor` in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/net/`, preserving the existing `finance` behaviour for `dataRetrofit`
- [ ] T024 **RED**: write `SupabaseClientFactoryTest` cases asserting `identityRetrofit` carries **no** `ConsentInterceptor` and `dataRetrofit` still does — the single most important test in this feature (see plan.md Risks)
- [ ] T025 Add `identityRetrofit` to `SupabaseClientFactory.kt` on the **`authClient`** chain with an `identity` schema header, making T024 pass
- [ ] T026 **RED**: write failing `GoTrueApi` contract tests for `signup`, `token?grant_type=password`, `verify`, `recover`, `PUT /user`, `logout` per `contracts/gotrue-endpoints.md`
- [ ] T027 [P] Add request/response DTOs for the above to `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/dto/`
- [ ] T028 Extend `GoTrueApi.kt` with the six endpoints, making T026 pass
- [ ] T029 Extend `ErrorMapper` so unknown-email, wrong-password and locked-account all produce **one generic message** (FR-004, FR-040d, SC-005)
- [ ] T030 Register the new APIs and repositories in `apps/finance/app/src/main/java/com/dhruv/finance/app/di/PlatformModule.kt`

**Checkpoint**: `:apps:finance:data:testDebugUnitTest` green; T019 passes; foundation ready.

---

## Phase 3: User Story 1 — Password sign-up and sign-in (Priority: P1) 🎯 MVP

**Goal**: a person creates an account with email + password and signs back in with it.

**Independent Test**: with email confirmation temporarily **off** on `dhruv-dev`, sign up with a new
email, sign out, sign back in. Delivers a working account reachable without Google. US2 then turns
confirmation on — that ordering is what keeps these two P1 stories genuinely independent rather than
one being a hidden prerequisite of the other.

### Tests (write first, must fail)

- [ ] T031 [P] [US1] **RED**: `AuthRepositoryTest` cases for `signUpWithPassword` — success, email-already-registered, weak-password, in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/auth/`
- [ ] T032 [P] [US1] **RED**: `AuthRepositoryTest` cases for `signInWithPassword` — success, wrong password, unknown email (both failures produce the **same** generic error)
- [ ] T033 [P] [US1] **RED**: `OnboardingViewModelTest` cases for the sign-up and password sign-in state transitions

### Implementation

- [ ] T034 [US1] Add `signUpWithPassword(email, password, username?)` and `signInWithPassword(identifier, password)` to `AuthRepository.kt` + `AuthRepositoryImpl`, preserving the shipped `CancellationException`-rethrow pattern (a bare `runCatching` breaks structured concurrency — already fixed once in this file)
- [ ] T035 [US1] Route the identifier: send it as `email` only when it parses as one — username sign-in needs Phase 10 (research R5a)
- [ ] T036 [US1] Extend `OnboardingUiState.kt` with `SignUp` and password-sign-in states
- [ ] T037 [US1] Extend `OnboardingViewModel.kt` with `onSignUpSubmitted` / `onPasswordSignInSubmitted`, making T033 pass
- [ ] T038 [US1] Create `SignUpScreen.kt` in the onboarding module — email, optional username, password; tokens and `:libs:core` components only
- [ ] T039 [P] [US1] Add all sign-up copy to `OnboardingConfig.kt` and every user-visible string to `strings.xml` (FR-028 — never a literal in a screen)
- [ ] T040 [US1] Client-side password-strength pre-check so FR-006's rejection message is explanatory, with the server remaining authoritative

**Checkpoint**: password sign-up and sign-in work end to end with confirmation off.

---

## Phase 4: User Story 2 — Email OTP verification (Priority: P1) 🎯 MVP

**Goal**: prove email ownership with a 6-digit code at sign-up, and gate password reset behind one.

**Independent Test**: turn email confirmation on; sign up and confirm the OTP screen blocks entry
until a correct code is entered; separately run forgot-password end to end.

### Tests (write first, must fail)

- [ ] T041 [P] [US2] **RED**: `AuthRepositoryTest` cases for `verifyOtp` — correct, incorrect, expired
- [ ] T042 [P] [US2] **RED**: `AuthRepositoryTest` cases for `requestPasswordReset` and `resendOtp` (cooldown, newest-code-wins)
- [ ] T043 [P] [US2] **RED**: `OnboardingViewModelTest` cases for the pending-verification resume path (FR-001g)

### Implementation

- [ ] T044 [P] [US2] Build `PinEntry` in `libs/core/src/main/kotlin/com/dhruv/core/ui/components/PinEntry.kt` — design system §5.2 batch B2, in `:libs:core`, **never** in a feature module (Article VI)
- [ ] T045 [US2] Move `PinEntry`'s row from §5.2 to §5.1 in `platform/DESIGN-SYSTEM.md` in the same change that builds it — nothing is listed as built before the code exists, and nothing built stays listed as planned
- [ ] T046 [US2] Add `verifyOtp(email, token, type)`, `resendOtp(email)` and `requestPasswordReset(email)` to `AuthRepository.kt`, making T041/T042 pass
- [ ] T047 [US2] Derive pending-verification state from `email_confirmed_at` being null on an active session, and resume at the OTP step on next app open (FR-001g), making T043 pass
- [ ] T048 [US2] Create `OtpVerifyScreen.kt` — `PinEntry`, resend button with cooldown, attempt counter
- [ ] T049 [US2] Distinguish **expired** from **incorrect** in the UI, tracking local issue time if the server returns one undifferentiated error (FR-001d — the requirement is that the *person* can tell)
- [ ] T050 [US2] Surface send-failure with a retry rather than leaving the person waiting on a code that is not coming (FR-001h)
- [ ] T051 [P] [US2] Create `ForgotPasswordScreen.kt` — always the same confirmation whether or not the address exists (FR-004)
- [ ] T052 [US2] Create `ResetPasswordScreen.kt`, reachable only after `verify(type=recovery)`
- [ ] T053 [P] [US2] OTP and reset copy into `OnboardingConfig.kt` + `strings.xml`
- [ ] T054 [US2] Turn email confirmation **on** for `dhruv-dev` and re-run Phase 3's checks to confirm US1 still passes with OTP in the path

**Checkpoint**: sign-up requires a verified email; password reset works end to end.

---

## Phase 5: User Story 3 — Google sign-in unchanged (Priority: P1)

**Goal**: Google sign-in keeps working exactly as today, alongside password sign-in, and seeds the
profile once.

**Independent Test**: complete Google sign-in and confirm the person lands signed in exactly as
before, with a profile row created from the Google-supplied values.

### Tests (write first, must fail)

- [ ] T055 [P] [US3] **RED**: regression test asserting `signInWithGoogleIdToken`'s behaviour and its `ONB-BR-001` pre-consent guarantee are unchanged by this feature
- [ ] T056 [P] [US3] **RED**: test that a first Google sign-in seeds the profile and a **later** Google sign-in does **not** overwrite an edited name/photo (FR-015 vs FR-016)

### Implementation

- [ ] T057 [US3] Add `seedFromGoogleIfAbsent(name, avatarUrl)` to `ProfileRepository`, writing only when no profile row exists, making T056 pass
- [ ] T058 [US3] Call it once on Google sign-in success in `OnboardingViewModel.kt`
- [ ] T059 [US3] Confirm no OTP step is inserted into the Google path (FR-038) with an explicit test

**Checkpoint**: both P1 sign-in methods work; neither regressed the other.

---

## Phase 6: User Story 4 — Session persistence (Priority: P2)

**Goal**: stay signed in across restarts for both methods; silent refresh; clean sign-out.

**Independent Test**: sign in each way, force-quit, reopen, expire the access token, confirm silent
refresh; then sign out — including while offline.

### Tests (write first, must fail)

- [ ] T060 [P] [US4] **RED**: `SessionStoreTest` cases proving a password-account session persists and restores identically to a Google one
- [ ] T061 [P] [US4] **RED**: `AuthInterceptorTest` case confirming the existing single-retry-then-`SignedOut` behaviour (`DAT-BR-003`) covers the new endpoints without per-call retry logic
- [ ] T062 [P] [US4] **RED**: test that sign-out clears the local session even when the network call fails

### Implementation

- [ ] T063 [US4] Add `signOut()` to `AuthRepository.kt` calling `POST /logout`, then clearing `SessionStore` **unconditionally** — a person tapping sign-out offline must still end up signed out (T062)
- [ ] T064 [US4] Confirm `SessionStore` needs no schema change for password accounts (it already persists `display_name`/`avatar_url`); document the finding in the Implementation record rather than changing it speculatively

**Checkpoint**: session behaviour is identical across both sign-in methods.

---

## Phase 7: User Story 5 — Own profile, independent of Google (Priority: P2)

**Goal**: display name and photo stored by Dhruv, surviving reinstall, editable, never overwritten.

**Independent Test**: create a password account, set name and photo, reinstall, sign back in, and
confirm both survive with no Google account involved.

### Tests (write first, must fail)

- [ ] T065 [P] [US5] **RED**: `ProfileRepositoryTest` — read own profile, empty result renders placeholder rather than an error (FR-014)
- [ ] T066 [P] [US5] **RED**: `ProfileRepositoryTest` — upsert name, upsert username, `409` maps to a "username taken" result (FR-005)
- [ ] T067 [P] [US5] **RED**: `ProfileRepositoryTest` — FR-013c: no financial field can be written to the profile record
- [ ] T068 [P] [US5] **RED**: avatar upload/fetch/delete against a fake, including oversized and unsupported-format rejection **before** upload

### Implementation

- [ ] T069 [P] [US5] Create `ProfileApi.kt` in `apps/finance/data/src/main/java/com/dhruv/finance/data/identity/` per `contracts/profile-api.md`
- [ ] T070 [P] [US5] Create `AvatarStorageApi.kt` in the same package — upload, signed-URL fetch, delete
- [ ] T071 [US5] Create `ProfileRepository.kt` + impl exposing `profile: StateFlow<Profile?>` and the methods in `contracts/profile-api.md`, making T065–T068 pass
- [ ] T072 [US5] Check username availability at **write time** via the `409`, not a pre-flight lookup — do **not** add a `security definer` availability RPC (it would be an `anon`-callable username-enumeration endpoint, research R5a)
- [ ] T073 [US5] Create `ProfileSetupScreen.kt` in onboarding — **skippable** (FR-023b), `InitialsTile` placeholder when no photo
- [ ] T074 [P] [US5] Profile copy into `OnboardingConfig.kt` + `strings.xml`
- [ ] T075 [US5] Verify erasure removes the profile row **and** the avatar object end to end (FR-018) — the row alone leaves orphaned personal data outliving a deleted account

**Checkpoint**: profile survives reinstall; edits are never overwritten by Google.

---

## Phase 8: User Story 6 — One consistent, onboarding-integrated experience (Priority: P2)

**Goal**: one sign-in screen offering both methods, the FR-023a flow order, the offline path intact,
and every new screen indistinguishable in style from the rest of the app.

**Independent Test**: walk a fresh install through onboarding in both light and dark mode; confirm
both sign-in options appear together, the offline option still works, and no screen looks bolted on.

### Tests (write first, must fail)

- [ ] T076 [P] [US6] **RED**: `OnboardingViewModelTest` asserting the FR-023a order — sign-in/up → OTP → consent → skippable profile setup
- [ ] T077 [P] [US6] **RED**: test that "Use offline — calculators only" still exits to the shell, skipping OTP and profile setup (FR-031/FR-023c). **Highest-value test in this phase** — the offline option is the easiest thing to lose while rewriting the sign-in screen
- [ ] T078 [P] [US6] Update the existing `ONB-FLOW-001`/`ONB-FLOW-002` tests in `OnboardingViewModelTest.kt`, which currently assert a flow FR-023a changes

### Implementation

- [ ] T079 [US6] Rewrite `SignInScreen.kt` as one dual-method screen — identifier + password, "Sign in with Google", "Create an account", **and the existing offline option** (FR-022/FR-031)
- [ ] T080 [US6] Wire the full route set per `contracts/routes.md` into the onboarding host, making T076/T077 pass
- [ ] T081 [US6] Wrap every new route in `FeatureHost` (Article IV, FR-029)
- [ ] T082 [US6] Apply N4 confirm-on-discard to `signup`, `account/profile` and `account/password` (FR-030)
- [ ] T083 [P] [US6] Define loading / error / offline / signed-out states on every new screen using the existing state components (FR-026)
- [ ] T084 [P] [US6] Accessibility pass on every new screen — ≥48dp targets, contrast in both themes, `contentDescription` on icon-only actions, no colour-only meaning (FR-027)
- [ ] T085 [US6] Audit every new screen for raw hex/dp/sp literals and hardcoded strings; screen-level data into `*Config.kt` (Article V, FR-024/FR-028)

**Checkpoint**: first-run flow is one continuous, consistent path; offline users lost nothing.

---

## Phase 9: User Story 7 — Manage the account from Settings (Priority: P2)

**Goal**: edit profile, change password, view and add sign-in methods — all from Settings › Account.

**Independent Test**: sign in, open Settings › Account, edit name and photo, change the password and
confirm the new one works, then add the second sign-in method and sign in with it.

### Tests (write first, must fail)

- [ ] T086 [P] [US7] **RED**: `AccountSettingsViewModelTest` cases for profile edit, change password, and linked-methods listing
- [ ] T087 [P] [US7] **RED**: test that changing the password invalidates other sessions but keeps **this** device signed in (FR-034/FR-008)
- [ ] T088 [P] [US7] **RED**: test asserting GoTrue refuses unlinking the last identity (FR-037) — **assert the server rule, do not re-implement it client-side** (research R4)
- [ ] T089 [P] [US7] **VERIFY-AT-RED (open item O3)**: failing test establishing whether `PUT /user {password}` alone gives a Google-only account a working password identity, or manual linking is also required. Record the answer in research.md

### Implementation

- [ ] T090 [US7] Add `changePassword`, `linkGoogle`, `setPassword` and `listIdentities` to `AuthRepository.kt`, making T086–T089 pass
- [ ] T091 [P] [US7] Create `ProfileEditScreen.kt` in `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/` (FR-033)
- [ ] T092 [P] [US7] Create `ChangePasswordScreen.kt` in the same package (FR-034)
- [ ] T093 [US7] Create `LinkedMethodsScreen.kt` — link, and offer unlink only when it is not the last method (FR-035/FR-036/FR-037)
- [ ] T094 [US7] Duplicate the Credential Manager call for Google linking rather than importing from `com.dhruv.finance.onboarding` — `feature → feature` is forbidden (Article III), same precedent as `SET-ARCH-003`
- [ ] T095 [US7] Hide the change-password row entirely on a Google-only account and offer "Set a password" in `account/methods` instead — a visible-but-broken row is worse than an absent one
- [ ] T096 [US7] Register all three rows through the Settings contribution mechanism with `single(qualifier = named(moduleKey))` — the qualifier is required; without it the rows are silently absent from Settings (FR-039)
- [ ] T097 [P] [US7] All Settings strings into `strings.xml` — 004-settings 0b.5 already had to retrofit 11 hardcoded strings on this exact screen

**Checkpoint**: full account self-service; no path requires support contact or reinstall.

---

## Phase 10: Username sign-in + account lockout *(plan Phase F — gated on T009)*

**⚠️ Only if T009 decided to build it.** Everything above works without this phase. If T009 narrowed
scope instead, **skip to Phase 11 and amend spec.md** — FR-002 becomes email-only sign-in with
username as a display handle, FR-040 falls back to GoTrue's built-in rate limiting, and both are
recorded in the Implementation record (Article Xa), never silently dropped.

Both requirements need a server-side sign-in path: GoTrue's password grant has no username field and
no per-account lock-until-reset, and a client-side failure counter is reset by clearing app data.

- [ ] T098 Create `supabase/schemas/identity/10_tables/auth_lockouts.sql` per data-model.md — RLS enabled, **no policies and no grants**, so it is unreachable via PostgREST by design
- [ ] T099 Create the `supabase/functions/auth-signin/` Edge Function — resolve identifier → email (service role; **the email never leaves the function**), check lockout, call the password grant, record the outcome, lock at threshold, return one generic failure shape
- [ ] T100 Send the FR-040b lockout notification email exactly once per lock, guarded by `notified_at`
- [ ] T101 Ensure the lock applies to password sign-in only — Google sign-in on the same account keeps working (FR-040c)
- [ ] T102 Switch the client's password sign-in call to the function endpoint in `AuthRepositoryImpl`, leaving the Google path untouched
- [ ] T103 Enable username sign-in in `SignInScreen.kt` now that resolution exists (FR-002)
- [ ] T104 Add Edge Function deployment to `.github/workflows/supabase-migrate.yml` — dev-auto, prod-gated, matching the migration path
- [ ] T105 **Negative test**: assert no client-callable endpoint maps a username to an email address. If one exists, the implementation diverged from research R5a and is a PII-harvesting surface

**Checkpoint**: username sign-in and lockout work, with no email-enumeration surface.

---

## Phase 11: Polish & closure *(plan Phase G)*

- [ ] T106 Execute every `AUTH-*` scenario row and close it, or defer it with a stated reason — never silently drop one
- [ ] T107 Run `/dhruv-security`. It **must** land on the two accepted risks the spec's clarification recorded: profile data outside the consent gate (lawful only if FR-013b's disclosure copy actually shipped and FR-013c holds), and the deliberate lockout DoS window
- [ ] T108 **Verify FR-013b's disclosure copy is really present** in the consent/privacy screen — if it was cut during implementation, the profile design is non-compliant, not merely undocumented
- [ ] T109 Run `/dhruv-ui-review` across all new screens
- [ ] T110 Run `/dhruv-boundaries` to confirm no `feature → feature` edge was introduced by the Settings linking work
- [ ] T111 Run `./gradlew regressionCheck` and raise the JaCoCo coverage floor **at this checkpoint only**, never ahead of landed tests (Article X)
- [ ] T112 Work through `quickstart.md` Steps 0–8 (and 9 if Phase 10 shipped) on a real device with a real inbox
- [ ] T113 [P] Update `apps/finance/FEATURES.md` — onboarding module row moves to its shipped state
- [ ] T114 [P] Update `apps/finance/feature/onboarding/onboarding/README.md` — real screens, ViewModels, data dependencies; drop the "not yet built" preamble
- [ ] T115 [P] Add a `CHANGELOG.md` entry under the `finance-*` heading
- [ ] T116 [P] Add route and settings rows to `apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md`
- [ ] T117 Write spec.md's **Implementation record** — what shipped, what deviated and why, what was deferred and to where (Article Xa)
- [ ] T118 Finalise ADR-0037 in `platform/DECISIONS.md` with the as-built consequences

---

## Dependencies & execution order

### Phase dependencies

- **Phase 1 (Setup)**: no dependencies. **T001 blocks everything email-shaped.**
- **Phase 2 (Foundational)**: depends on Phase 1. **Blocks all user stories.** T010–T012 precede all code (Article II).
- **Phases 3–9 (stories)**: all depend on Phase 2.
- **Phase 10**: gated on T009; depends on Phase 2 only.
- **Phase 11**: depends on every shipped story.

### Story dependencies

```text
Phase 1 ──> Phase 2 ──┬──> US1 (P1) ──> US2 (P1) ──┐
                      ├──> US3 (P1) ───────────────┤
                      ├──> US4 (P2) ───────────────┼──> US6 (P2) ──> Phase 11
                      ├──> US5 (P2) ──────────────┬┘        ▲
                      │                           └──> US7 (P2)
                      └──> Phase 10 (gated) ───────────────────────> Phase 11
```

- **US1 → US2**: US1 is validated with email confirmation **off**; US2 turns it on. Sequenced, not
  coupled — that ordering is what keeps both P1 stories independently testable.
- **US5 → US7**: Settings' profile edit consumes `ProfileRepository` from US5.
- **US6 depends on US1/US2/US5's screens existing** — it is the integration and consistency pass over
  them, not a separate set of screens.
- **US3, US4** are independent of everything after Phase 2.

### Within each story

Tests are written and **must fail** before implementation. Then: DTOs → API → repository → ViewModel
→ screen → copy/strings.

### Parallel opportunities

- T002, T003, T004, T008 (Phase 1 console work)
- T016, T021, T022 (Phase 2, after the migration lands)
- All `[P]`-marked RED tests within a story
- **US3, US4, US5 can run fully in parallel** once Phase 2 completes
- US7 can start as soon as US5's `ProfileRepository` exists

---

## Parallel example: User Story 5

```bash
# All four RED tests together — different files, no shared state:
Task: "ProfileRepositoryTest — read own profile, empty renders placeholder"
Task: "ProfileRepositoryTest — upsert name/username, 409 maps to username-taken"
Task: "ProfileRepositoryTest — FR-013c, no financial field writable"
Task: "AvatarStorage tests — upload/fetch/delete, oversize and bad-format rejection"

# Then the two API files in parallel, before the repository that composes them:
Task: "Create ProfileApi.kt in data/identity/"
Task: "Create AvatarStorageApi.kt in data/identity/"
```

---

## Implementation strategy

### MVP (US1 + US2)

1. Phase 1 Setup — **T001 first, and verify a real email actually arrives**
2. Phase 2 Foundational
3. Phase 3 (US1) → Phase 4 (US2)
4. **STOP and validate**: quickstart Steps 3 and 4

That is a working password account with verified email, alongside the Google sign-in that already
ships. Everything after it is additive.

### Incremental delivery

1. Setup + Foundational → foundation ready
2. + US1 + US2 → **MVP**, password accounts work
3. + US3 + US4 → both methods proven, sessions solid
4. + US5 → profile is genuinely Dhruv's, not Google's
5. + US6 → one coherent first-run experience
6. + US7 → full self-service
7. + Phase 10 (if T009 says so) → username sign-in and lockout
8. Phase 11 → close

Each step leaves the app shippable.

---

## Notes

- `[P]` = different files, no incomplete dependency
- Every test cites its `AUTH-*` scenario ID (Article I) — a test without one does not count as done
- **Verify tests fail before implementing.** RED → GREEN → REFACTOR is non-negotiable here
- Commit per task or logical group
- **Three tasks are load-bearing and easy to skip**: T001 (custom SMTP — the feature is inoperable
  without it), T024 (the consent-boundary assertion — without it the FR-013a exception reads as a bug
  and gets "fixed" into a real Article VIII violation), and T077 (the offline path — the easiest
  regression to ship unnoticed)