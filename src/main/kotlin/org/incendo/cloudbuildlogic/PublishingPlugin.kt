package org.incendo.cloudbuildlogic

import net.kyori.indra.IndraExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class PublishingPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("net.kyori.indra.publishing")
        target.plugins.apply(JavadocLinksPlugin::class)
        target.plugins.apply(CrossdocConventions::class)

        target.extensions.configure(IndraExtension::class) {
            signWithKeyFromProperties("signingKey", "signingPassword")
        }
    }
}
