# Dhruv Finance — App-Wide UI/UX Design Standard

> Status: **BINDING** for all new work app-wide (tracker phases P1–P6, R-phases, and any screen a
> future PR touches). Extends `2026-07-03-tracker-design-system.md` — that document remains the
> authority for the tracker component contracts it defines; this document widens the same rules to
> **every corner of the application**: navigation, settings, legacy tool screens, notifications,
> widget, PDF, lock screen. On any perceived conflict, the tracker design system wins for tracker
> components; raise an edit rather than diverging.
> Companion: `2026-07-12-spec-consistency-review.md` (findings F6–F15 are resolved by the sections
> below). Legacy screens adopt lazily (ADR-0014 §8): when a PR touches a screen, that screen
> conforms before the PR merges — never a big-bang retrofit.

---

## 1. Token architecture (three layers, Compose dialect)

```
Primitive  →  raw palette, type scale, spacing scale   (DhruvTheme internals; :libs:core only)
Semantic   →  MaterialTheme.colorScheme.* roles + SectionTheme accent + typography roles
Component  →  contracts in com.dhruv.core.ui.components.* (BentoCard, MoneyText, …)
```

Rules (extend the micro-frontend rule to the whole app):
- Feature/app modules consume **semantic and component layers only**. No raw hex, no raw `sp`,
  no ad-hoc `dp` outside the spacing scale, anywhere — including `:apps:finance:app` (Settings,
  Dashboard, MainActivity chrome) and legacy tool screens as they are touched.
- Primitive values live only inside `DhruvTheme`. A component may not branch on `isDark` (theme
  roles already resolve it).
- Spacing scale: 4dp base grid; sanctioned steps 4/8/12/16/24/32. Screen edge + card padding 16dp,
  bento gutters 12dp, list row min height 56dp (inherited, now app-wide).
- Detekt guard (aspirational, tracked): forbid `Color(0x` literals outside `:libs:core` theme
  package.

## 2. Color & section accents (resolves F15)

| Domain | Accent |
|---|---|
| Home + ALL tracker routes (net worth, expenses, budgets, recurring, goals, payoff, insurance, retirement, reports, search, trash) | **green** SectionTheme (one domain, one accent — P1 rule generalized) |
| Tools hub + calculator | user's `calculatorColor` |
| Converter (currency/unit) | `converterColor` |
| Date / Time tools | `dateColor` / `timeColor` |
| Finance calculators (loans/investments/tax/everyday) | `financeColor` |
| Settings, onboarding, lock screen | neutral (base DhruvTheme, no section accent) |

Positive/negative money semantics: tertiary-container vs error-container roles + ▲/▼ glyphs —
never color alone (inherited, app-wide).

## 3. Navigation & information architecture (resolves F7, F8)

### 3.1 Tab model (post-P1 target)

Three pager pages + system back contract (P1 G16, restated as the app-wide law):

| Page | Tab | Content |
|---|---|---|
| 0 | **Home** | `NetWorthScreen` state machine → `DashboardContent` bento + tracker sub-screens |
| 1 | **Tools** | Launcher grid (all calculators/converters/utilities/assistant) |
| 2 | **Settings** | Settings tree (§4) |

Back: sub-screen open in current tab → pop to tab root; else pager → page 0; page 0 → exit.
Nested `BackHandler` enabled only when its tab is current AND a sub-screen is open. Predictive
back (R8/N19): every BackHandler verified under gesture preview.

### 3.2 Home bento card registry (fixed order; resolves F7)

| # | Card | Phase | Span |
|---|------|-------|------|
| 1 | Net worth hero (delta + sparkline) | P1 | full — always first, not hideable |
| 2 | Assets | P1 | half |
| 3 | Liabilities | P1 | half |
| 4 | This month (spent vs budget + savings chip + "N to review" badge + daily-pace line + "N due this week" → Upcoming) | P2/R5b/R6-PG3/PG4 | half |
| 5 | Top goal ring + debt-free date | P3 | half |
| 6 | Insurance cover + next renewal | P4 | half |
| 7 | Retirement on-track/gap chip | P5 | half |
| 8 | This year (YTD savings) → Reports | R7 | half |

Governance: hero + **max 6** half-cards visible; every non-hero card individually hideable
(Settings › Features, G15 toggle precedent); hidden or flag-disabled cards simply don't render
(no ghost slots); a card whose feature has no data renders its empty-CTA variant, not blank.
New phases must claim a registry slot here before adding a card.

