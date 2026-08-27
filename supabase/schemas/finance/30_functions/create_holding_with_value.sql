-- create_holding_with_value — creates a holding and its first valuation in ONE transaction.
--
-- Resolves the second blocking defect the 2026-08-22 audit found: FR-002/BR-C2 require the holding
-- and its first valuation to be written atomically, but that is two PostgREST inserts over HTTP.
-- 001's data-model conceded it was "not expressible as a single-table constraint" and delegated it
-- to "the repository layer either writes both or neither" — which no client can guarantee across
-- two requests. A failed second insert left an orphan holding, and `holdings` has no client DELETE
-- policy to compensate with.
--
-- Used for CREATION ONLY. An ordinary later valuation append stays a plain PostgREST insert against
-- the existing valuations_insert_own policy — this function is not a general write path.
--
-- `p_request_id` is generated client-side at the moment the user commits (not at send time), so an
-- automatic retry after a timeout reuses it, collides on `holdings.request_id unique`, and returns
-- the already-created holding instead of duplicating it (readiness decisions §2.7).
create or replace function finance.create_holding_with_value(
    p_name text,
    p_kind text,
    p_sector text,
    p_value_paise bigint,
    p_as_of date,
    p_source text default 'MANUAL',
    p_invested_paise bigint default null,
    p_notes text default null,
    p_request_id uuid default null
)
returns uuid
language plpgsql
security definer
set search_path = finance, public
as $$
declare
    v_holding_id uuid;
begin
    if auth.uid() is null then
        raise exception 'not authenticated' using errcode = 'insufficient_privilege';
    end if;

    -- Idempotent replay: a retry with the same request_id returns the original row rather than
    -- creating a second holding. Checked before insert so the caller gets the id either way.
    if p_request_id is not null then
        select id into v_holding_id
          from finance.holdings
         where request_id = p_request_id
           and user_id = auth.uid();

        if v_holding_id is not null then
            return v_holding_id;
        end if;
    end if;

    -- Column CHECKs enforce the enum sets, name length, non-negative money and the no-future-date
    -- rule; they are not restated here so there is exactly one place to change them.
    insert into finance.holdings (user_id, name, kind, sector, invested_paise, notes, request_id)
    values (auth.uid(), btrim(p_name), p_kind, p_sector, p_invested_paise, p_notes, p_request_id)
    returning id into v_holding_id;

    insert into finance.valuations (holding_id, value_paise, as_of, source)
    values (v_holding_id, p_value_paise, p_as_of, p_source);

    return v_holding_id;
end;
$$;

revoke all on function finance.create_holding_with_value(text, text, text, bigint, date, text, bigint, text, uuid) from public;
grant execute on function finance.create_holding_with_value(text, text, text, bigint, date, text, bigint, text, uuid) to authenticated;