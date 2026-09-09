-- fn_transaction_audit — the trigger function behind BR-D5 / MNY-BR-006 ("any mutation appends an
-- event"). AFTER INSERT OR UPDATE on finance.transactions, FOR EACH ROW. research R4 explains why
-- this is a trigger and not two client calls: PostgREST cannot wrap two requests in one
-- transaction, so a client that writes the transaction and then writes the event has an
-- unrecoverable window between the two. The trigger makes the guarantee structural.
--
-- DEVIATION FROM data-model.md's literal trigger clause ("AFTER INSERT OR UPDATE OR DELETE"): this
-- trigger is registered for INSERT OR UPDATE only, not DELETE. finance.transactions has no
-- client-facing DELETE policy — the only real SQL DELETE against it is public.delete_my_data()'s
-- erasure sweep, which deletes the whole table for that user. An AFTER DELETE handler here would
-- try to INSERT a transaction_events row referencing a transaction_id that no longer exists (its
-- own event rows already cascaded away via the transaction_events_transaction_id_fkey ON DELETE
-- CASCADE), raising a foreign-key violation and breaking erasure. The documented "DELETED" event
-- kind is produced by the soft-delete UPDATE (deleted_at transitions from null), not a hard DELETE
-- — data-model.md's spec.md state-transition section already describes it this way ("soft-deleted
-- ... Never hard-deleted by a client"). Flagged here and in tasks.md T008 rather than silently
-- diverging from the written spec.
--
-- INSERT dispatches on NEW.source to cover all six transaction_events.kind values with exactly one
-- row per mutation (MNY-BR-006's own acceptance test wants exactly one row for accept-from-
-- recurring and for reconcile, not a CREATED row plus a second explicit one):
--   source = 'RECURRING' -> ACCEPTED_FROM_RECURRING
--   source = 'RECONCILE' -> RECONCILED
--   anything else (MANUAL, SMS, IMPORT)         -> CREATED
create or replace function finance.fn_transaction_audit()
returns trigger
language plpgsql
security invoker
set search_path = finance, public
as $$
declare
    v_kind text;
    v_detail jsonb;
begin
    if TG_OP = 'INSERT' then
        v_kind := case NEW.source
            when 'RECURRING' then 'ACCEPTED_FROM_RECURRING'
            when 'RECONCILE' then 'RECONCILED'
            else 'CREATED'
        end;
        insert into finance.transaction_events (transaction_id, kind, detail)
        values (NEW.id, v_kind, jsonb_build_object(
            'type', NEW.type,
            'amount_paise', NEW.amount_paise,
            'account_id', NEW.account_id,
            'category_id', NEW.category_id,
            'source', NEW.source
        ));
        return NEW;
    end if;

    if TG_OP = 'UPDATE' then
        -- Soft delete: deleted_at transitions from null to non-null. Checked first — a delete
        -- request may arrive alongside other field changes, and deletion is the dominant fact.
        if OLD.deleted_at is null and NEW.deleted_at is not null then
            insert into finance.transaction_events (transaction_id, kind, detail)
            values (NEW.id, 'DELETED', jsonb_build_object('deleted_at', NEW.deleted_at));
            return NEW;
        end if;

        if OLD.category_id is distinct from NEW.category_id then
            insert into finance.transaction_events (transaction_id, kind, detail)
            values (NEW.id, 'CATEGORY_CHANGED', jsonb_build_object(
                'old_category_id', OLD.category_id,
                'new_category_id', NEW.category_id
            ));
            return NEW;
        end if;

        -- Generic edit: record only the fields that actually changed, old/new, so the client can
        -- render a plain-language line without re-deriving the diff itself (FR-007).
        v_detail := '{}'::jsonb;
        if OLD.amount_paise is distinct from NEW.amount_paise then
            v_detail := v_detail || jsonb_build_object('amount_paise',
                jsonb_build_object('old', OLD.amount_paise, 'new', NEW.amount_paise));
        end if;
        if OLD.payee is distinct from NEW.payee then
            v_detail := v_detail || jsonb_build_object('payee',
                jsonb_build_object('old', OLD.payee, 'new', NEW.payee));
        end if;
        if OLD.note is distinct from NEW.note then
            v_detail := v_detail || jsonb_build_object('note',
                jsonb_build_object('old', OLD.note, 'new', NEW.note));
        end if;
        if OLD.occurred_at is distinct from NEW.occurred_at then
            v_detail := v_detail || jsonb_build_object('occurred_at',
                jsonb_build_object('old', OLD.occurred_at, 'new', NEW.occurred_at));
        end if;
        if OLD.cleared is distinct from NEW.cleared then
            v_detail := v_detail || jsonb_build_object('cleared',
                jsonb_build_object('old', OLD.cleared, 'new', NEW.cleared));
        end if;
        if OLD.account_id is distinct from NEW.account_id then
            v_detail := v_detail || jsonb_build_object('account_id',
                jsonb_build_object('old', OLD.account_id, 'new', NEW.account_id));
        end if;

        -- Nothing tracked changed (e.g. only receipt_path or an untracked field) — no-op, no event.
        if v_detail = '{}'::jsonb then
            return NEW;
        end if;

        insert into finance.transaction_events (transaction_id, kind, detail)
        values (NEW.id, 'EDITED', v_detail);
        return NEW;
    end if;

    return NEW;
end;
$$;

create trigger trg_transaction_audit
    after insert or update on finance.transactions
    for each row execute function finance.fn_transaction_audit();
