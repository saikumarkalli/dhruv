# Migrations

Empty on purpose — this is the W0 scaffold (`docs/PRD.md` §8). Schema authorship starts at
W1 alongside the P1 net worth tables, following the workflow in
`docs/sdd/05-shared-data-contracts-sdd.md` §4:

1. Design spec proposes schema (already done for P1 — `docs/sdd/02-backend-api-sdd.md` §1.1).
2. Author `supabase/migrations/<timestamp>_<name>.sql` (via `supabase migration new <name>`,
   not by hand-naming the file — the CLI's timestamp prefix is what orders migrations).
3. PR reviewed.
4. `supabase db push` to the `dhruv-dev` project for testing.
5. `npx supabase gen types typescript` regenerates `web/src/shared/types/database.ts`.
6. Kotlin DTOs updated manually in `apps/finance/data/`.
7. CI verifies both platforms against the new schema.
8. Merge → push to `dhruv-prod`.

Every table needs RLS enabled with the `user_id = auth.uid()` policy (SDD-02 §3) — no
exceptions, the app servers run no business logic of their own.
