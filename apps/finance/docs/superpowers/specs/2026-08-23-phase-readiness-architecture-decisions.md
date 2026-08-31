# Finance design-v1 — phase-readiness architecture decisions (SA)

> **Purpose.** The 2026-08-22 multi-agent audit
> (`../reviews/2026-08-22-spec-phase-gap-register.md`) left 125 remediation tasks across six
> spec-kit phases. Many were open *questions*, not work — and a question with no answer blocks
> every engineer who reaches it. This document answers them, so UI, Backend and DB can start.
>
> **Status of each decision is explicit.** `[SA]` = decided here on technical grounds, with the
> governing ADR named; a reader may implement it directly. `[SA-DEFAULT]` = a product call the
> architecture does not determine — decided here so nothing blocks, **reversible, and flagged for
> the maintainer**. Nothing below is silently assumed.
>
> **Precedence.** This document resolves open items; it does not override `platform/DECISIONS.md`,
> `platform/DESIGN-SYSTEM.md`, or the functional spec. Where it makes a call those documents did not
> make, that is stated. Where a decision deserves an ADR, §7 lists it.

---

## 0. Maintainer decisions — ANSWERED 2026-08-23

All six `[SA-DEFAULT]` calls were put to the maintainer and are decided. **Four confirmed the
default; two overturned it.** Nothing in this section is provisional any more.

| # | Question | Outcome | Status |
|---|---|---|---|
| D-1 | Does a holding record what it cost? | **Yes — `invested_paise` kept** (§2.3) | ✅ confirmed — already in the authored schema |
| D-2 | Where does the net-worth trend come from? | **Phase 2 ships `v_net_worth_history`** (§2.4) | ✅ confirmed — already authored |
| D-3 | Credit-card screens (Cards, Card detail, Card statement) | **Descoped.** D6's credit group covers the need (§5.1) | ✅ confirmed |
| D-4 | Custom fields | **IN SCOPE — descope rejected** (§5.1) | ⚠️ **overturned.** Needs an owning phase **and a design** — 004 records it as "never designed" |
| D-5 | Trash / recently-deleted surface | **Settings › Trash, Phase 0b** (§5.2) | ✅ confirmed |
| D-6 | Glance widget | **IN SCOPE — descope rejected** (§5.3) | ⚠️ **overturned.** Needs an owning phase. 004's FR-025/SC-010 masking assertions and T078 are now **valid**, not to be removed |

### Consequences of the two overturns

Both add scope no phase currently owns, and neither can be absorbed by an existing spec without a
decision about where it lands.

**D-4 · Custom fields.** The harder of the two: unlike the widget it has **no design at all** — no
screens, no data model, no place in the 61-screen inventory. It also cuts against a shipped
decision, because `holdings.sector` is a frozen append-only enum (BR-C3) precisely so the schema
stays predictable, and user-defined fields are the opposite pressure. Before it can be specced it
needs: which entities carry custom fields (holdings only? transactions? policies?), the storage
shape (`jsonb` column vs a key/value table), whether they are filterable and reportable, and what
happens to them on export and erasure. **Recommendation: its own spec-kit phase after Phase 7**,
not bolted onto an existing one.

**D-6 · Glance widget.** Cheaper — DESIGN-SYSTEM §11 already specifies its conventions (day/night
token mapping, compact money, `contentDescription` on the root, and its own value / masked /
signed-out / disabled states), and the surface registry already names a widget as a `QUICK_ADD`
producer. It needs a home and a data source. **Recommendation: Phase 6**, which already owns the
shell-level notification and intent surfaces and touches `QUICK_ADD` — reading its net-worth value
from Phase 2's `v_net_worth_history`, so the widget has something to show.

Neither recommendation is decided here; both need a call on phase placement, which is scheduling
rather than architecture.

Everything else in this document is `[SA]` — technical, determined by an existing ADR, and needed no
product input.

---

## 1. Blocking correctness defects — resolved

### 1.1 `[SA]` Valuation correction: add `finance.correct_valuation()`

