package org.incendo.cloudbuildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.newInstance
import org.incendo.cloudbuildlogic.JavadocLinksExtension.LinkOverride.Companion.replaceVariables
import java.util.function.Function
import javax.inject.Inject
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

@Suppress("LeakingThis")
abstract class GenerateJavadocLinksFile : DefaultTask() {
    @get:Nested
    abstract val overrides: MapProperty<String, JavadocLinksExtension.LinkOverride>

    @get:Input
    abstract val skip: SetProperty<String>

    @get:OutputFile
    abstract val linksFile: RegularFileProperty

    @get:OutputDirectory
    abstract val unpackedJavadocs: DirectoryProperty

    @get:Input
    abstract val defaultJavadocProvider: Property<String>

    @get:Nested
    abstract val filter: Property<JavadocLinksExtension.DependencyFilter>

    @get:Inject
    abstract val objects: ObjectFactory

    @get:Inject
    abstract val fsOps: FileSystemOperations

    @get:Inject
    abstract val archiveOps: ArchiveOperations

    // We use two separate configurations to allow for excluding dependencies from javadoc downloading without excluding them
    // from javadoc linking entirely.

    @get:Nested
    val artifacts: Artifacts = objects.newInstance(Artifacts::class)

    @get:Nested
    val javadocArtifacts: Artifacts = objects.newInstance(Artifacts::class)

    fun dependenciesFrom(configuration: NamedDomainObjectProvider<Configuration>) {
        artifacts.setFrom(configuration)

        val javadocView = project.configurations.register(configuration.name + "Javadoc") {
            isCanBeResolved = true
            isCanBeConsumed = false

            attributes {
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
                attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.JAVADOC))
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            }

            // Gradle doesn't consider transitives when we simply extend the configuration using the above attributes
            defaultDependencies {
                for (artifact in configuration.get().incoming.artifacts.artifacts) {
                    val id = artifact.componentIdentifier() ?: continue
                    add(project.dependencies.create(coordinates(id)))
                }
            }
        }

        javadocArtifacts.setFrom(javadocView)
    }

    @TaskAction
    fun run() {
        val file = linksFile.asFile.get().toPath()
        file.deleteIfExists()
        file.parent.createDirectories()

        val unpackedDocs = unpackedJavadocs.get().asFile.toPath()
        unpackedDocs.toFile().deleteRecursively()
        unpackedDocs.createDirectories()

        val output = StringBuilder()
        for (resolvedArtifactResult in artifacts.artifacts.sorted()) {
            val id = resolvedArtifactResult.componentIdentifier() ?: continue
            val coordinates = coordinates(id)
            if (!filter.get().test(id) || skip.get().any { coordinates.startsWith(it) }) {
                continue
            }

            var link: String? = null

            for ((c, o) in overrides.get()) {
                if (coordinates.startsWith(c)) {
                    link = o.link(defaultJavadocProvider.get(), id)
                    break
                }
            }

            if (link == null) {
                link = defaultJavadocProvider.get().replaceVariables(id)
            }

            val javadocArtifact = javadocArtifacts.artifacts.get()
                .find { it.componentIdentifier() != null && it.componentIdentifier() == id }

            if (javadocArtifact == null) {
                output.append("-link ").append(link)
            } else {
                val unpackTo = unpackedDocs.resolve(coordinates(id).replace(":", "_"))

                fsOps.copy {
                    from(archiveOps.zipTree(javadocArtifact.file))
                    into(unpackTo)
                }

                output.append("-linkoffline ")
                    .append(link)
                    .append(' ')
                    .append(unpackTo.absolutePathString())
            }

            output.append('\n')
        }
        file.writeText(output.toString())
    }

    private fun ResolvedArtifactResult.componentIdentifier(): ModuleComponentIdentifier? =
        id.componentIdentifier as? ModuleComponentIdentifier

    private fun Provider<Set<ResolvedArtifactResult>>.sorted(): List<ResolvedArtifactResult> = get().sortedWith(
        Comparator.comparing<ResolvedArtifactResult, String> { it.id.componentIdentifier.displayName }
            .thenComparing(Function { it.file.name })
    )

    abstract class Artifacts {
        @get:Internal
        abstract val artifacts: SetProperty<ResolvedArtifactResult>

        /**
         * Only here to ensure inputs get wired properly. [artifacts] is what we care about.
         */
        @get:InputFiles
        @get:Optional
        abstract val files: ConfigurableFileCollection

        fun setFrom(configuration: NamedDomainObjectProvider<Configuration>) {
            artifacts.set(configuration.map { it.incoming.artifacts })
            files.setFrom(configuration)
        }
    }
}
