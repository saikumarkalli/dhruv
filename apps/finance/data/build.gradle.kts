plugins {
    id("dhruv.android.library")
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.dhruv.finance.data"
}

dependencies {
    implementation(project(":libs:core"))

    // Data
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Network
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // AI
    implementation(libs.google.generativeai)

    // DI (Koin)
    implementation(libs.koin.android)
}
