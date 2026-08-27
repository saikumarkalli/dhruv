# Feature Specification: Settings — application control plane

**Feature Branch**: `004-settings`

**Created**: 2026-08-19

**Status**: Draft

**Input**: User description: "Settings should be like an application control — Account, App details,
and all modules' and submodules' settings live in this place. Account login/logout and everything
account-related here. In future phases, each phase enables its own settings here."

**Source of truth**: `apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md`
§2 (notification channel registry), §1 (route registry), §4 (the settings tree — **superseded in
shape by this spec**, see FR-005). Screen conventions: `platform/DESIGN-SYSTEM.md` §6 (navigation
law N1–N7), §7 (screen-state matrix), §11 (notification and export conventions). Consent
obligations: `platform/DECISIONS.md` ADR-0014 §7, ADR-0029 §5. Feature-flag semantics (enabled,
minimum version, consent-required) are the existing per-app flag mechanism. Known open item
deliberately **not** resolved here: functional spec §8.7 (cross-device consent sync). This document
restates that material as spec-kit's `spec.md` (what/why only) — storage, module topology,
navigation mechanics and component work belong in `plan.md`, written separately.

**What this feature is.** Settings is the application's control plane: the one place where a user
manages their account, sees what the app is, and reaches the settings of every module and submodule
the app contains. It is not a screen with a fixed list of options — it is a **container that modules
plug into**. Three tiers:

| Tier | Holds | Who owns it |
|---|---|---|
| **Account** | identity, sign in, sign out, consent, export, erasure | the shell — one implementation, always present |
| **App** | appearance, security, notification permissions and master switches, language, app details | the shell — always present |
| **Modules** | one entry per module that has settings, each containing that module's rows and its submodules' rows | **the module itself** — contributed, never hardcoded by Settings |

**Cross-phase note.** The tier structure is fixed once, here. Every later phase ships its module's
settings entry **with that module**, and turning the module on turns its settings entry on — no
phase edits a central list, and no phase renegotiates the structure. This is the mechanism the whole
feature exists to provide.

**Build position — Phase 0b (shell foundation), before phase 2.** Settings is shell-owned: no
feature flag of its own, no fault-isolation wrapper, and the app-wide lock it requires (FR-021) is a
shell checkpoint that wraps every phase rather than living inside one. Phase 0b carries the control
plane itself — the top level, the quick rows, the Account and App tiers, the contribution mechanism
modules plug into, the lock checkpoint — plus the migration of today's 19 rows and the three defect
fixes in the shipped surface (no sign-in/sign-out control, app lock that enforces nothing, assistant
consent that forgets on restart). Module entries arrive with their modules.

**0b is delivered in five independently shippable sub-phases**, each ending green on the regression
gate, closing its own scenario rows, and merging separately — one 95-task merge was too large to be
a real checkpoint:

| Sub-phase | Delivers | Stories |
|---|---|---|
| **0b.1** | Control plane + Appearance — tiers, contribution mechanism, module entries for what ships today | US1 |
| **0b.2** | Account & identity — sign in, sign out, consent, erasure | US2 |
| **0b.3** | App lock & privacy — the real lock, auto-lock, hide amounts, notification master | US3 |
| **0b.4** | Module conventions, assistant, app details | US4, US5, US6 |
| **0b.5** | Feature-level verification and the coverage ratchet | — |

0b.1 blocks the rest; 0b.2, 0b.3 and 0b.4 are independent of each other. Appearance sits in 0b.1
rather than with the rest of the App tier because FR-002 requires each quick row to mirror its
owning section's row, so theme and accent must arrive with the top level. App lock's quick row is
the stated exception: it ships in 0b.1 labelled preference-only (FR-043) and becomes enforcing in
0b.3. Task-level detail is in `tasks.md`.

## Clarifications

### Session 2026-08-19

- Q: How should Settings be laid out — one long scrolling page as today, or a short index where each
  section opens its own screen? → A: Hybrid — a fixed set of quick rows inline at the top level,
  every other row nested in its own screen.
- Q: Should the Settings restructure ship as its own unit of work now, or should each future phase
  carry its own slice of it? → A: All ten section screens are built now, each showing a
  not-available state until its feature ships; rows still arrive with their owning phase.
  **SUPERSEDED** by the control-plane redirection below — there is no fixed ten-section list any
  more, so there is nothing to build ten of. What survives: the shell tiers are built in full now
  (Account, App), which was the intent behind the answer.
