# Dhruv Finance — QA Test Scenario Catalog (design v1.0 FINAL build-out)

> **Process this catalog serves:** `2026-08-09-module-standard-and-tdd-process.md` §2 — no
> engineering task starts until its module's rows here exist and are reviewed. Every row cites the
> functional-spec anchor (`2026-08-08-design-v1-final-functional-spec.md`) it was derived from —
> nothing here is invented; it is the spec's business rules, flows and NFRs made testable.
>
> **Status legend:** ☐ defined → 🔴 test written (RED) → 🟢 implemented (GREEN) → ✅ QA closed.
> Every row starts **☐** — that is the point: scenarios exist before code.
>
> **Owner column:** role from the RACI in the standard doc (`Backend`, `Android`, `QA`, `SA`).

---

## 0. Cross-cutting NFR checklist (applies to every module below, not repeated per row)

Each module's phase checkpoint (standard doc §4 step 6) re-runs this table against that module's
screens instead of every module carrying its own copy.

| ID | Check | Source | Automatable |
|---|---|---|---|
| NFR-001 | No off-device call fires before its A3 consent switch is on | NFR-1 | Y (DAT-BR-001) |
| NFR-002 | Every route is `FeatureHost`-wrapped; a thrown error shows `FeatureErrorCard`, never a blank screen | NFR-2 | Y |
| NFR-003 | Every tracker money field is `Long` paise, no `Double`/`Float` | NFR-3 | Y (`checkTrackerMoneyPrecision` Gradle task — scheduled Phase 1, not built yet) |
| NFR-004 | Every network-backed screen renders `SignedOutCard`/`OfflineStateCard`/loaded correctly per session+connectivity state | NFR-4 | Y (Robolectric-Compose) |
| NFR-005 | Screen renders identically light/dark from the same token set (no raw hex/dp/sp literal) | NFR-5 | Partial (detekt for literals; visual diff manual) |
| NFR-006 | 4.5:1 text contrast, ≥48dp touch targets, content descriptions on icon-only actions, tabular numerals for money | NFR-6 | N (manual, `dhruv-ui-review`) |
| NFR-007 | Standard easing `cubic-bezier(.16,1,.3,1)`; splash ≤2.5s; charts animate once | NFR-7 | Partial (splash timing Y; motion feel N) |
| NFR-008 | Tab switch <100ms; lists virtualised; charts read pre-aggregated view data, no client-side full-ledger reduction | NFR-8 | Partial (perf trace Y; reduction-free Y via code review) |
| NFR-009 | No secret in repo/APK; anon key via `.env`; CA-level pinning | NFR-9 | Y (DAT-BR-005, GitLeaks) |
| NFR-010 | `regressionCheck` green; coverage floor not regressed | NFR-10 | Y (CI) |

---

## 1. NAV — Shell & navigation

| ID | Given | When | Then | Source | Size | Auto | Owner | Status |
|---|---|---|---|---|---|---|---|---|
| NAV-ARCH-001 | any tab root screen | rendered | no back arrow is shown | N1 | S | Y | Android | ☐ |
| NAV-ARCH-002 | any non-root screen | rendered | exactly one back arrow targeting its single documented parent | N2 | S | Y | Android | ☐ |
| NAV-UI-001 | a bottom-sheet route (quick add, filter, consent, app switcher) is open | user swipes/taps down | sheet dismisses; no navigation entry is pushed or popped | N3 | S | Y | Android | ☐ |
| NAV-FLOW-001 | an add/edit form (C4, D3) has unsaved changes | user presses back | a discard-confirmation dialog appears before the form closes | N4 | M | Y | Android | ☐ |
| NAV-UI-002 | any tab is active | top bar renders | Alerts, Apps-switcher and Settings icons are present and functional | N5 | S | Y | Android | ☐ |
| NAV-FLOW-002 | app is cold-launched via a deep link to a non-root route | app finishes launch | the owning tab's root is on the back stack below the deep-linked screen | N6 | M | Y | Android | ☐ |
| NAV-UI-003 | any screen | theme toggled System→Light→Dark in Settings | only token values change; layout/structure is identical | N7 | M | N (visual) | QA | ☐ |
| NAV-ARCH-003 | `TabKey` list reordered or a tab hidden by flag | `NavTarget.SelectTab` dispatched for a tab | `pageIndexFor` resolves by key, not position (existing `NavTargetTest`, extended for 5 tabs) | impl plan §4.1 ("NAV4" is an implementation-plan-internal label, not a functional-spec ID — corrected 2026-08-09, was miscited as a spec anchor) | S | Y | Android | 🟢 |
| NAV-FLOW-003 | Insights tab hidden by flag mid-session | `SelectTab(INSIGHTS)` dispatched | pager falls back to the first visible tab, no crash | impl plan §4.1 ("NAV4") | S | Y | Android | 🟢 (`NavTargetTest.pageIndexFor falls back to the first visible tab when the target tab is hidden`, updated for the 5-tab list — corrected 2026-08-09, catalog was stale) |
| NAV-UI-004 | Money or Insights module not yet built (Phase 0/pre-Phase-5) | tab opened | `NotConfiguredCard` renders with the phase-appropriate message | spec §2.2 | S | Y | Android | 🟢 |
| NAV-ARCH-004 | any two feature Gradle modules | `DependencyRulesTest` runs | neither imports the other directly | PLATFORM.md §4 | S | Y | SA | 🟢 (pre-existing, re-verified per new module) |

---

## 2. ONB — Onboarding (A2–A4)

