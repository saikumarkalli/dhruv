# Shared Data Contracts SDD (05)

> **Status:** ACTIVE
> **Scope:** Defines the shared validation rules, money representation, and schema migration ownership for both platforms.

## 1. Type Parity

- **Kotlin:** DTOs are manually written in `apps/finance/data/` utilizing Moshi `@JsonClass`.
- **TypeScript:** Types are auto-generated via Supabase CLI (`npx supabase gen types typescript`) into `web/src/shared/types/database.ts`.

## 2. Money Representation (Paise)

Money is always stored and calculated as integer `paise`.

- **Storage**: `bigint` (Postgres).
- **Android**: `Long`.
- **Web**: `number` (JS numbers are safe up to $90 Million in paise).

**Formatting Rules**:
- Compact: `₹12.5L`, `₹100Cr`
- Full: `₹12,50,000.00`
Both platforms must implement and test these exact formatting utilities.

## 3. Validation Rules (Source of Truth)

Validation logic must be duplicated identically on both platforms and tested against the same edge cases.

| Field | Rule | Error Message |
|---|---|---|
| Asset/Liability name | Non-blank, trimmed, max 100 chars | "Name is required" / "Name too long" |
| Asset value (paise) | ≥ 0 | "Value must be zero or positive" |
| Liability value (paise)| > 0 | "Amount must be positive" |
| Valuation date | ≤ today (IST), not null | "Date cannot be in the future" |
| Transaction amount | > 0 | "Amount must be positive" |
| Transaction category | Non-blank, valid enum value | "Category is required" |

## 4. Schema Migration Workflow

1. Design spec proposes schema.
2. `supabase/migrations/NNN_feature.sql` is authored.
3. PR reviewed.
4. Supabase CLI pushes to `dhruv-dev` project.
5. TypeScript types generated and committed.
6. Kotlin DTOs manually updated.
7. CI verifies both platforms against the new schema.
8. Merged and pushed to `dhruv-prod`.

## 5. Calculator History Migration Path

- **V1 (Current)**: Android uses local Room DB. Web uses in-memory/sessionStorage.
- **V3 (Phase 6+)**: History migrates to Supabase `calculator_history` table. Room becomes an offline cache. Web syncs directly with Supabase.
