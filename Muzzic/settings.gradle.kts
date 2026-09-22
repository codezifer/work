pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
// NOTE: foojay version is intentionally hardcoded here.
// The settings `plugins {}` block is evaluated before the version catalog,
// so `alias(libs....)` is not available in this scope. Keep in sync with
// `foojayResolver` in gradle/libs.versions.toml.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Muzzic"
include(":app", ":logging", ":id3", ":playlist", ":visualization")
