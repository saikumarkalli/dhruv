plugins {
    id("dhruv.android.library")
    id("dhruv.android.compose")
}

android {
    namespace = "com.dhruv.core"
}

dependencies {
    implementation(libs.androidx.core.ktx)

    // Security
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.sqlcipher.android)

    // Observability
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)

    // Feature flags
    implementation(libs.firebase.remote.config)

    // Play Integrity
    implementation(libs.play.integrity)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
}
