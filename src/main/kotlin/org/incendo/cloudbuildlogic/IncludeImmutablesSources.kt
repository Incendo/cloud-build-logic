package org.incendo.cloudbuildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.incendo.cloudbuildlogic.util.maybeConfigure

/**
 * Plugin that includes generated sources in source jars and javadoc for source sets with the
 * immutables value annotation processor. Must be applied *after* javadoc/sourcesJar is enabled.
 */
class IncludeImmutablesSources : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.withId("java") {
            project.extensions.getByType(SourceSetContainer::class).configureEach {
                val sourceSet = this
                val compile = project.tasks.named(compileJavaTaskName, JavaCompile::class)

                project.tasks.maybeConfigure<Zip>(sourcesJarTaskName) {
                    from(generatedSourceOrEmpty(compile, project, sourceSet))
                }
                project.tasks.maybeConfigure<Javadoc>(javadocTaskName) {
                    source(generatedSourceOrEmpty(compile, project, sourceSet))
                }
            }
        }
    }
}

private fun generatedSourceOrEmpty(
    compile: TaskProvider<JavaCompile>,
    project: Project,
    sourceSet: SourceSet
): Provider<Any> = compile.flatMap {
    if (hasImmutablesProcessor(project, sourceSet)) {
        it.options.generatedSourceOutputDirectory
    } else {
        project.provider { emptyList<Any>() }
    }
}

/**
 * Based on checks in [com.palantir.baseline.plugins.BaselineImmutables]
 */
private fun hasImmutablesProcessor(project: Project, sourceSet: SourceSet): Boolean {
    return project
        .configurations
        .getByName(sourceSet.annotationProcessorConfigurationName)
        .incoming
        .resolutionResult
        .allComponents
        .any(::isImmutablesValue)
}

private fun isImmutablesValue(component: ResolvedComponentResult): Boolean {
    val id = component.id as? ModuleComponentIdentifier ?: return false

    return id.group == "org.immutables" && id.module == "value"
}
