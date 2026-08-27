# Specification Quality Checklist: Settings — application control plane

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-08-19 (re-validated same day after the control-plane redirection)
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain — the five highest-impact questions were answered in
      the 2026-08-19 clarification session, and the session's closing redirection replaced the fixed
      ten-section tree with the three-tier control plane; see Notes
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded — Phase 0b (the control plane) vs later phases (module entries, with
      their modules) vs explicitly out of scope
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

### What changed in the re-plan

Settings is now specified as the **application control plane** with three tiers — Account, App,
Modules — where each module **declares its own settings entry** and the phase that ships a module
switches its entry on. The previous fixed ten-section tree is gone.

Consequences worth carrying into `plan.md`:

1. **FR-004 is the load-bearing requirement.** "Settings MUST NOT contain a hardcoded list of
   modules" is what makes every later phase additive. SC-004 makes it verifiable: add a module, diff,
   expect zero changes to Settings-owned files. Design the contribution mechanism against the
   existing module-boundary rules — a module cannot reach into Settings and Settings cannot import a
   module, so the shell aggregates declarations, the same shape DI aggregation already uses.
2. **The surface registry §4 tree must be rewritten, not amended** (FR-005). The earlier plan only
   needed its "amortised, never a big-bang retrofit" sentence changed; the redirection replaces the
   whole section's shape. That document is binding, so this is the first task, not cleanup.
3. **Alert controls move into modules** (FR-030). The channel registry §2 stays the authority on
   which channels exist, but its 1:1 partner is now "the module that defines the channel", not a
   central notifications section. The App tier keeps only the master switch and permission state.
4. **Two defects sit in the foundation slice**, because they are wrong in a surface users already
   have: sign-in is reachable only through first-run onboarding and there is no sign-out at all
   (FR-012, FR-013), and assistant consent is held in memory so it forgets on restart (FR-036).
5. **The lock checkpoint is architecture, not a screen.** FR-021 (single app-wide gate, no exempt
   surface) and FR-023 (hold-and-dispatch of links arriving while locked) belong in the shell; the
   surface registry §3 already assumes a hold-and-dispatch mechanism for intent extras.
6. **Cross-device consent sync stays closed.** Functional spec §8.7 defers it explicitly and says
   "do not build ad hoc when a future phase touches Settings/A3" — this is that phase. `plan.md` must
   not quietly introduce a synced consent record while restructuring the consent rows.
7. **The 19-row inventory is stable** because Phase 0b runs before phase 2. If that position slips,
   re-take the inventory before planning — SC-001 measures against it.

### Decisions taken with the maintainer (session 2026-08-19)

| Decision | Outcome |
|---|---|
| Layout | Hybrid — three quick rows (theme, accent, app lock) inline, everything else nested |
| Delivery model | Superseded by the redirection: shell tiers built in full now, module entries ship with their modules |
| App-lock scope | Whole app, no exemptions, calculators included; one shell checkpoint |
| Export scope | Financial records only; calculator history excluded as tool output |
| Build position | Phase 0b — shell foundation, before phase 2 |

### Still assumptions

- **Module entries are hidden, not greyed out**, when a module is off or unshipped (FR-006) — the
  tier answers "what do I have", not "what could this app have".
- **What counts as a module/submodule** — an independently enable-able unit, with submodules being
  units inside one (individual converters, individual calculators).
- **App-lock credential** is the device's own authentication, not an app-specific secret.
- **Update check** reports against published releases; direct download, so "available" never means
  automatic install.
- **Retired phase annotations** (`R3`…`R8`, `P4`) in the registry are dangling pointers to roadmaps
  deleted 2026-08-15; read as "the phase that ships the feature", mapped properly in `plan.md`.

## QA catalog coverage — CLOSED 2026-08-19

Constitution principle II ("Scenarios Before Code") requires this feature's QA catalog rows to exist
and be reviewed before any task is generated. They now do:
`2026-08-09-qa-test-scenario-catalog.md` **§13 SET — Settings control plane**, 50 rows, all ☐.

| Group | Rows | Covers |
|---|---|---|
| §13.1 Contribution mechanism | 7 | resolution by type, the add-a-module diff check (SC-004), both ArchUnit rules, `moduleKey` validity, key append-only, per-entry fault isolation |
| §13.2 Registry and row behaviour | 12 | flag/version gating, ordering, preference retention across disable, channel↔control 1:1, immediate persistence, quick-row mirroring, no-orphan-preference audit, notification master, durable assistant consent, AI-key secrecy |
| §13.3 App lock | 13 | every rule in `contracts/app-lock-gate.md` — cold-start lock, timeout semantics, no exempt surface, no flash of unlocked content, held-target dispatch, hide-amounts independence, legacy history lock |
| §13.4 Account | 5 | sign-in without onboarding, sign-out, typed confirmation, honest erasure failure, export row absent until it works |
| §13.5 Structure and presentation | 13 | top-level order, submodule grouping, back steps, the 19-row over-install migration, consent-gated entries, no inert rows, theming, TalkBack |

Nine rows are **reused by id, not restated** (principle II) — `ONB-BR-004/005/006/008/009`,
`ONB-FLOW-005`, `DAT-BR-001`, `NAV-ARCH-002/004`, `NAV-UI-002` — listed in a reuse table at the top
of §13 with the reason each already covers its concern.

**Catalog renumbering**: SET took §13 so the module sections stay contiguous (§1–§13); the coverage
summary moved §13 → §14, and the five inbound references to it (002/003 `tasks.md` and
`quickstart.md`, the design-v1 implementation plan) were updated in the same change.

**SA step also done 2026-08-19** — surface registry §4 rewritten to the control-plane model (FR-005);
§2's channel↔control rule and §1's Automation owner corrected to match, and the same channel↔row
claim fixed in `platform/DESIGN-SYSTEM.md` §11 where it was stated as a global rule.

**Nothing blocks `/speckit-tasks`.**