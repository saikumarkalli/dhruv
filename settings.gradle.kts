pluginManagement {
    includeBuild("build-logic")
    // Default versions for plugins applied via bare id("...") inside convention plugin bodies.
    // alias(libs.plugins.*) in build files handles versioning itself; this covers the gap
    // for plugins that convention plugins apply without an inline version.
    plugins {
        id("com.google.devtools.ksp")          version "2.3.5"
        id("com.google.dagger.hilt.android")   version "2.52"
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
include(":libs:core")
include(":libs:settings")
