# CI Cost Optimization + Commit-Type Version Bump — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cut GitHub Actions runner minutes ~75–80% per merged PR by eliminating every duplicate build, make CI choose the semver segment from commit types instead of always-patch, and close the remaining dev/prod infrastructure cost leaks found in the 2026-08-14 audit.

**Architecture:** The PR becomes the single full-validation pass; the merge push runs the release job only (safe because branch protection requires up-to-date branches). OWASP moves to a monthly cron out of the merge path. A `changes` gate job short-circuits docs-only work while still reporting required statuses — resolved via the compare API, with no checkout at all. Version-bump logic moves out of inline YAML into two testable scripts (`bump_version.py`, `detect_bump.sh`). Part 2 then covers everything outside Actions: a Supabase keep-alive, a currency-API TTL guard, local-first dev, and an explicit list of what was audited and deliberately left alone.

**Scope note:** Part 1 (Tasks 0–11) is one CI PR. Part 2's Task 13 is **app code and ships separately**; Tasks 12, 14 and 15 can ride with either. **Task 14 Step 4 (baseline capture) must run before Part 1 merges** — it is the only chance to measure the pre-change cost. **Task 16 (selective tests) ships last, as its own PR**, because it is the one change whose value depends on the measurement Task 14 produces, and the only one carrying a revert condition. **Task 18 (web CI) is its own PR** — different language, different reviewer, and the web code has never been linted, so it will surface unrelated failures that must not block the CI work. Task 17 (Dependabot) is one file and can ride with anything.

**Not purely a cost plan any more.** The 2026-08-14 pipeline audit found that the release job publishes a **non-functional APK** — see Task 5 Steps 5–7. That fix is folded into the cost PR because Task 5 already rewrites that exact job. The gaps this plan deliberately leaves open are listed in §CI/CD gaps this plan does NOT close.

**Tech Stack:** GitHub Actions YAML, bash, Python 3 stdlib, Gradle 9 / AGP 9, `gh` CLI (preinstalled on runners).

**Source spec:** [docs/superpowers/specs/2026-07-04-ci-cost-optimization-commit-type-versioning-design.md](../specs/2026-07-04-ci-cost-optimization-commit-type-versioning-design.md) — status Approved, implementation deferred to a dedicated branch. Section references below (§4.A, §5.G, …) point at that spec.

## Global Constraints

