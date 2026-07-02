plugins {
    `kotlin-dsl`
}

group = "com.dhruv.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    // implementation (not compileOnly): the dhruv.detekt convention plugin applies this at
    // runtime via its plugins {} block, so the marker artifact must be on the runtime classpath.
    implementation(libs.detekt.gradlePlugin)
}
