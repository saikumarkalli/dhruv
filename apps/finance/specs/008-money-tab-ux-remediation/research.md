# Research: Money tab — usability remediation

**Phase 0 output** · 2026-09-06 · [spec.md](spec.md)

Every finding below was verified against this repository's code or schema, not assumed. Where a
conclusion changes something the spec asserts, that is called out and the spec is corrected rather
than left to disagree.

---

## R1 — Back handling: how a screen intervenes without forking the back logic

**Question.** FR-208a/b require the phone's back gesture and the on-screen back control to behave
identically, including firing the unsaved-changes confirmation. How, given the app already resolves
back centrally?

**Findings.** `MainActivity` registers one `OnBackPressedCallback` in a `DisposableEffect` and
routes through `resolveBackAction` (`libs/core/.../navigation/BackContract.kt`) —
detail route → active tab's nested stack → first tab → exit. That resolution is correct and is not
the problem. The problem is that `BackAction.POP_NESTED` calls `moneyNavController.popBackStack()`
directly, and **no `BackHandler` exists anywhere in the Money module or `:libs:core`** — verified by
searching the whole tree; the only two in the repo are in the calculator (keypad dismissal) and a
comment in onboarding noting its own absence. `DiscardGuard.attemptDismiss()` is therefore reachable
only from a screen's own close button.

`androidx.activity`'s dispatcher is LIFO among *enabled* callbacks: a `BackHandler` composed inside a
screen registers after `MainActivity`'s and wins while enabled. So a screen can intervene without
touching the central resolver at all.

**Decision.** A screen that has something to say about leaving hosts a `BackHandler(enabled =
isDirty)` that calls the same `DiscardGuard.attemptDismiss()` its close control calls. The central
resolver is untouched. Both triggers reach one code path, satisfying FR-208a by construction rather
than by two implementations kept in step.

**Consequence for the plan.** The visible back control (FR-208) and the guard wiring (FR-208b) must
land together. Shipping the arrow alone fixes the icon and leaves the silent data loss — and makes it
*more* reachable, since a visible control invites use of a screen people currently avoid.

**Alternatives considered.** Extending `resolveBackAction` with a "screen may veto" concept — rejected:
it pushes screen-local state into a pure function that is deliberately stateless and unit-tested as
such. Intercepting in `MainActivity` per route — rejected: the shell would need to know which routes
have dirty forms, exactly the coupling `NavTarget` exists to avoid.

---

## R2 — Date and time entry

**Question.** FR-229 needs a date and a time field. The spec records that `:libs:core` has no
single-date picker (only a planned, unbuilt date *range* sheet).

**Findings.** Confirmed absent. Compose BOM is `2026.08.00`, which carries Material 3's
`DatePicker`/`DatePickerDialog` and `TimePicker` — no new dependency is required. The constitution's
component-reuse principle (Article VI) and DESIGN-SYSTEM §5 forbid a screen hand-rolling this.

**Decision.** Add `NxDatePickerSheet` and `NxTimePickerSheet` to `:libs:core`, wrapping the Material 3
primitives behind the project's own token styling, and consume them from the Money form. Register
them in DESIGN-SYSTEM §5.1 only once they exist — §5's own rule, written after a component library
was documented that had never been built.

**Alternatives considered.** A third-party picker — rejected, no need and a new dependency on an
AGP-9 toolchain that has already rejected three libraries (ADR-0010/0013/0014). Free-text date entry
— rejected as hostile on a phone and a validation problem in every locale.

---

## R3 — Receipt attachment

**Question.** FR-232 needs a photo picker; FR-234 requires the image never leaves the device.

**Findings.** `ReceiptStore` already implements `attach(Uri)`, `exists`, `resolve` and `delete`, and
`TransactionDetailScreen` already renders the image through Coil with the "stays on this device"
disclaimer. Only the picker and the form field are missing. `androidx.activity` is `1.13.0`, so
`ActivityResultContracts.PickVisualMedia` is available — the system photo picker, which requires **no
storage permission at any API level** and returns a scoped URI.

**Decision.** Use `PickVisualMedia`. No new dependency, no new permission, no manifest change. The
absence of a permission request is itself the privacy story and should stay that way.

**Consequence.** FR-234's "no new off-device flow" is satisfied structurally: `ReceiptStore` writes
to app-private storage and `receipt_path` is a local URI. Nothing in the outbound DTO carries image
bytes. A test asserting the receipt path never appears in a Supabase payload is cheap and worth
having, because this is exactly the kind of guarantee that erodes silently.

