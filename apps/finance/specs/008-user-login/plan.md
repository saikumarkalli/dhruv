# Implementation Plan: User Login — Password + Google, Email OTP, Own Profile Storage

**Branch**: `008-user-login` | **Date**: 2026-09-04 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `apps/finance/specs/008-user-login/spec.md`

**Companion artifacts**: [research.md](research.md) · [data-model.md](data-model.md) ·
[contracts/](contracts/) · [quickstart.md](quickstart.md)

## Summary

Add email/password sign-in and sign-up alongside the shipped Google sign-in, verify email ownership
with a 6-digit OTP, store each person's display name and photo in Dhruv's own database instead of
depending on Google, and expose full account self-service in Settings — all inside the existing
onboarding flow and the existing design system.

Technically this is **an extension of what already ships, plus one new Postgres schema**. The
Retrofit/Moshi/OkHttp GoTrue client, `SessionStore`, `AuthInterceptor`, the consent split between
`authClient` and `dataClient`, and the Settings control plane all exist and are reused unchanged. The
genuinely new surfaces are: an `identity` Postgres schema holding the profile (ADR-0037), a Supabase
Storage bucket for avatars, a fourth Retrofit instance that is auth-gated but deliberately **not**
consent-gated, and — only if the maintainer takes Phase F — the platform's first Edge Function.

Two findings shape the phasing more than anything else:

1. **Custom SMTP is a hard prerequisite.** Supabase's default mail service allows 2 emails/hour. The
   OTP feature is not degraded without a real provider, it is inoperable. → Phase A, first item.
2. **Username sign-in and account lockout cannot be done from the client.** GoTrue's password grant
   takes email or phone only, and has no per-account lock-until-reset. Both need a server-side
   sign-in path, and the obvious client-side shortcut for username lookup is a PII-harvesting
   endpoint. → isolated into Phase F so the other 90% of the feature is not held hostage to it.

## Technical Context

**Language/Version**: Kotlin 2.x, JVM target 17 (Android Studio JBR)

**Primary Dependencies**: Jetpack Compose · Koin (DI — never Hilt, ADR-0010) · Retrofit + Moshi +
OkHttp (Supabase REST, ADR-0029) · Coroutines/Flow · `androidx.credentials` + `googleid` (Google
sign-in, already present in `:apps:finance:app`) · AndroidX DataStore + `EncryptedDataStoreFactory`

**Storage**:
- Session tokens → `EncryptedDataStore` (`tracker_session`), unchanged
- Profile row → **new** `identity.profiles` in Supabase Postgres (ADR-0037, proposed)
- Avatar image → **new** private Supabase Storage bucket, per-user path
- Auth identities/credentials → `auth.users` (GoTrue-owned; we never write password hashes ourselves)

**Testing**: JUnit4 + `kotlinx-coroutines-test` + Turbine + hand-written fakes (no Robolectric-SQLite
— it fails on Windows) · ArchUnit `DependencyRulesTest` · JaCoCo via `./gradlew regressionCheck` ·
SQL verification scripts under `supabase/verification/`

**Target Platform**: Android minSdk 26, single-activity Compose

**Project Type**: Mobile app + Supabase backend (schema-first, declarative per ADR-0032)

**Performance Goals**: OTP email delivered < 60s p95 (SC-001a) · sign-in round trip within the
existing 15s OkHttp timeout · no added cold-start cost (profile is fetched after session resolution,
never blocking first frame)

**Constraints**: No secrets in repo or APK (GitLeaks gates CI) · CA-level cert pinning stays as-is
(GTS Root R1/R4) · zero tracker network calls before consent (Article VIII) — profile calls are
deliberately outside that gate per FR-013a and must therefore be structurally unable to reach
`dataRetrofit` · every string a resource from birth · no raw hex/dp/sp in screens

**Scale/Scope**: single-maintainer personal app; tens of users. ~6 new screens, 1 new Postgres schema
with 1 table, 1 storage bucket, ~8 new GoTrue endpoints, ~7 new Settings rows.

## Constitution Check

*GATE: evaluated before Phase 0 research, re-evaluated after Phase 1 design. Both passes below.*

