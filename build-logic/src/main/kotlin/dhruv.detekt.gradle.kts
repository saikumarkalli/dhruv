import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    id("io.gitlab.arturbosch.detekt")
}

configure<DetektExtension> {
    config.setFrom("${rootDir}/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    autoCorrect = false
    parallel = true
}
