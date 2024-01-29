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
            LinkOverride.KyoriRule.KEY to LinkOverride.KyoriRule(),
            LinkOverride.PaperApiRule.KEY to LinkOverride.PaperApiRule(),
            LinkOverride.Log4jRule.API_KEY to LinkOverride.Log4jRule(),
            LinkOverride.Log4jRule.CORE_KEY to LinkOverride.Log4jRule()
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

    private fun key(dep: ModuleDependency) = dep.group + ':' + dep.name + ':' + (dep.version ?: "")

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

        data class Simple(
            @get:Input
            val replacement: String
        ) : LinkOverride {
            override fun link(defaultProvider: String, id: ModuleComponentIdentifier): String {
                return replacement.replaceVariables(id)
            }
        }

        class PaperApiRule : LinkOverride {
            companion object {
                const val KEY = "io.papermc.paper:paper-api"
            }

            override fun link(defaultProvider: String, id: ModuleComponentIdentifier): String {
                val ver = id.version.split('.').take(2).joinToString(".")
                return "https://jd.papermc.io/paper/$ver/"
            }
        }

        class KyoriRule : LinkOverride {
            companion object {
                const val KEY = "net.kyori:"
            }

            override fun link(defaultProvider: String, id: ModuleComponentIdentifier): String {
                val name = id.module.replace("adventure-", "")
                if (name.contains("examination")) {
                    return PassThrough().link(defaultProvider, id)
                }
                return "https://jd.advntr.dev/$name/${id.version}"
            }
        }

        class Log4jRule : LinkOverride {
            companion object {
                const val API_KEY = "org.apache.logging.log4j:log4j-api:"
                const val CORE_KEY = "org.apache.logging.log4j:log4j-core:"
            }

            override fun link(defaultProvider: String, id: ModuleComponentIdentifier): String {
                return "https://logging.apache.org/log4j/2.x/javadoc/${id.module}/"
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
