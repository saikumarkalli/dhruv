---
description: "Task list for Settings — application control plane (Phase 0b, split 0b.1–0b.5)"
---

# Tasks: Settings — application control plane (Phase 0b)

**Input**: Design documents from `apps/finance/specs/004-settings/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/settings-contribution.md](contracts/settings-contribution.md),
[contracts/app-lock-gate.md](contracts/app-lock-gate.md), [quickstart.md](quickstart.md)

**Tests**: **Required, not optional.** Constitution Article I is NON-NEGOTIABLE — RED → GREEN →
REFACTOR, and every test cites the `SET-*` scenario ID it satisfies from
`apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §13. A test with no
citation is a review-blocking finding. Article II is **pre-satisfied**: all 50 `SET-*` rows were
written and reviewed 2026-08-19, so no task below authors catalog rows — they are cited and closed,
never recreated.

**SA step also pre-satisfied**: surface registry §4 was rewritten to the control-plane model on
2026-08-19 (spec FR-005), together with §2's channel↔control rule, §1's Automation owner, and
`platform/DESIGN-SYSTEM.md` §11's global restatement of the same rule. No task below re-does that.

**No new Gradle module.** This feature adds code only to `:libs:settings`, `:libs:core` and
`:apps:finance:app` — all three already in `coveredModules` (root `build.gradle.kts`) and in
`PACKAGE_TO_MODULE` (`scripts/ci/regression_summary.py`). T003 verifies that rather than assuming it.

## Why this is split into five sub-phases

Phase 0b as one unit was 95 tasks and one merge — larger than any phase in the design-v1 plan, and
large enough that nothing ships until all of it works. It is split into **0b.1 … 0b.5**, each of
which:

- ends green on `./gradlew regressionCheck`,
- closes its own `SET-*` catalog rows and updates the §14 coverage summary,
- is independently shippable and independently valuable,
- and is a separate merge.

The split follows the story seams already in spec.md, with one deliberate move: **Appearance sits in
0b.1, not with the rest of the App tier.** The quick rows (theme, accent, app lock) land with the
top level in 0b.1, and FR-002 requires each quick row to mirror its owning section's row — so
Appearance has to arrive with them or the mirror requirement is unsatisfiable until 0b.3. App lock's
quick row is the exception and is stated explicitly in 0b.1's notes.

| Sub-phase | Delivers | Stories | Tasks |
|---|---|---|---|
| **0b.1** | Control plane + Appearance | US1 | T001–T041 |
| **0b.2** | Account & identity | US2 | T042–T059 |
| **0b.3** | App lock & privacy | US3 | T060–T080 |
| **0b.4** | Module conventions, assistant, app details | US4, US5, US6 | T081–T108 |
| **0b.5** | Feature-level verification & ratchet | — | T109–T118 |

**Constitution workflow mapping**: SA = done · QA-author = done · Backend/Android = each sub-phase's
own body · QA-close + Sec + Checkpoint = the last three tasks of each sub-phase, with the
cross-cutting remainder in 0b.5.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: US1–US6, matching spec.md's six user stories
- Every path below is a real path in this repo, not a placeholder

## Path Conventions

`:libs:core` uses `src/main/kotlin` + `src/test/kotlin`; `:libs:settings` and `:apps:finance:app`
use `src/main/java` + `src/test/java`, except the app's ArchUnit tests which live under
`src/test/kotlin/com/dhruv/finance/app/arch/`. Tests mirror their module's tree — there is no
separate `tests/` root.

---

# Sub-phase 0b.1 — Control plane + Appearance

**Ships**: Settings restructured into its three tiers, assembling the modules tier from
contributions; the calculators and converters appear as module entries; theme and accent work from
both the quick row and Appearance. Nothing regressed.

**Independent value**: Settings is better organised and self-assembling, and every later phase adds
rows instead of editing a central list — even if 0b.2–0b.5 never shipped.

## 0b.1 Setup

- [ ] T001 Create the package directory `libs/settings/src/main/java/com/dhruv/settings/contribution/` and its test twin `libs/settings/src/test/java/com/dhruv/settings/contribution/`
- [ ] T002 [P] Record the pre-change Settings row inventory — walk the running app and write the 19 rows with their current values into `apps/finance/specs/004-settings/quickstart.md` §3's checklist form, so SC-001 has a baseline that was observed rather than remembered
- [ ] T003 [P] Verify `:libs:settings`, `:libs:core` and `:apps:finance:app` are present in `coveredModules` (root `build.gradle.kts`) and that `com/dhruv/settings`, `com/dhruv/core`, `com/dhruv/finance/app` are present in `PACKAGE_TO_MODULE` (`scripts/ci/regression_summary.py`) — they are today; this task fails loudly if that changed
- [ ] T004 Confirm `./gradlew regressionCheck` is green before any change lands, and record the merged line-coverage % as this feature's baseline

## 0b.1 Foundational (blocks every sub-phase)

**⚠️ CRITICAL**: no story work in any sub-phase begins until T005–T017 are complete.