- Q: When app lock is on, should it gate the entire app, or only the screens that show money and
  account data? → A: Whole app, no exemptions — calculators and converters included.
- Q: What should "Export my data" actually put in the file? → A: Financial records only —
  calculator history is a tool's scratch output, not the user's data, and is excluded.
- Q: When should this Settings work be built relative to phases 2–4? → A: As **Phase 0b**, a second
  shell-foundation slice before phase 2.
- **Redirection (same session)**: Settings is the application control plane — Account, App details,
  and every module's and submodule's settings — with modules contributing their own entries and each
  phase enabling its module's settings as it ships. This replaces the fixed ten-section tree.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - One place that controls the whole app (Priority: P1)

A user opens Settings and finds everything the app lets them decide: who they are signed in as, how
the app looks and locks, what it is, and — listed together — every part of the app that has its own
settings. They never have to remember that one feature hides its options inside its own screen.

**Why this priority**: This is the feature. Every other story is content inside the container this
one builds, and the container is what makes later phases additive instead of each inventing its own
settings surface.

**Independent Test**: Open Settings and confirm the three tiers are present; confirm each module the
app currently ships with settings appears in the modules tier; confirm no module's settings are
reachable *only* from inside that module's own screens.

**Acceptance Scenarios**:

1. **Given** Settings is open, **When** the user reads the top level, **Then** they see the fixed
   quick rows, then Account, then App, then the modules tier listing every module that has settings —
   and no individual controls other than the quick rows.
2. **Given** a module has settings, **When** the user opens its entry, **Then** they see that
   module's own rows, and its submodules' settings grouped under it rather than promoted to the top
   level.
3. **Given** a module is turned off or has not shipped, **When** the modules tier is read, **Then**
   its entry is not listed — the list describes what the user actually has.
4. **Given** a new module ships in a later release, **When** the user opens Settings, **Then** its
   entry appears in the modules tier without any other part of Settings changing.
5. **Given** any of the 19 rows present before this change, **When** the control plane is walked end
   to end, **Then** each is reachable and behaves as it did, at its new home.

---

### User Story 2 - Everything about my account, in one section (Priority: P1)

A user manages their whole identity from Settings: sign in when signed out, see who they are signed
in as, sign out, grant or withdraw each consent, take a copy of their financial records, erase their
data, or erase the account entirely.

**Why this priority**: Legal obligations (revocable consent, erasure) plus two things the app cannot
do at all today — there is no sign-out control, and signing in exists only during first-run
onboarding, so a user who skipped it or signed out has no way back in.

**Independent Test**: From a signed-out state, sign in from Settings and confirm the session starts;
sign out and confirm it ends; then confirm each consent, the export and both erasure actions are
reachable and each names its consequence before executing.

**Acceptance Scenarios**:

1. **Given** no session exists, **When** Account is opened, **Then** a signed-out state with a
   working sign-in action is shown — not a placeholder identity, and not a dead end that forces the
   user through first-run onboarding again.
2. **Given** an active session, **When** Account is opened, **Then** the real signed-in identity is
   shown.
3. **Given** an active session, **When** the user signs out, **Then** the session ends, stored
   credentials are cleared, and on-device calculator data is untouched.
4. **Given** any consent is on, **When** the user turns it off, **Then** it stays off across app
   restarts and every surface depending on it immediately shows its no-consent state rather than
   failing or retrying.
5. **Given** the user chooses to erase data or the account, **When** the confirmation appears,
   **Then** it names exactly what is destroyed and that it cannot be undone; account erasure
   requires a typed confirmation rather than a single tap.
6. **Given** the account has been erased, **When** the app is next opened, **Then** the user is taken
   through first-run setup again with no residue of the previous account.
7. **Given** the user requests an export, **When** it completes, **Then** they receive a file
   containing every financial record they own, the file states its own scope, and the action never
   appears if it cannot produce one.

---

### User Story 3 - Make the app behave the way I want (Priority: P2)

A user sets how the app looks (theme, accent), how it protects itself (app lock, auto-lock timeout,
hide amounts), and whether it may interrupt them at all — the app-wide switches, distinct from any
individual module's own alerts.

