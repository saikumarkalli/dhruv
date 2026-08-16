# AGENTS.md — Dhruv Platform (master)

Read this first in every AI/agent session, before touching code.

## Session bootstrap (read in order)
1. `platform/PLATFORM.md` — architecture, source of truth
2. `platform/DECISIONS.md` — why things are the way they are
3. `platform/DESIGN-SYSTEM.md` — the design contract (tokens, components, nav law, states) —
   binding for **every** app; read before touching any UI
4. `platform/versions.json` — module compatibility
5. `platform/contracts/` — the contracts you must not break
6. `<module>/AGENTS.md` — module-local rules (if present)
7. The task/handoff prompt

## Hard rules (do not violate)
- **No code in `platform/`.** Docs and contracts only.
- **Do not redesign.** Decisions in `DECISIONS.md` are accepted. Propose a new ADR if you think one
  is wrong; do not silently diverge.
- **Branch rules**: always branch from `develop`. PRs target `develop`. `develop` is DEV
  (`dhruv-dev`, Vercel Preview, debug APK, ungated). `main` is PROD (`dhruv-prod`, Vercel
  Production, signed APK → GitHub Release, gated by one approval click on a GitHub issue — native
  Environment reviewer rules need GitHub Pro on a private repo, unavailable here) — never push to
  `main` directly; it only receives `develop → main` PRs
  (ADR-0032, supersedes the former "main = Play Store only" rule).
- **Module boundaries are enforced** (ArchUnit + Gradle): `feature → feature` forbidden;
  `vault → network/ai/analytics` forbidden; `feature → data` via Repository only; `core` depends on
  nothing internal.
- **Contract changes go through `platform/contracts/` first**, then `:libs:core` implements.
- **No secrets/keys in the repo or APK.** Online AI key lives in the Worker proxy. GitLeaks gates CI.
- **Vault is special.** No network/AI/analytics dependency. Biometric is convenience only; the real
  key derives from the master password. Emit `vault_module_error` and nothing else.
- **Every new online-data flow needs a consent gate** (DPDP) and a Play Data Safety entry.
- **Every feature route is wrapped in `FeatureHost`** with a fallback card — never a blank crash.

## Definition of done for a feature module
- ArchUnit dependency rules pass.
- Feature flag entry exists in `feature-flags/<app>.json`.
- Crash tagging (`setCustomKey "module"`) + at least one Performance trace.
- Unit + integration tests pass; bundle-size delta within budget.
- If it sends data off-device: consent gate + Data Safety entry present.