**Alternatives considered.** Camera capture in-app — rejected: needs `CAMERA`, a file provider, and a
second code path, for a case the system picker already covers (its camera affordance included).

---

## R4 — Accounts and categories reached from Settings

**Question.** FR-209/FR-209a move management into Settings. Can the Settings contract express that,
and does it force screen code into `:libs:settings`?

**Findings.** `SettingsRow` already has a **`Navigate`** variant, documented as "the only escape hatch
to bespoke UI — a module needing a custom control points here at its own screen via the existing
`NavTarget` vocabulary." It renders as a chevron row and is dispatched by
`SettingsRowRenderer`'s `onNavigate`. `NavTarget` already carries `OpenAccount` and `OpenTransaction`
alongside `SelectTab`/`OpenPlanTool`.

Today's Money contribution uses `SettingsRow.Info` for categories — a value row, explicitly
documented as "never interactive". That is the dead count the spec describes.

**Decision.** Change the categories row from `Info` to `Navigate`, add an accounts row the same way,
and add the two `NavTarget` cases the shell resolves to the existing Money screens. No screen code
moves; no module boundary is crossed. This is what the spec's Assumptions section already warned
against misreading, now confirmed against the contract.

**Consequence.** The screens keep living in `:apps:finance:feature:money`. They gain a second entry
point, not a new home — which is also what makes FR-209b's "take the person there directly" from a
Money empty state a plain `NavTarget` dispatch rather than a special case.

---

## R5 — First-run seeding, and how it avoids resurrecting deleted rows

**Question.** FR-238–FR-241 require seeding that is idempotent, survives across devices, and never
recreates something the person deleted. Where does the "already seeded" fact live?

**Findings — the useful one.** `finance.categories`, `finance.accounts`, `finance.transactions` and
`finance.recurring_templates` **all already carry `request_id uuid unique`**. Categories and accounts
are soft-deleted (`deleted_at`), so a deleted row keeps occupying its `request_id`.

That gives idempotency for free. Seed each row with a **deterministic** `request_id` derived from the
signed-in user's id and a stable key for that row. Re-running the seed attempts the same inserts, the
unique constraint rejects them, and the result is:

- run twice → no duplicates (FR-240),
- row deleted then app reopened → the soft-deleted row still holds the id, so nothing is recreated
  (FR-240),
- two devices racing on first sign-in → one wins per row, the other's insert is rejected (edge case),
- interrupted part-way → the completed rows are skipped, the missing ones are created (FR-240).

**Decision.** Deterministic per-user `request_id` per seeded row. **No new column, no new table, no
device-local flag.** A device-local flag was the obvious first answer and is wrong: it lives on one
device, so signing in on a second device would seed a second time.

**Consequence.** Seeding must run **after** sign-in and **after** consent, since it writes through the
consent-gated client (constitution Article VIII). It therefore belongs where
`ensureReservedCategories()` already runs, extending that call rather than adding a parallel
mechanism. The two reserved rows keep their existing separate meaning; the starter set sits alongside
them, deletable where the reserved rows are not.

**Alternatives considered.** A Postgres function invoked on first sign-in — rejected: more moving
parts than the constraint already gives, and it puts product content (the category list) in a
migration where it cannot be translated. A `seeded_at` column on a profile row — rejected: a new
column to express something an existing constraint already expresses.

---

## R6 — Writing a split atomically

**Question.** FR-224/FR-227 record one payment as sibling rows. PostgREST cannot wrap several
requests in one transaction — a fact this repo already discovered when it made the audit trail a
trigger (see R9).

**Findings.** `TransactionRepository`'s own doc states the pattern: "callers write each part as its
own `createTransaction` call with the same `splitGroupId`." There is deliberately **no parent row
holding a total**, so no aggregate can double-count. But N separate inserts means part 3 can fail
after parts 1 and 2 are written, leaving a payment whose parts do not add up — the exact invariant
FR-225 states.

PostgREST does accept an **array body as a single INSERT**, which is one statement and therefore one
transaction. `MoneyApi.createTransaction` currently takes a single DTO.

**Decision.** Add a batch create that posts all parts as one array body, so a split is written
atomically or not at all. Each part carries the shared `split_group_id`; the **request id belongs to
the split as a whole**, not to each part, so a retry of a partially-failed split cannot write parts
twice.

**Consequence.** The audit trigger is `FOR EACH ROW`, so a three-part split produces three `CREATED`
events — one per part. That is correct: each part is independently a transaction, and FR-226 makes
that visible to the person rather than hiding it.

