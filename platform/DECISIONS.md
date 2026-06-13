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