**Why this priority**: These apply to every screen regardless of which modules exist, which is why
they belong to the shell tier and not to any module. App lock in particular is a defect today: it
saves a preference and enforces nothing.

**Independent Test**: Change theme and accent and confirm both apply app-wide immediately. Turn on
app lock, background the app past the timeout, return, and confirm the unlock gates every tab
including the calculators; cancel and confirm nothing is readable. Turn on hide amounts and confirm
money masks everywhere while structure stays readable.

**Acceptance Scenarios**:

1. **Given** app lock is on, **When** the app is opened or resumed after the auto-lock timeout,
   **Then** every surface including the calculators is gated until the user authenticates, and a
   failed or cancelled attempt leaves content hidden.
2. **Given** app lock is on and a notification or deep link is followed, **When** the app opens,
   **Then** the unlock happens first and the destination is reached afterwards — never skipped,
   never lost.
3. **Given** the device has no enrolled credential, **When** the user tries to turn app lock on,
   **Then** they are told what to enrol and the switch does not turn on as though it were protecting
   something.
4. **Given** hide amounts is on, **When** any screen, widget or notification shows money, **Then**
   values are masked while counts, percentages and dates stay readable; a user-initiated export is
   exempt and says so at the point of export.
5. **Given** notification permission is denied at system level, **When** the App tier's notification
   area is opened, **Then** a banner states that no alert can be delivered and offers the route to
   the system screen that fixes it, above any per-alert control.
6. **Given** the user turns the app-wide notification master switch off, **When** any module would
   post an alert, **Then** none is posted regardless of that module's own alert settings.

---

### User Story 4 - Tune an individual module (Priority: P2)

A user opens one module's entry and changes how that module behaves: its own preferences, its
submodules' preferences, its alerts, and — for modules that are optional — whether it appears in the
app at all.

**Why this priority**: This is the tier that grows with every phase. Getting its rules right once is
what stops each phase inventing a different settings pattern.

**Independent Test**: Open a shipped module's entry, change one of its preferences, confirm the
module's own screens honour it immediately, and confirm the change survives restart. Turn an optional
module off and confirm it disappears from the app and its entry disappears from the modules tier.

**Acceptance Scenarios**:

1. **Given** a module entry is open, **When** the user changes one of its settings, **Then** that
   module's behaviour changes immediately and the change survives restart.
2. **Given** a module has submodules with their own settings, **When** its entry is open, **Then**
   the submodules' settings are grouped under it and identified by submodule.
3. **Given** an optional module, **When** the user turns it off, **Then** it disappears from the
   app's navigation and content, its entry leaves the modules tier, and its stored settings are kept
   so turning it back on restores them.
4. **Given** a module that is a primary navigation destination, **When** the user looks for a way to
   turn it off, **Then** none is offered — optionality applies to content and tools, not to the
   app's primary structure.
5. **Given** a module that defines alerts, **When** its entry is open, **Then** each alert it defines
   has exactly one control there, and none appears anywhere else in Settings.
6. **Given** a module whose settings require consent the user has not granted, **When** its entry is
   opened, **Then** it states what consent is needed and offers the route to grant it, rather than
   showing controls that cannot take effect.

---

### User Story 5 - Know what this app is (Priority: P3)

A user reads the exact version they run, checks whether a newer one exists, re-reads the intro, and
reaches the privacy policy, licences and source.

**Why this priority**: Support and trust surface, cheap to build, and the version row already exists.

**Independent Test**: Open App details, confirm the version matches the installed build, and confirm
each informational destination opens.

**Acceptance Scenarios**:

1. **Given** App details is open, **When** the version is read, **Then** it matches the installed
   build exactly, including its build number.
2. **Given** an update check runs, **When** it completes, **Then** the user is told they are current,
   that a newer version exists, or that the check failed — never silently "current" on failure.
3. **Given** the privacy policy or licence list is opened, **When** it renders, **Then** it names the
   third-party components actually bundled with the app.

---

### User Story 6 - Control the assistant (Priority: P3)

A user sees whether the assistant may use their financial information, grants or withdraws that
permission at any time, and can supply their own AI key to use their own quota.

