# Quickstart — validating 008 User Login

How to prove this feature works end to end. Validation guide, not implementation — task detail lives
in `tasks.md` (`/speckit-tasks`).

## Prerequisites

| # | Requirement | Verify with |
|---|---|---|
| P1 | `JAVA_HOME` = Android Studio JBR (JDK 17+) | `./gradlew -version` |
| P2 | `.env` carries real `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID` | app reaches sign-in without `NotConfiguredCard` |
| P3 | **Custom SMTP configured** (Phase A1) | Step 0 below — nothing email-shaped works without it |
| P4 | `identity` in the hosted `db_schema` on `dhruv-dev` (Phase A6) | Step 0 |
| P5 | Manual identity linking enabled (Phase A4) | needed only for FR-036 |
| P6 | A real email inbox you control | OTP steps |

## Step 0 — Prove the platform prerequisites before writing any client code

Two failures here are silent, and both have bitten this repo before. Check them first.

**0a. Email actually sends.** Trigger a password reset for a known address and confirm a **6-digit
code** (not a link) arrives within 60 seconds.

> Supabase's default mail service allows **2 emails per hour**. If P3 was skipped, the first
> validation run appears to work and every later one silently does not. Verifying delivery is not
> optional ceremony — it is the difference between a working feature and a mystery.

**0b. The `identity` schema is reachable.**

```bash
curl -i "$SUPABASE_URL/rest/v1/profiles?select=user_id" \
     -H "apikey: $SUPABASE_ANON_KEY" -H "Accept-Profile: identity"
```

**Expect `401`** (unauthenticated — past the schema check, into the auth check).
**`406` means `identity` is missing from the hosted Data API's `db_schema`**, which `config.toml`
does not control — exactly ADR-0033's 2026-09-03 failure. Fix via the Management API before going
further; `dhruv-prod` needs the same patch and is still outstanding for `finance`.

## Step 1 — Automated suites

```bash
# Data layer: GoTrue endpoints, repositories, the consent-boundary assertion
./gradlew :apps:finance:data:testDebugUnitTest

# Onboarding ViewModel state machine (incl. the amended ONB-FLOW-001/002)
./gradlew :apps:finance:feature:onboarding:testDebugUnitTest

# Settings + ArchUnit boundaries
./gradlew :apps:finance:app:testDebugUnitTest

# Full gate — what CI runs
./gradlew regressionCheck
```

The single most important unit test in this feature:

> `identityRetrofit` carries **no** `ConsentInterceptor`, and `dataRetrofit` still does.

That asserts FR-013a's deliberate consent-gate exception *and* that Article VIII still holds for
everything else. Without it, the exception reads as a bug and gets "fixed" in one of two ways that
each break something real.

## Step 2 — RLS (run as SQL, not from the app)

```bash
psql "$SUPABASE_DB_URL" -f supabase/verification/auth_identity_rls.sql
```

Asserts a second signed-in user reads **zero** rows from `identity.profiles` and cannot fetch another
person's avatar object. Article IXa: client-side filtering is never authorization — a query missing
its user filter must return nothing, not everything.

## Step 3 — Manual: password sign-up + OTP (US1, US2)

1. Fresh install → onboarding → **Create an account**.
2. Real email + password → submit.
3. **OTP screen appears** before the app is reachable (FR-001b).
4. Code arrives; enter it → verified, flow continues (FR-001c).
5. **Then** the consent screen, **then** skippable profile setup (FR-023a/FR-023b).

Negatives, all required:

| Try | Expect | FR |
|---|---|---|
| Wrong code | Clear error, retry allowed | FR-001e |
| Wait past 10 min | **"expired"**, distinct from "incorrect" | FR-001d |
| Resend immediately | Cooldown enforced; new code invalidates old | FR-001f |
| Force-quit at OTP, reopen | **Returns to OTP**, not a blank sign-up form | FR-001g |
| Sign up with a taken email | "already registered" | FR-005 |
| Wrong password at sign-in | Generic message — never reveals whether the email exists | FR-004, SC-005 |

## Step 4 — Manual: Google + session + offline (US3, US4)

