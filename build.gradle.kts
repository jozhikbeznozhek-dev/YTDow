// Top-level build file
group = "com.jozhikbeznozhek.ytdow"
version = "2.3.0"

plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("com.google.dagger.hilt.android") version "2.58" apply false
    id("org.cyclonedx.bom") version "3.4.0"
}

allprojects {
    group = rootProject.group
    version = rootProject.version

    dependencyLocking {
        lockAllConfigurations()
    }

    tasks.withType<org.cyclonedx.gradle.BaseCyclonedxTask>().configureEach {
        licenseChoice.set(org.cyclonedx.model.LicenseChoice().apply {
            addLicense(org.cyclonedx.model.License().apply { id = "GPL-3.0-only" })
        })
        externalReferences.set(listOf(org.cyclonedx.model.ExternalReference().apply {
            type = org.cyclonedx.model.ExternalReference.Type.VCS
            url = "https://github.com/jozhikbeznozhek-dev/YTDow"
        }))
        projectType.set(
            if (project == rootProject || project.path == ":app") {
                org.cyclonedx.model.Component.Type.APPLICATION
            } else {
                org.cyclonedx.model.Component.Type.LIBRARY
            }
        )
    }

    tasks.withType<org.cyclonedx.gradle.CyclonedxDirectTask>().configureEach {
        // The shipped application only needs production runtime components in its SBOM.
        // Avoid resolving test, lint, KAPT, and debug-only configurations.
        includeConfigs.set(listOf("releaseRuntimeClasspath", "runtimeClasspath"))
    }
}