- [ ] T005 [P] Create the contract types in `libs/settings/src/main/java/com/dhruv/settings/contribution/SettingsContribution.kt` (`moduleKey`, `title`, `summary`, `order`, `groups`) and `SettingsGroup.kt` (`label?`, `rows`) exactly as `contracts/settings-contribution.md` §1 defines them
- [ ] T006 [P] Create the closed row vocabulary in `libs/settings/src/main/java/com/dhruv/settings/contribution/SettingsRow.kt` — sealed `Toggle`/`Choice`/`Stepper`/`Action`/`Navigate`/`Info` plus `ChoiceOption` and `ConfirmSpec`, each row carrying `key`, `label`, `description`, `enabled` per contract §2. No Compose types anywhere in this file
- [ ] T007 Add the five new preference keys to `libs/settings/src/main/java/com/dhruv/settings/SettingsKeys.kt` — `app_lock_timeout`, `hide_amounts`, `notifications_master`, `assistant_consent_granted`, `module_enabled_<moduleKey>` — **appending only**, never renaming an existing key (Article IX, data-model.md §3)
- [ ] T008 Extend `libs/settings/src/main/java/com/dhruv/settings/AppSettings.kt` with `appLockTimeout`, `hideAmounts`, `notificationsMaster` and their defaults from data-model.md §3, and flow them through `SettingsRepositoryImpl`'s `observe()`/`update()` without changing the interface
- [ ] T009 RED: write `libs/settings/src/test/java/com/dhruv/settings/SettingsKeyPreservationTest.kt` asserting today's key set is a **subset** of the shipped key set — cites `SET-ARCH-006`. This is the only guard against a migrated row silently resetting every user's preference
- [ ] T010 RED: prove Koin 3.5.6's `getAll` returns definitions bound to `SettingsContribution` across all loaded modules, in `libs/settings/src/test/java/com/dhruv/settings/contribution/ContributionResolutionProbeTest.kt`. Research R1 names this the one mechanism assumption worth proving before building on it — **it is proved here, in Foundational, not deferred to a story task**
- [ ] T011 [P] RED: write the "no feature-module type in Settings" ArchUnit rule in `apps/finance/app/src/test/kotlin/com/dhruv/finance/app/arch/DependencyRulesTest.kt` (`SET-ARCH-003`). Prove it fails by writing a violating class, watching it fail, then deleting it (quickstart §2)
- [ ] T012 [P] Add the settings sub-route destinations to `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/shell/DetailRoute.kt` — `SettingsAccount`, `SettingsApp`, `SettingsModule(moduleKey)` — alongside the existing `Settings` root
- [ ] T013 GREEN: extend `resolveBackAction`'s caller in `apps/finance/app/src/main/java/com/dhruv/finance/app/MainActivity.kt` so a settings sub-route pops to the Settings top level and the top level pops to the originating tab, without re-deriving the precedence order `libs/core/src/main/kotlin/com/dhruv/core/navigation/BackContract.kt` already encodes — cites `SET-UI-006`, reuses `NAV-ARCH-002`
- [ ] T014 [P] Add every new user-visible string to `apps/finance/app/src/main/res/values/strings.xml` — tier names, section labels, row labels and descriptions, lock copy, confirmation copy. No literal reaches a composable (design system §10)
- [ ] T015 [P] Create `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/SettingsConfig.kt` holding the quick-row set, tier order and the auto-lock timeout option ids — screen-level data lives in a Config file, never inline (Article V)
- [ ] T016 Register the contribution resolution point in `apps/finance/app/src/main/java/com/dhruv/finance/app/di/AppModule.kt` so contributions are resolved **by type**, never from a list (research R1, proved by T010)
- [ ] T017 Confirm `./gradlew :libs:settings:testDebugUnitTest :libs:core:testDebugUnitTest` is green with the new types in place

## 0b.1 Registry and renderer (US1)

- [ ] T018 [P] [US1] RED: `libs/settings/src/test/java/com/dhruv/settings/contribution/SettingsRegistryTest.kt` — every registered contribution is returned by type and the registry holds no module names of its own (`SET-ARCH-001`)
- [ ] T019 [P] [US1] RED: `libs/settings/src/test/java/com/dhruv/settings/contribution/SettingsRegistryTest.kt` (same file as T018) — a contribution whose `moduleKey` is disabled is dropped (`SET-BR-001`); one below its `minVersion` is dropped (`SET-BR-002`); enabling a module makes its entry appear with no other change (`SET-BR-003`); ordering is `order` then title, identical across two resolutions (`SET-BR-004`)
- [ ] T020 [P] [US1] RED: `libs/settings/src/test/java/com/dhruv/settings/contribution/ContributionValidityTest.kt` — every registered contribution's `moduleKey` exists in `platform/feature-flags/dhruv-finance.json` (`SET-ARCH-005`)
- [ ] T021 [P] [US1] RED: `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/settings/ModuleEntryIsolationTest.kt` — a contribution that throws while producing rows degrades to that entry's error card while the tier and every other entry still render (`SET-ARCH-007`)
- [ ] T022 [P] [US1] RED: `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/settings/SettingsRowWriteTest.kt` — a row write persists immediately with no save action, and a failing write reverts the displayed value and states why (`SET-BR-007`)
- [ ] T023 [US1] GREEN: implement `libs/settings/src/main/java/com/dhruv/settings/contribution/SettingsRegistry.kt` — resolve by type, drop disabled/version-gated entries, sort by `order` then title, resolve once per Settings open rather than per recomposition (contract §4)
- [ ] T024 [US1] Implement `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/SettingsRowRenderer.kt` — the single place a row type becomes a component, built from existing `ListGroup`/`ListGroupRow`/`SwitchRow`/`SegmentedRow`/`NxCard`; `Choice` renders as a segmented row at ≤3 options and a selection sheet above that
- [ ] T025 [US1] Rewrite `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/SettingsScreen.kt` as the top level — quick rows (theme, accent, app lock), then Account, then App, then the modules tier, with no other inline controls (`SET-UI-004`)
- [ ] T026 [US1] Create `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/ModuleSettingsScreen.kt` — renders one contribution, groups submodule rows under their labels, wrapped in `FeatureHost` keyed on the contribution's `moduleKey` (`SET-UI-005`, `SET-ARCH-007`)
- [ ] T027 [US1] Wire the three new detail routes from T012 into `DetailRouteContent` in `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/shell/ExistingScreenDetails.kt`, each with `NxTopBar` back chrome

## 0b.1 Appearance and the quick rows (US1)

