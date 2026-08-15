# Calc tab

Modules reached from the **Calc** tab.

| Module | Status |
|---|---|
| [calculator](calculator/README.md) | live |

Calc is the smallest tab — it owns only the keypad + calculation history (Room-local, works fully
offline per the functional spec's route map: "Calc › Offline · Room-local"). The four everyday
calculators (Loan, SIP, Tax, Everyday maths) look similar in kind but are **Plan-tab owned**, not
Calc — see `apps/finance/feature/plan/README.md`. That split is deliberate: Calc is the raw keypad
utility; Plan is where those same calculators sit alongside the live planning modules (budgets,
goals, retirement) they inform.
