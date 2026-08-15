pluginManagement {
    includeBuild("build-logic")
    // Default versions for plugins applied via bare id("...") inside convention plugin bodies.
    // alias(libs.plugins.*) in build files handles versioning itself; this covers the gap
    // for plugins that convention plugins apply without an inline version.
    plugins {
        id("com.google.devtools.ksp")          version "2.3.5"
        id("io.gitlab.arturbosch.detekt")      version "1.23.7"
    }
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "dhruv"

include(":apps:finance:app")
include(":apps:finance:data")
include(":apps:finance:feature:calculator")
include(":apps:finance:feature:loans")
include(":apps:finance:feature:investments")
include(":apps:finance:feature:tax")
include(":apps:finance:feature:everyday")
include(":apps:finance:feature:currency")
include(":apps:finance:feature:unit")
include(":apps:finance:feature:date")
include(":apps:finance:feature:time")
include(":apps:finance:feature:assistant")
include(":apps:finance:feature:onboarding")
include(":libs:core")
include(":libs:settings")

// Physical layout groups feature modules by the DhruvNext tab that owns them
// (apps/finance/feature/<home|money|calc|plan|insights|onboarding|shell>/<module>/) — see
// apps/finance/feature/README.md. Gradle coordinates above are deliberately UNCHANGED (every
// `project(":apps:finance:feature:X")` reference across build.gradle.kts files and every
// `./gradlew :apps:finance:feature:X:...` command in docs keeps working); only each module's
// projectDir is remapped to its new physical location.
project(":apps:finance:feature:calculator").projectDir = file("apps/finance/feature/calc/calculator")
project(":apps:finance:feature:loans").projectDir = file("apps/finance/feature/plan/loans")
project(":apps:finance:feature:investments").projectDir = file("apps/finance/feature/plan/investments")
project(":apps:finance:feature:tax").projectDir = file("apps/finance/feature/plan/tax")
project(":apps:finance:feature:everyday").projectDir = file("apps/finance/feature/plan/everyday")
project(":apps:finance:feature:currency").projectDir = file("apps/finance/feature/shell/currency")
project(":apps:finance:feature:unit").projectDir = file("apps/finance/feature/shell/unit")
project(":apps:finance:feature:date").projectDir = file("apps/finance/feature/shell/date")
project(":apps:finance:feature:time").projectDir = file("apps/finance/feature/shell/time")
project(":apps:finance:feature:assistant").projectDir = file("apps/finance/feature/shell/assistant")
project(":apps:finance:feature:onboarding").projectDir = file("apps/finance/feature/onboarding/onboarding")
