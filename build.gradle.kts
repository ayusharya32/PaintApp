// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.hilt.android) apply false
}

buildscript {
    dependencies {
        val navVersion = "2.8.8"
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")
    }
}