# Quickstart: proving Automation (Phase 7)

How to verify each sub-phase end to end. Every slice ends green on `regressionCheck`, closes its own
scenario rows and merges separately, so each section below is a **merge gate**, not a suggestion.

Prerequisites for the whole phase: Phases 1–6 shipped; `JAVA_HOME` = Android Studio JBR; a signed-in
`dhruv-dev` session with sync consent granted.

```bash
# The gate every sub-phase must pass
./gradlew regressionCheck

# This module alone, while iterating
./gradlew :apps:finance:feature:automation:testDebugUnitTest
./gradlew :apps:finance:data:testDebugUnitTest

# Money-precision scan — the parser is the one place a Double could sneak in
./gradlew checkTrackerMoneyPrecision
```

---

## Before any code: the DB slice

Postgres objects are authored declaratively, then diffed — never hand-written into `migrations/`
(ADR-0032).

```bash
# 1. Edit the declarative files
#    supabase/schemas/finance/10_tables/automation_rules.sql      (new)
#    supabase/schemas/finance/10_tables/holdings.sql              (+ auto_value_series)
#    supabase/schemas/finance/30_functions/find_possible_duplicates.sql   (new)
#    supabase/schemas/public/30_functions/delete_my_data.sql      (+ automation_rules)

# 2. Generate the migration
supabase db diff -f automation

# 3. Hand-append what db diff cannot emit (ADR-0032 caveat list, Article IXa):
#      - grant select, insert, update on finance.automation_rules to authenticated;
#      - security invoker on find_possible_duplicates
#    Then READ the generated file. Both omissions are silent.

# 4. Static guards
python scripts/db/gen_schema_docs.py equiv
python scripts/db/gen_schema_docs.py docs --check

# 5. Regenerate typed web client (both schemas — omitting one silently loses coverage)
supabase gen types typescript --schema public,finance > web/src/shared/types/database.ts
```

**RLS check that actually proves something** (Article IXa): sign in as a second user and confirm they
read **zero rows** from `finance.automation_rules` and get **zero matches** from
`find_possible_duplicates` against the first user's transactions. A `security definer` on that
function would let one user probe another's spending by amount — this test is what catches it.

---

## 7a — Review queue + Ignored list

Ships the queue with **only Phase 3's recurring proposals**. No SMS, no permission, no flag
dependency — this is why it is first.

| Prove | How | Closes |
|---|---|---|
| Rows render not-yet-accepted | Seed three recurring proposals; open G2. All three dashed, showing amount, date, account, proposed category | `AUT-UI-001` |
| Accept creates one record | Accept one; check `finance.transactions` gained exactly one row with those values, and its history says it came from an automated source | `AUT-FLOW-001` |
| Correct-then-accept wins | Change the category, accept; the created row carries **the user's** category | US1-3 |
| Ignore creates nothing | Ignore one; no transaction anywhere; the row appears in the Ignored list | `AUT-FLOW-002` |
| Restore round-trips | Restore it; it returns to pending with the same details | **new row** |
| Never twice | Re-run the recurring materialiser over the same occurrence; **no** new proposal | **new row** |
| Accept-all partitions | Queue of {3 clean, 1 missing category}; accept-all records 3, leaves 1, reports why | FR-009b |
| States | Force each of the six; confirm the **remote-failure** case still renders local rows plus a retry, not a full error screen | FR-011 |

**Unit, not UI**: `partitionForAcceptAll` gets a table-driven test over every queue composition before
the screen exists. That is what makes SC-006a provable later.

---

## 7b — Hub, G3, module wiring

```bash
# The module must be registered in BOTH places, or coverage silently excludes it
grep -n "feature:automation" settings.gradle.kts
grep -n "feature:automation" build.gradle.kts     # coveredModules — Article X
grep -n '"automation"' platform/feature-flags/dhruv-finance.json
```

| Prove | How |
|---|---|
| Flag exists and is off | The key is present, `enabled: false`, `requiresConsent: true` |
| Flag off ⇒ disabled state | All four routes render `FeatureDisabledCard`, not a crash |
| Header rule is stated | G1 says every source only suggests and the user approves each entry |
| Each row states its scope | Every source row says what it reads, in the user's terms, without a second screen |
| Unavailable is honest | The account-aggregator row is present and marked unavailable — not hidden, not fake-working |
| G3 states three things first | Scope, duration and purpose are all readable **before** any consent control (`AUT-FLOW-003`) |
| Dismiss grants nothing | Close G3; nothing enabled, nothing granted |
| Settings entry ships with the module | Settings › Modules › Automation appears because the module declares it — no central list was edited |

---

## 7c — Bank message source

**Test the parser first, on the JVM, with no device involved.** A table of representative messages in,
`Parsed`/`Unparseable` out. This is where SC-003 and SC-004 are actually proven.

