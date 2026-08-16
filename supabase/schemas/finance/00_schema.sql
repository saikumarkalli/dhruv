-- finance schema — declarative state (ADR-0033). Per-app Postgres schema: every table/view/
-- function that belongs to the Finance app's tracker domain lives under `finance.*`, not `public.*`.
-- Cross-app orchestration (the two erasure functions) stays in `public` — see
-- `supabase/schemas/public/30_functions/`.
create schema if not exists finance;

-- Custom (non-public) schemas are not auto-exposed to PostgREST roles — explicit grants are
-- required (unlike legacy `public`, whose exposure default is ambiguous and out of scope here;
-- see ADR-0033 consequences). No `anon` grant: every tracker call is authenticated (ADR-0029
-- AuthInterceptor) — there is no anonymous tracker access to expose.
grant usage on schema finance to authenticated;
