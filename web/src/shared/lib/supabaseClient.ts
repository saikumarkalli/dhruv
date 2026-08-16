import { createClient } from "@supabase/supabase-js";
import type { Database } from "../types/database";

// SDD-02 §5 / SDD-06 §1: same Supabase project as Android, consumed as plain
// PostgREST + GoTrue over supabase-js. Values come from Vite env (.env.local,
// gitignored) in dev and Vercel env vars in prod — never hardcoded (ADR-0014 §7).
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;

if (!supabaseUrl || !supabaseAnonKey) {
  console.warn(
    "Supabase env vars missing (VITE_SUPABASE_URL / VITE_SUPABASE_ANON_KEY). " +
      "Copy .env.example to .env.local and fill in dhruv-dev project values.",
  );
}

export const supabase = createClient<Database>(
  supabaseUrl ?? "",
  supabaseAnonKey ?? "",
);

// ADR-0033: tracker tables (holdings, valuations, ...) live in the `finance` Postgres schema, not
// `public`. Query them via `supabase.schema("finance").from("holdings")` — the default `.from()`
// above only reaches `public`, where the two cross-app erasure RPCs
// (delete_my_data/delete_my_account) intentionally still live.
