#!/usr/bin/env python3
"""Keep every generated version location in sync with platform/versions.json.

Protocol: platform/VERSIONING.md. `platform/versions.json` (T1) is the only place
a version is authored; the files below (T2) are generated from it, and a third
group (T3) is validated for coherence without being rewritten.

    T2 GENERATED                      mirrors
      gradle.properties               apps.finance.version / .buildNumber
      web/package.json                web.finance.version
      web/src/shared/version.ts       web.finance.version
      README.md (between markers)     every active component

    T3 CHECKED
      requiresCore / requiresSettings satisfied by libs.*
      feature-flag minVersion <= its own platform's version
      component tag patterns are unique
      CHANGELOG has a heading for each component's current version

Usage
    python scripts/ci/sync_versions.py --check              exit 1 on any drift
    python scripts/ci/sync_versions.py --check --format=github
    python scripts/ci/sync_versions.py --write              regenerate T2

--write is deterministic: running it twice produces no second diff. That property
is what makes --check a valid merge gate.

Writes are surgical — a value is rewritten in place, never the whole file — so
web/package.json keeps its key order and formatting, and the README keeps
everything outside its markers.

Pure stdlib, like bump_version.py. Exit codes: 0 clean, 1 drift, 2 bad config.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
from typing import Callable

VERSIONS_JSON = "platform/versions.json"
README_START = "<!-- version:start -->"
README_END = "<!-- version:end -->"

SEMVER_RE = re.compile(r"^\d+\.\d+\.\d+$")
INACTIVE = ("planned", "future")


# ── Findings ────────────────────────────────────────────────────────────────

class Finding:
    def __init__(self, check: str, path: str, message: str, fixable: bool):
        self.check = check
        self.path = path
        self.message = message
        self.fixable = fixable

    def render(self, fmt: str) -> str:
        if fmt == "github":
            return f"::error file={self.path}::[{self.check}] {self.message}"
        fix = "" if self.fixable else "   (not auto-fixable)"
        return f"  DRIFT  {self.path}\n         [{self.check}] {self.message}{fix}"


# ── Registry helpers ────────────────────────────────────────────────────────

def load_registry(path: str) -> dict:
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except (OSError, json.JSONDecodeError) as exc:
        print(f"ERROR: cannot read {path}: {exc}", file=sys.stderr)
        raise SystemExit(2)


def active_components(data: dict):
    """Yield (key, meta) for every component that actually ships.

    Key is the dotted registry path, e.g. "apps.finance" / "web.finance" — the
    same string the Release-Component / Release-Skip trailers use.
    """
    for group in ("apps", "web"):
        for name, meta in data.get(group, {}).items():
            if meta.get("status") in INACTIVE:
                continue
            yield f"{group}.{name}", meta


def version_of(data: dict, key: str) -> str | None:
    group, _, name = key.partition(".")
    return data.get(group, {}).get(name, {}).get("version")


def parse_constraint(spec: str) -> tuple[int, int, int]:
    """Only `>=X.Y.Z` is supported — the only form this repo uses.

    Anything else raises rather than silently passing: a constraint nobody can
    parse is worse than no constraint, which is how requiresCore became
    decorative in the first place.
    """
    m = re.fullmatch(r">=\s*(\d+)\.(\d+)\.(\d+)", spec.strip())
    if not m:
        raise ValueError(f"unsupported constraint {spec!r}; only '>=X.Y.Z' is supported")
    return tuple(int(g) for g in m.groups())  # type: ignore[return-value]


def as_tuple(version: str) -> tuple[int, int, int]:
    return tuple(int(p) for p in version.split("."))  # type: ignore[return-value]


# ── T2: generated targets ───────────────────────────────────────────────────
#
# Each target is (path, read_current, render_expected, write). `write` returns
# the new file text, or None when it cannot fix it (e.g. missing README markers,
# which must be placed by a human — silently inserting a block into a
# hand-written README is presumptuous).

def _read_text(path: str) -> str | None:
    """Read with line endings normalised to \\n so the checks stay ending-agnostic."""
    try:
        with open(path, encoding="utf-8", newline="") as f:
            return f.read().replace("\r\n", "\n")
    except OSError:
        return None


def _newline_of(path: str) -> str:
    """The file's existing line ending, so --write preserves it.

    Without this, rewriting one value in a CRLF file (which is what
    gradle.properties is in a Windows working tree) rewrites EVERY line and the
    diff becomes unreviewable — the change is correct but nobody can see it.
    A file that does not exist yet gets \\n.
    """
    try:
        with open(path, "rb") as f:
            return "\r\n" if b"\r\n" in f.read(65536) else "\n"
    except OSError:
        return "\n"


def _write_text(path: str, text: str) -> None:
    newline = _newline_of(path)
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="") as f:
        f.write(text.replace("\n", newline))


def check_gradle_properties(data: dict, findings: list, fix: bool) -> None:
    path = "gradle.properties"
    meta = data.get("apps", {}).get("finance", {})
    want_name, want_code = meta.get("version"), meta.get("buildNumber")
    text = _read_text(path)
    if text is None:
        findings.append(Finding("gradle", path, "file missing", False))
        return

    out = text
    for key, want in (("VERSION_NAME", want_name), ("VERSION_CODE", want_code)):
        m = re.search(rf"^{key}=(.*)$", out, re.M)
        if not m:
            findings.append(Finding("gradle", path, f"{key} not present", False))
            continue
        have = m.group(1).strip()
        if have != str(want):
            findings.append(Finding(
                "gradle", path,
                f"{key}={have} but versions.json apps.finance says {want}", True))
            out = re.sub(rf"^{key}=.*$", f"{key}={want}", out, count=1, flags=re.M)

    if fix and out != text:
        _write_text(path, out)


def check_web_package_json(data: dict, findings: list, fix: bool) -> None:
    path = "web/package.json"
    want = data.get("web", {}).get("finance", {}).get("version")
    text = _read_text(path)
    if text is None:
        findings.append(Finding("web-pkg", path, "file missing", False))
        return

    # Surgical: rewrite only the top-level "version" value, so key order,
    # indentation and trailing newline survive untouched.
    m = re.search(r'^(\s*"version"\s*:\s*")([^"]*)(")', text, re.M)
    if not m:
        findings.append(Finding("web-pkg", path, 'no top-level "version" key', False))
        return
    if m.group(2) != want:
        findings.append(Finding(
            "web-pkg", path,
            f'version="{m.group(2)}" but versions.json web.finance says {want}', True))
        if fix:
            _write_text(path, text[:m.start(2)] + str(want) + text[m.end(2):])


WEB_VERSION_TS = '''// GENERATED by scripts/ci/sync_versions.py from platform/versions.json.
// Do not edit by hand — see platform/VERSIONING.md.
//
// The web app versions INDEPENDENTLY of the Android app (VERSIONING.md D1), so
// this is web.finance's own line, not apps.finance's.
export const APP_VERSION = "{version}";
'''


def check_web_version_ts(data: dict, findings: list, fix: bool) -> None:
    path = "web/src/shared/version.ts"
    want = data.get("web", {}).get("finance", {}).get("version")
    expected = WEB_VERSION_TS.format(version=want)
    text = _read_text(path)
    if text is None:
        findings.append(Finding(
            "web-version-ts", path,
            "missing — the web app exposes no version anywhere, so a web bug "
            "report cannot state one", True))
    elif text != expected:
        findings.append(Finding(
            "web-version-ts", path,
            f"does not match versions.json web.finance ({want})", True))
    else:
        return
    if fix:
        _write_text(path, expected)


def render_readme_block(data: dict) -> str:
    rows = [
        "| Component | Version |",
        "| --- | --- |",
    ]
    for key, meta in active_components(data):
        rows.append(f"| `{key}` | `{meta['version']}` |")
    body = "\n".join(rows)
    return (
        f"{README_START}\n"
        f"<!-- GENERATED by scripts/ci/sync_versions.py — do not edit by hand. -->\n"
        f"{body}\n"
        f"{README_END}"
    )


def check_readme(data: dict, findings: list, fix: bool) -> None:
    path = "README.md"
    text = _read_text(path)
    if text is None:
        findings.append(Finding("readme", path, "file missing", False))
        return

    if README_START not in text or README_END not in text:
        findings.append(Finding(
            "readme", path,
            f"no {README_START} / {README_END} markers — add them where the version "
            f"table should live. Not inserted automatically: placing a block into a "
            f"hand-written README is the author's call.", False))
        return

    start = text.index(README_START)
    end = text.index(README_END) + len(README_END)
    expected = render_readme_block(data)
    if text[start:end] != expected:
        findings.append(Finding(
            "readme", path, "version block is stale", True))
        if fix:
            _write_text(path, text[:start] + expected + text[end:])


# ── T3: checked, never rewritten ────────────────────────────────────────────

def check_lib_constraints(data: dict, findings: list, _fix: bool) -> None:
    libs = data.get("libs", {})
    for key, meta in active_components(data):
        for field, lib in (("requiresCore", "core"), ("requiresSettings", "settings")):
            spec = meta.get(field)
            if spec is None:
                continue
            have = libs.get(lib)
            if have is None:
                findings.append(Finding(
                    "constraints", VERSIONS_JSON,
                    f"{key}.{field} references libs.{lib}, which is not defined", False))
                continue
            try:
                floor = parse_constraint(spec)
            except ValueError as exc:
                findings.append(Finding("constraints", VERSIONS_JSON, str(exc), False))
                continue
            if not SEMVER_RE.match(have):
                findings.append(Finding(
                    "constraints", VERSIONS_JSON,
                    f"libs.{lib}={have!r} is not a semver triple", False))
                continue
            if as_tuple(have) < floor:
                findings.append(Finding(
                    "constraints", VERSIONS_JSON,
                    f"{key}.{field}={spec} is not satisfied by libs.{lib}={have}", False))


# Legacy schema: a bare string means Android only. VERSIONING.md §5.1 migrates
# these to {"android": ..., "web": ...}; until then a bare string is accepted and
# checked against Android, which is exactly how the app behaves today.
PLATFORM_TO_COMPONENT = {"android": "apps.finance", "web": "web.finance"}


def check_feature_flag_min_versions(data: dict, findings: list, _fix: bool) -> None:
    flag_dir = "platform/feature-flags"
    if not os.path.isdir(flag_dir):
        return
    for fname in sorted(os.listdir(flag_dir)):
        if not fname.endswith(".json"):
            continue
        path = os.path.join(flag_dir, fname).replace("\\", "/")
        # Only the finance flag file maps onto components that exist today.
        if "finance" not in fname:
            continue
        try:
            with open(path, encoding="utf-8") as f:
                flags = json.load(f).get("features", {})
        except (OSError, json.JSONDecodeError) as exc:
            findings.append(Finding("min-version", path, f"unreadable: {exc}", False))
            continue

        for feature, meta in flags.items():
            raw = meta.get("minVersion")
            if raw is None:
                continue
            mins = {"android": raw} if isinstance(raw, str) else raw
            if not isinstance(mins, dict):
                findings.append(Finding(
                    "min-version", path,
                    f"{feature}.minVersion must be a semver string or a "
                    f"{{platform: semver}} object", False))
                continue
            for platform, floor in mins.items():
                component = PLATFORM_TO_COMPONENT.get(platform)
                if component is None:
                    findings.append(Finding(
                        "min-version", path,
                        f"{feature}.minVersion has unknown platform {platform!r}", False))
                    continue
                current = version_of(data, component)
                if current is None:
                    continue
                if not SEMVER_RE.match(str(floor)):
                    findings.append(Finding(
                        "min-version", path,
                        f"{feature}.minVersion.{platform}={floor!r} is not a semver triple",
                        False))
                    continue
                if as_tuple(str(floor)) > as_tuple(current):
                    findings.append(Finding(
                        "min-version", path,
                        f"{feature}.minVersion.{platform}={floor} is above "
                        f"{component}={current} — the feature can never appear", False))


def check_tag_patterns(data: dict, findings: list, _fix: bool) -> None:
    seen: dict[str, str] = {}
    for key, meta in active_components(data):
        pattern = meta.get("tag")
        if not pattern:
            findings.append(Finding(
                "tags", VERSIONS_JSON, f"{key} has no tag pattern", False))
            continue
        if "{version}" not in pattern:
            findings.append(Finding(
                "tags", VERSIONS_JSON,
                f"{key}.tag={pattern!r} has no {{version}} placeholder", False))
            continue
        resolved = pattern.format(version=meta["version"])
        if resolved in seen:
            findings.append(Finding(
                "tags", VERSIONS_JSON,
                f"{key} and {seen[resolved]} both resolve to tag {resolved!r}", False))
        seen[resolved] = key


def check_changelog(data: dict, findings: list, _fix: bool) -> None:
    path = "CHANGELOG.md"
    text = _read_text(path)
    if text is None:
        findings.append(Finding("changelog", path, "file missing", False))
        return
    headings = set(re.findall(r"^##\s*\[([^\]]+)\]", text, re.M))
    for key, meta in active_components(data):
        _, _, name = key.partition(".")
        prefix = "web-" if key.startswith("web.") else ""
        want = f"{prefix}{name}-{meta['version']}"
        if want not in headings:
            findings.append(Finding(
                "changelog", path,
                f"no `## [{want}]` heading for the current {key} version "
                f"(headings are namespaced per component — VERSIONING.md D3)", False))


CHECKS: list[Callable[[dict, list, bool], None]] = [
    check_gradle_properties,
    check_web_package_json,
    check_web_version_ts,
    check_readme,
    check_lib_constraints,
    check_feature_flag_min_versions,
    check_tag_patterns,
    check_changelog,
]


# ── Entry point ─────────────────────────────────────────────────────────────

def run(data: dict, fix: bool) -> list[Finding]:
    findings: list[Finding] = []
    for check in CHECKS:
        check(data, findings, fix)
    return findings


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true", help="report drift, change nothing")
    mode.add_argument("--write", action="store_true", help="regenerate every T2 file")
    parser.add_argument("--file", default=VERSIONS_JSON)
    parser.add_argument("--format", choices=("text", "github"), default="text")
    args = parser.parse_args()

    data = load_registry(args.file)
    findings = run(data, fix=args.write)

    if args.write:
        # Re-check after writing: anything still reported is genuinely not
        # auto-fixable, and saying so beats implying the tree is now clean.
        findings = run(load_registry(args.file), fix=False)
        if not findings:
            print("sync_versions: all generated files are up to date.")
            return 0
        print("sync_versions: wrote what it could; these need a human:")
        for f in findings:
            print(f.render(args.format))
        return 1

    if not findings:
        print("sync_versions: no drift.")
        return 0

    print(f"sync_versions: {len(findings)} drift(s) against {args.file}\n")
    for f in findings:
        print(f.render(args.format))
    fixable = sum(1 for f in findings if f.fixable)
    print(f"\n{fixable} of {len(findings)} are auto-fixable: "
          f"python scripts/ci/sync_versions.py --write")
    return 1


if __name__ == "__main__":
    sys.exit(main())