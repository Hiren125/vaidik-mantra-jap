// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
//    alias(libs.plugins.kotlinAndroid) apply false // Matches [plugins] kotlinAndroid
    alias(libs.plugins.ksp) apply false           // Matches [plugins] ksp
    id("com.google.gms.google-services") version "4.4.1" apply false
}