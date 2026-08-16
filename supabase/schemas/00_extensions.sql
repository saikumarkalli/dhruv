-- Extensions — declarative state (ADR-0032). Backfilled from migrations/0001_init.sql; this file
-- describes what should exist, `supabase db diff` is what generates the migration that gets it there.
--
-- `with schema extensions` (not the default `public`) keeps pgcrypto's functions (digest, crypt,
-- gen_random_uuid, ...) out of PostgREST's auto-exposed api schema list (security review,
-- 2026-08-15) — Supabase's documented best practice, not load-bearing here (generic crypto
-- utilities, no table access) but removes the ambiguity for free.
create extension if not exists pgcrypto with schema extensions;
