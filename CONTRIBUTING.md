# Contributing to Dhruv

Dhruv is a **personal project**, published for transparency and reuse. It is built and maintained by
one person, to a roadmap that serves one person's needs.

That has two honest consequences, and it is kinder to say them up front than to leave you guessing:

- **Outside contributions are genuinely welcome** — bug reports, reproductions, small fixes,
  documentation corrections, and questions about how something works. Those are useful and get read.
- **The maintainer's roadmap takes priority, and large unsolicited pull requests may be declined** —
  not because the work is bad, but because a big change to an architecture governed by a decision
  register costs more to review, reconcile and maintain than it returns. **Open an issue or a
  discussion before starting anything substantial**, so nobody spends a weekend on a patch that was
  never going to land.

If your interest is reuse rather than contribution: the code is [MIT](LICENSE). Fork it freely.

By participating you agree to the [Code of Conduct](CODE_OF_CONDUCT.md).

**Read first**, in this order — these are the source of truth and this file is not:

1. [`platform/AGENTS.md`](platform/AGENTS.md) — session rules and hard constraints
2. [`platform/PLATFORM.md`](platform/PLATFORM.md) — architecture (the *what*)
3. [`platform/DECISIONS.md`](platform/DECISIONS.md) — the decision register (the *why*)
4. [`platform/DESIGN-SYSTEM.md`](platform/DESIGN-SYSTEM.md) — binding design contract for all UI

---

## Local setup

```sh
git clone https://github.com/saikumarkalli/dhruv.git
cd dhruv

# REQUIRED — activates the versioned hooks in scripts/hooks/
git config core.hooksPath scripts/hooks

cp .env.example .env                    # Android/platform secrets
cp web/.env.example web/.env.local      # web SPA
```

**`JAVA_HOME` must point at the Android Studio JBR (JDK 17+).** The project is on AGP 9; an
unrelated JDK is the most common first-run failure.

**Placeholder values are fine for building.** Both `.env` and `web/.env.local` are gitignored.
`.env.example` ships deliberately fake-but-non-empty placeholders (`MY_SUPABASE_URL`, …) — a
genuinely empty value makes the `secrets` Gradle plugin emit a non-compiling `BuildConfig` field, so
do not "tidy" them to blanks. Real values are only needed for a **live** Google sign-in or a live
Supabase call; everything else — compiling, tests, `regressionCheck`, the debug APK — works on
placeholders. Never commit a real key: GitLeaks blocks the PR (see [`SECURITY.md`](SECURITY.md)).

---

## Commands

Android (from the repo root):

```sh
./gradlew :apps:finance:app:assembleDebug   # build the app
./gradlew detekt                            # lint
./gradlew regressionCheck                   # THE pre-merge gate
```

`regressionCheck` is every module's `testDebugUnitTest` (ArchUnit and Robolectric live in the debug
variant) plus the merged JaCoCo report and the coverage floor. It is exactly what CI runs as
`Gate 3+4 · Tests + ArchUnit + Coverage + Build` — if it passes locally it passes there
(ADR-0013, ADR-0026).

Single module or single test:

```sh
./gradlew :apps:finance:feature:<name>:assembleDebug
./gradlew :apps:finance:feature:<name>:testDebugUnitTest --tests "com.dhruv.finance.<name>.SomeTest"
```

Web (from `web/`) — these are the scripts actually defined in
[`web/package.json`](web/package.json):

| Script | Command |
|---|---|
| `npm run dev` | `vite` — dev server |
| `npm run build` | `tsc -b && vite build` |
| `npm run lint` | `eslint .` |
| `npm run typecheck` | `tsc -b --noEmit` |
| `npm test` | `vitest run` |
| `npm run test:watch` | `vitest` |
| `npm run preview` | `vite preview` |

---

## Branching

