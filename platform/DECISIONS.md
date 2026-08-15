# Dhruv Platform — Decision Register

Lightweight ADRs. Each records *why*, not *what* (the *what* is in `PLATFORM.md`).
All decisions below are **ACCEPTED**. Split into individual `adr/NNNN-*.md` files later if useful.

---

## ADR-0001 — Monorepo, not multi-repo
**Context.** Solo maintainer; stated primary driver is cost (and implicitly time). The original
plan used 8 repos + a contracts repo + GitHub Packages + git submodules with auto-update PRs.
**Decision.** One repo (`dhruv`) with Gradle modules.
**Why.** Fault isolation and module-dependency rules (ArchUnit/Gradle) are identical in one repo or
eight, so the split bought almost nothing while taxing every shared change with a
publish→version→consume cycle plus submodule pain. Splitting later is easy; un-splitting is not.
**Consequences.** GitHub Packages and the submodule/auto-PR machinery are removed entirely.
`platform/` becomes a top-level docs folder read at the start of every AI session.

## ADR-0002 — Online AI: proxy + per-device quota, BYO override
**Context.** "Online AI = Gemini API" with no key custody, quota, or cost ceiling — directly at odds
with the cost-first driver. An embedded key is extractable from the APK and drainable.
**Decision.** Default route through a Cloudflare Worker proxy holding the key and enforcing a
per-device quota; users may paste their own key to bypass the quota at zero cost to the platform.
**Why.** Only secure *and* free option. Keeps the key off-device, caps spend via free-tier quota,
and gives power users an escape hatch.
**Consequences.** A consent screen must precede online calls (DPDP). One small always-on Worker to
maintain. BYO-key handling lives in Settings.

## ADR-0003 — Vault key: master-password-derived + recovery key
**Context.** "E2E encrypted backup" keyed by a hardware-backed Keystore key is impossible — those
keys are non-exportable and device-bound, so no new-device restore exists. Biometric enrollment can
also invalidate Keystore keys, destroying data.
**Decision.** Derive the real vault key from a user master password (Argon2id). Show a one-time
recovery key. Biometric/Keystore is a convenience unlock layer only.
**Why.** A password manager that cannot survive a phone upgrade is not worth shipping. A
user-secret-derived key is the only thing that is both restorable and truly E2E.
**Consequences.** Forgotten master password + lost recovery key = unrecoverable by design (stated to
the user). Adds a recovery-key setup flow. Vault is built last, after this flow is fully specced.

## ADR-0004 — Conflict resolution: HLC-based LWW
**Context.** The doc had two contradictory rules ("LWW default" vs "Client-Wins always"); raw
client-timestamp LWW is unreliable under cross-device clock skew.
**Decision.** Last-Write-Wins keyed on a Hybrid Logical Clock; field-level merge for Notes.
**Why.** Removes the skew bug and the internal contradiction; HLC gives a causal, monotonic ordering
without a central clock.
**Consequences.** Entities carry an HLC stamp. Sync contract designed now, built in Phase 2.

## ADR-0005 — DPDP compliance as a first-class layer
**Context.** India-based, shipping to Indian users; DPDP Rules 2025 in force (enforcement May 2027).
No "legitimate interests" basis; under-18 = child; 7-day erasure; consent notices.
**Decision.** Consent screen before any data leaves the device; guaranteed hard-delete path within
7 days; Play Data Safety declaration for AI traffic.
**Why.** Non-optional legal exposure; retrofitting consent/deletion later is costly.
**Consequences.** "Never hard delete" is amended to "soft-delete UX, guaranteed hard-delete on
request/timer." Tombstone GC (ADR-adjacent) implements the purge.

## ADR-0006 — Firebase for flags, crash, performance
**Context.** Choice between Firebase free tier and self-hosted GitHub-raw JSON for flags.
**Decision.** Firebase Remote Config + Crashlytics + Performance (Spark free tier).
**Why.** Free, zero-maintenance, supports targeting and caching. The raw-JSON alternative loses
targeting/caching and exposes config publicly for no benefit. Aligns with cost *and* time drivers.
**Consequences.** A Firebase dependency in every app; vault keeps a minimal Crashlytics surface
(`vault_module_error` only).

## ADR-0007 — On-device AI is a progressive enhancement
**Context.** Gemini Nano reaches a narrow device set (Pixel 8+, Galaxy S24+, SD 8 Gen 3+; "v3" tier
is 2026 flagships only).
**Decision.** Default assumption is online/no AI; a capability check gates Nano with graceful
fallback.
**Why.** Treating Nano as a baseline would break AI features for the large majority of installs.
**Consequences.** AI features are designed online-first; Nano is an optional accelerator.

## ADR-0008 — Signed APK now; AAB + Play App Signing deferred
**Context.** No Play launch is planned yet; distribution is direct APK for now. Play will be
revisited later.
**Decision.** Build a **signed release APK** using the existing `dhruv-calc` keystore; CI attaches it
to a **GitHub Release** per version tag. AAB output, Play App Signing, internal/production tracks,
and staged rollout are deferred until a Play launch is planned.
**Why.** APK is buildable and distributable anytime with no Play setup, so the release loop isn't
gated on Play. Keeping the existing keystore avoids re-signing churn.
**Consequences.** The build job is written so APK→AAB is a one-line swap later. **DPDP consent +
erasure (ADR-0005) are NOT Play-dependent and apply now**; only the Play Data Safety form is
deferred. Users must enable install-from-unknown-sources for direct APKs.

---

## Resolved "pending decisions" from the original doc
- **Firebase vs self-hosted** → Firebase (ADR-0006).
- **Public vs private repos** → moot under the monorepo (ADR-0001): one private repo, no GitHub
  Packages.

## ADR-0009 — Branch strategy: develop for all work, main for Play Store only
**Context.** Need a clear branch model for a solo developer with incremental APK releases now
and a future Play Store launch.
**Decision.** `develop` is the default branch — all feature work, all PRs, APK builds, GitHub
Releases. `main` is reserved for Play Store deployment only; PRs to main come only from develop.
Both branches run identical 4-gate CI. `develop` builds a signed APK; `main` builds a signed AAB.
**Why.** Keeps the release loop simple now (tag develop → APK on GitHub) while ensuring main is
always Play-ready (AAB, same CI gates) whenever that decision is made. No last-minute pipeline
changes needed at Play launch time.
**Consequences.** develop is set as the GitHub default branch. Branch protection on both branches.
All feature branches: `feat/* → develop`. Play launch = merge develop → main + tag.

---

## ADR-0010 — DI is Koin (Hilt deferred); Finance split is thematic + hub-navigated
**Context.** PLATFORM.md originally specified Hilt, but the Hilt Gradle plugin (2.52) is incompatible
with AGP 9 (it looks up the removed `BaseExtension`), so the app was already wired with Koin. Phase 4
also had to place 10 calculators that lived in one `FinanceViewModel`/`FinanceScreen` and a 14-tool
`ConverterScreen` into modules, against a strict "code-move, not rewrite" rule.
**Decision.**
1. **Koin is the DI framework** for all modules until a Hilt version supporting AGP 9 lands. Each
   feature exposes a `module {}` object; the app aggregates them in `CalculatorApplication`.
