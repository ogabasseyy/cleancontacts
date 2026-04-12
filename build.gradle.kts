buildscript {
    val jose4jVersion = "0.9.6"
    val jdom2Version = "2.0.6.1"
    val commonsLang3Version = "3.20.0"
    val httpClientVersion = "4.5.14"
    val nettyVersion = "4.1.132.Final"

    configurations.all {
        resolutionStrategy {
            force("org.bitbucket.b_c:jose4j:$jose4jVersion")
            force("org.jdom:jdom2:$jdom2Version")
            force("org.apache.commons:commons-lang3:$commonsLang3Version")
            force("org.apache.httpcomponents:httpclient:$httpClientVersion")
            force("io.netty:netty-codec-http:$nettyVersion")
            force("io.netty:netty-codec-http2:$nettyVersion")
            force("io.netty:netty-codec:$nettyVersion")
            force("io.netty:netty-buffer:$nettyVersion")
            force("io.netty:netty-common:$nettyVersion")
            force("io.netty:netty-handler:$nettyVersion")
            force("io.netty:netty-handler-proxy:$nettyVersion")
            force("io.netty:netty-resolver:$nettyVersion")
            force("io.netty:netty-transport:$nettyVersion")
            force("io.netty:netty-transport-native-unix-common:$nettyVersion")
            force("io.netty:netty-codec-socks:$nettyVersion")
        }
    }
}

// Top-level build file for Contacts Cleaner KMP project
plugins {
    // Android plugins (apply false - applied in subprojects)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // 2026 Best Practice: New KMP-native Android library plugin
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false

    // Kotlin plugins
    // Note: kotlin.android removed — AGP 9.0 has built-in Kotlin support
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false

    // Compose Multiplatform
    alias(libs.plugins.compose.multiplatform) apply false

    // Build tools
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

subprojects {
    configurations.all {
        resolutionStrategy {
            val jose4jVersion = libs.versions.jose4j.get()
            val jdom2Version = libs.versions.jdom2.get()
            val protobufVersion = libs.versions.protobuf.get()
            val commonsCompressVersion = libs.versions.commonsCompress.get()
            val commonsLang3Version = libs.versions.commonsLang3.get()
            val httpClientVersion = libs.versions.httpClient.get()
            val nettyVersion = libs.versions.netty.get()

            // jose4j: CVE-2023-31582, CVE-2023-51775, CVE-2024-29371
            force("org.bitbucket.b_c:jose4j:$jose4jVersion")

            // JDOM2: CVE-2021-33813 XXE
            force("org.jdom:jdom2:$jdom2Version")

            // Protobuf: CVE-2024-7254 DoS
            force("com.google.protobuf:protobuf-java:$protobufVersion")
            force("com.google.protobuf:protobuf-kotlin:$protobufVersion")
            force("com.google.protobuf:protobuf-java-util:$protobufVersion")

            // Commons Compress: CVE-2024-25710, CVE-2024-26308
            force("org.apache.commons:commons-compress:$commonsCompressVersion")

            // Commons Lang3: CVE-2025-48924 StackOverflow/DoS
            force("org.apache.commons:commons-lang3:$commonsLang3Version")

            // Apache HttpClient: CVE-2020-13956 XSS
            force("org.apache.httpcomponents:httpclient:$httpClientVersion")

            // Netty: CVE-2026-33870, CVE-2026-33871
            force("io.netty:netty-codec-http:$nettyVersion")
            force("io.netty:netty-codec-http2:$nettyVersion")
            force("io.netty:netty-codec:$nettyVersion")
            force("io.netty:netty-buffer:$nettyVersion")
            force("io.netty:netty-common:$nettyVersion")
            force("io.netty:netty-handler:$nettyVersion")
            force("io.netty:netty-handler-proxy:$nettyVersion")
            force("io.netty:netty-resolver:$nettyVersion")
            force("io.netty:netty-transport:$nettyVersion")
            force("io.netty:netty-transport-native-unix-common:$nettyVersion")
            force("io.netty:netty-codec-socks:$nettyVersion")
        }
    }
}
