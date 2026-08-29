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
- **FR-046**: A contributed row's `onChange`/`onInvoke` MUST read and write only that module's own
  preference keys. It MUST NOT read, write or otherwise reach app lock, consent, or the secret-key
  store — those stay shell-owned regardless of which tier renders the control (resolved 2026-08-29,
  security checklist CHK046 — the contract governed row *shape*, not row *behaviour*, leaving
  nothing structural stopping a module's own row from weakening the app's security surface).

**Assistant**

- **FR-036**: Assistant consent MUST be persisted and MUST survive restart; the assistant MUST NOT
  re-ask a user who has already answered.
- **FR-037**: Users MUST be able to view assistant consent status and withdraw it; withdrawal MUST
  return the assistant to its consent gate before its next request.
- **FR-038**: A user-supplied AI key MUST be stored **encrypted** (the existing `secure_settings`
  EncryptedDataStore surface, never a plaintext preference) so it is never displayed in full after
  entry, never included in an export, diagnostic or crash report, and removable in one action. Its
  masked on-screen representation MUST NOT leak the real value's length or any of its characters —
  a fixed-width placeholder token, not a partial reveal (prefix/suffix) or a length-matched mask,
  since either narrows a brute-force or social-engineering search space (resolved 2026-08-29,
  security checklist CHK032/CHK034 — was a plan/data-model-only decision with no FR statement).

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
  data. The "user removed their credential" edge case is settled concretely by
  `contracts/app-lock-gate.md` §1 rule 5 / §2 rule 12 (added 2026-08-29, security checklist `CHK005`):
  the gate falls open to UNLOCKED and resets the preference, with a one-time notice, rather than
  permanently excluding the user from their own Settings.
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

