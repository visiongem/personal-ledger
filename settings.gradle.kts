@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "personal-ledger"

include(":app")
include(":core-base")
include(":core-utils")
include(":core-extensions")
include(":core-network")
include(":core-ui")
include(":core-data")
include(":feature-record")
include(":feature-account")
include(":feature-stats")
include(":feature-settings")
