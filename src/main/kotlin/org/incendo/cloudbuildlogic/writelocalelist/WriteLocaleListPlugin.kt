package org.incendo.cloudbuildlogic.writelocalelist

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType

class WriteLocaleListPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val sourceSets = target.extensions.getByType(SourceSetContainer::class)
        target.extensions.create("writeLocaleList", WriteLocaleListExtension::class, target.tasks, sourceSets)
    }
}
