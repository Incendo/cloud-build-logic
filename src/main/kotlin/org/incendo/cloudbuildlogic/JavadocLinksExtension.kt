package org.incendo.cloudbuildlogic

import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.internal.artifacts.repositories.resolver.MavenUniqueSnapshotComponentIdentifier
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.incendo.cloudbuildlogic.JavadocLinksExtension.LinkOverride.Companion.replaceVariables
import java.util.function.Predicate
import javax.inject.Inject

abstract class JavadocLinksExtension @Inject constructor(providers: ProviderFactory) {
    /**
     * Resolvers for custom Javadoc links.
     */
    abstract val overrides: ListProperty<OverrideRule>

    /**
     * Filters whether modules should be linked against.
     */
    abstract val filter: Property<DependencyFilter>

    /**
     * Excludes in addition to [filter]. Behaves the same as [DependencyFilter.StartsWithAnyOf].
     */
    abstract val excludes: ListProperty<String>

    /**
     * The Javadoc provider to use when no [overrides] matched. Will be processed with [LinkOverride.replaceVariables].
     */
    abstract val defaultJavadocProvider: Property<String>

    /**
     * Whether to check for availability of resolved Javadoc links.
     */
    abstract val checkJavadocAvailability: Property<Boolean>

    init {
        init(providers)
    }

    private fun init(providers: ProviderFactory) {
        filter.convention(DependencyFilter.NoSnapshots())
        overrides.addAll(defaultOverrides())
        defaultJavadocProvider.convention("https://javadoc.io/doc/{group}/{name}/{version}")
        checkJavadocAvailability.convention(
            providers.gradleProperty("cloud-build-logic.checkJavadocAvailability")
                .map { it.toBoolean() }
                .orElse(true)
        )
    }

    fun defaultOverrides(): List<OverrideRule> {
        return listOf(
            LinkOverride.KyoriRule.RULE,
            LinkOverride.PaperApiRule.RULE,
            LinkOverride.Log4jRule.RULE,
        )
    }

    fun override(dep: ModuleDependency, link: String) {
        override(dep, LinkOverride.Simple(link))
    }

    fun override(dep: Provider<out ModuleDependency>, link: String) {
        override(dep.get(), link)
    }

    fun override(dep: ModuleDependency, link: LinkOverride) {
        override(DependencyFilter.StartsWithAnyOf(key(dep)), link)
    }

    fun override(dep: Provider<out ModuleDependency>, link: LinkOverride) {
        override(dep.get(), link)
    }

    fun override(filter: DependencyFilter, link: LinkOverride) {
        overrides.add(OverrideRule(filter, link))
    }

    fun exclude(dep: ModuleDependency) {
        excludes.add(key(dep))
    }

    fun exclude(dep: Provider<out ModuleDependency>) {
        exclude(dep.get())
    }

    private fun key(dep: ModuleDependency) = dep.group + ':' + dep.name + ':' + (dep.version ?: "")

    data class OverrideRule(
        @get:Nested
        val filter: DependencyFilter,
        @get:Nested
        val override: LinkOverride
    )

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
                val FILTER = DependencyFilter.StartsWithAnyOf(
                    "io.papermc.paper:paper-api:",
                    "com.destroystokyo.paper:paper-api:",
                )
                val RULE = OverrideRule(FILTER, PaperApiRule())
            }

            override fun link(defaultProvider: String, id: ModuleComponentIdentifier): String {
                val ver = id.version.split('.').take(2).joinToString(".")
                return "https://jd.papermc.io/paper/$ver/"
            }
        }

        class KyoriRule : LinkOverride {
            companion object {
                val FILTER = DependencyFilter.StartsWithAnyOf("net.kyori:")
                val RULE = OverrideRule(FILTER, KyoriRule())
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
                val FILTER = DependencyFilter.StartsWithAnyOf(
                    "org.apache.logging.log4j:log4j-api:",
                    "org.apache.logging.log4j:log4j-core:",
                )
                val RULE = OverrideRule(FILTER, Log4jRule())
            }

            override fun link(defaultProvider: String, id: ModuleComponentIdentifier): String {
                return "https://logging.apache.org/log4j/2.x/javadoc/${id.module}/"
            }
        }
    }

    fun interface DependencyFilter : Predicate<ModuleComponentIdentifier> {
        data class NoSnapshots(
            @get:Input
            val exceptFor: Set<String> = DEFAULT_EXCEPTIONS
        ) : DependencyFilter {
            companion object {
                val DEFAULT_EXCEPTIONS = buildSet {
                    addAll(LinkOverride.PaperApiRule.FILTER.strings)
                }
            }

            override fun test(t: ModuleComponentIdentifier): Boolean {
                val coords = coordinates(t)
                if (exceptFor.any { coords.startsWith(it) }) {
                    return true
                }
                return t !is MavenUniqueSnapshotComponentIdentifier && !t.version.endsWith("-SNAPSHOT")
            }
        }

        class PassThrough : DependencyFilter {
            override fun test(ignore: ModuleComponentIdentifier): Boolean {
                return true
            }
        }

        class StartsWithAnyOf(
            @get:Input
            val strings: Set<String>
        ) : DependencyFilter {
            constructor(vararg strings: String) : this(strings.toSet())

            override fun test(id: ModuleComponentIdentifier): Boolean {
                val coords = coordinates(id)
                return strings.any { coords.startsWith(it) }
            }
        }
    }

    /* start dependency filter helpers */
    fun noSnapshots(exceptFor: Set<String> = DependencyFilter.NoSnapshots.DEFAULT_EXCEPTIONS): DependencyFilter.NoSnapshots =
        DependencyFilter.NoSnapshots(exceptFor)

    fun passThrough(): DependencyFilter.PassThrough = DependencyFilter.PassThrough()

    fun startsWithAnyOf(vararg strings: String): DependencyFilter.StartsWithAnyOf =
        DependencyFilter.StartsWithAnyOf(strings.toSet())
    /* end dependency filter helpers */
}
