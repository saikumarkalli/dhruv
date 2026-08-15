# R4 — In-App Update Check (GitHub Releases)

> Status: **SPECCED** (rides the R4 phase with the currency/metals/notification work — reuses the
> WorkManager + notification-channel plumbing that plan lands). Master sequence:
> `../plans/2026-07-12-master-roadmap-personal-app.md` (R4; gap N4). Companion plan:
> `../plans/2026-07-03-currency-metals-notification.md` (plumbing owner).
> **Constraint verified 2026-07-12: `saikumarkalli/dhruv` is a PRIVATE repo** — unauthenticated
> Releases API returns 404, so the check requires a user-supplied token (BYO pattern, same as the
> Gemini key). Decision below becomes **ADR-0020 (update channel = BYO-token GitHub poll)**.

## Goal

The app is distributed as a sideloaded APK via GitHub Releases and currently has **no update
channel at all** — installs rot silently. Add a daily background check of the latest GitHub
Release; when a newer version exists, notify once and surface an "Update available" row in
Settings linking to the release page for download.

## Non-goals

- No auto-download, no in-app APK install (`REQUEST_INSTALL_PACKAGES`), no delta updates —
  browser download from the release page is acceptable for a personal app. Revisit only if
  manual install proves painful (own ADR then).
- No forced updates, no minimum-version kill switch (Remote Config `minVersion` on flags already
  covers per-feature gating).
- Not a consent-gated data flow: the request sends a token the user created plus standard HTTP
  headers to GitHub; no personal/app data leaves the device. Documented in PRIVACY.md, no DPDP
  consent screen (same class as the keyless FX fetch decision in the currency spec).

## Decisions (proposed)

| # | Decision | Rationale |
|---|----------|-----------|
| D1 | Poll `GET /repos/saikumarkalli/dhruv/releases/latest` | `latest` excludes drafts + prereleases by definition; no filtering logic needed |
| D2 | Auth = user-pasted **fine-grained PAT** (this repo only, `Contents: Read`), stored in encrypted DataStore | Repo is private; embedding any token in the APK violates the GitLeaks/no-secrets rule. Identical custody model to the BYO Gemini key |
| D3 | No token ⇒ feature shows **NotConfigured** ("Add a GitHub token to enable update checks") and the worker is not scheduled | First-class not-configured state, same taxonomy as the tracker's blank-BuildConfig state |
| D4 | Version parse: release `tag_name` `dhruv-finance-vX.Y.Z` → compare against `BuildConfig.VERSION_NAME` using the existing `SemVer` comparator in `:libs:core` | Comparator already exists and is tested (`SemVerTest`); CI tag format is stable per ADR-0011 |
| D5 | Notify **once per distinct newer version** (last-notified version persisted) | A daily worker must not nag daily about the same release |
| D6 | If the repo is ever made public, the token becomes optional (header attached only when present) | Zero code restructuring; documented fallback path |

## Architecture

New dependencies: none (Retrofit/Moshi/OkHttp exist; WorkManager + Koin worker DI land with the
currency-notification work in this same phase).

| Piece | Detail |
|-------|--------|
| `:apps:finance:data` | `GitHubReleasesApi` (Retrofit: `releases/latest`, `Authorization: Bearer <token>` via header param); `UpdateCheckRepository` (`IUpdateCheckRepository`): fetch → parse tag → `UpdateStatus` = `NotConfigured / UpToDate / UpdateAvailable(version, htmlUrl) / CheckFailed`; token read at call time from settings (call-time resolution, the C2 lesson) |
| `:libs:settings` | `githubUpdateToken` in the **encrypted** DataStore (alongside Gemini key; `clearGithubUpdateToken()`); `updateCheckEnabled: Boolean` (default false → flips true when token saved); `lastNotifiedUpdateVersion: String` |
| `:apps:finance:app` | `UpdateCheckWorker` (daily periodic, CONNECTED, reuses scheduler pattern from `DailyRateWorker`); notification channel `app_updates`; Settings > About > "Updates" block: token entry (masked field + paste; "how to create a token" helper link), "Check now" button, status line (version found / up to date / last checked / error), "Update available → Download" row opening `htmlUrl` in browser |
| Pure logic (TDD) | Tag→SemVer extraction (`dhruv-finance-v1.4.2` → `1.4.2`; reject malformed); notify-decision function `(current, latest, lastNotified) -> Boolean` |

