# Contract — routes added and changed by 008

Registry rows land in `apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md`
at Phase G (Article Xa: registry rows change in the same PR as the behaviour).

## Onboarding (pre-shell, bare full-frame, no tab bar)

Order fixed by **FR-023a**: sign-in/up → OTP → consent → skippable profile setup.

| Route | Screen | State | Notes |
|---|---|---|---|
| `signin` | `SignInScreen` | **amended** | Dual-method on ONE screen (FR-022): identifier + password, "Sign in with Google", "Create an account", **and the existing "Use offline — calculators only"** (FR-031) |
| `signup` | `SignUpScreen` | new | Email + optional username + password |
| `verify` | `OtpVerifyScreen` | new | 6-digit entry, resend cooldown, expired-vs-wrong distinction. Resumable (FR-001g) |
| `forgot` | `ForgotPasswordScreen` | new | Identifier entry; always the same confirmation (FR-004) |
| `reset` | `ResetPasswordScreen` | new | Reached only after `verify(type=recovery)` |
| `consent` | existing consent screen | unchanged | Already shipped as A3 |
| `profilesetup` | `ProfileSetupScreen` | new | **Skippable** (FR-023b); after consent, not before |

**Regression watch**: the offline option is the single easiest thing to lose while rewriting
`SignInScreen` into a dual-method layout. It has its own `AUTH-*` scenario row for exactly that
reason.

### Flow

```text
signin ──password──> signup ──> verify ──> consent ──> profilesetup ──> shell
   │                              ▲                         │(skip)
   │                              └── resumes here on        └────────> shell
   │                                  next open (FR-001g)
   ├──Google──────────────────> consent ──> profilesetup ──> shell
   │                            (no OTP — FR-038)
   ├──forgot──> verify ──> reset ──> signin
   └──offline─────────────────────────────────────────────> shell (no OTP, no profile setup — FR-023c)
```

## Settings › Account (detail routes, back top bar, no tab bar)

Parents to the existing `SettingsAccount` route. Registered through the Settings contribution
mechanism (FR-039), **not** as a parallel surface.

| Route | Screen | Notes |
|---|---|---|
| `account/profile` | `ProfileEditScreen` | Name + photo (FR-033) |
| `account/password` | `ChangePasswordScreen` | Current + new; invalidates other sessions (FR-034/FR-008) |
| `account/methods` | `LinkedMethodsScreen` | View (FR-035) + link missing (FR-036); unlink refused at the last method by GoTrue (FR-037) |

`account/password` is **hidden** when the account has no password identity, and `account/methods`
offers "set a password" instead — a change-password row on a Google-only account is a dead end.

## Navigation law (design system §6)

| Rule | How it is met |
|---|---|
| N1 tab roots have no back arrow | Onboarding routes are pre-shell; Settings routes are details with one back arrow |
| N2 one back arrow to a single parent | Each Settings route's parent is `SettingsAccount`; onboarding is a linear flow |
| N3 sheets dismiss, never navigate | None of these are sheets |
| **N4 add/edit forms confirm on discard** | `signup`, `account/profile`, `account/password` confirm on back with unsaved input (FR-030) |
| N7 light + dark from one switch | FR-025 |

**Back-press precedence** stays in `resolveBackAction` (`libs/core/.../navigation/BackContract.kt`) —
never re-derived inline.

**Cross-feature navigation by `NavTarget` id, never a class reference** (Article III). Settings does
not import an onboarding screen; the Google-linking call is **duplicated**, matching
`SET-ARCH-003`'s existing precedent rather than opening a `feature → feature` edge.

## Fault isolation

Every new route is wrapped in `FeatureHost` (Article IV, FR-029). Onboarding has no feature flag — it
gates everything else — so its wrapper carries the error path only, matching the shipped arrangement.