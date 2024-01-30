package org.incendo.cloudbuildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.newInstance
import org.incendo.cloudbuildlogic.JavadocLinksExtension.LinkOverride.Companion.replaceVariables
import javax.inject.Inject
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

@CacheableTask
@Suppress("LeakingThis")
abstract class GenerateJavadocLinksFile : DefaultTask() {
    @get:Nested
    abstract val overrides: ListProperty<JavadocLinksExtension.OverrideRule>

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

    @get:ServiceReference
    abstract val javadocAvailabilityService: Property<JavadocAvailabilityService>

    @get:Input
    abstract val checkJavadocAvailability: Property<Boolean>

    // We use two separate configurations to allow for excluding dependencies from javadoc downloading without excluding them
    // from javadoc linking entirely.

    @get:Nested
    val artifacts: Artifacts = objects.newInstance(Artifacts::class)

    @get:Nested
    val javadocArtifacts: Artifacts = objects.newInstance(Artifacts::class)

    fun dependenciesFrom(
        configuration: NamedDomainObjectProvider<Configuration>,
        javadocView: NamedDomainObjectProvider<Configuration>
    ) {
        artifacts.setFrom(configuration)
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

        val output = mutableListOf<String>()
        for (resolvedArtifactResult in artifacts.artifacts.get().shuffled()) {
            val id = resolvedArtifactResult.moduleComponentId() ?: continue
            val coordinates = coordinates(id)
            if (!filter.get().test(id) || skip.get().any { coordinates.startsWith(it) }) {
                continue
            }

            var link: String? = null

            for ((filter, linkOverride) in overrides.get()) {
                if (filter.test(id)) {
                    link = linkOverride.link(defaultJavadocProvider.get(), id)
                    break
                }
            }

            if (link == null) {
                link = defaultJavadocProvider.get().replaceVariables(id)
            }

            val javadocArtifact = javadocArtifacts.artifacts.get()
                .find { it.moduleComponentId() != null && it.moduleComponentId() == id }

            if (checkJavadocAvailability.get()) {
                // We only really need to verify offline linked docs. But with semi-reliable services like javadoc.io,
                // it's useful to have our requests for a given module's docs be blocked behind a single 'priming' request.
                val online = javadocAvailabilityService.get().areJavadocsAvailable(link)
                if (!online) {
                    throw GradleException("Javadoc host is offline or invalid: '$link' (see above for further details)")
                }
            }

            val line = StringBuilder()
            if (javadocArtifact == null) {
                line.append("-link ").append(link)
            } else {
                val unpackTo = unpackedDocs.resolve(coordinates(id).replace(":", "_"))

                fsOps.copy {
                    from(archiveOps.zipTree(javadocArtifact.file))
                    into(unpackTo)
                }

                line.append("-linkoffline ")
                    .append(link)
                    .append(' ')
                    .append(unpackTo.absolutePathString())
            }

            output.add(line.toString())
        }
        file.writeText(output.sorted().joinToString("\n"))
    }

    abstract class Artifacts {
        @get:Internal
        abstract val artifacts: SetProperty<ResolvedArtifactResult>

        /**
         * Only here to ensure inputs get wired properly. [artifacts] is what we care about.
         */
        @get:InputFiles
        @get:Optional
        @get:PathSensitive(PathSensitivity.NONE)
        abstract val files: ConfigurableFileCollection

        fun setFrom(configuration: NamedDomainObjectProvider<Configuration>) {
            artifacts.set(configuration.map { it.incoming.artifacts })
            files.setFrom(configuration)
        }
    }
}
