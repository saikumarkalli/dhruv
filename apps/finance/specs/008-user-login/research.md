# Research — 008 User Login

**Date**: 2026-09-04 · **Spec**: [spec.md](spec.md) · **Plan**: [plan.md](plan.md)

Every finding below was **verified against live Supabase documentation on 2026-09-04**, not assumed
from memory. That discipline is not ceremony here: ADR-0029's own correction records certificate pins
that shipped wrong because a CA was inferred from prose instead of observed, and ADR-0033's
correction records a hosted Data API setting that no local config file controls. Items still marked
**VERIFY-AT-RED** below are ones documentation alone cannot settle — they are pinned to a failing
test in the Backend RED step rather than being assumed correct now.

---

## R1 — GoTrue password endpoints exist and match the shipped Retrofit stack

**Decision**: Extend the existing `GoTrueApi` (Retrofit + Moshi on `SupabaseClientFactory.authClient`)
with the password, verification and recovery endpoints. No new HTTP stack, no supabase-kt.

| Purpose | Endpoint | Body fields |
|---|---|---|
| Password sign-up | `POST /auth/v1/signup` | `email`, `password`, `data` (user metadata) |
| Password sign-in | `POST /auth/v1/token?grant_type=password` | `email`, `password` |
| Verify an OTP | `POST /auth/v1/verify` | `email`, `token`, `type` |
| Request reset | `POST /auth/v1/recover` | `email` |
| Change password / metadata | `PUT /auth/v1/user` | `password`, `email`, `data`, `nonce` |

**Rationale**: identical shape to the shipped `signInWithIdToken`/`refresh` pair — same base path,
same Moshi converter, same `AuthInterceptor`. Constitution Article XI (stack is fixed) is satisfied
without a new dependency.

**Alternatives rejected**: supabase-kt/Ktor — already rejected by ADR-0029 as an unproven AGP 9
Gradle-plugin risk; nothing here needs it.

**Source**: https://supabase.com/docs/reference/self-hosting-auth/introduction

---

## R2 — Email OTP is a template change, not a different API

**Decision**: Deliver 6-digit codes by putting `{{ .Token }}` in the **Confirm signup** and
**Reset password** email templates instead of `{{ .ConfirmationURL }}`. Verification is then
`POST /auth/v1/verify` with the typed code.

**Verified defaults** (both configurable in Auth settings):

| Property | Default | Spec requirement | Action |
|---|---|---|---|
| OTP expiry | **1 hour** | FR-001d "short validity window" | Reduce to **10 minutes** — an hour is far longer than a person needs and widens the guessing window |
| Resend cooldown | **60 seconds** | FR-001f resend cooldown | Accept the default as-is |

**Rationale**: the OTP requirement (FR-001b) costs one template edit and one settings change, not a
custom mail pipeline. `{{ .Token }}` is Supabase's own documented mechanism.

**Consequence for the spec**: FR-001e's *per-code attempt limit* is **not** a GoTrue feature — see
R5. FR-001f's "each new code invalidates the previous one" is GoTrue's native behaviour and needs no
client work.

**Source**: https://supabase.com/docs/guides/auth/auth-email-passwordless

---

## R3 — BLOCKING PREREQUISITE: the default email service cannot carry this feature

**Finding**: Supabase's built-in SMTP is rate-limited to **2 messages per hour**, and Supabase states
it is "provided as best-effort only" for non-production use. With a custom SMTP provider configured,
the limit starts at **30/hour** and is adjustable.

**Decision**: Configuring a custom SMTP provider is a **hard prerequisite**, sequenced as the first
item of Phase A. Nothing in User Story 2 (OTP), User Story 1's sign-up completion, or password reset
functions without it — at 2 emails/hour the feature is not degraded, it is inoperable.

