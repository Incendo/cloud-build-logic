import org.incendo.cloudbuildlogic.jmp

plugins {
    `kotlin-dsl`
    `maven-publish`
    signing
    alias(libs.plugins.nexus.publish)
}

dependencies {
    api(libs.indra.common)
    api(libs.indra.publishing.sonatype)
    api(libs.errorprone.gradle)
    api(libs.spotless)
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
}

nexusPublishing {
    repositories {
        sonatype()
    }
}

val ghUrl = "https://github.com/Incendo/cloud-build-logic"

publishing.publications.withType(MavenPublication::class).configureEach {
    pom {
        name.convention("Cloud Build Logic")
        description.convention(project.description)
        url.convention(ghUrl)
        licenses {
            license {
                name = "MIT License"
                url = "https://www.opensource.org/licenses/mit-license.php"
            }
        }
        developers {
            jmp()
        }
        scm {
            connection = "scm:git:git://github.com/Incendo/cloud-build-logic.git"
            developerConnection = "scm:git:ssh://github.com:Incendo/cloud-build-logic.git"
            url = "https://github.com/Incendo/cloud-build-logic/tree/master"
        }
    }
}

java {
    disableAutoTargetJvm()
    withJavadocJar()
    withSourcesJar()
}

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
    vcsUrl = ghUrl
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
