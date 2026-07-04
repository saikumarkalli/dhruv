# Design — Accurate Currency Rates + Gold/Silver + Configurable Daily Rate Notification

- **Date:** 2026-07-03
- **App / modules:** `:apps:finance` (`data`, `feature:currency`, `app`) + `:libs:settings`
- **Status:** Approved design → ready for implementation plan
- **Related:** PLATFORM.md §5/§8/§10, DECISIONS.md ADR-0002/ADR-0005, `apps/finance/CLAUDE.md`
- **UI:** the currency screen, metal cards, and detail view are designed with the
  **ui-ux-pro-max** skill at implementation time (see §9 D11).

---

## 1. Problem & goals

Enhance the existing Finance currency converter
([CurrencyRepository.kt](../../../apps/finance/data/src/main/java/com/dhruv/finance/data/CurrencyRepository.kt),
[CurrencyViewModel.kt](../../../apps/finance/feature/currency/src/main/java/com/dhruv/finance/currency/CurrencyViewModel.kt)):

1. **More accurate data source** with an honest on-screen freshness indicator.
2. **Surface source/limit/staleness** on the same screen ("if the API has limits, show it").
3. **Gold & silver** spot cards on the same screen; tapping a card opens **basic investor details**
   for that metal. Card visibility is controllable in Settings.
4. **Daily notification** (configurable time, default 09:00 local) reporting USD→INR **and** gold &
   silver, each with day-over-day change.
5. **Upgrade the currency screen's UI/UX** (visual hierarchy, states, accessibility, motion) using
   the ui-ux-pro-max skill, **strictly within this app's rules** (Dhruv design system, Compose
   patterns, FeatureHost, ArchUnit boundaries) — see §12.
6. Opt-in only; respects the platform's boundary, security, and DPDP rules.

### Non-goals (YAGNI)
- No true minute-by-minute "real-time" FX/metals (unavailable free at volume; collides with the
  "no keys in APK" rule). "Real-time" = accurate **daily** reference/spot values.
- No keyed/paid API, **no Worker proxy** for currency or metals.
- No local Indian **retail** metal prices (22K/24K street price with duty + GST + making charges) —
  keyless data is **international spot** only; the UI labels this clearly.
- No platinum/palladium (not in the keyless source).
- No remote feature-flag kill-switch — Settings toggles are the only control.
- No exact-alarm scheduling, no BootReceiver.

---

## 2. Data sources (all keyless — verified 2026-07-03)

| Series | Source | Notes |
|--------|--------|-------|
| FX (USD→INR + majors) | **Frankfurter** (ECB) primary | Returns rate `date` for freshness. Verified `USD→INR` with `date` field. No metals. |
| FX fallback + **metals** | **Fawaz Ahmed's currency-api** (`@fawazahmed0`) | Unlimited, no key, returns `date`. Verified it exposes **`xau` "Gold Ounce"** and **`xag` "Silver Ounce"**. |
| Offline | existing **Room cache** | Final self-healing layer (unchanged behavior). |

**Metal math (keyless, derived):** Fawaz `usd.json` gives `usd.xau` = troy-oz of gold per 1 USD.
`goldUsdPerOz = 1 / usd.xau`; `goldInrPerGram = goldUsdPerOz * usdInr / 31.1035`;
`per10g = perGram * 10`. Silver via `usd.xag`. All values honest **international spot**.

---

## 3. Key decisions (settled)