**Why this priority**: The assistant is the only route for data leaving the device other than tracker
sync, so its consent must be as revocable as the others. Today that consent is held in memory and
forgotten on every restart, so the user is re-asked forever and the app holds no durable answer.

**Independent Test**: Grant assistant consent, restart, and confirm it is remembered; withdraw it and
confirm the assistant returns to its consent gate before its next request. Enter a personal key,
restart, and confirm it is retained but never shown in full.

**Acceptance Scenarios**:

1. **Given** assistant consent has been granted, **When** the app restarts, **Then** the grant still
   holds and the user is not asked again.
2. **Given** assistant consent is withdrawn, **When** the assistant is opened, **Then** it shows its
   consent gate and makes no request until consent is granted again.
3. **Given** a personal AI key has been saved, **When** its row is viewed, **Then** the key is
   masked, can be replaced or removed in one action, and never appears in full in any export,
   diagnostic or crash report.

---

### Edge Cases

- The user opens Settings while signed out — Account shows sign-in; consent, export and erasure rows
  show their signed-out state rather than acting on a session that is not there.
- Every optional module has been turned off — the modules tier states that and offers the way back,
  never renders as an empty list.
- A module is turned off while one of its screens is open.
- A module's settings entry exists but the module's own data is unavailable (offline, no consent) —
  the entry still opens and states why its controls cannot take effect.
- The user withdraws the consent a module depends on while that module is open — the module falls
  back to its no-consent state without an error dialog.
- Erasure fails midway (offline, server rejection) — the user is told it did not complete and the
  action remains available; the app never claims a success it did not get.
- The user turns app lock on, then removes their device credential — the next launch must not lock
  them out of their own data permanently.
- Notification permission is granted, then revoked from the system while the app is running.
- A stored personal AI key is present but no longer valid — the assistant reports a key failure
  distinctly from a lack of consent.
- A module is removed from a future release entirely — its stored settings must not resurface as an
  orphan entry.
- A row moves between tiers across releases — the user's stored preference survives the move;
  nothing resets to default because its home changed.

## Requirements *(mandatory)*

### Functional Requirements

**The control plane and its contribution mechanism**

- **FR-001**: Settings MUST present a top level containing, in order: a fixed set of quick rows
  operable in place (theme, accent, app lock); the **Account** entry; the **App** entry; and the
  **modules** tier.
- **FR-002**: The quick-row set MUST be exactly those three rows and MUST NOT grow or change by
  phase; changing it is a documented change to this spec. Each quick row MUST also remain present in
  its owning section, showing the same value from the same stored preference — a quick row is a
  shortcut, never a second copy of the setting.
- **FR-003**: Every setting the application offers MUST be reachable from Settings. A module MUST NOT
  keep a persistent preference reachable only from inside its own screens; in-screen controls that
  affect only the current view (a sort order, a chosen period) are not settings and are out of scope.
- **FR-004**: A module's settings entry MUST be **declared by that module**. Settings MUST NOT
  contain a hardcoded list of modules, and adding a module MUST NOT require editing the Settings
  surface — the modules tier is assembled from whatever modules are present and enabled.
- **FR-005**: The surface registry §4's fixed ten-section tree MUST be replaced by this three-tier
  control-plane model in the same change set that implements FR-001, since that document is binding
  and this spec supersedes its shape. Implementing against the unamended registry is a documentation
  conflict, not an acceptable shortcut.
- **FR-006**: A module's entry MUST be listed only when that module is present **and** enabled for
  the running version; a module that is off, unshipped, or version-gated MUST NOT appear.
- **FR-007**: Enabling a module MUST make its settings entry appear with no other change to
  Settings — no central list edited, no other tier touched.
- **FR-008**: Every setting MUST have exactly one owning entry and MUST NOT appear under two; the
  three quick rows are the only permitted duplication and are defined as shortcuts by FR-002.
- **FR-009**: Navigation MUST be one back step from any settings screen to the Settings top level and
  one from the top level to the tab it was opened from; submodule settings MUST be reachable within
  their module's entry rather than as separate top-level destinations.
- **FR-010**: Settings MUST be reachable from the top bar on every primary tab.
- **FR-011**: Each of the 19 rows present before this change MUST be reachable afterwards with
  unchanged behaviour and unchanged stored value; relocation MUST NOT reset a user's preference.

**Account**

