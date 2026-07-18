# Technical Review — Security & Navigation Across the Full Spec Set

> Status: **REVIEW COMPLETE** (2026-07-12). Lens: engineering soundness of every security feature
> and every navigation mechanism in the plan (P1–P6, R0–R11, design standard), verified against
> the actual codebase where claims were checkable. Findings: **SEC1–SEC9** (security) and
> **NAV1–NAV6** (navigation). Dispositions marked **PATCHED** were applied to the owning spec the
> same day; **ADR** items reserve a decision number. Companions: consistency review (F1–F16),
> PO review (PG1–PG10).

---

## Part 1 — Threat model (the missing five minutes)

**Assets:** Supabase session tokens (full account takeover), the financial dataset itself
(privacy), BYO secrets (Gemini key, GitHub PAT), the on-screen balance (shoulder surfing),
write paths (data corruption).

**Trust boundaries:** device screen/switcher (R3), notification shade (R6/R7), launcher widget
(R8), Supabase REST (P1), GitHub API (R4), user-supplied CSV files (R7), SAF export files (R7),
intent extras from launcher/notifications (R5b), local snapshot stores (R5b/P4/R8).

**Deliberate non-goals (stated, not implied):** defense against a *rooted/forensic* attacker
(data at rest relies on platform encryption + Keystore-wrapped stores; app lock is NOT a crypto
gate — see SEC2), and DoS (single-user app).

## Part 2 — Security findings

### 🔴 SEC1 — R3 biometric API strategy breaks below API 30
R3 specifies `setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)`. On API 28–29 this
combination is **not supported** by the platform (androidx.biometric documents it; the prompt can
throw or silently fail), and DEVICE_CREDENTIAL-alone is unsupported below API 30. minSdk is 26 —
the app-lock flagship feature would be broken on every pre-Android-11 device.
**Resolution: PATCHED in R3** — per-API strategy: API 30+ uses the STRONG|DEVICE_CREDENTIAL
combo; API 26–29 uses `BIOMETRIC_STRONG` with `setNegativeButtonText("Use screen lock")` routing
to `KeyguardManager.createConfirmDeviceCredentialIntent()` as the credential fallback. Both paths
covered in the manual device checklist (one legacy-API device/emulator required).

### 🔴 SEC2 — App lock's threat model was implicit (it is a UI gate, not a crypto gate)
Nothing in R3 binds decryption to authentication (no `CryptoObject`); Supabase tokens and
DataStore remain decryptable by the process regardless of lock state. That is a *reasonable*
design (device credential + platform FDE already gate at-rest access; a CryptoObject-bound key
would re-lock on biometric re-enrollment — the ADR-0003 failure mode). But unstated, a future
contributor could "harden" it into the re-enrollment trap, or overstate the lock in user copy.
**Resolution: PATCHED in R3** — explicit threat-model paragraph: lock defends against casual
device access/shoulder-surfing; it is not at-rest encryption; user-facing copy must never call it
"encryption".

### 🟡 SEC3 — Background workers vs consent withdrawal / sign-out
R5b (recurring), R6 (alerts), R8 (widget refresh) all run scheduled work over tracker data.
Sign-out/withdraw-consent flows specced clearing the *widget* snapshot only. Without an explicit
rule, the recurring/renewal workers keep evaluating their local snapshots and keep posting
finance notifications after the user has withdrawn consent — processing after consent withdrawal
is a DPDP violation, not just a bug.
**Resolution: PATCHED (R5b + R6)** — on sign-out or consent withdrawal: cancel all
tracker-domain workers, clear all tracker snapshot stores (rules, renewal dates, widget), clear
alert-state keys. One `TrackerSessionTeardown` helper owns the list; sign-out and withdraw both
call it. Roadmap §4 gains the standing rule: *every tracker-domain local snapshot store registers
in teardown AND in backup exclusions.*

### 🟡 SEC4 — CSV formula injection on export (and hygiene on import)
R7 exports user-entered text (names, notes) into CSV that will be opened in Excel/Sheets. A note
like `=HYPERLINK(...)` or `+cmd|...` becomes an executing formula on the user's desktop — classic
CSV injection. Import side: amounts/dates validated, but text fields weren't length-capped.
**Resolution: PATCHED in R7** — `CsvCodec` escapes cells starting with `=`, `+`, `-`, `@`, tab,
CR (prefix `'`) on export and strips the guard on import (round-trip test covers it); import
validation gains text-length caps (name 120, notes 2000 — matching editor limits).

### 🟡 SEC5 — Export file is plaintext financial data with no user warning
R7's ZIP is the user's entire financial life, unencrypted, written wherever they point SAF
(often cloud-synced folders).
**Resolution: PATCHED in R7 (copy) + Not-Doing** — export dialog states "This file contains your
complete financial data, unencrypted. Store it somewhere you trust." Password-protected/AES export
is a recorded Not-Doing (no platform-native encrypted ZIP; a crypto container dependency is not
worth it for a personal app — revisit only on real need).

### 🟡 SEC6 — R4 GitHub call: pinning stance and header hygiene unstated
Supabase traffic is CA-pinned (ISRG X1/X2). The GitHub API call specced nothing — ambiguity
invites either accidental pinning (GitHub rotates across DigiCert/Sectigo chains → self-brick) or
an OkHttp logging interceptor leaking `Authorization: Bearer <PAT>` in debug logs.
**Resolution: PATCHED in R4** — explicitly NOT pinned (public release metadata; token is the only
secret and TLS suffices; pinning risk asymmetric), and the shared OkHttp client's logging
interceptor redacts `Authorization` headers globally (release builds log nothing network-level
anyway; the redaction protects debug builds).