### Flow

1. Token saved → worker scheduled (daily) + immediate one-shot check.
2. Worker: fetch latest release → newer than `VERSION_NAME`? → if not yet notified for that
   version: post notification ("Dhruv 1.4.2 available", tap → browser `htmlUrl`), record version.
3. Settings "Check now" runs the same repository call in-foreground with visible result.
4. 401/404 → status line shows "Token invalid or expired" (never auto-clears the token);
   IOException → "Couldn't check — offline"; both silent in the worker (no error notifications).

### Security notes

- Token only ever in encrypted DataStore; never logged, never in crash breadcrumbs
  (`CrashReporter` gets status codes only); excluded from backup by R0's backup rules
  (`secure_settings` already excluded there).
- Fine-grained PAT scoped to one repo, read-only Contents — worst-case leak = read access to
  this repo. Helper copy instructs exactly that scope and a 1-year expiry.
- **No certificate pinning on api.github.com (SEC6)** — deliberate: GitHub rotates across CA
  chains (pinning risks self-bricking the update channel), the payload is public release
  metadata, and the PAT is protected by ordinary TLS. Supabase remains the only pinned host.
- Any OkHttp logging interceptor (debug builds) **redacts the `Authorization` header** globally —
  the PAT and Supabase bearer tokens must never appear in logcat (SEC6).

## Tests

Tag-parse + notify-decision pure tests; `UpdateCheckRepositoryTest` with fake API (newer / equal /
older / malformed tag / 401 / IOException → each `UpdateStatus`); worker logic delegated to the
repository, worker itself kept thin; SemVer comparator already covered in core. ArchUnit green.

## Dependencies

R4 currency-notification plumbing (WorkManager, Koin worker factory, channels, permission flow).
R0 backup rules (encrypted store exclusion). Independent of all tracker phases.

## UI/UX detail

| Surface | Layout & states |
|---|---|
| Settings > About > Updates | NotConfigured: explainer + token field + "Create token" link; Configured: status line ("Up to date · checked 2h ago" / "v1.4.2 available"), Check now, Download button when available, Remove token action |
| Notification | Channel "App updates", low importance, single line + version; tap → browser; auto-dismiss on tap |

## Rollout & rollback

No feature flag needed (inert without a token — D3 is the kill switch); rollback = revert PR.
Worker cancelled when token removed.

## Risks / open questions

- CI auto-bump means a release exists ~minutes after every merge — a develop-tracking installer
  gets frequent (but correct) update notices. Accepted for a personal app; D5 keeps it to one per
  version.
- PAT expiry (max 1 year) → check silently fails; status line surfaces it, but only if the user
  looks. Mitigation: after 3 consecutive 401s, post one low-priority "update checks stopped"
  notification (reuses D5 dedupe machinery).
- If GitHub API rate limits ever matter (5k/hr authenticated): daily single call — never.

## Execution checklist

- [ ] `GitHubReleasesApi` + DTO + `UpdateCheckRepository` + fakes/tests (TDD)
- [ ] Encrypted `githubUpdateToken` + settings fields + About/Updates UI
- [ ] Tag-parse + notify-decision pure functions + tests
- [ ] `UpdateCheckWorker` + channel + schedule-on-token-save + cancel-on-remove
- [ ] 401-streak stopped-working notification
- [ ] PRIVACY.md note; `/dhruv-security` + `/dhruv-pre-merge`

## TDD Mandate

> **Test-Driven Development (TDD) is strictly required for this phase.**
> All pure logic, calculators, reducers, and state machines MUST be written with failing tests first, followed by implementation. UI components must be tested for accessibility and rendering states on both Android and Web platforms.

