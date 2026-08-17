# Agent Protocol + Doc Verifier — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stop agent-instruction drift by making the agent-doc set machine-verifiable: one canonical protocol in `platform/AGENTS.md` with a declared surface registry, and `scripts/ci/verify_agent_docs.py` proving every claim in it resolves to something real. A stale claim becomes a failing Gate 1 check on the PR that introduces it.

**Architecture:** `AGENTS.md` becomes the agent contract — a task lifecycle plus a **surface registry** (tables listing every module-context file, project skill, subagent, and slash command). The verifier diffs that registry against disk in **both directions** (declared-but-missing *and* on-disk-but-undeclared), resolves every link in the agent-doc set, validates every `ADR-NNNN` reference against `DECISIONS.md`, and flags any `platform/*.md` unreachable from the bootstrap chain. Genesis-era docs that the bootstrap no longer reaches are retired.

**Tech Stack:** Python 3 stdlib, Markdown, GitHub Actions, POSIX sh (pre-push hook).

**Design basis:** Settled in-session 2026-08-15 across three decisions — goal = *consistency / stop drift*; enforcement = *doc + CI verifier*; module context = *targeted, only where rules are real*.

## Global Constraints

- **Branch:** `chore/agent-protocol` off `develop`. **Sequence after `chore/ci-cost-optimization` merges** — see Task 0.
- **Python: pure stdlib only**, matching `scripts/ci/regression_summary.py` and `scripts/ci/bump_version.py`.
- **Commit prefixes: `docs:` or `chore:` only.** Never `feat:` — ADR-0025's detection would bump minor for a docs change.
- **ADR number is 0034.** Reserved as 0032 when this plan was written (highest was then 0031); by
  execution time both 0032 (dev/prod environment topology) and 0033 (per-app Postgres schema
  namespacing) had already landed on `develop`, the same collision class the numbering-hygiene note
  already documents once for ADR-0015 — a second note for this collision is added to
  `DECISIONS.md` alongside this correction. 0016–0023 are still reserved-but-unwritten per that same
  note. Verify with `grep '^## ADR-' platform/DECISIONS.md` before writing — if the highest has
  moved again since, use the next free number and update this file the same way.
- **Nothing enters the registry before it exists on disk.** Same rule as `DESIGN-SYSTEM.md` §5 — that rule was written *because* a fiction component library shipped, and this plan generalizes it.
- **Retirement follows the ADR-0030 precedent:** fold salvageable content into its destination first, redirect every inbound reference, then `git rm`. Never orphan a link.

---

## File Structure

| File | Status | Responsibility |
|---|---|---|
| `scripts/ci/verify_agent_docs.py` | create | Four checks proving the agent-doc set is true |
| `scripts/ci/test_verify_agent_docs.py` | create | unittest coverage, fixture-driven (no repo I/O) |
| `platform/AGENTS.md` | rewrite | The protocol: task lifecycle + surface registry |
| `libs/core/CLAUDE.md` | create | Component-library law, dependency-free rule |
| `apps/finance/data/CLAUDE.md` | create | Repository-only access, paise integers, append-only |
| `platform/Implementation.md` | **retire** | Genesis-era Phase 0–7 plan, superseded |
| `platform/RUNBOOK.md` | **retire** | Genesis-era runbook ("rename the GitHub repo") |
| `platform/CLAUDE-MD-TEMPLATES.md` | **retire** | Templates now served by the 4 real files |
| `platform/adr/0010-initial-repo-state.md` | **retire** | Vestigial split; 31 ADRs live in `DECISIONS.md` |
| `platform/DECISIONS.md` | modify | ADR-0036 (renumbered 0032 → 0034 → 0036 — see Global Constraints) |
| `README.md`, `docs/PRD.md` | modify | Redirect references to retired docs |
| `.gitignore` | modify | `.claude/worktrees/` (currently local-only) |
| `.github/workflows/ci.yml` | modify | Verifier step in Gate 1 |
| `scripts/hooks/pre-push` | modify | Verifier call (deterministic, sub-second) |

---

## Task 0: Sequencing prerequisite (blocking)

**Files:** none.

`chore/ci-cost-optimization` is open and **rewrites the `static-analysis` job** this plan adds a step to. Both branches editing that job means a guaranteed conflict, and the CI branch is the larger, already-verified change.

- [ ] **Step 1: Confirm the CI PR state**

```bash
git fetch origin
git log --oneline origin/develop | head -3
```
Expected: the CI cost commits are present on `develop`. If not, **stop** — this plan's Task 5 targets a version of Gate 1 that does not exist yet.

- [ ] **Step 2: Branch**

```bash
git switch -c chore/agent-protocol origin/develop
```

- [ ] **Step 3: Record the ADR number to use**

```bash
grep '^## ADR-' platform/DECISIONS.md | tail -3
```
Expected highest at execution time: re-run the grep above — `ADR-0034` is the highest as of the
second correction (2026-08-18). Use **ADR-0036**. This line has now been proved right twice: the
number moved 0032 → 0034 when ADR-0032/0033 landed, then 0034 → 0036 when ADR-0034 (public
repository) and `platform/VERSIONING.md`'s reservation took 0034 and 0035 respectively — both
times while this plan sat dormant. Do not trust the number printed here; re-run the grep and use
the next free one, noting it in Task 6.

