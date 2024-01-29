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

        target.plugins.withId("java-library") {
            target.extensions.getByType(SourceSetContainer::class).configureEach {
                if (apiElementsConfigurationName !in target.configurations.names) {
                    return@configureEach
                }

                val linkDependencies = target.configurations.register(formatName("javadocLinks")) {
                    extendsFrom(target.configurations.named(apiElementsConfigurationName).get())
                    isCanBeResolved = true
                    isCanBeConsumed = false
                }

                val linksFileTask = target.tasks.register<GenerateJavadocLinksFile>(formatName("javadocLinksFile")) {
                    linksFile.convention(target.layout.buildDirectory.file("tmp/$name.options"))
                    overrides.convention(ext.overrides)
                    skip.convention(ext.excludes)
                    defaultJavadocProvider.convention("https://javadoc.io/doc/{group}/{name}/{version}")
                    filter.convention(ext.filter)
                    dependenciesFrom(linkDependencies)
                }

                val linksOutput = linksFileTask.flatMap { it.linksFile }
                target.tasks.maybeConfigure<Javadoc>(javadocTaskName) {
                    inputs.file(linksOutput)
                        .withPropertyName("javadocLinksFile")
                    doFirst {
                        val opts = options as StandardJavadocDocletOptions
                        opts.linksFile(linksOutput.get().asFile)
                    }
                }
            }
        }
    }
}
