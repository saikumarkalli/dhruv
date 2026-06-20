Use the `dhruv-ci-engineer` agent to fix or improve CI/CD and build tooling.

Arguments: $ARGUMENTS (describe the CI issue or improvement needed)

Hand off to dhruv-ci-engineer with this task: $ARGUMENTS. Scope: .github/workflows/, build-logic/ convention plugins, quality config (ktlint, detekt, GitLeaks, OWASP, ArchUnit). The four CI gates are: (1) static analysis, (2) security scan, (3) tests, (4) build + size delta. develop builds a signed APK; main builds a signed AAB. Do not skip hooks or bypass gates.
