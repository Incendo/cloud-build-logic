package org.incendo.cloudbuildlogic

import com.palantir.gradle.revapi.RevApiShim
import com.palantir.gradle.revapi.RevapiExtension
import com.palantir.gradle.revapi.RevapiPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

abstract class RevApiConventions : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply(RevapiPlugin::class)

        target.extensions.configure(RevapiExtension::class) {
            oldVersions.set(RevApiShim.oldVersionsProvider(target))
        }
    }
}
