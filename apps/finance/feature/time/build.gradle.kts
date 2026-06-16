plugins {
    id("dhruv.android.library")
    id("dhruv.android.compose")
}

android {
    namespace = "com.dhruv.finance.time"
}

dependencies {
    implementation(project(":apps:finance:data"))
    implementation(project(":libs:core"))
    implementation(project(":libs:settings"))

    // BootReceiver opens AppDatabase directly outside the Koin scope (BroadcastReceiver lifecycle),
    // so Room's RoomDatabase supertype must be on this module's classpath. See CHANGELOG / follow-up.
    implementation(libs.androidx.room.runtime)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // DI (Koin)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
}
