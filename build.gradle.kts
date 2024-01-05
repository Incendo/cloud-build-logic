plugins {
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.1"
}

dependencies {
    api(libs.indra.common)
    api(libs.indra.publishing.sonatype)
    api(libs.errorprone.gradle)
    api(libs.revapi.gradle)
    api(libs.spotless)
}

java {
    disableAutoTargetJvm()
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
}

kotlin {
    target {
        compilations.configureEach {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
}

gradlePlugin.plugins.register("base") {
    id = "org.incendo.cloud-build-logic"
    implementationClass = "org.incendo.cloudbuildlogic.BasePlugin"
}

plugin("errorprone", "org.incendo.cloudbuildlogic.ErrorpronePlugin")
plugin("spotless", "org.incendo.cloudbuildlogic.SpotlessPlugin")
plugin("spotless.root-project", "org.incendo.cloudbuildlogic.SpotlessRootProjectPlugin")
plugin("publishing", "org.incendo.cloudbuildlogic.PublishingPlugin")
plugin("publishing.root-project", "org.incendo.cloudbuildlogic.RootProjectPublishingPlugin")

fun plugin(name: String, implClass: String) {
    val prefixedId = "org.incendo.cloud-build-logic.$name"
    gradlePlugin.plugins.register(name) {
        id = prefixedId
        implementationClass = implClass
        description = project.description
    }
}
