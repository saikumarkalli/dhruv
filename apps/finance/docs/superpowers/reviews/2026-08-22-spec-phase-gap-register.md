# Finance design-v1 — spec-kit phase gap register

> **Status: PARTIAL (2026-08-22).** Multi-agent audit of every generated spec-kit phase against the
> binding design (`../specs/2026-08-08-design-v1-final-functional-spec.md`), the implementation plan,
> the surface registries and `platform/DESIGN-SYSTEM.md`.
>
> **All slices complete**: screen/route coverage · field-level data model · business rules +
> screen states + entity lifecycle + FR→task traceability · technical architecture (schema/RLS/DPDP,
> flags, Gradle modules, components, phase DAG, web parity).
>
> **Read §0 first — it is a cross-user data-leak defect present in three phases.**
>
> **Folded into the specs on 2026-08-22.** Every finding below now has a home in the owning phase's
> `tasks.md` as a trailing **"Gap remediation"** block (001 T045–T061 · 002 T084–T095 ·
> 003 T135–T148 · 004 T119–T126 · 005 T176–T183 · 006 T122–T131). The security and build blockers
> in §0/§0b were corrected **in place** — in T001/T004/T004a/T004b (001), T007 (002) and
> T017/T019a (003) — because a developer running the originals would ship an RLS bypass and a build
> failure, and a cleanup section at the end of the file would not stop that. Items owned by no phase
> are recorded in the implementation plan §7 tracking table's Phase 7 row and its audit-gate note.
> This register stays the evidence; the tasks are the work.
>
> **Resolved 2026-08-23.** The open *questions* in this register are answered in
> `../specs/2026-08-23-phase-readiness-architecture-decisions.md`, and Phase 2's DB layer is
> authored against those decisions (declarative files + migration + the two RPCs). What remains
> open is execution and verification, not design — see
> `../../../specs/001-net-worth-tracker/data-model.md` § "DB readiness" and 001 T078–T084.
> **This register is now historical evidence**; the specs' own task lists are the live work.
>
> **Round 2 (same date)** ran three further agents — UI/UX fidelity to the finalized design,
> design-system enforcement, and a requirements re-review aimed deliberately at what round 1 did not
> look at. Findings in §9–§11 below, folded into a second block per phase (001 T062–T077 ·
> 002 T096–T108 · 003 T149–T156 · 004 T127–T132 · 005 T184–T189 · 006 T132–T137).
>
> Every row below is cited to `file:line`. This is a findings register, not a decision record —
> nothing here is resolved until an ADR, a spec edit, or an explicit deferral-with-reason lands.

Phase ↔ spec-kit map (from the implementation plan §7 tracking table):

| Phase | Screens | Spec-kit dir |
|---|---|---|
| 0 — Shell foundation | — | none (pre-spec-kit, shipped) |
| 1 — Identity & consent | 00, A2, A3, A4 | none (pre-spec-kit, shipped) |
| 0b — Settings control plane | Settings tree | `apps/finance/specs/004-settings/` |
| 2 — Net worth + real Home | 01, C1–C7 | `apps/finance/specs/001-net-worth-tracker/` |
| 3 — Money tab | D1–D9 | `apps/finance/specs/002-money-tab/` |
| 4 — Plan live modules | E1–E9 | `apps/finance/specs/003-plan-live-modules/` |
| 5 — Insights | F1–F5 | `apps/finance/specs/005-insights/` |
| 6 — Search & notifications | B2, B3 | `apps/finance/specs/006-search-notifications/` |
| 7 — Automation | G1–G3 | **not created** |

---

## 0. CRITICAL — RLS bypass: no planned view sets `security_invoker = on`

**Every one of the 8 planned views would return every user's rows to every signed-in user.**

Postgres 15+ views execute as their **owner**, not the caller, so RLS on the underlying table is
bypassed. PostgREST exposes these views over `/rest/v1/`. Grepping `security_invoker` across
`001/`, `002/` and `003/` returns nothing.

Affected: `v_latest_valuation`, `v_net_worth_by_sector` (Phase 2) · `v_account_balances`,
`v_month_summary`, `v_category_spend` (Phase 3) · `v_budget_status`, `v_goal_progress`,
`v_annual_income` (Phase 4).

Compounding detail: **005 argued this exact point for reporting *functions*** and chose
security-definer with an explicit `auth.uid()` filter — nobody carried the reasoning back to the
views. ADR-0032's own caveat list already names security-invoker views as *not* expressible through
`db diff`, so this must be hand-written into each migration.

`001/tasks.md:47`; `002/tasks.md:63`; `003/tasks.md:69-71`; `005/plan.md:138-146`

Nothing downstream is safely implementable until this is fixed in all three phases.

---

## 0b. CRITICAL — Phase 2 (001) will not build or merge as written

Three independent blockers, all in `001/tasks.md`, all correctly handled by 002/003/005:

| Blocker | Detail | Evidence |
|---|---|---|
| **Missing `projectDir` remap** | T001 says "register in `settings.gradle.kts`" only. Module lives at `apps/finance/feature/home/networth` but its coordinate is `:apps:finance:feature:networth` — without the remap Gradle resolves `apps/finance/feature/networth` and **configuration fails** | `001/tasks.md:34` vs `002/tasks.md:47`, `003/tasks.md:43`, `settings.gradle.kts:59-69` |
| **Hand-written migration, no declarative twin, no guards** | Touches no `supabase/schemas/finance/**` file, adds no `db diff` step, runs neither `gen_schema_docs.py equiv` nor `docs --check`. **ADR-0032's PR equivalence guard fails on merge.** 002/003/005/006 all carry these steps | `001/tasks.md:47`; cf. `002/tasks.md:66`, `005/tasks.md:81` |
| **No grants** | No `grant usage` / `grant select,insert,update` for `liabilities_meta` or the two views. ADR-0033 decision 4 requires them for custom-schema objects and `db diff` cannot emit them. 002 (T010) and 003 (T022) both do it | `001/tasks.md:47-55` |

Also in 001: `:apps:finance:feature:networth` is never added to `coveredModules`/`_FEATURES`, so its
coverage is invisible to the JaCoCo gate and release notes. **Three consecutive later plans report
this and each explicitly declines to fix it.** `002/plan.md:101-107`; `003/plan.md:130-136`;
`005/plan.md:148-155`

