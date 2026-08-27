# Research: Search & Notifications (Phase 6)

Phase 0 output. Each entry is a decision the plan depends on, its reasoning, and what was rejected.
Nothing here restates the spec; this is only the *how*, and only where the how was not obvious.

---

## R1 — The alert log is device-local Room, not a Supabase table

**Decision.** Every alert this app raises is recorded in a new Room table, `alert_log`, in the
existing `AppDatabase` (v5 → v6). It is never synced, never uploaded, and never appears in Postgres.

**Rationale.** ADR-0014 confines Supabase to *tracker entities* — the user's financial records. An
alert is not a financial record; it is a delivery receipt for something the records already say. Four
concrete consequences follow from keeping it local, and every one of them is a cost avoided:

1. **No new table means no new RLS policy and no new grant.** This phase's entire Postgres footprint
   stays at two functions.
2. **No new DPDP erasure obligation.** `public.delete_my_data()` is documented as spanning "all
   tracker tables"; a server-side notification table would have to be added to it, and would then be
   a second thing that must stay in step as apps are added. Local data is erased with the app.
3. **No background write path into the user's financial records.** A worker that writes "last
   notified at" columns onto `budgets` and `policies` is a background job mutating tracker rows with
   nobody watching. Reading in the background is already a meaningful trust surface; writing is a
   larger one, taken for no user-visible gain.
4. **The centre reads offline.** The record of what the app already told you should not need a
   network round trip, and should still be readable after consent is withdrawn — the alerts were
   raised while consent was on, and withdrawing it stops future evaluation, not the user's own
   history.

**Consequence, stated rather than engineered away.** Alerts raised on one device do not appear on
another. The spec records this as an assumption. It is the honest trade: cross-device notification
history would require exactly the server-side table this entry rejects, plus a sync path, for a
benefit the maintainer has not asked for.

**Alternatives considered.** *A `finance.notifications` table* — rejected for the four reasons above.
*No log at all, deriving the centre from current conditions* — rejected in the spec's clarification;
it makes today/earlier grouping and mark-all-read meaningless and contradicts `SRC-UI-002` and
`SRC-FLOW-002`.

---

## R2 — Evaluation runs on-device on WorkManager, at a daily cadence

**Decision.** A single `PeriodicWorkRequest` with a one-day interval and a `CONNECTED` constraint,
plus a one-shot evaluation on app foreground so a user who opens the app is never told stale news.
Enqueued and cancelled by the `alerts` flag. New dependency: `androidx.work:work-runtime-ktx`, with
`androidx.work:work-testing` for tests.

**Rationale.** WorkManager is already the platform's named background mechanism (`PLATFORM.md` §3
and §5 both specify it), so this is not a stack choice being reopened — it is the first phase that
needs it. It is a plain AndroidX library with **no Gradle plugin**, which places it in the same risk
class as `credentials` and `googleid` (accepted in ADR-0029) rather than the plugin-compatibility
class that ruled out Hilt (ADR-0010) and Kover (ADR-0013). WorkManager also handles exactly the two
things a hand-rolled scheduler gets wrong: surviving reboot, and deferring until the network is
actually available.

**Verification, not assumption.** The first task of 6c adds the dependency and builds. This project
has been burned three times by a dependency that "should" work on AGP 9, so the build check is an
explicit task with its own outcome, and the arm work does not start until it passes.

**Why daily and not hourly.** Every condition this phase evaluates is a date-grained fact: a budget
period, a due date, a renewal offset, a valuation age, a closed month. None of them changes between
09:00 and 10:00 in a way the user needs to hear about within the hour. An hourly job would spend
24× the battery and network to deliver the same five sentences.