**Problem.** FR-004 (001) requires hiding a wrong valuation by setting `deleted_at`. That is an
UPDATE. `finance.valuations` carries SELECT and INSERT policies only, and
`grant select, insert` — verified directly in
`supabase/schemas/finance/10_tables/valuations.sql`. Three documents assert the correction works
and the RLS that makes it impossible is cited as the guarantee.

**Decision.** Implement the security-definer RPC **ADR-0029 decision 4 already named** and assigned
to "Phase 2's SA step" — `finance.correct_valuation(p_valuation_id uuid, p_value_paise bigint,
p_as_of date, p_source text)`. It soft-deletes the target row and inserts the corrected one in a
single transaction, after asserting the caller owns the parent holding.

**Explicitly rejected: adding an UPDATE policy to `valuations`.** That would make the table
ordinarily mutable and destroy BR-C1's database-level append-only guarantee, which is the entire
reason the table has no UPDATE policy. The append-only property must remain enforced by the
database, not by client discipline (ADR-0029 decision 4, DAT-BR-007).

Artifact: `supabase/schemas/finance/30_functions/correct_valuation.sql` (written).

### 1.2 `[SA]` Holding + first valuation atomicity: add `finance.create_holding_with_value()`

**Problem.** FR-002/BR-C2 require the holding and its first valuation to be written atomically.
That is two PostgREST inserts over HTTP; `001/data-model.md:38-40` concedes it is "not expressible
as a single-table constraint" and delegates it to a repository that cannot be transactional across
two requests. A failed second insert leaves an orphan holding, and `holdings` has no client DELETE
policy to compensate with.

**Decision.** One security-definer RPC performing both inserts in one transaction. The repository
calls it for *creation only*; ordinary valuation appends stay a plain PostgREST insert against the
existing INSERT policy.

Artifact: `supabase/schemas/finance/30_functions/create_holding_with_value.sql` (written).

### 1.3 `[SA]` Every view is `security_invoker = on`

**Problem.** Postgres 15+ views execute as their owner and bypass RLS on the underlying table;
PostgREST exposes them. All 8 planned views across Phases 2–4 would have returned every user's rows
to every signed-in caller.

**Decision.** Every view in `supabase/schemas/finance/20_views/` **must** carry
`with (security_invoker = on)`. `supabase db diff` cannot express security-invoker views (ADR-0032's
own documented caveat list), so the clause is hand-verified in the generated migration, and each
phase's RLS test asserts a second user reads zero rows from every view it adds.

Phase 2's two views are written. Phases 3 and 4 author theirs at their own SA step under this rule
(002 T007, 003 T017/T018/T019 + T019a).

### 1.4 `[SA]` `liabilities_meta` lives in `finance`, authored declaratively

`public.liabilities_meta` (001 `data-model.md:44`) is wrong: ADR-0033 moved tracker tables to
`finance`, and a `public` table is unreachable under the `Accept-Profile: finance` header every
tracker call sends. It also had no declarative twin, which fails ADR-0032's PR equivalence guard.

Artifact: `supabase/schemas/finance/10_tables/liabilities_meta.sql` (written), with the ADR-0033
grants `db diff` cannot emit.

---

## 2. Data model — resolved

### 2.1 `[SA]` Frozen enum value lists

BR-C3 makes these append-only forever, so they are fixed here rather than left to a task:

| Enum | Values | Owner |
|---|---|---|
| `holdings.sector` | `BANK` `MUTUAL_FUND` `STOCKS` `PROPERTY` `GOLD` `EPF_PPF` `CASH` `VEHICLE` `CRYPTO` `OTHER` | 001 |
| `holdings.kind` | `ASSET` `LIABILITY` | shipped |
| `valuations.source` | `MANUAL` `STATEMENT` `IMPORT` `CORRECTION` | 001 |
| `liabilities_meta.liability_type` | `HOME_LOAN` `CAR_LOAN` `CREDIT_CARD` `BNPL` | 001 |
| `transactions.type` | `EXPENSE` `INCOME` `TRANSFER` | 002 |

`valuations.source` gains two values the audit surfaced as needed: `IMPORT` (the CSV path, if D-4's
sibling import ever ships) and **`CORRECTION`**, written by `correct_valuation()` so a corrected row
is distinguishable in C3's history list. Phase 5's `has_self_valued` partition is therefore
**`MANUAL` and `CORRECTION` are self-valued; `STATEMENT` and `IMPORT` are not** — previously
undefined and required by F4's footnote.

