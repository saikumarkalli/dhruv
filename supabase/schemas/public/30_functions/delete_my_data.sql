-- delete_my_data — declarative state (ADR-0032). Runs `security definer` so it can act across a
-- user's own rows without a service-role key or an Edge Function — callable only by the signed-in
-- user, always scoped to auth.uid(). Stays in `public` (ADR-0033): this is the cross-app erasure
-- orchestrator, not a Finance-domain object — it will grow calls into other apps' schemas
-- (`tools.delete_my_data()`, etc.) as they add Supabase-backed data, while each app's own tables
-- stay namespaced under that app's schema.
--
-- IMPORTANT: every future migration that adds a new tracker table (in any app schema) MUST add a
-- matching DELETE here, scoped to auth.uid() (directly or transitively through a parent table,
-- same as finance.valuations->finance.holdings below). This function is the entire DPDP 7-day
-- erasure guarantee (ADR-0014 §7) for "Delete my data" (ONB-BR-008) — a forgotten table here
-- breaks that guarantee silently, with no test failure (DAT-FLOW-001 is Automatable: N, verified
-- manually per migration).
create or replace function public.delete_my_data()
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    delete from finance.valuations
    where holding_id in (select id from finance.holdings where user_id = auth.uid());

    delete from finance.holdings where user_id = auth.uid();

    -- ADD NEW TABLES HERE (delete children before parents, same pattern as finance.valuations above)
end;
$$;

revoke all on function public.delete_my_data() from public;
grant execute on function public.delete_my_data() to authenticated;
