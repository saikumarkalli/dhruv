-- v_net_worth_by_sector — C1's donut and ranked legend, and the source of the net-worth total.
--
-- SECURITY: `security_invoker = on` is mandatory — see the header of v_latest_valuation.sql for
-- why. `db diff` cannot express it; verify it survived into the generated migration.
--
-- BR-C4: net worth = Σ latest asset valuations − Σ latest liability outstandings, computed here
-- and never from a client-side cache. A liability holding's "outstanding" is its latest valuation
-- (C6's "outstanding, not original" rule) — which is why liabilities need no separate sum column.
create or replace view finance.v_net_worth_by_sector
with (security_invoker = on) as
select
    h.user_id,
    h.kind,
    h.sector,
    count(*) as holding_count,
    sum(lv.value_paise) as value_paise
from finance.holdings h
join finance.v_latest_valuation lv on lv.holding_id = h.id
where h.deleted_at is null
group by h.user_id, h.kind, h.sector;

grant select on finance.v_net_worth_by_sector to authenticated;