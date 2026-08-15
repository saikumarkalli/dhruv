package com.dhruv.finance.app.arch

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
 * in Phase 4–6.
 */
class DependencyRulesTest {
    private val classes by lazy {
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("com.dhruv")
    }

    /**
     * Finance feature modules must not depend on each other (PLATFORM.md §4, ADR-0001).
     * Each `com.dhruv.finance.<feature>` package is a slice; slices must not depend on one another.
     * Dependencies onto the shared :apps:finance:data layer (`com.dhruv.finance.data`) are allowed
     * (features → data via Repository is the sanctioned path), so they are ignored here.
     * The app shell (`com.dhruv.finance.app`) is the composition root, not a feature peer — it is
     * expected to depend on every feature (NavHost wiring). The debug-only `com.dhruv.finance.mocks`
     * package (Compose @Preview scaffolding) similarly references real screens across features by
     * design. Dependencies originating from either are ignored too.
     */
    @Test
    fun `finance feature modules must not depend on each other`() {
        slices()
            .matching("com.dhruv.finance.(*)..")
            .should()
            .notDependOnEachOther()
            .ignoreDependency(alwaysTrue(), resideInAPackage("com.dhruv.finance.data.."))
            .ignoreDependency(resideInAPackage("com.dhruv.finance.app.."), alwaysTrue())
            .ignoreDependency(resideInAPackage("com.dhruv.finance.mocks.."), alwaysTrue())
            .check(classes)
    }

    /** :libs:core must not import anything from individual apps (active now). */
    @Test
    fun `core lib must not depend on app-specific code`() {
        noClasses()
            .that()
            .resideInAPackage("com.dhruv.core..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.dhruv.finance..",
                "com.dhruv.tools..",
                "com.dhruv.vault..",
            ).because("core→app imports are FORBIDDEN — core is a shared library")
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
            .that()
            .resideInAPackage("com.dhruv.vault..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..network..", "..ai..", "..analytics..")
            .because("vault→network/ai/analytics is FORBIDDEN (PLATFORM.md §6 vault isolation)")
            .allowEmptyShould(true)
            .check(classes)
    }

    /**
     * Feature modules must not import DAOs or DTOs directly — must go through Repository.
     * Real feature packages are `com.dhruv.finance.<feature>` (Phase 4 layout), not the
     * `feature.*.ui.*` shape this rule originally targeted — that pattern never matched and the
     * rule passed vacuously. `com.dhruv.finance.data` legitimately owns the DAOs/DTOs and is
     * excluded; `com.dhruv.finance.app` (Koin composition root, `PlatformModule.kt`) legitimately
     * wires `HistoryDao` into `HistoryRepository` at startup — same composition-root exception as
     * the sibling rule above — and is excluded too.
     *
     * The `.*Dto` half (module-standard doc §1.1) was previously vacuous — no DTO existed under
     * `com.dhruv.finance..` until `tracker/dto/GoTrueSessionDto.kt` (ADR-0029), which is what
     * makes this half of the rule non-vacuous for the first time.
     */
    @Test
    fun `feature modules must not import DAOs or DTOs directly`() {
        noClasses()
            .that()
            .resideInAPackage("com.dhruv.finance..")
            .and()
            .resideOutsideOfPackage("com.dhruv.finance.data..")
            .and()
            .resideOutsideOfPackage("com.dhruv.finance.app..")
            .should()
            .dependOnClassesThat()
            .haveNameMatching(".*Dao|.*Dto")
            .because("feature code must access data only through Repository classes")
            .allowEmptyShould(true)
            .check(classes)
    }

    /**
     * New feature screens must not import other screens (NavHost-based navigation).
     * Enforces once Phase 4 feature screens are created.
     */
    @Test
    fun `new feature screens must not import other screens directly`() {
        noClasses()
            .that()
            .haveNameMatching(".*Screen")
            .and()
            .resideInAPackage("com.dhruv..")
            .should()
            .dependOnClassesThat()
            .haveNameMatching(".*Screen")
            .because("screens must navigate via NavHost, not direct class references")
            .allowEmptyShould(true)
            .check(classes)
    }
}
