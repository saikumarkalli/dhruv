# P1 Net Worth Tracker — Execution Checklist (session handoff)

> Purpose: any future session picks up exactly here. Read order for context:
> `platform/DECISIONS.md` ADR-0014 → `docs/superpowers/specs/2026-07-03-tracker-roadmap-overview.md`
> → `2026-07-03-p1-networth-tracker-design.md` (build target) → `2026-07-03-tracker-design-system.md`
> (UI rules) → `2026-07-04-tracker-engineering-playbook.md` (gates/roles) →
> `2026-07-04-p1-gap-analysis.md` (why decisions look like this).
>
> Branch: `feat/networth-tracker` (from develop). Session of 2026-07-03/04 produced planning +
> spec docs ONLY — no code yet. Work strictly top-to-bottom; each step verifiable before the next.

## Legend
- **[YOU]** = maintainer manual action. **[CC]** = Claude Code session action.
- TDD steps: test file FIRST, then implementation, then run module tests.

---

## A. Cleanup + docs commit — [CC]
- [ ] A1. Unstage unrelated currency files (separate work, do not mix):
      `docs/superpowers/plans/2026-07-03-currency-metals-notification.md`,
      `docs/superpowers/specs/2026-07-03-currency-realtime-rates-daily-notification-design.md`
- [ ] A2. Commit staged doc suite (ADR-0014 in DECISIONS.md, `dhruv-finance.json` networth flag,
      `versions.json` 1.3.0, 10 spec docs, this checklist) — docs-only commit:
      `docs: ADR-0014 tracker-first pivot + complete P1-P6 spec suite`

## B. Manual prerequisites — [YOU] (~30 min, blocking for C10+)
- [ ] B1. Create Supabase project (free tier); note URL, anon key, hosting region (region goes
      into consent copy).
- [ ] B2. Run the FROZEN SQL from P1 spec §4.1 in the Supabase SQL editor (3 tables, RLS policies,
      `auth.uid()` defaults, moddatetime triggers, `delete_my_account()` function).
- [ ] B3. Google Cloud console: create **Web application** OAuth client; paste its client ID +
      secret into Supabase → Auth → Providers → Google.
- [ ] B4. `./gradlew signingReport` → collect debug + release SHA-1s → create **Android** OAuth
      client(s) for package `com.dhruv.finance` with those SHA-1s.
- [ ] B5. Local `.env` (repo root, gitignored): `SUPABASE_URL=…`, `SUPABASE_ANON_KEY=…`,
      `GOOGLE_WEB_CLIENT_ID=…` (the WEB client ID, not the Android one).
- [ ] B6. GitHub repo → Actions secrets: same three keys (release builds need them).

## C. Data layer (TDD) — [CC]
- [ ] C7. **T2**: `apps/finance/data/src/test/.../util/PaiseTest.kt` →
      `apps/finance/data/src/main/.../util/Paise.kt`
      (`parseToPaise: String -> Long?`; `formatPaise` via existing `CurrencyFormatter`;
      `formatPaiseCompact` ₹X.XL / ₹X.XCr / plain < 1L; negatives for liabilities).
      Verify: `./gradlew :apps:finance:data:testDebugUnitTest`
- [ ] C8. **T3a**: version catalog += androidx `credentials`, `credentials-play-services-auth`,
      `googleid` (NO supabase-kt/ktor — ADR-0014 §6). `.env.example` += the 3 keys with empty
      values. App build.gradle exposes them as BuildConfig via existing secrets plugin.
- [ ] C9. **T3b**: `apps/finance/data/.../remote/` — `SupabaseAuthApi` (Retrofit: id_token grant,
      refresh_token grant, logout), `SupabaseRestApi` (assets/liabilities/valuation_entries CRUD +
      `rpc/delete_my_account`), OkHttp interceptor (apikey + Bearer, transparent refresh on 401,
      single retry), `CertificatePinner` ISRG Root X1+X2, blank-config → NotConfigured.
      `auth/IAuthRepository.kt` + `AuthRepository.kt` (AuthState: SignedOut/SignedIn/Loading/
      NotConfigured; encrypted-DataStore session store; cold-start restore; `deleteAllMyData()`;
      `deleteMyAccount()`), Koin wiring, `AuthRepositoryTest` (fake boundary).
      R8: proguard-rules.pro += DTO package + credentials/googleid keeps.
      ci.yml release job += write `.env` from secrets before assembleRelease.
- [ ] C10. **T4**: Moshi DTOs (no user_id/updated_at — server defaults) → `INetWorthRepository` +
      `SupabaseNetWorthRepository` behind fakeable `NetWorthRemoteDataSource` (StateFlow cache,
      Mutex single-flight refresh, append-only enforced) → `NetWorthRepositoryTest`.

## D. M0 gate — [YOU]+[CC] (BLOCKING — no UI before this passes)
- [ ] D11. Device smoke: temporary debug entry (or first wired screen): consent → Google sign-in →
      one authenticated GET returns 200. Proves B3–B5 correct while zero UI code exists.

