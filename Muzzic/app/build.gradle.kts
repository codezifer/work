import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val appName = "muzzic"
val appId = "de.carsten.android.$appName"
val appVersion = libs.versions.appVersion.get()
val appBuildTime = System.currentTimeMillis()
val appApkName = "muzzic-$appVersion.apk"
val appTargetSdk = libs.versions.appMaxSdk.get().toInt()
val appMinSdk = libs.versions.appMinSdk.get().toInt()
val rootJvmVersion = libs.versions.appJvmVersion.get().toInt()
val compatibility: JavaVersion = JavaVersion.toVersion(rootJvmVersion)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
val muzzicBuild = layout.buildDirectory
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

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
        buildConfigField("long", "BUILD_TIME", appBuildTime.toString())
    }

    signingConfigs {
        create("release") {
            storeFile = file("$rootDir/app/keystore/release-key.jks")
            storePassword = localProperties.getProperty("keystore.password")
            keyAlias = localProperties.getProperty("keystore.alias")
            keyPassword = localProperties.getProperty("keystore.key.password")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
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
    // sub-modules
    implementation(projects.logging)
    implementation(projects.id3)
    implementation(projects.playlist)

    // kotlin
    val kotlinxCoroutinesBom = platform(libs.kotlinx.coroutines)
    implementation(kotlinxCoroutinesBom)
    implementation(libs.kotlinx.coroutines.code)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.kotlin.reflection)

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
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.androidx.room.compiler)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.serialization.json)
    androidTestImplementation(libs.kotlinx.serialization.core)

    // koin
    implementation(libs.koin.android)
    implementation(libs.koin.android.compose)

    // coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    // okhttp
    implementation(libs.okhttp)

    // paging
    implementation(libs.androidx.paging)
    implementation(libs.androidx.paging.compose)

    // permissions
    implementation(libs.permissions)

    // testing
    testImplementation(composeBomPlatform)
    testImplementation(libs.junit)
    testImplementation(libs.test.assertj)
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
    androidTestImplementation(libs.test.assertj)
    androidTestImplementation(libs.mockk.android)
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
    register("prepareRelease") {
        group = "release"
        description = "Prepares code for release build"
        dependsOn("ktlintFormat", "ktlintCheck")
        val appRelease = "$appName-$appVersion"
        doLast {
            println("Prepared release $appRelease")
        }
    }
    register("buildReleaseApk") {
        group = "release"
        description = "Assembles the release APK"
        dependsOn("prepareRelease", "assembleRelease")
        val apkFile = muzzicBuild.dir("outputs/apk/release").map { it.file(appApkName) }
        doLast {
            println("Release APK build successfully!")
            println("You can find it: ${apkFile.get().asFile.absolutePath}")
        }
    }
}
