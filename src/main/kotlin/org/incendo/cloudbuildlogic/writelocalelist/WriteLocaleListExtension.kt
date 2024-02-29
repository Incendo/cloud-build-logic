package org.incendo.cloudbuildlogic.writelocalelist

import org.gradle.api.Action
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

abstract class WriteLocaleListExtension(
    private val tasks: TaskContainer,
    private val sourceSets: SourceSetContainer
) {
    @JvmOverloads
    fun registerBundle(sourceSet: String, key: String, op: Action<WriteLocaleList> = Action {}): TaskProvider<WriteLocaleList> {
        val task = tasks.register(
            "writeLocales_${sourceSet}_${key.replace(".", "-")}",
            WriteLocaleList::class
        ) {
            this.sourceSet.set(sourceSet)
            this.sourceSet.disallowChanges()
            this.key.set(key)
            op.execute(this)
        }
        sourceSets.named(sourceSet) {
            resources {
                srcDir(task.flatMap { it.outputDir })
            }
        }
        return task
    }

    @JvmOverloads
    fun registerBundle(key: String, op: Action<WriteLocaleList> = Action {}): TaskProvider<WriteLocaleList> {
        return registerBundle("main", key, op)
    }
}
