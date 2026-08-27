# Phase 0 Research — Settings control plane

Eight decisions this design rests on. Each was open when the spec was written; none is left as
NEEDS CLARIFICATION.

---

## R1 — How the shell discovers a module's settings entry

**Decision**: modules register their `SettingsContribution` as a Koin definition bound to that type;
the shell resolves **all** of them by type (`getAll<SettingsContribution>()`) at Settings open, then
filters and orders them.

**Rationale**: FR-004 forbids a hardcoded module list and SC-004 makes it verifiable — add a module,
diff, expect zero changes to Settings-owned files. Koin 3.5.6 is already the DI container and already
aggregates one module object per feature in `CalculatorApplication`; type resolution needs no new
mechanism, no annotation processor (which would be a new Gradle plugin, and the AGP 9 toolchain has
already rejected three of those — ADR-0010/0013/0014), and no reflection of our own.

**Alternatives considered**:
- *Explicit list in `AppModule`* — readable, but it is the central list FR-004 exists to remove; every
  phase would edit it, which is the drift the control plane is meant to prevent.
- *Annotation + code generation* — compile-time safety, but adds a processor to a toolchain with a
  documented history of plugin incompatibility, for a list that changes a handful of times.
- *Service-loader / manifest metadata* — works, but splits the declaration away from the Koin module
  where every other piece of a feature's wiring already lives.

**Verify during implementation**: `getAll` returns definitions bound to the type across all loaded
modules in Koin 3.5.6. If it does not behave as expected, the fallback is a single qualified `single`
per feature collected by qualifier — same property, marginally more ceremony. This is the one
mechanism assumption worth proving in the first RED test rather than at integration time.

---

## R2 — What a module is allowed to contribute

**Decision**: data only. A closed vocabulary of row types — Toggle, Choice, Stepper, Action,
Navigate, Info — each carrying its label, description, a `Flow` for its current value and a `suspend`
lambda to write it. No composable slots. A module needing bespoke UI contributes a `Navigate` row
pointing at its own screen via the existing `NavTarget` id mechanism.

**Rationale**: the design system's micro-frontend rule (ADR-0014 §8, constitution V/VI) says features
own screens and flows, never styling. Settings is the one surface where every module's UI would meet;
a composable slot would let each module style its own rows differently in a shared list, which is
precisely the fragmentation the component library exists to prevent. Data-only contributions are also
unit-testable without Compose, and the ArchUnit rule "no `SettingsContribution` implementation
references a Compose type" makes the constraint mechanical rather than reviewed.

**Alternatives considered**:
- *Composable slot per row* — maximal flexibility, immediate styling drift, contributions become
  untestable outside Robolectric.
- *Hybrid (data rows plus an optional slot)* — the escape hatch becomes the default within two
  phases; a closed vocabulary plus `Navigate` covers the same ground without that failure mode.

**Consequence**: the vocabulary must be complete enough at Phase 0b that the first module needing
something absent does not force a redesign. `contracts/settings-contribution.md` fixes it; adding a
row type later is an additive change to a sealed type, which is safe.

---

## R3 — Where the app-lock decision lives, and how the gate is enforced

**Decision**: split decision from effect. A pure function in `:libs:core/security/AppLockDecision.kt`
takes (lock enabled, timeout, elapsed-since-backgrounded) and returns LOCKED or UNLOCKED. The effect
— presenting `BiometricPrompt` with device-credential fallback and withholding content until it
succeeds — lives in one shell-level gate composable wrapping the entire content tree in
`MainActivity`.

**Rationale**: this is exactly the shape `navigation/BackContract.kt` already uses (`resolveBackAction`
decides, `MainActivity` performs), and it makes the security-critical rule provable in a JVM test with
no Robolectric. FR-021 requires a *single* app-wide checkpoint rather than a per-screen property —
wrapping the content tree once is the only placement where "no exempt surface" is structural rather
than a discipline every future screen must remember. `androidx.biometric` 1.1.0 is already in the
catalog and already declared by `:libs:settings`, so nothing new is added; it is simply used for the
first time.

