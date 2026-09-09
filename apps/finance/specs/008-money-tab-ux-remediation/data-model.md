# Data Model: Money tab — usability remediation

**Phase 1 output** · 2026-09-06 · [spec.md](spec.md) · [research.md](research.md)

**This feature adds no entity, no table and no column.** Everything below is an entity
`002-money-tab` already defines, described here only where this feature changes what is *written* to
it or what must hold true about it. The one schema change is a trigger extension (R9).

---

## 1. Entities touched

### Transaction (`finance.transactions`)

No shape change. Three fields that exist and are carried end to end become **writable for the first
time**:

| Field | Today | After |
|---|---|---|
| `occurred_at` | Always the save instant — no setter exists on either view model | Set by the person (FR-229); must not be in the future (FR-230) |
| `receipt_path` | Always null — nothing can attach one | A device-local URI written by `ReceiptStore` (FR-232) |
| `split_group_id` | Always null — nothing constructs one | Shared by the sibling parts of one divided payment (FR-224) |

Two fields become **editable** for the first time, via the edit surface FR-212 adds: `type` and
`to_account_id`. The database already constrains their combination and will continue to:

```
TRANSFER  → to_account_id present, ≠ account_id, category_id null
EXPENSE / → to_account_id null, category_id present
INCOME
```

That constraint (`transactions_transfer_shape`) is what makes FR-228's field swap non-negotiable
rather than cosmetic: a type change that does not swap the fields is rejected by the database, which
is precisely how the current quick-add transfer trap fails today.

**Validation owned by the client** (the database cannot express it):

- `occurred_at` ≤ now at save time (FR-230). A same-day earlier time is valid.
- A split's parts sum exactly to its stated total (FR-225) — see §2.
- A split has at least two parts, or it is not a split (FR-227).

### Transaction history entry (`finance.transaction_events`)

Append-only, written **only** by the `finance.fn_transaction_audit()` trigger — never by a client.
No client-facing UPDATE or DELETE exists, which is what makes FR-008's "history cannot be edited"
structural rather than a convention.

**This is the one thing this feature changes in the schema.** The trigger records amount, payee,
note, `occurred_at`, `cleared`, `account_id`, a dedicated `CATEGORY_CHANGED` kind and a `DELETED`
kind. It does **not** record `type`, `to_account_id` or `receipt_path`.

Consequence if left alone: a person changes an expense to a transfer — the single most consequential
edit this feature introduces — and the history says nothing. If the type was the *only* change, the
trigger takes its "nothing tracked changed" branch and writes no row at all. FR-212's promise would
be false for exactly the edits FR-228 adds.

**Change**: extend the trigger's `EDITED` detail to include `type`, `to_account_id` and receipt
attach/replace/remove. No new event kind, no rename of a shipped constant — Article IX holds.

### Account (`finance.accounts`)

No shape change. Already carries `request_id uuid unique`, which this feature uses for seeding (§3).
Soft-deleted via `deleted_at`, which is what makes seeding non-resurrecting.

Editing (FR-214) writes fields the create path already writes, under the same validation: a blank
name is rejected; the name truncates at 60 characters as typed; the mask keeps only the last 4
digits by construction.

### Category (`finance.categories`)

No shape change. Same `request_id unique` + soft-delete properties as Account, used the same way.

The two **reserved** rows (`Uncategorised`, `Adjustment`) keep their existing, separate meaning and
are unchanged. The starter set this feature seeds sits **alongside** them and is ordinary: fully
renameable, editable and deletable (FR-239). Nothing about the reserved rows becomes a template for
seeded rows or vice versa.

### Recurring definition (`finance.recurring_templates`)

No shape change. Already carries `request_id uuid unique` — so FR-203a needs **no migration**, only a
parameter on `createFromTransaction` (R7).

Editing (FR-215) changes the rule going forward only. Entries it has already produced are ordinary
transactions and are never rewritten (FR-216) — they carry `recurring_id` but no back-reference that
would let an edit cascade.

### Saved view

Already stored and reachable through `SavedViewRepository` (`listSavedViews`, `saveView`,
`deleteSavedView`). No repository change: rename is `saveView` on an existing id. What is missing is
entirely surface (R10).

A saved view holds a filter combination — type, categories, amount range, account — and belongs to
one person. It references categories and accounts by id, so §4's dangling-reference rule applies.

