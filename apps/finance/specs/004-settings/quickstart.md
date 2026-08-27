# Quickstart — verifying the Settings control plane

How to prove this feature works. Ordered so a failure tells you which layer broke.

**Prerequisites**: `JAVA_HOME` set to the Android Studio JBR (JDK 17+). A device or emulator with a
screen lock enrolled — several checks below cannot run without one.

---

## 1. Gate the whole thing

```bash
./gradlew regressionCheck
```

All unit tests + ArchUnit + merged JaCoCo + the coverage floor. This is what CI runs; nothing below
matters if this is red.

Narrower loops while working:

```bash
./gradlew :libs:settings:testDebugUnitTest      # contribution contract + registry
./gradlew :libs:core:testDebugUnitTest          # AppLockDecision (pure)
./gradlew :apps:finance:app:testDebugUnitTest   # tier ViewModels, renderer, ArchUnit rules
```

---

## 2. Prove the contribution mechanism (the load-bearing requirement)

**Registry behaviour** — unit tests in `:libs:settings`:

- a contribution whose `moduleKey` is disabled does not appear;
- a contribution whose `moduleKey` is enabled but below `minVersion` does not appear;
- ordering is `order` then title, deterministically;
- a contribution that throws while producing rows does not remove the others.

**FR-004 / SC-004 — the diff test.** The claim is "adding a module changes no Settings file". Verify it
literally:

```bash
git status --porcelain      # clean first
# add a throwaway module with a SettingsContribution, register it in that module's Koin module
./gradlew :apps:finance:app:assembleDebug
git status --porcelain      # expect: only the new module's own files
```

If any file under `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/settings/` appears in that
diff, the mechanism is not done — a list is hiding somewhere. Delete the throwaway module afterwards.

**ArchUnit rules** (in `:apps:finance:app`'s existing `DependencyRulesTest`):

- no class in `…app.ui.settings` references a feature-module type;
- no `SettingsContribution` implementation references a Compose type.

Both should fail if you deliberately break them — write the violating class, watch it fail, delete it.
A rule nobody has seen fail is a rule nobody knows works.

---

## 3. Prove the 19 rows survived (SC-001)

Unit test first: assert today's `SettingsKeys` key set is a **subset** of the shipped set. A moved row
that got a new key silently resets every user's preference — this test is the only thing that catches
it, because the app will look fine.

Then on device, with a build of the previous version installed:

1. Set a distinctive value for each of the 19 rows (odd precision, non-default accent, history lock on).
2. Install this build over it — do not uninstall.
3. Walk the new tiers and confirm every value survived at its new home.

Row-by-row inventory is in [data-model.md](./data-model.md) §2.

---

## 4. Prove the lock is real

Unit tests cover the decision (`appLockState`) — cold start, each timeout, unlock scope. Run those
first; they are cheap and cover most of the rules.

On device, with a screen lock enrolled:

| Check | Expected |
|---|---|
| Enable app lock, kill the app, relaunch | Prompt before any content. Cold start always locks. |
| Cancel the prompt | No content visible. Not dimmed — absent. Screenshot to confirm. |
| Background past the timeout, return | Prompt again. |
| Open the **Calc** tab specifically after a lock | Also gated. There is no exempt surface. |
| Tap a notification while locked | Unlock first, then land on the notification's destination. |
| Cancel that unlock, then unlock successfully | Still lands on the held destination, once. |
| Cold start with a held target from a previous launch | No held target. Process death clears it. |
| Disable the screen lock on the device, relaunch | Not permanently locked out. |
| Try to enable app lock on a device with no credential | Refused, with what to enrol. |

Watch for a **flash of unlocked content** on cold start — record the screen and step through if unsure.
It is a defect, not a cosmetic issue.

---

## 5. Prove account control works

| Check | Expected |
|---|---|
| Signed out, open Settings › Account | Sign-in offered and working — no placeholder identity, no forced trip through first-run onboarding |
| Sign in, reopen | Real identity shown |
| Sign out | Session ends; calculator history still there |
| Toggle a consent off, restart | Still off; dependent surfaces show their no-consent state |
| Delete my data, confirm | Financial records gone, still signed in |
| Delete my account, confirm | Typed confirmation required; next launch is first-run |
| Turn airplane mode on, then delete my data | Reported as failed; action still available; nothing claims success |

---

## 6. Prove the assistant consent is durable (FR-036)

1. Grant assistant consent.
2. Force-stop the app and relaunch.
3. Open the assistant — it must **not** ask again.
4. Withdraw consent in Settings; open the assistant — it must show its gate before any request.

This is a defect fix, so the useful version of this check is running it against the current build first
and watching it fail.

---

## 7. Prove nothing hides outside Settings (SC-005)

Enumerate every persisted preference key in `SettingsKeys.kt` and any module's own store, then locate
each one in Settings. Any key with no row is either a violation of FR-003 or dead state that should be
deleted. Do this as a checklist once at the QA step; it is not automatable cheaply and it is exactly
the drift this feature exists to stop.

---

## 8. Accessibility and theming (SC-012, SC-013)

- Every settings screen in light and dark, at smallest and largest system text size — no clipped or
  truncated row label.
- TalkBack over each tier: every switch, icon-only control and destructive action announces its
  subject and current state.
- Destructive rows are visually distinct and never the first-focused element in their group.