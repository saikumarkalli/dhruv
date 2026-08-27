# Contract — `finance.search_all`

**What this is**: the single database function behind screen B3. One round trip, four record kinds,
exact per-kind counts.

**Where it lives**: authored declaratively at `supabase/schemas/finance/30_functions/search_all.sql`;
`supabase db diff` generates the executed migration (ADR-0032). Called from
`SearchApi` via `rpc/search_all` on the **consent-gated `dataClient`** — the only PostgREST-capable
client in the app (ADR-0029 §2).

---

## 1. Signature

```sql
finance.search_all(q text, max_per_kind int default 25)
returns table (
  kind         text,      -- 'TRANSACTION' | 'HOLDING' | 'POLICY' | 'GOAL'
  id           uuid,
  title        text,
  subtitle_a   text,
  subtitle_b   text,
  amount_paise bigint,    -- paise, never numeric, never null-as-zero
  occurred_at  date,
  is_closed    boolean,
  kind_total   integer    -- count(*) over (partition by kind) — the TRUE count
)
language sql
stable
security invoker            -- RLS on the underlying tables does the isolation
```

`grant execute on function finance.search_all(text, int) to authenticated;`
No `anon` grant — every tracker call is authenticated (ADR-0029 §3).

---

## 2. Per-kind projection

| Kind | Source | `title` | `subtitle_a` | `subtitle_b` | `amount_paise` | `occurred_at` | `is_closed` |
|---|---|---|---|---|---|---|---|
| `TRANSACTION` | `finance.transactions` | description / counterparty | account name | category name | the transaction amount | `occurred_at` | account closed |
| `HOLDING` | `finance.holdings` + `v_latest_valuation` | holding name | sector | last-valued date | latest value | last valuation date | holding closed |
| `POLICY` | `finance.policies` | policy name | insurer | renewal date | cover amount | renewal date | matured / lapsed |
| `GOAL` | `finance.goals` | goal name | target date | progress share | target amount | target date | completed |

**Rules**
1. Money is **`bigint` paise** in every row (Article VII). A kind with no single defining amount
   returns SQL `NULL`, never `0` — the device renders an absent amount differently from a zero one.
2. `occurred_at` is the within-kind sort key, descending, `NULLS LAST`.
3. Nothing is pre-formatted. No currency symbol, no thousands separator, no relative date. The device
   owns every rendering (design system §10 — all user-visible strings in `strings.xml`).

---

## 3. Matching

```
lower(unaccented is NOT applied) — plain: column ilike '%' || q || '%'
```

4. Matching is **case-insensitive substring** over the human-entered text columns only: the fields in
   `title` and `subtitle_a` above. Not `subtitle_b` when it is a derived date.
5. **Amounts and dates are not matched** (FR-007). The no-results state says so, so a user typing
   `4500` is told what search covers rather than shown a bare empty list.
6. `q` shorter than **2 characters** returns zero rows. The device does not call at all below that
   length; the function enforces it too, so a direct caller cannot table-scan on one letter.
7. `q` is a **bound parameter**, never string-concatenated into SQL by the client. PostgREST's `rpc/`
   path parameterises by construction; the function body concatenates only into the `ilike` pattern,
   where `%` and `_` in user input are literal wildcards — acceptable, since the worst case is a
   broader match over the caller's own rows.

**No new extension.** `pg_trgm` is not installed and is not added: every row is RLS-restricted to one
user, so the scan is over one person's records. If NFR-8 is missed on a realistic data set, the index
is a contained follow-up (research R6).

---

## 4. Exclusions and inclusions

8. **Soft-deleted records are excluded in SQL**, not filtered on the device (FR-009). A deleted record
   never crosses the wire.
9. **Closed, matured and completed records are included**, flagged by `is_closed`, and the screen
   labels them (FR-009). A user searching their own history is not told a real record does not exist.

---

## 5. Counts and caps

10. `kind_total` is `count(*) over (partition by kind)` — the count **before** the per-kind cap. The
    filter chips render from it, so a chip reading `Transactions 41` is true even when 25 rows came
    back (FR-003).
11. `max_per_kind` defaults to 25 and is passed explicitly by the client from `SearchConfig.kt`, never
    hardcoded in a screen (Article V).
12. The "All" chip's number is the sum of the distinct per-kind totals, computed on the device from
    the returned rows. It is never a separate query.

**Why the window count rather than a second call**: a capped list plus a real count is the only
combination that satisfies FR-003 and NFR-8 at the same time. Counting in a second round trip doubles
the latency of the screen's only interaction.

---

## 6. Failure and state mapping

| Condition | Client behaviour |
|---|---|
| Consent off | `ConsentInterceptor` short-circuits before dispatch; screen shows the not-configured / consent state. No call is made |
| No session | `SignedOutCard` (FR-011). No call is made |
| No connectivity | `OfflineStateCard` — there is no cached search index and the screen says so, rather than showing stale results |
| Function error / 5xx | `RetryErrorCard` with a retry, phrased as an action not an exception (design system §10) |
| Empty result | The nothing-matched state, echoing `q` and naming the four kinds searched (FR-008) |
| `q` under 2 chars | The initial "type to search" state — not the nothing-matched state. These are two different states (FR-008) |

---

## 7. Enforcement

| Rule | Enforced by |
|---|---|
| Counts equal the listed results per kind (§5) | Unit test over the mapper with a fixture whose totals exceed the cap — `SRC-UI-001` |
| Each kind opens its own detail screen | Dispatcher test per kind — `SRC-FLOW-001` |
| Money is paise `bigint` end to end | `checkTrackerMoneyPrecision`, plus a DTO test asserting `Long` |
| Deleted excluded, closed included | SQL-level test at the QA step against seeded fixtures |
| No call before consent | A test asserting `SearchApi` is constructed only from the consent-gated client |
| All five screen states render | Screen-state review at the 6a checkpoint (design system §7) |