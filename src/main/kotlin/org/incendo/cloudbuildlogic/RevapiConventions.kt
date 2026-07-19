package org.incendo.cloudbuildlogic

import com.palantir.gradle.revapi.RevapiExtension
import com.palantir.gradle.revapi.RevapiPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

abstract class RevapiConventions : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply(RevapiPlugin::class)

        val oldVersionsProviderFactory = target.objects.newInstance(OldVersionsProviderFactory::class.java)
        target.extensions.configure(RevapiExtension::class) {
            oldVersions.set(oldVersionsProviderFactory.create())
        }
    }
}
