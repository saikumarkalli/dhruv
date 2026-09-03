# Contract — Profile API (`identity.profiles` + avatar storage)

Reached through a **new fourth Retrofit instance**, `SupabaseClientFactory.identityRetrofit`:

| Property | Value | Why |
|---|---|---|
| Base path | `{SUPABASE_URL}/rest/v1/` | PostgREST |
| OkHttp chain | **`authClient`** — `AuthInterceptor` only | Profile is account data, deliberately outside the financial-sync consent gate (FR-013a) |
| Schema header | `Accept-Profile: identity` / `Content-Profile: identity` | ADR-0033 — omitting it does not error, it silently `404`/`406`s against `public` |

**Why not an existing instance** ([research.md](../research.md) R8): `dataRetrofit` is
consent-gated (would hide a person's own name when sync is off); `erasureRetrofit` has the right
chain but its doc comment forbids other callers and it sends no schema header.

> **This is a deliberate exception to constitution Article VIII, not an oversight.** Phase C ships a
> test asserting `identityRetrofit` carries no `ConsentInterceptor` **and** `dataRetrofit` still does.
> Without that test, a future reader correctly recognises the gap and "fixes" it — either by gating
> profile calls (breaking FR-013a) or by adding a bypass flag to the gated client (breaking Article
> VIII for everything). The test states the intent in the one place that cannot drift.

## Endpoints

### Read own profile — FR-013

```http
GET /rest/v1/profiles?user_id=eq.{uid}&select=*
Accept-Profile: identity
```

RLS returns the caller's row only. Empty result = profile not yet created (skipped setup, FR-023b) →
render FR-014's placeholder, **not** an error state.

### Create / update own profile — FR-013, FR-016, FR-033

```http
POST /rest/v1/profiles          # upsert
Content-Profile: identity
Prefer: resolution=merge-duplicates, return=representation
{ "user_id": "...", "display_name": "...", "username": "...", "avatar_path": "..." }
```

- `user_id` must equal `auth.uid()` — RLS enforces it; the client sends it because `user_id` is the PK.
- **FR-016**: a Google sign-in after an edit must not overwrite. Enforced by *never* writing Google
  values on any sign-in except the first (`updated_at`/row absence distinguishes them). The initial
  Google copy (FR-015) happens **only when no row exists**.
- **FR-013c**: no financial fields exist in the payload or the table.

**Errors**: `409` on username uniqueness → "that username is taken" (FR-005).

### Username availability — FR-005

```http
GET /rest/v1/profiles?username=eq.{candidate}&select=user_id
```

**Returns nothing useful under RLS** — a candidate belonging to someone else yields an empty result,
identical to "available". So availability is checked at **write time** via the `409`, not by a
pre-flight lookup.

**Do not add a `security definer` availability RPC.** It would be callable by `anon` and turn into a
username-enumeration endpoint. The `409` path costs one round trip and leaks nothing.

## Avatar storage — FR-013a, FR-014, FR-018

Private bucket `avatars`, object path `{auth.uid()}/avatar.<ext>`.

| Operation | Call |
|---|---|
| Upload / replace | `POST /storage/v1/object/avatars/{uid}/avatar.jpg` |
| Fetch | `POST /storage/v1/object/sign/avatars/{uid}/avatar.jpg` → signed URL |
| Delete | `DELETE /storage/v1/object/avatars/{uid}/avatar.jpg` |

- Storage RLS scopes every operation to the caller's own path prefix.
- **Private, not public.** A public bucket makes every photo world-readable by URL — beyond what
  FR-013b's disclosure covers.
- Client validates format and size **before** upload (spec Edge Cases: rejected with a clear message,
  never a silent failure).
- `avatar_path` in the profile stores the path, never a signed URL — signed URLs expire.

## Erasure — FR-018

`public.delete_my_data()` and `public.delete_my_account()` are amended to remove the profile row
**and the stored avatar object**. `create or replace function`, never drop+recreate (ADR-0033's
precedent).

**O4, VERIFY-AT-RED in Phase B**: whether the object can be deleted from SQL, or whether the client
must delete it before calling the RPC. If it must be a client call, the ordering matters — delete the
object first, because after `delete_my_account()` there is no session left to authorise the storage
call.

## Repository shape

`com.dhruv.finance.data.identity.ProfileRepository` — features reach it only through this repository
(Article III), never through `ProfileApi` directly.

```kotlin
interface ProfileRepository {
    val profile: StateFlow<Profile?>
    suspend fun refresh(): Result<Profile?>
    suspend fun updateName(displayName: String): Result<Unit>
    suspend fun updateUsername(username: String): Result<Unit>   // 409 -> UsernameTaken
    suspend fun uploadAvatar(bytes: ByteArray, mime: String): Result<Unit>
    suspend fun removeAvatar(): Result<Unit>
    suspend fun seedFromGoogleIfAbsent(name: String?, avatarUrl: String?): Result<Unit>  // FR-015
}
```

`Result` over exceptions, matching `AuthRepository`. **Preserve the shipped
`CancellationException`-rethrow pattern** — a bare `runCatching` swallows cancellation and breaks
structured concurrency, a defect this repo has already fixed once in `AuthRepositoryImpl`.