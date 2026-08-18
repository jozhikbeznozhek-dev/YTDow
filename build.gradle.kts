import org.cyclonedx.gradle.CyclonedxDirectTask

// Top-level build file
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.4.10" apply false
    id("com.google.dagger.hilt.android") version "2.58" apply false
    id("org.cyclonedx.bom") version "3.4.0"
}

allprojects {
    group = "com.jozhikbeznozhek"
    version = "2.3.0"

    dependencyLocking {
        lockAllConfigurations()
    }

    tasks.withType<CyclonedxDirectTask>().configureEach {
        // The release SBOM must describe shipped code, not Gradle, lint, kapt or tests.
        includeConfigs.set(listOf("releaseRuntimeClasspath", "runtimeClasspath"))
        includeLicenseText.set(true)
        includeBuildEnvironment.set(false)
    }
}
