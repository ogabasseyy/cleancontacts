import java.util.Properties
import java.io.File

// Load local.properties or env vars
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

val waApiKey = localProperties.getProperty("WHATSAPP_DETECTOR_API_KEY")
    ?: System.getenv("WHATSAPP_DETECTOR_API_KEY")
    ?: ""
val waBaseUrl = localProperties.getProperty("WHATSAPP_DETECTOR_BASE_URL")
    ?: System.getenv("WHATSAPP_DETECTOR_BASE_URL")
    ?: "https://api.contactscleaner.tech"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    // 2026 AGP 9.0: New KMP-native Android library plugin
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

val secretsDir = layout.buildDirectory.dir("generated/secrets/kotlin")

val generateSecrets by tasks.registering {
    val apiKey = waApiKey
    val baseUrl = waBaseUrl
    val outputDir = secretsDir.get().asFile
    outputs.dir(outputDir)

    doLast {
        val file = File(outputDir, "com/ogabassey/contactscleaner/platform/Secrets.kt")
        file.parentFile.mkdirs()
        file.writeText("""
            package com.ogabassey.contactscleaner.platform

            internal object Secrets {
                const val WHATSAPP_DETECTOR_API_KEY = "$apiKey"
                const val WHATSAPP_DETECTOR_BASE_URL = "$baseUrl"
            }
        """.trimIndent())
    }
}

kotlin {
    // 2026 AGP 9.0: androidLibrary block replaces separate android {} block
    androidLibrary {
        namespace = "com.ogabassey.contactscleaner.shared"
        compileSdk = 36
        minSdk = 26
    }

    sourceSets {
        commonMain.dependencies {
            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // Datetime
            implementation(libs.kotlinx.datetime)

            // Koin DI
            implementation(libs.koin.core)

            // Room KMP
            implementation(libs.androidx.room.runtime)
            implementation(libs.sqlite.bundled)

            // Paging KMP
            implementation("androidx.paging:paging-common:3.3.6")

            // Multiplatform Settings
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)

            // Ktor HTTP Client (for WhatsApp API)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.websockets)

            // RevenueCat KMP (in-app purchases - await functions included in core)
            implementation(libs.purchases.kmp.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain {
            dependencies {
                // Android-specific Koin
                implementation(libs.koin.android)

                // Android Coroutines
                implementation(libs.kotlinx.coroutines.android)

                // Room Paging (Android)
                implementation(libs.androidx.room.paging)

                // Phone number formatting
                implementation(libs.libphonenumber)

                // Ktor CIO engine for Android
                implementation(libs.ktor.client.cio)
            }
            // Register generated secrets
            kotlin.srcDir(secretsDir)
        }

        iosMain.dependencies {
            // Ktor Darwin engine for iOS
            implementation(libs.ktor.client.darwin)
        }
    }

    // iOS targets with 2026 Best Practice compiler flags
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
            binaryOption("bundleId", "com.ogabassey.contactscleaner.shared")
        }
    }

    // 2026 Best Practice: Suppress expect/actual beta warning
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }
}

// 2026 AGP 9.0: Configure JVM target for all Kotlin compilations

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
    dependsOn(generateSecrets)
}

// 2026 Fix: Ensure KSP tasks depend on generated secrets
tasks.configureEach {
    if (name.startsWith("ksp")) {
        dependsOn(generateSecrets)
    }
}


// Room KMP configuration
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Room KSP for all targets
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosX64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}
