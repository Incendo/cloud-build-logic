package org.incendo.cloudbuildlogic.writelocalelist

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject
import kotlin.io.path.listDirectoryEntries

@Suppress("LeakingThis")
abstract class WriteLocaleList : DefaultTask() {
    @get:InputDirectory
    abstract val dir: DirectoryProperty

    @get:Input
    abstract val sourceSet: Property<String>

    @get:Input
    abstract val key: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val layout: ProjectLayout

    init {
        sourceSet.convention("main")
        outputDir.convention(
            key.zip(sourceSet) { key, sourceSet ->
                layout.buildDirectory.dir("tmp/writeLocales/$sourceSet/${key.replace(".", "_")}")
            }.flatMap { it }
        )
        dir.convention(
            key.zip(sourceSet) { key, sourceSet ->
                layout.projectDirectory.dir("src/$sourceSet/resources").dir(
                    key.replace(".", "/").substringBeforeLast("/")
                )
            }
        )
    }

    @TaskAction
    fun run() {
        val dir = outputDir.get().asFile
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val name = key.get().substringAfterLast(".")
        val f = dir.resolve(key.get().replace(".", "/") + "-locales.list")
        if (f.exists()) {
            f.delete()
        }
        f.parentFile.mkdirs()

        f.writeText(
            this.dir.get().asFile.toPath().listDirectoryEntries()
                .mapNotNull {
                    if (!it.fileName.toString().startsWith("${name}_")) {
                        return@mapNotNull null
                    }

                    it.fileName.toString().substringAfter("${name}_").substringBefore(".properties")
                }
                .sorted()
                .joinToString("\n")
        )
    }
}
