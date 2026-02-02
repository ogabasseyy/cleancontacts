plugins {
    alias(libs.plugins.kotlin.multiplatform)
    // 2026 AGP 9.0: New KMP-native Android library plugin
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // 2026 AGP 9.0: androidLibrary block replaces separate android {} block
    androidLibrary {
        namespace = "com.ogabassey.contactscleaner.composeapp"
        compileSdk = 36
        minSdk = 26
    }

    // iOS targets with 2026 Best Practice compiler flags
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "composeApp"
            isStatic = true
            binaryOption("bundleId", "com.ogabassey.contactscleaner.composeapp")
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

    sourceSets {
        commonMain.dependencies {
            // Shared module
            implementation(project(":shared"))

            // Compose Multiplatform
            @Suppress("DEPRECATION")
            implementation(compose.runtime)
            @Suppress("DEPRECATION")
            implementation(compose.foundation)
            @Suppress("DEPRECATION")
            implementation(compose.material3)
            @Suppress("DEPRECATION")
            implementation(compose.materialIconsExtended)
            @Suppress("DEPRECATION")
            implementation(compose.ui)
            @Suppress("DEPRECATION")
            implementation(compose.components.resources)
            @Suppress("DEPRECATION")
            implementation(compose.components.uiToolingPreview)

            // Navigation - JetBrains Compose Multiplatform version
            implementation(libs.navigation.compose.multiplatform)

            // Lifecycle ViewModel Compose
            implementation(libs.lifecycle.viewmodel.compose)

            // Koin Compose
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Serialization (for navigation)
            implementation(libs.kotlinx.serialization.json)

            // DateTime for KMP
            implementation(libs.kotlinx.datetime)

            // Multiplatform Settings (for WhatsApp device ID)
            implementation(libs.multiplatform.settings)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            // Android-specific Compose
            @Suppress("DEPRECATION")
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            // Accompanist Permissions
            implementation(libs.accompanist.permissions)

            // Android Koin
            implementation(libs.koin.android)

            // Android Paging
            implementation(libs.androidx.paging.compose)
        }

        iosMain.dependencies {
            // iOS-specific dependencies
        }
    }
}

// 2026 AGP 9.0: Configure JVM target for all Kotlin compilations

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

