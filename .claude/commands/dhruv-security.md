---
description: Security + India DPDP compliance review of a Dhruv change (secrets, vault, consent, erasure).
argument-hint: "[scope or path — omit to use the git diff]"
---

Use the **dhruv-security-compliance-reviewer** subagent to review `$ARGUMENTS` for security risks (secrets/keys, the 8 security layers, vault rules) and DPDP compliance (consent before off-device data, 7-day erasure path, no PII in telemetry).

If `$ARGUMENTS` is empty, scope to the current git diff.

Relay the subagent's findings and verdict verbatim. A leaked secret or an ungated off-device flow is always a blocking ❌.
