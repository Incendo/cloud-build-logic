package org.incendo.cloudbuildlogic

import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.util.function.Predicate

abstract class JavadocLinksExtension {
    abstract val overrides: MapProperty<String, String>
    abstract val skip: ListProperty<String>
    abstract val filter: Property<DependencyFilter>

    init {
        init()
    }

    private fun init() {
        filter.convention(DependencyFilter.NoSnapshots())
    }

    fun override(dep: ModuleDependency, link: String) {
        overrides.put(key(dep), link)
    }

    fun override(dep: Provider<out ModuleDependency>, link: String) {
        override(dep.get(), link)
    }

    fun exclude(dep: ModuleDependency) {
        skip.add(key(dep))
    }

    fun exclude(dep: Provider<out ModuleDependency>) {
        exclude(dep.get())
    }

    private fun key(dep: ModuleDependency) = dep.group + ':' + dep.name + (dep.version?.let { ":$it" } ?: "")

    fun interface DependencyFilter : Predicate<ModuleComponentIdentifier> {
        class NoSnapshots : DependencyFilter {
            override fun test(t: ModuleComponentIdentifier): Boolean {
                return !t.version.endsWith("-SNAPSHOT")
            }
        }
    }
}
