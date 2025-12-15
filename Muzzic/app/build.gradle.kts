import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

val rootKotlinVersion = "2.2"
val rootTargetSdk = 36
val rootJvmVersion = "21"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.room)
    alias(libs.plugins.ksp)
}

android {
    namespace = "de.carsten.android.muzzic"
    compileSdk = rootTargetSdk

    defaultConfig {
        applicationId = "de.carsten.android.muzzic"
        minSdk = rootTargetSdk - 1
        targetSdk = rootTargetSdk
        versionCode = 1
        versionName = "0.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildToolsVersion = "36.0.0"
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(rootJvmVersion)
        languageVersion = KotlinVersion.fromVersion(rootKotlinVersion)
        apiVersion = KotlinVersion.fromVersion(rootKotlinVersion)
    }
    sourceSets.all {
        languageSettings.enableLanguageFeature("ExplicitBackingFields")
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.media)

    // jestpack compose
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
    implementation(libs.coil3)

    // charts
    implementation(libs.charts)

    // mp3agic
    implementation(libs.mp3agic)

    // paging
    implementation(libs.androidx.paging)
    implementation(libs.androidx.paging.compose)

    // permissions
    implementation(libs.permissions)

    // testing
    testImplementation(composeBomPlatform)
    testImplementation(libs.junit)

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
