# Shell (no tab)

Modules reached **from the running shell's chrome** — the top-bar icon, the floating Ask pill, or
Settings — never from a tab's own nested back stack. This is not a junk drawer: the nav contract
(`apps/finance/docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md` §4) explicitly states
Currency/Unit/Date/Time/Settings/Ask "belong to no tab" — they're shell-level detail routes with
their own back-top-bar, deliberately excluded from `NavTarget`'s cross-tab dispatch because nothing
needs to jump to them from another tab's nested stack.

| Module | Status |
|---|---|
| [currency](currency/README.md) | live |
| [unit](unit/README.md) | live |
| [date](date/README.md) | live, flag **disabled** |
| [time](time/README.md) | live, flag **disabled** |
| [assistant](assistant/README.md) | live |
| [automation](automation/README.md) | not yet created — Phase 7 (last), reached from Settings |

Each is wired into `apps/finance/app/.../ui/shell/DetailRoute.kt`'s sealed interface + the shell's
`detailRoute` overlay state in `MainActivity.kt` — not a nested `NavHost` (that shape is reserved
for tabs with real drill-in sub-routes, currently only Plan; see `MainActivity.kt`'s class doc).
