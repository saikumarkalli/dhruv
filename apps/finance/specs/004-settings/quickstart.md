# Quickstart — verifying the Settings control plane

How to prove this feature works. Ordered so a failure tells you which layer broke.

**Prerequisites**: `JAVA_HOME` set to the Android Studio JBR (JDK 17+). A device or emulator with a
screen lock enrolled — several checks below cannot run without one.

---

## 1. Gate the whole thing

```bash
./gradlew regressionCheck
```

All unit tests + ArchUnit + merged JaCoCo + the coverage floor. This is what CI runs; nothing below
matters if this is red.

Narrower loops while working:

```bash
./gradlew :libs:settings:testDebugUnitTest      # contribution contract + registry
./gradlew :libs:core:testDebugUnitTest          # AppLockDecision (pure)
./gradlew :apps:finance:app:testDebugUnitTest   # tier ViewModels, renderer, ArchUnit rules
```

---

## 2. Prove the contribution mechanism (the load-bearing requirement)

**Registry behaviour** — unit tests in `:libs:settings`:

- a contribution whose `moduleKey` is disabled does not appear;
- a contribution whose `moduleKey` is enabled but below `minVersion` does not appear;
- ordering is `order` then title, deterministically;
- a contribution that throws while producing rows does not remove the others.

**FR-004 / SC-004 — the diff test.** The claim is "adding a module changes no Settings file". Verify it
literally:

```bash
git status --porcelain      # clean first
# add a throwaway module with a SettingsContribution, register it in that module's Koin module
./gradlew :apps:finance:app:assembleDebug
git status --porcelain      # expect: only the new module's own files
```

If any file under `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/` appears in that
diff, the mechanism is not done — a list is hiding somewhere. Delete the throwaway module afterwards.

