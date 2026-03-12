// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    kotlin("kapt") version "2.3.10"
    id("com.google.gms.google-services") version "4.4.4" apply false
}