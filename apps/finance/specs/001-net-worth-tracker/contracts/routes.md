# Route contract: Net Worth Tracker (Phase 2)

Per-screen expansion of the existing combined row in
`apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md` §1
("Net worth / Assets / Liabilities (C1–C7)"). That registry stays the authoritative flat index —
add these individual rows to it when this feature is implemented; not duplicated as a second
source here going forward, shown expanded once for task-planning granularity.

| Route | Screen ID | Owner tab | `FeatureHost` key | Presentation | Consent | Parent |
|---|---|---|---|---|---|---|
| Home | 01 | Home | — (shell, `:apps:finance:app`) | root | — | none (tab root, N1) |
| Net worth overview | C1 | Home | `networth` | push | `requiresConsent` | Home (01) |
| Assets | C2 | Home | `networth` | push | `requiresConsent` | Net worth overview (C1) |
| Holding detail | C3 | Home | `networth` | push | `requiresConsent` | Assets (C2) or Liabilities (C6) |
| Add/edit holding | C4 | Home | `networth` | modal (close ✕, not back) | `requiresConsent` | Assets (C2) or Liabilities (C6), FAB |
| Add valuation | C5 | Home | `networth` | sheet | `requiresConsent` | Holding detail (C3) |
| Liabilities | C6 | Home | `networth` | push | `requiresConsent` | Net worth overview (C1) |
| Liability detail | C7 | Home | `networth` | push | `requiresConsent` | Liabilities (C6) |

**Navigation law compliance** (`platform/DESIGN-SYSTEM.md` §6, constitution reference in Article
III's enforcement): every non-root row above has exactly one parent (N2); C4 and C5 are the
presentation classes (modal / sheet) the design law reserves for add/edit forms and quick entry
(§6 "Presentation classes"); cross-navigation from Home's quick actions or C7's prepay hand-off
into the loan calculator is by `NavTarget` id, never a class reference (N-rule already enforced by
`NavTarget.kt`/`NavigationDispatcher.kt`).

## Feature flag

`networth` already exists in `platform/feature-flags/dhruv-finance.json`
(`enabled: true, requiresConsent: true`) — scaffolded ahead of the module in Phase 1. This feature
does not add a new flag; it is the first feature to actually be gated by this one.

## Signed-out / offline contract (FR-011, NFR-4)

Every route above except Home's own shell chrome renders one of `SignedOutCard` /
`OfflineStateCard` / normal content, per the screen-state matrix (`platform/DESIGN-SYSTEM.md` §7) —
this is a contract every C1–C7 screen and Home must satisfy identically, not a per-screen choice.
**Known gap, found 2026-09-02 (Phase 8 QA pass):** C2 (Assets) and C6 (Liabilities) were shipped
without this gating at all — only C1 implemented it. Closed for C2/C6 in Phase 9; see the
Implementation record.

## NavTarget additions (T049 — this spec's contracts previously had no section for this)

**This phase adds zero new `NavTarget` cases**, contrary to what 002/003/006 assumed
("`OpenHolding`/`OpenLiability` added by Phase 2" — that citation is incorrect and should be
corrected in those specs when they're implemented). The reason: every C1↔C7 navigation
(overview→assets→holding detail→add valuation, overview→liabilities→liability detail) is
**intra-module**, driven by a local `NavHostController` owned by `NetWorthFeatureRoot`
(`NetWorthNavHost.kt`) — `NavTarget`'s own doc comment scopes it to cross-feature/cross-tab
dispatch, and nothing inside this module ever needed to reach across that boundary to open a
specific holding. If a future phase (e.g. 006's search results, or a notification deep link) needs
to open C3/C7 for a specific holding **from outside this module**, that is the point at which
`NavTarget.OpenHolding(holdingId)`/`OpenLiability(holdingId)` cases would need to be added to
`NavTarget.kt` (`:libs:core`) plus a matching top-level destination in `NetWorthNavHost.kt` (or a
shell-level entry analogous to `DetailRoute.NetWorth`, since this module has no tab of its own to
receive a `SelectTab` dispatch into) — not assumed to already exist.

**Home → Currency quick action (the one real cross-tab case this phase's Home screen added,
resolved as a deviation in the Phase 7 Implementation record):** `NavTarget.kt` deliberately
excludes Currency (a shell-level detail route, not tab-scoped). Loan EMI/SIP/GST route via
`NavigationDispatcher` + `NavTarget.OpenPlanTool` as HOM-UI-002 specifies; Currency routes through
the pre-existing `DetailRoute.Currency` shell mechanism instead — not a new `NavTarget` case, and
not the same mechanism as the other three actions.

**C7's prepay hand-off** (already shipped, Phase 6) also adds no new case — `PlanTool.LOAN` already
existed; this phase is simply its first consumer via the (also Phase-6-relocated)
`NavigationDispatcher` in `:libs:core`.