| Prove | How | Closes |
|---|---|---|
| Non-bank senders produce nothing | Feed promotional messages containing `₹`, and OTPs from a bank's own sender. **Zero** proposals | SC-004 |
| Parse accuracy | ≥ 90% of a representative bank-message set yields correct amount, date and account | SC-003 |
| Unparseable is visible | The remainder become rows naming what they need — none dropped, none guessed | FR-010 |
| Paise, not float | `checkTrackerMoneyPrecision` green; no `Double` in the parse path | Article VII |
| Two gates | Permission without consent ⇒ nothing read. Consent without permission ⇒ nothing read. The hub says **which** is missing | FR-020 |
| No bypass | Assert there is no call path to `SmsInboxReader` that skips the gate check | Article VIII |
| Background scan works | Message arrives with the app closed; `TestListenableWorkerBuilder` runs the worker; a proposal exists without the app being opened | SC-003a |
| Missed window loses nothing | Skip several scan intervals, then run; everything since the watermark appears, in date order | FR-027d |
| Not live | The queue states it refreshes periodically | FR-027b |
| Nothing leaves | Inspect every outbound payload the feature can produce: **zero** occurrences of message text | SC-005, `AUT-BR-002` |
| Suggestion, not transaction | A parsed message writes a proposal, never a ledger row | `AUT-BR-001` |
| Freeze cycle | Withdraw consent with rows pending ⇒ visible, non-actionable, banner with delete/re-enable; other origins still actionable. Re-grant ⇒ same rows actionable again, **no duplicates** | **new row** |
| No telephony | On a device with no SMS provider, the source states it is unavailable | FR-028 |

---

## 7d — Duplicate detection

| Prove | How | Closes |
|---|---|---|
| Callout appears | Create a transaction; produce a proposal matching amount + account within 3 days. The row carries a callout **identifying** the matched record | `AUT-UI-002` |
| Advisory, not blocking | Accept the flagged proposal anyway; the transaction is created | FR-030 |
| Genuine repeats work | Two identical same-day payments both recordable | US4-3 |
| No false callout | A proposal with no close match carries none | US4-4 |
| One round trip | The whole visible queue is checked in a single call, not one per row | R10 |
| Accept-all excludes them | Queue with a flagged row; accept-all records **zero** duplicates | SC-006a |
| Rule is stated | The matching rule is visible to the user, not opaque | FR-031 |

---

## 7e — Learned rules

| Prove | How | Closes |
|---|---|---|
| Taught only on purpose | Correct a category and accept; the offer appears. **Decline leaves no rule** | FR-032 |
| Applies next time | A second matching proposal arrives with the taught category proposed, no user action | SC-007 |
| Count is accurate | The rule shows the right applied count | `AUT-BR-003` |
| Disable and remove work | Both are single actions; a removed rule stops applying | FR-035 |
| History untouched | Transactions already created under the rule are unchanged after removal | FR-035 |
| Zero-match rules survive | A rule matching nothing stays listed with count 0, not pruned | edge case |
| No message text stored | Inspect `automation_rules` — the matcher is a merchant token | R5 |

---

## 7f — Price feed

**The predicate is the test.** Run the ten-case table in
[contracts/price-feed.md](./contracts/price-feed.md) §2 as a parameterised unit test before anything
fetches a price.

| Prove | How | Closes |
|---|---|---|
| Opt-in only | A holding with null `auto_value_series` is never included | spec assumption |
| Below threshold ⇒ silence | A small move produces **zero** proposals; a flat price produces zero over any period | SC-013a |
| Above threshold ⇒ proposal | Names the affected holdings, from → to, how far it moved, and as-at | FR-044, FR-044c |
| Kind is unmistakable | A user can tell a value update from a transaction without opening it | SC-014 |
| Accept inserts a valuation | `finance.valuations` gains one row; append-only intact | R3 |
| Ignore changes nothing | Zero values move; the refused move does not return | SC-013, FR-045a |
| A later move does propose | Case 5 of the table, end to end | FR-045 |
| Threshold is findable | Change it in Settings › Modules › Automation without leaving the entry | SC-013b |
| Unreachable is stated | Break the source; the hub row says so and no stale proposal is presented as current | FR-046 |
| Manual update wins | Revalue by hand with a proposal pending; accepting does not blindly overwrite the hand-set value | edge case |

---

## 7g — Alert, erasure, closure

| Prove | How | Closes |
|---|---|---|
| Alert fires and counts | With proposals pending, an alert states how many need review | FR-036 |
| Deep link lands | Tapping opens **G2 directly** — app closed, and with the app lock engaged (unlock first, then land) | SC-009, FR-037 |
| Silence when empty | Queue emptied ⇒ next evaluation raises nothing | FR-039 |
| One control | The alert has exactly one switch, in the Automation entry, and obeys the app-wide master switch | FR-038 |
| **Erasure, both arms** | Run "Delete my data". `finance.automation_rules` empty **and** `automation_proposal` / `automation_seen_key` empty | SC-012, FR-050 |
| Masking sweep | Privacy mode on: amounts masked in the queue, the Ignored list **and** the alert | FR-051 |
| Registry corrected | §4's *Recently deleted* line deleted; §1 and §3 rows updated | FR-054 |
| Full chain, by hand | Enable the source → real message → proposal in G2 → accept → transaction in D1, with **no** direct-to-ledger write anywhere | `AUT-FLOW-004` |
| Sec pass | Full DPDP review — this is the phase that requests SMS | impl plan §7 step 6 |
| Flag on | `automation` flipped to `enabled: true` **only after** everything above passes | FR-052 |

**The flag flip is the last action of the phase.** Everything ships behind `enabled: false` until this
gate, per surface registry §1 and implementation plan §7.

---

## Closure (Article Xa)

Not optional, and not a follow-up:

- `apps/finance/FEATURES.md` — the `automation` row moves from *planned* to *enabled*
- `apps/finance/feature/shell/automation/README.md` — real screens, ViewModels, data deps, flag key
- `CHANGELOG.md` — an entry under the `finance-*` release heading
- `spec.md` § **Implementation record** — what shipped, what deviated and why, what was deferred
- Impl plan §7's Phase 7 row — status, and the correction that `suggestions` was Phase 3's all along
- QA catalog §9 — all nine `AUT-*` rows plus the six new ones closed or explicitly re-deferred with a
  stated reason