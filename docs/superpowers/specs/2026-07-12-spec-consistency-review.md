# Spec Consistency Review — Cross-Spec Gaps & Missing Connections

> Status: **REVIEW COMPLETE** (2026-07-12). Scope: every tracker/platform spec — P1–P6, tracker
> design system, engineering playbook, currency/metals plan, CI-cost spec, and the eight R-phase
> specs (`2026-07-12-r*`). Each finding lists its resolution; findings marked **PATCHED** were
> fixed in the referenced spec the same day; findings marked **RULE** became standing rules
> (master roadmap §4 / app design standard). Severity: 🔴 breaks correctness or compliance if
> unfixed · 🟡 breaks consistency/UX · ⚪ note.

## Findings

### 🔴 F1 — R7 export table list loses data on round-trip
`r7-reports-export-import` enumerated assets, liabilities, valuations, transactions, budgets,
policies, goals — **missing `goal_links` (P3) and `recurring_rules` (R5b)**. An export → wipe →
import cycle would silently drop every goal-asset link and every recurring rule, violating R7's
own losslessness guarantee. Also unstated whether soft-deleted rows export.
**Resolution: PATCHED in R7** — export list is now a named **Tracker Table Registry** (see F2);
`goal_links` + `recurring_rules` added; soft-deleted rows explicitly excluded (trash does not
survive export, documented in dialog copy + manifest).

