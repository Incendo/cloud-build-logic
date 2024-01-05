package org.incendo.cloudbuildlogic

import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withType

class ErrorpronePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("net.ltgt.errorprone")

        target.tasks.withType<JavaCompile>().configureEach {
            options.errorprone {
                /* These are just annoying */
                disable(
                    "JdkObsolete",
                    "FutureReturnValueIgnored",
                    "ImmutableEnumChecker",
                    "StringSplitter",
                    "EqualsGetClass",
                    "CatchAndPrintStackTrace",
                    "InlineMeSuggester",
                    "InlineTrivialConstant",
                    "FunctionalInterfaceMethodChanged"
                )
                disableWarningsInGeneratedCode.set(true)
            }
        }
    }
}