**Why here**: FR-002 requires each quick row to mirror its owning section's row. Theme and accent
therefore need Appearance in the same sub-phase. **App lock's quick row is the exception** — it
lands here reading the existing `biometric_enabled` preference and is not yet enforcing anything;
0b.3 makes it real. T031 keeps that honest to the user rather than shipping a switch that implies
protection it does not provide (FR-043).

- [ ] T028 [P] [US1] RED: `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/settings/QuickRowMirrorTest.kt` — theme and accent show the same value from the quick row and from Appearance, and changing either updates both with no restart; the same test asserts no setting has two owning entries, quick rows being the only permitted duplication (`SET-BR-008`, **FR-008**)
- [ ] T029 [US1] Create `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/AppSettingsScreen.kt` with its Appearance area only; the Security, Notifications and App-details areas are added in 0b.3 and 0b.4
- [ ] T030 [US1] Migrate theme, accent and the disabled "Use wallpaper colours" row from `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/SettingsScreen.kt` into Appearance, keeping `dark_mode` and `accent_color_hex` unchanged, and confirm both apply app-wide immediately (**FR-019**)
- [ ] T031 [US1] Ensure the app-lock quick row states that it is preference-only until 0b.3 lands, so no row implies a capability the app lacks (`SET-UI-009`, FR-043)
- [ ] T032 [US1] RED then GREEN: assert Settings is the **only** place the app's theme is chosen — a test enumerating theme-write call sites in `apps/finance/app/src/main` and failing on any outside `ui/settings/` (**FR-020, previously uncovered — analysis finding C1**)

## 0b.1 Module entries for what ships today (US1)

- [ ] T033 [P] [US1] Create the calculators contribution at `apps/finance/feature/calc/calculator/src/main/java/com/dhruv/finance/calculator/settings/CalculatorSettingsContribution.kt` — number format, decimal precision with live preview, angle mode, and history preview/export/clear — **migrating the existing rows and reusing their existing preference keys** (`SET-ARCH-006`)
- [ ] T034 [P] [US1] Register that contribution in the calculator module's Koin module (`di/CalculatorModule.kt`) bound to `SettingsContribution`
- [ ] T035 [P] [US1] Create and register the converter contributions — `apps/finance/feature/calc/currency/src/main/java/com/dhruv/finance/currency/settings/CurrencySettingsContribution.kt` and `apps/finance/feature/calc/unit/src/main/java/com/dhruv/finance/unit/settings/UnitSettingsContribution.kt` — each in its own module's Koin module, with the individual converters as submodule groups
- [ ] T036 [US1] RED: now that real contributions exist, add the "no Compose type in a `SettingsContribution` implementation" ArchUnit rule to `apps/finance/app/src/test/kotlin/com/dhruv/finance/app/arch/DependencyRulesTest.kt`, **including an assertion that at least one implementation was found** so the rule cannot pass vacuously (`SET-ARCH-004`; **analysis finding U1** — authored after T033–T035 deliberately)
- [ ] T037 [US1] Delete the calculator and appearance rows from the old monolithic body in `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/SettingsScreen.kt`, confirming against T002's inventory that nothing was dropped
- [ ] T038 [US1] REFACTOR: split `SettingsUiState.kt`/`SettingsViewModel.kt` per tier, keeping each ViewModel to one concern

## 0b.1 Close-out

- [ ] T039 [US1] Define the screen-state matrix for every screen this sub-phase introduces — top level, `ModuleSettingsScreen`, `AppSettingsScreen`'s Appearance area — covering the applicable loading, empty, error, offline, signed-out, not-configured and disabled states (**FR-044, previously uncovered — analysis finding C2**; design system §7)
- [ ] T040 [US1] Verify `SET-ARCH-002` by hand: add a throwaway module with a contribution, assemble, run `git status --porcelain`, confirm no file under `app/ui/settings/` appears in the diff, then delete the throwaway module (quickstart §2)
- [ ] T041 **Checkpoint 0b.1** — close `SET-ARCH-001` … `SET-ARCH-007`, `SET-BR-001` … `SET-BR-004`, `SET-BR-007`, `SET-BR-008`, `SET-UI-004`, `SET-UI-005`, `SET-UI-006` in catalog §13 and update §14 by recount; `./gradlew regressionCheck` green; coverage floor not regressed. **Merge gate**

---

# Sub-phase 0b.2 — Account & identity

**Ships**: sign in, sign out, consent, erasure — all from Account. Fixes two defects in a shipped
surface: sign-in exists only inside first-run onboarding, and there is no sign-out at all.

**Independent value**: a signed-in user can leave, and a signed-out user can return, without
reinstalling. That is not true today.

**Depends on**: 0b.1 (the Account entry needs the tier to exist).