---

## Task 1: The verifier + tests

**Files:**
- Create: `scripts/ci/verify_agent_docs.py`
- Test: `scripts/ci/test_verify_agent_docs.py`

**Interfaces:**
- Produces: CLI `python3 scripts/ci/verify_agent_docs.py [--root PATH]`. Prints one line per violation to stderr, a summary to stdout. Exit 0 clean, 1 on any violation.
- Produces (importable, for tests): `parse_registry(text) -> dict[str, set[str]]`, `check_registry(declared: set[str], actual: set[str], kind: str) -> list[str]`, `extract_links(text) -> list[str]`, `extract_adr_refs(text) -> set[str]`.

The four checks are separated so each is unit-testable against fixtures with no filesystem dependency.

- [ ] **Step 1: Write the failing test**

```python
# scripts/ci/test_verify_agent_docs.py
"""Tests for verify_agent_docs.py. Pure stdlib:
    python3 -m unittest discover -s scripts/ci -p 'test_*.py'
"""
import unittest

from verify_agent_docs import (
    check_registry,
    extract_adr_refs,
    extract_links,
    parse_registry,
)

REGISTRY_DOC = """
# AGENTS.md

Some prose.

<!-- registry:skills -->
| Skill | Purpose |
|---|---|
| `dhruv-feature-scaffold` | New feature module |
| `dhruv-room-entity` | Data layer |
<!-- /registry:skills -->

More prose.

<!-- registry:agents -->
| Agent | Use for |
|---|---|
| `dhruv-module-auditor` | Pre-merge audit |
<!-- /registry:agents -->
"""


class ParseRegistryTest(unittest.TestCase):
    def test_extracts_named_blocks(self):
        reg = parse_registry(REGISTRY_DOC)
        self.assertEqual(reg["skills"], {"dhruv-feature-scaffold", "dhruv-room-entity"})
        self.assertEqual(reg["agents"], {"dhruv-module-auditor"})

    def test_missing_block_is_empty_not_error(self):
        reg = parse_registry("# nothing here")
        self.assertEqual(reg, {})

    def test_ignores_prose_backticks_outside_blocks(self):
        doc = "Use `not-a-skill` freely.\n" + REGISTRY_DOC
        reg = parse_registry(doc)
        self.assertNotIn("not-a-skill", reg["skills"])


class CheckRegistryTest(unittest.TestCase):
    def test_clean_when_sets_match(self):
        self.assertEqual(check_registry({"a", "b"}, {"a", "b"}, "skills"), [])

    def test_reports_declared_but_missing(self):
        errs = check_registry({"a", "ghost"}, {"a"}, "skills")
        self.assertEqual(len(errs), 1)
        self.assertIn("ghost", errs[0])
        self.assertIn("declared", errs[0])

    def test_reports_on_disk_but_undeclared(self):
        errs = check_registry({"a"}, {"a", "orphan"}, "agents")
        self.assertEqual(len(errs), 1)
        self.assertIn("orphan", errs[0])
        self.assertIn("not declared", errs[0])

    def test_reports_both_directions_at_once(self):
        errs = check_registry({"a", "ghost"}, {"a", "orphan"}, "commands")
        self.assertEqual(len(errs), 2)


class ExtractLinksTest(unittest.TestCase):
    def test_extracts_at_prefixed_paths(self):
        self.assertIn("platform/PLATFORM.md", extract_links("Read @platform/PLATFORM.md now"))

    def test_extracts_markdown_relative_links(self):
        self.assertIn("feature/README.md", extract_links("see [x](feature/README.md)"))

    def test_ignores_external_urls(self):
        links = extract_links("[gh](https://github.com/x) and [m](mailto:a@b.c)")
        self.assertEqual(links, [])

    def test_ignores_anchor_only_links(self):
        self.assertEqual(extract_links("[top](#section)"), [])

    def test_strips_line_anchor_from_path(self):
        self.assertIn("a/b.kt", extract_links("[b](a/b.kt#L12-L20)"))

    def test_extracts_backticked_paths(self):
        # The bootstrap list writes paths as `platform/PLATFORM.md`, not as markdown
        # links. Missing these would make check 4 flag every platform doc as an orphan.
        self.assertIn("platform/PLATFORM.md", extract_links("1. `platform/PLATFORM.md` — truth"))

    def test_ignores_backticked_non_paths(self):
        links = extract_links("Use `regressionCheck` and `feat:` and `Long`")
        self.assertEqual(links, [])

    def test_ignores_backticked_placeholder_paths(self):
        # e.g. `apps/finance/feature/<name>/` — a pattern, not a real file
        self.assertEqual(extract_links("see `apps/finance/feature/<name>/README.md`"), [])

    def test_ignores_backticked_glob_paths(self):
        self.assertEqual(extract_links("`platform/skills/*/SKILL.md`"), [])


class ExtractAdrRefsTest(unittest.TestCase):
    def test_finds_adr_references(self):
        self.assertEqual(extract_adr_refs("per ADR-0014 and ADR-0030"), {"ADR-0014", "ADR-0030"})

    def test_deduplicates(self):
        self.assertEqual(extract_adr_refs("ADR-0001 ADR-0001"), {"ADR-0001"})

    def test_ignores_malformed(self):
        self.assertEqual(extract_adr_refs("ADR-14 ADR- ADRXXXX"), set())


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python -m unittest discover -s scripts/ci -p "test_verify*.py" -v`
Expected: FAIL — `ModuleNotFoundError: No module named 'verify_agent_docs'`.

- [ ] **Step 3: Write the implementation**

```python
#!/usr/bin/env python3
"""Prove every claim in the Dhruv agent-doc set resolves to something real.

Agent instructions rot silently: platform/AGENTS.md pointed at `<module>/AGENTS.md`
files that never existed, CLAUDE-MD-TEMPLATES.md templated files nobody created, and
platform/adr/ held one file while DECISIONS.md held thirty-one. Each was found by a
human, months late. This makes each one a failing check on the PR that introduces it.

Four checks:
  1. registry  — the declared surface in AGENTS.md == disk, BOTH directions
  2. links     — every @path and relative markdown link in the agent docs resolves
  3. adr       — every ADR-NNNN referenced anywhere resolves to a header in DECISIONS.md
  4. orphans   — every platform/*.md is reachable from the bootstrap chain

Exit 0 clean, 1 on any violation. Pure stdlib.

Usage:
    python3 scripts/ci/verify_agent_docs.py
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

# Docs whose claims this script holds to account. Adding a doc here makes it verified;
# it does NOT make it reachable — check 4 governs that separately.
AGENT_DOCS = (
    "CLAUDE.md",
    "platform/AGENTS.md",
    "platform/PLATFORM.md",
    "platform/DESIGN-SYSTEM.md",
    "apps/finance/CLAUDE.md",
    "libs/core/CLAUDE.md",
    "apps/finance/data/CLAUDE.md",
)

# registry kind -> (directory, how to derive the name from a path)
REGISTRY_SOURCES = {
    "skills": ("platform/skills", lambda p: p.name),          # dir/SKILL.md
    "agents": (".claude/agents", lambda p: p.stem),           # <name>.md
    "commands": (".claude/commands", lambda p: p.stem),       # <name>.md
    "module-context": (None, None),                           # special-cased below
}

# platform/*.md exempt from the orphan check, with the reason each is exempt.
ORPHAN_EXEMPT = {
    "AGENTS.md": "the bootstrap root itself",
}

REGISTRY_BLOCK = re.compile(
    r"<!--\s*registry:([a-z-]+)\s*-->(.*?)<!--\s*/registry:\1\s*-->",
    re.DOTALL,
)
BACKTICKED = re.compile(r"`([^`]+)`")
AT_LINK = re.compile(r"(?<![\w`])@([A-Za-z0-9_./-]+\.[A-Za-z0-9]+)")
MD_LINK = re.compile(r"\[[^\]]*\]\(([^)]+)\)")
ADR_REF = re.compile(r"\bADR-(\d{4})\b")
# A backticked string is treated as a path only if it contains a slash AND ends in a file
# extension. The bootstrap list writes `platform/PLATFORM.md` rather than a markdown link,
# so without this every platform doc would look unreachable to check 4. Placeholders
# (`<name>`) and globs (`*`) are patterns, not files, and are excluded.
BACKTICK_PATH = re.compile(r"^[A-Za-z0-9_./-]+/[A-Za-z0-9_.-]+\.[A-Za-z0-9]+$")


