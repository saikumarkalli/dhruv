# Finance business rules — full text

The authoritative source is
`apps/finance/docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md` §5, stated per
screen group. Reproduced here with the failure mode each one prevents, because the rule alone rarely
explains why it is written the way it is.

---

## First: two id systems, do not confuse them

| shape | what it is | where |
|---|---|---|
| `BR-C1`, `BR-D4`, `BR-E2`, `BR-G1` | **Business rules.** Domain law. A screen may not contradict one | functional spec §5, per group |
| `NW-BR-003`, `DAT-BR-008`, `ONB-BR-001`, `AUT-FLOW-004` | **QA scenario rows** — `<MODULE>-BR-nnn` = module, business-rule scenario #n; `-FLOW-` = a chained flow | `2026-08-09-qa-test-scenario-catalog.md` |

A QA row is a *test* of something, and its `Source` column names the `BR-*` or screen it tests. So
`NW-BR-003` is not a fourth net-worth business rule — it is the catalog's scenario proving `BR-C1`
holds for corrections. Citing a QA id as though it were domain law (or vice versa) makes a spec
untraceable.

`DAT-BR-008` is the one worth memorising anyway: no `Double`/`Float` under
`apps/finance/data/**/tracker/**`, enforced by `checkTrackerMoneyPrecision`.

---

## Group C — Net worth

> **BR-C1** Valuations are **append-only**. No update path exists for a valuation row.
> Corrections = soft-delete + append (ADR-0014 §5).

Enforced at the database, not by client discipline: `finance.valuations` carries SELECT and INSERT
policies only. Since setting `deleted_at` is itself an UPDATE, the client cannot soft-delete either —
a correction is a `security definer` RPC (`finance.correct_valuation()`) that soft-deletes and
appends in one transaction.

