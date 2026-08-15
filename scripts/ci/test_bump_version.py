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