| Article | Gate | Pre-research | Post-design |
|---|---|---|---|
| I — Test-First | Every test cites a scenario ID; no code before a failing test | ✅ planned | ✅ `AUTH-*` catalog rows are a Phase B deliverable, before any Phase C code |
| II — Scenarios Before Code | QA rows exist and are reviewed first | ✅ | ✅ Phase B step 1 |
| III — Module Boundaries | `feature → feature` forbidden; `feature → data` Repository-only | ⚠️ risk | ✅ Credential Manager call is **duplicated** in Settings rather than shared with `onboarding` — same rule 004-settings' `SET-ARCH-003` already followed |
| IV — Fault Isolation | Every route in `FeatureHost` + flag entry | ✅ | ✅ FR-029; onboarding routes stay pre-shell as today |
| V — No Hardcoding | Tokens only; screen data in `<Name>Config.kt` | ✅ | ✅ FR-024/FR-028; copy extends the existing `OnboardingConfig.kt` pattern |
| VI — Component Reuse | Extend `:libs:core`, never fork a parallel component | ⚠️ risk | ✅ OTP entry needs `PinEntry`, which design system §5.2 lists as **planned, not built** — it is built **in `:libs:core`**, not in the feature module |
| VII — Money Is Exact | Tracker amounts are `Long` paise | N/A | N/A — this feature touches no money |
| VIII — Consent Before Network | No tracker call before consent, enforced by interceptor | ⚠️ **material** | ✅ see Complexity Tracking — profile is deliberately outside the gate (FR-013a), which is why it gets its **own** client rather than a bypass flag on the gated one |
| IX — Append-Only History | Shipped TEXT enum constants never renamed; ADRs append-only | ✅ | ✅ ADR-0037 is a new entry; no existing ADR body edited |
| IXa — Authorization Is Server-Side | RLS on every table, `security_invoker` on every view, explicit grants on custom schemas, `security definer` does its own ownership check | ⚠️ **material** | ✅ `identity.profiles` RLS + explicit grants + storage RLS; RLS test asserts a second user reads **zero** rows |
| X — Coverage Ratchets | Floor moves up only at a checkpoint | ✅ | ✅ Phase G |
| Xa — Docs Track Reality | Implementation record, CHANGELOG, registry rows in the same change | ✅ | ✅ Phase G; `ONB-*` rows **amended**, not left describing a flow that no longer exists |
| XI — Stack Is Fixed | No Hilt/Kover/Ktor; Supabase is plain REST | ✅ | ✅ no new client dependency. Phase F adds a *server-side* Deno function, which is outside the Android stack this article governs |

**Verdict**: **PASS**, with two entries justified in Complexity Tracking below. Neither is a
violation smuggled through — both are consequences of decisions the spec's clarification session took
explicitly, recorded so a reviewer can disagree with them on purpose.

## Project Structure

### Documentation (this feature)

```text
apps/finance/specs/008-user-login/
├── spec.md              # /speckit-specify + /speckit-clarify output
├── plan.md              # This file
├── research.md          # Phase 0 — verified findings + open items
├── data-model.md        # Phase 1 — entities, schema, state transitions
├── contracts/
│   ├── gotrue-endpoints.md      # GoTrue REST surface this feature consumes
│   ├── profile-api.md           # identity.profiles + avatar storage contract
│   ├── routes.md                # new/changed nav routes
│   └── settings-rows.md         # Settings › Account contribution rows
├── quickstart.md        # Phase 1 — how to validate end to end
└── checklists/
    ├── requirements.md  # spec quality (exists)
    └── qa.md            # AUTH-* scenario rows (Phase B deliverable)
```

### Source code

```text
supabase/
├── schemas/identity/                     # NEW (ADR-0037)
│   ├── 00_schema.sql                     # create schema + usage grant
│   └── 10_tables/profiles.sql            # table + RLS + explicit grants
├── schemas/public/30_functions/          # AMENDED — erasure covers profile + avatar
│   ├── delete_my_data.sql
│   └── delete_my_account.sql
├── migrations/<ts>_identity_profiles.sql # generated via db diff, grants hand-appended
├── verification/auth_identity_rls.sql    # NEW — second user reads zero rows
└── functions/auth-signin/                # Phase F ONLY — first Edge Function

apps/finance/data/src/main/java/com/dhruv/finance/data/
├── tracker/auth/
│   ├── GoTrueApi.kt                      # AMENDED — signup/password/verify/recover/PUT user
│   ├── AuthRepository.kt                 # AMENDED — password, OTP, reset, link, change password
│   └── SessionStore.kt                   # unchanged (already stores name/avatar)
├── tracker/dto/                          # AMENDED + new request/response DTOs
├── tracker/net/
│   ├── SupabaseClientFactory.kt          # AMENDED — + identityRetrofit
│   └── SchemaInterceptor.kt              # RENAMED/generalised from FinanceSchemaInterceptor
└── identity/                             # NEW package
    ├── ProfileApi.kt · ProfileRepository.kt · AvatarStorageApi.kt

apps/finance/feature/onboarding/onboarding/src/main/java/com/dhruv/finance/onboarding/
├── OnboardingViewModel.kt · OnboardingUiState.kt · OnboardingConfig.kt   # AMENDED
├── SignInScreen.kt                       # AMENDED — dual method on one screen
└── SignUpScreen.kt · OtpVerifyScreen.kt · ForgotPasswordScreen.kt
    · ResetPasswordScreen.kt · ProfileSetupScreen.kt                     # NEW

apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/
├── AccountSettingsScreen.kt · AccountSettingsViewModel.kt               # AMENDED
└── ProfileEditScreen.kt · ChangePasswordScreen.kt · LinkedMethodsScreen.kt  # NEW

libs/core/src/main/kotlin/com/dhruv/core/ui/components/
└── PinEntry.kt                           # NEW — design system §5.2 batch B2
```