*Prevents:* history being silently rewritten, which makes every trend chart and month delta a lie
about the past. *Never fix a blocked correction by adding an UPDATE policy* — that makes the table
ordinarily mutable and destroys the guarantee. This exact contradiction ("append-only" **and** "the
client marks the row deleted") shipped into three documents before an audit caught it.

> **BR-C2** Creating a holding writes its first valuation row **atomically**.

Both or neither, via `finance.create_holding_with_value()`.

*Prevents:* a holding with no valuation. Every downstream sum treats it as zero, so a ₹12L holding
silently contributes nothing and the user sees a net worth that is wrong by exactly that amount, with
nothing on screen to indicate it.

> **BR-C3** Sector/liability-type enums persist as TEXT and are **append-only** — never rename a
> shipped constant.

Frozen enums get a `CHECK` constraint, not prose. Adding a value is a migration; that cost is
intended.

*Prevents:* orphaning every existing row that holds the old string. There is no foreign key to catch
it and no test that goes red — the rows simply stop matching anything.

> **BR-C4** Net worth = Σ latest asset valuations − Σ latest liability outstandings, computed
> server-side or from the latest-valuation view, never from a stale client cache.

*Prevents:* two things. A stale cache renders a confident wrong total; and because the table is
append-only, naively summing `value_paise` double-counts every historical update the user has ever
made. Resolve latest-per-holding first, then sum.

Liabilities are always **outstanding, not original** — a paid-down ₹50L home loan is a ₹31L
liability, and showing the original overstates debt by the entire amount repaid.

---

## Group D — Money

> **BR-D1** Three types: `EXPENSE`, `INCOME`, `TRANSFER`. **Transfers are never spend** — excluded
> from expense totals, budgets and category shares, and reported separately (F2 "MOVED, NOT SPENT").

*Prevents:* the single most damaging finance bug available here. Moving ₹50,000 into your own savings
account reads as a ₹50,000 expense: every budget blows, the savings rate inverts, and the category
breakdown is nonsense. The user's most responsible act becomes their worst-looking month.

The cashflow statement reports transfers as their own line precisely so they are visible without
being counted.

> **BR-D2** Credit-card accounts hold **negative** balances and are excluded from "spendable now".

`SPENDABLE NOW` is bank + cash + wallet only. Cards group under `CREDIT — OWED, NOT HELD`.

*Prevents:* rendering debt as available money — the most consequential possible sign error, since it
directly encourages the user to spend money they do not have.

> **BR-D3** Category rename preserves history. **Merge is irreversible** and must be confirmed with a
> danger dialog stating the transaction count moved.

*Prevents:* a silent, unrecoverable bulk mutation of a person's records. The count in the dialog is
the load-bearing part — "this moves 412 transactions" is a decision; "merge these categories?" is not.

> **BR-D4** Recurring templates never post silently into the ledger — they post into the **review
> queue** (G2) and require acceptance.

*Prevents:* the app inventing transactions that never happened — a rent template still firing after
the user moved out, quietly accumulating phantom spend.

> **BR-D5** Every mutation appends to the transaction's audit trail (D4 HISTORY).

`Created manually on this device`, `Category changed Shopping → Groceries`.

*Prevents:* the unanswerable question. Without it, "why does this say Groceries now" has no answer,
and a user who cannot explain their own data stops trusting all of it.

---

## Group E — Plan

> **BR-E1** Goal progress = Σ current value of **linked holdings** (whole or earmarked fraction).
> Linking never moves, locks or duplicates money.

`earmark_bps` 1–10000, where 10000 is the whole holding.

*Prevents:* two opposite failures — the same rupee counted toward two goals *and* toward net worth
(inflation), or money the user believes has been "moved into a goal" and can no longer locate
(panic). The screen states the rule verbatim: *"A goal's progress is the value of the holdings you
link to it. Nothing is moved or locked."*

> **BR-E2** Budget period is a calendar month; pace = elapsed-days fraction of the period.

Not half the month, not 30 days. On day 20 of a 30-day month, on-pace is 66.7%.

*Prevents:* warning a user who is comfortably fine (60% spent on the 20th is *behind* pace), which
teaches them to ignore the warning that matters.

> **BR-E3** Categories excluded from spend (e.g. `Investment`) are excluded from budgets too.

*Prevents:* investing reading as overspending — the app punishing the exact behaviour it exists to
encourage.

> **BR-E4** Every projection screen must display its assumptions on the same screen (E9), and every
> AI/derived insight is labelled as such.

*Prevents:* a guess being read as a fact. A retirement corpus, a prepay saving and an XIRR are all
model outputs; on a screen of otherwise-real numbers they inherit unearned authority. Assumptions on
the same screen are what let the user disagree, which is the only thing making the number useful.

---

## Group G — Automation

> **BR-G1** No automated source ever writes directly to the ledger. Every suggestion passes through
> G2 and an explicit user action.

The hub states this to the user verbatim: *"Every source below only suggests. You approve each entry
before it becomes part of your records."*

*Prevents:* the app's headline promise becoming false. Note the non-obvious corollary: a fetched
gold/silver price arrives as a **proposed value update in the review queue**, not a direct valuation
write. BR-G1 literally only forbids automated writes to the *ledger*, so a direct valuation write
would have been technically legal — and would have made the hub's own header a lie. The queue
therefore carries two proposal kinds, and code assuming one kind breaks on the other.

> **BR-G2** SMS parsing happens **on device**; raw SMS never leaves the device.

Reinforced by `AUT-BR-002`: raw message text may not appear in any outbound Supabase payload. This is
why bank-message proposals stay entirely device-local (Room), and why `finance.suggestions.raw_text`
— reserved by an earlier phase — stays permanently unwritten.

*Prevents:* a DPDP breach, and the far worse version where it is discovered after shipping.

> **BR-G3** A learned rule is user-visible, counted, and revocable.

*Prevents:* invisible behaviour. A rule that silently recategorises is indistinguishable from a bug,
and a user who cannot see or switch it off has no recourse but to distrust every categorisation.

---

## Cross-cutting rules that are not `BR-*` but bind just as hard

| rule | source | failure mode |
|---|---|---|
| Money is `Long` paise; proportions are integer basis points | ADR-0014 §4 | drift; whole-percent cannot express three equal nominees |
| INR only — no `currency` column exists anywhere, by design | ADR-0029 decision 4 | adding one invites false flexibility the schema does not have |
| No client `DELETE` policy on tracker tables; erasure is `public.delete_my_data()` / `delete_my_account()` | ADR-0029 decision 5 | unauditable deletion paths; a missed table silently breaks the 7-day guarantee |
| Views need `security_invoker = on` | ADR-0032 caveats | the view runs as owner, bypasses RLS, returns **every user's rows to every signed-in caller**. An audit found all 8 planned views missing it |
| Every tracker request sends `Accept-Profile: finance` | ADR-0033 correction | silently 404s against the empty `public` schema — no loud failure |
| Consent gates the network structurally, via `ConsentInterceptor` on the one PostgREST client | ADR-0029 decision 2 | a second client would route around the only DPDP gate in the app |
| No colour-only meaning; every delta carries ▲/▼ or a label | DESIGN-SYSTEM §1, §9 | unreadable for colour-blind users and in monochrome contexts |
| Money never ellipsised — wrap or compact-format | DESIGN-SYSTEM §2 | `₹1,24,50…` is worse than no number |

---

## Related

- `../SKILL.md` — money representation, the universe split, the wrong-by-default checklist
- `calculations.md` — the formulas these rules constrain
- `2026-08-09-qa-test-scenario-catalog.md` — the scenario rows that prove each rule holds