Every phase declared "ready for `/speckit-implement`" (002, 003, 005a–e, 006) depends on 001.

---

## 1. Screens and routes with no owning spec at all

Nine designed surfaces are in the binding navigation contract (functional spec §4) and owned by no
spec-kit phase. Three of them already have **written QA rows** pointing at a directory the plan
records as "not yet created".

| Surface | Evidence | Note |
|---|---|---|
| **G1 Automation hub** | plan `:300`; registry `:40`; QA catalog `:184-192` | QA rows `AUT-*` already written |
| **G2 Review queue** | `002/spec.md:378-381`; `006/spec.md:422-423` | **Two shipping phases defer work into it.** Phase 3's D9 recurring review and Phase 6's transactions-to-review both point here |
| **G3 AA consent** | functional spec `:162`, `:112-113` | Design-declared full-screen modal + dark hero; presentation class asserted in no routes contract |
| **Credit cards · Card detail · Card statement** | functional spec `:146-147`; `002/spec.md:372-377` | In the nav contract under Money; 002 explicitly declines them; no later spec adopts them. Open item §8.2/§8.2a still open |
| **Custom fields** | functional spec `:143`, `:386`; `004/spec.md:444-447` | 004 marks it "never designed", out of scope. Open item §8.3 unresolved |
| **Import a CSV / import mapper** | functional spec `:181`, `:295`, `:379`; `EmptyStartScreen.kt:30`; registry `:153` | A4 ships the CTA as a **deliberately disabled stub**; flow F-1 routes it to a mapper with no design and no spec. Registry parks "Import" under Phase 7 |
| **Trash / Recently deleted** | functional spec `:144`; registry `:152-153` | DESIGN-SYSTEM §8 mandates soft-delete + "a recoverable location". String appears in **zero** phase specs. Every soft-deleted holding, transaction, account, category, goal and policy has no recovery surface |
| **Profile** | functional spec `:144`, `:66`; registry `:42`; `DetailRoute.kt:28` | Shipped stub the functional spec says must fold into the Settings sub-tree. **004 never mentions Profile once** — absorb / keep / delete is undecided |
| **C7 *Record payment* + recent-payments list** | functional spec `:204`; `001/data-model.md:44-58`; `001/tasks.md:188-192` | No payments table in any phase; 002 never links a transaction to a liability |

---

## 2. CRITICAL — a value the design displays has no data source anywhere

| Screen / field | Finding | Evidence |
|---|---|---|
| **C3 `INVESTED`, `GAIN`** | No cost-basis column exists. `finance.holdings` = id/user_id/name/kind/sector/notes/created_at/deleted_at. C4's form has no invested field. FR-006a says "for an asset holding **with a known invested amount**" but never says where that amount is stored or captured. T023 builds the stat regardless | `001/data-model.md:9-32`; `001/spec.md:179-181`; `001/tasks.md:131-133`; `supabase/schemas/finance/10_tables/holdings.sql:8-17` |
| **01 Home + C1 — net-worth ▲/▼ % delta and area sparkline** | FR-010 mandates the trend and a percentage change. Phase 2 defines only `v_latest_valuation` and `v_net_worth_by_sector`, both current-state. **No historical net-worth series view exists, and "delta vs when" is undefined.** Phase 5's `report_balance_sheet(p_as_of)` would supply it — three phases later | functional spec `:190`,`:198`; `001/spec.md:139`,`:188`; `001/data-model.md:71-81` |
| **C2 per-holding sparkline, last-updated, % change, per-sector subtotal** | Design requires a sparkline and % change per holding **in a list**; only a one-row-per-holding current-state view exists. T017 silently reduces C2 to "sector-grouped list, filter chips, FAB add". **No deferral stated anywhere** | functional spec `:199`; `001/tasks.md:98-99` |
| **C7 recent payments with principal/interest split** | No payments/instalments table in any phase. `liabilities_meta` holds current terms only | functional spec `:204`; `001/data-model.md:44-64` |
| **E9 `PROJECTED CORPUS AT 60`** | `assumptions` jsonb holds six fields, all describing the *target* (retire age, monthly spend, inflation, pre/post return, life expectancy). **Nothing names current corpus or a contribution stream.** `RetirementProjectionEngine` (T106) has no defined input for the figure it exists to produce | `003/data-model.md:155-174`; `003/research.md:89-116`; `003/tasks.md:273` |
| **`finance.policies` has no `name` column** | E7 groups policies for display, E8 is "Policy detail", and Phase 6's search contract returns **policy name as the result title** — its data-model even lists `policies (name, …)` as a column it reads | `003/data-model.md:96-118`; `006/contracts/search-rpc.md:44`; `006/data-model.md:139` |

---

## 3. HIGH — specified but ambiguous, contradicted, or mis-phased

### Data / schema