**Structure Decision**: Mobile + backend, following the shipped topology exactly. Auth/profile data
access lives in `:apps:finance:data` (Article III: features reach data only through repositories);
screens live in the existing `onboarding` feature module and the `app` module's Settings package; the
one new reusable component lands in `:libs:core`, never in a feature module (Article VI).

The `identity` schema is **not** placed under `finance` deliberately — spec FR-021 makes the profile
cross-app Dhruv ID data, and putting a shared identity inside one app's schema would state the
opposite. Reasoning in [research.md](research.md) R6.

---

## Phases

Seven phases. **A–E deliver the whole feature except username sign-in and lockout**; F is separable
and carries the only new architectural surface; G closes. Each phase is independently mergeable and
leaves the app working.

### Phase A — Platform prerequisites *(no app code; mostly credentialed human actions)*

Nothing downstream is testable until these land. They are runbook steps in the same category as
ADR-0032's manual setup, not automatable from this session.

| # | Item | Why it blocks |
|---|---|---|
| A1 | **Configure custom SMTP** on `dhruv-dev` and `dhruv-prod` | Default is 2 emails/hour — OTP is inoperable (research R3) |
| A2 | Edit **Confirm signup** + **Reset password** email templates to use `{{ .Token }}` | Without it Supabase sends links, not codes (R2) |
| A3 | Set OTP expiry to **10 min** (default 1h); keep 60s resend cooldown | FR-001d/FR-001f |
| A4 | Enable **manual identity linking** (beta flag) | FR-036 |
| A5 | Write **ADR-0037** (`identity` schema). **Re-run `grep '^## ADR-' platform/DECISIONS.md` first** | R6 — three numbering collisions in three months, all from stale reservations |
| A6 | Add `identity` to `config.toml` `[api] schemas` **and** to hosted `db_schema` on **both** projects via Management API | ADR-0033's correction: config.toml does not drive the hosted Data API. `dhruv-prod` is still unpatched for `finance` — fix both in one pass |
| A7 | Create the private `avatars` storage bucket | FR-013a |
| A8 | **Decide O2**: build Phase F, or narrow FR-002/FR-040? | Deciding now avoids designing screens around a username field that may never work |

**Exit**: an OTP email actually arrives in a real inbox, and `GET /rest/v1/` with
`Accept-Profile: identity` returns 401 (past the schema check) rather than 406.

### Phase B — SA + QA: schema, RLS, erasure, scenarios

Constitution Articles II and IXa own this phase. No Kotlin.

- **B1** `supabase/schemas/identity/00_schema.sql` + `10_tables/profiles.sql` — table, RLS
  (`user_id = auth.uid()`), explicit `grant usage on schema` + per-table grants. `db diff` emits
  neither grants nor `security_invoker` — hand-append and read the generated SQL back.
- **B2** Storage RLS on `avatars`: a person reads/writes only their own path prefix.
- **B3** Amend `public.delete_my_data()` / `delete_my_account()` to remove the profile row **and the
  stored avatar object** (FR-018). `create or replace function`, never drop+recreate.
  → **VERIFY-AT-RED (O4)**: can SQL delete a storage object, or is a client call needed first?
- **B4** `supabase/verification/auth_identity_rls.sql` — a second signed-in user reads **zero** rows
  from `identity.profiles` and cannot read another person's avatar object.
- **B5** Regenerate `supabase/SCHEMA.md`; extend the `gen types` invocations to `--schema
  public,finance,identity` (a schema omitted from that flag silently loses typed coverage).
