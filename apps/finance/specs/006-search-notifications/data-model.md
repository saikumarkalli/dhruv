# Data model: Search & Notifications (Phase 6)

Phase 1 output. Three parts: what this phase **adds** (one device-local table and two enum sets),
what it **reads** (existing tables, through two functions), and what it deliberately **does not add**.

---

## 0. What this phase does not add

Stated first, because it is the most load-bearing fact in this document.

- **No new Postgres table.** Not for alerts, not for search, not for dedupe state.
- **No new RLS policy.** Both new functions are `security invoker`; the policies already on
  `holdings`, `transactions`, `budgets`, `goals`, `policies` and `liabilities_meta` do the isolation.
- **No new column on any existing tracker table.** In particular, no `last_notified_at` — see
  research R1 and R4.
- **No write path to Postgres at all.** This phase is read-only against the user's financial records.

The Postgres delta is exactly two functions plus their `execute` grant to `authenticated`.

---

## 1. `alert_log` — device-local, Room, new

The record of every alert this device has raised. Lives in the existing `AppDatabase`, taking it from
**version 5 to version 6** via `MIGRATION_5_6` (a plain `CREATE TABLE` plus two indexes; nothing
existing is touched, nothing is dropped).

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | INTEGER PK autoincrement | no | Room surrogate |
| `alert_key` | TEXT | no | **unique** — the dedupe key, grammar fixed in `contracts/alert-pipeline.md` |
| `type` | TEXT | no | one of the `AlertType` constants below; append-only |
| `subject_id` | TEXT | no | the id of the record the alert is about; the deep-link argument |
| `raised_at` | INTEGER | no | epoch millis, UTC. Local calendar grouping is derived at read time |
| `payload_json` | TEXT | no | the structured values the message is rendered from — never a rendered sentence |
| `read_at` | INTEGER | yes | null = unread |
| `displayed` | INTEGER (bool) | no | false when suppressed by the master switch or a denied permission (R7 steps 6–7) |

**Indexes**
- `UNIQUE(alert_key)` — this is what makes "raise once per condition" a database property rather than
  application discipline (research R4).
- `INDEX(raised_at DESC)` — the centre's only read pattern is "newest first, then group".

**Validation**
- `alert_key` must match the grammar `<type>:<subject_id>:<period_token>`; a malformed key is a bug,
  not a recoverable state, and is rejected at insert.
- `payload_json` holds integers and strings only. Every money value is **paise as a JSON integer**
  (Article VII). No decimal string, no float, no pre-formatted currency.
- `raised_at` is set by the device, not by the server — the server reports what is *true*, not when
  the user was told (research R3).

**Retention**: rows with `raised_at` older than **90 days** are deleted, on the same worker pass that
evaluates conditions. Purging a row does **not** free its `alert_key` in any meaningful way — a
condition still true after 90 days is a different period and therefore a different key anyway.

**Privacy**: never synced, never exported, never included in a PDF or CSV. Erased with the app's data
like any other Room content; it is not part of `delete_my_data()` because it never left the device.

---

## 2. Enum sets — TEXT-persisted, append-only from birth (Article IX)

### `AlertType` — 5 constants

| Constant | Channel | Subject | Destination |
|---|---|---|---|
| `BUDGET_BREACH` | `budget_alerts` | category id | `OpenBudget(categoryId)` |
| `INSTALMENT_DUE` | `emi_reminders` | liability id | `OpenLiability(id)` |
| `RENEWAL_DUE` | `renewal_reminders` | policy id | `OpenPolicy(id)` |
| `VALUATION_STALE` | `stale_valuations` | holding id | `OpenHolding(id)` |
| `MONTHLY_SUMMARY` | `monthly_digest` | the closed month | `OpenReports(period)` |

Deliberately absent, with reasons the spec already states: `RECURRING_REVIEW` (its review queue is a
Phase 7 screen), `DAILY_RATES` and `APP_UPDATES` (owned by the currency and app-details modules).
Adding any of the three later is an append, never a renumbering.

### `SearchKind` — 4 constants

`TRANSACTION` · `HOLDING` · `POLICY` · `GOAL`

Fixed by FR-002. The set is what the no-results state names to the user, so it is screen-visible
config as well as a persisted constant — it lives in `SearchConfig.kt`, never inline in the screen.

---

## 3. `finance.search_all` — the search row shape

Full signature and matching rules in [`contracts/search-rpc.md`](./contracts/search-rpc.md). The
returned row:

| Field | Type | Notes |
|---|---|---|
| `kind` | text | a `SearchKind` constant |
| `id` | uuid | the record's own id — the deep-link argument |
| `title` | text | the primary line: description, holding name, policy name, goal name |
| `subtitle_a` | text | kind-appropriate: account name, sector, insurer, target date |
| `subtitle_b` | text, null | kind-appropriate second fragment: category, last-valued date, renewal date |
| `amount_paise` | bigint, null | **paise integer**. Null for kinds with no single defining amount |
| `occurred_at` | date, null | the sort key within a kind; null sorts last |
| `is_closed` | boolean | closed account, matured policy, completed goal — labelled, not hidden (FR-009) |
| `kind_total` | integer | `count(*) over (partition by kind)` — the true count, independent of the cap |

**Why `kind_total` is on every row.** The chips must show the real count (FR-003) while the list stays
bounded (NFR-8). A window count gives both from one query; the alternative is a second round trip
whose only job is counting.

**Deleted records are excluded** at the SQL level (FR-009), not filtered on the device — a deleted
record should never cross the wire.

---

## 4. `finance.due_alerts` — the condition row shape

Full signature, per-arm SQL semantics and the dedupe grammar in
[`contracts/alert-pipeline.md`](./contracts/alert-pipeline.md). The returned row:

| Field | Type | Notes |
|---|---|---|
| `type` | text | an `AlertType` constant (never `MONTHLY_SUMMARY` — that arm is device-evaluated, R3) |
| `subject_id` | uuid | the record the condition is about |
| `period_token` | text | the third segment of the dedupe key, computed where period boundaries are known |
| `payload` | jsonb | structured values only — paise integers, integer percentages, ISO dates, names |

**No rendered text crosses this boundary.** `payload` for a budget breach is
`{"category":"Dining","spent_paise":940000,"budget_paise":840000,"days_left":9}` — the device turns
that into a `strings.xml` sentence, applies privacy masking, and renders it differently in the shade
than in the centre. A server-formatted sentence could do none of those three things (research R3).

---

## 5. What is read, and from which phase

| Read | From | Arm / feature |
|---|---|---|
| `transactions` (description, counterparty, `amount_paise`, `occurred_at`), `categories` | Phase 3 | search; the budget arm's spend |
| `holdings`, `v_latest_valuation` | Phase 2 | search; the valuation-stale arm |
| `goals` (name, target, progress) | Phase 4 | search |
| `policies` (name, insurer, renewal date, `remind_days_before`) | Phase 4 | search; the renewal arm |
| `budgets` (`alert_pct`, period, limit) | Phase 4 | the budget arm |
| `liabilities_meta` (`debit_day`, instalment fields) | Phase 2 | the instalment arm |
| The monthly-summary preference | Phase 5 (device-local settings) | the summary arm — no RPC |
| `notifications_master`, hide-amounts, app-lock state, permission state | Settings (004, device-local) | the suppression ladder |

`budgets.alert_pct` and `policies.remind_days_before` are the two columns Phase 4 wrote with nothing
reading them (its research R8). This phase is that reader, and closes `PLN-FLOW-003`.

---

## 6. Alert lifecycle

```
condition true (server) ── or ── month closed + preference on (device)
        │
        ▼
   dedupe key formed  ──── key already in alert_log ──▶ dropped, silently
        │ new
        ▼
   row inserted in alert_log          ← the alert now EXISTS, and the centre shows it
        │
        ├── suppression ladder steps 6–7 fail ──▶ displayed = false, never posted
        │                                          (still in the centre — FR-016)
        ▼
   posted to its channel
        │
        ├── opened ──▶ read_at set ──▶ NavTarget dispatched (held first if locked)
        └── ignored
        │
        ▼
   raised_at older than 90 days ──▶ purged
```

Two properties are worth naming because they are easy to get backwards:

- **Insertion is the moment of existence, not posting.** A suppressed alert is a real alert that was
  not shown. This is the direct consequence of the spec's clarification that the centre is the app's
  own record.
- **Read state is device-local and terminal-ish.** "Mark all read" sets `read_at` on every unread row;
  nothing sets it back. There is no unread-again path, because there is no second device to
  re-deliver from (research R1).

---

## 7. Migration

`MIGRATION_5_6` creates `alert_log` and its two indexes. It is additive: no existing table is
altered, no data is moved, and the migration is reversible by dropping one table. It follows the
existing style in `AppDatabase.kt` — an explicit `Migration(5, 6)` object with raw `execSQL`, matching
`MIGRATION_3_4` and `MIGRATION_4_5`, and registered in the same `addMigrations` chain.

**Testing note.** The DAO is exercised through a **fake**, not an in-memory Room database —
Robolectric's SQLite does not load on this project's Windows toolchain, a constraint already recorded
against the regression suite. The migration itself is verified on device at the sub-phase checkpoint,
not in the JVM gate.