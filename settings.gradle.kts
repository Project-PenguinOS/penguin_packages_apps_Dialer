pluginManagement {
    repositories {
        google()
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

rootProject.name = "Dialer"

include(":protos", ":rootcomponentprocessor", ":systemstubs")
project(":protos").projectDir = file("gradle-modules/protos")
project(":rootcomponentprocessor").projectDir = file("gradle-modules/rootcomponentprocessor")
project(":systemstubs").projectDir = file("gradle-modules/systemstubs")