- [ ] T042 [P] [US2] RED: `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/settings/AccountSettingsViewModelTest.kt` — signed-out shows a working sign-in that does not route through first-run onboarding, and no placeholder identity is shown in either state (`SET-FLOW-003`)
- [ ] T043 [P] [US2] RED: `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/settings/AccountSettingsViewModelTest.kt` — sign-out clears the session and stored credentials and leaves on-device calculator history intact (`SET-FLOW-004`)
- [ ] T044 [P] [US2] RED: `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/settings/AccountSettingsViewModelTest.kt` — account erasure requires a typed confirmation and the dialog names what is destroyed (`SET-BR-021`); an erasure that fails offline or is rejected reports failure, claims no success, and stays available for retry (`SET-BR-022`)
- [ ] T045 [P] [US2] RED: `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/settings/AccountSettingsViewModelTest.kt` — the export row is absent while no financial records exist (`SET-BR-023`)
- [ ] T046 [US2] GREEN: create `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/AccountSettingsScreen.kt` with the identity row, sign-in/sign-out, the three consent switches, and the erasure actions
- [ ] T047 [US2] Wire that screen's sign-in to the auth repository under `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/auth/` directly — not to the onboarding module's screen (research R6). Sign-in uses the unauthenticated GoTrue client and is deliberately not consent-gated (reuses `ONB-BR-001`)
- [ ] T048 [US2] Implement sign-out: clear `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/auth/SessionStore.kt` and stored credentials, leave Room-backed calculator data untouched
- [ ] T049 [US2] Migrate the three consent switches from `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/SettingsScreen.kt` into `AccountSettingsScreen.kt`, wired to the same `ConsentRepository` setters — reuses `ONB-BR-004` (persistence) and `ONB-BR-005` (independent revocation), so no new persistence test is written here (**FR-014, SC-007 — traceability finding T1**)
- [ ] T050 [US2] Migrate "Delete my data" and "Delete my account" into `AccountSettingsScreen.kt`, reusing the dialogs in `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/SettingsDialogs.kt`, adding type-to-confirm on account erasure; the underlying calls are unchanged (reuses `ONB-BR-008`, `ONB-BR-009`, `DAT-FLOW-001`)
- [ ] T051 [US2] Confirm the post-erasure state — account erasure returns the app to first-run setup on next launch with no residual account state (**FR-017**, reuses `ONB-BR-009`)
- [ ] T052 [US2] Surface erasure failures honestly in `AccountSettingsScreen.kt` — replace any toast that reports success unconditionally with one driven by the `Result`, and keep the action enabled after a failure
- [ ] T053 [US2] Remove the non-functional "Export my data" placeholder row from `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/SettingsRows.kt` and its call site entirely; add a code comment naming the phase that reinstates it and the requirement (FR-018) forbidding an empty export
- [ ] T054 [US2] Verify `ONB-FLOW-005` still holds through the relocated controls — toggle a consent off, then run "Delete my data", in the same session
- [ ] T055 [US2] Delete the account/consent/erasure rows from `SettingsScreen.kt`, checking against T002's inventory
- [ ] T056 [US2] Define the screen-state matrix for `AccountSettingsScreen.kt` — signed-out, offline, error and loading states are all reachable here (**FR-044**, design system §7)
- [ ] T057 [US2] Confirm Settings remains reachable from the top bar on every primary tab after the tier split (**FR-010**, reuses `NAV-UI-002`)
- [ ] T058 [US2] **Sec pass** — consent revocable and durable through the relocated controls, erasure paths unchanged, no new off-device data class, no PostgREST call from Settings beyond the two erasure RPCs (Article VIII)
- [ ] T059 **Checkpoint 0b.2** — close `SET-FLOW-003`, `SET-FLOW-004`, `SET-BR-021`, `SET-BR-022` in catalog §13 (`SET-BR-023` closes **deferred**, with the reason from research R7) and update §14 by recount; `regressionCheck` green. **Merge gate**

---

# Sub-phase 0b.3 — App lock & privacy

**Ships**: an app lock that actually gates, auto-lock, hide-amounts, and the app-wide notification
master. The highest user-visible value in the feature, and the one that fixes a control which today
implies protection while providing none.

**Independent value**: financial data on a shared or lost device is actually protected.

**Depends on**: 0b.1 (Security area needs `AppSettingsScreen`; the app-lock quick row needs its
owning section to satisfy FR-002).

