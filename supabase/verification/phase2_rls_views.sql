-- 001-net-worth-tracker Phase 11, T081 — RLS verification for the three views this phase adds.
-- Manual verification script (see README.md). Not a migration, not run automatically.
--
-- WHY THIS IS NEEDED: `security_invoker = on` is confirmed present on all three views by static
-- review of both the declarative schema files and the generated migration (data-model.md's "DB
-- readiness" section records this), but a text match is not proof the clause actually does what
-- it's supposed to at query time — only a real RLS-context query against real rows is. This script
-- is that query, written so it can be run the moment credentials exist rather than re-derived then.
--
-- BEFORE RUNNING: replace :'user_a' with a real auth.users.id that owns at least one holding with
-- at least one valuation (any kind). :'user_b' is any other real auth.users.id — it owns nothing
-- that matters here, since the whole point is that it should see none of user_a's rows.
\set user_a '00000000-0000-0000-0000-000000000000'
\set user_b '00000000-0000-0000-0000-000000000001'

begin;

-- Simulates the auth context PostgREST attaches to every request for a signed-in user. This is
-- Supabase's own documented technique for exercising RLS from plain SQL — auth.uid() reads
-- request.jwt.claims->>'sub', which the JWT's "sub" claim populates in production.
set local role authenticated;
set local request.jwt.claims = json_build_object('sub', :'user_b', 'role', 'authenticated')::text;

do $$
declare
    v_count int;
begin
    select count(*) into v_count from finance.v_latest_valuation where user_id = :'user_a'::uuid;
    if v_count <> 0 then
        raise exception 'RLS FAILURE: user_b read % row(s) from v_latest_valuation owned by user_a', v_count;
    end if;

    select count(*) into v_count from finance.v_net_worth_by_sector where user_id = :'user_a'::uuid;
    if v_count <> 0 then
        raise exception 'RLS FAILURE: user_b read % row(s) from v_net_worth_by_sector owned by user_a', v_count;
    end if;

    select count(*) into v_count from finance.v_net_worth_history where user_id = :'user_a'::uuid;
    if v_count <> 0 then
        raise exception 'RLS FAILURE: user_b read % row(s) from v_net_worth_history owned by user_a', v_count;
    end if;

    raise notice 'PASS: user_b reads zero rows from all three views for user_a''s data';
end $$;

-- Sanity check the other direction — user_a impersonating itself should see its own row(s), or
-- this test is vacuous (proving nothing about isolation because there was nothing to isolate).
set local request.jwt.claims = json_build_object('sub', :'user_a', 'role', 'authenticated')::text;

do $$
declare
    v_count int;
begin
    select count(*) into v_count from finance.v_latest_valuation where user_id = :'user_a'::uuid;
    if v_count = 0 then
        raise exception
            'FIXTURE FAILURE: user_a has zero rows in v_latest_valuation — this test is vacuous. '
            'Point :user_a at a real user with at least one holding + valuation.';
    end if;
    raise notice 'Fixture OK: user_a sees % row(s) of its own data', v_count;
end $$;

rollback;
