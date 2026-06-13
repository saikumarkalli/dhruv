---
name: dhruv-release
description: Handle Dhruv platform releases — version bumps, changelogs, tagging, and GitHub Release creation. Use whenever the user says "release", "bump version", "tag a release", "ship it", "create a release", "prepare release", "what version are we on", "changelog", or any request related to versioning or distributing APKs. Also triggers on "push to GitHub Releases", "make an APK", "version bump". This skill manages the full release flow from version increment through tag push.
---

# Dhruv Release

Manages versioning, changelogs, and GitHub Release workflow.

## Version scheme

Format: `dhruv-{app}-v{MAJOR}.{MINOR}.{PATCH}`
- **MAJOR** — breaking change (new minSdk, architecture overhaul)
- **MINOR** — new feature module added
- **PATCH** — bug fix, UI tweak

`versionCode` — auto-incremented by CI only, never manual.
Source of truth: `platform/versions.json`

## Release flow

### Step 1: Pre-release audit
Before any release, verify:
```bash
./gradlew test detekt                    # all tests + lint pass
./gradlew :apps:finance:app:assembleRelease  # signed APK builds
# Repeat for each app that changed (tools, vault)
```

### Step 2: Generate changelog
Collect commits since the last tag:
```bash
git log $(git describe --tags --abbrev=0)..HEAD --oneline --no-merges
```

Group by type:
```markdown
## What's new in v{VERSION}

### Features
- feat: add QR scanner to tools app (#42)
- feat: clipboard history with pinning (#45)

### Fixes
- fix: timer notification persists after reset (#43)

### Internal
- refactor: extract FeatureHost to :libs:core (#41)
```

### Step 3: Version bump
Run the bump script:
```bash
./scripts/bump-version.sh {major|minor|patch}
```

What the script does:
1. Reads current version from `platform/versions.json`
2. Increments the specified segment
3. Updates `versions.json` with new version
4. Updates each app's `build.gradle.kts` `versionName`
5. Auto-increments `versionCode` (+1)
6. Creates git commit: `release: bump to v{NEW_VERSION}`
7. Creates git tag: `v{NEW_VERSION}`
8. Prints: `Run 'git push origin main --tags' to trigger release`

### Step 4: Push and release
```bash
git push origin main --tags
```

This triggers `.github/workflows/release.yml`:
1. Runs all 4 CI gates
2. Builds signed release APK for each app
3. Creates GitHub Release with tag name
4. Attaches APKs as release assets
5. Includes the changelog as release body

### Step 5: Verify
- [ ] GitHub Release page shows the new version
- [ ] APK(s) attached and downloadable
- [ ] Install APK on device — runs correctly
- [ ] Crashlytics dashboard shows the new version string

## bump-version.sh implementation

```bash
#!/usr/bin/env bash
set -euo pipefail

VERSIONS_FILE="platform/versions.json"
BUMP_TYPE="${1:?Usage: bump-version.sh [major|minor|patch]}"

# Read current version
CURRENT=$(grep -oP '"version":\s*"\K[^"]+' "$VERSIONS_FILE" | head -1)
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT"

# Bump
case "$BUMP_TYPE" in
  major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
  minor) MINOR=$((MINOR + 1)); PATCH=0 ;;
  patch) PATCH=$((PATCH + 1)) ;;
  *) echo "Invalid: use major, minor, or patch"; exit 1 ;;
esac

NEW_VERSION="${MAJOR}.${MINOR}.${PATCH}"
echo "Bumping: ${CURRENT} → ${NEW_VERSION}"

# Update versions.json
sed -i "s/\"version\": \"${CURRENT}\"/\"version\": \"${NEW_VERSION}\"/g" "$VERSIONS_FILE"

# Update each app's build.gradle.kts versionName
find apps/*/app/build.gradle.kts -exec \
  sed -i "s/versionName = \".*\"/versionName = \"${NEW_VERSION}\"/" {} \;

# Auto-increment versionCode in each app
find apps/*/app/build.gradle.kts -exec \
  sed -i -E 's/versionCode = ([0-9]+)/echo "versionCode = $((\1+1))"/e' {} \;

# Commit and tag
git add -A
git commit -m "release: bump to v${NEW_VERSION}"
git tag "v${NEW_VERSION}"

echo ""
echo "✅ Version bumped to v${NEW_VERSION}"
echo "Run: git push origin main --tags"
```

## release.yml workflow

```yaml
name: Release

on:
  push:
    tags: ['v*']

jobs:
  ci:
    uses: ./.github/workflows/ci.yml

  release:
    needs: ci
    runs-on: ubuntu-latest
    permissions:
      contents: write

    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0  # full history for changelog

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Decode keystore
        run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > keystore.jks

      - name: Build signed APKs
        env:
          STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: |
          BUILD_TYPE=assembleRelease  # Change to bundleRelease for AAB/Play
          for app_dir in apps/*/app; do
            app_name=$(echo "$app_dir" | cut -d'/' -f2)
            module=":apps:${app_name}:app"
            echo "Building ${module}..."
            ./gradlew "${module}:${BUILD_TYPE}" || echo "Skipping ${app_name}"
          done

      - name: Generate changelog
        id: changelog
        run: |
          PREV_TAG=$(git describe --tags --abbrev=0 HEAD^ 2>/dev/null || echo "")
          if [ -n "$PREV_TAG" ]; then
            CHANGES=$(git log ${PREV_TAG}..HEAD --oneline --no-merges)
          else
            CHANGES=$(git log --oneline --no-merges -20)
          fi
          echo "changes<<EOF" >> $GITHUB_OUTPUT
          echo "$CHANGES" >> $GITHUB_OUTPUT
          echo "EOF" >> $GITHUB_OUTPUT

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          body: |
            ## Changes
            ${{ steps.changelog.outputs.changes }}
          files: |
            apps/*/app/build/outputs/apk/release/*.apk
```

## When to use which bump

| Change | Bump | Example |
|--------|------|---------|
| New feature module added | minor | Added QR scanner to tools |
| Existing feature enhanced | patch | Added lap tracking to timer |
| Bug fix | patch | Fixed crash on empty notes list |
| Architecture change | major | Migrated to new nav library |
| New app added | minor | Added vault app |
| Core library breaking change | major | DhruvEntity contract changed |

## Future: Play Store release
When ready for Play, the only changes needed:
1. `BUILD_TYPE=bundleRelease` (one-line swap)
2. Add Gradle Play Publisher plugin or manual upload
3. Add Play App Signing setup step
4. Add Data Safety form (manual, in Play Console)
