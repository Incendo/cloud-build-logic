plugins {
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.gradle.plugin.publish)
}

dependencies {
    api(libs.indra.common)
    api(libs.indra.publishing.sonatype)
    api(libs.errorprone.gradle)
    api(libs.spotless)
}

java.disableAutoTargetJvm()

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
    target {
        compilations.configureEach {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
}

gradlePlugin {
    website = "https://github.com/Incendo"
    vcsUrl = "https://github.com/Incendo/cloud-build-logic"
}

gradlePlugin.plugins.register("base") {
    id = "org.incendo.cloud-build-logic"
    displayName = "Cloud Build Logic"
    implementationClass = "org.incendo.cloudbuildlogic.BasePlugin"
    description = project.description
    tags.addAll("Cloud", "Build-Logic")
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
        displayName = "Cloud Build Logic ($name)"
        implementationClass = implClass
        description = project.description
        tags.addAll("Cloud", "Build-Logic", name)
    }
}
