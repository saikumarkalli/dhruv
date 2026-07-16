# P1 — Net Worth Tracker (assets, liabilities, valuations)

> Status: **APPROVED**. Umbrella: `2026-07-03-tracker-roadmap-overview.md`. ADR: ADR-0014.
> Design system: `2026-07-03-tracker-design-system.md`. Playbook: `2026-07-04-tracker-engineering-playbook.md`.
> Gap analysis: `2026-07-04-p1-gap-analysis.md`. Branch: `feat/networth-tracker` → `develop`.

## 1. Goal & success criteria

Foundation phase: track assets and liabilities with manual, append-only valuations; computed net
worth with month delta and trend; Material You bento Home; Google sign-in + persisted/revocable
DPDP consent; calculators relocated behind a Tools tab; shared design-system components landed in
`:libs:core`.

**Done means:**
- Sign in with Google, add an asset + liability, record valuations, see net worth + trend on Home.
- Kill/restart → session restored, data refetched. Airplane mode → Offline state with retry.
- Withdraw consent → tracker gated again. Delete my data → tables empty. Delete my account → auth
  user gone.
- Every existing tool works exactly as before (Tools tab). `regressionCheck` green. Release
  (minified) build works.

## 2. UI/UX (screen by screen)

All components from `:libs:core` design system (see design-system doc — micro-frontend rule:
feature owns flows, core owns visuals). Accent: Home = "green" section theme.

### 2.1 `NetWorthScreen` (entry, state machine)
States render in priority order:
1. **NotConfigured** (blank BuildConfig): `EmptyStateCard` — "Tracker not configured", explains
   `.env` setup; no network attempted.