| Item | Finding | Evidence |
|---|---|---|
| `liabilities_meta` schema placement | **Three-way disagreement, flagged twice, resolved zero times.** 001 declares `public.liabilities_meta`; 002 and 003 each raise a "correction to check when this lands"; 005 silently assumes `finance.liabilities_meta`. Under ADR-0033 a `public` table is unreachable through the `Accept-Profile: finance` header 002 mandates. Every downstream FK is conditional on an unmade decision | `001/data-model.md:44`; `002/data-model.md:17-21`; `003/data-model.md:23-26`; `005/data-model.md:17` |
| `liabilities_meta` authorship path | T004 hand-writes `0002_networth_phase2.sql` with **no matching `supabase/schemas/finance/10_tables/liabilities_meta.sql`**. ADR-0032 decision 4 requires declarative-first; its PR equivalence guard fails on any object in one and not the other | `001/tasks.md:47-52`; `supabase/schemas/finance/10_tables/` (2 files only) |
| `sector` 10-value enum | The list (`BANK·MUTUAL_FUND·STOCKS·PROPERTY·GOLD·EPF_PPF·CASH·VEHICLE·CRYPTO·OTHER`) exists **only in functional-spec prose**. 001 says "enum-validated at the Kotlin repository boundary" and "fixed list" without listing values; T011 tests rejection against a list defined nowhere in the owning spec. `liability_type` **is** frozen in the same file — inconsistent | `001/data-model.md:16`,`:50`; `001/spec.md:166`; `001/tasks.md:80` |
| `valuations.source` values | Documented as "**e.g.** `MANUAL`, `STATEMENT`" — never frozen, violating BR-C3 (TEXT enums append-only). C5 ships a "source picker" with no defined options. Phase 5's `has_self_valued` needs the exact partition of that set | `001/data-model.md:30`; `001/tasks.md:161`; `005/data-model.md:111` |
| B3 search ↔ transactions column names | Search contract returns `description` / `counterparty` for a `TRANSACTION` row; `finance.transactions` has **`payee`** and **`note`**. 006's own read table repeats the wrong names | `006/contracts/search-rpc.md:42`; `006/data-model.md:136`; `002/data-model.md:78-79` |
| C7 amortisation donut | Derivable from rate/EMI/tenure/paid_months, but **original principal is not stored** and the derivation is stated nowhere. T028 asserts "split sums to total obligation" against an undefined computation. C6's "outstanding, not original" rule means outstanding comes from the latest valuation — a different quantity than the amortisation schedule implies | functional spec `:203-204`; `001/data-model.md:47-57`; `001/tasks.md:178-179` |
| `nominee_share_pct` typed `integer` whole percent | Three equal nominees (33.33%) cannot be expressed and cannot sum to 100 — the exact condition the spec says it will "surface, not block". Every other proportional field in the repo uses basis points (`earmark_bps`, `rate_bps`, `share_bps`). Precision inconsistency on a legally-consequential field | `003/data-model.md:114` |
| E5 projection chart + *Add a contribution* | Goal **progress** is fully specified (`v_goal_progress`); the **projection** is not — no growth assumption, no contribution rate, no input modelled. "Add a contribution" appears in no FR and no task, and has no write target given BR-E1's "nothing is moved" | functional spec `:247`; `003/spec.md:347`; `003/tasks.md:186` |
| F5 investment returns (XIRR) | `report_investment_returns` signature "deliberately unspecified", gated on an unwritten ADR. R8 notes the underlying data may not support any candidate answer — "Phase 3's `transactions.goal_id` exists but **there is no holding link**". The cashflow set for XIRR has no schema path today, not merely an undecided formula | `005/data-model.md:133-137`; `005/research.md:188-206` |
| `003/data-model.md` cites `liabilities_meta (… outstanding balance)` | Not a column on that table — outstanding is the liability holding's latest valuation. `DebtPayoffEngine` is specced against a column that does not exist | `003/data-model.md:19` |

### Navigation

| Item | Finding | Evidence |
|---|---|---|
| **`NavTarget` is the largest cross-cutting defect** | Today only `SelectTab` and `OpenPlanTool` exist. Three navigations the design requires have **no declarable case** under the current contract | `libs/core/src/main/kotlin/com/dhruv/core/navigation/NavTarget.kt:20-34` |
| — Home → Currency quick action | Design's four quick actions are Loan EMI · SIP · Currency · GST; three map to `OpenPlanTool`, but `NavTarget.kt` states Currency/Unit/Date/Time/Settings/Ask are **deliberately not** NavTargets. T036 and QA row HOM-UI-002 both require all four "via `NavTarget`". No spec adds the case or an alternative | functional spec `:190`; `001/tasks.md:222-225`; QA catalog `:241` |
| — E3 → filtered ledger | 003 says the jump is `SelectTab(MONEY)` "+ a category filter argument", but `SelectTab` carries only a `TabKey` and 003's NavTarget-additions table adds no filtered-ledger case | `003/contracts/routes.md:83` vs `:49-54` |
| — Phase 2 has no NavTarget section at all | 001's routes contract is the only phase contract lacking one, yet 003 and 006 both record `OpenHolding`/`OpenLiability` as "added by Phase 2". The registry's own sealed-case-plus-registry-row pairing rule is unstated for Phase 2; the cases exist only inside a task line | `001/contracts/routes.md:9-37`; `001/tasks.md:134`; `003/contracts/routes.md:84-85`; `006/contracts/routes.md:60-61` |
| `OpenTransaction` (→ D4) | Plan §4.1 lists it as required. 002 deliberately declines to add it; 006 adds it only conditionally ("if Phase 3 has not added it"). **No phase unconditionally owns it** | plan `:135`; `002/contracts/routes.md:44-47`; `006/contracts/routes.md:70-75` |
| `OpenReport(ReportKind)` | Plan §4.1's declared case does not exist in 005, which declares `OpenStatement` + `OpenReports` + `OpenBalanceSheet` instead. The plan's contract was never reconciled to the spec that supersedes it | plan `:140` vs `005/contracts/routes.md:47-53` |

### Underspecified screens

| Screen | Finding | Evidence |
|---|---|---|
| **C2 Assets** | Design requires per-holding sparkline, last-updated date, filter chips and **search + filter in the top bar**. `001/spec.md` contains zero occurrences of "search", "filter", "sparkline" or "sector". 006 later assumes C2's asset search already exists | functional spec `:199`; `006/contracts/routes.md:33` |
| **C7 Liability detail** | FR-008/FR-009 cover only outstanding/rate/payment/progress and prepay. Collateral, linked account and payment history are in the data model and tasks but have **no requirement** | `001/spec.md:184-189` |
| **C3 "Link to goal"** | T023 builds the action, but goals/`goal_links` do not exist until Phase 4, and 003 specifies linking only from the E5 side (FR-023). Unlike the credit-card-bill and budget-impact deferrals, this forward dependency is **flagged nowhere** | `001/tasks.md:231-234`; `003/spec.md:348-349` |
| **C4 Add/edit holding** | Design requires current value, **as-of date** and **optional notes**. FR-001 covers only category + current value; date and notes appear only in data-model and tasks, in no requirement | `001/spec.md:165-167` |
| **B2 Notifications** | Design draws five row types; 006 ships five *different* ones. **Price/rate alert** is explicitly out of scope and **recurring-posted digest** is deferred to Phase 7. QA row SRC-FLOW-003 still tests a rate-alert deep link | functional spec `:191`; `006/spec.md:339-343`,`:422-425`; QA catalog `:204` |
| **Settings detail routes** | 004 adds `SettingsAccount`/`SettingsApp`/`SettingsModule` but is the only phase with **no `contracts/routes.md`**, and its tasks state no task rewrites the surface registry. Three routes ship with no registry row and no route contract | `004/tasks.md:20-22`,`:99`,`:345` |