**Rationale**: this is the single dependency most capable of silently sinking the phase. It is
external, needs a credentialed human action (same category as ADR-0032's runbook steps), and has a
lead time the code does not.

**Alternatives considered**: Supabase's *Send Email* auth hook (more control, more surface — not
needed when a plain SMTP provider suffices); shipping OTP against the default service (rejected: two
codes per hour across *all* users is not a testable system, let alone a shippable one).

**Source**: https://supabase.com/docs/guides/auth/auth-smtp

---

## R4 — Identity linking: automatic linking is native; manual linking is beta and must be enabled

**Findings**, verified:

- **Automatic linking** already does what FR-020/FR-020a describe: Supabase "will attempt to look for
  an existing user that uses the same email address" and links the new identity — and it requires the
  email to be **verified**, "to prevent account takeover attacks". FR-020a is therefore enforced by
  the platform, not by client code we write.
- **Manual linking** (needed for FR-036, adding a method deliberately) is **in beta** and must be
  switched on in the project's auth configuration.
- **Unlinking requires at least 2 linked identities.** This means **FR-037 — never leave an account
  with zero sign-in methods — is enforced server-side by GoTrue itself**, not by a client-side guard
  that could be bypassed.
- Native mobile apps may link using a provider **ID token** directly rather than a web redirect —
  so linking Google to a password account reuses the Credential Manager flow already shipped in
  `SignInScreen.kt` / `AccountSettingsScreen.kt`.

**Decision**: rely on automatic linking for FR-020; enable manual linking for FR-036; treat FR-037 as
satisfied by GoTrue's own constraint and assert it with a test rather than re-implementing it.

**VERIFY-AT-RED**: whether `PUT /auth/v1/user {password}` on a Google-only account creates a usable
`email` identity (FR-036/FR-038's "Google-only person sets a password"), or whether that path needs
manual linking too. Documentation does not settle it. This is a failing integration test in Phase C
before any UI depends on the answer.

**Source**: https://supabase.com/docs/guides/auth/auth-identity-linking

---

## R5 — BLOCKING FINDING: username sign-in and account lockout both need a server-side sign-in path

Two spec requirements cannot be met by calling GoTrue directly from the app.

### R5a — GoTrue has no username concept (FR-002)

`POST /token?grant_type=password` accepts **`email` or `phone` only**. There is no username field.
Signing in by username therefore requires resolving username → email *before* the GoTrue call.

**The naive fix is a security defect and is rejected.** A `security definer` RPC like
`email_for_username(text) returns text` is callable by `anon` (sign-in is by definition
unauthenticated) and hands out a real email address for any guessed username — a PII-harvesting
endpoint, and precisely the kind of "client-side filtering is not authorization" failure
constitution Article IXa exists to prevent.

### R5b — GoTrue has no per-account lock-until-reset (FR-040)

GoTrue applies its own rate limiting, but the spec's decided behaviour — lock **this account's**
password sign-in until a reset completes, count consecutive failures, email the owner (FR-040b), and
leave Google sign-in working (FR-040c) — is application state that only exists if something we
control observes each failed attempt. The client cannot be that something: a failed sign-in that
never reaches our code cannot be counted, and a counter the client keeps is trivially reset by
clearing app data.

### Decision: one Edge Function, `auth-signin`, owns both — and it is deferred to its own phase

A single Deno Edge Function resolves both at once. It accepts `{identifier, password}`, and:

1. resolves `identifier` to an email (username lookup via service role, or passthrough if it is
   already an email) — the email **never leaves the function**;
2. reads the account's lockout state and refuses early if locked;
3. calls GoTrue's password grant server-side;
4. records the outcome — increment on failure, clear on success, lock and send the FR-040b
   notification at the threshold;
5. returns either the session or **one generic failure shape** — which is also what makes FR-004 and
   FR-040d ("never reveal whether the identifier exists") true by construction rather than by
   remembering to phrase every error carefully.

The service-role key lives in the function's environment. This does **not** violate ADR-0014 §7 /
ADR-0029 decision 5 — their rule is "no service-role key **anywhere near the device**", and a
server-side function is the opposite of that. It does, however, introduce the platform's **first
Edge Function**, with its own deploy surface in CI.

**Consequence — this is why the plan phases it last (Phase F).** Email sign-in, password sign-up,
OTP, profile, and all of Settings › Account work without it. Recommend shipping A–E first and taking
F as a deliberate, separately-reviewed decision.

**Alternatives considered**:
- *Username as display handle only, sign-in by email* — deletes the entire Edge Function surface.
  This is the cheapest honest option and is worth reconsidering at Phase F; it narrows FR-002, which
  is a clarified decision, so it needs the maintainer's call, not a silent downgrade.
- *Accept GoTrue's built-in rate limiting instead of FR-040's lockout* — leaves the spec's decided
  behaviour unimplemented. Rejected as a silent scope drop (constitution Article Xa).

---

## R6 — Profile data belongs in a new `identity` Postgres schema, and that needs an ADR

**Decision**: create schema `identity` holding `identity.profiles`. Propose **ADR-0037**.

**Rationale**: ADR-0033 fixed one Postgres schema per app that owns Supabase-backed data, with
`public` "reserved for genuinely cross-app orchestration only". Spec FR-021 makes the profile
explicitly cross-app — the shared Dhruv ID identity of ADR-0031, not Finance's data. So:

- `finance.profiles` would be **wrong**: it states that Finance owns a person's identity, and the
  next app would have to reach across an app boundary to read a display name.
- `public.profiles` would be **defensible but muddy**: `public` currently holds two erasure
  *functions*, deliberately, because they act on the shared `auth.users` row. A table is a different
  kind of thing, and `public` becoming "whatever isn't Finance" is the drift ADR-0033 was written to
  stop.

**ADR number**: `platform/DECISIONS.md`'s highest **written** entry is **ADR-0034**. ADR-0035 is
reserved by `platform/VERSIONING.md` §9 and ADR-0036 by the agent-protocol plan — both still unbuilt.
Per that file's own rule (a written entry outranks a reservation, and reservations older than a merge
are re-checked at execution time), this takes **ADR-0037**. Re-run
`grep '^## ADR-' platform/DECISIONS.md` immediately before writing it — three collisions in three
months have all come from trusting a stale reservation.

