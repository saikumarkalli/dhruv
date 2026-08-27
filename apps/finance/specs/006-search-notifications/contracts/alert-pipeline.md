# Contract — Alert pipeline

**What this is**: everything between "a condition became true" and "the user tapped the notification
and landed on the right screen". Five pieces: the function that decides what is true, the key that
decides what is new, the worker that runs it, the ladder that decides whether to show it, and the
channels it posts to.

**Where it lives**: `finance.due_alerts` in
`supabase/schemas/finance/30_functions/due_alerts.sql`; everything else in
`:apps:finance:app/alerts/` with the repository and key function in `:apps:finance:data`.

---

## 1. `finance.due_alerts` — the server arm

```sql
finance.due_alerts(as_of date)
returns table (
  type         text,   -- AlertType, never 'MONTHLY_SUMMARY' (that arm is device-evaluated)
  subject_id   uuid,
  period_token text,   -- third segment of the dedupe key, computed where periods are known
  payload      jsonb   -- structured values only
)
language sql
stable
security invoker
```

`grant execute on function finance.due_alerts(date) to authenticated;`

1. The function reports **what is true on `as_of`**, never what was true earlier. It has no memory and
   no state — the device owns "has the user already been told" (research R5).
2. `payload` carries integers, ISO dates and names. **No rendered sentence, no currency symbol, no
   percentage sign** — the device formats and masks (research R3).
3. Every money value in `payload` is a **paise integer** (Article VII).
4. `as_of` is passed by the caller rather than read from `now()` inside, so every arm is testable
   against a fixed date.

### Arms

| Arm | Condition | `period_token` | `payload` |
|---|---|---|---|
| `BUDGET_BREACH` | period-to-date spend ≥ `budgets.limit_paise × budgets.alert_pct / 100`, evaluated within the budget's own period. `alert_pct` null ⇒ arm skipped for that budget | the budget period's start date, `YYYY-MM-DD` | `category`, `spent_paise`, `budget_paise`, `pct`, `days_left` |
| `INSTALMENT_DUE` | a liability's next instalment date falls within its reminder window | the instalment due date, `YYYY-MM-DD` | `name`, `due_on` — **no amount** |
| `RENEWAL_DUE` | `renewal_date - policies.remind_days_before ≤ as_of < renewal_date`. `remind_days_before` null ⇒ arm skipped | the renewal date, `YYYY-MM-DD` | `name`, `renews_on` — **no amount** |
| `VALUATION_STALE` | latest valuation for a holding is older than the configured staleness window | the staleness bucket start, `YYYY-MM-DD` | `names[]`, `count`, `oldest_age_days` — **no amounts** |

`BUDGET_BREACH` and `RENEWAL_DUE` are driven by the two columns Phase 4 wrote with nothing reading
them (its research R8). This function is that reader.

### The device arm

5. `MONTHLY_SUMMARY` is **not** a row from this function. It depends only on "a month has closed" plus
   a preference Phase 5 stored as **device-local settings state**, so there is nothing for the server
   to evaluate. The worker raises it locally with `period_token = YYYY-MM` of the closed month, and
   resolves the report link on tap. Do not add a server arm for it.

---

## 2. Dedupe key grammar — append-only

```
alert_key = "<type>:<subject_id>:<period_token>"
```

6. Unique-indexed in `alert_log`. Raising is insert-if-absent: a row that inserts is posted, a row
   that collides is dropped silently. This makes FR-023 a database property, not a per-arm behaviour.
7. **The grammar is append-only** in the same sense as a TEXT-persisted enum (Article IX). A new type
   gets a new prefix; an existing type's token shape **never changes**. Changing one makes every
   historical key stop matching, and the next evaluation re-raises every alert the user has already
   seen and dismissed — silently, in bulk, and looking like a pipeline bug rather than a format
   change (research R4).
8. Recurrence needs no reset logic: a new budget period, a new due date or a new month produces a
   different token, so the same subject alerts again on its own.

---

## 3. The worker

9. One `PeriodicWorkRequest`, **one-day interval**, `NetworkType.CONNECTED`, unique work name so
   re-enqueue is idempotent. Plus a **one-shot evaluation on app foreground**, so a user who opens the
   app is never told stale news.
10. Enqueued when the `alerts` flag resolves enabled; **cancelled** when it resolves disabled. In the
    background the flag resolves from the cached last-known-good value — a worker must not block on a
    remote-config fetch.
11. Each pass, in order: resolve the ladder (§5) → call `due_alerts(today)` → add the device arm →
    form keys → insert-if-absent → post what inserted → **purge rows older than 90 days**.
12. The worker **never writes to Postgres**. This phase is read-only against the user's records.
13. A failed pass retries with WorkManager's default backoff and raises nothing. A missed window is
    not replayed — the next pass reports what is true then (research R5), which is why a five-day
    outage produces one alert per condition rather than five.
14. Cadence, retention window and the staleness window live in `AlertConfig.kt`, never inline
    (Article V).

---

## 4. Channels

Registered once, idempotently, at first use. Ids, names, importance and masking are the surface
registry §2 rows, unchanged — this contract does not invent channels, it implements the registered
ones.

