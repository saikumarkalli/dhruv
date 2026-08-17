# Dhruv Versioning Protocol

> Status: **DESIGN — not yet implemented.** §1–§8 are the intended contract; §9 is the build order.
> Governing decisions: ADR-0011 (CI owns the bump), ADR-0025 (commit types pick the segment),
> ADR-0032 §9 (`main` derives its own segment). A future ADR ratifies this document.
>
> Written 2026-08-17 after an audit found **five of eight** version locations drifted or dead.
> **Decision D1 (2026-08-17, maintainer): components version independently.** The web app keeps its
> own line at `0.1.0`; it does not inherit the Android app's number. Everything below follows from
> that — it is the reason a single repo-wide bump is not enough.

---

## 1. The two rules

1. **`platform/versions.json` is the only place a version is authored.** Every other occurrence in
   the repository is generated from it or checked against it.
2. **Each component carries its own version line, bumped only by changes to its own paths.** A
   web-only merge does not touch the Android version, produce an APK, or cut an Android tag — and
   the reverse.

Rule 1 is stated today (versions.json's `notes` field says "CI-owned") but enforced for only two
files. Rule 2 does not exist today at all: `bump_version.py` bumps *every* active app with *one*
segment derived from *all* commits in the range.

---

## 2. Version axes

Four different things carry versions. Conflating them causes most of the drift.

| Axis | Means | Moves when |
|---|---|---|
| **Release version** (`2.0.4`) | what a user sees; names the tag/APK/Release | that component's paths change on a promotion to `main` |
| **Build number** (`versionCode 16`) | Android's monotonic install ordinal | every Android release; never reused, never decreases |
| **Contract version** (`libs.core`, `libs.settings`, `platformContractVersion`) | compatibility between internal modules | that library's public contract changes |
| **Document version** (`PRIVACY.md` "Policy version: 2.0") | legal revision | policy text materially changes |

The fourth is **out of scope** — it tracks a legal document, not code. Named here so nobody wires it
into the automation by accident.

---

## 3. Audit — every version location, 2026-08-17

Evidence-based. `❌` = observed drift.

| # | Location | Value | Written by | State |
|---|---|---|---|---|
| 1 | `versions.json` › `apps.finance.version` | `2.0.4` | CI `bump_version.py` | ✅ authoritative |
| 2 | `versions.json` › `apps.finance.buildNumber` | `16` | CI | ✅ |
| 3 | `gradle.properties` › `VERSION_NAME` / `VERSION_CODE` | `2.0.4` / `16` | CI | ✅ in sync |
| 4 | `web/package.json` › `version` | `0.1.0` | nobody | ❌ never bumped since scaffold |
| 5 | `versions.json` › `web.finance.version` | `0.1.0` | "manually" per its own note | ❌ matches #4 only by coincidence |
| 6 | `versions.json` › `libs.*`, `platformContractVersion` | `1.0.0` | nobody, ever | ❌ dead field |
| 7 | `README.md` badge + §2 status block | was `1.2.5` / `versionCode 7` | hand-edited | ❌ **8 releases stale**; hand-corrected 2026-08-17, will re-drift |
| 8 | `CHANGELOG.md` | newest entry `[2.0.0]` | hand-edited | ❌ `2.0.1`–`2.0.4` undocumented; `[Unreleased]` sits *below* `[2.0.0]` |

### Adjacent findings

- **`scripts/bump-version.sh` is a live footgun.** It does a manual bump → commit → tag. ADR-0025
  warns that a hand-raised version plus `feat:` commits in the same merge **double-bumps**. It
  contradicts an accepted ADR and is deleted by this protocol (§8).
- **`requiresCore: ">=1.0.0"` is vacuous today** — nothing reads it. Under independent versioning it
  becomes checkable and is enforced (§6.3).
- **`minVersion` gates only Android.** `dhruv-finance.json` sets `assistant.minVersion = "1.2.0"`;
  Android honours it via `BuildConfig.VERSION_NAME` (`PlatformModule.kt:42`), but
  `web/src/shared/hooks/useFeatureFlag.ts:20` reads only `.enabled`. **Independent versioning makes
  this unfixable as-is**: with Android at `2.x` and web at `0.x`, one bare string cannot mean the
  same thing on both. → §5.
- **The web app displays no version.** The bug-report form makes Version required; a web reporter
  cannot answer it. → §7.

---

## 4. The component registry

Each component is a row in `platform/versions.json`, extended with the three fields that make it
self-describing. The machine and this document then cannot disagree — the paths that trigger a bump
are data, not prose.

```jsonc
"apps": {
  "finance": {
    "version": "2.0.4",
    "buildNumber": 16,
    "status": "active",
    "paths":    ["apps/finance/**", "libs/**", "build-logic/**",
                 "gradle/**", "settings.gradle.kts", "build.gradle.kts"],
    "tag":      "dhruv-finance-v{version}",
    "artifact": "github-release",          // signed APK attached
    "requiresCore": ">=1.0.0",
    "requiresSettings": ">=1.0.0"
  }
}
```

| Component | key | Owns | Tag | Artifact |
|---|---|---|---|---|
| Finance (Android) | `apps.finance` | `apps/finance/**`, `libs/**`, `build-logic/**`, `gradle/**`, root Gradle files | `dhruv-finance-v{version}` | signed APK + GitHub Release |
| Finance (web) | `web.finance` | `web/**` | `dhruv-web-finance-v{version}` | **tag only** — Vercel deploys from `main` on its own (ADR-0032 §5), so a GitHub Release would be a second artifact for the same deploy |
| Core library | `libs.core` | `libs/core/**` | — | none (never published, ADR-0001) |
| Settings library | `libs.settings` | `libs/settings/**` | — | none |
| Platform contract | `platformContractVersion` | `platform/contracts/**` | — | none |
| Tools / Vault / Health / Relationship | `apps.*`, `web.*` | — | — | `status: planned`/`future` → skipped entirely |

**Deliberate overlap.** `libs/**` appears in both `apps.finance.paths` and `libs.core.paths`. That is
correct, not a bug: a change to `:libs:core` changes the shipped app *and* the library contract, so
both lines move.

### 4.1 Paths that never trigger a bump

Excluded from **every** component's path match:

```
platform/versions.json    gradle.properties    web/package.json
CHANGELOG.md              README.md            web/src/shared/version.ts
```

These are the generated files (§5). Without this exclusion the release job's own
`[skip ci]` bump commit — which touches `gradle.properties`, inside `apps.finance.paths` — would
appear in the *next* promotion's range and force a phantom Android bump on a web-only release.
A generated file is an *effect* of a release, never a cause of one.

---

## 5. Source, Generated, Checked

Three tiers. A file sits in exactly one.

```
                    ┌───────────────────────────────────────┐
   T1  SOURCE       │  platform/versions.json               │  CI writes; humans never
                    └───────────────────┬───────────────────┘
                                        │  sync_versions.py --write
                    ┌───────────────────▼───────────────────┐
   T2  GENERATED    │  gradle.properties        (apps.finance)
                    │  web/package.json         (web.finance)
                    │  web/src/shared/version.ts(web.finance)
                    │  README badge + status    (all)
                    │  CHANGELOG heading        (per release)
                    └───────────────────┬───────────────────┘
                                        │  sync_versions.py --check
                    ┌───────────────────▼───────────────────┐
   T3  CHECKED      │  feature-flag minVersion (per platform)
                    │  requiresCore / requiresSettings
                    │  docs citing a version
                    │  tag + Release naming
                    └───────────────────────────────────────┘
```

**T2 is per-component.** `gradle.properties` mirrors `apps.finance` only; `web/package.json` and
`web/src/shared/version.ts` mirror `web.finance` only. A hand edit to a T2 file is *discarded* by
the next `--write`, and `--check` fails the PR that made it — drift is caught at review, not
discovered eight releases later.

### 5.1 Per-platform `minVersion`

Independent version lines break the shared flag file's bare-string `minVersion`. Android is at `2.x`
and web at `0.x`, so `"1.2.0"` cannot mean the same thing on both — read literally on web it hides
every gated feature forever. The schema becomes an object keyed by platform:

```jsonc
"assistant": {
  "enabled": true,
  "minVersion": { "android": "1.2.0" },   // key absent for a platform = no minimum there
  "requiresConsent": true
}
```

Validation (T3): `minVersion.android` ≤ `apps.finance.version`, `minVersion.web` ≤
`web.finance.version`. A minimum above the current version is a permanently-invisible feature and
fails the check.

Until the migration lands (§9 step 6), web continues to ignore `minVersion` — that is today's actual
behaviour, written down rather than left to be rediscovered.

---

## 6. The protocol

### 6.1 How a version moves

On every promotion to `main`, for each component `C` with `status: active`:

```
range  = <previous main>..<head>
files  = C.paths  −  §4.1 exclusions
log    = git log --format='%s%n%b' <range> -- <files>

if log is empty          →  C does not move. No bump, no tag, no artifact.
else segment = detect_bump.sh <<< log
     if C.version starts with "0."  and segment == major  →  segment = minor
     C.version = bump(C.version, segment)
```

Two behaviours worth stating plainly because they are new:

- **`detect_bump.sh` must not be called on an empty range.** Its documented contract is that empty
  input yields `patch`; the caller checks emptiness first and skips. Do not "fix" this inside
  `detect_bump.sh` — its current behaviour is correct for its own contract.
- **`0.x` components never take a major bump.** Semver §4: major-version-zero is initial
  development. A `type!:` commit on `web.finance` moves `0.1.0 → 0.2.0`, not `1.0.0`. Promoting web
  to `1.0.0` is a deliberate, manual, one-time act recorded in an ADR — never an automatic
  consequence of a `!` in a commit subject.

### 6.2 Build number

`versionCode` increments **only when `apps.finance` itself bumps**. Monotonic, never reused, never
decreased. A web-only promotion leaves it untouched — today it would have incremented for nothing,
burning an install ordinal on a release that produced no APK.

### 6.3 Cross-component constraints

Enforced by `--check`, no longer decorative:

- `apps.finance.requiresCore` must be satisfied by `libs.core.version`
- `apps.finance.requiresSettings` must be satisfied by `libs.settings.version`
- every `tag` pattern must resolve to a unique string across components
- no component version may decrease relative to its newest existing tag

### 6.4 Escape hatches

Paths cannot always tell the story — a docs-only change that must still cut a release, or a
refactor that touches a component but must not. Two commit trailers, honoured by the release job:

```
Release-Component: web.finance     force this component to bump even if no paths matched
Release-Skip: apps.finance         suppress this component's bump even if paths matched
```

Both are logged in the release job summary. `Release-Skip` never suppresses a **security** fix —
that is a review-time judgement, not something the tooling can decide, and it is called out here so
the hatch is not used casually.

### 6.5 Adding a new component

When Tools ships, or the web app gains a second surface:

1. Add the entry to `versions.json` with `version: "0.1.0"`, `status: "active"`, `paths`, `tag`,
   `artifact`.
2. Add its T2 targets to `sync_versions.py` and a test in `test_sync_versions.py`.
3. Add its feature-flag file, if any, to the `minVersion` validation set.

Nothing else. No workflow edit — the release job iterates the registry.

### 6.6 Rules for humans and agents

| | |
|---|---|
| **Always** | Write conventional-commit types — they pick the segment. Keep a commit scoped to one component's paths where practical; a commit spanning two components bumps both. |
| **Ask first** | Promoting a `0.x` component to `1.0.0`. Using `Release-Skip`. Changing a component's `paths`. |
| **Never** | Hand-edit `versions.json`, `gradle.properties`, `web/package.json` `version`, the README version markers, or `web/src/shared/version.ts`. Reuse or decrease a version or `versionCode`. Add a version number to a doc without it existing as a released tag. |

Preview what a merge will do, before merging:

```sh
python scripts/ci/bump_version.py --range origin/main..HEAD --dry-run
#   apps.finance   2.0.4 -> 2.0.4   (no matching paths, skipped)
#   web.finance    0.1.0 -> 0.2.0   (feat: 3 commits)
#   libs.core      1.0.0 -> 1.0.0   (no matching paths, skipped)
```

---

## 7. Where a version becomes visible

Version maintenance is pointless if a user reporting a bug cannot state one.

| Surface | Today | Target |
|---|---|---|
| Android Settings › About | ✅ reads `packageInfo.versionName` at runtime (`SettingsScreen.kt:98`) | unchanged |
| Web | ❌ nothing, anywhere | footer/Settings line from generated `version.ts` + build short SHA |
| GitHub Release | ✅ `Dhruv finance v2.0.4`, tag `dhruv-finance-v2.0.4` | unchanged; web gets `dhruv-web-finance-v0.2.0`, tag only |
| Bug report form | requires a version a web user cannot find | satisfied once the web surface exists |

Android deliberately reads the **installed package**, not a compile-time constant. Keep it that way —
the generated values feed the *build*; the app reports what actually shipped.

---

## 8. Lifecycle

```
feature branch ──► PR to develop ──────────► merge ──► promotion PR ──► merge to main
                    │                                                       │
                    │ sync_versions.py --check                              │ per-component:
                    │   T2 drift · T3 coherence                             │   path-scoped range
                    │   no version is bumped on a PR                        │   detect_bump.sh
                    └── fails the PR on drift                               │   bump_version.py     (T1)
                                                                            │   sync_versions.py --write (T2)
                                                                            │ one "[skip ci]" commit
                                                                            │ tag(s) + artifact(s) for
                                                                            │   the components that moved
                                                                            └ CHANGELOG heading prepended
```

- **A PR never bumps.** It proves the invariant holds. `--check` is a sub-second stdlib run inside
  the existing `static-analysis` job — no new job, no added billable minutes, ADR-0026's budget
  untouched.
- **`develop` merges bump nothing** (ADR-0032 decision 1 — `develop` has no release job).
- **The promotion to `main` is the only place a version changes**, and T1 + every affected T2 file
  change in the *same* commit. There is no window where they disagree.

### Retirements

`scripts/bump-version.sh` is **deleted**. It predates ADR-0025, writes T1 by hand, and creates the
double-bump ADR-0025 warns about. Its only legitimate use — "what will my commits do?" — is served
by `bump_version.py --dry-run` (§6.6).

---

## 9. Build order

Each step is independently mergeable and leaves the tree working.

| # | Step | Verify | State |
|---|---|---|---|
| 1 | Extend `versions.json` with `paths` / `tag` / `artifact` / `excludedPaths` | file parses; `bump_version.py` dry-run and its 9 tests unaffected | ✅ **done** |
| 2 | `sync_versions.py` (`--check` / `--write`) + `test_sync_versions.py` | 17 tests green; `--check` reported exactly the known drifts | ✅ **done** (writer landed with the checker — same code path inverted, splitting it would have meant fixing step 4's drifts by hand) |
| 3 | Wire `--check` into CI | new `Gate 1b · Version sync` job, `pull_request` only, **not** path-gated | ✅ **done** |
| 4 | Fix the drifts; README markers; `web/src/shared/version.ts`; CHANGELOG namespacing + backfill | `--check` exits 0 | ✅ **done** |
| 5 | Rewrite `bump_version.py` for per-component path-scoped bumps (`--range`, `--dry-run`, `0.x` major clamp, trailers); extend `test_bump_version.py` | dry-run on real history matches §6.6's expected output | ⬜ next |
| 6 | Per-platform `minVersion` schema + `HardcodedFeatureFlagResolver` + `useFeatureFlag.ts`; surface the version in the web UI from `version.ts` | flag-parity test across both platforms | ⬜ |
| 7 | Release job: iterate the registry, per-component tag/artifact, `--write`, CHANGELOG heading injection | a web-only promotion cuts a web tag and **no** APK | ⬜ |
| 8 | Delete `scripts/bump-version.sh`; write the ADR | no inbound references remain | ⬜ |

**Gate 1b is deliberately not gated on `changes.outputs.android`.** The surfaces it guards — README,
CHANGELOG, `web/package.json`, feature-flag minimums — are touched by web-only and docs-only PRs,
which is precisely how the README badge drifted eight releases unnoticed. Same reasoning ADR-0026
already applies to running GitLeaks on docs-only PRs. It uses no Gradle, no JDK and no cache, so it
is a rounding error against the ≤90 min/PR budget.

**Risk to watch in step 7.** The release job currently assumes exactly one artifact and one tag; the
`softprops/action-gh-release` step, the APK verification, and the dev-ref guard (ADR-0032 decision 7)
all sit on that assumption. They must become conditional on `apps.finance` having moved, or a
web-only promotion will fail trying to verify an APK that was never built.

**ADR number:** this takes **ADR-0035**. The reasoning below is unchanged and was correct — it was
the arithmetic that went stale. When this section was written, ADR-0033 was the highest *written*
register entry, so this document reserved 0034 by the register's own rule that a written entry
outranks a reservation, and instructed `docs/superpowers/plans/2026-08-15-agent-protocol-and-doc-
verifier.md` to move from 0034 to 0035. That assumed this plan would land next; **ADR-0034 (public
repository) landed first**, on 2026-08-18, while every step in §9's table was still unchecked. Both
reservations therefore shifted by one, preserving the order prescribed here — this document to
**0035**, the agent-protocol plan to **0036** — recorded in the third numbering-hygiene note in
`DECISIONS.md`. Confirm the number is still free with `grep '^## ADR-' platform/DECISIONS.md` at
step 8, not before: that check is the only authority, and this is now the third time a reservation
written ahead of execution has gone stale underneath a dormant plan.

---

## 10. Decisions

All four are recorded here rather than in prose above, so a reader can see what was chosen and why
without reconstructing it from the mechanism.

### D1 — Components version independently *(maintainer, 2026-08-17)*

The web app keeps its own line at `0.1.0`; it does not inherit the Android number. Everything in
§4/§6 follows from this — it is why a single repo-wide segment is not enough and why
`bump_version.py` has to become path-scoped.

### D2 — Contract versions are frozen and human-bumped, not auto-bumped

`libs.core`, `libs.settings` and `platformContractVersion` keep their own lines but do **not**
auto-bump from path matches.

**Why.** `libs/core/**` holds the entire design system, so a path-scoped rule would fire on nearly
every PR and produce a number nothing consumes. Under ADR-0001 nothing is published and every app
compiles against the same tree at the same commit, so `requiresCore` is structurally unviolatable
today — auto-incrementing it would be manufacturing motion, not information.

**What they get instead.** They are a *contract* axis (§2), bumped by hand in the PR that changes a
`:libs:*` public contract, gated on an ADR. `--check` enforces that every
`requiresCore`/`requiresSettings` constraint is actually satisfied and that an unparseable
constraint fails loudly rather than passing silently — which is what turns those fields from
decoration into a real check.

**Reversible in one field.** If Tools ships and can genuinely lag `:libs:core`, give the libs
entries `paths` and they join the auto-bumped set with no other change.

### D3 — CHANGELOG: heading-only injection, namespaced per component

The release job prepends `## [finance-2.0.5] - 2026-08-17` plus GitHub's generated PR list; the
narrative underneath stays hand-written.

**Why namespaced.** With independent lines, a single mixed version column is unreadable — `2.0.4`
and `0.2.0` in one chronological list tells the reader nothing about which app moved. The heading
prefix matches the tag name, so a changelog entry and a tag are trivially cross-referenced.

**Why heading-only.** Full generation reduces the file to a PR list; the hand-written narrative in
the `finance-2.0.0` entry is the part actually worth reading. Injecting only the heading guarantees
no release goes undocumented while leaving room for prose.

### D4 — Web gets a tag, not a GitHub Release

**Why.** Vercel already owns web deployment history and one-click rollback. A GitHub Release with a
`dist/` zip attached duplicates that badly — the zip cannot be redeployed to Vercel from GitHub, so
it would be an artifact nobody can use. The tag answers the one question Vercel's dashboard is bad
at: *which commit was web `0.2.0`*. `artifact: "tag-only"` in the registry encodes this, and
flipping it to `"github-release"` is a one-field change if that judgement turns out wrong.