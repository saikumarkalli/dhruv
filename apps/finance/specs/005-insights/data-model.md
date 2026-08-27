# Phase 1 Data Model: Insights (Phase 5)

**Feature**: `apps/finance/specs/005-insights/` | **Date**: 2026-08-22

Insights **records nothing**. It has no table of its own in sub-phases 5a–5e, no snapshot, no cached
statement. Everything below is either an entity another phase owns and this one reads, a
server-side aggregation, or a client-side domain model that exists only for the duration of a screen.

---

## Existing entities referenced, not changed

| Entity | Owner | Read for |
|---|---|---|
| `finance.holdings` (`kind`, `sector`, `deleted_at`) | Phase 2 | balance-sheet grouping |
| `finance.valuations` (`value_paise`, `as_of`, `source`, `deleted_at`) | Phase 2 | as-at position; `source` drives the self-valued footnote |
| `finance.liabilities_meta` (`liability_type`) | Phase 2 | balance-sheet liability grouping |
| `finance.v_latest_valuation`, `finance.v_net_worth_by_sector` | Phase 2 | the FR-020 agreement assertion only |
| `finance.accounts` (`opening_balance_paise`, `type`, `deleted_at`) | Phase 3 | cashflow opening and closing balance |
| `finance.categories` (`name`, `kind`, `excluded_from_spend`, `deleted_at`) | Phase 3 | line labels, spend exclusion |
| `finance.transactions` (`type`, `amount_paise`, `account_id`, `to_account_id`, `category_id`, `occurred_at`, `split_group_id`, `deleted_at`) | Phase 3 | every period figure |
| `finance.v_month_summary`, `finance.v_category_spend` | Phase 3 | month-grain cross-check fixtures |

**Split transactions need no special handling.** Phase 3 models splits as sibling rows sharing
`split_group_id`, with no parent row holding a total, so every aggregation below sums the parts
correctly with no change and nothing can double-count.

## New tables

**None in 5a–5e.**

**5f only**: one column added to a Phase 3 table.

| Table | Column | Type | Notes |
|---|---|---|---|
| `finance.categories` | `tax_section` | text, nullable, default null | User-set tax grouping (research R12). Null = not tax-relevant. Values are append-only TEXT constants, same convention as `sector` and `liability_type` (Article IX) |

---

## Reporting functions (server-side aggregation — NFR-8)

All are `security invoker` and `stable`, in the `finance` schema, reached over PostgREST
`POST /rest/v1/rpc/<name>` with `Content-Profile: finance`. RLS applies as the caller, so each
returns only the caller's own data (research R3). All amounts are `bigint` paise. **No function
returns a percentage** — shares and movements are computed at render from the paise the function
returns (research R13).

Soft-deleted rows (`deleted_at is not null`) are excluded everywhere, without exception.

### `finance.report_period_summary(p_from date, p_to date)`

Returns one row: `income_paise`, `expense_paise`, `excluded_paise`, `transfer_paise`.

`expense_paise` excludes `TRANSFER` rows and rows in `excluded_from_spend` categories — the same two
exclusions `v_month_summary` already applies, so a month-grained call must equal that view for the
same month. `excluded_paise` keeps the excluded money visible so the savings-rate arithmetic is
explainable rather than mysteriously short.

Backs F1's savings rate and its three headline figures (FR-001, FR-002).

### `finance.report_cashflow(p_from date, p_to date)`

Returns an ordered line set:

| Column | Type | Notes |
|---|---|---|
| `section` | text | `OPENING · MONEY_IN · MONEY_OUT · MOVED_NOT_SPENT · NET_CHANGE · CLOSING` |
| `line_key` | text | category id, account id, or a literal for the balance rows |
| `line_label` | text | |
| `amount_paise` | bigint | always positive; meaning comes from `section`, never from a sign |
| `sort_order` | integer | server-fixed presentation order |

`MOVED_NOT_SPENT` carries `TRANSFER` rows and rows in `excluded_from_spend` categories; `MONEY_OUT`
carries neither (FR-010, BR-D1).

