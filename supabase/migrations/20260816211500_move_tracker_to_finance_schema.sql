-- Move tracker tables from `public` into a per-app `finance` Postgres schema (ADR-0033).
-- Matches the declarative state in supabase/schemas/finance/ and
-- supabase/schemas/public/30_functions/ (ADR-0032's authorship workflow).
--
-- `alter table ... set schema` is metadata-only in Postgres: data, indexes, foreign keys,
-- RLS enablement, and RLS policies all move with the table unchanged — this is why the
-- move is expressed as ALTER, not a drop+recreate.

create schema if not exists finance;

-- Custom (non-public) schemas are not auto-exposed to PostgREST roles the way `public` is —
-- explicit grants are required (see supabase/schemas/finance/00_schema.sql). No `anon` grant:
-- every tracker call is authenticated (ADR-0029 AuthInterceptor).
grant usage on schema finance to authenticated;

alter table public.holdings set schema finance;
alter table public.valuations set schema finance;

grant select, insert, update on finance.holdings to authenticated;
grant select, insert on finance.valuations to authenticated;

-- Erasure functions stay in `public` (cross-app orchestrator, ADR-0033) — only their
-- bodies change, to reference the tables' new qualified names. Grants/ownership already
-- applied by 0001_init.sql are untouched by `create or replace function` (same signature).
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
