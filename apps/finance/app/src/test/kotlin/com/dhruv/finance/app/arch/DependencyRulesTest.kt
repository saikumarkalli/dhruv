package com.dhruv.finance.app.arch

import com.tngtech.archunit.base.DescribedPredicate
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
            .withImportOption(excludeTestClasses)
            .importPackages("com.dhruv")
    }

    companion object {
        /**
         * Replaces `ImportOption.DoNotIncludeTests()`, found (2026-08-27, adding `SET-ARCH-003`)
         * to silently never exclude anything on this build: it checks for a literal `/test/` path
         * segment, but this project's Kotlin/AGP unit-test class output is
         * `build/tmp/kotlin-classes/debugUnitTest/…` — one camelCase `debugUnitTest` segment, not
         * `debug/test/`. Every existing rule below has therefore been importing test classes all
         * along; it only surfaced once a test class (`ModuleEntryIsolationTest`,
         * `ContributionValidityTest`) happened to violate one (`settings package must not
         * reference a feature-module type`, testing exactly the cross-module contribution helpers
         * these tests legitimately import). This predicate matches the real output layout instead.
         */
        private val excludeTestClasses =
            ImportOption { location ->
                val path = location.toString()
                !path.contains("UnitTest") && !path.contains("/test/") && !path.contains("androidTest")
            }
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
     * `SET-ARCH-003` (004-settings contract §5, FR-004): Settings must never hardcode a specific
     * module — the modules tier is *assembled* from `SettingsContribution`s resolved by type
     * ([com.dhruv.settings.contribution.SettingsRegistry]), never by importing a feature module's
     * own screen/viewmodel/config class into the shell's Settings package. `:apps:finance:data`
     * (repository layer) is not a feature module and is excluded, matching the sibling rule above.
     */
    @Test
    fun `settings package must not reference a feature-module type`() {
        noClasses()
            .that()
            .resideInAPackage("com.dhruv.finance.app.ui.settings..")
            .should()
            .dependOnClassesThat(
                resideInAPackage("com.dhruv.finance..")
                    .and(DescribedPredicate.not(resideInAPackage("com.dhruv.finance.data..")))
                    .and(DescribedPredicate.not(resideInAPackage("com.dhruv.finance.app..")))
                    .and(DescribedPredicate.not(resideInAPackage("com.dhruv.finance.mocks.."))),
            ).because("Settings must assemble modules by type, never hardcode one (FR-004)")
            .check(classes)
    }

    /**
     * `SET-ARCH-004` (contract §2 rule 3, §5): a `SettingsContribution` factory must stay
     * declarative data — no Compose type. Contributions are plain functions returning
     * `SettingsContribution` (e.g. `calculatorSettingsContribution()`), each living in its
     * feature's own `<feature>.settings` package by convention (T033–T035), so that package
     * pattern is what this rule targets. `allowEmptyShould` is deliberately **not** set — T036
     * (analysis finding U1) requires this rule to be authored only after real contributions exist,
     * specifically so it cannot pass vacuously; the explicit assertion below is the second guard.
     */
    @Test
    fun `a SettingsContribution factory must not reference a Compose type`() {
        val contributionClasses = classes.filter { it.packageName.matches(Regex("""com\.dhruv\.finance\.\w+\.settings""")) }
        org.junit.Assert.assertTrue(
            "expected at least one <feature>.settings package (T033-T035) — found none, so this rule would pass vacuously",
            contributionClasses.isNotEmpty(),
        )

        noClasses()
            .that()
            .resideInAPackage("com.dhruv.finance.*.settings")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("androidx.compose..")
            .because("a SettingsContribution must stay declarative data (contract §2 rule 3)")
            .check(classes)
    }

    /**
     * `FR-046` / contract §2 rule 10 / security checklist CHK046 (0b.4, T106's Sec pass): a
     * `SettingsContribution`'s own package must not reach the shell-owned security surfaces
     * directly — app lock and the secret-key store (`com.dhruv.core.security..`, which is where
     * both `AppLockDecision` and `EncryptedDataStoreFactory` live) and tracker consent
     * (`com.dhruv.finance.data.tracker.auth..`, where `ConsentRepository` lives). A contribution's
     * only sanctioned path to any of these is through `SettingsRepository`'s own public API — this
     * rule is what makes that a structural guarantee instead of a code-review convention.
     * Non-vacuous today: `calculator`/`currency`/`unit`/`assistant` settings packages all exist.
     */
    @Test
    fun `a SettingsContribution package must not reach shell-owned security surfaces directly`() {
        val contributionClasses = classes.filter { it.packageName.matches(Regex("""com\.dhruv\.finance\.\w+\.settings""")) }
        org.junit.Assert.assertTrue(
            "expected at least one <feature>.settings package — found none, so this rule would pass vacuously",
            contributionClasses.isNotEmpty(),
        )

        noClasses()
            .that()
            .resideInAPackage("com.dhruv.finance.*.settings")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.dhruv.core.security..", "com.dhruv.finance.data.tracker.auth..")
            .because("a contribution reaches app lock/secrets/consent only through SettingsRepository (FR-046)")
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