2. **Finance calculators are grouped thematically** into `loans` (EMI + comparison), `investments`
   (SIP/ROI/FD-RD), `tax` (GST/salary), `everyday` (interest/discount/tip/inflation) — superseding the
   originally-sketched `emi`/`sip`/`loan` modules.
3. A shared **`:apps:finance:data`** module holds the single Room DB + repositories; features depend
   on it (Repository-only), satisfying `feature → data` without splitting the database.
4. The app keeps its **pager + bottom-nav** UX; `currency`+`unit` and the four finance themes are
   presented behind **Converter/Finance hub** screens, each sub-feature wrapped in `FeatureHost`.
**Why.** Koin is the only DI that builds today; thematic grouping keeps the screen split a move rather
than a rewrite; a shared data module avoids a risky multi-database migration; hubs preserve existing
navigation while honouring "every route in FeatureHost".
**Consequences.** Docs saying "Hilt only" are corrected. `GeminiRepository` takes its key as a ctor
arg (app supplies `BuildConfig.GEMINI_API_KEY`) so it can live in `:data` and be shared without a
`feature → feature` edge. `date`/`time` ship flag-disabled; `assistant` ships `enabled = true` but
**version-gated** (`minVersion 1.2.0`, so hidden until the app reaches 1.2.0) and consent-gated — the
`FeatureFlagResolver` now honors `minVersion`/`requiresConsent` (was boolean-only).
`AlarmViewModel`/`BootReceiver` still touch Room directly (documented follow-up).

## ADR-0011 — CI auto-increments patch version on every merge
**Context.** The original `version-bump` CI job only incremented `versionCode` (build number) and
updated `buildNumber` in `platform/versions.json`. The semantic `version` field (e.g. `"1.2.0"`)
was never touched by automation. The `auto-tag` job reads that field to derive the tag name
(`dhruv-finance-v1.2.0`), and it is idempotent — once a tag exists it is never re-created. Result:
the tag was created exactly once (on the first merge) and silently skipped on every subsequent merge,
so no new GitHub Release was ever produced. Additionally, `VERSION_NAME` was absent from
`gradle.properties`, so `BuildConfig.VERSION_NAME` and the APK filename always defaulted to `"1.0"`.
**Decision.** The `version-bump` job now also increments the patch segment (`MAJOR.MINOR.PATCH+1`)
for every active app in `platform/versions.json` and writes `VERSION_NAME` to `gradle.properties`.
Major and minor remain manually controlled. The commit message format changes from
`auto-bump versionCode to N` to `auto-bump to vX.Y.Z (versionCode=N)`.
**Why.** A patch bump on every merge matches the project's versioning semantics (PATCH = fix/merge)
and requires zero developer action for the common case. It guarantees that every merge produces a
unique tag and therefore a unique GitHub Release with a correctly-named APK. Manual major/minor
bumps handle breaking changes and new feature modules respectively, consistent with semver intent.
Keeping the decision in CI (not in developer workflow) removes a class of "forgot to bump" errors.
**Consequences.** `platform/versions.json` and `gradle.properties` are modified by CI on every
develop/main push (two extra changed files in the auto-bump commit). Developers must not manually
edit `VERSION_CODE`, `VERSION_NAME`, or `buildNumber` — those are CI-owned. To ship a minor/major
release, bump only the `version` field in `platform/versions.json` before merging; CI handles
everything else from that baseline.

## ADR-0012 — PR CI summary comment, posted via a dedicated "Dhruv Bot" GitHub App
**Context.** Before this change, the only thing in `ci.yml` that ever commented on a PR was
GitLeaks (`gitleaks/gitleaks-action@v2`), and only when it found a leaked secret — a clean run
produced zero PR feedback, indistinguishable from CI not having run at all. Separately, the default
`actions/github-script` identity (`GITHUB_TOKEN`) always posts as `github-actions[bot]`, whose
name/avatar cannot be customized.
**Decision.** Added a `pr-summary` job (Post-build, runs only on `pull_request`, `if: always()`)
that posts/updates a single sticky comment (matched via a hidden HTML marker, edited in place on
every push rather than duplicated) summarizing all four gate results — security, OWASP, tests,
build — on every PR run, pass or fail. To brand the comment, a dedicated GitHub App named
**"Dhruv Bot"** (custom avatar, `Issues: Read & write` permission only, installed solely on this
repo) mints a short-lived installation token via `actions/create-github-app-token@v1`, fed from the
`DHRUV_BOT_APP_ID` / `DHRUV_BOT_PRIVATE_KEY` repo secrets. If minting fails for any reason (secrets
missing, App not installed, transient API error), the step falls back to the default `GITHUB_TOKEN`
(`steps.dhruv-bot.outputs.token || github.token`) so commenting never breaks the pipeline.
**Why.** A GitHub App is the only way to get a custom bot name/avatar with the official "Bot" badge;
a long-lived PAT under a fake human account was rejected as a less secure, harder-to-rotate
alternative. Scoping the App to `Issues: Read & write` only (not `Pull requests`) follows
least-privilege, since PR conversation comments are implemented via the Issues API. The
`continue-on-error` + `||` fallback chain mirrors the same "never block merge over a comment"
principle already applied to GitLeaks' fork-PR token limitation.
**Consequences.** Two new repo secrets (`DHRUV_BOT_APP_ID`, `DHRUV_BOT_PRIVATE_KEY`) exist in GitHub
Actions secrets — never in the repo or APK, consistent with the GitLeaks-gated "no secrets in repo"
rule. `pr-summary` is intentionally excluded from branch-protection required checks: because
`continue-on-error: true` makes the job always report success, requiring it would be purely
cosmetic — it is informational only, never a merge gate. OWASP's row in the comment will always show
✅ regardless of actual findings (pre-existing `continue-on-error: true` on that scan step, §11);
this is a known, accepted limitation, not something this change fixes.

---

## ADR-0013 — Pre-merge regression suite: one `regressionCheck` gate, JaCoCo (not Kover) coverage
**Context.** Gate 3 already ran `testDebugUnitTest` on every PR, but there was (a) no coverage
measurement or floor, so coverage could silently erode, and (b) no visible test/coverage result on a
merge — a push to `develop`/`main` produces no PR comment, so nothing surfaced the numbers. The most
correctness-critical code (the finance calculators) needed a non-regression ratchet, and the
maintainer asked to *see* the test results and coverage on every merge, not just on PRs.
**Decision.**
1. A single Gradle entry point **`./gradlew regressionCheck`** = every module's `testDebugUnitTest`
   (ArchUnit + Robolectric live in the debug variant) + a **merged JaCoCo report** +
   **`jacocoCoverageVerification`** (a global LINE-coverage floor). CI Gate 3 runs this.
2. **Coverage is JaCoCo, not Kover.** Kover 0.9.1 applies on AGP 9.1.1 but its Android integration
   creates **no per-variant report tasks** and measures nothing — the same class of AGP-9
   incompatibility that rules out Hilt (ADR-0010). JaCoCo is the Gradle built-in and AGP-version-
   agnostic: modules emit exec data via `enableUnitTestCoverage = true` (set in the
   `dhruv.android.library`/`.application` convention plugins) and the root aggregates it. On AGP 9 the
   Kotlin classes live under `build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes`.