## E. Feature module (TDD) — [CC]
- [ ] E12. **T5**: `settings.gradle.kts` += `:apps:finance:feature:networth`; module scaffold from
      loans template; **root `build.gradle.kts` `coveredModules` += networth** (hardcoded list —
      gap G2). Verify empty module builds.
- [ ] E13. **T6**: `NetWorthCalculatorTest` (10 cases: empty; latest-per-parent; assets−liabilities;
      tie-break createdAt→id; soft-deleted parent/valuation excluded; device-TZ month delta; trend
      per distinct timestamp; negative net worth; holding absent before first valuation) →
      `domain/NetWorthCalculator.kt` (pure, no Android imports) + `NetWorthModels.kt`.
- [ ] E14. **T7**: VM tests (Turbine + FakeNetWorthRepository + FakeAuthRepository) →
      `NetWorthDashboardViewModel` (state machine ConsentNeeded→SignedOut→Loading→Content/Error/
      Offline/NotConfigured; `trace("networth_summary")`; sub-screen state) +
      `NetWorthEditorViewModel` (validation: blank name, bad amount, liability>0, no future date;
      save-in-flight guard; `trace("networth_save")`). Consent = persisted SettingsRepository
      boolean + withdraw (NOT assistant's in-memory pattern — gap G3). Koin module + registration.

## F. UI — [CC] (design-system doc is binding; micro-frontend rule)
- [ ] F15. **T8a**: `:libs:core` `com.dhruv.core.ui.components.*` — BentoGrid, BentoCard,
      HeroStatCard, StatDeltaChip, TrendLineChart, EmptyStateCard, OfflineBanner, RetryErrorCard,
      ConsentGateScaffold, DhruvModalSheet, ConfirmDangerDialog. New files only; MaterialTheme
      roles only; previews per component.
- [ ] F16. **T8b**: feature screens composing core components (ZERO local styling):
      `NetWorthScreen` (state machine + overflow: Refresh/Sign out/Withdraw consent/Delete my
      data/Delete my account), `DashboardContent` (hero + assets/liabilities cards + empty state),
      `ConsentGateContent`/`SignInContent` (Credential Manager flow), `HoldingListScreen`,
      `HoldingDetailScreen`, `EditorSheets`.

## G. Navigation — [CC]
- [ ] G17. **T9**: `ui/hub/ToolsHub.kt` launcher grid (cards: Calculator/Converter/Finance/Date/
      Time; visibility = feature flag AND existing Settings toggle — gap G15; FeatureHost blocks
      moved VERBATIM from MainActivity; BackHandler to grid; per-tool SectionTheme;
      calculatorViewModel passthrough). MainActivity tabs → home/tools/assistant/settings
      (settings stays top-bar-only; testTags `nav_item_<key>` kept). Delete placeholder
      `DashboardScreen.kt`. App build.gradle += networth dep. Settings subtitles "shown in Tools".
      Verify ArchUnit green.

## H. Docs sync — [CC]
- [ ] H18. **T10**: `apps/finance/FEATURES.md` networth section; `apps/finance/CLAUDE.md` (module
      list, new nav shape, `.env` config note); `platform/PRODUCTION_READINESS.md` tracker rows;
      `IMPLEMENTATION.md` pointer to ADR-0014 + spec suite.

## I. Quality gates — [CC] (playbook order)
- [ ] I19. `/code-review` high effort on branch diff — every finding fixed or explicitly waived.
- [ ] I20. `/dhruv-security` (consent both ways, no PII/values in logs, session storage, .env vs
      GitLeaks, erasure) + `/dhruv-boundaries` + `/dhruv-ui-review` (a11y checklist) +
      `/dhruv-coverage`.
- [ ] I21. `./gradlew regressionCheck` + `assembleRelease` (dummy .env) + full device smoke script
      (P1 spec §8: consent→sign-in→add→valuation→restart→offline→NotConfigured→withdraw→delete
      data→delete account→flag off→tools intact).
- [ ] I22. `/dhruv-pre-merge` — single PASS verdict. Fill outcome column in
      `2026-07-04-p1-gap-analysis.md`.

## J. Ship — [CC]+CI
- [ ] J23. Push branch, PR → `develop` (4 CI gates must pass; Dhruv CI Bot sticky comment).
- [ ] J24. Self-merge → CI auto: version-bump 1.3.0→1.3.1, tag `dhruv-finance-v1.3.1`,
      GitHub Release with signed APK.
- [ ] J25. [YOU] Install APK, live smoke. P1 DONE → next phase: P2 spec → same cycle.

---

## Hard rules for any executing session (from playbook — do not relax)
1. Order is binding; M0 (D11) blocks all UI work.
2. Existing feature modules byte-untouched; `AppDatabase` untouched; shared-file edits additive-only.
3. TDD: tests before implementation for all logic; regressionCheck green at every commit.
4. All visuals from `:libs:core` components; zero feature-local styling.
5. No new networking/serialization frameworks (ADR-0014 §6).
6. Schema is FROZEN (P1 spec §4.1) — any change = spec amendment first, not code drift.
