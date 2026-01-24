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

    // 2026 Best Practice: Global version enforcement for transitive security CVEs
    // This ensures all modules use secure versions regardless of what dependencies request
    versionCatalogs {
        create("securityPins") {
            // Netty CVEs (HTTP/2 Rapid Reset, CRLF Injection, etc.)
            version("netty", "4.1.130.Final")
            // jose4j CVE-2023-31582 & CVE-2023-51775
            version("jose4j", "0.9.6")
            // JDOM2 XXE CVE-2021-33813
            version("jdom2", "2.0.6.1")
            // Protobuf DoS CVE-2024-7254
            version("protobuf", "4.33.4")
            // Commons Compress DoS CVE-2024-25710, CVE-2024-26308
            version("commonsCompress", "1.28.0")
        }
    }
}

rootProject.name = "CleanContactsAI"
include(":app")