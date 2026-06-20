Use the `dhruv-feature-builder` agent to scaffold a new feature module.

Arguments: $ARGUMENTS

Hand off to dhruv-feature-builder with this task: scaffold a new feature module for $ARGUMENTS. Follow the dhruv-feature-scaffold skill, create the Gradle module under the correct app's feature/ directory, register it in settings.gradle.kts, add a feature flag entry in platform/feature-flags/, wrap the route in FeatureHost, and wire Koin DI. End by handing off to dhruv-module-auditor for a pre-merge compliance check.
