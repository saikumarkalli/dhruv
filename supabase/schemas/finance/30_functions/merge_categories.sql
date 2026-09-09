-- merge_categories — the ONLY path by which two categories are merged (FR-024, MNY-BR-004,
-- research R9). Re-points every non-deleted transaction from the source category to the target,
-- soft-deletes the source, and returns the count moved so the confirmation dialog's stated number
-- and the actual result can never disagree.
--
-- Invoker rights (not definer): RLS already restricts finance.categories/finance.transactions to
-- the caller's own rows, so no elevated privilege is needed — unlike the erasure functions, which
-- need definer rights specifically to reach auth.users. One function call is atomic, so a
-- half-finished merge (the worst outcome for an operation the dialog just said is irreversible)
-- cannot happen.
create or replace function finance.merge_categories(p_source uuid, p_target uuid)
returns integer
language plpgsql
security invoker
set search_path = finance, public
as $$
declare
    v_moved integer;
begin
    if p_source = p_target then
        raise exception 'cannot merge a category into itself' using errcode = 'check_violation';
    end if;

    -- Ownership + existence check up front: both categories must belong to the caller and neither
    -- may already be soft-deleted, otherwise the UPDATE below would silently move zero rows.
    if not exists (
        select 1 from finance.categories
        where id = p_source and user_id = auth.uid() and deleted_at is null
    ) or not exists (
        select 1 from finance.categories
        where id = p_target and user_id = auth.uid() and deleted_at is null
    ) then
        raise exception 'source or target category not found or not owned by caller'
            using errcode = 'no_data_found';
    end if;

    update finance.transactions
       set category_id = p_target
     where category_id = p_source
       and user_id = auth.uid()
       and deleted_at is null;

    get diagnostics v_moved = row_count;

    update finance.categories
       set deleted_at = now()
     where id = p_source
       and user_id = auth.uid();

    return v_moved;
end;
$$;

revoke all on function finance.merge_categories(uuid, uuid) from public;
grant execute on function finance.merge_categories(uuid, uuid) to authenticated;
