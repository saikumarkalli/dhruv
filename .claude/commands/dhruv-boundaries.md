---
description: Check Dhruv module boundary / dependency rules (ArchUnit contract) for violations.
argument-hint: "[module or scope — omit to use the git diff]"
---

Use the **dhruv-architecture-guardian** subagent to review `$ARGUMENTS` for module-boundary and dependency-rule violations (feature→feature, vault→network/ai/analytics, feature→data-via-Repository-only, core→nothing-internal, and FeatureHost route coverage).

If `$ARGUMENTS` is empty, scope to the current git diff.

Relay the subagent's findings and verdict verbatim. Read-only — report violations and the fix, do not rewrite code.
