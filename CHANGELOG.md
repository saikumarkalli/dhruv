# Changelog

All notable changes to the **Dhruv Calculator & Conversions** application will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Automated CI/CD Pipeline**: GitHub Actions workflow to automatically build, sign, and release APK and AAB files upon pushing tags to the repository.
- **Dynamic Versioning**: `versionCode` and `versionName` are now dynamically injected into the build process using GitHub run numbers and release tags.
- **Security Protocols**: Implemented Base64 Keystore injection via GitHub Secrets to prevent credential leakage. Added `*.jks` to `.gitignore`.
- **Knowledge Hub**: Added `06_ci_cd_deployment.md` outlining the CI/CD and deployment strategy.

### Changed
- Refactored `build.gradle.kts` release signing config to read version details securely from Gradle project properties `-PVERSION_CODE` and `-PVERSION_NAME`.

## [1.0.0] - Initial Structure
- Initial project scaffolding and foundational architecture setup.
- Basic functional capabilities documented in `knowledge_hub`.
