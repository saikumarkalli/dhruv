---
description: Full pre-merge gate — runs all three review agents in parallel plus the regression suite, then gives one merge verdict.
argument-hint: "[module path — omit to use the git diff]"
---

Run the complete Dhruv pre-merge gate for `$ARGUMENTS` (if empty, scope to the current git diff — state which module you chose).

Do these concurrently:
1. Launch the **dhruv-module-auditor** subagent on the target.
2. Launch the **dhruv-architecture-guardian** subagent on the target.
3. Launch the **dhruv-security-compliance-reviewer** subagent on the target.
4. Run `./gradlew regressionCheck` (all unit tests + ArchUnit + JaCoCo coverage floor — this is what CI runs). Requires `JAVA_HOME` = Android Studio JBR; if Gradle can't run, say so and mark this leg unverified rather than guessing.

Then produce a single consolidated report:

```
# Pre-merge gate: <module>

- Module audit          ✅/❌  <one-line verdict>
- Architecture          ✅/❌  <one-line verdict>
- Security & DPDP       ✅/❌  <one-line verdict>
- regressionCheck       ✅/❌/⚠️ <pass | failing tests | not run>

## Blocking issues
1. <issue> — <exact fix / file>

## Verdict: ✅ READY TO MERGE  |  ❌ NOT READY (N blockers)
```

The overall verdict is READY only if all four legs pass. List every blocker with its fix; do not modify code.
