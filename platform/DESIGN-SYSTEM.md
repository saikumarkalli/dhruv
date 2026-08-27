# Dhruv Design System — v1.0 FINAL (global)

> Status: **BINDING for every Dhruv app and surface** — Finance, Tools, Vault, Health,
> Relationship, and the web SPA. This is the single design contract; there is no per-app design
> system. Governing decision: **ADR-0030** (`DECISIONS.md`), which supersedes ADR-0014 §8's tracker
> design system and the former app-design-standard.
>
> **Scope boundary — read this first.** This document defines *how anything in Dhruv looks and
> behaves*: tokens, typography, components, navigation law, states, motion, accessibility, copy.
> It defines **no screens**. An app's own screen inventory, business rules and flows live in that
> app's spec (Finance: `apps/finance/docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md`),
> and its notification/intent/settings registries in that app's surface registry
> (Finance: `apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md`).
>
> **Source of truth for values is the code, not this file.** Every token below has a named code
> twin in `:libs:core`. Where this document and the code disagree, the code is right and this
> document is a bug — fix it here rather than forking a second set of values.

---

## 1. Two colour systems, not one

Dhruv runs **brand chrome** and **app accent** side by side. Confusing them is the most common
design error, so the distinction is stated before any value.

| | Brand chrome | App accent + surfaces |
|---|---|---|
| Code twin | `DhruvBrand` (`ui/theme/DhruvBrandColors.kt`) | `DhruvNextColors` via `LocalDhruvNextColors` (`ui/theme/DhruvNextTokens.kt`) |
| Flips with light/dark? | **No** — theme-invariant | **Yes** |
| Carries | Brand identity: splash, hero gradient cards, settings identity card, deliberately-dark hero screens, launcher/notification marks | Everything else: page surfaces, text, actions, charts, states |
| User-overridable? | No | Accent only, via the Settings 4-swatch picker (ADR-0024 §2) |

### 1.1 Brand chrome (theme-invariant)

| Token | Hex | Use |
|---|---|---|
| `DhruvBrand.navy` | `#0D1B2A` | Ground, splash, launcher background, hero gradient end |
| `DhruvBrand.navyElevated` | `#132B4D` | Hero gradient start, glass surface |
| `DhruvBrand.blueMid` | `#1E3A6D` | Stat-card gradient, orbit ring |
| `DhruvBrand.accentBlue` | `#3FA7FF` | Info / links / positive-on-navy |
| `DhruvBrand.silver` | `#C0C6D1` | Secondary mark fill |
| `DhruvBrand.silverLight` | `#E6E9EF` | Star fill on navy |
| `DhruvBrand.steel` | `#8E97A6` | Meta text on navy |
| `DhruvBrand.logoBg` | `#F4F6FA` | Light tint behind the full-colour mark |

### 1.2 App tokens (theme-flipping)

Code twin: `DhruvNextLightColors` / `DhruvNextDarkColors`. Read only via
`LocalDhruvNextColors.current` — never import a raw colour constant into a screen.

| Role | Light | Dark |
|---|---|---|
| `bg` / `surf` / `surf2` | `#F9F9F9` / `#FFFFFF` / `#F3F4F6` | `#0A0A0A` / `#1E1E1E` / `#2C2C2C` |
| `line` / `lineStrong` | `#E5E7EB` / `#D1D5DB` | `#3A3A3A` / `#4A4A4A` |
| `tx` / `tx2` / `tx3` | `#111827` / `#374151` / `#6B7280` | `#F5F5F5` / `#CFD8DC` / `#9E9E9E` |
| `acc` / `accBright` | `#F05A28` / `#FF6D3B` | `#FF6D3B` / `#FF8A5C` |
| `accSoft` / `accLine` / `onAcc` | `#FFF1ED` / `#F9CDBC` / `#FFFFFF` | 14 % / 32 % accent alpha / `#003258` |
| `pos` / `posSoft` | `#00796B` / `#E0F2F0` | `#4DB6AC` / 16 % alpha |
| `neg` / `negSoft` | `#B3261E` / `#FCECEB` | `#F2B8B5` / 14 % alpha |
| `warn` / `warnSoft` | `#B45309` / `#FEF3E2` | `#FFB300` / 15 % alpha |
| `chart1…chart6` | `#F05A28` `#00796B` `#00B0FF` `#4A148C` `#B45309` `#455A64` | `#FF6D3B` `#4DB6AC` `#80D8FF` `#B388FF` `#FFB300` `#CFD8DC` |

