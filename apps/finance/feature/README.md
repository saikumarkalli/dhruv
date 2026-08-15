# Finance feature modules

Every module here is a `dhruv.android.library` + `dhruv.android.compose` Koin module, namespace
`com.dhruv.finance.<name>`, depending on `:apps:finance:data` (Repository-only), `:libs:core`,
`:libs:settings` — never on another feature module (ArchUnit-enforced,
`apps/finance/app/src/test/kotlin/com/dhruv/finance/app/arch/DependencyRulesTest.kt`). Feature
flags live in `platform/feature-flags/dhruv-finance.json`. `apps/finance/FEATURES.md` is the
cross-module index (one row per module, links to each README below) plus the shared design-system
token reference — per-module detail (screens, ViewModels, data deps) lives only in each module's
own `README.md`, not duplicated in FEATURES.md too.

## Folder scheme — grouped by owning tab, added 2026-08-09

```
home/          Home tab       — networth
money/         Money tab      — money
calc/          Calc tab       — calculator
plan/          Plan tab       — loans, investments, tax, everyday, planning, insurance, retirement
insights/      Insights tab   — insights
onboarding/    (pre-tab)      — onboarding
shell/         (no tab)       — currency, unit, date, time, assistant, automation
```

**The physical folder does not change any Gradle coordinate.** `:apps:finance:feature:loans` is
still `:apps:finance:feature:loans` — only its `projectDir` moved (`settings.gradle.kts`). Every
`project(":apps:finance:feature:X")` dependency declaration and every
`./gradlew :apps:finance:feature:X:...` command anywhere in the repo's docs keeps working
unchanged. This was a deliberate choice over renaming coordinates to match the new nesting (e.g.
`:apps:finance:feature:plan:loans`) — the coordinate rename would have touched ~26 files (build
scripts + a dozen markdown docs with literal example commands) for purely cosmetic gain; the
`projectDir` remap achieves the same physical organization for two file changes
(`settings.gradle.kts`, and a `moduleDir()` fix in the root `build.gradle.kts` that was relying on
path-string-munging instead of Gradle's real project-resolution API — fixed as part of this reorg
since the old approach would have silently broken coverage aggregation the moment directories moved).

Ownership above is not a new decision — it's the same tab ownership already recorded in
`apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §6 (module topology)
and `apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md`'s module codes
(`HOM`/`MNY`/`PLN`/`INS`/`RET`/`SIG`/`ONB`, plus the shell-level ones), just made physical.

## README convention (binding for every module from here on)

Every module — existing or new — has a `README.md` at its root:

```markdown
# <name>
<one-line description>
- **Gradle module:** `:apps:finance:feature:<name>`
- **Owner tab:** <tab, or "none — shell", or "none — pre-tab">
- **Flag:** `<key>` in platform/feature-flags/dhruv-finance.json — <state>
## Screens
## ViewModels          (or "## QA scenarios" + "## Business rules" for a not-yet-built module)
## Data dependencies
```

Writing this file is part of building the module, not a follow-up — it's now listed in the
canonical module shape in
`apps/finance/docs/superpowers/specs/2026-08-09-module-standard-and-tdd-process.md` §1.1, alongside the
`<Name>Screen.kt`/`<Name>ViewModel.kt`/`di/` files every module already requires.
