plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
}

val releaseKeystorePath = providers.environmentVariable("YTDOW_KEYSTORE").orNull
val releaseStorePassword = providers.environmentVariable("YTDOW_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("YTDOW_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("YTDOW_KEY_PASSWORD").orNull
val productionSigningConfigured = listOf(
    releaseKeystorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.hermes.downloader"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jozhikbeznozhek.ytdow"
        minSdk = 24
        targetSdk = 36
        versionCode = 9
        versionName = "2.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk { abiFilters += listOf("arm64-v8a") } // arm64 only — сокращает APK на 40%. Для x86 эмуляторов используйте arm-образ.
    }

    signingConfigs {
        if (productionSigningConfigured) {
            create("production") {
                storeFile = file(requireNotNull(releaseKeystorePath))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("production")
        }
    }

    packaging { jniLibs { useLegacyPackaging = true } }
    testOptions { unitTests.isReturnDefaultValues = true }
    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

val checkProductionSigning = tasks.register("checkProductionSigning") {
    group = "verification"
    doLast {
        check(productionSigningConfigured) {
            "Production signing is not configured. Set YTDOW_KEYSTORE, YTDOW_STORE_PASSWORD, " +
                "YTDOW_KEY_ALIAS and YTDOW_KEY_PASSWORD."
        }
        check(file(requireNotNull(releaseKeystorePath)).isFile) { "Production keystore does not exist" }
    }
}

tasks.register("productionRelease") {
    group = "build"
    description = "Builds a release only when production signing is configured."
    dependsOn(checkProductionSigning, "assembleRelease")
}

tasks.matching { it.name == "assembleRelease" }.configureEach {
    mustRunAfter(checkProductionSigning)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Hilt
    implementation("com.google.dagger:hilt-android:2.60.1")
    kapt("com.google.dagger:hilt-android-compiler:2.60.1")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.activity:activity-ktx:1.13.0")

    // Room
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    kapt("androidx.room:room-compiler:2.8.4")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    // Core
    implementation(project(":domain"))
    implementation(project(":core"))
    implementation(project(":data"))
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.webkit:webkit:1.16.0")
    implementation("com.google.android.material:material:1.14.0")

    // YouTube DL
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.22.1"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    constraints {
        implementation("commons-io:commons-io:2.22.0")
        implementation("org.apache.commons:commons-compress:1.28.0")
    }

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("io.mockk:mockk:1.14.9")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}