1. Google sign-in still works unchanged; **no OTP** (FR-038).
2. Force-quit, reopen → still signed in (FR-009).
3. Let the access token lapse → silent refresh, no prompt (FR-010).
4. Sign out → session cleared, signed-out state (FR-012).
5. **Airplane mode → sign out** → still signs out locally.
6. **"Use offline — calculators only"** → calculators fully usable, tracker shows signed-out
   (FR-031, SC-010). *Highest-value check in this list — it is the easiest thing to lose while
   rewriting the sign-in screen.*
7. Later, from Settings, sign in without reinstalling (FR-032).

## Step 5 — Manual: profile independence (US5)

1. Password account, no photo → placeholder shown (FR-014).
2. Set a name and photo.
3. **Uninstall, reinstall, sign back in → both survive** (SC-004). This is what makes the profile
   genuinely ours rather than Google's.
4. Google account: first sign-in seeds name/photo (FR-015); edit them; sign out and back in →
   **edits survive, not overwritten** (FR-016).
5. Oversized / unsupported image → clear rejection **before** upload, not a silent failure.

## Step 6 — Manual: Settings › Account (US7)

| Action | Expect | FR |
|---|---|---|
| Edit name/photo | Persists, reflected everywhere | FR-033 |
| Change password | Succeeds; other sessions signed out; **this** device stays in | FR-034, FR-008 |
| View sign-in methods | Shows linked/not for both | FR-035 |
| Google-only → set password | Works, **no OTP**; both methods then usable | FR-036, FR-038 |
| Password-only → link Google | Attaches to the **same** account, no duplicate | FR-036 |
| Unlink the last method | **Not offered** — and refused server-side if forced | FR-037, SC-012 |
| Change-password row on a Google-only account | **Absent**, not present-and-broken | — |

## Step 7 — Manual: erasure (FR-018, SC-006)

Set a profile photo → **Delete my account** (type-to-confirm) → verify the profile row **and the
stored avatar object** are both gone.

> Deleting the row alone leaves the image file behind. That is orphaned personal data outliving a
> deleted account, and it breaks the DPDP erasure guarantee — the reason FR-018 says "including the
> stored photo file itself, not merely the reference to it". Check the object, not just the row.

## Step 8 — Design system + accessibility

```bash
/dhruv-ui-review     # tokens, components, light/dark, states
/dhruv-security      # secrets, DPDP, consent, RLS
```

Manual: every new screen in **both themes**; largest system font (no clipping); TalkBack order;
≥48dp targets; zero hardcoded strings.

`/dhruv-security` **must** land on the two accepted risks the spec's clarification recorded:

1. **Profile data leaves the device with no consent switch** (FR-013a). Lawful only because FR-013b's
   disclosure copy actually shipped and FR-013c holds. *Verify the disclosure copy is really in the
   consent/privacy screen* — if it was cut, the design is non-compliant.
2. **The lockout is deliberately abusable** (FR-040) — someone who knows an email can lock its owner
   out. Bounded by FR-040a–c, not removed.

## Step 9 — Phase F only (username + lockout)

Skip unless the O2 decision built the Edge Function.

| Check | Expect | FR |
|---|---|---|
| Sign in by username | Works | FR-002 |
| Wrong password × threshold | Account locked | FR-040 |
| Lockout email | Arrives, explains why and how to unlock | FR-040b |
| Google sign-in while locked | **Still works** | FR-040c |
| Password reset | Clears the lock | FR-040 |
| Any failure message | Never reveals whether the account exists | FR-040d |

**Also verify the negative**: no client-callable endpoint maps a username to an email address. If one
exists, it is a PII-harvesting surface and the implementation diverged from
[research.md](research.md) R5a.

## Definition of done

- [ ] Steps 0–8 pass (9 if Phase F shipped)
- [ ] `regressionCheck` green; coverage floor raised at this checkpoint only
- [ ] Every `AUTH-*` scenario row CLOSED or **deferred with a stated reason** — never silently dropped
- [ ] `ONB-*` rows amended where FR-023a changed the flow
- [ ] Docs closed per `apps/finance/CLAUDE.md`: FEATURES.md, module README, CHANGELOG, spec.md
      Implementation record, registry rows, design system §5.1 gains `PinEntry`, ADR-0037 written