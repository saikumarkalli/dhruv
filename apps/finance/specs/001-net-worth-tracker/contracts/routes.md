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
