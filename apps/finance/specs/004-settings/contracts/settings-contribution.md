# Contract — Settings contribution

**What this is**: the interface a module implements to appear in Settings' modules tier. It is the
only way a module gets settings, and it is the contract that makes FR-004/FR-007 true — the shell
never names a module, and shipping a module ships its settings.

**Where it lives**: `:libs:settings`, package `com.dhruv.settings.contribution`. Every feature module
already depends on `:libs:settings`, and `:libs:settings` depends only on `:libs:core`, so this adds
no edge to the dependency graph.

**Stability**: additive only. Adding a row type or an optional field is safe; removing or renaming one
breaks every module at once and requires the same treatment as a shipped enum constant (constitution
IX — append-only).

---

## 1. What a module publishes

```
SettingsContribution
  moduleKey     : String        // the module's existing feature-flag key — the tier gates on it
  title         : String res id // entry name in the modules tier
  summary       : String res id // one line under the title; what this entry controls
  order         : Int           // stable sort position; ties break on title
  groups        : List<SettingsGroup>
```

```
SettingsGroup
  label : String res id?        // submodule name; null = ungrouped rows at the top of the entry
  rows  : List<SettingsRow>
```

**Rules**

1. `moduleKey` MUST be a key that exists in the app's feature-flag file. An unknown key resolves to
   disabled, so the entry silently never appears — this is a wiring bug and MUST be caught by a test,
   not discovered by a user.
2. All user-visible text is a string resource id, never a literal (design system §10).
3. A contribution MUST NOT reference a Compose type. Enforced by ArchUnit; see §5.
4. A module contributes **at most one** contribution. Submodules are `SettingsGroup`s inside it, not
   sibling entries — that is what keeps the tier a list of modules rather than a flat row dump.

---

## 2. Row vocabulary (closed set)

Every row carries: `key` (stable preference key, append-only), `label` (string res), `description`
(string res, plain language — FR-043), and `enabled` state.

| Row type | Carries | Renders as | Use for |
|---|---|---|---|
| `Toggle` | `value: Flow<Boolean>`, `onChange: suspend (Boolean) -> Unit` | switch row | on/off preferences, alert on/off |
| `Choice` | `options: List<ChoiceOption>`, `selected: Flow<String>`, `onSelect: suspend (String) -> Unit` | segmented row (≤3 options) or selection sheet (>3) | enumerated preferences, thresholds, timeouts |
| `Stepper` | `value: Flow<Int>`, `range`, `step`, `onChange: suspend (Int) -> Unit` | stepper row | bounded numeric preferences |
| `Action` | `onInvoke: suspend () -> Result<Unit>`, `confirm: ConfirmSpec?`, `destructive: Boolean` | tappable row; destructive styling and confirm dialog when specified | clear, reset, run-now |
| `Navigate` | `target: NavTarget` | chevron row | anything needing bespoke UI — the module's own screen |
| `Info` | `value: Flow<String>` | read-only value row | version, status, last-run |

**Rules**

5. A row's `key` MUST equal the persisted preference key it reads and writes. When a row moves between
   entries or tiers, the key does not change (constitution IX; a changed key silently resets every
   user's preference).
6. `Choice` option ids are persisted values and are therefore append-only — never rename a shipped
   option id.
7. `Action` returns a `Result`. A failure MUST surface to the user (FR-016's shape, generalised); the
   renderer shows the failure and leaves the row available.
8. `Navigate` is the only escape hatch to custom UI. A module MUST NOT model a bespoke control as a
   `Choice` with one option or an `Action` that opens a dialog it drew itself.
9. Writes are `suspend` and the renderer applies them immediately — there is no save action anywhere in
   Settings (FR-042). A write that throws reverts the displayed value and shows why.

---

## 3. Alerts

A module that defines notification channels contributes one `Toggle` per channel, plus any threshold or
delivery-time rows as `Choice`/`Stepper` in the same group.

10. Every channel in the app's notification channel registry MUST have exactly one control, in the
    module that owns the channel. No alert control may live in the App tier — that tier holds only the
    app-wide master switch and the system-permission state (FR-026, FR-030).
11. An alert whose source feature has not shipped MUST be absent, not present-and-inert (FR-031).

---

## 4. Resolution, ordering and failure

The shell's `SettingsRegistry`:

1. resolves every contribution from the DI container by type — never from a list;
2. drops any whose `moduleKey` is not enabled for the running version;
3. sorts by `order`, then `title`;
4. renders each entry inside a `FeatureHost` keyed on `moduleKey`.

12. A contribution that throws while producing rows degrades to that module's error card inside
    Settings. The tier and every other entry keep working — Settings never blanks (constitution IV).
13. Resolution happens once per Settings open, not per recomposition.
14. When a module is turned off, its entry disappears but its stored preferences are retained, so
    re-enabling restores them (FR-032).

---

## 5. Enforcement

| Rule | Enforced by |
|---|---|
| Settings never names a module | ArchUnit: no class in `com.dhruv.finance.app.ui.settings` references a feature-module type |
| Contributions stay declarative | ArchUnit: no `SettingsContribution` implementation references a Compose type |
| `moduleKey` is a real flag key | Unit test over all registered contributions against the flag asset |
| Channel ↔ control is 1:1 | Unit test comparing the channel registry to the contributed alert toggles (SC-006) |
| Adding a module changes no Settings file | Verification step: add a throwaway module, diff (SC-004) |
| Preference keys are append-only | Unit test asserting today's key set is a subset of the shipped set (SC-001, constitution IX) |