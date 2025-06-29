rootProject.name = "cloud-build-logic"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            mavenContent { snapshotsOnly() }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

buildscript {
    dependencies {
        classpath("org.incendo:cloud-build-logic:0.0.17") {
            isTransitive = false
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        gradlePluginPortal()
    }
}