**Consequences carried into the plan**:
- `identity` must be added to `config.toml`'s `[api] schemas` **and** to the hosted project's
  `db_schema` via the Management API — ADR-0033's 2026-09-03 correction proves the second is not
  implied by the first, and its absence fails as a silent `406`, on **both** `dhruv-dev` and the
  still-unpatched `dhruv-prod`.
- Explicit `grant usage on schema identity` + per-table grants (Article IXa) — `db diff` emits
  neither grants nor `security_invoker`, so both are hand-appended and read back.
- `public.delete_my_data()` / `public.delete_my_account()` gain the profile row and the stored avatar
  object (FR-018). Editing them is a `create or replace function` migration, matching how ADR-0033
  already moved the tracker tables.

---

## R7 — Avatars: Supabase Storage bucket, per-user path, RLS-enforced

**Decision**: a private `avatars` bucket; each object keyed by the owner's `auth.uid()` path prefix;
storage RLS policies scoped so a person can only write and replace their own object. The profile row
stores the object path, not the bytes.

**Rationale**: bytes in Postgres bloat every row read and every backup; a public bucket makes every
person's photo world-readable by URL, which FR-013b's disclosure obligation does not cover.

**Erasure (FR-018)**: deleting the row is not enough — the spec says the *stored photo file itself*.
The storage object is removed in the same erasure path, or it is orphaned data that outlives the
account and breaks the DPDP guarantee.

**VERIFY-AT-RED**: whether the erasure functions can delete a storage object from SQL, or whether
that step must be an explicit client call before the RPC. Determines whether FR-018 is one call or
two, and is a failing test in Phase B.

