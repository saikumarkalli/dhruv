#!/usr/bin/env python3
"""Fail CI on dangling documentation references.

Two checks, both cheap and both guarding failure modes this repo has actually hit:

1. **Broken relative links.** Every `[text](path)` in a tracked markdown file must resolve to a
   file or directory that exists. This is what catches a doc that points at something deleted or
   moved — the failure ADR-0030 diagnosed once for the design docs and the 2026-08-15 doc-retirement
   note diagnosed again for the P1-P6 / R0-R11 specs.

2. **References to retired documents.** `platform/DECISIONS.md`'s doc-retirement note states that
   any new reference to a `p1-` / `p2-` / `r3-`...`r9-` / `master-roadmap-` path is a dangling
   pointer to a deleted file and must not be resurrected. Those names are matched by pattern, not
   by resolution, because the whole point is that the target no longer exists to resolve against.

`platform/DECISIONS.md` is exempt from check 2 by design: the register is append-only, and its ADR
bodies deliberately keep citing paths that were accurate when each ADR was written. That exemption
is stated in the note itself, so it is honoured here rather than being worked around.

Deliberately NOT checked: `http(s)://` and `mailto:` targets (network calls in CI are slow and
flaky), and `file:///` absolute URLs (machine-specific by construction -- they are a separate
problem, reported as a warning so they are visible without failing the build).

ASCII-only output on purpose: `scripts/db/gen_schema_docs.py` crashes on Windows consoles printing
a check-mark under cp1252, and there is no reason to repeat that here.

Usage:
    python scripts/ci/doc_link_check.py            # check the whole tree
    python scripts/ci/doc_link_check.py --warn     # report but always exit 0
    python scripts/ci/doc_link_check.py path/a.md  # check only the named files
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]

# Directories that are never ours to police.
SKIP_DIRS = {
    ".git",
    "node_modules",
    "build",
    ".gradle",
    ".idea",
    "__pycache__",
    "worktrees",  # .claude/worktrees -- stale agent copies, not the live tree
    ".superpowers",  # agent scratch: in-flight task briefs, not published documentation
}

# `[text](target)` -- target captured up to the closing paren, angle-bracket form supported.
LINK_RE = re.compile(r"\[[^\]]*\]\(\s*<?([^)\s>]+)>?\s*(?:\"[^\"]*\")?\s*\)")

# Inline code spans hold illustrations, not references -- a checklist proving a link verifier fires
# legitimately contains `[x](platform/NOPE.md)`. Stripped before links are extracted.
INLINE_CODE_RE = re.compile(r"`[^`]*`")

# GitHub renders `../../releases` and friends relative to the *repository*, not the filesystem.
# They are correct links on github.com and unresolvable on disk, so they are not our business.
GITHUB_SHORTCUT_RE = re.compile(
    r"^(?:\.\./)+(?:releases|issues|pulls|wiki|discussions|actions|security|commits|tree|blob|graphs|compare)(?:/|$)",
    re.IGNORECASE,
)

# Retired spec families (2026-08-15 doc-retirement note in platform/DECISIONS.md).
RETIRED_RE = re.compile(
    r"\b(?:"
    r"p[1-6]-[a-z0-9-]+"
    r"|r(?:[0-9]|1[01])-[a-z0-9-]+"
    r"|master-roadmap-[a-z0-9-]+"
    r")\.md\b",
    re.IGNORECASE,
)

# The register is append-only and its citations are historical records, not live pointers.
RETIRED_EXEMPT = {Path("platform/DECISIONS.md")}


def iter_markdown(paths: list[str] | None) -> list[Path]:
    if paths:
        return [Path(p).resolve() for p in paths]
    found: list[Path] = []
    for path in REPO_ROOT.rglob("*.md"):
        if any(part in SKIP_DIRS for part in path.parts):
            continue
        found.append(path)
    return sorted(found)


def check_file(path: Path) -> tuple[list[str], list[str]]:
    """Return (errors, warnings) for one markdown file."""
    errors: list[str] = []
    warnings: list[str] = []
    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return ([f"{_rel(path)}: not valid UTF-8"], [])

    in_fence = False
    fence_marker = ""
    for lineno, line in enumerate(text.splitlines(), start=1):
        # Fenced code blocks are examples, not references -- a doc explaining this very checker
        # legitimately contains links that do not resolve. Track ``` and ~~~ fences, matching the
        # closing marker to the opening one so a nested fence does not end the block early.
        stripped = line.lstrip()
        if not in_fence and (stripped.startswith("```") or stripped.startswith("~~~")):
            in_fence = True
            fence_marker = stripped[:3]
            continue
        if in_fence:
            if stripped.startswith(fence_marker):
                in_fence = False
            continue

        scannable = INLINE_CODE_RE.sub("", line)
        for target in LINK_RE.findall(scannable):
            if target.startswith(("http://", "https://", "mailto:", "#")):
                continue
            if GITHUB_SHORTCUT_RE.match(target):
                continue
            if target.startswith("file://"):
                warnings.append(
                    f"{_rel(path)}:{lineno}: absolute file:// link is machine-specific: {target}"
                )
                continue
            # Strip a trailing anchor; we verify the file exists, not the heading.
            clean = target.split("#", 1)[0]
            if not clean:
                continue
            resolved = (path.parent / clean).resolve()
            if not resolved.exists():
                errors.append(f"{_rel(path)}:{lineno}: link target does not exist: {target}")

        if _rel(path) not in RETIRED_EXEMPT:
            for hit in RETIRED_RE.findall(line):
                errors.append(
                    f"{_rel(path)}:{lineno}: reference to a retired spec ({hit}). "
                    f"Those files were deleted 2026-08-15 -- extend the design-v1 plan instead of "
                    f"resurrecting them (see platform/DECISIONS.md's doc-retirement note)."
                )

    return (errors, warnings)


def _rel(path: Path) -> Path:
    try:
        return path.resolve().relative_to(REPO_ROOT)
    except ValueError:
        return path


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paths", nargs="*", help="markdown files to check (default: whole tree)")
    parser.add_argument(
        "--warn",
        action="store_true",
        help="report problems but always exit 0 (for a first non-blocking rollout)",
    )
    args = parser.parse_args()

    all_errors: list[str] = []
    all_warnings: list[str] = []
    files = iter_markdown(args.paths)

    for path in files:
        errors, warnings = check_file(path)
        all_errors.extend(errors)
        all_warnings.extend(warnings)

    for warning in all_warnings:
        print(f"WARN  {warning}")

    if not all_errors:
        print(f"OK: {len(files)} markdown files checked, no dangling references.")
        return 0

    for error in all_errors:
        print(f"FAIL  {error}")
    print(f"\n{len(all_errors)} dangling reference(s) across {len(files)} markdown files.")

    if args.warn:
        print("(--warn set: not failing the build)")
        return 0
    return 1


if __name__ == "__main__":
    sys.exit(main())