- **FR-012**: Account MUST offer sign-in when no session exists, and MUST show the real signed-in
  identity when one does — never a placeholder identity, and never a state that forces the user back
  through first-run onboarding to sign in.
- **FR-013**: Users MUST be able to sign out from Account; signing out MUST clear the session and
  stored credentials and MUST NOT destroy on-device calculator data.
- **FR-014**: Every consent the app collects MUST be independently viewable and revocable from
  Account, MUST persist across restarts, and MUST take effect immediately on withdrawal.
- **FR-015**: Account MUST offer erasure of financial data (leaving the account intact) and erasure of
  the account (data plus account), each stating its exact consequence before executing, with account
  erasure requiring a typed confirmation.
- **FR-016**: A failed erasure MUST be reported as failed and MUST leave the action available for
  retry.
- **FR-017**: After account erasure the app MUST return to first-run setup on next launch with no
  residual account state.
- **FR-018**: Account MUST offer an export producing a machine-readable file of the user's **financial
  records** — every record they own, including those added by later phases. Calculator history is
  excluded as tool output. The row MUST NOT appear until it can produce that file, and the export MUST
  state its own scope at the point of export.

**App — appearance, security, notifications, language**

- **FR-019**: Users MUST be able to choose theme (system, light, dark) and a global accent from a
  fixed swatch set, applying app-wide immediately.
- **FR-020**: Settings MUST remain the only place the app's theme is chosen.
- **FR-021**: When app lock is on, the app MUST gate **all** content behind device authentication on
  launch and on resume after the configured idle timeout — no exempt surface, calculators and
  converters included. A cancelled or failed attempt MUST leave content hidden. The gate MUST be a
  single app-wide checkpoint, not a property each screen declares for itself.
- **FR-022**: App lock MUST NOT be enablable on a device with no enrolled credential; the user MUST be
  told what to enrol instead.
- **FR-023**: A deep link or notification tap arriving while locked MUST be held until unlock and then
  dispatched to its destination — never dropped, never delivered before unlock.
- **FR-024**: The auto-lock idle timeout MUST be user-selectable from a fixed set of durations,
  including an immediate option.
- **FR-025**: When hide-amounts is on, every surface that renders money — screens, widget and
  notifications — MUST mask values while keeping counts, percentages and dates readable; a
  user-initiated export MUST be exempt and MUST say so at the point of export.
- **FR-026**: The App tier MUST hold an app-wide notification master control and the system-permission
  state; turning the master off MUST suppress every module's alerts regardless of that module's own
  settings.
- **FR-027**: When notification permission is denied at system level, the notification area MUST show
  a banner above its controls explaining that no alert can be delivered and offering a route to the
  system settings screen.
- **FR-028**: The legacy calculator history lock MUST remain functional, MUST be labelled as
  superseded by app lock, and MUST NOT be silently changed when app lock is turned on.

**Module settings**

- **FR-029**: A module's entry MUST contain that module's own settings and its submodules' settings,
  grouped and labelled by submodule.
- **FR-030**: Each alert a module defines MUST have exactly one control, and that control MUST live in
  that module's entry — never in the App tier, which holds only the app-wide master and permission
  state. Every alert channel the app defines MUST have such a control, and no control may exist
  without a channel.
- **FR-031**: An alert whose source feature has not shipped MUST be absent from its module's entry
  rather than present and inert.
- **FR-032**: Optional modules MUST offer a control to turn them off; turning one off MUST remove it
  from the app's navigation and content and remove its entry from the modules tier, while retaining
  its stored settings so re-enabling restores them.
- **FR-033**: Primary navigation destinations MUST NOT be user-hideable.
- **FR-034**: A surface whose content has been entirely hidden by a module setting MUST show an empty
  state pointing back to the setting that hid it.
- **FR-035**: A module entry whose controls require a consent the user has not granted MUST state
  which consent is needed and offer the route to grant it, rather than showing inert controls.

**Assistant**

- **FR-036**: Assistant consent MUST be persisted and MUST survive restart; the assistant MUST NOT
  re-ask a user who has already answered.
- **FR-037**: Users MUST be able to view assistant consent status and withdraw it; withdrawal MUST
  return the assistant to its consent gate before its next request.
- **FR-038**: A user-supplied AI key MUST be stored so it is never displayed in full after entry,
  never included in an export, diagnostic or crash report, and removable in one action.