| # | Decision | Rationale |
|---|----------|-----------|
| D1 | No-key free sources only (see §2). Replaces current `open.er-api.com` + deprecated `exchangerate-api.com/v4`. | "No secrets in APK", no DPDP key custody, no quota to drain; both sources expose the rate `date`. |
| D2 | "Limits" display = **rate-source status line** via `RateMeta` (`sourceName`, `rateDate`, `fetchedAt`, optional `quotaLimit`/`quotaRemaining` = null for keyless). | No-key sources have no quota; show source + staleness truthfully, keep optional quota fields to future-proof a keyed source. |
| D3 | Notification content = **USD→INR + gold + silver**, each with day-over-day delta. Multi-line (`BigTextStyle`). | User asked for INR *and* metals; delta matches "compared to". |
| D4 | **Opt-in** Settings toggle, default OFF, **with a full time picker (default 09:00 local)**. | DPDP/UX-friendly; user chose configurable time. |
| D5 | Scheduling = WorkManager `PeriodicWorkRequest` (24h, initial delay to next chosen time). Not AlarmManager. | Platform-standard, survives reboot without a BootReceiver, battery-friendly; reschedules on time change. |
| D6 | **No new DPDP consent gate.** Rate/metal fetch is a GET for *public* data (no user PII off-device); notification is local. | Consistent with today's currency feature. |
| D7 | **Gold/silver cards on the currency screen**; tap → **investor-basics detail** (bottom sheet, VM state-driven — no new NavHost route). Card visibility toggled in Settings. | Keeps everything in `feature:currency` with no `feature→feature` edge and no extra nav wiring/crash surface. |
| D8 | Metal detail = **spot essentials + investor basics**: 24K per 10g & per gram (INR), per troy oz (USD & INR), day change ▲▼, and a short "spot vs local retail differs (duty/GST/making charges); gold-as-hedge" note. | Everything keyless-derivable and honestly labelled. |
| D9 | Snapshots generalized to a **keyed daily series** table (`USD_INR`, `XAU_USD_OZ`, `XAG_USD_OZ`) for both the screen's day-change and the notification delta. | One mechanism serves FX + both metals. |

---

## 4. Architecture & module placement

Honors ArchUnit rules (`feature → feature` FORBIDDEN, `feature → data` via repository, `core`
depends on nothing internal, `app` may depend on everything).

### 4.1 `:apps:finance:data` (pure data)
- **New API clients**: Frankfurter (FX) + Fawaz (FX fallback + metals). Base URLs → `BuildConfig`
  (like the existing `CURRENCY_API_BASE_URL`).
- **`RateMeta`**: `sourceName`, `rateDate: LocalDate?`, `fetchedAt: Long`, `quotaLimit: Int? = null`,
  `quotaRemaining: Int? = null`.
