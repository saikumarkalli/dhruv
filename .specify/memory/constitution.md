# Dhruv Constitution

Governs every `/speckit-*` workflow in this repo. This file does not restate or override
`platform/DECISIONS.md` (ADRs, append-only, why) or `platform/DESIGN-SYSTEM.md` (the design
contract) — it operationalizes them as spec-kit gates. Where this file and either of those
disagree, DECISIONS.md / DESIGN-SYSTEM.md win and this file is the bug.

## Core Principles

### I. Test-First (NON-NEGOTIABLE)
RED → GREEN → REFACTOR. Every test cites the scenario ID it satisfies (e.g. `NW-BR-003`) against
that feature's QA scenario catalog. No implementation code before its failing test exists.
Enforcement: `apps/finance/docs/superpowers/specs/2026-08-09-module-standard-and-tdd-process.md` §4;
reviewed at every phase checkpoint.

### II. Scenarios Before Code
No Backend or Android task may start until its phase's QA catalog rows exist and are reviewed
against the functional spec. `/speckit-tasks` output for a Dhruv feature must reference catalog
row IDs, not restate them.
Enforcement: design-v1 implementation plan §7.0, step 2 before step 3/4.

### III. Module Boundaries
`feature → feature` is FORBIDDEN. `feature → data` is Repository-only. `core → anything internal`
is FORBIDDEN. `vault → network/ai/analytics` is FORBIDDEN. Cross-feature navigation is by
`NavTarget` id, never a class reference.
Enforcement: ArchUnit `DependencyRulesTest`, CI Gate 3.

### IV. Fault Isolation
Every route is wrapped in `FeatureHost` with a flag entry in `platform/feature-flags/<app>.json`.
A disabled feature renders `FeatureDisabledCard`; a thrown error renders `FeatureErrorCard`. A
feature crash never takes down the app shell.
Enforcement: ArchUnit route-registry test.

### V. No Hardcoding
No raw dp/sp/hex/color literal in a screen file. Tokens only (`LocalDhruvNextColors`,
`DhruvNextType`, `DhruvNextSpacing`, `DhruvNextRadii`, `DhruvBrand`). Screen-level data lives in
`<Name>Config.kt`, never inline.
Enforcement: detekt custom rule + review, `platform/DESIGN-SYSTEM.md` §1–§3.

### VI. Component Reuse, Not Parallel Components
A new visual element extends an existing `:libs:core` component (§5.3 of the design system) before
a new one is proposed. Nothing is listed as "built" in the design system until verified against
`libs/core/src/main` by symbol search — never assumed from a design file.
Enforcement: `platform/DESIGN-SYSTEM.md` §5, §13 verification script.

### VII. Money Is Exact
Tracker amounts are `Long` paise end to end (Kotlin) / `bigint` (SQL). `BigDecimal` is reserved for
pure calculation engines (calculators, retirement projection) and never appears on a tracker write
path.
Enforcement: `checkTrackerMoneyPrecision` Gradle task, wired into `regressionCheck`.

### VIII. Consent Before Network
No tracker network call may execute before its DPDP consent switch is on. Consent is enforced by
an interceptor in the client construction path, not by screen-level discipline.
Enforcement: `ConsentInterceptor` + unit test, ADR-0014 §7, ADR-0029.

### IX. Append-Only History
Enum constants persisted as TEXT are append-only — never rename a shipped constant. ADRs in
`platform/DECISIONS.md` are ACCEPTED and append-only; a changed decision is a new ADR or a dated
note, never an edit to prior ADR body text.

**A table that is append-only has SELECT and INSERT policies only — no UPDATE, no DELETE, and a
grant of only `select, insert`.** That absence is the guarantee; it is what makes "history is never
overwritten" true at the database layer instead of by client discipline.

**Corollary, and the reason this paragraph exists:** a correction to such a table is a
`security definer` RPC, never a client write. Soft-deleting a row means setting `deleted_at`, which
*is* an UPDATE — so a spec that says "append-only, and the client marks the wrong row deleted" is
self-contradictory. That exact contradiction shipped into three documents, each citing the missing
UPDATE policy as the guarantee for an operation the missing UPDATE policy forbade. Adding an UPDATE
policy to make it work is never the fix: it makes the table ordinarily mutable and destroys the
article.
Enforcement: migration review checklist; `platform/DECISIONS.md` governance rule; ADR-0029
decision 4.

### IXa. Authorization Is Server-Side
Every table has RLS enabled and scoped to `auth.uid()`, directly or transitively through its parent.
**Every view carries `security_invoker = on`** — a Postgres 15+ view otherwise executes as its owner,
bypasses RLS on the underlying tables, and returns every user's rows to every signed-in caller
through PostgREST. **Every `security definer` function performs its own explicit ownership check**
and sets `search_path`, because `security definer` is precisely the thing that turns RLS off.

Custom-schema objects need explicit `GRANT`s; `supabase db diff` emits neither the grants nor the
`security_invoker` clause, so both are hand-appended to the generated migration and verified by
reading it.

Client-side filtering is never authorization. A query that omits a user filter must return nothing,
not everything.
Enforcement: per-phase RLS test asserting a second user reads **zero rows from every table and every
view**; `platform/skills/dhruv-supabase-object`; ADR-0029, ADR-0033.

### X. Coverage Ratchets, Never Regresses
The JaCoCo line-coverage floor in the root `build.gradle.kts` only moves up, at a stated checkpoint,
never ahead of landed tests.
Enforcement: `./gradlew regressionCheck`, ADR-0013.

### Xa. Documentation Tracks Reality
A spec describes what will be built **until it is built**, and what *was* built from then on. When a
phase ships, its `spec.md` gains an **Implementation record** stating what actually landed, what
deviated from the spec and why, and what was deferred — deferrals with a stated reason, never
silently dropped scope.

