# Contract — GoTrue endpoints consumed by 008

All paths are relative to `{SUPABASE_URL}/auth/v1/`, reached through
`SupabaseClientFactory.authApi` (Retrofit + Moshi on the **`authClient`** chain: `AuthInterceptor`
only, no `ConsentInterceptor` — sign-in is pre-consent, `ONB-BR-001`).

Endpoint shapes verified against Supabase's self-hosting auth reference on 2026-09-04
([research.md](../research.md) R1). Items marked **VERIFY-AT-RED** are pinned to a failing test in
Phase C, not assumed.

## Existing (shipped — unchanged)

| Method | Path | Purpose |
|---|---|---|
| POST | `token?grant_type=id_token` | Google sign-in (`GoogleIdTokenRequest`) |
| POST | `token?grant_type=refresh_token` | Silent session refresh (FR-010) |

## New

### 1. Password sign-up — FR-001

```http
POST /auth/v1/signup
{ "email": "...", "password": "...", "data": { "username": "..." } }
```

Returns a `GoTrueSessionDto`. When email confirmation is on, the user exists with
`email_confirmed_at = null` — the `PENDING_VERIFICATION` state of the data model — and an OTP is
emailed (Confirm signup template carrying `{{ .Token }}`).

`data` writes to `user_metadata`. The username is **also** persisted to `identity.profiles`, which is
the uniqueness authority — `user_metadata` is unconstrained and cannot enforce FR-005.

**Errors**: `422` email already registered → FR-005's "identifier is taken". Weak password → FR-006's
explanatory message.

### 2. Password sign-in — FR-002

```http
POST /auth/v1/token?grant_type=password
{ "email": "...", "password": "..." }
```

**Accepts `email` or `phone` only — no username field.** Signing in by username needs Phase F's
server-side resolution (research R5a). Until then the client sends the identifier only when it
parses as an email.

**Errors**: `400` invalid credentials. Surface **one generic message** for both wrong-email and
wrong-password (FR-004) — never branch the copy on which failed.

### 3. Verify an OTP — FR-001c, FR-007

```http
POST /auth/v1/verify
{ "email": "...", "token": "123456", "type": "signup" | "recovery" }
```

Returns a session on success. `type` is `signup` for email verification and `recovery` for a password
reset. **VERIFY-AT-RED**: exact accepted `type` strings — the reference documents the field but not
its value set.

**Errors**: expired vs incorrect must be distinguished in the UI (FR-001d). If GoTrue returns one
undifferentiated error, the client tracks local issue time to say "expired" — the requirement is that
the *person* can tell, not that the server does.

### 4. Request a password reset — FR-007

```http
POST /auth/v1/recover
{ "email": "..." }
```

Emails an OTP (Reset password template with `{{ .Token }}`). Then `verify(type=recovery)` → session →
`PUT /user` with the new password.

**Always report the same confirmation**, whether or not the address exists (spec Edge Cases, FR-004).

### 5. Update the current user — FR-013, FR-016, FR-034, FR-036, FR-038

```http
PUT /auth/v1/user
{ "password": "...", "email": "...", "data": { ... }, "nonce": "..." }
```

Three uses:
- **Change password** (FR-034) — must invalidate other sessions (FR-008). **VERIFY-AT-RED**: whether
  GoTrue revokes other refresh tokens automatically or needs an explicit global sign-out.
- **Set a password on a Google-only account** (FR-036/FR-038). **VERIFY-AT-RED (O3)**: whether this
  alone yields a usable `email` identity, or manual linking is also required.
- **User metadata** — mirrors of `display_name`/`avatar` only. `identity.profiles` stays authoritative
  (data-model.md); metadata is a convenience copy, never read as truth.

### 6. Sign out — FR-012

```http
POST /auth/v1/logout
```

The shipped client clears `SessionStore` locally. Calling `logout` additionally revokes server-side.
Local clear must happen **regardless** of the network result — a person tapping "sign out" offline
must still end up signed out on the device.

### 7. Identity linking — FR-035, FR-036, FR-037

Uses the SDK-level `linkIdentity` semantics; for native apps the **provider ID token** path is
available, so Google linking reuses the shipped Credential Manager flow rather than a web redirect
(research R4).

**Requires manual linking enabled** (beta) — Phase A4.

- **FR-020 auto-link** needs no call at all: GoTrue links a new identity to an existing user when the
  email matches and is verified.
- **FR-037** is enforced by GoTrue — unlink requires ≥2 identities. Assert; don't re-implement.

**VERIFY-AT-RED**: the exact REST path behind `linkIdentity`/`unlinkIdentity` — documented at SDK
level, not REST level.

## Cross-cutting

**Error mapping.** Extend the shipped `ErrorMapper`. Rule: **never let a message reveal whether an
identifier exists** (FR-004, FR-040d, SC-005). Same generic text for unknown-email, wrong-password
and locked-account.

**401 handling is already correct and must not be duplicated.** `AuthInterceptor` retries once on
401, then forces `SignedOut` on a second consecutive 401 (`DAT-BR-003`). New endpoints inherit this
by construction — do not add per-call retry logic.

**Cert pinning** is unchanged: CA-level GTS Root R1/R4. New endpoints are new paths on an existing
pinned host, so nothing about pinning changes.