# Git hooks (versioned)

Shared git hooks for the Dhruv monorepo. These live in the repo (unlike `.git/hooks/`, which is
local-only and not version-controlled) so every clone runs the same checks.

## Activate (once per clone)

```sh
git config core.hooksPath scripts/hooks
```

This points git at this directory instead of `.git/hooks/`. After running it, the hooks below fire
automatically.

> On a fresh clone the hook files may need the executable bit: `chmod +x scripts/hooks/*`.
> (The bit is also tracked in git, so a normal clone already has it.)

## Hooks

### `pre-push`
Deterministic compliance audit (no AI tokens) on every **feature module** changed vs
`origin/develop`. For each changed `apps/<app>/feature/<module>` it verifies:

1. **FeatureHost wrapping** — the module uses `FeatureHost`, or the app shell wraps its route as
   `FeatureHost("<module>", …)` (routes are wrapped at the app/hub layer, e.g. `FeatureHubs.kt`).
2. **Feature flag entry** — `<module>` exists in `platform/feature-flags/dhruv-<app>.json`.
3. **Koin module** — a `val …Module = module { … }` (or `fun …Module()`) is declared.
4. **No hardcoded secrets** — no inline API keys/tokens (`BuildConfig`/proxy only).

A failure blocks the push and lists the violations.