**This obligation does not end at the checkpoint.** Any later change to shipped behaviour — a defect
fix, a functional change, a schema migration, a removed feature — updates the owning spec's
Implementation record in the **same change** that alters the behaviour, plus `CHANGELOG.md`, plus
any registry row it touches. A bug fix names the FR whose stated behaviour was not actually
delivered; that is what separates a fix from an undocumented behaviour change.

The failure this article exists to prevent is well attested here: an SDD claimed certificate pins
the code never used and a component that never existed; three design documents each claimed
authority while two described a UI that was never built (ADR-0030); a spec asserted a correction
path its own RLS forbade. **A confidently wrong document is worse than a missing one**, because it
is trusted.

If a change makes a doc wrong and there is no time to fix it properly, say so in the doc — a dated
"known stale" line is honest; leaving the false statement standing is not.
Enforcement: per-spec closure tasks; the PR template checklist; `scripts/ci/doc_link_check.py`;
code review.

### XI. Stack Is Fixed
Kotlin + Jetpack Compose + Koin (DI) + Coroutines/Flow only. No Hilt (AGP 9 incompatible, ADR-0010).
No Kover (AGP 9 incompatible, ADR-0013). No supabase-kt/Ktor (unproven on this toolchain, ADR-0029)
— Supabase is plain REST on Retrofit/Moshi/OkHttp. Do not reopen a stack choice already settled by
an ADR; propose a new ADR instead of silently diverging.
Enforcement: `platform/AGENTS.md` hard rules; code review.

## Spec-Kit Directory Rule

**Default: app-level.** A spec-kit feature directory lives under `apps/<app>/specs/NNN-slug/`
(e.g. `apps/finance/specs/001-net-worth-tracker/`) — pass this explicitly as
`SPECIFY_FEATURE_DIRECTORY` when running `/speckit-specify` for app-scoped work, which is nearly
everything. Root `specs/NNN-slug/` is reserved for features that are **genuinely cross-app** (touch
more than one of `apps:finance`/`apps:tools`/`apps:vault`/`apps:health`/`apps:relationship`, or the
web SPA, in the same unit of work) — check before defaulting to root; when in doubt, it's app-level.

**Why**: this repo already isolates docs per app (`apps/<app>/docs/`, 2026-08-15 doc migration) —
a root-level `specs/` for every feature would recreate exactly the mixed-ownership problem that
migration fixed. Each app's own `NNN-` numbering sequence is independent (Finance's `001-…` does
not collide with a future Tools `001-…`) — numbering is scoped per `specs/` directory, not global.

**Tracking**: which design-v1 phase (implementation plan §7) maps to which `apps/finance/specs/NNN-`
directory is recorded at the top of that plan's Phase Plan section (§7) — check there before
creating a new phase's spec-kit directory, so phase numbers and spec-kit numbers don't drift apart
by accident. Read that one line instead of scanning every `specs/` directory to find out what
exists — the whole point is a future session shouldn't have to load every spec to get its bearings.

## Spec-Kit Artifact Mapping

Each Dhruv "phase" (design-v1 implementation plan §7) is one spec-kit feature:

| spec-kit artifact | Dhruv source |
|---|---|
| `spec.md` | Functional spec §A–G screens + `*-BR-*`/NFR rows for that phase |
| `plan.md` | Implementation plan §3–§6 (topology, schema, nav, modules) for that phase |
| `tasks.md` | Implementation plan §7 phase step table, role-ordered (SA/QA/Backend/Android/Sec) |
| `checklists/qa.md` | QA Test Scenario Catalog rows for that phase's module codes |

`/speckit-specify` and `/speckit-plan` output for a Dhruv feature must stay technology-free in
`spec.md` (the *what*) and put schema/interceptor/module decisions only in `plan.md` (the *how*) —
Dhruv's existing specs mixed these; do not repeat that in new ones.

## Development Workflow

Fixed step order per phase — no step 3/4 before steps 1–2 complete:

1. **SA** finalizes schema/migration + `NavTarget`/route-registry additions.
2. **QA** writes/reviews that phase's QA catalog rows against the functional spec — before any code.
3. **Backend**: RED (failing repository/mapper/interceptor tests citing scenario IDs) → GREEN → REFACTOR.
4. **Android**: RED (failing ViewModel/screen-state tests citing scenario IDs) → GREEN → REFACTOR.
5. **QA** executes/verifies scenario rows, closes them, updates the coverage-summary table.
6. **Sec** DPDP/secrets/RLS checklist pass (skipped only if the phase touches no off-device data).
7. **Checkpoint**: `regressionCheck` green, coverage floor not regressed, every phase scenario row
   CLOSED or explicitly deferred with a stated reason. Merge gate.

`/speckit-converge` is run against a phase's `spec.md`/`plan.md` before extending that phase further
— it is how drift between spec and shipped code (the failure ADR-0030 named) gets caught early
instead of accumulating until an audit forces a retirement.

## Governance

This constitution amends by new dated note or version bump here — never by silently editing a
principle's meaning out from under existing specs. Conflicts with `CLAUDE.md`, `platform/AGENTS.md`,
`platform/PLATFORM.md`, `platform/DECISIONS.md`, or `platform/DESIGN-SYSTEM.md` are resolved in
favor of those documents; this file is scoped to how spec-kit's workflow operates inside Dhruv's
existing rules, not a second source of architectural truth.

**Version**: 1.1.0 | **Ratified**: 2026-08-16 | **Last Amended**: 2026-08-16 (added Spec-Kit
Directory Rule — app-level vs global placement)
