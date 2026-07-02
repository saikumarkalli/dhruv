#!/usr/bin/env python3
"""Build the regression-suite summary (test counts + coverage) for CI.

Single source of truth reused by three surfaces (see ADR-0012 / PLATFORM.md §11):
  * GitHub Job Summary  ($GITHUB_STEP_SUMMARY)   — every push and PR
  * Dhruv CI Bot sticky PR comment               — reads the job outputs below
  * GitHub Release notes                         — reads regression-summary.md

Inputs (produced by `./gradlew regressionCheck` / `jacocoAggregatedReport`):
  * JUnit XML:    **/build/test-results/testDebugUnitTest/*.xml
  * JaCoCo XML:   build/reports/jacoco/jacocoAggregatedReport/jacocoAggregatedReport.xml

Outputs:
  * writes Markdown to $GITHUB_STEP_SUMMARY (if set) and to ./regression-summary.md
  * appends tests_total/tests_passed/tests_failed/tests_skipped/coverage_pct to $GITHUB_OUTPUT

Pure stdlib; never raises on missing inputs (a partial summary is better than a failed job).
"""
from __future__ import annotations

import glob
import os
import xml.etree.ElementTree as ET
from collections import defaultdict

REPO = os.getcwd()
JACOCO_XML = "build/reports/jacoco/jacocoAggregatedReport/jacocoAggregatedReport.xml"

# JaCoCo package name (slash-separated) -> Gradle module path. Longest prefix wins. Values are the
# full module paths so they merge 1:1 with the labels derived from test-results file paths.
_FEATURES = (
    "calculator", "loans", "investments", "tax", "everyday",
    "currency", "unit", "date", "time", "assistant",
)
PACKAGE_TO_MODULE = {
    "com/dhruv/core": ":libs:core",
    "com/dhruv/settings": ":libs:settings",
    "com/dhruv/finance/data": ":apps:finance:data",
    "com/dhruv/finance/app": ":apps:finance:app",
    "com/dhruv/finance/mocks": ":apps:finance:app",
    **{f"com/dhruv/finance/{f}": f":apps:finance:feature:{f}" for f in _FEATURES},
}


def module_for_package(pkg: str) -> str:
    best = ""
    for prefix in PACKAGE_TO_MODULE:
        if (pkg == prefix or pkg.startswith(prefix + "/")) and len(prefix) > len(best):
            best = prefix
    return PACKAGE_TO_MODULE.get(best, "(other)")


def module_for_path(path: str) -> str:
    """Derive a readable module label from a test-results file path."""
    rel = os.path.relpath(path, REPO).replace("\\", "/")
    head = rel.split("/build/")[0]
    return ":" + head.replace("/", ":")


def collect_tests():
    """Returns (overall dict, per-module dict keyed by module label)."""
    overall = dict(tests=0, failures=0, errors=0, skipped=0)
    per_module = defaultdict(lambda: dict(tests=0, failures=0, errors=0, skipped=0))
    for f in glob.glob("**/build/test-results/testDebugUnitTest/*.xml", recursive=True):
        try:
            root = ET.parse(f).getroot()
        except ET.ParseError:
            continue
        suites = [root] if root.tag == "testsuite" else root.iter("testsuite")
        mod = module_for_path(f)
        for s in suites:
            for k in overall:
                v = int(s.get(k, 0))
                overall[k] += v
                per_module[mod][k] += v
    return overall, per_module


def collect_coverage():
    """Returns (overall {LINE,BRANCH}: (covered,total), per-module line {module: (cov,total)})."""
    overall = {}
    per_module = defaultdict(lambda: [0, 0])
    if not os.path.exists(JACOCO_XML):
        return overall, per_module
    try:
        root = ET.parse(JACOCO_XML).getroot()
    except ET.ParseError:
        return overall, per_module
    for c in root.findall("counter"):
        t = c.get("type")
        if t in ("LINE", "BRANCH"):
            m, cov = int(c.get("missed")), int(c.get("covered"))
            overall[t] = (cov, m + cov)
    for pkg in root.findall("package"):
        mod = module_for_package(pkg.get("name", ""))
        for c in pkg.findall("counter"):
            if c.get("type") == "LINE":
                m, cov = int(c.get("missed")), int(c.get("covered"))
                per_module[mod][0] += cov
                per_module[mod][1] += m + cov
    return overall, per_module


def pct(cov, total):
    return (cov / total * 100.0) if total else 0.0


def main():
    t_overall, t_mod = collect_tests()
    c_overall, c_mod = collect_coverage()

    passed = t_overall["tests"] - t_overall["failures"] - t_overall["errors"] - t_overall["skipped"]
    failed = t_overall["failures"] + t_overall["errors"]
    line_cov, line_tot = c_overall.get("LINE", (0, 0))
    coverage_pct = pct(line_cov, line_tot)
    verdict = "✅ Passing" if failed == 0 else f"❌ {failed} failing"

    lines = []
    lines.append(f"## 🧪 Regression Suite — {verdict}")
    lines.append("")
    lines.append(
        f"**Tests:** {passed} passed"
        + (f", **{failed} failed**" if failed else ", 0 failed")
        + f", {t_overall['skipped']} skipped ({t_overall['tests']} total)"
    )
    br_cov, br_tot = c_overall.get("BRANCH", (0, 0))
    lines.append(
        f"**Coverage (line):** {coverage_pct:.1f}%  ({line_cov}/{line_tot} lines"
        + (f", branch {pct(br_cov, br_tot):.1f}%" if br_tot else "")
        + ")"
    )
    lines.append("")

    modules = sorted(set(t_mod) | {m for m in c_mod if m != "(other)"})
    lines.append("| Module | Tests | Line Cov |")
    lines.append("|---|---|---|")
    for m in modules:
        t = t_mod.get(m, dict(tests=0, failures=0, errors=0, skipped=0))
        mp = t["tests"] - t["failures"] - t["errors"] - t["skipped"]
        mf = t["failures"] + t["errors"]
        tcell = "—" if t["tests"] == 0 else (f"{mp}✓" + (f" / {mf}✗" if mf else ""))
        cov = c_mod.get(m)
        ccell = "—" if not cov or cov[1] == 0 else f"{pct(cov[0], cov[1]):.0f}%"
        lines.append(f"| `{m}` | {tcell} | {ccell} |")

    md = "\n".join(lines) + "\n"

    with open("regression-summary.md", "w", encoding="utf-8") as fh:
        fh.write(md)

    step_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if step_summary:
        with open(step_summary, "a", encoding="utf-8") as fh:
            fh.write(md)

    gh_out = os.environ.get("GITHUB_OUTPUT")
    if gh_out:
        with open(gh_out, "a", encoding="utf-8") as fh:
            fh.write(f"tests_total={t_overall['tests']}\n")
            fh.write(f"tests_passed={passed}\n")
            fh.write(f"tests_failed={failed}\n")
            fh.write(f"tests_skipped={t_overall['skipped']}\n")
            fh.write(f"coverage_pct={coverage_pct:.1f}\n")

    try:
        print(md)
    except UnicodeEncodeError:  # non-UTF8 console (e.g. Windows cp1252) — write raw bytes
        import sys
        sys.stdout.buffer.write(md.encode("utf-8"))


if __name__ == "__main__":
    main()