**App details**

- **FR-039**: App details MUST show the installed version name and build number, matching the running
  build.
- **FR-040**: App details MUST offer an update check reporting current, available, or failed — never
  silently reporting current on failure.
- **FR-041**: App details MUST provide the privacy policy, third-party licences for the components
  actually bundled, a source link, and a way to replay the introduction.

**Behaviour common to every settings surface**

- **FR-042**: Every setting change MUST persist immediately without a save action and MUST survive
  restart; a change that cannot be persisted MUST revert visibly and say why.
- **FR-043**: Every row MUST state what it does in plain language; a row that only records a
  preference for a capability that does not exist yet MUST say so rather than implying the capability
  exists.
- **FR-044**: Every settings screen MUST define its applicable loading, empty, error, offline,
  signed-out, not-configured and disabled states, and MUST NOT show an unresolving spinner.
- **FR-045**: Destructive rows MUST be visually distinct and MUST never be the default or
  first-focused action in their group.

### Scope Boundaries

**Phase 0b — the control plane itself**: the top level with its quick rows; the Account tier
complete except export; the App tier complete (appearance, security with the app-wide lock
checkpoint, notification master and permission state, app details); the contribution mechanism the
modules tier is assembled from; the migration of today's 19 rows into their homes; and the module
entries for the modules that already ship (calculators and their submodules, converters, assistant).

**Later phases — module entries, with their modules**: each phase ships its module's settings entry
alongside the module, and enabling the module enables its entry. This includes the tracker modules'
own preferences and alerts, planning and insights module settings, automation, and the export row —
which waits for the financial records it exports to exist (FR-018 forbids shipping it empty).

**Explicitly out of scope**: cross-device consent synchronisation — consent remains per-device, as
recorded in functional spec §8.7, and MUST NOT be resolved incidentally by this work; custom fields
(mentioned in the route map under Settings, never designed); any change to what a consent permits, as
opposed to where it is controlled; per-screen view state (sort orders, chosen periods), which is not
a setting.

### Key Entities

- **Tier**: one of the three fixed divisions — Account, App, Modules. Tiers do not change between
  releases.
- **Settings entry**: a named destination in a tier, holding rows. Shell entries are permanent;
  module entries exist only while their module is present and enabled.
- **Module settings declaration**: a module's own statement of what settings it offers — its entry
  name, its rows, its submodules' rows, and its alerts. Owned by the module, consumed by Settings.
- **Setting row**: one user-changeable value or one action, belonging to exactly one entry, carrying
  a label, plain-language description, current value, and availability state.
- **Preference record**: the persisted value behind a row; survives restart, app upgrade, the row
  moving between entries, and its module being turned off and on again.
- **Consent record**: a per-purpose grant with a scope statement, independently revocable, durable
  across restarts, and device-local.
- **Alert control**: the on/off choice for one notification channel plus that channel's threshold,
  offset or delivery time; lives in the module that defines the channel.
- **App-lock policy**: whether lock is on, the idle timeout before it re-engages, and whether amounts
  are masked.
- **Personal AI key**: a user-supplied credential, write-and-replace only, never readable back in full
  and excluded from every export and diagnostic path.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the 19 setting rows present before the restructure are reachable afterwards
  with their stored value intact — verified row by row against the pre-change inventory.
- **SC-002**: Any single setting is reachable in at most three taps from the Settings entry point
  (module entry, then submodule group at most), and the three quick rows need no navigation at all.
- **SC-003**: A quick row and its owning section row always show the same value — changing either is
  reflected in the other with no restart.
- **SC-004**: Adding a new module to the app adds its settings entry with **zero** changes to any
  Settings-owned file or list — verified by adding a module and diffing.
- **SC-005**: Zero persistent user preferences exist that are reachable only from inside a module's
  own screens — verified by enumerating stored preferences and locating each one in Settings.
- **SC-006**: The count of alert channels the app defines equals the count of alert controls in the
  modules tier, one-to-one — verified as a test, not by inspection.
- **SC-007**: Every consent shown in Settings is still in the state the user left it after an app
  restart, in 100% of restarts.
- **SC-008**: A signed-out user can reach a working sign-in from Settings without passing through
  first-run onboarding, and a signed-in user can complete sign-out or account erasure without leaving
  Settings.
