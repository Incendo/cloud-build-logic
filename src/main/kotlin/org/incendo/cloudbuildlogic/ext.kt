package org.incendo.cloudbuildlogic

import org.gradle.api.Action
import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.internal.artifacts.repositories.resolver.MavenUniqueSnapshotComponentIdentifier
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.maven.MavenPomDeveloperSpec
import org.gradle.api.tasks.SourceSet

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

fun coordinates(componentId: ModuleComponentIdentifier): String {
    val builder = StringBuilder()
        .append(componentId.group)
        .append(':')
        .append(componentId.module)
        .append(':')

    val ver = if (componentId is MavenUniqueSnapshotComponentIdentifier) {
        componentId.snapshotVersion
    } else {
        componentId.version
    }

    return builder
        .append(ver)
        .toString()
}

fun ResolvedArtifactResult.moduleComponentId(): ModuleComponentIdentifier? =
    id.componentIdentifier as? ModuleComponentIdentifier

fun SourceSet.formatName(taskName: String): String {
    if (name == "main") {
        return taskName
    }
    return name + taskName.replaceFirstChar(Char::uppercase)
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
