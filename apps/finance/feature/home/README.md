# Home tab

Modules reached from the **Home** tab (ADR-0027, DhruvNext's first of 5 tabs).

| Module | Status |
|---|---|
| [networth](networth/README.md) | not yet created — Phase 2 |

The Home tab's own root screen (01 — greeting, net-worth hero, quick actions, UPCOMING list) is
**shell-owned**, not a feature module — it lives in `apps/finance/app/.../ui/dashboard/` (currently
`DashboardScreen.kt`, a placeholder until Phase 2 replaces it with the real screen). Notifications
(B2) and Search (B3) are also shell-owned, same reasoning as Home itself: they don't have enough
independent logic to justify their own Gradle module, and splitting the app shell's own top-level
screens into modules would just add indirection with no boundary to enforce.
