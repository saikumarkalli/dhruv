# Route contract: Plan Live Modules (Phase 4)

Per-screen expansion of the four existing combined rows in
`apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md` §1 ("Plan root (E1)",
"Budgets / Goals / Debt payoff (E2–E6)", "Insurance / Policy detail (E7–E8)", "Retirement (E9)").
That registry stays the authoritative flat index — add these individual rows to it when this feature
is implemented; shown expanded once here for task-planning granularity, not maintained as a second
source.

| Route | Screen ID | Owner tab | `FeatureHost` key | Presentation | Consent | Parent |
|---|---|---|---|---|---|---|
| Plan root | E1 | Plan | — (shell, `PlanLauncher`) | root | — | none (tab root, N1) |
| Budgets | E2 | Plan | `budgets` | push | `requiresConsent` | Plan root (E1) |
| Budget detail | E3 | Plan | `budgets` | push | `requiresConsent` | Budgets (E2) |
| Goals | E4 | Plan | `goals` | push | `requiresConsent` | Plan root (E1) |
| Goal detail | E5 | Plan | `goals` | push (dark hero) | `requiresConsent` | Goals (E4) |
| Link holding | E5-link | Plan | `goals` | sheet | `requiresConsent` | Goal detail (E5) |
| Debt payoff | E6 | Plan | `debtpayoff` | push | `requiresConsent` | Plan root (E1) |
| Insurance | E7 | Plan | `insurance` | push | `requiresConsent` | Plan root (E1) |
| Policy detail | E8 | Plan | `insurance` | push | `requiresConsent` | Insurance (E7) |
| Retirement | E9 | Plan | `retirement` | push (dark hero) | `requiresConsent` | Plan root (E1) |

**The Plan root itself carries no consent gate and no feature key.** It is shell-owned
(`:apps:finance:app`, `ui/plan/PlanLauncher.kt` — module-standard `HOM`/`PLN` correction, impl plan
§6) and must remain reachable while signed out, because it also hosts the four offline calculators.
Its live rows degrade individually: a module whose flag is off, or whose consent is withdrawn,
renders that one row in its designed state (FR-004) rather than taking the tab down with it. This is
the same reason Home is shell-owned and `networth` is not.

**Navigation law compliance** (`platform/DESIGN-SYSTEM.md` §6): E1 is a tab root and shows no back
arrow (N1); every other row has exactly one parent (N2). E5-link is a sheet and dismisses down; it
never navigates (N3). E3's "Raise budget" and E9's assumption edits confirm on discard (N4) via
`rememberDiscardGuard`. Alerts, app-switcher and Settings stay reachable from the top bar on the Plan
tab (N5). Deep links land on E1, then push (N6). Every screen renders light and dark from the same
tokens (N7) — including the two dark-hero screens, whose "dark" is `DhruvBrand`'s theme-invariant
chrome (ADR-0028), not a dark-theme variant.

**Dark-hero screens.** E5 and E9 are drawn dark regardless of the user's theme, and are named as such
in the implementation plan §3.1's consumer list. They read `DhruvBrand.*`; every other screen in this
feature reads `LocalDhruvNextColors`. Neither reads a raw hex (Article V).

**Nested `NavHost`.** Plan already owns drill-in routes (the four calculators) and is the tab the
existing back contract was first written against. This phase adds six push routes and one sheet to
that same nested host — it introduces no new navigation mechanism, unlike Phase 3, which had to
generalise the host to a second tab.

## `NavTarget` additions

| Target | Resolves to | Consumer this phase |
|---|---|---|
| `OpenPlanModule(module: PlanModule)` | Plan tab → E2 / E4 / E6 / E7 / E9 | E1's five live rows |
| `OpenBudget(categoryId: String)` | Plan tab → E3 | Phase 6's budget-breach notification (row added now, consumer arrives then — see below) |
| `OpenGoal(id: String)` | Plan tab → E5 | E4's rows; Phase 5's insights cross-links |
| `OpenPolicy(id: String)` | Plan tab → E8 | E7's rows |