**Alternatives considered.** *`AlarmManager` exact alarms* — needs `SCHEDULE_EXACT_ALARM`, which is
a user-visible special-access permission on recent Android and is intended for alarms and calendar
events, not reminders; rejected as both heavier and less honest about what the app is doing.
*Server-side `pg_cron` plus a push service* — rejected on three grounds: no push infrastructure
exists in this stack (`PLATFORM.md` §3's Firebase set is Remote Config, Crashlytics and Performance —
not Messaging); pushing a payload like "Dining is 112% of budget" through a third-party delivery
service moves financial state off-device and creates a DPDP surface this phase otherwise does not
have; and it would make notification behaviour untestable without a live project.

---

## R3 — The server decides *what is true*; the device decides *what to say and whether to say it*

**Decision.** One parameterised function, `finance.due_alerts(as_of date)`, returns the set of
conditions currently true for the calling user — each as a row carrying its type, its subject id, the
components of its dedupe key, and the **structured values** the message needs (paise amounts,
integer percentages, dates, names). It never returns a formatted sentence. The device de-duplicates,
formats, masks and posts.

**Rationale.** Three of this project's own rules point the same way. Article VII forbids money
arithmetic on a tracker write path and the comparison "spend ≥ budget × alert_pct" is exactly that
arithmetic — it belongs where the paise already live. NFR-8 forbids computing on the main thread and
favours pre-aggregated data. And Phases 4 and 5 already implement budget-period and financial-year
resolution in SQL; re-implementing period boundaries in Kotlin would be a second source of truth for
"which month is this budget in", which is precisely the drift ADR-0030 diagnosed for documents.

**Why not formatted strings from the server.** Every user-visible string must live in `strings.xml`
(design system §10). A server-formatted sentence cannot be localised, cannot be masked by the
device's privacy-mode setting, and cannot be re-rendered differently in the centre than in the
notification shade. Returning `{ category: "Dining", spent_paise: 940000, budget_paise: 840000,
days_left: 9 }` lets the device produce all three renderings from one row.

**The one arm that is not server-evaluated.** The monthly summary depends only on "a month has
closed" and a **device-local** preference (Phase 5 stored it as device settings state, not a
Postgres column). It therefore needs no RPC arm at all — the worker raises it locally and only then
resolves the report link. Recording this explicitly stops a future task from inventing a server arm
for it.

**Alternatives considered.** *Five client-side evaluators, one per module repository* — five network
calls per evaluation, duplicated period arithmetic, client-side paise comparison. *One RPC per alert
type* — same duplication of the round trip, with the added problem that the dedupe pass then has to
merge five independently-shaped results.

---

## R4 — The dedupe key is a stable, append-only grammar

**Decision.** Every alert carries `alert_key = "<type>:<subject_id>:<period_token>"`, unique-indexed
in `alert_log`. Raising an alert is an insert-if-absent: a row that inserts is posted, a row that
collides is silently dropped. The per-type period token is fixed in
[`contracts/alert-pipeline.md`](./contracts/alert-pipeline.md).

**Rationale.** This makes "raise once per condition" (FR-023) a database property rather than a
behaviour someone has to remember to implement in each of five arms. It also makes the *recurrence*
rule fall out for free: a new budget period produces a different token, so the same category alerts
again next month without any explicit reset.

**The append-only rule, and why it is stated here.** Changing how a key is formed — adding a field,
reordering, changing a date format — makes every historical key stop matching, and the next
evaluation re-raises every alert the user has already seen and dismissed. That failure is silent,
arrives in bulk, and looks like a bug in the pipeline rather than in a string format. The grammar is
therefore append-only in the same sense as Article IX's TEXT-persisted enums: a new type gets a new
prefix; an existing type's token shape never changes.

**Alternatives considered.** *A `last_notified_at` timestamp per subject* — cannot distinguish "the
same breach continuing" from "a new breach in a new period" without re-deriving period boundaries on
the device, which R3 rejects. *Content hashing the rendered message* — a wording change would re-raise
everything, and the message is deliberately not formed until after the dedupe pass.

---

## R5 — A missed window produces the current state, never a backlog

**Decision.** The worker asks what is true **now**. It never replays days it did not run.

