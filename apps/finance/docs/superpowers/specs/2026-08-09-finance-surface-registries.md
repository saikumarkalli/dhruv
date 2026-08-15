# Dhruv Finance — Surface Registries (routes · notifications · intents · settings)

> Status: **BINDING** for `:apps:finance`. These are the Finance-app-specific registries — the flat
> per-surface indexes that the global design system's conventions point at.
>
> **Design conventions live globally**, not here: `platform/DESIGN-SYSTEM.md` §6 (navigation law),
> §11 (notification/widget/PDF conventions), §7 (screen-state matrix). This document holds only the
> **rows** — which routes, which channels, which intents, which settings — because those are per-app
> by nature. A Tools or Vault app gets its own registry file; it does not edit this one.
>
> Extracted 2026-08-09 from the retired `2026-07-12-app-design-standard.md` (ADR-0030), which mixed
> these Finance rows with global conventions. Content carried forward as-is except where noted.
>
> **Screens themselves** (what each route renders, its business rules and flows) live in
> `2026-08-08-design-v1-final-functional-spec.md` — this is the index, that is the spec.

---

## 1. Route registry

Every route: `FeatureHost`-wrapped + a feature-flag key + one owner (a tab, or the shell). Build
order is authoritative in `../plans/2026-08-08-design-v1-final-implementation-plan.md` §7; this
table is the flat per-route index.

| Route | Owner tab | FeatureHost key | Presentation | Consent (DPDP) | Status |
|---|---|---|---|---|---|
| Home (01) | Home | — (shell, `:apps:finance:app`) | root | — | Phase 2 |
| Net worth / Assets / Liabilities (C1–C7) | Home | `networth` | push | `requiresConsent` | Phase 2 |
| Notifications (B2) | Home | — (shell) | push | — | Phase 6 |
| Search (B3) | Home | — (shell) | push | — | Phase 6 |
| Ledger / Quick add / Accounts / Categories / Recurring (D1–D9) | Money | `money` | root (D1) / sheet (D2) / push / modal | `requiresConsent` | Phase 3 |
| Keypad (Calc tab) | Calc | `calculator` | root | — | live today |
| Calc history | Calc | `calculator` | push (top-bar) | — | live today |
| Plan root (E1, live modules + calculator strip) | Plan | — (shell, `PlanLauncher`) | root | — | Phase 4 rewrite |
| Loan / Invest / Tax / Everyday | Plan | own keys (`loans`/`investments`/`tax`/`everyday`) | push (nested `NavHost`) | — | live today |
| Budgets / Goals / Debt payoff (E2–E6) | Plan | `budgets`/`goals`/`debtpayoff` | push | `requiresConsent` | Phase 4 |
| Insurance / Policy detail (E7–E8) | Plan | `insurance` | push | `requiresConsent` | Phase 4 |
| Retirement (E9) | Plan | `retirement` | push | `requiresConsent` | Phase 4 |
| Monthly summary / Cashflow / P&L / Balance sheet / Reports (F1–F5) | Insights | `insights` | root (F1) / push | `requiresConsent` | Phase 5 |
| Automation hub / Review queue / AA consent (G1–G3) | — (Settings) | `automation` | push / modal | `requiresConsent`, ships `enabled: false` until its checkpoint | Phase 7 |
| Currency, Unit, Date, Time | — (shell detail route) | own keys | push, back-top-bar, no tab bar | — | live today |
| Settings, Ask, Profile | — (shell detail route) | `assistant` (Ask only) | push, back-top-bar, no tab bar | `requiresConsent` (Ask only) | live today |
| Sign-in / DPDP consent / Empty start (A2–A4) | — (shell, pre-tab) | — | bare, full-frame, no chrome | consent screen itself | Phase 1 |

Every tracker/planning/insights/automation route is FeatureHost-wrapped **and**
`requiresConsent: true` in `platform/feature-flags/dhruv-finance.json`. Calc, the Plan calculators
and the converters stay FeatureHost-wrapped with **no** consent — they are offline and Room-local
(ADR-0014). This table is the checklist column for `/dhruv-ui-review` on any nav change.

**Code twin.** This registry maps 1:1 onto the sealed `NavTarget` type in `:libs:core` — the only
cross-feature navigation mechanism. **Adding a route = adding the registry row AND the sealed
subtype**, in the same change. Intent extras (§3) are untrusted input: an unknown or foreign id in
a target (e.g. `OpenPolicy(id)`) resolves to the normal not-found state, never a crash.

---

## 2. Notification channel registry

Copy and action conventions are global (`platform/DESIGN-SYSTEM.md` §11). These are the channels.

