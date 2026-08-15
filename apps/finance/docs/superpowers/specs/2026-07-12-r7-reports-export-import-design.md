# R7 — Reports, Data Export & Import

> Status: **SPECCED** (build after P3/P4 — reports surface the inputs P5 consumes; export covers
> every table that exists by then). Master sequence:
> `../plans/2026-07-12-master-roadmap-personal-app.md` (R7; gaps N11/N12/N13). Umbrella + design
> system + playbook binding; inherits all shared invariants incl. P1 §4.4 error taxonomy.

## Goal

Three capabilities that turn logged data into answers and remove the cloud-primary lock-in trap:
(1) **Reports** — month/year statements and trends the bento cards can't show; (2) **Export** —
the user's own copy of every row (CSV zip + PDF net-worth statement), which is also the DPDP
portability story; (3) **Import** — CSV seeding so years of existing spreadsheet history doesn't
have to be typed by hand. Export → wipe → import must round-trip losslessly.

## Non-goals

- No scheduled/automatic backups (manual, user-initiated only — consistent with the vault's
  manual-export philosophy; revisit only with usage evidence).
- No cross-app import mappers (no "import from Walnut/Money Manager" format adapters in v1 —
  the documented CSV template is the interchange format).
- No cash-flow forecasting; no custom report builder. Fixed report set below.
- PDF is a statement (tables + headline numbers), not a designed brochure — `PdfDocument`
  platform API, no new dependency.

## Decisions (proposed)

