plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("signing")
}

kotlin {
    jvmToolchain(17)
}

group = "com.auxilliumhealth"
version = "1.0.1"

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
            version = "1.0.1"

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("Wound Tissue Classification SDK")
                description.set("AI-powered tissue segmentation and wound measurement SDK")
                url.set("https://github.com/AuxilliumHealth/woundTissueClassification")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("auxillium")
                        name.set("Auxillium Health")
                        email.set("contact@auxilliumhealth.com")
                    }
                }

                scm {
                    url.set("https://github.com/AuxilliumHealth/woundTissueClassification")
                    connection.set("scm:git:https://github.com/AuxilliumHealth/woundTissueClassification.git")
                    developerConnection.set("scm:git:ssh://github.com/AuxilliumHealth/woundTissueClassification.git")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["release"])
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.metadata)

    implementation("androidx.camera:camera-core:1.5.0")
    implementation("androidx.camera:camera-camera2:1.5.0")
    implementation("androidx.camera:camera-lifecycle:1.5.0")
    implementation("androidx.camera:camera-view:1.5.0")
    implementation("androidx.camera:camera-extensions:1.5.0")

    implementation("org.opencv:opencv:4.12.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.airbnb.android:lottie:6.6.9")
}