---

## 4. MED — registry / contract inconsistency

| Item | Finding | Evidence |
|---|---|---|
| Dark-hero surfaces | Functional spec D-2 and plan §3.1 name C3·D2·D7·E5·E9·F3·G3 as theme-invariant dark hero. 003 marks E5/E9 and 005 marks F3; **001 and 002 never mention dark hero or `DhruvBrand` at all** — so C3, D2 and D7 will be built on the flipping palette | functional spec `:112-113`; plan `:79`; grep of `001-*`/`002-*` = zero hits |
| Registry §1 C-row presentation | Says `push` for all seven C-screens, contradicting 001's C4 = modal and C5 = sheet and the functional spec's own presentation-class list. The D1–D9 row does it correctly — inconsistency inside one table | registry `:28` vs `:31`; functional spec `:160-162` |
| Registry §1 is five routes behind | `D9-review` (002), `E5-link` (003), `F5-export`, `F5-range`, `F4-date` (005) are declared in phase contracts with no registry row. Each spec defers the row to implementation time | `002/contracts/routes.md:20`; `003/contracts/routes.md:17`; `005/contracts/routes.md:16-18` |
| Registry §1 missing shipped surfaces | Splash (00) and the app-switcher sheet are shipped code and named presentation classes, with no registry row | registry `:25-43` |
| Registry §2/§3 cite deleted docs | "Source phase" values are retired roadmap codes (`R4`, `R5b`, `R6`, `R7`, `R8`, `P4`) pointing at documents deleted 2026-08-15. 004 acknowledges them as unmapped and defers; no spec has mapped them | registry `:61-70`,`:85-92`; `004/spec.md:522-524` |
| Intent registry orphans | `QUICK_ADD` (launcher shortcut + widget), `REVIEW_INBOX` (→ G2) and `OPEN_UPCOMING` have **no owning spec** — grep across all six spec dirs returns zero hits | registry `:85-92`; `002/contracts/routes.md:44-47` |
| A3 consent presentation class | Functional spec §4 lists consent among **bottom sheets**; registry §1 says A2–A4 are "bare, full-frame"; DESIGN-SYSTEM §6 puts scoped consent in **full-screen modals**. Three documents, three classes, one screen (shipped as a full screen) | functional spec `:160-162`; registry `:43` |
| Ask (Gemini) owner tab | Nav contract draws it under Insights; registry assigns it to the shell under `assistant`; 005 explicitly excludes it and leaves open item §8.4 (which tab owns its back stack) unresolved | functional spec `:152`,`:387-388`; registry `:42`; `005/spec.md:545-547` |
| D5 "Save as view" | A Key Entity in 002's spec with **no row in its data-model**; storage is decided only inside a task line (encrypted DataStore, not a table). A data-model reader concludes the entity is unowned | `002/spec.md:332`; `002/tasks.md:129` |
| D4 budget impact | Deferred with a stated reason (budgets are Phase 4), but **Phase 4 carries no reciprocal task to add it back** — the follow-up is named in Phase 3 and unclaimed in Phase 4 | `002/spec.md:368-371` |
| `categories.tax_section` values | Declared "append-only TEXT constants, same convention as `sector`" — and, like `sector`, never enumerated. F5's tax summary groups by it; FR-037 requires stating on what basis a category is tax-relevant | `005/data-model.md:36` |
| Glance widget | DESIGN-SYSTEM §11 defines widget conventions and the registry names a widget as a `QUICK_ADD` producer; **no phase owns a widget** | registry `:86` |

---

## 4b. Entity lifecycle — create / edit / delete / undo

Two findings here are CRITICAL and cut across every phase.

**CRITICAL — a holding cannot be edited or deleted.** No FR, no task, no QA row, no RLS DELETE
policy. A mistakenly-entered holding can only be removed by full-account erasure. The design titles
C4 "Add / **edit** holding"; 001 has no edit FR either — `T018 AddEditHoldingScreen` builds the UI
with nothing specifying its behaviour. `001/spec.md:165-193` (FR-001..FR-012); `001/tasks.md:96`

**CRITICAL — undo is specified nowhere, in any of the six phases.** `platform/DESIGN-SYSTEM.md` §8
makes soft-delete + `UndoSnackbarHost` (5s) + a recoverable location binding, and
`UndoSnackbarHost` is already built in `:libs:core` §5.1. Grep across `apps/finance/specs/**`
returns only the phrase "cannot be undone". `transactions.deleted_at` exists, so the mechanism is
there — the UX obligation is simply unwritten. Pairs with the unowned **Trash** surface (§1).

| Entity | Create | Edit | Delete | Undo |
|---|---|---|---|---|
| Holding | 001 FR-001 | **MISSING** | **MISSING** | **MISSING** |
| Valuation | 001 FR-003 | forbidden by design ✅ | soft-delete via FR-004 | **MISSING** |
| Transaction | 002 FR-001 | 002 FR-006 | 002 FR-006 (soft) | **MISSING** |
| Account | 002 FR-016 | 002 FR-016 | Edge Case only, **no FR** | **MISSING** |
| Category | **MISSING** | FR-023 rename / FR-025 exclude | **MISSING** (merge ≠ delete) | n/a — merge irreversible by design |
| Budget | 003 FR-005 | 003 FR-013 | **MISSING** | **MISSING** |
| Goal | 003 FR-016 | 003 FR-016 | 003 FR-016 | **MISSING** |
| Goal link | 003 FR-023 | earmark-qty change unspecified | 003 FR-023 unlink | **MISSING** |
| Policy | 003 FR-032 | 003 FR-032 | 003 FR-032 | **MISSING** |
| Premium payment | 003 FR-039 | append-only | **no correction path** (valuations have soft-delete+append; this does not) | **MISSING** |
| Policy document | 003 FR-038 | n/a | **MISSING** | **MISSING** |
| Recurring template | 002 FR-027 | **MISSING** | **MISSING** — the key-entity row claims it can be deleted | n/a |
| Saved view | 002 FR-015 | **MISSING** | **MISSING** | n/a |
| Saved scenario | 003 FR-046 | Assumption only, no FR | **MISSING** | n/a |
| Alert | 006 FR-020 (system) | FR-014 read/unread | auto-only, FR-018 90d — user cannot dismiss | n/a |

