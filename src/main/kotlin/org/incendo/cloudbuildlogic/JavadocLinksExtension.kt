package org.incendo.cloudbuildlogic

import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider

abstract class JavadocLinksExtension {
    abstract val overrides: MapProperty<String, String>

    fun override(dep: ModuleDependency, link: String) {
        val key = dep.group + ':' + dep.name + (dep.version?.let { ":$it" } ?: "")
        overrides.put(key, link)
    }

    fun override(dep: Provider<out ModuleDependency>, link: String) {
        override(dep.get(), link)
    }
}
