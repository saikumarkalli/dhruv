# Route contract: Money Tab (Phase 3)

Per-screen expansion of the existing combined row in
`apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md` §1 ("Ledger / Quick
add / Accounts / Categories / Recurring (D1–D9)"). That registry stays the authoritative flat index
— add these individual rows to it when this feature is implemented; shown expanded once here for
task-planning granularity, not maintained as a second source.

| Route | Screen ID | Owner tab | `FeatureHost` key | Presentation | Consent | Parent |
|---|---|---|---|---|---|---|
| Ledger | D1 | Money | `money` | root | `requiresConsent` | none (tab root, N1) |
| Quick add | D2 | Money | `money` | sheet | `requiresConsent` | Ledger (D1), FAB |
| Transaction form | D3 | Money | `money` | modal (close ✕, not back) | `requiresConsent` | Ledger (D1) via D2 "more options", or Transaction detail (D4) |
| Transaction detail | D4 | Money | `money` | push | `requiresConsent` | Ledger (D1) |
| Ledger filter | D5 | Money | `money` | sheet | `requiresConsent` | Ledger (D1) |
| Accounts | D6 | Money | `money` | push | `requiresConsent` | Ledger (D1) |
| Account detail | D7 | Money | `money` | push | `requiresConsent` | Accounts (D6) |
| Categories | D8 | Money | `money` | push | `requiresConsent` | Ledger (D1) |
| Recurring | D9 | Money | `money` | push | `requiresConsent` | Ledger (D1) |
| Recurring review | D9-review | Money | `money` | push | `requiresConsent` | Recurring (D9) |

**Navigation law compliance** (`platform/DESIGN-SYSTEM.md` §6): D1 is a tab root and shows no back
arrow (N1); every other row has exactly one parent (N2) — D3's two possible entry points are the
same *form*, entered from whichever screen launched it, and it returns there, so its parent is still
single-valued per instance. D2/D5 are sheets and dismiss down; neither navigates (N3). D3 confirms
on discard (N4) via `rememberDiscardGuard` — the helper Phase 0 descoped until a form needed it and
Phase 2 (C4) builds. Deep links land on D1, then push (N6).

**Presentation classes** follow the design's Finance list verbatim: sheet = D2 quick add, D5 filter;
full-screen modal (close ✕) = D3 full form.

**Nested `NavHost` generalisation.** Money is the second tab (after Plan) to own drill-in routes,
which is exactly the trigger Phase 0 named when it descoped this work: the back contract moves from
Plan's own controller to "the active tab's controller" in `resolveBackAction`
(`libs/core/.../navigation/BackContract.kt`), covered by `BackContractTest`. This phase generalises
it; it does not redesign it.

## `NavTarget` additions

| Target | Resolves to | Consumer this phase |
|---|---|---|
| `OpenAccount(accountId)` | Money tab → D7 | Home's UPCOMING credit-card-bill row (FR-034) |

Nothing else is added. Targets that later phases will need (filtered-ledger from a budget, D4 from a
search result, quick-add from a launcher shortcut) are deliberately **not** added until their
consumer exists — an unused `NavTarget` case is the speculative abstraction the project's own
altitude rule warns against, and adding one later is a two-line change (sealed case + registry row).

Cross-feature navigation stays by id, never by class reference — `:apps:finance:app`'s
`NavigationDispatcher` maps the target to (tab, nested route).

## Feature flag

**New flag**, added by this phase to `platform/feature-flags/dhruv-finance.json`:

```json
"money": { "enabled": true, "minVersion": "1.0.0", "requiresConsent": true }
```

Already mapped in the implementation plan's A3-switch table: `money` is gated by the **"Sync my
financial records"** switch, the same switch that gates `networth`. No new consent switch, no new
consent copy — `ConsentInterceptor` (Phase 1) already fails the call before dispatch when it is off.

While the flag is off, D1 renders `FeatureDisabledCard` through `FeatureHost`; the Money tab's
current `NotConfiguredCard` placeholder (Phase 0) is replaced by the real root.

## Signed-out / offline contract (FR-032, NFR-4)

Every route above renders one of `SignedOutCard` / `OfflineStateCard` / `EmptyStateCard` /
`RetryErrorCard` / content, per the screen-state matrix (`platform/DESIGN-SYSTEM.md` §7). This is a
contract every D1–D9 screen satisfies identically, not a per-screen choice. D1's empty state carries
a verb CTA ("Add your first transaction"), per the design system's copy rule.

## PostgREST schema profile

Every request to a `finance.*` table must carry `Accept-Profile: finance` (reads) or
`Content-Profile: finance` (writes) — ADR-0033's consequence note. Omitting it does not fail loudly;
it 404s against the empty `public` schema. This is client-construction level, applied once in
`SupabaseClientFactory`, not per call site.