- [ ] T060 [P] [US3] RED: `libs/core/src/test/kotlin/com/dhruv/core/security/AppLockDecisionTest.kt` — cold start with lock enabled is LOCKED (`SET-BR-013`); each timeout locks exactly when elapsed ≥ timeout and `Immediate` locks on any backgrounding (`SET-BR-014`); a successful auth covers the current foreground session only (`SET-BR-015`); lock off is always UNLOCKED with no stale state (`SET-BR-016`)
- [ ] T061 [P] [US3] RED: `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/settings/HeldTargetTest.kt` — a target arriving while locked is held and dispatched exactly once after unlock (`SET-FLOW-002`); a second arrival replaces the first and only one is held (`SET-BR-018`)
- [ ] T062 [P] [US3] RED: `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/settings/AppSettingsViewModelTest.kt` — app lock cannot be enabled with no enrolled credential, against a stubbed credential-availability check (`SET-BR-017`)
- [ ] T063 [P] [US3] RED: `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/settings/AppSettingsViewModelTest.kt` — the notification master off suppresses every module's alerts regardless of that module's own setting (`SET-BR-010`)
- [ ] T064 [P] [US3] RED: `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/settings/HideAmountsTest.kt` — with hide-amounts on, the money formatting path masks values while counts, dates and percentages stay readable, independent of lock state (`SET-BR-019`)
- [ ] T065 [US3] GREEN: implement `libs/core/src/main/kotlin/com/dhruv/core/security/AppLockDecision.kt` — total, side-effect free, elapsed time passed in rather than read from a clock, mirroring `navigation/BackContract.kt`'s decision/effect split (gate contract §1)
- [ ] T066 [US3] Create `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/AppLockGate.kt` — hosts `BiometricPrompt` at Class 3 with device-credential fallback, retry affordance on cancel/failure, no attempt limit of our own (gate §2 rules 8–9)
- [ ] T067 [US3] Wrap the **entire** content tree in `apps/finance/app/src/main/java/com/dhruv/finance/app/MainActivity.kt` with the gate — above the pager, above the detail-route overlay, above every tab including Calc. While LOCKED no content is composed or drawn (`SET-UI-001`, `SET-UI-003`)
- [ ] T068 [US3] Resolve the gate in `MainActivity.kt` before the first content frame so no unlocked frame ever appears on cold start — a correctness condition, not polish (`SET-UI-002`, gate §2 rule 7)
- [ ] T069 [US3] Implement hold-and-dispatch in `MainActivity.kt` for links, notification taps and launcher shortcuts arriving while locked; dispatch once after unlock, retain across a cancelled unlock within the same launch, clear on process death, never persist (`SET-FLOW-001`, gate §3, data-model §5)
- [ ] T070 [US3] Add the Security area to `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/AppSettingsScreen.kt` — app lock, the auto-lock timeout `Choice` (**FR-024**), hide-amounts
- [ ] T071 [US3] Migrate the legacy history lock and PIN rows into Security, keeping them working and labelling them superseded by app lock (`SET-BR-020`)
- [ ] T072 [US3] Make the app-lock quick row real — it now mirrors Security's row and enforces (removes T031's preference-only caption), completing `SET-BR-008` for all three quick rows
- [ ] T073 [US3] Apply hide-amounts at the shared money formatting path — `libs/core/src/main/kotlin/com/dhruv/core/format/Paise.kt`, `libs/core/src/main/kotlin/com/dhruv/core/ui/components/MoneyText.kt`, `apps/finance/data/src/main/java/com/dhruv/finance/data/util/CurrencyFormatter.kt` — so screens, the widget and notification builders all inherit it, and the export path deliberately ignores it and says so (research R5)
- [ ] T074 [US3] Add the Notifications area to `AppSettingsScreen.kt` — system-permission state, the denied banner routing to system settings, and the app-wide master switch; **no per-channel row here** (`SET-UI-012`, `SET-BR-010`)
- [ ] T075 [US3] Delete the security rows from `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/SettingsScreen.kt`, checking against T002's inventory
- [ ] T076 [US3] Define the screen-state matrix for the Security and Notifications areas, including the permission-denied state (**FR-044**, design system §7)
- [ ] T077 [US3] Manual device pass for the app lock table in `apps/finance/specs/004-settings/quickstart.md` §4 — cold start, cancel, timeout, Calc tab, notification-while-locked, held target after cancel, cold start clears held target, credential removed, credential absent. Record the screen capture proving no unlocked frame (`SET-UI-002`, **SC-009**)
- [ ] T078 [US3] Confirm hide-amounts masks on all three surfaces — screen, widget, notification — with counts and percentages still readable (**SC-010**)
- [ ] T079 [US3] **Sec pass** — the gate has no exempt surface, held targets are never persisted, hide-amounts covers every money surface, and the export exemption is disclosed at the point of export
- [ ] T080 **Checkpoint 0b.3** — close `SET-BR-013` … `SET-BR-020`, `SET-UI-001`, `SET-UI-002`, `SET-UI-003`, `SET-UI-012`, `SET-FLOW-001`, `SET-FLOW-002` in catalog §13 and update §14 by recount; `regressionCheck` green. **Merge gate**

---

# Sub-phase 0b.4 — Module conventions, assistant, app details

**Ships**: the rules every future module entry follows, durable assistant consent, a safe personal
AI key, and App details.

**Independent value**: after this, a later phase adds its module's settings by writing a
contribution and nothing else — no decisions left to make.

**Depends on**: 0b.1. Independent of 0b.2 and 0b.3.

## 0b.4 Module entry conventions (US4)

- [ ] T081 [P] [US4] RED: `libs/settings/src/test/java/com/dhruv/settings/contribution/ModuleToggleTest.kt` — turning an optional module off then on retains and restores its stored preferences, with no reset on disable (`SET-BR-005`)
- [ ] T082 [P] [US4] RED: `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/settings/AlertControlCoverageTest.kt` — the notification channel registry and the contributed alert toggles are equal in count and map one-to-one (`SET-BR-006`)
- [ ] T083 [P] [US4] RED: `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/settings/ModuleConsentGateTest.kt` — a module entry whose controls need an ungranted consent states which consent and offers the route to grant it, rather than rendering inert controls (`SET-UI-008`)
- [ ] T084 [P] [US4] RED: `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/settings/PrimaryDestinationTest.kt` — no primary navigation destination offers a hide control (`SET-UI-010`)
- [ ] T085 [P] [US4] RED: `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/settings/AlertControlCoverageTest.kt` — an alert whose source feature has not shipped is absent from its module's entry rather than present and inert (**FR-031, previously uncovered — analysis finding C4**)
- [ ] T086 [P] [US4] RED: `libs/settings/src/test/java/com/dhruv/settings/contribution/ModuleToggleTest.kt` — a module removed from the build leaves no orphan entry, and a module turned off while one of its screens is open degrades that screen rather than crashing (**spec Edge Cases, previously uncovered — analysis finding C5**)
- [ ] T087 [US4] GREEN: implement the optional-module on/off control in `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/ModuleSettingsScreen.kt`, writing `module_enabled_<moduleKey>` and removing the module from navigation and content when off while retaining its preferences (`SET-BR-005`, FR-032)
- [ ] T088 [US4] Implement the consent-required state in `ModuleSettingsScreen.kt`, reading `requiresConsent` from the existing flag resolver rather than new metadata (research R8, `SET-UI-008`)
- [ ] T089 [US4] Implement the hidden-content empty state via `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/SettingsRowRenderer.kt` — a surface whose items are all hidden by a module setting points back at the setting that hid it, never renders blank (`SET-UI-011`)
- [ ] T090 [US4] Add the first real alert control — the currency/metals daily-rates toggle plus time picker — in that module's own contribution, establishing the convention `SET-BR-006` measures
- [ ] T091 [US4] Ensure primary tabs expose no hide control anywhere in `ModuleSettingsScreen.kt`, checked against `TabKey` in `libs/core/src/main/kotlin/com/dhruv/core/navigation/` (`SET-UI-010`)
- [ ] T092 [US4] REFACTOR: extract any repetition between `ModuleSettingsScreen.kt`, `AccountSettingsScreen.kt` and `AppSettingsScreen.kt` into `SettingsRowRenderer.kt` rather than duplicating row-rendering logic (Article VI — extend, never parallel)

