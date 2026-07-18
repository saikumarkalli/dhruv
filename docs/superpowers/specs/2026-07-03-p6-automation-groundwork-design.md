# P6 — Automation Groundwork (manual → automatic ingestion)

> Status: **SPECCED** (research-heavy; each item ships behind its own ADR + consent).
> Umbrella: `2026-07-03-tracker-roadmap-overview.md`. Design system + playbook binding.
> Inherits all shared invariants incl. P1 §4.4 error taxonomy and auth/session/consent handling.
> Hard rule: every NEW data source gets its own DPDP consent screen (`ConsentGateScaffold`) — the
> P1 account consent does NOT cover new processing purposes.
>
> **UI/UX**: review inbox = list of parsed-transaction cards (`BentoCard`: parsed amount/merchant/
> date + source badge) with per-row Confirm/Discard + bulk actions; auto valuations carry an "AUTO"
> badge in P1 history lists; per-source consents + kill switches grouped in Settings → "Automation".
> **Rollout**: each item behind its own flag; disabling a source stops its worker and hides its
> inbox; parsed-but-unconfirmed candidates stay device-local only and purge on disable.

## Goal

Reduce manual entry: parsed transactions, automatic valuations, live multi-device updates.
No new tracked domain — P6 feeds the existing P1–P5 features.

## Items (each = own mini-spec + ADR before build)

### 1. SMS/notification transaction ingestion (spike first)
- Compare **NotificationListenerService** vs `RECEIVE_SMS` permission. Play policy is hostile to
  SMS permissions; listener is the preferred route. Direct-APK distribution (ADR-0008) softens the
  Play constraint but the decision should survive a future Play launch.
- On-device regex/parser pack for Indian bank + UPI message formats (versioned, updatable).
- Parsed candidates land in a **review inbox** — nothing saved to Supabase until the user confirms
  each transaction. Raw SMS/notification text NEVER leaves the device; only the user-confirmed
  structured row (amount, category, date, merchant) is uploaded.
- Dedicated consent screen for this source (purpose-specific, revocable in Settings, kill switch).

### 2. Account Aggregator evaluation (decision doc only)
- RBI AA ecosystem (Setu / Finvu / OneMoney): cost model, KYC burden, API shape, solo-maintainer
  viability vs ADR-0001 cost-first driver. Output = go/no-go ADR, no code.

### 3. Auto price feeds (mutual funds, gold)
- AMFI NAV feed (MFs) + a metals price API (gold) — daily WorkManager fetch writes valuation
  entries tagged `source = 'AUTO'`.
- Schema migration: `alter table valuation_entries add column source text not null default 'MANUAL'`
  (append-only enum: MANUAL | AUTO). UI badges auto valuations; per-source consent + kill switch;
  auto entries are soft-deletable like any other.
- Trend math unchanged (P1 calculator is source-agnostic).

### 4. Supabase Realtime
- Subscribe to tracked tables for live multi-device updates; replaces refresh-after-mutation.
  Verify free-tier connection limits; graceful degradation to manual refresh.

### 5. Offline cache revisit (U7 review)
- If online-only tracking has proven painful (usage evidence: error rates, airplane-mode
  complaints), design Room mirror + write queue. This intentionally re-opens ADR-0014's
  cloud-primary decision **with data** — new ADR either way.

## Dependencies

All prior phases (feeds their features). Item 3 depends on P1 valuations; item 1 on P2
transactions; items are otherwise independent and individually shippable.

## Risks

- SMS parsing fragility (format drift across banks) → versioned parser pack + review inbox keeps
  the user in the loop; never silent auto-save.
- AA onboarding cost may fail the cost-first driver → evaluation is decision-doc-only by design.
- Realtime free-tier limits → optional enhancement, manual refresh remains the baseline.

## TDD Mandate

> **Test-Driven Development (TDD) is strictly required for this phase.**
> All pure logic, calculators, reducers, and state machines MUST be written with failing tests first, followed by implementation. UI components must be tested for accessibility and rendering states on both Android and Web platforms.

