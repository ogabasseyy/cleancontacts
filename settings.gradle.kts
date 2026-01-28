pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CleanContactsAI"

// KMP Modules (2026 Best Practice)
include(":shared")
include(":composeApp")

// Platform-specific app modules
include(":androidApp")
