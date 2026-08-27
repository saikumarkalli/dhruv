# Quickstart: Search & Notifications (Phase 6)

How to prove each sub-phase actually works. One section per sub-phase, each ending in a checkpoint
that is the merge gate. Nothing here duplicates the contracts — see
[`search-rpc.md`](./contracts/search-rpc.md), [`alert-pipeline.md`](./contracts/alert-pipeline.md)
and [`routes.md`](./contracts/routes.md) for the shapes being verified.

---

## Prerequisites

**Phases 2, 3, 4 and 5 must have shipped.** Every record search returns and every preference an alert
reads belongs to one of them. Attempting 6a against a database with only `holdings` and `valuations`
(today's shape) produces a function that compiles and returns nothing.

```bash
# JAVA_HOME must point at the Android Studio JBR (JDK 17+)
./gradlew regressionCheck                    # baseline green before starting
```

Database work is authored declaratively and diffed, never hand-written into `migrations/`:

```bash
# edit supabase/schemas/finance/30_functions/<name>.sql, then
supabase db diff -f <name>                   # generates the migration
python scripts/db/gen_schema_docs.py equiv   # schemas/ ≡ migrations/ — the ADR-0032 guard
python scripts/db/gen_schema_docs.py docs --check
```

Session data: sign in on a device with the tracker consent switch **on**. Every check below except
the offline and signed-out states requires a live session.

---

## 6a — Global search (B3)

**Prove the function first, the screen second.**

```bash
supabase db reset                            # loads seed.sql fixtures (dev only)
./gradlew :apps:finance:data:testDebugUnitTest --tests "*Search*"
./gradlew :apps:finance:app:testDebugUnitTest --tests "*Search*"
```

| Check | How | Expected |
|---|---|---|
| Counts are true under a cap | Seed 40 transactions matching one substring, call with `max_per_kind = 25` | 25 rows returned, `kind_total = 40` on each; the chip reads 40 |
| All four kinds | Seed one holding, transaction, policy and goal sharing a substring | Four groups, four chips plus the total |
| Each result opens its own screen | Tap one of each on device | Holding detail, transaction detail, goal detail, policy detail — `SRC-FLOW-001` |
| Deleted excluded, closed included | Soft-delete one record, close another | Deleted absent; closed present and labelled |
| Amounts are not matched | Search a number that exists as an amount | Nothing-matched state, naming the four kinds searched |
| Short query | Type one character | The "type to search" state, **not** the nothing-matched state — these are different |
| Five screen states | Sign out; disable network; withdraw consent; turn the `search` flag off | `SignedOutCard`, `OfflineStateCard`, the consent state, `FeatureDisabledCard` — no spinner in any of them |
| No call before consent | Withdraw consent, search, inspect the network log | Zero PostgREST requests |

**Checkpoint 6a**: `regressionCheck` green · floor ratcheted to the measured value · `SRC-UI-001` and
`SRC-FLOW-001` closed · the `search` flag and its registry row landed together.

---

## 6b — Notification centre (B2)

The centre ships **empty** — nothing raises alerts yet. That is the point: it is provable by seeding
the fake, and the first real alert lands in a screen that already works.

```bash
./gradlew :apps:finance:data:testDebugUnitTest --tests "*AlertLog*"
./gradlew :apps:finance:app:testDebugUnitTest --tests "*NotificationCentre*"
```

| Check | How | Expected |
|---|---|---|
| TODAY / EARLIER grouping | Seed alerts today and on three earlier days, including one just before local midnight | Two groups by **local** calendar date, newest first — `SRC-UI-002` |
| Read state survives restart | Mark all read, kill the process, reopen | Zero unread — `SRC-FLOW-002` |
| Unread count | Seed 3 unread | The top-bar badge reads 3; marking all read clears it |
| Retention | Seed a row at 91 days old, run the purge | Row gone; a row at 89 days remains |
| Suppressed alerts still listed | Seed a row with `displayed = false` | Present in the centre — the FR-016 guarantee |
| Empty state | Empty store | `EmptyStateCard`, no sample content |
| Migration | Install over a v5 build | Opens, calculator history and currency cache intact, `alert_log` present |

> The DAO is tested through a **fake**, not in-memory Room — Robolectric's SQLite does not load on
> this Windows toolchain. The migration itself is verified on device here, not in the JVM gate.

**Checkpoint 6b**: `regressionCheck` green · `SRC-UI-002` and `SRC-FLOW-002` closed · the retention
row written and closed · `DayGroupHeader` present in `:libs:core` (inherited from Phase 3 or built
here) · `CountBadge` **extended**, not duplicated.

---

## 6c — Pipeline and the first arm (budget breach)

**Verify the dependency before writing the arm.**

```bash
./gradlew :apps:finance:app:assembleDebug    # androidx.work on AGP 9 — pass/fail, explicitly
./gradlew :apps:finance:data:testDebugUnitTest --tests "*AlertKey*"
./gradlew :apps:finance:app:testDebugUnitTest --tests "*Suppression*"
```

| Check | How | Expected |
|---|---|---|
| The ladder | Table test over all seven branches | Steps 1–5 not raised; 6–7 raised, recorded, not displayed |
| First breach | Push a category past its stored `alert_pct` | Exactly one alert, naming the category and its position |
| No duplicate | Record more spending in the same period, run three more passes | Still one alert |
| Recurrence | Roll into the next budget period, breach again | A second alert — different `period_token` |
| Missed window | Skip five daily passes, then run one | One alert, not five — the R5 property |
| Deep link | Tap it, cold start | Budget detail (E3), Plan tab beneath, back returns to Plan — `SRC-FLOW-003` budget leg |
| Locked path | App lock on, tap the alert | Authentication first, then the budget detail; destination not lost |
| Deleted subject | Delete the budget, tap the alert | Not-found state, no crash |
| Consent in the background | Withdraw consent, force a worker pass | Zero PostgREST requests, nothing raised |
| Flag kill switch | Set `alerts` disabled | Worker cancelled; no pass runs |

**Checkpoint 6c**: `regressionCheck` green · `SRC-FLOW-003` budget leg closed · **`PLN-FLOW-003`
closed** — the row Phase 4 deferred here, citing research R8 · the dedupe and missed-window rows
written and closed · `POST_NOTIFICATIONS` declared and its request path exercised on API 33+ and on
API 26.

---

## 6d — Obligation arms

| Check | How | Expected |
|---|---|---|
| Instalment reminder | Liability with a due date, advance to its window | One reminder, **name and date only, no amount** |
| Renewal reminder | Policy with `remind_days_before`, advance to the offset | One reminder, name and renewal date |
| Settled obligation | Mark it paid before the date, advance | Nothing raised |
| Quick action | "Mark paid" from the shade | Same result as in-app; verified by reading the record, not the toast |
| Deep links | Tap each | Liability detail (C7), policy detail (E8) — `SRC-FLOW-003` EMI and renewal legs |

**Checkpoint 6d**: `regressionCheck` green · both `SRC-FLOW-003` legs closed · one quick-action path,
not two.

---

## 6e — Periodic arms

| Check | How | Expected |
|---|---|---|
| Valuation stale | Age a holding's last valuation past the window | One alert, names and age, **no amounts** |
| Monthly summary on | Preference on, roll into a new month | One summary, opening the report for **that** month |
| Monthly summary off | Preference off, roll over | Nothing raised |
| No server arm for the summary | Inspect `due_alerts` output | It never returns `MONTHLY_SUMMARY` — device-evaluated by design |

**Checkpoint 6e**: `regressionCheck` green · both new rows closed · the monthly-summary preference
Phase 5 stored is read without a migration or a re-prompt.

---

## 6f — Controls, masking, closure

The verification sub-phase. Nothing new is built; everything is swept.

| Check | How | Expected |
|---|---|---|
| One control per channel | Walk all of Settings | Five controls, each in its owning module's entry, zero duplicates, zero orphans |
| Per-type isolation | Turn each control off in turn | Only that type stops |
| Master switch | Turn it off | Nothing displayed for any type; alerts still recorded |
| Permission denied | Deny at system level | 004's banner appears with a route to system settings; no re-prompt loop |
| Masking sweep | Privacy mode on, one alert of each type | No amount legible anywhere — shade or centre; percentages, counts and dates readable |
| Masking floor | Privacy mode **off**, EMI and renewal alerts | Still no amounts — the rule is a floor, not a toggle |

```bash
./gradlew regressionCheck                    # final gate
```

**Checkpoint 6f — the phase gate**: `regressionCheck` green · coverage floor ratcheted, never
regressed · every `SRC-*` row and every row written during 6a–6e **closed or explicitly deferred with
a stated reason** · Sec pass done (this phase adds one permission and one background network caller —
both in scope for it) · the two flags, the two registry route rows and the two new intent-registry
rows all landed · `platform/versions.json` minor bumped by CI from the `feat:` commits.