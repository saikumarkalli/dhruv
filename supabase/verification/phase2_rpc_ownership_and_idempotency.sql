-- 001-net-worth-tracker Phase 11 — RPC verification.
-- Manual verification script (see README.md). Not a migration, not run automatically.
--
-- Covers three tasks in one script since all three exercise the same two RPCs:
--   T081 (Sec)     — correct_valuation() and create_holding_with_value() both reject/scope by
--                    caller identity; a non-owner cannot act on another user's row.
--   T082 (Backend) — correct_valuation() end to end: corrected row carries source = 'CORRECTION',
--                    the original is excluded from v_latest_valuation, both happen in one
--                    transaction, and re-correcting an already-corrected row is refused.
--   T083 (Backend) — create_holding_with_value() idempotency: replaying the same p_request_id
--                    returns the original holding id and creates no second holding or valuation.
--
-- Static review already done (recorded in tasks.md's T081 closure note): both functions' bodies
-- were read directly. correct_valuation() resolves the target holding and asserts
-- `h.user_id = auth.uid()` in the SAME statement (no window for a TOCTOU gap), returning null —
-- and therefore raising — for a valuation the caller doesn't own. create_holding_with_value()
-- always inserts with `user_id = auth.uid()`, so there is no "wrong owner" case to reject; its
-- idempotency check additionally scopes the request_id lookup to `user_id = auth.uid()`, so a
-- request_id collision can never leak another user's holding id back to the caller. This script is
-- the live confirmation that logic actually behaves this way under real RLS/auth context.
--
-- BEFORE RUNNING: :'owner' must be a real auth.users.id with at least one non-deleted holding whose
-- id is :'holding_id', which must have at least one non-deleted valuation whose id is
-- :'valuation_id'. :'other' is any other real auth.users.id.
\set owner '00000000-0000-0000-0000-000000000000'
\set other '00000000-0000-0000-0000-000000000001'
\set holding_id '00000000-0000-0000-0000-000000000002'
\set valuation_id '00000000-0000-0000-0000-000000000003'

-- ── T081: ownership rejection ─────────────────────────────────────────────────────────────────
begin;
set local role authenticated;
set local request.jwt.claims = json_build_object('sub', :'other', 'role', 'authenticated')::text;

do $$
begin
    perform finance.correct_valuation(
        p_valuation_id => :'valuation_id'::uuid,
        p_value_paise => 1,
        p_as_of => current_date
    );
    raise exception 'RLS FAILURE: correct_valuation() let a non-owner correct another user''s valuation';
exception
    when others then
        if sqlerrm like '%not found, already corrected, or not owned%' then
            raise notice 'PASS: correct_valuation() rejected a non-owner caller';
        else
            raise; -- a different failure — do not swallow it
        end if;
end $$;
rollback;

-- ── T082: correct_valuation() end to end (owner's own valuation) ─────────────────────────────
begin;
set local role authenticated;
set local request.jwt.claims = json_build_object('sub', :'owner', 'role', 'authenticated')::text;

do $$
declare
    v_new_id uuid;
    v_latest_valuation_id uuid;
    v_source text;
begin
    v_new_id := finance.correct_valuation(
        p_valuation_id => :'valuation_id'::uuid,
        p_value_paise => 12345,
        p_as_of => current_date,
        p_note => 'phase2_rpc_ownership_and_idempotency.sql verification run'
    );

    select source into v_source from finance.valuations where id = v_new_id;
    if v_source is distinct from 'CORRECTION' then
        raise exception 'T082 FAILURE: corrected row has source = %, expected CORRECTION', v_source;
    end if;

    select valuation_id into v_latest_valuation_id
    from finance.v_latest_valuation where holding_id = :'holding_id'::uuid;
    if v_latest_valuation_id is distinct from v_new_id then
        raise exception
            'T082 FAILURE: v_latest_valuation still points at %, expected the corrected row %',
            v_latest_valuation_id, v_new_id;
    end if;

    -- Re-correcting the now-superseded original row must be refused — it is deleted_at-stamped,
    -- so the same ownership+not-deleted lookup that rejects a foreign caller also rejects this.
    begin
        perform finance.correct_valuation(
            p_valuation_id => :'valuation_id'::uuid,
            p_value_paise => 99999,
            p_as_of => current_date
        );
        raise exception 'T082 FAILURE: correcting an already-corrected row was allowed';
    exception
        when others then
            if sqlerrm like '%not found, already corrected, or not owned%' then
                raise notice 'PASS: re-correcting an already-corrected row was refused';
            else
                raise;
            end if;
    end;

    raise notice 'PASS: correct_valuation() end-to-end (source=CORRECTION, latest view updated, re-correction refused)';
end $$;
rollback; -- never actually commit a test correction against real data

-- ── T083: create_holding_with_value() idempotent replay ──────────────────────────────────────
begin;
set local role authenticated;
set local request.jwt.claims = json_build_object('sub', :'owner', 'role', 'authenticated')::text;

do $$
declare
    v_request_id uuid := gen_random_uuid();
    v_first_id uuid;
    v_second_id uuid;
    v_holding_count int;
begin
    v_first_id := finance.create_holding_with_value(
        p_name => 'phase2 verification holding',
        p_kind => 'ASSET',
        p_sector => 'OTHER',
        p_value_paise => 100,
        p_as_of => current_date,
        p_request_id => v_request_id
    );

    v_second_id := finance.create_holding_with_value(
        p_name => 'phase2 verification holding',
        p_kind => 'ASSET',
        p_sector => 'OTHER',
        p_value_paise => 100,
        p_as_of => current_date,
        p_request_id => v_request_id
    );

    if v_second_id is distinct from v_first_id then
        raise exception
            'T083 FAILURE: replay with the same request_id returned a different holding id (% vs %)',
            v_first_id, v_second_id;
    end if;

    select count(*) into v_holding_count from finance.holdings where request_id = v_request_id;
    if v_holding_count <> 1 then
        raise exception 'T083 FAILURE: expected exactly 1 holding for this request_id, found %', v_holding_count;
    end if;

    raise notice 'PASS: create_holding_with_value() replay is idempotent (same id, no duplicate row)';
end $$;
rollback; -- never actually commit a test holding against real data
