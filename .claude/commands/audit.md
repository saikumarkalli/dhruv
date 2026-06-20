Use the `dhruv-module-auditor` agent to run a read-only pre-merge compliance check.

Arguments: $ARGUMENTS (module path, or leave blank to audit all changed modules on the current branch)

Hand off to dhruv-module-auditor with this task: run a compliance audit on $ARGUMENTS. Check: ArchUnit dependency rules (feature→feature forbidden, vault→network/ai/analytics forbidden), FeatureHost wrapping on every route, feature flag entry exists in platform/feature-flags/, Koin module registered, DhruvEntity contract satisfied, no secrets or API keys present. Report pass/fail per check. Do NOT edit any files — report only.