**One global accent, not per-section.** `SectionTheme` (per-domain accent, ADR-0014 §8) is retired
by ADR-0024 §2 — every tab renders under one theme instance. The user may swap the accent globally
via a 4-swatch Settings picker; `resolveDhruvNextColors(darkTheme, accentColorHex)` derives
`acc`/`accBright`/`accSoft`/`accLine`/`onAcc` from that choice, so an override needs no component
changes.

**Never colour-only.** Positive/negative meaning always carries a glyph (▲/▼) or label alongside
the colour.

---

## 2. Typography

Three families, all shipped as bundled fonts (`ui/theme/DhruvFont.kt` — `SpaceGroteskFamily`,
`InterFamily`, `JetBrainsMonoFamily`). *(The pre-2026-08 `DESIGN.md` claimed "System Default
Roboto/Inter" — that was never true of the shipped app and is the reason that file was retired.)*

| Family | Weights | Use |
|---|---|---|
| **Space Grotesk** | 400/500/600/700 | Display, titles, brand wordmark — echoes the geometric logo |
| **Inter** | 400/500/600 | All UI and body copy |
| **JetBrains Mono** | 400/500 | Numerals, money, code, calculator entry — always tabular |

Named scale — read via `DhruvNextType.*`, never a raw `.sp` literal:

| Role | Phone | Tablet | Small |
|---|---|---|---|
| `hero` | 38sp | 46sp | 32sp |
| `title` | 17sp | 20sp | 15sp |
| `cardTitle` | 15sp | 17sp | 13.5sp |
| `body` | 13.5sp | 15sp | 12sp |
| `meta` | 11.5sp | 13sp | 10sp |
| `sectionLabel` | 10sp | 11sp | 9sp |

Keypad glyphs scale separately (`DhruvNextKeypad.*`) — they are fixed-grid button glyphs, not
content text, and sizing them on the content scale looks disproportionate.

**Money is always tabular numerals, and never ellipsised** — wrap or switch to compact format
(`₹4.8L` / `₹1.25Cr`) instead. Full format in lists, sheets, history and PDF; compact on cards and
widgets.

---

## 3. Spacing, radii, responsiveness

4dp base grid. Read via `DhruvNextSpacing.*` / `DhruvNextRadii.*`; resolved per screen size by
`calculateDhruvNextResponsiveTokens(widthDp, heightDp)`.

| Token | Phone | Tablet (≥600dp w) | Small (<360dp w or <600dp h) |
|---|---|---|---|
| `cardPadding` | 22dp | 24dp | 14dp |
| `screenGutter` | 16dp | 20dp | 12dp |
| `interCardGap` | 12dp | 16dp | 10dp |
| `sectionGap` | 24dp | 28dp | 20dp |
| `inputGroupGap` | 12dp | 16dp | 10dp |

Radii are **constant across all tiers** (radius scaling is not a standard responsive pattern):
`card` 16dp · `listGroup` 18dp · `innerTile` 14dp · `pill` 26dp.

List row minimum height 56dp; settings rows minimum 56dp; every touch target ≥ 48dp.

---

## 4. Logo & marks

Three locked directions (design v1.0 §3 D-3). Code: the `Dhruv*` composables in §5; assets in
`libs/core/src/main/res/drawable/` and each app's `mipmap-anydpi-v26/`.

| Direction | Shape | Where |
|---|---|---|
| **1a monoline** | Single-colour star silhouette | Adaptive-icon **monochrome** layer (themed launchers), notification small icon, OS splash icon |
| **1b solid duotone** | Filled star, navy body + one accent point | In-app mark, app-icon workhorse |
| **1c orbit tile** | Navy squircle + silver star + accent orbit | **Launcher icon** — adaptive foreground inside the 66 % safe zone, background `DhruvBrand.navy` |

The detailed chrome master PNG is splash / store-listing only, never below 96px.

---

## 5. Component library (`com.dhruv.core.ui.components`)

**Every component listed below exists in `:libs:core` today** — verified by symbol search, not
assumed. This is deliberate: the retired tracker design system declared a `BentoGrid`/`BentoCard`/
`HeroStatCard` library that was never built, and screens were written against a fiction for months.
**Rule: nothing enters this table before the code exists.** Planned-but-unbuilt work goes in §5.2,
clearly separated.

Feature modules own **screens and flows only**. Every reusable visual lives here, themed through
tokens. Zero feature-local styling: no raw hex, no ad-hoc typography, no one-off card shapes,
no `.dp`/`.sp` literals in a screen file. `:libs:core` stays internally dependency-free
(Compose + Material3 only).

### 5.1 Built

| Group | Components |
|---|---|
| **Brand** | `DhruvLogo` · `DhruvCrest` · `DhruvWordmark` · `DhruvWordmarkImage` · `DhruvWordmarkVertical` · `DhruvLogoWordmark` · `DhruvLogoWordmarkVertical` · `DhruvNotificationIcon` |
| **Surfaces** | `NxCard` · `NxInsetSurface` · `DhruvGlassCard` · `ListGroup` · `ListGroupRow` · `SectionLabel` |
| **Actions** | `NxButton` (Primary/Soft/Outline/Ghost/Destructive) · `NxIconButton` · `NxFab` · `NxExtendedFab` · `AskPill` · `QuickActionTile` · `KeypadButton` |
| **Inputs** | `NxTextField` · `SearchField` · `SegmentedRow` · `SwitchRow` · `Stepper` · `SliderWithPresets` · `NumericKeypad` · `Chip` · `Pill` · `ModeChipRow` · `PeriodChipRow` |
| **Data display** | `MoneyText` · `StatDeltaChip` · `ThreeUpStatRow` · `CountBadge` · `InitialsTile` · `SyncStatusChip` |
| **Charts** | `BarChart` · `TrendSparkline` · `AllocationStackedBar` · `CategoryBarRow` · `ProgressRing` · `CountdownRing` · `FinancialHealthRing` |
| **Navigation** | `BottomBar` · `NxTopBar` (back + title + actions) · `NxHomeTopBar` |
| **Overlays** | `DhruvModalSheet` · `ConfirmDangerDialog` · `ConsentGateScaffold` |
| **States** | `EmptyStateCard` · `SignedOutCard` · `OfflineStateCard` · `NotConfiguredCard` · `OfflineBanner` · `RetryErrorCard` · `SkeletonBlock` · `UndoSnackbarHost` · `DisclaimerFooter` |
| **AI** | `AiInsightStrip` · `SmartInsightCard` · `ChatBubble` |

Fault-isolation wrappers live one level up in `com.dhruv.core.ui`: **`FeatureHost`** (wraps every
route; renders `FeatureDisabledCard` on a flag-off and `FeatureErrorCard` on a thrown error — never
a blank crash) — see PLATFORM.md §4.

Selected contracts worth stating explicitly:
- **`MoneyText`** — THE money renderer for all new Compose work. Takes a pre-formatted string,
  tabular numerals, hero/row/inline variants. Non-Compose surfaces format via the same helpers, not
  a second implementation.
- **`StatDeltaChip`** — ▲/▼ glyph + text; `pos`/`neg` roles. Never colour-only.
- **`ProgressRing`** — stateless Canvas ring, centre slot, min 48dp, `contentDescription` required
  ("funded 62 percent").
- **`DhruvModalSheet`** — drag handle, keyboard-safe padding, primary action filled full-width,
  destructive as an error-colour text button.
- **`ConfirmDangerDialog`** — irreversible confirms only; account-level deletions use type-to-confirm.
- **`SegmentedRow`** — the one segmented control. No custom segmented controls anywhere.
- **`UndoSnackbarHost`** — 5s snackbar with Undo; one host per screen scaffold; queues, never stacks.

### 5.2 Planned (not built — do not write screens against these yet)

Reconciled against the design's own Component Library on **2026-08-09** (all 11 sections read
card-by-card, not just section headings — the earlier pass read headings only, which is why B1–B4
below were incomplete). Everything the design draws and the code lacks is now listed.

