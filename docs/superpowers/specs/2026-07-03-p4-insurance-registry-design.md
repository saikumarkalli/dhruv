# P4 — Insurance Registry & Renewal Reminders

> Status: **SPECCED** (parallelizable with P3 — depends only on P1 patterns).
> Umbrella: `2026-07-03-tracker-roadmap-overview.md`. Design system + playbook binding.
> Inherits all shared invariants incl. P1 §4.4 error taxonomy and auth/session/consent handling.

## Goal

One registry of every policy (life, term, health, vehicle, home, travel) with coverage totals and
local renewal reminders. Introduces the app's first background work (WorkManager) and the
`POST_NOTIFICATIONS` runtime-permission flow.

## Server schema

```sql
create table policies (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  type text not null,                -- LIFE | TERM | HEALTH | VEHICLE | HOME | TRAVEL | OTHER
  insurer text not null,
  policy_ref_masked text not null default '',   -- masked only (e.g. last 4); full number never stored
  sum_assured_paise bigint not null,
  premium_paise bigint not null,
  premium_frequency text not null,   -- MONTHLY | QUARTERLY | HALF_YEARLY | YEARLY
  start_date date not null,
  renewal_date date not null,
  nominee text not null default '',
  notes text not null default '',
  created_at timestamptz not null default now(),
  is_deleted boolean not null default false
);
-- RLS: standard "own rows".
```

Security: policy reference stored masked-only; never logged, never in crash breadcrumbs.

## Feature module — `:apps:finance:feature:insurance`

| Piece | Detail |
|-------|--------|
| Screens | `PolicyListScreen` (grouped by type; headline: total life cover / total health cover); `PolicyEditorSheet`; `PolicyDetailScreen` (premium schedule, days-to-renewal, nominee) |
| ViewModels | `InsuranceViewModel`, `PolicyEditorViewModel` |
| Pure logic (TDD) | `CoverageSummaryCalculator` (totals per type); `RenewalScheduler` (next-due date math incl. frequency rollover, reminder offsets) |
| Repository | `IPolicyRepository` in `:data` |
| Flag | `"insurance"` |
| Home bento card | Total cover + next renewal countdown |

## Reminders (background work)

- WorkManager periodic daily check (no CONNECTED constraint — reads cached local snapshot of
  renewal dates persisted at last successful fetch; cloud-primary caveat documented).
- Local notifications at 30/7/1 days before `renewal_date`; tap deep-links to `PolicyDetailScreen`.
- `POST_NOTIFICATIONS` runtime permission flow; denial → in-app renewal badges as fallback.
- Reminder preferences (on/off, offsets) in Settings.

## Tests

`RenewalSchedulerTest` (frequency rollovers incl. month-length edges, offset scheduling, past-due);
`CoverageSummaryCalculatorTest`; ViewModel tests with fakes; WorkManager worker logic kept thin and
delegated to tested pure functions; ArchUnit green.

## Dependencies

P1 only (auth/consent/patterns). Independent of P2/P3 — can ship in parallel after P1.

## UI/UX detail (states per design system; components from `:libs:core`)

| Screen | Layout & states |
|---|---|
| `PolicyListScreen` | Coverage headline `HeroStatCard` (total life + health cover compact); type-grouped list (`BentoCard`: insurer, type chip, sum assured, renewal countdown badge — error tint < 30 days); FAB add; empty/loading/offline/error standard |
| `PolicyEditorSheet` (`DhruvModalSheet`) | Type dropdown, insurer, masked policy ref (last-4 only helper text), sum assured ₹, premium ₹ + frequency dropdown, start/renewal dates, nominee, notes; validation: insurer, sums > 0, renewal ≥ start |
| `PolicyDetailScreen` | Header card, premium schedule rows (next 4 dues from `RenewalScheduler`), days-to-renewal, nominee; actions edit/delete (confirm) |
| Home bento card | Total cover compact + next renewal countdown; tap → PolicyList |

Notification: tap → deep-link PolicyDetail; channel "Renewal reminders"; settings toggle + offsets.

## Rollout & rollback

Flag `insurance` kill-switch → card hidden + `FeatureDisabledCard`; WorkManager job cancelled on
disable. Schema additive (1 table). Local renewal snapshot (dates + names only) documented as the
minimal offline exception (P4 ADR note).

## Risks / open questions

- Notification permission denial → fallback badges (accepted).
- Premium double-count with P2 expenses → `INSURANCE_PREMIUM` category convention (documented in
  both features' helper text).
- Renewal reminder needs renewal dates available offline → small non-sensitive local snapshot
  (dates + policy names only, no amounts) — deliberate, minimal exception to cloud-primary,
  recorded in the P4 PR's ADR note.

## TDD Mandate

> **Test-Driven Development (TDD) is strictly required for this phase.**
> All pure logic, calculators, reducers, and state machines MUST be written with failing tests first, followed by implementation. UI components must be tested for accessibility and rendering states on both Android and Web platforms.

