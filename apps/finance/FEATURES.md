# Dhruv Finance — Feature Modules

Index for all Finance feature modules after the Phase 4 split. Each is a `dhruv.android.library`
+ `dhruv.android.compose` Koin module under `apps/finance/feature/<bucket>/<name>/` (grouped by
owning tab since 2026-08-09 — Gradle coordinates unchanged, see
[feature/README.md](feature/README.md)), namespace `com.dhruv.finance.<name>`, depending on
`:apps:finance:data`, `:libs:core`, `:libs:settings`. Flags live in
`platform/feature-flags/dhruv-finance.json`.

**Per-module detail (screens, ViewModels, data deps) lives in each module's own `README.md`** —
this table is deliberately just an index (removed the duplicated per-module prose 2026-08-09; the
README is now the one place that detail is written, so it can't drift out of sync with itself).

## Tracking rule (binding)

**This table is the single index of every Finance module — shipped and planned.** A module exists
here from the moment a phase *plans* it, not from the moment it compiles, so that a reader can
always tell the difference between "built", "planned", and "nobody owns this".

Three things move together whenever a phase completes; none of them is optional, and each spec
carries a closure task for it:

1. **This table's row** — `Status` moves from *planned* to *enabled* / *disabled*, and the phase
   column is cleared.
2. **The module's own `README.md`** — the "not yet created" preamble is removed and the real
   screens / ViewModels / data dependencies / flag key are written. Per-module detail lives *only*
   there, never duplicated back into this table (that duplication was removed 2026-08-09 precisely
   because it drifted).
3. **`CHANGELOG.md`** at the repo root — an entry under the release heading CI injects, namespaced
   `finance-*`. The heading is CI's; the prose underneath is hand-written.

A shared library (`:libs:core`, `:libs:settings`) that a phase extends is **not** listed here — it
is not a Finance module. Its changes go in the CHANGELOG entry only.

## Modules

| Module | Gradle coordinate | Owner tab | Flag | Status |
|---|---|---|---|---|
| [app](app/README.md) | `:apps:finance:app` | — | — | shipped — shell, `MainActivity`, hubs, Koin aggregation |
| [data](data/README.md) | `:apps:finance:data` | — | — | shipped — Room + repositories + tracker network layer |
| [calculator](feature/calc/calculator/README.md) | `:apps:finance:feature:calculator` | Calc | `calculator` | enabled |
| [loans](feature/plan/loans/README.md) | `:apps:finance:feature:loans` | Plan | `loans` | enabled |
| [investments](feature/plan/investments/README.md) | `:apps:finance:feature:investments` | Plan | `investments` | enabled |
| [tax](feature/plan/tax/README.md) | `:apps:finance:feature:tax` | Plan | `tax` | enabled |
| [everyday](feature/plan/everyday/README.md) | `:apps:finance:feature:everyday` | Plan | `everyday` | enabled |
| [currency](feature/shell/currency/README.md) | `:apps:finance:feature:currency` | none — shell | `currency` | enabled |
| [unit](feature/shell/unit/README.md) | `:apps:finance:feature:unit` | none — shell | `unit` | enabled |
| [date](feature/shell/date/README.md) | `:apps:finance:feature:date` | none — shell | `date` | **disabled** |
| [time](feature/shell/time/README.md) | `:apps:finance:feature:time` | none — shell | `time` | **disabled** |
| [assistant](feature/shell/assistant/README.md) | `:apps:finance:feature:assistant` | none — shell | `assistant` | enabled, gated `minVersion 1.2.0`, `requiresConsent` |
| [onboarding](feature/onboarding/onboarding/README.md) | `:apps:finance:feature:onboarding` | none — pre-session | `onboarding` | enabled — A2 sign-in, A3 consent, A4 empty start (design-v1 Phase 1) |

### Planned — flag and README exist, Gradle module does not

Each row's module directory holds only a `README.md` until its phase creates it. Flags are
provisioned ahead of the code, the same way `date`/`time` were.

| Module | Gradle coordinate | Owner tab | Flag | Builds in | Spec |
|---|---|---|---|---|---|
| [networth](feature/home/networth/README.md) | `:apps:finance:feature:networth` | Home | `networth` | Phase 2 | [001-net-worth-tracker](specs/001-net-worth-tracker/) |
| [money](feature/money/money/README.md) | `:apps:finance:feature:money` | Money | `money` | Phase 3 | [002-money-tab](specs/002-money-tab/) |
| [planning](feature/plan/planning/README.md) | `:apps:finance:feature:planning` | Plan | `budgets`, `goals`, `debtpayoff` | Phase 4 | [003-plan-live-modules](specs/003-plan-live-modules/) |
| [insurance](feature/plan/insurance/README.md) | `:apps:finance:feature:insurance` | Plan | `insurance` | Phase 4 | [003-plan-live-modules](specs/003-plan-live-modules/) |
| [retirement](feature/plan/retirement/README.md) | `:apps:finance:feature:retirement` | Plan | `retirement` | Phase 4 | [003-plan-live-modules](specs/003-plan-live-modules/) |
| [insights](feature/insights/insights/README.md) | `:apps:finance:feature:insights` | Insights | `insights` | Phase 5 | [005-insights](specs/005-insights/) |
| [automation](feature/shell/automation/README.md) | `:apps:finance:feature:automation` | none — shell | `automation` | Phase 7 | **no spec-kit directory yet** — see the implementation plan §7 |

**Phases that add no Gradle module**, and so gain no row above — their work lands in
`:apps:finance:app` and `:libs:core`:

| Phase | Scope | Spec |
|---|---|---|
| 0b | Settings control plane (Account · App · Modules tiers), app lock | [004-settings](specs/004-settings/) |
| 6 | B2 notification centre, B3 global search | [006-search-notifications](specs/006-search-notifications/) |

---

## Design system

All feature screens use the **DhruvNext design system** (ADR-0024). Token usage:
- Colors: `LocalDhruvNextColors.current` (`acc`, `surf`, `tx`, `tx2`, `tx3`, `line`, `neg`, `pos`, etc.)
- Typography: `DhruvNextType.*` (`hero`, `title`, `cardTitle`, `body`, `meta`, `sectionLabel`)
- Spacing: `DhruvNextSpacing.*` (`screenGutter`, `interCardGap`, `sectionGap`, `cardPadding`, `inputGroupGap`)
- Radii: `DhruvNextRadii.*` (`card`, `listGroup`, `innerTile`, `pill`)
- Components: `NxCard`, `NxButton`, `NxTextField`, `SegmentedRow`, `SectionLabel`, `ListGroup`, `StatDeltaChip`, etc.

Zero `MaterialTheme.colorScheme` / `MaterialTheme.typography` refs remain in any screen file.
`CardDefaults` usage in date sub-views is intentional (accent-tinted `accSoft` result cards).
