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
# Highest wins across the whole range. The $REF argument is accepted for
# call-site compatibility but no longer special-cased: ADR-0032 moves the
# `release` job to run on pushes to `main` only (develop no longer releases),
# so there is nothing left to double-bump on a develop -> main promotion — main
# now derives its segment the same way every branch does. (Previously this
# script forced `main` to always `patch`, per the now-superseded ADR-0025 rule.)
#
# Usage:  git log --format='%s%n%b' "$RANGE" | detect_bump.sh "$GITHUB_REF_NAME"
set -uo pipefail

LOG=$(cat)
BUMP=patch

printf '%s\n' "$LOG" | grep -Eq '^[a-zA-Z]+(\([^)]*\))?!:' && BUMP=major
printf '%s\n' "$LOG" | grep -Eq 'BREAKING[ -]CHANGE:'      && BUMP=major
if [ "$BUMP" != "major" ]; then
  printf '%s\n' "$LOG" | grep -Eq '^feat(\([^)]*\))?:' && BUMP=minor
fi

printf '%s\n' "$BUMP"
