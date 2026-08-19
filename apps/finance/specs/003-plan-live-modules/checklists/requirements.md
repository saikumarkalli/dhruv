# Specification Quality Checklist: Plan Live Modules (Phase 4)

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-08-19
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain — four source-level gaps were resolved as stated
      defaults in Assumptions rather than left as markers (insurance gap-category list, retirement
      target derivation, alert/reminder delivery phase, debts-are-liabilities); see Notes
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

Four gaps in the source material were resolved as documented defaults rather than blocking
clarifications. Each is a stated assumption in `spec.md`, so a later reversal is a scope change with
a named owner, not a silent rediscovery:

1. **Insurance gap categories** — the functional spec (E7) requires a GAPS section "naming uncovered
   risks" but never lists the risk set being checked. Defaulted to term life, health, personal
   accident, critical illness, and home/property. Widening the list later adds rows, not a mechanism.
2. **Retirement target derivation** — E9 states the five assumptions and the outputs (target, % of
   target, shortfall) but not how the target corpus is derived from them. Defaulted to today's
   monthly spend inflated to the retirement age and sustained to life expectancy at the
   post-retirement return, stated on screen alongside the other assumptions per BR-E4.
3. **Alert and reminder delivery is Phase 6** — `PLN-FLOW-003` (catalog §5) chains a budget-overrun
   *notification* into the budget detail, but notifications are screen B2, scheduled for Phase 6.
   This feature stores the threshold (FR-014) and the policy reminder preference (FR-040) and covers
   the flow from the budget detail onward; the notification-initiated leg is explicitly deferred with
   this reason rather than closed. Carry this into `tasks.md` so the QA row is deferred, not skipped.
4. **Debts are liabilities** — E6 assumes a debt set but no debt record is specified. Defaulted to
   reading Phase 2's liabilities, because a parallel debt table would let net worth and the payoff
   plan disagree.

Two further notes for `/speckit-plan`:

- **User Story 7 (Plan root rewrite) ships last within this feature** even though it is the tab's
  entry point — the root summarises modules that must exist first. Its live rows appear
  incrementally as Stories 1–6 land. This is a sequencing choice, not a priority judgement about the
  root's importance.
- **Every derived statement must be labelled** (FR-047, catalog `PLN-BR-005`). This cuts across five
  screens in this feature and is easy to satisfy per-screen and miss globally — worth one shared
  mechanism in `plan.md`, not five local ones.

## QA catalog coverage

This feature's rows already exist and were reviewed on 2026-08-09 (constitution principle II,
"Scenarios Before Code" — step 2 is done for this phase before any task is generated):

| Module | Rows | Catalog section |
|---|---|---|
| `PLN-*` | 13 | §5 — Planning: budgets, goals, debt payoff (E1–E6) |
| `INS-*` | 4 | §6 — Insurance (E7–E8) |
| `RET-*` | 4 | §7 — Retirement (E9) |

`/speckit-tasks` must reference these row IDs rather than restating them.