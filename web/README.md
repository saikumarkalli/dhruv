# Dhruv Web

Vite + React 19 + TypeScript SPA — the web counterpart to the Android Finance app. See
`docs/sdd/04-web-app-sdd.md` for the technical design and `docs/PRD.md` for the roadmap
(this is the W0 scaffold; no business logic yet).

## Commands

- `npm run dev` — dev server
- `npm run build` — typecheck + production build
- `npm run lint` — ESLint
- `npm run typecheck` — `tsc -b --noEmit`
- `npm test` — Vitest

## Setup

Copy `.env.example` to `.env.local` and fill in the `dhruv-dev` Supabase project values
(`VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`).

## Structure

See `docs/sdd/04-web-app-sdd.md` §2. `src/apps/finance` is the only app with a real route
today; `tools`/`vault`/`health`/`relationship` are scaffolded placeholders gated behind
`platform/feature-flags/dhruv-finance.json` — the same file the Android app packages as an
asset, so flag keys never drift between platforms.
