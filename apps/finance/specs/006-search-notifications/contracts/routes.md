# Route contract: Search & Notifications (Phase 6)

Expansion of the two rows the surface registry
(`apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md` §1) already carries for
this phase — "Notifications (B2)" and "Search (B3)". That registry stays the authoritative flat
index; update those two rows when this feature is implemented. Shown expanded here for
task-planning granularity, not maintained as a second source.

## Routes

| Route | Screen ID | Owner tab | `FeatureHost` key | Presentation | Consent | Parent |
|---|---|---|---|---|---|---|
| Notification centre | B2 | Home | `alerts` | push, back top bar, no tab bar | `requiresConsent` | Home (01) |
| Global search | B3 | Home | `search` | push, back top bar, no tab bar | `requiresConsent` | Home (01) |

**Both are shell-owned and neither gets a Gradle module.** The implementation plan §6 assigns
`"Home 01, B2, B3, shell, Plan root E1"` to `:apps:finance:app`; this contract honours that. The
registry's FeatureHost-key column currently reads `— (shell)` for both, which meant *no feature
module*, not *no flag* — Article IV requires a flag entry for every route. **This phase adds the two
flag keys and updates those two registry rows accordingly.**

This is not the Plan-root case. Phase 4's route contract argued the Plan root carries no flag and no
consent gate *because it must stay reachable while signed out to host the offline calculators*. B2
and B3 have no offline content whatsoever — every byte either screen shows is derived from tracker
records — so flagging and consent-gating them costs nothing and buys a kill switch.

## Entry points

| Screen | Reached from | Notes |
|---|---|---|
| B2 | The Home top bar's alerts icon | Design-system N5 makes alerts reachable from the top bar on **every** tab, so the icon is on the shared top bar rather than Home's alone. The unread count rides this icon (FR-017) |
| B2 | A posted notification's tap target | Only when the notification's own destination cannot be resolved; otherwise a notification opens its **subject**, not the centre (FR-028) |
| B3 | The Home top bar's search icon | Home only. Scoped searches inside other screens (C2's asset search, D1's ledger filter) are separate controls and are not replaced |

## Feature flags

**Two new flags**, added to `platform/feature-flags/dhruv-finance.json`:

```json
"search": { "enabled": true, "minVersion": "1.0.0", "requiresConsent": true },
"alerts": { "enabled": true, "minVersion": "1.0.0", "requiresConsent": true }
```

Neither is reserved in implementation plan §5.5 (`money · budgets · goals · debtpayoff · insurance ·
retirement · insights · automation`), so both are additions this phase makes to that list. Both map
to the **"Sync my financial records"** A3 switch in §5.5's consent table, alongside every other
tracker flag.

`alerts` governs more than the screen: **flag off means the evaluation worker is never enqueued**, and
an already-scheduled worker is cancelled. That is the first thing the suppression ladder checks
(research R7 step 1) and the reason the two flags are separate — a misbehaving background job must be
switchable off without taking a working search field with it.

## `NavTarget` additions

**None.** This is the notable outcome of the design pass, not an omission.

| Destination | Target | Added by | First consumer |
|---|---|---|---|
| Holding detail (C3) | `OpenHolding(id)` | Phase 2 | search results; the valuation-stale alert |
| Liability detail (C7) | `OpenLiability(id)` | Phase 2 | the instalment-due alert |
| Budget detail (E3) | `OpenBudget(categoryId)` | Phase 4 | **the budget-breach alert — this phase** |
| Goal detail (E5) | `OpenGoal(id)` | Phase 4 | search results |
| Policy detail (E8) | `OpenPolicy(id)` | Phase 4 | search results; the renewal alert |
| Reports at a period (F5) | `OpenReports(period)` | Phase 5 | the monthly-summary alert |

Phase 4's route contract added `OpenBudget` with no consumer, called it *"the one speculative case,
and it is deliberate"*, and justified it by naming this feature. That bet is settled here.

**The one possible addition.** Search results include transactions, whose detail screen is D4. Phase 3
deliberately declined to add `OpenTransaction` until something needed it — *"targets that later phases
will need (… D4 from a search result …) are deliberately not added until their consumer exists"*. If
Phase 3 has not added it by the time 6a runs, **6a adds it**: the sealed case and the registry row in
one change, per the registry's own pairing rule. It belongs to the search work, not to the alert
pipeline.

## Intent action registry additions

Surface registry §3 lists intent extras by producer. This phase becomes the producer for three rows
that already exist, and adds two:

| Extra value | Destination | Producer | Status |
|---|---|---|---|
| `OPEN_BUDGETS` | Budget detail (E3) | budget-breach alert | existing row — **producer arrives here**; note the row currently points at E2, and the alert dispatches to E3 with a category, so the row is corrected when implemented |
| `OPEN_POLICY(id)` | Policy detail (E8) | renewal alert | existing row — producer arrives here |
| `OPEN_REPORTS(month)` | Reports at month (F5) | monthly-summary alert | existing row — producer arrives here |
| `OPEN_LIABILITY(id)` | Liability detail (C7) | instalment-due alert | **new row** |
| `OPEN_HOLDING(id)` | Holding detail (C3) | valuation-stale alert | **new row** |

Every extra is untrusted input. An unknown, foreign or deleted id resolves to that record's normal
not-found state, never a crash — the registry's own rule, restated here because a background-posted
notification can outlive the record it names, which is a far more likely path to a bad id than a
hand-crafted link.

## Navigation law compliance (`platform/DESIGN-SYSTEM.md` §6)

- **N1** — neither screen is a tab root; both show a back arrow. Correct by construction.
- **N2** — both have exactly one parent, Home (01). A notification opening a *subject* lands on that
  subject's own parent chain, not on B2 — the centre is not inserted into the back stack of an alert
  the user opened from the shade.
- **N3** — neither is a sheet; nothing here dismisses down.
- **N4** — neither screen has a form; no discard guard applies.
- **N5** — the alerts icon is on the shared top bar and stays reachable from every tab, which is why
  the unread badge rides it rather than a Home-only affordance.
- **N6** — every alert lands on the owning tab's root and then pushes the subject (FR-029). This is
  what makes the back gesture return to that tab instead of exiting the app.
- **N7** — both screens render light and dark from the same tokens; neither is a dark-hero surface, so
  both read `LocalDhruvNextColors` and neither reads `DhruvBrand`.

## Interaction with the app-lock gate

Consumed unchanged from `apps/finance/specs/004-settings/contracts/app-lock-gate.md`:

- §4 rule 18 — a locked app does **not** stop an alert being posted, only opened. The worker and the
  notifier are unaffected by lock state.
- §3 rules 11–15 — an alert opened while locked is **held**, dispatched once after unlock, replaced if
  a second arrives, cleared on process death, and resolves an unknown id to a not-found state.

This phase adds no lock behaviour. It is the first real producer of held intents, so 6c verifies the
contract against a real notification rather than a synthetic link — but it verifies, it does not
re-specify.