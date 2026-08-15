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