3. The floor starts at the **measured baseline (~6.7% line; most of the tree is Compose UI)** as a
   non-regression ratchet and is ramped at checkpoints as tests land — it is **not** a hard 40/70/85
   target on day one (the original plan's ~40% baseline assumption was wrong).
4. **Merge visibility**: `scripts/ci/regression_summary.py` parses JUnit + JaCoCo XML into one
   Markdown summary (test pass/fail/skip + per-module line coverage) reused by three surfaces so the
   numbers show up regardless of event type — GitHub **Job Summary** (push + PR), the sticky PR
   comment (PR), and the **GitHub Release notes** with the coverage HTML attached (each merge).
5. The sticky-comment GitHub App is renamed **"Dhruv Bot" → "Dhruv CI Bot"** (display-name change in
   App settings only; the `DHRUV_BOT_APP_ID`/`DHRUV_BOT_PRIVATE_KEY` secret names are unchanged).
**Why.** One command is the whole gate for CI and developers; JaCoCo is the only coverage tool that
actually works on this AGP 9 / Gradle 9 toolchain; a baseline-anchored ratchet gates real regressions
without blocking unrelated PRs; a single parse step feeding three surfaces guarantees every merge
shows its coverage + test results, which PR-only comments cannot.
**Consequences.** New dev deps: JaCoCo (root), `turbine`, `androidx-room-testing` in the catalog.
`enableUnitTestCoverage = true` on the debug build type of every module. The coverage floor
(`globalLineFloor` in the root `build.gradle.kts`) is CI-owned and bumped only at plan checkpoints,
never ahead of landed tests. Instrumented (`connectedAndroidTest`) tests stay developer-local — the
gate is JVM + Robolectric only. Re-enabling static analysis in the gate is tracked separately.

---

## ADR-0014 — Tracker-first pivot: Supabase-primary personal-finance tracker, Google sign-in
**Context.** The Finance app was a calculator collection with a placeholder Dashboard. The
maintainer's actual goal is tracking complete personal finances — assets, liabilities, net worth,
then expenses, goals, insurance, retirement — with manual entry first and automation later. The
platform's locked design assumed offline-first Room with future Supabase *sync* (§5), but the
maintainer explicitly chose Supabase as the **primary store now** for tracker data, accepting the
trade-offs (online-required tracking, user accounts in Phase 1, DPDP consent in Phase 1).
**Decision.**
1. **App identity**: tracker-first. Net Worth dashboard = Home tab; all calculators/converters move
   behind a single "Tools" tab (launcher grid). Calculator internals are untouched (code-move rule).
2. **Tracker data lives in Supabase (Postgres + RLS), not local Room.** This *narrowly overrides*
   §5's offline-first rule for the tracker domain only. Room/`AppDatabase` continues to serve the
   calculators (history, currency cache) unchanged. DhruvEntity/HLC sync fields are NOT used by
   tracker entities — the server is the single source of truth and RLS (`user_id = auth.uid()`)
   owns identity; there is no client-side conflict resolution to do.
3. **Auth = Google sign-in via Supabase Auth** (Credential Manager → Google ID token →
   `signInWithIdToken`). This supersedes the "Firebase Auth (Dhruv ID SSO)" plan for this app until
   a cross-app Dhruv ID actually ships; revisit in a future ADR then.
4. **Money = integer paise (`Long` / SQL `bigint`)** for all tracked amounts — exact, summable,
   no floating point. `BigDecimal` remains for fractional *calculation* domains (existing
   calculators, future retirement projection engine).
5. **Valuations are append-only.** Every value update inserts a new timestamped row (enables trend
   charts and month deltas). Corrections = soft-delete the bad row + append a corrected one; there
   is deliberately no update path for valuation rows.
6. **Supabase is consumed as plain REST on the existing network stack** — Retrofit + Moshi +
   OkHttp against GoTrue (`/auth/v1/*`) and PostgREST (`/rest/v1/*`). supabase-kt/Ktor was
   rejected: it is the same class of AGP-9 compatibility unknown that killed Hilt (ADR-0010) and
   Kover (ADR-0013), while Supabase's REST surface needs nothing beyond the libraries already
   proven in this build. Only genuinely new dependencies: androidx `credentials` + `googleid`
   (Google sign-in UI, plain AndroidX). Certificate pinning attaches to OkHttp pinned at
   **CA level (ISRG Root X1 + X2)** — leaf pinning would brick the app on Supabase's routine
   certificate rotations.
7. **DPDP applies from day one**: consent screen (naming Supabase + hosting region) gates sign-in
   and any network call from the tracker; the flag entry `networth.requiresConsent = true` is
   honored with **persisted, revocable consent** (a `SettingsRepository` DataStore flag +
   "Withdraw consent" action — NOT the assistant's current in-memory pattern, which forgets
   consent on restart; fixing the assistant is a tracked follow-up). Erasure is fully in-app:
   "Delete my data" hard-deletes all tracker rows, and "Delete my account" calls a
   `delete_my_account()` **security-definer SQL function** (deletes rows + the `auth.users` row,
   executable by the signed-in user) — no Edge Function, no service-role key anywhere near the
   device. This satisfies the 7-day erasure guarantee immediately. Category enums persisted as
   TEXT are append-only: never rename a constant that has shipped.
8. **UI is micro-frontend style with one design system**: every reusable visual component (bento
   grid/cards, hero stat card, delta chip, trend chart, sheets, consent scaffold, state cards)
   lives in `:libs:core` (`com.dhruv.core.ui.components.*`) themed only through
   `DhruvTheme`/`SectionTheme` MaterialTheme roles. Feature modules own screens/flows only — zero
   feature-local styling, so the entire application keeps one theme and style. Core stays
   internally dependency-free; existing screens adopt the components in later phases, not P1.
**Why.** Cloud-primary was the maintainer's informed choice (single account across future devices,
no sync machinery in P1). Supabase free tier fits the cost-first driver (ADR-0001); RLS gives
per-user isolation without server code; Google sign-in avoids password custody entirely; the
REST-on-existing-stack decision removes the single biggest schedule risk (unproven Gradle-plugin
dependencies on AGP 9).
**Consequences.** Tracker screens require internet + session; signed-out/offline/not-configured
states are first-class UI states. `SUPABASE_URL` / `SUPABASE_ANON_KEY` / `GOOGLE_WEB_CLIENT_ID`
ride the existing secrets-plugin `.env` mechanism (`.env` gitignored, `.env.example` committed with
empty defaults so CI debug builds succeed; the release CI job writes `.env` from GitHub secrets).
The anon key is publishable-by-design under RLS but stays out of the repo per the GitLeaks gate.
Session tokens persist only in encrypted DataStore. The phased roadmap (P1 net worth → P2
expenses/budgets → P3 goals/debt payoff → P4 insurance → P5 retirement → P6 automation) is specced
in `apps/finance/docs/superpowers/specs/2026-07-03-*` with the engineering playbook and P1 gap analysis in
`apps/finance/docs/superpowers/specs/2026-07-04-*`.

---

