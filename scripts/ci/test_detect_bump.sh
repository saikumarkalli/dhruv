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
check "main derives normally"     main    major "feat!: drop api v1"
check "main feat -> minor"        main    minor "feat: add networth screen"
check "empty log"                 develop patch ""
check "bang only mid-word"        develop patch "fix: resolve foo! in parser"

[ "$FAIL" -eq 0 ] && echo "ALL PASS" || echo "FAILURES"
exit "$FAIL"
