package com.example.arch

import com.tngtech.archunit.base.DescribedPredicate.alwaysTrue
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.Test

/**
 * ArchUnit dependency-boundary enforcement.
 *
 * Rules targeting future package layouts (feature.*, vault.*) use allowEmptyShould(true)
 * so they pass vacuously today and enforce automatically once those packages are created
 * in Phase 4–6. Legacy com.example.* code is grandfathered — it will be migrated in Phase 5.
 */
class DependencyRulesTest {

    private val classes by lazy {
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("com.example", "com.dhruv")
    }

    /**
     * Finance feature modules must not depend on each other (PLATFORM.md §4, ADR-0001).
     * Each `com.dhruv.finance.<feature>` package is a slice; slices must not depend on one another.
     * Dependencies onto the shared :apps:finance:data layer (`com.dhruv.finance.data`) are allowed
     * (features → data via Repository is the sanctioned path), so they are ignored here.
     */
    @Test
    fun `finance feature modules must not depend on each other`() {
        slices()
            .matching("com.dhruv.finance.(*)..")
            .should().notDependOnEachOther()
            .ignoreDependency(alwaysTrue(), resideInAPackage("com.dhruv.finance.data.."))
            .check(classes)
    }

    /** :libs:core must not import anything from individual apps (active now). */
    @Test
    fun `core lib must not depend on app-specific code`() {
        noClasses()
            .that().resideInAPackage("com.dhruv.core..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.dhruv.finance..",
                "com.dhruv.tools..",
                "com.dhruv.vault..",
                "com.example.."
            )
            .because("core→app imports are FORBIDDEN — core is a shared library")
            .allowEmptyShould(true)
            .check(classes)
    }

    /**
     * Vault must be completely isolated from network/ai/analytics.
     * Enforces once com.dhruv.vault.* exists (Phase 6).
     */
    @Test
    fun `vault must not depend on network, ai or analytics packages`() {
        noClasses()
            .that().resideInAPackage("com.dhruv.vault..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..network..", "..ai..", "..analytics..")
            .because("vault→network/ai/analytics is FORBIDDEN (PLATFORM.md §6 vault isolation)")
            .allowEmptyShould(true)
            .check(classes)
    }

    /**
     * Feature UI must not import DAOs directly — must go through Repository.
     * Enforces once com.dhruv.*.feature.*.ui.* packages exist (Phase 4).
     */
    @Test
    fun `feature UI must not import DAOs directly`() {
        noClasses()
            .that().resideInAPackage("com.dhruv..feature..ui..")
            .should().dependOnClassesThat()
            .haveNameMatching(".*Dao")
            .because("feature UI must access data only through Repository classes")
            .allowEmptyShould(true)
            .check(classes)
    }

    /**
     * New feature screens must not import other screens (NavHost-based navigation).
     * Scoped to com.dhruv.* only — legacy com.example.* screens are grandfathered.
     * Enforces once Phase 4 feature screens are created.
     */
    @Test
    fun `new feature screens must not import other screens directly`() {
        noClasses()
            .that().haveNameMatching(".*Screen")
            .and().resideInAPackage("com.dhruv..")
            .should().dependOnClassesThat()
            .haveNameMatching(".*Screen")
            .because("screens must navigate via NavHost, not direct class references")
            .allowEmptyShould(true)
            .check(classes)
    }
}
