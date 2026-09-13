plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
}

dependencies {
    implementation(project(":logging"))
    testImplementation(libs.junit)
    testImplementation(libs.test.assertj)
}