Evidence: `002/spec.md:200-201`,`:276-286`,`:296-301`,`:328`,`:332`; `003/spec.md:386-388`,
`:404-406`,`:513`; `006/spec.md:325-333`.

---

## 4c. Business-rule propagation across phases

| BR | Owning FR | QA row(s) | Gap |
|---|---|---|---|
| BR-C1 append-only valuations | 001 FR-004 | NW-BR-002/003, DAT-BR-007 | — |
| BR-C2 atomic first valuation | 001 FR-002 | NW-BR-001 | — |
| BR-C3 enums append-only | 001 FR-012 | NW-BR-005 (manual) | value lists unfrozen (§3) |
| BR-C4 net worth never stale | 001 FR-005; 005 FR-020 | NW-BR-006; SIG-BR-003 | — |
| **BR-D1 transfers never spend** | 002 FR-003; 003 FR-010; 005 FR-010 | MNY-BR-001; **no PLN row**; SIG-BR-001 | `MNY-BR-001` asserts *budgets*, which do not exist in Phase 3 — **the row is untestable where it lives**; Phase 4 restates the rule in FR-010 but has no QA row for the transfer clause |
| BR-D2 credit cards excluded | 002 FR-017/018 | MNY-BR-002, MNY-UI-004 | — |
| BR-D3 rename keeps / merge irreversible | 002 FR-023/024 | MNY-BR-003/004 | — |
| BR-D4 recurring → review queue | 002 FR-028/029 | MNY-BR-005, MNY-FLOW-002 | queue itself is unowned Phase 7 (§1) |
| **BR-D5 audit on every mutation** | 002 FR-007/008 | MNY-BR-006 | **not propagated to Phase 4.** 002 implements it well (`fn_transaction_audit` trigger, `transaction_events` SELECT+INSERT-only); 003 has **no** audit FR for budget, goal, goal_link, policy, premium or scenario mutations and no QA row asserting one. A raised budget or changed earmark is silently mutable with no record |
| BR-E1 goal progress = linked holdings | 003 FR-017/018 | PLN-BR-001, PLN-FLOW-002 | — |
| BR-E2 calendar-month pace | 003 FR-007 | PLN-BR-002 | — |
| **BR-E3 excluded categories** | 003 FR-010 | PLN-BR-003 | **no FR and no QA row in 005.** FR-010 covers investment contributions in cashflow only; savings rate, expense, "where it went" (FR-001/002/005) and P&L (FR-013..015) carry no exclusion clause. 005 relies on an Assumption ("inherited, not redefined") rather than a requirement |
| BR-E4 assumptions visible, derived labelled | 003 FR-041/047 | RET-UI-002, PLN-BR-005 | — |
| BR-G1/G2/G3 | NONE — Phase 7 | AUT-BR-001..003 written | rows exist, phase does not (§1) |

---

## 4d. Screen states, TDD gate, traceability

| Item | Finding | Evidence |
|---|---|---|
| **001 screen states** | Defines **only** signed-out + offline (FR-011 is the sole state requirement). No loading, error/retry, empty or not-configured FR for any of its 8 screens. "Empty" appears once, as an Edge Case for C1, not as an FR. Contrast 002 FR-032 and 003 FR-048, which mandate all five | `001/spec.md:190-191`, `:148` |
| **005 and 006 screen states** | Both mandate signed-out/offline/not-configured/disabled/empty (005 FR-041/045, 006 FR-011/019) but neither requires a `SkeletonBlock` **loading** state or a retryable `RetryErrorCard`. 006 FR-011 is search-only; B2's states appear in SC-015 with no FR behind them | `005/spec.md:478-487`; `006/spec.md:316`,`:334`,`:465` |
| **001 violates the RED→GREEN gate** | Repo-layer tests exist (T011–T013, T021, T025, T028, T033–T034), but T016/T017/T018 (C1/C2/C4), T023 (C3), T027 (C5 live delta), T031, T036 and the state-card tasks T020/T037 have **no preceding RED task**. Implementation plan §7.0 makes RED→GREEN binding | `001/tasks.md:82-108`,`:126-133`,`:145-152`,`:222-228` |
| **FR→task traceability is largely absent** | FR-id references in tasks.md: **001 = 1/13**, 006 = 10/38, 002 = 15/35, 003 = 16/52, 004 = 18/45. Only **005 (46/51)** traces properly. 001/002/003 cite QA-catalog rows and user stories instead, so an FR with no QA row (001 FR-009 prepay projection, 003 FR-024 earmark guard UI) has **no verifiable owner** | `001/tasks.md:11-15` |
| NFR ownership | 001 states no FR for NFR-3 (precision), NFR-6 (a11y), NFR-7 (motion), NFR-8 (perf) — relies wholly on the catalog's cross-cutting §0. 002/003/005 each restate precision. **NFR-7 (motion) has no FR in any phase** | `qa-catalog:20-31` |
| Open items §8.3 / §8.4 / §8.7 | Custom fields, Ask's owner tab, and **cross-device consent sync** are declined by every spec that touches them. 004 puts custom fields *and* cross-device consent out of scope while shipping the consent controls — and is marked "ready for `/speckit-implement`" | `004/spec.md:19`,`:443-444`; `005/spec.md:545-547` |

---

## 4e. DPDP erasure cascade — one real hole

Server-side erasure is correct: 001 T004, 002 T009 and 003 T021 each extend `delete_my_data()`.

**HIGH — `alert_log` survives "Delete my data".** Phase 6 stores paise amounts and `subject_id`
for erased records in a device-local Room table, explicitly excluded from `delete_my_data()`.
004's erasure FRs (FR-015..FR-017) are server-side only. After a successful erasure the
notification centre still holds readable money figures for records that no longer exist.
`006/data-model.md:63-65`; `004/spec.md:342-348`