### 🔴 F2 — No "new tracker table" registration rule (DPDP + export + trash drift)
Four mechanisms must know about every tracker table: (a) `delete_my_account()` / "Delete my data"
hard-delete (DPDP 7-day erasure — P2–P5 specs never say "extend the delete paths to the new
tables"), (b) R7 export manifest, (c) R8 trash (`deleted_at` column + purge job), (d) optionally
R8 search. Concrete instance: P5's `retirement_scenarios` ships **after** R7/R8 — without a rule,
it is born unexportable, untrashable, and (worst) **outside the DPDP erasure path**.
**Resolution: RULE** — added to master roadmap §4 standing debt: *every PR that creates a tracker
table must, in the same PR, register it in the delete paths, the export registry, the trash
(`deleted_at` from birth + purge), and decide search inclusion; the R7 round-trip test fixture
enumerates the registry, so a missed registration fails CI.* R8 patched to state trash columns
come from the registry; P5/P3/P2 inherit the rule without edits.

### 🔴 F3 — Money masking hole on non-Compose surfaces
R3's privacy mode masks via `MoneyText` + `LocalHideAmounts` — Compose-only. But money renders on
three non-Compose surfaces: R6 **notifications**, R8 **Glance widget** (own composition tree, no
MaterialTheme locals), R7 **PDF**. R6/R8 hand-waved "masked per R3"; the mechanism literally
cannot reach them.
**Resolution: PATCHED in R3** — core gains `MaskedMoney.mask(formatted: String): String` (pure
string transform, `₹••••••` stable-width) + rule: every non-Compose surface reads `hideAmounts`
itself and applies `MaskedMoney`. R6 (notification copy) and R8 (widget render model) patched to
reference it. R7 PDF deliberately unmasked (already documented — explicit user act).

### 🟡 F4 — QUICK_ADD intent vs app lock: contract owned by nobody
R5b routes launcher-shortcut extras through MainActivity and *assumes* they survive the R3 lock
overlay ("document in AppLockController"); R3's spec and checklist never mentioned intent-extra
survival. First integration bug waiting to happen.
**Resolution: PATCHED in R3** — lock flow now states: pending navigation extras are held, lock
resolves first, extras dispatch after unlock; checklist item added (test: locked + shortcut →
unlock → QuickAddSheet).

### 🟡 F5 — R9's privacy carve-out references a rule R3 doesn't contain
R9 shows percentages unmasked under privacy mode "documented there at implementation [R3]" — R3
had no such carve-out.
**Resolution: PATCHED in R3** — privacy-mode section now states: percentages and ratios remain
visible (cannot reconstruct balances); absolute ₹ always masks.

### 🟡 F6 — P3's "progress ring" is not in the design-system component inventory
`GoalsScreen` renders progress rings; the binding component inventory (tracker-design-system) has
no ring primitive — P3 would have been forced to violate the micro-frontend rule or invent one
ad hoc.
**Resolution: RULE/DESIGN** — `ProgressRing` added to the component inventory in the app design
standard (`2026-07-12-app-design-standard.md` §5), Canvas conventions matching TrendLineChart.

### 🟡 F7 — Home bento card sprawl ungoverned
By P5 the Home grid accumulates: hero + assets + liabilities (P1), this-month budget (P2), top
goal (P3), insurance cover (P4), retirement status (P5), YTD savings (R7), review-inbox badge
(R5b). No spec defines order, density cap, or per-card visibility. Home becomes a wall.
**Resolution: DESIGN** — app design standard §3.2 defines the Home card registry: fixed order,
hero always first, max 6 tracker cards visible, per-card hide toggles in Settings (G15 toggle
precedent), overflow collapses to a "More" row.

### 🟡 F8 — Navigation topology never specced app-wide
P1 defines pager tabs + page-0-home + nested BackHandler; every later spec adds screens with only
"tap → X". No route registry, no FeatureHost-key list, no statement of which routes are `secure`
(R3) — each spec re-derives it.
**Resolution: DESIGN** — app design standard §3 is the authoritative route/IA map (tab list, route
registry with FeatureHost key + secure flag + accent per route, back contract restated).

### 🟡 F9 — Settings IA is accretion, not design
Blocks land per-phase: Security (R3), About>Updates (R4), Alerts (R6), Data (R7/R8 export/import/
trash), reminder prefs (P4), plus seven existing sections (General, Calculator, Sections,
Appearance, Security-legacy, History, AI, About). Nobody designed the tree; the current
`SettingsScreen` is already a monolith (M4-adjacent).
**Resolution: DESIGN** — app design standard §4 defines the complete target settings tree; every
existing + specced setting mapped; phases slot rows into it instead of appending sections.

### 🟡 F10 — Notification channels & intent actions: six channels, no registry
Channels across specs: daily rates (currency plan), `app_updates` (R4), `recurring_review` (R5b),
`budget_alerts` + `emi_reminders` (R6), renewal reminders (P4). Intent extras: `QUICK_ADD`,
`REVIEW_INBOX` (R5b) — while P4 says "deep-links to PolicyDetailScreen" with no mechanism.
**Resolution: DESIGN** — app design standard §7 carries the channel registry (ids, importance,
masking behavior) and the intent-action registry; P4's deep link aligns to the extra mechanism
(`OPEN_POLICY(id)`) at build time — noted there, no P4 edit needed before its phase starts.

### 🟡 F11 — Coverage-aggregation registration not stated for new modules
G2 (P1 gap analysis) established that new modules must join root `coveredModules` or JaCoCo
silently ignores them. R7 (`:feature:reports`) and R8 (`:feature:search`) specs omitted it.
**Resolution: RULE** — added to master roadmap §4 standing debt (applies to every new module).

### ⚪ F12 — Widget snapshot store vs backup rules
R8 claims the widget snapshot is "excluded from backup (R0 rules)" — R0's rules predate the store.
**Resolution: PATCHED in R8** — execution note: add the snapshot DataStore file to the backup
exclusion set in the same PR.

### ⚪ F13 — MoneyText retrofit direction
P2–P5 specs (written before R3) say raw `formatPaise` text. Rule needed so post-R3 phases build
with `MoneyText` from the start. **Resolution: DESIGN** — standard §5 marks `MoneyText` as the
only sanctioned money renderer for all new Compose work; legacy adoption stays lazy.

### ⚪ F14 — Pull-to-refresh inconsistency
P1 explicitly defers pull-to-refresh (overflow Refresh instead); no later spec revisits, some
imply list refreshing. **Resolution: DESIGN** — standard §6: overflow/explicit refresh remains the
pattern until a dedicated revisit; no per-feature pull-to-refresh improvisation.

### ⚪ F15 — SectionTheme accents unassigned for new surfaces
Tracker = green (P1); reports/search/insurance/goals/retirement accents unstated.
**Resolution: DESIGN** — standard §2 assigns: all tracker-domain routes inherit the green section
theme (one domain, one accent); Tools sub-features keep their existing per-section accents.

### ⚪ F16 — Feature-flag JSON entries for `recurring`, `search`, `reports`
Flag names specced; `platform/feature-flags/dhruv-finance.json` + hardcoded-floor defaults must
gain entries at each phase. Covered by existing playbook DoD ("flag entry exists") — no action.

## Summary

| Severity | Count | Patched now | Became rule/design-standard |
|---|---|---|---|
| 🔴 | 3 | F1, F3 | F2 |
| 🟡 | 8 | F4, F5 | F6–F11 |
| ⚪ | 5 | F12 | F13–F16 |

No contradictions found between the P1–P6 specs themselves; all findings are connective tissue
between the older tracker specs and the newer R-phase specs, plus governance that only becomes
visible when all fifteen documents are read as one system — which is exactly what the app design
standard (`2026-07-12-app-design-standard.md`) now is for the UI half.