| ID | Given | When | Then | Source | Size | Auto | Owner | Status |
|---|---|---|---|---|---|---|---|---|
| ONB-FLOW-001 | cold install, no session | splash finishes | A2 sign-in is shown | F-1 | S | Y | Android | 🟢 `onboarding/.../OnboardingViewModelTest.kt#cold install with no session shows SignIn` |
| ONB-BR-001 | A2 is shown, no consent decision made yet | "Continue with Google" tapped | Google auth completes but zero tracker network calls fire before A3 | F-1, NFR-1 | M | Y | Backend | 🟢 `onboarding/.../OnboardingViewModelTest.kt#sign-in touches only AuthRepository, never the consent repository` + `data/.../ConsentInterceptorTest.kt` (auth is unconditionally pre-consent by construction — `authClient` never carries `ConsentInterceptor`, `SupabaseClientFactoryTest.kt`) |
| ONB-FLOW-002 | Google sign-in succeeds | — | A3 DPDP consent is shown next, never skipped | F-1 | S | Y | Android | 🟢 `onboarding/.../OnboardingViewModelTest.kt#sign-in success transitions SignIn to Consent with the repository's current switches` |
| ONB-BR-002 | user declines every A3 switch | app enters the shell | Calc + Plan calculators fully usable; Home/Money/Insights show `SignedOutCard` | §2.1, NFR-4 | M | Y | Android | 🟢 `onboarding/.../OnboardingViewModelTest.kt#declining every consent switch still lets Continue proceed to EmptyStart` — **implemented shape differs from this row's literal wording**: declining does not skip straight to the shell; it proceeds to A4 (matches F-1's flow diagram, `Sign-in → Consent → Empty start` unconditionally), and the shell is reached from A4 via the "Skip for now" action added by the final whole-branch review's fix wave (`#resetForNewOnboardingSession …`/`onSkipEmptyStart`, same file). Calc/Plan calculators being usable regardless of consent is confirmed structurally (they render outside any consent check, `MainActivity.kt`'s `CalcTab`/`PlanTab`), not by a dedicated test. `SignedOutCard` itself is defined (`libs/core/.../StateCards.kt`) but has no tracker-screen caller yet — Home/Money/Insights render `EmptyStateCard`/`NotConfiguredCard` today, correct for Phase 1 scope (those screens are Phase 2+), not yet the row's literal `SignedOutCard` claim. |
| ONB-BR-003 | "Sync my financial records" switch is OFF | any tracker repository method is called | `ConsentInterceptor` rejects before any HTTP request is made | NFR-1 | S | Y | Backend | 🟢 `data/.../tracker/net/ConsentInterceptorTest.kt` (Task 1) — same test also closes DAT-BR-001, see §11 |
| ONB-BR-004 | any A3 switch is toggled | app is force-killed and reopened | the switch's value is unchanged (persisted, not in-memory) | A3 | S | Y | Backend | 🟢 `data/.../tracker/auth/ConsentRepositoryTest.kt` (DataStore round-trip persistence tests) |
| ONB-BR-005 | a consent switch is ON | user turns it OFF in Settings › Privacy | value persists OFF; other switches are unaffected | A3 | S | Y | Backend | 🟢 `data/.../tracker/auth/ConsentRepositoryTest.kt` + `app/.../SettingsScreen.kt`'s 3 switches wired straight to `ConsentRepository.setXxx` (Task 4, independently verified switch-to-setter matching in that task's review) |
| ONB-BR-006 | "Sync my financial records" is turned OFF while tracker screens are visible | — | Home/Money/Plan-live/Insights immediately degrade to `SignedOutCard` | ADR-0014 §7 | M | Y | Android | ☐ (deferred — no tracker-content screens exist yet to degrade; Home/Money/Insights are Phase 2+. `ConsentRepository.state` already flips immediately and reactively, so the mechanism this row needs is in place — only the consumer screens are missing. Re-open and close for real once Phase 2's Home/Money screens land.) |
| ONB-FLOW-003 | A2 shown | "Use offline — calculators only" tapped | shell opens directly; A3/A4 never shown; Calc/Plan-calculators live | F-1 | S | Y | Android | 🟢 `onboarding/.../OnboardingViewModelTest.kt#use offline selected exits to shell without ever showing Consent or EmptyStart` |
| ONB-FLOW-004 | A4 shown, zero accounts and zero holdings | user completes "Add your first account" OR "Record what you own" | app exits A4 to Home (01) | A4 | M | Y | Android | ☐ (deferred — Phase 2. `EmptyStartScreen`'s two actions dispatch `NavTarget`s toward their eventual owner tab today, inert until `AppShell` is reachable from there and D6/C4 exist; `hasAccountOrHolding()` is honestly stubbed `false`, documented in `OnboardingViewModel.kt`.) |
| ONB-BR-007 | A4 shown | user taps "Import a CSV" | mapper flow opens (Phase-gated — see functional spec open item §8.1) | A4 | L | N | Backend | ☐ (deferred — open item, unchanged) |
| ONB-BR-008 | user has tracker data | "Delete my data" tapped and confirmed | every tracker row for that user is hard-deleted within the same session, not just soft-deleted | ADR-0014 §7 | M | Y | Backend | 🟢 `data/.../tracker/auth/TrackerAccountRepositoryTest.kt` (Kotlin call/composition proven against a fake RPC boundary). The SQL function's actual row-deletion behavior against real data is `DAT-FLOW-001` (§11) — Automatable:N, not yet run against the dev project. |
| ONB-BR-009 | user requests account deletion | "Delete my account" tapped and confirmed | `delete_my_account()` removes all rows + the `auth.users` row; session is force-signed-out | ADR-0014 §7 | M | N (against dev project) | Backend | ✅ (closed 2026-08-15, both halves) Kotlin: `data/.../tracker/auth/TrackerAccountRepositoryTest.kt` — RPC call + `sessionStore.clear()` + `consentRepository.setHasCompletedOnboarding(false)`, success-only-gated. SQL, against the live `dhruv` project: called `delete_my_account()` as the test user (via `SET ROLE authenticated` + `request.jwt.claim.sub`), then confirmed `select count(*) from auth.users where id = '<test uuid>'` returned `0` — the `auth.users` row is genuinely gone, not just the tracker rows. |
| ONB-FLOW-005 | user has synced tracker data | Settings › Privacy → toggles a consent OFF, then separately runs "Delete my data" | dependent surfaces degrade immediately on the toggle (ties ONB-BR-005/006 together); the erasure action then removes the data entirely — both steps of the withdrawal-then-erasure sequence work in order | flow F-8 (functional-spec §6 "consent withdrawal / erasure") | M | Y | Backend | 🟢 `data/.../tracker/auth/TrackerAccountRepositoryTest.kt#TrackerAccountRepositoryConsentDeclinedTest` (added during Task 4's Critical fix round — proves erasure survives declined consent through the real interceptor chain, the exact toggle-then-erase sequence this row describes) |

---

## 3. NW — Net worth (C1–C7)

| ID | Given | When | Then | Source | Size | Auto | Owner | Status |
|---|---|---|---|---|---|---|---|---|
| NW-BR-001 | C4 form filled and saved | save completes | one `holdings` row AND its first `valuations` row are written atomically (both-or-neither) | BR-C2 | M | Y | Backend | ☐ |
| NW-BR-002 | a holding has ≥1 valuation | C5 "Record valuation" is submitted | a **new** `valuations` row is inserted; the previous row is untouched | BR-C1 | S | Y | Backend | ☐ |
| NW-BR-003 | a wrong valuation exists | user corrects it | old row is soft-deleted (`deleted_at` set) and a new corrected row is appended; no UPDATE occurs on the value | BR-C1 | M | Y | Backend | ☐ |
| NW-BR-004 | C4 sector field | any value is submitted | only the fixed enum set is accepted; free text is rejected at the repository boundary | C4 | S | Y | Backend | ☐ |
| NW-BR-005 | a sector/liability-type enum constant has shipped | a future migration is authored | the constant is never renamed/removed (migration-review checklist, not a runtime test) | BR-C3 | S | N | SA | ☐ |
| NW-BR-006 | assets and liabilities with varying valuation dates | C1 net worth is computed | equals Σ latest asset valuations − Σ latest liability outstandings from `v_net_worth_by_sector`, not a client-side reduction | BR-C4, NFR-8 | M | Y | Backend | ☐ |
| NW-UI-001 | C1 donut with ≥2 sectors | a sector segment is tapped | C2 opens filtered to that sector | C1 | S | Y | Android | ☐ |
| NW-UI-002 | a holding with 3+ valuation entries | C3 is opened | entries are ordered newest-first, each shows its delta vs the entry before it | C3 | M | Y | Android | ☐ |
| NW-UI-003 | a holding's last valuation is known | C5 sheet opened, new value typed | delta (amount + %) updates live against the last recorded value, before submit | C5 | M | Y | Android | ☐ |
| NW-UI-004 | a liability with principal/interest paid amounts | C7 opened | amortisation donut's three segments (principal paid / interest paid / remaining) sum to total obligation | C7 | M | Y | Android | ☐ |
| NW-FLOW-001 | C4 opened | holding saved | C1's total updates to include it | F-2 | M | Y | Android | ☐ |
| NW-FLOW-002 | C3 opened | "Update value" → C5 → Record | C3's chart and XIRR recompute; C1 total updates | F-2 | M | Y | Android | ☐ |
| NW-UI-005 | session signed out / device offline with nothing cached | C1, C2, or C6 opened | `SignedOutCard` / `OfflineStateCard` renders per NFR-4, not a blank or crashing screen | NFR-4 | M | Y | Android | ☐ |
| NW-BR-007 | a holding's full valuation-entry set | XIRR is displayed on C3 | computed over that holding's cashflow set per the ADR-reserved XIRR definition (blocked — spec open item §8.6) | C3 | L | N | SA | ☐ (blocked on ADR) |

---

## 4. MNY — Money / ledger (D1–D9)

| ID | Given | When | Then | Source | Size | Auto | Owner | Status |
|---|---|---|---|---|---|---|---|---|
| MNY-BR-001 | a TRANSFER transaction exists between two of the user's accounts | expense totals/budgets/category shares are computed | the transfer is excluded from all three | BR-D1 | M | Y | Backend | ☐ |
| MNY-BR-002 | a CREDIT_CARD account has spend on it | "spendable now" (D6) is computed | its negative balance is excluded from the sum | BR-D2 | S | Y | Backend | ☐ |
| MNY-BR-003 | a category is renamed | — | its id and all linked transactions are unchanged; only the label changes | BR-D3 | S | Y | Backend | ☐ |
| MNY-BR-004 | two categories with N and M transactions | user merges them | `ConfirmDangerDialog` states "N+M transactions will move"; merge is irreversible once confirmed | BR-D3 | M | Y | Android | ☐ |
| MNY-BR-005 | a recurring template's next-run date arrives | scheduler runs | a `suggestions` row is created, not a `transactions` row | BR-D4 | M | Y | Backend | ☐ |
| MNY-BR-006 | any transaction mutation (create/edit/category-change/delete) | mutation completes | a `transaction_events` row is appended describing it | BR-D5 | M | Y | Backend | ☐ |
| MNY-UI-001 | D1 open, FAB tapped | amount entered → category confirmed → account confirmed → Save | entry completes in 3 taps after the amount pad opens | D2 | M | N (manual tap-count check) | QA | ☐ |
| MNY-UI-002 | a month with transactions across multiple days | D1 opened | rows are day-grouped; pinned header shows correct INCOME/EXPENSE/SAVED% | D1 | M | Y | Android | ☐ |
| MNY-UI-003 | D5 filter sheet open | any filter chip toggled | result count updates before "Show N results" is tapped | D5 | S | Y | Android | ☐ |
| MNY-UI-004 | accounts of every type exist | D6 opened | "SPENDABLE NOW" total matches MNY-BR-002 exactly | D6 | S | Y | Android | ☐ |
| MNY-UI-005 | an account's `reconciled_at` is beyond the staleness threshold | D7 opened | reconciliation banner shows; tapping "Fix" clears it after reconciliation completes | D7 | M | Y | Android | ☐ |
| MNY-FLOW-001 | D1 open | FAB → D2 → Save | new row appears in today's group; month summary and any affected budget update | F-3 | M | Y | Android | ☐ |
| MNY-FLOW-002 | D3 open, "Make it recurring" toggled with a schedule | Save | a `recurring_templates` row is created; no duplicate immediate transaction is written | D3, BR-D4 | M | Y | Backend | ☐ |
| MNY-FLOW-003 | D3 open, "Link to a goal" set | Save | `transactions.goal_id` is set; E5's FUNDED BY reflects it (cross-module — depends on PLN) | D3 | M | Y | Backend | ☐ |
| MNY-NFR-001 | `tracker/money/**` source | `checkTrackerMoneyPrecision` runs (Phase 1 task, not yet built) | zero `Double`/`Float` usages on a money-bearing field | NFR-3 | S | Y | Backend | ☐ |
| MNY-UI-006 | a transaction with a receipt, budget link, and 2+ history events | D4 opened | amount/payee/datetime/cleared-state render; budget-impact line matches the linked category's usage %; HISTORY lists every event in order | D4 | M | Y | Android | ☐ (added 2026-08-09 — D4 had zero coverage; budget-impact clause ships Phase 4, see impl plan Phase 3/4 notes) |
| MNY-FLOW-004 | D4 open | "Duplicate" tapped | a new draft transaction pre-filled from the original opens in D3, not yet saved | D4 | S | Y | Android | ☐ (added 2026-08-09) |
| MNY-FLOW-005 | D4 open | "Make recurring" tapped | D3's recurring toggle opens pre-filled from this transaction (same path as MNY-FLOW-002) | D4 | S | Y | Android | ☐ (added 2026-08-09) |
| MNY-UI-007 | categories with sub-categories, budgets, and an excluded-from-spend category | D8 opened | Expense/Income tabs show correct counts; each row shows the right special-case rendering (budgeted amount, "Excluded from spend", "N need a category") | D8, BR-D3 | M | Y | Android | ☐ (added 2026-08-09 — D8 had zero direct coverage) |
| MNY-UI-008 | recurring templates due within 30 days, one paused | D9 opened | MONTHLY IN/OUT totals correct; NEXT 30 DAYS list ordered by date with correct auto-debit/variable-amount tags; PAUSED section shows the paused entry with its pause date | D9, BR-D4 | M | Y | Android | ☐ (added 2026-08-09 — D9 had zero direct coverage) |

---

## 5. PLN — Planning: budgets, goals, debt payoff (E1–E6)

| ID | Given | When | Then | Source | Size | Auto | Owner | Status |
|---|---|---|---|---|---|---|---|---|
| PLN-BR-001 | a goal linked to 2 holdings (one whole, one earmarked) | goal progress is computed | equals Σ current value of both links; no `transactions` write occurs from linking | BR-E1 | M | Y | Backend | ☐ |
| PLN-BR-002 | a budget period (calendar month) | pace is computed on day N of a 30-day month | pace = N/30, matching the E2 "X% faster/slower" statement for ≥3 fixtures | BR-E2 | M | Y | Backend | ☐ |
| PLN-BR-003 | a category with `excluded_from_spend = true` (e.g. Investment) | budgets/E2 totals are computed | that category's spend is excluded | BR-E3 | S | Y | Backend | ☐ |
| PLN-UI-001 | 3 pace fixtures (ahead/on/behind pace) | E2 opened | the pace statement text matches each fixture's math exactly | E2 | M | Y | Android | ☐ |
| PLN-UI-002 | a category bar and the month-position marker | E2 opened | bar renders "ahead of pace" styling iff spend-fraction > elapsed-day-fraction | E2 | M | Y | Android | ☐ |
| PLN-UI-003 | a category's last-N-transaction average and current overage | E3 opened | recovery insight's stated numbers match a recomputation from the same transactions | E3 | M | Y | Android | ☐ |
| PLN-UI-004 | 3 goal fixtures (on-track / needs-more/mo / unfunded) | E4 opened | each shows the exact matching status text | E4 | M | Y | Android | ☐ |
| PLN-UI-005 | a goal with one whole-linked and one earmarked holding | E5 opened | FUNDED BY shows the earmark quantity/fraction correctly, not the holding's full value | E5 | M | Y | Android | ☐ |
| PLN-BR-004 | a fixture of ≥3 debts with varying APR and balance | avalanche vs snowball order is computed | avalanche = APR descending; snowball = balance ascending | E6 | M | Y | Backend | ☐ |
| PLN-UI-006 | the same debt fixture | both orderings' projections are computed | "N months slower, ₹X more interest" statement is internally consistent with both projections | E6 | M | Y | Android | ☐ |
| PLN-FLOW-001 | Plan tab opened (post Phase-4 rewrite) | — | E1 shows live modules first, calculator strip (Loan/SIP/Tax/Everyday) below | ADR-0027, E1 | S | Y | Android | ☐ |
| PLN-FLOW-002 | E4 open | "Link another holding" → pick → confirm | E5 progress recomputes; assert no call to any transaction-write endpoint occurred | F-5, BR-E1 | M | Y | Backend | ☐ |
| PLN-FLOW-003 | a budget over 100% | B2 shows the overrun notification | tapping it opens E3; E3's recovery insight and "Raise budget"/"Alert me at 80%" actions work; the chained B2→E3 sequence is exercised end-to-end, not just E3 in isolation | F-4 | M | Y | Android | ☐ (added 2026-08-09 — F-4 had no chained-flow row, only its pieces) |
| PLN-BR-005 | E3's recovery insight, E6's avalanche/snowball trade-off statement, and any other derived/AI insight text | rendered | each is visually labelled as a derived/AI insight, not presented as a plain fact | BR-E4 (2nd clause) | S | Y | Android | ☐ (added 2026-08-09 — BR-E4's insight-labelling clause had zero coverage; only its 1st clause, assumptions-visible, was tested via RET-UI-002) |

---

## 6. INS — Insurance (E7–E8)

| ID | Given | When | Then | Source | Size | Auto | Owner | Status |
|---|---|---|---|---|---|---|---|---|
| INS-BR-001 | annual income + outstanding loans fixture | E7 rule-of-thumb cover is computed | equals 10× income + outstanding loans; shortfall = rule-of-thumb − actual cover | E7 | M | Y | Backend | ☐ |
| INS-UI-001 | a policy's `renews_on` inside vs outside the renewal window | E7 opened | banner shows only when inside the window, states correct days-remaining and lapse consequence | E7 | M | Y | Android | ☐ |
| INS-UI-002 | a policy set missing a risk category (e.g. no personal accident) | E7 opened | GAPS section names that specific missing category | E7 | M | Y | Android | ☐ |
| INS-FLOW-001 | a policy with a due premium | E8 "Mark as paid" tapped | `policy_premiums` row appended; E7's renewal banner for that policy clears | E8 | M | Y | Backend | ☐ |

---

## 7. RET — Retirement (E9)

| ID | Given | When | Then | Source | Size | Auto | Owner | Status |
|---|---|---|---|---|---|---|---|---|
| RET-BR-001 | a fixed base-assumption set | each assumption is varied one at a time (retire age, inflation, pre/post-return, life expectancy) | projected corpus changes in the expected direction, matching golden-value fixtures | E9 | L | Y | Backend | ☐ |
| RET-UI-001 | one base input set | Base/Optimistic/Cautious toggled | three distinct corpus values are shown (scenarios are not aliased) | E9 | M | Y | Android | ☐ |
| RET-UI-002 | E9 opened | — | all 5 assumption fields are visible on the same screen as the projected corpus (no drill-in needed) | E9 | S | Y | Android | ☐ |
| RET-FLOW-001 | assumptions set to non-default values | "Save this scenario" tapped, screen reopened later | the saved scenario's values reappear (persisted to `retirement_scenarios`) | E9 | M | Y | Backend | ☐ |

---

## 8. SIG — Insights (F1–F5)

| ID | Given | When | Then | Source | Size | Auto | Owner | Status |
|---|---|---|---|---|---|---|---|---|
| SIG-BR-001 | a period's transactions (in/out/transfers) | F2 cashflow is computed | opening + money_in − money_out − net(moved_not_spent) = closing, for ≥3 period fixtures | F2 | M | Y | Backend | ☐ |
| SIG-BR-002 | a month's income/expense lines + the same month last year | F3 is computed | line sums equal displayed totals; YoY% compares the same calendar month | F3 | M | Y | Backend | ☐ |
| SIG-BR-003 | asset/liability balances as at a date | F4 is computed | assets − liabilities = the displayed net worth, for ≥3 date fixtures | F4 | M | Y | Backend | ☐ |
| SIG-UI-001 | a period with known income/surplus | F1 ring shown | savings-rate % matches surplus/income and matches F2's numbers for the same period | F1 | M | Y | Android | ☐ |
| SIG-UI-002 | two months of fixture transactions | F1 "WHERE IT WENT" opened | per-category vs-last-month % matches a recomputation | F1 | M | Y | Android | ☐ |
| SIG-FLOW-001 | F5 period picker | Month/Quarter/FY/Custom selected in turn | every statement's data updates consistently; FY uses Apr–Mar boundary | F5 | M | Y | Backend | ☐ |
| SIG-FLOW-002 | any statement on screen | export (CSV/PDF) tapped | exported totals match the on-screen statement for the same period | F5 | M | Y (file diff) | Backend | ☐ |
| SIG-FLOW-003 | F1 open for a period | user navigates F1→F2→F3→F4→F5 in sequence, matching that period throughout | every statement stays period-consistent across the chain; F5 export matches what was just viewed | flow F-6 (functional-spec §6 "month-end review" — not to be confused with screen ID F5/Reports) | M | N (manual chain walk) | QA | ☐ (added 2026-08-09 — F-6 had no chained-flow row) |

---

## 9. AUT — Automation (G1–G3)

| ID | Given | When | Then | Source | Size | Auto | Owner | Status |
|---|---|---|---|---|---|---|---|---|
| AUT-BR-001 | any automated source fires (SMS/AA/recurring) | a suggestion is produced | it is written to `suggestions`, never directly to `transactions` | BR-G1 | M | Y | Backend | ☐ |
| AUT-BR-002 | an SMS is parsed on-device | the resulting suggestion is sent to the network layer (only the suggestion, if synced) | no raw SMS text field is present in any outbound Supabase payload | BR-G2 | M | Y | Backend | ☐ |
| AUT-BR-003 | a rule has been applied N times | G1 opened | the rule row shows `applied_count = N` and a working disable/remove control | BR-G3 | S | Y | Android | ☐ |
| AUT-UI-001 | a pending suggestion | G2 opened | row renders with the dashed "not accepted" border treatment | G2 | S | Y | Android | ☐ |
| AUT-UI-002 | a suggestion whose amount+date+account closely matches an existing transaction | G2 opened | duplicate-detection callout appears on that row | G2 | M | Y | Backend | ☐ |
| AUT-FLOW-001 | a pending suggestion | "Accept" with a category tapped | a real `transactions` row is created with `source = SMS`; audit trail records "from SMS" | G2, BR-D5 | M | Y | Backend | ☐ |
| AUT-FLOW-002 | a pending suggestion | "Ignore" tapped | suggestion discarded; no transaction created | G2 | S | Y | Android | ☐ |
| AUT-FLOW-003 | user starts "Link an account" | G3 opened | scope, duration and purpose are stated before any consent action is available | G3, ADR-0014 §7 | S | Y | Android | ☐ |
| AUT-FLOW-004 | SMS-alerts source enabled in G1, a matching SMS exists | the chain runs: SMS parsed → suggestion in G2 → Accept → transaction in D1 | the full chain completes without a direct-to-ledger write anywhere in it (ties AUT-BR-001/AUT-FLOW-001 together as one sequence) | flow F-7 (functional-spec §6 "automation approval") | M | N (manual chain walk) | QA | ☐ (added 2026-08-09 — F-7 had no chained-flow row) |

---

## 10. SRC — Search & notifications (B2–B3)

| ID | Given | When | Then | Source | Size | Auto | Owner | Status |
|---|---|---|---|---|---|---|---|---|
| SRC-UI-001 | a search query with matches across types | B3 opened | filter-chip counts (`All N`, `Holdings n`, ...) match the actual grouped results | B3 | M | Y | Android | ☐ |
| SRC-FLOW-001 | a search result row of each entity type | tapped | navigates to that entity's detail screen (holding→C3, transaction→D4, goal→E5, policy→E8) | B3 | M | Y | Android | ☐ |
| SRC-UI-002 | notifications spanning today and earlier dates | B2 opened | grouped correctly by local calendar date into TODAY/EARLIER | B2 | S | Y | Android | ☐ |
| SRC-FLOW-002 | unread notifications exist | "Mark all read" tapped, app restarted | all remain read after restart | B2 | S | Y | Backend | ☐ |
| SRC-FLOW-003 | one notification of each type (budget overrun, EMI due, renewal, rate alert) | tapped | deep-links to its subject (E3, C7, E8, C3 respectively) | B2 | M | Y | Android | ☐ |

---

## 11. DAT — Tracker data layer (cross-cutting: auth, consent, RLS, precision)

| ID | Given | When | Then | Source | Size | Auto | Owner | Status |
|---|---|---|---|---|---|---|---|---|
| DAT-BR-001 | a consent flag is false | any tracker repository method touching that scope is called | `ConsentInterceptor` short-circuits before an HTTP request is dispatched | NFR-1 | S | Y | Backend | 🟢 `data/.../tracker/net/ConsentInterceptorTest.kt` — asserts `server.requestCount == 0` (MockWebServer never receives a request), the only way to actually prove "zero bytes hit the wire" |
| DAT-BR-002 | an active session | any tracker API request is built | `apikey` and `Authorization: Bearer` headers are present | ADR-0014 §6 | S | Y | Backend | 🟢 `data/.../tracker/net/AuthInterceptorTest.kt` |
| DAT-BR-003 | a request returns 401 | — | exactly one refresh-token attempt occurs; a second consecutive 401 forces `SignedOut`, no retry loop | §5.3 impl plan | M | Y | Backend | 🟢 `data/.../tracker/net/AuthInterceptorTest.kt` (single-refresh + forced-SignedOut-on-repeat-401 both asserted; no-retry-loop verified by request-count assertions, independently re-traced during Task 1's review for an off-by-one/infinite-loop risk — none found) |
| DAT-BR-004 | a session token is issued | stored | it is written only to encrypted DataStore; no plaintext `SharedPreferences` write occurs | ADR-0014 §6 | S | Y | Backend | 🟢 `data/.../tracker/auth/SessionStoreTest.kt` (round-trip against `EncryptedDataStoreFactory`; per Task 1's review, tested against an injected `DataStore<Preferences>` rather than the real Android Keystore — consistent with this repo's existing precedent, `SettingsRepositoryImplTest` has the same gap, not new here) |
| DAT-BR-005 | `CertificatePinner` config | inspected | pins match the CA Supabase's TLS chain actually roots to (CA level), not a leaf certificate | ADR-0014 §6, ADR-0029 correction 2026-08-15 | S | Y | Backend | 🟢 (re-opened and fixed 2026-08-15, not yet re-confirmed live) `data/.../tracker/net/SupabaseClientFactoryTest.kt` — Task 1's original pins (ISRG Root X1/X2) were unit-test-verified against Let's Encrypt's own certs but never exercised against a real Supabase connection; a real device's first live Google sign-in threw `SSLPeerUnverifiedException` against them. Actual root is Google Trust Services' GTS Root R4; corrected pins (GTS Root R1 + R4) independently verified against Google's published trust store (`pki.goog/repo/certs/gtsr{1,4}.der`), test updated to match. Not marked ✅ until a rebuilt app is confirmed reaching the real host on-device — see ADR-0029's Correction paragraph in `platform/DECISIONS.md`. |
| DAT-BR-006 | a new migration adds a table | migration review | an RLS policy scoped to `user_id = auth.uid()` is present | ADR-0014 §7 | M | N (manual, per migration) | SA | ✅ (closed 2026-08-15, SA/controller review — code review, no live project needed) `supabase/migrations/0001_init.sql`: `holdings` has `holdings_select_own`/`_insert_own`/`_update_own`, each `using (user_id = auth.uid())`; `valuations` (no `user_id` column of its own) has `valuations_select_own`/`_insert_own`, each scoped via `holding_id in (select id from holdings where user_id = auth.uid())` — transitively equivalent. Every future migration adding a table must repeat this pattern; this row re-opens per-migration, does not stay closed forever. |
| DAT-BR-007 | the `valuations` table | an UPDATE is attempted via the authenticated role against a dev project | it is rejected — no UPDATE policy exists | BR-C1 | M | N (against dev project) | Backend | ✅ (closed 2026-08-15, against the live `dhruv` project) Seeded a real holding+valuation, switched to `SET ROLE authenticated` + `request.jwt.claim.sub` set to a real `auth.users` id (via Management API SQL access, migration-review PAT), attempted `UPDATE valuations SET value_paise = 999999`, then re-selected as `postgres`: value was still the original `100000` — the UPDATE silently matched zero rows (RLS's expected no-policy-for-this-command behavior, not a hard error). Bonus: the same session's `INSERT`/`SELECT` as `authenticated` on the test user's own row succeeded cleanly, confirming the security review's open "verify GRANTs exist on the live project" question (§6 of that review) — they do, nothing missing. |
| DAT-BR-008 | `tracker/**` source | `checkTrackerMoneyPrecision` runs (Phase 1 task, not yet built) | zero `Double`/`Float` types on any money field | NFR-3 | S | Y | Backend | ✅ (closed 2026-08-15) `./gradlew checkTrackerMoneyPrecision` — real `CheckTrackerMoneyPrecisionTask : DefaultTask()` (root `build.gradle.kts`, wired into `regressionCheck`), re-run fresh (`--rerun-tasks`) and confirmed `BUILD SUCCESSFUL` against the current `tracker/**` tree as part of this closure pass |
| DAT-FLOW-001 | a user with rows across every tracker table | `delete_my_account()` is invoked | every owned row across every table AND the `auth.users` row are gone | ADR-0014 §7 | L | N (against dev project) | Backend | ✅ (closed 2026-08-15, against the live `dhruv` project) Only `holdings`/`valuations` exist as tracker tables this phase (Phase 2+ adds the rest — this row re-opens then, same as DAT-BR-006's per-migration note). Seeded a real holding+valuation for a real `auth.users` row, called `delete_my_data()` first (`holdings_left`/`valuations_left` both `0`), then `delete_my_account()` on the same now-empty user (`auth.users` row count `0` afterward) — the full chain confirmed end to end, not just each function's SQL body read. |

**Note added 2026-08-09 (finding #18):** `DAT-BR-001`'s "consent flag" was previously ambiguous —
which of A3's 4 switches gates which of §5.5's 7 tracker feature flags was unstated anywhere. Fixed
by the explicit mapping table added to the implementation plan (§5.5): all 7 tracker flags
(`money`/`budgets`/`goals`/`debtpayoff`/`insurance`/`retirement`/`insights`, plus `networth`) key
off "Sync my financial records"; SMS-sourced automation additionally requires "Read transaction
SMS"; the assistant flow requires "Ask Dhruv about my money". `ConsentInterceptor` implements that
table, not an ad hoc guess.

---

## 12. HOM — Home tab (01, shell-owned)

Added 2026-08-09 — the original catalog had zero rows for Home despite it being the single most
user-facing screen in the build (functional spec §5 Group B, "01"). Shell-owned (`:apps:finance:app`,
not a feature module — see the module-standard doc's `HOM`/`PLN` correction).

| ID | Given | When | Then | Source | Size | Auto | Owner | Status |
|---|---|---|---|---|---|---|---|---|
| HOM-UI-001 | net worth data available | Home opened | greeting matches time-of-day, date line renders, net-worth hero shows value + ▲/▼% delta + area sparkline | 01 | M | Y | Android | ☐ |
| HOM-UI-002 | 4 quick actions defined (Loan EMI/SIP/Currency/GST) | any quick action tapped | navigates to the correct Plan/Calc destination via `NavTarget` | 01 | S | Y | Android | ☐ |
| HOM-UI-003 | Phase 2: only loan/EMI obligations exist; Phase 3+: card bills also exist | Home opened | UPCOMING shows loan/EMI rows from Phase 2 onward, and additionally shows credit-card-bill rows from Phase 3 onward (see impl plan Phase 2/3 scoped-dependency notes) | 01 | M | Y | Android | ☐ |
| HOM-FLOW-001 | Home open, session signed out or offline with nothing cached | — | `SignedOutCard`/`OfflineStateCard` renders per NFR-4 instead of the hero card | NFR-4 | M | Y | Android | ☐ |
| HOM-UI-004 | any tab | Ask pill visibility | Ask pill renders on Home/Plan/Insights per the design, not on Calc/Money | ADR-0024 decision 4 | S | Y | Android | ☐ |

---

## 13. SET — Settings control plane (shell-owned, Phase 0b)

Added 2026-08-19 for `apps/finance/specs/004-settings/`. Shell-owned (`:apps:finance:app`), like
`HOM` and `NAV` — Settings has no feature flag and no `FeatureHost` of its own, though each
**contributed** module entry does (SET-ARCH-007).

Larger than a typical module section because Settings is a control plane, not a screen: it carries a
contract other modules implement (`004/contracts/settings-contribution.md`), a shell-level security
checkpoint (`004/contracts/app-lock-gate.md`), and the migration of 19 shipped rows. Source column
cites `004 FR-*`/`SC-*` from that feature's `spec.md`, and the two contracts by name.

**Rows reused, not restated** (constitution principle II — cite the id, do not duplicate the row):

| Concern | Reused row | Why it already covers it |
|---|---|---|
| Consent persists across restart | `ONB-BR-004` (§2) | same `ConsentRepository`, same DataStore round-trip |
| Consent withdrawn in Settings persists; others unaffected | `ONB-BR-005` (§2) | already written against Settings' switches |
| Dependent surfaces degrade on withdrawal | `ONB-BR-006` (§2) | deferred there for the same reason (Phase 2 screens) |
| No network call before consent | `DAT-BR-001` (§11) | interceptor-level, unchanged by this feature |
| Erasure removes rows / account | `ONB-BR-008`, `ONB-BR-009`, `DAT-FLOW-001` | Settings only relocates the action; the mechanism is closed |
| Withdraw-then-erase sequence | `ONB-FLOW-005` (§2) | exactly this feature's Account flow |
| One back arrow to a single parent | `NAV-ARCH-002` (§1) | the tier back-steps are an instance of N2 |
| Settings icon present on every tab | `NAV-UI-002` (§1) | already asserts the top-bar trio |
| No `feature → feature` import | `NAV-ARCH-004` (§1) | re-verified per new module, contributions included |

### 13.1 Contribution mechanism (architecture)

| ID | Given | When | Then | Source | Size | Auto | Owner | Status |
|---|---|---|---|---|---|---|---|---|
| SET-ARCH-001 | several modules each register a `SettingsContribution` | the registry resolves | every registered contribution is returned by type — the registry holds no module names of its own | 004 FR-004, contract §4 | M | Y | Backend | ☐ |
| SET-ARCH-002 | a throwaway module with a contribution is added to the build | `git status` after a successful assemble | no file under `app/ui/settings/` appears in the diff — only the new module's own files | 004 SC-004 | M | N (manual diff) | QA | ☐ |
| SET-ARCH-003 | `com.dhruv.finance.app.ui.settings` sources | `DependencyRulesTest` runs | no class there references a feature-module type | 004 FR-004, contract §5 | S | Y | SA | ☐ |
| SET-ARCH-004 | every `SettingsContribution` implementation | `DependencyRulesTest` runs | none references a Compose type — contributions stayed declarative data | contract §2, constitution V | S | Y | SA | ☐ |
| SET-ARCH-005 | every registered contribution | its `moduleKey` is checked against `platform/feature-flags/dhruv-finance.json` | the key exists — an unknown key would make the entry silently never appear | contract §1 rule 1 | S | Y | Backend | ☐ |
| SET-ARCH-006 | today's `SettingsKeys` key set | the shipped build's key set is enumerated | today's set is a **subset** — no migrated row got a new key | 004 FR-011, SC-001, constitution IX | S | Y | Backend | ☐ |
| SET-ARCH-007 | one contribution throws while producing its rows | the modules tier renders | that entry shows its `FeatureErrorCard`; every other entry and the tier itself still render | contract §4 rule 12, constitution IV | M | Y | Android | ☐ |

### 13.2 Registry and row behaviour

| ID | Given | When | Then | Source | Size | Auto | Owner | Status |
|---|---|---|---|---|---|---|---|---|
| SET-BR-001 | a module whose flag is `enabled: false` | modules tier resolves | its entry is absent — not greyed out, not present-and-inert | 004 FR-006 | S | Y | Backend | ☐ |
| SET-BR-002 | a module enabled but with `minVersion` above the running version | modules tier resolves | its entry is absent, matching how the resolver already gates routes | 004 FR-006 | S | Y | Backend | ☐ |
| SET-BR-003 | a module previously disabled | its flag is enabled | its entry appears in the tier with no other change to Settings | 004 FR-007 | S | Y | Backend | ☐ |
| SET-BR-004 | contributions with differing `order` and titles, registered in arbitrary order | the tier renders twice | ordering is `order` then title, identical both times | contract §4 | S | Y | Backend | ☐ |
| SET-BR-005 | an optional module with non-default settings | the module is turned off, then on again | its stored preferences are retained and restored — no reset on disable | 004 FR-032 | M | Y | Backend | ☐ |
| SET-BR-006 | the notification channel registry (surface registries §2) and the contributed alert toggles | both are enumerated | counts are equal and the mapping is one-to-one; no channel without a control, no control without a channel | 004 FR-030, SC-006 | M | Y | Backend | ☐ |
| SET-BR-007 | any row is changed | the change is made | it persists immediately with no save action; a write that fails reverts the displayed value and states why | 004 FR-042, contract §2 rule 9 | M | Y | Android | ☐ |
| SET-BR-008 | theme, accent or app lock changed from the quick row | the owning section is opened (and vice versa) | both show the same value from the same stored preference — one setting, two surfaces | 004 FR-002, SC-003 | S | Y | Android | ☐ |
| SET-BR-009 | every persisted preference key in the app | each is located in Settings | every key has exactly one row; a key with no row is either an FR-003 violation or dead state to delete | 004 FR-003, SC-005 | L | N (manual enumeration) | QA | ☐ |
| SET-BR-010 | the app-wide notification master is off | any module would post an alert | none is posted, regardless of that module's own alert setting | 004 FR-026 | M | Y | Backend | ☐ |
| SET-BR-011 | assistant consent granted | app force-stopped and relaunched | the grant still holds and the assistant does not ask again — replaces today's in-memory flag | 004 FR-036 | S | Y | Backend | ☐ |
| SET-BR-012 | a personal AI key saved | screens, logs, the export file and a crash report are searched | the key appears in full in none of them; the row shows it masked and removes it in one action | 004 FR-038, SC-014 | M | Y | Backend | ☐ |

### 13.3 App lock (see `004/contracts/app-lock-gate.md`)

| ID | Given | When | Then | Source | Size | Auto | Owner | Status |
|---|---|---|---|---|---|---|---|---|
| SET-BR-013 | app lock enabled | app cold-started | state is LOCKED — there is no "recently used" credit across process death | gate §1 rule 1 | S | Y | Backend | ☐ |
| SET-BR-014 | each timeout option in turn | app backgrounded for less than, and more than, that duration | LOCKED exactly when elapsed ≥ timeout; `Immediate` locks on any backgrounding however brief | gate §1 rules 2–3 | M | Y | Backend | ☐ |
| SET-BR-015 | a successful authentication | the app stays foregrounded | it does not re-prompt; the unlock covers the current foreground session only | gate §1 rule 3 | S | Y | Backend | ☐ |
| SET-BR-016 | app lock turned off while a locked state exists | — | state is UNLOCKED immediately; no stale locked state survives | gate §1 rule 4 | S | Y | Backend | ☐ |
| SET-BR-017 | a device with no enrolled credential | the user toggles app lock on | it is refused with what to enrol; the switch does not turn on | 004 FR-022, gate §2 rule 10 | S | Y | Android | ☐ |
| SET-UI-001 | app lock on, gate showing | each tab is attempted, **including Calc** | no content is composed or drawn anywhere — absent, not dimmed or blurred; a screenshot shows the lock surface only | 004 FR-021, gate §2 rules 6–7 | M | Y | Android | ☐ |
| SET-UI-002 | app lock on | app cold-started and the launch recorded | no frame of unlocked content appears before the gate resolves | gate §2 rule 7 | M | N (screen recording) | QA | ☐ |
| SET-UI-003 | the unlock prompt showing | the user cancels or fails | content stays hidden with a retry affordance; no attempt limit or lockout of our own | gate §2 rule 9 | S | Y | Android | ☐ |
| SET-FLOW-001 | app locked | a notification or deep link is tapped | unlock happens first, then the destination is reached — never skipped, never delivered before unlock | 004 FR-023, gate §3 | M | Y | Android | ☐ |
| SET-FLOW-002 | a held target and a cancelled unlock | the user unlocks successfully later in the same launch | the held target is dispatched exactly once; a process death before that clears it | gate §3 rules 12–13 | M | Y | Backend | ☐ |
| SET-BR-018 | a target already held while locked | a second target arrives | the second replaces the first — the user's most recent intent wins, and only one is held | gate §3 rule 14 | S | Y | Backend | ☐ |
| SET-BR-019 | hide-amounts on | money is rendered on a screen, the widget and a notification | all three mask the value while counts, dates and percentages stay readable; masking is independent of lock state | 004 FR-025, gate §4 rule 16 | M | Y | Backend | ☐ |
| SET-BR-020 | the legacy calculator history lock is on | app lock is enabled | the history lock is unchanged and still gates history inside an unlocked app; it is labelled legacy | 004 FR-028, gate §4 rule 17 | S | Y | Android | ☐ |

### 13.4 Account

| ID | Given | When | Then | Source | Size | Auto | Owner | Status |
|---|---|---|---|---|---|---|---|---|
| SET-FLOW-003 | no session | Settings › Account opened and sign-in used | sign-in completes without passing through first-run onboarding; no placeholder identity is shown at any point | 004 FR-012, SC-008 | M | Y | Android | ☐ |
| SET-FLOW-004 | an active session | sign out | session and stored credentials are cleared; on-device calculator history survives | 004 FR-013 | M | Y | Backend | ☐ |
| SET-BR-021 | account erasure requested | the confirmation is shown | it names exactly what is destroyed and that it cannot be undone, and requires a typed confirmation rather than a single tap | 004 FR-015 | S | Y | Android | ☐ |
| SET-BR-022 | erasure is attempted offline or the server rejects it | — | the failure is reported as a failure, nothing claims success, and the action stays available for retry | 004 FR-016 | M | Y | Backend | ☐ |
| SET-BR-023 | the export cannot yet produce a file (no financial records exist) | Account is opened | no export row is present at all | 004 FR-018 | S | Y | Android | ☐ (phase-gated — closes in the phase that ships the records; see 004 research R7) |

### 13.5 Structure, migration and presentation

| ID | Given | When | Then | Source | Size | Auto | Owner | Status |
|---|---|---|---|---|---|---|---|---|
| SET-UI-004 | Settings opened | the top level renders | order is quick rows → Account → App → modules tier, with no inline controls other than the three quick rows | 004 FR-001 | S | Y | Android | ☐ |
| SET-UI-005 | a module with submodules | its entry is opened | submodule settings are grouped and labelled under it, never promoted to the top level | 004 FR-029, contract §1 rule 4 | S | Y | Android | ☐ |
| SET-UI-006 | any settings screen | back is pressed | one step to the Settings top level, one more to the originating tab (instance of `NAV-ARCH-002`) | 004 FR-009 | S | Y | Android | ☐ |
| SET-UI-007 | a build of the previous version with all 19 rows set to distinctive values | this build is installed **over** it without uninstalling | all 19 rows are reachable at their new homes with values intact | 004 SC-001, data-model §2 | L | N (manual over-install) | QA | ☐ |
| SET-UI-008 | a module entry whose controls need a consent the user has not granted | the entry is opened | it states which consent is needed and offers the route to grant it, rather than showing inert controls | 004 FR-035 | M | Y | Android | ☐ |
| SET-UI-009 | every shipped row | each is exercised | zero rows both appear operable and change nothing; a preference-only row says it is preference-only | 004 FR-043, SC-011 | M | N (review pass) | QA | ☐ |
| SET-UI-010 | a primary navigation destination | the user looks for a way to hide it | none is offered; optionality applies to content and tools only | 004 FR-033 | S | Y | Android | ☐ |
| SET-UI-011 | every item on a surface hidden by a module setting | that surface is opened | an empty state points back to the setting that hid it — never a blank screen | 004 FR-034 | M | Y | Android | ☐ |
| SET-UI-012 | notification permission denied at system level | the App tier's notification area is opened | a banner above the controls explains no alert can be delivered and offers the route to system settings | 004 FR-027 | M | Y | Android | ☐ |
| SET-UI-013 | App details opened | the version is read | it matches the installed build exactly, including build number | 004 FR-039 | S | Y | Android | ☐ |
| SET-UI-014 | an update check runs and fails | — | the failure is reported; the app never silently reports "current" on a failed check | 004 FR-040 | M | Y | Android | ☐ (phase-gated — update channel not built) |
| SET-UI-015 | every settings screen | rendered light and dark, at smallest and largest system text size | no clipped or truncated row label; only token values change (NFR-005/006 apply) | 004 SC-012 | M | N (visual) | QA | ☐ |
| SET-UI-016 | every switch, icon-only control and destructive action in Settings | TalkBack traverses the tier | each announces its subject and current state; destructive rows are visually distinct and never first-focused | 004 SC-013, FR-045 | M | N (manual) | QA | ☐ |

---

## 14. Coverage summary (updated as rows close)

**Recount 2026-08-15b** (Phase 1 fully closed — the manual dev-project pass ran against the live
`dhruv` Supabase project after the 2026-08-15 code-review-only recount; `DAT-BR-007`, `DAT-FLOW-001`,
and `ONB-BR-009`'s SQL half all closed for real, not just code-reviewed):

**SET added 2026-08-19** (Phase 0b, `apps/finance/specs/004-settings/`) — 50 rows, all **☐**. This is a definitional addition, not a recount: no existing row changed state. The counts below are the 2026-08-15b figures plus SET.

| Module | Rows | ☐ | 🔴 | 🟢 | ✅ |
|---|---|---|---|---|---|
| NAV | 11 | 7 | 0 | 4 | 0 |
| ONB | 14 | 3 | 0 | 10 | 1 |
| NW | 14 | 14 | 0 | 0 | 0 |
| MNY | 20 | 20 | 0 | 0 | 0 |
| PLN | 14 | 14 | 0 | 0 | 0 |
| INS | 4 | 4 | 0 | 0 | 0 |
| RET | 4 | 4 | 0 | 0 | 0 |
| SIG | 8 | 8 | 0 | 0 | 0 |
| AUT | 9 | 9 | 0 | 0 | 0 |
| SRC | 5 | 5 | 0 | 0 | 0 |
| DAT | 9 | 0 | 0 | 5 | 4 |
| HOM | 5 | 5 | 0 | 0 | 0 |
| SET | 50 | 50 | 0 | 0 | 0 |
| **Total** | **167** | **143** | **0** | **19** | **5** |

Phase 1's own module (`ONB`, `DAT`) has zero rows blocked on infrastructure now — every remaining
`ONB` ☐ row is deliberately deferred with a stated reason, not silently missing: `ONB-BR-006`/
`ONB-FLOW-004` (Phase 2 — no tracker-content screens or accounts/holdings repository exist yet to
exercise them), `ONB-BR-007` (CSV import, open item). `DAT` is fully closed (0 ☐ rows) — every
Automatable:N row that needed the dev project was run against it directly (SQL verification method:
Management API SQL access under a personal access token, `SET ROLE authenticated` + a real
`auth.users` row's id set as `request.jwt.claim.sub`, real seeded data, real erasure calls, results
independently re-queried afterward — not just the function bodies read).

QA updates this table at every phase checkpoint (standard doc §4 step 7) — by recounting rows
directly, not by incrementing the previous total by hand. A phase may not close as "done" while any
of its rows are still ☐ or 🔴, except rows explicitly marked deferred with a reason (e.g. NW-BR-007
blocked on the XIRR ADR, ONB-BR-007 deferred to the CSV-import spec).
