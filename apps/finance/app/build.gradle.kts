plugins {
    id("dhruv.android.application")
    id("dhruv.android.compose")
    // dhruv.hilt intentionally not applied yet: app is on Koin (see deps below).
    // The Hilt Gradle plugin (2.52) is also incompatible with AGP 9 (looks up the
    // removed BaseExtension). Re-add when the Koin→Hilt migration lands on a
    // Hilt version that supports AGP 9.
    // KSP is needed for Room + Moshi codegen below (was previously pulled in by dhruv.hilt).
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.secrets)
}

android {
    namespace = "com.example"

    defaultConfig {
        applicationId = "com.dhruv.finance"
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests { isIncludeAndroidResources = true }
    }
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}

dependencies {
    // Firebase BOM (Compose BOM comes from dhruv.android.compose convention plugin)
    implementation(platform(libs.firebase.bom))

    // UI
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Data
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
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

    // DI (Koin — Hilt migration is a separate step)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // AI
    implementation(libs.google.generativeai)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
}

val appVersionName = (project.findProperty("VERSION_NAME") as? String) ?: "1.0"
base.archivesName.set("DhruvCalc-v${appVersionName}")
