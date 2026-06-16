plugins {
    id("dhruv.android.library")
    id("dhruv.android.compose")
}

android {
    namespace = "com.dhruv.finance.investments"
}

dependencies {
    implementation(project(":apps:finance:data"))
    implementation(project(":libs:core"))
    implementation(project(":libs:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // DI (Koin)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
}
