// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    id ("org.jetbrains.kotlin.android") version "1.8.20" apply false
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"

}


// Only apply the Nexus publishing plugin if we have the required credentials
val sonatypeUsername = project.findProperty("sonatypeUsername")?.toString()
val sonatypePassword = project.findProperty("sonatypePassword")?.toString()

if (sonatypeUsername != null && sonatypePassword != null) {
    configure<io.github.gradlenexus.publishplugin.NexusPublishExtension> {
        repositories {
            sonatype {
                username.set(sonatypeUsername)
                password.set(sonatypePassword)
                
                // Configure the repository URLs
                nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
                snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
                
                // Optional: Configure the staging profile ID if you have one
                stagingProfileId.set(project.findProperty("stagingProfileId")?.toString())
            }
        }
    }
} else {
    logger.warn("Sonatype credentials not found. Publishing to Maven Central will not be available.")
    logger.warn("Please set sonatypeUsername and sonatypePassword in ~/.gradle/gradle.properties")
}
