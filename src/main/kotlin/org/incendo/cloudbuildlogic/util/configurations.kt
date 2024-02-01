package org.incendo.cloudbuildlogic.util

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.project

/**
 * When [includeProjectDependencies] is false, project dependencies will be excluded,
 * but their transitive dependencies will not be.
 */
fun Configuration.extendsFromFlattened(
    other: NamedDomainObjectProvider<Configuration>,
    dependencyHandler: DependencyHandler,
    includeProjectDependencies: Boolean = false,
) {
    defaultDependencies {
        for (artifact in other.get().incoming.artifacts.artifacts) {
            if (includeProjectDependencies && artifact.id.componentIdentifier is ProjectComponentIdentifier) {
                val projectId = artifact.id.componentIdentifier as ProjectComponentIdentifier
                add(dependencyHandler.project(projectId.projectPath))
                continue
            }
            val id = artifact.moduleComponentId() ?: continue
            add(dependencyHandler.create(coordinates(id)))
        }
    }
}

fun Configuration.resolvable() {
    isCanBeResolved = true
    isCanBeConsumed = false
}