## 0b.4 Assistant (US6)

- [ ] T093 [P] [US6] RED: `apps/finance/data/src/test/java/com/dhruv/finance/data/AssistantConsentTest.kt` — a granted consent survives a force-stop and the assistant does not re-ask (`SET-BR-011`). Run against the current build first and watch it fail — it is a defect fix, not new behaviour
- [ ] T094 [P] [US6] RED: `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/settings/AiKeySecrecyTest.kt` — a saved key never appears in full in any screen state, log, export or crash report, and is removable in one action (`SET-BR-012`)
- [ ] T095 [US6] GREEN: replace the in-memory consent flag in `apps/finance/feature/shell/assistant/src/main/java/com/dhruv/finance/assistant/AssistantViewModel.kt` (state type in `AssistantUiState.kt`, same package) with the persisted `assistant_consent_granted` preference, so the assistant reads durable state rather than its own transient copy (FR-036)
- [ ] T096 [US6] Create and register the assistant's contribution at `apps/finance/feature/shell/assistant/src/main/java/com/dhruv/finance/assistant/settings/AssistantSettingsContribution.kt` — consent status + re-consent, BYO Gemini key
- [ ] T097 [US6] Implement the masked key row in `SettingsRowRenderer.kt` against the encrypted `secure_settings` store (`libs/settings/src/main/java/com/dhruv/settings/SettingsRepositoryImpl.kt`), with a single-action remove; extend `NxTextField` with its error state (design system §5.3 gap) rather than adding a parallel field (Article VI)
- [ ] T098 [US6] Confirm withdrawal returns `apps/finance/feature/shell/assistant/src/main/java/com/dhruv/finance/assistant/AssistantScreen.kt` to its consent gate before its next request, and that no request fires in between (**FR-037**, reuses `DAT-BR-001`)

## 0b.4 App details (US5)

- [ ] T099 [P] [US5] RED: `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/settings/AppDetailsViewModelTest.kt` — the reported version matches the installed build including build number (`SET-UI-013`)
- [ ] T100 [P] [US5] RED: `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/settings/AppDetailsViewModelTest.kt` — a failed update check reports the failure and never silently reports "current" (`SET-UI-014`)
- [ ] T101 [US5] GREEN: add the App-details area to `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/AppSettingsScreen.kt` — version name + code from the package info, privacy policy, licences (the bundled components named in `NOTICE`/`third_party/`), source link, replay intro (**FR-041**)
- [ ] T102 [US5] Implement the update check's three honest outcomes in `AppSettingsScreen.kt` — current, available, failed. If the update channel does not exist yet, the row is absent rather than inert (FR-043); mark `SET-UI-014` deferred with that reason
- [ ] T103 [US5] Delete the old "About Dhruv Finance" row from `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/SettingsScreen.kt`, checking against T002's inventory

## 0b.4 Close-out

- [ ] T104 Define the screen-state matrix for the module entry screen and the App-details area (**FR-044**, design system §7)
- [ ] T105 [P] Confirm the old monolithic body in `SettingsScreen.kt` now holds only the top level — every migrated row has left it, checked against T002's inventory
- [ ] T106 [P] **Sec pass** — the AI key is absent from every export, diagnostic and crash output; a contributed module cannot declare a row that bypasses a consent gate (spec §Security checklist CHK046)
- [ ] T107 Close `SET-BR-005`, `SET-BR-006`, `SET-BR-011`, `SET-BR-012`, `SET-UI-008`, `SET-UI-010`, `SET-UI-011`, `SET-UI-013` in catalog §13 (`SET-UI-014` closes **deferred**) and update §14 by recount
- [ ] T108 **Checkpoint 0b.4** — `regressionCheck` green; coverage floor not regressed. **Merge gate**

---

# Sub-phase 0b.5 — Feature-level verification & ratchet

**Ships**: no new behaviour. The checks that only mean something once every tier and entry exists,
plus the coverage ratchet and the doc updates.

**Depends on**: 0b.1–0b.4 all merged.

- [ ] T109 Run the SC-001 migration check from `apps/finance/specs/004-settings/quickstart.md` §3 — install this build **over** the previous version without uninstalling, and confirm all 19 rows from T002's inventory are reachable at their new homes with values intact (`SET-UI-007`)
- [ ] T110 [P] Run the no-orphan-preference audit — enumerate every persisted key in `libs/settings/src/main/java/com/dhruv/settings/SettingsKeys.kt` and any module store, locate each in Settings, and record any key with no row as either an FR-003 violation to fix or dead state to delete (`SET-BR-009`, **SC-005**)
- [ ] T111 [P] Measure tap depth — confirm every setting is reachable in at most three taps from the Settings entry point and the three quick rows need no navigation at all (**SC-002, previously uncovered — analysis finding C3**)
- [ ] T112 [P] Run the inert-row review from `apps/finance/specs/004-settings/quickstart.md` §7 — exercise every shipped row and confirm none appears operable while changing nothing; every preference-only row says it is preference-only (`SET-UI-009`, **SC-011**)
- [ ] T113 [P] Theming and text-size pass per `quickstart.md` §8 across every screen in `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/`, light and dark, smallest and largest (`SET-UI-015`, **SC-012**)
- [ ] T114 [P] TalkBack pass per `quickstart.md` §8 over the same screens — every switch, icon-only control and destructive action announces its subject and current state; destructive rows are visually distinct and never first-focused (`SET-UI-016`, **SC-013**)
- [ ] T115 Confirm every `SET-*` row in catalog §13 is CLOSED or explicitly deferred with a stated reason, and that §14's totals were reached by recounting rather than by incrementing
- [ ] T116 Read the measured numbers from `build/reports/jacoco/jacocoAggregatedReport/jacocoAggregatedReport.xml` (or `python scripts/ci/regression_summary.py`) and record in §14: merged line coverage before vs after, plus `:libs:settings`'s and `:libs:core`'s own percentages. Compose screen files are not exercised by the JVM gate — record both numbers rather than reporting one and implying the other
- [ ] T117 Raise `globalLineFloor` in the root `build.gradle.kts` only if the measured merged coverage rose, and only to just under the new measured value (Article X — ratchets, never ahead of landed tests)
- [ ] T118 [P] Update `apps/finance/CLAUDE.md`'s Phase line, `platform/DESIGN-SYSTEM.md` §5.3 (strike the `NxTextField` error-state row once T097 closes it), and the design-v1 implementation plan §7 tracking row