Always branch **from `develop`**. PRs target **`develop`**. `main` is production-only and receives
`develop → main` PRs exclusively (ADR-0032, superseding ADR-0009's branch roles).

| Branch | Role | How it advances |
|---|---|---|
| `develop` | **DEV** — default branch. `dhruv-dev`, Vercel Preview, debug APK. | merged PR only |
| `main` | **PROD** — `dhruv-prod`, Vercel Production, signed APK + GitHub Release. | merged `develop → main` PR only |
| `feat/*` `fix/*` `chore/*` | feature work | branch from `develop`, PR back to `develop` |

**`main` and `develop` are PR-only. Never push to either directly.**

That is now enforced server-side. While this repository was private on GitHub Free, rulesets and
classic branch protection were both Pro-gated and verified to refuse (`GET /rulesets` →
`403 "Upgrade to GitHub Pro"`); publishing the repository made them free, and
[`scripts/env/apply-branch-protection.ps1`](scripts/env/apply-branch-protection.ps1) applies them
(ADR-0034 decision 2).

Two earlier substitutes are **kept** rather than deleted — a local check that fails in two seconds
still beats a server-side rejection after a push, and defence in depth costs nothing here:

- **[`scripts/hooks/pre-push`](scripts/hooks/pre-push)** refuses the push on your machine. This is
  why `git config core.hooksPath scripts/hooks` above is not optional.
- **[`.github/workflows/branch-guard.yml`](.github/workflows/branch-guard.yml)** fails the run and
  red-X's the commit if something reached `main`/`develop` without a merged PR.

Typical loop:

```sh
git switch develop
git pull
git switch -c feat/money-tab-ledger
# ... work, commit in slices ...
git push -u origin feat/money-tab-ledger
gh pr create --base develop --fill
```

Delete the branch after merge.

---

## Commits

Conventional commits, and **the type is load-bearing**: CI reads the commit types in the merge range
and derives the semver segment from them — [`scripts/ci/detect_bump.sh`](scripts/ci/detect_bump.sh),
ADR-0025. A wrong type silently ships a wrong version.

| Type | Effect on version |
|---|---|
| `feat:` / `feat(scope):` | **MINOR** bump |
| `type!:` or a `BREAKING CHANGE:` trailer | **MAJOR** bump |
| `fix:` `chore:` `docs:` `test:` `refactor:` `perf:` `ci:` | **PATCH** bump |

Highest type in the range wins.

Message shape:

```
<type>(<optional scope>): <short description>

<why this change exists — the diff already shows the what>
```

**Never hand-edit `VERSION_CODE`, `VERSION_NAME`, or [`platform/versions.json`](platform/versions.json).**
CI owns all three (ADR-0011, ADR-0025). A manual bump on a branch that also carries `feat:` commits
produces a double bump, which is why it is a rule and not a preference.

**Label your PR** — GitHub's generated release notes categorise by label, not by commit type
([`.github/release.yml`](.github/release.yml)). An unlabelled PR still appears, just under
"Other changes":

```sh
gh pr create --base develop --fill --label enhancement   # or bug / documentation / security
```

Two more rules that matter more than they look:

- **One logical change per commit.** No formatting mixed with behaviour; no refactor mixed with a
  feature.
- Target ~100 changed lines per PR; split anything over ~1000.

---

## Before you open a PR

```sh
./gradlew regressionCheck                                  # unit + ArchUnit + coverage floor
./gradlew detekt                                           # lint
cd web && npm run lint && npm run typecheck && npm test    # if you touched web/
```

The pre-push hook additionally audits every changed feature module for `FeatureHost` wrapping, a
feature-flag entry, a Koin module, and inline secrets.

---

## Architecture constraints

These are not style preferences. Each one is mechanically enforced, and a PR that violates one goes
red rather than getting a review comment.

| Rule | Enforced by |
|---|---|
| `feature → feature` imports are **forbidden** | ArchUnit (`DependencyRulesTest`), runs inside `regressionCheck` |
| `vault → network / ai / analytics` is **forbidden** | ArchUnit + ADR-0031 decision 3 |
| `feature → data` **only via a Repository** | ArchUnit + Gradle module graph |
| `:libs:core` depends on **nothing internal** | ArchUnit — it is a pure library |
| Every feature route is wrapped in **`FeatureHost`** | pre-push hook + review; never a blank crash (PLATFORM.md §4) |
| **Kotlin + Compose + Koin + Coroutines/Flow only** | Hilt is *not* used — its Gradle plugin is incompatible with AGP 9 (ADR-0010) |
| **No hardcoded `dp`/`sp`/hex in screen files** | design tokens only, via [`platform/DESIGN-SYSTEM.md`](platform/DESIGN-SYSTEM.md) §1–§3; screen-level data comes from a config file |
| **No secrets in the repo or the APK** | GitLeaks (`Gate 2 · Security`), blocking on every PR |
| **Money is integer paise (`Long`)** for tracked amounts | ADR-0014 §4 — `BigDecimal` only for fractional calculation domains |
| **Consent before any data leaves the device**, persisted and revocable | `ConsentInterceptor` on the only PostgREST-capable client (ADR-0029) |
| **No SQL from the Supabase dashboard editor** | a schema change that is not a committed file is invisible to the drift guard (ADR-0032 decision 3) |
| **Pin third-party Actions to a commit SHA**, tag in a trailing comment | `uses: owner/action@<sha> # v2`. `actions/*` may stay on major tags. Resolve with `gh api repos/<owner>/<action>/commits/<tag> --jq .sha` |

**Architecture changes need an ADR.** Decisions in [`platform/DECISIONS.md`](platform/DECISIONS.md)
are ACCEPTED and the file is append-only. Disagree by proposing a new ADR — never by silently
diverging. Take the **next free number from `DECISIONS.md` itself**; a number "reserved" in an older
plan file is a guess, and that has already caused two collisions.

---

## If you are contributing from a fork

GitHub withholds every repository secret from a fork-originated workflow run, and forces
`GITHUB_TOKEN` to read-only. This is a platform security boundary, not a misconfiguration — a fork
PR is untrusted code, and giving it the release keystore or a Supabase token would be the actual
vulnerability. So some jobs are **designed** to skip or degrade on your PR:

| What you will see | Why |
|---|---|
| `Verify · database.ts freshness (same-repo PRs)` — **skipped** | It needs `SUPABASE_ACCESS_TOKEN`. The `head.repo.fork == false` guard in [`supabase-migrate.yml`](.github/workflows/supabase-migrate.yml) makes the failure mode a *skipped* job, which counts as passing |
| The sticky **PR summary comment** may not post | Posting needs `pull-requests: write`, which a fork PR's token does not have. The step is `continue-on-error` so it never blocks merge (ADR-0012) |
| GitLeaks may not post its own PR comment | Same read-only-token limitation. The **scan itself still runs and still blocks** |

**Please do not "fix" a skipped job by changing a workflow trigger.** In particular, switching
`pull_request` to `pull_request_target` is the exact change that would hand secrets to untrusted
branch code — the workflow says so in a comment above that guard. A PR that does this will be
declined.

Your PR is still fully gated by everything that matters: static analysis (ktlint, detekt, Android
lint), GitLeaks, version sync, and `regressionCheck` (unit tests, ArchUnit, coverage floor, debug
APK). A green run on those is a real green run.

One more thing to expect: **a first-time contributor's workflow run requires maintainer approval
before it executes at all.** Your PR will sit showing no checks until that click. It is not stuck,
and re-pushing will not speed it up.

---

## Working with AI agents

[`CLAUDE.md`](CLAUDE.md), [`platform/AGENTS.md`](platform/AGENTS.md), and the skills under
`platform/skills/` and `.claude/skills/` define how agents operate here. Two rules carry the most
weight:

- **Read the matching skill before starting the task**, not after.
- **Nothing enters a "built" list before the code exists.** The design system carried a component
  library that was never built for months, and screens were written against a fiction (ADR-0030).
  Verify by symbol search, then document.

---

## Reporting bugs and vulnerabilities

Bugs go through the [**Bug report** form](.github/ISSUE_TEMPLATE/bug_report.yml) — it is the only
issue type, and blank issues are disabled.

**Security vulnerabilities never go in an issue.** Use GitHub's private vulnerability reporting
(Security tab → Report a vulnerability), or email `kallileelasaikumar@gmail.com`. See
[`SECURITY.md`](SECURITY.md).