`categories.tax_section` (005) is **not** frozen here: its values are Indian tax sections the
maintainer selects per category, so the column is free TEXT with a documented convention, and
FR-037's "state on what basis" requirement is satisfied by displaying the user's own value.

### 2.2 `[SA]` Field corrections

- **`policies.nominee_share_pct` → `nominee_share_bps`** (`integer`, basis points). Whole percent
  cannot express three equal nominees at 33.33%, which is exactly the case the spec says it will
  "surface, not block". Every other proportional field in the repo is bps (`earmark_bps`,
  `rate_bps`, `share_bps`). → 003.
- **`policies` gains `name text not null`.** E7 groups by it, E8 titles on it, and Phase 6's search
  contract returns it as the result title. → 003.
- **Search contract uses `payee` and `note`**, not `description`/`counterparty`. The columns are
  002's; 006's contract is the document that is wrong. → 006.

### 2.3 `[SA-DEFAULT]` D-1 · Cost basis: add `invested_paise`

**Decision: add `invested_paise bigint null` to `finance.holdings`**, captured as an optional field
on C4.

**Why this default.** Three separate surfaces already assume it exists — C3's `INVESTED` and `GAIN`
stats, FR-006a's own wording ("with a known invested amount"), and Phase 5's
`report_investment_returns` — and dropping it removes a stat from the finalized design. Nullable
because a holding whose cost the user does not know is normal (inherited gold, an old EPF balance),
and C3 must render without the stat rather than show a wrong zero.

**What it does not do.** It does **not** resolve XIRR. XIRR needs a dated cashflow series, which
`invested_paise` (a single scalar) cannot provide, and 005's research R8 correctly records that no
holding↔transaction link exists to build one from. XIRR stays deferred to its own ADR; C3 ships
`(current − invested) / invested` labelled as **"Simple return"**, not as XIRR — the label matters,
because the design's own text says XIRR and the two are not comparable.

### 2.4 `[SA-DEFAULT]` D-2 · Net-worth history: Phase 2 ships `v_net_worth_history`

**Decision.** Phase 2 authors a third view, `finance.v_net_worth_history(user_id, as_of, net_paise,
assets_paise, liabilities_paise)`, deriving a month-end series from `valuations` via
"latest valuation ≤ date". Home's `▲/▼ %` delta compares the **latest value against the same
holding set 30 days prior**; C2's per-holding sparkline reads that holding's own valuation rows
directly.