### 🟡 SEC7 — Quick actions must never write via BroadcastReceiver
PG10's "Mark paid"/"Confirm all" could naively ship as `PendingIntent.getBroadcast` — a receiver
writing to Supabase outside the lock gate and outside FeatureHost error isolation.
**Resolution: already correct in R6, now explicit** — all notification actions are
`PendingIntent.getActivity` routing through MainActivity → lock hold-and-dispatch → the same
ReviewInbox confirm path. No receiver write paths exist anywhere in the app.

### ⚪ SEC8 — `delete_my_account()` search-path pinning
Verified: P1's SQL already pins `set search_path = public` on the security-definer function —
the standard privilege-escalation guard is present. R7's report RPCs are security-**invoker**
(RLS applies); the spec already says so. No action.

### ⚪ SEC9 — Intent extras are data, not commands
`QUICK_ADD` etc. are exported-activity inputs (launcher shortcuts make MainActivity's intent
surface reachable by other apps). The registry pattern already constrains them to an enum of
navigation targets with no payload beyond validated IDs; `OPEN_POLICY(id)` must treat the id as
untrusted (unknown/foreign id → normal not-found state, never a crash). Noted in the design
standard registry (§7.2).

## Part 3 — Navigation findings

### 🔴 NAV1 — The architecture says NavHost; the code has none (verified)
`CLAUDE.md`/`PLATFORM.md` mandate "single-activity **NavHost**"; the ArchUnit rule's `because`
text says "screens must navigate via NavHost". **Verified in code: there is no NavHost, no
NavController, no navigation-compose dependency anywhere in main source** — MainActivity is a
`HorizontalPager` + one `OnBackPressedCallback` (`MainActivity.kt:204-315`), and features manage
sub-screens as local state. This was fine while every screen was a leaf. It stops being fine the
moment the R-specs land:
- R5b/R6/R7 notifications and R8 widget need **addressable destinations** (`OPEN_POLICY(id)`,
  `OPEN_REPORTS(month)`, `REVIEW_INBOX`…);
- R8 **search results navigate cross-feature** (search → holding detail / policy detail /
  transaction) — with local-state navigation the search feature would need feature→feature
  references, which ArchUnit forbids;
- R3 lock hold-and-dispatch needs a single place to park and replay a pending destination;
- process-death restoration of sub-screen state is ad-hoc per feature today.

**Resolution: reserve ADR-0024 — navigation architecture.** Proposed shape (decide before R3/R5b
build; P1 can ship on the current model):
1. Keep the pager for the 3 top-level tabs (it is the product's swipe UX — not the problem).
2. Inside each tab, a **nested `NavHost`** (navigation-compose, stable AndroidX) owns sub-screen
   stacks — SavedState restoration and predictive back come free.
3. A **`NavigationDispatcher`** (Koin singleton in `:apps:finance:app`) is the only cross-feature
   navigation mechanism: consumers emit a `NavTarget` (sealed type in `:libs:core` — route id +
   validated args, no screen class references), the dispatcher maps it to (tab index, route) and
   drives pager + NavController. Intent extras, notification actions, widget taps, and search
   results all go through it — one mechanism, four producers.
4. R3's lock holds/replays exactly one pending `NavTarget`.
5. ArchUnit gains a real rule: features may depend on the `NavTarget` contract in core, never on
   another feature's screens — making the currently-aspirational test text true.

### 🟡 NAV2 — Route registry had no owner in code
The design standard's §3.3 route registry was documentation only. With ADR-0024, `NavTarget` is
its code twin — each registry row = one sealed subtype. **Resolution: PATCHED in design standard**
(§3.3 note) — the registry and the sealed type must stay 1:1; adding a route = adding both.

### 🟡 NAV3 — Back contract vs nested NavHosts
P1's hand-rolled `BackHandler` contract (sub-screen → tab root → page 0 → exit) must be
re-verified when nested NavControllers own the stacks: back order becomes NavController pop →
pager-to-0 → system. Predictive back (R8/N19) only works if all three layers use
back-dispatcher-aware APIs. **Resolution:** folded into ADR-0024 acceptance criteria + R8's
predictive-back checklist.

### 🟡 NAV4 — Dynamic tab set vs pager indices
Tabs are computed from feature flags + settings toggles; a Remote Config flag flip mid-session
changes `tabs.size` while `pagerState.currentPage` points at an old index (today: possible IOOB
or wrong-tab render; `tabs.getOrNull(...)` at `MainActivity.kt:220` already hints at the smell).
**Resolution:** ADR-0024 scope — tab list captured as stable keys, page index resolved by key not
position; flag flips move the user to page 0 rather than shifting content under them.

### ⚪ NAV5 — Secure-flag lifecycle on swipe
`FeatureHost(secure)` via DisposableEffect: during a pager swipe two pages compose
simultaneously — a tracker page entering + tools page leaving must not clear the flag while
tracker content is on screen. Counting semantics (flag set while ≥1 secure route composed), not
last-writer-wins. **Resolution: PATCHED in R3** (one line: secure-flag effect uses a ref-count).

### ⚪ NAV6 — Onboarding/lock are overlays, not destinations
Both correctly specced as shell overlays outside any nav graph (no route = can't be deep-linked
past). Confirmed consistent with ADR-0024 shape; no action.

## Part 4 — Verdict

Security: the *designs* are sound (custody model consistent — user secrets in encrypted DataStore,
nothing in APK; RLS + invoker RPCs correct; consent/erasure paths complete after SEC3). The two
🔴 items were implementability bugs (SEC1) and an implicit threat model (SEC2), both patched.

Navigation: one real architectural debt (NAV1) that every R-phase quietly assumed away. ADR-0024
must be decided **before R3/R5b implementation starts** — it is the substrate for lock dispatch,
notifications, widget, and search. Sequencing note added to the master roadmap: ADR-0024 lands
with R3 at the latest.
