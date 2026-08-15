# R6 — Budget Overrun & EMI Due Alerts

> Status: **SPECCED** (rides the P4 insurance phase — same worker/notification pattern; the alert
> evaluator is the only new machinery). Master sequence:
> `../plans/2026-07-12-master-roadmap-personal-app.md` (R6, gap N16). Umbrella + design system +
> playbook binding; inherits all shared invariants.

## Goal

Budgets (P2) currently only report when you look; the actionable half of budgeting is being told
*before* the month is blown. Add local notifications when a category crosses 80% / 100% of its
monthly budget, and EMI-due reminders derived from recurring rules. One daily worker, one
evaluator, zero new screens beyond Settings toggles.

## Non-goals

- No server-side push (everything is a local check — no FCM, no new data egress, no consent gate).
- No new alert domain tables — dedupe state is device-local (DataStore).
- No spending *forecasts* ("on pace to exceed" predictions) in v1 — threshold crossings plus the
  simple remaining÷days pace line (PG3 below); no projection models.
- EMI reminders come **only** from `recurring_rules` with `category = EMI` (R5b). No schema change
  to P3 `payoff_plans` (it has no due-day and gaining one would duplicate the recurring-rules
  source of truth). Insurance premium reminders stay P4's.

## Decisions (proposed)

| # | Decision | Rationale |
|---|----------|-----------|
| D1 | Thresholds fixed at **80% and 100%**, warning threshold user-configurable OFF/50/80/90 (default 80) | Two levels cover "slow down" and "stop"; a full custom-threshold editor is complexity without evidence |
| D2 | Dedupe key = `(category, threshold, month_key)` persisted in DataStore | Notify once per crossing per month; survives restarts; auto-resets at month rollover because the key includes month |
| D3 | Budget check is **online-only, silent-skip when offline** | Spend totals live in Supabase; a stale local total would false-alarm. Missing a day of alerts is acceptable; missing = check again tomorrow |
| D4 | EMI-due check reuses the R5b rule snapshot (offline-capable) with offsets 3/1/0 days before due | Same P4 pattern; dates+names only, no amounts in the snapshot beyond what R5b already stores |
| D5 | One worker `FinanceAlertsWorker`, one channel per alert type (`budget_alerts`, `emi_reminders`) | Per-type channels give the OS-level mute granularity users expect |
| D6 | Worker registers in `TrackerSessionTeardown` (R5b/SEC3) — cancelled + state cleared on sign-out/consent withdrawal; every run first checks consent + session and exits silently if either is gone | Alerting from tracker data after consent withdrawal is a DPDP violation |
| D7 | Notification quick actions (PG10) are `PendingIntent.getActivity` **only** — never a BroadcastReceiver write path (SEC7); they route through MainActivity → lock hold-and-dispatch → ReviewInbox confirm | Writes stay inside the lock gate and FeatureHost error isolation |

## Architecture