| # | Decision | Rationale |
|---|----------|-----------|
| D1 | Report aggregation = **security-invoker SQL RPCs**, never client fetch-all | G20 (valuations fetch-all) already flagged as non-scaling; monthly rollups over years of transactions are exactly the case; PostgREST RPC = same auth model as `delete_my_account()` precedent (invoker, RLS applies) |
| D2 | Export format = one ZIP: `manifest.json` (schema version, export date, app version, row counts) + one CSV per table in the **Tracker Table Registry** | Single artifact to store; manifest makes import validation and future format evolution deterministic |
| D2a | **Tracker Table Registry** (authoritative, in `:data`): `assets, liabilities, valuation_entries, transactions, budgets, recurring_rules, goals, goal_links, payoff_plans, policies` — and, per the roadmap §4 standing rule, **every future tracker table registers here in its creating PR** (P5 adds `retirement_scenarios`). The registry also drives the R8 trash surface and the round-trip test, so a missed registration fails CI | F1/F2 in the consistency review: the original list dropped `goal_links` + `recurring_rules`, which would have silently broken round-trip; a registry makes the omission structurally impossible |
| D2b | Soft-deleted rows (`is_deleted = true`) are **excluded** from export — trash does not survive an export→wipe→import cycle | Restoring deleted junk on import surprises more than it helps; stated in the export dialog copy and manifest |
| D3 | File I/O via Storage Access Framework (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`) | Zero storage permissions on any API level |
| D4 | Import assigns **fresh UUIDs** and never upserts | Idempotent-import complexity rejected; preview screen warns "importing twice duplicates rows"; wipe-then-import is the documented restore path |
| D5 | CSV = RFC-4180 subset (quoted fields, escaped quotes, CRLF tolerant), hand-rolled parser/writer in `:data`. **CSV-injection hardening (SEC4):** cells starting with `=` `+` `-` `@` tab or CR are prefixed with `'` on export (so Excel/Sheets never executes a note as a formula) and the guard prefix is stripped on import — round-trip test covers it. Import text fields length-capped (name 120, notes 2000, matching editors) | ~100 lines, fully TDD-able; a CSV dependency is not worth a supply-chain entry |
| D5a | Export dialog copy warns: "This file contains your complete financial data, unencrypted. Store it somewhere you trust." (SEC5). Password-protected export = recorded Not-Doing (no platform-native encrypted ZIP; not worth a crypto dependency for a personal app) | Honest about the artifact; the user chose the destination |
| D6 | Export/import are **not** consent events | Data goes to a user-chosen local file, not a third party; PRIVACY.md documents export as the portability mechanism |

## Server: report RPCs (frozen for R7)

```sql
-- All: security invoker (RLS applies), stable, called via PostgREST /rpc/.
create function monthly_summary(from_month text, to_month text)
returns table (month_key text, income_paise bigint, expense_paise bigint) ...;
create function category_summary(p_month_key text)
returns table (category text, type text, total_paise bigint) ...;
create function networth_points(p_granularity text)  -- 'MONTH' v1
returns table (period_end date, assets_paise bigint, liabilities_paise bigint) ...;
-- networth_points = latest valuation per holding per period end (soft-deleted excluded),
-- same semantics as the client NetWorthCalculator — parity asserted by test fixtures.
```

Additive only; no table changes.

## Feature module — `:apps:finance:feature:reports`

| Piece | Detail |
|-------|--------|
| Screens | `ReportsScreen` (period selector: month chevrons + year mode toggle; income-vs-expense `BarChart` per month; savings-rate trend `TrendLineChart`; category `DonutChart` for selected month; net-worth statement card: period start/end/delta) |
| ViewModels | `ReportsViewModel` (RPC-backed, per-period cache in memory) |
| Pure logic (TDD) | `ReportAssembler` (RPC rows → chart models, savings-rate series, delta math); `CsvCodec` (D5); `ExportManifest` (versioned) |
| Repository | `IReportsRepository` (RPCs), `IExportRepository` (streams every table's rows page-wise → CSV zip; import: parse → validate → batched POST 500 rows/call) in `:data` |
| Export/Import UI | Settings > Data: "Export my data" (SAF create → progress → done with row counts); "Export net-worth statement (PDF)" (period picker → SAF); "Import data" (SAF open → validation preview screen: per-table row counts + first 3 errors → Confirm import → progress → summary) |
| Flag | `"reports": { "enabled": true, "minVersion": "<release>", "requiresConsent": true }` (account-level consent; consistency only) |
| Home bento card | "This year" mini card: YTD savings rate + tap → Reports |

## PO additions (2026-07-12 review — PG6/PG9; same phase, same RPCs)

| Item | Detail |
|------|--------|
| **PG6 Monthly digest notification** | Worker branch (R4 scheduler; fires on the 1st, IST): builds one line from the prior month's `monthly_summary` RPC — "June: saved 31% · ₹42k under budget · net worth ▲2.1%"; channel `monthly_digest` (LOW); privacy mode → ₹ masked via `MaskedMoney`, percentages stay (R3 carve-out); tap → `OPEN_REPORTS(month)` intent extra → ReportsScreen on that month; skipped silently when offline or <1 full month of data; Settings toggle under Notifications & alerts |
| **PG9 Insight chips** | `InsightRules` (pure, TDD): fixed deterministic rule set over the same RPC rows — category vs its 3-month average (±25% threshold), best/worst savings rate in trailing 12 months, over-budget category count; max 3 chips, priority-ordered, suppressed with <2 months of data; rendered as chips atop `ReportsScreen`; no AI, no network beyond the report fetch already happening |

**Tests:** `InsightRulesTest` (threshold edges, <2-months suppression, priority when >3 fire,
zero-income month); digest formatting test (masked/unmasked variants, month-boundary IST);
digest-skip conditions.

## PDF statement (v1 scope)

Header (name-free — just "Dhruv · Net worth statement", period, generated date), hero numbers
(net worth start/end/delta), assets table (name, category, latest value), liabilities table,
totals row. `MoneyText` formatting rules; privacy mode does NOT mask exports (explicit user act).

## Tests

`CsvCodecTest` (quotes, commas-in-fields, newlines-in-notes, round-trip property: write→parse =
identity); `ReportAssemblerTest` (empty months, single month, year rollup, savings-rate 0-income
guard — same edge set as P2's rollup); `ExportRepositoryTest` with fakes (manifest counts match
rows, pagination stitching); import validation tests (bad header, wrong schema version, malformed
row → error list with line numbers, valid file → batched POSTs); **round-trip test**: fake-server
export → wipe → import → row-equivalent modulo fresh IDs. RPC parity: `networth_points` fixture
results equal `NetWorthCalculator` output on same inputs. ArchUnit green.

## Dependencies

P1+P2 hard (valuations, transactions, budgets); P3/P4/R5b tables via the Tracker Table Registry
(D2a) — later phases extend the registry, not this spec. D1 RPCs deployed before client work
(M0-style gate: RPC smoke via authenticated call).

## UI/UX detail (states per design system)

| Surface | Layout & states |
|---|---|
| `ReportsScreen` | Period bar; charts in `BentoCard`s; empty ("Nothing logged in this period"), loading, offline, error standard; year mode swaps donut for top-categories bar list |
| Import preview | Per-table `BentoCard` (icon, "transactions · 1,204 rows"), error card listing first errors with line numbers, duplicate warning (D4), Confirm = `ConfirmDangerDialog` |
| Export progress | Modal progress with per-table ticks; success shows file name + row counts; failure → `RetryErrorCard` (partial file deleted via SAF) |

## Rollout & rollback

Flag `reports` gates the screen + Home card. Export/import live under Settings (not flag-routed)
but no-op to empty states when tracker features are off. RPCs additive; rollback = flag off /
revert PR; RPC removal optional cleanup.

## Risks / open questions

- RPC ↔ client calculator drift on net-worth semantics → parity fixtures are a merge gate (test
  above); any change to `NetWorthCalculator` must update fixtures (playbook note).
- Huge imports (10k+ rows) on free tier → batches of 500 with progress + resume-from-batch on
  failure; abort leaves partial data (documented: wipe-and-retry path).
- PDF on privacy-mode devices shows real numbers (D-note above) — deliberate, called out in the
  export dialog copy.

## TDD Mandate

> **Test-Driven Development (TDD) is strictly required for this phase.**
> All pure logic, calculators, reducers, and state machines MUST be written with failing tests first, followed by implementation. UI components must be tested for accessibility and rendering states on both Android and Web platforms.