**ArchUnit rules** (in `:apps:finance:app`'s existing `DependencyRulesTest`):

- no class in `…app.ui.settings` references a feature-module type;
- no `SettingsContribution` implementation references a Compose type.

Both should fail if you deliberately break them — write the violating class, watch it fail, delete it.
A rule nobody has seen fail is a rule nobody knows works.

---

## 3. Prove the 19 rows survived (SC-001)

### Pre-change inventory (T002, recorded 2026-08-27)

No device with pre-existing user data was available in this session, so the baseline below is the
**shipped-default value** for each row, read from `SettingsRepositoryImpl`'s defaults and
`SettingsKeys.kt` — the same values a fresh install shows today. It is a valid SC-001 baseline
because SC-001 only requires that a row survives *whatever value it held*; a fresh install's default
is a legitimate starting value to walk forward. A future device-based pass (T109, 0b.5) should
replace this table with values captured from an actual upgraded install, per this file's original
instructions in §3 below.

| # | Row | Key | Pre-change default |
|---|---|---|---|
| 1 | identity card | — (`SessionStore`) | `SignedOut` — "Local device only — no account yet" |
| 2 | theme | `dark_mode` | `"system"` |
| 3 | accent | `accent_color_hex` | `"#F05A28"` |
| 4 | wallpaper colours (disabled) | — | always off, labelled "coming soon" |
| 5 | number format | `format_locale` | `"international"` |
| 6 | decimal precision | `decimal_precision` | `4` |
| 7 | precision preview | — (derived) | `12.3456` formatted at precision 4 |
| 8 | angle mode | `is_degree` | `true` (DEG) |
| 9 | app lock | `biometric_enabled` | `false` |
| 10 | lock history | `is_history_locked` | `false` |
| 11 | change PIN | `history_pin_code` | `"1234"` (unset sentinel) |
| 12 | consent — sync financial records | `ConsentRepository` | `false` |
| 13 | consent — read transaction SMS | `ConsentRepository` | `false` |
| 14 | consent — ask Dhruv about money | `ConsentRepository` | `false` |
| 15 | export my data | — (placeholder) | non-interactive, row removed by this feature (FR-018) |
| 16 | delete my data | — (action) | N/A — action row |
| 17 | delete my account | — (action) | N/A — action row |
| 18 | clear history | — (action) | N/A — action row |
| 19 | about version | — (derived) | matches installed `versionName`/`versionCode` |

Unit test first: assert today's `SettingsKeys` key set is a **subset** of the shipped set. A moved row
that got a new key silently resets every user's preference — this test is the only thing that catches
it, because the app will look fine.

Then on device, with a build of the previous version installed:

1. Set a distinctive value for each of the 19 rows (odd precision, non-default accent, history lock on).
2. Install this build over it — do not uninstall.
3. Walk the new tiers and confirm every value survived at its new home.

Row-by-row inventory is in [data-model.md](./data-model.md) §2.

---

## 4. Prove the lock is real

Unit tests cover the decision (`appLockState`) — cold start, each timeout, unlock scope. Run those
first; they are cheap and cover most of the rules.

On device, with a screen lock enrolled:

| Check | Expected |
|---|---|
| Enable app lock, kill the app, relaunch | Prompt before any content. Cold start always locks. |
| Cancel the prompt | No content visible. Not dimmed — absent. Screenshot to confirm. |
| Background past the timeout, return | Prompt again. |
| Open the **Calc** tab specifically after a lock | Also gated. There is no exempt surface. |
| Tap a notification while locked | Unlock first, then land on the notification's destination. |
| Cancel that unlock, then unlock successfully | Still lands on the held destination, once. |
| Cold start with a held target from a previous launch | No held target. Process death clears it. |
| Disable the screen lock on the device, relaunch | Not permanently locked out. |
| Try to enable app lock on a device with no credential | Refused, with what to enrol. |

Watch for a **flash of unlocked content** on cold start — record the screen and step through if unsure.
It is a defect, not a cosmetic issue.

---

## 5. Prove account control works

| Check | Expected |
|---|---|
| Signed out, open Settings › Account | Sign-in offered and working — no placeholder identity, no forced trip through first-run onboarding |
| Sign in, reopen | Real identity shown |
| Sign out | Session ends; calculator history still there |
| Toggle a consent off, restart | Still off; dependent surfaces show their no-consent state |
| Delete my data, confirm | Financial records gone, still signed in |
| Delete my account, confirm | Typed confirmation required; next launch is first-run |
| Turn airplane mode on, then delete my data | Reported as failed; action still available; nothing claims success |

---

## 6. Prove the assistant consent is durable (FR-036)

1. Grant assistant consent.
2. Force-stop the app and relaunch.
3. Open the assistant — it must **not** ask again.
4. Withdraw consent in Settings; open the assistant — it must show its gate before any request.

This is a defect fix, so the useful version of this check is running it against the current build first
and watching it fail.

---

## 7. Prove nothing hides outside Settings (SC-005)

Enumerate every persisted preference key in `SettingsKeys.kt` and any module's own store, then locate
each one in Settings. Any key with no row is either a violation of FR-003 or dead state that should be
deleted. Do this as a checklist once at the QA step; it is not automatable cheaply and it is exactly
the drift this feature exists to stop.

**Run 2026-08-29 (0b.5, T110).** Walked every key in `SettingsKeys.kt`. Found 9 with no row and zero
consumers anywhere in the app (confirmed by full-repo grep, not just absence of a row):
`color_calculator`/`color_converter`/`color_date`/`color_finance`/`color_time` (per-section accents,
retired by ADR-0024's single global accent) and `is_converter_enabled`/`is_date_enabled`/
`is_finance_enabled`/`is_time_enabled` (per-tab visibility, retired with the old 5-tab pager) —
**deleted** (dead state, not an FR-003 violation to fix; see `SettingsRepository.kt`'s class doc for
the full rationale). One key, `font_family`, is genuinely gapped: it IS consumed (`DhruvTheme`'s
`font` param actively branches on `DhruvFont`), but no Settings row anywhere lets a user change it —
a real FR-003 violation, not dead state. Left open rather than force-built: `platform/DESIGN-SYSTEM.md`
§2 documents a fixed 3-font system with no font-picker row anywhere in its component or screen
inventory, so adding one now would be new, unspecified UI scope, not a mechanical fix. Tracked as a
deferred finding, not silently dropped. `sync_enabled` is unconsumed too but is not a violation —
`AppSettings.kt`'s own doc comment already states it is a deliberate Phase-2 stub.

## 7a. Inert-row review (SC-011, T112)

Every shipped row either does something when interacted with, or is visually marked non-interactive
(no chevron, disabled/greyed styling) — never something that looks operable and silently changes
nothing.

**Run 2026-08-29.** Walked every row category shipped through 0b.1–0b.4:

| Row | Looks interactive? | Actually does something? |
|---|---|---|
| `DisabledSwitchRow` (wallpaper colours) | No — rendered at `SETTINGS_DISABLED_ALPHA`, `Switch(enabled = false)` | N/A by design — the greyed styling *is* the "not yet" signal (design system §7's `disabled` state) |
| `SettingsRow.Info` rows (supported-currency count, precision preview, etc.) | No — `showChevron = false`, no `onClick` | Read-only by design, matches its visual affordance |
| Every `Toggle`/`Choice`/`Stepper`/`Action`/`SecretText`/`Navigate` row shipped | Yes | Yes — all wired to a real repository write or navigation target; none is a stub with a live-looking control |

No row was found that renders as tappable/toggleable while changing nothing. The former "Export my
data" `PlaceholderRow` — the one row that used to fail this check (rendered a row with no real
action) — was removed outright in 0b.2 (T053) rather than kept as a disabled placeholder.

## 7b. Tap depth (SC-002, T111)

Every setting is reachable in at most three taps from the Settings entry point; the three quick rows
need no navigation past opening Settings at all.

**Run 2026-08-29 (by inspection, not device — no emulator available).**

- **Quick rows** (theme, accent, app lock): tap 1 opens Settings (top-bar icon on every primary tab)
  — the three rows are directly interactable on that same screen. 0 navigation taps, matching FR-002.
- **Account / App tier rows** (sign-in, sign-out, consent, erasure, appearance, security,
  notifications, app details): tap 1 opens Settings, tap 2 opens Account or App, tap 3 interacts with
  the row itself (already on-screen, no further push). 2 navigation taps.
- **A module's own rows** (calculator, currency, unit, assistant): tap 1 opens Settings, tap 2 opens
  the module's entry from the modules tier, tap 3 interacts with the row. 2 navigation taps.

No shipped row requires a 4th tap. The one structural exception is `Choice` rows with >3 options,
which open a plain `ListGroupRow` list (contract §2 rule — no `SelectionSheet` component exists yet,
design system §5.2 batch B9) — selecting an option is still tap 3, since the list itself renders
inline rather than pushing a new screen.

---

## 8. Accessibility and theming (SC-012, SC-013)

- Every settings screen in light and dark, at smallest and largest system text size — no clipped or
  truncated row label.
- TalkBack over each tier: every switch, icon-only control and destructive action announces its
  subject and current state.
- Destructive rows are visually distinct and never the first-focused element in their group.

## 9. Screen-state matrix for 0b.1's screens (T039, FR-044, design system §7)

Only the applicable states are listed per screen — `platform/DESIGN-SYSTEM.md` §7's full set is
`default | loading | empty | error | offline | signed-out | not-configured | disabled`.

| Screen | default | empty | error | not-configured | Notes |
|---|---|---|---|---|---|
| `SettingsScreen` (top level) | quick rows + Account/App entries + modules tier | `EmptyStateCard` when the modules tier resolves to zero contributions | — (no network/async call at this level) | — | No loading state: contributions are already-resolved Koin values, not an async fetch (contract §4 rule 13) |
| `ModuleSettingsScreen` | renders the contribution's groups/rows | *(gap — see below)* | `FeatureErrorCard` when a row's `Flow` throws while collecting (`SET-ARCH-007`, `ModuleEntryIsolationTest`) | `NotConfiguredCard` when `SettingsDetailContent` resolves no contribution for the route's `moduleKey` (a stale/foreign deep link) | `disabled` is structurally unreachable — `SettingsRegistry` already filters a disabled `moduleKey` out of the tier, so its entry can never be opened |
| `AppSettingsScreen` (Appearance area, 0b.1 scope) | theme/accent/wallpaper rows | — | — | — | No network/auth dependency; the other five states don't apply to this area |

**Known gap, not closed by 0b.1**: a contribution with zero groups/rows has no defined empty state
in `ModuleSettingsScreen` — it would render as a blank scrollable column. None of the three real
0b.1 contributions (calculator/currency/unit) ship with zero rows, so this is unreached today; a
future contribution shipping empty must not rely on this being handled. Tracked as a follow-up for
whichever sub-phase first needs it, not resolved here.

### 0b.3 additions (T076) — Security and Notifications areas, and the lock gate itself

| Screen | default | disabled | not-configured | Notes |
|---|---|---|---|---|
| `AppSettingsScreen` — Security area | app lock switch, auto-lock timeout (shown only when lock is on), hide-amounts, legacy history-lock/PIN | — | — | No loading/empty/error/offline/signed-out states apply — everything here is a local preference read, no network |
| `AppSettingsScreen` — Notifications area | master switch | — | `NotificationManagerCompat.areNotificationsEnabled() == false` renders the permission-denied banner above the master switch (`SET-UI-012`) — this is the area's `not-configured`-shaped state, named "permission-denied" to match the design system's own vocabulary for this exact case | No per-channel rows exist here to have their own states (contract §3 rule 10) |
| `AppLockGate`'s locked surface | static app chrome + retry button, shown only after a failed/cancelled attempt (gate §2 rule 9) | — | — | Deliberately has no loading/empty/error states of its own — it is binary (LOCKED shows this, UNLOCKED shows `content()`), and `SET-UI-001`/`SET-UI-003` already specify exactly what may render while LOCKED |

### 0b.2 additions (T056) — AccountSettingsScreen

| State | Trigger | Rendering |
|---|---|---|
| signed-out | `SessionState.SignedOut`/`Expired` | Identity card shows "Local device"; a "Sign in with Google" button replaces the Sign out row |
| offline | Sign-in tapped with no validated network (`NET_CAPABILITY_VALIDATED` false) | Inline error text under the button before Credential Manager is ever invoked — same pre-flight check `SignInScreen` already uses |
| loading | A sign-in attempt in flight | Button disabled, label swaps to "Signing in…" |
| error | Credential Manager cancellation/provider error, or a backend sign-in failure after a successful credential step | Inline error text under the button, cleared on the next attempt; erasure failures use the existing Toast pattern (unchanged from 0b.1's `SettingsAccountBody`) |
| signed-in (default) | `SessionState.Active` | Identity card shows avatar/name/email; Sign out button; consent switches; delete-my-data/delete-my-account rows |

## 10. 0b.3 verification status (T077, T078) — recorded honestly rather than claimed

**T077 (manual device pass)**: **not performed this session** — no physical device or emulator was
attached to the environment this sub-phase was implemented in. The device-pass table in §4 above
is unchanged and still the correct procedure; it needs to be walked on a real device before this
sub-phase is considered fully verified end-to-end, not only unit-tested. Recorded as a deferred
manual step, not silently skipped.

**T078 (hide-amounts on all three surfaces)**: **partially verified**. The "screen" surface is
proven — `HideAmountsTest` (`Paise`/`MASKED_TOKEN`) and `MoneyText`'s `LocalHideAmounts`
composition local wiring. The "widget" and "notification" surfaces named in FR-025/SC-010 **do not
exist yet** in this codebase — no Glance widget is built (platform decision: a widget ships,
`docs/superpowers/...` T128, but is not yet implemented) and no notification-posting code exists
(alerts are 0b.4+ scope). `CurrencyFormatter.MASKED_TOKEN` was added as the capability those two
surfaces will need, but there is nothing to wire it into today. This mirrors 0b.1 §9's "known gap"
pattern: not resolved here, not silently dropped — whichever phase builds the widget or the first
real notification-posting code must consume this masking capability from day one, not add it later.

### 0b.4 additions (T104) — module entry screen and App details area

| Screen | default | empty | not-configured | disabled | Notes |
|---|---|---|---|---|---|
| `ModuleSettingsScreen` | its on/off row + groups/rows | — (contract §1's zero-rows gap from 0b.1 §9 still applies; unreached today) | `NotConfiguredCard` when no contribution resolves for the route (unchanged from 0b.1) | `EmptyStateCard` (`settings_module_disabled_empty`) when the module's own on/off toggle is off — this is the *module's* disabled state, distinct from the *tier entry's* not-configured state above; the on/off toggle itself stays visible so this is always the "way back" (FR-034/`SET-UI-011`) | Consent-gated content (FR-035) is a third state on top of these two: on/on, requires-consent-ungranted → `ConsentNeededRow` instead of the groups |
| `AppSettingsScreen` — App details area | version/privacy/licences/source rows | — | — | — | Update-check and replay-intro rows are absent, not disabled — no update channel or on-demand onboarding-replay route exists yet (FR-043); this is `not-configured`-shaped but expressed as row-absence per contract convention, not a card, since the surrounding area itself is fully configured |

**Known gap, not closed by 0b.4**: `ModuleSettingsScreen`'s consent-needed state (`ConsentNeededRow`)
has no `not-configured`/`disabled` variant of its own — it is a single row, not a full-screen state
card, since only one contribution (`assistant`, and only for its *own* consent) exercises this path
today and a single ungranted-consent row was judged sufficient. A future module gating several
groups behind different consents would need this revisited.