2. **ConsentNeeded**: `ConsentGateScaffold` — title "Before you start", bullets: data stored in
   your private Supabase space (name region), nothing shared, erase anytime in-app ("Delete my
   data/account"), consent revocable in overflow menu. Buttons: "Agree & continue" / "Not now"
   (stays gated).
3. **SignedOut**: `SignInContent` — app logo, one-line value prop, "Continue with Google" filled
   button (Credential Manager flow). Cancel → stays here, no error.
4. **Loading**: centered spinner (first fetch only; later refreshes are silent).
5. **Offline**: `OfflineBanner` + `RetryErrorCard` retry button.
6. **Error**: `RetryErrorCard` with message + retry.
7. **Content**: dashboard + sub-screens below.

Overflow menu (Content only): Refresh · Sign out · Withdraw consent · Delete my data ·
Delete my account. Both delete actions use `ConfirmDangerDialog` (irreversible wording; account
delete requires typing DELETE).

Back handling: local `BackHandler` enabled only when own tab is current AND a sub-screen is open
(pops to dashboard); otherwise system default (pager to page 0 / exit).

### 2.2 `DashboardContent` (bento)
- `BentoGrid` (2-col, 16dp margin, 12dp gutter):
  - **Hero** `HeroStatCard` full-width: label "Net worth", value `formatPaiseCompact`
    (₹48.2L), `StatDeltaChip` month delta (▲/▼ + ₹ compact + %), sparkline `TrendLineChart`
    (bottom third, whole-history).
  - **Assets** half `BentoCard`: label, total compact, "N items", tap → AssetList.
  - **Liabilities** half `BentoCard`: same, tap → LiabilityList.
- Empty (no holdings): hero shows ₹0 + `EmptyStateCard` "Add your first asset" CTA → editor sheet.
- Pull-to-refresh not in P1; overflow Refresh instead.

### 2.3 `HoldingListScreen` (parameterized ASSET | LIABILITY)
- Top bar: back, title (Assets/Liabilities), total compact value subtitle.
- List of `BentoCard` rows: name (titleMedium), category label chip, latest value (tabular,
  `formatPaise`), "updated <relative date>" (bodySmall onSurfaceVariant).
- FAB "+ Add" → holding editor sheet. Empty → `EmptyStateCard`.
- Tap row → `HoldingDetailScreen`.

### 2.4 `HoldingDetailScreen`
- Header `BentoCard`: name, category, latest value, notes.
- `TrendLineChart` (this holding's history, axis-less).
- History list (append-only): value + recorded date rows, newest first; row overflow → "Remove
  entry" (soft-delete, `ConfirmDangerDialog`).
- Actions: "Update value" (valuation sheet), "Edit details" (editor sheet), "Delete" (soft-delete
  holding, confirm dialog).

### 2.5 Sheets (`DhruvModalSheet`)
- **Holding editor** (create/edit): name field, category dropdown (enum labels), notes optional;
  create-mode adds initial value (₹ prefixed field) + date (default today). Validation inline:
  blank name; unparseable amount; liability > 0, asset ≥ 0; date not in future. Primary button
  full-width "Save", disabled while saving.
- **Valuation sheet**: ₹ field + date picker (default today, max today). Same validation.

### 2.6 Accessibility (gate for `/dhruv-ui-review`)
Touch targets ≥48dp; contrast AA; `TrendLineChart` contentDescription "Net worth trend from ₹X on
<date> to ₹Y today"; delta chips carry ▲/▼ glyphs (not color-only); TalkBack order hero → assets →
liabilities; dynamic type safe (no fixed-height text boxes).

## 3. Design

Tokens only from `DhruvTheme`/`SectionTheme` MaterialTheme roles (no hex in feature code).
Bento specs, card anatomy, chart styling, sheet conventions, motion: see
`2026-07-03-tracker-design-system.md` (binding). Money display: `formatPaiseCompact` on cards,
`formatPaise` in lists/sheets; tabular numerals.

## 4. Backend (Supabase)

### 4.1 Schema (run once in SQL editor — FROZEN for P1)

```sql
create table assets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name text not null,
  category text not null,          -- BANK|MUTUAL_FUND|STOCKS|PROPERTY|GOLD|EPF_PPF|CASH|VEHICLE|CRYPTO|OTHER
  notes text not null default '',
  currency text not null default 'INR',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  is_deleted boolean not null default false
);
create table liabilities (         -- category: HOME_LOAN|PERSONAL_LOAN|CAR_LOAN|EDUCATION_LOAN|CREDIT_CARD|BNPL|BORROWED_FROM_FRIEND|OTHER
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  name text not null, category text not null, notes text not null default '',
  currency text not null default 'INR',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  is_deleted boolean not null default false
);
create table valuation_entries (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  parent_id uuid not null,
  parent_type text not null,        -- ASSET | LIABILITY
  value_paise bigint not null,      -- integer paise: exact, summable
  currency text not null default 'INR',
  recorded_at timestamptz not null,
  created_at timestamptz not null default now(),
  is_deleted boolean not null default false
);
create index on valuation_entries (user_id, parent_id);
create index on valuation_entries (user_id, recorded_at);

alter table assets enable row level security;
alter table liabilities enable row level security;
alter table valuation_entries enable row level security;
create policy "own rows" on assets for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "own rows" on liabilities for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "own rows" on valuation_entries for all using (user_id = auth.uid()) with check (user_id = auth.uid());

-- server owns bookkeeping: client never sends user_id / updated_at
create extension if not exists moddatetime;
create trigger assets_updated_at before update on assets
  for each row execute procedure moddatetime(updated_at);
create trigger liabilities_updated_at before update on liabilities
  for each row execute procedure moddatetime(updated_at);

-- full in-app erasure (rows + auth account) — DPDP, no Edge Function / service-role on device
create or replace function delete_my_account() returns void
language plpgsql security definer set search_path = public as $$
begin
  delete from valuation_entries where user_id = auth.uid();
  delete from liabilities where user_id = auth.uid();
  delete from assets where user_id = auth.uid();
  delete from auth.users where id = auth.uid();
end $$;
revoke all on function delete_my_account() from public;
grant execute on function delete_my_account() to authenticated;
```

Semantics: `recorded_at` = user-meaningful valuation date; append-only — no update path for
valuation rows (correction = soft-delete + re-append). Latest-per-parent tie-break: equal
`recorded_at` → higher `created_at`, then `id` lexicographic.

### 4.2 Client stack (ADR-0014 §6 — no supabase-kt)

Retrofit + Moshi + OkHttp (existing, proven on this toolchain):
- `SupabaseAuthApi`: `POST /auth/v1/token?grant_type=id_token` (Google ID token → session),
  `POST /auth/v1/token?grant_type=refresh_token`, `POST /auth/v1/logout`.
- `SupabaseRestApi`: `/rest/v1/assets|liabilities|valuation_entries` (headers `apikey`,
  `Authorization: Bearer`, `Prefer: return=representation`; filters e.g. `is_deleted=eq.false`)
  + `POST /rest/v1/rpc/delete_my_account`.
- OkHttp interceptor: attaches headers, transparent refresh-token rotation on 401 (single retry),
  refresh failure → SignedOut.
- `CertificatePinner`: ISRG Root X1 + X2 (CA-level — survives leaf rotation).
- Session persisted in encrypted DataStore only.

### 4.3 Access patterns
Fetch-all per table on sign-in / manual refresh / after each mutation (single-flight Mutex).
Personal dataset — hundreds of rows; windowed fetch is a P6 revisit item.

### 4.4 Error taxonomy → UI state
| Condition | UI |
|---|---|
| Blank BuildConfig | NotConfigured |
| No consent persisted | ConsentNeeded |
| No/expired session (refresh failed) | SignedOut |
| IOException / timeout | Offline (retry) |
| HTTP 4xx/5xx (non-401) | Error (retry) + crashReporter (no financial values in logs) |
| Success | Content |

## 5. Architecture

- `:apps:finance:data`: DTOs (Moshi codegen, snake_case), `IAuthRepository`/`AuthRepository`
  (`AuthState`: SignedOut/SignedIn(userId,email)/Loading/NotConfigured; `signInWithGoogle(idToken)`,
  `signOut()`, `deleteAllMyData()`, `deleteMyAccount()`), `INetWorthRepository`/
  `SupabaseNetWorthRepository` behind fakeable `NetWorthRemoteDataSource`; `Paise.kt`
  (`parseToPaise`, `formatPaise`, `formatPaiseCompact`).
- `:libs:core`: design-system components (new files only).
- `:apps:finance:feature:networth`: `NetWorthCalculator` (pure), `NetWorthDashboardViewModel`
  (combine flows → `trace("networth_summary")` → `stateIn`; sub-screen state), `NetWorthEditorViewModel`
  (`trace("networth_save")`, save-in-flight guard), Koin `networthModule`, screens (§2).
- Consent: persisted `SettingsRepository` boolean + withdraw path.
- Observability: `crashReporter.setModule("networth")`, `featureError` StateFlow, perf traces.
- Flag: `networth` — enabled, minVersion **1.0.0** (dev builds carry VERSION_NAME 1.2.x until CI
  bumps; a 1.3.0 gate would hide the feature during development), requiresConsent true.
- Secrets: `.env` (secrets gradle plugin) — `SUPABASE_URL`, `SUPABASE_ANON_KEY`,
  `GOOGLE_WEB_CLIENT_ID`; `.env.example` committed with empty defaults; release CI writes `.env`
  from GitHub secrets. R8: Moshi/Retrofit rules exist; add DTO package + credentials/googleid keeps.

## 6. Tests

- `PaiseTest`: Indian-format parse/format round-trips; garbage → null; compact ₹92,500 / ₹4.8L /
  ₹1.25Cr / 0 / negative.
- `NetWorthCalculatorTest` (10+): empty → zeros; latest-per-parent; assets−liabilities;
  deterministic tie-break; soft-deleted parent/valuation excluded; month delta vs device-TZ month
  start; trend point per distinct timestamp; negative net worth; holding absent before first
  valuation.
- `AuthRepositoryTest` (fake Retrofit boundary): sign-in transitions; cold-start restore; refresh
  rotation; 401→SignedOut; deleteAll/deleteAccount call shapes; NotConfigured on blank config.
- `NetWorthRepositoryTest` (fake remote): write-through + refresh; append-only enforced;
  soft-delete flips flag; network vs unauthorized errors distinguished; concurrent refresh
  single-flight.
- ViewModel tests (Turbine + fakes): full state machine incl. Offline/NotConfigured; consent
  grant/withdraw round-trip; validation (blank name, bad amount, liability>0, future date);
  double-save ignored; failure → `featureError`.
- ArchUnit: `com.dhruv.finance.networth` in slices — green. Root `coveredModules` includes networth.

**M0 milestone (blocking, after data layer)**: device auth smoke — consent → Google sign-in →
authenticated GET succeeds. No feature UI work before M0 passes.

## 7. Rollout & rollback

- Flag kill-switch: `networth.enabled=false` → Home renders `FeatureDisabledCard`; rest of app
  identical to today. Verified in smoke.
- Rollback = disable flag or revert PR; server data unaffected either way.
- Version: 1.3.0 baseline in versions.json; CI auto-bumps patch + tags + releases APK as usual.

## 8. Verification

```
./gradlew :apps:finance:data:testDebugUnitTest
./gradlew :apps:finance:feature:networth:testDebugUnitTest
./gradlew :apps:finance:app:testDebugUnitTest   # ArchUnit
./gradlew :apps:finance:app:assembleDebug
./gradlew :apps:finance:app:assembleRelease     # minified smoke (dummy .env)
./gradlew regressionCheck
```
Device smoke: consent → sign-in → add asset → valuation → hero updates → restart → session
restored → airplane mode → Offline retry → blank .env → NotConfigured → withdraw consent → gated →
delete my data → empty → delete my account → fresh sign-in required → flag off →
FeatureDisabledCard → all existing tools still work.

## 9. Risks — all closed at plan time

| # | Risk | Resolution |
|---|------|-----------|
| R1 | Online-only | Accepted (U7); Offline is a specced state; P6 cache revisit |
| R2 | OAuth SHA-1 friction | M0 blocking milestone + `signingReport` checklist |
| R3 | supabase-kt AGP-9 unknown | Eliminated — REST on existing Retrofit/Moshi/OkHttp |
| R4 | Cert pinning vs rotation | CA-level pins (ISRG Root X1+X2) |
| R5 | Coverage floor | Pure-logic tests first; floor untouched (ADR-0013) |
| R6 | DPDP full erasure | `delete_my_account()` security-definer RPC |

## TDD Mandate

> **Test-Driven Development (TDD) is strictly required for this phase.**
> All pure logic, calculators, reducers, and state machines MUST be written with failing tests first, followed by implementation. UI components must be tested for accessibility and rendering states on both Android and Web platforms.