- **B6** **QA catalog rows** (`AUTH-*`) in the QA scenario catalog, covering every FR. Amend the
  `ONB-*` rows whose flow changes under FR-023a. Article II: this precedes all of Phase C.

**Exit**: migration applied to `dhruv-dev`; B4 passes; `AUTH-*` rows reviewed against the spec.

### Phase C — Backend/data layer (TDD: RED → GREEN → REFACTOR)

- **C1** Extend `GoTrueApi` with `signup`, password grant, `verify`, `recover`, `PUT /user`
  (contracts/gotrue-endpoints.md). DTOs in `tracker/dto/`.
- **C2** Extend `AuthRepository`: `signUpWithPassword`, `signInWithPassword`, `verifyOtp`,
  `resendOtp`, `requestPasswordReset`, `changePassword`, `linkGoogle`, `listIdentities`.
  Preserve the shipped `CancellationException` rethrow pattern — a bare `runCatching` there breaks
  structured concurrency, as this file's own comment records.
  → **VERIFY-AT-RED (O3)**: does `PUT /user {password}` alone give a Google-only account a working
  password identity, or is manual linking needed?
- **C3** `SupabaseClientFactory` gains `identityRetrofit` on the **`authClient`** chain (auth-gated,
  not consent-gated) with an `identity` schema header. Generalise `FinanceSchemaInterceptor` to take
  a schema name.
  **Test the boundary explicitly**: assert `identityRetrofit` does **not** carry `ConsentInterceptor`
  and `dataRetrofit` still does. FR-013a is a deliberate consent-gate exception — the test is what
  stops it being read later as a bug and "fixed" into a bypass on the gated client.
- **C4** `identity/ProfileApi` + `ProfileRepository` + `AvatarStorageApi` — read, upsert, upload,
  delete. Enforce FR-013c (no financial fields in the profile record) at the repository boundary,
  the same way `sector` is enforced there today.
- **C5** Koin wiring in `PlatformModule.kt`.

**Exit**: `./gradlew :apps:finance:data:testDebugUnitTest` green; every test cites an `AUTH-*` row.

### Phase D — Onboarding UI (TDD: ViewModel RED first)

Order is fixed by FR-023a: **sign-in/up → OTP → consent → skippable profile setup**.

- **D1** `PinEntry` in **`:libs:core`** (design system §5.2 batch B2). Move it from §5.2 to §5.1 in
  the same change — Article VI's "nothing is listed as built before the code exists" cuts both ways.
- **D2** `SignInScreen` becomes dual-method on one screen: identifier + password, "Sign in with
  Google", "Create an account", and the **existing** "Use offline — calculators only" (FR-031 —
  regression risk: this option must survive the rewrite).
- **D3** `SignUpScreen` → `OtpVerifyScreen` (resend cooldown, attempt limit, expired-vs-wrong
  distinction) → existing consent screen → `ProfileSetupScreen` (**skippable**, FR-023b).
- **D4** `ForgotPasswordScreen` → OTP → `ResetPasswordScreen`.
- **D5** Extend `OnboardingViewModel`'s state machine and `OnboardingConfig.kt` copy. Resume at the
  OTP step for a pending account (FR-001g). Update the existing `ONB-FLOW-001/002` tests — they
  currently assert a flow that FR-023a changes.
- **D6** All screens: light + dark, state components, ≥48dp targets, `contentDescription`s, every
  string in `strings.xml` (FR-024–FR-030).

**Exit**: `:feature:onboarding:testDebugUnitTest` green; `/dhruv-ui-review` clean.

### Phase E — Settings › Account (TDD: ViewModel RED first)

- **E1** Profile edit (name + photo) — FR-033.
- **E2** Change password (current + new, invalidates other sessions) — FR-034/FR-008.
- **E3** Linked-methods view — FR-035.
- **E4** Link the missing method — FR-036. Google linking **duplicates** the Credential Manager call
  (Article III; same rule as `SET-ARCH-003`), never imports from `onboarding`.
- **E5** FR-037 (never zero methods) is enforced by GoTrue itself — unlink needs ≥2 identities
  (research R4). **Assert it with a test; do not re-implement it client-side.**
- **E6** Register every row through the Settings contribution mechanism (FR-039).

**Exit**: `:apps:finance:app:testDebugUnitTest` green including ArchUnit.

### Phase F — Username sign-in + account lockout *(separable; decide at A8)*

Gated on the O2 decision. Everything here needs a server-side sign-in path; nothing in A–E depends
on it.

