plugins {
    id("dhruv.android.application")
    id("dhruv.android.compose")
    // DI is Koin, not Hilt (ADR-0010 — the Hilt Gradle plugin is incompatible with AGP 9).
    // KSP here is only for the Moshi codegen used by FeatureFlagAssetLoader.
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.secrets)
}

android {
    namespace = "com.dhruv.finance.app"

    defaultConfig {
        applicationId = "com.dhruv.finance"

        // Network/data config (no hardcoded values in :data — see PlatformModule.kt).
        buildConfigField("String", "CURRENCY_API_BASE_URL", "\"https://open.er-api.com/\"")
        buildConfigField("String", "CURRENCY_API_FALLBACK_BASE_URL", "\"https://api.exchangerate-api.com/\"")
        buildConfigField("long", "CURRENCY_API_TIMEOUT_SECONDS", "15L")
        buildConfigField(
            "String",
            "CURRENCY_API_USER_AGENT",
            "\"Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36\""
        )
        buildConfigField("String", "APP_DATABASE_NAME", "\"omni_calculator_db\"")
        buildConfigField("long", "HISTORY_RECYCLE_BIN_RETENTION_MILLIS", "${30 * 24 * 60 * 60 * 1000L}L")
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests { isIncludeAndroidResources = true }
    }

    sourceSets {
        // platform/feature-flags/*.json bundled as assets so the JSON files there remain the
        // single canonical source (no copy/duplication). dhruv-tools.json rides along too —
        // harmless (tiny, non-secret) since :apps:tools doesn't exist yet to conflict.
        getByName("main") {
            assets.srcDirs("${rootDir}/platform/feature-flags")
        }
    }
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}

dependencies {
    implementation(project(":libs:core"))
    implementation(project(":libs:settings"))

    // Shared data layer + feature modules (Phase 4 split)
    implementation(project(":apps:finance:data"))
    implementation(project(":apps:finance:feature:calculator"))
    implementation(project(":apps:finance:feature:loans"))
    implementation(project(":apps:finance:feature:investments"))
    implementation(project(":apps:finance:feature:tax"))
    implementation(project(":apps:finance:feature:everyday"))
    implementation(project(":apps:finance:feature:currency"))
    implementation(project(":apps:finance:feature:unit"))
    implementation(project(":apps:finance:feature:date"))
    implementation(project(":apps:finance:feature:time"))
    implementation(project(":apps:finance:feature:assistant"))
    implementation(project(":apps:finance:feature:onboarding"))

    // Firebase BOM (Compose BOM comes from dhruv.android.compose convention plugin)
    implementation(platform(libs.firebase.bom))

    // UI
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    // Settings' Account card renders the signed-in user's real Google profile photo
    // (SessionState.Active.avatarUrl) — first real consumer of this already-catalogued dependency.
    implementation(libs.coil.compose)

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Moshi — used by FeatureFlagAssetLoader to parse platform/feature-flags/*.json.
    // Room/Retrofit/OkHttp/Gemini deps deliberately live only in :apps:finance:data, which
    // encapsulates them; the app shell touches just :data's public types, so it needs none here.
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // DI (Koin)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // Testing
    testImplementation(libs.archunit.core)
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
base.archivesName.set("DhruvFinance-v${appVersionName}")
