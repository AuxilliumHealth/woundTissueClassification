plugins {
    alias(libs.plugins.android.application)

}

android {
    namespace = "com.auxilliumhealth.imaging"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.auxilliumhealth.imaging"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )

        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    ndkVersion = "27.1.12297006" // r27b — check your SDK Manager for installed version

    packagingOptions {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += listOf("META-INF/DEPENDENCIES", "META-INF/NOTICE", "META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/NOTICE.txt")
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(project(":woundtissueclassification"))
//    implementation("com.github.AuxilliumHealth:woundtissueclassification:1.0.0")
}
