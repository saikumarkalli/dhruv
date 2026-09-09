# Finance calculations — Dhruv Finance

Every formula this app computes, where it is implemented, and the boundary conditions that are easy
to get wrong. Read `SKILL.md` first for money representation and the universe split.

**Contents**
1. Where floating point is allowed (and where the build stops you)
2. Net worth
3. Amortisation, payoff and prepayment — *shipped*
4. Budget pace
5. Savings rate and the monthly summary
6. Goal progress and earmarks
7. Debt payoff ordering — avalanche vs snowball
8. Insurance cover shortfall
9. Retirement corpus projection
10. Investment returns and XIRR — **blocked**
11. Remainder allocation and rounding

Sections 3 is implemented; the rest are specified but not yet built. Each says which, because citing
an unbuilt formula as though it exists is exactly the doc-drift failure this repo has hit before.

---

## 1. Where floating point is allowed

The rule is narrower than "never use `Double`", and the shipped code depends on the distinction.

| location | rule |
|---|---|
| `apps/finance/data/**/tracker/**/*.kt` | **No `Double`/`Float`.** `checkTrackerMoneyPrecision` fails the build (`DAT-BR-008`). This is stored money and running totals |
| feature modules (projections, derived insights) | `Double` is fine, and is what ships. `AmortisationMath.kt` uses `ln`/`ceil`/`round` and says so in a comment naming the gate's scope |
| calculator engines | `BigDecimal`, per the app's long-standing convention |

The principle behind the boundary: **stored money and sums must be exact; a projection is already an
estimate.** A prepay saving of "₹4.86L" is a hypothesis about the future — float error is orders of
magnitude below its real uncertainty. A net-worth total is a fact, and drift there is unacceptable.

When a projection produces a paise figure that reaches a screen, round once at the end
(`round(x).toLong()`) and clamp to a sane floor — never let float noise emit a negative "saving".

---

## 2. Net worth

**BR-C4.** Computed server-side or from the latest-valuation view — never from a client cache.

```
net worth = Σ (latest valuation per asset holding)
          − Σ (latest outstanding per liability holding)
```

The trap: `finance.valuations` is **append-only**, so it holds every historical value of every
holding. `SELECT sum(value_paise) FROM valuations` double-counts every update the user has ever
made. Always resolve *latest per holding* first (`v_latest_valuation`), then sum.

`liabilities_meta` carries the loan terms (`rate_bps`, `emi_paise`); the **outstanding** balance is a
valuation row like any other. Liabilities are always shown **outstanding, not original** — a paid-down
₹50L home loan is a ₹31L liability, and showing ₹50L overstates debt by the entire amount repaid.

---

## 3. Amortisation, payoff and prepayment — *shipped*

`apps/finance/feature/home/networth/.../AmortisationMath.kt`.

### Monthly rate from basis points

```kotlin
monthlyRate = rateBps / 10_000.0 / 12.0
```

`rate_bps` is `CHECK (0..10000)`, so 7.35% p.a. is `735`. Dividing by 100 instead of 10000 is the
classic bug and produces a plausible-looking wrong answer rather than an obvious one.

### Remaining months

```
r > 0:   n = ln(EMI / (EMI − r·P)) / ln(1 + r)
r = 0:   n = P / EMI
```

where `P` = outstanding principal, `r` = monthly rate, `EMI` = monthly payment.

**Boundary condition that must be handled, not assumed away:** if `EMI ≤ r·P`, the payment does not
even cover the interest and the balance never shrinks. The formula's argument goes non-positive and
`ln` returns NaN. Return `null` and render an honest "can't project this" state — never a NaN, never
an absurd month count. The shipped code returns `null` in exactly four cases: no EMI terms, EMI ≤ 0,
outstanding ≤ 0, and this one.

Round the result **up** (`ceil`). A loan with 84.2 months remaining takes 85 payments.

### Prepayment saving

```
interest saved = EMI·n_current − EMI·n_new − extraPayment      (floored at 0)
months saved   = ceil(n_current) − ceil(n_new)                 (floored at 0)
```

Subtracting `extraPayment` is the part people forget: the user paid that money, so it is not a
saving. Without it the screen overstates the benefit by the size of the prepayment itself.

