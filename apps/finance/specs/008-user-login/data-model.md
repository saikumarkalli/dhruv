# Data Model — 008 User Login

**Date**: 2026-09-04 · **Spec**: [spec.md](spec.md) · **Plan**: [plan.md](plan.md) ·
**Research**: [research.md](research.md)

Maps the spec's Key Entities onto concrete storage. Three of the five entities are **owned by GoTrue**
and are not ours to model — recording that explicitly matters, because inventing a parallel users
table beside `auth.users` is the classic way to end up with two disagreeing sources of identity.

## Ownership map

| Spec entity | Where it lives | Who owns it |
|---|---|---|
| User Account | `auth.users` + `auth.identities` | **GoTrue** — we never write it directly |
| Linked Sign-in Methods | `auth.identities` (one row per provider) | **GoTrue** — read via `PUT /user` response / identities list |
| Session | `EncryptedDataStore` (`tracker_session`) on-device; refresh tokens server-side | Shipped `SessionStore`, unchanged |
| One-Time Code (OTP) | GoTrue internal | **GoTrue** — issued by `signup`/`recover`, consumed by `verify` |
| Password Reset Request | GoTrue internal | **GoTrue** — `recover` + `verify(type=recovery)` |
| **User Profile** | **`identity.profiles`** + avatar object in Storage | **Ours** (new, ADR-0037) |
| **Verification State** | `auth.users.email_confirmed_at` | **GoTrue** — read, never written by us |
| **Lockout State** | `identity.auth_lockouts` | **Ours — Phase F only** |

**Consequence**: the only table this feature adds in phases A–E is `identity.profiles`. Everything
else is either GoTrue's or already shipped.

---

## `identity.profiles`

The person-facing identity Dhruv owns — display name and photo — kept independent of Google
(FR-013/FR-013a). One row per account, cross-app by design (FR-021), which is why it is in `identity`
and not `finance` (research R6).

```sql
create table if not exists identity.profiles (
    user_id uuid primary key references auth.users (id) on delete cascade,
    display_name text check (
        display_name is null or length(btrim(display_name)) between 1 and 60
    ),
    username text unique check (
        username is null or username ~ '^[a-z0-9_]{3,30}$'
    ),
    avatar_path text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
```

| Column | Notes |
|---|---|
| `user_id` | PK **and** FK — one profile per account, and `on delete cascade` means deleting the auth row cannot orphan a profile |
| `display_name` | Nullable: FR-023b makes profile setup skippable, so "no name yet" is a normal state, not an error. Trimmed length bounded like `finance.holdings.name` |
| `username` | Nullable and **unique**. Lowercased, restricted charset — a case-insensitive collision (`Sai` vs `sai`) would otherwise make FR-005's uniqueness claim false. Sign-in *by* username is Phase F (research R5a) |
| `avatar_path` | Storage object path, **not** the bytes and **not** a public URL. Null → FR-014's placeholder |
| `updated_at` | Distinguishes "never edited" from "edited back to the Google value" — the state FR-016 turns on |

**Deliberately absent**: `email`, `email_verified`, any password field. All three live in `auth.users`;
duplicating them creates a second truth that drifts. `email_confirmed_at` is read from the session's
user object, never mirrored here.

**FR-013c (no financial data in the profile)** is a *schema-level* guarantee here: there is no column
that could hold an amount. The repository boundary enforces it for `display_name` content only in the
sense that nothing writes derived financial text into it.

### RLS + grants (Article IXa)

```sql
alter table identity.profiles enable row level security;

create policy "profiles_select_own" on identity.profiles
    for select using (user_id = auth.uid());
create policy "profiles_insert_own" on identity.profiles
    for insert with check (user_id = auth.uid());
create policy "profiles_update_own" on identity.profiles
    for update using (user_id = auth.uid()) with check (user_id = auth.uid());

grant usage on schema identity to authenticated;
grant select, insert, update on identity.profiles to authenticated;
```

- **No DELETE policy.** Same rule as `finance.holdings` (ADR-0029 decision 5): rows disappear only via
  `public.delete_my_data()` / `delete_my_account()`, keeping erasure auditable and central.
- **No `anon` grant anywhere.** Every profile call is authenticated.
- `db diff` emits neither the grants nor `security_invoker` — hand-append and read back (Article IXa).
- **Username uniqueness leaks nothing by itself**, but a "username taken" check at sign-up is an
  enumeration surface. It returns only taken/available for the *submitted* value, never a lookup that
  maps a username to a person.

