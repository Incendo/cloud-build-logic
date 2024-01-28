package org.incendo.cloudbuildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

abstract class JavadocLinksPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val ext = target.extensions.create("javadocLinks", JavadocLinksExtension::class)

        target.plugins.withId("java") {
            target.extensions.getByType(SourceSetContainer::class).configureEach {
                if (apiElementsConfigurationName !in target.configurations.names) {
                    return@configureEach
                }

                val apiElementsCopy = target.configurations.register(apiElementsConfigurationName + "JavadocLinksCopy") {
                    extendsFrom(target.configurations.named(apiElementsConfigurationName).get())
                    isCanBeResolved = true
                    isCanBeConsumed = false
                    isCanBeDeclared = false
                }

                val linksFileTask = target.tasks.register<GenerateJavadocLinksFile>(name + "JavadocLinksFile") {
                    linksFile.convention(target.layout.buildDirectory.file(name))
                    overrides.convention(ext.overrides)
                    skip.convention(ext.skip)
                    defaultJavadocProvider.convention("https://javadoc.io/doc/{group}/{name}/{version}")
                    filter.convention(ext.filter)
                    apiElements(apiElementsCopy)
                }

                target.tasks.maybeConfigure<Javadoc>(javadocTaskName) {
                    val opts = options as StandardJavadocDocletOptions
                    dependsOn(linksFileTask)
                    opts.linksFile(linksFileTask.get().linksFile.get().asFile)
                }
            }
        }
    }
}
