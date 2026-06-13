plugins {
    `kotlin-dsl`
}

group = "com.dhruv.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    implementation(libs.hilt.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
}