```kotlin
enum class PlanModule { BUDGETS, GOALS, DEBT_PAYOFF, INSURANCE, RETIREMENT }
```

`PlanModule` sits alongside the existing `PlanTool` (`LOAN`, `INVEST`, `TAX`, `EVERYDAY`) rather than
extending it: the calculator strip and the live modules are two different groups on E1 with different
flags, different consent, and different states, and collapsing them into one enum would make E1's own
grouping unrepresentable. Both are append-only constants (Article IX).

**`OpenBudget` is the one speculative case, and it is deliberate.** Phase 3's route contract argued
against adding a `NavTarget` before its consumer exists, and that reasoning stands. The exception
here is narrow: `OpenBudget` is what Phase 6's budget-breach notification dispatches to, that
notification is the *only* reason `budgets.alert_pct` is being written this phase at all (research
R8), and `PLN-FLOW-003` names the chain explicitly. Adding the case now keeps the deferred QA row
pointing at something real rather than at a gap. If Phase 6 slips indefinitely, this is one unused
sealed case — a smaller cost than a deferral that points nowhere.

Targets later phases will need — a filtered ledger from E3, a holding detail from E5, a liability
detail from E6 — are **not** added here. E3, E5 and E6 do link to those screens, and they do it
through targets that already exist or that their owning phase adds; see below.

## Cross-feature navigation in this phase

Three jumps leave the Plan tab. All three go through `NavTarget`, never an import (Article III):

| From | To | Mechanism |
|---|---|---|
| E3 "see these transactions" | Money tab, ledger filtered to that category | `SelectTab(MONEY)` + a category filter argument — the filter surface is Phase 3's (D5), so this jump lands only once Phase 3 has shipped |
| E5 "funded by" row | Home tab, holding detail (C3) | `OpenHolding(id)` — added by Phase 2 |
| E6 debt row | Home tab, liability detail (C6/C7) | `OpenLiability(id)` — added by Phase 2 |

`:feature:planning` never imports `:feature:money` or `:feature:networth`. If any of those three
target cases is missing when this phase is built, the correct move is to add the sealed case plus its
registry row — not to reach for a direct dependency.

## Feature flags

**Five new flags**, added by this phase to `platform/feature-flags/dhruv-finance.json`:

```json
"budgets":    { "enabled": true, "minVersion": "1.0.0", "requiresConsent": true },
"goals":      { "enabled": true, "minVersion": "1.0.0", "requiresConsent": true },
"debtpayoff": { "enabled": true, "minVersion": "1.0.0", "requiresConsent": true },
"insurance":  { "enabled": true, "minVersion": "1.0.0", "requiresConsent": true },
"retirement": { "enabled": true, "minVersion": "1.0.0", "requiresConsent": true }
```

All five are already mapped in the implementation plan §5.5's A3-switch table: each is gated by the
**"Sync my financial records"** consent switch, enforced by `ConsentInterceptor` in the client
construction path (Article VIII), not by screen-level checks. No new HTTP client is constructed by
this phase, so there is no path to PostgREST that bypasses the gate.

Five flags rather than one `plan` flag is deliberate: the five modules ship in priority order across
this phase, and a single flag would mean either holding all five back until the last one lands, or
turning on rows that render nothing.

## Screen-state coverage

Every route above declares the full matrix (`platform/DESIGN-SYSTEM.md` §7, spec FR-048). Two states
are worth naming because they are easy to get wrong here:

- **empty vs unfunded.** E4's "no funding linked yet" (FR-020) is a *goal state*, not an empty state
  — the goal exists and is shown. `EmptyStateCard` appears only when there are no goals at all. The
  `link_count` column in `v_goal_progress` exists precisely so these two are distinguishable.
- **not-configured vs empty.** A module whose flag is off renders `FeatureDisabledCard` via
  `FeatureHost`; a module that is on but has no data yet renders `EmptyStateCard` with a verb CTA
  ("Set your first budget"). On E1 both collapse to a single row's text (FR-004), and the row must
  say which — "Budgets are off" and "No budgets yet" are different sentences with different actions.