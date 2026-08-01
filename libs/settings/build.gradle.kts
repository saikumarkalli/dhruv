plugins {
    id("dhruv.android.library")
    id("dhruv.android.compose")
}

android {
    namespace = "com.dhruv.settings"
}

dependencies {
    // Core lib — provides EncryptedDataStoreFactory, CrashReporter, DhruvTheme, AppTheme, DhruvFont
    implementation(project(":libs:core"))

    // DataStore (plain preferences for legacy keys + encrypted for secure keys)
    implementation(libs.androidx.datastore.preferences)

    // Biometric
    implementation(libs.androidx.biometric)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // DI (Koin)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.core)
}
