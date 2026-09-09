---
name: dhruv-finance-domain
description: Subject-matter rules for building anything that touches money in the Dhruv Finance app — net worth, holdings, valuations, ledger, transactions, accounts, categories, budgets, goals, debt payoff, insurance, retirement, cashflow, P&L, balance sheet, reports, automation proposals. Use whenever a task involves an amount, a balance, a total, a percentage, a period, a projection, or a financial calculation, and whenever you see "paise", "net worth", "budget", "spend", "savings rate", "XIRR", "EMI", "amortisation", "reconcile", "as at", "financial year", or a `BR-*` rule id. Read this BEFORE writing the money model, the query, the calculation or the screen — most rules here exist because an audit found them violated in shipped code or contradicted across three documents. Also use when reviewing a finance change for correctness.
---

# Dhruv Finance Domain

The finance rules that are load-bearing and non-obvious. Almost nothing here is generic
personal-finance advice — it is the specific set of decisions this app has already made, plus the
specific ways they have already been broken.

Two things make finance bugs unusually expensive here, and they shape everything below:

1. **Money errors are silent.** A float rounding drift, a transfer counted as spend, a view that
   bypasses RLS — none of them throw. The app keeps rendering a confident number that is wrong.
2. **The data is append-only and user-owned.** A bad write is not a bug you fix; it is a row in a
   person's financial history that now needs a correction path (§5) and a DPDP story (§8).

## Before you write anything

1. **Decide which money universe you are in** (§2). Getting this wrong is the single most common
   design error in this app, and it is invisible until two screens disagree.
2. **Read the phase's `data-model.md`** in `apps/finance/specs/NNN-*/`. It, not this skill, decides
   what columns exist.
3. **Find the `BR-*` ids your change touches.** The functional spec
   (`apps/finance/docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md` §5) states
   them per screen group. §3 below is the index; `references/business-rules.md` is the full text.
4. **If you are calculating anything**, read `references/calculations.md` first — the formulas are
   already fixed, and several are fixed in a non-obvious way (pace, avalanche, cover shortfall).

---

## 1. Money representation

### Tracker money is `Long` paise. Always.

`bigint` in Postgres, `Long` in Kotlin, integer paise end to end — no `numeric`, no `Double`, no
`Float`, no `BigDecimal` (ADR-0014 §4). Exact, summable, no accumulated drift across a year of rows.

This is enforced, not merely documented: `checkTrackerMoneyPrecision` (root `build.gradle.kts`)
fails the build on `\b(Double|Float)\b` anywhere under `apps/finance/data/**/tracker/**/*.kt`, and
`regressionCheck` depends on it. That gate exists because prose alone did not hold.

**The gate's scope is narrower than "never use `Double`", and the shipped code depends on that.**
Three tiers, and knowing which you are in matters:

| where | use | why |
|---|---|---|
| `apps/finance/data/**/tracker/**` | `Long` paise only — gate fails the build | stored money and running totals must be exact |
| feature-module projections | `Double` is fine, and is what ships | the output is already an estimate |
| calculator engines | `BigDecimal` | long-standing convention for these modules |

`AmortisationMath.kt` is the shipped precedent for the middle tier: it uses `ln`/`ceil`/`round` and
carries a comment naming the gate's scope so the next reader does not "fix" it. The principle is
that a prepay saving of "₹4.86L" is a hypothesis about the future — float error sits orders of
magnitude below its real uncertainty — whereas a net-worth total is a fact.

Two things that still hold: never let float reach a stored column, and never convert tracker paise to
`BigDecimal` just to reuse a calculator helper. Convert at the boundary, inside the calculator's own
module. When a projection emits a paise figure for a screen, round once at the end and clamp — float
noise must never surface as a negative "saving".

### Proportions are integer basis points, never percent, never float

`rate_bps integer CHECK (rate_bps between 0 and 10000)`; `earmark_bps` 1–10000 where **10000 means
the whole holding**.

Whole-percent columns look sufficient until you try to represent three equal nominees, or a 7.35%
interest rate, or a partial gold earmark. Basis points cost nothing and remove the class of problem.

### Rendering goes through `Paise`, never a local formatter

