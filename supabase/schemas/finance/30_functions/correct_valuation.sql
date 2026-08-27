-- correct_valuation — the ONLY path by which a valuation row is ever amended.
--
-- This function is what ADR-0029 decision 4 named and assigned to "Phase 2's SA step", and it is
-- the resolution of the 2026-08-22 audit's most severe correctness defect: FR-004 (001) requires
-- hiding a wrong valuation by setting `deleted_at`, which is an UPDATE — and `finance.valuations`
-- has SELECT and INSERT policies only, with `grant select, insert`. The correction was asserted as
-- working in three documents, and the RLS that made it impossible was cited as the guarantee.
--
-- WHY A FUNCTION AND NOT AN UPDATE POLICY. Granting UPDATE on `valuations` would make the table
-- ordinarily mutable and destroy BR-C1's database-level append-only guarantee — the entire reason
-- the table has no UPDATE policy. `security definer` lets exactly this one audited, named operation
-- through, and nothing else. The soft-delete and the corrected append happen in one transaction, so
-- a holding can never be left with both rows live or neither.
--
-- The corrected row is written with source = 'CORRECTION', so C3's valuation history can label it
-- rather than showing two indistinguishable entries (readiness decisions §2.1).
create or replace function finance.correct_valuation(
    p_valuation_id uuid,
    p_value_paise bigint,
    p_as_of date,
    p_note text default null
)
returns uuid
language plpgsql
security definer
set search_path = finance, public
as $$
declare
    v_holding_id uuid;
    v_new_id uuid;
begin
    -- Ownership check is explicit, because `security definer` bypasses the RLS that would normally
    -- do it. Resolving the holding and asserting the caller owns it are the same statement, so
    -- there is no window between the two.
    select v.holding_id
      into v_holding_id
      from finance.valuations v
      join finance.holdings h on h.id = v.holding_id
     where v.id = p_valuation_id
       and v.deleted_at is null
       and h.deleted_at is null
       and h.user_id = auth.uid();

    if v_holding_id is null then
        raise exception 'valuation not found, already corrected, or not owned by caller'
            using errcode = 'no_data_found';
    end if;

    if p_value_paise < 0 then
        raise exception 'value_paise must be >= 0' using errcode = 'check_violation';
    end if;

    if p_as_of > current_date then
        raise exception 'as_of may not be in the future' using errcode = 'check_violation';
    end if;

    update finance.valuations
       set deleted_at = now()
     where id = p_valuation_id;

    insert into finance.valuations (holding_id, value_paise, as_of, source, request_id)
    values (v_holding_id, p_value_paise, p_as_of, 'CORRECTION', null)
    returning id into v_new_id;

    if p_note is not null then
        update finance.holdings
           set notes = coalesce(notes || E'\n', '') || p_note
         where id = v_holding_id;
    end if;

    return v_new_id;
end;
$$;

revoke all on function finance.correct_valuation(uuid, bigint, date, text) from public;
grant execute on function finance.correct_valuation(uuid, bigint, date, text) to authenticated;