pluginManagement {
    repositories {
        google()
        maven { url = uri("https://maven-central.storage.googleapis.com/maven2") }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        maven { url = uri("https://maven-central.storage.googleapis.com/maven2") }
        mavenCentral()
    }
}

rootProject.name = "YTDow"
include(":app", ":domain", ":core", ":data")
