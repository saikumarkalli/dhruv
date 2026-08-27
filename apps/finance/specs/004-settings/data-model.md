# Data Model — Settings control plane

No database change. No Supabase migration, no Room entity, no schema. Everything here is preference
state in the two DataStores that already exist, plus in-memory types for the contribution contract.

---

## 1. Storage placement

| Store | Holds | Why |
|---|---|---|
| `app_settings` (plaintext DataStore) | every ordinary preference — theme, accent, calculator prefs, app-lock policy, module preferences | already the home of all 19 existing rows; nothing here is a secret |
| `secure_settings` (EncryptedDataStore) | the personal AI key, the calculator history PIN | secrets, never plaintext (`SettingsKeys` already routes the AI key here) |
| `SessionStore` (`:apps:finance:data`, encrypted) | auth session and tokens | Phase 1 owns it; Settings reads and clears, never re-implements |
| `ConsentRepository` (`:apps:finance:data`) | the three DPDP consent switches | Phase 1 owns it; Settings is a second, equal consumer alongside onboarding |

**Rule that governs all of it (constitution IX)**: a preference key is append-only. Moving a row
between entries or tiers MUST reuse its existing key string. `SettingsKeys.kt` already says this at the
top of the file; the migration in this feature is the first real test of it.

---

## 2. Existing preference keys — reused unchanged

These 19 rows migrate into the new tiers. Keys do not change; only their home does.

| Current row | Key | New home |
|---|---|---|
| identity card | — (derived from `SessionStore`) | Account |
| theme | `dark_mode` | App › Appearance **and** quick row |
| accent | `accent_color_hex` | App › Appearance **and** quick row |
| wallpaper colours (disabled) | — | App › Appearance (stays labelled unavailable) |
| number format | `format_locale` | Modules › Calculators |
| decimal precision | `decimal_precision` | Modules › Calculators |
| precision preview | — (derived) | Modules › Calculators |
| angle mode | `is_degree` | Modules › Calculators |
| app lock | `biometric_enabled` | App › Security **and** quick row |
| lock history | `is_history_locked` | App › Security (labelled legacy) |
| change PIN | `history_pin_code` | App › Security (legacy, conditional) |
| consent — sync financial records | `ConsentRepository` | Account |
| consent — read transaction SMS | `ConsentRepository` | Account |
| consent — ask Dhruv about money | `ConsentRepository` | Account |
| export my data | — (placeholder today) | Account — **row removed until it works** (FR-018) |
| delete my data | — (action) | Account |
| delete my account | — (action) | Account |
| clear history | — (action) | Modules › Calculators |
| about version | — (derived) | App › App details |

The `font_family` and `sync_enabled` keys also exist in `AppSettings` but have no row today;
`sync_enabled` is an unused stub and is **not** given a row by this feature (FR-043 — a row must not
imply a capability that does not exist).

---

## 3. New preference keys

| Key | Type | Default | Owner | Notes |
|---|---|---|---|---|
| `app_lock_timeout` | String enum | `after_1_min` | App › Security | option ids append-only; `immediate`, `after_1_min`, `after_5_min`, `after_15_min` |
| `hide_amounts` | Boolean | `false` | App › Security | read by the money formatting path, not per screen (research R5) |
| `notifications_master` | Boolean | `true` | App › Notifications | off suppresses every module's alerts (FR-026) |
| `assistant_consent_granted` | Boolean | `false` | Account (consent) / Modules › Assistant | **replaces the in-memory flag** that forgets on restart (FR-036) |
| `module_enabled_<moduleKey>` | Boolean | `true` | Modules | per optional module; retained when off so re-enabling restores (FR-032) |

`biometric_enabled` is **reused** for app lock rather than replaced — it is the key the existing switch
already writes, so a user who turned it on before this feature keeps their choice, and it starts
actually enforcing.

---

## 4. In-memory types (the contract)

Defined by [contracts/settings-contribution.md](./contracts/settings-contribution.md); shapes repeated
here only for the entity view.

| Type | Fields | Lifetime |
|---|---|---|
| `SettingsContribution` | `moduleKey`, `title`, `summary`, `order`, `groups` | one per module, resolved at Settings open |
| `SettingsGroup` | `label?`, `rows` | inside a contribution |
| `SettingsRow` (sealed) | `key`, `label`, `description`, `enabled` + per-variant payload | inside a group |
| `ChoiceOption` | `id` (persisted, append-only), `label` | inside a `Choice` row |
| `ConfirmSpec` | `title`, `body`, `confirmLabel`, `typeToConfirm: Boolean` | on destructive `Action` rows |
| `SettingsRegistry` | resolved + filtered + ordered contributions | per Settings open |

---

## 5. App-lock state

| Type | Fields | Where |
|---|---|---|
| `LockTimeout` | enum: `Immediate`, `After1Min`, `After5Min`, `After15Min` | `:libs:core` |
| `LockState` | enum: `LOCKED`, `UNLOCKED` | `:libs:core` |
| `AppLockPolicy` | `enabled`, `timeout`, `hideAmounts` | derived from `AppSettings` |
| held target | `NavTarget?`, cleared on dispatch and on process death | shell state in `MainActivity`, not persisted |

Held targets are deliberately **not** persisted: surviving process death would mean a link the user
never authenticated for is waiting after a cold start, which contradicts the gate's rule 1.

---

## 6. State transitions worth stating

**Module enable/disable**
`enabled → disabled`: entry leaves the modules tier, module leaves navigation and content, preferences
retained. `disabled → enabled`: entry returns with previous values. There is no "reset on disable".

**Consent withdrawal**
`granted → withdrawn`: persisted immediately; dependent surfaces show their no-consent state on their
next read, without Settings notifying them individually — they already observe the consent state.

**Account erasure**
`delete my data`: financial records removed, session and consent intact.
`delete my account`: records removed, account row removed, session cleared, next launch is first-run.
A failure at either step leaves state unchanged and the action available (FR-016).

**App lock**
Enabling requires an enrolled device credential; disabling takes effect immediately and clears any
locked state. Changing the timeout applies from the next backgrounding, never retroactively.