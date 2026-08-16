# specs/ — reserved for cross-app spec-kit features only

This directory is intentionally empty by default. Per-app spec-kit work lives at
`apps/<app>/specs/NNN-slug/` (e.g. `apps/finance/specs/001-net-worth-tracker/`) — that is the
normal case for nearly every feature.

Put a feature here only if it genuinely touches more than one app (or an app plus the web SPA) in
the same unit of work. If you're unsure, it's app-level, not here.

Full rule: `.specify/memory/constitution.md` → **Spec-Kit Directory Rule**.
Per-app tracking tables (which phase maps to which spec-kit directory): each app's own
implementation plan, e.g. `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §7.
