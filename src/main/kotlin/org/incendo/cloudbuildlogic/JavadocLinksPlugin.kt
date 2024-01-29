package org.incendo.cloudbuildlogic

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.Usage
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
import javax.inject.Inject

abstract class JavadocLinksPlugin : Plugin<Project> {
    @get:Inject
    abstract val buildEventsListenerRegistry: BuildEventsListenerRegistry

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
                    extendsFrom(target.configurations.named(apiElementsConfigurationName).get())
                    isCanBeResolved = true
                    isCanBeConsumed = false
                }

                val javadocView = target.configurations.register(linkDependencies.name + "Javadoc") {
                    isCanBeResolved = true
                    isCanBeConsumed = false

                    attributes {
                        attribute(Category.CATEGORY_ATTRIBUTE, target.objects.named(Category.DOCUMENTATION))
                        attribute(Bundling.BUNDLING_ATTRIBUTE, target.objects.named(Bundling.EXTERNAL))
                        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, target.objects.named(DocsType.JAVADOC))
                        attribute(Usage.USAGE_ATTRIBUTE, target.objects.named(Usage.JAVA_RUNTIME))
                    }

                    // Gradle doesn't consider transitives when we simply extend the configuration using the above attributes
                    defaultDependencies {
                        for (artifact in linkDependencies.get().incoming.artifacts.artifacts) {
                            val id = artifact.moduleComponentId() ?: continue
                            add(target.dependencies.create(coordinates(id)))
                        }
                    }
                }

                target.tasks.register<GenerateJavadocLinksFile>(formatName("javadocLinksFile")) {
                    linksFile.convention(target.layout.buildDirectory.file("tmp/$name/links.options"))
                    unpackedJavadocs.convention(target.layout.buildDirectory.dir("tmp/$name/unpackedJavadocs"))
                    overrides.convention(ext.overrides)
                    skip.convention(ext.excludes)
                    defaultJavadocProvider.convention("https://javadoc.io/doc/{group}/{name}/{version}")
                    filter.convention(ext.filter)
                    javadocAvailabilityService.set(service)
                    checkJavadocHostAvailability.convention(
                        target.providers.gradleProperty("cloud-build-logic.checkJavadocHostAvailability")
                            .map { it.toBoolean() }
                            .orElse(true)
                    )
                    dependenciesFrom(linkDependencies, javadocView)
                }
            }
        }

        target.afterEvaluate {
            forEachTargetedSourceSet {
                // Just disable for Kotlin projects. Supporting mixed language source sets is out of scope.
                if (!plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
                    val linksFileTask = tasks.named<GenerateJavadocLinksFile>(formatName("javadocLinksFile"))
                    val linksOutput = linksFileTask.flatMap { it.linksFile }
                    tasks.maybeConfigure<Javadoc>(javadocTaskName) {
                        inputs.file(linksOutput)
                            .withPropertyName("javadocLinksFile")
                        inputs.dir(linksFileTask.flatMap { it.unpackedJavadocs })
                            .withPropertyName("unpackedJavadocs")
                        doFirst {
                            val opts = options as StandardJavadocDocletOptions
                            opts.linksFile(linksOutput.get().asFile)
                        }
                    }
                }
            }
        }
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
