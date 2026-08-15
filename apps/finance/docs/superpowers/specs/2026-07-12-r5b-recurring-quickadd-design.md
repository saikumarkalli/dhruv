# R5b — Recurring Transactions & Quick-Add Surfaces

> Status: **SPECCED** (fast-follow on P2 — needs `transactions` live). Master sequence:
> `../plans/2026-07-12-master-roadmap-personal-app.md` (R5b; gaps N7/N8). Umbrella conventions:
> `2026-07-03-tracker-roadmap-overview.md`; design system + playbook binding. Inherits all shared
> invariants (Supabase REST, paise, soft-delete, FeatureHost, Koin, TDD, P1 §4.4 error taxonomy).

## Goal

Kill the biggest daily friction in P2: predictable money (salary, rent, EMI, SIP) logs itself.
Rules describe the recurrence; a daily worker materializes due occurrences into a **review inbox**;
one tap confirms them into `transactions`. Plus launcher-level quick-add (N8) so ad-hoc entry
beats P2's <5s target from anywhere.

## Non-goals

- No fully-automatic posting in v1 (every generated row passes the review inbox — a per-rule
  `auto_post` toggle is a listed fast-follow once trust is earned).
- No SMS/notification parsing (that is P6, with its own consent).
- No Quick Settings tile in v1 (shortcut covers the need; tile revisit-able later).
- No end-of-month proration or business-day shifting (clamp rule below is the whole calendar
  story for v1).

## Server schema

```sql
create table recurring_rules (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  type text not null,                    -- INCOME | EXPENSE
  category text not null,               -- P2 enums
  amount_paise bigint not null,
  notes text not null default '',
  account_ref uuid references assets(id) on delete set null,   -- ADR-0017
  schedule text not null,                -- MONTHLY | WEEKLY
  day_of_month int,                      -- 1..31, MONTHLY only (31 => clamp to month end)
  day_of_week int,                       -- 1..7 ISO, WEEKLY only
  start_date date not null,
  end_date date,                         -- nullable = open-ended
  last_processed_date date,              -- last occurrence date confirmed OR skipped
  is_paused boolean not null default false,
  created_at timestamptz not null default now(),
  is_deleted boolean not null default false,
  currency text not null default 'INR' check (currency = 'INR')   -- ADR-0018
);
create index on recurring_rules (user_id, is_deleted);
-- RLS: standard "own rows".
```

Candidates are **never persisted server-side** — they are computed from rules on demand. Only
user-confirmed rows are POSTed to `transactions`. `last_processed_date` advances on confirm AND
skip (skip = "acknowledge, don't post").

## Feature module — extends `:apps:finance:feature:expenses`

Recurrence is an expenses-domain concern; a separate module would force `feature → feature` edges.

| Piece | Detail |
|-------|--------|
| Screens | `RecurringRulesScreen` (rule list + pause/edit/delete); `RecurringRuleSheet` (editor); `ReviewInboxScreen` (candidate list, per-row Confirm / Edit-then-confirm / Skip, Confirm-all) |
| ViewModels | `RecurringRulesViewModel`, `ReviewInboxViewModel` |
| Pure logic (TDD) | `RecurrenceEngine.dueOccurrences(rule, from, to): List<LocalDate>` — sole owner of calendar math: monthly clamp (31st → Feb 28/29), ISO weekly, start/end bounds, paused = empty, catch-up across multiple missed periods (device off a month → all occurrences listed) |
| Repository | `IRecurringRuleRepository` in `:data` (same remote-data-source + fake pattern); local **rule snapshot** (names/amounts/dates, non-sensitive-minimal, P4-precedent) persisted at each successful fetch so the worker can evaluate offline |
| Worker | `RecurringCheckWorker` — daily (R4 plumbing, no CONNECTED constraint): snapshot → `RecurrenceEngine` → any due ⇒ notification "N transactions ready to review" (channel `recurring_review`), tap deep-links to ReviewInbox. Confirming requires online (POST) |
| Flag | `"recurring": { "enabled": true, "minVersion": "<release>", "requiresConsent": true }` (P1 account-level consent; flag-consistency only, same note as P2) |
| Home | P2's this-month bento card gains a "N to review" badge when inbox non-empty |

