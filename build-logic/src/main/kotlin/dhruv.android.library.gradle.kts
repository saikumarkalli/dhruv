import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("dhruv.detekt")
}

// AGP 9.x auto-applies kotlin.android when Kotlin sources are present.
// Same pattern as dhruv.android.application — configure JVM target via withPlugin.

configure<LibraryExtension> {
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            // Emit JaCoCo execution data from debug unit tests (consumed by dhruv.coverage).
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        // Robolectric needs merged Android resources; default-return stubs keep
        // un-mocked android.* calls from throwing in plain JVM unit tests.
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

pluginManager.withPlugin("org.jetbrains.kotlin.android") {
    extensions.getByType<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension>()
        .compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
}
