import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Load from local.properties
val localProperties = Properties()
localProperties.load(project.rootProject.file("local.properties").inputStream())

android {
    namespace = "com.otistran.flash_trade"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.otistran.flash_trade"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Vector drawable support
        vectorDrawables.useSupportLibrary = true

        // Build config fields from local.properties
        buildConfigField(
            "String",
            "PRIVY_APP_ID",
            "\"${localProperties.getProperty("PRIVY_APP_ID")}\""
        )
        buildConfigField(
            "String",
            "PRIVY_APP_CLIENT_ID",
            "\"${localProperties.getProperty("PRIVY_APP_CLIENT_ID")}\""
        )

        // Etherscan API Key
        buildConfigField(
            "String",
            "ETHERSCAN_API_KEY",
            "\"${localProperties.getProperty("ETHERSCAN_API_KEY") ?: ""}\""
        )

        // Alchemy API Key (for Prices API)
        buildConfigField(
            "String",
            "ALCHEMY_API_KEY",
            "\"${localProperties.getProperty("ALCHEMY_API_KEY") ?: ""}\""
        )

        // KyberSwap Client ID (for rate limit elevation)
        buildConfigField(
            "String",
            "KYBER_CLIENT_ID",
            "\"${localProperties.getProperty("KYBER_CLIENT_ID") ?: ""}\""
        )

        // Privy auth config
        buildConfigField("String", "PRIVY_RELYING_PARTY", "\"https://flash-trade-assetlinks.netlify.app\"")
        buildConfigField("String", "PRIVY_OAUTH_SCHEME", "\"com.otistran.flashtrade.privy\"")
    }

    signingConfigs {
        // Release keystore configuration from local.properties
        create("release") {
            val keystoreFile = localProperties.getProperty("KEYSTORE_FILE")
            if (keystoreFile != null && file(keystoreFile).exists()) {
                // Use release keystore from local.properties
                storeFile = file(keystoreFile)
                storePassword = localProperties.getProperty("KEYSTORE_PASSWORD")
                keyAlias = localProperties.getProperty("KEY_ALIAS")
                keyPassword = localProperties.getProperty("KEY_PASSWORD")
            } else {
                // Fallback to debug keystore for testing
                storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            // Enable for faster debug builds
            isMinifyEnabled = false
            isShrinkResources = false // Don't shrink in debug for faster builds
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Faster builds
            isDebuggable = true

//            // Speed up debug builds
//            applicationIdSuffix = ".debug"
//
//            // Faster debug builds
//            versionNameSuffix = "-debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Optimize for speed
            isDebuggable = false

            // Sign with the release signing config
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll("-Xskip-metadata-version-check")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true  // For BuildConfig access

        // Disable unused features for faster builds
        aidl = false
        renderScript = false
        shaders = false
        resValues = false
        viewBinding = false
        dataBinding = false
    }

    // Packaging options - exclude unnecessary files
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
                "/META-INF/DEPENDENCIES",
                "DebugProbesKt.bin",
                "kotlin/**",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            )
        }
    }

    // Optimize compose compiler
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    // Rename APK output
    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = if (buildType.name == "release") {
                "flash-trade.apk"
            } else {
                "flash-trade-debug.apk"
            }
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose libs
    implementation(libs.androidx.compose.ui)                    // UI primitives
    implementation(libs.androidx.compose.ui.tooling.preview)    // @Preview
    implementation(libs.androidx.compose.foundation)            // LazyColumn, LazyRow
    implementation(libs.androidx.compose.material3)             // Material3 components
    implementation(libs.androidx.compose.material.icons.extended) // Material Icons
    implementation(libs.androidx.navigation.compose)            // Navigation

    // Dependency Injection
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)  // Code generation - zero runtime cost

    // Work Manager
    implementation(libs.androidx.work.runtime.ktx)

    // Data Storage
    implementation(libs.androidx.datastore.preferences)

    // Biometric - commented out, not implemented
    // implementation(libs.androidx.biometric)

    // Splash Screen (backward compatible API)
    implementation(libs.androidx.core.splashscreen)

    // Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    // Paging 3
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // QR code generation
    implementation(libs.zxing.core)

    // Web3
    implementation(libs.privy.core)
    implementation(libs.web3j.core)

    // Credentials API for passkey - commented out, using Privy embedded wallet instead
    // implementation(libs.androidx.credentials)
    // implementation(libs.androidx.credentials.play.services.auth)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Jetpack Startup
    //implementation(libs.androidx.startup.runtime)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    //
    implementation(libs.okhttp.logging)

    // Logging
    implementation(libs.timber)

    // Firebase - Crash reporting
//    implementation(platform(libs.firebase.bom))
//    implementation(libs.firebase.crashlytics)
//    implementation(libs.firebase.analytics)

    // Camera for scanning (disabled - app only generates QR codes)
    // implementation(libs.androidx.camera.camera2)
    // implementation(libs.androidx.camera.lifecycle)
    // implementation(libs.androidx.camera.view)

    // CameraX ML Kit barcode scanning (disabled - not used)
    // implementation(libs.barcode.scanning)

    // Debug only - won't be in release APK
    debugImplementation(libs.androidx.compose.ui.tooling)
}