**Alternatives considered**:
- *Per-screen gate* — the pattern the consent interceptor was explicitly built to avoid (ADR-0029
  decision 2's reasoning applies verbatim: make it structural, not a discipline).
- *A separate lock Activity* — clean isolation, but the app is deliberately single-activity, and a
  second Activity reopens the deep-link routing question the shell already answers.
- *Biometric only, no device-credential fallback* — locks a user out when their biometric fails or is
  unenrolled. The spec's edge case ("user removes their credential") is answered by allowing device
  credential, which is also what makes FR-022 ("no enrolled credential → cannot enable") coherent.

**Note on timing**: the gate must resolve before the first content frame, so the decision reads
persisted state synchronously enough to avoid a flash of unlocked content. This is a real risk on a
cold start where DataStore reads are asynchronous; the plan's step 4 treats "no flash" as an
acceptance condition, not an implementation detail.

---

## R4 — Links and notifications arriving while locked

**Decision**: the shell holds the pending target and dispatches it after a successful unlock; a
cancelled unlock keeps the target held for the next successful unlock in the same launch, and drops it
on process death.

**Rationale**: FR-023 forbids both dropping the target and delivering it before unlock. The surface
registry §3 already assumes a hold-and-dispatch mechanism for intent extras (the quick-add shortcut,
review-queue and policy deep links), so this is one mechanism serving two requirements rather than a
new one invented for the lock.

**Alternatives considered**:
- *Drop and land on Home* — simplest, but silently loses the user's intent, and notification actions
  ("Mark paid") become unreliable, which the channel registry's quick actions depend on.
- *Deliver then gate the destination screen* — reintroduces the per-screen gate R3 rejected.

---

## R5 — How hide-amounts reaches widgets and notifications

**Decision**: hide-amounts is a preference read by the money formatting path, not a per-screen flag.
Screens, the widget and notification builders all format through the shared helpers, so the masking
decision is applied once where the string is produced. The export path reads the same preference and
deliberately ignores it, stating so at the point of export.

**Rationale**: FR-025 lists three surfaces; the design system §11 already requires non-Compose
surfaces to format through the same helpers rather than a second implementation. Putting the decision
at the formatter is the only placement where a future surface inherits it automatically.

**Alternatives considered**: a per-screen `if (hideAmounts)` — every new surface must remember, and
the widget and notification paths are exactly the ones a screen author forgets.

---

## R6 — Sign-in from Settings without a feature-to-feature edge

**Decision**: Account's sign-in calls the auth repository in `:apps:finance:data` directly. It does not
reuse the onboarding module's sign-in screen.

**Rationale**: the credential flow already lives behind a repository in `:data`, which the app module
may use (`feature → data` via Repository is allowed; `app → data` more so). Reusing onboarding's
*screen* would either duplicate its UI or make Settings depend on a feature module's composables —
allowed for `app → feature` but poor separation, and it would drag onboarding's step sequencing into a
single-row action. Settings needs the action, not the flow.

**Alternatives considered**:
- *Launch onboarding's consent/sign-in flow from Settings* — reuses tested UI, but a user who is
  merely signing back in should not be walked through first-run consent steps they already answered;
  the spec's FR-012 explicitly rejects that dead end.

---

## R7 — Why the export row is not in Phase 0b

**Decision**: the export row ships with the phase that produces the financial records it exports
(phase 2 at the earliest), not with the control plane.

**Rationale**: FR-018 forbids the row appearing before it can produce a file, and the records to
export do not exist until the tracker screens and their repositories land. Shipping a row that
exports an empty file would violate FR-043 (a row must not imply a capability the app lacks) and
SC-011 (zero rows that appear operable and change nothing).

**Alternatives considered**: ship it exporting only preferences — but the export's scope was
deliberately narrowed to financial records in clarification, so an export of preferences is a
different feature wearing the same label.

---

## R8 — How the modules tier keys off feature flags

**Decision**: a contribution's `moduleKey` is the module's existing feature-flag key
(`calculator`, `assistant`, `networth`, …). The tier lists a contribution only when the resolver says
that key is enabled for the running version, and hides it otherwise.

**Rationale**: the flag file already carries `enabled`, `minVersion` and `requiresConsent` per module,
and the resolver already gates routes on the first two. Reusing the same key means "enable the module"
and "enable its settings" are one action, which is exactly the user's stated model — and it makes
`requiresConsent` available to satisfy FR-035 (state which consent is needed rather than showing inert
controls) with no new metadata.

**Alternatives considered**:
- *A separate settings-visibility flag per module* — a second switch to forget, and it lets settings
  and module visibility disagree.
- *Always list, grey out when disabled* — rejected in the spec's assumptions: the tier answers "what
  do I have", and the flag mechanism already hides disabled features everywhere else in the app.

**Open sub-question for tasks**: modules that are not feature modules today (the shell-owned
calculators' submodules, converters) still need stable keys. The flag file already has `currency`,
`unit`, `date`, `time`, so the keys exist; the grouping of submodules under a parent entry is a
presentation decision recorded in `data-model.md`, not new flag data.