- **Branch:** fresh `chore/ci-cost-optimization` off `develop`. NOT on `feat/redesign-ui-ux-componenets`, NOT on `feat/networth-tracker` (spec §7, branch note).
- **All tasks land in ONE PR.** Intermediate commits are branch checkpoints only — task ordering below is chosen so no single commit leaves a dangling `needs:` reference, but the end-to-end behaviour is only observable after merge.
- **Commit prefixes for this branch: `ci:` or `chore:` only.** Never `feat:` — once Part 2 is live, a `feat:` commit in this very merge range would trigger a minor bump of the CI change itself.
- **Python: pure stdlib only**, same style as the existing `scripts/ci/regression_summary.py`.
- **No new third-party GitHub Actions.** The repo deliberately downloads the ktlint binary directly rather than take an action dependency ([ci.yml:57-61](../../../.github/workflows/ci.yml#L57-L61)); `dorny/paths-filter` is therefore replaced with a `gh api .../compare` call (spec §4.D explicitly allows implementer's choice), which also avoids a checkout in the one job that runs on every event.
- **ADR numbers are 0025 and 0026**, never 0015/0016 — that range collided with the accepted Web-app ADR (DECISIONS.md numbering-hygiene note).
- **Job-level `if:`, never trigger-level `paths-ignore`.** A skipped job reports as skipped (branch protection counts that as passing); `paths-ignore` never creates the check run and leaves required checks permanently pending.
- **Every `platform/**` file except `*.md` counts as CODE**, not docs — `versions.json` and the feature-flag JSON feed the build.

---

## File Structure

| File | Status | Responsibility |
|---|---|---|
| `scripts/ci/bump_version.py` | create | Bump every active app in `platform/versions.json` by a given segment; emit new version |
| `scripts/ci/test_bump_version.py` | create | unittest coverage of the above (stdlib, no pytest) |
| `scripts/ci/detect_bump.sh` | create | stdin commit-log text → `major`/`minor`/`patch` on stdout |
| `scripts/ci/test_detect_bump.sh` | create | Table-driven test of the §5.H detection matrix |
| `.github/workflows/ci.yml` | modify | Add `changes` gate; PR-only conditions on gates; delete `build` + `owasp` jobs; rework `release`; runner tuning |
| `.github/workflows/owasp-scheduled.yml` | create | The former `owasp` job on a monthly cron + `workflow_dispatch` (weekly once findings unmask) |
| `.github/workflows/fast-feedback.yml` | modify | Skip the compile job when an open PR already exists; runner tuning |
| `.github/workflows/supabase-keepalive.yml` | create | 5-daily ping so the free-tier Supabase project never auto-pauses |
| `scripts/ci/affected_modules.py` | create | Changed paths → the Gradle test tasks that actually need to run |
| `scripts/ci/test_affected_modules.py` | create | unittest coverage of the mapping, incl. `projectDir` remaps |
| `.github/workflows/regression-full.yml` | create | Weekly full `regressionCheck` — owns the coverage floor once PRs go selective |
| `scripts/ci/actions_usage.py` | create | Billed-minutes report per pipeline, from the Actions timing API |
| `scripts/ci/test_actions_usage.py` | create | unittest coverage of the aggregation (network injected, not called) |
| `.github/workflows/ci-usage-report.yml` | create | Monthly Job Summary of billed minutes (~1 min/month) |
| `.github/dependabot.yml` | create | Grouped monthly dependency updates (gradle, npm, actions) |
| `.github/workflows/web-ci.yml` | create | Web lint/typecheck/test/build — **separate PR** (Task 18) |
| `platform/DECISIONS.md` | modify | ADR-0025, ADR-0026 |
| `platform/PLATFORM.md` | modify | §11 gates table, §12 versioning bullets |
| `platform/versions.json` | modify | `notes` field — all three segments CI-owned |
| `supabase/README.md` | modify | Local-first dev workflow (Part 2) |
| `apps/finance/data/.../CurrencyRepository.kt` + tests | modify | TTL guard — **separate PR** (Part 2, Task 13) |

**Deliberate refinement of spec §5.H:** the spec puts the bump-type detection inline in the release job's YAML. This plan extracts it to `scripts/ci/detect_bump.sh` reading the commit log from **stdin**, for exactly the reason D7 gave for moving the Python out of YAML: inline heredoc logic cannot be tested locally. Behaviour is byte-identical to the spec's grep chain. Everything else follows the spec as written.

---

## Task 0: Repo settings prerequisite (manual, do FIRST)

**Files:** none — GitHub web UI.

This is what makes the whole change safe (spec D3). Without it, a stale branch can merge semantically-conflicting code that no gate ever ran against.

- [ ] **Step 1: Enable up-to-date branches**

GitHub → Settings → Branches → branch protection rule for `develop` → check **"Require branches to be up to date before merging"**. Repeat for `main`.

- [ ] **Step 2: Record current required status check names**

Same screen, note the exact names under "Require status checks to pass". After this change: `Gate 4 · Build (debug)` and `Gate 2b · OWASP (non-blocking)` disappear, and `Gate 3 · Tests + ArchUnit + Coverage` is renamed. Any of those listed as required must be updated in Task 10, or merges block forever on a check that no longer exists.

- [ ] **Step 3: Create the branch**

```bash
git fetch origin
git switch -c chore/ci-cost-optimization origin/develop
```

---

## Task 1: `bump_version.py` + tests

**Files:**
- Create: `scripts/ci/bump_version.py`
- Test: `scripts/ci/test_bump_version.py`

**Interfaces:**
- Produces: CLI `python3 scripts/ci/bump_version.py --bump {major,minor,patch} --build-number N [--file PATH] [--dry-run]`. Prints the first active app's new version to stdout; appends `version=<v>` to `$GITHUB_OUTPUT` when set and not dry-run. Exit 1 on malformed input or zero active apps. Consumed by Task 5.

- [ ] **Step 1: Write the failing test**

```python
# scripts/ci/test_bump_version.py
"""Tests for bump_version.py. Pure stdlib: python3 -m unittest discover -s scripts/ci -p 'test_*.py'"""
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parent / "bump_version.py"

FIXTURE = {
    "apps": {
        "finance": {"version": "1.2.7", "buildNumber": 9},
        "tools": {"version": "0.0.0", "status": "planned"},
        "vault": {"version": "0.0.0", "status": "future"},
    },
    "web": {"finance": {"version": "0.1.0", "status": "in-development"}},
}


class BumpVersionTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.path = Path(self.tmp.name) / "versions.json"
        self.path.write_text(json.dumps(FIXTURE, indent=2), encoding="utf-8")

    def tearDown(self):
        self.tmp.cleanup()

    def run_script(self, *args):
        return subprocess.run(
            [sys.executable, str(SCRIPT), "--file", str(self.path), *args],
            capture_output=True, text=True,
        )

    def test_patch_bump(self):
        res = self.run_script("--bump", "patch", "--build-number", "10")
        self.assertEqual(res.returncode, 0, res.stderr)
        self.assertEqual(res.stdout.strip(), "1.2.8")

    def test_minor_bump_zeroes_patch(self):
        res = self.run_script("--bump", "minor", "--build-number", "10")
        self.assertEqual(res.stdout.strip(), "1.3.0")

    def test_major_bump_zeroes_minor_and_patch(self):
        res = self.run_script("--bump", "major", "--build-number", "10")
        self.assertEqual(res.stdout.strip(), "2.0.0")

    def test_build_number_written_and_planned_apps_untouched(self):
        self.run_script("--bump", "patch", "--build-number", "10")
        data = json.loads(self.path.read_text(encoding="utf-8"))
        self.assertEqual(data["apps"]["finance"]["buildNumber"], 10)
        self.assertEqual(data["apps"]["tools"]["version"], "0.0.0")
        self.assertNotIn("buildNumber", data["apps"]["vault"])

    def test_web_section_untouched(self):
        self.run_script("--bump", "major", "--build-number", "10")
        data = json.loads(self.path.read_text(encoding="utf-8"))
        self.assertEqual(data["web"]["finance"]["version"], "0.1.0")

    def test_dry_run_does_not_write(self):
        before = self.path.read_text(encoding="utf-8")
        res = self.run_script("--bump", "major", "--build-number", "99", "--dry-run")
        self.assertEqual(res.stdout.strip(), "2.0.0")
        self.assertEqual(self.path.read_text(encoding="utf-8"), before)

    def test_no_active_app_exits_nonzero(self):
        self.path.write_text(json.dumps({"apps": {"x": {"version": "1.0.0", "status": "planned"}}}), encoding="utf-8")
        res = self.run_script("--bump", "patch", "--build-number", "1")
        self.assertEqual(res.returncode, 1)

    def test_malformed_json_exits_nonzero(self):
        self.path.write_text("{not json", encoding="utf-8")
        res = self.run_script("--bump", "patch", "--build-number", "1")
        self.assertNotEqual(res.returncode, 0)

    def test_github_output_written(self):
        out = Path(self.tmp.name) / "gh_out"
        out.touch()
        env = {**os.environ, "GITHUB_OUTPUT": str(out)}
        subprocess.run(
            [sys.executable, str(SCRIPT), "--file", str(self.path), "--bump", "minor", "--build-number", "10"],
            capture_output=True, text=True, env=env,
        )
        self.assertIn("version=1.3.0", out.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python -m unittest discover -s scripts/ci -p "test_*.py" -v`
Expected: FAIL — `bump_version.py` does not exist (subprocess returns non-zero, `stdout` empty).

- [ ] **Step 3: Write the implementation**

```python
#!/usr/bin/env python3
"""Bump app versions in platform/versions.json (commit-type-driven, see ADR-0025).

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

- [ ] **Step 4: Run tests to verify they pass**

Run: `python -m unittest discover -s scripts/ci -p "test_*.py" -v`
Expected: PASS, 9 tests.

- [ ] **Step 5: Sanity-check against the real file**

Run: `python scripts/ci/bump_version.py --file platform/versions.json --build-number 15 --bump minor --dry-run`
Expected: stdout `2.1.0` (current finance version is `2.0.2`), and `git diff --stat` shows no change.

- [ ] **Step 6: Commit**

```bash
git add scripts/ci/bump_version.py scripts/ci/test_bump_version.py
git commit -m "ci: add commit-type-driven version bump script (ADR-0025)"
```

---

## Task 2: `detect_bump.sh` + tests

**Files:**
- Create: `scripts/ci/detect_bump.sh`
- Test: `scripts/ci/test_detect_bump.sh`

**Interfaces:**
- Produces: `scripts/ci/detect_bump.sh [ref_name] < commit-log-text` → prints `major`|`minor`|`patch`. When `ref_name` is `main`, always `patch` (spec D6). Consumed by Task 5.

- [ ] **Step 1: Write the failing test**

```bash
#!/usr/bin/env bash
# scripts/ci/test_detect_bump.sh — table-driven test of the §5.H detection matrix.
# Run: bash scripts/ci/test_detect_bump.sh
set -uo pipefail
SCRIPT="$(dirname "$0")/detect_bump.sh"
FAIL=0

check() {
  local desc="$1" ref="$2" expected="$3" log="$4"
  local actual
  actual=$(printf '%s' "$log" | bash "$SCRIPT" "$ref")
  if [ "$actual" = "$expected" ]; then
    echo "  ok   — $desc ($expected)"
  else
    echo "  FAIL — $desc: expected '$expected', got '$actual'"
    FAIL=1
  fi
}

echo "detect_bump.sh"
check "feat subject"              develop minor "feat: add networth screen"
check "feat with scope"           develop minor "feat(tracker): add assets"
check "fix subject"               develop patch "fix: rounding error"
check "chore subject"             develop patch "chore: deps"
check "docs subject"              develop patch "docs: spec"
check "refactor subject"          develop patch "refactor: extract mapper"
check "breaking bang"             develop major "feat!: drop api v1"
check "breaking bang with scope"  develop major "refactor(core)!: split module"
check "BREAKING CHANGE body"      develop major "fix: thing

BREAKING CHANGE: config format moved"
check "BREAKING-CHANGE hyphen"    develop major "fix: thing

BREAKING-CHANGE: config format moved"
check "merge commit alone"        develop patch "Merge pull request #16 from saikumarkalli/feat/x"
check "feat beats fix in range"   develop minor "fix: a
feat: b"
check "breaking beats feat"       develop major "feat: a
refactor!: b"
check "main forces patch"         main    patch "feat!: drop api v1"
check "empty log"                 develop patch ""
check "bang only mid-word"        develop patch "fix: resolve foo! in parser"

[ "$FAIL" -eq 0 ] && echo "ALL PASS" || echo "FAILURES"
exit "$FAIL"
```

- [ ] **Step 2: Run test to verify it fails**

Run: `bash scripts/ci/test_detect_bump.sh`
Expected: FAIL — every row errors, `detect_bump.sh` does not exist.

- [ ] **Step 3: Write the implementation**

```bash
#!/usr/bin/env bash
# scripts/ci/detect_bump.sh — derive the semver bump segment from commit messages.
#
# Reads the commit log (subjects + bodies) on STDIN and prints one of
# major | minor | patch to STDOUT. See ADR-0025 and the design spec §5.H.
#
#   feat: / feat(scope):            -> minor
#   any type!: / BREAKING[ -]CHANGE -> major
#   anything else (incl. merge commits, empty input) -> patch
#
# Highest wins across the whole range. Pushes to `main` are ALWAYS patch (D6):
# a develop -> main promotion replays develop's already-bumped feat commits, and
# re-detecting them would double-bump.
#
# Usage:  git log --format='%s%n%b' "$RANGE" | detect_bump.sh "$GITHUB_REF_NAME"
set -uo pipefail

REF="${1:-}"
LOG=$(cat)
BUMP=patch

printf '%s\n' "$LOG" | grep -Eq '^[a-zA-Z]+(\([^)]*\))?!:' && BUMP=major
printf '%s\n' "$LOG" | grep -Eq 'BREAKING[ -]CHANGE:'      && BUMP=major
if [ "$BUMP" != "major" ]; then
  printf '%s\n' "$LOG" | grep -Eq '^feat(\([^)]*\))?:' && BUMP=minor
fi

[ "$REF" = "main" ] && BUMP=patch

printf '%s\n' "$BUMP"
```

- [ ] **Step 4: Run test to verify it passes**

Run: `bash scripts/ci/test_detect_bump.sh`
Expected: every row `ok`, final line `ALL PASS`, exit 0.

- [ ] **Step 5: Commit**

```bash
git add scripts/ci/detect_bump.sh scripts/ci/test_detect_bump.sh
git commit -m "ci: add commit-type bump detection script + matrix tests"
```

---

## Task 3: OWASP → scheduled workflow

**Files:**
- Create: `.github/workflows/owasp-scheduled.yml`
- Modify: `.github/workflows/ci.yml` — delete the `owasp` job ([lines 115-159](../../../.github/workflows/ci.yml#L115-L159)), drop it from `pr-summary`'s `needs` and table

**Why first among the YAML tasks:** biggest single saving, zero risk. The job was `continue-on-error: true` with findings masked — it had no gate value to lose (spec D4).

- [ ] **Step 1: Create the scheduled workflow**

```yaml
# .github/workflows/owasp-scheduled.yml
name: OWASP Weekly

# Moved out of ci.yml (ADR-0026). It was warn-only with findings masked by
# continue-on-error, ran twice per merge (PR + push), and pulled a ~700 MB NVD
# update each time — pure cost on the merge path for zero gate value.
#
# Cadence is MONTHLY, not weekly, and that is deliberate: while
# `continue-on-error: true` masks every finding (the dependency-check plugin is
# still unwired — PRODUCTION_READINESS T11/M1), this scan produces no signal
# anyone acts on. A weekly masked scan is 52 × 30 min/year of pure cost. Monthly
# keeps the artifact available for manual review at ~1/4 the spend. Move it back
# to weekly in the same change that flips continue-on-error to false.
on:
  schedule:
    - cron: "0 3 1 * *"   # 1st of the month, 03:00 UTC
  workflow_dispatch:

permissions:
  contents: read

env:
  JAVA_VERSION: "21"
  GRADLE_OPTS: "-Dorg.gradle.jvmargs=-Xmx4g"

jobs:
  owasp:
    name: "OWASP dependency check (scheduled)"
    runs-on: ubuntu-latest
    timeout-minutes: 30
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: temurin

      - name: Gradle cache
        uses: gradle/actions/setup-gradle@v3
        with:
          cache-read-only: true

      - name: Compute NVD cache key (year-month)
        id: nvd
        run: echo "ym=$(date +%Y-%m)" >> "$GITHUB_OUTPUT"

      # Persist the OWASP NVD database across runs → incremental update only.
      - name: Cache NVD database
        uses: actions/cache@v4
        with:
          path: ~/.gradle/dependency-check-data
          key: nvd-${{ steps.nvd.outputs.ym }}
          restore-keys: |
            nvd-

      # Still warn-only until the dependency-check plugin is wired in build-logic
      # (PRODUCTION_READINESS.md M1 / T11). Flip continue-on-error to false then.
      - name: OWASP Dependency Check
        run: ./gradlew dependencyCheckAnalyze
        continue-on-error: true

      - name: Upload OWASP report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: owasp-report
          path: "**/build/reports/dependency-check-report.html"
          retention-days: 30
```

- [ ] **Step 2: Delete the `owasp` job from ci.yml**

Remove lines 115–159 — the whole block from the `# ── Gate 2b: OWASP dependency check (non-blocking, off critical path) ──────` comment through the `owasp-report` upload step. Also trim the now-stale second paragraph of the Gate 2 comment at [ci.yml:89-92](../../../.github/workflows/ci.yml#L89-L92) to:

```yaml
  # ── Gate 2: Security scan (GitLeaks) ──────────────────────────────────────
  # GitLeaks is the BLOCKING secret gate — fast, gates the build. OWASP now runs
  # on a schedule in owasp-scheduled.yml, off the merge path entirely (ADR-0026).
```

- [ ] **Step 3: Update `pr-summary`**

In `pr-summary`, change `needs:` ([ci.yml:277](../../../.github/workflows/ci.yml#L277)) from `[ static-analysis, security, owasp, tests, build ]` to `[ static-analysis, security, tests, build ]`. In the script, delete the `owasp: '${{ needs.owasp.result }}',` line from `results`, and replace the OWASP row in `rows` with a static note:

```javascript
              ['Gate 2b · OWASP', 'ℹ️', 'scheduled scan (owasp-scheduled.yml)'],
```

- [ ] **Step 4: Verify workflow syntax**

Run: `actionlint .github/workflows/*.yml` if installed. Otherwise verify no `needs.owasp` / `owasp:` references survive:

```bash
grep -n "owasp" .github/workflows/ci.yml
```
Expected: only the comment line mentioning `owasp-scheduled.yml`.

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/owasp-scheduled.yml .github/workflows/ci.yml
git commit -m "ci: move OWASP scan to a scheduled workflow, off the merge path"
```

---

## Task 4: `changes` gate job (added, no consumers yet)

**Files:**
- Modify: `.github/workflows/ci.yml` — new first job

**Interfaces:**
- Produces: job `changes` with output `code` = `'true'` | `'false'`. Consumed by Tasks 5, 6, 7.

Added with no consumers so this commit cannot break anything; Tasks 5–7 wire it up.

- [ ] **Step 1: Add the job**

Insert immediately after `jobs:` ([ci.yml:38](../../../.github/workflows/ci.yml#L38)), before `static-analysis`:

```yaml
  # ── Gate 0: Changed-path filter ───────────────────────────────────────────
  # Docs-only changes skip the build gates AND the release (ADR-0026). This is a
  # JOB, not trigger-level `paths-ignore`: a job skipped via `if:` reports as
  # "skipped", which branch protection counts as passing, whereas `paths-ignore`
  # never creates the check run at all and leaves required checks pending forever.
  #
  # Only `docs/**`, any nested `*/docs/**`, and `**/*.md` count as docs.
  # `platform/versions.json` and the feature-flag JSON are CODE — they feed the build.
  changes:
    name: "Gate 0 · Changed paths"
    runs-on: ubuntu-latest
    timeout-minutes: 5
    outputs:
      code: ${{ steps.filter.outputs.code }}
    steps:
      # NO CHECKOUT on purpose. This job runs on EVERY event — including the docs-only
      # ones it exists to make cheap — so a clone here would eat the saving it creates.
      # The compare API returns the changed-file list directly, in ~2s.
      - name: Detect non-docs changes
        id: filter
        env:
          GH_TOKEN: ${{ github.token }}
          PR_BASE: ${{ github.event.pull_request.base.sha }}
          PR_HEAD: ${{ github.event.pull_request.head.sha }}
          PUSH_BASE: ${{ github.event.before }}
        run: |
          if [ "$GITHUB_EVENT_NAME" = "pull_request" ]; then
            BASE="$PR_BASE"; HEAD="$PR_HEAD"
          else
            BASE="$PUSH_BASE"; HEAD="$GITHUB_SHA"
          fi

          FILES=""
          COUNT=0
          if [ -n "$BASE" ] && [ "$BASE" != "0000000000000000000000000000000000000000" ]; then
            FILES=$(gh api "repos/$GITHUB_REPOSITORY/compare/${BASE}...${HEAD}" \
                      --jq '.files[].filename' 2>/dev/null || echo "")
            COUNT=$(printf '%s\n' "$FILES" | grep -c . || true)
          fi

          # Fail safe 1: unresolvable range (new branch, force push, API error) → run everything.
          if [ -z "$FILES" ]; then
            echo "code=true" >> "$GITHUB_OUTPUT"
            echo "No resolvable file list — treating as code."
            exit 0
          fi

          # Fail safe 2: the compare API caps `.files` at 300 entries. At the cap the list may be
          # truncated and a code file could sit past the boundary — never skip on a list we
          # cannot trust.
          if [ "$COUNT" -ge 300 ]; then
            echo "code=true" >> "$GITHUB_OUTPUT"
            echo "File list hit the 300-entry API cap — treating as code."
            exit 0
          fi

          CODE=false
          while IFS= read -r f; do
            [ -z "$f" ] && continue
            case "$f" in
              docs/*|*/docs/*|*.md) ;;
              *) CODE=true; echo "code change: $f"; break ;;
            esac
          done <<< "$FILES"

          echo "code=$CODE" >> "$GITHUB_OUTPUT"
          echo "Changed files: $(printf '%s\n' "$FILES" | wc -l) · code=$CODE"
```

- [ ] **Step 2: Test the filter logic locally**

The shell body is testable outside Actions. Save the classifier to a scratch file and feed it fixtures:

```bash
classify() {
  CODE=false
  while IFS= read -r f; do
    [ -z "$f" ] && continue
    case "$f" in
      docs/*|*/docs/*|*.md) ;;
      *) CODE=true; break ;;
    esac
  done <<< "$1"
  echo "$CODE"
}
[ "$(classify 'docs/a.md')" = false ] && echo ok1
[ "$(classify 'apps/finance/docs/superpowers/plans/x.md')" = false ] && echo ok2
[ "$(classify 'README.md')" = false ] && echo ok3
[ "$(classify 'platform/versions.json')" = true ] && echo ok4
[ "$(classify 'platform/DECISIONS.md')" = false ] && echo ok5
[ "$(classify $'docs/a.md\nlibs/core/src/main/X.kt')" = true ] && echo ok6
[ "$(classify '.github/workflows/ci.yml')" = true ] && echo ok7
```
Expected: `ok1` … `ok7` all printed.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add changed-path gate job (docs-only short-circuit)"
```

---

## Task 5: Rework the `release` job

**Files:**
- Modify: `.github/workflows/ci.yml` — the `release` job ([lines 378-602](../../../.github/workflows/ci.yml#L378-L602))

**Interfaces:**
- Consumes: `changes.outputs.code` (Task 4), `scripts/ci/detect_bump.sh` (Task 2), `scripts/ci/bump_version.py` (Task 1).

Done before deleting the `build` job so `needs:` is never dangling.

- [ ] **Step 1: Rewire `needs`, `if`, and permissions**

Replace [ci.yml:382-389](../../../.github/workflows/ci.yml#L382-L389):

```yaml
    needs: [ changes ]
    # Skip on PRs, on docs-only pushes, and on commits made by this job itself.
    if: |
      github.event_name == 'push' &&
      (github.ref == 'refs/heads/develop' || github.ref == 'refs/heads/main') &&
      needs.changes.outputs.code == 'true' &&
      !contains(github.event.head_commit.message, '[skip ci]')
    permissions:
      contents: write  # push bump commit + tag, create GitHub Release
      actions: read    # download artifacts from the PR's CI run (cross-run fetch below)
```

`actions: read` is **required** — without it the cross-run `download-artifact` in Step 5 returns 403.

- [ ] **Step 2: Add the bump-type detection step**

Insert directly after the checkout step ([ci.yml:398](../../../.github/workflows/ci.yml#L398)), before `Increment VERSION_CODE`:

```yaml
      # ── 0. Derive the semver segment from the commit types in this push ──
      # feat: -> minor, type!:/BREAKING CHANGE -> major, else patch; main = always
      # patch (D6). Checkout above already used fetch-depth: 0, so the full range
      # is available. Logic lives in a script so it is testable locally (ADR-0025).
      - name: Determine bump type
        id: bumptype
        run: |
          BASE="${{ github.event.before }}"
          if [ "$BASE" = "0000000000000000000000000000000000000000" ] \
             || ! git cat-file -e "${BASE}^{commit}" 2>/dev/null; then
            RANGE="${GITHUB_SHA}~1..${GITHUB_SHA}"
          else
            RANGE="${BASE}..${GITHUB_SHA}"
          fi
          BUMP=$(git log --format='%s%n%b' "$RANGE" | bash scripts/ci/detect_bump.sh "$GITHUB_REF_NAME")
          echo "bump=$BUMP" >> "$GITHUB_OUTPUT"
          echo "Range $RANGE → bump=$BUMP"
```

- [ ] **Step 3: Replace the inline-Python version step**

Replace the whole `Bump patch version and buildNumber in platform/versions.json` step ([ci.yml:417-448](../../../.github/workflows/ci.yml#L417-L448)) with:

```yaml
      - name: Bump version and buildNumber in platform/versions.json
        id: version_json
        run: |
          python3 scripts/ci/bump_version.py \
            --bump "${{ steps.bumptype.outputs.bump }}" \
            --build-number "${{ steps.bump.outputs.new }}"
```

The script writes `version=<v>` to `$GITHUB_OUTPUT` itself, so `steps.version_json.outputs.version` keeps working for every downstream step unchanged.

- [ ] **Step 4: Update the auto-bump commit message**

At [ci.yml:528](../../../.github/workflows/ci.yml#L528), include the bump type:

```yaml
          git commit -m "chore: auto-bump (${{ steps.bumptype.outputs.bump }}) to v${{ steps.version_json.outputs.version }} (versionCode=${{ steps.bump.outputs.new }}) [skip ci]"
```

- [ ] **Step 5: Write `.env` from secrets before the release build (CRITICAL — fixes a shipped bug)**

> **This is not part of the cost work — it is a live defect in the artifact this job publishes.** It lands here because Task 5 already rewrites this job, and shipping a cost optimization for a release that does not function would be backwards.

`apps/finance/app/build.gradle.kts` configures the secrets plugin as `propertiesFileName = ".env"` with `defaultPropertiesFileName = ".env.example"`. No workflow has ever created a `.env`, so **every published APK has been built from `.env.example`** and carries the literal placeholders `MY_SUPABASE_URL`, `MY_SUPABASE_ANON_KEY`, `MY_GEMINI_API_KEY`, `MY_GOOGLE_WEB_CLIENT_ID` — dead tracker, dead sign-in, dead AI. ADR-0014 §6 and ADR-0029 both state the release job writes `.env` from GitHub secrets; it does not.

Insert immediately **before** `Assemble signed release APK`:

```yaml
      # The secrets plugin reads .env and silently falls back to .env.example, so a missing
      # .env does not fail the build — it produces a signed APK full of MY_* placeholders.
      # Written here, never committed (.env is gitignored), and the runner is ephemeral.
      # No value is echoed: writing them into a file keeps them out of the step log.
      - name: Write .env from secrets
        env:
          GEMINI_API_KEY: ${{ secrets.GEMINI_API_KEY }}
          SUPABASE_URL: ${{ secrets.SUPABASE_URL }}
          SUPABASE_ANON_KEY: ${{ secrets.SUPABASE_ANON_KEY }}
          GOOGLE_WEB_CLIENT_ID: ${{ secrets.GOOGLE_WEB_CLIENT_ID }}
        run: |
          MISSING=""
          for name in GEMINI_API_KEY SUPABASE_URL SUPABASE_ANON_KEY GOOGLE_WEB_CLIENT_ID; do
            eval "value=\${$name}"
            [ -z "$value" ] && MISSING="$MISSING $name"
          done
          if [ -n "$MISSING" ]; then
            echo "❌ Release secrets missing:$MISSING" >&2
            echo "Set them in Settings → Secrets and variables → Actions." >&2
            exit 1
          fi
          {
            printf 'GEMINI_API_KEY=%s\n'       "$GEMINI_API_KEY"
            printf 'SUPABASE_URL=%s\n'         "$SUPABASE_URL"
            printf 'SUPABASE_ANON_KEY=%s\n'    "$SUPABASE_ANON_KEY"
            printf 'GOOGLE_WEB_CLIENT_ID=%s\n' "$GOOGLE_WEB_CLIENT_ID"
          } > .env
          echo "✅ .env written ($(wc -l < .env) keys)"
```

Failing hard on a missing secret is the point: a warn-and-continue here reproduces the exact bug, just louder.

- [ ] **Step 6: Guard against placeholders reaching a published APK**

The check that would have caught this in the first place. Add to the `Locate and verify signed APK` step, after the size check:

```bash
          # The secrets-plugin fallback is silent, so verify the artifact itself rather than
          # trusting the build. `strings` over the APK finds the baked BuildConfig constants.
          if strings "$DEST" | grep -qE 'MY_(SUPABASE_URL|SUPABASE_ANON_KEY|GEMINI_API_KEY|GOOGLE_WEB_CLIENT_ID)'; then
            echo "❌ APK contains .env.example placeholders — real secrets were not applied." >&2
            exit 1
          fi
          echo "✅ No placeholder secrets in the APK"
```

Aborts before any tag or Release is created, so a broken artifact is never published.

- [ ] **Step 7: Add the size *delta* check PLATFORM.md §11 already claims**

§11 lists "size delta check" as part of Gate 4; only an absolute 50 MB cap exists, so a regression from 12 MB to 45 MB passes silently. The previous release's asset size is the baseline — no state file needed.

Add after the placeholder guard, in the same step:

```bash
          # Compare against the previous release's APK. Informational on the first release
          # (no prior asset) and never fatal on a lookup failure — a GitHub API hiccup must
          # not block a release — but a >20% jump fails, because that is a real regression.
          PREV_BYTES=$(gh release list --repo "$GITHUB_REPOSITORY" --limit 1 --json tagName \
                         --jq '.[0].tagName // empty' 2>/dev/null \
                       | xargs -r -I{} gh release view {} --repo "$GITHUB_REPOSITORY" \
                         --json assets --jq '[.assets[] | select(.name|endswith(".apk")) | .size][0] // empty' \
                         2>/dev/null || echo "")
          NEW_BYTES=$(stat -c %s "$DEST")
          if [ -n "$PREV_BYTES" ] && [ "$PREV_BYTES" -gt 0 ]; then
            DELTA=$(( (NEW_BYTES - PREV_BYTES) * 100 / PREV_BYTES ))
            echo "APK size: $((NEW_BYTES/1048576)) MB (previous $((PREV_BYTES/1048576)) MB, ${DELTA}%)"
            if [ "$DELTA" -gt 20 ]; then
              echo "❌ APK grew ${DELTA}% over the previous release (budget: 20%)." >&2
              exit 1
            fi
          else
            echo "No previous release asset — size delta baseline established at $((NEW_BYTES/1048576)) MB"
          fi
```

Requires `GH_TOKEN: ${{ github.token }}` in that step's `env:`.

- [ ] **Step 8: Replace same-run artifact downloads with a cross-run fetch**

The `tests` job no longer runs on push, so the `regression-summary` / `coverage-report` artifacts live in the **PR's** run. Replace the two `download-artifact` steps ([ci.yml:557-569](../../../.github/workflows/ci.yml#L557-L569)) with:

```yaml
      # The tests job no longer runs on push (ADR-0026), so the regression summary
      # and coverage report live in the PR's CI run. Find it and pull them across.
      # Entirely best-effort: release notes degrade to the APK line if anything fails.
      - name: Locate the PR's CI run
        id: prrun
        continue-on-error: true
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          PR=$(gh api "repos/$GITHUB_REPOSITORY/commits/$GITHUB_SHA/pulls" \
                 --jq '.[0].number // empty' 2>/dev/null || echo "")
          if [ -z "$PR" ]; then
            echo "No PR associated with $GITHUB_SHA — skipping artifact fetch."
            exit 0
          fi
          HEAD_SHA=$(gh pr view "$PR" --repo "$GITHUB_REPOSITORY" --json headRefOid --jq .headRefOid)
          RUN=$(gh run list --repo "$GITHUB_REPOSITORY" --workflow ci.yml \
                  --event pull_request --status success --limit 50 \
                  --json databaseId,headSha \
                  --jq "[.[] | select(.headSha==\"$HEAD_SHA\")][0].databaseId // empty")
          echo "run_id=$RUN" >> "$GITHUB_OUTPUT"
          echo "PR #$PR head=$HEAD_SHA run=$RUN"

      - name: Download regression summary (from PR run)
        if: steps.prrun.outputs.run_id != ''
        uses: actions/download-artifact@v4
        continue-on-error: true
        with:
          name: regression-summary
          path: .ci-summary
          run-id: ${{ steps.prrun.outputs.run_id }}
          github-token: ${{ github.token }}

      - name: Download coverage report (from PR run)
        if: steps.prrun.outputs.run_id != ''
        uses: actions/download-artifact@v4
        continue-on-error: true
        with:
          name: coverage-report
          path: .ci-coverage
          run-id: ${{ steps.prrun.outputs.run_id }}
          github-token: ${{ github.token }}
```

- [ ] **Step 9: Make `release` the Gradle cache writer**

`tests` no longer runs on develop pushes, so nothing would populate the default-branch cache that every PR reads from. Change the release job's cache step ([ci.yml:471-474](../../../.github/workflows/ci.yml#L471-L474)):

```yaml
      # Cache WRITER: tests no longer runs on push, so this is the only job on the
      # default branch that can populate the shared Gradle cache PRs read from.
      - name: Gradle cache
        uses: gradle/actions/setup-gradle@v3
        with:
          cache-read-only: false
```

- [ ] **Step 10: Verify no stale references**

```bash
grep -n "needs.build\|needs: \[ build \]\|version_json.*python3 - <<" .github/workflows/ci.yml
grep -n "Write .env from secrets\|MY_(SUPABASE\|PREV_BYTES" .github/workflows/ci.yml
```
Expected: only `pr-summary`'s `needs: [ ..., build ]` (removed in Task 6), no inline python heredoc, and all three of the `.env` write / placeholder guard / size-delta steps present.

**Local dry-run of the placeholder guard** — prove it catches the current bug before trusting it in CI. Build a release APK with no `.env` present and confirm the guard fires:

```bash
mv .env .env.bak 2>/dev/null || true
./gradlew :apps:finance:app:assembleRelease
strings apps/finance/app/build/outputs/apk/release/*.apk | grep -c 'MY_SUPABASE_URL'
mv .env.bak .env 2>/dev/null || true
```
Expected: a non-zero count — that is the shipped bug, reproduced. After Step 5 lands in CI the same grep against a CI-built APK must return 0.

- [ ] **Step 11: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: release job runs off the changes gate with commit-type bump, real secrets, size delta"
```

---

## Task 6: Fold Gate 4 into the tests job

**Files:**
- Modify: `.github/workflows/ci.yml` — `tests` job, delete `build` job ([lines 233-266](../../../.github/workflows/ci.yml#L233-L266)), `pr-summary`

The `build` job re-compiled `assembleDebug` on a **fresh cold runner** after `tests` had already compiled the entire project via `regressionCheck`. Folding it in reuses the warm daemon and build cache — minutes instead of a full cold build.

- [ ] **Step 1: Rename the tests job**

[ci.yml:163](../../../.github/workflows/ci.yml#L163):

```yaml
    name: "Gate 3+4 · Tests + ArchUnit + Coverage + Build"
```

Also update the cache comment at [ci.yml:182-184](../../../.github/workflows/ci.yml#L182-L184) — it currently says it populates the cache "so the build job reuses those compile/KSP outputs", which is no longer true:

```yaml
      # Warm the Gradle build cache for subsequent pushes to this same PR: this job
      # compiles the whole project once (regressionCheck) and assembles the debug
      # APK below on the same warm daemon.
```

- [ ] **Step 2: Add the assemble + upload steps to `tests`**

Append after the `Upload regression summary` step ([ci.yml:231](../../../.github/workflows/ci.yml#L231)):

```yaml
      # Gate 4, folded in (ADR-0026): regressionCheck above already compiled every
      # module, so this reuses the warm daemon + build cache instead of paying for a
      # fresh runner with a cold cache restore. The signed RELEASE build is NOT here —
      # it is built in the `release` job after the version bump.
      - name: Assemble debug APK (Gate 4)
        run: ./gradlew :apps:finance:app:assembleDebug

      - name: Upload debug APK
        uses: actions/upload-artifact@v4
        with:
          name: debug-apk
          path: apps/finance/app/build/outputs/apk/debug/*.apk
          if-no-files-found: warn
          retention-days: 7
```

- [ ] **Step 3: Delete the `build` job**

Remove lines 233–266 entirely — the `# ── Gate 4: Build (debug) ──` comment block through the `debug-apk` upload step.

- [ ] **Step 4: Update `pr-summary`**

`needs:` becomes `[ static-analysis, security, tests ]`. Delete `build: '${{ needs.build.result }}',` from `results`, delete the `Gate 4 · Build (debug)` row from `rows`, and rename the tests row:

```javascript
              ['Gate 3+4 · Tests + ArchUnit + Coverage + Build', icons[results.tests] ?? '❔', results.tests],
```

- [ ] **Step 5: Verify**

```bash
grep -n "needs.build\|Gate 4 · Build" .github/workflows/ci.yml
```
Expected: no matches.

- [ ] **Step 6: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: fold debug build into the tests job, delete the build gate"
```

---

## Task 7: PR-only conditions on the gates

**Files:**
- Modify: `.github/workflows/ci.yml` — `static-analysis`, `security`, `tests`

This is the change that removes the identical full re-run on the merge push (audit finding #1, ~half the total minutes). It is safe **only** because Task 0 enabled up-to-date branches: the merged tree is byte-identical to the PR-validated tree.

- [ ] **Step 1: Gate `static-analysis`**

After `runs-on: ubuntu-latest` in `static-analysis`:

```yaml
    needs: [ changes ]
    # PR is the ONLY full-validation pass (ADR-0026). The merge push re-runs nothing:
    # required up-to-date branches guarantee the merged tree is the tree the PR validated.
    if: github.event_name == 'pull_request' && needs.changes.outputs.code == 'true'
    timeout-minutes: 20
```

(`static-analysis` currently has no `timeout-minutes` — add it, every other job has one.)

- [ ] **Step 2: Gate `security` (GitLeaks) — PR only, but NOT docs-gated**

```yaml
    needs: [ changes ]
    # No changes-gate here on purpose: secrets hide in markdown too, so GitLeaks
    # runs on docs-only PRs as well.
    if: github.event_name == 'pull_request'
```

- [ ] **Step 3: Gate `tests`**

```yaml
    needs: [ changes ]
    if: github.event_name == 'pull_request' && needs.changes.outputs.code == 'true'
```

- [ ] **Step 4: Verify the event matrix by inspection**

```bash
grep -n "if: github.event_name\|needs: \[ changes \]\|^  [a-z-]*:$" .github/workflows/ci.yml
```
Expected job set and conditions:

| Job | PR (code) | PR (docs-only) | Push develop (code) | Push develop (docs-only) |
|---|---|---|---|---|
| `changes` | run | run | run | run |
| `static-analysis` | run | skip | skip | skip |
| `security` | run | run | skip | skip |
| `tests` (+build) | run | skip | skip | skip |
| `pr-summary` | run | run | skip | skip |
| `release` | skip | skip | **run** | skip |

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: run validation gates on PRs only; merge push builds the release"
```

---

## Task 8: fast-feedback dedupe

**Files:**
- Modify: `.github/workflows/fast-feedback.yml`

Once a PR is open, both `fast-feedback` and `ci.yml`'s `pull_request` run compile and test the same commit. Removes ~10–15 min duplicated per push during review.

- [ ] **Step 1: Add the `pr-check` job**

Insert as the first job, before `compile-and-test`:

```yaml
jobs:
  # Once a PR is open for this branch, ci.yml's pull_request run is the authoritative
  # build — running fast-feedback too would compile and test the same commit twice
  # (ADR-0026). This job costs seconds and gates the expensive one below.
  pr-check:
    name: "Open PR check"
    runs-on: ubuntu-latest
    timeout-minutes: 5
    outputs:
      open: ${{ steps.pr.outputs.open }}
    steps:
      - id: pr
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          COUNT=$(gh pr list --repo "$GITHUB_REPOSITORY" --head "$GITHUB_REF_NAME" \
                  --state open --json number --jq 'length' 2>/dev/null || echo "0")
          echo "open=$COUNT" >> "$GITHUB_OUTPUT"
          echo "Open PRs for $GITHUB_REF_NAME: $COUNT"
```

`gh pr list` needs read access to pull requests, which the workflow's current `contents: read` does not grant. Widen the workflow-level `permissions` block:

```yaml
permissions:
  contents: read
  pull-requests: read
```

- [ ] **Step 2: Gate the expensive job**

On `compile-and-test`:

```yaml
    needs: pr-check
    if: needs.pr-check.outputs.open == '0'
```

- [ ] **Step 3: Verify**

```bash
grep -n "pr-check\|needs.pr-check\|pull-requests: read" .github/workflows/fast-feedback.yml
```
Expected: job definition, the `needs`/`if` pair, and the permission line.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/fast-feedback.yml
git commit -m "ci: skip fast-feedback once a PR is open for the branch"
```

---

## Task 9: Runner tuning, artifact retention, ktlint cache

**Files:**
- Modify: `.github/workflows/ci.yml`, `.github/workflows/fast-feedback.yml`

- [ ] **Step 1: Right-size Gradle for the CI runner**

`gradle.properties` sets `org.gradle.workers.max=4`, which is correct for a dev machine. GitHub-hosted **standard** runners on a private repo are **2-core / 7 GB** — four workers on two cores adds context-switching, not throughput, and every extra second is billed. Override in CI only (leave `gradle.properties` alone so local builds keep all four).

Replace the `env:` block in **both** `ci.yml` and `fast-feedback.yml`:

```yaml
env:
  JAVA_VERSION: "21"
  # gradle.properties keeps workers.max=4 for dev machines. GitHub-hosted standard
  # runners on a private repo are 2-core/7 GB, so four workers just thrash. Overridden
  # here (CI only) — the daemon and configuration cache settings stay as configured.
  GRADLE_OPTS: "-Dorg.gradle.jvmargs=-Xmx4g -Dorg.gradle.workers.max=2"
```

**Consider (measure first, do not apply blind):** the Kotlin compile daemon is a second JVM (~1 GB) on a 7 GB runner. `-Pkotlin.compiler.execution.strategy=in-process` appended to the `./gradlew` invocations avoids it. This can go either way — in-process compilation loses daemon warm-up reuse across tasks in the same job. Time one `regressionCheck` run with and without before adopting; do not bundle it into this PR unless the measurement is positive.

- [ ] **Step 2: Trim the release job's clone**

The `release` job checkout uses `fetch-depth: 0` + `fetch-tags: true` — it needs full history (commit range for bump detection) and tags (idempotent tagging), but it does **not** need every historical blob. `actions/checkout` v4.2+ supports partial clone:

```yaml
      - uses: actions/checkout@v4
        with:
          ref: ${{ github.ref }}
          fetch-depth: 0
          fetch-tags: true
          filter: blob:none
          token: ${{ secrets.GITHUB_TOKEN }}
```

`blob:none` fetches commits and trees but downloads file contents lazily — history and tags stay complete, so `git log` and `git rev-parse` behave identically. Blobs the build actually reads are fetched on checkout of the working tree.

Artifacts default to **90 days** of billed storage. Nothing here is worth keeping that long.

- [ ] **Step 3: Set retention on every upload that lacks it**

| Artifact | Job | Add |
|---|---|---|
| `lint-reports` | `static-analysis` | `retention-days: 7` |
| `test-reports` | `tests` | `retention-days: 7` |
| `coverage-report` | `tests` | `retention-days: 14` |
| `regression-summary` | `tests` | `retention-days: 7` |

(`debug-apk` already has 7 from Task 6; `fast-feedback-test-reports` already has 3; `owasp-report` got 30 in Task 3.)

Example, on the `lint-reports` upload:

```yaml
      - name: Upload lint reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: lint-reports
          retention-days: 7
          path: |
            **/build/reports/lint-results*.html
            **/build/reports/detekt/*.html
            build/reports/ktlint/ktlint.xml
```

- [ ] **Step 4: Cache the ktlint binary**

Replace the download step ([ci.yml:57-61](../../../.github/workflows/ci.yml#L57-L61)) with a cache + conditional download:

```yaml
      # ktlint binary is downloaded directly (no third-party action dependency) and
      # cached by version so it is fetched once, not on every run.
      - name: Cache ktlint
        id: ktlint-cache
        uses: actions/cache@v4
        with:
          path: ktlint
          key: ktlint-1.5.0

      - name: Download ktlint
        if: steps.ktlint-cache.outputs.cache-hit != 'true'
        run: |
          curl -sSLO https://github.com/pinterest/ktlint/releases/download/1.5.0/ktlint
          chmod +x ktlint

      - name: Ensure ktlint is executable
        run: chmod +x ktlint
```

The extra `chmod` step is needed because `actions/cache` does not preserve the executable bit on restore.

- [ ] **Step 5: Verify**

```bash
grep -c "retention-days" .github/workflows/ci.yml
grep -n "workers.max=2" .github/workflows/ci.yml .github/workflows/fast-feedback.yml
grep -n "filter: blob:none" .github/workflows/ci.yml
```
Expected: 5 retention entries (lint-reports, test-reports, coverage-report, regression-summary, debug-apk); `workers.max=2` in both workflows; one `filter: blob:none`.

- [ ] **Step 6: Commit**

```bash
git add .github/workflows/ci.yml .github/workflows/fast-feedback.yml
git commit -m "ci: right-size Gradle workers, trim release clone, bound artifact retention"
```

---

## Task 10: Documentation

**Files:**
- Modify: `platform/DECISIONS.md`, `platform/PLATFORM.md`, `platform/versions.json`

- [ ] **Step 1: Append ADR-0025 to `platform/DECISIONS.md`**

Append at the end of the file (the register is append-only):

```markdown
---

## ADR-0025 — Commit-type-driven semver bump (amends ADR-0011)
**Context.** ADR-0011 made CI auto-increment the PATCH segment on every merge, leaving MINOR and
MAJOR as manual edits to `platform/versions.json`. In practice that meant hand-editing the file
before merging a feature branch (e.g. 1.2.x → 1.3.0 for the networth work) — a step that is easy
to forget and produces a wrong version silently when forgotten.
**Decision.** CI derives the segment from the commit types in the push range:
`feat:` / `feat(scope):` → **minor**; any `type!:` or a `BREAKING CHANGE:` / `BREAKING-CHANGE:`
trailer → **major**; everything else (including bare merge commits) → **patch**. Highest wins
across the range. Pushes to `main` are **always patch** — a `develop → main` promotion replays
develop's already-bumped `feat:` commits, and re-detecting them would double-bump.
Detection lives in `scripts/ci/detect_bump.sh` (stdin → segment) and the file rewrite in
`scripts/ci/bump_version.py`, both with local tests, rather than in an inline YAML heredoc.
**Why.** Removes the whole class of "forgot to bump minor" errors, matches the conventional-commit
messages the repo already writes, and keeps ADR-0011's semantics (PATCH = fix/merge) intact.
Scripts over inline YAML follows the `scripts/ci/regression_summary.py` precedent — testable
locally with `--dry-run` instead of only observable after a merge.
**Consequences.** Manual minor/major edits to `versions.json` are no longer needed and are
**discouraged**: a manually raised version still works as a new baseline, but if the same merge
also carries `feat:` commits the result is a double bump. Any branch holding such a manual edit
must revert it before merging. `VERSION_CODE` increment, `VERSION_NAME` sync, APK verification,
idempotent tagging and Release publishing are unchanged.
```

- [ ] **Step 2: Append ADR-0026**

```markdown
---

## ADR-0026 — CI cost model: single-validation pipeline
**Context.** An audit of the three workflows found no trigger loops but heavy duplicate work —
roughly 2–3 GitHub-hosted runner-hours per merged PR on a private repo with a 2000 min/month floor,
i.e. ~11–16 merges before exhaustion. Five distinct duplications: (1) a full 4-gate run on the PR
and an identical re-run on the merge push over the same tree; (2) `fast-feedback` compiling the
same commit `ci.yml`'s PR run already compiles, once a PR is open; (3) the `build` job re-compiling
`assembleDebug` on a fresh cold runner after `tests` had already compiled everything via
`regressionCheck`; (4) OWASP running twice per merge with a ~700 MB NVD update, up to 30 min, and
`continue-on-error` masking every finding — paying full price for zero gate value; (5) docs-only
commits triggering full builds plus a version bump, APK and Release.
**Decision.** The PR is the **only** full-validation pass. `static-analysis` and `tests` run on
`pull_request` only; `security` (GitLeaks) runs on every PR including docs-only ones, because
secrets hide in markdown too. The merge push runs the `release` job only. `build` is deleted and
its `assembleDebug` folded into `tests` on the warm daemon. OWASP moves to `owasp-scheduled.yml`
(monthly cron + `workflow_dispatch`). A `changes` gate job short-circuits docs-only work. The
Gradle cache **writer** moves from `tests` to `release`, since `tests` no longer runs on the
default branch. Release notes fetch the regression summary and coverage artifacts **cross-run**
from the PR's successful CI run, best-effort.
**Why.** Safe because branch protection now requires up-to-date branches (see Consequences): the
merged tree is byte-identical to the tree the PR validated, so re-running the gates on push
verifies nothing new. The `release` job's own `assembleRelease` still catches compile-level
breakage. Docs-only skipping uses a job-level `if:` rather than trigger-level `paths-ignore`
because a skipped job reports as skipped — which branch protection counts as passing — whereas
`paths-ignore` never creates the check run and leaves required checks permanently pending.
**Consequences.** *"Require branches to be up to date before merging"* on `develop` and `main` is
now a **load-bearing repo setting**, not a preference — disabling it silently removes the only
thing validating merged code. Required-status-check names changed: `Gate 4 · Build (debug)` and
`Gate 2b · OWASP (non-blocking)` no longer exist, and Gate 3 is renamed
`Gate 3+4 · Tests + ArchUnit + Coverage + Build`. The `release` job needs `actions: read` to pull
artifacts across runs; if the lookup fails, release notes degrade to the APK line and the release
still publishes. ADR-0013's "coverage visible on every merge" promise is kept via the PR run's
artifacts rather than a re-run. `pr-summary` remains informational-only (ADR-0012); its OWASP row
becomes a static pointer to the scheduled workflow. Cost is now **measured, not assumed**:
`scripts/ci/actions_usage.py` reports billed minutes per pipeline from the Actions timing API, a
monthly `ci-usage-report.yml` posts it to a Job Summary, and four standing budgets bound future
growth — **≤ 90 billed min per merged PR**, **≤ 70 %** of that in the commit pipeline, **≤ 1600 min**
projected monthly (80 % of the Free-tier cap), and **≤ +4 min** on `regressionCheck` per new module.
Exceeding one is a decision to take deliberately, with the remedy named in the plan's cost-budget
table; test sharding is the first lever and is deliberately unbuilt until the measurement calls for
it. The cadence of the OWASP scan is monthly **only while its findings are masked** — restore weekly
in the same change that flips `continue-on-error` to false.
```

- [ ] **Step 3: Update `platform/PLATFORM.md` §11**

In the four-gate list, note when each runs, and rewrite the post-build paragraph:

- Gate 1 (static analysis), Gate 3 (tests + build) — **runs on: PR only**
- Gate 2 (GitLeaks) — **runs on: every PR, including docs-only**
- OWASP — **scheduled cron (`owasp-scheduled.yml`)**, no longer a per-merge gate
- Merge push to `develop`/`main` — **`changes` + `release` only**
- `version-bump` bullet: replace "atomically increments `MAJOR.MINOR.PATCH+1`" with "derives the segment from commit types (`feat:` → minor, `type!:`/`BREAKING CHANGE` → major, else patch; `main` always patch — ADR-0025)"

- [ ] **Step 4: Update `platform/PLATFORM.md` §12**

Replace the first two bullets with:

```markdown
- The **segment is chosen by CI from the commit types** in the merge range: `feat:` → MINOR,
  `type!:` / `BREAKING CHANGE:` → MAJOR, anything else → PATCH. Pushes to `main` are always PATCH.
  See ADR-0025. No manual version edits are needed — or wanted.
```

Delete the "Minor/Major are bumped manually in `platform/versions.json`" sentence.

- [ ] **Step 5: Update `platform/versions.json` notes**

```json
  "notes": "All three version segments (major.minor.patch) and versionCode are CI-owned (ADR-0025) — do not edit by hand. Web version is bumped manually in web/package.json. Update requiresCore when a breaking core change lands."
```

- [ ] **Step 6: Update branch-protection required checks**

Back in GitHub Settings → Branches, using the names recorded in Task 0 Step 2: remove
`Gate 4 · Build (debug)` and `Gate 2b · OWASP (non-blocking)` if listed; rename the Gate 3 entry to
`Gate 3+4 · Tests + ArchUnit + Coverage + Build`. **Do this before merging** — a required check
that no longer exists blocks merges permanently.

- [ ] **Step 7: Commit**

```bash
git add platform/DECISIONS.md platform/PLATFORM.md platform/versions.json
git commit -m "docs: ADR-0025 commit-type bump, ADR-0026 single-validation CI"
```

---

# Part 2 — Infrastructure cost beyond GitHub Actions

Tasks 0–10 address the only line item that currently costs money. This part covers the rest of the estate, audited 2026-08-14. **Most of it needs no change, and that is stated explicitly** so it does not get re-litigated later.

## Cost model as audited

| Layer | Tier | Current cost | Real risk | Action |
|---|---|---|---|---|
| GitHub Actions minutes | private repo, 2000 min/mo free | **the entire bill** | ~11–16 merges/month exhausts it | Tasks 0–10 |
| Actions artifact storage | 500 MB free (private) | near zero after Task 9 | 90-day default retention accumulates | Task 9 Step 3 |
| Actions cache | 10 GB/repo, free | none today | NVD (~700 MB) + Gradle competing for the cap → LRU evicts the Gradle cache PRs depend on, costing cold builds | Task 11 (monitor only) |
| GitHub Releases (APK assets) | free | ₹0 | none — release assets are not billed like artifacts/packages | none |
| Supabase | Free | ₹0 | **project pauses after ~7 days inactivity** → tracker dead until manually resumed | Task 12 |
| Supabase DB size | 500 MB free | ₹0 | append-only `valuations` grows forever — but rows are ~100 bytes, so 500 MB ≈ 5M valuations. Not a real ceiling for a personal tracker | **none — do not build a rollup** |
| Firebase (Crashlytics / Perf / Remote Config) | Spark free | ₹0, and currently inert | unlimited at this scale; Remote Config already rate-limits fetches to 12h | none |
| Gemini | Worker proxy + BYO key (ADR-0002) | ₹0 | proxy not built yet | out of scope |
| Currency API (`open.er-api.com`) | free, updates once daily | ₹0 | **fetched on every screen open, no TTL** — burns free-tier request quota, mobile data and battery for data that changes daily | Task 13 |
| Vercel (web) | Hobby | ₹0 | none at current traffic | none |
| Dev environment | Android Studio, JBR, emulator, Docker | ₹0 | none | Task 14 (guidance only) |

**Deliberately not done, with reasons:**

- **Remote Gradle build cache.** Would speed local cold builds, but hosting one costs money to save a solo developer's laptop time. Net negative.
- **Larger GitHub runners.** Bill per-minute at a multiplier; a 2× faster 4-core runner at 2× the rate is cost-neutral at best and this build is not CPU-bound end to end.
- **Batching releases** (build the APK only on tagged/labelled merges instead of every merge). Would remove the last ~10–15 min of per-merge cost, but it breaks the "every merge produces a versioned Release" promise (ADR-0011, ADR-0013). Listed as an option, **not recommended** — raise a new ADR if the merge rate ever makes it worth it.
- **A `valuations` rollup / retention job.** YAGNI at 5M-row headroom, and it would fight the append-only design (ADR-0014 §5).

---

## Task 11: Actions cache hygiene (monitoring, not a change)

**Files:** none — a check to run, plus a note in the OWASP workflow.

GitHub gives 10 GB of Actions cache per repo and evicts least-recently-used entries when full. This repo caches the Gradle build cache (the thing PRs depend on for warm builds), the NVD database (~700 MB, keyed `nvd-<year-month>` so old months linger), and now ktlint. If the NVD entries ever push the total past 10 GB, the victim is likely the Gradle cache — turning every PR into a cold build, which costs far more than the NVD download saves.

Not a problem today. Worth knowing how to check.

- [ ] **Step 1: Measure current cache usage**

```bash
gh cache list --repo <owner>/dhruv --limit 100 --json key,sizeInBytes,lastAccessedAt \
  --jq 'sort_by(-.sizeInBytes) | .[] | "\(.sizeInBytes/1048576|floor) MB  \(.key)"'
```

Expected today: comfortably under 10 GB total.

- [ ] **Step 2: If total exceeds ~8 GB, drop stale NVD entries**

```bash
gh cache list --repo <owner>/dhruv --json key --jq '.[].key' \
  | grep '^nvd-' | sort | head -n -1 \
  | xargs -r -I{} gh cache delete {} --repo <owner>/dhruv
```

Keeps the newest `nvd-` entry, deletes the rest. Safe: `restore-keys: nvd-` means a miss costs one slower monthly scan, off the critical path.

- [ ] **Step 3: Record the threshold in `owasp-scheduled.yml`**

Add above the NVD cache step:

```yaml
      # The NVD DB is ~700 MB and shares the repo's 10 GB Actions cache with the Gradle
      # build cache that every PR depends on. If `gh cache list` ever totals >8 GB, delete
      # stale nvd-* entries first — losing the Gradle cache costs far more than one slow scan.
```

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/owasp-scheduled.yml
git commit -m "ci: document Actions cache budget for the NVD database"
```

---

## Task 12: Supabase keep-alive (prod availability)

**Files:**
- Create: `.github/workflows/supabase-keepalive.yml`

Supabase free projects **pause after ~7 days without API activity**. A paused project fails every tracker request, and the app has no "paused" state — the user sees a generic error. An authenticated request every 5 days keeps it live for roughly **9 runner-minutes per year**, which is the cheapest possible fix; the alternative is a paid plan.

**Prerequisite:** `SUPABASE_URL` and `SUPABASE_ANON_KEY` already exist as repo secrets (ADR-0029 consequences — the release job writes `.env` from them). No new secrets.

- [ ] **Step 1: Create the workflow**

```yaml
# .github/workflows/supabase-keepalive.yml
name: Supabase Keep-Alive

# Supabase free-tier projects pause after ~7 days without API activity, and a paused
# project fails every tracker request with no dedicated UI state. One cheap request
# every 5 days keeps it awake for ~9 runner-minutes a year.
#
# Uses the anon key against PostgREST's root endpoint: no table access, no RLS surface,
# nothing that could read or write user data even if the key leaked (it is publishable
# by design under RLS — ADR-0014).
on:
  schedule:
    - cron: "0 6 */5 * *"   # every 5 days, 06:00 UTC — comfortably inside the 7-day window
  workflow_dispatch:

permissions: {}

jobs:
  ping:
    name: "Ping Supabase"
    runs-on: ubuntu-latest
    timeout-minutes: 5
    steps:
      - name: Ping PostgREST root
        env:
          SUPABASE_URL: ${{ secrets.SUPABASE_URL }}
          SUPABASE_ANON_KEY: ${{ secrets.SUPABASE_ANON_KEY }}
        run: |
          if [ -z "$SUPABASE_URL" ] || [ -z "$SUPABASE_ANON_KEY" ]; then
            echo "::warning::SUPABASE_URL/SUPABASE_ANON_KEY not set — skipping keep-alive."
            exit 0
          fi
          CODE=$(curl -sS -o /dev/null -w '%{http_code}' \
                   --max-time 30 \
                   -H "apikey: $SUPABASE_ANON_KEY" \
                   "${SUPABASE_URL%/}/rest/v1/")
          echo "PostgREST responded: $CODE"
          case "$CODE" in
            2*|3*|401|404) echo "Project is awake." ;;
            *) echo "::error::Unexpected status $CODE — project may be paused or misconfigured."; exit 1 ;;
          esac
```

`401`/`404` count as awake: the point is that *something served the request*, not that the endpoint authorised it.

- [ ] **Step 2: Verify**

Trigger via `workflow_dispatch` in the Actions tab. Expected: `Project is awake.` and a green run in well under a minute. If secrets are unset it warns and passes — it must never become a red check for a project that is not configured yet.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/supabase-keepalive.yml
git commit -m "ci: keep the Supabase free-tier project from auto-pausing"
```

---

## Task 13: Currency rate TTL guard (SEPARATE PR — app code)

**Files:**
- Modify: `apps/finance/data/src/main/java/com/dhruv/finance/data/CurrencyRepository.kt`
- Modify: `apps/finance/app/build.gradle.kts` (new `buildConfigField`)
- Modify: `apps/finance/app/src/main/java/com/dhruv/finance/app/di/PlatformModule.kt`
- Test: `apps/finance/data/src/test/java/com/dhruv/finance/data/CurrencyRepositoryTest.kt`

> **Do not bundle this into the CI PR.** It is app behaviour, not pipeline config — different reviewer, different blast radius, and it must go through `regressionCheck`.

`CurrencyViewModel.init` calls `syncCurrencyRates()` unconditionally ([CurrencyViewModel.kt:76-79](../../../apps/finance/feature/shell/currency/src/main/java/com/dhruv/finance/currency/CurrencyViewModel.kt#L76-L79)), and `fetchAndCacheLatestRates` always hits the network — the Room cache is consulted **only in the failure path**. `staleThresholdMs` exists but merely *labels* cached data stale; it never suppresses a request. `open.er-api.com` publishes new rates **once per day**, so every single screen open spends a free-tier request, mobile data and radio wake-up on data that cannot have changed.

**Interfaces:**
- Produces: `CurrencyRepository.fetchAndCacheLatestRates(baseCurrency: String, force: Boolean = false)`. When `force` is false and the newest cached row is younger than the TTL, returns the cached map without any network call.

- [ ] **Step 1: Write the failing test**

Add to `CurrencyRepositoryTest.kt` (follow the file's existing fake-DAO/fake-API setup — do not introduce a mocking library):

```kotlin
@Test
fun `skips the network when cached rates are younger than the TTL`() =
    runTest {
        val api = RecordingCurrencyApi(rates = mapOf("INR" to 83.0))
        val dao = FakeCurrencyRateDao(
            seeded = listOf(
                CurrencyRateEntity("INR", 82.0, timestamp = System.currentTimeMillis() - 60_000L),
            ),
        )
        val repo = CurrencyRepositoryImpl(dao, api, rateTtlMillis = 24 * 60 * 60 * 1000L)

        val result = repo.fetchAndCacheLatestRates("USD")

        assertEquals(0, api.callCount)
        assertEquals(82.0, result.getOrThrow()["INR"])
    }

@Test
fun `fetches when the cache is older than the TTL`() =
    runTest {
        val api = RecordingCurrencyApi(rates = mapOf("INR" to 83.0))
        val dao = FakeCurrencyRateDao(
            seeded = listOf(
                CurrencyRateEntity("INR", 82.0, timestamp = System.currentTimeMillis() - 48 * 60 * 60 * 1000L),
            ),
        )
        val repo = CurrencyRepositoryImpl(dao, api, rateTtlMillis = 24 * 60 * 60 * 1000L)

        val result = repo.fetchAndCacheLatestRates("USD")

        assertEquals(1, api.callCount)
        assertEquals(83.0, result.getOrThrow()["INR"])
    }

@Test
fun `force bypasses a fresh cache`() =
    runTest {
        val api = RecordingCurrencyApi(rates = mapOf("INR" to 83.0))
        val dao = FakeCurrencyRateDao(
            seeded = listOf(
                CurrencyRateEntity("INR", 82.0, timestamp = System.currentTimeMillis()),
            ),
        )
        val repo = CurrencyRepositoryImpl(dao, api, rateTtlMillis = 24 * 60 * 60 * 1000L)

        repo.fetchAndCacheLatestRates("USD", force = true)

        assertEquals(1, api.callCount)
    }

@Test
fun `empty cache always fetches regardless of TTL`() =
    runTest {
        val api = RecordingCurrencyApi(rates = mapOf("INR" to 83.0))
        val repo = CurrencyRepositoryImpl(FakeCurrencyRateDao(seeded = emptyList()), api,
                                          rateTtlMillis = 24 * 60 * 60 * 1000L)

        repo.fetchAndCacheLatestRates("USD")

        assertEquals(1, api.callCount)
    }
```

- [ ] **Step 2: Run and verify it fails**

Run: `./gradlew :apps:finance:data:testDebugUnitTest --tests "com.dhruv.finance.data.CurrencyRepositoryTest"`
Expected: compile failure — `CurrencyRepositoryImpl` has no `rateTtlMillis` parameter and `fetchAndCacheLatestRates` has no `force` parameter.

- [ ] **Step 3: Add the TTL as configuration, not a literal**

Project rule: no hardcoded values in code. The TTL rides the same `buildConfigField` → `PlatformModule` → constructor path `HISTORY_RECYCLE_BIN_RETENTION_MILLIS` already uses.

In `apps/finance/app/build.gradle.kts`, beside the other currency fields:

```kotlin
        // Rates from open.er-api.com refresh once daily; anything shorter is a wasted request.
        buildConfigField("long", "CURRENCY_RATE_TTL_MILLIS", "${24 * 60 * 60 * 1000L}L")
```

In `PlatformModule.kt`, extend the existing `CurrencyRepository` registration (or add one if it is currently constructed in `dataModule`) to pass `BuildConfig.CURRENCY_RATE_TTL_MILLIS`, exactly as `HistoryRepository` receives its retention value.

- [ ] **Step 4: Implement the guard**

In `CurrencyRepository.kt`, widen the interface method and gate the fetch:

```kotlin
    override suspend fun fetchAndCacheLatestRates(
        baseCurrency: String,
        force: Boolean,
    ): Result<Map<String, Double>> {
        if (!force) {
            val cached = currencyRateDao.getAllRates()
            val newest = cached.maxOfOrNull { it.timestamp }
            if (cached.isNotEmpty() && newest != null &&
                System.currentTimeMillis() - newest < rateTtlMillis
            ) {
                return Result.success(cached.associate { it.currencyCode to it.rate })
            }
        }
        return fetchFromNetwork(baseCurrency)   // the existing body, extracted unchanged
    }
```

Declare it on the interface as `suspend fun fetchAndCacheLatestRates(baseCurrency: String, force: Boolean = false)` so existing single-argument call sites keep compiling.

- [ ] **Step 5: Update the ViewModel fakes**

`CurrencyViewModelTest.kt:35` and `CurrencyViewModelEdgeCaseTest.kt:41` both `override` this function. A default value on an interface method does **not** carry to an override's signature — both fakes must gain the `force: Boolean` parameter or the modules will not compile.

- [ ] **Step 6: Wire the explicit refresh path**

The user-triggered refresh must still work. Wherever `CurrencyViewModel.syncCurrencyRates()` is invoked from a retry/refresh action (as opposed to `init`), pass `force = true`. Keep `init` on the TTL-respecting path — that is the whole point of the change.

- [ ] **Step 7: Run the tests**

Run: `./gradlew :apps:finance:data:testDebugUnitTest :apps:finance:feature:currency:testDebugUnitTest`
Expected: PASS, including the four new cases.

- [ ] **Step 8: Full regression + commit**

```bash
./gradlew regressionCheck
git add apps/finance/data apps/finance/app apps/finance/feature/shell/currency
git commit -m "fix: skip currency API fetch while cached rates are within TTL"
```

---

## Task 14: CI cost telemetry — measure every run, commit and merge pipeline

**Files:**
- Create: `scripts/ci/actions_usage.py`
- Create: `scripts/ci/test_actions_usage.py`
- Create: `.github/workflows/ci-usage-report.yml`

> **Run Step 4 (baseline capture) BEFORE merging Part 1.** Without a measured "before", the reduction stays modeled — see the caveat under §Cost model. This task exists precisely to replace estimates with the numbers GitHub actually bills.

**Why this is not optional going forward.** Build cost grows with the codebase. `regressionCheck` compiles and tests every module, and the roadmap adds seven more (`money`, `planning`, `insurance`, `retirement`, `insights`, `automation`, `networth`). A one-time 55% cut that silently erodes as modules land is worth little; a standing measurement turns cost into something you notice at 10% drift instead of at the monthly cap.

### On-call questions this must answer

1. What did the **last merge** actually cost, split into commit pipeline (PR + fast-feedback) vs merge pipeline (release)?
2. Which workflow is the largest line item this month?
3. At the current rate, do we cross the 2000-minute Free-tier cap before month end?
4. Did a newly added module make the build materially slower — and when did it start?

Each output below maps to one of these. **Use the timing API, not wall-clock:** `GET /repos/{repo}/actions/runs/{id}/timing` returns `billable.UBUNTU.total_ms`, which is what GitHub charges — job wall-clock from the runs list is not, because billing rounds each job up to the minute.

**Interfaces:**
- Produces: `python3 scripts/ci/actions_usage.py --days N [--repo owner/name] [--format md|text]`. Reads `GH_TOKEN` (or `GITHUB_TOKEN`) from the environment. Prints a Markdown or plain-text report to stdout. Exit 1 on auth failure or an unreachable API.

- [ ] **Step 1: Write the failing test**

The network layer is injected so the aggregation is testable without hitting GitHub.

```python
# scripts/ci/test_actions_usage.py
"""Tests for actions_usage.py. Pure stdlib: python3 -m unittest discover -s scripts/ci -p 'test_*.py'"""
import unittest

from actions_usage import Run, Report, build_report, MERGE_BRANCHES


def run(workflow, event, branch, minutes, created="2026-08-01T00:00:00Z"):
    return Run(workflow=workflow, event=event, branch=branch,
               billable_ms=int(minutes * 60_000), created_at=created)


class BuildReportTest(unittest.TestCase):
    def test_splits_commit_and_merge_pipelines(self):
        runs = [
            run("CI", "pull_request", "feat/x", 40),
            run("Fast Feedback", "push", "feat/x", 18),
            run("CI", "push", "develop", 19),
        ]
        rep = build_report(runs, days=30)
        self.assertEqual(rep.commit_minutes, 58)
        self.assertEqual(rep.merge_minutes, 19)

    def test_merge_branch_pushes_count_as_merge_pipeline(self):
        for branch in MERGE_BRANCHES:
            rep = build_report([run("CI", "push", branch, 20)], days=30)
            self.assertEqual(rep.merge_minutes, 20, branch)

    def test_per_merge_average_divides_by_merge_count(self):
        runs = [
            run("CI", "pull_request", "feat/a", 40),
            run("CI", "push", "develop", 20),
            run("CI", "pull_request", "feat/b", 40),
            run("CI", "push", "develop", 20),
        ]
        rep = build_report(runs, days=30)
        self.assertEqual(rep.merge_count, 2)
        self.assertEqual(rep.per_merge_minutes, 60)

    def test_per_merge_average_is_zero_when_no_merges(self):
        rep = build_report([run("CI", "pull_request", "feat/a", 40)], days=30)
        self.assertEqual(rep.merge_count, 0)
        self.assertEqual(rep.per_merge_minutes, 0)

    def test_projection_scales_window_to_thirty_days(self):
        rep = build_report([run("CI", "push", "develop", 100)], days=10)
        self.assertEqual(rep.projected_monthly_minutes, 300)

    def test_by_workflow_totals_are_sorted_descending(self):
        runs = [
            run("Fast Feedback", "push", "feat/x", 5),
            run("CI", "pull_request", "feat/x", 40),
            run("OWASP Weekly", "schedule", "develop", 25),
        ]
        rep = build_report(runs, days=30)
        self.assertEqual([w for w, _ in rep.by_workflow], ["CI", "OWASP Weekly", "Fast Feedback"])

    def test_over_cap_flag(self):
        under = build_report([run("CI", "push", "develop", 500)], days=30)
        over = build_report([run("CI", "push", "develop", 2500)], days=30)
        self.assertFalse(under.over_cap)
        self.assertTrue(over.over_cap)


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python -m unittest discover -s scripts/ci -p "test_*.py" -v`
Expected: FAIL — `ModuleNotFoundError: No module named 'actions_usage'`.

- [ ] **Step 3: Write the implementation**

```python
#!/usr/bin/env python3
"""Report GitHub Actions billed minutes for this repo, split by pipeline.

Answers, from the numbers GitHub actually bills rather than wall-clock:
  * what a merge costs, split into commit pipeline (PR + fast-feedback) and
    merge pipeline (push to develop/main),
  * which workflow is the biggest line item,
  * whether the current rate crosses the Free-tier cap before month end.

Billing source is the per-run timing endpoint's `billable.UBUNTU.total_ms` —
the runs list only exposes wall-clock, and billing rounds every job up to the
minute, so wall-clock understates the bill.

Usage:
    GH_TOKEN=... python3 scripts/ci/actions_usage.py --days 30
    GH_TOKEN=... python3 scripts/ci/actions_usage.py --days 90 --format md

Pure stdlib.
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone

API = "https://api.github.com"
MERGE_BRANCHES = ("develop", "main")
# GitHub Free, private repo. Pro is 3000. Override with --cap.
DEFAULT_CAP_MINUTES = 2000


@dataclass(frozen=True)
class Run:
    workflow: str
    event: str
    branch: str
    billable_ms: int
    created_at: str


@dataclass
class Report:
    days: int
    total_minutes: int = 0
    commit_minutes: int = 0
    merge_minutes: int = 0
    merge_count: int = 0
    per_merge_minutes: int = 0
    projected_monthly_minutes: int = 0
    cap_minutes: int = DEFAULT_CAP_MINUTES
    over_cap: bool = False
    by_workflow: list[tuple[str, int]] = field(default_factory=list)


def build_report(runs: list[Run], days: int, cap: int = DEFAULT_CAP_MINUTES) -> Report:
    rep = Report(days=days, cap_minutes=cap)
    per_workflow: dict[str, int] = {}

    for r in runs:
        minutes = round(r.billable_ms / 60_000)
        rep.total_minutes += minutes
        per_workflow[r.workflow] = per_workflow.get(r.workflow, 0) + minutes

        is_merge = r.event == "push" and r.branch in MERGE_BRANCHES
        if is_merge:
            rep.merge_minutes += minutes
            if r.workflow == "CI":
                rep.merge_count += 1
        elif r.event in ("pull_request", "push"):
            rep.commit_minutes += minutes

    if rep.merge_count:
        rep.per_merge_minutes = round(
            (rep.commit_minutes + rep.merge_minutes) / rep.merge_count
        )

    if days:
        rep.projected_monthly_minutes = round(rep.total_minutes * 30 / days)
    rep.over_cap = rep.projected_monthly_minutes > cap
    rep.by_workflow = sorted(per_workflow.items(), key=lambda kv: -kv[1])
    return rep


def _get(url: str, token: str) -> dict:
    req = urllib.request.Request(
        url,
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "dhruv-actions-usage",
        },
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.load(resp)


def fetch_runs(repo: str, days: int, token: str) -> list[Run]:
    since = (datetime.now(timezone.utc) - timedelta(days=days)).strftime("%Y-%m-%d")
    runs: list[Run] = []
    page = 1
    while True:
        url = (f"{API}/repos/{repo}/actions/runs"
               f"?created=%3E%3D{since}&per_page=100&page={page}")
        data = _get(url, token)
        items = data.get("workflow_runs", [])
        if not items:
            break
        for item in items:
            # One extra call per run; the timing endpoint is the only billed source.
            try:
                timing = _get(f"{API}/repos/{repo}/actions/runs/{item['id']}/timing", token)
            except urllib.error.HTTPError:
                continue  # run too old to have timing data — skip, do not guess
            ms = timing.get("billable", {}).get("UBUNTU", {}).get("total_ms", 0)
            runs.append(Run(
                workflow=item.get("name", "?"),
                event=item.get("event", "?"),
                branch=item.get("head_branch") or "?",
                billable_ms=ms,
                created_at=item.get("created_at", ""),
            ))
        if len(items) < 100:
            break
        page += 1
    return runs


def render(rep: Report, fmt: str) -> str:
    status = "OVER CAP" if rep.over_cap else "within cap"
    lines = [
        f"## Actions usage — last {rep.days} days",
        "",
        "| Metric | Value |",
        "|---|---|",
        f"| Total billed | {rep.total_minutes} min |",
        f"| Commit pipeline (PR + fast-feedback) | {rep.commit_minutes} min |",
        f"| Merge pipeline (push to develop/main) | {rep.merge_minutes} min |",
        f"| Merges | {rep.merge_count} |",
        f"| **Per merge** | **{rep.per_merge_minutes} min** |",
        f"| Projected 30-day | {rep.projected_monthly_minutes} min / {rep.cap_minutes} ({status}) |",
        "",
        "| Workflow | Minutes |",
        "|---|---|",
    ]
    lines += [f"| {name} | {mins} |" for name, mins in rep.by_workflow]
    md = "\n".join(lines)
    if fmt == "md":
        return md
    return md.replace("|", " ").replace("---", "").replace("#", "").strip()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--days", type=int, default=30)
    parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY", ""))
    parser.add_argument("--cap", type=int, default=DEFAULT_CAP_MINUTES)
    parser.add_argument("--format", choices=("md", "text"), default="md")
    args = parser.parse_args()

    token = os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_TOKEN")
    if not token:
        print("ERROR: set GH_TOKEN or GITHUB_TOKEN", file=sys.stderr)
        return 1
    if not args.repo:
        print("ERROR: pass --repo owner/name (or set GITHUB_REPOSITORY)", file=sys.stderr)
        return 1

    try:
        runs = fetch_runs(args.repo, args.days, token)
    except urllib.error.HTTPError as err:
        print(f"ERROR: GitHub API {err.code}: {err.reason}", file=sys.stderr)
        return 1

    print(render(build_report(runs, args.days, args.cap), args.format))
    return 0


if __name__ == "__main__":
    sys.exit(main())
```

- [ ] **Step 4: Capture the BEFORE baseline (do this before merging Part 1)**

```bash
GH_TOKEN=<your token> python scripts/ci/actions_usage.py --days 30 --repo <owner>/dhruv \
  > docs/superpowers/plans/ci-usage-baseline-before.md
```

Commit that file alongside the plan. It is the only chance to measure the pre-change cost — once Part 1 merges, the old pipeline is gone.

- [ ] **Step 5: Run the tests**

Run: `python -m unittest discover -s scripts/ci -p "test_*.py" -v`
Expected: PASS — 7 new cases plus the bump-version cases from Task 1.

- [ ] **Step 6: Add the monthly report workflow**

```yaml
# .github/workflows/ci-usage-report.yml
name: CI Usage Report

# Standing cost telemetry (ADR-0026). Build cost grows with the codebase — seven more
# feature modules are planned — so a one-time cut that silently erodes is worth little.
# This posts the billed-minutes breakdown to the run's Job Summary every month.
#
# Its own cost: ~1 runner-minute per month. That is the point — 12 min/year to keep a
# five-figure annual minute budget visible.
on:
  schedule:
    - cron: "0 7 1 * *"   # 1st of the month, 07:00 UTC
  workflow_dispatch:
    inputs:
      days:
        description: "Window in days"
        default: "30"

permissions:
  actions: read
  contents: read

jobs:
  report:
    name: "Billed minutes report"
    runs-on: ubuntu-latest
    timeout-minutes: 10
    steps:
      - uses: actions/checkout@v4
        with:
          sparse-checkout: scripts/ci
          sparse-checkout-cone-mode: false

      - name: Build usage report
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          python3 scripts/ci/actions_usage.py \
            --repo "$GITHUB_REPOSITORY" \
            --days "${{ github.event.inputs.days || 30 }}" \
            | tee -a "$GITHUB_STEP_SUMMARY"
```

`sparse-checkout` pulls only `scripts/ci` — no reason to clone the repo to run one script.

- [ ] **Step 7: Verify**

Trigger `ci-usage-report.yml` via `workflow_dispatch`. Expected: the Job Summary tab shows the two tables, `Per merge` is populated, and the run itself takes under a minute.

- [ ] **Step 8: Commit**

```bash
git add scripts/ci/actions_usage.py scripts/ci/test_actions_usage.py \
        .github/workflows/ci-usage-report.yml
git commit -m "ci: report billed Actions minutes per pipeline, monthly"
```

---

## Task 15: Dev-environment guidance (docs only)

**Files:**
- Modify: `supabase/README.md`

ADR-0029 reuses the hosted `dhruv` Supabase project as the dev/RLS-testing target. Every dev run therefore spends the free tier's egress and row budget on throwaway test data, and mixes test rows into the same project that will hold real holdings. The Supabase CLI runs the whole stack locally in Docker at zero cost, from the same `supabase/migrations/0001_init.sql` that ships to production.

- [ ] **Step 1: Document the local-first workflow**

Add to `supabase/README.md`:

```markdown
## Local development (default)

Run the stack locally instead of pointing debug builds at the hosted project — same
migrations, zero free-tier consumption, and no test rows mixed into real data.

    supabase start                 # Postgres + GoTrue + PostgREST in Docker
    supabase db reset              # applies migrations/ from scratch
    supabase status                # prints the local API URL and anon key

Put the printed values in your local `.env` as `SUPABASE_URL` / `SUPABASE_ANON_KEY`
(`.env` is gitignored). `supabase stop` when finished.

The hosted project is for real data and for verifying RLS behaviour against the
managed GoTrue before a release — not for day-to-day development.
```

- [ ] **Step 2: Commit**

```bash
git add supabase/README.md
git commit -m "docs: prefer local Supabase for development over the hosted project"
```

---

## Task 16: Run only the affected modules' tests on a PR

**Files:**
- Create: `scripts/ci/affected_modules.py`
- Create: `scripts/ci/test_affected_modules.py`
- Modify: `.github/workflows/ci.yml` — the `tests` job's Gradle invocation
- Create: `.github/workflows/regression-full.yml`

> **Concern, stated before building it.** Gradle's build cache already makes an untouched module's `testDebugUnitTest` a `FROM-CACHE` no-op costing seconds, so the saving here is smaller than it looks — the expensive part is compiling what *did* change plus Robolectric startup, and selective testing avoids neither. This task is therefore **gated on Task 14's measurement**: if the baseline shows a feature-only PR's `tests` job already runs fast, the weekly full-regression cost below outweighs the saving. Build it, measure, keep it only if the numbers agree. The break-even is roughly **8 feature-only PRs per month**.

### Three problems a naive implementation gets wrong

1. **Path → Gradle module is not mechanical here.** `settings.gradle.kts` remaps `projectDir` for every feature module — `:apps:finance:feature:loans` lives at `apps/finance/feature/plan/loans`. A `path.replace('/', ':')` mapping silently resolves nothing and tests nothing. The script parses `settings.gradle.kts` (the source of truth) instead of guessing.
2. **ArchUnit is global.** `DependencyRulesTest` lives in `:apps:finance:app`, and a `feature → feature` edge added in *any* module is only caught there. That module's tests run on **every** PR regardless of what changed.
3. **The coverage floor cannot be partially computed.** `jacocoCoverageVerification` ([build.gradle.kts:106-119](../../../build.gradle.kts#L106-L119)) enforces `globalLineFloor` over the **merged** report. Running a subset makes the aggregate meaningless, so a selective run must not gate on it — and something else must, or ADR-0013's ratchet quietly stops ratcheting.

### Resolution

| PR touches | Runs | Coverage gated? |
|---|---|---|
| `libs/**`, `apps/finance/data/**`, `build-logic/**`, `gradle/**`, root build files, `.github/workflows/**` | full `regressionCheck` | **yes** — unchanged from today |
| One or more feature modules only | those modules' `testDebugUnitTest` + `:apps:finance:app:testDebugUnitTest` | no |
| — | weekly `regression-full.yml` on `develop` | **yes** |

Shared code — the only place a coverage change can move the global number meaningfully — still gates on every PR that touches it. Feature-only PRs trade a same-merge coverage check for a within-7-days one.

**Interfaces:**
- Produces: `scripts/ci/affected_modules.py < changed-files` → prints either `regressionCheck` (run everything) or a space-separated list of `:module:testDebugUnitTest` tasks. Consumed by the `tests` job.

- [ ] **Step 1: Write the failing test**

```python
# scripts/ci/test_affected_modules.py
"""Tests for affected_modules.py. Pure stdlib."""
import unittest

from affected_modules import ALWAYS, parse_modules, affected, render

SETTINGS = '''
include(":apps:finance:app")
include(":apps:finance:data")
include(":apps:finance:feature:loans")
include(":apps:finance:feature:currency")
include(":libs:core")
project(":apps:finance:feature:loans").projectDir = file("apps/finance/feature/plan/loans")
project(":apps:finance:feature:currency").projectDir = file("apps/finance/feature/shell/currency")
'''


class ParseModulesTest(unittest.TestCase):
    def test_include_without_remap_uses_the_default_path(self):
        mods = parse_modules(SETTINGS)
        self.assertEqual(mods[":apps:finance:app"], "apps/finance/app")
        self.assertEqual(mods[":libs:core"], "libs/core")

    def test_projectdir_remap_overrides_the_default(self):
        mods = parse_modules(SETTINGS)
        self.assertEqual(mods[":apps:finance:feature:loans"], "apps/finance/feature/plan/loans")


class AffectedTest(unittest.TestCase):
    def setUp(self):
        self.mods = parse_modules(SETTINGS)

    def test_feature_change_selects_that_module_plus_app(self):
        got = affected(["apps/finance/feature/plan/loans/src/main/java/X.kt"], self.mods)
        self.assertEqual(got, sorted({":apps:finance:feature:loans", ALWAYS}))

    def test_two_features_select_both(self):
        got = affected([
            "apps/finance/feature/plan/loans/src/main/java/X.kt",
            "apps/finance/feature/shell/currency/src/main/java/Y.kt",
        ], self.mods)
        self.assertIn(":apps:finance:feature:loans", got)
        self.assertIn(":apps:finance:feature:currency", got)

    def test_app_module_is_always_included(self):
        got = affected(["apps/finance/feature/plan/loans/src/main/java/X.kt"], self.mods)
        self.assertIn(ALWAYS, got)

    def test_libs_change_forces_a_full_run(self):
        self.assertIsNone(affected(["libs/core/src/main/kotlin/T.kt"], self.mods))

    def test_data_change_forces_a_full_run(self):
        self.assertIsNone(affected(["apps/finance/data/src/main/java/Repo.kt"], self.mods))

    def test_build_logic_and_catalog_force_a_full_run(self):
        self.assertIsNone(affected(["build-logic/src/main/kotlin/C.kt"], self.mods))
        self.assertIsNone(affected(["gradle/libs.versions.toml"], self.mods))

    def test_root_build_file_forces_full_but_a_module_build_file_does_not(self):
        self.assertIsNone(affected(["build.gradle.kts"], self.mods))
        got = affected(["apps/finance/feature/plan/loans/build.gradle.kts"], self.mods)
        self.assertEqual(got, sorted({":apps:finance:feature:loans", ALWAYS}))

    def test_workflow_change_forces_a_full_run(self):
        self.assertIsNone(affected([".github/workflows/ci.yml"], self.mods))

    def test_unmapped_file_still_runs_the_app_module(self):
        self.assertEqual(affected(["scripts/ci/x.py"], self.mods), [ALWAYS])


class RenderTest(unittest.TestCase):
    def test_none_renders_the_full_gate(self):
        self.assertEqual(render(None), "regressionCheck")

    def test_modules_render_as_test_tasks(self):
        self.assertEqual(
            render([":apps:finance:app", ":apps:finance:feature:loans"]),
            ":apps:finance:app:testDebugUnitTest :apps:finance:feature:loans:testDebugUnitTest",
        )


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python -m unittest discover -s scripts/ci -p "test_*.py" -v`
Expected: FAIL — `ModuleNotFoundError: No module named 'affected_modules'`.

- [ ] **Step 3: Write the implementation**

```python
#!/usr/bin/env python3
"""Map changed files to the Gradle test tasks that actually need to run.

Reads a newline-separated list of changed paths on STDIN and prints either:
  * `regressionCheck` — run the full gate (shared code changed), or
  * a space-separated list of `:module:testDebugUnitTest` tasks.

Module paths are parsed from settings.gradle.kts rather than derived from the
directory layout: feature modules are physically grouped by owning tab
(apps/finance/feature/plan/loans) while keeping flat Gradle coordinates
(:apps:finance:feature:loans) via projectDir remapping, so a path->coordinate
guess resolves nothing and would silently test nothing.

Usage:
    git diff --name-only BASE HEAD | python3 scripts/ci/affected_modules.py

Pure stdlib.
"""
from __future__ import annotations

import argparse
import re
import sys

SETTINGS_FILE = "settings.gradle.kts"

# ArchUnit's DependencyRulesTest lives in the app module and is GLOBAL: a
# feature -> feature edge introduced in any module is only caught there. It also
# depends on every feature, so it compiles them anyway. Always run it.
ALWAYS = ":apps:finance:app"

# Exact-path triggers for a full run (a module's own build.gradle.kts is NOT one).
GLOBAL_FILES = ("settings.gradle.kts", "build.gradle.kts", "gradle.properties")

# Prefix triggers: everything depends on these, or they change the build itself.
# :libs:* and :apps:finance:data are dependencies of every feature, so a change there
# can break any module — a selective run would be unsound. This is deliberately
# conservative: no dependency-graph parsing, no chance of missing a dependent.
GLOBAL_PREFIXES = ("gradle/", "build-logic/", "libs/", "apps/finance/data/", ".github/workflows/")


def parse_modules(settings_text: str) -> dict[str, str]:
    """Return {gradle_path: source_dir}; projectDir remaps override include() defaults."""
    modules: dict[str, str] = {}
    for m in re.finditer(r'include\("(:[^"]+)"\)', settings_text):
        path = m.group(1)
        modules[path] = path[1:].replace(":", "/")
    for m in re.finditer(r'project\("(:[^"]+)"\)\.projectDir\s*=\s*file\("([^"]+)"\)', settings_text):
        modules[m.group(1)] = m.group(2).rstrip("/")
    return modules


def _is_global(path: str) -> bool:
    return path in GLOBAL_FILES or path.startswith(GLOBAL_PREFIXES)


def affected(files: list[str], modules: dict[str, str]) -> list[str] | None:
    """None = run everything. Otherwise the sorted list of affected Gradle module paths."""
    if any(_is_global(f) for f in files if f):
        return None

    hit = {ALWAYS}
    for f in files:
        if not f:
            continue
        best_path, best_len = None, -1
        for gradle_path, src_dir in modules.items():
            prefix = src_dir.rstrip("/") + "/"
            if f.startswith(prefix) and len(prefix) > best_len:
                best_path, best_len = gradle_path, len(prefix)
        if best_path:
            hit.add(best_path)
    return sorted(hit)


def render(modules: list[str] | None) -> str:
    if modules is None:
        return "regressionCheck"
    return " ".join(f"{m}:testDebugUnitTest" for m in modules)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--settings", default=SETTINGS_FILE)
    args = parser.parse_args()

    with open(args.settings, encoding="utf-8") as f:
        modules = parse_modules(f.read())

    files = [line.strip() for line in sys.stdin]
    print(render(affected(files, modules)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `python -m unittest discover -s scripts/ci -p "test_*.py" -v`
Expected: PASS — 13 new cases.

- [ ] **Step 5: Sanity-check against the real settings file**

```bash
printf 'apps/finance/feature/plan/loans/src/main/java/X.kt\n' | python scripts/ci/affected_modules.py
printf 'libs/core/src/main/kotlin/T.kt\n' | python scripts/ci/affected_modules.py
```
Expected: `:apps:finance:app:testDebugUnitTest :apps:finance:feature:loans:testDebugUnitTest`, then `regressionCheck`.

- [ ] **Step 6: Publish the file list from the `changes` job**

The list is already fetched there (Task 4). Expose it so `tests` can consume it without a second API call — add to the `changes` job's `outputs:` and write it in the filter step:

```yaml
    outputs:
      code: ${{ steps.filter.outputs.code }}
      files: ${{ steps.filter.outputs.files }}
```

and inside the step, after `FILES` is resolved:

```bash
          # Newline-delimited, base64'd: job outputs cannot carry raw newlines.
          echo "files=$(printf '%s' "$FILES" | base64 -w0)" >> "$GITHUB_OUTPUT"
```

- [ ] **Step 7: Make the `tests` job selective**

This **supersedes Task 6 Step 2's** plain `regressionCheck` invocation. Replace the regression step in `tests`:

```yaml
      # Only the modules the diff touches, plus :apps:finance:app (global ArchUnit rules).
      # Shared code (libs/**, data/**, build-logic/**, catalog, root build files) resolves to
      # the full `regressionCheck` including the coverage floor — see scripts/ci/affected_modules.py.
      - name: Resolve affected test tasks
        id: affected
        run: |
          TASKS=$(echo "${{ needs.changes.outputs.files }}" | base64 -d \
                  | python3 scripts/ci/affected_modules.py)
          echo "tasks=$TASKS" >> "$GITHUB_OUTPUT"
          echo "Running: $TASKS"

      - name: Run affected tests
        run: ./gradlew ${{ steps.affected.outputs.tasks }} --continue
```

The coverage/summary steps that follow already carry `if: always()`; leave them — on a selective run the aggregated report simply covers fewer modules, and **nothing gates on it** because `jacocoCoverageVerification` only runs as part of `regressionCheck`.

Add `changes` to the job's existing `needs` (it is already there from Task 7).

- [ ] **Step 8: Add the weekly full regression**

```yaml
# .github/workflows/regression-full.yml
name: Full Regression

# PRs now run only the modules their diff touches (ADR-0026), so the global JaCoCo
# coverage floor — which is only meaningful over the MERGED report of every module —
# has no per-PR home unless the PR touched shared code. This weekly run is that home,
# and doubles as the safety net for any cross-module breakage a selective run missed.
#
# Cost: ~25 min/week. It is what buys the per-PR saving; if the measured saving is
# smaller than this, revert Task 16 rather than keep both.
on:
  schedule:
    - cron: "0 4 * * 0"   # Sundays 04:00 UTC
  workflow_dispatch:

permissions:
  contents: read

env:
  JAVA_VERSION: "21"
  GRADLE_OPTS: "-Dorg.gradle.jvmargs=-Xmx4g -Dorg.gradle.workers.max=2"

jobs:
  full:
    name: "regressionCheck + coverage floor (develop)"
    runs-on: ubuntu-latest
    timeout-minutes: 40
    steps:
      - uses: actions/checkout@v4
        with:
          ref: develop

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: temurin

      - name: Gradle cache
        uses: gradle/actions/setup-gradle@v3
        with:
          cache-read-only: true

      - name: Full regression suite
        run: ./gradlew regressionCheck --continue

      - name: Aggregate coverage report
        if: always()
        run: ./gradlew jacocoAggregatedReport --continue

      - name: Publish summary
        if: always()
        run: python3 scripts/ci/regression_summary.py | tee -a "$GITHUB_STEP_SUMMARY"

      - name: Upload coverage report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: coverage-report-weekly
          path: build/reports/jacoco/jacocoAggregatedReport/
          retention-days: 30
```

- [ ] **Step 9: Record the ADR-0013 amendment**

Append to ADR-0026's consequences in Task 10:

> **Amends ADR-0013.** The coverage floor no longer runs on *every* PR — only on PRs touching `libs/**`, `apps/finance/data/**`, `build-logic/**`, the version catalog or root build files, plus a weekly full run on `develop`. A feature-only PR that erodes coverage is therefore caught within 7 days rather than at merge. Accepted because the floor is a non-regression ratchet (currently 9 %), not a release gate, and because a feature-only diff cannot move the global aggregate far. ArchUnit's dependency rules are **not** weakened — `:apps:finance:app:testDebugUnitTest` runs on every PR regardless of the diff.

- [ ] **Step 10: Verify**

```bash
python -m unittest discover -s scripts/ci -p "test_*.py"
grep -n "affected_modules\|regressionCheck" .github/workflows/ci.yml
```
Expected: tests pass; `ci.yml` calls `affected_modules.py` and no longer hardcodes `regressionCheck`.

- [ ] **Step 11: Commit**

```bash
git add scripts/ci/affected_modules.py scripts/ci/test_affected_modules.py \
        .github/workflows/ci.yml .github/workflows/regression-full.yml
git commit -m "ci: run only the affected modules' tests on a PR"
```

---

## Task 17: Dependency update automation

**Files:**
- Create: `.github/dependabot.yml`

The repo runs a dependency vulnerability scan (OWASP) and has **nothing that ever updates a dependency**. Scanning without updating produces a report no one can act on cheaply. One file closes it, at zero runner cost — Dependabot runs on GitHub's infrastructure, not your minutes; only the resulting PRs consume CI.

- [ ] **Step 1: Create the config**

```yaml
# .github/dependabot.yml
# Dependabot itself costs no Actions minutes — only the PRs it opens do, which is why the
# limits below are deliberately low and the cadence monthly. Gradle and npm are grouped so
# a routine bump is one PR through the pipeline, not eight (ADR-0026's cost model).
version: 2
updates:
  - package-ecosystem: gradle
    directory: /
    schedule:
      interval: monthly
    open-pull-requests-limit: 3
    commit-message:
      # `chore:` keeps ADR-0025's bump detection at patch — a dependency bump is not a feature.
      prefix: chore
    groups:
      androidx:
        patterns: ["androidx.*"]
      kotlin-and-compose:
        patterns: ["org.jetbrains.kotlin*", "androidx.compose*"]
      test-libraries:
        patterns: ["*junit*", "*mockk*", "*turbine*", "*robolectric*"]

  - package-ecosystem: npm
    directory: /web
    schedule:
      interval: monthly
    open-pull-requests-limit: 3
    commit-message:
      prefix: chore
    groups:
      react:
        patterns: ["react", "react-dom", "@types/react", "@types/react-dom"]
      tooling:
        patterns: ["vite", "vitest", "typescript", "eslint*", "@vitest/*", "typescript-eslint"]

  - package-ecosystem: github-actions
    directory: /
    schedule:
      interval: monthly
    open-pull-requests-limit: 2
    commit-message:
      prefix: chore
```

- [ ] **Step 2: Verify**

Settings → Code security → Dependabot: the config parses with no errors. Then Insights → Dependency graph → Dependabot shows all three ecosystems with a next-run time.

- [ ] **Step 3: Commit**

```bash
git add .github/dependabot.yml
git commit -m "chore: enable grouped monthly Dependabot for gradle, npm and actions"
```

---

## Task 18: Web CI (SEPARATE PR)

**Files:**
- Create: `.github/workflows/web-ci.yml`

ADR-0015 and PLATFORM.md §3 both specify a `web-ci.yml` with path-based triggers deploying to Vercel. `web/package.json` and `web/src/` exist with `lint`, `typecheck`, `test` and `build` scripts and a committed `package-lock.json` — and **none of it has ever run in CI**. Web code has never been linted, type-checked, built or tested.

Path filtering is load-bearing here: without it, every Android PR would run the web pipeline and vice versa, which is exactly the duplicate-work class ADR-0026 exists to remove.

- [ ] **Step 1: Create the workflow**

```yaml
# .github/workflows/web-ci.yml
name: Web CI

# ADR-0015: the web SPA is a separate NPM project inside this Gradle monorepo, so both
# pipelines are path-filtered — an Android change must never run web jobs and vice versa.
# Trigger-level `paths` is safe HERE (unlike ci.yml, which needs job-level `if` for its
# required status checks) because none of these jobs is a required check on the Android side.
on:
  push:
    branches: [ main, develop ]
    paths: [ "web/**", ".github/workflows/web-ci.yml" ]
  pull_request:
    branches: [ main, develop ]
    paths: [ "web/**", ".github/workflows/web-ci.yml" ]

concurrency:
  group: web-${{ github.ref }}
  cancel-in-progress: ${{ github.event_name == 'pull_request' }}

permissions:
  contents: read

defaults:
  run:
    working-directory: web

jobs:
  verify:
    name: "Lint · Typecheck · Test · Build"
    runs-on: ubuntu-latest
    timeout-minutes: 15
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: "22"
          cache: npm
          cache-dependency-path: web/package-lock.json

      # `npm ci` (not `install`): reproducible from the lockfile, and fails loudly if
      # package.json and package-lock.json have drifted apart.
      - name: Install
        run: npm ci

      # --continue equivalent: run all four and report every failure in one pass rather
      # than making the developer fix one, push, and wait to discover the next.
      - name: Lint
        run: npm run lint
        continue-on-error: true
        id: lint

      - name: Typecheck
        run: npm run typecheck
        continue-on-error: true
        id: typecheck

      - name: Test
        run: npm test
        continue-on-error: true
        id: test

      - name: Build
        run: npm run build
        continue-on-error: true
        id: build

      # The gate: any of the four failing fails the job. continue-on-error above only
      # defers the failure so all four results are visible in one run.
      - name: Gate
        run: |
          FAILED=""
          [ "${{ steps.lint.outcome }}"      = "failure" ] && FAILED="$FAILED lint"
          [ "${{ steps.typecheck.outcome }}" = "failure" ] && FAILED="$FAILED typecheck"
          [ "${{ steps.test.outcome }}"      = "failure" ] && FAILED="$FAILED test"
          [ "${{ steps.build.outcome }}"     = "failure" ] && FAILED="$FAILED build"
          if [ -n "$FAILED" ]; then
            echo "❌ Failed:$FAILED" >&2
            exit 1
          fi
          echo "✅ lint, typecheck, test, build all passed"

      - name: Upload build output
        if: success()
        uses: actions/upload-artifact@v4
        with:
          name: web-dist
          path: web/dist
          retention-days: 7
```

Vercel deployment is **not** wired here. Vercel's own Git integration deploys on push without consuming Actions minutes; adding a deploy job would pay for something Vercel does free. Connect the repo in the Vercel dashboard with root directory `web/` instead.

- [ ] **Step 2: Verify the current state honestly**

Run the pipeline locally first — this code has never been checked, so expect failures:

```bash
cd web && npm ci && npm run lint; npm run typecheck; npm test; npm run build
```

Fix whatever it surfaces **in this PR** before enabling the workflow. Landing a gate that is red on arrival trains everyone to ignore it.

- [ ] **Step 3: Confirm path filtering both ways**

- [ ] A PR touching only `apps/finance/**` → `Web CI` does not appear in the checks list
- [ ] A PR touching only `web/**` → `Web CI` runs and `CI` skips its build gates via the `changes` job (`web/` is not `docs/`, so `code=true` — the Android gates run but find nothing changed and finish on cache). **Note this:** the `changes` filter in Task 4 treats `web/**` as code, so an Android build still runs for a web-only PR. If the measurement shows that costs real minutes, add `web/` to the docs-side exclusions — but only after Task 14 says it is worth it.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/web-ci.yml
git commit -m "ci: add web lint, typecheck, test and build pipeline"
```

---

## Cost budget for future development

Task 14's measurement is only useful against a number. These are the standing budgets; they belong in ADR-0026's consequences so a future PR that blows through one is a visible decision rather than drift.

| Budget | Value | What to do when exceeded |
|---|---|---|
| Per merged PR | **≤ 90 billed min** | Investigate before adding more modules. First lever: shard `regressionCheck` across a matrix by module group. |
| Commit-pipeline share | **≤ 70%** of per-merge cost | If higher, the PR gates have grown — check whether `static-analysis` is recompiling what `tests` already built. |
| Projected monthly | **≤ 1600 min** (80% of the 2000 Free cap) | Ticket, not a page. Options in order: widen the docs filter, shard tests, then reconsider batching releases. |
| New module's cost | **≤ +4 min** on `regressionCheck` | A module costing more than that has a slow test suite — fix the suite, not the pipeline. |

**Deliberately not built yet (YAGNI):** test sharding via a job matrix. It only pays off once `regressionCheck` alone exceeds ~30 min; today it does not, and a matrix multiplies fixed per-job overhead (checkout, JDK setup, cache restore) across every shard — below that threshold it costs more than it saves. Revisit when the measurement says so. That is what the measurement is for.

---

## Verification

### Local, before pushing

- [ ] `python -m unittest discover -s scripts/ci -p "test_*.py" -v` → all pass
- [ ] `bash scripts/ci/test_detect_bump.sh` → `ALL PASS`
- [ ] `python scripts/ci/bump_version.py --file platform/versions.json --build-number 15 --bump minor --dry-run` → prints `2.1.0`, `git status` clean
- [ ] `actionlint .github/workflows/*.yml` (if installed) → clean
- [ ] `grep -rn "needs.build\|needs.owasp" .github/workflows/` → no matches

### On GitHub, in this order

- [ ] **Docs-only PR** (touch one `.md`) → `changes` reports `code=false`; `static-analysis` and `tests` show **skipped**; `security` runs; PR is mergeable. Merge → **no** version bump, **no** APK, **no** Release.
- [ ] **`fix:` PR** → PR runs the full gate set exactly **once**. Merge push shows exactly two jobs: `changes`, `release`. Version bumps **patch**. Release notes still contain the test/coverage summary (proves the cross-run artifact fetch works).
- [ ] **`feat:` PR** → merge bumps **minor**; auto-bump commit message reads `chore: auto-bump (minor) to vX.Y.0 (versionCode=N) [skip ci]`.
- [ ] **Push a commit to a branch with an open PR** → `fast-feedback` runs `pr-check` only, `compile-and-test` skipped.
- [ ] **`workflow_dispatch` `owasp-scheduled.yml`** → completes, `owasp-report` artifact present.

### Release correctness (Task 5 Steps 5–7 — the shipped-artifact fixes)

- [ ] Before the change: local `assembleRelease` with no `.env` → `strings <apk> | grep -c MY_SUPABASE_URL` returns **non-zero**. That is the bug, reproduced.
- [ ] After the change: the same grep against the **CI-built** APK returns **0**.
- [ ] Temporarily unset one release secret → the `Write .env from secrets` step **fails the job** naming the missing key, and no tag or Release is created.
- [ ] First release after the change → size-delta step prints "baseline established"; the second prints an actual percentage.
- [ ] Install the published APK on a device and confirm sign-in reaches Google and the tracker reaches Supabase. **Nothing in CI proves this** — it is the manual step that closes the loop until an instrumented/smoke test exists.

### Pipeline coverage (Tasks 17, 18)

- [ ] Dependabot config parses (Settings → Code security) and all three ecosystems show a next-run time
- [ ] `cd web && npm ci && npm run lint && npm run typecheck && npm test && npm run build` passes **locally** before `web-ci.yml` is enabled — a gate that is red on arrival gets ignored
- [ ] Android-only PR → `Web CI` absent from checks; web-only PR → `Web CI` runs
- [ ] **Settings → Actions → Usage** one week later → minutes-per-merged-PR down ~75–80%.

### Part 2

- [ ] `gh cache list` total under 10 GB, Gradle cache present (Task 11)
- [ ] `workflow_dispatch` `supabase-keepalive.yml` → `Project is awake.`, run under a minute (Task 12)
- [ ] With secrets deliberately unset, the keep-alive still passes with a warning — it must never turn red for an unconfigured project (Task 12)
- [ ] `./gradlew regressionCheck` green after the TTL guard, coverage floor still met (Task 13)
- [ ] Open the Currency screen twice within a minute → exactly **one** network request (verify via logcat/OkHttp or a breakpoint on `fetchFromNetwork`) (Task 13)
- [ ] Retry/refresh on the Currency screen still fetches immediately (`force = true` path intact) (Task 13)
- [ ] `supabase start` + `supabase db reset` applies `0001_init.sql` cleanly against local Postgres (Task 15)

### Part 2 — cost telemetry (Task 14)

- [ ] `ci-usage-baseline-before.md` captured and committed **before** Part 1 merges — no baseline, no proof
- [ ] `python -m unittest discover -s scripts/ci -p "test_*.py"` → all pass, including the 7 aggregation cases
- [ ] `workflow_dispatch` `ci-usage-report.yml` → Job Summary shows both tables, `Per merge` populated, run under a minute
- [ ] One month after Part 1 merges, re-run with `--days 30` and diff against the baseline. **This is what replaces the modeled 55% with a measured number.**
- [ ] Every budget in §Cost budget is inside its threshold; if not, act on that row's stated remedy rather than accepting the drift

### Part 2 — selective tests (Task 16)

- [ ] Feature-only PR → the `tests` job log prints `Running: :apps:finance:app:testDebugUnitTest :apps:finance:feature:<name>:testDebugUnitTest`, and the job is measurably shorter than the baseline
- [ ] PR touching `libs/core` → log prints `Running: regressionCheck`, coverage floor enforced as before
- [ ] PR touching only a module's own `build.gradle.kts` → that module selected, **not** a full run
- [ ] Introduce a deliberate `feature → feature` import in a PR that touches only that one feature → `:apps:finance:app`'s ArchUnit test still fails it (proves rule 2 holds under selection). Revert after.
- [ ] `workflow_dispatch` `regression-full.yml` → green, coverage summary in the Job Summary, artifact attached
- [ ] **Decision gate:** compare the measured feature-only `tests` duration against the ~25 min/week `regression-full` cost. Below ~8 feature-only PRs/month this task is net-negative — **revert it rather than keep both**

### Rollback

Every change is confined to `.github/workflows/` and `scripts/ci/`. `git revert` of the merge commit restores the previous pipeline in one step; no build, app, or data artifact depends on any of it. The one thing revert does **not** undo is the branch-protection setting from Task 0 — leave it enabled, it is strictly an improvement either way.

---

## CI/CD gaps this plan does NOT close

Audited 2026-08-14 alongside the cost work. Recorded here so they are visible decisions rather than things nobody looked at. **Two of the four merge gates are currently cosmetic** — worth knowing while optimizing what the gates cost.

| # | Gap | Evidence | Why not here |
|---|---|---|---|
| H1 | **OWASP is a fake gate.** `continue-on-error: true` and the `org.owasp.dependencycheck` plugin is unwired in `build-logic`, so findings are masked and `pr-summary` always shows ✅ | ci.yml `owasp` job; ADR-0012's own consequences admit it | Already tracked as PRODUCTION_READINESS M1/T11. Task 3 moves it off the merge path so it stops costing money while it stays useless; wiring the plugin is that ticket's job. Task 17 (Dependabot) is the partial mitigation — updating dependencies beats scanning ones nobody updates. |
| H2 | **No post-release signal.** Firebase entirely unwired — an APK ships and returns zero crash or performance data | `libs/core/.../CrashReporter.kt:32-34` defensively no-ops; no `google-services.json`, no plugin | PRODUCTION_READINESS H1/T7. Needs a DPDP consent gate for Crashlytics collection, which makes it app work, not pipeline work. |
| H4 | **No rollback path.** `release.yml` is a `workflow_dispatch` republish tool; a bad APK on GitHub Releases has no documented un-publish or roll-forward procedure, and users install directly | release.yml | Needs a decision (delete the release? publish a `+1` patch? mark pre-release?) before it can be automated. Worth an ADR, not a script. |
| M2 | **Supabase migrations are never validated in CI.** `supabase/migrations/0001_init.sql` is applied by hand; nothing catches drift between the file and the live project | only `0001_init.sql` + README under `supabase/` | Task 15's local-first workflow makes `supabase db reset` routine, which is the cheap 80 %. A CI job needs a throwaway Postgres service container — worth doing once a second migration exists. |
| M3 | **Coverage floor is 9 %** — a non-regression ratchet by design (ADR-0013), not a gate that catches much | `build.gradle.kts:45` `globalLineFloor = "0.09"` | Deliberate. Raising it is the `dhruv-coverage` agent's job, ramped at plan checkpoints, never ahead of landed tests. |
| M4 | **No instrumented or smoke tests anywhere.** Nothing verifies the published APK installs and launches | ADR-0013 keeps `connectedAndroidTest` developer-local | An emulator job is the single most expensive thing you can add to CI (~10–15 min/run). Revisit only if Task 14's budget shows headroom — and even then, one launch smoke test on the release APK, not a suite. |
| M5 | **Workflows are unvalidated.** No `actionlint`; a YAML error is discovered by pushing | `.github/workflows/` | Cheap to add, but only worth it once the workflow churn of this plan settles. Add to `static-analysis` afterwards. |

**Sequencing implied by the above:** Task 5's Steps 5–7 (release correctness) are the only items in the gap register that block a working release, which is why they were folded into the cost PR rather than deferred. Everything else is either already ticketed or needs a decision first.

---

## Known caveats carried from the spec

1. **`feat/networth-tracker` double-bump (spec §7.2).** That branch carries a manual `1.3.0` edit in `platform/versions.json`. If it merges after this change with `feat:` commits, CI bumps again. **Revert the manual edit in that branch before merging it.** The same applies to any other open branch holding a hand-edited version.
2. **`pr-summary` stays informational-only** (ADR-0012) — `continue-on-error: true` makes it always report success, so requiring it as a status check would be purely cosmetic.
3. **OWASP findings are still masked** (`continue-on-error: true`) until the `org.owasp.dependencycheck` plugin is wired in `build-logic` — tracked separately as PRODUCTION_READINESS T11/M1. Moving it to a monthly schedule loses no gate value it currently has, but does not fix it either.
4. **Concurrency blocks are unchanged**: PR runs still cancel superseded runs; pushes to protected branches are never cancelled.
