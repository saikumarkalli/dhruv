# R8 — Daily-Driver Polish (Onboarding, Widget, Global Search, Trash)

> Status: **SPECCED** (build after R7; four independently-shippable slices in one phase — each
> slice is its own PR in the listed order). Master sequence:
> `../plans/2026-07-12-master-roadmap-personal-app.md` (R8; gaps N5/N9/N10/N17 + N19 leftovers).
> Umbrella + design system + playbook binding; inherits all shared invariants.

## Goal

The features that make the app feel finished as a daily driver: a first-run that explains and
seeds (onboarding), a launcher widget with net worth + quick-add, one search box over everything,
and a working trash so soft-delete finally has a user-facing story (restore + undo).

## Slice 1 — Onboarding (N5)

**Non-goals:** no feature tour overlays, no sample/demo data mode.

- 3-page horizontal pager, shown once (`onboardingDone` DataStore flag), Skip always visible:
  1. Identity — "Track your complete financial life" (net worth, expenses, goals, insurance) +
     "calculators live under Tools".
  2. Privacy stance — plain-language: data stored in your Supabase-backed account (region named,
     same copy source as the consent screen), app lock available, export anytime, delete anytime.
     This page *frames* consent; the actual `ConsentGateScaffold` remains the P1 gate that follows.
  3. CTA — "Sign in & add your first asset" → existing P1 flow (consent → sign-in → Home empty
     state CTA); or "Explore tools first" → Tools tab.
- Re-viewable: Settings > About > "Show intro". No flag (app shell). No new components — pager +
  existing cards.
- **PG8 seed checklist (PO review):** after the first asset saves, a one-time `DhruvModalSheet` —
  "What else do you have?" — chip grid of standard Indian holdings (Bank · EPF/PPF · Mutual funds
  · Stocks · Gold · Property · Vehicle · Home loan · Credit card); each tap opens the holding
  editor pre-categorized (asset or liability per chip); dismiss = done, never re-shown (DataStore
  flag). Cuts time-to-complete-net-worth from "figure it out" to a guided 10 minutes.
- **Tests:** shown-once logic (flag set on finish AND on skip), routing per CTA (Robolectric);
  seed-chip → pre-categorized editor mapping; seed sheet never re-shows.

## Slice 2 — Net-worth widget (N9)

**Non-goals:** no transaction list widget, no widget configuration activity in v1 (single 2×2).

- **New dependency:** `androidx.glance:glance-appwidget` (stable AndroidX — allowed class).
- Widget never talks to the network. `WidgetSnapshotStore` (DataStore): net worth paise, month
  delta, updated-at — written by (a) every successful Home refresh, (b) a daily `WidgetRefreshWorker`
  (R4 scheduler pattern, CONNECTED, silent-skip offline).
- Render states: value + `▲/▼` delta + "as of <date>" | masked via `MaskedMoney.mask()` when R3
  privacy mode on (Glance has no Compose locals — widget reads `hideAmounts` from the snapshot
  store and applies the pure transform, per R3/F3) | "Sign in to Dhruv" when signed out / consent
  withdrawn (snapshot cleared on sign-out — security: no stale balance on a signed-out device) |
  "Enable Net worth" when flag-disabled.
- Snapshot DataStore file added to the backup **exclusion** set (R0 rules) in this same PR (F12).
- Tap body → app Home. Quick-add button → `QUICK_ADD` intent extra (R5b contract; app-lock
  intercepts first per R3).
- **Tests:** snapshot-store round-trip; state selection pure function (signedIn × consent ×
  privacy × staleness → render model) TDD; Glance composition = manual checklist (no reliable
  JVM harness).

## Slice 3 — Global search (N10)

**Non-goals:** no fuzzy ranking, no full-text-search indexes (ILIKE is enough at personal-data
scale), no search over calculator history (Room domain — separate concern, not in v1).

- New thin module `:apps:finance:feature:search` (cross-domain; querying via `:data` repositories
  keeps `feature → feature` at zero).
