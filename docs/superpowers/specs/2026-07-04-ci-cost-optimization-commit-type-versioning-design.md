# Design — CI Cost Optimization (zero double builds) + Commit-Type-Driven Version Bump

- **Date:** 2026-07-04
- **Scope:** `.github/workflows/` (`ci.yml`, `fast-feedback.yml`, new `owasp-weekly.yml`), new `scripts/ci/bump_version.py`, `platform/` docs
- **Status:** Approved design → ready for implementation (plan was approved in-session 2026-07-04; implementation deferred to a dedicated branch)
- **Related:** PLATFORM.md §11/§12, DECISIONS.md ADR-0009/ADR-0011/ADR-0012/ADR-0013; will add **ADR-0015** and **ADR-0016**
- **Branch note:** implement on a fresh `chore/*` branch off `develop` — NOT on `feat/networth-tracker`.

---

## 1. Problem & goals

Two maintainer asks, one change set:

1. **Cost / build time.** Audit of the three workflows found **no infinite trigger loops** (the
   `[skip ci]` guard on the auto-bump commit, GitHub's tag-push anti-recursion for `GITHUB_TOKEN`,
   and `workflow_dispatch`-only `release.yml` are all correct), but heavy **duplicate work**: every
   merged PR burns ~2–3 runner-hours. Goal: **exactly ONE test-suite build (on the PR) and exactly
   ONE release build (on the merge push) per change** — no other builds anywhere in the lifecycle.
2. **Versioning.** CI auto-bumps only the patch segment (ADR-0011); minor/major are manual edits to
   `platform/versions.json` (the maintainer just had to hand-edit 1.2.x → 1.3.0 for the networth
   feature). Goal: the pipeline bumps the **respective** semver segment itself from commit types —
   `feat:` → minor, `type!:` / `BREAKING CHANGE:` → major, everything else → patch.

Expected effect: ~75–80 % fewer GitHub-hosted runner minutes per merged PR; zero manual version
edits ever again.

