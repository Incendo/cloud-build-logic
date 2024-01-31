package org.incendo.cloudbuildlogic.javadoclinks

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registerIfAbsent
import org.incendo.cloudbuildlogic.util.apiElements
import org.incendo.cloudbuildlogic.util.extendsFromFlattened
import org.incendo.cloudbuildlogic.util.formatName
import org.incendo.cloudbuildlogic.util.javadocElements
import org.incendo.cloudbuildlogic.util.maybeConfigure
import org.incendo.cloudbuildlogic.util.resolvable
import org.incendo.cloudbuildlogic.util.sourcesElements
import javax.inject.Inject

abstract class JavadocLinksPlugin : Plugin<Project> {
    @get:Inject
    abstract val buildEventsListenerRegistry: BuildEventsListenerRegistry

    @get:Inject
    abstract val objects: ObjectFactory

    override fun apply(target: Project) {
        val ext = target.extensions.create("javadocLinks", JavadocLinksExtension::class)
        val service = target.gradle.sharedServices.registerIfAbsent(
            JavadocAvailabilityService::class.java.simpleName,
            JavadocAvailabilityService::class
        ) {}
        buildEventsListenerRegistry.onTaskCompletion(service)

        target.plugins.withId("java-library") {
            target.forEachTargetedSourceSet {
                val linkDependencies = target.configurations.register(formatName("javadocLinks")) {
                    resolvable()
                    attributes {
                        apiElements(objects)
                    }
                    extendsFrom(target.configurations.named(apiElementsConfigurationName).get())
                }

                val javadocConfig = target.configurations.register(linkDependencies.name + "Javadoc") {
                    resolvable()
                    attributes {
                        javadocElements(objects)
                    }
                    // Gradle doesn't consider transitives when we simply extend the configuration using the above attributes
                    extendsFromFlattened(linkDependencies, target.dependencies)
                }

                val sourcesConfig = target.configurations.register(linkDependencies.name + "Sources") {
                    resolvable()
                    attributes {
                        sourcesElements(objects)
                    }
                    // Gradle doesn't consider transitives when we simply extend the configuration using the above attributes
                    extendsFromFlattened(linkDependencies, target.dependencies)
                }
                val sourcesView = sourcesConfig.map {
                    val view = it.incoming.artifactView {
                        lenient(true)
                    }
                    return@map view.artifacts
                }

                target.tasks.register<PrepareJavadocLinks>(taskName()) {
                    linksFile.convention(target.layout.buildDirectory.file("tmp/$name/links.options"))
                    unpackedJavadocs.convention(target.layout.buildDirectory.dir("tmp/$name/unpackedJavadocs"))
                    overrides.convention(ext.overrides)
                    skip.convention(ext.excludes)
                    defaultJavadocProvider.convention(ext.defaultJavadocProvider)
                    filter.convention(ext.filter)
                    javadocAvailabilityService.set(service)
                    checkJavadocAvailability.convention(ext.checkJavadocAvailability)
                    dependenciesFrom(linkDependencies, javadocConfig, sourcesView)
                }
            }
        }

        target.afterEvaluate {
            // Just disable for Kotlin projects. Supporting mixed language source sets is out of scope.
            if (plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
                return@afterEvaluate
            }

            forEachTargetedSourceSet {
                val linksFileTask = tasks.named<PrepareJavadocLinks>(taskName())
                val linksOutput = linksFileTask.flatMap { it.linksFile }
                tasks.maybeConfigure<Javadoc>(javadocTaskName) {
                    inputs.file(linksOutput)
                        .withPropertyName("javadocLinksFile")
                    inputs.dir(linksFileTask.flatMap { it.unpackedJavadocs })
                        .withPropertyName("unpackedJavadocs")
                    inputs.files(linksFileTask.map { it.sourcesArtifacts.files })
                        .withPropertyName("sourceArtifacts")
                    doFirst {
                        val opts = options as StandardJavadocDocletOptions
                        opts.linksFile(linksOutput.get().asFile)
                    }
                }
            }
        }
    }

    private fun SourceSet.taskName(): String {
        return formatName("prepare", "javadocLinks")
    }

    private fun Project.forEachTargetedSourceSet(action: Action<SourceSet>) {
        extensions.getByType(SourceSetContainer::class).configureEach {
            if (apiElementsConfigurationName !in configurations.names) {
                return@configureEach
            }

            action.execute(this)
        }
    }
}