### Split group

**Not a table.** It is the `split_group_id` value shared by the parts of one divided payment. There is
deliberately **no parent row holding a total**, so no aggregate can double-count and every existing
total already sums the parts correctly without knowing splits exist.

This is why FR-225's "the parts must add up" is an **entry-time rule, not a stored constraint** —
there is nothing stored for it to constrain. Once written, the parts are simply transactions.

---

## 2. Split invariants

| Invariant | Where enforced | Why there |
|---|---|---|
| Parts sum exactly to the stated total | Client, at entry (FR-225) | No stored total exists to compare against |
| At least two parts, else not a split | Client, at entry (FR-227) | A one-part split is an ordinary transaction |
| All parts share one `split_group_id` | Client, single batch write | — |
| All parts written, or none | **Database**, via a single array-body insert (R6) | PostgREST cannot wrap two requests in one transaction; one statement is one transaction |
| Each part is independently a full transaction | Existing schema | Aggregates need no split awareness |
| Each part counts once against its account; each category charged only its part | Existing aggregates (SC-211) | Follows from the no-parent-row design |

Money is `Long` paise throughout, so "sums exactly" is exact integer arithmetic with no rounding step
(Article VII). This is the concrete reason paise was chosen over a decimal type.

A three-part split produces **three** `CREATED` history events, one per part — the trigger is
`FOR EACH ROW`. That is correct and visible: FR-226 requires each part to identify itself as part of a
larger payment rather than pretending to be a lone transaction.

---

## 3. Seeding identity

FR-240/FR-241 require seeding that is idempotent, correct across devices, and never resurrects a
deleted row. **No new column expresses this** — an existing constraint already does.

Each seeded row is inserted with a **deterministic `request_id`** derived from the signed-in user's id
and a stable key for that row. `request_id` is `unique` on both `finance.categories` and
`finance.accounts`, and both tables soft-delete.

| Situation | Outcome | Requirement |
|---|---|---|
| Seeding runs twice | Second run's inserts rejected by the unique constraint | FR-240 |
| Person deletes a seeded row, reopens the app | Soft-deleted row still holds the id — nothing is recreated | FR-240 |
| Two devices sign in at nearly the same moment | One insert wins per row; the other is rejected | Edge case |
| Seeding interrupted part-way | Completed rows skipped, missing rows created | FR-240 |
| Person already has their own accounts or categories | Their rows have their own ids; nothing of theirs is touched | FR-241 |

A device-local "already seeded" flag was the obvious first answer and is **wrong**: it lives on one
device, so a second device would seed again. Recorded because it is the answer a reader will reach
for.

Seeding writes through the consent-gated client, so it necessarily runs after sign-in and after
consent (Article VIII) — it extends where `ensureReservedCategories()` already runs rather than
adding a parallel mechanism.

The seeded **content** — the category names — is product data and lives in `MoneyConfig` as string
resources, not inline in a view model (Article V), because it is user-visible text that must be
translated like any other.

---

## 4. Cross-entity rules this feature must not break

- **A category referenced by a saved view is later merged or deleted.** The view must degrade
  honestly rather than silently returning a different result set than the person saved.
- **A split part is edited to become a transfer.** A transfer holds no category, so it cannot remain
  part of a category split; the person is told what happens to the rest of the payment (FR-226,
  spec Edge Cases).
- **A transaction is edited onto a different account.** Both accounts' balances and any running
  balance reflect the move (spec Edge Cases).
- **A transaction's date moves to another month.** Both months' summaries update (FR-231).
- **A transaction carrying a receipt is deleted, then restored by the undo action.** The receipt is
  still resolvable — soft delete does not touch device storage.
- **An account is edited from a spendable type to credit, or back.** "Spendable now" is defined as
  bank + cash + wallet only, so it must recompute (spec Edge Cases, `002` FR-017).

---

## 5. What is deliberately not modelled

- **No receipt storage entity.** A receipt is a device-local URI on the transaction. It is never
  uploaded, so there is no bucket, no key, no server-side row, and no new consent surface (FR-234).
- **No saved-view sharing or ownership beyond the one person.** RLS already scopes every row to
  `auth.uid()`.
- **No split parent.** Stated again here because it is the single most likely thing for a future
  reader to "fix" by adding, which would reintroduce the double-counting the current design makes
  impossible.
