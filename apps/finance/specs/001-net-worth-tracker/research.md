# Phase 0 Research: Net Worth Tracker (Phase 2)

No items in Technical Context were left as NEEDS CLARIFICATION — this is a brownfield feature
extending Phase 1's already-decided architecture, so "research" here is confirming reuse of
existing decisions rather than evaluating new options from scratch.

## R1 — Networking: extend Phase 1's stack, add nothing new

**Decision**: `LiabilityRepository` uses the same Retrofit + Moshi + OkHttp `dataClient`
(`SupabaseClientFactory`) that `HoldingRepository`/`ValuationRepository` already use.

**Rationale**: ADR-0029 already settled this for the tracker domain (reusing the same stack
`CurrencyApiClient` proved on this AGP 9 toolchain, avoiding unproven Gradle plugins like
supabase-kt/Ktor). Nothing about liabilities changes that calculus.

**Alternatives considered**: A dedicated liabilities client/module — rejected, no isolation need
exists that Repository-only access doesn't already provide (same reasoning as ADR-0029's rejection
of a separate `:tracker-data` module).

## R2 — Component strategy: extend existing components before adding new ones

**Decision**: Build component batches B3 (charts), B6 (`NxSelect`), B9 (`SelectionSheet`) as new
`:libs:core` components since no equivalent exists yet (verified by symbol search per the design
system's own §13 verification method); extend `NxTextField` (error/helper state) and `NxButton`
(sizes/loading/block) rather than creating parallel form components, since both already exist and
only lack these variants.

**Rationale**: Constitution Article VI + design system §5.3's explicit rule: "closing a §5.3 row
means extending the existing component, never adding a parallel one." C4/C5 are the first validated
forms in the app — nothing before them needed `NxTextField`'s error state, so the gap is real, not
an oversight to route around.

**Alternatives considered**: Hand-rolled Material3 form fields inside `:feature:networth` — rejected
outright, this is exactly the feature-local-styling violation the micro-frontend rule (ADR-0014 §8)
exists to prevent.

## R3 — Test strategy: fakes, not Robolectric-SQLite

**Decision**: `HoldingRepositoryTest`/`ValuationRepositoryTest`/`LiabilityRepositoryTest` run
against in-memory fakes of the Supabase API layer (MockWebServer or hand-written fakes), not a real
Room/SQLite instance.

**Rationale**: This project's Robolectric-SQLite path is unreliable on Windows dev machines (a
standing, already-hit constraint in this repo, not something to rediscover per feature). Tracker
data isn't in Room at all (Supabase-primary, ADR-0014) — the fake boundary is the HTTP client, not
a database.

**Alternatives considered**: A real Postgres test container against a dev Supabase project —
possible for the SQL/RLS layer test the module-standard doc already calls for at the Sec step, but
not for fast unit-level Backend tests; those stay fake-based for iteration speed.

## R4 — Aggregation: server-side views, not client-side reduction

**Decision**: Net worth totals and the sector breakdown are read from `v_net_worth_by_sector`
(new view, this phase); the client never sums a holding's full valuation history to compute a
current total.

**Rationale**: NFR-8 states this directly, and it is also the difference between an O(1)-ish read
and an O(holdings × valuations) client-side scan that gets slower as history grows — a real scale
concern, not a style preference.

**Alternatives considered**: Client-side reduction over `v_latest_valuation` rows — rejected; even
reducing over *latest* values only (not full history) still repeats work the database can do once
and cache via the view.

## R5 — Returns calculation: simple % now, IRR-style figure later (resolved via user clarification)

**Decision**: Story 2's "gain since invested" ships as `(current − invested) / invested` this
phase. The more accurate IRR-style calculation (referred to as XIRR in the source design) is
deferred to its own future ADR.

**Rationale**: NW-BR-007 in the QA catalog already marks the IRR-style figure "blocked" pending an
ADR that doesn't exist yet — inventing that ADR's content inside this plan would be exactly the
kind of undocumented architectural decision the constitution's Article IX / append-only-decisions
rule warns against. Asked the maintainer directly (spec.md's resolved clarification); simple-%-now
was the chosen path over blocking the whole gain display or silently picking the IRR formula.

**Alternatives considered**: presented as the three spec-time options — ship without any rate,
ship the simple percentage, or block the entire gain/invested display. See spec.md Assumptions.

## R6 — Module placement: one shared `:apps:finance:data`, no new tracker-data module

**Decision**: `LiabilityRepository` lives in the existing `apps/finance/data/.../tracker/repo/`
alongside Phase 1's repositories. No new Gradle module.

**Rationale**: Already decided in the implementation plan §5.1 — ArchUnit's Repository-only rule
already gives features the isolation a second module would otherwise buy, at the cost of a second
Koin graph and Gradle dependency edge. Nothing about this phase changes that trade-off.

**Alternatives considered**: none re-evaluated — re-opening a settled ADR-adjacent decision without
new information would violate constitution Article XI.
