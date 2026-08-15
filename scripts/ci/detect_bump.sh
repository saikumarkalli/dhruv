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
