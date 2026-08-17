# AI PR-review bots — CodeRabbit + Qodo Merge

> **Status:** configured, **not yet installed.** The two config files are committed; the GitHub
> App installs are manual browser steps only the repo owner can perform — see §3.
>
> **Scope:** these bots are **advisory**. `.github/workflows/ci.yml` is the merge gate (ADR-0026).
> Neither bot requests changes, fails a status check, approves, or applies a label.

Config files:

| File | Bot | Read from |
|---|---|---|
| `.coderabbit.yaml` | CodeRabbit | repo root, **default branch** (`develop`) |
| `.pr_agent.toml` | Qodo Merge (PR-Agent) | repo root, **default branch** (`develop`) |

Both tools read their config from the repository's **default branch**, not from the PR head. Until
this branch merges to `develop`, PRs are reviewed with the vendors' stock defaults — expect the
first post-merge PR to be the first one that behaves as configured.

---

## 1. Why two bots, and who owns what

Two AI reviewers is only worth it if they do not say the same thing twice. Surfaces are split so
each bot owns exactly one output:

| Surface | Owner | Why |
|---|---|---|
| PR walkthrough / high-level summary / changed-files table | **CodeRabbit** | Its strongest output, and it is the bot with no monthly cap on a public repo — the always-needed surface should be the one that cannot run out. |
| Inline, line-level review comments | **CodeRabbit** | Same review pass produces them; splitting inline comments from the summary would mean paying for two full passes over the same diff. |
| A single collapsed **code-suggestion table** (`/improve`) | **Qodo Merge** | A genuinely different output shape: scored, concrete, ranked patches in one comment, not a stream of inline threads. This is where Qodo's capped monthly budget buys something CodeRabbit is not already producing. |
| PR description rewrite | **nobody** | Both bots *can* do it; both are configured not to. The maintainer's own PR body is left alone. |

Concretely, the anti-overlap settings are:

- `.pr_agent.toml` → `[github_app] pr_commands = ["/improve"]` — **not** `/describe`, **not**
  `/review`. So there is no second walkthrough and no second description.
- `.pr_agent.toml` → `[pr_code_suggestions] commitable_code_suggestions = false` — Qodo posts one
  collapsed table comment instead of inline threads, so it never interleaves with CodeRabbit's
  inline comments.
- `.pr_agent.toml` → `[pr_description] publish_description_as_comment = true` and
  `generate_ai_title = false` — even if someone types `/describe` by hand, it comments rather than
  overwriting the PR body or the title.
- `.coderabbit.yaml` → `reviews.high_level_summary: true` + `collapse_walkthrough: true` —
  CodeRabbit owns the summary, folded into one collapsible block.

**Note on the assignment.** The obvious split ("Qodo describes, CodeRabbit reviews") was rejected:
Qodo's free plan is capped at 30 PR reviews per org per month, and a month where the cap is hit
would leave PRs with no summary at all. CodeRabbit's public-repo tier has no monthly cap, so it
owns the surface that must always be there.

---

## 2. Free-tier status on a public repo

| Bot | Plan | Cost | Caveats |
|---|---|---|---|
| **CodeRabbit** | Free for public repositories — the full **Pro** feature set, indefinitely | $0 | Rate limited (roughly 200 files and 4 PRs per hour). Ample for a solo maintainer. |
| **Qodo Merge** | Free **Developer** plan via the hosted GitHub App | $0 | **30 PR reviews per month, pooled per org**, not per user. |

There is a second, better Qodo listing — **"Qodo - Free for Open Source Projects"**
(`github.com/marketplace/qodo-merge-pro-for-open-source`) — which is unmetered, but its stated
eligibility is *"your project is public and has 100+ GitHub stars."* `saikumarkalli/dhruv` will not
qualify on day one. Install the standard app on the Developer free plan now, and **re-check the
open-source listing if the repo ever passes 100 stars** — the config file needs no change either
way.

### Why there is no `.github/workflows/qodo-pr-agent.yml`

Qodo/PR-Agent has two install paths. The **self-hosted GitHub Action** was evaluated and rejected:

1. It requires a maintainer-supplied LLM key (`OPENAI_KEY`) as a repo secret — a real, uncapped
   per-PR bill, versus $0 for the hosted App.
2. It runs on GitHub-hosted runners, spending minutes against ADR-0026's standing budget of
   **≤ 90 billed minutes per merged PR**. The hosted App runs on Qodo's infrastructure and costs
   zero runner minutes.