## ADR-0015 — Web Application: Vite + React SPA hosted on Vercel
**Context.** The maintainer requires a web interface for the Finance app alongside Android. The
web interface must be cost-free to host, simple to maintain for a solo developer, and integrate
with the existing Supabase backend. Server-side rendering (SSR) frameworks like Next.js or Remix
were considered but rejected due to hosting constraints (Vercel serverless limits/costs over time)
and architectural mismatch (the Android app is already a "dumb client" over a REST API; the web
app should be the same).
**Decision.**
1. **Framework:** Vite + React SPA (TypeScript).
2. **Hosting:** Vercel (free tier).
3. **Repository:** Integrated into the existing `dhruv` monorepo inside a `web/` directory.
4. **Data fetching:** `@supabase/supabase-js` and `React Query`.
5. **Styling:** Vanilla CSS with custom properties porting the Dhruv design system.
**Why.** An SPA matches the Android app's architecture exactly — both are dumb clients talking to
the shared Supabase PostgREST API. Vite produces static files that can be hosted anywhere for free.
A monorepo setup ensures feature flags, Supabase schema migrations, and documentation are shared.
React Query elegantly replicates Android's `StateFlow`/`Repository` layer for server state.
**Consequences.** The `dhruv` repository now contains both Gradle and NPM projects. CI must use
path-based triggers to avoid running Android tests on Web changes and vice versa. Supabase CORS
must be configured to allow the Vercel domain. DPDP consent logic and feature flags must be
re-implemented identically in TypeScript.

---

## Numbering-hygiene note — ADR-0015 collision (found 2026-07-25)
**Context.** `2026-07-04-ci-cost-optimization-commit-type-versioning-design.md` was authored the
same day as this register still ended at ADR-0014, and reserved ADR-0015 (commit-type-driven
semver bump) and ADR-0016 (CI cost model) for its own not-yet-built decisions. By the time that
spec's decisions are implemented, ADR-0015 above (Web application) had already been accepted into
this register first, taking the number. The CI-cost spec's *content* was never wrong — only its
self-assigned numbers were, since they were written before this register caught up. A same-day
`master-roadmap-personal-app.md` reference and the CI-cost spec itself both perpetuated the stale
0015/0016 pair.
**Resolution.** The CI-cost-optimization spec and the roadmap reference to it are corrected to
reserve **ADR-0025** (commit-type-driven semver bump) and **ADR-0026** (CI cost model:
single-validation pipeline) instead — the two decisions are unchanged, only their future ADR
numbers moved, chosen past the ADR-0017–0024 block already reserved (but not yet written) by the
R3–R9 phase specs (accounts-are-assets, INR-only, app-lock, update-channel, XIRR, TRANSFER type,
loan balance, navigation architecture) so no second collision is created. No entry for ADR-0025/
0026 exists yet in this register — per the append-only rule, they get written here (as full ADRs,
context/decision/why/consequences) only when that spec is actually implemented, same as every
other ADR in this file.
**Why.** This register is append-only and ADR-0015 above is ACCEPTED — it does not get renumbered
or displaced. A collision between a *written* register entry and a *spec's forward reservation* is
resolved by moving the reservation, never the register.
**Consequences.** Any future PR implementing the CI-cost-optimization spec must write ADR-0025 and
ADR-0026 (not 0015/0016) into this register. Anyone reserving a new ADR number in a spec should
first check this file's highest defined number, not just other specs' reservations — that is what
let two specs claim 0015 independently.

---

## ADR-0024 — Navigation: DhruvNext 4-tab shell + single global accent (supersedes ADR-0014 §1, §8)
**Note (2026-08-09):** the source doc this ADR cites below
(`2026-07-25-dhruvnext-ui-ux-design-reference.md`) was removed — its content lineage is fully
absorbed by the finalized `2026-08-08-design-v1-final-functional-spec.md` (same Claude Design
import, later/final version), which ADR-0027 adopted, superseding this ADR's §1 in turn. This
ADR's own narrative below is unchanged (append-only) — it explains *why* the 4-tab decision was
made at the time, which stands regardless of the source file's removal.

**Context.** The Finance app's shell had drifted from ADR-0014 §1's 3-tab design into an
undesigned 5-visible-tab pager (Dashboard/Calc/Converter/Finance/Assistant + a hidden Settings
page), and `:libs:core`'s design-system component layer (bento/stat/chart/sheet components ADR-
0014 §8 calls for) was never built — every feature screen hand-rolls Material3 widgets, four of
them (Loans/Investments/Tax/Everyday) with hardcoded red/green hex and visibly machine-written
copy. Separately, the maintainer had a complete Claude-designed system on file — **DhruvNext**
(`apps/finance/docs/superpowers/specs/2026-07-25-dhruvnext-ui-ux-design-reference.md`), imported as a
reference-only document specifically *because* it conflicts with the binding standard: DhruvNext
draws a **4-tab bottom nav (Home · Calc · Plan · Insights)** with Settings reached via a top-bar
icon, a **single global accent** color, and a calculator-suite-first IA with a full "Ask Dhruv" AI
chat — versus ADR-0014 §1's 3-tab (Home/Tools/Settings) with calculators demoted into a Tools
launcher grid, and §8's per-domain `SectionTheme` accents (tracker always green, calculators their
own color, etc.). The reference doc's own §3 recommendation was to either reconcile the two models
with an ADR or explicitly supersede the binding standard's §3; this reserved ADR number (flagged
for "navigation architecture" by the security-navigation-technical-review's NAV1 finding, and
listed in the ADR-0017–0024 reservation block above) is that ADR. The maintainer reviewed both
models side by side and chose to adopt DhruvNext as-drawn rather than merge them.
**Decision.**
1. **Nav = DhruvNext's 4-tab bottom bar: Home, Calc, Plan, Insights.** Settings is reached via a
   top-bar icon, not a tab. The former calculator feature modules (loans/investments/tax/everyday)
   become **Plan** drill-in sub-routes (`loan`/`invest`/`tax`/`everyday`) that keep the Plan tab
   highlighted rather than being Tools-grid tiles or their own tabs. This supersedes ADR-0014 §1's
   "Home + single Tools tab" model; Net Worth remains the Home tab's content, unchanged.
2. **Accent = one global accent (`--acc`)**, defaulting to the existing `PrimaryLight`/`PrimaryDark`
   orange already shipped in `Color.kt` — DhruvNext's `#F05A28`/`#FF6D3B` tokens are numerically
   identical to those, so the default look is unchanged. The user may override it via a 4-swatch
   global picker in Settings (orange/green/blue/purple), reusing the existing light-mode hex values
   already present in `ThemeColorConfig.kt`'s `ColorOptions` as the swatch source. This supersedes
   ADR-0014 §8's per-domain `SectionTheme` accent rule; `SectionTheme` is retired in favor of one
   theme root applied app-wide.
3. **Shell mechanics**: a pager for the 4 top-level tabs, each hosting a **nested NavHost** for
   drill-in routes. Utility/detail routes (`history`, `currency`, `unit`, `date`, `stopwatch`,
   `timer`, `addtxn`, `settings`, `profile`, `notif`, `shell`, `ask`) show a back top-bar and no tab
   bar. `consent`, `shell`, and `addtxn` render as bottom sheets. `splash`/`onboard` are bare,
   full-frame, no chrome. This is the shape NAV1 in the security-navigation-technical-review already
   proposed for this ADR number.
4. **"Ask Dhruv" becomes real surface area**: a `FeatureHost`-wrapped `ask` chat route + floating
   pill (shown on Home/Plan/Insights) + a route-registry row + its own consent entry. The AI
   key-delivery/consent plumbing this requires (deferred earlier as part of production-hardening
   Phase 2) is tracked and lands alongside it — no shared Gemini key is ever embedded in the APK,
   consistent with ADR-0002.
