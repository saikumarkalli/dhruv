# Specification Quality Checklist: Automation (Phase 7)

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-08-23
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

**24/24 pass** after one validation iteration.

**Iteration 1 (2026-08-23)** raised two [NEEDS CLARIFICATION] markers — both genuine scope questions
with no reasonable default. Both were answered in the same session and are recorded in the spec's
Clarifications section:

- **Price-feed behaviour** → a fetched price arrives as a **proposal in the review queue**, not a
  direct write to a holding's value. BR-G1 forbids automated writes only to the *ledger*, so a direct
  valuation write would have been legal — but would have made the hub's own "every source only
  suggests" header false for one row. Consequence, specified rather than discovered later: the queue
  now carries **two kinds of proposal** (proposed transaction, proposed value update). Added US8,
  FR-003a, FR-043–FR-046, SC-013/SC-014, three assumptions and two edge cases; broadened FR-001 and
  SC-001 from "the ledger" to "anything the user's records show".
- **CSV import** → **out of scope**. The onboarding call to action stays present and disabled with
  copy naming that it arrives later. The functional spec's own open item records that the
  column-mapping step has no design at all, so building it here would mean designing a new subsystem
  inside the largest phase in the plan. Recorded as FR-048 and an explicit out-of-scope line, so the
  deferral is visible rather than a silent descope.

Three further ambiguities were resolved as documented assumptions rather than markers, per the
"maximum 3, prioritise by impact" rule:

- **Account-aggregator liveness** — the design itself marks account linking as `COMING SOON`, so G3
  ships as the consent statement and the source row is marked unavailable.
- **Message backfill window** — the design's per-month count implies a recent window, so enabling
  the source reads roughly the last 30 days and continues forward, not the whole inbox.
- **How a standing rule is created** — explicitly from a user's correction, never silently inferred.
  That is what makes BR-G3's "user-visible, counted, revocable" honest.

**One content-quality fix applied**: the front-matter correction originally named Phase 3's
proposal store by its database object name. Reworded to describe the store functionally and point at
`plan.md` for the name — the constitution's Spec-Kit Artifact Mapping requires `spec.md` to stay
technology-free, and Dhruv's earlier specs mixed the two.

---

**`/speckit-clarify` pass, 2026-08-23 (same session).** Five further questions asked and integrated;
the Clarifications section now holds **7** entries. Checklist re-validated against the updated spec:
**24/24 → 24/24**, no item changed state, no regressions. What the pass changed:

| # | Question | Answer | Spec impact |
|---|---|---|---|
| 3 | Fate of an ignored proposal | Remembered, and shown in an **Ignored** list, restorable, never re-proposed | FR-008a/b/c, US1 scenarios 4a/4b, SC-016/SC-017, a fourth surface in scope |
| 4 | SMS consent withdrawn with proposals pending | **Freeze**, don't delete — read-only under a banner, delete-all or re-enable | FR-026a–d, US3 scenarios 7/8, SC-011/SC-011a, new proposal state |
| 5 | When messages are read | **Periodic background scan** (~1h), not per-message, not open-only | FR-027 rewritten + FR-027a–d, SC-003a; **saved US6** — open-only reading would have made the entries-waiting alert unable to announce anything |
| 6 | Accept-all scope | Confidently-parsed **transactions with no duplicate warning** only, with a reason breakdown | FR-009 rewritten + FR-009a/b, SC-006a — closed a literal reading where one tap could double-count and revalue holdings |
| 7 | Price-feed cadence | **On a meaningful move** past a user-set threshold (default 5%), not on a clock | FR-044a–d, FR-045/045a, SC-013a/b, new `Price move threshold` entity and setting |

**Two defects the scan caught and fixed outside the Q&A:**

- The edge-case list said pending proposals on consent withdrawal "must have a stated fate" and then
  never stated it — an unresolved decision wearing the costume of a requirement. Q4 replaced it.
- **Terminology split**: user stories said *suggestion*, requirements and entities said *proposal*.
  Normalised to **proposal** throughout (39 occurrences), with a note in Key Entities mapping it to
  the *suggestion* wording the design and the QA catalog use, so an `AUT-*` row still resolves.