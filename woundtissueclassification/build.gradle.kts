plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

// Configure Kotlin JVM target
kotlin {
    jvmToolchain(17)
}

group = "com.auxilliumhealth"
version = "1.0"

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

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packagingOptions {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.txt"
            )
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.auxilliumhealth"
            artifactId = "woundtissueclassification"
            version = "1.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }


android {
    buildFeatures {
        viewBinding = true
    }
}}
dependencies {
    // AndroidX
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // TensorFlow Lite helpers
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.metadata)


    // CameraX
    implementation("androidx.camera:camera-core:1.5.0")
    implementation("androidx.camera:camera-camera2:1.5.0")
    implementation("androidx.camera:camera-lifecycle:1.5.0")
    implementation("androidx.camera:camera-view:1.5.0")
    implementation("androidx.camera:camera-extensions:1.5.0")

    // OpenCV
    implementation("org.opencv:opencv:4.12.0")    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // fixed version (3.x doesn’t exist)
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")    // latest stable
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0") // latest stable
    implementation("com.github.bumptech.glide:glide:4.16.0") // ✅ latest stable

    // Math
    implementation("org.apache.commons:commons-math3:3.6.1")

    // Media
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // UI
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.google.code.gson:gson:2.10.1") // ✅ updated version

    implementation("com.airbnb.android:lottie:6.6.9")

}