| Batch | Components | Design section |
|---|---|---|
| **B2 — input** | `NxCheckbox` · `NxRadio` · `PinEntry` (PIN/OTP) · `QwertyKeypad` · `DateRangeSheet` · `EnumPickerGrid` | Selection, Keypads |
| **B3 — data viz** | `DonutChart` + ranked legend · `PieChart` · `AmortisationDonut` · `PaceRing` (ring + period marker) | Charts |
| **B4 — list** | `DayGroupHeader` · `LedgerRow` · `SuggestedRow` (dashed until accepted) · `ReconcileBanner` | (Finance screens) |
| **B6 — form** *(new)* | `NxSelect` (dropdown field, e.g. "Reducing balance ▾") · `NxTextArea` (multi-line + helper text) · `InputChip` (removable, `×`) | Inputs, Selection |
| **B7 — feedback** *(new)* | `StatusBadge` (success / warning / error / accent dot variants — `CountBadge` covers counts only) · `Spinner` (indeterminate; distinct from `ProgressRing`) · `InfoBanner` (the design draws SNACKBAR · INFO · OFFLINE as three; only offline + snackbar exist) | Status / Feedback |
| **B8 — navigation** *(new)* | `NxTabs` with animated indicator (e.g. EMI / Schedule / Compare). **Not** the same control as `SegmentedRow` — the design draws segmented under Actions and tabs under Navigation, as two distinct components. | Navigation |
| **B9 — overlays** *(new)* | `SelectionSheet` (picker rows with ✓ selected state, e.g. "Choose currency") | Overlays |