- `ISearchRepository` in `:data`: parallel PostgREST queries with `or=(name.ilike.*q*,notes.ilike.*q*)`
  per table — assets, liabilities, transactions (notes), policies (insurer, notes), goals (name);
  `is_deleted=eq.false`; limit 20/table; merged into grouped `SearchResults`.
- Escape `%`/`_`/`,`/`(`/`)` in user input before interpolation into the `ilike` pattern
  (PostgREST filter-injection hygiene — unit-tested).
- UI: search icon in Home top bar → `SearchScreen`: debounced field (300ms, min 2 chars), grouped
  results (`BentoCard` rows with type chip + `MoneyText`), tap → owning detail screen, recent
  queries (max 10, local DataStore, clearable).
- Flag `"search"` (enabled, `minVersion <release>`, requiresConsent true — consistency only).
- **Tests:** query-escape TDD; repository merge/partial-failure (one table 500s → others still
  render + error chip); ViewModel debounce/min-length (Turbine).

## Slice 4 — Trash & undo (N17)

**Non-goals:** no version history/audit trail; restore restores the row, not its edit history.

- **Schema (additive migration; table set = the R7 Tracker Table Registry, D2a):**
  `alter table <t> add column deleted_at timestamptz;` — soft-delete PATCH now sets
  `is_deleted=true, deleted_at=now()`; restore sets `is_deleted=false, deleted_at=null`.
  Tables created after this phase include `deleted_at` in their create script (roadmap §4 rule).
- **Purge:** Supabase **pg_cron** daily job: `delete from <t> where is_deleted and deleted_at <
  now() - interval '30 days'` per table. Client-side fallback purge (fire-and-forget on app open)
  guards against pg_cron unavailability; DPDP hard-delete paths (delete-my-data/account) unchanged
  and immediate.
- UI: Settings > Data > "Recently deleted" — type-grouped rows (name, `MoneyText` value, "deleted
  N days ago", days-left badge), per-row Restore, Delete forever (`ConfirmDangerDialog`), Empty
  trash. Plus **undo snackbar** (5s) on every tracker delete action, everywhere — the snackbar's
  undo = the same restore PATCH.
- Valuation rows: restorable like everything else (P1's correction flow = soft-delete + append;
  restore simply un-hides the row — trend math already excludes/includes by `is_deleted`).
- **Tests:** repository soft-delete now sets/clears `deleted_at`; restore round-trip; purge-
  eligibility pure function (30-day boundary); ViewModel restore/undo with fakes.

## N19 leftovers (ride this phase, no spec of their own)

- Predictive back: `android:enableOnBackInvokedCallback="true"` + verify every `BackHandler`
  (pager-to-Home contract from P1 G16) under gesture preview.
- App-switcher blanking already done (R3); per-app language deliberately waits for the
  localization decision (N18, R10 — excluded from this scope).

## Dependencies

Slices 1+3: P1 merged. Slice 2: R3 (privacy flag), R5b (QUICK_ADD contract), R4 plumbing.
Slice 4: any tracker phase (tables exist since P1); pg_cron enable = manual Supabase step
(checklist item, M0-style smoke).

## Rollout & rollback

Onboarding: DataStore-gated, no flag. Widget: inert unless placed; snapshot cleared on sign-out.
Search: flag kill-switch. Trash: schema additive; snackbar/undo degrade to plain delete if the
screen is flag-hidden (undo PATCH needs no UI). Each slice = separate PR, standard gates
(`regressionCheck`, `/dhruv-ui-review` for slices 1–3, `/dhruv-security` for slice 4's purge).

## Risks / open questions

- Widget snapshot is a small plaintext-ish local copy of one number — mitigations: masked under
  privacy mode, cleared on sign-out, excluded from backup (R0 rules). Accepted residual.
- pg_cron on Supabase free tier: verify availability at slice-4 M0; fallback client purge already
  specced.
- ILIKE scans at scale — fine for personal volumes; if search ever feels slow, pg_trgm index is a
  server-only additive fix (noted for P6-era revisit).

## TDD Mandate

> **Test-Driven Development (TDD) is strictly required for this phase.**
> All pure logic, calculators, reducers, and state machines MUST be written with failing tests first, followed by implementation. UI components must be tested for accessibility and rendering states on both Android and Web platforms.

