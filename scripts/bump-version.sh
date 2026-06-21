#!/usr/bin/env bash
# bump-version.sh — Increment versionName in platform/versions.json + app build.gradle.kts,
# auto-increment versionCode, commit, and tag.
#
# Usage:  ./scripts/bump-version.sh <app> <major|minor|patch>
# Example: ./scripts/bump-version.sh finance minor
#
# The tag pushed triggers .github/workflows/release.yml to build the signed APK.

set -euo pipefail

# ── Args ──────────────────────────────────────────────────────────────────────
APP="${1:-}"
BUMP="${2:-}"

if [[ -z "$APP" || -z "$BUMP" ]]; then
  echo "Usage: $0 <app> <major|minor|patch>"
  echo "  Apps: finance | tools | vault"
  echo "  Bump: major | minor | patch"
  exit 1
fi

VALID_APPS=("finance" "tools" "vault")
if [[ ! " ${VALID_APPS[*]} " =~ " ${APP} " ]]; then
  echo "Unknown app '$APP'. Valid: ${VALID_APPS[*]}"
  exit 1
fi

VALID_BUMPS=("major" "minor" "patch")
if [[ ! " ${VALID_BUMPS[*]} " =~ " ${BUMP} " ]]; then
  echo "Unknown bump type '$BUMP'. Valid: major | minor | patch"
  exit 1
fi

# ── Resolve paths ─────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
VERSIONS_FILE="$REPO_ROOT/platform/versions.json"

case "$APP" in
  finance) BUILD_GRADLE="$REPO_ROOT/apps/finance/app/build.gradle.kts" ;;
  tools)   BUILD_GRADLE="$REPO_ROOT/apps/tools/app/build.gradle.kts" ;;
  vault)   BUILD_GRADLE="$REPO_ROOT/apps/vault/app/build.gradle.kts" ;;
esac

# ── Read current version ───────────────────────────────────────────────────────
CURRENT_VERSION=$(python3 -c "
import json, sys
with open('$VERSIONS_FILE') as f:
    d = json.load(f)
print(d['apps']['$APP']['version'])
")

IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"

# ── Compute new version ────────────────────────────────────────────────────────
case "$BUMP" in
  major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
  minor) MINOR=$((MINOR + 1)); PATCH=0 ;;
  patch) PATCH=$((PATCH + 1)) ;;
esac

NEW_VERSION="${MAJOR}.${MINOR}.${PATCH}"
TAG="dhruv-${APP}-v${NEW_VERSION}"

echo "Bumping $APP: $CURRENT_VERSION → $NEW_VERSION  (tag: $TAG)"

# ── Update versions.json ──────────────────────────────────────────────────────
python3 - <<PYEOF
import json
with open('$VERSIONS_FILE') as f:
    d = json.load(f)
d['apps']['$APP']['version'] = '$NEW_VERSION'
with open('$VERSIONS_FILE', 'w') as f:
    json.dump(d, f, indent=2)
    f.write('\n')
PYEOF

# ── Update versionName in build.gradle.kts ────────────────────────────────────
if [[ -f "$BUILD_GRADLE" ]]; then
  # Update the archivesName line that embeds version
  sed -i "s/base\.archivesName\.set(\"Dhruv${APP^}-v.*\")/base.archivesName.set(\"Dhruv${APP^}-v${NEW_VERSION}\")/" "$BUILD_GRADLE"
fi

# ── Auto-increment versionCode via Gradle property ────────────────────────────
GRADLE_PROPS="$REPO_ROOT/gradle.properties"
CURRENT_CODE=$(grep "^VERSION_CODE=" "$GRADLE_PROPS" 2>/dev/null | cut -d= -f2 || echo "0")
NEW_CODE=$((CURRENT_CODE + 1))

if grep -q "^VERSION_CODE=" "$GRADLE_PROPS" 2>/dev/null; then
  sed -i "s/^VERSION_CODE=.*/VERSION_CODE=$NEW_CODE/" "$GRADLE_PROPS"
else
  echo "VERSION_CODE=$NEW_CODE" >> "$GRADLE_PROPS"
fi

if grep -q "^VERSION_NAME=" "$GRADLE_PROPS" 2>/dev/null; then
  sed -i "s/^VERSION_NAME=.*/VERSION_NAME=$NEW_VERSION/" "$GRADLE_PROPS"
else
  echo "VERSION_NAME=$NEW_VERSION" >> "$GRADLE_PROPS"
fi

# ── Commit + tag ──────────────────────────────────────────────────────────────
cd "$REPO_ROOT"
git add platform/versions.json gradle.properties
[[ -f "$BUILD_GRADLE" ]] && git add "$BUILD_GRADLE"

git commit -m "chore(release): bump $APP to v${NEW_VERSION} (versionCode $NEW_CODE)"
git tag -a "$TAG" -m "Release $TAG"

echo ""
echo "✅  $TAG created locally."
echo "    Run: git push origin \$(git branch --show-current) --tags"
echo "    This triggers release.yml → signed APK → GitHub Release."