### 5.3 Built, but narrower than the design draws

These exist and work; they are missing **variants the design specifies**. Listed separately from
§5.2 because the file is there — the gap is in its API, and a screen author will otherwise assume
full coverage and hand-roll the missing variant locally (exactly the feature-local styling the
micro-frontend rule forbids).

| Component | Has | Design also draws | Impact |
|---|---|---|---|
| `NxButton` | 5 variants (Primary/Soft/Outline/Ghost/Destructive), `enabled` | **Sizes** (Small / Medium), **Loading** state, **Block** (full-width) treatment | Sheets need the full-width primary (§8); forms need in-flight disable + spinner |
| `NxTextField` | label, prefix/suffix, placeholder, `singleLine`, read-only | **Error state with message** ("Enter a valid amount"), **helper/supporting text** | Every validated form field; currently no way to show a field error through the system |
| `CountBadge` | numeric count, "99+" cap | **Status dot** variants (success / warning / error / accent) | Status indicators fall back to ad-hoc dots |
| `Chip` / `Pill` | selected, filled, strong, leading icon | **Removable** input chip (trailing `×`) | Filter/tag removal UI |

Closing a §5.3 row means **extending the existing component**, never adding a parallel one — two
button components is precisely the fragmentation this library exists to prevent.

---

## 6. Navigation law

Applies to any Dhruv app's shell, whatever its tab set. An app's *own* route map lives in its spec;
these rules govern all of them.

| # | Rule |
|---|---|
| **N1** | Tab roots never show a back arrow. |
| **N2** | Every non-root shows exactly **one** back arrow to its **single** parent. |
| **N3** | Sheets dismiss down; a sheet never navigates. |
| **N4** | Add/edit forms confirm on discard. |
| **N5** | Alerts · app-switcher · Settings are reachable from the top bar on **every** tab. |
| **N6** | Deep links land on the tab root, then push. |
| **N7** | Every screen renders light and dark from the same tokens. Settings › Theme (System/Light/Dark) is the **only** switch. |

**Presentation classes.** Bottom sheet = quick entry, filters, consent, app switcher. Full-screen
modal (close ✕, not back ←) = add/edit forms and scoped consent flows. Bare full-frame, no chrome =
splash and pre-session onboarding.

**Mechanics.** A pager over the visible (flag-filtered) tab list, each tab hosting its own nested
`NavHost` for drill-ins; shell-level "no tab" routes render over the tabs with a back top bar.
Back-press precedence is encoded once in `resolveBackAction`
(`libs/core/.../navigation/BackContract.kt`) — detail route → active tab's nested stack → first tab
→ exit. Never re-derive that order inline.

**Cross-feature navigation is by id, never by class reference.** Features never import each other's
screens; a `NavTarget` names *where*, and the app shell resolves it (see `NavTarget.kt`). Adding a
route means adding the sealed subtype **and** the row in that app's route registry.

---

## 7. Screen-state matrix (app-wide law)

Every screen defines all applicable states. `/dhruv-ui-review` gates on this per screen.

