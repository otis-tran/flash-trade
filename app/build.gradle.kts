plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ethers.abigen)
}

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

            // Speed up debug builds
            applicationIdSuffix = ".debug"

            // Faster debug builds
            versionNameSuffix = "-debug"
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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xskip-metadata-version-check"
        )
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
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose libs
    implementation(libs.androidx.compose.ui)                    // UI primitives
    implementation(libs.androidx.compose.ui.tooling.preview)    // @Preview
    implementation(libs.androidx.compose.foundation)            // LazyColumn, LazyRow
    implementation(libs.androidx.compose.material3)             // Material3 components

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

    // Biometric
    implementation(libs.androidx.biometric)

    // Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // QR Code
    implementation(libs.zxing.core)

    // Web3
    implementation(libs.privy.core)
    implementation(platform(libs.ethers.bom))

    // Individual (recommended with BOM)
    implementation(libs.ethers.abi)
    implementation(libs.ethers.core)
    implementation(libs.ethers.providers)
    implementation(libs.ethers.signers)

    // Debug only - won't be in release APK
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Testing
//    testImplementation(libs.junit)
//    testImplementation(libs.kotlinx.coroutines.test)
}