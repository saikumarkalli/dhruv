-- v_category_spend — per user, month and category: total spend and its share of that month's
-- total expense. Backs D8's per-row spend/share (FR-022) and, from Phase 4, budget consumption.
--
-- SECURITY: `security_invoker = on` is mandatory (constitution Article IXa) — see
-- v_account_balances.sql's header for the full rationale; identical reasoning applies here.
--
-- Same two exclusions as v_month_summary: TRANSFER rows and `excluded_from_spend` categories never
-- contribute (BR-D1, FR-025). Share is a row's fraction of its OWN kind's month total (an expense
-- category's share of expense_paise; an income category's share of income_paise) — sharing one
-- denominator across both kinds would make an income category's "share" a fraction of spend, which
-- is not a meaningful number.
create or replace view finance.v_category_spend
with (security_invoker = on) as
select
    t.user_id,
    date_trunc('month', t.occurred_at)::date as month,
    t.category_id,
    c.name as category_name,
    c.kind as category_kind,
    c.excluded_from_spend,
    sum(t.amount_paise) as spend_paise,
    case
        when c.kind = 'EXPENSE' and ms.expense_paise > 0
            then round((sum(t.amount_paise)::numeric / ms.expense_paise) * 100, 1)
        when c.kind = 'INCOME' and ms.income_paise > 0
            then round((sum(t.amount_paise)::numeric / ms.income_paise) * 100, 1)
        else 0
    end as share_percent
from finance.transactions t
join finance.categories c on c.id = t.category_id
join finance.v_month_summary ms
    on ms.user_id = t.user_id and ms.month = date_trunc('month', t.occurred_at)::date
where t.deleted_at is null
  and t.type in ('EXPENSE', 'INCOME')
group by t.user_id, date_trunc('month', t.occurred_at)::date, t.category_id, c.name, c.kind,
    c.excluded_from_spend, ms.expense_paise, ms.income_paise;

grant select on finance.v_category_spend to authenticated;