**Rationale.** Combined with R4, this makes FR-027 automatic rather than a special case: a phone that
was off for five days runs one evaluation on return, gets the conditions that are currently true, and
inserts one row per condition — because the four intermediate days would have produced the same keys
and would have collided anyway. The user is told "Dining is at 112%", once, not "Dining crossed 80%"
five times.

**The behaviour this deliberately gives up.** A condition that became true and then false while the
device was off is never mentioned. That is correct: a reminder for a bill that has since been paid is
noise, and a budget that recovered does not need an alert about the week it did not.

---

## R6 — Search is one union function with exact per-kind counts, and no new extension

**Decision.** `finance.search_all(q text, max_per_kind int)` returns a typed union across
transactions, holdings, policies and goals. Each row carries `count(*) over (partition by kind)` as
`kind_total`, so the filter chips show the **true** count even when the returned list is capped.
Matching is case-insensitive substring (`ilike '%' || q || '%'`) over the human-entered text columns
only. No new Postgres extension.

**Rationale for the window count.** FR-003 requires the chip count to equal the results listed, and a
capped result set would otherwise break that the moment someone has more than the cap. Returning the
partition total makes the chip honest and lets the list stay bounded — the alternative is either an
uncapped query or a second round trip purely to count.

**Rationale for no `pg_trgm`.** The only extension installed today is `pgcrypto`. Every row this
function touches is already RLS-restricted to one user, so the scan is over one person's own records
— hundreds to a few thousand rows, not a corpus. A trigram index is real work (a migration, an
extension, index maintenance on every write) bought against a cost that has not been measured. If
NFR-8 is missed on a realistic data set, the index is a contained follow-up; adding it pre-emptively
is the speculative work this project's altitude rule warns against.

**Rationale for text-only matching.** Amounts and dates are deliberately excluded (FR-007), and the
no-results state says so. Amount matching sounds useful and is a trap: `4500` would need to match
₹45.00 and ₹4,500 and ₹4,500.00 and a paise-exact `450000`, and every choice there surprises someone.
Date matching has the same problem in more formats. Both are better served by the ledger's own
filters, which already exist.

**Alternatives considered.** *Four parallel PostgREST queries with client-side merging* — four round
trips, and the counts problem gets worse rather than better. *Postgres full-text search
(`tsvector`)* — stemming and lexeme matching are wrong for this data: a user searching `HDF` wants
`HDFC`, which a substring match finds and a lexeme match does not.

---

## R7 — Suppression is one ordered ladder, evaluated as a pure function

**Decision.** Whether an alert is raised, recorded and displayed is decided by one pure function over
six inputs, in a fixed order:

```
1. `alerts` feature flag off        → nothing happens; the worker is not even enqueued
2. consent withdrawn                → the interceptor short-circuits; no evaluation, nothing raised
3. no session                       → nothing raised
4. the owning module's flag off     → that type is not raised           (FR-024)
5. the module's own alert control off → that type is not raised         (FR-035)
6. app-wide notification master off → raised and recorded, not displayed (FR-033)
7. system permission denied         → raised and recorded, not displayed (FR-016)
```

**Rationale for the split at step 6.** Steps 1–5 mean *this alert should not exist*. Steps 6–7 mean
*this alert exists but cannot be shown right now*. The spec's clarification settled that the centre is
the app's own record, so the second group must still write to `alert_log` — otherwise a user who
denied notification permission opens the centre to a permanently empty screen, which is the failure
that decision exists to prevent.

**Rationale for purity.** Seven boolean inputs and two distinct outcomes is exactly the shape that
gets one branch wrong and is never noticed, because the wrong branch is a *missing* notification.
Extracted as a function it is a table-driven unit test; embedded in the worker it is a device
experiment. This mirrors `appLockState` and `resolveBackAction`, both of which the project already
split decision-from-effect for the same reason.

---

## R8 — This phase adds no new `NavTarget` case

**Decision.** Every destination — for both search results and alerts — is a sealed case an earlier
phase already added. Phase 6 adds zero.

