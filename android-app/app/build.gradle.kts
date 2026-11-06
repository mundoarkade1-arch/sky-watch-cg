plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.skywatch.cg"
    compileSdk = 34

    val configuredVersionCode = providers.environmentVariable("VERSION_CODE")
        .orNull?.toIntOrNull()
        ?: providers.gradleProperty("android.versionCode").orNull?.toIntOrNull()
        ?: 1
    val configuredVersionName = providers.environmentVariable("VERSION_NAME")
        .orNull
        ?.takeIf { it.isNotBlank() }
        ?: providers.gradleProperty("android.versionName").orNull
        ?: "1.0.0"
    val keyStorePath = providers.environmentVariable("KEYSTORE_PATH").orNull
    val keyStorePassword = providers.environmentVariable("KEYSTORE_PASSWORD").orNull
    val keyAlias = providers.environmentVariable("KEY_ALIAS").orNull
    val keyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
    val releaseSigningConfigured = listOf(
        keyStorePath,
        keyStorePassword,
        keyAlias,
        keyPassword
    ).all { !it.isNullOrBlank() }

    defaultConfig {
        applicationId = "com.skywatch.cg"
        minSdk = 24
        targetSdk = 34
        versionCode = configuredVersionCode
        versionName = configuredVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.create("release") {
                    storeFile = file(keyStorePath!!)
                    this.storePassword = keyStorePassword
                    this.keyAlias = keyAlias
                    this.keyPassword = keyPassword
                }
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // Room schema export location for migration testing
    kapt {
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Jetpack Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    // Material Icons (Using standard core icons or keeping core set if extended is not used, but let's replace custom imports with standard set or remove material-icons-extended if desired)
    // implementation("androidx.compose.material:material-icons-extended")

    // Room Database
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    testImplementation("androidx.room:room-testing:$room_version")

    // Network - OkHttp for SIROS CSV download
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // WorkManager for periodic sync
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    testImplementation("androidx.work:work-testing:2.9.0")

    // Google Fonts for Compose
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest")
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}
