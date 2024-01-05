package org.incendo.cloudbuildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class SpotlessRootProjectPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("com.diffplug.spotless")

        target.extensions.configure(SpotlessExtension::class) {
            predeclareDeps()
        }
    }
}
