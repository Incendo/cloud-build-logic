package org.incendo.cloudbuildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.util.function.Function
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

abstract class GenerateJavadocLinksFile : DefaultTask() {
    @get:Input
    abstract val overrides: MapProperty<String, String>

    @get:OutputFile
    abstract val linksFile: RegularFileProperty

    @get:Internal
    abstract val apiElements: SetProperty<ResolvedArtifactResult>

    @get:InputFiles
    @get:Optional
    abstract val apiElementsFiles: ConfigurableFileCollection

    @get:Input
    abstract val defaultJavadocProvider: Property<String>

    fun apiElements(configuration: NamedDomainObjectProvider<Configuration>) {
        apiElements.set(configuration.map { it.incoming.artifacts })
        apiElementsFiles.setFrom(configuration)
    }

    @TaskAction
    fun run() {
        val file = linksFile.asFile.get().toPath()
        file.deleteIfExists()
        file.parent.createDirectories()
        val output = StringBuilder()
        for (resolvedArtifactResult in apiElements.sorted()) {
            val id = resolvedArtifactResult.componentIdentifier() ?: continue

            output.append("-link ")
            val coordinates = coordinates(id)
            var overridden = false
            for ((c, o) in overrides.get()) {
                if (coordinates.startsWith(c)) {
                    overridden = true
                    output.append(o.replaceVariables(id))
                    break
                }
            }
            if (!overridden) {
                output.append(defaultJavadocProvider.get().replaceVariables(id))
            }
            output.append('\n')
        }
        file.writeText(output.toString())
    }

    private fun String.replaceVariables(id: ModuleComponentIdentifier): String {
        return replace("{group}", id.group)
            .replace("{name}", id.module)
            .replace("{version}", id.version)
    }

    private fun ResolvedArtifactResult.componentIdentifier(): ModuleComponentIdentifier? =
        id.componentIdentifier as? ModuleComponentIdentifier

    private fun coordinates(componentId: ModuleComponentIdentifier): String {
        return StringBuilder()
            .append(componentId.group)
            .append(':')
            .append(componentId.module)
            .append(':')
            .append(componentId.version)
            .toString()
    }

    private fun Provider<Set<ResolvedArtifactResult>>.sorted(): List<ResolvedArtifactResult> = get().sortedWith(
        Comparator.comparing<ResolvedArtifactResult, String> { it.id.componentIdentifier.displayName }
            .thenComparing(Function { it.file.name })
    )
}
