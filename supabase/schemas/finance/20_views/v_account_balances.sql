-- v_account_balances — per-account current balance, server-side (NFR-8: the client never sums a
-- ledger to draw a screen). Balance = opening_balance_paise + signed sum of non-deleted
-- transactions: INCOME +, EXPENSE -, TRANSFER - on account_id and + on to_account_id.
--
-- SECURITY: `security_invoker = on` is mandatory (constitution Article IXa) — a Postgres 15+ view
-- otherwise executes as its owner and bypasses RLS on finance.accounts/finance.transactions,
-- returning every user's account balances to every signed-in caller through PostgREST. `supabase
-- db diff` cannot express this clause (ADR-0032 caveat list) — confirm by hand it survives in the
-- generated migration, and the RLS test must assert a second user reads zero rows from this view,
-- not only from the underlying tables (same class of defect the 2026-08-22 audit found in 001).
--
-- `counts_as_spendable` backs FR-017's "spendable now" — a filtered sum of this view's rows, not a
-- rule the client re-derives. Credit accounts land negative naturally (BR-D2): a CREDIT_CARD
-- account's transactions are typically EXPENSE against it, so the signed sum goes negative with no
-- special case needed.
create or replace view finance.v_account_balances
with (security_invoker = on) as
select
    a.id as account_id,
    a.user_id,
    a.type,
    a.type in ('BANK', 'CASH', 'WALLET') as counts_as_spendable,
    a.opening_balance_paise
        + coalesce(sum(
            case
                when t.account_id = a.id and t.type = 'INCOME' then t.amount_paise
                when t.account_id = a.id and t.type = 'EXPENSE' then -t.amount_paise
                when t.account_id = a.id and t.type = 'TRANSFER' then -t.amount_paise
                when t.to_account_id = a.id and t.type = 'TRANSFER' then t.amount_paise
                else 0
            end
        ), 0) as balance_paise
from finance.accounts a
left join finance.transactions t
    on (t.account_id = a.id or t.to_account_id = a.id)
    and t.deleted_at is null
where a.deleted_at is null
group by a.id, a.user_id, a.type, a.opening_balance_paise;

grant select on finance.v_account_balances to authenticated;