### 3.3 Route registry (FeatureHost key · secure flag · accent)

| Route | FeatureHost key | Secure (R3) | Accent | Entry point |
|---|---|---|---|---|
| Home dashboard | `networth` | ✔ | green | tab 0 |
| Holding list / detail | `networth` | ✔ | green | Home cards |
| Transactions / QuickAdd / Budgets | `expenses` | ✔ | green | Home card 4 |
| Recurring rules / Review inbox | `recurring` | ✔ | green | Budgets top bar · card badge · notification |
| Goals / Payoff | `goals` | ✔ | green | Home card 5 |
| Insurance list / detail | `insurance` | ✔ | green | Home card 6 · notification |
| Retirement list / projection | `retirement` | ✔ | green | Home card 7 |
| Reports | `reports` | ✔ | green | Home card 8 · Settings › Data |
| Search | `search` | ✔ | green | Home top bar |
| Recently deleted | — (Settings surface) | ✔ | green | Settings › Data |
| Tools grid | — (shell) | ✘ | per-tool | tab 1 |
| Calculator, Converter, Date, Time, Finance calcs, Assistant | own keys | ✘ | per-section | Tools grid |
| Settings tree | — (shell) | ✘ | neutral | tab 2 |
| Onboarding, Lock overlay | — (shell) | lock sets FLAG_SECURE while shown | neutral | first-run / R3 |

Every tracker route = FeatureHost-wrapped + `secure = true`. Tools routes keep FeatureHost,
never secure. This table is the checklist column for `/dhruv-ui-review` on any nav change.

**Code twin (ADR-0024 / NAV1-2):** this registry maps 1:1 to the sealed `NavTarget` type in
`:libs:core` — the only cross-feature navigation mechanism (features never reference another
feature's screens; the app-level `NavigationDispatcher` resolves targets to tab + nested-NavHost
route). Adding a route = adding the registry row AND the sealed subtype. Intent extras (§7.2) are
untrusted input: an unknown/foreign id in a target (e.g. `OPEN_POLICY(id)`) resolves to the
normal not-found state, never a crash.

### 3.4 Tools hub grid

Sections in order: **Calculators** (Calculator, Loans, Investments, Tax, Everyday) ·
**Converters** (Currency, Unit) · **Date & time** (Date, Time — appear when flags enable) ·
**AI** (Assistant). Visibility = feature flag AND user toggle (G15). Grid = 3-column
`BentoCard` tiles (icon, label), per-section accent on tile icon.

## 4. Settings information architecture (resolves F9)

Target tree — every existing and specced setting has exactly one home. Phases slot **rows** into
this tree; adding a new top-level section requires an edit to this document first.

```
Settings
├─ Account                      (P1; mirrors Home overflow — same actions, one implementation)
│   signed-in identity row · Sign out · Withdraw consent
│   Delete my data · Delete my account          (ConfirmDangerDialog, type-to-confirm)
├─ Appearance
│   Theme (System/Light/Dark) · Section accent colors (5 pickers)
├─ Security                     (R3)
│   App lock (switch + enrollment explainer) · Auto-lock timeout (segmented)
│   Hide amounts (privacy mode) · [legacy] History lock — caption "superseded by App lock"
├─ Notifications & alerts       (permission-state banner on top when denied → system settings)
│   Daily rates (toggle + time picker)                     (currency plan)
│   Budget alerts (toggle + warn threshold OFF/50/80/90)   (R6)
│   EMI reminders (toggle)                                 (R6)
│   Renewal reminders (toggle + offsets)                   (P4)
│   Recurring review (toggle)                              (R5b)
│   Value-update reminders (toggle + 60/90-day threshold)  (R6/PG5)
│   Monthly summary (toggle)                               (R7/PG6)
│   App updates (toggle)                                   (R4)
├─ Features
│   Home cards (per-card hide, §3.2) · Tools sections & tools (G15 toggles)
│   Gold/silver cards (currency plan)
├─ Calculators                  (merges today's "General" + "Calculator" sections)
│   Number format (Indian/International) · Decimal precision · Angle mode (DEG/RAD)
│   Calculator history (preview · export · clear)
├─ Data                         (R7/R8)
│   Export my data (ZIP) · Export net-worth statement (PDF) · Import data
│   Recently deleted (trash)
├─ AI Assistant
│   Consent status + re-consent · BYO Gemini key (masked, encrypted)
└─ About
    Version (name + code) · Updates (R4: token, status, Check now, Download)
    Show intro again (R8) · GitHub · Privacy policy · Licenses
```