- **F1** Edge Function `auth-signin`: resolve identifier → email (service role, email never returned
  to the client), check lockout, call the password grant, record outcome, lock at threshold, send the
  FR-040b notification, return one generic failure shape (FR-004/FR-040d true by construction).
- **F2** Lockout state table in `identity`, written only by the function.
- **F3** Client switches password sign-in to the function endpoint; Google sign-in path unchanged
  (FR-040c).
- **F4** CI: Edge Function deploy in `supabase-migrate.yml`, dev-auto / prod-gated like migrations.

**If O2 narrows scope instead**: FR-002 becomes email-only sign-in with username as a display handle,
and FR-040 falls back to GoTrue's built-in rate limiting. **That is a spec change** — amend spec.md
and record it in the Implementation record (Article Xa), never a silent drop.

### Phase G — Closure

- **G1** `AUTH-*` rows executed and closed; deferrals stated with reasons.
- **G2** Security + DPDP pass (`/dhruv-security`). Must confirm the two accepted risks the spec's
  clarification recorded: **profile data outside the consent gate** (lawful only if FR-013b's
  disclosure copy actually shipped and FR-013c holds), and **the deliberate lockout DoS window**.
- **G3** `regressionCheck` green; coverage floor raised at this checkpoint only.
- **G4** Docs (Article Xa + `apps/finance/CLAUDE.md`'s tracking rule): FEATURES.md row, module
  README, CHANGELOG, spec.md **Implementation record**, route/settings registry rows, design system
  §5.1 gains `PinEntry`, ADR-0037 finalised.

---

## Sequencing

```text
A (prereqs) ──> B (schema+QA) ──> C (data) ──> D (onboarding UI) ──> G (closure)
                                    │                                  ▲
                                    └──────> E (settings) ─────────────┘

F (username + lockout) ── separable; only if A8 says build it ──> G
```

D and E both depend on C and are otherwise independent — parallelisable. F touches no A–E code path
except swapping one call site.

## Complexity Tracking

| Violation | Why needed | Simpler alternative rejected because |
|---|---|---|
| **Profile network calls bypass `ConsentInterceptor`** (Article VIII) | Spec FR-013a decides the profile is *account* data, not tracker financial data — the same class as the Google name/email/photo the app **already** sends pre-consent today (`ONB-BR-001`). Bounded by FR-013b (mandatory disclosure) and FR-013c (no financial fields) | Putting profile behind the sync consent gate would make a person's own name and photo vanish whenever they decline financial sync — and would contradict a clarified decision. Adding a bypass flag to `dataRetrofit` was rejected outright: it converts a structural guarantee into a boolean any future call site can set |
| **A fourth Retrofit instance** (`identityRetrofit`) | The consent boundary is currently guaranteed by *which client a call can physically reach*. A new consent posture needs a new client, or the guarantee degrades to per-call-site discipline | Reusing `erasureRetrofit` (right chain) was rejected: its doc comment forbids exactly this, and it sends no schema header. Reusing `dataRetrofit` reintroduces the gate this data is deliberately outside of |
| **First Edge Function** (Phase F only) | FR-002 (username) and FR-040 (lockout) are both unreachable from the client: GoTrue's password grant has no username field and no per-account lock. A client-side counter is resettable by clearing app data | The naive `email_for_username()` RPC is a PII-harvesting endpoint callable by `anon` — rejected on Article IXa grounds. Accepting GoTrue's rate limiting instead silently drops decided scope (Article Xa). Isolated to its own phase so the decision can be taken deliberately |

## Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Custom SMTP not configured | OTP inoperable — most of the feature dead | Phase A1, first item; exit criterion is a real email in a real inbox |
| `identity` missing from hosted `db_schema` | Silent `406` on every profile call — exactly ADR-0033's 2026-09-03 failure repeating | Phase A6 patches **both** projects; `dhruv-prod` is *already* unpatched for `finance` |
| Erasure misses the avatar object | Orphaned personal data outliving a deleted account — breaks the DPDP guarantee and FR-018 | O4 verified at RED in Phase B, not assumed |
| Profile consent exception misread later as a bug | Someone "fixes" it into a bypass flag on the gated client, quietly breaking Article VIII for everything | C3 asserts the interceptor sets of both clients; Complexity Tracking states the intent |
| Rewriting `SignInScreen` drops the offline path | Silent regression for every no-account user (FR-031) | Explicit D2 line item + an `AUTH-*` scenario row |
| Phase F's Edge Function grows into a general auth proxy | New unreviewed architectural surface | Scope frozen to F1's five steps; anything more needs its own ADR |