package org.incendo.cloudbuildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class RootProjectPublishingPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("net.kyori.indra.publishing.sonatype")
    }
}
