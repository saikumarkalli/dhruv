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
| Automation hub / Review queue / AA consent (G1–G3) | — (Settings › Modules › Automation) | `automation` | push / modal | `requiresConsent`, ships `enabled: false` until its checkpoint | Phase 7 |
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

Every channel has exactly one control in Settings, in the **module that owns the channel**
(§4’s Modules tier) — registry and controls stay 1:1. The App tier holds only the app-wide
master switch and the system-permission state, never a per-channel row. Enforced by `SET-BR-006`
(QA catalog §13).

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

## 4. Settings information architecture — the application control plane

**Rewritten 2026-08-19** (`apps/finance/specs/004-settings/`, FR-005). The previous version of this
section was a fixed ten-section tree that each phase slotted rows into. That model is retired:
Settings is now a **control plane of three tiers**, where the shell owns Account and App, and every
module **declares its own entry** — so shipping a module ships its settings, and no phase edits a
central list. The requirements are `004/spec.md`; the mechanism is
`004/contracts/settings-contribution.md`; the scenarios are QA catalog §13 (`SET-*`).

**What still requires editing this document first**: adding, removing or renaming a **tier**, or
changing where a class of setting lives (which tier owns alerts, which owns erasure). Adding a
**module entry** does not — that is the point of the redesign, and doing it by hand would reintroduce
the central list FR-004 removes.

```
Settings
├─ Quick rows                                  [the only inline controls at the top level]
│   Theme (System/Light/Dark) · Accent colour (4-swatch global picker, ADR-0024 §2) · App lock
│   Each mirrors its owning section's row — a shortcut, never a second copy. Set is frozen at
│   these three (004 FR-002); growing it is a change to that spec, not a judgement call.
│
├─ Account                                     [shell-owned · always present]
│   Signed-in identity · Sign in (when signed out) · Sign out
│   Consent switches — all three, independently revocable, persisted
│   Export my financial records                (row absent until it can produce a file, 004 FR-018)
│   Delete my data · Delete my account         (ConfirmDangerDialog; account = type-to-confirm)
│
├─ App                                         [shell-owned · always present]
│   Appearance      Theme · Accent colour · Use wallpaper colours (labelled unavailable)
│   Security        App lock (switch + enrolment explainer) · Auto-lock timeout (segmented)
│                   Hide amounts (privacy mode) · [legacy] History lock — "superseded by App lock"
│   Notifications   System-permission state + banner when denied (→ system settings)
│                   App-wide master switch — off suppresses every module's alerts
│                   Per-alert controls live in the module that owns the channel, not here (§2)
│   App details     Version (name + code) · Updates (status · Check now · Download)
│                   Show intro again · Source · Privacy policy · Licences
│
└─ Modules                                     [assembled from contributions · never hardcoded]
    One entry per module that is present AND enabled for the running version; a module that is off,
    unshipped or version-gated is absent, not greyed out. Each entry holds that module's own rows,
    its submodules' rows grouped and labelled, its alert controls, and — for optional modules — its
    own on/off control. Ordering is the contribution's own, deterministically.

    ├─ Calculators        Number format (Indian/International) · Decimal precision · Angle mode
    │                     Calculator history (preview · export · clear)
    │                     submodules: the individual calculators and converters
    ├─ AI Assistant       Consent status + re-consent · BYO Gemini key (masked, encrypted)
    ├─ Currency & metals  Daily rates (toggle + time picker) · Gold/silver cards
    ├─ Net worth          (Phase 2)  its own rows · Value-update reminders (toggle + 60/90-day)
    ├─ Money              (Phase 3)  its own rows · Recurring review (toggle) · Home-card visibility
    ├─ Planning           (Phase 4)  Budget alerts (toggle + threshold OFF/50/80/90) · EMI reminders
    ├─ Insurance          (Phase 4)  Renewal reminders (toggle + offsets)
    ├─ Insights           (Phase 5)  Monthly summary (toggle) · net-worth statement export (PDF)
    └─ Automation         (Phase 7)  Bank SMS alerts · Account aggregator · Price feeds
                                     Recurring templates · Learned rules · Import · Recently deleted
```

**Where the retired sections went.** Three of the old tree's top-level sections dissolved rather than
moved, because each was a central list of things modules own:

| Old section | Now |
|---|---|
| Notifications & alerts | per-alert controls in the module owning each channel; only the master switch and permission state remain, in App |
| Features | hiding a card or tool is that module's own setting, in its entry |
| Data | export → Account; statement export → Insights; import and recently-deleted → the module owning the data |

Everything else kept its content and changed only its address: Appearance and Security became areas
inside **App**; Calculators and AI Assistant became **module entries**; About became **App details**;
Account and Automation kept their names.

**Build position.** The control plane ships in Phase 0b (before Phase 2) with the Account and App
tiers complete and the modules tier assembled from what ships today. Each later phase adds its
module's entry with the module — see the design-v1 implementation plan §7.

**Changes from the pre-2026-08-09 version of this tree** (retained for lineage): "Section accent
colors (5 pickers)" became the single global accent picker (`SectionTheme` retired, ADR-0024 §2);
"Tools sections & tools" re-worded since there is no Tools tab (ADR-0027); an `Automation` section
was added for Phase 7's G1 hub, which previously had no home.
