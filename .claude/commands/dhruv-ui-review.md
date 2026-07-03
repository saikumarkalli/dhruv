---
description: UI/UX + accessibility review of Dhruv Compose screens, powered by ui-ux-pro-max (Compose-scoped).
argument-hint: "[screen/component or path — omit to use the git diff]"
---

Use the **dhruv-ui-ux-reviewer** subagent to review `$ARGUMENTS` for UX quality, accessibility, and visual consistency, using the ui-ux-pro-max design intelligence scoped to the Jetpack Compose stack.

If `$ARGUMENTS` is empty, scope to the current git diff.

Relay the subagent's findings and verdict verbatim. Accessibility failures (missing contentDescription, sub-48.dp targets, failing contrast) are blocking ❌, not nits.