| State | Component | When |
|---|---|---|
| default | — | content loaded |
| loading | `SkeletonBlock` | first load only; silent refresh afterwards |
| empty | `EmptyStateCard` | no data yet — always message **+ verb CTA** |
| error | `RetryErrorCard` | request failed, retryable |
| offline | `OfflineStateCard` (nothing cached) or `OfflineBanner` (cached content still shown) | no connectivity |
| signed-out | `SignedOutCard` | network-backed surface, no session |
| not-configured | `NotConfiguredCard` | feature has no implementation/config yet |
| disabled | `FeatureDisabledCard` (via `FeatureHost`) | feature flag off |

Signed-out / offline / not-configured are **designed states, not error dialogs** — never a spinner
that never resolves.

---

## 8. Interaction standards

- **Refresh** — explicit refresh (overflow action, retry buttons). No per-feature pull-to-refresh
  until one revisit lands it everywhere at once.
- **Sheets over screens** — create/edit flows are sheets or full-screen modals, not new pushed
  screens. Primary action filled full-width, disabled in-flight.
- **Dialogs** — irreversible confirms only.
- **Deletes** — soft-delete + `UndoSnackbarHost` (5s) + a recoverable location. Hard delete exists
  only in an explicit trash/account surface, and states its consequence.
- **FAB** — one per screen maximum, always "add the primary noun". Sheet-only screens have no FAB.
- **Top bars** — never more than 3 icons + overflow. Sub-screens: back + title (+ contextual
  subtitle). Sheets have drag handles, no top bars.
- **Haptics** — destructive confirms only.

**Motion.** Standard easing `cubic-bezier(.16, 1, .3, 1)`. Splash ≤ 2.5s, always. Charts animate in
once, not on every recomposition. Material default springs elsewhere; pager settle; sheet slide.
No bespoke choreography.

---

## 9. Accessibility (gate, not aspiration)

- Touch targets ≥ 48dp; settings/list rows ≥ 56dp.
- Text and icon contrast ≥ 4.5:1 (AA) in **both** themes.
- No colour-only meaning — glyphs accompany every delta and status.
- `contentDescription` on every icon-only action, chart, ring, chip and widget root
  ("Net worth, ₹18.42 lakh, up 6.4 percent this month").
- Dynamic-type safe: no fixed-height text containers; long money values wrap or compact-format,
  never ellipsise.
- TalkBack order follows visual hierarchy; an auto-firing prompt must not steal focus.

---

## 10. Copy conventions

Sentence case throughout. `₹` with a space in full format (`₹ 1,20,000.50`), tight in compact
(`₹4.8L`). Relative dates for recency ("updated 3 days ago"), absolute for obligations ("due 12 Aug").

- Empty states pair a message with a **verb** CTA ("Add your first asset").
- Errors say what to do, not what happened internally — "Couldn't reach your data. Retry.", never
  exception text.
- Destructive dialogs name the consequence: "This permanently deletes all your tracker data. This
  cannot be undone."
- Consent copy names the processor and hosting region, and the in-app erasure paths.
- Derived/AI output is **labelled as derived** — never presented as plain fact.
- All user-visible strings land in `strings.xml` from birth. New screens never add hardcoded literals.

---

## 11. Non-Compose surfaces

Same type hierarchy and token values, expressed in each surface's own dialect. Per-app channel and
intent **rows** live in that app's surface registry; the conventions below are global.

- **Notifications** — sentence case, ≤1 line collapsed (≤2 expanded; `BigTextStyle` only for
  genuinely long-form content). Never more than **two** action buttons. Never a policy/account
  number. Never an account name and an amount in the same line under privacy mode. Every channel
  has exactly one control in Settings, owned by the module that defines the channel — channel
  registry and controls stay 1:1. An app-wide notification master switch and the system
  permission state are app-level and never duplicate a per-channel control.
- **Widget (Glance)** — theme mapped from the same day/night roles; compact money only;
  `contentDescription` on the root; defines its own value / masked / signed-out / disabled states.
- **PDF export** — same type hierarchy mapped to `PdfDocument` text sizes; **full** money format,
  never masked (it is an explicit user act, and the export dialog says so); footer carries generated
  date + app version; no logo beyond the wordmark.

---

## 12. Web parity

The web SPA consumes the **same token values** through CSS custom properties
(`web/src/shared/styles/tokens.css`) and mirrors the component contracts in
`web/src/shared/components/`. Two hand-maintained implementations of one system: nothing
automatically catches drift, so **any change to §1–§3 here must land on both sides in the same
change set**. Web-specific layout (desktop sidebar shell) is expression, not a second system.

