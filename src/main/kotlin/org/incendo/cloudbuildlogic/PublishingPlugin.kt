package org.incendo.cloudbuildlogic

import dev.lukebemish.centralportalpublishing.CentralPortalRepositoryHandlerExtension
import net.kyori.indra.IndraExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.credentials
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.maven
import org.incendo.cloudbuildlogic.javadoclinks.JavadocLinksPlugin
import javax.inject.Inject

abstract class PublishingPlugin : Plugin<Project> {
    @get:Inject
    abstract val providers: ProviderFactory

    override fun apply(target: Project) {
        target.plugins.apply("net.kyori.indra.publishing")
        target.plugins.apply("dev.lukebemish.central-portal-publishing")
        target.plugins.apply(JavadocLinksPlugin::class)
        target.plugins.apply(CrossdocConventions::class)
        target.plugins.apply(IncludeImmutablesSources::class)

        target.extensions.configure(IndraExtension::class) {
            signWithKeyFromProperties("signingKey", "signingPassword")
        }

        target.extensions.configure(PublishingExtension::class) {
            repositories {
                val portal = (this as ExtensionAware).extensions.getByType(CentralPortalRepositoryHandlerExtension::class)
                portal.portalBundle(":", "release")

                maven("https://central.sonatype.com/repository/maven-snapshots/") {
                    name = "SonatypeSnapshots"
                    credentials(PasswordCredentials::class) {
                        username = providers.gradleProperty("sonatypeUsername").getOrElse("username")
                        password = providers.gradleProperty("sonatypePassword").getOrElse("password")
                    }
                }
            }
        }
    }
}