---

## 4f. Component ownership — the micro-frontend rule is being violated

DESIGN-SYSTEM §5 forbids feature-local styling; anything reusable must live in `:libs:core`.
Components the phases need but no phase owns will be hand-rolled inside feature modules.

| Component | Finding | Evidence |
|---|---|---|
| **`NxTabs` (batch B8)** | **Owned by no phase.** 005 needs it for statement tabs and states it is "owed by earlier phases"; 001/002/003 build nothing from B8. 003 mentions it only to say it is *not* `SegmentedRow`. Phase 5 will hand-roll it | `005/plan.md:101`,`:119`; `005/research.md:180`; `003/research.md:159` |
| **`PaceRing` built twice** | 001 T006 puts it in `ui/components/charts/`; 003 T043 puts it in `ui/components/Rings.kt` and calls it "genuinely new (verified absent by symbol search)". Whichever lands second duplicates or conflicts | `001/tasks.md:56-59`; `003/tasks.md:121`; `003/plan.md:111`,`:228` |
| **B7 `Spinner`; B2 minus `DateRangeSheet`** | `NxCheckbox`, `NxRadio`, `PinEntry`, `QwertyKeypad`, `EnumPickerGrid`, `Spinner` — zero grep hits across all six spec dirs. Note `PinEntry` is needed by 004's app lock | grep across `apps/finance/specs/*/` |
| **§5.3 removable `Chip`** | Unclaimed. 002 instead adds a **parallel `InputChip`** — which §5.3's own closing rule forbids: "extending the existing component, never adding a parallel one" | `002/plan.md:182` |

---

## 4g. Settings contributions and notification channels — a structural break

004 declares the binding model: **"every later phase ships its module's settings entry with the
module"** (impl-plan `:301`; `004/contracts/settings-contribution.md:41`,`:78`).

**001, 002, 003 and 006 plan no `SettingsContribution` at all** — zero grep hits. Only 005 plans one
(5a), and even that never cites the contract. Four modules ship with no Settings presence, breaking
004 FR-003/FR-004.

This cascades into notifications: 006 §17 requires every channel to have exactly one control **in
the Settings entry of the module that owns it**, but 4 of the 5 owning modules (networth ×2,
planning, insurance) plan no settings entry. Only the monthly-summary control has a stated owner and
storage location. **Sub-phase 6f is a "verification" task against controls that were never planned.**
`006/contracts/alert-pipeline.md:114`,`:189`; `006/plan.md:148`; `005/plan.md:193`

---

## 4h. Remaining technical findings

| Sev | Finding | Evidence |
|---|---|---|
| MED | **`suggestions` claimed by two phases.** 002 owns it (D9 recurring, `MNY-BR-005`), but impl-plan Phase 7 step 1 still reads "`suggestions`, `automation_rules` schema" — unamended, even though 002 wrote its correction note into §7 Phase 3. Phase 7's SA would re-create an existing table | impl-plan `:225`,`:416-417`,`:479`; `002/plan.md:38` |
| MED | 006 states `grant execute … to authenticated` for both its functions in the contracts, but **no task hand-adds them** — and `db diff` cannot emit grants (ADR-0032 caveat). 005 makes this an explicit task at every sub-phase | `006/contracts/search-rpc.md:33`; `006/tasks.md:78`,`:178` |
| MED | **The "Parallel · Web" track is orphaned.** No phase plans any web UI work. Only web references anywhere are `database.ts` regeneration in 002/003/005; 001, 004 and 006 have none, and 006 states "No web work this phase". Nothing schedules the "one phase behind" React track the plan promises | impl-plan `:487-495`; `006/plan.md:62-63` |

**Clean — verified, no action needed:** feature-flag reservation (`money`/`budgets`/`goals`/
`debtpayoff`/`insurance`/`retirement`/`insights` all reserved in §5.5, all added with
`requiresConsent: true`, `minVersion 1.0.0`, by the correct phase; `automation` reserved and
correctly deferred; no unreserved key beyond the known `search`/`alerts`). Dependency DAG is
acyclic and no phase depends on unspecced Phase 7 — 002 pulling `suggestions`/`recurring_templates`
forward is what removes that edge. Cross-phase column dependencies all resolve (`liability_type`,
`split_group_id`, `remind_days_before`, `alert_pct`, `tax_section` each created by the phase the
consumer names). ArchUnit needs no per-phase registration (`DependencyRulesTest` is package-pattern
based with `allowEmptyShould(true)`). Room v5→v6 has a stated additive `MIGRATION_5_6` plus an
on-device verification task; `AppDatabase.kt:12` confirms current version 5.

---

## 5. Deferred **with** a stated reason — process correct, consequence recorded

Not defects. Listed so the shipped behaviour is not mistaken for the designed behaviour.

- **C3 XIRR** → blocked on an unwritten ADR; interim stat is `(current − invested)/invested`, which is
  not the stat the design labels and is not comparable across holdings. `001/research.md:66-79`
- **E5 "56 g earmarked" / C1 legend "count/qty"** → holdings carry no quantity or unit, so a gram
  figure is unrenderable; T073 renders percentage + rupee value and says so in copy. Consequence:
  C1's legend ships as count only — the `qty` half has no source in any phase. `003/research.md:10-43`
- **D4 budget impact** → budgets are Phase 4 (but see the unclaimed reciprocal task, §4 above).
- **Phase 2 UPCOMING credit-card row** → needs `accounts.due_day` from Phase 3; carried as an
  explicit Phase 3 follow-up. plan `:389-393`

---

## 6. Verified **not** gaps

Recorded so the next audit does not re-open them.

- **Money precision.** No float, decimal or rupee-denominated field in any tracker table across all
  six data models. All amounts `bigint`/`Long` paise; rates and shares in integer basis points or
  whole percent. `BigDecimal` is confined to calculator/projection engines outside `tracker/`
  (`003/plan.md:112`). Sole wart: `nominee_share_pct` (§3).
- **Attachments.** Genuinely specified, not hand-waved — `transactions.receipt_path` and
  `policies.documents` hold **device-local** paths, with matched rationale and the stated consequence
  that an attachment does not follow the user to a new device. `002/research.md:94-110`;
  `003/research.md:179-196`