**Why not defer to Phase 5.** Home (01) is Phase 2's own headline screen and the delta is drawn on
it; deferring means Phase 2 ships its flagship surface with a missing element and no recorded
deferral — which is what the audit found T017 silently doing. The derivation ("latest valuation ≤
date") is the same one 005's `report_balance_sheet(p_as_of)` uses, so this is not a competing
mechanism, and 005 may reuse the view rather than re-derive.

**Comparison window is a stated product choice**, not a fact: 30 days is chosen because the design's
copy says "this month". If the maintainer prefers calendar-month-to-date, only the view's window
changes.

### 2.5 `[SA]` Validation rules

State machine-checkable rules at the repository boundary; the database enforces what it can.

| Surface | Rules |
|---|---|
| C4 holding | `name` 1–120 chars, trimmed, non-empty; `sector` ∈ frozen list; `kind` ∈ frozen list; `invested_paise` ≥ 0 or null |
| C5 valuation | `value_paise` ≥ 0 (a holding is never negative — a *liability* holding's value is its outstanding, also ≥ 0); `as_of` **not in the future** (see below); `source` ∈ frozen list |
| Budget | `amount_paise` > 0; one per category per month |
| Goal | `target_paise` > 0; `target_date` may be past — E4 must show "target date passed", never a negative monthly requirement |
| Policy | `sum_assured_paise` > 0; `premium_paise` ≥ 0; `renews_on` required for an active policy; Σ`nominee_share_bps` ≤ 10000, with < 10000 surfaced as incomplete, never blocked |
| Retirement | `retire_age` 40–75; `life_expectancy` > `retire_age`, ≤ 110; `inflation_bps` 0–2500; return rates 0–3000 bps; `monthly_spend_paise` > 0 |

**Future-dated valuations are rejected** (`as_of <= current_date`), as a CHECK **and** at the
repository. This is not a nicety: `v_latest_valuation` orders `as_of DESC`, so a mistyped 2030 date
becomes permanently "latest" and — until `correct_valuation()` exists — could never be superseded.

### 2.6 `[SA]` Pagination

No spec stated any bound. Standing rule for every list surface:

- **Keyset pagination**, ordered by the surface's natural sort key, page size **50**, `Range` header
  on PostgREST. Never offset — it drifts under concurrent inserts.
- Applies to: C3 valuation history, C2 holdings, D1 ledger (within its month scope), D7 account
  activity, D8 category lists, E3 budget transactions, B2 notification centre, F-report line sets.
- **Search is the exception** and keeps its 25-per-kind cap, but the cap must be **stated in the UI**
  and `kind_total` labelled as the total, not the returned count — see §4.3.

`config.toml`'s `max_rows = 1000` is a backstop, not a design.

### 2.7 `[SA]` Write semantics: idempotency, retry, invalidation

Three related gaps, one answer.

- **Idempotency.** Every client-initiated create sends a `request_id uuid` generated at the point the
  user commits (not at send time, so a retry reuses it). `holdings`, `valuations`, `transactions`,
  `budgets`, `goals`, `policies` each carry `request_id uuid unique` — a retry after a timeout
  collides and returns the existing row instead of duplicating money.
- **Retry.** One automatic retry on a network timeout or 5xx, using the same `request_id`; a second
  failure surfaces `RetryErrorCard` and never auto-retries again. 401 keeps ADR-0029 decision 3's
  existing single-refresh behaviour, unchanged.
- **Invalidation.** SC-001 promises totals update without a manual refresh and DESIGN-SYSTEM §8
  forbids pull-to-refresh. **Decision: write-then-refetch.** A successful mutation invalidates the
  repository's cached flow for the affected views and re-reads; the screen shows the previous value
  with a subtle in-flight indicator until the re-read lands — it does **not** optimistically render
  a locally-computed total, because BR-C4 forbids deriving net worth from a client cache.

### 2.8 `[SA]` Concurrency

ADR-0014 removed client-side conflict resolution; the server is authoritative. The consequence,
unstated until now: **last write wins, and the loser is told.** A mutation that returns a row whose
`updated_at`/`created_at` differs from the one the screen was showing refreshes the screen and shows
a "Updated elsewhere" notice. No merge, no conflict UI, no HLC — those belong to the calculator
domain's offline-first sync (PLATFORM.md §5), not to the tracker.

`finance.valuations` needs none of this: append-only means concurrent appends both succeed and
`v_latest_valuation` resolves by `as_of`.

---

## 3. Backend / module architecture — resolved

### 3.1 `[SA]` Phase 2's build blockers

- `settings.gradle.kts` needs the **`projectDir` remap** — the module lives at
  `apps/finance/feature/home/networth` and its coordinate is `:apps:finance:feature:networth`.
- `:apps:finance:feature:networth` joins `coveredModules`/`_FEATURES`, or its coverage is invisible
  to the JaCoCo gate (ADR-0013) and to release notes.
- Schema is authored **declaratively first**, then `supabase db diff` generates the migration
  (ADR-0032 decision 4); grants and the `delete_my_data()` line are hand-appended because `db diff`
  cannot emit them (ADR-0033 decision 4).

### 3.2 `[SA]` Every feature module ships the same four things

Currently planned by 003 and 005 only. Binding for all:

1. Every route wrapped in `FeatureHost` — **every** route, not the tab root only (001 wraps 1 of 8,
   002 wraps 1 of 9 today).
2. `crashReporter.setModule("<flag key>")` in each ViewModel's `init`.
3. `performanceTracer.trace("<key>_<operation>")` on one primary operation.
4. A `featureError: StateFlow<Throwable?>` fed by a `CoroutineExceptionHandler`.

### 3.3 `[SA]` Settings contributions

004's model — "every later phase ships its module's settings entry with the module" — is binding and
currently honoured by 005 alone. **Every phase that ships a module ships its `SettingsContribution`**
per `../../../specs/004-settings/contracts/settings-contribution.md`. This is also what makes Phase
6's alert controls reachable: four of its five channels are owned by modules that plan no settings
entry, which is why its sub-phase 6f verifies controls nobody built.

### 3.4 `[SA]` Token enforcement becomes a real check

"Zero `MaterialTheme.colorScheme`/`.typography`, zero raw hex/dp/sp in screen files" is enforced by
nothing today: `config/detekt/detekt.yml` sets `MagicNumber: active: false`, no
`ForbiddenImport`/`ForbiddenMethodCall` rule exists, and `DependencyRulesTest`'s five rules are all
about module boundaries. 005 T169 claims detekt verifies NFR-5; it cannot.

**Decision.** Add an ArchUnit test in `:apps:finance:app`'s arch package asserting no
`androidx.compose.material3.MaterialTheme` reference in any `…feature…ui` screen file, plus a detekt
`ForbiddenImport` entry. It lands in **Phase 2** — the first phase to build tracker screens — so
every later phase inherits it rather than each re-deciding.

---

## 4. UI — resolved

### 4.1 `[SA]` The five unowned cross-cutting rules

Each becomes a per-phase obligation, listed here once so no phase re-derives it:

| Rule | Binding form |
|---|---|
| Money rendering | `MoneyText` is the only money renderer. **Compact** (`₹18.42L`) on cards, heroes, chips and widgets; **full** (`₹ 1,20,000.50`) in lists, sheets, detail rows, history and PDF. Never ellipsised — wrap or compact |
| Deltas and stat rows | `StatDeltaChip` for every ▲/▼ value, `ThreeUpStatRow` for every three-stat header. Both already exist in `:libs:core`; neither is named in any phase's tasks today |
| Strings | Every user-visible string in `strings.xml` from birth (§10). Missing as a task in 001 and 002 |
| Responsive | Every screen verified at phone, tablet ≥600dp and small <360dp via `calculateDhruvNextResponsiveTokens`. Unplanned in **all six** phases |
| Theme | Every screen verified in light **and** dark from the same tokens (N7) |

### 4.2 `[SA]` Missing components — who builds what

| Component | Decision |
|---|---|
| **`NxTabs`** (B8) | Built in **Phase 2** (`:libs:core`), because D8 (Phase 3) and F2–F4 (Phase 5) both need it and Phase 2 is the earliest phase that can carry it. It is **not** `SegmentedRow` — the design draws them as two distinct controls |
| **`EnumPickerGrid`** (B2) | **Not built.** C4's sector picker uses `SelectionSheet` (B9), already built. Ten enum values in a scrolling sheet is the same interaction with less new surface — recorded as a deliberate substitution rather than an accidental one |
| **Area chart** | Extend the existing **`TrendSparkline`** with a filled variant rather than adding a component. Consumers: 01, C2, C3, D7, E5, E9 |
| **`PaceRing`** | One implementation, in `libs/core/.../ui/components/charts/`. 001 T006 owns it; 003 T043 consumes it and must not re-create it |
| **`Chip` removable** | Extend `Chip`; **do not** add 002's parallel `InputChip` (DESIGN-SYSTEM §5.3's own rule) |
| B2's `NxCheckbox`/`NxRadio`/`QwertyKeypad`, B7's `Spinner` | Not needed by any specced screen. Left unbuilt deliberately |
| **`PinEntry`** (B2) | Built in **Phase 0b** — the app-lock flow needs it |

### 4.3 `[SA]` Screen-level fidelity fixes

Phase 2 (1 of 8 screens currently matches the design): C3 gains `LAST VALUED <date>` + sector in its
header and uses the existing `PeriodChipRow` for 3M/6M/1Y/All; C6's rows gain rate and EMI; 01 gains
its one-line state; C1's legend gains the enum tag; C7's prepay projection carries the §10
derived-output label. Phase 3: D8 uses `NxTabs`; D2 gains the camera affordance; D7 gains
*Add transaction*; D9's rows distinguish monthly/yearly and auto-debit/variable; D3 gains top-bar
delete. Phase 4: E1's module cards carry live figures. Phase 5: F1 gains its export affordance.
`PieChart` is dropped from 001 T006 — no screen in any phase consumes it.

**Search count copy (006).** `kind_total` is the true total and the chip shows it; when it exceeds
the returned page the results list ends with "Showing first 25 of 41 — refine your search". This
resolves FR-003/SC-001 against the contract without paging `search_all`.

### 4.4 `[SA]` Accessibility is a gate

001, 002 and 006 have zero accessibility tasks; 006 claims NFR-6 in scope and plans none. Binding
for every phase, per DESIGN-SYSTEM §9:

`contentDescription` on every icon-only action and on **every chart, ring and sparkline**, at the
design's stated verbosity ("Net worth, ₹18.42 lakh, up 6.4 percent this month") · touch targets
≥48dp, list and settings rows ≥56dp · contrast ≥4.5:1 in **both** themes · no colour-only meaning
(every delta and status carries a glyph) · dynamic-type safe, money wraps or compacts and never
ellipsises · TalkBack order follows visual hierarchy.

### 4.5 `[SA]` Motion

Zero coverage in all six phases. Binding: standard easing `cubic-bezier(.16, 1, .3, 1)`; **charts
animate in once, not on every recomposition** (the concrete risk in 001/003/005, all of which build
animating charts); Material default springs elsewhere; splash ≤2.5s. No bespoke choreography.

### 4.6 `[SA]` Notification conventions (006)

Four of §11's six are unowned. Binding: sentence case · ≤1 line collapsed, ≤2 expanded,
`BigTextStyle` only for genuinely long-form content · never a policy or account number · **never an
account name and an amount in the same line under privacy mode** — that one is a privacy rule, not
styling, and needs a test.

---

## 5. Scope decisions

### 5.1 `[DECIDED 2026-08-23]` D-3 descoped · D-4 kept in scope

**Credit-card screens** (Cards, Card detail, Card statement) are **removed from the navigation
contract**. D6's `CREDIT — OWED, NOT HELD` group already shows negative balance, limit, due date and
utilisation — the information those screens would carry — and a statement view needs a
statement-cycle model no phase has. **Action:** amend the functional spec §4 navigation contract and
close open items §8.2/§8.2a.

**Custom fields are IN SCOPE.** The proposed descope was **rejected** by the maintainer. This is new
product scope with no design behind it: 004 records custom fields as "never designed", and they
appear nowhere in the 61-screen inventory. It also pushes against BR-C3, which freezes
`holdings.sector` as an append-only enum specifically to keep the schema predictable.

Before a spec can be written, four things need answering: **which entities** carry custom fields
(holdings only, or transactions and policies too), the **storage shape** (`jsonb` column vs a
key/value table), whether they are **filterable and reportable** (which decides whether `jsonb` is
viable at all), and their behaviour on **export and DPDP erasure**. Open item §8.3 therefore stays
**open**, reclassified from "descoped" to "accepted, undesigned". Recommended placement: its own
spec-kit phase after Phase 7.

### 5.2 `[SA-DEFAULT]` D-5 · Trash ships in Phase 0b

DESIGN-SYSTEM §8 mandates soft-delete plus **a recoverable location**, and 001/002/003 all
soft-delete. Undo (5s snackbar) is the immediate path; Trash is the durable one, and without it
"soft delete" is just invisible data.

**Decision.** A single **Settings › Trash** surface listing every soft-deleted entity across all
modules, restore-or-purge, auto-purging at 30 days. It belongs to Phase 0b because that phase owns
the Settings control plane and runs before Phase 2, so every later phase's deletes have somewhere to
go on day one.

**Undo** (`UndoSnackbarHost`, already built) is implemented once in Phase 2 as the shared pattern and
reused by 003/005/006 — not re-invented per phase.

### 5.3 `[DECIDED 2026-08-23]` D-6 · Glance widget kept in scope

The proposed descope was **rejected** by the maintainer. A widget ships.

This makes assertions that were previously dangling **correct**: 004's FR-025 and SC-010 bind
hide-amounts masking to "any screen, **widget** or notification", and its T078 verifies widget
masking. Those stay as written — the earlier recommendation to strip them is withdrawn. The
`QUICK_ADD` intent keeps its widget producer in the surface registry.

What it still needs: an **owning phase** and a **data source**. DESIGN-SYSTEM §11 already fixes the
conventions — day/night token mapping, compact money only, `contentDescription` on the root, and its
own value / masked / signed-out / disabled states — so this is placement work, not design work.
Recommended placement: **Phase 6**, which already owns the shell-level notification and intent
surfaces and touches `QUICK_ADD`, reading its value from Phase 2's `v_net_worth_history`.

### 5.4 `[SA]` Phase 7 must exist before Phase 3 ships

Two shipping phases defer into G2's review queue (Phase 3's D9 recurring, Phase 6's
transactions-to-review) and `AUT-*` QA rows are already written against a directory that does not
exist. **Phase 7 gets its spec-kit directory before Phase 3's checkpoint**, not after Phase 6.
Correction to fold in: `suggestions` is owned by Phase 3, not Phase 7 — the implementation plan's
Phase 7 step 1 would re-create an existing table.

