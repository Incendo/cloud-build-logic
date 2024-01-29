package org.incendo.cloudbuildlogic

import net.kyori.indra.crossdoc.CrossdocExtension
import net.kyori.indra.crossdoc.CrossdocPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

class CrossdocConventions : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.withId("java-library") {
            target.plugins.apply(CrossdocPlugin::class)

            // after any groupId changes
            target.afterEvaluate {
                target.extensions.getByType(CrossdocExtension::class).apply {
                    baseUrl().convention("https://javadoc.io/${target.group}/")
                }
            }
        }
    }
}
