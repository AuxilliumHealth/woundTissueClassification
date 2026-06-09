plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

kotlin {
    jvmToolchain(17)
}

group = "com.auxilliumhealth"
version = "1.0.4"

android {
    namespace = "com.auxilliumhealth.woundtissueclassification"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        mlModelBinding = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packagingOptions {
        jniLibs.useLegacyPackaging = false
        resources.excludes += setOf(
            "META-INF/DEPENDENCIES",
            "META-INF/NOTICE",
            "META-INF/LICENSE",
            "META-INF/LICENSE.txt",
            "META-INF/NOTICE.txt"
        )
    }
}

dependencies {
    // AndroidX
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // TensorFlow Lite
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.metadata)

    // CameraX
    val cameraxVersion = "1.5.0"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-extensions:$cameraxVersion")

    // OpenCV
    implementation("org.opencv:opencv:4.12.0")

    // Networking
    val retrofitVersion = "2.9.0"
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-scalars:$retrofitVersion")

    val okhttpVersion = "4.12.0"
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")

    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Utilities
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.airbnb.android:lottie:6.6.9")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")



    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