`libs/core/src/main/kotlin/com/dhruv/core/format/Paise.kt` is deliberately Compose-free so
notifications, widgets and PDF export use the same renderer as screens:

| call | output | use |
|---|---|---|
| `Paise.format(paise, showDecimals, masked)` | `₹18,42,600.00` | lists, sheets, history, PDF export |
| `Paise.formatCompact(paise, masked)` | `₹85.4K` · `₹18.4L` · `₹2.1Cr` | cards, hero stats, widgets |
| `Paise.MASKED_TOKEN` | `₹••••` | privacy mode |

In Compose, render through `MoneyText` (which takes a pre-formatted string) rather than calling
`Paise` inline — that is what keeps tabular numerals and the hero/row/inline variants consistent.

Three properties are deliberate and easy to break by reimplementing:

- **Indian grouping comes from `NumberFormat` with `Locale("en","IN")`**, not a hand-built
  `DecimalFormat` pattern. Only the locale's own data reliably alternates 3-then-2 digits.
- **Rounding is `RoundingMode.DOWN`.** A displayed total that rounds *up* can exceed the sum of the
  rows beneath it, and a user reading both will trust neither.
- **The mask is fixed width.** A mask that scaled with the value would leak the digit count, which
  defeats masking.

### Money is never ellipsised

Wrap, or switch to compact format. A truncated `₹1,24,50…` is worse than no number
(`platform/DESIGN-SYSTEM.md` §2).

### Allocation must reconcile exactly

Whenever one amount is divided into parts — a split transaction, a category share, an earmark — the
parts must sum to the original paise **exactly**. Integer division leaves a remainder; assign it
deliberately (conventionally to the largest part, or the first) rather than letting it vanish.

008 states the user-facing half of this: a split whose parts do not add to the stated total cannot
be saved, a part cannot be zero or negative, and a "split" with one part is an ordinary transaction,
not a split.

---

## 2. The money universes — do not conflate them

**This is the finding most worth internalising.** 005's research R11 records it after two phases had
already been specified as though the numbers should reconcile:

> *Net worth (holdings) and cashflow balances (accounts) are two different money universes and are
> not required to agree.* Each reconciles internally, and that is all any spec should require.

| universe | records | shape | asked "what?" | owner |
|---|---|---|---|---|
| **Position** | `finance.holdings` + `finance.valuations` | point-in-time, **append-only** | *what do I own and owe, right now* | net worth, balance sheet |
| **Flow** | accounts + transactions | period-bounded, mutable with audit trail | *what moved, over this period* | ledger, budgets, cashflow, P&L |
| **Projection** | nothing persisted | `BigDecimal`, hypothetical | *what would happen if* | calculators, retirement, prepay insight |

Practical consequences that catch people:

- Buying a mutual fund is a **transfer** in the Flow universe (money left a bank account) and a
  **new valuation** in the Position universe. It is not spend, and the two records are not derived
  from each other.
- "Spendable now" is a Flow concept over bank/cash/wallet accounts only. Holdings are not spendable
  and never appear in it.
- A balance sheet (Position, as-at a date) and a cashflow statement (Flow, over a period) landing on
  different totals is **correct behaviour**, not a bug to reconcile away.
- Never write a query, a test, or an acceptance criterion asserting that a Position total equals a
  Flow total.

---

## 3. Business-rule index

Full text and rationale: `references/business-rules.md`. The one-line version, because these are the
rules most often broken by someone who never read the spec section that states them:

| id | rule | what breaks if you ignore it |
|---|---|---|
| **BR-C1** | Valuations are append-only; no update path exists. Correction = soft-delete + append | history silently rewritten; trend charts lie about the past |
| **BR-C2** | Creating a holding writes its first valuation row **atomically** | a holding with no value, which every downstream sum treats as zero |
| **BR-C3** | Sector/type enums persist as TEXT and are **append-only forever** | renaming a shipped constant orphans every existing row |
| **BR-C4** | Net worth = Σ latest asset valuations − Σ latest liability outstandings, computed server-side or from the latest-valuation view | a stale client cache renders a confident wrong net worth |
| **BR-D1** | Three types: `EXPENSE`, `INCOME`, `TRANSFER`. **Transfers are never spend** | moving ₹50k to your own savings shows as a ₹50k expense and blows every budget |
| **BR-D2** | Credit-card accounts hold **negative** balances and are excluded from "spendable now" | card debt renders as available money |
| **BR-D3** | Category rename preserves history; **merge is irreversible** and needs a danger dialog stating the count moved | silent, unrecoverable bulk mutation of a person's records |
| **BR-D4** | Recurring templates never post silently — they post into the review queue and need acceptance | the app invents transactions the user never made |
| **BR-D5** | Every mutation appends to the transaction's audit trail | "why does this say Groceries now" has no answer |
| **BR-E1** | Goal progress = Σ current value of **linked** holdings (whole or earmarked). Linking never moves, locks or duplicates money | the same rupee counted twice, or money the user cannot find |
| **BR-E2** | Budget period is a calendar month; **pace = elapsed-days fraction** of that period | see `references/calculations.md` — "50% spent on the 20th" is not ahead of pace |
| **BR-E3** | Categories excluded from spend (e.g. `Investment`) are excluded from budgets too | investing looks like overspending |
| **BR-E4** | Every projection displays its assumptions on the same screen, and derived/AI output is labelled as derived | a guess reads as a fact |
| **BR-G1** | **No automated source ever writes to the ledger.** Every suggestion passes through the review queue and an explicit user action | the app's core promise is false |
| **BR-G2** | SMS parsing is on-device; raw SMS never leaves the device | DPDP breach |
| **BR-G3** | A learned rule is user-visible, counted, and revocable | invisible behaviour the user cannot switch off |
| **DAT-BR-008** | No `Double`/`Float` under `tracker/**` | build fails (`checkTrackerMoneyPrecision`) |

---

## 4. Periods, dates and "as at"

Period handling is where finance code quietly goes wrong, because every rule is a boundary rule.

- **The financial year runs 1 April to 31 March** (005 FR-024). Never a calendar year, never
  inferred from the locale. A quarter selection is a *calendar* quarter unless a spec says otherwise.
- **One period model for the whole Insights tab.** The period survives navigation between
  statements — changing statement must not silently snap the user back to "this month".
- **"As at" resolves to the latest valuation ≤ the date**, not the valuation *on* the date. Most
  dates have no valuation row at all; this is what makes a balance sheet answerable for any date.
- **A Postgres view cannot take a period.** Reporting is therefore parameterised `finance.*`
  functions, not `v_cashflow`/`v_pnl`/`v_balance_sheet` views. Three plan documents specified views
  before anyone noticed a view has no arguments.
- **Comparison periods are like-for-like**: the same month last year, the same quarter last year —
  never the immediately preceding period, which compares February against January and calls the
  difference a trend.
- Store timestamps as `timestamptz`. A transaction's *date* is user-stated and may differ from the
  row's `created_at`; they are different facts and both matter to the audit trail.

---

## 5. Append-only, corrections, and the audit trail

`finance.valuations` has **SELECT and INSERT policies only** — no UPDATE, no DELETE. That is what
makes "history is never overwritten" true at the database layer rather than by client discipline.

The consequence people miss: **setting `deleted_at` is an UPDATE**, so on an append-only table the
client cannot soft-delete either. A correction is therefore a `security definer` RPC that
soft-deletes and appends in one transaction (`finance.correct_valuation()`), not a client write.

Never resolve this by adding an UPDATE policy. That makes the table ordinarily mutable and destroys
the guarantee the design exists for. A spec that says "append-only" *and* "the client marks the row
deleted" is self-contradictory — that exact contradiction shipped into three documents before an
audit caught it.

Schema mechanics for any of this: use the `dhruv-supabase-object` skill.

---

## 6. Automation proposes; it never writes

BR-G1 is the app's headline promise and the automation hub states it verbatim to the user. Two
non-obvious corollaries:

- A fetched gold/silver/currency price arrives as a **proposed value update in the review queue** —
  not a direct valuation write. BR-G1 only forbids automated writes to the *ledger*, so a direct
  valuation write would have been technically legal and would have made the hub's own header false.
