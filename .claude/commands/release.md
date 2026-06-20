Use the `dhruv-release-manager` agent to handle a version bump or release.

Arguments: $ARGUMENTS (e.g. "minor bump for finance" or "patch release")

Hand off to dhruv-release-manager with this task: $ARGUMENTS. Follow the dhruv-release skill. Remember: CI auto-increments PATCH on every merge (ADR-0011) — only bump MINOR or MAJOR manually in platform/versions.json. Do not touch VERSION_CODE or VERSION_NAME directly — those are CI-owned. Tag format: dhruv-<app>-vMAJOR.MINOR.PATCH.