### Non-goals (YAGNI)
- No change to the 4-gate model **on PRs** — PRs keep full validation.
- No merge queue, no self-hosted runners, no third-party release tooling (semantic-release etc. —
  the existing bash+python approach stays, per the repo's "no new machinery" bias).
- No change to `release.yml` (manual re-publish tool — already loop-free, already skips tests).
- No change to Gradle build performance itself (caching config in `gradle.properties` stays as is).

---

## 2. Audit findings — every double build, and its fix

| # | Double build found | Where | Fix (§) |
|---|--------------------|-------|---------|
| 1 | Full 4-gate run on the PR **+ identical re-run on the merge push** (same tree) | `ci.yml` `on: push` + `on: pull_request` | Push runs NO gates — release job only (§4.A) |
| 2 | `fast-feedback` compile+test **+ ci.yml PR run** on the same commit once a PR is open | `fast-feedback.yml` | Skip fast-feedback when an open PR exists for the branch (§4.E) |
| 3 | `tests` job compiles the whole project (`regressionCheck`) then the `build` job **re-compiles `assembleDebug` on a fresh runner** | `ci.yml` Gates 3+4 | Delete `build` job; fold `assembleDebug` into `tests` on the warm daemon (§4.C) |
| 4 | OWASP runs **twice per merge** (PR + push), ~700 MB NVD update, up to 30 min, `continue-on-error` masks every finding → zero gate value | `ci.yml` `owasp` job | Weekly `schedule:` workflow, out of the merge path (§4.B) |
| 5 | **Docs-only commits** (spec/markdown work is frequent in this repo) trigger full builds and even a version bump + APK + Release | `ci.yml` (no path filtering — deliberately, see the trigger comment) | Path-filter **gate job** that skips builds AND the release while still reporting required statuses (§4.D) |
| 6 | Historical double-publish risk: ci.yml release job + tag-triggered `release.yml` | already fixed — `release.yml` is `workflow_dispatch`-only | none needed (verified) |

**Not removable:** the release job's `assembleRelease` on the merge push. The versioned signed APK
*is* the release; it must be built after the bump so it carries the new versionName/versionCode,
and it is a different variant from anything the PR built. Single build — acceptable.
The auto-bump commit is `[skip ci]` → triggers nothing (also natively honored by Actions).

### Lifecycle after this change (event → what actually runs)

| Event | Jobs that run | Builds |
|-------|---------------|--------|
| Push to `feat/**` (no PR yet) | fast-feedback (compile debug + unit tests) | 1 |
| Push to `feat/**` (PR open) | fast-feedback pr-check job only (seconds, then skip); ci.yml PR run | 1 (the PR run) |
| PR opened / synchronized | `changes` gate → Gates 1–3 (incl. folded debug build) → pr-summary | 1 |
| PR docs-only | `changes` gate → GitLeaks only; Gates 1+3 skipped (reported as skipped = pass) | 0 |
| Merge push to `develop` | `changes` gate → `release` only (bump → signed APK → tag → Release) | 1 (release variant) |
| Merge push, docs-only | `changes` gate → everything skipped; no bump, no APK, no Release | 0 |
| Auto-bump commit `[skip ci]` | nothing | 0 |
| Weekly cron | owasp-weekly | 0 app builds |

---

## 3. Key decisions (settled with maintainer, 2026-07-04)

| # | Decision | Rationale |
|---|----------|-----------|
| D1 | **Commit-type-driven bump**: `feat:` → minor, any `type!:` or `BREAKING CHANGE:`/`BREAKING-CHANGE:` in body → major, else patch. Highest wins across the push range; default patch. | Maintainer picked this over per-app path detection. Matches ADR-0011's semantics (PATCH = fix/merge) while removing the manual minor/major step. |
| D2 | **Merge push runs the release job ONLY** — no gates re-run. (Supersedes an earlier in-session "keep tests on push" preference; the final instruction was "avoid all possible double builds".) | The PR already validated the identical tree; enforced by required up-to-date branches (D3). Release's own `assembleRelease` still catches compile-level breakage. |
| D3 | **Repo setting prerequisite**: branch protection on `develop` enables *"Require branches to be up to date before merging"*. | This is what makes D2 safe — the merged tree is byte-identical to the PR-validated tree. Without it, a stale branch could merge semantically-conflicting code untested. |
| D4 | OWASP moves to a **weekly scheduled workflow** + `workflow_dispatch`. | It was warn-only with findings masked (`continue-on-error`) — pure cost in the merge path, zero gate value lost by moving it. |
| D5 | Both parts (cost cuts + version bump) ship in **one change set**. | Maintainer choice; they touch the same `release` job. |
| D6 | `main` pushes always bump **patch**, never minor/major. | Promotion `develop → main` replays develop's already-bumped `feat:` commits in the push range; re-detecting them would double-bump. |
| D7 | Version bump logic lives in **`scripts/ci/bump_version.py`**, not inline YAML heredoc. | Same precedent as `scripts/ci/regression_summary.py`; locally testable with `--dry-run`. |
| D8 | Docs-only merges produce **no version bump / APK / Release**. | A release per docs commit is noise + cost; the `changes` gate condition on the release job implements this. |

---

## 4. Design — Part 1: CI cost cuts

### A. Merge push = `release` job only (`ci.yml`)

- `static-analysis`, `security`, `tests` all gain the event condition (combined with §D's filter):

  ```yaml
  if: github.event_name == 'pull_request' && needs.changes.outputs.code == 'true'
  ```

  (`security`/GitLeaks: `if: github.event_name == 'pull_request'` only — it must run on docs-only
  PRs too, secrets hide in docs.)
- `release` drops `needs: [build]` → `needs: [changes]`, keeps its own `if` (push to
  develop/main, `!contains(head_commit.message, '[skip ci]')`) plus
  `needs.changes.outputs.code == 'true'`.
- **Release-notes artifact fetch becomes cross-run**: today the release step downloads
  `regression-summary` / `coverage-report` artifacts from *the same run*; after this change those
  jobs don't run on push. Replace with a best-effort lookup: `gh api` (or `actions/github-script`)
  finds the latest successful **PR run** for `github.event.head_commit.id`'s tree, then
  `actions/download-artifact@v4` with `run-id: <that run>` + `github-token`. Both steps
  `continue-on-error: true` — release notes degrade gracefully (the "coverage on every merge"
  promise of ADR-0013 is kept via the PR run's artifacts, not a re-run).

### B. OWASP → `owasp-weekly.yml` (new workflow)

```yaml
on:
  schedule:
    - cron: "0 3 * * 1"   # Mondays 03:00 UTC
  workflow_dispatch:
```

- Move the existing `owasp` job content **verbatim** (JDK setup, Gradle cache read-only, NVD
  year-month cache key + restore-keys, `dependencyCheckAnalyze` with `continue-on-error: true`,
  report upload).
- Delete the `owasp` job from `ci.yml`; remove it from `pr-summary`'s `needs` and table (replace
  the row with a static note: "OWASP: weekly scheduled scan").

### C. Fold Gate 4 into the tests job (`ci.yml`)

- Delete the `build` job entirely.
- In `tests`, after the regression/coverage steps, add:

  ```yaml
  - name: Assemble debug APK (Gate 4)
    run: ./gradlew :apps:finance:app:assembleDebug
  ```

  Warm daemon + compile outputs already produced by `regressionCheck` → minutes, not a fresh
  runner with cold cache restore.
- Debug-APK artifact upload stays (PR-only by virtue of the job now being PR-only),
  `retention-days: 7`.
- Rename job: `"Gate 3+4 · Tests + ArchUnit + Coverage + Build"`. Update `pr-summary` `needs`
  and its table rows (Gate 4 row merges into the Gate 3 row).

### D. Docs-only short-circuit (`ci.yml`)

- New **first job** `changes` (runs on both PR and push, finishes in seconds):
  - `dorny/paths-filter@v3` (or plain `git diff --name-only` against the merge-base for PRs /
    `github.event.before` for pushes — implementer's choice, paths-filter is less code).
  - Output `code: 'true'` unless **all** changed files match `docs/**` or `**/*.md`.
  - Keep the filter list conservative: `platform/**` counts as **code** (versions.json and
    feature-flag JSON feed the build/release), only `docs/**` and markdown are "docs".
- Consumers: `static-analysis`, `tests`, `release` add `needs: changes` +
  `needs.changes.outputs.code == 'true'` to their `if`.
- **Why job-level `if`, not trigger-level `paths-ignore`:** required status checks. A job skipped
  via `if` is reported as *skipped*, which branch protection counts as passing; `paths-ignore`
  never creates the check runs at all, leaving required checks permanently pending and blocking
  the merge (the exact failure mode the existing comment at the top of `ci.yml` warns about).

### E. fast-feedback dedupe (`fast-feedback.yml`)

- New tiny first job `pr-check` (ubuntu, seconds):

  ```yaml
  - id: pr
    env:
      GH_TOKEN: ${{ github.token }}
    run: |
      COUNT=$(gh pr list --repo "$GITHUB_REPOSITORY" --head "$GITHUB_REF_NAME" \
              --state open --json number --jq 'length')
      echo "open=$COUNT" >> "$GITHUB_OUTPUT"
  ```

- `compile-and-test` gains `needs: pr-check` +
  `if: needs.pr-check.outputs.open == '0'`.
- Effect: pre-PR pushes keep fast feedback; once the PR exists, ci.yml's `pull_request` run is the
  single authoritative build (removes ~10–15 min duplicated per push during review).

### F. Small wins

1. **Gradle cache writer moves to `release`.** `tests` no longer runs on develop pushes, so
   nothing would write the trunk cache (branch caches are not shared across PRs — only the default
   branch's are). Set `cache-read-only: false` in the `release` job (was `true`); `tests` keeps
   `cache-read-only: false` on PRs (warm across pushes to the same PR).
2. **Artifact retention** (currently defaulting to 90 days, billed storage): `lint-reports` 7,
   `test-reports` 7, `coverage-report` 14, `regression-summary` 7 (`debug-apk` 7 and
   `fast-feedback-test-reports` 3 already set).
3. **Cache the ktlint binary** in `static-analysis` (`actions/cache`, key `ktlint-1.5.0`) instead
   of downloading it every run.

---

## 5. Design — Part 2: commit-type-driven version bump

### G. `scripts/ci/bump_version.py` (new; replaces the inline python heredoc in the release job)

Behavior:
- Args: `--bump {major,minor,patch}` (required), `--build-number N` (required),
  `--file` (default `platform/versions.json`), `--dry-run`.
- For every **active** app (skip `status: planned|future`):
  major `X.Y.Z → X+1.0.0` · minor `→ X.Y+1.0` · patch `→ X.Y.Z+1`; `buildNumber` = new
  versionCode.
- Prints the first active app's new version to stdout; appends `version=<v>` to `$GITHUB_OUTPUT`
  when set (not in dry-run). Non-zero exit on malformed input or zero active apps (aborts the
  release before anything is committed).
- Pure stdlib, same style as `scripts/ci/regression_summary.py`.

Reference implementation (drop in as-is):

```python
#!/usr/bin/env python3
"""Bump app versions in platform/versions.json (commit-type-driven, see ADR-0015).

Called by ci.yml's `release` job after it derives the bump type from the commit
messages in the push range (feat: -> minor, type!:/BREAKING CHANGE -> major,
anything else -> patch; pushes to main are always patch).

  * major: X.Y.Z -> X+1.0.0
  * minor: X.Y.Z -> X.Y+1.0
  * patch: X.Y.Z -> X.Y.Z+1

Every ACTIVE app (no "planned"/"future" status) is bumped; buildNumber is set to
--build-number (the new versionCode). The new version of the first active app is
printed to stdout (single line) for the workflow to capture, and appended as
`version=<v>` to $GITHUB_OUTPUT when that is set.

Pure stdlib. Exits non-zero on a malformed versions.json so the release job
aborts before committing anything.
"""
from __future__ import annotations

import argparse
import json
import os
import sys

VERSIONS_JSON = "platform/versions.json"


def bump_semver(version: str, bump: str) -> str:
    major, minor, patch = (int(p) for p in version.split("."))
    if bump == "major":
        return f"{major + 1}.0.0"
    if bump == "minor":
        return f"{major}.{minor + 1}.0"
    return f"{major}.{minor}.{patch + 1}"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--bump", required=True, choices=("major", "minor", "patch"))
    parser.add_argument("--build-number", required=True, type=int)
    parser.add_argument("--file", default=VERSIONS_JSON)
    parser.add_argument("--dry-run", action="store_true",
                        help="print the result, do not write the file")
    args = parser.parse_args()

    with open(args.file, encoding="utf-8") as f:
        data = json.load(f)

    first_version = None
    for app, meta in data.get("apps", {}).items():
        if meta.get("status") in ("planned", "future"):
            continue
        old = meta["version"]
        meta["version"] = bump_semver(old, args.bump)
        meta["buildNumber"] = args.build_number
        print(f"{app}: {old} -> {meta['version']} ({args.bump}, "
              f"buildNumber={args.build_number})", file=sys.stderr)
        if first_version is None:
            first_version = meta["version"]

    if first_version is None:
        print("ERROR: no active app found in versions.json", file=sys.stderr)
        return 1

    if not args.dry_run:
        with open(args.file, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)
            f.write("\n")

    github_output = os.environ.get("GITHUB_OUTPUT")
    if github_output and not args.dry_run:
        with open(github_output, "a", encoding="utf-8") as f:
            f.write(f"version={first_version}\n")

    print(first_version)
    return 0


if __name__ == "__main__":
    sys.exit(main())
```

### H. "Determine bump type" step in the `release` job

Inserted before the versions.json step (the release checkout already has `fetch-depth: 0`, so the
full commit range is available):

```bash
BASE="${{ github.event.before }}"
if [ "$BASE" = "0000000000000000000000000000000000000000" ] || ! git cat-file -e "$BASE" 2>/dev/null; then
  RANGE="${GITHUB_SHA}~1..${GITHUB_SHA}"     # new-branch / edge fallback
else
  RANGE="${BASE}..${GITHUB_SHA}"
fi
BUMP=patch
LOG=$(git log --format='%s%n%b' "$RANGE")
echo "$LOG" | grep -Eq '^[a-zA-Z]+(\([^)]*\))?!:' && BUMP=major
echo "$LOG" | grep -Eq 'BREAKING[ -]CHANGE:'      && BUMP=major
if [ "$BUMP" != "major" ]; then
  echo "$LOG" | grep -Eq '^feat(\([^)]*\))?:' && BUMP=minor
fi
# main = promotion of develop; its range replays develop's already-bumped feat commits → force patch (D6).
[ "${{ github.ref_name }}" = "main" ] && BUMP=patch
echo "bump=$BUMP" >> "$GITHUB_OUTPUT"
```

Notes:
- Merge commits (`Merge pull request #N …`) match nothing → contribute only the patch default; the
  underlying `feat:`/`fix:` commits are inside the range and carry the signal. Works for both
  merge-commit and squash-merge styles.
- The old inline-python step is replaced by:
  `python3 scripts/ci/bump_version.py --bump "<bump output>" --build-number "<new versionCode>"`.
- Auto-bump commit message gains the type:
  `chore: auto-bump (minor) to v1.3.0 (versionCode=10) [skip ci]`.
- `VERSION_CODE` increment, `VERSION_NAME` sync, APK verify, tag creation (idempotent), Release
  publishing: **unchanged**.

Expected detection matrix (also the local test fixture, §8.2):

| Commit subject / body | Bump |
|---|---|
| `feat: add networth screen` | minor |
| `feat(tracker): add assets` | minor |
| `fix: rounding error` | patch |
| `chore: deps` / `docs: spec` / `refactor: …` | patch |
| `feat!: drop api v1` | major |
| `refactor(core)!: split module` | major |
| body contains `BREAKING CHANGE: …` or `BREAKING-CHANGE: …` | major |
| `Merge pull request #16 from …` (alone) | patch |

---

## 6. Docs to update in the same change

1. **`platform/DECISIONS.md`**
   - **ADR-0015 — Commit-type-driven semver bump.** Amends ADR-0011: CI owns all three segments;
     manual minor/major edits of `versions.json` are no longer needed and are discouraged (a
     manually raised version still works as a new baseline but risks a double bump if the same
     merge also contains `feat:` commits); `main` always patch (D6); detection rules per §5.H.
   - **ADR-0016 — CI cost model: single-validation pipeline.** PR = the only full-gate validation
     pass; merge push = release build only, safe under required up-to-date branches (D3); OWASP
     weekly; docs-only skip via `changes` gate job; fast-feedback dedupe; cache-writer moves to
     the release job. Record the audit table from §2 as context.
2. **`platform/PLATFORM.md`** — §11: gates table gains "runs on: PR" vs "runs on: merge push"
   column/notes, post-build description updated (release job now the only push-side job, bump-type
   step, cross-run artifact fetch); §12: "Patch is auto-incremented" bullet → "the segment is
   chosen from commit types (feat→minor, breaking→major, else patch)"; drop the "Minor/Major are
   bumped manually" sentence.
3. **`platform/versions.json`** — `notes` field: all three version segments are CI-owned.

---

## 7. Prerequisites & caveats

1. **Branch protection (manual, BEFORE merging this change):** enable *"Require branches to be up
   to date before merging"* on `develop`. Without it, D2 is not safe.
2. **`feat/networth-tracker` double-bump caveat:** that branch carries a manual 1.3.0 edit in
   `platform/versions.json`. If Part 2 is live when it merges (with `feat:` commits), CI bumps
   again → 1.4.0. **Revert the manual edit in that branch before merging it** — CI will produce
   1.3.0 itself from the 1.2.7 baseline.
3. **`pr-summary` required-checks note:** stays informational-only (ADR-0012). The gate jobs'
   *names* change (`build` disappears, `tests` renamed) — if any required-status-check names are
   configured in branch protection, update them to match the new job names.
4. **Concurrency blocks unchanged** — PR runs still cancel superseded runs; push runs are never
   cancelled.

---

## 8. Verification (success criteria)

Local, before pushing:
1. **bump script**: copy `platform/versions.json` to a temp dir;
   `python scripts/ci/bump_version.py --file <copy> --build-number 10 --dry-run --bump <each>` →
   assert `1.2.7 → 2.0.0 / 1.3.0 / 1.2.8` and `tools`/`vault` (planned/future) untouched.
2. **detection regexes**: feed the §5.H matrix strings through the grep chain in a scratch bash
   script; assert every row.
3. **workflow syntax**: `actionlint` on all three workflows if available; otherwise GitHub's
   parser on push to the feature branch is the check.

On GitHub, after merge (in order):
4. Docs-only PR → gates skip in seconds; its merge produces **no** bump/APK/Release.
5. `fix:` PR → PR runs full gates **once**; merge push shows exactly two jobs (`changes`,
   `release`); patch bump; Release notes still contain test/coverage summary (cross-run fetch).
6. `feat:` PR → minor bump; auto-bump commit message shows `(minor)`.
7. Push a commit to a branch with an open PR → fast-feedback `pr-check` runs, main job skipped.
8. Trigger `owasp-weekly` via `workflow_dispatch` → report artifact appears.
9. Actions usage graph for the repo drops accordingly (~75–80 % per merged PR).

---

## 9. Implementation task breakdown (ordered)

- [ ] **T1 — `scripts/ci/bump_version.py`** (§5.G reference implementation).
  Verify: local dry-runs (§8.1). Files: 1 new.
- [ ] **T2 — `ci.yml` rework** (§4.A/C/D/F + §5.H): add `changes` job; event+changes `if` on
  gates; delete `build` + `owasp` jobs; fold `assembleDebug` into `tests`; release job — needs,
  bump-type step, script call, commit message, cache writer, cross-run artifact fetch, retention
  days; `pr-summary` needs/rows. Verify: actionlint + a scratch PR run. Files: 1.
- [ ] **T3 — `owasp-weekly.yml`** (§4.B). Verify: `workflow_dispatch` run. Files: 1 new.
- [ ] **T4 — `fast-feedback.yml` dedupe** (§4.E). Verify: push to branch with open PR → skipped.
  Files: 1.
- [ ] **T5 — docs** (§6): ADR-0015, ADR-0016, PLATFORM.md §11/§12, versions.json notes. Files: 3.
- [ ] **T6 — repo settings**: up-to-date-branches protection (§7.1); check required-check names
  (§7.3). Manual, GitHub UI.
- [ ] **T7 — end-to-end verification** (§8.4–8.9) with one docs-only PR, one `fix:` PR, one
  `feat:` PR.

## Boundaries

- **Always:** keep GitLeaks running on every PR event (docs included); keep the release job's
  APK verify step (signed, ≤50 MB) untouched; keep `[skip ci]` on auto-bump commits.
- **Ask first:** any change to branch-protection required-check semantics beyond renames; adding
  third-party actions beyond `dorny/paths-filter`.
- **Never:** re-introduce a tag-triggered release path (double-publish risk, §2.6); put version
  logic back inline in YAML; let CI bump versions on PR events (push-only, always).

## Open questions

None — all decisions settled (§3). Implementer chooses `dorny/paths-filter` vs plain `git diff`
in §4.D (functionally equivalent here).
