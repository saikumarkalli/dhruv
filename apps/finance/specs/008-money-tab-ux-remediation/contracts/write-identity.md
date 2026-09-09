# Contract: write identity, retries, and the split write

**Phase 1 output** · [spec.md](../spec.md) FR-201..FR-205, FR-203a, FR-224..FR-227 ·
[research.md](../research.md) R6, R7, R8

How a save reports itself, and how a retry avoids both duplicating and lying.

---

## 1. A save always produces a visible outcome

Exactly one of three, always, within a second (SC-201):

| Outcome | Person sees | Entry |
|---|---|---|
| Cannot proceed | The specific missing field, named | Preserved |
| Failed | It was not saved, and a retry | Preserved exactly as typed |
| Saved | The entry closes; the ledger shows it without a manual refresh | Committed |

**A save attempt is never a silent no-op.** Today `QuickAddViewModel.save()` returns early on a
missing field with no state change, and its failures call `reportFeatureError` on a `featureError`
flow that **nothing collects** — the shell wires the ledger's, not the sheet's. Both paths currently
end in nothing at all.

## 2. Retry identity

One identity per **user action**, not per network call. It is minted on the first attempt and reused
by every retry of that same attempt until it succeeds.

| Path | Identity today | Required |
|---|---|---|
| Quick add create | Present, tested | unchanged |
| Full form create | Present, tested | unchanged |
| "Make it recurring" write | **Absent** | Added — the table already has `request_id unique`, no migration |
| Accepting a pending entry | **Absent** | Added — it writes a transaction, which already has the column |
| Split | — | **One identity for the whole split**, not one per part |

The split rule matters: per-part identities would let a retry of a partially-failed split rewrite the
parts that already succeeded.

## 3. The collision case — a retry that was already saved

When a retry carries an identity the server has already recorded, the write is rejected as a
duplicate. **This means the original succeeded.**

**Required**: report it as **saved**, resolving to the already-written record. Reporting a failure
here is the one message guaranteed to be wrong, and it is reachable by exactly the retry the current
silent-save behaviour encourages.

**Rejected alternative**: treating the retry as an upsert. That would let a retry silently overwrite a
record the person had since edited on another device. Reading back the existing record is the
conservative reading of "it was already saved".

This applies to every path carrying an identity, so it belongs in one shared place in the data layer
rather than repeated per repository.

## 4. Split writes are all-or-nothing

A split is **one** write containing every part, so the database commits all of them or none.

Rejected: writing parts one at a time with client-side rollback — the rollback can itself fail, which
is how a half-written split becomes permanent. This repository already learned the underlying lesson
when it made the audit trail a database trigger rather than a second client call, for the same reason:
two requests cannot share one transaction.

**Entry rules, before any write:**

- Parts sum exactly to the stated total (exact integer paise, no rounding step)
- At least two parts, or it is not a split
- Every part carries the same split identity

**After the write**: each part is independently a full transaction. Every existing total already sums
them correctly, and each part appears in history in its own right — a three-part split produces three
creation events, which is correct and is surfaced rather than hidden.

## Test obligations

| Id | Assertion |
|---|---|
| WRITE-1 | Save with a missing field sets a named validation message and writes nothing |
| WRITE-2 | A failed save surfaces a retryable failure and preserves every entered value |
| WRITE-3 | A retry reuses the first attempt's identity — on quick add, the full form, the recurring write, and accepting a pending entry |
| WRITE-4 | A retry rejected as a duplicate reports **saved** and resolves to the existing record |
| WRITE-5 | A duplicate-rejected retry never overwrites the existing record |
| WRITE-6 | A split posts one write; a rejected write leaves zero parts |
| WRITE-7 | A split whose parts do not sum to the total cannot be saved |
| WRITE-8 | A one-part split is written as an ordinary transaction, carrying no split identity |
| WRITE-9 | A retried split reuses one identity for the whole split, never one per part |

All nine are view-model or repository tests against fakes. WRITE-4/WRITE-5 fake the duplicate
rejection; no server is required.
