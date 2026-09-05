-- tracker_user_id_default — fixes a real, previously-undetected bug found 2026-09-05 while
-- verifying 002-money-tab against a live device: none of the six client DTOs that create rows in
-- a tracker table (`finance.accounts`/`categories`/`transactions`/`recurring_templates`/
-- `suggestions`, plus the not-yet-consumed `finance.holdings`) ever sent `user_id` in their INSERT
-- payload. Every `user_id` column was `not null` with no default, so every real write was rejected
-- by that table's own `..._insert_own` RLS policy with HTTP 403 — invisible until this session,
-- since 002's migration had never actually been applied to a live project before now (see
-- 20260904120000_money_phase3.sql's own history — it sat unpushed since the day it was authored).
--
-- Fix: `default auth.uid()` on all six columns. The column self-populates from the caller's own
-- JWT instead of relying on a client to echo back an id it has no reason to know; RLS's existing
-- `with check (user_id = auth.uid())` on every one of these tables still rejects a client that
-- tries to override the default with someone else's id, so this is strictly additive, not a
-- weakening of the existing guarantee.
alter table finance.accounts
    alter column user_id set default auth.uid();

alter table finance.categories
    alter column user_id set default auth.uid();

alter table finance.transactions
    alter column user_id set default auth.uid();

alter table finance.recurring_templates
    alter column user_id set default auth.uid();

alter table finance.suggestions
    alter column user_id set default auth.uid();

alter table finance.holdings
    alter column user_id set default auth.uid();