**`OPENING` and `CLOSING` are computed independently of each other, deliberately.** Opening is
`Σ accounts.opening_balance_paise + Σ signed transactions with occurred_at < p_from`; closing is the
same expression evaluated at `<= p_to`. Deriving closing as *opening + net change* would make the
reconciliation identity a tautology and `SIG-BR-001` would test nothing. Computing both from the
ledger makes the identity a real assertion about the data.

### `finance.report_pnl(p_from date, p_to date, p_prior_from date, p_prior_to date)`

| Column | Type | Notes |
|---|---|---|
| `line_group` | text | `INCOME · EXPENSE` — income lines sort first (FR-014) |
| `category_id` | uuid | identity, so a rename between the two periods still matches one line (BR-D3) |
| `line_label` | text | current-period name |
| `amount_paise` | bigint | selected period |
| `prior_amount_paise` | bigint | prior-year period; `0` when the category had no activity, distinguished from absent by the caller-supplied prior range being valid |
| `sort_order` | integer | |

The prior range is supplied by the client from `PeriodResolver`, not derived in SQL, so the range
can be stated on screen (FR-016, and the leap-day edge case). When no prior-year data exists at all
the client passes a null prior range and the function returns no prior amounts — the screen then
states the comparison is unavailable rather than showing zeroes.

**Matching is by `category_id`, not by name** — a category renamed between the two periods stays one
line, and a category deleted after the prior period still contributes its prior amount.

### `finance.report_balance_sheet(p_as_of date)`

| Column | Type | Notes |
|---|---|---|
| `side` | text | `ASSET · LIABILITY` |
| `group_key` | text | `holdings.sector` for assets, `liabilities_meta.liability_type` for liabilities |
| `group_label` | text | |
| `value_paise` | bigint | latest non-deleted valuation with `as_of <= p_as_of` (research R4) |
| `value_prior_month_paise` | bigint | the same derivation at `p_as_of - 1 month`, for the change column (FR-019) |
| `holding_count` | integer | |
| `has_self_valued` | boolean | true when any holding in the group has a user-supplied `valuations.source`; drives the FR-021 footnote |

Net worth is `Σ ASSET − Σ LIABILITY` and is **not** returned as its own row — the screen computes it
from the groups so that FR-018's stated identity is visible arithmetic rather than a server opinion.

A date preceding the user's first valuation returns **no rows**, which the screen renders as "no
position existed" (FR-022) rather than as a zeroed statement.

### `finance.report_category_breakdown(p_from, p_to, p_prev_from, p_prev_to date)`

| Column | Type | Notes |
|---|---|---|
| `category_id` | uuid | |
| `label` | text | |
| `amount_paise` | bigint | selected period |
| `prior_amount_paise` | bigint | immediately preceding comparable period |
| `share_bps` | integer | share of the period's spend, in basis points — an integer, never a float |

Backs F1's "where it went" (FR-005) and F5's category-breakdown report (FR-026). The prior range is
the immediately preceding period of the same length, supplied by the client — a different rule from
`report_pnl`'s prior-year range, which is why both are caller-supplied rather than inferred.

### 5f — `finance.report_investment_returns(…)`

**Signature deliberately unspecified.** Its arguments and returned shape follow from the gating
decision record (research R8) — which cashflow set, which terminal value, what scope. Writing a
signature here would be inventing the answer the record exists to give.

### 5f — `finance.report_tax_summary(p_from date, p_to date)`

| Column | Type | Notes |
|---|---|---|
| `tax_section` | text | from `categories.tax_section`; rows with null are excluded entirely |
| `line_label` | text | |
| `kind` | text | `INCOME · DEDUCTION` |
| `amount_paise` | bigint | |

---

## Client domain models (`:apps:finance:data`, transient)

None of these is persisted. They exist for the lifetime of a screen and are the objects the export
writers serialise (research R5).

### `ReportingPeriod` (`data/reporting/`)

