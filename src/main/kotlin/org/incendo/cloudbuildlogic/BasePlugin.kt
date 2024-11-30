package org.incendo.cloudbuildlogic

import com.palantir.baseline.plugins.BaselineImmutables
import net.kyori.indra.IndraExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

class BasePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("net.kyori.indra")
        target.plugins.apply("net.kyori.indra.checkstyle")
        target.plugins.apply(BaselineImmutables::class)

        target.extensions.configure(IndraExtension::class) {
            javaVersions {
                minimumToolchain(21)
                target(8)
                testWith().set(setOf(8, 11, 17, 21))
            }
        }

        target.tasks.withType<JavaCompile>().configureEach {
            // -processing: ignore unclaimed annotations
            // -classfile: ignore annotations/annotation methods missing in dependencies
            // -serial: we don't support java serialization
            // -options: ignore java 8 deprecation with jdk 21
            options.compilerArgs.addAll(listOf("-Xlint:-processing,-classfile,-serial,-options", "-Werror"))
        }

        target.tasks.withType<AbstractArchiveTask>().configureEach {
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
        }
    }
}
