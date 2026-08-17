#!/usr/bin/env python3
"""Unit tests for sync_versions.py.

Run:  python scripts/ci/test_sync_versions.py

Each test builds a throwaway tree with only the files the check under test reads,
then chdirs into it — the same shape as test_bump_version.py.
"""
from __future__ import annotations

import json
import os
import shutil
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sync_versions as sv  # noqa: E402


def registry(**overrides) -> dict:
    data = {
        "platformContractVersion": "1.0.0",
        "libs": {"core": "1.0.0", "settings": "1.0.0"},
        "apps": {
            "finance": {
                "version": "2.0.4", "status": "active", "buildNumber": 16,
                "requiresCore": ">=1.0.0", "requiresSettings": ">=1.0.0",
                "paths": ["apps/finance/**"],
                "tag": "dhruv-finance-v{version}", "artifact": "github-release",
            },
            "tools": {"version": "0.0.0", "status": "planned",
                      "tag": "dhruv-tools-v{version}"},
        },
        "web": {
            "finance": {
                "version": "0.1.0", "status": "active", "paths": ["web/**"],
                "tag": "dhruv-web-finance-v{version}", "artifact": "tag-only",
            },
        },
    }
    data.update(overrides)
    return data


class Tree:
    """A temp working tree, seeded so that every check passes by default."""

    def __init__(self, data: dict):
        self.dir = tempfile.mkdtemp()
        self.prev = os.getcwd()
        os.chdir(self.dir)
        self.data = data
        fin = data["apps"]["finance"]
        web = data["web"]["finance"]

        self.write("platform/versions.json", json.dumps(data, indent=2) + "\n")
        self.write("gradle.properties",
                   f"org.gradle.jvmargs=-Xmx4g\nVERSION_CODE={fin['buildNumber']}\n"
                   f"VERSION_NAME={fin['version']}\n")
        self.write("web/package.json",
                   '{\n  "name": "dhruv-web",\n  "private": true,\n'
                   f'  "version": "{web["version"]}",\n  "type": "module"\n''}\n')
        self.write("web/src/shared/version.ts",
                   sv.WEB_VERSION_TS.format(version=web["version"]))
        self.write("README.md",
                   "# Dhruv\n\nintro\n\n" + sv.render_readme_block(data) + "\n\nrest\n")
        self.write("CHANGELOG.md",
                   f"# Changelog\n\n## [finance-{fin['version']}] - 2026-08-17\n\n"
                   f"## [web-finance-{web['version']}] - 2026-08-17\n")
        self.write("platform/feature-flags/dhruv-finance.json",
                   json.dumps({"features": {
                       "assistant": {"enabled": True, "minVersion": "1.2.0"},
                   }}, indent=2) + "\n")

    def write(self, path: str, text: str) -> None:
        os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)

    def read(self, path: str) -> str:
        with open(path, encoding="utf-8") as f:
            return f.read()

    def close(self) -> None:
        os.chdir(self.prev)
        shutil.rmtree(self.dir, ignore_errors=True)