---

## R8 — Client wiring: profile calls need a fourth Retrofit instance

**Decision**: add `identityRetrofit` to `SupabaseClientFactory`, built on the existing **`authClient`**
chain (auth-gated, **not** consent-gated) with an `identity` schema header interceptor.

**Rationale**: spec FR-013a classifies the profile as account data outside the financial-sync consent
gate. The three existing instances are all wrong for it:

| Instance | Chain | Why not |
|---|---|---|
| `dataRetrofit` | consent + schema + auth | Would block profile reads whenever sync consent is off — contradicts FR-013a |
| `erasureRetrofit` | auth only | Right chain, but its doc comment explicitly forbids other calls, and it sends no schema header |
| `authApi` | auth only, `/auth/v1/` | Wrong base path |

A named fourth instance keeps `erasureRetrofit`'s "nothing else goes here" rule intact rather than
quietly eroding it, and keeps the consent boundary a property of *which client you can reach*, which
is the structural guarantee ADR-0029 decision 2 and constitution Article VIII depend on.

**Guard**: `FinanceSchemaInterceptor` is `finance`-specific. Generalise it to take the schema name, or
add a sibling — do not send `Accept-Profile: finance` on an `identity` request.

---

## R9 — Settings integration reuses the shipped control plane

**Decision**: the FR-033–FR-039 rows are added to the **existing** `AccountSettingsScreen`, registered
through the Settings control-plane contribution mechanism (spec `004-settings`,
`contracts/settings-contribution.md`, FR-003/FR-004).

**Rationale**: 004-settings shipped all of 0b.1–0b.5 including the real Account screen — sign-in,
sign-out, the three consent switches, and erasure with type-to-confirm. This feature extends that
screen; it does not build a parallel one. `004-settings`' own FR-003 ("every persisted key has exactly
one row") means each new preference gets exactly one control.

**Note**: `AccountSettingsScreen` currently **duplicates** the Credential Manager call rather than
sharing it with `com.dhruv.finance.onboarding` — deliberate, per that spec's research R6 /
`SET-ARCH-003` (a `feature → feature` dependency is forbidden by Article III). The Google-linking work
in FR-036 follows the same rule: duplicate the call, do not reach across the boundary.

---

## R10 — TDD, scenarios and coverage obligations

**Decision**: this feature follows the constitution's fixed step order (SA → QA → Backend RED/GREEN →
Android RED/GREEN → QA close → Sec → checkpoint), with new QA catalog rows under an `AUTH-*` prefix
in `apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md`.

- Article I: every test cites its scenario ID; no implementation before a failing test.
- Article II: QA rows exist **before** Backend or Android work starts.
- Article X: the JaCoCo floor moves up at the closing checkpoint only, never ahead of landed tests.
- Existing `ONB-*` rows change meaning where onboarding's flow changes (FR-023a) — those rows are
  **amended**, and `ONB-FLOW-001`/`002`'s current green tests will need updating rather than being
  left describing a flow that no longer exists.

---

## Open items carried into the plan

| # | Item | Where it resolves |
|---|---|---|
| O1 | Custom SMTP provider chosen and configured | Phase A — blocks everything email |
| O2 | Username sign-in + lockout: build the Edge Function, or narrow FR-002/FR-040? | Phase F — maintainer decision, recommend deciding before Phase A ends |
| O3 | Does `PUT /user {password}` alone give a Google-only account a working password identity? | Phase C, VERIFY-AT-RED |
| O4 | Can erasure delete the storage object from SQL, or is a client call required? | Phase B, VERIFY-AT-RED |
| O5 | ADR-0037 written and the number re-checked against `DECISIONS.md` at write time | Phase A |
| O6 | `identity` added to hosted `db_schema` on **both** `dhruv-dev` and `dhruv-prod` | Phase A (`dhruv-prod` is still unpatched from ADR-0033's correction) |