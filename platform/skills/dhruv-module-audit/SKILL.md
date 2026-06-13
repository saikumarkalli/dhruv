---
name: dhruv-module-audit
description: Audit a Dhruv module for architectural compliance. Use whenever the user says "check", "audit", "validate", "review module", "is this correct", "pre-commit check", "ready to merge", "compliance check", or any request to verify a module follows Dhruv platform rules before committing or merging. Also triggers on "did I miss anything", "what's wrong with this module", "verify architecture". Checks dependency rules, flag entries, crash tagging, test coverage, ArchUnit compliance, security rules, and DPDP consent gates. Always run this before merging a feature branch.
---

# Dhruv Module Audit

Validates a module against Dhruv platform architecture rules. Run before every merge.

## How to audit

When asked to audit, run ALL checks below against the target module. Report results as:
- ✅ PASS — rule satisfied
- ❌ FAIL — violation found (with file + line if possible)
- ⚠️ WARN — not a blocker but should be addressed

## Checks

### 1. Module structure
- [ ] `build.gradle.kts` applies convention plugins (`dhruv.android.library` / `.application`)
- [ ] Namespace follows pattern: `com.dhruv.{app}.feature.{name}` or `com.dhruv.{lib}`
- [ ] Module is included in root `settings.gradle.kts`
- [ ] Parent app's `build.gradle.kts` has `implementation(project(":apps:{app}:feature:{name}"))`

### 2. Dependency rules (ArchUnit-enforced)
Scan imports in all `.kt` files:
- [ ] No imports from other feature modules (`com.dhruv.{app}.feature.{other}` → ❌ FAIL)
- [ ] No direct Room/DataStore access without Repository pattern (`@Dao` used only in `data/` package)
- [ ] Core module imports nothing app-specific
- [ ] **If vault**: no imports from network, ai, analytics packages → ❌ FAIL if found

### 3. Feature flag
- [ ] Entry exists in `platform/feature-flags/dhruv-{app}.json` for this feature
- [ ] If feature sends data off-device: `"requiresConsent": true` in flag → ❌ FAIL if missing

### 4. Fault isolation
- [ ] Feature route is wrapped in `FeatureHost(featureKey = "{name}")` in the app's NavHost
- [ ] FeatureHost wrapper catches exceptions (verify it delegates to `:libs:core` FeatureHost)

### 5. Observability
- [ ] `CrashReporter.setModule("{name}")` called in ViewModel init or feature entry point
- [ ] At least one Firebase Performance trace in the feature
- [ ] **If vault**: only `vault_module_error` emitted — no screen names, no user context → ❌ FAIL if more

### 6. Data layer (if feature has persistence)
- [ ] Entity implements `DhruvEntity` (or explicitly does NOT for vault — check which)
- [ ] `userId` field is indexed (`@Index(value = ["userId"])`)
- [ ] `hlc` field present and set by `HlcClock` (not manual timestamp)
- [ ] Soft-delete default (`isDeleted` flag); hard-delete only in GC/erasure paths
- [ ] Room migration uses `addColumn` only, never `dropTable`
- [ ] Repository interface exists; feature code depends on interface, not impl

### 7. Security
- [ ] No API keys, secrets, or tokens in source files (grep for patterns)
- [ ] No hardcoded URLs to production services (should be in BuildConfig)
- [ ] **If vault**: `FLAG_SECURE` on all screens, `allowBackup="false"` in manifest
- [ ] **If AI feature**: consent check via `ConsentManager` before any online call

### 8. DPDP compliance
- [ ] If any data leaves the device: consent screen gates the flow
- [ ] Erasure path exists (soft-delete + tombstone GC satisfies 7-day rule)
- [ ] No user PII logged to Crashlytics (check `setCustomKey` calls)

### 9. Tests
- [ ] At least one unit test file exists in `src/test/`
- [ ] ViewModel test covers initial state
- [ ] If Room entity: DAO test with in-memory DB exists in `src/androidTest/`
- [ ] `./gradlew :apps:{app}:feature:{name}:test` passes

### 10. Build
- [ ] `./gradlew :apps:{app}:feature:{name}:assembleDebug` succeeds
- [ ] `./gradlew :apps:{app}:app:assembleDebug` succeeds (app compiles with the module)
- [ ] `./gradlew detekt` passes for this module
- [ ] `./gradlew test` passes (including ArchUnit)

## Output format

```
# Module Audit: apps/{app}/feature/{name}

## Results
✅ Module structure — all files present
✅ Dependencies — no cross-feature imports
❌ Feature flag — missing entry in dhruv-{app}.json
✅ Fault isolation — FeatureHost wrapping confirmed
⚠️ Observability — no Performance trace found
✅ Data layer — DhruvEntity compliant, userId indexed
✅ Security — no secrets found
✅ DPDP — consent gate present
❌ Tests — no ViewModel test
✅ Build — compiles, detekt clean

## Actions required
1. Add flag entry to platform/feature-flags/dhruv-{app}.json
2. Add Performance trace (e.g., "{name}_load")
3. Create {Name}ViewModelTest with initial state test

## Verdict: ❌ NOT READY TO MERGE (2 failures, 1 warning)
```

## Quick audit command
For a fast check without full analysis, run:
```bash
./gradlew :apps:{app}:feature:{name}:assembleDebug test detekt
```
If all three pass, the build/test/lint checks are covered. The rest (flags, fault isolation,
observability, security) need the manual/AI audit above.
