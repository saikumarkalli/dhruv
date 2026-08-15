# Money tab

Modules reached from the **Money** tab (ADR-0027 — the 5th tab, inserted between Home and Calc;
not part of the original DhruvNext 4-tab shell, added when the finalized design proved out a
dedicated ledger tab).

| Module | Status |
|---|---|
| [money](money/README.md) | not yet created — Phase 3 |

Everything under Money (ledger, quick add, accounts, credit cards, categories, recurring — D1–D9)
lives in the single `money` module rather than being split further, matching how `planning` bundles
budgets/goals/debt for the same reason: these screens share repositories and cross-link constantly
(a transaction can link to a goal in `planning`, a category's spend feeds a budget also in
`planning`), so one module avoids either a forbidden `feature → feature` edge or duplicated logic.