Migration: the current monolithic `SettingsScreen` sections map 1:1 into this tree; each R/P phase
that adds rows also moves its neighboring legacy rows into place (amortized, M4-friendly). The
tree ships incrementally — order of sections is fixed from day one so items never jump around.

## 5. Component inventory — consolidated (resolves F6, F13)

Everything in `com.dhruv.core.ui.components.*`, themed via roles only.

| Component | Contract | Origin |
|---|---|---|
| `BentoGrid` / `BentoCard` / `HeroStatCard` / `StatDeltaChip` | as tracker design system | P1 |
| `TrendLineChart` (sparkline + axis modes) | as tracker design system | P1/P5 |
| `EmptyStateCard` / `OfflineBanner` / `RetryErrorCard` | as tracker design system | P1 |
| `ConsentGateScaffold` / `DhruvModalSheet` / `ConfirmDangerDialog` | as tracker design system | P1 |
| `BarChart` / `DonutChart` | Canvas conventions of TrendLineChart | P2 |
| **`MoneyText`** | THE money renderer for all new Compose work (F13): takes pre-formatted string, tabular numerals, reads `LocalHideAmounts`, stable-width mask `₹••••••`; variants hero/row/inline | R3 |
| **`MaskedMoney`** | pure `mask(formatted): String` for non-Compose surfaces (notifications, Glance, anywhere without composition locals) | R3/F3 |
| **`ProgressRing`** | stateless Canvas ring, 4dp stroke, primary on surfaceVariant track, center slot for %; min 48dp; required contentDescription ("funded 62 percent") | P3 needs it (F6) |
| **`CountBadge`** | small tonal count chip for cards/rows ("3 to review"); max "99+" | R5b |
| **`SearchField`** | top-aligned search input, debounce owned by caller, clear button, min 48dp | R8 |
| **`UndoSnackbarHost`** | standard 5s snackbar with Undo action; single host per screen scaffold; queues, never stacks | R8 trash |
| **`DisclaimerFooter`** | persistent bodySmall onSurfaceVariant footer ("illustration, not financial advice") | P5/R9 |
| `SegmentedRow` (convention, not component) | Material3 `SingleChoiceSegmentedButtonRow` used as-is for all segmented controls (P2 type toggle, P3 strategy, R3 timeout) — no custom segmented controls | — |
| `LockScreen`, onboarding pager | live in `:apps:finance:app` (app shell, not reusable) but compose only core primitives + roles | R3/R8 |

Money display rules (app-wide): compact (`formatPaiseCompact`) on cards/widget; full
(`formatPaise`) in lists, sheets, history, PDF; tabular numerals always; never ellipsize a ₹
value — wrap or compact-format. Percentages stay visible under privacy mode (R3 carve-out).

## 6. Interaction standards

- **Refresh (F14):** explicit refresh (overflow action / retry buttons) is the app-wide pattern.
  No per-feature pull-to-refresh until a dedicated revisit lands it everywhere at once.
- **Sheets:** all create/edit flows are `DhruvModalSheet`s — never full new screens for forms.
  Primary action filled full-width, disabled in-flight; destructive as error-text button.
- **Dialogs:** only for irreversible confirms (`ConfirmDangerDialog`); account-level deletions
  require type-to-confirm.
- **Deletes:** every tracker delete is soft + `UndoSnackbarHost` (5s) + lands in Recently deleted
  (R8). Hard deletes exist only in trash ("Delete forever") and Account section.
- **FAB:** one per screen max, always "add the primary noun" (holding, transaction, goal, policy,
  scenario, rule). Screens with sheets-only interactions have no FAB.
- **Top bars:** Home = wordmark + search icon (R8) + privacy eye (R3) + overflow (P1 actions).
  Sub-screens = back + title (+ contextual total subtitle). Sheets have drag handles, no top bars.
  Never more than 3 icons + overflow in any top bar.
- **Haptics:** confirm on destructive actions only (inherited).
- **Motion:** Material default springs; pager settle; sheet slide. No custom choreography.

## 7. Non-Compose surfaces (resolves F10)

### 7.1 Notification channel registry