| Channel id | Name | Importance | Money masking | Source phase |
|---|---|---|---|---|
| `daily_rates` | Daily rates | LOW | n/a (public FX data) | currency plan |
| `app_updates` | App updates | LOW | n/a | R4 |
| `recurring_review` | Transactions to review | DEFAULT | count only, no amounts | R5b |
| `budget_alerts` | Budget alerts | LOW (80 %) / DEFAULT (100 %) | masked under privacy mode; % always shown | R6 |
| `emi_reminders` | EMI reminders | DEFAULT | name + date only, no amounts | R6 |
| `renewal_reminders` | Renewal reminders | DEFAULT | policy name + date only | P4 |
| `stale_valuations` | Value updates due | LOW | asset names + age only, no amounts | R6 |
| `monthly_digest` | Monthly summary | LOW | masked under privacy mode; % stay | R7 |

**Quick actions.** EMI and renewal reminders carry **"Mark paid"**; recurring-review carries
**"Confirm all"** — both route through the review-queue confirm path and honour the app-lock
hold-and-dispatch. Global cap of two action buttons applies.

Every channel has exactly one row in the settings tree (§4) — registry and tree stay 1:1.

---

## 3. Intent action registry (single-activity contract)

| Extra value | Destination | Producer |
|---|---|---|
| `QUICK_ADD` | Quick-add sheet over the ledger (D2) | launcher shortcut (R5b), widget button (R8) |
| `REVIEW_INBOX` | Review queue (G2) | recurring notification (R5b) |
| `OPEN_POLICY(id)` | Policy detail (E8) | renewal notification (P4) |
| `OPEN_BUDGETS` | Budgets (E2) | budget alert (R6) |
| `OPEN_UPCOMING` | Home UPCOMING section (01) | future obligation notifications (R6) |
| `OPEN_REPORTS(month)` | Reports at month (F5) | monthly digest (R7) |

All extras pass through the app-lock hold-and-dispatch. New deep-link targets register here **and**
as a `NavTarget` subtype.

---

## 4. Settings information architecture

Target tree — every existing and specced setting has exactly one home. Phases slot **rows** into
this tree; adding a new top-level section requires editing this document first, so items never
jump around between releases.

```
Settings
├─ Account                      (P1; mirrors Home overflow — same actions, one implementation)
│   signed-in identity row · Sign out · Withdraw consent
│   Delete my data · Delete my account          (ConfirmDangerDialog, type-to-confirm)
├─ Appearance
│   Theme (System/Light/Dark) · Accent colour (4-swatch global picker, ADR-0024 §2)
├─ Security                     (R3)
│   App lock (switch + enrollment explainer) · Auto-lock timeout (segmented)
│   Hide amounts (privacy mode) · [legacy] History lock — caption "superseded by App lock"
├─ Notifications & alerts       (permission-state banner on top when denied → system settings)
│   Daily rates (toggle + time picker)                     (currency plan)
│   Budget alerts (toggle + warn threshold OFF/50/80/90)   (R6)
│   EMI reminders (toggle)                                 (R6)
│   Renewal reminders (toggle + offsets)                   (P4)
│   Recurring review (toggle)                              (R5b)
│   Value-update reminders (toggle + 60/90-day threshold)  (R6)
│   Monthly summary (toggle)                               (R7)
│   App updates (toggle)                                   (R4)
├─ Features
│   Home cards (per-card hide) · Plan/shell tool visibility toggles
│   Gold/silver cards (currency plan)
├─ Calculators                  (merges today's "General" + "Calculator" sections)
│   Number format (Indian/International) · Decimal precision · Angle mode (DEG/RAD)
│   Calculator history (preview · export · clear)
├─ Data                         (R7/R8)
│   Export my data (ZIP) · Export net-worth statement (PDF) · Import data
│   Recently deleted (trash)
├─ Automation                   (Phase 7 — G1 hub lives here)
│   Bank SMS alerts · Account aggregator · Price feeds · Recurring templates · Learned rules
├─ AI Assistant
│   Consent status + re-consent · BYO Gemini key (masked, encrypted)
└─ About
    Version (name + code) · Updates (R4: status, Check now, Download)
    Show intro again (R8) · GitHub · Privacy policy · Licenses
```

The current monolithic `SettingsScreen` sections map 1:1 into this tree. Each phase that adds rows
also moves its neighbouring legacy rows into place — amortised, never a big-bang retrofit.

**Changes from the pre-2026-08-09 version of this tree:** "Section accent colors (5 pickers)" became
the single global accent picker (`SectionTheme` retired, ADR-0024 §2); "Tools sections & tools"
re-worded since there is no Tools tab (ADR-0027); an `Automation` section was added for Phase 7's
G1 hub, which previously had no home.
