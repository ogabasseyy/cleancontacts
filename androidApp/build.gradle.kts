import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // 2026 AGP 9.0: kotlin.android removed - AGP has built-in Kotlin support
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    // 2026 AGP 9.0: Hilt removed - incompatible with AGP 9.0, migrated to Koin
}

// Load local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

val revenueCatApiKey = localProperties.getProperty("REVENUECAT_API_KEY") ?: ""

android {
    namespace = "com.ogabassey.contactscleaner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ogabassey.contactscleaner"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // 2026 Best Practice: Secrets via BuildConfig
        // TIP: Use an environment variable or local.properties for the real key!
        buildConfigField("String", "REVENUECAT_API_KEY", "\"$revenueCatApiKey\"")
    }

    // 2026 Best Practice: Only create signing config when all properties exist
    val releaseStoreFile = localProperties.getProperty("RELEASE_STORE_FILE")
    if (releaseStoreFile != null) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Only assign signing config if it was created
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            
            // Optimization for App Store
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/notice.txt"
            excludes += "/META-INF/ASL2.0"
            excludes += "/META-INF/INDEX.LIST"
        }
    }
}

dependencies {
    // KMP Modules (2026 Best Practice)
    implementation(project(":shared"))
    implementation(project(":composeApp"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    // Google Play Billing
    implementation(libs.billing.ktx)
    implementation(libs.revenuecat.purchases)

    // Google People API & Auth
    implementation(libs.play.services.auth)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.people)

    // 2026 AGP 9.0: Koin Dependency Injection (migrated from Hilt)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    // Multiplatform Settings
    implementation(libs.multiplatform.settings)
    implementation(libs.multiplatform.settings.coroutines)

    // Generative AI (ML Kit 2026 Best Practice)
    implementation(libs.mlkit.genai)

    // Room Persistence
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    // Paging 3
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    
    // Format Detection
    implementation(libs.libphonenumber)

    // DataStore Persistence
    implementation(libs.androidx.datastore.preferences)

    // BOM Platforms for Transitive Security (2026 Best Practice)
    implementation(platform(libs.netty.bom))
    // Note: protobuf-bom removed - it overrides force() directives
    // Protobuf security pins handled via resolutionStrategy.force() below

    // Security Constraint Pins (2026 Best Practice: Resolve transitive CVEs)
    constraints {
        implementation(libs.jose4j) {
            because("CVE-2023-31582 & CVE-2023-51775: JWE denial of service")
        }
        implementation(libs.jdom2) {
            because("CVE-2021-33813: XXE vulnerability")
        }
        implementation(libs.commons.compress) {
            because("CVE-2024-25710 & CVE-2024-26308: Denial of Service")
        }
    }
}