def parse_registry(text: str) -> dict[str, set[str]]:
    """Pull `<!-- registry:kind -->` blocks out of a doc.

    Only backticked cells INSIDE a block count — prose elsewhere in the document is
    deliberately ignored, so an agent can mention a name in passing without the
    verifier treating it as a declaration.
    """
    out: dict[str, set[str]] = {}
    for kind, body in REGISTRY_BLOCK.findall(text):
        out[kind] = set(BACKTICKED.findall(body))
    return out


def check_registry(declared: set[str], actual: set[str], kind: str) -> list[str]:
    """Diff a declared set against disk in both directions."""
    errors = []
    for name in sorted(declared - actual):
        errors.append(f"[registry:{kind}] `{name}` is declared in AGENTS.md but does not exist on disk")
    for name in sorted(actual - declared):
        errors.append(f"[registry:{kind}] `{name}` exists on disk but is not declared in AGENTS.md")
    return errors


def extract_links(text: str) -> list[str]:
    """Repo-relative paths referenced by a doc. External URLs and anchors excluded.

    Three forms, because the docs use all three: `@path` (CLAUDE.md imports),
    `[text](path)` (markdown links), and plain backticked paths (the bootstrap list).
    """
    links = list(AT_LINK.findall(text))
    for target in MD_LINK.findall(text):
        target = target.strip()
        if target.startswith(("http://", "https://", "mailto:", "#")):
            continue
        links.append(target.split("#", 1)[0])
    for cell in BACKTICKED.findall(text):
        cell = cell.strip()
        if BACKTICK_PATH.match(cell):
            links.append(cell)
    return [link for link in links if link]