> **Status: ALL FIVE SUB-PHASES IMPLEMENTED (0b.1 2026-08-27; 0b.3/0b.4 2026-08-29; 0b.2/0b.5
> 2026-08-29).** 0b.3 and 0b.4 were implemented ahead of 0b.2 at the maintainer's explicit request
> (both only depend on 0b.1, spec §"Why this is split", tasks.md lines 189/224); 0b.5 was then run
> once 0b.2 closed the last dependency it needed (tasks.md line 273). This section is
> **maintained for the life of the feature** — see constitution Article Xa ("Documentation Tracks
> Reality"). Everything above this line describes what *will* be built; everything below describes
> what *was*, sub-phase by sub-phase.
>
> Module(s): `:libs:settings`, `:libs:core`, `:apps:finance:app`, plus one `settings/` package each
> in `:apps:finance:feature:calculator`, `:currency`, `:unit`, `:assistant`.

### As built — 0b.1 (US1: control plane + Appearance)

| Story / FR | Shipped | Notes |
|---|---|---|
| FR-001, FR-004, contract §1–§4 | Contribution contract (`SettingsContribution`/`SettingsGroup`/`SettingsRow`), `SettingsRegistry`, `SettingsRowRenderer`, `ModuleSettingsScreen` | Registration requires a Koin qualifier per `moduleKey` — an unqualified `single` collides across modules (see Deviations) |
| FR-001, FR-002, FR-009 | `SettingsScreen` top level: quick rows → Account entry → App entry → modules tier, via `SettingsAccount`/`SettingsApp`/`SettingsModule` sub-routes layered on `MainActivity`'s existing detail-route state | — |
| FR-019, FR-020 | Appearance area in `AppSettingsScreen` (theme, accent, disabled wallpaper row); `ThemeWriteSiteTest` proves Settings is the only theme-write site | — |
| FR-006, FR-007, SC-004 | Modules tier assembles calculator/currency/unit contributions with zero Settings-file edits; verified live (T040) with a throwaway fourth contribution | — |
| FR-011, SC-001 | All 19 pre-existing rows reachable at their new homes: calculator rows → `CalculatorSettingsContribution`; theme/accent/wallpaper → Appearance; identity/consent/erasure → `SettingsAccountBody`; history-lock/PIN/about → `AppSettingsScreen`'s temporary Legacy section | Baseline recorded from shipped defaults, not a live device (T002) |
| SET-ARCH-003/004 | Two new ArchUnit rules: no feature-module type in `ui.settings`, no Compose type in a `SettingsContribution` factory | Both proved non-vacuous against real code, not just passing by construction (see Deviations) |

### As built — 0b.3 (US3: App lock & privacy)

| Story / FR | Shipped | Notes |
|---|---|---|
| FR-021, FR-022, FR-023, gate §1–§3 | Real enforcing app lock: pure `appLockState()` decision (`libs/core`) + `AppLockGate` effect (`BiometricPrompt` Class 3 + device-credential fallback) wrapping the entire content tree above the pager, above the detail-route overlay, above every tab including Calc | `MainActivity` migrated `ComponentActivity` → `FragmentActivity` (`BiometricPrompt` requirement) |
| FR-022, gate §1 rule 5, §2 rule 12 | Credential-absent handling: refuses to enable with no enrolled credential; falls open and resets the preference with a one-time notice if a credential later disappears | Added mid-implementation as a security.md-review fix (CHK005) before any code was written — see Deviations |
| FR-024 | Auto-lock timeout (`Immediate`/1 min/5 min/15 min) as a `SegmentedRow`, shown only when app lock is on | Boundary is inclusive (`elapsedSinceBackground >= timeout`) per the CHK011 contract fix |
| FR-023, SET-FLOW-001/002 | Hold-and-dispatch for links/notifications/shortcuts arriving while locked — `HeldTargetStore`, plain in-memory, dispatched once after unlock, cleared for free on process death | — |
| FR-025, SC-010 | Hide-amounts wired at the shared money path: `Paise.MASKED_TOKEN`/`format(masked=…)`, `MoneyText` via a new `LocalHideAmounts` composition local provided at `DhruvTheme` root, `CurrencyFormatter.MASKED_TOKEN` | Screen surface only — widget/notification surfaces don't exist yet (see Deferred) |
| FR-026, FR-027, FR-030 (partial), SET-UI-012 | Notifications area: app-wide master switch, permission-denied banner routing to system settings via `NotificationManagerCompat` | No per-channel rows — none exist yet (contract §3 rule 10) |
| FR-028, SET-BR-020 | Legacy history-lock/PIN rows migrated into Security, unchanged behaviour, relabelled superseded | — |
| SET-BR-008 | App-lock quick row now calls the same credential-checked `AppSettingsViewModel.setAppLockEnabled()` as the Security row, not a direct preference write | Completes the "all three quick rows enforce" requirement first partially shipped in 0b.1 |

### As built — 0b.2 (US2: Account & identity)

| Story / FR | Shipped | Notes |
|---|---|---|
| FR-012, SC-008, SET-FLOW-003 | Real sign-in: `AccountSettingsScreen` wired directly to `AuthRepository.signInWithGoogleIdToken()` — the Credential Manager call is duplicated here (same shape as `SignInScreen.kt`), never shared with `com.dhruv.finance.onboarding` (research R6, `SET-ARCH-003`) | New `androidx.credentials`/`androidx.credentials.play.services.auth`/`googleid` dependencies added to `:apps:finance:app` |
| FR-013, SET-FLOW-004 | Real sign-out: `AccountSettingsViewModel.signOut()` calls only `SessionStore.clear()` — holds no reference to Room/`HistoryRepository` at all, so calculator history survival is structural, not a runtime check | — |
| FR-015, SET-BR-021 | Type-to-confirm on account erasure (`DELETE_MY_ACCOUNT_CONFIRM_TEXT = "DELETE"`) | `ConfirmDangerDialog` (`:libs:core`) extended with an optional `typeToConfirmText` param (Article VI) rather than a bespoke dialog |
| FR-016, SET-BR-022 | Both erasure actions return the repository's `Result` unchanged — no fabricated success, no internal "completed" flag blocking retry | Unchanged from 0b.1's carried-forward code, now behind `AccountSettingsViewModel` |
| FR-017, SET-BR-021/022 | Post-erasure state (no residual account state) | Satisfied by construction — `TrackerAccountRepositoryImpl.deleteMyAccount()` already forced sign-out + `hasCompletedOnboarding` reset before this sub-phase touched anything |
| FR-014, FR-018, SET-BR-023 | Consent switches migrated unchanged; "Export my data" placeholder row removed outright (not conditionally hidden) | `PlaceholderRow` composable deleted (`SettingsRows.kt`) — its only call site |

### As built — 0b.4 (US4: module conventions; US5: App details; US6: assistant)

| Story / FR | Shipped | Notes |
|---|---|---|
| FR-032, SET-BR-005 | Generic module on/off: `SettingsRepository.isModuleEnabled`/`setModuleEnabled` (`module_enabled_<moduleKey>`), a `ModuleEnableRow` shown at the top of every `ModuleSettingsScreen` | See Deviations — the entry stays in the modules tier when off, rather than leaving it (needed to stay reachable) |
| FR-034, SET-UI-011 | Hidden-content empty state: when a module is off, `ModuleSettingsScreen` shows `EmptyStateCard` in place of its groups, with the on/off toggle itself staying visible directly above as the way back | — |
| FR-035, SET-UI-008 | Consent-gated module entries: `SettingsContribution` gained `consentGranted: Flow<Boolean>` + `consentRequiredMessage: Int?` (additive); `ModuleSettingsScreen` shows `ConsentNeededRow` instead of the module's rows when `resolver.requiresConsent(moduleKey) && !granted` | `resolver.requiresConsent` only answers *whether* a gate applies — the *current grant state* has no generic cross-module source, so each gated contribution states its own |
| FR-030, FR-031, SET-BR-006 | First real alert control: `daily_rates` — `NotificationChannelRegistry` (`:libs:core`), currency's `alert_daily_rates` `Toggle` + delivery-time `Choice`, `alertChannelCoverageIsOneToOne` proving the 1:1 mapping | Metals-rates deliberately absent (FR-031) — designed, not built (currency-metals-notification plan) |
| FR-033, SET-UI-010 | `PrimaryDestinationTest` — no registered `moduleKey` collides with a `TabKey` name | Guard test, not new runtime logic — nothing today needs a runtime check since the namespaces don't overlap |
| Article VI (T092) | `LabeledSettingsGroup` extracted into `SettingsRowRenderer.kt`, adopted by `ModuleSettingsScreen` and `AppSettingsScreen`'s three sections | `SettingsAccountBody.kt` left unchanged — out of scope for this pass |
| FR-036, SET-BR-011 | Durable assistant consent: `AssistantViewModel` reads `assistantConsentGranted` from `SettingsRepository` at construction, plus a reactive observer for an already-open instance | Bug found and fixed during implementation — see Deviations |
| FR-037, SET-FLOW (DAT-BR-001 reuse) | `withdrawConsent()` returns to `ConsentNeeded`; `AssistantScreen` already collects `uiState` reactively, no change needed there | — |
| FR-038, SET-BR-012 | `assistantSettingsContribution` (consent `Toggle` + Gemini key `SecretText`); new `SettingsRow.SecretText` row type; masked representation is a fixed constant string, never computed from the real value | See Deviations — save-then-persist entry, not persist-immediately |
| FR-039, FR-040, FR-041, FR-043 | `AppDetailsSection` replaces the temporary Legacy section: version+build, privacy policy/licences/source (real GitHub links), pluggable `AppDetailsViewModel`/`UpdateChecker` | Update check and replay-intro are absent, not inert — no update channel or on-demand onboarding-replay route exists yet |
| FR-046 (new FR, security checklist CHK046) | `DependencyRulesTest.a SettingsContribution package must not reach shell-owned security surfaces directly` — non-vacuous ArchUnit rule | Written as part of 0b.4's pre-code security.md re-review, not deferred |

### As built — 0b.5 (feature-level verification & ratchet — no new behaviour)

| Task | Finding / outcome | Notes |
|---|---|---|
| T110, SC-005 orphan-preference audit | 9 dead preference keys found and deleted (5 per-section accent colors, 4 per-tab enable flags — both retired by ADR-0024, zero consumers anywhere, verified by full-repo grep); 1 real gap found and tracked, not fixed (`font_family` — consumed but no row); `sync_enabled` already self-documented | `SettingsRepository`/`SettingsRepositoryImpl`/`SettingsKeys` shrunk; `SettingsKeyPreservationTest` updated to reflect the intentional removal, not a silent drop |
| T111, SC-002 tap-depth | Quick rows: 0 navigation taps. Every other setting: 2 navigation taps (well under the 3-tap ceiling) | Confirmed by inspection, no device |
| T112, SC-011 inert-row review | No row found that appears operable while changing nothing | `quickstart.md` §7 turned out to already be the orphan-key audit (T110's), not an inert-row review — new §7a written |
| T115, catalog closure audit | Found and closed `SET-BR-010` — its logic was proven correct since 0b.3 (T063) but never marked closed in the catalog | Exactly the class of drift this task exists to catch |
| T116/T117, coverage | Merged 14.91% (`:libs:settings` 38.38%, `:libs:core` 15.02%), up from 13.2% at the 0b.1 baseline; `globalLineFloor` raised 0.09 → 0.14 | Per-module numbers computed from the aggregated JaCoCo XML (no separate per-module report exists) |
| T109/T113/T114 | Not performed — no physical device or emulator available in this implementation session | Closed **deferred** in catalog §13/§14, not silently skipped |

### Post-implementation review pass (2026-08-29, SA + QA, whole feature 0b.1–0b.5)

A five-axis review of the merged change set. Findings and their fixes, most severe first — all
landed in the same pass, `regressionCheck` green after:

| Severity | Finding | Fix |
|---|---|---|
| **Critical** | `ModuleSettingsScreen` invoked `collectAsState` — a `@Composable` — *inside* a `when` branch condition, behind a short-circuiting `&&`. Its invocation count varied between recompositions (module toggled off, or a module that doesn't require consent), which corrupts the slot table. `ModuleConsentGateTest` never combined module-disabled with consent-gated, so nothing caught it | Both reads hoisted above the `when`; added `a disabled module that also needs consent…` driving the off→on transition that used to change the call count |
| **Critical** | `AppDetailsViewModel` extends `ViewModel` but was built with `remember {}` — never entered a `ViewModelStore`, so `onCleared()` never ran and `viewModelScope` was never cancelled. Dormant only because the shipped `updateChecker` is `null`; wiring a real one made it a live coroutine leak | Registered in `appModule`, resolved via `koinViewModel()` like every sibling; `PackageInfo` read moved into the Koin definition so the ViewModel stays Context-free |
| Required | `generateRawNonce()`/`sha256Hex()` — a security primitive on the auth path — duplicated verbatim between `SignInScreen` (onboarding) and `AccountSettingsScreen`, with no test on either copy. `SET-ARCH-003` forbids Settings referencing a *feature-module type*; it does not license duplicating pure crypto helpers both modules could share | Extracted to `com.dhruv.core.security.SignInNonce` (`:libs:core`, which both already depend on); both call sites consume it; new `SignInNonceTest` pins the raw-vs-hashed distinction, length, hex-ness and non-reuse — the swap failure is otherwise silent (picker still shows, GoTrue rejects afterwards) |
| Required | `trackerAccountRepository`/`sessionStore` threaded through four signatures (`MainActivity` → `AppShell` → `TabsScaffold` → `DetailRouteContent` → `SettingsDetailContent`) and used nowhere after 0b.2 replaced `SettingsAccountBody`. Originally left in place with a comment rationalising it as "avoiding churn" — that is how dead threading survives | Removed from all four signatures, their call sites, the two now-unused `koinInject()`s and imports |
| Required | Hardcoded user-visible strings in **new** code, against design system §10 / FR-043 ("every string a resource from birth"): the entire app-lock surface (`AppLockGate`'s prompt title, retry copy, action), the credential-loss Toast, `DeleteMyAccountDialog`'s rewritten copy, and the relocated `Lock history`/`Change PIN` rows | 11 new string resources; all call sites converted. The locked screen in particular had no translatable string at all |
| Required | Gate §2 rule 12's "one-time" notice fired on *every* resolve until the async preference write landed — a quick background/foreground cycle re-showed it. Separately, `resolveLockState` was a query that wrote a preference and showed a Toast | Split into a pure `resolveLockState` and an effectful `applyLockState`; the notice is latched on `credentialLossNotified` rather than on the not-yet-persisted flag |
| Required | Four tests asserted implementation details or passed vacuously: two reflected over member/constructor **names** (`…never depends on onboarding module types`, `…no export-shaped property`); `PrimaryDestinationTest` used a hand-typed key set a fifth module would never join; `AlertControlCoverageTest`'s two "real" cases built **fabricated** contributions, so dropping currency's real `alert_daily_rates` toggle would not have failed them | The two reflection tests deleted, each replaced by a comment naming the stronger guard that already covers it (`DependencyRulesTest`) or why the assertion isn't yet possible (`SET-BR-023`). New `realSettingsContributions()` helper builds contributions from the actual feature-module factories; `ContributionValidityTest`, `AlertControlCoverageTest` and `PrimaryDestinationTest` all consume it, with the residual hand-maintenance stated in its doc |
| Required | `ConfirmDangerDialog`'s new `typeToConfirmText` — the guard on the single most destructive action in the app — shipped untested; `SET-BR-021` closed on "verified by reading the composable" | New `DeleteMyAccountDialogTest` (3 cases) against the real shipped dialog: tap-alone never erases, near-miss stays disabled, exact word enables and fires once. `SET-BR-021`'s catalog citation updated to the test |

Also found, deliberately **not** changed: `CurrencyFormatter.format` is still called directly by the
four calculator screens, bypassing hide-amounts — that is the documented 0b.3 scope decision
(calculator output is not personal financial data), re-confirmed here rather than silently widened.
No settings file exceeds 392 lines, so no decomposition was warranted.

One process note worth keeping: the first version of the new `ModuleConsentGateTest` case drove
state through the repository's `suspend` setter with `runBlocking`, which deadlocks against
`waitForIdle` on Robolectric's single main thread — the test still passed, but took **25 minutes**.
`FakeSettingsRepository.moduleEnabledMap` is now public so Compose tests write it synchronously; the
same case runs in ~16s. A test that passes slowly enough is a broken gate.

### Edge-case & scenario gap pass (2026-08-29, whole feature)

Walked this spec's own **Edge Cases** list and each FR against the shipped code, asking "is this
actually reachable and does it actually do what the FR says" rather than "does a test pass". Four
requirements were **stated as delivered but were not**:

| Severity | Gap | Resolution |
|---|---|---|
| **Critical** | **FR-032 had no consumer.** `isModuleEnabled` was read in exactly one place — `ModuleSettingsScreen`, for its own empty state. Turning a module off did **not** remove it from navigation or content, so the toggle wrote a preference nothing acted on: precisely SC-011's "a row that appears operable and changes nothing". The 0b.5 inert-row review (T112) missed it by asking "does this row write?" rather than "does the write have a reader" | Gated the three optional modules' routes through `FeatureHost` — a turned-off module now renders `FeatureDisabledCard`, the same treatment a flag-disabled one already gets (PLATFORM.md §4), rather than a second bespoke "off" state |
| **Critical** | **FR-033 hazard: the on/off control was offered for every contribution**, including `calculator` — which *is* the Calc tab's content. `PrimaryDestinationTest` passed throughout because `"calculator"` collides with no `TabKey` **name**; the name check is a weaker proxy than it appeared. Once FR-032 gained a real consumer, this would have let the user hide a primary destination | `SettingsContribution.optional` (defaults **false** — a module is hideable only by declaring itself so); `ModuleSettingsScreen` offers the control only for optional modules. Two tests now pin it: `calculator` must not be optional, and `currency`/`unit`/`assistant` must be |
| Required | **Notification permission state was computed once and never refreshed** (`remember {}`, no key). The denied banner's own CTA sends the user to system settings to change exactly that value — so on the single journey the banner exists to support, it showed stale state on return. Named verbatim in this spec's Edge Cases | Re-read on every `ON_RESUME` via a lifecycle observer |
| Required | **Erasure rows acted while signed out.** Both `DangerRow`s rendered unconditionally, outside the `when (sessionState)` block, so a signed-out user could invoke `auth.uid()`-scoped RPCs and get a failure Toast that reads like a server fault. Edge Cases requires they "show their signed-out state rather than acting on a session that is not there" | Disabled and relabelled while signed out (`settings_erasure_signed_out`) |
| Required | **The personal AI key row promised behaviour the app does not have.** `GeminiRepository` is a singleton constructed with `BuildConfig.GEMINI_API_KEY`; a user-saved key is stored encrypted, masked and removable — and never read. The row said it "bypasses the shared quota" | Copy corrected to state what it actually does today (stored, not yet used) under FR-043's preference-only rule — the same pattern app lock shipped under in 0b.1 before 0b.3 made it enforcing. Consumption is ADR-0002's BYO-override and belongs to the AI feature, not this control plane; recorded in Deferred |

Confirmed handled, checked rather than assumed: consent withdrawn while a module entry is open
(now reactive on both sides); a module removed from a future release (the registry only enumerates
Koin-resolved contributions, so a stale `module_enabled_*` key has nothing to attach to); erasure
failing midway; app lock with the device credential removed; a row moving between tiers
(`SettingsKeyPreservationTest`); every optional module turned off (the tier keeps the entry, which
is what makes the "way back" reachable — the FR-032 deviation below is load-bearing for it).

One self-inflicted find worth recording: the first cut of the FR-032 route gate put
`rememberModuleEnabled(...)` behind a short-circuiting `&&`, reintroducing the exact conditional-
`@Composable` defect the review pass above had just fixed in `ModuleSettingsScreen`. Caught before
the gate ran; both call sites now hoist the read.

### Deviations from this spec

| Spec says | Built as | Reason |
|---|---|---|
| Contract §1: "modules register their `SettingsContribution` as a Koin definition bound to that type" | `single(qualifier = named(moduleKey)) { ... }` — qualifier required | Verified during implementation (T010): an unqualified `single` for the same concrete type collides across Koin modules — the second registration silently overwrote the first, and `getAll<SettingsContribution>()` returned only one. Contract §1 and research.md R1 updated in place with the finding |
| T033/T035: calculator ships "history preview/export/clear"; currency/unit ship contributions with unspecified content | Calculator ships format/precision/preview/angle-mode/clear-history only (matches data-model.md §2's actual pre-existing rows — no history-preview/export row ever existed). Currency/unit ship one real static `Info` row each (supported-currency count; per-category unit count from the same enums the converters use) rather than an invented toggle | Neither module has a persisted preference today; inventing one would violate SC-011 (no row that appears operable and changes nothing) |
| T029: "AppSettingsScreen.kt with its Appearance area only" | Appearance area plus a temporary "Legacy" section (history-lock/PIN, about-version) | Without it, those pre-existing rows would be unreachable between 0b.1 and 0b.3/0b.4 (which build their real Security/App-details areas), regressing FR-011/SC-001 mid-phase. Removed when 0b.3/0b.4 land their real areas |
| T021/T022 file paths (implied Compose/Robolectric infra, never used before in this repo) | Confirmed working; two real bugs found and fixed along the way: `SwitchRow`'s outer `Row` isn't itself clickable (tests must target `isToggleable()`, not the label text), and two `fillMaxSize()` screens stacked in an unweighted `Column` push the second one outside the visible bounds | First Compose UI tests in this codebase — recorded so the next one doesn't rediscover these |
| (pre-existing latent gap, not this spec's scope, fixed as a blocker) | `DependencyRulesTest`'s `ImportOption.DoNotIncludeTests()` never matched this project's actual `debugUnitTest` output path — every ArchUnit rule had been silently importing test classes all along. Replaced with a working exclusion predicate | Surfaced only because `SET-ARCH-003`'s new rule happened to be the first one a test class actually violated; left broken would have made the new rule non-functional |
| `checklists/security.md` — read-only reviewer artifact, not an implementation input | Reviewed against spec.md/contracts/data-model.md before 0b.3 code was written, at the maintainer's explicit direction; 35/46 items flipped `[x]`, 11 left `[ ]` with findings (non-blocking) | 4 of the findings (CHK005, CHK007, CHK011, CHK012) would have produced a wrong or ambiguous app-lock gate if coded as originally specified — the maintainer chose to fix `contracts/app-lock-gate.md`/spec.md first rather than implement around them |
| gate contract: original rule numbering (§1–§4, sequential but not renumbered for insertions) | Fully renumbered 1–20 across §1–§4 after inserting rule 5 (§1, credential-absent) and rule 12 (§2, credential re-check) | An earlier in-place insertion produced a duplicate "rule 6" across two sections; caught before merge and fixed by rewriting the whole file with consistent numbering |
| (found during implementation, not stated anywhere) | `SettingsRepository` gained `currentSnapshot(): AppSettings` — a synchronous read used as both settings ViewModels' `stateIn` initial value | Without it, the lock-state `StateFlow`'s async default (`AppSettings()`, `biometricEnabled=false`) would show a few frames of unlocked content on cold start before the real DataStore value arrived — a defect this spec's own SC-009/gate rule 8 exists to prevent. Found by reasoning about the existing repository architecture, not by a failing test |
| FR-032: "turning one off MUST remove it... and remove its entry from the modules tier" | The entry stays visible in the modules tier when off — its content collapses to the empty state (FR-034) instead | The spec edge case "every optional module turned off — the tier states that and offers the way back, never renders as an empty list" requires *some* reachable path to re-enable a module; removing the entry from the tier entirely would make it unreachable. Keeping the entry (showing its off-state) is the "way back," and is a stronger reading of the edge case than the literal FR-032 sentence — recorded as a deliberate interpretation, not an oversight |
| `checklists/security.md` — read-only reviewer artifact | Re-reviewed against 0b.4's actual scope before its code was written (same pattern as 0b.3): 4 more items resolved (CHK032, CHK034, CHK046 fully; CHK018 judged not directly blocking 0b.4's scope and left as-is) | CHK032/034 (AI key storage/masking) and CHK046 (module security-boundary) are exactly what 0b.4 builds (T097, T106) — same "fix the requirement before coding the thing it governs" pattern as 0b.3's CHK005/007/011/012 |
| tasks.md T093's stated path (`apps/finance/data/.../AssistantConsentTest.kt`) | Written at `apps/finance/feature/shell/assistant/src/test/.../AssistantConsentTest.kt` | `AssistantViewModel` — the class actually holding the FR-036 defect — lives in `:apps:finance:feature:assistant`, not `:apps:finance:data`; the test is written where the behaviour is |
| tasks.md T103's stated file (`SettingsScreen.kt`) | The "About Dhruv Finance" row was actually in `AppSettingsScreen.kt`'s temporary Legacy section | Same path-naming looseness already noted for T093; `SettingsScreen.kt` never held this row |
| FR-042: "no save action anywhere in Settings" | `SettingsRow.SecretText`'s unset-state entry is save-then-persist (an explicit "Save" button), not persist-on-every-keystroke | Persisting an encrypted secret character-by-character on every keystroke is a worse property than one explicit confirm — judged the one row type this rule shouldn't apply to literally. `SettingsRow.SecretText`'s own doc comment states this |
| (found during implementation, not stated anywhere) | `AssistantViewModel` gained a reactive `settingsRepository.observe()` collector (in `init`, `.drop(1)` to skip its own replay) alongside the construction-time read | Without it, an assistant screen instance already alive when consent is withdrawn elsewhere in Settings would keep showing `Idle` — FR-037's guarantee only held for a freshly-constructed instance. Found the hard way: the fix's first version raced against `grantConsent()`'s own in-flight persist write and broke a pre-existing test (`blank prompt after consent stays Idle`) before `.drop(1)` fixed it |
| (found during implementation, not stated anywhere) | `ModuleToggleTest`'s Robolectric tests each use a unique `moduleKey` per test method | The `app_settings` `preferencesDataStore` file is not reset between test methods in the same Robolectric test class (keyed by file path, not by `Context` instance) — a shared moduleKey across tests in that file caused order-dependent failures. First real instance of this in the codebase (no prior test constructed `SettingsRepositoryImpl` directly) |

### Deferred

> **Cross-phase items are also registered outside this spec.** Four of the rows below are consumers
> this control plane cannot own — a shipped phase's spec is the last place the engineer who needs
> them will look. They are enumerated with recommended owners in
> `apps/finance/docs/superpowers/specs/2026-08-23-phase-readiness-architecture-decisions.md` §5.5,
> which the design-v1 implementation plan already points at for cross-cutting unowned work:
> the `daily_rates` delivery pipeline (recommended: Phase 6), BYO AI-key consumption (the Ask Dhruv
> work, per ADR-0024 decision 4), hiding launcher entries for turned-off modules, and `font_family`'s
> missing row (a DESIGN-SYSTEM decision first). Rows below that are purely 004's own — the device
> passes, the export row, the `toString()` risk — stay here only.

| Item | Deferred to | Reason |
|---|---|---|
| Account tier's real sign-in/sign-out/consent/erasure screen (`AccountSettingsScreen.kt`) | 0b.2 (T046) | `SettingsAccountBody` is 0b.1's zero-regression stand-in behind the same route; 0b.2 replaces it |
| A contribution with zero groups/rows has no defined empty state in `ModuleSettingsScreen` | Whichever sub-phase first ships one | None of 0b.1's three real contributions is empty; recorded in quickstart.md §9 |
| `SelectionSheet` component for `Choice` rows with >3 options | Design system batch B9 (planned, not built) | None of 0b.1's `Choice` rows exceed 3 options; renderer falls back to a plain `ListGroupRow` list in the meantime |
| T077 manual device pass (app lock table, quickstart.md §4) — cold start, cancel, timeout, Calc tab, notification-while-locked, held-target scenarios, credential removed/absent | Next session with a physical device or emulator available | No device was attached to the 0b.3 implementation environment; the decision logic is unit-tested (`AppLockDecisionTest`) but the actual on-screen no-unlocked-frame guarantee needs a real device pass |
| Hide-amounts on the widget and notification surfaces (FR-025/SC-010's other two-thirds) | Whichever phase first builds the widget (Glance, not yet implemented) or posts a real notification | `CurrencyFormatter.MASKED_TOKEN` exists as the capability; nothing exists yet to wire it into. Recorded in quickstart.md §10, not silently dropped |
| Per-channel notification rows / notification-channel registry parity check for the other 7 named channels (`app_updates`, `recurring_review`, `budget_alerts`, `emi_reminders`, `renewal_reminders`, `stale_valuations`, `monthly_digest`) | Phase 6 (006-search-notifications) delivers five of them; the rest follow their owning module | `daily_rates` (currency, T090) is the only one with a shipped owning module today; `SET-BR-006`'s 1:1 rule already holds for it. Readiness register §3.3 already flags the inverse problem — four of Phase 6's five channels are owned by modules planning no settings entry |
| The real currency/metals daily-rates delivery pipeline (WorkManager job fetching rates and posting the notification) | **Unowned — recommended Phase 6.** Registered in readiness §5.5 item 1 | T090 shipped the real Android notification channel + Settings control only, establishing the convention — nothing posts to the channel yet. Phase 6 explicitly disclaims this channel ("`daily_rates`/`app_updates` stay with the currency and app-details modules") and the currency module plans no such work, so it has no owner today. Phase 6 builds the app's only `androidx.work` scheduling, so a second scheduler for one channel would be waste |
| Update-check `UpdateChecker` real implementation | Whichever phase connects a real update source | No update channel exists — distribution is a signed APK via GitHub Releases (ADR-0008); `AppDetailsViewModel` is fully built and tested against the pluggable interface, ready to receive one |
| On-demand "replay intro" route in the shell's `DetailRoute`/`NavTarget` system | Whichever phase adds it | No such route exists today; App details' "Replay intro" row is absent rather than wired to nothing (FR-043) |
| `ConsentNeededRow`'s single-row shape (no full `not-configured`/`disabled` screen-state variant) | Whichever phase first gates several groups behind different consents in one entry | Only `assistant` gates its own single consent today; recorded in quickstart.md §9 |
| `AppSettings.toString()` could leak `geminiApiKey` in plaintext if any future code logs the whole object | Whichever change first needs to log `AppSettings` | No call site does this today (grepped, T106's Sec pass); recorded as a residual risk, not fixed, since fixing a plain `data class`'s `toString()` pre-emptively for a call site that doesn't exist would be speculative |
| **Consuming** the user's personal AI key (ADR-0002's BYO-key override) | **Unowned — the Ask Dhruv work** (ADR-0024 decision 4 says this plumbing lands alongside it; no phase in the design-v1 plan §7 table owns Ask Dhruv). Registered in readiness §5.5 item 2 | `GeminiRepository` is a Koin singleton built once with `BuildConfig.GEMINI_API_KEY`, and `currentSnapshot()` deliberately skips the encrypted read — so honouring a user key means a key-provider indirection in `:apps:finance:data`, not a settings change. FR-038 requires the key be *stored* safely (done); it does not require it be used. The row now says so (FR-043) instead of promising otherwise |
| Hiding the **launcher entry points** (the Calc-tab converter tiles, the Ask pill) for a turned-off optional module | **Unowned — whichever phase next revisits those surfaces.** Registered in readiness §5.5 item 3 | The route gate added in the gap pass makes a turned-off module render `FeatureDisabledCard` — content genuinely removed, matching how flag-disabled modules already behave. The tile still being present is the same behaviour a flag-disabled module has today, so this is a consistency gap in the existing pattern rather than one this feature introduced |

### Change log for this feature

| Date | Change | Type | FR affected | PR |
|---|---|---|---|---|
| 2026-08-27 | 0b.1 shipped: control plane, contribution mechanism, Appearance, calculator/currency/unit module entries | change | FR-001–FR-011, FR-019, FR-020, FR-029, FR-032 (partial — module-tier assembly only, on/off toggle itself is 0b.4), FR-042, FR-043 | — |
| 2026-08-29 | 0b.3 shipped: enforcing app lock (BiometricPrompt gate over the whole app), auto-lock timeout, hide-amounts (screen surface), notifications master switch; `contracts/app-lock-gate.md` renumbered with 2 new rules (credential-absent fallback, credential re-check) from a pre-implementation security.md review | change | FR-021–FR-028, FR-024, SC-009, SC-010 (partial — screen surface only) | — |
| 2026-08-29 | 0b.4 shipped: module on/off convention, consent-gated module entries, first real alert control (currency daily-rates), durable assistant consent (fixes the in-memory-flag defect), personal AI key row (new `SettingsRow.SecretText`), App details (version/privacy/licences/source, pluggable update check); new FR-046 + a non-vacuous ArchUnit rule closing security checklist CHK046; `settings-contribution.md` gained rule 10 | change | FR-030–FR-041, FR-043, FR-046 (new), SC-006, SC-014 | — |
