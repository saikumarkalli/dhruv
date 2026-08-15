# Onboarding (pre-tab)

Not owned by any tab — these are the bare, full-frame screens shown *before* the 5-tab shell
exists at all (sign-in → consent → empty start), reached exactly once per install (or once per
sign-out).

| Module | Status |
|---|---|
| [onboarding](onboarding/README.md) | not yet created — **Phase 1, the next phase to build** |

Kept as its own top-level bucket rather than folded into `shell/` because it's temporally distinct:
`shell/` modules are reached *from* the running shell's chrome (top bar, Ask pill, Settings) once
a session exists; onboarding runs *before* the shell renders at all.
