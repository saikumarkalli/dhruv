# BSA / Product-Owner Review — Premium Daily-Use Bar

> Status: **REVIEW COMPLETE** (2026-07-12). Lens: Business Systems Analyst + Product Owner, not
> engineering. Question: *if everything currently specced (P1–P6 + R0–R11) ships, is Dhruv a
> premium daily-driver personal-finance app — or a well-built app you open twice a month?*
> Benchmarks: daily-use behaviors of Money Manager, Walnut/Axio, ET Money, Jupiter, YNAB,
> Monarch, 1Money (feature patterns, not clones — this is a single-user personal app by design).
> Product gaps below are **PG1–PG10** (accepted, mapped to phases — patched into the owning specs
> same day) plus an explicit **Not-Doing** register. Companions: master roadmap (sequence),
> consistency review (engineering gaps), app design standard (UI law).

## Problem statement

**How might Dhruv become the app its owner opens every single day and trusts as the complete,
current picture of his money — not just a beautifully architected tracker he visits on payday?**

## The daily-use loop (what "premium" actually means day-to-day)

| Loop stage | Premium behavior | Plan coverage | Verdict |
|---|---|---|---|
| 1. Glance (morning, 5s) | Widget + one daily signal | R8 widget · R4 rates notification | ✅ covered |
| 2. Capture (many times/day, <5s) | Frictionless add, auto-recurrence | P2 quick-add · R5b shortcut + recurring | ✅ covered |
| 3. Trust (always) | Numbers are *current*, not just stored | ⚠️ nothing keeps manual valuations fresh | 🔴 **gap — PG5** |
| 4. Guide (daily decision) | "Can I spend this today?" forward guidance | ⚠️ budgets are monthly rear-view only | 🔴 **gap — PG3, PG4** |
| 5. Ritual (monthly) | Digest that pulls you back + insight | ⚠️ R7 reports exist but never call you | 🟡 **gap — PG6, PG9** |
| 6. Act (from anywhere) | Do the thing from the notification itself | ⚠️ all notifications are open-app-first | 🟡 **gap — PG10** |

Stages 1–2 are genuinely premium-grade as specced. Stages 3–5 are where the current plan produces
a *monthly* app, not a *daily* one. That is the headline of this review.

## Product gap register (accepted → patched into owning specs)

### 🔴 PG1 — P2 transaction CRUD is capture-only (BSA finding)
P2 specs QuickAdd + list + budgets; **editing, deleting, duplicating an existing transaction is
never stated**. Fat-fingered ₹4,500 → ₹450 on day one with no correction path is a trust killer.
**Disposition:** P2 build requirement — row tap → editor sheet (same QuickAdd sheet, pre-filled),
delete = soft + undo (R8 pattern), "duplicate" row action (yesterday's coffee = today's coffee).
Recorded in the R5 decisions spec (P2 pre-work home).

### 🔴 PG2 — No TRANSFER type: self-transfers corrupt the books
P2's `type` enum = INCOME | EXPENSE only. Moving ₹50k savings→FD, or an ATM withdrawal, must be
logged as fake expense + fake income — inflating both sides and wrecking the savings rate (which
feeds P3 ETAs and P5 defaults — the corruption propagates). Every benchmark app has transfers.
**Disposition:** decide with the other P2 pre-work (R5 decisions spec, new Problem 3 → reserve
**ADR-0022**): add `TRANSFER` to the type enum at P2 creation (append-only-safe), add nullable
`transfer_to_ref uuid` (destination asset) beside `account_ref` (source), and **exclude TRANSFER
from every income/expense rollup** (`MonthlyRollupCalculator` test cases mandatory).

### 🔴 PG3 — No forward guidance: "safe to spend today"
Budgets show `spent/limit` after the fact. The premium daily anchor is pacing: *"₹9,400 left on
Food · ₹470/day for the next 20 days"*. Pure arithmetic over data P2 already has — zero new
storage.
**Disposition:** rides R6 (same rollup inputs as budget alerts): `SpendPaceCalculator` (pure,
TDD — remaining ÷ remaining days, IST month) + pace line on BudgetScreen rows + daily-pace chip
on Home card 4.

### 🔴 PG4 — No "Upcoming" view: the week ahead is scattered across three features
Recurring dues (R5b), EMI reminders (R6), insurance renewals (P4) each notify separately; nowhere
answers *"what's hitting me in the next 7 days and how much?"* — the single most-opened view in
bills-aware apps.
**Disposition:** rides R6: `UpcomingScreen` — merged 7/30-day timeline (recurring occurrences from
`RecurrenceEngine` + renewal dates + committed total at top), read-only rows deep-linking to their
owners; entry: "N due this week" line on Home card 4 + `OPEN_UPCOMING` notification routing.

