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
    ":apps:finance:feature:onboarding",
    ":apps:finance:feature:networth",
    ":libs:core",
    ":libs:settings",
  )

// Global LINE-coverage floor for the merged report. A non-regression ratchet: it sits just under the
// current measured coverage and is bumped as tests land. Baseline was ~6.7%; 0.09 held through the
// data/core/assistant/date tests. Raised to 0.14 at the 004-settings 0b.5 checkpoint (T117) — merged
// coverage measured at 14.91% (:libs:settings 38.38%, :libs:core 15.02%) after 0b.1-0b.5. Most of the
// remaining uncovered code is Compose UI, which the JVM gate does not exercise. (Per-module floors
// are a later-phase refinement.)
val globalLineFloor = "0.14".toBigDecimal()

// Generated / non-logic classes excluded from the coverage denominator.
val coverageExcludes =
  listOf(
    "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
    "**/*_Factory.*", "**/*ComposableSingletons*.*",
    "**/databinding/**", "**/*Binding.*",
    "**/di/*Module*.*",
  )

// Resolves via Gradle's own project graph, not by string-munging the path into a directory guess —
// correct regardless of a module's physical location (feature modules are grouped into tab
// buckets under apps/finance/feature/<bucket>/, remapped in settings.gradle.kts's `projectDir`
// lines; naive path-to-directory munging broke silently the moment that remap landed).
fun moduleDir(path: String) = project(path).layout.projectDirectory

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

// ── Tracker money-precision guard (DAT-BR-008) ────────────────────────────────
// tracker/** must use Long paise everywhere for money — zero Double/Float (PLATFORM.md §5,
// ADR-0014 §4, ADR-0029). A plain regex scan (not a compiler plugin) keeps this cheap and
// dependency-free; it is not a type-system guarantee, just a fast, obvious tripwire.
//
// Implemented as a real Task subclass, not a `doLast { ... }` lambda — any lambda literal written
// directly in this script (even a "top-level" one assigned to a val) captures the build script
// instance as a synthetic `this$0` field, which the configuration-cache serializer rejects
// ("cannot serialize Gradle script object references", config_cache:requirements:disallowed_types).
// A task class's @TaskAction is a plain method with no such capture, and its only state is the
// two Gradle-managed properties below (ConfigurableFileCollection / DirectoryProperty), both of
// which are natively configuration-cache-safe.
abstract class CheckTrackerMoneyPrecisionTask : DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val trackerSources: ConfigurableFileCollection

  @get:Internal
  abstract val scanRootDir: DirectoryProperty

  @TaskAction
  fun check() {
    val moneyTypePattern = Regex("\\b(Double|Float)\\b")
    val root = scanRootDir.get().asFile
    val offenders = mutableListOf<String>()
    trackerSources.files.sortedBy { it.path }.forEach { f ->
      f.readLines().forEachIndexed { index, line ->
        if (moneyTypePattern.containsMatchIn(line)) {
          offenders += "${f.relativeTo(root)}:${index + 1}: ${line.trim()}"
        }
      }
    }
    if (offenders.isNotEmpty()) {
      throw GradleException(
        "DAT-BR-008 violation — Double/Float found under tracker/** (money must be Long paise):\n" +
          offenders.joinToString("\n"),
      )
    }
  }
}

val checkTrackerMoneyPrecision =
  tasks.register<CheckTrackerMoneyPrecisionTask>("checkTrackerMoneyPrecision") {
    group = "verification"
    description = "DAT-BR-008: fails if Double/Float appears under apps/finance/data/**/tracker/**/*.kt."
    trackerSources.from(
      fileTree("apps/finance/data/src/main") {
        include("**/tracker/**/*.kt")
      },
    )
    scanRootDir.set(layout.projectDirectory)
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
  dependsOn(checkTrackerMoneyPrecision)
}
