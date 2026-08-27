# Route contract: Insights (Phase 5)

Per-screen expansion of the single combined row in
`apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md` §1 ("Monthly summary /
Cashflow / P&L / Balance sheet / Reports (F1–F5)"). That registry stays the authoritative flat index —
add these individual rows to it when this feature is implemented; shown expanded once here for
task-planning granularity, not maintained as a second source.

| Route | Screen ID | Owner tab | `FeatureHost` key | Presentation | Consent | Parent | Sub-phase |
|---|---|---|---|---|---|---|---|
| Monthly summary | F1 | Insights | `insights` | root | `requiresConsent` | none (tab root, N1) | 5a |
| Cashflow statement | F2 | Insights | `insights` | push | `requiresConsent` | Monthly summary (F1) | 5b |
| Balance sheet | F4 | Insights | `insights` | push | `requiresConsent` | Monthly summary (F1) | 5c |
| Profit & loss | F3 | Insights | `insights` | push (dark hero) | `requiresConsent` | Monthly summary (F1) | 5d |
| Reports & export | F5 | Insights | `insights` | push | `requiresConsent` | Monthly summary (F1) | 5e |
| Export format | F5-export | Insights | `insights` | sheet | `requiresConsent` | Reports (F5) or the statement being exported | 5e |
| Custom range picker | F5-range | Insights | `insights` | sheet | `requiresConsent` | whichever screen opened it | 5e |
| Balance-sheet date | F4-date | Insights | `insights` | sheet | `requiresConsent` | Balance sheet (F4) | 5c |

**Unlike the Plan root, the Insights root is not shell-owned and does carry a consent gate.** Every
screen in this tab is network-backed with no offline counterpart (functional spec D-6 — Calc and the
converters are the only offline surfaces). There is nothing in Insights that stays useful while
signed out, so the tab root itself is a `FeatureHost`-wrapped feature route rather than a shell
launcher with degrading rows. Signed-out, offline and consent-withdrawn all render the designed
state trio on the root, not a partial screen.

**One `FeatureHost` key for all five screens**, not one per screen. Phase 4 used five keys because
its modules are genuinely separable products a user might want individually; the five Insights
screens are one statement set behind one period model, and a user who disables "profit & loss" but
keeps "cashflow" is not a case anyone asked for. One key also means the flag-off state is coherent:
the whole tab renders `FeatureDisabledCard` rather than a root whose links go nowhere.

**The tab already exists.** `TabKey.INSIGHTS` and its `BottomNavItems` row shipped in Phase 0
(ADR-0027); the tab currently renders `NotConfiguredCard`. This feature swaps that root for F1 and
**retains** `NotConfiguredCard` as the not-configured state, rather than deleting it — it is still
the correct rendering when the flag is present but the backend is unreachable.

**Navigation law compliance** (`platform/DESIGN-SYSTEM.md` §6): F1 is a tab root and shows no back
arrow (N1). F2–F5 each have exactly one parent, F1, and show one back arrow (N2) — they are siblings
reached from the root, never chained into each other, even though the month-end review flow walks
them in sequence. The three sheets dismiss down and never navigate (N3). No Insights screen is an
add/edit form, so N4 does not apply. Alerts, app-switcher and Settings remain reachable from the top
bar on the tab root (N5). `OPEN_REPORTS` deep links land on the tab root and then push (N6).

## `NavTarget` additions

```kotlin
enum class StatementKind { CASHFLOW, PROFIT_LOSS, BALANCE_SHEET, CATEGORY_BREAKDOWN }

data class OpenStatement(val kind: StatementKind) : NavTarget
data class OpenReports(val period: ReportingPeriodRef?) : NavTarget   // null = whatever is current
data class OpenBalanceSheet(val asOf: LocalDate?) : NavTarget         // null = period end
```

`OpenReports` is the target the surface registry §3 already reserves as `OPEN_REPORTS(month)` for the
monthly digest. It is registered **now**, in 5e, because FR-033 requires Reports to accept a period
from outside the screen; the notification that will dispatch it arrives in the notifications phase
(FR-048). A target that exists before its only producer is deliberate here — the alternative is the
notifications phase reaching into this feature to add one.

`ReportingPeriodRef` is a serialisable period reference (kind + start + end), not the domain
`ReportingPeriod` — `:libs:core` must not gain a Finance domain type (Article III). The feature
resolves the ref through `PeriodResolver` on arrival.

**Intent extras are untrusted input** (surface registry §1): an out-of-range or unparseable period
ref, or an `asOf` outside the user's data, resolves to the screen's normal designed state — the
period falls back to the current month, the date falls back to the period end. Never a crash, never
an empty screen with no explanation.

## Cross-feature navigation in this phase

**None, deliberately.** A statement line is the one place this feature could plausibly link out —
tapping an expense line to see the transactions behind it, or an asset row to open the holding. It is
out of scope (spec Scope Boundaries: statements are read surfaces; corrections happen where the
record lives). Consequence: `:feature:insights` needs no `NavTarget` dispatch to `:feature:money` or
`:feature:networth`, and the module has no outbound cross-feature edge at all — the simplest possible
position under Article III.

If drill-through is added later it is a `NavTarget` dispatch (`OpenTransaction`, `OpenHolding` —
both of which those phases will already own), never an import.

## Feature flags

One new entry in `platform/feature-flags/dhruv-finance.json`, added in **5a**:

```json
"insights": { "enabled": true, "minVersion": "1.0.0", "requiresConsent": true }
```

The name is already reserved by the implementation plan §5.5, and §5.5's consent table already maps
`insights` to the "Sync my financial records" A3 switch — so `ConsentInterceptor` gates it with no
change to the interceptor, only the flag entry. `requiresConsent: true` is mandatory here: every
route in this tab reaches PostgREST.

**Sub-phase flag posture.** The flag ships `enabled: true` in 5a because 5a delivers a complete,
honest screen (F1). Sub-phases 5b–5f each add routes behind the same flag; a route that does not yet
exist is simply not reachable from F1, rather than being a dead row. No sub-phase ships a link to a
screen it did not build.

## Screen-state coverage

Every route above renders the full NFR-4 trio plus the flag-off state, per the design system's
screen-state matrix (§7) and spec FR-041 / FR-045:

| State | Component | When |
|---|---|---|
| loading | `SkeletonBlock` | first load of a statement only; a period change refreshes silently |
| default | — | statement rendered |
| empty | `EmptyStateCard` | period contains no records (FR-045) — distinct from a zeroed statement |
| error | `RetryErrorCard` | the reporting call failed and is retryable |
| offline | `OfflineStateCard` | no connectivity; nothing is cached, so there is no banner variant here |
| signed-out | `SignedOutCard` | no session |
| not-configured | `NotConfiguredCard` | backend unreachable or unconfigured — the state the tab shows today |
| disabled | `FeatureDisabledCard` (via `FeatureHost`) | `insights` flag off |

**No offline banner variant.** `OfflineBanner` exists for surfaces with cached content still worth
showing. Insights caches nothing (data-model: statements are never stored), so offline is always the
full-card state. Recorded so nobody adds a banner and implies stale figures are live ones.