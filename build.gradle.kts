// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  // Root-only: provides the JaCoCo tooling for the aggregated report/verification tasks below.
  // Coverage is JaCoCo, not Kover — Kover's Android integration doesn't recognize AGP 9 variants
  // (no per-variant report tasks are created), the same AGP-9 incompatibility that rules out Hilt
  // (ADR-0010). Modules emit exec data via `enableUnitTestCoverage = true` (set in the
  // dhruv.android.library / dhruv.android.application convention plugins); aggregation lives here.
  jacoco
}

jacoco {
  toolVersion = libs.versions.jacoco.get()
}

// ── Coverage aggregation across all modules ───────────────────────────────────
// Every module whose coverage feeds the merged report + the regression gate.
val coveredModules =
  listOf(
    ":apps:finance:app",
    ":apps:finance:data",
    ":apps:finance:feature:calculator",
    ":apps:finance:feature:loans",
    ":apps:finance:feature:investments",
    ":apps:finance:feature:tax",
    ":apps:finance:feature:everyday",
    ":apps:finance:feature:currency",
    ":apps:finance:feature:unit",
    ":apps:finance:feature:date",
    ":apps:finance:feature:time",
    ":apps:finance:feature:assistant",
    ":libs:core",
    ":libs:settings",
  )

// Global LINE-coverage floor for the merged report. A non-regression ratchet: it sits just under the
// current measured coverage and is bumped as tests land. Baseline was ~6.7%; with the data / core /
// assistant / date tests it is ~9.9%, so the floor is 0.09. Most of the remaining uncovered code is
// Compose UI, which the JVM gate does not exercise. (Per-module floors are a later-phase refinement.)
val globalLineFloor = "0.09".toBigDecimal()

// Generated / non-logic classes excluded from the coverage denominator.
val coverageExcludes =
  listOf(
    "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
    "**/*_Factory.*", "**/*ComposableSingletons*.*",
    "**/databinding/**", "**/*Binding.*",
    "**/di/*Module*.*",
  )

fun moduleDir(path: String) =
  rootProject.layout.projectDirectory.dir(path.removePrefix(":").replace(":", "/"))

// AGP 9 compiles Kotlin to `built_in_kotlinc/...`; older AGP/KGP and Java paths are included too so
// this survives layout changes. Non-existent dirs in a fileTree simply contribute nothing.
fun classDirsFor(path: String): FileCollection {
  val b = moduleDir(path).dir("build")
  val candidates =
    listOf(
      "intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes",
      "tmp/kotlin-classes/debug",
      "intermediates/javac/debug/compileDebugJavaWithJavac/classes",
      "intermediates/javac/debug/classes",
    )
  return files(candidates.map { d -> fileTree(b.dir(d)) { exclude(coverageExcludes) } })
}

fun sourceDirsFor(path: String): FileCollection {
  val m = moduleDir(path)
  return files(m.dir("src/main/java"), m.dir("src/main/kotlin"))
}

fun execDataFor(path: String): FileCollection {
  val b = moduleDir(path).dir("build")
  return files(
    fileTree(b.dir("outputs/unit_test_code_coverage/debugUnitTest")) { include("*.exec") },
    fileTree(b.dir("jacoco")) { include("*.exec") },
  )
}

val testTaskPaths = coveredModules.map { "$it:testDebugUnitTest" }

val jacocoAggregatedReport =
  tasks.register<JacocoReport>("jacocoAggregatedReport") {
    group = "verification"
    description = "Merged JaCoCo coverage report across all modules (debug unit tests)."
    dependsOn(testTaskPaths)
    executionData.from(coveredModules.map { execDataFor(it) })
    classDirectories.from(coveredModules.map { classDirsFor(it) })
    sourceDirectories.from(coveredModules.map { sourceDirsFor(it) })
    reports {
      html.required.set(true)
      xml.required.set(true)
      csv.required.set(false)
    }
  }

val jacocoCoverageVerification =
  tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    group = "verification"
    description = "Fails the build if merged line coverage is below the global floor."
    dependsOn(testTaskPaths)
    executionData.from(coveredModules.map { execDataFor(it) })
    classDirectories.from(coveredModules.map { classDirsFor(it) })
    sourceDirectories.from(coveredModules.map { sourceDirsFor(it) })
    violationRules {
      rule {
        limit {
          counter = "LINE"
          value = "COVEREDRATIO"
          minimum = globalLineFloor
        }
      }
    }
  }

// ── Pre-merge regression suite ────────────────────────────────────────────────
// Single entry point the CI test gate (and developers) run: every module's debug unit tests
// (which include ArchUnit + Robolectric), the merged coverage report, and the coverage floor gate.
tasks.register("regressionCheck") {
  group = "verification"
  description = "Pre-merge regression suite: all unit tests + ArchUnit + coverage report + floor."
  dependsOn(testTaskPaths)
  dependsOn(jacocoAggregatedReport)
  dependsOn(jacocoCoverageVerification)
}
