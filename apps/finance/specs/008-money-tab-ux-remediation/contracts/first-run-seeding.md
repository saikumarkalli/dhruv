# Contract: first-run seeding

**Phase 1 output** · [spec.md](../spec.md) FR-238..FR-241 · [research.md](../research.md) R5

What a brand-new person starts with, and why it can never duplicate or resurrect.

---

## The problem being fixed

A new person is given exactly two categories — `Uncategorised` and `Adjustment`, both internal
plumbing — and **zero accounts**. An account is required to save anything. So the first action a new
person takes is guaranteed to fail, with the silent no-op described in
[write-identity.md](write-identity.md) §1 as the failure mode.

## Required behaviour

- A person using the app for the first time can record a transaction **without opening any setup
  screen** (FR-238). Both pickers offer real, usable choices.
- They start with a small set of everyday categories and **one cash account** (FR-239).
- Seeded rows are **ordinary**: renameable, editable, deletable, with no protected or special-cased
  behaviour. The two reserved rows keep their existing separate meaning and are not part of this set.
- Seeding runs **once** and never recreates anything since deleted or renamed (FR-240).
- Seeding never alters anything belonging to a person who already has their own rows (FR-241).

## How "once" is guaranteed

Each seeded row is written with an identity **deterministically derived from the signed-in person and
a stable key for that row**. Both tables involved already enforce identity uniqueness and both
soft-delete, so a deleted row keeps occupying its identity.

| Situation | Result | Requirement |
|---|---|---|
| Seeding runs again | Inserts rejected as duplicates | FR-240 |
| Person deletes a seeded row, reopens the app | The soft-deleted row still holds the identity — nothing returns | FR-240 |
| Two devices sign in at nearly the same moment | One insert wins per row, the other is rejected | Edge case |
| Interrupted part-way | Completed rows skipped, missing rows created | FR-240 |
| Person already has their own rows | Their rows carry their own identities; nothing of theirs is touched | FR-241 |
| Person renames a seeded row | It keeps its identity, so it is never re-seeded alongside the rename | FR-240 |

**No new column, no new table, no device-local flag.** A device-local "already seeded" flag is the
obvious first answer and is wrong: it lives on one device, so a second device seeds again. Recorded
because it is what a reader will reach for.

## When it runs

After sign-in **and** after consent — it writes through the consent-gated client, so it structurally
cannot run earlier. It extends the existing point where the reserved categories are ensured, rather
than adding a parallel mechanism that could drift from it.

Offline or without consent, seeding does not run and does not fail loudly; the person sees the
module's ordinary signed-out or offline state.

## Content

The category names are **product data, not code**: they live in the module's screen-level config as
string resources, because they are user-visible text that must be translated like everything else.

Keep the set small and genuinely everyday. It is a starting point people edit, not a taxonomy — and
adding to the list later does **not** reach anyone already seeded, which is a reason to get the
initial set modest rather than comprehensive.

The account is a single **cash** account. Cash is universal and needs no personal detail, unlike a
bank name — which is why seeding an account is safe here at all.

## Test obligations

| Id | Assertion |
|---|---|
| SEED-1 | A first-time person's category and account pickers are both non-empty |
| SEED-2 | A first-time person saves a transaction without visiting a setup screen |
| SEED-3 | Running seeding twice produces no duplicates |
| SEED-4 | A deleted seeded row is not recreated on the next run |
| SEED-5 | A person with existing rows has nothing seeded and nothing changed |
| SEED-6 | Seeding interrupted after some rows completes the remainder without duplicating |
| SEED-7 | Seeded rows are deletable and renameable exactly like person-created rows |
| SEED-8 | Seeding does not run before consent |

All eight run against the repository fakes. SEED-8 is the Article VIII guard and is worth an explicit
test rather than trusting the interceptor by inspection.