| Piece | Detail |
|-------|--------|
| Pure logic (TDD) | `BudgetAlertEvaluator.evaluate(spentByCategory, budgets, alreadyNotified, warnThreshold): List<Alert>` — all threshold/dedupe/rollover logic; `EmiReminderEvaluator.evaluate(rules, today, offsets, alreadyNotified): List<Reminder>` |
| `:apps:finance:data` | `IAlertStateStore` (DataStore-backed notified-keys set, month-scoped pruning); spend rollup reuses P2's `MonthlyRollupCalculator` inputs via `ITransactionRepository` (current-month fetch) |
| `:apps:finance:app` | `FinanceAlertsWorker` — daily periodic (R4 scheduler pattern): budget part (D3 online), EMI part (D4 snapshot); posts via existing notifier util; channels + Settings toggles |
| Settings | "Alerts" block in Settings: budget alerts on/off + warning-threshold selector (D1); EMI reminders on/off; both honor `POST_NOTIFICATIONS` state with the R4-established re-request row |
| Notifications | Budget: "Food at 84% of ₹8,000 budget" (privacy mode ON ⇒ notifier reads `hideAmounts` and drops/masks ₹ via `MaskedMoney.mask()` — R3/F3 mechanism; percentages stay, per the R3 carve-out); tap → `BudgetScreen`. EMI: "Home loan EMI due tomorrow" → ReviewInbox (the rule's candidate is there) |
| Flag | Rides `expenses` + `recurring` flags — no new flag; each evaluator is inert when its source feature is disabled |

## PO additions (2026-07-12 review — PG3/PG4/PG5/PG10; same phase, same worker)

| Item | Detail |
|------|--------|
| **PG3 Safe-to-spend pace** | `SpendPaceCalculator` (pure, TDD): per-category and overall `remaining ÷ remaining IST days`; pace line on BudgetScreen rows ("₹470/day for 20 days"); daily-pace chip on Home card 4. No storage, no worker involvement — computed at render |
| **PG4 Upcoming view** | `UpcomingScreen` (this feature-set, route under expenses): merged 7/30-day timeline — recurring occurrences (`RecurrenceEngine` over the R5b snapshot) + insurance renewal dates (P4 snapshot) + committed-total header (`MoneyText`); rows read-only, deep-link to owners (ReviewInbox / PolicyDetail); entry: "N due this week" line on Home card 4 + `OPEN_UPCOMING` intent extra |
| **PG5 Stale-valuation nudges** | `StalenessEvaluator` (pure, TDD): assets whose latest valuation age > threshold (Settings: 60/90 days, default 60; CASH category exempt); worker branch posts monthly "3 assets need a value update" (channel `stale_valuations`, dedupe once per asset per month via `IAlertStateStore`); HoldingList rows show warning-role tint on the "updated N months ago" line past threshold; notification → holding → valuation sheet |
| **PG10 Notification quick actions** | EMI/renewal reminders gain **"Mark paid"** action → confirms the rule's candidate through the existing ReviewInbox confirm path (same validation, pre-filled) and dismisses; recurring-review notification gains **"Confirm all"**. Actions require unlock if R3 app lock enabled (PendingIntent → activity → lock hold-and-dispatch); no new write paths |

Settings additions: staleness threshold selector under Alerts; Upcoming needs no toggle (screen,
not notification).

## Tests

`SpendPaceCalculatorTest` (month boundaries IST, day 1, last day, zero remaining, negative =
overspent shows ₹0/day + overspent copy); `StalenessEvaluatorTest` (threshold edge, CASH exempt,
dedupe month key, no valuations = onboarding case excluded); `UpcomingScreen` ViewModel merge test
(ordering, empty, one source offline → partial banner); quick-action intent routing test.

`BudgetAlertEvaluatorTest`: crossing 79→80 fires warn once, 100 fires stop once, both crossed in
one jump fires both, month rollover re-arms, custom warn threshold, OFF suppresses warn but not
100%, budget edited upward un-crosses (no re-fire until re-crossed — dedupe key persists).
`EmiReminderEvaluatorTest`: offsets 3/1/0, paused/deleted rules excluded, catch-up day skipped (no
stale "due yesterday" spam — past-due excluded, ReviewInbox already owns overdue).
Worker thin; ArchUnit green.

## Dependencies

P2 (budgets, rollup), R5b (rules + snapshot + ReviewInbox deep-link), R4 plumbing (worker,
channels, permission), R3 (privacy-mode masking in notification copy). Ships alongside P4 —
shares its PR-train but is a separate PR.

## UI/UX detail

| Surface | Layout & states |
|---|---|
| Settings > Alerts | Two switch rows + threshold selector; disabled-with-explainer when notifications permission denied (link to system settings), matching R4 pattern |
| `BudgetScreen` | Rows already show progress; crossed categories additionally get the error-container tint P2 defines — no new UI |
| Notifications | Low-importance for 80%, default-importance for 100% and EMI-due; grouped per channel |

## Rollout & rollback

No new flag (rides source features); toggles default ON for budget alerts (users who set budgets
want to hear about them), EMI reminders default ON. Rollback = revert PR; DataStore alert-state
keys are harmless residue. Schema: none.

## Risks / open questions

- Notification fatigue → per-type channels (D5) + once-per-crossing (D2) + threshold OFF option.
- D3 means a fully-offline day silently skips budget checks — accepted; worker retries daily.
- Privacy-mode users lose amounts in notification copy by design (R3 masking) — copy written to
  stay meaningful without numbers.

## TDD Mandate

> **Test-Driven Development (TDD) is strictly required for this phase.**
> All pure logic, calculators, reducers, and state machines MUST be written with failing tests first, followed by implementation. UI components must be tested for accessibility and rendering states on both Android and Web platforms.

