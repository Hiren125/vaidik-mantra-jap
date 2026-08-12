plugins {
    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlinAndroid) // Accesses the camelCase name from TOML
    alias(libs.plugins.ksp)           // Replaces 'kapt' for 2026 compatibility
    id("com.google.gms.google-services") //
}

android {
    namespace = "com.vedicapps.mantrajap"

    // Proper 2026 syntax for SDK 36
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vedicapps.mantrajap"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

dependencies {
// Google Mobile Ads SDK
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    //new
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Room Database with modern KSP
    val roomVersion = "2.7.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("androidx.appcompat:appcompat:1.7.1")

    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation("com.google.firebase:firebase-database-ktx:20.3.0")
    implementation("com.google.firebase:firebase-config-ktx:21.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Core UI Libraries
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation(libs.androidx.constraintlayout)


    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}