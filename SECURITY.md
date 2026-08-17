# Security policy

Dhruv is a personal-finance ecosystem. It handles money figures, a Google identity, and (in a
future module) passwords. Security issues are the highest-priority class of bug here.

This is a solo-maintained project. The commitments below are what one person can actually keep —
they are not an enterprise SLA, and they are stated honestly rather than optimistically.

---

## Supported versions

Only the **latest GitHub Release** of `:apps:finance` is supported. There are no LTS branches and
no backports to older tags.

| | |
|---|---|
| Supported | The most recent `dhruv-finance-v*` release on the [Releases page](../../releases) |
| Not supported | Every earlier tag, any locally-built APK, any fork |
| Current versions | [`platform/versions.json`](platform/versions.json) |

`develop` and `main` are development branches, not products. A vulnerability found only on an
unreleased branch is still worth reporting — it just will not produce a release advisory of its own.

---

## Reporting a vulnerability

**Do not open a public issue for a security problem.** Blank issues are disabled and the bug-report
form is public; a vulnerability filed there is disclosed the moment you press submit.

Use, in order of preference:

1. **GitHub private vulnerability reporting** — the repository **Security** tab →
   **Report a vulnerability**
   ([direct link](https://github.com/saikumarkalli/dhruv/security/advisories/new)). This is the
   primary channel: the report, the discussion, and the fix all stay private until an advisory is
   published.
2. **Email** `kallileelasaikumar@gmail.com` — fallback, if private reporting is unavailable to you
   or you would rather not use a GitHub account.

Include:

- which surface it affects — Android app, `web/`, Supabase schema/RLS, or a GitHub Actions workflow
- the release tag or commit SHA you found it on
- reproduction steps or a proof of concept
- the impact you believe it has, and any preconditions (signed-in user, second account, etc.)

### Response targets

| Stage | Target |
|---|---|
| Acknowledgement that the report was received | within **7 days** |
| Triage — confirmed, duplicate, out of scope, or accepted risk, with reasoning | within **14 days** |
| Fix | no fixed target; anything that exposes one user's data to another jumps the queue |

If you have not heard anything after 14 days, send a reminder — assume it was missed, not ignored.
Please give a reasonable window to ship a fix before disclosing publicly.

---

## Scope

### In scope

- The **Android app** — `apps/`, `libs/`, and the release APK published on the Releases page
- The **web SPA** — `web/`
- The **Supabase schema and its RLS policies** — `supabase/schemas/`, `supabase/migrations/`
- The **GitHub Actions workflows** — `.github/workflows/`

### Report these specifically

These are the findings worth the most here, in rough order of severity:

- **Any RLS policy that lets one user read or write another user's rows.** RLS
  (`user_id = auth.uid()`) is the entire authorization model for tracker data — a gap in it is the
  single highest-impact bug this project can have (ADR-0014, ADR-0029).
- **Any code path that reaches PostgREST without passing the consent interceptor.** `ConsentInterceptor`
  sits on the only PostgREST-capable HTTP client in the app; a second client, a direct call, or a
  bypass defeats a DPDP guarantee, not just a preference (ADR-0029 decision 2).
- **Secrets in a *current* build artifact** — a published release APK, the `web/dist` bundle, a
  workflow log. History is a separate matter, covered under accepted risks below.
- **Workflow injection, or any path that leaks a secret to an untrusted branch** — an unquoted
  expression interpolated into a `run:` block, a `pull_request_target` trigger that checks out fork
  code, an artifact that carries a token.

### Out of scope

- Reports requiring a rooted/jailbroken device, or a physical attacker holding an unlocked phone
- Vulnerabilities in Supabase, Google, Firebase, Vercel or Cloudflare themselves — report those to
  the respective vendor
- Automated scanner output with no demonstrated impact
- Missing hardening that is documented as deliberate — everything in the next section

---

## Known and accepted — please do not report these

Each of these is a decision with a written rationale, not an oversight. Reporting one is a
duplicate, and the answer will be a link back to this section.

### In git history

- **An Android *debug* keystore.** `debug.keystore.base64` (alias `androiddebugkey`, the standard
  `android` password published in Android's own documentation) was committed in **May 2026** and
  later deleted; the blob remains reachable in history. A debug keystore is not a security control —
  its credentials are a documented constant that every Android SDK installation shares, and it
  cannot sign anything a user would install as an update to the published app. **The release
  keystore has never been in the repository**; it lives only in the `prod` GitHub Environment
  (`KEYSTORE_BASE64`), and `*.jks` / `*.keystore.base64` / `debug.keystore` are gitignored.
- **A revoked Supabase Management API token.** A personal access token was captured verbatim into
  `.claude/settings.json` during a debugging session. It was **revoked in the Supabase dashboard on
  2026-08-15** — dead before the mitigation was even committed — and stripped from the working tree
  in the same fix. History was not rewritten because the branch was already pushed, so the dead
  string is still reachable. The documented fingerprint is in
  [`.gitleaksignore`](.gitleaksignore) at the repository root, which is what keeps GitLeaks green on
  that one historical commit without re-embedding the value anywhere.

### By design

- **The Supabase anon key is publishable.** It is designed to ship in clients and is safe *because*
  RLS is enforced on every tracker table (ADR-0014 §6). "The anon key is exposed" is not a
  vulnerability. An RLS gap that makes it dangerous **is** — report that instead.
- **Supabase project refs appear in the repository.** A project ref is an identifier, not a
  credential; both the dev and prod refs are stored as GitHub repository **Variables** (not Secrets)
  precisely so the release job's dev-ref guard can print them in its own log (ADR-0032 decision 7).
  That guard fails the release if the signed APK contains the dev ref, or does not contain the prod
  ref.
- **Play Integrity is warn-only.** Root detection is bypassable and produces false positives. It
  restricts the vault; it never blocks app launch (PLATFORM.md §7, layer 7).
- **Certificate pinning is CA-level, not leaf-level** (GTS Root R1 + R4). Leaf pinning would brick
  the app on Supabase's routine certificate rotations (ADR-0029 decision 6).
- **A locally-built release APK could carry dev keys.** Android env selection is zero-code: the
  `secrets` plugin reads whatever `.env` is present. Mitigated — not eliminated — by the CI dev-ref
  guard above; the published artifact is always the CI-built one (ADR-0032 decision 6).
- **Two approval gates now overlap, deliberately.** While this repository was private on GitHub
  Free, rulesets, classic branch protection and native Environment reviewer rules were all
  Pro/Team-gated and verified to refuse on this account (ADR-0032's correction); a local pre-push
  hook, a `branch-guard.yml` detective check and an issue-based approval gate stood in. Publishing
  the repository made the native controls free, so branch protection and the `prod` Environment
  reviewer rule are now enforced server-side (ADR-0034 decision 2) — but the issue-based
  `release-approval` / `prod-approval` jobs were **kept** rather than removed in the same change.
  Two gates in front of production is redundant, not wrong. Removing the substitute is tracked
  follow-up work.

---

## Security design

The full model is [`platform/PLATFORM.md`](platform/PLATFORM.md) §7 (eight layers) and §8
(compliance), with rationale in [`platform/DECISIONS.md`](platform/DECISIONS.md). Summary:

| # | Layer | Mechanism |
|---|---|---|
| 1 | Key storage | Android Keystore (hardware-backed) |
| 2 | Preferences | `EncryptedDataStore` — session tokens never land in plaintext prefs |
| 3 | Data at rest | Room; SQLCipher for the future vault (separate file, separate key) |
| 4 | Data in transit | OkHttp `CertificatePinner`, pinned at CA level (GTS Root R1 + R4) |
| 5 | Access control | `BiometricPrompt` Class 3 — convenience only, never the sole guardian |
| 6 | Screen capture | `FLAG_SECURE` on vault screens |
| 7 | Integrity | Play Integrity API, warn-only — never blocks app launch |
| 8 | Release builds | R8 full obfuscation |

**Secrets never live in the repo or the APK.** The online AI key sits behind a Cloudflare Worker
proxy (ADR-0002); Supabase and Google client values ride the `.env` secrets-plugin mechanism, with
an empty-defaulted `.env.example` committed and `.env` gitignored. Release secrets live in the
`dev` and `prod` GitHub Environments (ADR-0032 decision 2).

**GitLeaks gates every pull request**, including docs-only ones — secrets hide in markdown too
(`Gate 2 · Security` in [`.github/workflows/ci.yml`](.github/workflows/ci.yml); ADR-0026). It is
blocking, with no `continue-on-error`. OWASP dependency-check runs on a monthly schedule in
`owasp-scheduled.yml`, off the merge path.

**The vault is special** (ADR-0003, ADR-0031). Its real key derives from a user master password via
Argon2id, never from a device-bound Keystore key, and it has no network, AI, or analytics dependency
at all. Dhruv ID sign-in is never a path into vault data. A forgotten master password plus a lost
recovery key means the data is unrecoverable *by design*.

---

## Privacy and DPDP

India's Digital Personal Data Protection Rules 2025 are treated as a first-class layer and apply
from day one, independently of any Play Store launch (ADR-0005):

- **Consent precedes any off-device processing.** For tracker data this is structural rather than a
  convention — no code path can reach the server with consent off, because the interceptor is
  attached to the only client that can talk to PostgREST (ADR-0029).
- **Erasure is in-app and guaranteed within 7 days.** `delete_my_data()` removes every row you own;
  `delete_my_account()` also removes your identity row. Both are security-definer SQL functions
  callable by the signed-in user, with no service-role key anywhere near the device.
- **Consent is persisted and revocable**, not a one-time dialog.

Full detail: [`PRIVACY.md`](PRIVACY.md).