---

## Dependencies & Execution Order

### Sub-phase dependencies

```
0b.1 ──┬── 0b.2 ──┐
       ├── 0b.3 ──┼── 0b.5
       └── 0b.4 ──┘
```

- **0b.1** depends on nothing. Its Foundational block (T005–T017) blocks every other sub-phase
- **0b.2, 0b.3, 0b.4** each depend on 0b.1 and are independent **of each other** — three people could take one each
- **0b.5** depends on all four

### Within a sub-phase

- RED tests before implementation, always (Article I)
- Pure logic (registry, lock decision) before the UI consuming it
- A migrated row moves in the sub-phase that owns its new home, always checked against T002
- Each sub-phase's last task is its merge gate; do not carry a red gate into the next

### Parallel opportunities

- T002, T003 · T005, T006, T011, T012, T014, T015
- All RED tests within a sub-phase: T018–T022 · T042–T045 · T060–T064 · T081–T086, T093–T094, T099–T100
- T033, T034, T035 — three different modules' contributions
- 0b.2, 0b.3, 0b.4 in parallel once 0b.1 merges
- T110–T114 in 0b.5

---

## Implementation Strategy

### MVP — 0b.1 alone

Settings is restructured, self-assembling, and nothing regressed. Shippable and worth shipping on
its own: every later phase gains the ability to add its settings without touching Settings.

### Recommended order

0b.1 → 0b.3 → 0b.2 → 0b.4 → 0b.5.

0b.3 before 0b.2 because a lock that enforces nothing is the more dangerous of the two shipped
defects — it implies protection that does not exist, whereas a missing sign-out is merely an absence
the user can see. If the three are staffed in parallel, order stops mattering.

### Stopping points

Each checkpoint is a real stopping point. Stopping after 0b.1 leaves a coherent product. Stopping
after 0b.3 leaves the two highest-value fixes shipped. Nothing later is load-bearing for anything
earlier.

---

## Notes

- Every test cites its `SET-*` row. A test with no citation is a review-blocking finding (Article I)
- No task authors catalog rows or rewrites the surface registry — both were done 2026-08-19
- Preference keys are append-only; a migrated row keeps its key or the migration is wrong (Article IX)
- Contributions are data. If a task tempts you toward a composable in a contribution, the answer is a
  `Navigate` row to the module's own screen (contract §2 rule 8)
- **Coverage gaps closed by this split**: T032 (FR-020), T036 (vacuous ArchUnit rule), T039/T056/T076/T104 (FR-044 screen-state matrix, per sub-phase), T085 (FR-031), T086 (two edge cases), T111 (SC-002). Tasks reusing an `ONB`/`NAV`/`DAT` row now name it (T047, T049, T050, T051, T057, T098)
- **Pre-existing, out of scope**: `:apps:finance:feature:onboarding` is in `coveredModules` but
  `onboarding` is absent from `_FEATURES` in `scripts/ci/regression_summary.py`, so its coverage
  reports as `(other)`
- Commit after each task or logical group; stop at any checkpoint to validate independently

---

## Phase 0b.6: Gap remediation (multi-agent spec audit, 2026-08-22)

**Source**: `apps/finance/docs/superpowers/reviews/2026-08-22-spec-phase-gap-register.md`.

This phase owns the control plane every later module contributes into, so most of its findings are
about surfaces that are declared here and never claimed anywhere.

- [ ] T119 [SA] **Enforce the settings-contribution model.** This spec declares "every later phase
      ships its module's settings entry with the module"
      (`contracts/settings-contribution.md:41`, `:78`), but **001, 002, 003 and 006 plan no
      `SettingsContribution` at all** — only 005 does, and it never cites the contract. Four modules
      would ship with no Settings presence, breaking FR-003/FR-004. Either add a CI/ArchUnit check
      that a feature module registering a flag also registers a contribution, or add the missing
      contribution tasks to each phase (already appended there — this task tracks closure)
- [ ] T120 [SA] **Decide Profile's fate.** The navigation contract lists it, surface registry §1
      lists it as a live shell detail route, and `ProfileScreen.kt` is a shipped stub the functional
      spec says must become the Settings sub-tree — and **this spec never mentions Profile once**.
      Absorb into the Account tier, keep as a separate route, or delete; record the choice
- [ ] T121 [SA] **Decide Trash / Recently deleted.** The navigation contract lists
      `Security·Privacy·Trash`, DESIGN-SYSTEM §8 mandates soft-delete plus "a recoverable location",
      and the string appears in **zero** phase specs and zero registries — while 001/002/003 all
      soft-delete rows. Surface registry §4 currently parks "Recently deleted" under the Automation
      module (Phase 7, which has no spec). Either own it here or record an accepted descope, and tell
      001 T053 / 002 T088 / 003 T142 where undo's recoverable location lives
