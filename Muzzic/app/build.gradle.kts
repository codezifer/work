import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val appId = "de.carsten.android.muzzic"
val appVersion = libs.versions.appVersion.get()
val appApkName = "muzzic-$appVersion.apk"
val rootKotlinVersion = "2.3"
val appTargetSdk = 37
val appMinSdk = 36
val rootJvmVersion = 17
val compatibility: JavaVersion = JavaVersion.toVersion(rootJvmVersion)

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
}

extensions.configure<ApplicationExtension> {
    namespace = appId
    compileSdk = appTargetSdk

    defaultConfig {
        applicationId = appId
        minSdk = appMinSdk
        targetSdk = appTargetSdk
        versionCode = 1
        versionName = appVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "SEED_DATABASE", "false")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("debug") {
            // Priority:
            // 1. project property (e.g. -PforceSeed=true or in local.properties)
            // 2. Automatic emulator detection
            val forceSeedProperty =
                project.findProperty("forceSeed")?.toString()
                    ?: project.findProperty("muzzic.seed_database")?.toString()

            val shouldSeed = forceSeedProperty ?: "false"
            buildConfigField("boolean", "SEED_DATABASE", shouldSeed)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
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

room {
    schemaDirectory("$projectDir/schemas")
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
    // kotlin
    val kotlinxCoroutinesBom = platform(libs.kotlinx.coroutines)
    implementation(kotlinxCoroutinesBom)
    implementation(libs.kotlinx.coroutines.code)
    implementation(libs.kotlinx.coroutines.guava)

    // core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.media)
    implementation(libs.androidx.media.session)
    implementation(libs.androidx.work.runtime)

    // jetpack compose
    val composeBomPlatform = platform(libs.androidx.compose.bom)
    implementation(composeBomPlatform)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.ext)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // ui
    implementation(libs.material)
    implementation(libs.googlefonts)

    // media3-exoplayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.ui)
    implementation(libs.media3.exoplayer.common)

    // room
    implementation(libs.androidx.room)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    // koin
    implementation(libs.koin.android)
    implementation(libs.koin.android.compose)

    // coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    // okhttp
    implementation(libs.okhttp)

    // jaudiotagger
    implementation(libs.jaudiotagger)

    // paging
    implementation(libs.androidx.paging)
    implementation(libs.androidx.paging.compose)

    // permissions
    implementation(libs.permissions)

    // testing
    testImplementation(composeBomPlatform)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    // android testing
    androidTestImplementation(composeBomPlatform)
    androidTestImplementation(libs.androidx.compose.ui.testing)
    androidTestImplementation(libs.androidx.junit.ktx)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(libs.koin.test)

    // debug deps.
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.manifest)
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set(appApkName)
        }
    }
}

tasks {
    register("buildReleaseApk") {
        group = "build"
        description = "Assembles the release APK"
        dependsOn("assembleRelease")
        doLast {
            println("Release APK build successfully!")
            println("You can find it: ${project.buildDir}/outputs/apk/release/$appApkName")
        }
    }
}
