# Reporting function contract: Insights (Phase 5)

The client-facing contract for the six (later eight) `finance` schema functions this feature adds.
Column shapes live in [`../data-model.md`](../data-model.md); this document covers the calling
convention, the guarantees each function makes, and the rules a caller must not violate.

**Why functions and not views**: research R1. **Why `security invoker`**: research R3.

---

## Calling convention

| Aspect | Value |
|---|---|
| Transport | `POST /rest/v1/rpc/<function_name>` on the existing consent-gated `dataClient` |
| Schema header | `Content-Profile: finance` **and** `Accept-Profile: finance` — omitting either 404s against the empty `public` schema rather than erroring usefully (ADR-0033's recorded consequence) |
| Auth | `AuthInterceptor`'s `apikey` + `Authorization: Bearer` — unchanged from Phase 1 |
| Consent | `ConsentInterceptor` short-circuits before dispatch when "Sync my financial records" is off. No Insights code path may construct its own client (Article VIII) |
| Sole caller | `InsightsRepository` in `:apps:finance:data`. No feature module calls `rpc/` directly (Article III) |
| Date arguments | ISO-8601 `date`, inclusive at both ends, always resolved by `PeriodResolver` — never assembled at a call site |

**Grants required** (ADR-0033 decision 4 — a custom schema has no legacy auto-exposure to fall back
on): `grant usage on schema finance to authenticated` already exists; each function additionally needs
`grant execute on function finance.<name>(…) to authenticated`. No `anon` grant on anything — every
Insights call is authenticated. `supabase db diff` does not emit grants, so these are hand-added to
the generated migration, as ADR-0032's caveat list requires.

---

## Guarantees every function makes

1. **Caller isolation.** RLS applies as the caller. A second user's rows are unreachable, and the Sec
   step for each SQL-bearing sub-phase asserts this with a real second account rather than trusting
   the declaration.
2. **Soft-deleted rows are excluded**, in every function, without exception — including rows deleted
   *after* the period they fall in.
3. **Amounts are `bigint` paise, always positive.** Direction comes from the row's `section` or
   `line_group`, never from a sign. This is what lets the client treat every figure as a magnitude and
   removes an entire class of double-negation bugs.
4. **No percentages, ever.** Shares are returned as integer basis points where a share is genuinely
   part of the aggregation (`report_category_breakdown.share_bps`); everything else the client derives
   at render (research R13).
5. **Ordering is server-fixed.** `sort_order` is authoritative; the client renders in the order it
   receives and never re-sorts a statement, so the same period always reads the same way.
6. **Empty is empty.** A period with no qualifying records returns **zero rows**, not rows of zeroes.
   The distinction is what lets the screen tell "nothing happened" from "everything netted out"
   (FR-045, FR-022).

---

## Rules the caller must not violate

- **Never sum across calls to build a wider period.** Two month calls added together is not a quarter
  call — it is the client-side reduction NFR-8 forbids, and it silently mis-handles any range that
  does not align to month boundaries. Ask for the range you want.
- **Never derive closing balance from opening plus net change.** The function returns both computed
  independently for a reason (data-model, `report_cashflow`); recomputing one from the other turns
  `SIG-BR-001` into a tautology.
- **Never pass a prior range the resolver did not produce.** `report_pnl` and
  `report_category_breakdown` both take caller-supplied comparison ranges, and they use *different*
  rules — prior **year** for P&L, previous **comparable period** for movement figures. Hand-assembling
  either at a call site is how the two get swapped.
- **Never call with an unvalidated custom range.** `PeriodResolver` rejects `end < start` and future
  ends before a call is built (FR-025). The functions do not defend against it — a caller that skips
  validation gets an empty result and no explanation.

---

## Function summary

| Function | Arguments | Returns | Backs | Sub-phase |
|---|---|---|---|---|
| `report_period_summary` | `p_from`, `p_to` | one row: income / expense / excluded / transfer paise | F1 savings rate + headline figures (FR-001, FR-002) | 5a |
| `report_category_breakdown` | `p_from`, `p_to`, `p_prev_from`, `p_prev_to` | per category: amount, prior amount, share bps | F1 "where it went" (FR-005), F5 category breakdown (FR-026) | 5a |
| `report_cashflow` | `p_from`, `p_to` | ordered lines across six sections | F2 (FR-008 – FR-012) | 5b |
| `report_balance_sheet` | `p_as_of` | asset/liability groups with prior-month values | F4 (FR-017 – FR-022) | 5c |
| `report_pnl` | `p_from`, `p_to`, `p_prior_from`, `p_prior_to` | income then expense lines with prior amounts | F3 (FR-013 – FR-016) | 5d |
| `report_investment_returns` | **unspecified** | **unspecified** | F5 "More" (FR-034 – FR-036) | 5f — gated |
| `report_tax_summary` | `p_from`, `p_to` | lines grouped by `tax_section` | F5 "More" (FR-037) | 5f |

`report_investment_returns` is intentionally blank. Its shape follows from the gating decision record
(research R8); writing a signature here would be inventing the answer that record exists to give.

---

## Equivalence assertions

Three assertions tie the new functions to what already ships. Each is a real test, not a comment.

| Assertion | Why | Where |
|---|---|---|
| `report_period_summary(month)` equals `v_month_summary` for that month | The new path must not quietly disagree with the figures D1 already shows the user | 5a, Sec step |
| `report_category_breakdown(month)` equals `v_category_spend` for that month | Same reason, for per-category figures | 5a, Sec step |
| `report_balance_sheet(today)` net worth equals `v_net_worth_by_sector` net worth | FR-020 / SC-002, and the reason `v_latest_valuation` is not refactored (research R4) | 5c, Sec step |

These run against the dev Supabase project, not as JVM unit tests — they are assertions about SQL
agreeing with SQL, and a Kotlin fake would prove nothing about either.