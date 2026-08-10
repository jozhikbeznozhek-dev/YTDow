plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.hermes.downloader.data"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core"))
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    kapt("androidx.room:room-compiler:2.8.4")
    implementation("com.google.dagger:hilt-android:2.58")
    kapt("com.google.dagger:hilt-android-compiler:2.58")
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.22.1"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    constraints {
        implementation("commons-io:commons-io:2.22.0")
        implementation("org.apache.commons:commons-compress:1.28.0")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}