**Why.** DhruvNext arrived as a complete, ready system — full token set, 23 screen states, and an
accent that already matches the shipped default color exactly — while the binding standard's
component layer remained entirely unbuilt eight months on. Rebuilding the shell once against a
finished design was judged cheaper than partially building the 3-tab component layer and
reconciling it with DhruvNext later. Per-domain `SectionTheme` accents were also producing visible
inconsistency across tabs that a single brand accent removes.
**Consequences.** `SectionTheme` (`Theme.kt`) and its call sites (`MainActivity`'s per-page
wrapping) are retired during the shell rebuild as a working, tested commit — not deleted by this
docs-only ADR. `ColorOptions`/`getAccentColor` (`ThemeColorConfig.kt`) are repurposed from a
per-section picker to the new global picker's swatch source; same data, new consumer. Two open
items DhruvNext's own §8 flagged remain open and block their respective screens: the Home
financial-health "score out of 100" has no data spec, and `history` (calculator-result history)
must stay disambiguated from any future transaction-history surface. The full module-by-module
build order, component inventory, and gap register live in the DhruvNext overhaul plan (see
`apps/finance/docs/superpowers/plans/`), not duplicated here.

---

## ADR-0025 — Commit-type-driven semver bump (amends ADR-0011)
**Context.** ADR-0011 made CI auto-increment the PATCH segment on every merge, leaving MINOR and
MAJOR as manual edits to `platform/versions.json`. In practice that meant hand-editing the file
before merging a feature branch (e.g. 1.2.x → 1.3.0 for the networth work) — a step that is easy
to forget and produces a wrong version silently when forgotten.
**Decision.** CI derives the segment from the commit types in the push range:
`feat:` / `feat(scope):` → **minor**; any `type!:` or a `BREAKING CHANGE:` / `BREAKING-CHANGE:`
trailer → **major**; everything else (including bare merge commits) → **patch**. Highest wins
across the range. Pushes to `main` are **always patch** — a `develop → main` promotion replays
develop's already-bumped `feat:` commits, and re-detecting them would double-bump.
Detection lives in `scripts/ci/detect_bump.sh` (stdin → segment) and the file rewrite in
`scripts/ci/bump_version.py`, both with local tests, rather than in an inline YAML heredoc.
**Why.** Removes the whole class of "forgot to bump minor" errors, matches the conventional-commit
messages the repo already writes, and keeps ADR-0011's semantics (PATCH = fix/merge) intact.
Scripts over inline YAML follows the `scripts/ci/regression_summary.py` precedent — testable
locally with `--dry-run` instead of only observable after a merge.
**Consequences.** Manual minor/major edits to `versions.json` are no longer needed and are
**discouraged**: a manually raised version still works as a new baseline, but if the same merge
also carries `feat:` commits the result is a double bump. Any branch holding such a manual edit
must revert it before merging. `VERSION_CODE` increment, `VERSION_NAME` sync, APK verification,
idempotent tagging and Release publishing are unchanged.

---

## ADR-0026 — CI cost model: single-validation pipeline
**Context.** An audit of the three workflows found no trigger loops but heavy duplicate work —
roughly 2–3 GitHub-hosted runner-hours per merged PR on a private repo with a 2000 min/month floor,
i.e. ~11–16 merges before exhaustion. Five distinct duplications: (1) a full 4-gate run on the PR
and an identical re-run on the merge push over the same tree; (2) `fast-feedback` compiling the
same commit `ci.yml`'s PR run already compiles, once a PR is open; (3) the `build` job re-compiling
`assembleDebug` on a fresh cold runner after `tests` had already compiled everything via
`regressionCheck`; (4) OWASP running twice per merge with a ~700 MB NVD update, up to 30 min, and
`continue-on-error` masking every finding — paying full price for zero gate value; (5) docs-only
commits triggering full builds plus a version bump, APK and Release.
**Decision.** The PR is the **only** full-validation pass. `static-analysis` and `tests` run on
`pull_request` only; `security` (GitLeaks) runs on every PR including docs-only ones, because
secrets hide in markdown too. The merge push runs the `release` job only. `build` is deleted and
its `assembleDebug` folded into `tests` on the warm daemon. OWASP moves to `owasp-scheduled.yml`
(monthly cron + `workflow_dispatch`). A `changes` gate job short-circuits docs-only work. The
Gradle cache **writer** moves from `tests` to `release`, since `tests` no longer runs on the
default branch. Release notes fetch the regression summary and coverage artifacts **cross-run**
from the PR's successful CI run, best-effort.
**Why.** Safe because branch protection now requires up-to-date branches (see Consequences): the
merged tree is byte-identical to the tree the PR validated, so re-running the gates on push
verifies nothing new. The `release` job's own `assembleRelease` still catches compile-level
breakage. Docs-only skipping uses a job-level `if:` rather than trigger-level `paths-ignore`
because a skipped job reports as skipped — which branch protection counts as passing — whereas
`paths-ignore` never creates the check run and leaves required checks permanently pending.
**Consequences.** *"Require branches to be up to date before merging"* on `develop` and `main` is
now a **load-bearing repo setting**, not a preference — disabling it silently removes the only
thing validating merged code. Required-status-check names changed: `Gate 4 · Build (debug)` and
`Gate 2b · OWASP (non-blocking)` no longer exist, and Gate 3 is renamed
`Gate 3+4 · Tests + ArchUnit + Coverage + Build`. The `release` job needs `actions: read` to pull
artifacts across runs; if the lookup fails, release notes degrade to the APK line and the release
still publishes. ADR-0013's "coverage visible on every merge" promise is kept via the PR run's
artifacts rather than a re-run. `pr-summary` remains informational-only (ADR-0012); its OWASP row
becomes a static pointer to the scheduled workflow. Cost is now **measured, not assumed**:
`scripts/ci/actions_usage.py` reports billed minutes per pipeline from the Actions timing API, a
monthly `ci-usage-report.yml` posts it to a Job Summary, and four standing budgets bound future
growth — **≤ 90 billed min per merged PR**, **≤ 70 %** of that in the commit pipeline, **≤ 1600 min**
projected monthly (80 % of the Free-tier cap), and **≤ +4 min** on `regressionCheck` per new module.
Exceeding one is a decision to take deliberately, with the remedy named in the plan's cost-budget
table; test sharding is the first lever and is deliberately unbuilt until the measurement calls for
it. The cadence of the OWASP scan is monthly **only while its findings are masked** — restore weekly
in the same change that flips `continue-on-error` to false.

**Implementation note (2026-08-15):** the cost-telemetry script (`scripts/ci/actions_usage.py`,
`ci-usage-report.yml`) and the standing budgets referenced above are specified in the implementation
plan but were not part of this change's initial scope — this ADR records the full intended design;
the telemetry itself lands in a follow-up commit before it can be relied upon.

---

