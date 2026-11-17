pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AetherFlow"

include(":app")
include(":core:common")
include(":core:parser")
include(":core:routing")
include(":core:tun")
include(":core:engine-xray")
include(":core:engine-singbox")
include(":core:engine-wireguard")
include(":core:vpn")
include(":feature:dashboard")
include(":feature:profiles")
include(":feature:subscriptions")
include(":feature:routing")
include(":feature:diagnostics")
include(":feature:settings")
