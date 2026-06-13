---
name: dhruv-release
description: Handle Dhruv platform releases — version bumps, changelogs, tagging, and GitHub Release creation. Use whenever the user says "release", "bump version", "tag a release", "ship it", "create a release", "prepare release", "what version are we on", "changelog", or any request related to versioning or distributing APKs. Also triggers on "push to GitHub Releases", "make an APK", "version bump". This skill manages the full release flow from version increment through tag push.
---

# Dhruv Release

Manages versioning, changelogs, and GitHub Release workflow.

## Branch strategy (read first)

| Branch | Purpose | Artifact | Who tags |
|--------|----------|----------|----------|
| `develop` | Default — all dev work | Signed APK → GitHub Release | You, via bump-version.sh |
| `main` | Play Store only (future) | Signed AAB | Only when Play launch planned |
| `feat/*` etc. | Feature work | — | Never tagged |

**All release tags go on `develop`. Never tag main manually — that is for Play Store deployment only.**

## Version scheme

Format: `dhruv-{app}-v{MAJOR}.{MINOR}.{PATCH}`
- **MAJOR** — breaking change (new minSdk, architecture overhaul)
- **MINOR** — new feature module added
- **PATCH** — bug fix, UI tweak

`versionCode` — auto-incremented by CI only, never manual.
Source of truth: `platform/versions.json`

## Release flow (develop → GitHub Release APK)

### Step 1: Pre-release audit
```bash
git switch develop
./gradlew test detekt
./gradlew :apps:finance:app:assembleRelease   # signed APK builds
# Repeat for each app that changed
```

### Step 2: Generate changelog
```bash
git log $(git describe --tags --abbrev=0)..HEAD --oneline --no-merges
```
Group into Features / Fixes / Internal.

### Step 3: Version bump on develop
```bash
git switch develop
./scripts/bump-version.sh {major|minor|patch}
```

### Step 4: Push tag — triggers GitHub Release
```bash
git push origin develop --tags
```
CI runs 4 gates → builds signed APK → creates GitHub Release → attaches APK(s).

### Step 5: Verify
- [ ] GitHub Release page shows new version
- [ ] APK(s) attached and downloadable
- [ ] Install APK on device — runs correctly
- [ ] Crashlytics shows new version string

## bump-version.sh

```bash
#!/usr/bin/env bash
set -euo pipefail

VERSIONS_FILE="platform/versions.json"
BUMP_TYPE="${1:?Usage: bump-version.sh [major|minor|patch]}"

CURRENT=$(grep -oP '"version":\s*"\K[^"]+' "$VERSIONS_FILE" | head -1)
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT"

case "$BUMP_TYPE" in
  major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
  minor) MINOR=$((MINOR + 1)); PATCH=0 ;;
  patch) PATCH=$((PATCH + 1)) ;;
  *) echo "Invalid: use major, minor, or patch"; exit 1 ;;
esac

NEW_VERSION="${MAJOR}.${MINOR}.${PATCH}"
echo "Bumping: ${CURRENT} → ${NEW_VERSION}"

sed -i "s/\"version\": \"${CURRENT}\"/\"version\": \"${NEW_VERSION}\"/g" "$VERSIONS_FILE"

find apps/*/app/build.gradle.kts -exec \
  sed -i "s/versionName = \".*\"/versionName = \"${NEW_VERSION}\"/" {} \;

find apps/*/app/build.gradle.kts -exec \
  sed -i -E 's/versionCode = ([0-9]+)/echo "versionCode = $((\1+1))"/e' {} \;

git add -A
git commit -m "release: bump to v${NEW_VERSION}"
git tag "v${NEW_VERSION}"

echo ""
echo "✅ Version bumped to v${NEW_VERSION} on develop"
echo "Run: git push origin develop --tags"
```

## ci.yml structure

```yaml
name: CI

on:
  push:
    branches: [develop, main]
  pull_request:
    branches: [develop, main]

jobs:
  lint:
    # ktlint, detekt, android lint

  security:
    # GitLeaks, OWASP dependency-check

  test:
    # ./gradlew test (unit + ArchUnit)

  build:
    steps:
      - name: Build
        run: |
          if [ "${{ github.ref_name }}" = "main" ]; then
            # main: AAB for Play Store
            ./gradlew bundleRelease
          else
            # develop: APK for GitHub Releases
            ./gradlew assembleRelease
          fi
```

## release.yml structure

```yaml
name: Release

on:
  push:
    tags: ['v*']

jobs:
  ci:
    uses: ./.github/workflows/ci.yml    # all 4 gates must pass

  release:
    needs: ci
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - name: Detect branch for tag
        run: |
          BRANCH=$(git branch -r --contains ${{ github.ref }} | grep -v HEAD | head -1 | xargs)
          echo "TAG_BRANCH=$BRANCH" >> $GITHUB_ENV

      - name: Build signed APK (develop tags only)
        if: contains(env.TAG_BRANCH, 'develop')
        run: ./gradlew assembleRelease
        # signs using KEYSTORE_BASE64, KEY_ALIAS, KEY_PASSWORD, STORE_PASSWORD secrets

      - name: Create GitHub Release (develop tags only)
        if: contains(env.TAG_BRANCH, 'develop')
        uses: softprops/action-gh-release@v2
        with:
          body: "Auto-generated changelog from commits"
          files: apps/*/app/build/outputs/apk/release/*.apk

      # Future: main tag → Play Store AAB upload (add when Play launch planned)
```

## When to use which bump

| Change | Bump | Example |
|--------|------|---------|
| New feature module | minor | Added QR scanner |
| Existing feature enhanced | patch | Added lap tracking to timer |
| Bug fix | patch | Fixed crash on empty list |
| New app added | minor | Added vault app |
| Architecture / minSdk change | major | — |

## Future: Play Store (main branch)
When ready: merge `develop → main` → tag on main → CI builds AAB → upload to Play.
The only addition needed is a Play Publisher step in release.yml gated on `main` branch.