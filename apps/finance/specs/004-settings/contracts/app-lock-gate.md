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
  alreadyAuthenticatedThisForeground : Boolean,
  hasEnrolledCredential : Boolean,  // checked at every resolve, not only when the switch is toggled
) : LockState   // LOCKED | UNLOCKED
```

**Rules**

1. Cold start with `enabled = true` is **LOCKED**, always — there is no "recently used" credit across
   process death.
2. `Immediate` locks on any backgrounding, however brief. Every other timeout locks when
   `elapsedSinceBackground >= timeout` — the boundary is inclusive, not exclusive (found
   underspecified during 0b.3's security checklist review, `CHK011`: the `>=` operator previously
   existed only in `tasks.md`'s paraphrase of this rule, not here).
3. A successful authentication unlocks for the current foreground session only. **"Foreground
   session"** is defined precisely as the interval between `ProcessLifecycleOwner`'s `ON_START` and
   its next `ON_STOP` — the whole-process lifecycle signal, not a single Activity's `onStart`/
   `onStop` (which also fire across a configuration change, e.g. rotation, that must **not** count as
   backgrounding). `elapsedSinceBackground` is measured from that `ON_STOP` (found underspecified
   during 0b.3's security checklist review, `CHK012`).
4. `enabled = false` is always UNLOCKED. Turning lock off never leaves a stale locked state.
5. **`hasEnrolledCredential = false` is always UNLOCKED, regardless of `enabled`** (found
   underspecified during 0b.3's security checklist review, `CHK005`). Rule 10 (§2) already prevents
   *enabling* app lock without a credential; this rule covers the case the device's *only* credential
   is removed **after** app lock was already on — with zero credential enrolled, `BiometricPrompt`
   cannot present any prompt at all, and without this rule the user would be permanently excluded
   from Settings (where they would otherwise turn the switch off), since Settings is itself behind
   the same gate. The effect (§2 rule 12) additionally resets `biometric_enabled` to `false` and
   shows a one-time notice when this path is taken, so the user knows protection was intentionally
   removed, not silently bypassed — this decision rule only says the *state* is UNLOCKED; the
   preference write and notice are the effect's job.
6. The function is total and side-effect free: no clock access inside it, elapsed time is passed in,
   and credential availability is passed in rather than queried. This is what makes every rule above
   a unit test rather than a manual device check.

---

## 2. Effect (the gate)

The gate wraps the **entire** content tree in `MainActivity` — above the pager, above the detail-route
overlay, above every tab including Calc.

7. While LOCKED, no app content is composed or drawn. Not dimmed, not blurred — absent. A screenshot
   of the locked app shows the lock surface only. **The lock surface itself shows only static app
   chrome (wordmark/logo) and the unlock affordance — no session-derived data** (display name,
   avatar, email, or any other identity-bearing value) is shown before authentication succeeds
   (found underspecified during 0b.3's security checklist review, `CHK007`).
8. The gate MUST resolve before the first content frame. A flash of unlocked content on cold start is a
   defect, not a cosmetic issue — it is the exact failure the whole feature exists to prevent.
9. Authentication is `BiometricPrompt` at Class 3 (Strong) **with device-credential fallback**, so a
   user whose biometric fails or is unenrolled can still reach their own data.
10. A cancelled or failed attempt leaves the gate LOCKED with a retry affordance. There is no attempt
    limit and no lockout of our own — the platform owns that.
11. App lock MUST NOT be enablable when the device has no enrolled credential; Settings tells the user
    what to enrol instead (FR-022). The check runs when the switch is toggled, not only at lock time.
12. Credential availability (§1 rule 5's `hasEnrolledCredential`) is also re-checked at every gate
    resolution, not only at toggle time. When it is `false` while `enabled = true`, the gate falls
    open to UNLOCKED, persists `biometric_enabled = false` back through `SettingsRepository`, and
    shows a one-time notice ("App lock was turned off — no screen lock is set on this device").

---

## 3. Held-intent dispatch

13. A deep link, notification tap, or launcher shortcut arriving while LOCKED is **held**, not
    delivered and not dropped (FR-023).
14. On successful unlock, the held target is dispatched exactly once, then cleared.
15. A cancelled unlock retains the held target for the next successful unlock **within the same
    launch**; process death clears it.
16. Only one target is held. A second arrival while locked replaces the first — the user's most recent
    intent wins.
17. The held target uses the existing `NavTarget` vocabulary; an unknown or foreign id resolves to the
    normal not-found state after unlock, never a crash (surface registry §1's rule for intent extras
    applies unchanged).

---

## 4. Interaction with the rest of the app

18. Hide-amounts is **independent** of lock state. Masking applies whether or not lock is on; unlocking
    does not unmask.
19. The legacy calculator history lock keeps working and is not changed by enabling app lock (FR-028).
    Both may be on; the history PIN gates history within an already-unlocked app.
20. Lock state never blocks a background alert from being posted — only from being *opened*. The
    notification itself is governed by the module's alert control and the app-wide master switch.

---

## 5. Enforcement

| Rule | Enforced by |
|---|---|
| Cold start locks; timeout semantics; unlock scope; no-credential fall-open | Unit tests over `appLockState` — all of §1 |
| No exempt surface | A test asserting the gate wraps the content root, plus manual verification per tab at the QA step |
| No flash of unlocked content | Explicit acceptance condition at the phase checkpoint |
| Locked surface shows no session-derived data | Manual verification per tab at the QA step (screenshot review) |
| Held target dispatched once | Unit test over the hold/dispatch state machine |
| Cannot enable without an enrolled credential; falls open when credential later removed | ViewModel test with a stubbed credential-availability check, both at toggle time and at gate-resolve time |
