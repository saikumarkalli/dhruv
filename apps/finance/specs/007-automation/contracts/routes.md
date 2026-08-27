# Route contract: Automation (Phase 7)

Expansion of the single row the surface registry
(`apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md` §1) carries for this
phase — *"Automation hub / Review queue / AA consent (G1–G3)"*. That registry stays the authoritative
flat index; update its row when this feature is implemented. Expanded here for task-planning
granularity, not maintained as a second source.

## Routes

| Route | Screen ID | Owner tab | `FeatureHost` key | Presentation | Consent | Parent |
|---|---|---|---|---|---|---|
| Automation hub | G1 | — (shell detail) | `automation` | push, back top bar, no tab bar | `requiresConsent` | Settings › Modules › Automation |
| Review queue | G2 | — (shell detail) | `automation` | push, back top bar, no tab bar | `requiresConsent` | G1 |
| Ignored list | — (new, FR-008a) | — (shell detail) | `automation` | push, back top bar, no tab bar | `requiresConsent` | G2 |
| Account linking | G3 | — (shell detail) | `automation` | **full-screen modal, close ✕** | `requiresConsent` | G1 |

**All four are shell detail routes**, reached from Settings and belonging to no tab — the same class
as Settings, Ask and the converters, which `NavTarget`'s own doc comment already carves out. They get
a Gradle module (`:apps:finance:feature:automation`, impl plan §6) but no tab and no bottom bar.

**G3 is a modal, not a push** (design-system §6: "full-screen modal, close ✕ = add/edit forms and
**scoped consent flows**"). It grants nothing and enables nothing on dismiss (FR-041).

**The Ignored list is new to the design.** The 2026-08-23 clarification introduced it; no screen was
drawn for it. It is a plain list reusing G2's row component in a non-actionable variant plus a
Restore action — deliberately not a fourth designed surface, so it inherits G2's states and copy
rather than inventing its own.

## Entry points

| Screen | Reached from | Notes |
|---|---|---|
| G1 | Settings › Modules › **Automation** | The module ships its own settings entry (004's control-plane mechanism); no phase edits a central list |
| G2 | G1's "N entries need review" banner | The primary path |
| G2 | The **entries-waiting alert** (`REVIEW_INBOX`) | Through the app lock's hold-and-dispatch — FR-037 |
| G2 | Phase 3's D9 recurring banner | **This phase repoints it.** D9's banner currently opens Phase 3's recurring-only review surface; once G2 exists it opens G2, discharging Phase 3's deferral |
| Ignored list | G2's overflow / footer entry | Not a tab, not a top-level surface |
| G3 | G1's Account-aggregator row | Which is marked unavailable — the row is tappable to explain, per FR-042 |

## Feature flag

**One new flag**, added to `platform/feature-flags/dhruv-finance.json`:

```json
"automation": { "enabled": false, "minVersion": "1.0.0", "requiresConsent": true }
```

**This key is genuinely absent from that file today** — verified 2026-08-23, the file holds eleven
keys ending at `networth`. Implementation plan §5.5 reserves the name; the reservation was never
applied. This phase applies it.

`enabled: false` until this phase's checkpoint passes (surface registry §1, impl plan §5.5). The flag
governs more than the screens: **flag off means neither background worker is ever enqueued**, and an
already-scheduled one is cancelled.

**One flag, not two.** 006 split `search`/`alerts` because a slow query and a misbehaving worker are
unrelated failures. Here every surface is one feature — switching automation off should stop all of
it, including scanning.

### Consent mapping

Not re-decided here; implementation plan §5.5's table already fixes it.

| A3 switch | Effect on this feature |
|---|---|
| **Sync my financial records** | Gates the `automation` flag like every tracker flag. Off ⇒ no PostgREST call, including from the worker (`ConsentInterceptor`) |
| **Read transaction SMS** | Required **additionally**, on top of the row above, before the message source may read anything (FR-020). Withdrawing it freezes that source's outstanding proposals (FR-026a) |

## `NavTarget` additions

**None.** Verified against the code, not assumed: `libs/core/.../navigation/NavTarget.kt` today has
exactly two cases, `SelectTab` and `OpenPlanTool`.

Shell detail routes do not need a `NavTarget` case — the sealed type exists for **cross-tab** dispatch,
and its doc comment says so directly of the existing detail routes. G1–G3 and the Ignored list are the
same shape, so they are reached by the shell's own navigation, not by a target id.

**Two targets this phase would *consume* if they existed**, and what it does instead:

| Want | Target | Status | This phase |
|---|---|---|---|
| Open a duplicate's existing transaction (D4) from the callout | `OpenTransaction(id)` | Phase 3 declined to add it until a consumer existed; 006's search may add it first | 7d **shows** the matched record inline on the callout rather than navigating. If the case exists by then, the callout also links; if not, this phase does not add it — the inline summary satisfies FR-029, which asks the callout to *identify* the match, not to navigate to it |
| Open a revalued holding (C3) from a price row | `OpenHolding(id)` | Phase 2's to add | Same rule — the row names the holdings; navigation is additive if the case has landed |

This keeps the registry's pairing rule (a sealed case and a registry row land together) from being
satisfied speculatively by a phase that does not need the case.

## Intent action registry

Surface registry §3 already carries the row this phase needs:

| Extra | Destination | Producer (registry today) | Producer (after this phase) |
|---|---|---|---|
| `REVIEW_INBOX` | Review queue (G2) | recurring notification (R5b) | **+ the entries-waiting alert (this phase, 7g)** |

**No new extras.** `REVIEW_INBOX` was registered against a destination that did not exist; 7g makes it
real. The extra passes through 004's app-lock hold-and-dispatch, so FR-037's "honouring the app lock
rather than bypassing it" is inherited. An extra arriving while the feature flag is off resolves to
the disabled state, never a crash — the registry's own rule for unknown or foreign ids.

## Registry rows this phase changes

| File | Section | Change |
|---|---|---|
| Surface registries | §1 | The Automation row gains the Ignored list and the `automation` flag key; drop "— (Settings › Modules › Automation)" ambiguity in the owner-tab column by naming it a shell detail route |
| Surface registries | §3 | `REVIEW_INBOX` gains this phase as a producer |
| Surface registries | §4 | **Delete *Recently deleted* from the Automation entry** (FR-054). Trash belongs to Phase 0b's Settings control plane per readiness §5.2; the line is stale. *Import* also leaves — CSV import is deferred (spec clarification 2) and the A4 CTA is its only surface |
| `platform/feature-flags/dhruv-finance.json` | — | + `automation` |
| Impl plan §7 | Phase 7 row | Status → implemented; the sub-phase table |