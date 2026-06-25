pluginManagement {
    repositories {
        google()
        // Maven Central can 403 shared egress IPs; keep a public mirror before Central.
        maven {
            name = "GoogleMavenCentralMirror"
            url = uri("https://maven-central.storage-download.googleapis.com/maven2/")
            mavenContent {
                releasesOnly()
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Keep local builds working when direct Maven Central access is blocked.
        maven {
            name = "GoogleMavenCentralMirror"
            url = uri("https://maven-central.storage-download.googleapis.com/maven2/")
            mavenContent {
                releasesOnly()
            }
        }
        mavenCentral()
    }
}

rootProject.name = "ContactsCleaner"

// KMP Modules (2026 Best Practice)
include(":shared")
include(":composeApp")

// Platform-specific app modules
include(":androidApp")