**Profile** (D-5's sibling): folded into Settings › Account as ADR-0027's shell intended;
`ProfileScreen.kt`'s stub route is retired in Phase 0b.

**Import CSV**: the A4 CTA stays a disabled stub with copy naming it as coming later. It is the one
descope the design explicitly draws as a day-one escape hatch, so it is **deferred, not cancelled** —
Phase 7 owns it alongside the other ingestion paths.

### 5.5 `[SA]` Unowned work surfaced by Phase 0b shipping (added 2026-08-29)

Phase 0b (004-settings) is **shipped**. Building it real, and then auditing it against its own FRs,
surfaced four items 0b **cannot** own — each is a *consumer* that 0b's control plane now has a
control for, and each currently has no phase. Recorded here rather than only in 004's Deferred
table, because the engineer who needs them is the one starting Phase 6 or the Ask Dhruv work, and
they will never open a shipped phase's spec. 004's Deferred table cross-references this section.

| # | Unowned item | What 0b shipped | Why 0b cannot own it | Recommended owner |
|---|---|---|---|---|
| 1 | **`daily_rates` delivery pipeline** | The real Android notification channel, the `alert_daily_rates` toggle, a delivery-time `Choice`, and `NotificationChannelRegistry` — the first entry, which makes `SET-BR-006`'s 1:1 channel↔control rule non-vacuous | 0b is the control plane; it owns *the control*, never the alert-posting job. **Nothing posts to this channel today.** Phase 6 explicitly disclaims it — its scope note reads "`daily_rates`/`app_updates` stay with the currency and app-details modules", and neither module plans that work. This is the exact inverse of the gap §3.3 already names (there, controls nobody built; here, a control with no producer) | **Phase 6**, alongside its five alert arms — it builds the only `androidx.work` scheduling in the app, and a second scheduler for one channel is waste. Otherwise it needs a stated owner before 0b's toggle stops being honest |
| 2 | **BYO AI-key consumption** (ADR-0002's override) | FR-038's storage half: encrypted in `secure_settings`, fixed-token masking that cannot leak length or characters, one-action removal, `SettingsRow.SecretText` | FR-038 requires the key be *stored* safely — it does not require it be *used*, and using it is an AI-path change in `:apps:finance:data`, not a settings one. `GeminiRepository` is a Koin singleton built once from `BuildConfig.GEMINI_API_KEY`, and `currentSnapshot()` deliberately skips the encrypted read, so honouring a user key needs a key-provider indirection | **The Ask Dhruv work** — ADR-0024 decision 4 already says this plumbing "is tracked and lands alongside it". No phase in §7's table owns Ask Dhruv; that is the real gap. Until then the row states it is stored-not-used (FR-043), the same way app lock shipped preference-only in 0b.1 |
| 3 | **Hiding launcher entry points** for a module the user turned off | FR-032's enforcement: `SettingsContribution.optional`, the on/off control for the three optional modules, and route gating so a turned-off module renders `FeatureDisabledCard` | Content removal is done and is the load-bearing half. The remaining half — hiding the Calc-tab converter tiles and the Ask pill — lives in `CalculatorScreen` (a feature module) and the shell's pill logic, neither of which 0b owns | **Whichever phase next revisits those surfaces.** Low urgency by design: a still-present tile leading to `FeatureDisabledCard` is *exactly* how a flag-disabled module already behaves, so this is a pre-existing consistency gap in that pattern, not one 0b introduced |
| 4 | **`font_family` has no Settings row** | 0b.5's `SET-BR-009`/SC-005 orphan-preference audit found it and deleted the 9 keys that were genuinely dead | This one is **not** dead: `DhruvTheme` actively branches on `DhruvFont`, so the preference is live and simply unreachable — a real FR-003 violation ("every persisted key has exactly one row"). 0b did not invent a font picker to fix it because DESIGN-SYSTEM §2 specifies a fixed three-family system and lists no font-picker in its component or screen inventory; adding one is unspecified UI scope, and deleting font support is a design decision | **A DESIGN-SYSTEM decision first** (add a picker, or drop `DhruvFont`'s non-default variants), then whichever phase implements the answer. Recorded in 004's `quickstart.md` §7 with the same reasoning |

Two of these (1 and 2) are the same shape and worth naming as a pattern: **0b's control plane can
ship an honest control faster than the feature can ship its consumer.** FR-043's preference-only
labelling is the designed release valve and both use it — but a control that stays labelled
"not in use yet" indefinitely eventually reads as broken, so each needs an owner, not just a label.

---

## 6. Readiness by discipline

| Discipline | Ready to start | Gate |
|---|---|---|
| **DB** | **Yes, Phase 2** — `liabilities_meta`, both views (`security_invoker`), `v_net_worth_history`, `correct_valuation()`, `create_holding_with_value()` are authored declaratively in this change. Run `supabase db diff -f networth_phase2`, review, hand-append grants + the `delete_my_data()` line, then `gen_schema_docs.py equiv` and `docs --check` | Phases 3–6 author their own at their SA step, under §1.3's view rule and §2.5's validation rules |
| **Backend** | **Yes, Phase 2** — repository contracts are determined by §2.7 (idempotency/retry/invalidation), §2.8 (concurrency), §2.6 (keyset pagination, page 50) and §2.5 | Blocked only on D-1 if the maintainer reverses `invested_paise` |
| **UI** | **Yes, Phase 2** — §4.1's five cross-cutting rules, §4.2's component decisions, §4.3's fidelity list, §4.4 accessibility, §4.5 motion | `NxTabs` and the `TrendSparkline` filled variant are Phase 2 `:libs:core` work and precede screen work, per the plan's "design-system work precedes every screen" |

**Phase 2 remains the gate for everything else.** Phases 3–6 all depend on it, and its two
correctness defects (§1.1, §1.2) were the reason nothing downstream was safely startable.

---

## 7. ADRs this work requires

Per `platform/DECISIONS.md`'s append-only rule, these are written **when implemented**, not now, and
must re-check the highest existing number at that moment — the register has had three numbering
collisions caused by exactly this gap. Highest written entry today is **ADR-0034**.

1. **Tracker write semantics** — idempotency keys, single-retry, write-then-refetch invalidation,
   last-write-wins-and-tell (§2.7, §2.8). Genuinely new architecture; ADR-0014 only said "no
   client-side conflict resolution".
2. **Investment returns / XIRR** — still open, still blocking 005's sub-phase 5f. §2.3 narrows it:
   `invested_paise` gives a simple return, and XIRR additionally needs a holding↔transaction link
   that no phase models.
3. **Design-v1 scope reduction** — D-3/D-4/D-6 remove drawn surfaces from the functional spec
   (§5.1, §5.3). Amending a finalized design deserves a record.

Items §1.1, §1.2, §1.3, §1.4, §2.1, §2.2, §2.5, §2.6, §3.x and §4.x need **no** ADR — each
implements an existing decision (ADR-0029, ADR-0032, ADR-0033, DESIGN-SYSTEM) that was simply not
carried into the phase specs.