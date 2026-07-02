import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    id("io.gitlab.arturbosch.detekt")
}

configure<DetektExtension> {
    config.setFrom("${rootDir}/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    autoCorrect = false
    parallel = true
    // Pre-existing complexity in the migrated feature modules (e.g. the recursive-descent
    // calculator engine, the large CalculatorViewModel) is grandfathered per-module so the gate
    // enforces "no NEW violations". Regenerate a module's baseline with `:<module>:detektBaseline`.
    // Run `./gradlew detektBaseline` only when intentionally accepting new debt.
    baseline = file("${projectDir}/detekt-baseline.xml")
}
