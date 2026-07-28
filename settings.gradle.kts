pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_INTEGRATION)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RetroRTS_Root"
include(":RetroRTS:app")