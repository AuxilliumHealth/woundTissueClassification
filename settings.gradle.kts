pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/AuxilliumHealth/woundTissueClassification")
            credentials {
                username = providers.gradleProperty("GITHUB_USER").getOrElse(System.getenv("GITHUB_USER") ?: "")
                password = providers.gradleProperty("GITHUB_TOKEN").getOrElse(System.getenv("GITHUB_TOKEN") ?: "")
             }
        }
    }
}

rootProject.name = "imaging"
include(":app")
include(":woundtissueclassification")