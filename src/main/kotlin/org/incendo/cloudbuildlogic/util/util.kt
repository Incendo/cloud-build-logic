package org.incendo.cloudbuildlogic.util

import org.gradle.api.Action
import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.internal.artifacts.repositories.resolver.MavenUniqueSnapshotComponentIdentifier
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
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

fun SourceSet.formatName(suffix: String) = formatName("", suffix)

fun SourceSet.formatName(prefix: String, suffix: String): String {
    if (name == "main") {
        val s = if (prefix.isBlank()) suffix else suffix.replaceFirstChar(Char::uppercase)
        return prefix + s
    }
    val name = if (prefix.isBlank()) name else name.replaceFirstChar(Char::uppercase)
    return prefix + name + suffix.replaceFirstChar(Char::uppercase)
}