- **Room vs Supabase split.** Clean. Room owns `alert_log` only (new, `MIGRATION_5_6`) plus
  pre-existing calculator history and currency cache; DataStore owns settings and saved views;
  everything else is Postgres. No field expected in both. `alert_log` is correctly excluded from
  `delete_my_data()` with a stated reason. `006/data-model.md:23-58`
- **Schema on disk vs data-models.** `holdings.sql` and `valuations.sql` match 001's declarations
  column-for-column. No drift. Every other declared table is unbuilt, as expected — except
  `liabilities_meta` (§3).
- **Splits, audit trail, append-only enforcement, earmark cap.** All pushed to the database layer
  (no UPDATE/DELETE policies, triggers over client checks) rather than client discipline. Consistent
  across 001/002/003.

---

## 7. Derived values with **no** stated computation

| Value | Screen | Missing |
|---|---|---|
| `INVESTED`, `GAIN` | C3 | no input |
| Net-worth % delta + sparkline series | 01, C1 | no historical series |
| Per-holding % change + sparkline | C2 | no history-bearing view |
| Amortisation principal / interest / left | C7 | no original principal, no formula |
| `MONTHLY OUTGO`, `DEBT-FREE BY` | C6 | derivable; location unstated |
| Credit utilisation %, running balance | D6, D7 | derivable; location unstated |
| Goal projection chart | E5 | no growth or contribution input |
| Projected retirement corpus | E9 | no starting corpus, no contribution stream |
| Next-30-days occurrence expansion from `rrule` | D9 | no expander named |
| Trailing-12-month comparative average | F1 | no function call specified |
| `XIRR` | C3, F5 | deferred to an unwritten ADR; the holding↔transaction link it needs does not exist |

Defined computations (for contrast) live in `v_latest_valuation`, `v_net_worth_by_sector`,
`v_account_balances`, `v_month_summary`, `v_category_spend`, `v_budget_status`, `v_goal_progress`,
`v_annual_income`, the six `finance.report_*` functions, and the client engines `BudgetPaceEngine`,
`DebtPayoffEngine`, `InsuranceCoverEngine`, `RetirementProjectionEngine`, `PeriodResolver`,
`StatementReconciler`.

---

## 9. Round 2 — CRITICAL: two Phase 2 requirements are impossible against the committed schema

**FR-004's correction path cannot work.** FR-004 requires hiding a wrong valuation; the only
mechanism is setting `deleted_at`, which is an UPDATE — and
`supabase/schemas/finance/10_tables/valuations.sql` carries **SELECT and INSERT policies only**,
`grant select, insert`, and an explicit comment "Deliberately no UPDATE policy (DAT-BR-007) and no
DELETE policy". `data-model.md` states that absence *as the guarantee*, T026 cites it *as
enforcement*, and T025 asserts a test that will fail at RLS. ADR-0029 decision 4 already named the
fix — a security-definer **`correct_valuation()` RPC** — and assigned it to "Phase 2's SA step".
No task creates it. Either the correction never works, or a developer adds an UPDATE policy and
destroys BR-C1's database-level append-only guarantee. → 001 T062.
*(Verified directly against the schema file, not inferred.)*

**FR-002's atomicity is not achievable.** "Holding + first valuation written atomically" is two
PostgREST inserts over HTTP. `001/data-model.md:38-40` concedes it is "not expressible as a
single-table constraint" and pushes it to "the repository layer either writes both or neither",
which cannot be transactional across two requests. No RPC, no compensating delete, and `holdings`
has no client DELETE policy — so a failed second insert leaves an orphan holding, violating FR-002's
own invariant. → 001 T063.

**Related, feature-wide:** no phase specifies write-retry semantics. The only idempotency key in the
repo is `(recurring_id, due_on)` for recurring materialisation; manual creates have none and there
is no client request id, so a retry after a timeout silently duplicates a money row. → 001 T077,
002 T097.

---

## 10. Round 2 — design-system enforcement is assumed, not planned

| Gate | State | Evidence |
|---|---|---|
| **Motion** (§8, NFR-7) | **Zero coverage in all six phases** — no FR, task, QA row or constant. Sole owner is catalog row NFR-007, marked "Partial (splash timing Y; motion feel N)", cited by nobody. 001/003/005 all build animating charts | `qa-test-scenario-catalog.md:28`; zero hits across `apps/finance/specs/**` |
| **Accessibility** (§9, NFR-6) | **001, 002 and 006 have zero a11y tasks.** 001 and 002 do not even claim NFR-6 in scope; **006 claims it and plans nothing**. 001 builds `DonutChart`, `AmortisationDonut`, `PaceRing`, a trend chart and sparklines with no `contentDescription` task | `001/spec.md:13`; `002/spec.md:14`; `006/spec.md:15` |
| **Token discipline** | **No automated enforcement exists or is planned.** `detekt.yml:34-35` sets `MagicNumber: active: false`; no `ForbiddenImport`/`ForbiddenMethodCall` rule; `DependencyRulesTest` has 5 rules, none about tokens. 005 T169 verifies NFR-5 "by review and **detekt**" — against a check that cannot fire | `config/detekt/detekt.yml:34-35`; `DependencyRulesTest.kt:36,49,69,95,116` |
| **Observability triad** | `crashReporter.setModule` / `performanceTracer.trace` / `featureError` — required of every feature ViewModel by `apps/finance/CLAUDE.md` — planned **only** by 003 and 005. Zero hits in 001, 002, 004, 006 | `003/tasks.md:124`; `005/tasks.md:135` |
| **`FeatureHost`** (NFR-2) | 001 wraps **1 of 8** screens; 002 wraps **1 of 9**. 003 wraps all seven explicitly — the contrast shows omission, not implicitness | `001/tasks.md:111`; `002/tasks.md:102` |
| **Responsive tiers** (§3) | **No phase mentions responsiveness at all** — zero hits for `calculateDhruvNextResponsive`, "tablet", "small tier" | grep across `apps/finance/specs/*/*.md` |
| **Light/dark** (N7) | Planned only in 004 (T113) and partially 003 (T122, contrast only) | `004/tasks.md:279` |
| **Web parity** (§12.1) | No spec plans to close any recorded drift; `tokens.css`, `--radius-control`, `--font-brand: Georgia` and the duplicate `--color-*` layer appear zero times. §12's "both sides in the same change set" has no CI check and no owner | grep across specs |