| Destination | Target | Added by | Used by |
|---|---|---|---|
| Holding detail (C3) | `OpenHolding(id)` | Phase 2 | search results, the value-overdue alert |
| Liability detail (C7) | `OpenLiability(id)` | Phase 2 | the instalment-due alert |
| Budget detail (E3) | `OpenBudget(categoryId)` | Phase 4 | the budget-breach alert |
| Goal detail (E5) | `OpenGoal(id)` | Phase 4 | search results |
| Policy detail (E8) | `OpenPolicy(id)` | Phase 4 | search results, the renewal alert |
| Reports at a period (F5) | `OpenReports(period)` | Phase 5 | the monthly-summary alert |

**Why this is worth recording.** Phase 4's route contract added `OpenBudget` with no consumer,
called it out as "the one speculative case, and it is deliberate", and justified it by naming this
exact feature. That bet is now settled: it has a consumer. Equally, Phase 3 declined to add
`OpenTransaction` until something needed it — and search does. **If Phase 3 has not added it by the
time 6a runs, 6a adds it**, as the sealed case plus the registry row in one change, per the registry's
own pairing rule. That is the only possible addition, and it belongs to the search result row rather
than to the alert pipeline.

**Consequence for the intent registry.** Surface registry §3 lists intent extras by producer. This
phase becomes the producer for `OPEN_BUDGETS`, `OPEN_POLICY(id)` and `OPEN_REPORTS(month)`, which are
already listed, and adds rows for the two that are not (the instalment and value-overdue targets).
All of them pass through the app-lock held-intent dispatch unchanged — 004's contract §3 already
specifies exactly this behaviour, including that a second arrival while locked replaces the first.

---

## R9 — Two component gaps, both closed by extension

**Decision.** A symbol search against `libs/core/src/main` on 2026-08-22 confirmed fourteen of the
sixteen components these screens need. Two are missing:

- **`DayGroupHeader`** — design-system §5.2 batch **B4**, owed by Phase 3's ledger, which groups
  transactions by day exactly as the centre groups alerts by TODAY/EARLIER. 6b **verifies it is
  present**; if Phase 3 has not landed it, 6b builds it once in `:libs:core` and Phase 3 consumes it
  rather than the reverse. Either way there is exactly one day-header component.
- **The unread dot** — design-system §5.3 already records this gap by name: *"`CountBadge` — has
  numeric count, 99+ cap; design also draws status-dot variants (success / warning / error /
  accent)"*. It is closed by **extending `CountBadge`**, never by adding a second badge component.
  §5.3's own closing rule says so: *"Closing a §5.3 row means extending the existing component, never
  adding a parallel one."*

**Rationale.** This project has already been burned once by screens written against a component
library that did not exist (ADR-0030's diagnosis of the retired tracker design system). Verifying by
symbol search before planning, rather than trusting the design document, is the direct remedy — and
it is cheap, being one grep loop the design system itself publishes in §13.

---

## R10 — Who owns the notification permission

**Decision.** The Settings phase (004) **displays** the permission state and the app-wide master
switch; **this phase declares `POST_NOTIFICATIONS` in the manifest and owns the runtime request.**

**Rationale.** Reading whether notifications are enabled needs no declared permission, which is why
004 could ship its banner and its master switch without one. Requesting the grant does. Declaring a
runtime permission in a phase that cannot yet post anything would put a permission prompt in front of
a user for a capability the app does not have — the kind of thing that gets an app distrusted. It is
declared in 6c, in the same change that first posts a notification.

**When the request is made.** At the point the user first turns on an alert control, or first opens
the notification centre — not at launch, and not on a cold start before the user has expressed any
interest. On API 26–32 there is no runtime permission at all and channels simply exist, so this is a
version branch rather than a universal path.

**Consequence.** 004's permission-denied banner (its FR-027) becomes reachable for the first time in
this phase, since before it there was nothing to deny. 6f verifies it rather than re-specifying it.