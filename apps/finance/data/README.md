# :apps:finance:data

The Finance app's single data module. Every feature module depends on this one and reaches storage
**through a repository only** — `feature → data` is allowed, direct DAO/DTO access is not, and
`DependencyRulesTest` enforces both.

- **Gradle coordinate:** `:apps:finance:data`
- **Directory:** `apps/finance/data/` (no `projectDir` remap — only feature modules are bucketed)
- **Namespace:** `com.dhruv.finance.data`
- **Depends on:** `:libs:core`. Never on a feature module, never on `:apps:finance:app`.

## Two storage domains, deliberately separate

The split is an ADR, not an accident — read ADR-0014 before moving anything across it.

### Room — calculators and converters
Offline-first, device-local, no account required. `AppDatabase` holds calculation history and the
currency-rate cache. This is the domain `PLATFORM.md` §5's `DhruvEntity` / HLC / sync design
describes.

Current schema version is **5**. A migration is additive and gets a matching
`MIGRATION_x_y` plus an on-device verification task in the phase that adds it. Phase 6 takes it to
6 for the alert log.

### Supabase — the tracker
Net worth, money, planning, insights. **Cloud-primary: no local Room mirror, no `DhruvEntity`, no
client-side conflict resolution** — the server plus RLS (`user_id = auth.uid()`) is the single
source of truth (ADR-0014, which narrowly overrides `PLATFORM.md` §5 for this domain only).

`tracker/` holds `net`, `auth`, `dto`, `model`, `mapper`, `repo`. Networking is Retrofit + Moshi +
OkHttp against GoTrue and PostgREST — the stack `CurrencyApiClient` already proves works on this
AGP 9 toolchain (ADR-0029 decision 1).

Three properties of that layer are structural, not conventions to remember:

- **`ConsentInterceptor` is attached only to the data client**, so no code path reaches PostgREST
  without passing it — because no other PostgREST-capable client is constructed anywhere in the app.
- **`AuthInterceptor`** attaches `apikey` + bearer token; a 401 triggers exactly one refresh, and a
  second consecutive 401 forces `SessionStore` to `SignedOut`. No retry loop.
- **Certificate pinning is CA-level** (Google Trust Services GTS Root R1 + R4). Leaf pinning would
  brick the app on Supabase's routine rotations — and the roots were wrong once already, see
  ADR-0029's correction.

Every tracker query must send **`Accept-Profile: finance`** (`Content-Profile` on writes). Tracker
tables live in the `finance` Postgres schema (ADR-0033); omitting the header does not error loudly,
it silently 404s against the empty `public` schema.

## Money

Integer **paise** (`Long` / `bigint`) for every tracked amount — exact, summable, no floating point.
Proportions are integer **basis points**. `BigDecimal` is confined to the calculator and projection
engines, which live outside `tracker/`. The `checkTrackerMoneyPrecision` Gradle task enforces the
boundary.

## Conventions

- One repository per aggregate; feature modules never see a DAO or a DTO.
- Repositories expose `Flow`; suspending functions for one-shot writes.
- Tests use fakes, never in-memory Room — Robolectric-SQLite is a known blocker on this toolchain.

## Schema

Declarative source of truth is `supabase/schemas/finance/`; `supabase db diff` generates the
migration. Current shape: `supabase/SCHEMA.md` (generated). Phase 2's authored objects and the open
DB gaps are in
[specs/001-net-worth-tracker/data-model.md](../specs/001-net-worth-tracker/data-model.md)
§ "DB readiness", which also carries the maintenance conventions every later phase inherits.