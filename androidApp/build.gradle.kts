import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kover)
}

// A ValueSource is re-evaluated on every build; a plain now() would be frozen by the configuration cache.
abstract class UtcHourVersionCode : ValueSource<Int, ValueSourceParameters.None> {
    override fun obtain(): Int =
        DateTimeFormatter.ofPattern("yyMMddHH").format(ZonedDateTime.now(ZoneOffset.UTC)).toInt()
}

val versionProps = Properties().apply { rootProject.file("version.properties").inputStream().use(::load) }
val gitSha = providers.exec { commandLine("git", "rev-parse", "--short", "HEAD") }
    .standardOutput.asText.get().trim()
// Exit code, not output, so edits to an already-dirty tree don't invalidate the configuration cache.
val gitDirty = providers.exec {
    commandLine("git", "diff", "--quiet", "HEAD")
    isIgnoreExitValue = true
}.result.get().exitValue != 0

android {
    namespace = "dev.jketterer.leaflog.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.jketterer.leaflog"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = providers.of(UtcHourVersionCode::class) {}.get()
        versionName = versionProps.getProperty("VERSION_NAME") + "+$gitSha" + if (gitDirty) ".dirty" else ""
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    buildTypes {
        getByName("release") { isMinifyEnabled = false }
    }
    testOptions {
        // Flow tests run the real MainActivity, so Robolectric needs the merged resources
        unitTests.isIncludeAndroidResources = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)

    testImplementation(libs.robolectric)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.compose.uiTestJunit4)
    testImplementation(libs.compose.components.resources)
    // The android variant ships device .so files; the jvm one carries host natives
    testImplementation(libs.androidx.sqlite.bundled.jvm)

    kover(project(":shared"))
}

// Coverage of logic, merged with :shared. Screens and components are left to the preview render
// tests: excluding @Composable alone keeps the lambdas inside them.
kover {
    reports {
        filters {
            includes {
                packages(
                    "dev.jketterer.leaflog.domain",
                    "dev.jketterer.leaflog.data",
                    "dev.jketterer.leaflog.di",
                    "dev.jketterer.leaflog.services",
                    "dev.jketterer.leaflog.presentation.ui.navigation",
                    "dev.jketterer.leaflog.presentation.ui.messages",
                    "dev.jketterer.leaflog.presentation.ui.search",
                    "dev.jketterer.leaflog.presentation.ui.viewmodel",
                )
                classes(
                    "dev.jketterer.leaflog.MainActivity*",
                    "dev.jketterer.leaflog.LeafLogApplication*",
                    "dev.jketterer.leaflog.presentation.ui.screens.*ViewModel*",
                    "dev.jketterer.leaflog.presentation.ui.screens.*State*",
                    "dev.jketterer.leaflog.presentation.ui.screens.*Intent*",
                )
            }
            excludes {
                annotatedBy("androidx.compose.runtime.Composable")
                classes(
                    "*ComposableSingletons*",
                    "*_Impl",
                    "*_Impl\$*",
                    "*.LeafLogDatabaseConstructor",
                    "*\$\$serializer",
                )
            }
        }
        verify {
            // A ratchet: raise it as tests land, toward 70
            rule { minBound(56) }
        }
    }
}
