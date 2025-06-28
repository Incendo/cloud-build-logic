package org.incendo.cloudbuildlogic

import org.revapi.gradle.RevapiShim
import org.revapi.gradle.RevapiExtension
import org.revapi.gradle.RevapiPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

abstract class RevapiConventions : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply(RevapiPlugin::class)

        target.extensions.configure(RevapiExtension::class) {
            oldVersions.set(RevapiShim.oldVersionsProvider(target))
        }
    }
}