- [ ] T122 [SA] **Wire `alert_log` purge into the erasure action** (DPDP). Phase 6 stores user money
      as paise in a device-local Room `alert_log.payload_json` and explicitly excludes it from
      `delete_my_data()`; this spec's erasure FRs (FR-015..FR-017) are server-side only. After a
      successful "Delete my data" the notification centre still holds readable amounts for records
      that no longer exist. Either purge the Room table in the same action or stop storing amounts in
      the payload
- [ ] T123 [SA] Add a **`contracts/routes.md`** to this spec. It adds three shell detail routes
      (`SettingsAccount`/`SettingsApp`/`SettingsModule`) and is the only phase with no routes
      contract, while its tasks state that no task rewrites the surface registry — so three routes
      would ship with no registry row and no contract
- [ ] T124 [SA] Record the status of the open items this spec **declines while shipping the feature
      they touch**: functional spec §8.3 (custom fields — declared "never designed", out of scope,
      owned by nobody) and §8.7 (cross-device consent sync — out of scope while this phase ships the
      consent controls). Both are currently unowned and this spec is marked ready to implement
- [ ] T125 [SA] Map surface registry §2/§3's **"source phase" column**, which still cites retired
      roadmap codes (`R4`, `R5b`, `R6`, `R7`, `R8`, `P4`) pointing at documents deleted 2026-08-15.
      This spec acknowledges them as unmapped and defers; nothing has mapped them since
- [ ] T126 [SA] Claim **`PinEntry`** (design-system batch B2), which the app-lock flow needs and no
      phase builds — grep across all six spec dirs returns no hits. Same for `NxCheckbox`, `NxRadio`,
      `QwertyKeypad`, `EnumPickerGrid` and B7's `Spinner` if any settings surface needs them

---

## Phase 0b.7: Gap remediation, round 2 (UI/UX + requirements audit, 2026-08-22)

- [ ] T127 [SA] **Three Success Criteria measure surfaces this phase does not build, and no later
      phase claims them**: SC-006 (channel↔control 1:1 — channels arrive in Phase 6), SC-009 (the
      lock covers "notifications and deep links" — Phase 6), SC-010 (hide-amounts on "any screen,
      **widget** or notification" — the money screens are 001–003, and **no phase builds a widget**).
      T078 *verifies* widget masking against a surface that does not exist, and `quickstart.md:90`
      instructs "Tap a notification while locked" at Phase 0b. Either scope these SCs to what 0b
      ships and hand the rest to their real phase, or state the dependency explicitly — the
      requirements checklist currently claims 24/24 including "Success criteria are measurable"
- [x] T128 [SA] **DECIDED 2026-08-23: a widget SHIPS.** The proposed descope was rejected, so
      FR-025/SC-010's widget masking clause and T078's widget assertion are **correct as written** —
      do not strip them. Remaining work is placement (recommended: Phase 6) and a data source
      (Phase 2's `v_net_worth_history`). Original task text below, kept for context:
- [ ] T128a [SA] ~~Decide whether a Glance widget ships at all.~~ DESIGN-SYSTEM §11 specifies its
      conventions (day/night token mapping, compact money, `contentDescription` on the root, its own
      value / masked / signed-out / disabled states), the surface registry names a widget button as a
      `QUICK_ADD` producer, this phase's FR-025 makes masking binding on it, and 006 records it
      descoped. Own it, or record the descope in one place and drop the assertions that depend on it
- [ ] T129 [SA] **Two Edge Cases have no stated expected behaviour at all** — "A module is turned off
      while one of its screens is open." and "Notification permission is granted, then revoked from
      the system while the app is running." T086 invents a behaviour for the first ("degrades that
      screen rather than crashing"); the second has neither FR nor task
- [ ] T130 [SA] State the **AI-key format and length validation** — currently unspecified
- [ ] T131 [Android] Add the **observability triad** this phase omits (`crashReporter.setModule`,
      `performanceTracer.trace`, `featureError` StateFlow) — required of every feature ViewModel by
      `apps/finance/CLAUDE.md`, planned only by 003 and 005
- [ ] T132 [Android] Extend the a11y work beyond T114 (TalkBack order, Settings only) to the rest of
      §9 for this phase's surfaces: contrast in both themes, ≥48dp targets and ≥56dp rows,
      dynamic-type safety. Add a `strings.xml` check for the copy rules in §10 — sentence case, the
      destructive-dialog consequence sentence, and the empty-state message-plus-**verb**-CTA rule

---

## Phase 0b.8: Closure — tracking (runs last, after the checkpoint is green)

Per the tracking rule in `apps/finance/CLAUDE.md`. **This phase creates no Gradle module**, so it
gets **no** FEATURES.md module row — it belongs in that file's "Phases that add no Gradle module"
table, which already lists it. Do not add a module row for it.

- [ ] T133 [P] Add the **root `CHANGELOG.md`** entry: the three-tier Settings control plane
      (Account · App · Modules), the app-wide lock checkpoint, the settings-contribution mechanism
      later phases use, and the three shipped-surface defect fixes
- [ ] T134 [P] Update **`apps/finance/app/README.md`** — this phase substantially changes what the
      shell owns (the Settings tree, the lock gate). The shell README is the module README for this
      phase's work
- [ ] T135 [P] Update the **spec-kit tracking table** (implementation plan §7) — Phase 0b to
      *shipped*, per sub-phase 0b.1–0b.5 if they merge separately
- [ ] T136 [P] Add the three new shell detail routes (`SettingsAccount`/`SettingsApp`/
      `SettingsModule`) to surface registry §1 — this phase currently plans **no** routes contract
      and states that no task rewrites the registry (T123 adds the contract; this adds the rows)
- [ ] T137 [P] **Record the settings-contribution obligation in FEATURES.md's tracking rule** so a
      later phase cannot ship a module without its Settings entry — the failure this phase's own
      FR-003/FR-004 depend on and that four phases currently miss