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
    abstract val overrides: MapProperty<String, LinkOverride>
    abstract val excludes: ListProperty<String>
    abstract val filter: Property<DependencyFilter>

    init {
        init()
    }

    private fun init() {
        filter.convention(DependencyFilter.NoSnapshots())
        overrides.putAll(defaultOverrides())
    }

    fun defaultOverrides(): Map<String, LinkOverride> {
        return mapOf(
            "net.kyori:" to LinkOverride.KyoriRule(),
            "io.papermc.paper:paper-api:" to LinkOverride.PaperApiRule(),
        )
    }

    fun override(dep: ModuleDependency, link: String) {
        overrides.put(key(dep), LinkOverride.Simple(link))
    }

    fun override(dep: Provider<out ModuleDependency>, link: String) {
        override(dep.get(), link)
    }

    fun override(dep: ModuleDependency, link: LinkOverride) {
        overrides.put(key(dep), link)
    }

    fun override(dep: Provider<out ModuleDependency>, link: LinkOverride) {
        override(dep.get(), link)
    }

    fun exclude(dep: ModuleDependency) {
        excludes.add(key(dep))
    }

    fun exclude(dep: Provider<out ModuleDependency>) {
        exclude(dep.get())
    }

    private fun key(dep: ModuleDependency) = dep.group + ':' + dep.name + (dep.version?.let { ":$it" } ?: "")

    fun interface LinkOverride {
        fun link(defaultProvider: String, id: ModuleComponentIdentifier): String

        companion object {
            fun String.replaceVariables(id: ModuleComponentIdentifier): String {
                return replace("{group}", id.group)
                    .replace("{name}", id.module)
                    .replace("{version}", id.version)
            }
        }

        class PassThrough : LinkOverride {
            override fun link(defaultProvider: String, id: ModuleComponentIdentifier): String {
                return defaultProvider.replaceVariables(id)
            }
        }

        data class Simple(val replacement: String) : LinkOverride {
            override fun link(defaultProvider: String, id: ModuleComponentIdentifier): String {
                return replacement.replaceVariables(id)
            }
        }

        class PaperApiRule : LinkOverride {
            override fun link(defaultProvider: String, it: ModuleComponentIdentifier): String {
                val ver = it.version.split('.').take(2).joinToString(".")
                return "https://jd.papermc.io/paper/$ver/"
            }
        }

        class KyoriRule : LinkOverride {
            override fun link(defaultProvider: String, it: ModuleComponentIdentifier): String {
                val name = it.module.replace("adventure-", "")
                if (name.contains("examination")) {
                    return PassThrough().link(defaultProvider, it)
                }
                return "https://jd.advntr.dev/$name/${it.version}"
            }
        }
    }

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
