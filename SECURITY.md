# Security Policy

Dhruv is a personal-finance ecosystem. It handles money figures, a Google identity, and (in a
future module) passwords. Security issues are treated as the highest-priority class of bug here.

---

## Reporting a vulnerability

**Do not open a public issue for a security problem.**

Email **kallileelasaikumar@gmail.com** with:

- what the issue is, and which surface it affects (Android app / web SPA / Supabase / CI)
- the version or commit you found it on
- reproduction steps, or a proof of concept
- the impact you believe it has

You will get an acknowledgement within **7 days**. Fixes for anything that exposes user data are
prioritised over all other work.

Please give a reasonable window to ship a fix before disclosing publicly.

### In scope

The Android app, the web SPA, the Supabase schema and its RLS policies, the CI/CD pipeline, and
anything in this repository.

### Out of scope

- Reports that require a rooted/jailbroken device or a physical attacker with an unlocked phone
- Vulnerabilities in Supabase, Google, Firebase, Vercel or Cloudflare themselves — report those
  to the respective vendor
- Missing hardening that is documented as deliberate (see "Accepted risks" below)
- Automated scanner output with no demonstrated impact

---

## Supported versions

Only the **latest published release** of each app is supported. Versions are
`dhruv-<app>-vMAJOR.MINOR.PATCH`; see [`platform/versions.json`](platform/versions.json) and the
[Releases page](../../releases).

---

## How this repo defends itself

The full design is in [`platform/PLATFORM.md`](platform/PLATFORM.md) §7–§8 and the ADRs in
[`platform/DECISIONS.md`](platform/DECISIONS.md). Summary:

| Layer | Mechanism |
|---|---|
| Key storage | Android Keystore (hardware-backed) |
| Preferences | `EncryptedDataStore` — session tokens never land in plaintext prefs |
| Data at rest | Room; SQLCipher for the future vault (separate file, separate key) |
| Data in transit | OkHttp `CertificatePinner`, pinned at **CA** level (GTS Root R1 + R4) |
| Access control | `BiometricPrompt` Class 3 — convenience only, never the sole guardian |
| Screen capture | `FLAG_SECURE` on vault screens |
| Integrity | Play Integrity API, warn-only — never blocks app launch |
| Release builds | R8 full obfuscation |

**Secrets never live in the repo or the APK.** The online AI key sits behind a Cloudflare Worker
proxy (ADR-0002); Supabase/Google client values ride the `.env` secrets-plugin mechanism with an
empty-defaulted `.env.example` committed. **GitLeaks gates every PR**, including docs-only ones.

**The vault is special** (ADR-0003, ADR-0031): its real key derives from a user master password via
Argon2id, never from a device-bound Keystore key, and it has no network, AI, or analytics
dependency at all. Dhruv ID sign-in is never a path into vault data. Forgotten master password
plus lost recovery key means the data is unrecoverable *by design*.

---

## Privacy & data protection

India's DPDP Rules 2025 apply from day one, independently of any Play Store launch:

- **Consent precedes any off-device processing.** For tracker data this is structural, not a
  convention — a `ConsentInterceptor` sits on the only PostgREST-capable HTTP client in the app,
  so no code path can reach the server with consent off (ADR-0029).
- **Erasure is in-app and guaranteed within 7 days.** `delete_my_data()` removes every row you
  own; `delete_my_account()` also removes your identity row. Both are security-definer SQL
  functions callable by you, with no service-role key anywhere near the device.
- Consent is **persisted and revocable**, not a one-time dialog.

Full detail: [`PRIVACY.md`](PRIVACY.md).

---

## Accepted risks (documented, not oversights)

Listing these so a report about them is a duplicate rather than a surprise:

- **The Supabase anon key is publishable by design.** It is safe only because RLS
  (`user_id = auth.uid()`) is enforced on every tracker table. An RLS *gap* is a real
  vulnerability — the key's presence in a client is not.
- **Play Integrity is warn-only.** Root detection is bypassable and produces false positives; it
  restricts the vault, it never blocks app launch.
- **CA-level certificate pinning, not leaf pinning.** Leaf pinning would brick the app on
  Supabase's routine certificate rotations.
- **A locally-built release APK could carry dev keys.** Mitigated by the CI dev-ref guard
  (ADR-0032 decision 7); the published artifact is always the CI-built one.
- **Branch protection is not enforced server-side.** This is a private repo on GitHub Free, where
  both rulesets and classic branch protection are Pro-gated. Substitutes are in place — see
  [`CONTRIBUTING.md`](CONTRIBUTING.md#branching).