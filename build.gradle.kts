plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.appdistribution) apply false
    alias(libs.plugins.androidx.navigation.safeargs) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
}

// Per-module JVM target is set to 21 via the convention plugins (compileOptions +
// kotlin jvmToolchain) and the baselineprofile module's own config.

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
