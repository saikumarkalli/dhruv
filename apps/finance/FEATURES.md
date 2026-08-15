# Dhruv Finance — Feature Modules

Index for all Finance feature modules after the Phase 4 split. Each is a `dhruv.android.library`
+ `dhruv.android.compose` Koin module under `apps/finance/feature/<bucket>/<name>/` (grouped by
owning tab since 2026-08-09 — Gradle coordinates unchanged, see
[feature/README.md](feature/README.md)), namespace `com.dhruv.finance.<name>`, depending on
`:apps:finance:data`, `:libs:core`, `:libs:settings`. Flags live in
`platform/feature-flags/dhruv-finance.json`.

**Per-module detail (screens, ViewModels, data deps) lives in each module's own `README.md`** —
this table is deliberately just an index (removed the duplicated per-module prose 2026-08-09; the
README is now the one place that detail is written, so it can't drift out of sync with itself).

| Module | Gradle coordinate | Owner tab | Flag | Status |
|---|---|---|---|---|
| [calculator](feature/calc/calculator/README.md) | `:apps:finance:feature:calculator` | Calc | `calculator` | enabled |
| [loans](feature/plan/loans/README.md) | `:apps:finance:feature:loans` | Plan | `loans` | enabled |
| [investments](feature/plan/investments/README.md) | `:apps:finance:feature:investments` | Plan | `investments` | enabled |
| [tax](feature/plan/tax/README.md) | `:apps:finance:feature:tax` | Plan | `tax` | enabled |
| [everyday](feature/plan/everyday/README.md) | `:apps:finance:feature:everyday` | Plan | `everyday` | enabled |
| [currency](feature/shell/currency/README.md) | `:apps:finance:feature:currency` | none — shell | `currency` | enabled |
| [unit](feature/shell/unit/README.md) | `:apps:finance:feature:unit` | none — shell | `unit` | enabled |
| [date](feature/shell/date/README.md) | `:apps:finance:feature:date` | none — shell | `date` | **disabled** |
| [time](feature/shell/time/README.md) | `:apps:finance:feature:time` | none — shell | `time` | **disabled** |
| [assistant](feature/shell/assistant/README.md) | `:apps:finance:feature:assistant` | none — shell | `assistant` | enabled, gated `minVersion 1.2.0`, `requiresConsent` |
| [networth](feature/home/networth/README.md) | `:apps:finance:feature:networth` | Home | `networth` | **not yet created** — Phase 2; flag provisioned ahead of the module, same as `date`/`time` |

---

## Design system

All feature screens use the **DhruvNext design system** (ADR-0024). Token usage:
- Colors: `LocalDhruvNextColors.current` (`acc`, `surf`, `tx`, `tx2`, `tx3`, `line`, `neg`, `pos`, etc.)
- Typography: `DhruvNextType.*` (`hero`, `title`, `cardTitle`, `body`, `meta`, `sectionLabel`)
- Spacing: `DhruvNextSpacing.*` (`screenGutter`, `interCardGap`, `sectionGap`, `cardPadding`, `inputGroupGap`)
- Radii: `DhruvNextRadii.*` (`card`, `listGroup`, `innerTile`, `pill`)
- Components: `NxCard`, `NxButton`, `NxTextField`, `SegmentedRow`, `SectionLabel`, `ListGroup`, `StatDeltaChip`, etc.

Zero `MaterialTheme.colorScheme` / `MaterialTheme.typography` refs remain in any screen file.
`CardDefaults` usage in date sub-views is intentional (accent-tinted `accSoft` result cards).
