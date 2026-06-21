# CI/CD and Deployment Strategy

This document outlines the Continuous Integration, Continuous Deployment (CI/CD), versioning, and secure signing processes implemented for **Dhruv Calculator & Conversions**.

## GitHub Actions Workflows

The application utilizes GitHub Actions to automate the build and release pipeline. The workflow file is located at `.github/workflows/android-release.yml`.

### Triggers
- **Pull Requests / Pushes to `main`**: Ensures that code integrates smoothly by running the unit test suite (`./gradlew testDebugUnitTest`) and building the application upon any push to the main branch. The workflow will fail and prevent merging if any tests fail.
- **Tags (`v*`)**: Specifically watches for semantic versioning tags (e.g., `v1.2.0`). When a tag is pushed, the workflow escalates to a full release deployment.

### Automated Versioning
The application uses dynamic version injection into the Gradle build process to eliminate manual version bumping in code.
- **`versionCode`**: Always dynamically mapped to the `${{ github.run_number }}`. This guarantees a strictly increasing, unique integer for every build, satisfying Google Play Store's rigid versioning requirements.
  - On a Tag push (e.g. `v2.1.0`), the workflow strips the `v` and sets the `versionName` to `2.1.0`.
  - On a non-tag push to `main` (like a PR merge), the workflow queries git for the **last released tag** (`git describe --tags`). It then generates a beta version name in the format: `<previous_version>.[run_number]-beta` (e.g. `1.2.0.45-beta`). If no tags exist, it falls back to `1.0.0.[run_number]-beta`.

The workflow executes the Gradle commands `./gradlew assembleRelease` to generate a critical deployment artifact. 

Additionally, the `build.gradle.kts` specifies `base.archivesName.set()` to automatically append the injected version name into the generated files (e.g., `DhruvCalc-v1.2.0.45-beta-release.apk` instead of `app-release.apk`).

1. **Release APK (`DhruvCalc-v[version]-release.apk`)**: A Universal APK that can be directly downloaded and installed (sideloaded) onto Android devices.

> [!NOTE]
> **AAB Fallback**: The generation of the Android App Bundle (AAB) via `bundleRelease` has been temporarily disabled as it is not yet ready for Play Store deployment. This is documented as a `TODO` in the workflow file for future implementation.

If triggered by a Tag, the workflow uses the `softprops/action-gh-release@v2` action to automatically draft a GitHub Release and attach the APK for easy access.

## Security & Keystore Management

### The Upload Key
The application is signed using a secure `upload-key.jks`. Because committing keystores to a public or private repository is a critical security flaw, the keystore is injected securely at runtime during the CI process.

### GitHub Secrets
To make this work, the repository relies on 4 GitHub Secrets configured in the repository settings:
- `KEYSTORE_BASE64`: A Base64-encoded string of the `my-upload-key.jks` file. The CI workflow decodes this string into a temporary `.jks` file during the build.
- `STORE_PASSWORD`: The password for the Keystore.
- `KEY_ALIAS`: The alias name of the key (e.g., `upload`).
- `KEY_PASSWORD`: The password for the specific key.

> [!TIP]
> **Unsigned Fallback Strategy**: If the `STORE_PASSWORD` secret is missing or empty, the `build.gradle.kts` signing configuration safely aborts the signing process. Instead of crashing the CI pipeline, it logs a warning and successfully generates an *unsigned* APK (e.g., `DhruvCalc-v...-release-unsigned.apk`). This ensures continuous delivery of testable artifacts even if production secrets are temporarily unavailable.

### PR Comment Bot ("Dhruv Bot")
The `pr-summary` job in `ci.yml` posts a sticky CI-results comment on every PR. To brand that
comment under a custom identity/avatar instead of the default `github-actions[bot]`, it mints a
token from a dedicated GitHub App named **Dhruv Bot** (`Issues: Read & write` permission only,
installed solely on this repo) via `actions/create-github-app-token@v1`. This relies on 2 more
GitHub Secrets:
- `DHRUV_BOT_APP_ID`: the numeric App ID of the Dhruv Bot GitHub App.
- `DHRUV_BOT_PRIVATE_KEY`: the full contents of the App's generated private key (`.pem` file).

> [!TIP]
> **Fallback Strategy**: If either secret is missing or the App isn't installed on the repo, token
> minting fails silently (`continue-on-error: true`) and the comment step falls back to the default
> `GITHUB_TOKEN`, posting as `github-actions[bot]` instead. The PR comment itself never blocks CI or
> merge — see ADR-0012 in `platform/DECISIONS.md`.

### Local Development Safety
The local project `.gitignore` is configured to ignore `*.jks` and `*-base64.txt` files to guarantee that keystores generated on developer machines are never tracked by git.