- The queue therefore carries **two proposal kinds**: proposed transaction and proposed value
  update. Code that assumes one kind will break on the other.

---

## 7. Wrong by default — check these before claiming done

Each has actually happened in this repo or is one line away from happening:

- [ ] A transfer counted in an expense total, a budget, or a category share (BR-D1).
- [ ] A credit-card balance added to "spendable" instead of subtracted (BR-D2).
- [ ] Net worth summed from *all* valuation rows instead of the latest per holding (BR-C4). The
      table is append-only, so "sum the column" double-counts every historical update.
- [ ] Pace compared against 50% of the month instead of the elapsed-day fraction (BR-E2).
- [ ] A percentage stored as a float or a whole percent instead of basis points.
- [ ] Positive/negative meaning carried by colour alone — every delta needs a ▲/▼ glyph or label
      (`platform/DESIGN-SYSTEM.md` §1, §9).
- [ ] A view created without `security_invoker = on` — it runs as owner, bypasses RLS, and returns
      **every user's rows to every signed-in caller**. An audit found all 8 planned views across
      three phases missing it.
- [ ] A new user-data table not added to `public.delete_my_data()` in the same migration. The miss
      is silent: nothing fails, no test goes red, the app just stops honouring a legal obligation.
- [ ] A device-local Room table holding amounts, assumed to be covered by server-side erasure. It is
      not — Room is outside that function's reach by construction and needs its own purge.
- [ ] A tracker request without `Accept-Profile: finance` (`Content-Profile` on writes). It does not
      error; it silently 404s against the empty `public` schema.
- [ ] An empty state showing `₹0` where the truth is "no data yet". A zero that reads as a real
      figure is a lie; say so instead (003 FR-004).
- [ ] Money ellipsised, or rendered with a hand-rolled formatter instead of `Paise`/`MoneyText`.
- [ ] A split whose parts do not sum to the total, or whose remainder was dropped by integer
      division.

---

## 8. DPDP, in finance terms

- **Consent gates the network, structurally.** `ConsentInterceptor` is attached to the PostgREST
  client and short-circuits before dispatch; no other PostgREST-capable client exists in the app, so
  "no call before consent" is a property of the wiring rather than of every call site remembering
  (ADR-0029 decision 2). Do not construct a second client.
- **Erasure is two named functions**, `public.delete_my_data()` and `public.delete_my_account()` —
  never a client `DELETE` policy on a tracker table. That keeps every deletion auditable and is the
  whole 7-day erasure guarantee.
- **Amounts are personal data wherever they live** — including device-local Room, notification
  payloads and widget state. Each needs its own purge path.
- **Never put an account number, a policy number, or an account name plus an amount in the same
  notification line** under privacy mode (`platform/DESIGN-SYSTEM.md` §11).

---

## 9. Verify before you claim done

```bash
./gradlew regressionCheck          # includes checkTrackerMoneyPrecision + checkDesignTokenUsage
python scripts/db/gen_schema_docs.py equiv    # schemas/ vs migrations/ — CI runs this
```

Then reason about the numbers, because none of the above catches a wrong figure:

- Pick one real holding and hand-compute its contribution to net worth. Does the screen agree?
- Feed one transfer through the ledger. Does expense stay flat and the budget stay untouched?
- Take one budget on day 20 of a 30-day month. Is "on pace" 66.7%, not 50%?

Tests that assert a total without a hand-checked expected value prove only that the code agrees with
itself.

---

## Related

- `references/calculations.md` — the formulas: pace, savings rate, EMI, amortisation, avalanche vs
  snowball, cover shortfall, corpus projection, XIRR's blocked status, remainder allocation
- `references/business-rules.md` — full `BR-*` text with source and failure mode
- `dhruv-supabase-object` — schema, RLS, `security_invoker`, erasure wiring
- `dhruv-compose-screen` — rendering the numbers this skill computes
- ADR-0014 (Supabase-primary tracker, integer paise, append-only valuations) ·
  ADR-0029 (client architecture, consent interceptor, erasure functions, INR-only) ·
  ADR-0033 (per-app Postgres schema) · `platform/DESIGN-SYSTEM.md` §1–§2, §9–§11