Assumes rate and EMI stay fixed for the remaining term. That assumption is a **derived estimate** and
must be labelled as such on screen (`platform/DESIGN-SYSTEM.md` §10, BR-E4).

---

## 4. Budget pace — *specified (003), not built*

**BR-E2.** The period is a calendar month. Pace is the **elapsed-day fraction of that period** —
not half the month, not a 30-day approximation.

```
elapsedFraction = daysElapsed / daysInMonth
spendFraction   = spentPaise / budgetPaise

ahead of pace  ⟺  spendFraction > elapsedFraction
```

Worked example, because the intuition is wrong: on day 20 of a 30-day month, "on pace" is **66.7%**,
not 50%. A user who has spent 60% of a budget on the 20th is *behind* pace — comfortably fine — and
a naive halfway check would warn them for no reason.

February has 28 or 29 days and 31-day months exist; deriving `daysInMonth` from the actual calendar
month is what makes the fraction correct. The per-category bar renders a **month-position marker** at
`elapsedFraction`, and a bar past that marker is what "ahead of pace" means visually.

Two rules that change the inputs:

- **BR-E3** — a category excluded from spend (e.g. `Investment`) contributes nothing to `spentPaise`
  or `budgetPaise`. Otherwise investing reads as overspending.
- **BR-D1** — transfers are not spend and never enter either figure.

Over-budget must be **stated in words and money** ("Over by ₹1,000 with 9 days left"), never carried
by colour alone (`platform/DESIGN-SYSTEM.md` §1, §9).

---

## 5. Savings rate and the monthly summary — *specified (005), not built*

```
surplus      = incomePaise − expensePaise
savingsRate  = surplus / incomePaise          (undefined when income = 0)
```

Transfers appear in neither term (BR-D1); the cashflow statement reports them separately under
`MOVED, NOT SPENT` precisely so they cannot inflate either side.

**Zero income is a real state**, not an error — a month with no income has no meaningful savings
rate. Render the empty/undefined state rather than `0%`, which reads as "you saved nothing" when the
truth is "there was nothing to save from".

A negative surplus is also legitimate (a month of spending down savings). The ring and the copy must
handle a negative rate without clamping it to zero — clamping hides exactly the month the user most
needs to see.

### Cashflow reconciliation

The statement must visibly balance on screen:

```
opening balance + money in − money out ± moved-not-spent = closing balance
```

If it does not reconcile, the bug is in the query, not in the presentation — do not paper over a
gap with a "rounding" line.

---

## 6. Goal progress and earmarks — *specified (003), not built*

**BR-E1.** Progress is the current value of **linked holdings**. Linking never moves, locks or
duplicates money — the same rupee stays in exactly one holding and simply counts toward a goal.

```
saved = Σ over linked holdings of ( latestValuationPaise × earmarkBps / 10000 )
```

`earmark_bps` is 1–10000, where **10000 means the whole holding**. Partial earmarks are how "56 g of
this gold is for the house deposit" is represented.

Derived figures:

```
stillNeeded  = max(0, targetPaise − saved)
monthsLeft   = whole months from today to targetDate
perMonth     = stillNeeded / monthsLeft        (monthsLeft > 0)
```

Three states the screen must distinguish, because they mean different things: **on track**, **needs
₹X/mo to hit \<date\>**, and **no funding linked yet**. The third is not zero progress — it is an
unconfigured goal, and showing it as 0% invites the user to "fix" a goal that is merely empty.

A past target date makes `monthsLeft` zero or negative; say the date has passed rather than dividing.

---

## 7. Debt payoff ordering — *specified (003), not built*

Two strategies over the same liability set:

| strategy | order by | rationale |
|---|---|---|
| **Avalanche** | highest `rate_bps` first | mathematically optimal — least total interest |
| **Snowball** | smallest outstanding first | behavioural — earliest visible wins |

Both assume minimum payments on everything, with `extraPerMonth` applied entirely to the current
target, rolling to the next debt as each clears. Project each ordering with §3's amortisation and
compare the totals.

**The trade-off is shown, not hidden** (E6). When the user picks the behaviourally-easier order, the
screen states what it costs *and* what it buys, in the same sentence — e.g. *"Snowball clears the
smallest balance first — 6 months slower here, ₹41,800 more interest, but two wins in year one."*