3. A new workflow on a public repo is new attack surface (`pull_request` event handling, a secret
   in the environment) for no functional gain.

The hosted GitHub App is the correct integration path here, so no workflow file was created.

---

## 3. Install steps (browser only — the maintainer must do these)

Both are one-time OAuth/App installs. Do them **after** this branch is merged to `develop`, so the
config files are already on the default branch when the first review fires.

### 3.1 CodeRabbit

1. Go to <https://app.coderabbit.ai> and click **Login with GitHub**.
2. Authorize the CodeRabbit OAuth app for the `saikumarkalli` account.
3. In the CodeRabbit dashboard, **Add Repositories** → this installs the CodeRabbit GitHub App.
   On GitHub's install screen choose **Only select repositories** → `saikumarkalli/dhruv`.
   Do **not** grant "All repositories".
4. Back in the dashboard, confirm `dhruv` is listed and shows the **free public-repo plan** (no
   card required, no seat charge).
5. Open a throwaway PR against `develop` and confirm: one walkthrough comment, inline comments,
   **no poem**, and no "requested changes".
6. Sanity-check that the config was actually picked up — comment `@coderabbitai configuration` on
   any PR; it prints the resolved settings and where each came from. If it shows defaults, the
   file has not reached `develop` yet.

### 3.2 Qodo Merge

1. Go to <https://github.com/marketplace/qodo-merge-pro> and click **Install it for free** (the
   Developer plan, 30 reviews/month).
   *If the repo has 100+ stars, use <https://github.com/marketplace/qodo-merge-pro-for-open-source>
   instead — same config file, no monthly cap.*
2. On GitHub's install screen choose **Only select repositories** → `saikumarkalli/dhruv`.
3. Complete the Qodo sign-up when redirected, linking the same GitHub account.
4. Open a throwaway PR against `develop` and confirm exactly **one** Qodo comment appears: a
   collapsed **code suggestions** table. If you also see a PR-description rewrite or a second
   review summary, `.pr_agent.toml` did not load — check that it is on `develop` and that
   `allow_only_specific_folders` is not excluding everything the PR touched.

### 3.3 Post-install

Neither bot should be added to any required-status-check list. There is nothing to add them to
today anyway — classic branch protection is Pro/Team-gated for private repos and this repo's rules
are enforced by the pre-push hook plus `branch-guard.yml` (ADR-0032 correction). Once the repo is
public, branch protection becomes available on the Free tier; **still do not make either bot
required.** They are advisory by design, and `continue-on-error`-style advisory checks that are
required are the ADR-0012 mistake repeated.

---

## 4. What each bot is actually configured to look for

Neither config is stock. Both encode the platform's own rules so the bots cite an ADR instead of
generic best practice. The shared rule set, drawn from `platform/AGENTS.md`, `PLATFORM.md`,
`DECISIONS.md` and `DESIGN-SYSTEM.md`:

| Rule | Source |
|---|---|
| `feature → feature` imports FORBIDDEN; `vault → network/ai/analytics` FORBIDDEN; `feature → data` only via a Repository (no `*Dao`/`*Dto` import); `:libs:core` imports nothing internal | PLATFORM.md §4, `DependencyRulesTest.kt` |
| DI is **Koin only** — `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@Inject`, `@Provides`, `@Binds`, `dagger.*` are flagged as wrong for this project | ADR-0010 |
| Kotlin + Compose + Coroutines/Flow only — no RxJava, no LiveData in new code, no new `.java` files, no `GlobalScope`, no `runBlocking` outside tests | CLAUDE.md hard rules |
| Every NavHost feature route wrapped in `FeatureHost`; every new feature has a `platform/feature-flags/dhruv-finance.json` row | AGENTS.md, PLATFORM.md §4 |
| No raw `.dp`/`.sp`, hex colours, `MaterialTheme.colorScheme/typography`, or hardcoded user-visible strings in screen files — tokens + `strings.xml` + `:libs:core` components only | DESIGN-SYSTEM.md §5, §10 |
| Tracked money is integer paise (`Long`); `BigDecimal` only in the calculator domains; a `Double`/`Float` carrying money is a defect | ADR-0014 §4 |
| `finance.valuations` is append-only — any client-side UPDATE/DELETE, or an UPDATE/DELETE RLS policy on it, is flagged | ADR-0029 |
| Any new off-device data path needs the persisted consent gate; a PostgREST-capable client that bypasses `ConsentInterceptor` is a structural DPDP bypass | ADR-0005, ADR-0029 |
| No secrets, keys or tokens in code, config or test fixtures | AGENTS.md |
| Supabase: per-app schema (`finance.*`, never bare `public.*`), RLS + `user_id = auth.uid()`, explicit grants to `authenticated` and never `anon`, no env-conditional SQL, no edits to applied migrations | ADR-0032, ADR-0033 |
| Workflows: SHA-pinned actions, secrets via `env:`, least-privilege `permissions:`, no re-running PR gates on push | ADR-0026, ADR-0032 |