### 12.1 Known drift (audited 2026-08-09 — open)

The design's own Component Library flags this risk explicitly: its TOKENS section carries a
**"PROPOSED · add to both platforms — promote them so web & Android never drift"** card. A
value-by-value audit found the drift had already happened:

| Token | Android (`DhruvNextTokens.kt`) | Web (`tokens.css`) | Status |
|---|---|---|---|
| list-group radius | `listGroup = 18.dp` | `--radius-list-group: 14px` | ❌ **drifted** |
| inner/control radius | `innerTile = 14.dp` | `--radius-inner: 12px` | ❌ **drifted** |
| card radius | `card = 16.dp` | `--radius-card: 16px` | ✅ |
| pill radius | `pill = 26.dp` | `--radius-pill: 26px` | ✅ |
| brand font | `brandSerifFamily` → **aliased to `SpaceGroteskFamily`** (correct; the name is legacy and misleading) | `--font-brand: Georgia, serif` | ❌ **web wrong** — pre-DhruvNext leftover |
| display / body / mono | Space Grotesk / Inter / JetBrains Mono | same three | ✅ |
| `--radius-control` (buttons, 12) | — | — | ⚠ design proposes it; **missing on both** |

Two further web-side issues, both cleanup rather than drift:
- **Duplicate colour layers.** `tokens.css` carries a legacy Material-3 `--color-*` set *and* the
  DhruvNext `--c-*` set. The design's PROPOSED card asks for semantic `--color-success` /
  `--color-error` / `--color-warning` / `--color-outline` / `--color-on-surface-faint` — but the
  `--c-*` layer **already provides** all of them (`--c-pos`, `--c-neg`, `--c-warn`, `--c-line`,
  `--c-text-3`), as it does chart 1–6. The correct resolution is to **retire the legacy `--color-*`
  layer**, not to extend it — adding the roles twice would institutionalise the duplication the
  design is warning about.
- Android's `brandSerifFamily` should be renamed (it is not serif); harmless but actively misleading.

**None of these are fixed by this document** — recording them is the point, so the next web or
tokens change closes them deliberately rather than picking a side by accident. Fixing the two
drifted radii is a visual change on the web and needs a look before it lands, which is why it is
tracked here instead of silently patched.

---

## 13. Traceability

Design v1.0 FINAL was imported from the Claude Design project
`Dhruv brand & UI/UX finalization` (2026-08-08) — files `Dhruv Brand & Theme.dc.html`,
`Dhruv Component Library.dc.html`, `Dhruv Launch & Logo.dc.html`, `Dhruv Web App.dc.html`,
`Dhruv Android Screens.dc.html`. (`support.js` is the generated dc-runtime — framework, no design
content.)

**Reconciliation pass 2026-08-09.** Re-pulled the project and byte-compared the Component Library
against the import: **source unchanged**. Read all 11 component sections card-by-card (the
2026-08-08 pass read section headings only), then diffed design ↔ `:libs:core` ↔ this document.
Result: §5.2 gained batches B6–B9, §5.3 (built-but-narrower) is new, and §12.1 records real
web/Android token drift the design had warned about. To repeat this audit:

```bash
# every component this doc claims is built must resolve in :libs:core
python - <<'PY'
import re, subprocess
built = open('platform/DESIGN-SYSTEM.md', encoding='utf-8').read() \
        .split('### 5.1 Built')[1].split('### 5.2 Planned')[0]
names = sorted(set(re.findall(r'`([A-Z][A-Za-z0-9]+)`', built)))
src = subprocess.run(['grep','-rhoE','(fun|object|val) [A-Z][A-Za-z0-9]*','libs/core/src/main'],
                     capture_output=True, text=True).stdout
have = {l.split()[-1] for l in src.strip().splitlines()}
print('MISSING:', [n for n in names if n not in have] or 'none — all verified real')
PY
```

Decision trail: **ADR-0014 §8** (design system in `:libs:core`, micro-frontend rule) → **ADR-0024**
(single global accent; `SectionTheme` retired) → **ADR-0027** (5-tab nav for Finance) → **ADR-0028**
(brand chrome as a second theme-invariant palette) → **ADR-0030** (this document becomes the global
binding system; supersedes the tracker design system and app-design-standard).