- **SC-009**: With app lock on, content is unreadable before authentication in 100% of launch and
  post-timeout resume attempts — every tab including Calc, and every arrival route including
  notifications and deep links.
- **SC-010**: With hide-amounts on, no money value is legible on any screen, widget or notification;
  counts, dates and percentages remain legible.
- **SC-011**: Zero shipped rows both appear operable and change nothing — every preference-only row
  states that it is preference-only.
- **SC-012**: Every settings screen renders correctly in light and dark themes and at the smallest and
  largest supported text sizes, with no clipped or truncated row label.
- **SC-013**: Every icon-only control, switch and destructive action in Settings carries a spoken
  description naming its subject and current state.
- **SC-014**: A saved personal AI key never appears in full in any screen, log, export or crash
  report — verified by search across those outputs.

## Assumptions

- **Presentation**: resolved by clarification — three quick rows inline above the tiers, each entry
  opening its own screen, submodule settings grouped inside their module's entry rather than promoted.
- **Module entries are hidden, not disabled, when a module is off or unshipped** (FR-006). The modules
  tier answers "what do I have", not "what could this app have"; the app's existing flag mechanism
  already hides disabled features elsewhere, so this matches. The alternative — listing every possible
  module greyed out — was rejected as advertising rather than control.
- **What counts as a module**: anything the app ships as an independently enable-able unit, including
  the calculators and converters that exist today. Submodules are units inside one of those (the
  individual converters, the individual calculators).
- **Export scope**: financial records only. Calculator history is tool output; preferences and consent
  values are device-local configuration rather than data the user contributed. The formatted statement
  export (for reading, not portability) stays a reporting concern.
- **App-lock credential**: the device's own authentication (biometric or device PIN/pattern/password)
  rather than an app-specific secret, so a user who can unlock their phone can always reach their own
  data. This also settles the "user removed their credential" edge case.
- **Retired phase annotations**: the surface registry's tree and channel registry annotate rows with
  `R3`/`R4`/`R5b`/`R6`/`R7`/`R8`/`P4`, pointing at roadmaps deleted on 2026-08-15. They are read as
  "the phase that ships the underlying feature"; mapping each to a design-v1 phase is `plan.md` work.
- **Update check**: reports against published releases; distribution is direct download, so
  "available" never implies an automatic install.
- **Erasure already works**: both erasure actions exist and are correct today; this feature moves and
  labels them and does not redesign what they delete.
- **Counts**: "19 rows" is the current Settings inventory (identity card, 3 appearance, 4 calculator,
  10 privacy-and-data, 1 about). The Phase 0b build position keeps this exact — phases 2–4 add their
  rows after the control plane exists, so SC-001 measures against 19 and not a moving number.

---

## Implementation record

> **Status: NOT YET IMPLEMENTED.** This section is filled in when {phase} ships, and is
> **maintained for the life of the feature** thereafter — see constitution Article Xa
> ("Documentation Tracks Reality"). Everything above this line describes what *will* be built;
> everything below describes what *was*.
>
> Module(s): {module}.

### As built

*(Fill on completion. What actually shipped, per user story. Keep it short — the tasks list the
work, this records the outcome.)*

| Story / FR | Shipped | Notes |
|---|---|---|
| | | |

### Deviations from this spec

*(Anything built differently from what is specified above, and **why**. A deviation recorded here is
a decision; a deviation left unrecorded is drift, and this repo has been burned by it — see
ADR-0030.)*

| Spec says | Built as | Reason |
|---|---|---|
| | | |

### Deferred

*(Scope named in this spec that did **not** ship, with a reason and an owner. Never silently drop
scope — an audit found several screens quietly reduced to a subset with no deferral recorded.)*

| Item | Deferred to | Reason |
|---|---|---|
| | | |

### Change log for this feature

Every later change to shipped behaviour lands a row here **in the same PR that changes the
behaviour** — defect fixes, functional changes, schema migrations, removals.

A defect row names the **FR whose stated behaviour was not actually delivered**. That is what
separates a bug fix from an undocumented behaviour change, and it is how the next reader learns the
spec was once wrong rather than assuming the code is.

| Date | Change | Type | FR affected | PR |
|---|---|---|---|---|
| | | fix / change / removal | | |
