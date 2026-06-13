import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension

// kotlin.plugin.compose applied outside plugins { } for the same accessor-gen reason as
// kotlin.android in dhruv.android.application / dhruv.android.library.
apply(plugin = "org.jetbrains.kotlin.plugin.compose")

val libs = the<VersionCatalogsExtension>().named("libs")

// AGP 9.x: CommonExtension is no longer generic, so we use pluginManager.withPlugin
// to configure the concrete extension type present in the consuming module.
pluginManager.withPlugin("com.android.application") {
    configure<ApplicationExtension> {
        buildFeatures { compose = true }
    }
}
pluginManager.withPlugin("com.android.library") {
    configure<LibraryExtension> {
        buildFeatures { compose = true }
    }
}

dependencies {
    "implementation"(platform(libs.findLibrary("androidx-compose-bom").get()))
    "implementation"(libs.findLibrary("androidx-compose-ui").get())
    "implementation"(libs.findLibrary("androidx-compose-ui-graphics").get())
    "implementation"(libs.findLibrary("androidx-compose-ui-tooling-preview").get())
    "implementation"(libs.findLibrary("androidx-compose-material3").get())
    "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
    "debugImplementation"(libs.findLibrary("androidx-compose-ui-test-manifest").get())
}
