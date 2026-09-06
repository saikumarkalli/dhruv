# Contract: back navigation and leaving a screen

**Phase 1 output** · [spec.md](../spec.md) FR-208..FR-208c · [research.md](../research.md) R1

The UI contract for how a person leaves any Money screen. It exists because the two ways of leaving
currently behave differently, and only one of them is reachable.

---

## The rule

**Leaving a screen is one behaviour with two triggers.** The on-screen back control and the phone's
back button or gesture MUST produce the same destination and the same confirmations. Neither may do
something the other does not.

## Current state, verified

| Fact | Consequence |
|---|---|
| Five drill-in screens render no top bar and accept no back callback | The phone gesture is the only route out |
| No `BackHandler` exists in the Money module or `:libs:core` | No screen can intervene in a back press |
| `MainActivity`'s callback routes `POP_NESTED` straight to `popBackStack()` | The nested stack pops with nothing consulted |
| `DiscardGuard.attemptDismiss()` is called only from close buttons | The confirmation is unreachable on screens without one |

Together: **on a form, the only working way out discards unsaved input without asking.**

## Required behaviour

### Every non-root Money screen

- Displays a title naming the screen, and exactly one back control (FR-208).
- A tab root displays no back control (navigation law N1).
- A full-screen form uses a close (✕) rather than a back arrow — the existing, correct convention;
  the parity rule below applies to it identically.

### Both triggers

| Screen state | On-screen control | Phone back | Must match |
|---|---|---|---|
| Read-only screen | Returns to parent | Returns to parent | ✓ |
| Form, nothing typed | Closes | Closes | ✓ |
| Form, unsaved changes | Asks "discard changes?" | **Asks "discard changes?"** | ✓ — currently ✗ |
| Confirmation dialog open | — | Dismisses the dialog only | Does not also leave the screen |
| Picker or sheet open | — | Dismisses the picker only | Does not also leave the screen |
| Save in flight | Disabled | Does not abandon a write silently | Person is told, or the leave waits |

### Mechanism

A screen with something to say about leaving hosts a back handler **enabled only while it has
something to say**, calling the *same* guard its close control calls. The central back resolution
(`resolveBackAction`) is not modified — it is correct, and it is deliberately a stateless, unit-tested
pure function that screen-local state must not leak into.

Enabling the handler conditionally matters: a permanently-enabled handler would swallow back presses
on a clean form and break the parent-return path.

## Test obligations

| Id | Assertion |
|---|---|
| BACK-1 | Every non-root Money screen exposes a back affordance and a title |
| BACK-2 | A tab root exposes no back affordance |
| BACK-3 | On a dirty form, the back trigger raises the discard confirmation and does **not** leave |
| BACK-4 | Confirming discard leaves; dismissing the confirmation stays, with input intact |
| BACK-5 | On a clean form, the back trigger leaves immediately with no confirmation |
| BACK-6 | Back with a dialog or picker open dismisses only that, and the screen remains |
| BACK-7 | The account form raises the same confirmation as the transaction form — it currently has none by any route |

BACK-3 through BACK-7 are view-model and guard-level tests using the existing fakes; they need no
device. BACK-1/BACK-2 are screen-composition assertions.

## Anti-requirement

**Do not ship the visible back control without the guard.** The control makes screens people
currently avoid feel navigable, which makes the silent discard *easier* to reach, not harder. The
two are one slice.
