// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    id ("org.jetbrains.kotlin.android") version "1.8.20" apply false
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}