### 🔴 PG5 — Nothing keeps net worth *true*: stale-valuation nudges
The app's core promise is the net-worth number, but manual valuations rot silently — gold from 8
months ago renders as confidently as yesterday's bank balance. Premium manual trackers surface
freshness. This is the **trust feature** for a manual-first tracker.
**Disposition:** rides R6 worker: `StalenessEvaluator` (pure: assets whose latest valuation >
configurable 60/90 days, excluding categories that don't drift — CASH), monthly "3 assets need a
value update" notification (channel `stale_valuations`) + "updated N months ago" warning tint on
HoldingList rows (design-standard copy rule already shows relative dates — this adds the
warning role at threshold) + one-tap path: notification → holding → valuation sheet.

### 🟡 PG6 — Reports never call you: monthly digest
R7 builds the answers; nothing creates the ritual. **Disposition:** rides R7: 1st-of-month
notification (channel `monthly_digest`) — "June: saved 31% · ₹42k under budget · net worth ▲2.1%"
(respects privacy mode via `MaskedMoney`; % survive per R3 carve-out), tap → Reports on that
month. Built from the same `monthly_summary` RPC — no new data.

### 🟡 PG7 — Loan balances shouldn't need manual valuations (P3 has the math)
P3's `PayoffScheduleEngine` computes the full amortization schedule — yet the user still manually
updates each liability's value monthly. Premium move: liabilities with an active payoff plan get a
**computed current balance** from the schedule (badge "computed"), manual override always wins.
**Disposition:** P3-build decision (reserve **ADR-0023**) — computed balance as a *derived
display* value; valuation rows stay append-only source of truth; a monthly auto-append is
explicitly P6 territory (source='AUTO' machinery).

### 🟡 PG8 — Onboarding ends at one asset; seeding is the real time-to-value
R8 onboarding CTA adds *one* asset. A personal net worth is ~8–12 standard Indian holdings.
**Disposition:** rides R8 slice 1: after first asset, a seed checklist sheet ("what else do you
have?" — Bank · EPF/PPF · Mutual funds · Stocks · Gold · Property · Vehicle · Home loan · Credit
card) where each tap opens a pre-categorized editor; skippable, shown once.

### 🟡 PG9 — Reports without insights are tables: rule-based insight chips
No AI needed: *"Food ▲38% vs 3-month avg"*, *"highest savings rate this year"*, *"2 categories
over budget"*. **Disposition:** rides R7: `InsightRules` (pure, TDD — fixed rule set over the
same RPC rows, max 3 chips, suppressed under 2 months of data) rendered as chips atop
ReportsScreen. On-device, deterministic, testable.

### 🟡 PG10 — Notifications are dead ends: quick actions
Every specced notification opens the app. Premium: act in place. **Disposition:** rides R6/R5b:
EMI/renewal reminder gains **"Mark paid"** action (logs the pre-filled transaction via the
existing confirm path, then dismisses); recurring-review notification gains **"Confirm all"**
action. Both reuse ReviewInbox logic — no new write paths, same validation.

## Verdict

As previously specced: **excellent monthly app, not yet a daily one** — world-class capture and
safety, weak forward guidance and freshness. With PG1–PG10 (all riding existing phases — **zero
new phases, zero new tables, one new column + one enum value**), the plan clears the premium
daily-use bar: every loop stage has an owner.

## Key assumptions to validate (in use, post-P2)

- [ ] Owner actually logs expenses daily when capture is <5s (usage: quick-add count/week ≥ 10) —
  else P6 SMS-ingestion rises in priority.
- [ ] Safe-to-spend pace (PG3) changes behavior (does Food stay under budget more months than
  before?) — else it's a vitamin, demote from Home card.
- [ ] Stale nudges (PG5) at 60 days is the right cadence — tune, don't assume.
- [ ] Monthly digest (PG6) actually gets tapped — if ignored 3 months straight, kill it (it's one
  worker branch).

## Not doing (and why) — the premium bar does NOT require these

- **Custom categories** — collides with the append-only TEXT-enum rule; fixed set is fine for a
  single known user. Revisit only on real friction.
- **Receipt/document attachments** — storage cost + sync surface for marginal single-user value;
  policies already store masked refs.
- **Split transactions** — complexity magnet; notes field covers the rare case.
- **Budget rollover / envelope semantics** — YNAB-grade methodology for a different user type;
  monthly reset is honest and simple.
- **Reorderable Home** — fixed registry (design standard §3.2) with hide toggles is deliberate;
  reorder is customization theater for one user.
- **Bank/AA auto-import** — already correctly parked as P6 decision-doc-only (cost-first driver).
- **Tracker-aware AI assistant** ("how much did I spend on food?") — sends financial rows to
  Google; parked behind a future ADR with its own DPDP consent screen. Not a daily-use blocker.
- **Search filters (date/amount)** — text search covers v1; add on real usage evidence.
- **Shared/family profiles, credit score, Play launch** — out of scope per roadmap §2.

## Traceability

PG1/PG2 → `r5-accounts-multicurrency-decisions` (P2 pre-work; ADR-0022 reserved) ·
PG3/PG4/PG5/PG10 → `r6-budget-alerts` · PG6/PG9 → `r7-reports-export-import` ·
PG8 → `r8-daily-driver-polish` · PG7 → P3 build ADR (ADR-0023 reserved) ·
New channels/intents → app design standard §7 registries. All patched 2026-07-12.
