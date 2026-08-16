import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("dhruv.detekt")
}

// AGP 9.x auto-applies kotlin.android when Kotlin sources are present.
// Explicitly applying it here would register the 'kotlin' extension twice → build error.
// We configure the JVM target via pluginManager.withPlugin so it runs after Kotlin is ready.

configure<ApplicationExtension> {
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        targetSdk = 37
        versionCode = (project.findProperty("VERSION_CODE") as? String)?.toInt() ?: 1
        versionName = (project.findProperty("VERSION_NAME") as? String) ?: "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
            val keystoreFile = file(keystorePath)
            val storePass = System.getenv("STORE_PASSWORD")
            if (keystoreFile.exists() && !storePass.isNullOrBlank()) {
                storeFile = keystoreFile
                storePassword = storePass
                keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
                keyPassword = System.getenv("KEY_PASSWORD")
            } else {
                logger.warn("[dhruv.android.application] Release keystore not found; unsigned APK will be produced.")
            }
        }
    }

    buildTypes {
        release {
            isCrunchPngs = false
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) signingConfig = releaseSigning
        }
        debug {
            isMinifyEnabled = false
            enableUnitTestCoverage = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

pluginManager.withPlugin("org.jetbrains.kotlin.android") {
    extensions.getByType<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension>()
        .compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
}
