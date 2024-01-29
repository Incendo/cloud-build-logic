package org.incendo.cloudbuildlogic

import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.internal.artifacts.repositories.resolver.MavenUniqueSnapshotComponentIdentifier
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import java.util.function.Predicate

abstract class JavadocLinksExtension {
    abstract val overrides: MapProperty<String, String>
    abstract val excludes: ListProperty<String>
    abstract val filter: Property<DependencyFilter>

    init {
        init()
    }

    private fun init() {
        filter.convention(DependencyFilter.NoSnapshots())
        overrides.putAll(defaultOverrides())
    }

    fun defaultOverrides(): Map<String, String> {
        return mapOf()
    }

    fun override(dep: ModuleDependency, link: String) {
        overrides.put(key(dep), link)
    }

    fun override(dep: Provider<out ModuleDependency>, link: String) {
        override(dep.get(), link)
    }

    fun exclude(dep: ModuleDependency) {
        excludes.add(key(dep))
    }

    fun exclude(dep: Provider<out ModuleDependency>) {
        exclude(dep.get())
    }

    private fun key(dep: ModuleDependency) = dep.group + ':' + dep.name + (dep.version?.let { ":$it" } ?: "")

    fun interface DependencyFilter : Predicate<ModuleComponentIdentifier> {
        data class NoSnapshots(
            @get:Input
            val exceptFor: Set<String> = emptySet()
        ) : DependencyFilter {
            override fun test(t: ModuleComponentIdentifier): Boolean {
                val coords = coordinates(t)
                if (exceptFor.any { coords.startsWith(it) }) {
                    return true
                }
                return t !is MavenUniqueSnapshotComponentIdentifier
            }
        }

        class PassThrough : DependencyFilter {
            override fun test(ignore: ModuleComponentIdentifier): Boolean {
                return true
            }
        }
    }
}
