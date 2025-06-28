package org.incendo.cloudbuildlogic

import dev.lukebemish.centralportalpublishing.CentralPortalProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.getByType
import javax.inject.Inject

abstract class RootProjectPublishingPlugin : Plugin<Project> {
    @get:Inject
    abstract val providers: ProviderFactory

    override fun apply(target: Project) {
        target.plugins.apply("dev.lukebemish.central-portal-publishing")

        target.extensions.getByType<CentralPortalProjectExtension>().bundle("release") {
            username.convention(providers.gradleProperty("sonatypeUsername"))
            password.convention(providers.gradleProperty("sonatypePassword"))
            publishingType.convention("AUTOMATIC")
        }
    }
}
