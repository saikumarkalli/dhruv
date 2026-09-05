# Manual verification scripts

Not migrations, not automated, not run by any CI workflow. Plain SQL scripts that assert one
thing each via `RAISE EXCEPTION` on failure, written for a human (or a future automated harness)
to run once real credentials/data exist. They exist because 001-net-worth-tracker Phase 11
(T081–T083) needed real RLS/RPC verification against a live Supabase project, and neither Docker
nor a `supabase login` session is available in the authoring environment — see
`apps/finance/specs/001-net-worth-tracker/data-model.md` § "DB readiness" for the full story.

## Running one

```powershell
# One-time, interactive:
supabase login

# Per script, against the linked dev project (never prod):
supabase db execute -f supabase/verification/<name>.sql --linked
```

`--linked` targets whatever project `supabase link` last pointed at — confirm that is `dhruv-dev`
(`dsfnrtckgpnvyvscevxn`, per `supabase/.temp/linked-project.json`) before running anything here.
Never run these against `dhruv-prod`.

## Scripts

| File | Covers | Needs |
|---|---|---|
| `phase2_rls_views.sql` | T081 — a second user reads zero rows from `v_latest_valuation`, `v_net_worth_by_sector`, `v_net_worth_history` | Two real `auth.users` rows, one with ≥1 holding + valuation |
| `phase2_rpc_ownership_and_idempotency.sql` | T081 (RPC ownership) + T082 (`correct_valuation` transaction/re-correction) + T083 (`create_holding_with_value` idempotent replay) | Same two users; the owning user's holding/valuation ids substituted in |

Each script's own header names exactly which `:variable` values to substitute before running.
