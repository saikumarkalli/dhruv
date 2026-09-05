-- v_month_summary — per user per calendar month: income, expense, excluded, transfer totals.
-- Backs D1's pinned summary (FR-011). NFR-8: server-side aggregation, never a client-side sum.
--
-- SECURITY: `security_invoker = on` is mandatory (constitution Article IXa) — see
-- v_account_balances.sql's header for the full rationale; identical reasoning applies here.
--
-- `expense_paise` excludes TRANSFER rows (BR-D1, research R3) and rows in `excluded_from_spend`
-- categories (FR-025) — encoded once here so Phase 4 (budgets) and Phase 5 (insights) inherit the
-- rule instead of each re-implementing it. `excluded_paise` keeps that excluded money visible so
-- D1's saved-percentage arithmetic is explainable rather than mysteriously short.
create or replace view finance.v_month_summary
with (security_invoker = on) as
select
    t.user_id,
    date_trunc('month', t.occurred_at)::date as month,
    coalesce(sum(t.amount_paise) filter (where t.type = 'INCOME'), 0) as income_paise,
    coalesce(sum(t.amount_paise) filter (
        where t.type = 'EXPENSE'
          and not coalesce(c.excluded_from_spend, false)
    ), 0) as expense_paise,
    coalesce(sum(t.amount_paise) filter (
        where t.type = 'EXPENSE' and coalesce(c.excluded_from_spend, false)
    ), 0) as excluded_paise,
    coalesce(sum(t.amount_paise) filter (where t.type = 'TRANSFER'), 0) as transfer_paise
from finance.transactions t
left join finance.categories c on c.id = t.category_id
where t.deleted_at is null
group by t.user_id, date_trunc('month', t.occurred_at)::date;

grant select on finance.v_month_summary to authenticated;