class SyncVersionsTest(unittest.TestCase):
    def setUp(self):
        self.tree = None

    def tearDown(self):
        if self.tree:
            self.tree.close()

    def build(self, data=None) -> Tree:
        self.tree = Tree(data or registry())
        return self.tree

    def checks(self, data=None):
        return sv.run(data or self.tree.data, fix=False)

    def names(self, findings):
        return sorted({f.check for f in findings})

    # ── baseline ────────────────────────────────────────────────────────────

    def test_clean_tree_has_no_findings(self):
        self.build()
        self.assertEqual(self.checks(), [])

    # ── T2 generated ────────────────────────────────────────────────────────

    def test_gradle_version_name_drift_detected_and_fixed(self):
        t = self.build()
        t.write("gradle.properties", "VERSION_CODE=16\nVERSION_NAME=1.2.5\n")
        self.assertIn("gradle", self.names(self.checks()))

        sv.run(t.data, fix=True)
        self.assertIn("VERSION_NAME=2.0.4", t.read("gradle.properties"))
        self.assertEqual(self.checks(), [])

    def test_gradle_build_number_drift_detected(self):
        t = self.build()
        t.write("gradle.properties", "VERSION_CODE=7\nVERSION_NAME=2.0.4\n")
        self.assertIn("gradle", self.names(self.checks()))

    def test_web_package_json_fix_preserves_key_order_and_formatting(self):
        t = self.build()
        t.write("web/package.json",
                '{\n  "name": "dhruv-web",\n  "private": true,\n'
                '  "version": "0.0.1",\n  "type": "module"\n}\n')
        self.assertIn("web-pkg", self.names(self.checks()))

        sv.run(t.data, fix=True)
        after = t.read("web/package.json")
        self.assertEqual(
            after,
            '{\n  "name": "dhruv-web",\n  "private": true,\n'
            '  "version": "0.1.0",\n  "type": "module"\n}\n')

    def test_missing_web_version_ts_is_created(self):
        t = self.build()
        os.remove("web/src/shared/version.ts")
        findings = self.checks()
        self.assertIn("web-version-ts", self.names(findings))
        self.assertTrue(all(f.fixable for f in findings if f.check == "web-version-ts"))

        sv.run(t.data, fix=True)
        self.assertIn('APP_VERSION = "0.1.0"', t.read("web/src/shared/version.ts"))

    def test_readme_without_markers_is_reported_but_not_auto_fixable(self):
        t = self.build()
        t.write("README.md", "# Dhruv\n\nno markers here\n")
        findings = [f for f in self.checks() if f.check == "readme"]
        self.assertEqual(len(findings), 1)
        self.assertFalse(findings[0].fixable)

        # --write must not invent the block.
        sv.run(t.data, fix=True)
        self.assertNotIn(sv.README_START, t.read("README.md"))

    def test_stale_readme_block_is_rewritten_in_place(self):
        t = self.build()
        t.write("README.md",
                f"# Dhruv\n\nbefore\n\n{sv.README_START}\nstale junk\n{sv.README_END}\n\nafter\n")
        self.assertIn("readme", self.names(self.checks()))

        sv.run(t.data, fix=True)
        after = t.read("README.md")
        self.assertIn("`apps.finance` | `2.0.4`", after)
        self.assertIn("before", after)
        self.assertIn("after", after)

    def test_readme_block_lists_only_active_components(self):
        self.build()
        block = sv.render_readme_block(self.tree.data)
        self.assertIn("apps.finance", block)
        self.assertIn("web.finance", block)
        self.assertNotIn("apps.tools", block)   # status: planned

    # ── T3 checked ──────────────────────────────────────────────────────────

    def test_unsatisfied_requires_core_is_reported(self):
        data = registry()
        data["apps"]["finance"]["requiresCore"] = ">=2.0.0"   # libs.core is 1.0.0
        self.build(data)
        self.assertIn("constraints", self.names(self.checks()))

    def test_unparseable_constraint_fails_loudly(self):
        data = registry()
        data["apps"]["finance"]["requiresCore"] = "^1.0.0"
        self.build(data)
        findings = [f for f in self.checks() if f.check == "constraints"]
        self.assertEqual(len(findings), 1)
        self.assertIn("unsupported constraint", findings[0].message)

    def test_min_version_above_current_is_reported(self):
        t = self.build()
        t.write("platform/feature-flags/dhruv-finance.json",
                json.dumps({"features": {
                    "assistant": {"enabled": True, "minVersion": {"android": "9.0.0"}},
                }}) + "\n")
        findings = [f for f in self.checks() if f.check == "min-version"]
        self.assertEqual(len(findings), 1)
        self.assertIn("can never appear", findings[0].message)

    def test_bare_string_min_version_is_treated_as_android(self):
        t = self.build()
        # 0.5.0 is above web.finance (0.1.0) but below apps.finance (2.0.4).
        # Legacy bare strings mean Android, so this must NOT be flagged.
        t.write("platform/feature-flags/dhruv-finance.json",
                json.dumps({"features": {
                    "assistant": {"enabled": True, "minVersion": "0.5.0"},
                }}) + "\n")
        self.assertEqual([f for f in self.checks() if f.check == "min-version"], [])

    def test_per_platform_min_version_checks_web_against_web_line(self):
        t = self.build()
        t.write("platform/feature-flags/dhruv-finance.json",
                json.dumps({"features": {
                    "assistant": {"enabled": True,
                                  "minVersion": {"android": "1.2.0", "web": "0.5.0"}},
                }}) + "\n")
        findings = [f for f in self.checks() if f.check == "min-version"]
        self.assertEqual(len(findings), 1)
        self.assertIn("web.finance=0.1.0", findings[0].message)

    def test_duplicate_tag_pattern_is_reported(self):
        data = registry()
        data["web"]["finance"]["tag"] = "dhruv-finance-v{version}"
        data["web"]["finance"]["version"] = "2.0.4"
        self.build(data)
        self.assertIn("tags", self.names(self.checks()))

    def test_missing_changelog_heading_is_reported(self):
        t = self.build()
        t.write("CHANGELOG.md", "# Changelog\n\n## [2.0.0] - 2026-07-26\n")
        findings = [f for f in self.checks() if f.check == "changelog"]
        self.assertEqual(len(findings), 2)   # one per active component

    # ── determinism ─────────────────────────────────────────────────────────

    def test_write_is_idempotent(self):
        t = self.build()
        t.write("gradle.properties", "VERSION_CODE=1\nVERSION_NAME=0.0.1\n")
        os.remove("web/src/shared/version.ts")

        sv.run(t.data, fix=True)
        snapshot = {p: t.read(p) for p in
                    ("gradle.properties", "web/package.json",
                     "web/src/shared/version.ts", "README.md")}

        sv.run(t.data, fix=True)
        for path, before in snapshot.items():
            self.assertEqual(before, t.read(path), f"{path} changed on second --write")

    def test_write_preserves_crlf_line_endings(self):
        t = self.build()
        # A Windows working tree has gradle.properties in CRLF. Rewriting one
        # value must not rewrite every line.
        with open("gradle.properties", "w", encoding="utf-8", newline="") as f:
            f.write("VERSION_CODE=16\r\nVERSION_NAME=1.2.5\r\n")

        sv.run(t.data, fix=True)
        with open("gradle.properties", "rb") as f:
            raw = f.read()
        self.assertEqual(raw, b"VERSION_CODE=16\r\nVERSION_NAME=2.0.4\r\n")
        self.assertNotIn(b"\n\n", raw)          # no stray LF introduced
        self.assertEqual(self.checks(), [])     # and the check reads it fine

    def test_write_leaves_lf_files_as_lf(self):
        t = self.build()
        t.write("gradle.properties", "VERSION_CODE=16\nVERSION_NAME=1.2.5\n")
        sv.run(t.data, fix=True)
        with open("gradle.properties", "rb") as f:
            self.assertEqual(f.read(), b"VERSION_CODE=16\nVERSION_NAME=2.0.4\n")

    def test_malformed_registry_exits_two(self):
        t = self.build()
        t.write("platform/versions.json", "{ not json")
        with self.assertRaises(SystemExit) as ctx:
            sv.load_registry("platform/versions.json")
        self.assertEqual(ctx.exception.code, 2)


if __name__ == "__main__":
    unittest.main(verbosity=2)