CodeRabbit gets these as `reviews.path_instructions` (per-glob, so a `.sql` file is judged by the
database rules and a `*Screen.kt` by the design-system rules). Qodo gets them as
`[pr_code_suggestions].extra_instructions` and `[pr_reviewer].extra_instructions`, since its config
format has no per-path hook.

### Paths

Reviewed: `apps/**`, `libs/**`, `build-logic/**`, `web/src/**`, `supabase/**`, `scripts/**`,
`.github/workflows/**`.

Ignored: `**/build/**`, `web/node_modules/**`, `web/dist/**`, `.gradle/**`, `.idea/**`,
`**/__pycache__/**`, `.claude/skills/**` (vendored third-party skill files, not this project's
code), `.claude/worktrees/**`, `gradle/wrapper/**`, and binary types (`*.jar`, `*.ttf`, `*.otf`,
`*.png`, `*.webp`, `*.jks`, `*.keystore`).

`docs/`, `platform/`, `specs/` and the root `*.md` tree are deliberately **not** line-reviewed —
but CodeRabbit ingests `CLAUDE.md`, `CONTRIBUTING.md` and the four `platform/*.md` contracts as
`knowledge_base.code_guidelines`, so they inform every review as context.

### Linters: what is off, and why

CI is authoritative. Any bot-side linter that duplicates a CI gate is disabled — a second opinion
on a finding that already fails the build is pure noise.

| Disabled in `.coderabbit.yaml` | Because |
|---|---|
| `detekt` | `ci.yml` Gate 1 runs `./gradlew detekt` and fails the build |
| `eslint`, `biome`, `oxc` | `web-ci.yml` runs `npm run lint` |
| `gitleaks`, `trufflehog` | `ci.yml` Gate 2 runs GitLeaks as a blocking gate |
| `github-checks` | the Dhruv CI Bot sticky comment already reports every gate |
| `markdownlint`, `languagetool` | prose nits across a docs-heavy repo |
| `yamllint` | `actionlint` covers workflows without the style noise |
| `sqlfluff` | SQL style noise; `squawk` covers actual migration danger |

| Enabled | Because CI does not cover it |
|---|---|
| `actionlint`, `zizmor` | nothing lints or security-reviews the workflows themselves |
| `shellcheck` | `scripts/ci/detect_bump.sh` and workflow `run:` bodies |
| `ruff` | `scripts/ci/*.py`, `scripts/db/*.py` |
| `semgrep` | security patterns beyond secret scanning |
| `squawk` | dangerous Postgres migrations |
| `osvScanner` | OWASP dependency-check is monthly and warn-only (ADR-0026) |

Qodo has no bot-side linters to disable; its `extra_instructions` explicitly tell it not to repeat
ktlint / detekt / ESLint / GitLeaks findings.

---

## 5. Noise control and how to silence them

### Already configured

- **No draft reviews.** CodeRabbit: `reviews.auto_review.drafts: false`. Qodo: the hosted App skips
  drafts in its webhook handler unconditionally — that is hardcoded upstream, not a setting. Mark a
  PR ready-for-review to trigger the first pass.
- **Auto-review on open and on subsequent pushes**, then self-limiting: CodeRabbit re-reviews
  incrementally and pauses itself after 5 reviewed commits; Qodo updates the *same* comment in
  place (`persistent_comment = true`) rather than adding one per push.
- **Never blocking.** `request_changes_workflow: false`, `commit_status: false`,
  `fail_commit_status: false`; Qodo neither approves nor labels.
- **Filler off.** CodeRabbit: `poem`, `in_progress_fortune`, `sequence_diagrams`,
  `estimate_code_review_effort`, `suggested_labels`, `suggested_reviewers`, `slop_detection` and
  the whole `finishing_touches` block are all off. `tone_instructions` explicitly bans praise,
  diff-restatement and LGTM filler.
