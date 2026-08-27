<!--
Target branch: `develop` for all work. `develop -> main` is the PROD promotion
PR and the only path that ships (ADR-0032).

The PR title drives the released version (ADR-0025):
  feat: / feat(scope):        -> MINOR bump
  type!: or BREAKING CHANGE:  -> MAJOR bump
  anything else               -> PATCH bump
-->

## What & why

<!-- One or two sentences. The *why* matters more than the *what* — the diff
     already shows the what. Link the issue if one exists. -->

Closes #

## Change summary

**Changed**
<!-- path — what changed there -->
-

**Deliberately NOT changed**
<!-- Nearby things you noticed and left alone, and why. This documents scope
     discipline; an empty list is a fine answer if you really touched nothing else. -->
-

**Concerns / follow-ups**
-

---

## Checklist

Delete rows that genuinely do not apply — do not leave them unticked and silent.

### Always
- [ ] Branched from `develop`, targets `develop` (or is the `develop -> main` promotion)
- [ ] Title follows conventional commits — it picks the semver segment
- [ ] `./gradlew regressionCheck` passes locally (unit + ArchUnit + coverage floor)
- [ ] No secrets, keys, or `.env` values in the diff (GitLeaks gates this anyway)
- [ ] No manual edits to `VERSION_CODE` / `VERSION_NAME` / `versions.json` (CI-owned, ADR-0025)

### If it adds or changes a feature module
- [ ] Route wrapped in `FeatureHost` — never a blank crash
- [ ] Flag entry present in `platform/feature-flags/dhruv-<app>.json`
- [ ] Module boundaries hold: no `feature -> feature`, `feature -> data` via Repository only
- [ ] Crash tagging (`setCustomKey "module"`) + at least one Performance trace

### If it touches UI
- [ ] Reads tokens only — no raw hex, `.dp`/`.sp` literals, or ad-hoc typography in a screen
- [ ] All applicable screen states defined (loading / empty / error / offline / signed-out)
- [ ] Renders correctly in light **and** dark
- [ ] Strings in `strings.xml`; `contentDescription` on every icon-only action
- [ ] Reused an existing `:libs:core` component, or extended one — did not add a parallel component

### If data leaves the device (DPDP)
- [ ] Consent gate precedes the call, and consent is persisted + revocable
- [ ] Erasure path still covers the new data
- [ ] No PII in telemetry / crash reports

### If it touches the database
- [ ] Object edited in `supabase/schemas/<schema>/`, migration generated via `supabase db diff`
- [ ] Migration is additive; no hand-edit of an already-applied migration
- [ ] `supabase/SCHEMA.md` regenerated
- [ ] New table has RLS enabled and an explicit `GRANT` (custom schemas are not exposed by default)
- [ ] **Every new view carries `security_invoker = on`** — without it the view runs as its owner,
      bypasses RLS, and returns every user's rows through PostgREST. `db diff` cannot emit it, so it
      is hand-verified in the generated migration
- [ ] **Every new user-data table added to `public.delete_my_data()`** in the same migration — this
      is the entire DPDP 7-day erasure guarantee and a miss is silent, with no failing test
- [ ] RLS test asserts a second user reads **zero rows** from every new table *and every new view*

### If it changes shipped behaviour (feature change, defect fix, removal)
- [ ] The owning spec's **Implementation record** updated in this same PR (constitution Art. Xa)
- [ ] For a defect fix: names the **FR whose stated behaviour was not actually delivered** — a fix
      that changes behaviour without naming what was wrong is an undocumented behaviour change
- [ ] `CHANGELOG.md` entry added under the `finance-*` heading
- [ ] Any affected registry row updated (route / notification channel / intent / settings)
- [ ] If a doc is now wrong and cannot be fixed here, it says so with a dated "known stale" line

### If it changes architecture
- [ ] A new ADR is proposed in `platform/DECISIONS.md` — decisions are not silently diverged from
- [ ] The ADR number is the next free one in `DECISIONS.md`, not one reserved in a stale plan