## ADR-0027 — Navigation: 5 tab roots (Home · Money · Calc · Plan · Insights); Plan root leads with
## live modules, calculators demoted to a strip (supersedes ADR-0024 §1)
**Context.** A finalized Claude Design project (`Dhruv brand & UI/UX finalization`, imported
2026-08-08 — see `apps/finance/docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md`) drew a
complete **61-screen, 5-root** route map: **Home · Money · Calc · Plan · Insights**. ADR-0024's
shipped shell has **4** roots — Home/Calc/Plan/Insights — with no owner for day-to-day money
movement (the design's Money tab: ledger, quick add, accounts, cards, categories, recurring —
D1–D9). The same design also revises the **Plan** root: live planning modules (Budgets, Goals,
Debt payoff, Insurance, Retirement) lead, with the four existing calculators (Loan/SIP/Tax/
Everyday) demoted to a strip below rather than being the whole tab.
**Decision.**
1. `TabKey` gains `MONEY`, inserted between `HOME` and `CALC`: `HOME, MONEY, CALC, PLAN, INSIGHTS`.
   `pageIndexFor` already resolves by key (not position, NAV4), so the insertion is safe for
   flag-driven tab hiding without further changes to that resolver.
2. Money is a new tab root owning the ledger and everything under it (D1–D9). It ships behind its
   own feature flag once its screens exist (Phase 3 of the implementation plan); until then it
   renders `NotConfiguredCard`, matching the existing pattern already used for the not-yet-built
   Insights tab rather than inventing a second "coming soon" treatment.
3. Plan's root screen (E1) is rewritten once its live modules exist (Phase 4): live modules first,
   `PlanSections`' calculator strip second. This ADR only fixes the target shape; the rewrite
   itself is tracked as implementation work, not re-decided here.
**Why.** The design is the finalized product definition — it is not exploratory, and the maintainer
chose it as the source of truth over the 8-month-old ADR-0024 shell when the two disagreed (see the
functional spec §3 D-1). Inserting `MONEY` rather than renaming or repurposing an existing tab
avoids clobbering Home/Calc/Plan/Insights, all of which keep their ADR-0024 meaning unchanged.
**Consequences.** `BottomNavItems` (`NavConfig.kt`) gains a Money row. `MainActivity`'s pager grows
a 5th page; `NavTargetTest`'s fixed tab-index assertions shift by one for every tab at or after
`MONEY`. ADR-0024 §1 ("Home + single Tools tab" / the 4-tab shape) is superseded by this ADR; its
§2 (single global accent) and the retirement of `SectionTheme` are **not** touched and remain in
force. The Money tab's actual screens (D1–D9) are separate implementation work, phased in
`apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md`.

## ADR-0028 — Brand chrome as a second, theme-invariant color group (`DhruvBrand`)
**Context.** The finalized design (see ADR-0027's source) draws two distinct color roles: an
**app accent** that already flips between `DhruvNextLightColors`/`DhruvNextDarkColors` (ADR-0024
§2, unchanged), and a **brand chrome** palette — navy `#0D1B2A`, elevated navy `#132B4D`, blue mid
`#1E3A6D`, accent blue `#3FA7FF`, silver `#C0C6D1`/`#E6E9EF`, steel `#8E97A6`, logo background
`#F4F6FA` — that does **not** flip with the system theme. Brand chrome carries identity on the
splash screen, hero gradient cards (net worth, goal progress), the Settings identity card, and the
screens the design renders as dark-hero regardless of the user's theme (holding detail, quick-add,
account detail, goal detail, retirement, P&L, the AA-consent modal). These exact hex values already
existed as ungrouped top-level constants in `Color.kt` (`DhruvNavy`, `DhruvSilver`, etc.), consumed
only by the logo/wordmark composables in `DhruvBrand.kt` — there was no themed, importable group for
a screen to consume when building one of the design's dark-hero surfaces.
**Decision.** Add `object DhruvBrand` in `:libs:core`'s theme package, bundling the existing
`Color.kt` brand constants (`navy`, `navyElevated`, `blueMid`, `accentBlue`, `silver`,
`silverLight`, `steel`, `logoBg`) as one theme-invariant token group, parallel to but independent
of `LocalDhruvNextColors`. Hero/glass surfaces and the dark-hero screens read `DhruvBrand.*`;
everything else continues to read `LocalDhruvNextColors` exactly as before.
**Why.** The values already existed and were already correct (verified byte-for-byte against the
design during import) — the gap was a consumable, named group, not new colors. A composition-local
was considered and rejected: brand chrome is deliberately theme-invariant, so routing it through
`staticCompositionLocalOf` (which exists precisely to vary by theme) would misstate its nature; a
plain `object` is the honest shape for a constant that never changes.
**Consequences.** No existing color values changed — `DhruvBrand.kt`'s logo/wordmark composables
keep working unmodified (they import the raw `Color.kt` constants directly, which is still
correct for logo rendering). New hero/glass/dark-hero surfaces built against the design (Phase 2+
of the implementation plan) must read `DhruvBrand.*`, never a raw hex, per the project's
no-hardcoding rule.

---