## Consent/session teardown (SEC3 — DPDP)

Sign-out and consent-withdrawal both call a single `TrackerSessionTeardown` (in `:data`): cancels
all tracker-domain workers (this one, R6 alerts, R8 widget refresh), clears all tracker snapshot
stores (rule snapshot here, P4 renewal snapshot, R8 widget snapshot) and alert-state keys.
Processing tracker data after consent withdrawal — including notifying from a stale local
snapshot — is a DPDP violation, not just a bug. Every tracker snapshot store registers in
teardown AND in backup exclusions (roadmap §4 rule).

## Quick-add surfaces (N8)

- `res/xml/shortcuts.xml` static shortcut "Add expense" → `MainActivity` with intent extra
  `com.dhruv.finance.ACTION = QUICK_ADD`.
- MainActivity routes the extra AFTER the R3 app-lock gate resolves (locked ⇒ unlock first, then
  land on QuickAddSheet — extra survives the lock overlay; document in `AppLockController`).
- Same intent path is the contract for the R8 widget button (`QUICK_ADD` is the single deep-link
  action; no URI deep links needed while the app has no web presence).
- Notification tap for review inbox uses a second extra value `REVIEW_INBOX`.

## Tests

`RecurrenceEngineTest` is the heart (TDD): monthly normal/clamp/Feb-leap, weekly ISO boundaries,
start mid-month, end_date inclusive, paused, multi-month catch-up, `last_processed_date`
idempotence (same input twice → no duplicates). `ReviewInboxViewModelTest` (confirm advances rule +
POSTs, skip advances only, confirm-all partial-failure leaves failed rows pending, offline confirm
→ error state). Repository tests with fakes. Worker thin, delegates to engine. Shortcut/intent
routing: Robolectric intent-extra test. ArchUnit green.

## Dependencies

P2 merged (transactions, QuickAddSheet, categories). ADR-0017/0018 (account_ref, INR check).
R4 plumbing (WorkManager, channels, POST_NOTIFICATIONS flow). R3 lock interplay (extra routing).

## UI/UX detail (states per design system)

| Screen | Layout & states |
|---|---|
| `RecurringRulesScreen` | `BentoCard` rows: category chip, amount (`MoneyText`), "Monthly · 1st" schedule line, next-due date, paused badge; FAB add; empty state "Automate your salary and rent"; standard loading/offline/error |
| `RecurringRuleSheet` (`DhruvModalSheet`) | Type toggle, amount keypad, category chips, schedule segmented (Monthly/Weekly), day picker (1–31 grid / weekday row), start date (default today), optional end date, optional account chip (ADR-0017), note; validation: amount > 0, category + day required |
| `ReviewInboxScreen` | Day-grouped candidate `BentoCard`s (rule name, amount, due date, source badge "Recurring"); swipe/buttons Confirm · Edit · Skip; top bar Confirm all; empty state "All caught up"; partial-failure rows keep error chip |
| Notification | "3 transactions ready to review" → ReviewInbox; grouped, one per day max |

## Rollout & rollback

Flag `recurring` kill-switch → rules screens + inbox hidden, worker cancelled; already-confirmed
transactions are ordinary P2 rows (unaffected). Schema additive (1 table). Rollback = flag off /
revert PR; server rules persist harmlessly.

## Risks / open questions

- Trust ramp: review-only v1 might feel like nagging for salary-day users → `auto_post` per-rule
  toggle is the designated fast-follow, gated on zero inbox-bug reports for a month.
- Rule edited between materialization and confirm → candidates always recomputed from the live
  snapshot at inbox open; the notification count may drift from the on-open list (accepted,
  banner explains "updated just now").
- Duplicate risk if user ALSO hand-logs salary → inbox shows a soft warning chip when a same-
  category same-amount transaction exists within ±2 days (heuristic, non-blocking).

## TDD Mandate

> **Test-Driven Development (TDD) is strictly required for this phase.**
> All pure logic, calculators, reducers, and state machines MUST be written with failing tests first, followed by implementation. UI components must be tested for accessibility and rendering states on both Android and Web platforms.