**Alternatives considered.** Sequential inserts with client-side rollback — rejected: the rollback
itself can fail, which is how a half-split becomes permanent. A security-definer function taking the
whole split — viable and stronger, but a heavier change than the array insert for the same guarantee.

---

## R7 — Extending retry identity to the recurring write and pending accept

**Question.** FR-203a closes the residual `002` FR-036 names.

**Findings.** `RecurringRepository.createFromTransaction(...)` and `SuggestionRepository.accept(entry)`
take no request id. But `finance.recurring_templates` **already has `request_id uuid unique`** — so no
migration is needed for the recurring path. `accept()` writes a row into `finance.transactions`
(`source = 'RECURRING'`), which has had the column since `002`.

**Decision.** Add a `requestId` parameter to both, minted once per user action by the calling view
model and reused across retries — the pattern `QuickAddViewModel`/`TransactionFormViewModel` already
implement and unit-test. **No schema change.**

---

## R8 — A retry that collides because the original actually succeeded

**Question.** FR-203 requires this case be reported as *saved*, not failed. `002` FR-036 explicitly
left it unresolved.

**Findings.** A retry carrying a `request_id` that already exists violates the unique constraint;
PostgREST returns **409 with SQLSTATE `23505`**. Today every non-2xx is funnelled into a generic
failure, so the person is told the entry was not saved — when it demonstrably was. This is the one
error message guaranteed to be wrong, and it is reachable by exactly the retry the current silent-save
UI encourages.

**Decision.** Treat `23505` on a `request_id` as **success**: fetch the existing row by its request id
and return it as the saved result. The alternative — `Prefer: resolution=merge-duplicates` — is an
upsert, which would let a retry silently overwrite a row the person had since edited on another
device. Read-back is the conservative reading of "it was already saved".

**Consequence.** This applies to every path that carries a request id, so it belongs in one shared
place in the data layer rather than in each repository. It is testable without a server by faking the
409.

---

## R9 — The audit trigger does not cover every field this feature can now change

**Question.** FR-212 and FR-228 add editing of fields `002` could not change. Does history record
them?

**Findings — this one changes the spec.** `finance.fn_transaction_audit()` fires `AFTER INSERT OR
UPDATE` and records: amount, payee, note, `occurred_at`, `cleared`, `account_id`, a dedicated
`CATEGORY_CHANGED` kind, and a `DELETED` kind. It does **not** track `type`, `to_account_id`,
`receipt_path` (named in its own comment as an untracked example) or `split_group_id`.

So a type change — the thing FR-228 introduces — would leave **no history entry at all**, and if it is
the only change the trigger takes its `v_detail = '{}'` branch and writes nothing. FR-212's promise
that "editing appends a plain-language history entry" would be false for exactly the edits this
feature adds.

**Decision.** Extend the trigger to track `type` and `to_account_id`, as a new declarative object
edit plus a generated migration per ADR-0032. Receipt changes are also worth recording — attaching or
removing a receipt is a meaningful act on a financial record — and are included.

**Consequence — the spec's Assumptions section is wrong and is corrected.** It states "No new stored
entity, no migration." The first half stands. The second does not: this feature needs one migration,
touching one trigger function, adding no table and no column. Recorded rather than quietly shipped,
because an assumption that is wrong in a spec is worse than one that was never made.

---

## R10 — Saved views

**Question.** FR-217/FR-218 need list, save, apply, rename, remove.

**Findings.** `SavedViewRepository` exists with `listSavedViews()`, `saveView(view)` and
`deleteSavedView(id)`. There is no rename operation, but `saveView` on an existing id covers it.

**Decision.** No repository change. The work is entirely the sheet's own save/apply/manage surface and
its view model wiring.

---

## Summary of what this feature actually needs

| Kind | Count | Detail |
|---|---|---|
| New dependencies | **0** | Material 3 pickers and the system photo picker are already available |
| New tables or columns | **0** | `request_id unique` already exists on every table involved |
| Migrations | **1** | Extend the audit trigger to cover `type`, `to_account_id`, receipt changes (R9) |
| New `:libs:core` components | 2 | Date and time picker sheets (R2) |
| New `NavTarget` cases | 2 | Accounts and categories, for the Settings rows (R4) |
| Repository signature changes | 3 | Batch split create (R6); request id on the recurring and accept paths (R7) |

The dominant work is screens and wiring, as the spec claims. The two things that are *not* just
wiring are the audit-trigger migration and the picker components — both discovered here rather than
assumed away.
