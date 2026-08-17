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

Runs two checks, in order. The first is a hard stop; the second is the module audit.

#### 1. Protected-branch block

`main` and `develop` are **PR-only** (AGENTS.md branch rules, ADR-0032 decision 1). A push to
either is refused here, with the branch-and-PR commands to use instead.

This hook is the *preventive* half of that rule and
[`.github/workflows/branch-guard.yml`](../../.github/workflows/branch-guard.yml) is the *detective*
half. GitHub's own enforcement — rulesets and classic branch protection — is Pro-gated for a
private repo, and this repo is on GitHub Free (`403 "Upgrade to GitHub Pro"`, verified live). So
**this hook is the only thing that actually stops a direct push**, which is why activating
`core.hooksPath` above is not optional.

Emergency override, for the genuine case only:

```sh
DHRUV_ALLOW_PROTECTED_PUSH=1 git push ...
```

`branch-guard.yml` still reports the violation server-side either way, so nothing is silent.
Plain `--no-verify` also skips it — and skips the module audit below with it.

Run [`scripts/env/apply-branch-protection.ps1`](../env/apply-branch-protection.ps1) once the
account is on GitHub Pro; real server-side rulesets make this block a fast-fail convenience
rather than the enforcement mechanism.

#### 2. Feature-module compliance audit

Deterministic compliance audit (no AI tokens) on every **feature module** changed vs
`origin/develop`. For each changed `apps/<app>/feature/<module>` it verifies:

1. **FeatureHost wrapping** — the module uses `FeatureHost`, or the app shell wraps its route as
   `FeatureHost("<module>", …)` (routes are wrapped at the app/hub layer, e.g. `FeatureHubs.kt`).
2. **Feature flag entry** — `<module>` exists in `platform/feature-flags/dhruv-<app>.json`.
3. **Koin module** — a `val …Module = module { … }` (or `fun …Module()`) is declared.
4. **No hardcoded secrets** — no inline API keys/tokens (`BuildConfig`/proxy only).

A failure blocks the push and lists the violations.