| Field | Type | Notes |
|---|---|---|
| `kind` | enum `MONTH · QUARTER · FINANCIAL_YEAR · CUSTOM` | append-only constants |
| `start` | `LocalDate` | inclusive |
| `end` | `LocalDate` | inclusive |
| `label` | `String` | resolved for display; the screen never formats a period itself |

**Invariants**, enforced in `PeriodResolver` and asserted by its tests:
- `start <= end` always; a range failing this is rejected at selection with a stated reason (FR-025).
- `end` never lies in the future for a `CUSTOM` range.
- `FINANCIAL_YEAR` always spans 1 April to 31 March (FR-024).
- `QUARTER` boundaries follow the financial year, not the calendar year — a consequence of FR-024
  that would otherwise be decided inconsistently by each screen.
- `priorYear(period)` shifts by exactly one year, clamping 29 February to 28 February; the resolved
  range is carried on the result so the screen can state it.
- `previousComparable(period)` shifts back by the period's own length — the rule for movement
  figures, distinct from `priorYear`.

### `Statement`

| Field | Type | Notes |
|---|---|---|
| `kind` | enum `CASHFLOW · PROFIT_LOSS · BALANCE_SHEET · CATEGORY_BREAKDOWN · INVESTMENT_RETURNS · TAX_SUMMARY` | append-only |
| `period` | `ReportingPeriod` | the period this statement is *of* — carried on the object, so an export can never disagree with it |
| `asOf` | `LocalDate?` | balance sheet only; the rendered date when it differs from `period.end` |
| `sections` | `List<StatementSection>` | ordered as returned |
| `footnotes` | `List<String>` | the transfers note, the self-valued note |
| `generatedAt` | `Instant` | stamped into every export (FR-030) |

### `StatementSection`

`key`, `label`, `lines: List<StatementLine>`, `subtotalPaise: Long`.

**Invariant**: `subtotalPaise == lines.sumOf { it.amountPaise }`. Asserted by `StatementReconciler`,
not assumed — this is the object-level form of FR-012.

### `StatementLine`

`key`, `label`, `amountPaise: Long`, `priorAmountPaise: Long?`, `shareBps: Int?`.

Percentages are absent (`null`), never `0`, when they do not apply — so a screen cannot render a
meaningless "0%".

### `PeriodSummary`

`incomePaise`, `expensePaise`, `excludedPaise`, `transferPaise`, and `savingsRateBps: Int?`.

**`savingsRateBps` is null when income is zero** (FR-003, research R13). Not `0`, not a sentinel, not
a floating NaN — an absent value the screen is forced to handle.

### `PositionSnapshot`

`asOf: LocalDate`, `assets: List<StatementSection>`, `liabilities: List<StatementSection>`,
`netWorthPaise: Long`, `hasSelfValued: Boolean`.

**Invariant**: `netWorthPaise == assets.sumOf { subtotal } - liabilities.sumOf { subtotal }`
(FR-018, SC-002).

### `ExportArtifact`

`format: CSV | PDF`, `statement: Statement`, `destinationUri`. Holds the statement by reference —
there is no second copy of the numbers to drift (research R5).

---

## State transitions

- **Reporting period**: selected → carried to every Insights screen until changed. Held in
  `InsightsPeriodStore`; reset to the current month on sign-out. No persistence across launches is
  specified, and none is added — a period is view state, not a setting (settings spec's own
  boundary: "per-screen view state … is not a setting").
- **Balance-sheet date override**: `null` → set by the user → cleared when the period changes
  (FR-017). Never propagates; never persisted. Precedence lives in one `collect` in
  `BalanceSheetViewModel` (research R6).
- **Statement**: loaded → rendered → optionally serialised to an export. Never stored, never cached
  across a period change, never re-read from a snapshot. A correction appended to an already-reported
  period changes the statement on its next load, by design.
- **Monthly-summary preference**: off ↔ on, device-local encrypted DataStore, read by the
  notifications phase when delivery ships. This feature never acts on it (FR-048).