| Channel id | Name | Importance | Money masking | Source |
|---|---|---|---|---|
| `daily_rates` | Daily rates | LOW | n/a (public FX data) | currency plan |
| `app_updates` | App updates | LOW | n/a | R4 |
| `recurring_review` | Transactions to review | DEFAULT | count only, no amounts | R5b |
| `budget_alerts` | Budget alerts | LOW (80%) / DEFAULT (100%) | `MaskedMoney` under privacy mode; % always | R6 |
| `emi_reminders` | EMI reminders | DEFAULT | name + date only, no amounts | R6 |
| `renewal_reminders` | Renewal reminders | DEFAULT | policy name + date only | P4 |
| `stale_valuations` | Value updates due | LOW | asset names + age only, no amounts | R6/PG5 |
| `monthly_digest` | Monthly summary | LOW | `MaskedMoney` under privacy mode; % stay | R7/PG6 |

Quick actions (PG10): EMI/renewal reminders carry **"Mark paid"**, recurring-review carries
**"Confirm all"** — both route through the ReviewInbox confirm path and honor the R3 lock
hold-and-dispatch. Notifications with actions never exceed two action buttons.

Copy pattern: sentence case, ≤ 1 line expanded ≤ 2 (BigTextStyle only for daily rates), never a
policy number, never an account name + amount in the same line under privacy mode. Every channel
has a Settings row (§4) — the channel registry and the Settings tree must stay 1:1.

### 7.2 Intent action registry (single-activity contract)

| Extra value | Destination | Producer |
|---|---|---|
| `QUICK_ADD` | QuickAddSheet over expenses | launcher shortcut (R5b), widget button (R8) |
| `REVIEW_INBOX` | ReviewInboxScreen | recurring notification (R5b) |
| `OPEN_POLICY(id)` | PolicyDetailScreen | renewal notification (P4 — aligns to this mechanism at build) |
| `OPEN_BUDGETS` | BudgetScreen | budget alert (R6) |
| `OPEN_UPCOMING` | UpcomingScreen | Home card 4 line, future notifications (R6/PG4) |
| `OPEN_REPORTS(month)` | ReportsScreen at month | monthly digest (R7/PG6) |

All extras pass through the R3 lock hold-and-dispatch (F4). New deep-link targets register here.

### 7.3 Widget (Glance)

Uses Glance theme mapped from DhruvTheme day/night roles; compact money only; `MaskedMoney` under
privacy mode; states per R8 (value / masked / sign-in / enable). ContentDescription on the root
("Net worth, ₹48 lakh, up 2 percent this month").

### 7.4 PDF statement

Same type hierarchy mapped to PdfDocument text sizes (headline/title/body from the primitive
scale); full `formatPaise`; **never masked** (explicit user act — dialog states it); footer:
generated date + app version; no logos beyond wordmark text.

## 8. Screen-state matrix (app-wide law)

Every screen defines: default · loading (first-load spinner only) · empty (`EmptyStateCard` + CTA)
· error (`RetryErrorCard`) · offline (`OfflineBanner` + retry) · disabled (`FeatureDisabledCard`)
· not-configured (tracker only). Additional surface states: widget (4, §7.3) · lock (locked /
error) · onboarding (3 pages + skip). `/dhruv-ui-review` gates on the matrix per screen.

## 9. Accessibility (inherited + additions)

Tracker design-system checklist applies app-wide (48dp targets, AA contrast, no color-only
meaning, dynamic-type safe). Additions: lock screen fully TalkBack-navigable with the prompt
auto-fire not stealing focus; privacy-eye toggle announces state ("amounts hidden"); charts,
rings, chips, widget carry contentDescription; trash rows announce days-left; settings rows are
minimum 56dp.

## 10. Copy conventions (inherited + additions)

Sentence case; ₹ with space full / compact per §5; relative dates; destructive dialogs name the
consequence. Additions: notification copy per §7.1; empty states always pair message + verb CTA
("Add your first asset"); errors say what to do, not what happened internally ("Couldn't reach
your data. Retry." — never exception text); all user-visible strings land in `strings.xml` from
birth (M3 debt rule — new screens never add hardcoded literals).

## 11. Legacy adoption order (amortized, per roadmap §4)

1. **Settings** — restructured into §4 tree starting R3 (it adds Security anyway); highest
   traffic, currently monolithic.
2. **Home/Dashboard chrome** — already replaced by P1.
3. **Currency screen** — touched by the currency/metals phase (R4) → adopts components then.
4. **Finance calculators (loans/investments/tax/everyday)** — adopt when P3 extracts EMI math
   (loans is touched anyway).
5. **Calculator + unit/date/time** — last; only when otherwise touched (M4 decomposition rides
   the same PRs).

No PR may leave a screen half-migrated: a touched screen conforms fully before merge.