## ADR-0029 — Tracker data architecture: Supabase REST on Retrofit/Moshi/OkHttp, append-only
## valuations, paise integers, currency-less schema (implements ADR-0014 §2/§4/§5/§6)
**Context.** ADR-0014 committed the tracker domain to Supabase-primary storage but left the actual
client architecture undecided — which HTTP stack, how auth/consent gate every call, and what the
first tables look like. Design-v1 Phase 1 (`apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-
implementation-plan.md` §5, §7) is the first phase to touch the tracker backend at all (sign-in +
consent), and its migration (`supabase/migrations/0001_init.sql`) commits to a schema shape that is
expensive to change once RLS-protected user rows exist against it — so the architecture is decided
and written here, not deferred. A blocking pre-step also had to resolve the functional spec's open
item §8.5 (multi-currency scope) before this migration could be authored at all: `holdings` and
`valuations` (this ADR's first two tables) commit to a **currency-less** `value_paise bigint` shape
with no `currency` column anywhere — R5's pre-work decision
(`apps/finance/docs/superpowers/specs/2026-07-12-r5-accounts-multicurrency-decisions.md`, "Option A:
INR-only, validated") already accepted INR-only for the maintainer's actual holdings; this ADR
implements that by omitting the column entirely rather than adding a `CHECK (currency = 'INR')`
guard on a column that would otherwise invite a false sense of future flexibility.
**Decision.**
1. **Networking = Retrofit + Moshi + OkHttp** against Supabase's GoTrue (`/auth/v1/*`) and PostgREST
   (`/rest/v1/*`) endpoints — the same stack `CurrencyApiClient` already proves works on this AGP 9 /
   Gradle 9 toolchain (ADR-0014 §6's own reasoning: no new unproven Gradle plugin, unlike the
   supabase-kt/Ktor path that was already rejected). Two Retrofit instances share one
   `OkHttpClient.Builder` base, split into an unauthenticated-consent `authClient` (GoTrue only —
   sign-in itself is not consent-gated, ONB-BR-001) and a consent-gated `dataClient` (PostgREST) —
   see `tracker/net/SupabaseClientFactory`.
2. **Consent is an interceptor, not a screen concern.** `ConsentInterceptor` is attached only to
   `dataClient` and short-circuits before dispatch if "Sync my financial records" is off (NFR-1,
   DAT-BR-001) — no code path can reach PostgREST without going through it, because no other
   PostgREST-capable client is constructed anywhere in the app.
3. **Auth = `AuthInterceptor`** attaches `apikey` + `Authorization: Bearer` to every tracker
   request; a 401 triggers exactly one refresh-token attempt, a second consecutive 401 forces
   `SessionStore` to `SignedOut` (DAT-BR-003) — no retry loop. Tokens live only in
   `EncryptedDataStore` (DAT-BR-004), never plaintext `SharedPreferences`.
4. **`holdings`/`valuations` are the first two tables**, RLS `user_id = auth.uid()` on `holdings`
   directly and via a `holding_id` join on `valuations` (which carries no `user_id` of its own —
   ownership is transitive through its parent holding). `valuations` gets `SELECT`+`INSERT` RLS
   policies only — no `UPDATE`, no `DELETE` — making it genuinely append-only at the database layer,
   not just by client convention (DAT-BR-007). The functional spec's "corrections = soft-delete +
   append" language (BR-C1, NW-BR-003) describes a Phase 2 concern once C4/C5's edit screens exist;
   this ADR deliberately does not resolve *how* a correction is issued yet (a future security-definer
   `correct_valuation()` RPC is the leading candidate, so a raw client UPDATE is never exposed even
   for corrections) — Phase 2's SA step owns that when it adds `v_latest_valuation` and the other
   views. The `deleted_at` column ships now (matching the schema table in the implementation plan
   §5.4) purely as forward-compatible shape; nothing writes to it yet.
5. **Erasure = two security-definer SQL functions**, not a client-facing `DELETE` policy on tracker
   tables: `delete_my_data()` (removes every row owned by `auth.uid()` across all tracker tables,
   leaves the account itself intact — ONB-BR-008) and `delete_my_account()` (calls `delete_my_data()`
   then removes the caller's own `auth.users` row — ONB-BR-009, ADR-0014 §7). Neither needs a
   service-role key or an Edge Function; both run as the signed-in user via PostgREST's `rpc/` path.
   This keeps `holdings`/`valuations` free of any `DELETE` RLS policy at all — the only way a row
   ever disappears is through one of these two named, auditable functions.
6. **Certificate pinning stays CA-level** (Google Trust Services GTS Root R1 + R4, DAT-BR-005 —
   corrected 2026-08-15, see this ADR's Correction paragraph below; ADR-0014 §6 originally named
   ISRG Root X1/X2) on both Retrofit clients — leaf pinning would brick the app on Supabase's
   routine certificate rotations, per ADR-0014 §6.
**Why.** Reusing `CurrencyApiClient`'s proven stack removes the single biggest schedule risk this
phase could have introduced (an unproven networking library on an already-fragile AGP 9 toolchain —
the same risk class that ruled out Hilt, Kover, and supabase-kt/Ktor in ADR-0010/0013/0014). Splitting
auth and data into two client chains makes "zero network call before consent" a structural property
of the code (nothing routes to PostgREST except through the gated client) instead of a discipline
every future call site has to remember. Dropping the currency column entirely — rather than adding
and constraining one — avoids building UI/DTO plumbing for a dimension the maintainer's actual data
will never use, consistent with the project's YAGNI stance elsewhere (ADR-0010's "code-move, not
rewrite," ADR-0013's baseline-anchored coverage ratchet).
**Consequences.** `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/{net,auth,dto,
model,mapper,repo}` exists from Phase 1 onward (§5.1); `tracker/dto/GoTrueSessionDto.kt` is the
first file under `tracker/dto/`, which is also what makes the module-standard doc's `.*Dto` ArchUnit
guard (previously vacuous, `.*Dao`-only) non-vacuous — that guard extension is a Phase 1 Backend
task, not a follow-up. `SUPABASE_URL`/`SUPABASE_ANON_KEY` ride the existing `.env` secrets-plugin
mechanism (ADR-0014 §6), sourced from the already-linked `dhruv` Supabase project
(`supabase/.temp/linked-project.json`, ref `dsfnrtckgpnvyvscevxn`) reused as the dev/RLS-testing
target rather than provisioning a second project. `GOOGLE_WEB_CLIENT_ID` ships with an empty
default in `.env.example` (same pattern as `GEMINI_API_KEY`) until the maintainer supplies a real
OAuth web client id — the Credential Manager call is built against the real API shape regardless, it
simply cannot complete a live sign-in until that value is set. No multi-currency ADR is written
separately — this ADR's decision 4/context paragraph is the resolution of functional-spec open item
§8.5; if multi-currency is ever revisited, it supersedes this ADR's schema decision, not ADR-0018
(reserved by `apps/finance/docs/superpowers/specs/2026-07-12-r5-accounts-multicurrency-decisions.md`
for the separate `accounts`/`transactions` currency stance, not yet written into this register).
**Correction (2026-08-15).** Decision 6 above, and ADR-0014 §6's original text, named ISRG Root
X1/X2 (Let's Encrypt) as the CA-level pin. Every "live" verification of this build up to that date
went through the Supabase Management API (`api.supabase.co`) or the raw Postgres wire protocol —
neither touches the app's actual pinned `OkHttpClient` — so the wrong pins shipped undetected until
a real device's first live Google sign-in attempt hit the real `*.supabase.co` REST domain through
`SupabaseClientFactory` and threw `SSLPeerUnverifiedException: Certificate pinning failure!`. The
exception's own peer-chain dump named the actual root: Google Trust Services' `GTS Root R4`. Both
`GTS_ROOT_R1_PIN`/`GTS_ROOT_R4_PIN` (`SupabaseClientFactory.kt`) are independently verified against
Google's own published trust store (`https://pki.goog/repo/certs/gtsr{1,4}.der`, SPKI SHA-256 via
openssl), not merely copied from the live failure's own dump. This register entry is corrected in
place (not superseded by a new ADR number) because the wrong pins were never live/reachable
architecture — this ADR and Phase 1 were still in the same active implementation session when the
error surfaced, and per the module-standard doc's TDD gate, nothing downstream had shipped against
the incorrect pins yet. Root cause for the next engineer: cert-pinning a third-party domain
requires observing that domain's *actual* live TLS chain, not assuming a CA from prose docs.

---

## ADR-0030 — One global design system (`platform/DESIGN-SYSTEM.md`); retires the tracker design
## system, the app-design-standard, and the root `DESIGN.md`
**Context.** Three documents each claimed design authority, and none was correct on its own:
1. **`DESIGN.md`** (repo root) — a token extraction predating DhruvNext. It stated the app's
   typography was "System Default (Roboto/Inter)"; the app has shipped Space Grotesk + Inter +
   JetBrains Mono since the DhruvNext work. It was nonetheless the cited upstream source for
   `web/src/shared/styles/tokens.css`, so the web tokens traced to a stale document.
2. **`2026-07-03-tracker-design-system.md`** — marked **BINDING** by ADR-0014 §8. A symbol search
   found that **every component it declared** (`BentoGrid`, `BentoCard`, `HeroStatCard`,
   `TrendLineChart`, `DonutChart`) and the `SectionTheme` mechanism it themed them through **do not
   exist in `:libs:core`**. It specified a component library that was never built, against a
   theming approach ADR-0024 had already retired. Screens were being written against a fiction.
3. **`2026-07-12-app-design-standard.md`** — also **BINDING**, and genuinely mixed. Its §2 section
   accents and §3.1/§3.2/§3.4 three-tab model were dead (ADR-0024, then ADR-0027). Its §6–§10
   (interaction, screen-state matrix, accessibility, copy) were sound app-wide law. Its §5 component
   table was 8-of-9 real. Its §7 notification-channel / intent / Glance / PDF registries existed
   **nowhere else in the repo**.
Meanwhile the finalized design (v1.0 FINAL, imported 2026-08-08) sat at Finance app level, mixing a
cross-app design system with Finance's 61-screen product spec — so a future Tools/Vault/Health app
had no design source at all, and Finance had three contradictory ones.
**Decision.**
1. **`platform/DESIGN-SYSTEM.md` is the single design contract for every Dhruv app and the web
   SPA.** It holds tokens (brand chrome + app palette), typography, spacing/radii/responsive tiers,
   logo directions, the component library, navigation law N1–N7, the screen-state matrix,
   interaction/motion/accessibility/copy standards, non-Compose surface conventions, and the web
   parity rule. It defines **no screens**.
2. **Product specs stay app-level.** An app's screen inventory, business rules and flows live in
   that app's spec; its route/notification/intent/settings **rows** live in that app's surface
   registry (Finance: `2026-08-09-finance-surface-registries.md`, extracted from the retired
   app-design-standard). Design system = how things look and behave; product spec = what screens
   exist. A Tools app gets its own product spec and registry, and inherits the same system.
3. **Nothing enters the component table before the code exists.** Built components are listed
   separately from planned batches, and every entry was verified by symbol search against
   `libs/core/src/main`. This is a direct response to failure mode (2) above.
4. All three documents above are **retired** (`git rm`); their salvageable content was folded into
   the two destinations before deletion, and every inbound reference redirected.
**Why.** Three competing BINDING documents is worse than none — a reader cannot tell which is
authoritative, and two of the three described a UI that does not exist. One system with an explicit
product/system boundary scales to the planned Tools/Vault/Health apps without duplicating tokens
per app, and `platform/` is the right home because `AGENTS.md`'s session bootstrap already reads it
first and its "docs and contracts only" rule fits a design contract exactly. The "verify before
listing" rule is cheap (one grep loop) and would have prevented the fiction-library problem outright.
**Consequences.** `platform/DESIGN-SYSTEM.md` joins the session-bootstrap reading list in
`AGENTS.md` and the root `CLAUDE.md`. `web/src/shared/styles/tokens.css` now cites the platform doc
and carries an explicit "values must stay numerically identical to `:libs:core`, nothing catches
drift automatically" warning — the two-implementation risk is stated rather than assumed away.
ADR-0014 §8 (design system lives in `:libs:core`, micro-frontend rule) is **not** overturned — its
principle survives; only the specific document it blessed is replaced. ADR-0024 §2, ADR-0027 and
ADR-0028 remain the decision trail this document implements.

---

## ADR-0031 — Dhruv ID: one Google OAuth Web Client + one Supabase project is the cross-app SSO
## mechanism (resolves ADR-0014 §6's deferred "revisit in a future ADR")
**Context.** ADR-0014 §6 picked Google sign-in via Supabase Auth for Finance and explicitly said
this "supersedes the 'Firebase Auth (Dhruv ID SSO)' plan for this app until a cross-app Dhruv ID
actually ships; revisit in a future ADR then." `PLATFORM.md` §5 already anticipated the shape of
that revisit: every `DhruvEntity`'s `userId` is `"local"` until "Dhruv ID ships," at which point "a
one-time WorkManager migration rewrites every `local` row to the real user id — cheap because of
the index." Design-v1 Phase 1 (this build) is the first phase to actually wire Google sign-in, and
the maintainer asked directly whether that sign-in should work as SSO across every future Dhruv app
(Tools, Vault, Health, Relationship — `PLATFORM.md` §1's app table), not just Finance. The answer
turns out to need no new infrastructure: the Supabase project Phase 1 already links to is named
`dhruv`, not `dhruv-finance` — it was never scoped to one app — and a single Google Cloud OAuth Web
Client ID is a property of the *Google Cloud project*, not of an individual Android package.
**Decision.**
1. **One Google Cloud project, one OAuth Web Client ID, reused verbatim by every Dhruv app's
   Credential Manager flow.** Each app additionally registers its own **Android** OAuth client
   (package name + release/debug SHA-1) under that same Google Cloud project — Android clients gate
   which package can request a credential; the Web Client ID is the `audience` GoTrue validates the
   ID token against, and staying identical across apps is what makes `auth.uid()` land on the same
   row for the same person no matter which app they signed in from.
2. **One Supabase project (`dhruv`) is the shared `auth.users` table.** No per-app Supabase project,
   no token-sharing code between apps — Android's own app-sandboxing already prevents one app's
   `EncryptedDataStore` session from being readable by another, so there is nothing to "sync"; each
   app independently runs its own Google sign-in and independently lands on the same `auth.uid()`
   because the same Google account, Web Client, and Supabase project are common to all of them. This
   *is* "Dhruv ID" — no separate SSO service, no shared identity provider beyond Supabase Auth
   itself.
3. **Vault is excluded, entirely.** `PLATFORM.md` §9 / ADR-0003's master-password-derived key is the
   vault's only real key; Vault also carries a hard "no network/AI/analytics dependency" rule
   (`PLATFORM.md` §4 table). Dhruv ID sign-in never becomes a path into vault data, not even as
   optional account linking — the maintainer's explicit call when this ADR was written, over the
   "optional linking" alternative, to keep Vault's already-locked-in security model exactly as
   simple as ADR-0003 states it, with zero exception surface for a module not yet built.
4. **Naming discipline**: the OAuth consent screen and both client entries are named generically
   ("Dhruv"), not "Dhruv Finance" — the first concrete artifact of this ADR, created directly in
   Google Cloud Console by the maintainer (no CLI/API path exists that doesn't itself require
   interactive Google OAuth login, so this step cannot be scripted).
**Why.** The deferred question in ADR-0014 §6 turned out to already be answered by two decisions
already on the books — ADR-0001's monorepo (one `dhruv` Supabase project was always the natural
default, never contested) and ADR-0014 §5's `userId`/`"local"` migration design (which already
assumed a future *shared* real user id, not a per-app one). Reusing the same Web Client ID needs no
new moving parts, so it is strictly cheaper than a dedicated SSO/identity service, and it satisfies
`PLATFORM.md` §5's "Dhruv ID" language exactly. Excluding Vault keeps ADR-0003's threat model
(unrecoverable-by-design, true E2E, key never derived from anything Google/Supabase can see) fully
intact rather than special-casing it later once Vault is under construction and the exception is
harder to say no to.
**Consequences.** `GOOGLE_WEB_CLIENT_ID` (`.env`/`.env.example`, Phase 1) is a **platform-level**
secret, not a Finance-scoped one, even though only `:apps:finance:app` reads it today — every future
app's `secrets`-plugin `.env` config points at the same root `.env` file already (monorepo-wide, per
ADR-0001), so no new plumbing is needed when Tools/Health/Relationship are built; each just adds its
own Android OAuth client (package + SHA-1) in the same Google Cloud project and reads the same env
key. `SUPABASE_URL`/`SUPABASE_ANON_KEY` are equally platform-level for the same reason — this ADR
makes that implicit sharing explicit rather than changing it. Vault, when built, gets no Koin wiring
to `SessionStore`/`AuthRepository`/`ConsentRepository` at all — its own future ADR (vault key
derivation, already mostly specified by ADR-0003) stays fully independent of this one.