- **`RateSnapshotEntity`** (`rate_snapshots`, composite PK `date: String` + `series: String`,
  `value: Double`) + `RateSnapshotDao` (upsert today's series, read latest-before-today per series).
  **Room migration** added for the new table (also addresses the destructive-migration risk noted in
  the production-readiness audit).
- **`MetalPricing`** pure helper: spot derivation (§2 math) → `MetalSpot(perGramInr, per10gInr,
  perOzUsd, perOzInr)`.
- **`ICurrencyRepository`** gains:
  - `fetchLatest(base): CurrencyResult` (rates map **+ `RateMeta`**).
  - `getDailyReport(): DailyRateReport` → USD→INR + gold + silver, each `{ current, previous?,
    absChange?, pctChange? }`; writes today's snapshots as a side effect. Used by screen + worker.
- **Pure `computeDelta(today, yesterday)`** (testable).

### 4.2 `:libs:settings`
- `AppSettings`: `dailyRateNotificationEnabled=false`, `dailyRateNotificationHour=9`,
  `dailyRateNotificationMinute=0`, `showGoldCard=true`, `showSilverCard=true`.
- Matching `SettingsKeys` (`daily_rate_notification_enabled`, `daily_rate_notification_hour`,
  `daily_rate_notification_minute`, `show_gold_card`, `show_silver_card`); wire through
  `SettingsRepositoryImpl.observe()/update()`.

### 4.3 `:apps:finance:feature:currency`
- **`DailyRateWorker : CoroutineWorker`** — injects `ICurrencyRepository` + reads notify prefs;
  delegates to injectable **`DailyRateUseCase`** (fetch → `getDailyReport()` → text) so `doWork()`
  stays thin/unit-testable. `Result.retry()` on failure.
- **`RateNotifier`** — idempotent channel + multi-line notification; content-intent opens the
  currency screen.
- **`DailyRateScheduler`** — `schedule(hour, minute)` (unique periodic work, `UPDATE` policy, initial
  delay to next occurrence, `CONNECTED` constraint) / `cancel()`.
- **VM/Screen** (designed via ui-ux-pro-max):
  - Converter body (existing) + **Gold card** + **Silver card** (per-10g INR + day change), shown
    per the Settings visibility toggles.
  - Status card gains a **source/freshness line** ("Source: ECB (Frankfurter) · rates as of \<date\>
    · refreshed \<ago\>"); optional quota line only if `quotaLimit != null`.
  - Tapping a metal card → VM `selectedMetal` state → **detail bottom sheet** (§3 D8 content).
- New deps: `androidx.work:work-runtime-ktx`, `koin-androidx-workmanager`.

### 4.4 `:apps:finance:app`
- **Manifest**: add `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`.
- **Koin startup** (`CalculatorApplication`): `workManagerFactory()` + `worker { DailyRateWorker(...) }`;
  disable default `WorkManagerInitializer` (androidx.startup provider removal) so Koin's factory runs.
- **Settings UI**: toggle row + **time picker** (visible when enabled) + **Show gold / Show silver**
  switches. Enable → request `POST_NOTIFICATIONS` on Android 13+ (guarded, minSdk 26) → on grant
  `scheduler.schedule(hour,minute)`; deny → revert + rationale. Time change → reschedule. Disable →
  `cancel()`.
- **App start**: re-ensure schedule matches persisted prefs (idempotent `UPDATE`).

---

## 5. Data flow

**Screen open** → VM `fetchLatest("USD")` (Frankfurter → Fawaz → cache) → emits rates + `RateMeta` +
gold/silver spot + day change → renders converter, metal cards, and source/freshness line.
Tap metal card → detail bottom sheet.

**Daily at chosen time** → WorkManager → `DailyRateWorker` → `DailyRateUseCase` →
`repo.getDailyReport()` (fetch, read yesterday snapshots, compute deltas, write today snapshots) →
`RateNotifier` posts:
```
1 USD = ₹95.39  ▲0.12 (+0.14%)
Gold 24K ₹—/10g ▲…   Silver ₹—/10g ▼…
```
Tap → opens currency screen. Missing prior snapshot → value shown without delta.

---

## 6. Error handling & edge cases
- Network failures self-heal (Frankfurter → Fawaz → cache); Worker failures → `Result.retry()`
  (backoff), never crash.
- First run / missing snapshot → value shown, delta omitted. Unchanged → "no change".
- Metal source unavailable → hide/placeholder metal cards + omit metals from the notification (FX
  line still sent).
- Permission denied (Android 13+) → toggle reverts, nothing scheduled, rationale shown.
- Feature stays under `FeatureHost`; VM keeps `crashReporter.setModule("currency")`, `featureError`
  via `CoroutineExceptionHandler`, and a `performanceTracer.trace("currency_…")`.
- Time zone/DST: initial delay recomputed from device local time on each reschedule; 24h period drift
  is acceptable for a daily reminder.

---

## 7. Testing (regressionCheck / JaCoCo floor)
- `computeDelta` + `MetalPricing` pure fns — rise/fall/zero/first-run + oz→gram/10g conversions.
- Repository source-selection + keyed snapshot upsert/read — unit tests with **fakes** (Room via
  fakes, not real DB — known Windows Robolectric-SQLite limitation).
- `DailyRateUseCase` — fake repo + fake notifier: with-delta, missing-delta, metals-unavailable,
  retry-on-failure.
- Moshi DTO parsing for Frankfurter + Fawaz (incl. `xau`/`xag`).
- ArchUnit `DependencyRulesTest` re-run — currency depends only on `:data`/core; no other-feature
  imports.
- Keep merged line-coverage at/above the current floor.

---

## 8. Definition of done (platform checklist)
- ArchUnit rules pass; currency flag entry unchanged (already enabled).
- Crash tagging + ≥1 performance trace present.
- Unit tests pass; bundle-size delta within budget (WorkManager + Koin-WM added).
- No secrets/keys added (GitLeaks clean) — all sources keyless.
- No off-device user data → no consent gate / Data Safety entry (documented §3 D6).
- `POST_NOTIFICATIONS` is the only new permission; requested at opt-in, SDK-guarded.
- Metal values labelled **international spot**, not local retail.

---

## 9. Cross-cutting decisions

| # | Decision |
|---|----------|
| D10 | Metal detail is a **bottom sheet** inside `feature:currency` (VM `selectedMetal` state), not a new NavHost route — avoids extra nav wiring and keeps one `FeatureHost`. |
| D11 | **UI is designed with the ui-ux-pro-max skill** (metal cards, detail sheet, source/freshness line, Settings time picker) — invoked during implementation, following the Compose Screen→UiState→Content + Koin pattern (`apps/finance/CLAUDE.md`), not web/React. Full scope + constraints in §12. |

---

## 10. Suggested phasing (for the implementation plan)
- **Phase A — FX + notification:** source swap (Frankfurter/Fawaz) + `RateMeta`/freshness line +
  keyed snapshot table + configurable daily USD→INR notification (toggle + time picker + permission +
  WorkManager). Shippable on its own.
- **Phase B — Metals:** gold/silver spot cards + investor-basics detail sheet + metals in the
  notification + Settings visibility toggles. Builds on Phase A's snapshot/source plumbing.

The **UI/UX upgrade (§12) is folded into each phase**, not deferred: Phase A refreshes the converter,
the source/freshness affordance, states, and the new Settings rows; Phase B styles the metal cards and
detail sheet. Each phase ends with a **dhruv-ui-review** pass.

---

## 11. Open follow-ups (out of scope)
- Keyed hourly source later → behind a Worker proxy (ADR-0002); populate `RateMeta.quota*` (UI ready).
- Optional: N-day metal sparkline (needs longer history retention); localized notification text;
  per-user target currency; platinum/palladium if a keyless source appears.

---

## 12. UI/UX upgrade — scope & app-rule constraints

The currency screen is refreshed end-to-end (not just the new cards), using **ui-ux-pro-max** for
the design and reviewed with the **dhruv-ui-review** skill/agent. It **must obey this app's rules**:

**Scope of the refresh**
- Rework the converter's visual hierarchy: clearer From/To grouping, prominent result, one-tap swap,
  amount input affordance; consistent card radii/spacing/elevation with the rest of Finance.
- Elevate the **source/freshness** line into a legible status affordance (live vs offline vs cached),
  and a manual refresh with visible loading/settled states.
- **Metal cards** (gold/silver) and the **detail bottom sheet** styled as first-class components.
- All four states designed: **loading, success (live/offline), error, empty/first-run**.
- Motion: subtle, purposeful (result update, swap, sheet expand) — no gratuitous animation.

**Non-negotiable app rules it follows**
- **Design system from `:libs:core`**: `AppTheme` (dark/light/system), the **user's accent color**
  (default Dhruv gold `#D4AF37`) and **`DhruvFont`** selection — never hardcode colors/fonts; use
  `MaterialTheme` tokens. Metals may use tasteful gold/silver accents *derived from* theme, still
  respecting dark mode + contrast.
- **Compose + Material3 only**, Koin DI; **Screen→UiState→Content** structure; the screen body stays
  in `:apps:finance:feature:currency` (no `feature→feature`, no new NavHost route — detail is a
  bottom sheet, D10).
- **FeatureHost** wrapping preserved (disabled → `FeatureDisabledCard`, error → `FeatureErrorCard`);
  VM keeps `setModule("currency")`, `featureError`, and a performance trace.
- **Preserve existing `testTag`s** (`currency_from_btn`, `currency_to_btn`, `currency_input_field`,
  `currency_swap_btn`, `currency_output_val`) so current tests keep passing; add tags for new
  elements.
- **Accessibility**: content descriptions on icons/cards, ≥48dp touch targets, sufficient contrast in
  both themes, dynamic-type-friendly text; the metals disclaimer is readable, not fine print.
- Keep the existing **pager + Converter hub** navigation (ADR-0010); the hub still owns the toolbar.
- **No new dependencies** for UI beyond what's already used (Compose/Material3); no web/React output.

**Done when**: dhruv-ui-review passes, screenshots render correctly in light+dark with a non-default
accent, existing UI tests are green, and no ArchUnit boundary is crossed.
