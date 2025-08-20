import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.ksp)
}

android {
    namespace = "de.carsten.android.muzzic"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.carsten.android.muzzic"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildToolsVersion = "36.0.0"
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("17")
        languageVersion = KotlinVersion.fromVersion("2.1")
        apiVersion = KotlinVersion.fromVersion("2.1")
    }
}

dependencies {
    // core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity)

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

    // media3-exoplayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.ui)
    implementation(libs.media3.exoplayer.common)

    // room
    implementation(libs.androidx.room)
    implementation(libs.androidx.room.ktx)

    // koin
    implementation(libs.koin.android)
    implementation(libs.koin.android.compose)

    // coil
    implementation(libs.coil)

    // charts
    implementation(libs.charts)

    // mp3agic
    implementation(libs.mp3agic)

    // permissions
    implementation(libs.permissions)

    // testing
    testImplementation(composeBomPlatform)
    testImplementation(libs.junit)
    androidTestImplementation(composeBomPlatform)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
