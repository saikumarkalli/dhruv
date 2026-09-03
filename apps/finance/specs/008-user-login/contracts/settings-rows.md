# Contract — Settings › Account rows added by 008

Extends the **shipped** `AccountSettingsScreen` (spec `004-settings`, sub-phase 0b.2). This feature
adds rows to that screen; it does not build a second account surface (FR-039, research R9).

Registration follows `apps/finance/specs/004-settings/contracts/settings-contribution.md`
(FR-003/FR-004): `single(qualifier = named(moduleKey)) { … }` — **the qualifier is required**. A
module without a contribution is silently absent from Settings, with no error and nothing to notice
in review.

## Existing rows (unchanged)

Sign in · Sign out · the three consent switches · Export · Delete my data · Delete my account
(type-to-confirm).

## New rows

| # | Row | Type | Visible when | FR |
|---|---|---|---|---|
| 1 | **Profile** | navigates → `account/profile` | signed in | FR-033 |
| 2 | **Change password** | navigates → `account/password` | signed in **and** a password identity exists | FR-034 |
| 3 | **Sign-in methods** | navigates → `account/methods` | signed in | FR-035, FR-036 |

### 1. Profile — `account/profile`

Display name (`NxTextField`, 1–60 chars trimmed) · photo (tap to pick/replace/remove;
`InitialsTile` placeholder when unset, FR-014) · username (`NxTextField`, `409` → "that username is
taken").

**FR-016**: an edit here is never overwritten by a later Google sign-in — the Google copy runs only
when no profile row exists (FR-015).

### 2. Change password — `account/password`

Current password · new password · confirm. Strength policy explained on rejection (FR-006). On
success: other sessions invalidated (FR-008) and the person **stays signed in on this device**.

**Hidden entirely** for a Google-only account — `account/methods` offers "Set a password" instead. A
visible-but-broken row is worse than an absent one.

### 3. Sign-in methods — `account/methods`

Lists Password and Google, each showing linked / not linked (FR-035).

- Not linked → **Link** (Google: the Credential Manager flow, **duplicated** here per Article III and
  `SET-ARCH-003`, never imported from `onboarding`. Password: set one, no OTP — the email is already
  verified, FR-038).
- Linked, and it is not the last one → **Unlink**, behind `ConfirmDangerDialog`.
- **Last remaining method → unlink is not offered.** GoTrue refuses it server-side anyway (needs ≥2
  identities); the UI simply does not present an action that cannot succeed. **Assert the server rule
  with a test; do not re-implement it client-side** (research R4).

## States (design system §7)

| State | Component | When |
|---|---|---|
| signed-out | `SignedOutCard` | no session — the three new rows are absent, existing sign-in shows |
| loading | `SkeletonBlock` | first profile load only; silent on refresh |
| error | `RetryErrorCard` | profile fetch failed |
| offline | `OfflineBanner` | cached profile shown; edits disabled with a reason |

## Design system compliance

Tokens and existing components only (FR-024, Article V/VI): `ListGroup`/`ListGroupRow`,
`NxTextField` (its `errorMessage` param already shipped in 004-settings 0b.4), `NxButton`,
`ConfirmDangerDialog`, `InitialsTile`, `SectionLabel`. Every string in `strings.xml` from birth
(FR-028) — 004-settings 0b.5 already had to retrofit 11 hardcoded strings on this exact screen; do
not repeat that.

**One new component, in `:libs:core`, not here**: `PinEntry` (design system §5.2 batch B2), for OTP
entry. Move its row from §5.2 to §5.1 in the same change that builds it.

## Orphan-preference rule

004-settings FR-003: **every persisted key has exactly one row**. 0b.5's audit removed 9 orphaned
keys. Any preference this feature persists gets exactly one control here — and nothing is persisted
that no row exposes.