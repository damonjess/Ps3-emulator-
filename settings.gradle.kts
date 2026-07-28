pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_LOCAL)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RetroRTS_Root"
include(":RetroRTS:app")