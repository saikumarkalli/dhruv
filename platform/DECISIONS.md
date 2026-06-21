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