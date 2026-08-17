# Contributing to Dhruv

This is a solo-maintained private monorepo. These rules exist so that the maintainer (and any AI
agent working in the repo) behaves consistently, and so a second contributor could be onboarded
without archaeology.

**Read first**, in this order — they are the source of truth and this file is not:

1. [`platform/AGENTS.md`](platform/AGENTS.md) — session rules and hard constraints
2. [`platform/PLATFORM.md`](platform/PLATFORM.md) — architecture (the *what*)
3. [`platform/DECISIONS.md`](platform/DECISIONS.md) — the decision register (the *why*)
4. [`platform/DESIGN-SYSTEM.md`](platform/DESIGN-SYSTEM.md) — binding design contract for all UI

---

## One-time setup

```sh
git clone https://github.com/saikumarkalli/dhruv.git
cd dhruv

# REQUIRED — activates the versioned hooks in scripts/hooks/
git config core.hooksPath scripts/hooks

cp .env.example .env        # fill in your own dev values
```

`JAVA_HOME` must point at the Android Studio JBR (JDK 17+); the project is on AGP 9.

---

## Branching

| Branch | Role | How it advances |
|---|---|---|
| `develop` | **DEV** — default branch. `dhruv-dev`, Vercel Preview, debug APK. | merged PR only |
| `main` | **PROD** — `dhruv-prod`, Vercel Production, signed APK + GitHub Release. | merged `develop -> main` PR only |
| `feat/*` `fix/*` `chore/*` `refactor/*` `docs/*` | feature work | branch from `develop`, PR back to `develop` |

**`main` and `develop` are PR-only. Never push to either directly.**

GitHub's own enforcement of that is unavailable here — rulesets and classic branch protection are
both Pro-gated on a private repo, and this repo is on GitHub Free (verified: `GET /rulesets` →
`403 "Upgrade to GitHub Pro"`). Two substitutes stand in:

- **[`scripts/hooks/pre-push`](scripts/hooks/pre-push)** refuses the push on your machine. This is
  why `git config core.hooksPath scripts/hooks` is not optional.
- **[`.github/workflows/branch-guard.yml`](.github/workflows/branch-guard.yml)** fails the run and
  red-X's the commit if something reached `main`/`develop` without a merged PR.

The moment the account is on GitHub Pro, run
[`scripts/env/apply-branch-protection.ps1`](scripts/env/apply-branch-protection.ps1) — it applies
real rulesets and makes both substitutes redundant.

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

Conventional commits, and **the type is load-bearing** — CI reads it to pick the semver segment
(ADR-0025):

| Type | Effect on version |
|---|---|
| `feat:` / `feat(scope):` | **MINOR** bump |
| `type!:` or a `BREAKING CHANGE:` trailer | **MAJOR** bump |
| `fix:` `chore:` `docs:` `test:` `refactor:` `perf:` `ci:` | **PATCH** bump |

Highest type in the merge range wins.

Message shape:

```
<type>(<optional scope>): <short description>

<why this change exists — the diff already shows the what>
```

**Label your PR** — GitHub's generated release notes categorise by label, not by commit type
(`.github/release.yml`). An unlabelled PR still appears, just under "Other changes":

```sh
gh pr create --base develop --fill --label enhancement   # or bug / documentation / security
```

Rules that matter more than they look:

- **One logical change per commit.** No formatting mixed with behaviour; no refactor mixed with a
  feature.
- **Never hand-edit** `VERSION_CODE`, `VERSION_NAME`, or `platform/versions.json` — CI owns them.
  A manual bump on a branch that also carries `feat:` commits produces a double bump.
- Target ~100 changed lines per PR; split anything over ~1000.

---

## Before you open a PR

```sh
./gradlew regressionCheck      # unit + ArchUnit + JaCoCo report + coverage floor
./gradlew detekt               # lint
cd web && npm run lint && npm run typecheck && npm test
```

`regressionCheck` is exactly what CI Gate 3 runs. If it passes locally it passes there.

The pre-push hook additionally audits every changed feature module for `FeatureHost` wrapping, a
feature-flag entry, a Koin module, and inline secrets.

---

## Hard rules

These are not style preferences — CI, ArchUnit, or an ADR enforces each one.

- **Do not redesign architecture.** Decisions in `DECISIONS.md` are ACCEPTED. Disagree by proposing
  a new ADR, never by silently diverging. Take the next free number from `DECISIONS.md` itself — a
  number "reserved" in some older plan file is a guess, and has collided twice already.
- **Module boundaries.** `feature -> feature` forbidden. `vault -> network/ai/analytics` forbidden.
  `feature -> data` via Repository only. `core` depends on nothing internal. ArchUnit fails the
  build on a violation.
- **Every feature route is wrapped in `FeatureHost`.** Never a blank crash.
- **No secrets in the repo or the APK.** GitLeaks gates CI.
- **Kotlin + Compose + Koin + Coroutines/Flow only.** Not Hilt — its Gradle plugin is incompatible
  with AGP 9 (ADR-0010).
- **Money is integer paise (`Long`)** for tracked amounts. `BigDecimal` only for fractional
  calculation domains.
- **No hardcoded values in screens.** Colours, spacing, radii and type come from tokens; screen-level
  data comes from a config file. No raw hex, no `.dp`/`.sp` literals in a feature file.
- **Consent before any data leaves the device** (DPDP), and it must be persisted and revocable.
- **No SQL from the Supabase dashboard editor.** A schema change that is not a committed file is
  invisible to the drift guard and voids the dev/prod equivalence the whole design rests on.
- **Pin third-party GitHub Actions to a commit SHA**, tag in a trailing comment:
  `uses: owner/action@<sha> # v2`. A mutable tag can be repointed at any commit, and the `release`
  job holds the signing keystore. `actions/*` stay on major tags (GitHub-owned). Dependabot reads
  the comment and bumps the SHA, so pinning does not mean going stale. Resolve one with
  `gh api repos/<owner>/<action>/commits/<tag> --jq .sha`.

---

## Working with AI agents

`CLAUDE.md`, `platform/AGENTS.md`, and the skills under `platform/skills/` and `.claude/skills/`
define how agents operate here. Two rules carry the most weight:

- **Read the matching skill before starting the task**, not after.
- **Nothing enters a "built" list before the code exists.** The design system carried a component
  library that was never built for months, and screens were written against a fiction (ADR-0030).
  Verify by symbol search, then document.

---

## Reporting bugs

Open an issue using the **Bug report** form — it is the only issue type, and it is required
(blank issues are disabled). Security vulnerabilities go to email instead, never to an issue:
see [`SECURITY.md`](SECURITY.md).