def extract_adr_refs(text: str) -> set[str]:
    return {f"ADR-{num}" for num in ADR_REF.findall(text)}


def _actual_module_context(root: Path) -> set[str]:
    """Every CLAUDE.md below the root, as repo-relative posix paths."""
    found = set()
    for path in root.rglob("CLAUDE.md"):
        rel = path.relative_to(root).as_posix()
        if rel.startswith((".claude/", "web/node_modules/")) or "/build/" in rel:
            continue
        found.add(rel)
    return found


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", default=".", type=Path)
    args = parser.parse_args()
    root: Path = args.root.resolve()

    errors: list[str] = []

    agents_path = root / "platform/AGENTS.md"
    if not agents_path.is_file():
        print("ERROR: platform/AGENTS.md not found", file=sys.stderr)
        return 1
    agents_text = agents_path.read_text(encoding="utf-8")
    declared = parse_registry(agents_text)

    # ── 1. registry ──────────────────────────────────────────────────────────
    for kind, (subdir, name_of) in REGISTRY_SOURCES.items():
        if kind == "module-context":
            actual = _actual_module_context(root)
        else:
            base = root / subdir
            actual = {name_of(p) for p in base.iterdir() if p.is_dir() or p.suffix == ".md"} \
                if base.is_dir() else set()
        errors += check_registry(declared.get(kind, set()), actual, kind)

    # ── 2. links ─────────────────────────────────────────────────────────────
    present_docs = [d for d in AGENT_DOCS if (root / d).is_file()]
    for doc in present_docs:
        text = (root / doc).read_text(encoding="utf-8")
        for link in extract_links(text):
            if link.startswith("/"):
                continue  # absolute: out of scope, not a repo path
            target = (root / link) if (root / link).exists() else ((root / doc).parent / link)
            if not target.exists():
                errors.append(f"[links] {doc} references `{link}` which does not exist")

    # ── 3. adr ───────────────────────────────────────────────────────────────
    decisions = (root / "platform/DECISIONS.md")
    if decisions.is_file():
        dtext = decisions.read_text(encoding="utf-8")
        real_adrs = {f"ADR-{n}" for n in re.findall(r"^##\s+ADR-(\d{4})", dtext, re.MULTILINE)}
        for doc in present_docs:
            text = (root / doc).read_text(encoding="utf-8")
            for ref in sorted(extract_adr_refs(text) - real_adrs):
                errors.append(f"[adr] {doc} references {ref}, which has no header in DECISIONS.md")

    # ── 4. orphans ───────────────────────────────────────────────────────────
    reachable = set()
    for doc in present_docs:
        text = (root / doc).read_text(encoding="utf-8")
        for link in extract_links(text):
            reachable.add(link.lstrip("./"))
    for md in sorted((root / "platform").glob("*.md")):
        rel = md.relative_to(root).as_posix()
        if md.name in ORPHAN_EXEMPT:
            continue
        if rel not in reachable and md.name not in {Path(d).name for d in present_docs}:
            errors.append(
                f"[orphans] {rel} is not reachable from the bootstrap chain — "
                f"reference it from AGENTS.md or retire it"
            )

    for err in errors:
        print(err, file=sys.stderr)
    if errors:
        print(f"\n{len(errors)} agent-doc violation(s).", file=sys.stderr)
        return 1
    print(f"Agent docs verified: {len(present_docs)} docs, registry consistent.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `python -m unittest discover -s scripts/ci -p "test_*.py" -v`
Expected: PASS — the 19 new cases plus every existing case.

- [ ] **Step 5: Run it against the real repo (expected to FAIL loudly)**

Run: `python scripts/ci/verify_agent_docs.py`
Expected: **non-zero exit, listing the known drift** — undeclared skills/agents/commands, the orphaned `Implementation.md`/`RUNBOOK.md`/`CLAUDE-MD-TEMPLATES.md`/`TELEGRAM_BOT.md`. That output is the to-do list for Tasks 2–4. Capture it:

```bash
python scripts/ci/verify_agent_docs.py 2> agent-doc-violations.txt; cat agent-doc-violations.txt
```

Do **not** fix them by weakening the checks. Delete the scratch file before committing.

- [ ] **Step 6: Commit**

```bash
git add scripts/ci/verify_agent_docs.py scripts/ci/test_verify_agent_docs.py
git commit -m "docs: add agent-doc verifier (registry, links, ADR refs, orphans)"
```

---

## Task 2: Rewrite `platform/AGENTS.md` as the protocol

**Files:**
- Modify: `platform/AGENTS.md`

**Interfaces:**
- Consumes: nothing.
- Produces: the four `<!-- registry:* -->` blocks Task 1's check 1 parses. Task 3 adds rows to `module-context`; Task 4 removes retired docs from the reading order.

The rewrite keeps every existing hard rule verbatim — this task adds structure and the registry; it does not relax architecture rules.

- [ ] **Step 1: Replace the session-bootstrap section**

The current step 6 (`<module>/AGENTS.md (if present)`) is the fiction. Replace the whole reading list with:

```markdown
## Session bootstrap (read in order)
1. `platform/PLATFORM.md` — architecture, source of truth
2. `platform/DECISIONS.md` — why things are the way they are
3. `platform/DESIGN-SYSTEM.md` — the design contract, binding for **every** app
4. `platform/versions.json` — module compatibility
5. `platform/contracts/` — the contracts you must not break
6. The nearest `CLAUDE.md` to the files you are touching (see the module-context
   registry below — those files are the complete set; there are no others)
7. The task/handoff prompt
```

- [ ] **Step 2: Add the task lifecycle**

```markdown
## The loop (every task, no exceptions)

1. **Classify** the task against the matrix below.
2. **Load** the bootstrap docs plus the nearest `CLAUDE.md`.
3. **Invoke the required skill** before writing anything. If the matrix names one, it is
   not optional — skills encode decisions already made, and skipping one re-litigates them.
4. **Work.** Use a worktree when the change spans more than one module or will outlive a
   single session (`git worktree add ../dhruv-<topic> -b <branch> origin/develop`).
5. **Prove** it with the gate named in the matrix. Paste the command output — a claim that
   something passes is not evidence that it did.
6. **Report** what you ran, what passed, and what you did not do.

### Task matrix

| Task type | Required skill | Subagent | Gate before done |
|---|---|---|---|
| New feature module | `dhruv-feature-scaffold` | — | `/dhruv-audit` + `regressionCheck` |
| New Room entity / data layer | `dhruv-room-entity` | — | `regressionCheck` |
| New Compose screen | `dhruv-compose-screen` | `dhruv-compose-ui-designer` | `/dhruv-ui-review` |
| Bug fix / behaviour change | `test-driven-development` | — | `regressionCheck` |
| Dependency or module-graph change | — | `dhruv-architecture-guardian` | `/dhruv-boundaries` |
| Off-device data, AI, vault, new permission | `security-and-hardening` | `dhruv-security-compliance-reviewer` | `/dhruv-security` |
| Raising test coverage | `test-driven-development` | `dhruv-coverage-booster` | `regressionCheck` |
| CI / release / workflow | `ci-cd-and-automation` | — | `verify_agent_docs.py` + YAML parse |
| Docs / ADR | `documentation-and-adrs` | — | `verify_agent_docs.py` |
| Pre-merge, any change | — | `dhruv-module-auditor` | `/dhruv-pre-merge` |
```

- [ ] **Step 3: Add the surface registry**

Every name below must exist on disk — check 1 fails the build otherwise. **Do not add a row for something you are about to build; add it in the same commit that creates the file.**

```markdown
## Agent surface registry

> Verified by `scripts/ci/verify_agent_docs.py` on every PR. Declared-but-missing and
> on-disk-but-undeclared both fail. Nothing enters a table before the file exists.

<!-- registry:module-context -->
| File | Scope |
|---|---|
| `CLAUDE.md` | Platform-wide rules |
| `apps/finance/CLAUDE.md` | Finance app: modules, flags, conventions |
| `libs/core/CLAUDE.md` | Component library law, dependency-free rule |
| `apps/finance/data/CLAUDE.md` | Repository-only access, money precision |
<!-- /registry:module-context -->

<!-- registry:skills -->
| Skill | Use for |
|---|---|
| `dhruv-feature-scaffold` | New feature module |
| `dhruv-room-entity` | New Room entity / data layer |
| `dhruv-compose-screen` | New Compose screen |
| `dhruv-module-audit` | Pre-merge module check |
| `dhruv-release` | Version bump / release |
<!-- /registry:skills -->

<!-- registry:agents -->
| Subagent | Use for |
|---|---|
| `dhruv-architecture-guardian` | Module boundary / ArchUnit contract |
| `dhruv-compose-ui-designer` | Building Compose UI |
| `dhruv-coverage-booster` | Raising JVM unit-test coverage |
| `dhruv-module-auditor` | Pre-merge PASS/FAIL verdict |
| `dhruv-security-compliance-reviewer` | Security + DPDP review |
| `dhruv-ui-ux-reviewer` | UI/UX + accessibility review |
<!-- /registry:agents -->

<!-- registry:commands -->
| Command | Runs |
|---|---|
| `dhruv-audit` | Module compliance audit |
| `dhruv-boundaries` | Boundary / dependency check |
| `dhruv-coverage` | Coverage improvement pass |
| `dhruv-pre-merge` | All three reviewers + regression suite |
| `dhruv-security` | Security + DPDP review |
| `dhruv-ui-review` | UI/UX + accessibility review |
<!-- /registry:commands -->
```

- [ ] **Step 4: Keep the hard rules and definition-of-done sections unchanged**

Both sections stay verbatim. If a rule's wording changes here, it diverges from `PLATFORM.md` — the exact split-brain this plan exists to prevent.

- [ ] **Step 5: Verify (registry should now pass; orphans will still fail)**

```bash
python scripts/ci/verify_agent_docs.py
```
Expected: `[registry:*]` violations **gone**; `[orphans]` violations **remain** for the four genesis-era docs. Task 4 clears those.

- [ ] **Step 6: Commit**

```bash
git add platform/AGENTS.md
git commit -m "docs: rewrite AGENTS.md as the agent protocol with a verified surface registry"
```

---

## Task 3: Module-context files where the rules are real

**Files:**
- Create: `libs/core/CLAUDE.md`
- Create: `apps/finance/data/CLAUDE.md`

These two modules have rules that are non-derivable from the code and repeatedly matter: `:libs:core` owns the component library and must stay internally dependency-free; `:apps:finance:data` is Repository-only and money-precision-critical.

- [ ] **Step 1: Write `libs/core/CLAUDE.md`**

```markdown
# :libs:core

Pure shared library. Consumed by every app and feature module. **Depends on nothing
internal** — no `:apps:*`, no `:libs:settings`. ArchUnit enforces this; a new internal
dependency here is an architecture change, not a convenience.

## What lives here
- `ui/components/` — **the** design-system component library (`Nx*`, charts, states, overlays).
  Every reusable visual belongs here, never in a feature module.
- `ui/theme/` — `DhruvNextColors`, `DhruvNextType`, `DhruvNextSpacing`, `DhruvNextRadii`,
  `DhruvBrand` (theme-invariant brand chrome).
- `ui/FeatureHost.kt` — fault isolation: wraps every route, renders `FeatureDisabledCard`
  on a flag-off and `FeatureErrorCard` on a throw. Never a blank crash.
- `navigation/` — `NavTarget`, `BackContract` (`resolveBackAction` — never re-derive back
  precedence inline).
- `observability/` — `CrashReporter`, `PerformanceTracer`, `FeatureViewModel`.
- `flags/`, `format/`, `security/`, `integrity/`, `domain/`.

## Rules
- **`platform/DESIGN-SYSTEM.md` is binding.** Read it before touching `ui/`.
- **Nothing enters the §5.1 component table before the code exists.** A component library
  that documents unbuilt components is how screens got written against fiction once already
  (ADR-0030).
- Closing a §5.3 gap means **extending the existing component**, never adding a parallel one.
- No feature-specific logic. If it only makes sense for one screen, it belongs in that feature.
- Compose + Material3 only.
```

- [ ] **Step 2: Write `apps/finance/data/CLAUDE.md`**

```markdown
# :apps:finance:data

Shared data layer for the Finance app. Feature modules reach it **through repositories
only** — never a DAO, never a DTO, never a Retrofit service (ArchUnit enforced).

## What lives here
- Room `AppDatabase`, entities, DAOs — calculator history, currency cache.
- `tracker/` — Supabase-backed tracker domain (ADR-0029): `net/`, `auth/`, `dto/`, `model/`,
  `mapper/`, `repo/`.
- `api/CurrencyApiClient`, `GeminiRepository`, `CurrencyFormatter`.

## Rules
- **Money is integer paise (`Long`).** Never `Double`, never `Float`. `BigDecimal` is for
  fractional *calculation* domains (the calculators), not for stored amounts (ADR-0014 §4).
- **Valuations are append-only** — insert a new timestamped row, never update. The database
  enforces it: `valuations` has SELECT + INSERT policies only (ADR-0029).
- **Nothing reaches PostgREST except through the consent-gated client.** `ConsentInterceptor`
  short-circuits before dispatch when sync consent is off; no second PostgREST-capable client
  may be constructed anywhere (ADR-0029 §2).
- **Tokens live only in `EncryptedDataStore`** — never plaintext `SharedPreferences`.
- BuildConfig values are injected from `:apps:finance:app` via Koin — this module never reads
  app `BuildConfig` directly.
- Category/sector enums persisted as TEXT are **append-only**: never rename a shipped constant.
```

- [ ] **Step 3: Verify the registry still matches**

```bash
python scripts/ci/verify_agent_docs.py
```
Expected: no `[registry:module-context]` violations — the two new files were already declared in Task 2 Step 3, and now exist.

- [ ] **Step 4: Commit**

```bash
git add libs/core/CLAUDE.md apps/finance/data/CLAUDE.md
git commit -m "docs: add module context for :libs:core and :apps:finance:data"
```

---

## Task 4: Retire the genesis-era docs

**Files:**
- Delete: `platform/Implementation.md`, `platform/RUNBOOK.md`, `platform/CLAUDE-MD-TEMPLATES.md`, `platform/adr/0010-initial-repo-state.md`
- Modify: `README.md`, `docs/PRD.md`

These are the orphans check 4 reports. All four are genesis-era: `RUNBOOK.md` Phase 0 is "rename the GitHub repo"; `Implementation.md` still sequences "Phase 4 Tools". Live planning lives in `apps/finance/docs/superpowers/plans/`. An agent that reads them gets stale marching orders — which is the whole failure mode.

**`platform/TELEGRAM_BOT.md` is NOT retired.** It documents real, partially-built work. It is not agent infra, so Task 4 Step 3 gives it a proper home instead.

- [ ] **Step 1: Confirm nothing salvageable is lost**

```bash
grep -n "Phase" platform/Implementation.md | head -20
grep -c "" platform/RUNBOOK.md platform/CLAUDE-MD-TEMPLATES.md
```
Read both. Anything still true and not already in `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` gets folded there **before** deletion. Expected: nothing — both predate the tracker pivot (ADR-0014) and the design-v1 import.

- [ ] **Step 2: Redirect inbound references**

| File | Line | Change |
|---|---|---|
| `README.md` | 20 | Drop the `Implementation.md` bullet; point at `apps/finance/docs/superpowers/plans/` |
| `README.md` | 97–98 | Remove the four retired names from the tree diagram |
| `README.md` | 301 | Keep the `TELEGRAM_BOT.md` link, repointed per Step 3 |
| `README.md` | 464 | Replace "From `platform/Implementation.md` (Phase 0–7)" with the design-v1 plan |
| `docs/PRD.md` | 46 | Repoint **Implementation Plan** row at the design-v1 plan |
| `docs/PRD.md` | 53 | Delete the **Runbook** row |

`docs/PRD.md` uses absolute `file:///d:/...` URLs — replace with repo-relative paths while there; absolute local paths break for every other clone.

- [ ] **Step 3: Relocate `TELEGRAM_BOT.md` out of `platform/`**

`platform/` is bootstrap territory; a bot plan sitting there is what check 4 flags. Move it:

```bash
git mv platform/TELEGRAM_BOT.md docs/TELEGRAM_BOT.md
```
Then fix the `README.md:301` link to `docs/TELEGRAM_BOT.md`.

- [ ] **Step 4: Delete**

```bash
git rm platform/Implementation.md platform/RUNBOOK.md platform/CLAUDE-MD-TEMPLATES.md
git rm platform/adr/0010-initial-repo-state.md
rmdir platform/adr 2>/dev/null || true
```

`platform/adr/` goes because `DECISIONS.md` is the register — its own header says "split into individual files later **if useful**", and one file against thirty-one says it was not.

- [ ] **Step 5: Verify the verifier is now clean**

```bash
python scripts/ci/verify_agent_docs.py
```
Expected: **exit 0**, `Agent docs verified: N docs, registry consistent.` This is the first moment the whole check passes.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "docs: retire genesis-era platform docs superseded by the design-v1 plans"
```

---

## Task 5: Wire the verifier into the gates

**Files:**
- Modify: `.github/workflows/ci.yml`
- Modify: `scripts/hooks/pre-push`
- Modify: `.gitignore`

- [ ] **Step 1: Add to Gate 1**

In `static-analysis`, before the ktlint steps (it is the fastest check — fail early):

```yaml
      # Proves the agent-doc set is true: declared surface == disk, links resolve,
      # ADR references exist, no orphaned platform/ doc. See ADR-0036.
      - name: Verify agent docs
        run: python3 scripts/ci/verify_agent_docs.py
```

Gate 1 is `pull_request`-only (ADR-0026), which is correct — this catches drift on the PR that introduces it.

- [ ] **Step 2: Add to the pre-push hook**

`scripts/hooks/pre-push` already runs deterministic zero-token checks. Append before its final exit:

```sh
# Agent-doc verification — deterministic, sub-second, no AI tokens (same rationale as the
# module audit above). Catches doc drift before it reaches CI.
if command -v python3 >/dev/null 2>&1; then
  if ! python3 scripts/ci/verify_agent_docs.py; then
    echo "Dhruv: agent-doc verification failed — fix the violations above or amend the docs."
    FAIL=$((FAIL + 1))
  fi
fi
```

Match the existing script's `PASS`/`FAIL` accumulator convention — read the tail of the file first and follow whatever it actually does rather than assuming.

- [ ] **Step 3: Commit `.claude/worktrees/` to `.gitignore`**

It is currently ignored via `.git/info/exclude`, which is **local-only and uncommitted** — a fresh clone or a second machine will see three stale agent worktrees as untracked noise. Add to `.gitignore` beside the other tooling entries:

```gitignore
# Agent worktrees (created by subagent isolation; never committed)
.claude/worktrees/
```

- [ ] **Step 4: Verify**

```bash
python -c "import yaml; yaml.safe_load(open('.github/workflows/ci.yml',encoding='utf-8')); print('YAML OK')"
sh -n scripts/hooks/pre-push && echo "hook syntax OK"
git check-ignore -v .claude/worktrees/ | grep -q '^.gitignore' && echo "worktrees ignored via committed .gitignore"
```

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/ci.yml scripts/hooks/pre-push .gitignore
git commit -m "chore: run agent-doc verifier in Gate 1 and pre-push"
```

---

## Task 6: ADR-0036

**Files:**
- Modify: `platform/DECISIONS.md`

- [ ] **Step 1: Append the ADR**

```markdown
---

## ADR-0036 — Agent instructions are machine-verified, not just written
**Context.** The agent-doc set drifted repeatedly and silently. `AGENTS.md`'s session
bootstrap told every agent to read `<module>/AGENTS.md`; no such file has ever existed.
`CLAUDE-MD-TEMPLATES.md` templated module files nobody created. `platform/adr/` held one
file while `DECISIONS.md` held thirty-one. `Implementation.md` and `RUNBOOK.md` still
sequenced a pre-tracker-pivot build order — `RUNBOOK.md` Phase 0 was "rename the GitHub
repo". Each was found by a human, months late, and each had already misdirected work.
ADR-0030 caught the same class of failure in the design docs (a component library that
documented components nobody had built) and noted the prevention would have been cheap —
"one grep loop" — but that check was written for one document and never generalized.
Six subagents and six slash commands, meanwhile, were referenced by **zero** project docs.
**Decision.** `platform/AGENTS.md` becomes the single agent protocol: a task lifecycle
(classify → load → required skill → work → prove → report), a task-type matrix binding each
kind of work to its required skill, subagent and gate, and a **surface registry** declaring
every module-context file, project skill, subagent and slash command.
`scripts/ci/verify_agent_docs.py` then proves the whole set true — registry versus disk in
**both** directions, every link resolving, every `ADR-NNNN` reference resolving to a real
header, and no `platform/*.md` unreachable from the bootstrap chain. It runs in Gate 1 and
in the pre-push hook. The four genesis-era docs are retired per the ADR-0030 precedent
(fold, redirect, `git rm`).
**Why.** Documentation that nothing checks is documentation that will be wrong, and wrong
agent instructions are worse than none — an agent follows them confidently. Verifying both
directions is what makes the registry hold: declared-but-missing catches fiction, and
on-disk-but-undeclared catches the opposite failure this repo actually had, where working
subagents were invisible to every doc. A declared registry is used rather than fuzzy
grepping for names in prose because backticked text is ambiguous; an explicit block is
unambiguous and cheap to parse.
**Consequences.** Adding a skill, subagent, slash command or module `CLAUDE.md` now
**requires** a registry row in the same commit — CI fails otherwise. That is the intended
cost: the registry cannot drift because drifting fails the build. Any new `platform/*.md`
must be referenced from the bootstrap chain or it is reported as an orphan, which makes
"drop a doc in `platform/` and forget it" impossible. `platform/adr/` is removed;
`DECISIONS.md` is the register, and this ADR settles the half-finished split its own header
proposed. `TELEGRAM_BOT.md` moves to `docs/` — it is real work, but not agent infrastructure,
and `platform/` is bootstrap territory. The verifier checks that claims **resolve**, not that
prose is accurate; a doc can still be misleading in ways no script catches, so this lowers
the drift rate, it does not eliminate review.
```

- [ ] **Step 2: Verify the ADR check accepts its own number**

```bash
python scripts/ci/verify_agent_docs.py
```
Expected: exit 0. If ADR-0036 is referenced from `AGENTS.md` or `ci.yml` comments, check 3 now resolves it.

- [ ] **Step 3: Commit**

```bash
git add platform/DECISIONS.md
git commit -m "docs: ADR-0036 machine-verified agent instructions"
```

---

## Verification

### Local

- [ ] `python -m unittest discover -s scripts/ci -p "test_*.py"` → all pass, including 19 new
- [ ] `python scripts/ci/verify_agent_docs.py` → **exit 0**
- [ ] Break it deliberately and confirm each check fires:
  - [ ] Add a fake row `| \`ghost-skill\` |` to the skills registry → `[registry:skills]` violation
  - [ ] Delete a registry row for a real agent → `[registry:agents]` "not declared" violation
  - [ ] Add `[x](platform/NOPE.md)` to `CLAUDE.md` → `[links]` violation
  - [ ] Reference `ADR-9999` in `AGENTS.md` → `[adr]` violation
  - [ ] `touch platform/STRAY.md` → `[orphans]` violation
  - [ ] Revert all five
- [ ] `sh -n scripts/hooks/pre-push` → syntax OK
- [ ] YAML parse of `ci.yml` → OK

### On GitHub

- [ ] PR runs Gate 1 → "Verify agent docs" step green
- [ ] Push a commit adding an undeclared file to `.claude/agents/` → Gate 1 **fails** with the registry violation. Revert.
- [ ] `git grep -n "Implementation.md\|RUNBOOK.md\|CLAUDE-MD-TEMPLATES" -- '*.md'` → no hits outside `DECISIONS.md`'s historical ADR text

### Rollback

Everything is docs plus one script plus two gate wirings. `git revert` of the merge restores the previous state; no build output or runtime artifact depends on any of it. The retired docs return with the revert — they are deleted, not rewritten.

---

## Deliberately not in scope

- **Hook-blocked enforcement** (`PreToolUse` refusing non-conforming edits) — rejected in design: brittle, false positives stall real work, and the hook logic becomes infrastructure that itself drifts.
- **Module `CLAUDE.md` for every module** — rejected in design: ~15 more files is the largest new drift surface, and most modules have no rule that is not derivable from the code.
- **Consolidating `settings.json`'s ~40 accreted permission entries** — real cruft (many are one-off Gradle invocations from single sessions), but it is a separate concern with its own tool (`/fewer-permission-prompts`) and does not affect correctness. Worth doing; not here.
- **Verifying prose accuracy.** The verifier proves references resolve. A doc can still say something untrue in ways no script detects. This reduces drift; it does not replace reading.
