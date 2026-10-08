import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
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
}
