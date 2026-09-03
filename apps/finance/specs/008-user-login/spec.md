# Feature Specification: User Login — Password + Google, Email OTP, Own Profile Storage

**Feature Branch**: `008-user-login`

**Created**: 2026-09-03

**Status**: Draft

**Input**: User description: "Login Options, User can use username and password and standed protocal for login & maintain the login session for the app in device. User can login with google and manual login creds. handle the sql data models as well for saving user details & DP in the DB it self insted of depends on google. handle all Login functional & technical in this spec"

**Amended 2026-09-03** with: "1) Build the Mail verification as well by sending the OTP. 2) User can
login with login creds or direct google. 3) Include to handle the all UI/UX screens as same theme and
map with the onbording future"

## Clarifications

### Session 2026-09-03

- Q: With password sign-up added, is having an account still optional (onboarding's "Use offline — calculators only" path)? → A: Keep the offline path — sign-in stays optional, tracker features stay gated behind it exactly as today.
- Q: Where does the profile photo live — device-only, or server-side? → A: Server-side, treated as **account data rather than tracker data**, so it is not behind the financial-sync consent switch. Consistent with the shipped pre-consent auth path, which already transmits Google-supplied name/email/photo. Requires explicit disclosure in privacy copy (FR-013b) and erasure on account deletion (FR-018).
- Q: Which account-management rows does this feature add to the existing Settings › Account screen? → A: The full self-service set — edit name and photo, change password, view linked sign-in methods, and link the missing method (add a password to a Google-only account, or link Google to a password account).
- Q: Where do sign-up, OTP and profile setup slot into onboarding's existing sign-in → consent → empty start order? → A: Identity first — sign-in/sign-up and OTP verification complete before the consent screen, preserving the shipped order; profile setup is offered after consent and is skippable.
- Q: What happens after repeated wrong-password attempts on one account? → A: Lock the account until the person completes a password reset via emailed OTP. Accepted with a known denial-of-service trade-off (someone who knows an email can deliberately lock its owner out), bounded by FR-040a–FR-040c: a high failure threshold, an email notifying the person why they are locked and how to unlock, and Google sign-in remaining unaffected.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sign up and sign in with password (Priority: P1)

A person who does not want to use a Google account creates an account directly in the app with an
email address (always collected, doubling as the recovery channel), an optional chosen username,
and a password — then returns later and signs back in using either the email or the username, plus
the password.

**Why this priority**: Today the app only supports Google sign-in (ADR-0031). Without a
password-based path, anyone without or unwilling to use a Google account cannot use the tracker at
all — this is the core gap the feature closes.

**Independent Test**: Can be fully tested by completing sign-up with a new identifier/password,
signing out, and signing back in with the same credentials — delivers a working account reachable
without Google.

**Acceptance Scenarios**:

1. **Given** a person has never signed up, **When** they submit a valid email, an optional unique
   username, and a password meeting the strength policy, **Then** an account is created and they
   land signed-in.
2. **Given** a person already has a password account, **When** they enter either their email or
   their username, plus the correct password, **Then** they are signed in.
3. **Given** a person enters an identifier and an incorrect password, **When** they submit,
   **Then** sign-in is rejected with a clear, generic message that does not reveal whether the
   identifier exists.
4. **Given** a person tries to sign up with an identifier already in use, **When** they submit,
   **Then** sign-up is rejected and they are told the identifier is taken.

---

### User Story 2 - Verify email via one-time code (Priority: P1)

Right after password sign-up, and whenever a password-account person requests a password reset, the
app sends a one-time numeric code (OTP) to their email address. The person enters that code in the
app to prove they own the email before the account is trusted (for sign-up) or before a new password
is accepted (for reset).

**Why this priority**: Every other password-account guarantee in this spec depends on the email
being verified — the auto-link to a matching Google account (FR-020) is explicitly gated on
verification (FR-020a), and password reset must not be completable by anyone who merely knows the
email address. Without OTP verification, both of those are unsafe.

**Independent Test**: Can be fully tested by signing up with a real email, receiving the OTP,
entering it correctly to complete verification, and separately by triggering "forgot password,"
receiving a fresh OTP, and using it to set a new password.

**Acceptance Scenarios**:

1. **Given** a person just submitted the sign-up form, **When** the account is created, **Then** an
   OTP is sent to the supplied email and the person is shown an OTP-entry screen before they reach
   the signed-in app.
2. **Given** a person on the OTP-entry screen, **When** they enter the correct, still-valid code,
   **Then** their email is marked verified and they proceed into the app.
3. **Given** a person on the OTP-entry screen, **When** they enter an incorrect code, **Then** they
   see a clear error and may try again, up to a limited number of attempts.
4. **Given** a person on the OTP-entry screen, **When** the code has expired, **Then** they can
   request a new one (subject to a resend cooldown) rather than being stuck.
5. **Given** a person requests "forgot password," **When** they submit their email/username,
   **Then** an OTP is sent to the account's email, and entering it correctly is required before they
   can set a new password.
6. **Given** a person has exceeded the maximum incorrect-OTP attempts, **When** they try again,
   **Then** the code is invalidated and they must request a new one, rather than being allowed
   unlimited guesses.

---

### User Story 3 - Sign in with Google (Priority: P1)

A person signs in using their existing Google account, as the app already supports, and this
remains available side-by-side with password sign-in as an equal option, not a replacement.

**Why this priority**: Google sign-in is the app's existing, already-shipped path (ADR-0031) and
must keep working unchanged for current users while password sign-in is added alongside it.

**Independent Test**: Can be fully tested by completing Google sign-in and confirming the person
lands signed-in exactly as today, with no regression.

**Acceptance Scenarios**:

1. **Given** a person picks "Sign in with Google," **When** they complete the Google account
   picker, **Then** they are signed in and a profile record exists for them in the app's own
   storage (not read live from Google on every screen).
2. **Given** a signed-in Google user, **When** they later open the app, **Then** they remain
   signed in without repeating the Google picker, identical to today's behavior.

---

### User Story 4 - Session stays signed in on the device (Priority: P2)

A signed-in person closes and reopens the app, restarts their phone, or leaves the app for days,
and remains signed in without re-entering credentials, until they explicitly sign out or the
session is no longer valid.

**Why this priority**: A login system that forces re-authentication constantly is unusable day to
day; persistent session is a baseline expectation for both login methods, not just Google's
existing one.

**Independent Test**: Can be fully tested by signing in, force-closing and reopening the app (and
simulating a lapsed short-lived token), and confirming the person is still signed in without
prompts, for both a password account and a Google account.

**Acceptance Scenarios**:

1. **Given** a signed-in person, **When** they reopen the app after the short-lived part of their
   session has expired, **Then** the app silently re-establishes the session without prompting for
   credentials.
2. **Given** a signed-in person, **When** their session can no longer be re-established (e.g.
   revoked, or the refresh itself fails), **Then** the app signs them out and shows a clear
   signed-out state rather than an infinite loading or repeated silent-retry loop.
3. **Given** a person taps "Sign out," **When** the action completes, **Then** the on-device
   session is fully cleared and the app shows the signed-out state.

---

### User Story 5 - Own profile: name and photo, independent of Google (Priority: P2)

A person's display name and profile photo are stored by the app itself, so they exist and stay
consistent whether the person signed up with a password or with Google — and a password-account
person can set a name and photo even though they never had a Google profile to draw from.

**Why this priority**: Today, whatever profile display exists depends on Google-provided values.
Password accounts have no Google profile to fall back on, so the app must own this data itself for
the feature to work for both login methods.

**Independent Test**: Can be fully tested by creating a password account, setting a display name
and photo, reinstalling the app, signing back in, and confirming the same name and photo appear
without any Google account involved.

**Acceptance Scenarios**:

1. **Given** a new password account with no photo set, **When** the person views their profile,
   **Then** a default placeholder photo is shown alongside their chosen display name.
2. **Given** a person signs in with Google for the first time, **When** their account is created,
   **Then** the app copies the Google-provided name/photo into its own profile storage once, as a
   starting value.
3. **Given** a person (either login method) edits their display name or photo afterward, **When**
   they save, **Then** the app's own stored value is updated and is not overwritten back to the
   Google value on a later Google sign-in.
4. **Given** a person deletes their account, **When** deletion completes, **Then** their stored
   profile (name and photo) is permanently removed along with their tracker data, per the app's
   existing erasure guarantee.

---

### User Story 6 - One consistent, onboarding-integrated experience (Priority: P2)

A first-time person opens the app, is guided through the existing onboarding flow, and reaches a
single sign-in/sign-up entry point that offers email-or-username-with-password and "Sign in with
Google" as two equally-presented options on the same screen — not a separate, disconnected login
module bolted on afterward. Every screen this feature adds (sign-up, OTP entry, forgot/reset
password) looks and behaves like the rest of the app: same colors, type, spacing, and components in
both light and dark mode.

**Why this priority**: The app already has an onboarding flow and a Google-only sign-in screen
(`feature/onboarding`). Adding password sign-in as a visually or structurally separate flow would
fragment the first-run experience and violate the platform's one-design-system rule.

**Independent Test**: Can be fully tested by walking a fresh install through onboarding into the
sign-in screen, confirming both sign-in options appear together, completing each new screen (sign-up,
OTP entry, forgot/reset password) in both light and dark mode, and confirming none of them look or
behave like a bolted-on, differently-styled flow.

**Acceptance Scenarios**:

1. **Given** a fresh install completes onboarding's introductory screens, **When** the person
   reaches the sign-in step, **Then** they see one screen offering both "email or username +
   password" fields and a "Sign in with Google" action, with a clear path from there to "create an
   account" for password sign-up and the existing "use offline — calculators only" option still
   present.
2. **Given** any new screen this feature introduces (sign-up, OTP entry, forgot password, reset
   password, profile photo/name setup), **When** it is viewed in light or dark mode, **Then** it
   renders using the same tokens and components as every other screen in the app — no screen-local
   colors, fonts, or one-off layouts.
3. **Given** a person completes sign-up, OTP verification, the existing consent screen, and the
   skippable profile-setup step, **When** they finish, **Then** they land in the same place the
   existing onboarding flow already hands off to today — no new, parallel "post-login home."
4. **Given** a person selects "use offline — calculators only," **When** they proceed, **Then** they
   skip OTP and profile setup entirely and reach the app with calculators usable and tracker
   features showing their existing signed-out state.

---

### User Story 7 - Manage the account from Settings (Priority: P2)

A signed-in person opens Settings › Account and can edit their display name and photo, change their
password, see which sign-in methods their account has, and add the method they are missing — a
Google-only person can set a password, and a password person can link Google — all without contacting
support or reinstalling.

**Why this priority**: Settings › Account already exists and already owns sign-in, sign-out, consent
and erasure. Everything this feature adds to a person's account is meaningless if they cannot manage
it afterward, and the alternative — using "forgot password" as a de-facto change-password flow — is a
workaround, not a design.

**Independent Test**: Can be fully tested by signing in, opening Settings › Account, editing name and
photo, changing the password and confirming the new one works, and adding the second sign-in method
then signing in with it.

**Acceptance Scenarios**:

1. **Given** a signed-in person on Settings › Account, **When** they edit their display name or
   photo and save, **Then** the change is stored and reflected everywhere the profile is shown.
2. **Given** a signed-in person with a password on their account, **When** they choose "change
   password" and supply their current password plus a new one meeting the strength policy, **Then**
   the password is changed and their other sessions are signed out.
3. **Given** a signed-in person, **When** they open Settings › Account, **Then** they can see which
   sign-in methods are linked to their account (password, Google, or both).
4. **Given** a person whose account has Google only, **When** they choose to add a password, **Then**
   they set one and can afterward sign in with either method.
5. **Given** a person whose account has a password only, **When** they choose to link Google and
   complete the Google picker, **Then** Google is added to the same account rather than creating a
   second one.
6. **Given** a person with both methods linked, **When** they attempt to remove one, **Then** the app
   prevents removing the last remaining sign-in method, so no account can be left unreachable.

---

### Edge Cases

- What happens when someone requests a password reset for an identifier that does not exist? System
  gives the same generic confirmation as a real one, so identifier existence is never leaked.
- What happens when someone signs up for a password account using an email that already has a
  Google account? The two are auto-linked into one identity **once the new password account's
  email is verified** — until verification completes, the accounts stay unlinked so a person
  cannot gain access to someone else's existing tracker data just by typing their email.
- What happens when someone picks a username at sign-up that is unique, but their email is already
  registered (password or Google)? Sign-up is rejected on the email, even though the username was
  free — email uniqueness is checked regardless of which identifier the person leads with.
- What happens when the device is offline during sign-in or sign-up? Clear "no connection" state,
  no partial account/session created.
- What happens when a person changes their password? All other signed-in sessions/devices for that
  account are invalidated, not just the current one.
- What happens when a person uploads a profile photo that is too large or an unsupported format?
  Rejected with a clear message before upload, not a silent failure.
- What happens when someone repeatedly enters the wrong password? Password sign-in for that account
  locks after a high failure threshold and stays locked until a password reset completes (FR-040);
  the person is emailed an explanation, and Google sign-in on the same account keeps working.
- What happens when a person is locked out but only ever used a password (no Google linked)? Password
  reset via emailed OTP is their only route back in — which is why FR-001a's email verification is
  mandatory at sign-up rather than optional.
- What happens when someone deliberately triggers a lockout on another person's email address? The
  target is emailed an explanation and can unlock immediately via password reset. This denial-of-
  service window is a known, accepted trade-off of the lockout choice, bounded by FR-040a–FR-040c
  rather than eliminated.
- What happens if a person deletes their account and later tries to sign up again with the same
  identifier? Treated as a brand-new account — no residual profile or tracker data is restored.
- What happens when a person abandons sign-up at the OTP screen and reopens the app later? They
  return to the OTP step for that pending account, not to a blank sign-up form, and can request a
  fresh code — a half-created, unverified account never leaves the person stranded with an email
  address they can no longer use.
- What happens when a person requests OTP resends repeatedly? Resends are cooldown-limited and
  capped, and each new code invalidates the previous one, so only the most recent code ever works.
- What happens when the OTP email fails to send or never arrives? The person is told sending failed
  and offered a retry, rather than being left on a screen waiting for a code that is not coming.
- What happens when a person enters the OTP after the code's validity window has passed? The code
  is rejected as expired (distinct from "incorrect"), and they are prompted to request a new one.
- What happens when a device is in dark mode, or has a very small screen, or a large system font
  size? Every new screen renders correctly under all three, using the app's existing responsive
  token tiers — no fixed-height text containers, no truncated fields.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST let a person create a password account by supplying an email address
  (always required — doubles as the recovery and verification channel), an optional unique
  username, and a password meeting a minimum strength policy.
- **FR-001a**: System MUST require the email address to be verified before it is trusted for
  account recovery or for the auto-linking behavior in FR-020.
- **FR-001b**: System MUST send a one-time numeric code (OTP) to the supplied email address
  immediately after password sign-up, and MUST present an OTP-entry step before the person reaches
  the signed-in app.
- **FR-001c**: System MUST mark the email verified when the person enters the correct, unexpired
  OTP, and MUST NOT treat an email as verified by any other means for a password account.
- **FR-001d**: System MUST expire each OTP after a short validity window, and MUST distinguish an
  expired code from an incorrect code in the message it shows.
- **FR-001e**: System MUST limit the number of incorrect OTP attempts per code and invalidate the
  code once that limit is reached, requiring a new code rather than allowing unlimited guesses.
- **FR-001f**: System MUST let the person request a new OTP, subject to a resend cooldown and an
  overall resend cap, and MUST invalidate any previously-issued code when a new one is sent.
- **FR-001g**: System MUST return a person who abandoned sign-up mid-verification to the OTP step
  for that pending account on their next app open, not to a blank sign-up form.
- **FR-001h**: System MUST tell the person when an OTP could not be sent and offer a retry, rather
  than leaving them waiting on the entry screen.
- **FR-002**: System MUST let a person with an existing password account sign in using either their
  email or their username, plus the correct password.
- **FR-003**: System MUST continue to let a person sign in using their Google account, unchanged
  from today, as an equally supported method alongside password sign-in.
- **FR-004**: System MUST reject a sign-in attempt with a generic, non-identifying error when the
  identifier or password is wrong, never revealing which one was incorrect.
- **FR-005**: System MUST reject sign-up when the chosen identifier is already registered, and MUST
  tell the person it is taken.
- **FR-006**: System MUST enforce a minimum password strength policy at sign-up and on password
  change, and MUST explain the requirement when a password is rejected.
- **FR-007**: System MUST let a person who has forgotten their password request a reset, delivered
  as an OTP to the account's verified email address, and MUST require that OTP to be entered
  correctly before a new password is accepted — completing the reset without support intervention.
- **FR-007a**: System MUST apply the same expiry, attempt-limit, resend-cooldown and
  single-valid-code rules (FR-001d–FR-001f) to reset OTPs as to sign-up OTPs.
- **FR-008**: System MUST invalidate a person's other active sessions when their password is
  changed or reset.
- **FR-009**: System MUST keep a person signed in across app restarts and device reboots, on the
  device where they signed in, until they sign out, their credentials change, or the session is
  revoked/expired — for both password and Google sign-in.
- **FR-010**: System MUST silently re-establish a lapsed short-lived session without prompting the
  person for credentials, as long as the underlying session is still valid.
- **FR-011**: System MUST sign the person out and present a clear signed-out state when a session
  can no longer be re-established, rather than retrying indefinitely or showing a stuck loading
  state.
- **FR-012**: System MUST let a signed-in person explicitly sign out, fully clearing the on-device
  session.
- **FR-013**: System MUST store each person's display name and profile photo in the app's own data
  store, independent of any third-party sign-in provider, so the same profile is available
  regardless of which login method was used.
- **FR-013a**: The stored profile (display name and photo) MUST be held server-side so it survives a
  reinstall and is available on any device the person signs in from, and MUST be classified as
  **account data, not tracker financial data** — it therefore travels on the same pre-consent
  sign-in path the app already uses today, and is NOT gated behind the financial-sync consent
  switch.
- **FR-013b**: Because FR-013a places profile data outside the financial-sync consent gate, the
  app's privacy and consent copy MUST explicitly disclose that the person's display name and profile
  photo are stored on the server, name the processor and hosting region as the existing consent copy
  already does, and point to the erasure path — the disclosure is what makes this lawful, and it is a
  requirement, not a courtesy.
- **FR-013c**: Profile data MUST NOT include or infer any financial information — no balances,
  holdings, or amounts may be stored in the profile record, so that the non-consent-gated path
  cannot become a route for financial data to leave the device.
- **FR-014**: System MUST show a default placeholder photo for any account that has not set a
  custom profile photo.
- **FR-015**: System MUST populate a new Google-signed-in person's stored name/photo from their
  Google profile once, at account creation, as a starting value only.
- **FR-016**: System MUST let a person edit their stored display name and profile photo regardless
  of login method, and MUST NOT overwrite an edited value back to the Google-provided value on a
  later Google sign-in.
- **FR-017**: System MUST require the existing data-sharing consent gate before any tracker data
  syncs off the device, for password accounts exactly as it already does for Google accounts — the
  login method MUST NOT change or bypass this gate.
- **FR-018**: System MUST let a person permanently delete their account, which removes their stored
  profile (name, photo — including the stored photo file itself, not merely the reference to it)
  together with their tracker data, consistent with the existing erasure guarantee, for either login
  method.
- **FR-019**: System MUST record security-relevant authentication events (sign-in success, sign-in
  failure, password reset, sign-out) for audit purposes, and MUST NOT record the password itself in
  any such record.
- **FR-020**: System MUST automatically link a password account and a Google account that share the
  same verified email address into one identity, reachable by signing in with either method,
  without the person taking any extra manual "link account" step.
- **FR-020a**: System MUST NOT link an account on the strength of an unverified email — linking
  only happens once the password account's email has completed verification (FR-001a).
- **FR-021**: System MUST design the account, session, and profile model as the single shared
  identity intended for reuse by every current and future Dhruv app (the platform's existing "one
  Google Web Client, one Supabase project" Dhruv ID design), even though only the Finance app
  consumes it in this unit of work — nothing in this design may assume Finance is the only app that
  will ever read this identity.
- **FR-022**: System MUST present password sign-in and Google sign-in as two options on a **single**
  sign-in screen, with neither presented as the fallback or afterthought of the other.
- **FR-023**: System MUST reach that sign-in screen through the app's **existing onboarding flow**,
  and MUST hand off after successful sign-in/sign-up to the same destination onboarding already
  hands off to today — no second, parallel post-login entry path.
- **FR-023a**: The first-run order MUST be: sign-in **or** sign-up (one screen) → email OTP
  verification, for password sign-up only → the existing consent screen → the existing empty-start
  screen. Identity is fully established before consent is requested, preserving the order the
  shipped flow already uses for Google sign-in.
- **FR-023b**: Initial profile setup (display name and photo) MUST be offered **after** consent, and
  MUST be skippable — a person who skips it gets their default placeholder photo (FR-014) and can set
  both later from Settings › Account (FR-033).
- **FR-023c**: The offline path (FR-031) MUST remain selectable from the same sign-in/sign-up screen,
  and choosing it MUST skip OTP and profile setup entirely.
- **FR-024**: Every screen this feature adds (sign-up, OTP entry, forgot password, reset password,
  initial profile name/photo setup) MUST use the app's existing design system — its shared color,
  type, spacing and radius tokens and its shared component library — with no screen-local colors,
  fonts, sizes, or one-off components.
- **FR-025**: Every screen this feature adds MUST render correctly in **both light and dark mode**,
  driven solely by the app's single existing theme switch.
- **FR-026**: Every screen this feature adds MUST define its applicable loading, empty, error and
  offline states using the app's existing state components, rather than a spinner that never
  resolves or an unstyled error string.
- **FR-027**: Every screen this feature adds MUST meet the app's existing accessibility standard —
  minimum touch-target sizes, sufficient contrast in both themes, descriptions on icon-only
  actions, and no meaning conveyed by color alone.
- **FR-028**: Every user-visible string this feature adds MUST live in the app's shared string
  resources from the outset, never as a literal inside a screen.
- **FR-029**: The sign-in, sign-up, OTP, and password-reset routes MUST be wrapped in the app's
  standard per-feature fault-isolation wrapper, so a failure inside them shows the app's error card
  rather than a blank crash.
- **FR-030**: Navigation between these screens MUST follow the app's existing navigation law — one
  back path to a single parent, confirm-on-discard for partially-filled forms, and no back arrow on
  a tab root.
- **FR-031**: System MUST preserve the existing "use the app without an account" path — a person MUST
  still be able to decline sign-in at onboarding and use the calculator and converter features, with
  tracker features remaining gated behind sign-in exactly as they are today. Adding password sign-in
  MUST NOT make an account mandatory.
- **FR-032**: A person who declined sign-in MUST be able to sign in or create an account later
  without reinstalling or re-running first-run onboarding.

**Account management (Settings › Account)**

- **FR-033**: Settings › Account MUST let a signed-in person edit their stored display name and
  profile photo, satisfying FR-016's edit capability at a concrete, reachable location.
- **FR-034**: Settings › Account MUST let a person who has a password on their account change it by
  supplying their current password and a new one meeting the strength policy; the change MUST
  invalidate their other sessions per FR-008.
- **FR-035**: Settings › Account MUST show which sign-in methods are currently linked to the account
  (password, Google, or both).
- **FR-036**: Settings › Account MUST let a person add the sign-in method they do not yet have — set
  a password on a Google-only account, or link Google to a password-only account — with the linked
  method attaching to the **same** account rather than creating a second one.
- **FR-037**: System MUST prevent removing a person's last remaining sign-in method, so an account
  can never be left with no way to sign in.
- **FR-038**: Adding a password to a Google-only account MUST NOT require an email-verification OTP,
  because that account's email was already verified by Google — the OTP requirement (FR-001b) applies
  to email addresses the app has not otherwise seen verified.
- **FR-039**: All rows this feature adds to Settings MUST be registered through the Settings control
  plane's existing contribution mechanism and appear in the Account tier, not as a separate or
  parallel settings surface.

**Failed-attempt lockout**

- **FR-040**: After a defined number of consecutive failed password attempts on one account, System
  MUST lock password sign-in for that account, and MUST require a successful password reset (via the
  emailed OTP flow, FR-007) to unlock it.
- **FR-040a**: The lockout threshold MUST be high enough that a person mistyping their own password
  is not realistically locked out, while still bounding brute-force attempts — and the count MUST
  reset on any successful sign-in.
- **FR-040b**: When an account becomes locked, System MUST notify the account's verified email
  explaining that the account was locked, why, and how to unlock it — because the person who is
  locked out is frequently *not* the person who caused it.
- **FR-040c**: A lockout MUST affect password sign-in only. Google sign-in on the same account MUST
  continue to work, so a person with both methods linked always retains a way in.
- **FR-040d**: The locked-out message MUST NOT reveal whether the email address corresponds to a real
  account, consistent with FR-004.

### Key Entities *(include if feature involves data)*

- **User Account**: The authentication identity behind a person — the same identity every current
  and future Dhruv app is meant to share (FR-021). Holds the required, verified email address, an
  optional unique username, a securely-hashed password (present only when a password method is
  linked), which sign-in method(s) are linked to it (password and/or Google — auto-linked when
  their emails match and are verified, FR-020), account creation time, and last sign-in time.
- **User Profile**: The person-facing identity the app owns and displays — display name and profile
  photo — kept in the app's own storage and never re-read live from Google. Tied one-to-one to a
  User Account. Updated independently by the person after any initial value copied in from Google.
- **Session**: The record of a currently signed-in state on a device — a short-lived credential plus
  a longer-lived one used to silently re-establish it, and enough state to know when a session can
  no longer be renewed and must sign the person out.
- **One-Time Code (OTP)**: A short numeric code issued to an account's email for one of two purposes
  — verifying the email at sign-up, or authorizing a password reset. Carries which purpose it serves,
  when it expires, how many incorrect attempts remain against it, and whether it has already been
  consumed. Only the most recently issued code for a given account and purpose is ever valid.
- **Password Reset Request**: A time-limited, single-use request tied to an account, created when a
  person asks to reset a forgotten password, authorized by its own OTP, and consumed once the reset
  completes.
- **Lockout State**: Per account, the count of consecutive failed password attempts and whether
  password sign-in is currently locked. Reset by a successful sign-in; cleared by a completed password
  reset. Applies to the password method only, never to Google sign-in (FR-040c).
- **Linked Sign-in Methods**: Which methods an account can be reached by — password, Google, or both.
  Displayed in Settings › Account (FR-035), added to (FR-036), and constrained so the set is never
  empty (FR-037).
- **Verification State**: Whether an account's email has been proven to belong to the person —
  the gate FR-001a/FR-020a depend on, and the thing that distinguishes a fully-created account from
  one still pending OTP entry (FR-001g).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new person can complete password sign-up, including entering the emailed one-time
  code, and reach a signed-in state in under 3 minutes.
- **SC-001a**: The verification code arrives in the person's inbox within 60 seconds of sign-up in
  at least 95% of attempts.
- **SC-002**: A returning person with a valid session opens the app and is already signed in,
  without re-entering credentials, in 100% of cases where their session has not expired or been
  revoked.
- **SC-003**: A person who forgets their password can regain access on their own, without contacting
  support, in under 5 minutes end to end.
- **SC-004**: A person's display name and profile photo remain correct and unchanged after
  reinstalling the app and signing back in, for both login methods, in 100% of cases.
- **SC-005**: Sign-in failure messages never allow an outside party to determine whether a given
  identifier has an account, verified by review of every failure-path message.
- **SC-006**: Deleting an account removes 100% of that person's profile and tracker data within the
  same guaranteed erasure window the app already provides.
- **SC-007**: An expired or already-used one-time code is rejected in 100% of attempts, and no code
  remains usable after a newer one has been issued for the same account and purpose.
- **SC-008**: Every screen this feature adds passes the app's existing UI review in both light and
  dark mode, with zero screen-local colors, fonts, sizes, or hardcoded user-visible strings.
- **SC-009**: A person on a fresh install reaches the sign-in screen through the existing onboarding
  flow, and never encounters a second, separately-styled login entry point.
- **SC-010**: A person who declines sign-in can still use every calculator and converter feature,
  with zero features lost relative to the current release.
- **SC-011**: A signed-in person can change their password, edit their name and photo, and add their
  missing sign-in method entirely from Settings › Account, with no step requiring support contact,
  reinstall, or use of the "forgot password" flow as a workaround.
- **SC-012**: An account is never left with zero usable sign-in methods, verified across every
  add/remove path.
- **SC-013**: A person locked out by failed password attempts receives an explanatory email and can
  regain access via password reset in under 5 minutes, without support contact.

## Assumptions

- "Standard protocol" is read as the same token-based sign-in-plus-silent-refresh pattern the app's
  existing Google sign-in already uses, applied to password accounts too — not a new, separate
  transport mechanism.
- On-device session storage reuses the app's existing secure, encrypted storage approach for
  session credentials; it is not a new storage mechanism per login method.
- A default password policy (minimum length plus basic complexity) applies unless a more specific
  policy is set during technical planning.
- Profile photo is a person-uploaded image held server-side (FR-013a). The specific storage mechanism,
  accepted formats and maximum file size remain technical-planning decisions; that it is server-held
  account data outside the financial-sync consent gate is a decided requirement, not a planning
  choice.
- This feature covers the Finance app's existing tracker sign-in surface. It does not change
  Vault's login, which by platform rule never shares network-based authentication with any other
  module.
- Existing DPDP consent behavior (consent gates data leaving the device, not sign-in itself) is
  preserved unchanged for both login methods.
- "Cross-app Dhruv ID scope" (FR-021) means the account/profile/session *design* is shared —
  concretely, this reuses the platform's existing single Google Web Client and single Supabase
  project (ADR-0031) rather than inventing a Finance-specific identity. It does **not** mean this
  unit of work builds or touches Tools/Vault/Health/Relationship — none of those apps exist in
  `settings.gradle.kts` yet, so there is no second app to wire up. This spec's directory correctly
  stays under `apps/finance/specs/` per the repo's Spec-Kit Directory Rule (implementation is
  Finance-only; only the identity model is deliberately generalized).
- Auto-linking (FR-020) trusts email-verification, not a live "same Google account" check — this
  matches how Supabase GoTrue itself represents identities and avoids inventing a second
  reconciliation mechanism.
- Google sign-in needs no OTP step — the email is already verified by Google itself. OTP applies to
  password accounts only.
- Concrete OTP parameters (code length, validity window, attempt limit, resend cooldown and cap) use
  common industry defaults — a 6-digit code, a validity window of a few minutes, a small attempt
  limit, and a short resend cooldown — unless technical planning sets specific values. The exact
  numbers are a planning decision; the *existence* of each limit is the requirement here.
- Email delivery reuses the authentication provider's own built-in email sending rather than
  introducing a separate mail service. Whether the free-tier sending limits suffice, and whether a
  custom sender domain is needed, is a technical-planning question — this spec requires only that
  codes are delivered and that delivery failure is surfaced to the person (FR-001h).
- The UI/UX requirements (FR-022–FR-030) name the app's existing design system, navigation law,
  state components, accessibility standard and fault-isolation wrapper rather than restating their
  contents — those are defined once at platform level and are not re-specified per feature.
- Onboarding integration (FR-023) extends the app's existing onboarding module rather than creating
  a new one; whether the new screens live inside that module or beside it is a technical-planning
  decision, but the person-facing flow is one continuous path either way.
- The concrete lockout threshold, and whether the lock is scoped per-account or also per-device/
  network, are technical-planning decisions. FR-040a fixes the property the threshold must satisfy
  (a person mistyping their own password is not realistically locked out); the number itself is not
  a business requirement.
- The existing Settings control plane, its Account tier, and its contribution mechanism already ship
  (spec `004-settings`). This feature adds rows to that surface (FR-033–FR-039) rather than building
  a settings surface of its own.
- Linking a second sign-in method (FR-036) is expected to be representable in the existing
  authentication provider's own identity model, the same mechanism the auto-link in FR-020 relies on.
  If it turns out not to be, that is a planning-stage finding that changes *how* FR-036 is met, not
  whether it is required.