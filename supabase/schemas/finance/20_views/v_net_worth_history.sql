-- v_net_worth_history — the month-end net-worth series behind Home's ▲/▼ delta and area sparkline,
-- and C1's centre delta.
--
-- Added by the 2026-08-23 readiness decisions §2.4 (maintainer default D-2). The Phase 2 spec
-- required a trend and a percentage change on its own headline screen while defining only
-- current-state views, so the delta had no source and "delta vs when" was undefined. Deferring it
-- to Phase 5 would have shipped Home's hero with a missing element three phases early.
--
-- SECURITY: `security_invoker = on` is mandatory — see v_latest_valuation.sql's header.
--
-- Derivation is "latest valuation ≤ date", the same rule Phase 5's report_balance_sheet(p_as_of)
-- uses — deliberately not a competing mechanism. Phase 5 may read this view instead of re-deriving.
--
-- Window is the trailing 24 month-ends. The current month's point is clamped to today rather than
-- to a future month-end, so the newest point always means "now".
--
-- Cost: O(months × holdings) index lookups, served by valuations_holding_id_as_of_idx. That is
-- fine at personal-finance scale (tens of holdings). If a user ever reaches thousands of holdings,
-- this becomes a materialized view refreshed on write — not a reason to complicate it today.
create or replace view finance.v_net_worth_history
with (security_invoker = on) as
with months as (
    select least(
        (date_trunc('month', d) + interval '1 month - 1 day')::date,
        current_date
    ) as as_of
    from generate_series(
        date_trunc('month', current_date) - interval '23 months',
        date_trunc('month', current_date),
        interval '1 month'
    ) as d
)
select
    h.user_id,
    m.as_of,
    coalesce(sum(lv.value_paise) filter (where h.kind = 'ASSET'), 0)     as assets_paise,
    coalesce(sum(lv.value_paise) filter (where h.kind = 'LIABILITY'), 0) as liabilities_paise,
    coalesce(sum(lv.value_paise) filter (where h.kind = 'ASSET'), 0)
        - coalesce(sum(lv.value_paise) filter (where h.kind = 'LIABILITY'), 0) as net_paise
from months m
join finance.holdings h
    on h.deleted_at is null
-- A holding with no valuation on or before this month-end did not exist yet, so it contributes
-- nothing to that point. `cross join lateral` + `limit 1` drops it, which is the intended behaviour.
cross join lateral (
    select v.value_paise
    from finance.valuations v
    where v.holding_id = h.id
      and v.deleted_at is null
      and v.as_of <= m.as_of
    order by v.as_of desc, v.created_at desc
    limit 1
) lv
group by h.user_id, m.as_of;

grant select on finance.v_net_worth_history to authenticated;