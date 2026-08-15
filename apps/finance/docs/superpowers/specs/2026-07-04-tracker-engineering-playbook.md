# Dhruv Tracker — Engineering Playbook (roles, gates, deployment)

> Status: **BINDING** for every tracker phase. The "who does what, when" contract from spec to
> deployment. Umbrella: `2026-07-03-tracker-roadmap-overview.md`.

## Roles matrix (skill / agent / plugin per stage)

| Stage | Tool | When |
|---|---|---|
| Requirements & design | `superpowers:brainstorming`, `spec-driven-development` | before any phase starts |
| Planning | plan mode + `Plan` agent, `superpowers:writing-plans` | after spec approved |
| Design intelligence | `ui-ux-pro-max` (+ `design-system` skill) | UI spec + UI build tasks |
| Implementation discipline | `superpowers:test-driven-development` (every logic change), `incremental-implementation` | all coding tasks |
| Code exploration | `Explore` agent (read-only) | before touching unfamiliar areas |
| Debugging | `superpowers:systematic-debugging`, `debugging-and-error-recovery` | any failure — no guess-fixes |
| Code review | `/code-review` (high effort) on branch diff + `code-review-and-quality` skill; every finding fixed or explicitly waived | end of each task block + pre-PR |
| Bug hunt | `/code-review` bug-focused pass + `regressionCheck` + spec smoke script | pre-PR |
| Security & privacy | `/security-review` + `/dhruv-security` (DPDP-aware) + `security-and-hardening` skill; GitLeaks + OWASP dep-check (existing CI gates) | after auth work + pre-PR |
| Architecture guard | `/dhruv-boundaries` + ArchUnit (CI) | after any module/wiring change |
| UX review | `/dhruv-ui-review` + design-system a11y checklist | after UI tasks |
| Coverage | `/dhruv-coverage` + JaCoCo floor (regressionCheck) | pre-PR |
| Pre-merge verdict | `/dhruv-pre-merge` (single PASS/FAIL) | final gate |
| Verification honesty | `superpowers:verification-before-completion` | before any "done" claim |
| Release/deploy | existing CI (ADR-0011/0012/0013): 4 gates → version-bump → auto-tag → GitHub Release APK; `shipping-and-launch`, `git-workflow-and-versioning` | merge to develop |
| Observability | `observability-and-instrumentation`; Crashlytics `setCustomKey("module", …)` + perf traces (loans template) | built into every ViewModel |

## Framework-protection rules (existing app must never break)

1. Existing feature modules **byte-untouched** (calculator, loans, investments, tax, everyday,
   currency, unit, date, time, assistant) — only their nav entry points move (verbatim
   `FeatureHost` blocks).
2. `AppDatabase` untouched (stays v5) — tracker owns zero Room tables.
3. Shared-file edits (`MainActivity`, `CalculatorApplication`, `DataModule`,
   `settings.gradle.kts`, root `coveredModules`, `proguard-rules.pro`, `ci.yml`) are
   **additive-only, smallest possible diff**. `:libs:core` additions are new files only — no edits
   to existing core APIs; existing screens adopt new components in later phases, not P1.
4. ArchUnit + full `regressionCheck` green at **every commit** — never commit red.
5. Feature flag = kill switch: `networth.enabled=false` must yield a working app identical to
   today's minus Home (FeatureDisabledCard) — verified in the smoke script.
6. New third-party deps confined to `:apps:finance:data` (and plain AndroidX UI libs where
   unavoidable); build-verified before dependent code is written.
7. Rollback story: disable flag (asset JSON now, Remote Config later) or revert PR; client holds
   no data, so no client-side data-loss path exists.

## One-go development guarantees

1. **Zero framework unknowns** — no new networking/serialization framework (ADR-0014 §6:
   Retrofit/Moshi/OkHttp against Supabase REST); only stable AndroidX additions.
2. **Schema frozen at spec time** — full SQL incl. RLS, `auth.uid()` defaults, `moddatetime`
   triggers, `delete_my_account()` RPC lives in the P1 spec; no mid-development schema edits.
3. **API surface frozen** — repository interfaces + `AuthState`/`DashboardUiState`/`EditorUiState`
   defined in spec before implementation.
4. **All UI states enumerated up front** — ConsentNeeded / SignedOut / Loading / Content / Error /
   Offline / NotConfigured + empty + withdraw/delete flows.
5. **Fail-fast ordering** — the only external integration (Google sign-in + Supabase) is verified
   on-device at **M0**, immediately after the data layer, before any feature UI exists.
6. **Gap register complete** — see `2026-07-04-p1-gap-analysis.md`; every gap has a decided fix
   folded into a task; outcomes recorded at PR time.

## Deployment pipeline (existing — tracker rides it unchanged)

PR → 4 CI gates (static analysis; security scan GitLeaks+OWASP; tests + ArchUnit + coverage floor;
build debug+release) → sticky PR summary (Dhruv CI Bot) → self-merge to `develop` → `version-bump`
(1.3.0 → 1.3.1 …) → `auto-tag` `dhruv-finance-v*` → Release workflow → signed APK on GitHub
Release. Play Store deferred (ADR-0008). New for tracker: release job writes `.env` from GitHub
secrets before `assembleRelease`.

## Secrets inventory (never in repo/APK source)

| Secret | Where |
|---|---|
| Keystore (base64) + store/key passwords + alias | GitHub secrets (existing) |
| `GEMINI_API_KEY` | `.env` local / (not yet in CI) |
| `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID` | `.env` local + GitHub secrets (new); `.env.example` holds empty defaults |

Anon key is publishable-by-design under RLS but still kept out of the repo (GitLeaks gate).
Session tokens: encrypted DataStore only. No financial values or PII in logs/crash breadcrumbs.
