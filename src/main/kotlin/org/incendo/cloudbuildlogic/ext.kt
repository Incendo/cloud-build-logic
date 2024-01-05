package org.incendo.cloudbuildlogic

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.maven.MavenPomDeveloperSpec

// set by GitHub Actions
val ProviderFactory.ciBuild: Provider<Boolean>
    get() = environmentVariable("CI")
        .map { it.toBoolean() }
        .orElse(false)

fun MavenPomDeveloperSpec.city() {
    developer {
        id.set("Citymonstret")
        name.set("Alexander Söderberg")
        url.set("https://github.com/Citymonstret")
        email.set("alexander.soderberg@incendo.org")
    }
}

fun MavenPomDeveloperSpec.jmp() {
    developer {
        id.set("jmp")
        name.set("Jason Penilla")
        url.set("https://github.com/jpenilla")
    }
}
