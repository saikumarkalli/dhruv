# Specification Quality Checklist: Money tab — usability remediation

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-09-06
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

**Re-validated 2026-09-06 after clarification session — 24/24 passing** (was 22/24).

Four clarifications resolved, recorded in spec § Clarifications:

1. **Transfers in quick entry (FR-205)** — shown visibly unavailable with text pointing at the full
   form, rather than removed. Hiding it entirely would have read as "transfers unsupported".
2. **Split across categories (FR-224–FR-227)** — built in this remediation, not deferred. Added
   User Story 8 (P3), 4 requirements, 6 edge cases and SC-211. Verified against the code before
   sizing: the split-group identifier is already defined and carried end to end and the write
   pattern is already documented, so the new work is the entry screen, the balance rule and the
   sibling display — not storage.
3. **Destination placement (FR-209, FR-210)** — accounts, categories and recurring move into a
   Money top bar's overflow with text labels; the content row keeps only search and filter. Also
   resolves the narrow-screen truncation, and gives the tab the top bar navigation law N5 already
   assumes every tab has.
4. **Type change on edit (FR-228)** — any type change is allowed and the form swaps its required
   fields. Added 2 acceptance scenarios and 3 edge cases, including a split part being turned into
   a transfer (a transfer holds no category, so it cannot stay part of a category split).

**A device observation changed a priority.** US3 (missing back control) was written as P2 on the
assumption the absent icon was cosmetic, since the phone's own back gesture still worked. Confirming
it on hardware showed the opposite: the gesture is the *only* route back on those five screens, and
the gesture does not pass through the unsaved-changes confirmation — there is no back-press handler
anywhere in the module or the shared library, so the confirmation is reachable only from a close
button those screens do not have. The one affordance that works is the one that skips the safeguard.
US3 is now **P1**, and FR-208a/b/c cover route parity, back-gesture confirmation, and the account
form (which has no confirmation by any route at all).

**Scope grew three times.** First during clarification (Q2 pulled split in); then a pass folding in
the remaining known gaps rather than leaving them scattered (date and time entry, receipt
attachment, the two write-retry residuals `002` FR-036 names, `002`'s unfilled Implementation
record); then a third pass adding setup consolidation and first-run seeding; then the back-navigation pass
above. Final shape: **10 user stories, 45 requirements, 17 success criteria, 35 edge cases,
26 traced findings** — four of them P1.

**One recorded decision was superseded, deliberately and in place.** Clarification 3 put accounts,
categories and recurring together in the Money top-bar overflow. Clarification 5 splits them by what
they are — configuration (accounts, categories) to Settings, active queue (recurring) stays in
Money. The supersession is written into § Clarifications as a note rather than by editing
clarification 3, so the reasoning trail survives. Anyone reading only the requirements gets the
current answer; anyone reading the clarifications sees why it moved.

**A third P1 appeared, and it is the worst one in the spec.** A brand-new person is seeded two
categories — both internal plumbing — and **zero accounts**. An account is required to save
anything, so the first action a new user takes is guaranteed to fail. That is a total cold-start
block sitting directly behind the app's primary button (US10, FR-238–FR-241).

**Two items in that second pass are worth calling out to whoever plans this:**

- **FR-203 was quietly unsatisfiable as first written.** It promised a retry never double-writes,
  but the case where the original save *succeeded* and its response was lost currently surfaces as
  an ordinary failure — telling the person their entry was not saved when it was. `002` FR-036
  already flagged this as an open follow-up. FR-203 now states the correct message explicitly.
- **FR-229/FR-232 carry a real component dependency.** The shared library has no single-date picker
  (only a planned date *range* sheet) and no photo picker. Both must be added there and consumed,
  not hand-rolled in the Money screens. This is why the date field went undone in `002` in the first
  place, and restating the requirement does not make the dependency go away.

- **"Managed in Settings" is a row, not a relocation.** The Settings surface is assembled from what
  each module contributes. Accounts and categories moving there means Money contributes rows that
  open its own screens — moving screen code into the shared settings library would break the
  module-boundary rules the build enforces, and the Money contribution's own notes already record
  one boundary it had to work around. Stated in § Assumptions because the phrase reads like a
  relocation and is not one.

Split (US8), date/receipt (US9) and seeding (US10) are the three slices that are genuinely new
capability rather than reconnecting a built data layer. US10 is P1 and small; US8 and US9 are larger
and should be planned separately from the P0 fixes.

**Deliberate exclusions**, recorded in § Assumptions rather than asked about because a reasonable
default exists and was applied:

- String-resource extraction (113 literals) — carried forward as a residual, re-stated in `002`'s
  record so it is not mistaken for closed.
- Filter-count recomputation performance — belongs to the app-wide performance spec already
  deferred in the implementation plan §7a.

**Traceability check.** All 21 findings map to at least one requirement, and no requirement exists
without a finding or a clarification behind it — this spec adds no speculative scope. Every finding
was confirmed by reading the shipped screen, view model, navigation wiring or database constraint
involved; none is inferred from documentation.
