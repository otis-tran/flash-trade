plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.otistran.flash_trade"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.otistran.flash_trade"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Vector drawable support
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            // Enable for faster debug builds
            isMinifyEnabled = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
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
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "DebugProbesKt.bin"
            excludes += "kotlin/**"
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

    // Compose libs
    implementation(libs.androidx.compose.ui)                    // UI primitives
    implementation(libs.androidx.compose.ui.tooling.preview)    // @Preview
    implementation(libs.androidx.compose.foundation)            // LazyColumn, LazyRow
    implementation(libs.androidx.compose.material3)             // Material3 components

    // Debug only - won't be in release APK
    debugImplementation(libs.androidx.compose.ui.tooling)
}