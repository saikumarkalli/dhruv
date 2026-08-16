-- delete_my_account — declarative state (ADR-0032). Calls delete_my_data() then removes the
-- caller's own auth.users row — `security definer`, callable only by the signed-in user against
-- their own uid, no service-role key, no Edge Function. Stays in `public` (ADR-0033): auth.users is
-- shared across every Dhruv app (ADR-0031 Dhruv ID), so account deletion is cross-app by nature,
-- unlike the domain tables it cascades through.
create or replace function public.delete_my_account()
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    calling_user_id uuid := auth.uid();
begin
    perform public.delete_my_data();
    delete from auth.users where id = calling_user_id;
end;
$$;

revoke all on function public.delete_my_account() from public;
grant execute on function public.delete_my_account() to authenticated;