- **Low-value suggestions dropped.** Qodo: `suggestions_score_threshold = 6` and
  `focus_only_on_problems = true`.
- **Bot PRs skipped.** Both ignore `dependabot[bot]` and `github-actions[bot]`.

### Per-PR levers

| Want | CodeRabbit | Qodo Merge |
|---|---|---|
| Skip this PR entirely | put `@coderabbitai ignore` in the **PR description**, or title it `WIP`/`DO NOT MERGE` | label it `no-ai-review` or `do-not-merge`, or title it `WIP` |
| Pause mid-PR | comment `@coderabbitai pause`, later `@coderabbitai resume` | convert to draft |
| Re-run on demand | `@coderabbitai review` (incremental) / `@coderabbitai full review` | comment `/improve` |
| Mark all its comments resolved | `@coderabbitai resolve` | resolve/collapse the single comment |
| See what config actually loaded | `@coderabbitai configuration` | check `.pr_agent.toml` is on `develop` |

### Repo-wide levers

- Quieter still: `.coderabbit.yaml` → `reviews.profile: quiet`.
- Turn CodeRabbit off without uninstalling: `reviews.auto_review.enabled: false`.
- Turn Qodo off without uninstalling: `.pr_agent.toml` → `[github_app] pr_commands = []` and
  `handle_push_trigger = false`. Manual `/improve` still works.
- Conserve Qodo's 30/month budget: set `handle_push_trigger = false` so it runs once per PR
  instead of once per push.

**Deliberately not set:** `[config] ignore_pr_source_branches`. The vendor's own example ignores
`develop`/`main` as source branches — which would silently skip the `develop → main` promotion PR,
the single most important PR in this repo (ADR-0032). Do not copy that example in.

---

## 6. Public-repo consequence: two more third parties with read access

Both bots read the full contents of every PR — the diff, the surrounding files they pull for
context, the PR title and body, and the review conversation. Installing either one grants a
**third-party service standing read access to this repository** and sends its source code to that
vendor's LLM provider.

That is acceptable here specifically **because the repo is going public**: the code they read is
code anyone can already read. This is exactly why both bots are being enabled in the same change
that flips the repo to public, and not before.

What this does *not* change:

- **Secrets stay out of the repo, as always.** `.env` is gitignored, GitLeaks gates every PR
  including docs-only ones, and the release secrets live in GitHub Environments. A bot reading the
  tree must find nothing worth having. Going public does not relax this — it raises the stakes.
- **Least-privilege installs.** Both Apps are installed on `saikumarkalli/dhruv` **only**, never
  "All repositories". If a private repo is ever added to this account, neither bot should follow.
- **Knowledge base stays local.** `.coderabbit.yaml` pins `learnings`, `issues` and
  `pull_requests` scope to `local`, so nothing this repo teaches CodeRabbit leaks into another
  repo's reviews (and vice versa).
- **Vault is unaffected** — it does not exist yet, and when it does, its threat model
  (ADR-0003, ADR-0031) is about key custody, not source visibility. No bot ever sees a user secret.

Revocation, if ever needed, is GitHub → *Settings → Integrations → Applications → Installed GitHub
Apps → Configure → Uninstall*, for each app independently. Uninstalling one does not affect the
other; the config files can stay in place.

---

## 7. Where the rules come from — keep these in sync

The bot rules are a **copy** of the platform contracts, and copies drift. When any of the following
change, update the matching `path_instructions` entry in `.coderabbit.yaml` and the
`extra_instructions` block in `.pr_agent.toml` in the same change set:

- `platform/AGENTS.md` — hard rules, module boundaries, definition of done
- `platform/PLATFORM.md` §4 (dependency table), §7 (security layers)
- `platform/DESIGN-SYSTEM.md` §5 (component library), §7 (screen states), §10 (copy)
- `platform/DECISIONS.md` — any new ADR that adds or overturns a review rule
- `.github/workflows/ci.yml` / `web-ci.yml` — if a gate is added or removed, the corresponding
  bot-side linter must be disabled or re-enabled so the two never overlap

This is the same drift risk ADR-0030 diagnosed for the design docs and §12 of DESIGN-SYSTEM.md
records for the web tokens: two hand-maintained copies of one truth, with nothing automatic
catching divergence. Recording it here is the mitigation.