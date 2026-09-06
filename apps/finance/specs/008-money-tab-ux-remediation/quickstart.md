# Quickstart: validating the Money tab remediation

**Phase 1 output** · 2026-09-06 · [spec.md](spec.md) · [plan.md](plan.md)

How to prove this feature works. Most of it is device work, because most of what is being fixed is
invisible to a unit test — a button that does nothing looks identical to a button that works until
someone presses it.

---

## Prerequisites

- `JAVA_HOME` pointing at the Android Studio JBR (JDK 17+)
- A physical device or emulator, signed in, with tracker consent granted
- A **second, fresh account** for the first-run scenarios — Scenario 4 cannot be run twice on the
  same account, since seeding is deliberately once-only

## Automated gate

```bash
./gradlew regressionCheck
```

All unit tests + ArchUnit + merged coverage + the floor. Must be green before and after every slice.

Targeted while working:

```bash
./gradlew :apps:finance:feature:money:testDebugUnitTest
./gradlew :apps:finance:app:assembleDebug
```

Database change (one migration, the audit trigger — see [research.md](research.md) R9):

```bash
python scripts/db/gen_schema_docs.py equiv     # schemas/ and migrations/ agree
python scripts/db/gen_schema_docs.py docs --check
```

---

## Device scenarios

Each maps to the spec's own acceptance scenarios. Run them in order — the first four are the P1s and
are what "shippable" means.

### 1 · A save always says something (US1, FR-201..FR-205)

1. Tap add. Enter an amount. Clear the account. **Save.**
   → The missing field is named on screen. The amount is still there.
2. Complete the entry. Turn off connectivity. **Save.**
   → It says the entry was not saved and offers a retry. Every value is still exactly as typed.
3. Restore connectivity. **Retry.**
   → It saves. Check the ledger: **one** row, not two.
4. Open the type selector.
   → Transfer is visible, clearly unavailable, and says transfers are recorded in the full form. It
   cannot be selected. *(Before this feature, selecting it and saving failed forever with no message.)*
5. Save a normal entry.
   → The sheet closes and the row is in the ledger without any manual refresh.

### 2 · Back never eats your work (US3, FR-208..FR-208c)

1. Open accounts, account detail, categories, recurring, and the recurring review queue.
   → Each shows a title naming it and exactly one back control. *(Before: none of the five had either.)*
2. Open the full form. Type an amount. Press the **phone's** back button or gesture.
   → "Discard changes?" appears. It does **not** leave. *(Before: it left silently and the entry was gone.)*
3. Dismiss the confirmation.
   → Still on the form, amount intact.
4. Press back again, confirm discard.
   → Leaves, back on the ledger.
5. Repeat 2–4 on the **account** form. *(Before: it had no confirmation by any route.)*
6. Open a category picker, press back.
   → The picker closes. The form does not.
7. On the Money tab root, look for a back control.
   → There is none.

### 3 · A signed-out person can get in (US2, FR-206, FR-207)

1. Sign out. Open Money.
   → Signed-out state with a sign-in action.
2. Tap it. → The real sign-in flow starts. *(Before: nothing happened — all three of these buttons were empty callbacks.)*
3. Complete sign-in. → The ledger shows data, no restart, no manual refresh.
4. Repeat from the accounts screen and the account detail screen.

### 4 · A new account works immediately (US10, FR-238..FR-241) — **needs a fresh account**

1. Sign in for the very first time. Go straight to Money. Tap add.
   → Both pickers offer real choices. *(Before: two plumbing categories and **zero** accounts.)*
2. Enter an amount and save, **without visiting any setup screen.**
   → It saves. This is the whole scenario.
3. Rename a seeded category. Delete another. Force-stop, reopen.
   → The rename holds. The deleted one does **not** come back.
4. Sign in to the same account on a second device.
   → Nothing is seeded again; no duplicates.

### 5 · Setup lives in Settings (US4, FR-209..FR-211)

1. Settings → Money. → Accounts and categories are both real destinations that open. *(Before: categories was a number that went nowhere.)*
2. Add an account there, return to Money. → It is in the pickers.
3. Money's overflow. → Recurring is there, labelled. Accounts and categories are not.
4. Money's content row. → Search and filter only.
5. Narrowest supported width. → Search shows its full placeholder; nothing is clipped.
6. With no accounts, read the Money empty state. → It says where setup happens and takes you there.

### 6 · Correcting without destroying (US5, FR-212..FR-216, FR-228)

1. Open a transaction. → There is an edit affordance. *(Before: only delete or duplicate.)*
2. Change its category. Save. → History gains a line naming the change. Still one transaction.
3. Change an expense to a transfer. → It asks for a destination and stops asking for a category. It cannot save until complete.
4. Save. → History records the type change. **This is the case that would have gone unrecorded without the trigger migration.**
5. Edit an account's name. → It updates everywhere the account appears.
6. Edit a recurring rule's amount. → Future entries use it; entries it already produced are unchanged.
7. Edit a transaction a recurring rule produced. → The rule itself is unchanged.

### 7 · Honest states (US7, FR-219..FR-223)

1. Disable connectivity, visit every Money screen. → Offline state everywhere, not a generic error. *(Before: the ledger had no offline state at all while the accounts screen it leads to did.)*
2. Recurring review queue. → Dates read as dates, not `2026-09-06`.
3. Account detail. → The destructive action is visually separated from everyday actions and names its consequence.
4. Screen reader on quick add. → The amount is announced with its meaning, not as a bare number.
5. Focus the note field. → The keypad and Save are still reachable.

### 8 · When it happened (US9, FR-229..FR-234)

1. Full form → set the date three days back. Save. → It lands in that day's group, that month, and the month summary includes it.
2. Try a future date. → Refused, with a reason. A same-day earlier time is accepted.
3. Attach a photo. → It shows on the transaction and says it stays on the device.
4. Replace it, then remove it. → Both apply.
5. Edit a transaction's date into another month. → **Both** months' summaries update.

### 9 · Saved views (US6, FR-217, FR-218)

1. Build a filter, save it with a name. → Listed for re-use.
2. Reset, apply it by name. → Identical results to rebuilding by hand. Try three different combinations.
3. Rename it, remove it. → Both persist. No transaction is affected.

### 10 · Splitting a payment (US8, FR-224..FR-227)

1. Full form → divide one payment across two categories.
2. Make the parts not add up. → The shortfall or excess is shown and it cannot be saved.
3. Balance them and save. → Recorded as one payment.
4. Open a part. → It says it is part of a larger payment and offers to show the others.
5. Read the month. → Each category charged only its part; the account charged the full amount **once**.
6. Correct one part's amount. → The balance rule re-checks; history records it on that part.

---

## Closure checks

Not optional — this is the tracking rule the module's own documentation requires, and its absence in
`002` is why six requirements went undelivered while the phase read as shipped.

- [ ] `002-money-tab`'s Implementation record filled in — what shipped, what deviated, what deferred,
      each row naming the requirement whose behaviour was not delivered (FR-235)
- [ ] This spec's own Implementation record filled in
- [ ] `FEATURES.md`, the module `README.md`, and root `CHANGELOG.md` updated
- [ ] The two new shared components entered in DESIGN-SYSTEM §5.1 — **only now that they exist**
- [ ] The design system's known-drift and planned-component sections reconciled against what shipped

## What is deliberately not validated here

- **Scroll and render performance** — deferred to the future app-wide performance spec
  (implementation plan §7a), not measured per-phase.
- **String extraction** — 113 user-visible literals remain, carried forward as a recorded residual.
  New and changed text in this feature follows the rule from birth; the backlog does not close here.
