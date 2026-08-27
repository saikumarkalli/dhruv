-- v_latest_valuation — the current value of every non-deleted holding.
--
-- SECURITY: `security_invoker = on` is mandatory and is the single most important line in this
-- file. A Postgres 15+ view executes as its OWNER by default, which bypasses RLS on the underlying
-- tables — and PostgREST exposes this view. Without the clause, every signed-in caller reads every
-- user's holdings. `supabase db diff` cannot express security-invoker views (ADR-0032 decision 4's
-- documented caveat list), so after generating the migration, confirm by hand that the clause
-- survived, and keep the RLS test that asserts a second user reads zero rows here.
--
-- "Latest" = highest `as_of`, ties broken by `created_at` — two valuations dated the same day are
-- legitimate (a correction appended the same day), and the later-written one wins.
-- Soft-deleted valuations (written only by finance.correct_valuation()) are excluded, so a
-- corrected row stops being latest the moment its replacement lands.
create or replace view finance.v_latest_valuation
with (security_invoker = on) as
select distinct on (v.holding_id)
    v.holding_id,
    h.user_id,
    v.id as valuation_id,
    v.value_paise,
    v.as_of,
    v.source,
    v.created_at
from finance.valuations v
join finance.holdings h on h.id = v.holding_id
where v.deleted_at is null
  and h.deleted_at is null
order by v.holding_id, v.as_of desc, v.created_at desc;

grant select on finance.v_latest_valuation to authenticated;