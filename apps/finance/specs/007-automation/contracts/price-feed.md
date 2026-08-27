# Contract: the price feed

Covers FR-043…FR-046, FR-045a. The only source whose output is not a transaction, and the only one
whose triggering rule needed two requirements reconciled.

## 1. Opting in (FR-044, R4)

A holding participates only when the user marks it auto-valued:

```sql
finance.holdings.auto_value_series text
    check (auto_value_series is null or auto_value_series in ('GOLD_24K','GOLD_22K','SILVER'))
```

Null = the user maintains this value themselves and the feed never touches it. One nullable column,
not a boolean plus a series — a `true` with a null series would be an impossible state the code would
defend against forever.

**Currency is deliberately not a series.** The tracker is INR-only (`value_paise`, currency-less by
ADR-0029), so there is no holding a currency rate could revalue. The converter's existing rate cache
is a different concern and stays where it is. The hub row still reads "gold, silver and currency
rates" as the design draws it, because the *feed* covers them; only the **revaluation** is metals.

## 2. The move predicate — pure, and the heart of this slice (R6)

FR-044a says propose only on a real move. FR-045 says a later genuinely different move must still
propose. FR-045a says the move already refused must not come back. These read as contradictory and are
not — two terms satisfy all three:

```kotlin
fun shouldPropose(
    recordedPaise: Long,
    fetchedPaise: Long,
    lastIgnoredPaise: Long?,
    thresholdPct: Int,
): Boolean
```

```
movedFromRecorded = |fetched − recorded| × 100 ≥ thresholdPct × recorded      // FR-044a
notTheRefusedMove = lastIgnored == null ||
                    |fetched − lastIgnored| × 100 ≥ thresholdPct × recorded   // FR-045a

shouldPropose = movedFromRecorded && notTheRefusedMove
```

**Integer arithmetic throughout** — multiply by 100 and compare, never divide into a percentage
(Article VII; a float here would be the one money bug this phase could plausibly ship).

### Case table — the RED test

Threshold 5%, recorded ₹100.00 (10 000 paise), so the threshold is 500 paise.

| # | recorded | fetched | lastIgnored | propose? | Why |
|---|---|---|---|---|---|
| 1 | 10 000 | 10 200 | null | **no** | Moved 200 < 500. FR-044a |
| 2 | 10 000 | 10 600 | null | **yes** | Moved 600 ≥ 500 |
| 3 | 10 000 | 10 600 | 10 600 | **no** | Exactly the move the user refused. FR-045a |
| 4 | 10 000 | 10 700 | 10 600 | **no** | Moved 100 from the refusal — not a new move |
| 5 | 10 000 | 11 200 | 10 600 | **yes** | 600 past the refusal **and** 1 200 past recorded. FR-045 |
| 6 | 10 000 | 9 400 | 10 600 | **yes** | Reversed through and past the refusal in the other direction |
| 7 | 10 000 | 9 400 | null | **yes** | Downward moves count — `abs`, not signed |
| 8 | 10 000 | 10 000 | null | **no** | Flat forever produces nothing. SC-013a |
| 9 | 10 600 | 10 700 | null | **no** | After acceptance, `recorded` is the new value; re-measured from it |
| 10 | 10 000 | 10 600 | 10 600 | **no**, then case 5 applies later | Ignoring does not suppress the *next* fetch (FR-045), it suppresses *this* move |

Case 5 is the one that proves FR-045 and FR-045a coexist. Case 9 is the one that proves acceptance
resets the baseline.

## 3. Proposal identity (FR-008c interaction)

| Aspect | Value |
|---|---|
| `source_key` | `(holding_id, recorded_value_paise)` |
| Effect of accepting | `recorded` changes ⇒ the next proposal has a **different key**, so FR-008c's never-twice rule does not block it |
| Effect of ignoring | Key unchanged, but `last_ignored_price_paise` is set — the predicate's second term does the suppressing, not the key |
| Outstanding proposals per holding | **One.** A new qualifying move replaces the pending row rather than stacking a near-identical one (edge case: volatile period) |

This is why the data model calls the price case "documented special case" rather than an exception:
FR-008c still holds literally — the same key is never proposed twice — and FR-045 works because a
genuinely new move genuinely has a new key or a cleared refusal.

## 4. The threshold setting (FR-044b)

| Property | Value |
|---|---|
| Default | **5%** |
| Where | This module's Settings entry — Settings › Modules › Automation |
| Store | Device-local settings (004's control plane), not Postgres — it governs a device-local evaluator (R5) |
| Component | `SliderWithPresets` (built, verified) |
| Copy | States what it controls in plain language, per US8-3a |

**5% is a starting value, not a researched number** — the spec says so, and it is stated here so
nobody treats it as load-bearing. Changing the default later is tuning, not a spec change.

## 5. Scheduling and reachability

| Aspect | Value |
|---|---|
| Trigger | The same periodic `androidx.work` schedule as the message scan (FR-044d) — one cadence, not two |
| Unreachable source | The hub row states it (FR-046). **No proposal is produced from a stale price presented as current** |
| Stale pending proposal | Carries `price_taken_at` and shows it, so a price that moved back before the user acted is visibly out of date (edge case) |
| Threshold set unreachably high | Hub row says the source is on and has produced nothing — not "broken" (edge case) |
| Feed off | No proposal produced, no recorded value moves (US8-5) |

## 6. Accepting — an ordinary append-only insert (R3)

```
accept(VALUE_UPDATE) → INSERT finance.valuations (holding_id, value_paise, as_of)
```

No RPC, no privileged path. `finance.valuations` has SELECT + INSERT policies only (ADR-0029 decision
4 / Article IX), so this is exactly the write the table was designed for, and the append-only history
stays intact.

**Corrections are not ours.** A wrongly accepted price is a valuation correction — Phase 2's
`finance.correct_valuation()`, already authored. This phase adds no second correction path, and the
value-update row carries no edit affordance beyond correcting before accepting.

| Requirement | Behaviour |
|---|---|
| FR-044 | Never changes a recorded value without an accept action |
| FR-045 | Ignoring leaves every affected value untouched |
| SC-013 | Zero values change from the feed without an accept; ignoring leaves 100% untouched |
| Edge case | If the user manually revalued the holding while a proposal was pending, the threshold re-measures from the **new** recorded value — the pending row is recomputed, never applied blind over a hand-set value |
| Edge case | A proposal covering several holdings applies to the survivors if one was deleted; the deleted one is not resurrected |