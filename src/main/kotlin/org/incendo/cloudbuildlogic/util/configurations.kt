package org.incendo.cloudbuildlogic.util

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.incendo.cloudbuildlogic.coordinates
import org.incendo.cloudbuildlogic.moduleComponentId

fun Configuration.extendsFromFlattened(
    other: NamedDomainObjectProvider<Configuration>,
    dependencyHandler: DependencyHandler,
) {
    defaultDependencies {
        for (artifact in other.get().incoming.artifacts.artifacts) {
            val id = artifact.moduleComponentId() ?: continue
            add(dependencyHandler.create(coordinates(id)))
        }
    }
}
