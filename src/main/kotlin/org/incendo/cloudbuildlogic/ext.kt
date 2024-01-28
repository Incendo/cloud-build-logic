package org.incendo.cloudbuildlogic

import org.gradle.api.Action
import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.maven.MavenPomDeveloperSpec

// set by GitHub Actions
val ProviderFactory.ciBuild: Provider<Boolean>
    get() = environmentVariable("CI")
        .map { it.toBoolean() }
        .orElse(false)

inline fun <reified S> PolymorphicDomainObjectContainer<in S>.maybeConfigure(name: String, op: Action<S>) {
    if (name in names) {
        named(name, S::class.java, op)
    }
}

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
