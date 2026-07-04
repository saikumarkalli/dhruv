# Dhruv Tracker — Design System (UI/UX source of truth)

> Status: **BINDING** for all tracker phases (P1–P6) and, opportunistically, for existing screens
> as later phases touch them. Umbrella: `2026-07-03-tracker-roadmap-overview.md`. ADR-0014 §8.

## Micro-frontend UI rule (binding)

Feature modules behave like micro-frontends: each owns only its **screens and flows**. Every
reusable visual component lives in **`:libs:core`** (`com.dhruv.core.ui.components.*`), themed
exclusively through `DhruvTheme` / `SectionTheme` MaterialTheme roles.

**Zero feature-local styling**: no raw hex, no ad-hoc typography, no one-off card shapes inside
feature modules. One theme, one style, entire application. `:libs:core` stays internally
dependency-free (compose + Material3 only). Existing screens are NOT retrofitted in P1 (framework
protection); they adopt components in later phases.

## Component inventory (`com.dhruv.core.ui.components`)

| Component | Contract | Used by |
|---|---|---|
| `BentoGrid` | 2-column layout, 16dp outer margin, 12dp gutters; children declare full/half span | Every dashboard/hub |
| `BentoCard` | Tonal surface container, 24dp corner, elevation 0, optional onClick ripple; slots: label, value, supporting row | All cards |
| `HeroStatCard` | Full-width ~1.6:1; label (labelMedium onSurfaceVariant), value (headlineMedium, tabular numerals), `StatDeltaChip` slot, bottom-third sparkline slot | Home hero, P2 budget hero |
| `StatDeltaChip` | ▲/▼ glyph + text; positive → tertiary/success container roles, negative → error container; never color-only | Deltas everywhere |
| `TrendLineChart` | Stateless Canvas: 2dp stroke primary, gradient fill primary→12% alpha transparent, endpoint dot 4dp, no axes/gridlines (sparkline mode), min height 48dp; handles 0/1-point; required `contentDescription` ("… from ₹X on <date> to ₹Y today") | Net worth, holdings, P5 projection (axis mode later) |
| `EmptyStateCard` | Icon + one-line message + CTA button | All empty states |
| `OfflineBanner` | Persistent top banner, wifi-off icon, onSurfaceVariant | Offline states |
| `RetryErrorCard` | Message + Retry filled-tonal button | Error/offline retry |
| `ConsentGateScaffold` | Title, bullet list slot, accept (filled, full-width) + decline (text) | P1 consent; P6 per-source consents |
| `DhruvModalSheet` | Material3 `ModalBottomSheet`: drag handle, 28dp top radius, keyboard-safe padding, primary action filled full-width, destructive as error-color text button | All entry sheets |
| `ConfirmDangerDialog` | Irreversible-action confirm; optional type-to-confirm ("DELETE") | Delete data/account |

P2 additions: `BarChart`, `DonutChart` (same Canvas conventions).

## Grid & spacing

4dp base grid. Bento: 16dp outer margin, 12dp gutters, hero full-width, half-cards 1:1.1 aspect.
Content padding inside cards: 16dp. Screen edge padding: 16dp. List row min height 56dp.

## Color

- Roles only: `MaterialTheme.colorScheme.*` via `DhruvTheme` + per-tab `SectionTheme` accent.
- Home/tracker accent: **"green"** section theme. Tools keeps `calculatorColor` preference.
- Positive/negative: tertiary-vs-error container roles + glyphs (▲/▼) — never color alone.
- Dark/light: automatic via theme; no component may branch on isDark itself.

## Typography

Existing `DhruvTheme` scale. Money: headlineMedium (hero), titleMedium (rows), always tabular
numerals (`FontFeature "tnum"` where available). Compact money (`formatPaiseCompact`: ₹4.8L /
₹1.25Cr / plain below 1L) on cards; full `formatPaise` in lists, sheets, history.

## States (every screen must define all)

default · loading (spinner first-load only, silent refresh after) · empty (`EmptyStateCard`) ·
error (`RetryErrorCard`) · offline (`OfflineBanner` + retry) · disabled (`FeatureDisabledCard`
via FeatureHost) · not-configured (tracker only).

## Motion & haptics

Material default springs; pager settle; sheet slide. No custom choreography in P1. Haptic:
confirm on destructive actions only.

## Accessibility checklist (gate for `/dhruv-ui-review`, per screen)

- Touch targets ≥ 48dp.
- Contrast AA on all text/icon roles.
- Charts and chips have contentDescription; TalkBack order: hero → assets → liabilities → FAB.
- No color-only meaning (glyphs accompany deltas).
- Dynamic type safe: no fixed-height text containers; long ₹ values ellipsize never — wrap or
  compact-format instead.

## Copy conventions

Sentence case; ₹ prefix with space ("₹ 1,20,000.50" full / "₹4.8L" compact); relative dates
("updated 3 days ago"); destructive dialogs name the consequence ("This permanently deletes all
your tracker data. This cannot be undone."). Consent copy must name Supabase + hosting region and
the in-app erasure paths.
