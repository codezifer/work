import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val appTargetSdk = libs.versions.appMaxSdk.get().toInt()
val appMinSdk = libs.versions.appMinSdk.get().toInt()
val rootJvmVersion = libs.versions.appJvmVersion.get().toInt()
val androidNdkVersion = libs.versions.ndk.get()
val cmakeVersion = libs.versions.cmakeVersion.get()
val compatibility: JavaVersion = JavaVersion.toVersion(rootJvmVersion)

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "de.carsten.android.muzzic.visualization"
    compileSdk = appTargetSdk
    ndkVersion = androidNdkVersion

    defaultConfig {
        minSdk = appMinSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = cmakeVersion
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = compatibility
        targetCompatibility = compatibility
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(rootJvmVersion.toString()))
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    android = true
    ignoreFailures = false
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

dependencies {
    implementation(projects.logging)

    // kotlin
    val kotlinxCoroutinesBom = platform(libs.kotlinx.coroutines)
    implementation(kotlinxCoroutinesBom)
    implementation(libs.kotlinx.coroutines.code)

    // core & compose
    implementation(libs.androidx.core.ktx)
    val composeBomPlatform = platform(libs.androidx.compose.bom)
    implementation(composeBomPlatform)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.foundation.layout)

    // media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.common)

    // koin
    implementation(libs.koin.android)
    implementation(libs.koin.android.compose)

    // compose tooling
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.manifest)

    // testing
    testImplementation(libs.junit)
    testImplementation(libs.test.assertj)
    testImplementation(libs.mockk)
}
