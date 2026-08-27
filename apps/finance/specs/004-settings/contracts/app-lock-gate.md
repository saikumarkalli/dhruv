# Contract — App lock gate

**What this is**: the shell-level checkpoint that makes app lock real. It is architecture, not a
screen: FR-021 requires a *single* app-wide gate with no exempt surface, and FR-023 requires links
arriving while locked to be held and dispatched after unlock.

**Where it lives**: the decision in `:libs:core` (`security/AppLockDecision.kt`, pure); the effect in
`:apps:finance:app` (`ui/settings/AppLockGate.kt` plus `MainActivity` wiring). This is the same
decision/effect split `navigation/BackContract.kt` already uses.

---

## 1. Decision (pure, JVM-testable)

```
appLockState(
  enabled            : Boolean,     // the user's stored preference
  timeout            : LockTimeout, // Immediate | After1Min | After5Min | After15Min
  elapsedSinceBackground : Duration?, // null = cold start
  alreadyAuthenticatedThisForeground : Boolean
) : LockState   // LOCKED | UNLOCKED
```

**Rules**

1. Cold start with `enabled = true` is **LOCKED**, always — there is no "recently used" credit across
   process death.
2. `Immediate` locks on any backgrounding, however brief.
3. A successful authentication unlocks for the current foreground session only.
4. `enabled = false` is always UNLOCKED. Turning lock off never leaves a stale locked state.
5. The function is total and side-effect free: no clock access inside it, elapsed time is passed in.
   This is what makes every rule above a unit test rather than a manual device check.

---

## 2. Effect (the gate)

The gate wraps the **entire** content tree in `MainActivity` — above the pager, above the detail-route
overlay, above every tab including Calc.

6. While LOCKED, no app content is composed or drawn. Not dimmed, not blurred — absent. A screenshot
   of the locked app shows the lock surface only.
7. The gate MUST resolve before the first content frame. A flash of unlocked content on cold start is a
   defect, not a cosmetic issue — it is the exact failure the whole feature exists to prevent.
8. Authentication is `BiometricPrompt` at Class 3 (Strong) **with device-credential fallback**, so a
   user whose biometric fails or is unenrolled can still reach their own data.
9. A cancelled or failed attempt leaves the gate LOCKED with a retry affordance. There is no attempt
   limit and no lockout of our own — the platform owns that.
10. App lock MUST NOT be enablable when the device has no enrolled credential; Settings tells the user
    what to enrol instead (FR-022). The check runs when the switch is toggled, not only at lock time.

---

## 3. Held-intent dispatch

11. A deep link, notification tap, or launcher shortcut arriving while LOCKED is **held**, not
    delivered and not dropped (FR-023).
12. On successful unlock, the held target is dispatched exactly once, then cleared.
13. A cancelled unlock retains the held target for the next successful unlock **within the same
    launch**; process death clears it.
14. Only one target is held. A second arrival while locked replaces the first — the user's most recent
    intent wins.
15. The held target uses the existing `NavTarget` vocabulary; an unknown or foreign id resolves to the
    normal not-found state after unlock, never a crash (surface registry §1's rule for intent extras
    applies unchanged).

---

## 4. Interaction with the rest of the app

16. Hide-amounts is **independent** of lock state. Masking applies whether or not lock is on; unlocking
    does not unmask.
17. The legacy calculator history lock keeps working and is not changed by enabling app lock (FR-028).
    Both may be on; the history PIN gates history within an already-unlocked app.
18. Lock state never blocks a background alert from being posted — only from being *opened*. The
    notification itself is governed by the module's alert control and the app-wide master switch.

---

## 5. Enforcement

| Rule | Enforced by |
|---|---|
| Cold start locks; timeout semantics; unlock scope | Unit tests over `appLockState` — all of §1 |
| No exempt surface | A test asserting the gate wraps the content root, plus manual verification per tab at the QA step |
| No flash of unlocked content | Explicit acceptance condition at the phase checkpoint |
| Held target dispatched once | Unit test over the hold/dispatch state machine |
| Cannot enable without an enrolled credential | ViewModel test with a stubbed credential-availability check |