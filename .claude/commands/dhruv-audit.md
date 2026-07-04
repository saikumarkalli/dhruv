---
description: Pre-merge architectural compliance audit of a Dhruv module (PASS/FAIL verdict).
argument-hint: "[module path, e.g. apps/finance/feature/loans — omit to use the git diff]"
---

Use the **dhruv-module-auditor** subagent to audit `$ARGUMENTS`.

If `$ARGUMENTS` is empty, audit the module touched by the current git diff (`git diff --name-only develop...HEAD`, falling back to the working tree) and state which module was chosen.

Relay the subagent's full audit report and verdict verbatim. Do not fix anything — this is a read-only gate.