### Avatar object (Supabase Storage)

Private bucket `avatars`, object path `{auth.uid()}/avatar.<ext>`. Storage RLS scopes read and write
to the owner's own prefix. Profile stores the path; the client fetches a signed URL.

**Erasure (FR-018)** must remove the object, not just the row — "including the stored photo file
itself, not merely the reference to it". Whether SQL can do that or a client call is required is
**O4, VERIFY-AT-RED in Phase B**.

---

## `identity.auth_lockouts` — Phase F only

Only exists if the O2 decision builds the Edge Function. **Written exclusively by that function's
service-role connection; no `authenticated` grant at all** — a client that can write its own lockout
row can clear its own lockout, which defeats the entire mechanism.

```sql
create table if not exists identity.auth_lockouts (
    user_id uuid primary key references auth.users (id) on delete cascade,
    consecutive_failures int not null default 0,
    locked_at timestamptz,
    notified_at timestamptz
);

alter table identity.auth_lockouts enable row level security;
-- No policies and no grants: unreachable via PostgREST by design.
```

| Column | Purpose |
|---|---|
| `consecutive_failures` | FR-040a — reset to 0 on any successful sign-in |
| `locked_at` | Non-null ⇒ password sign-in refused until a reset completes (FR-040) |
| `notified_at` | FR-040b — guarantees exactly one lockout email, not one per subsequent attempt |

---

## State transitions

### Account lifecycle

```text
                    ┌─────────────────────────────────────────┐
                    │                                         │
 (none) ──signup──> PENDING_VERIFICATION ──verify(ok)──> ACTIVE ──delete──> (none)
                    │        ▲                             │
                    │        └── resend (new code          │
                    │            invalidates old)          │
                    └── abandoned: resumes here on          │
                        next app open (FR-001g)            │
                                                            │
 (none) ──Google sign-in──────────────────────────────────> ACTIVE
          (email pre-verified by Google — no OTP, FR-038)
```

`PENDING_VERIFICATION` is a real, resumable state, not a transient step — FR-001g requires returning
a person who abandoned sign-up to the OTP screen rather than a blank form. Read from
`auth.users.email_confirmed_at` being null while a session exists.

### Password sign-in (Phase F lockout states)

```text
UNLOCKED ──fail──> UNLOCKED(n+1) ──fail at threshold──> LOCKED ──reset completes──> UNLOCKED(0)
    │                                                      │
    └──success──> UNLOCKED(0)                              └── Google sign-in still succeeds (FR-040c)
```

### Linked methods

```text
GOOGLE_ONLY ──set password──> BOTH ──unlink Google──> PASSWORD_ONLY
PASSWORD_ONLY ──link Google──> BOTH ──unlink password──> GOOGLE_ONLY

BOTH ──unlink either──> the remaining one.  Single-method ──unlink──> REFUSED by GoTrue (needs ≥2).
```

FR-037 is enforced by GoTrue's own "at least 2 linked identities to unlink" rule (research R4).
**Assert it; do not re-implement it** — a client-side guard on a server-enforced invariant is dead
code that can silently disagree.

---

## Validation rules

| Rule | Enforced where | FR |
|---|---|---|
| Email required, unique, verified before trusted | `auth.users` (GoTrue) | FR-001, FR-001a |
| Password strength | GoTrue project setting + client pre-check for the message | FR-006 |
| Username unique, `^[a-z0-9_]{3,30}$` | DB constraint + repository | FR-001, FR-005 |
| Display name 1–60 chars trimmed | DB constraint + repository | FR-013 |
| OTP expiry 10 min, resend cooldown 60s, newest code wins | GoTrue settings | FR-001d, FR-001f |
| Per-code attempt limit | **Not native — Phase F** (research R2/R5b) | FR-001e |
| Profile row readable only by its owner | RLS | FR-013a |
| Avatar object readable only by its owner | Storage RLS | FR-013a |
| No financial data in the profile | No such column exists | FR-013c |
| Account never left with zero sign-in methods | GoTrue unlink rule | FR-037 |

**Note on FR-001e**: a per-code incorrect-attempt limit is not something GoTrue exposes. It lands
with Phase F's server-side path, or it is an explicit deferral in the Implementation record — not
something to quietly assume the platform already does.