Presenting snowball as simply worse is a real design failure: it is chosen deliberately by people for
whom the extra interest is a fair price for not giving up.

---

## 8. Insurance cover shortfall — *specified (003), not built*

The rule of thumb is stated on screen with its formula visible, so the user can disagree with it:

```
suggestedCover = 10 × annualIncome + outstandingLoans
shortfall      = max(0, suggestedCover − currentLifeCover)
```

`outstandingLoans` comes from the Position universe (liability holdings), not from the ledger. It is
a heuristic, labelled as one — never presented as a required amount.

The renewal banner needs `daysRemaining` **and the lapse consequence**; a date alone does not convey
that health cover lapsing means a fresh waiting period.

---

## 9. Retirement corpus projection — *specified (003), not built*

Three scenarios (Base / Optimistic / Cautious), each varying the return and inflation assumptions.
`BigDecimal` is appropriate here — it is a projection engine, not tracker money.

```
yearsToRetirement = retireAtAge − currentAge
spendAtRetirement = monthlySpendToday × (1 + inflation)^yearsToRetirement
yearsInRetirement = lifeExpectancy − retireAtAge

requiredCorpus    = present value of an inflation-adjusted annuity of
                    spendAtRetirement over yearsInRetirement,
                    discounted at the POST-retirement return

projectedCorpus   = futureValue(currentCorpus, preRetirementReturn, yearsToRetirement)
                  + futureValue of monthly contributions over the same period

shortfall         = max(0, requiredCorpus − projectedCorpus)
```

Two returns, deliberately: money compounds at the **pre-retirement** rate while working and is drawn
down at the more conservative **post-retirement** rate. Using one rate for both is the standard error
and overstates the corpus.

**BR-E4 is not optional here.** Every assumption — retire-at age, monthly spend today, inflation,
pre- and post-retirement return, life expectancy — renders on the same screen as the answer. A
corpus figure without its assumptions is a number the user cannot evaluate, argue with, or trust.

---

## 10. Investment returns and XIRR — **blocked, do not implement**

XIRR appears on C3 (holding detail) and in F5's "More" reports. It is **blocked on an accepted
decision record** and must not be implemented before that record lands:

- `NW-BR-007` (C3's stat) is open, "not silently dropped".
- 005's sub-phase 5f is gated on the same decision.

The substantive gap is not the algorithm — it is the **input**. XIRR needs the dated cashflow set for
a holding (every buy, sell and dividend), and **no phase currently models a holding↔transaction
link**. Without it there is nothing to compute over.

What *is* available today:

```
simpleReturn = (latestValuationPaise − investedPaise) / investedPaise
```

`invested_paise` is a real column. Ship the simple return, label it as a simple return, and leave
XIRR out — an XIRR computed over a guessed cashflow set is worse than an absent one, because it
looks authoritative.

When the decision record is written, it must settle: which cashflows count, how a partial sale is
treated, what happens with under a year of history, and how the holding↔transaction link is stored.

---

## 11. Remainder allocation and rounding

Integer paise means division leaves a remainder that must go somewhere explicit.

```kotlin
// splitting totalPaise across n parts
val base = totalPaise / n
val remainder = totalPaise - base * n     // 0 until n-1 paise
// assign `remainder` deliberately — conventionally to the first (or largest) part
```

The invariant to test directly: **the parts sum to the original, exactly.** 008 makes this
user-visible for split transactions — parts that do not add to the stated total cannot be saved, a
part cannot be zero or negative, and a one-part "split" is an ordinary transaction.

For proportional allocation (a category share, a nominee percentage), compute in basis points and
apply the same rule: the shares sum to 10000, with the remainder assigned to one named part.

### Display rounding

`Paise.format`/`Paise.formatCompact` round **DOWN**, deliberately. A displayed total that rounded up
could exceed the sum of the rows beneath it, and a user who adds up the rows would find the app
disagreeing with itself.

Round for display only, at the edge. Never round an intermediate value and carry it into a further
calculation — that is how a ₹1,00,000 total becomes ₹99,999.94 across twelve rows.

---

## Related

- `../SKILL.md` — money representation, the universe split, the business-rule index
- `business-rules.md` — full `BR-*` text
- `apps/finance/docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md` §5 groups
  C (net worth), D (money), E (plan), F (insights) — the authoritative statements