| Channel id | Name | Importance | Money masking | Quick action | Arm |
|---|---|---|---|---|---|
| `budget_alerts` | Budget alerts | LOW at the sub-100% threshold, DEFAULT at 100% | masked under privacy mode; **% always shown** | — | `BUDGET_BREACH` |
| `emi_reminders` | EMI reminders | DEFAULT | name + date only, **no amounts** | "Mark paid" | `INSTALMENT_DUE` |
| `renewal_reminders` | Renewal reminders | DEFAULT | policy name + date only | "Mark paid" | `RENEWAL_DUE` |
| `stale_valuations` | Value updates due | LOW | asset names + age only, **no amounts** | — | `VALUATION_STALE` |
| `monthly_digest` | Monthly summary | LOW | masked under privacy mode; % stay | — | `MONTHLY_SUMMARY` |

15. **Two action buttons maximum**, per design system §11. No channel here uses more than one.
16. A quick action routes through **the same confirm path as the in-app action** (FR-026) — never a
    parallel write. "Mark paid" from a notification and "Mark paid" from the screen are one code path.
17. Every channel has **exactly one control**, in the Settings entry of the module that owns it
    (surface registry §4, `SET-BR-006`). This phase adds no central alerts list, and 6f sweeps for
    duplicates.
18. `POST_NOTIFICATIONS` is declared by this phase and requested when the user first turns on an alert
    control or first opens the centre — not at launch (research R10). On API 26–32 there is no runtime
    permission and channels simply exist.

---

## 5. Suppression ladder — pure, ordered, table-tested

```
appliesTo(alert) :
  1. `alerts` flag off             → NOT RAISED   (worker not even enqueued)
  2. consent withdrawn             → NOT RAISED   (interceptor short-circuits; no evaluation)
  3. no session                    → NOT RAISED
  4. owning module's flag off      → NOT RAISED                       FR-024
  5. module's own alert control off→ NOT RAISED                       FR-035
  6. app-wide master switch off    → RAISED, RECORDED, NOT DISPLAYED  FR-033
  7. system permission denied      → RAISED, RECORDED, NOT DISPLAYED  FR-016
  otherwise                        → RAISED, RECORDED, DISPLAYED
```

19. **The split at step 6 is the contract's most important line.** Steps 1–5 mean the alert should not
    exist. Steps 6–7 mean it exists but cannot be shown — so it is still written to `alert_log` with
    `displayed = false`. This is what makes the centre complete for a user who denied notification
    permission, per the spec's clarification.
20. The function is **pure and total**: seven booleans in, one of three outcomes out, no clock, no
    context, no I/O. It is a table-driven unit test rather than a device experiment — the same
    decision/effect split `appLockState` and `resolveBackAction` already use.
21. App-lock state is **not** in the ladder. A locked app still posts; only *opening* is gated
    (app-lock-gate contract §4 rule 18).

---

## 6. Rendering and masking

22. Every notification title and body is a `strings.xml` resource with the `payload` values
    substituted. No sentence is ever assembled from server text.
23. Privacy mode masks amounts in **every** alert — shade, centre and any future widget — while
    percentages, counts and dates stay readable (FR-025). Masking goes through the shared formatting
    helpers, which is what makes the notification path inherit it rather than reimplement it (004's
    research R5 names notifications as exactly the path a screen author forgets).
24. A channel whose masking rule already says "no amounts" carries none regardless of privacy mode —
    the rule is a floor, not a toggle.
25. The centre and the shade render the **same row differently** and are allowed to: the shade is one
    line, the centre has room for the subject and the time. Both come from the same `payload`.

---

## 7. Dispatch

26. Every alert opens its own subject through an existing `NavTarget` case — never the centre, and
    never a tab root alone. The mapping is in [`routes.md`](./routes.md).
27. A deep link lands on the owning tab's root, then pushes (N6, FR-029), so back returns to that tab
    rather than exiting.
28. An unknown, foreign or deleted subject id resolves to that record's normal not-found state
    (FR-030). This is more likely here than anywhere else in the app: a notification can outlive the
    record it names.
29. When locked, the target is held and dispatched once after unlock, per app-lock-gate §3. This phase
    is the first real producer of held intents and **verifies** that contract; it does not re-specify
    it.

---

## 8. Enforcement

| Rule | Enforced by |
|---|---|
| One alert per condition; recurrence in a new period | Unit tests over `AlertKey` + a repository test asserting insert-if-absent — new QA row, written before 6c |
| A missed window produces one alert, not a backlog | Unit test running three passes over an unchanged condition — new QA row |
| The ladder's seven branches | Table-driven unit test over the pure function — all of §5 |
| Suppressed alerts still reach the centre | Repository test asserting `displayed = false` rows are returned by the centre query |
| No client-side money arithmetic | `checkTrackerMoneyPrecision` + DTO tests asserting `Long` paise |
| No PostgREST call without consent, **in the background** | A test asserting the worker's repository is constructed only from the consent-gated client |
| Every channel has exactly one Settings control | Settings sweep at 6f — new QA row |
| Per-channel masking | Notification-builder tests per channel, privacy mode on and off — new QA row |
| Each type deep-links to its subject | Dispatcher test per type — `SRC-FLOW-003` |
| The Phase 4 deferral is closed | `PLN-FLOW-003` closed at 6c, citing this contract |