Non-Compose surfaces: 006 owns 2 of §11's 6 notification conventions (unowned: sentence case, the
≤1-line collapsed rule, never a policy/account number, and **never an account name and an amount in
the same line under privacy mode**). 005's PDF work is missing "no logo beyond the wordmark" and has
no test behind its type-hierarchy mapping. **004 T078 verifies widget masking for a widget no phase
builds** — 006 records it descoped.

---

## 11. Round 2 — UI/UX fidelity to the design as drawn

**Five cross-cutting rules are owned by no phase**, so a per-phase review will not catch them:

1. **`MoneyText` appears in zero tasks in 001, 002 and 003** — it is THE money renderer, and no phase
   plans the compact-on-cards / full-in-lists split the design specifies (`₹18.42L` on the Home hero).
2. **`StatDeltaChip` and `ThreeUpStatRow` — both already built in `:libs:core` — are named in zero
   tasks across all six phases.** Every ▲/▼ delta and every three-stat header would be hand-rolled,
   breaking both the micro-frontend rule and §1's never-colour-only rule.
3. **No `strings.xml` task in 001 or 002** (003, 004, 005, 006 all have one).
4. **Responsiveness** — unplanned everywhere.
5. **Light/dark render verification** — planned only in 004, partially 003.

**Three components the design draws, `:libs:core` lacks, and no phase builds**: `EnumPickerGrid`
(C4's sector picker — 001 substitutes `SelectionSheet`), `NxTabs` (**two** orphaned consumers: D8's
Expense/Income tabs and 005's statement tabs), and an **area chart** (library has `TrendSparkline`
and `BarChart` only; 01, C2, C3, D7, E5 and E9 all draw areas).

Per-screen fidelity: **003 = 8/9 · 005 = 4/5 · 006 = 2/2 · 002 = 4/9 · 001 = 1/8.**
001's specific gaps: C3 missing `LAST VALUED <date>` + sector + a component for its range chips
(`PeriodChipRow` exists); C6 missing per-row rate and EMI; 01 missing the one-line state; C1's legend
missing the enum tag; C7's prepay projection missing the §10 derived-output label 003 gives its
equivalents. → 001 T074, 002 T107, 003 T154, 005 T188.

**Requirements re-review, remaining**: split transactions are one entity in 002's spec and N sibling
rows in its data model (T096); 006's FR-003/SC-001 contradict its own search contract's pre-cap
`kind_total`, and the results past the 25/kind cap are unreachable (T132, T133); 003's three
retirement scenarios have no parameters, so any invented delta passes T103's distinctness test
(T149); 003 says "five assumptions" and lists six, contradicted inside its own data-model (T150);
**zero pagination anywhere in six specs**; goals, policies, C4 and C5 have no field validation;
future-dated valuations are unguarded and self-perpetuating (T064); SC→task traceability is 0/5 in
001 and 0/14 in 003.

---

## 8. Suggested resolution order

**Blocking — nothing is safely implementable until these land.**

1. **Add `security_invoker = on` to all 8 planned views** across 001/002/003 (§0). Hand-write it
   into each migration — `db diff` cannot express it.
2. **Fix 001's three build/merge blockers** (§0b): `projectDir` remap, declarative schema twin +
   `db diff` + equivalence/docs guards, and the ADR-0033 grants. Add `networth` to `coveredModules`
   while there.
3. **Decide `liabilities_meta`'s schema** (`public` vs `finance`) and author its declarative twin —
   four phases reference it and every downstream FK is conditional on this.

**Correctness of the product definition.**

4. **Add cost basis** (`invested_paise` + capture in C4) or formally drop `INVESTED`/`GAIN`/XIRR
   from C3 — three phases' stats depend on the answer.
5. **Decide the net-worth history source** for the Home/C1 delta and C2 sparklines — either a
   history view in Phase 2 or an explicit deferral with the screens' copy adjusted.
6. **Specify holding edit + delete**, and **specify undo** once, app-wide, with the Trash surface it
   implies (§4b). Undo is binding design law today and appears in no spec.
7. **Propagate BR-D5 (audit trail) to Phase 4 entities** and **BR-E3 (excluded categories) into
   Phase 5's FRs**; move `MNY-BR-001`'s budget clause to a Phase-4 QA row (§4c).
8. **Add loading + error states** to 001 (which has neither), 005 and 006 (§4d).

**Structural / cross-cutting.**

9. **Plan a `SettingsContribution` in 001, 002, 003 and 006** (§4g) — without it 6f verifies
   controls nobody built.
10. **Assign `NxTabs` an owner; de-duplicate `PaceRing`**; claim the removable-`Chip` extension
    instead of 002's parallel `InputChip` (§4f).
11. **Extend `NavTarget`** for Home→Currency and E3→filtered ledger, make `OpenTransaction`
    unconditional, and add a NavTarget section to `001/contracts/routes.md` (§3).
12. **Create the Phase 7 spec-kit directory** — two shipping phases already defer work into G2, and
    `AUT-*` QA rows are already written. Amend impl-plan Phase 7 to drop `suggestions` (002 owns it).
13. **Assign owners** for Trash, Profile, Import mapper, Custom fields, credit-card screens, the
    three orphaned intents, and the Glance widget — or record each as an accepted permanent descope.
14. **Wire `alert_log` purge into the erasure action**, or stop storing paise in `payload_json`
    (§4e) — DPDP.
15. **Add `policies.name`**; reconcile the B3 search contract's `description`/`counterparty` with
    `payee`/`note`.
16. Fix `nominee_share_pct` to basis points before any policy row exists.
17. Freeze the `sector`, `valuations.source` and `categories.tax_section` value lists in their
    owning data-models.
18. Declare C3/D2/D7 as dark-hero surfaces in 001/002 before those screens are built.
19. Decide whether the **web track** is real (nothing schedules it) and whether **FR→task
    traceability** should be retrofitted — 001 cites 1 FR across 13 tasks; only 005 traces properly.