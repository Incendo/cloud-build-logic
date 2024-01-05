package org.incendo.cloudbuildlogic

import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create

class SpotlessPlugin : Plugin<Project> {
    companion object {
        private const val KOTLIN_JVM_PLUGIN_ID = "org.jetbrains.kotlin.jvm"
    }

    private fun FormatExtension.applyCommon(spaces: Int = 4) {
        indentWithSpaces(spaces)
        trimTrailingWhitespace()
        endWithNewline()
    }

    private fun Project.spotless(op: Action<SpotlessExtension>) {
        extensions.configure(SpotlessExtension::class, op)
    }

    override fun apply(target: Project) {
        target.plugins.apply("com.diffplug.spotless")

        val ext = target.extensions.create("cloudSpotless", CloudSpotlessExtension::class, target)

        target.spotless {
            java {
                applyCommon()
            }
            kotlinGradle {
                target("*.gradle.kts", "src/*/kotlin/**.gradle.kts")
                ktlint(ext.ktlintVersion.get())
                applyCommon()
            }
            format("configs") {
                target("**/*.yml", "**/*.yaml", "**/*.json")
                targetExclude("run/**")
                applyCommon(2)
            }
        }

        target.plugins.withId(KOTLIN_JVM_PLUGIN_ID) {
            target.spotless {
                kotlin {
                    targetExclude("src/*/kotlin/**.gradle.kts", "build/generated-sources/**")
                    ktlint(ext.ktlintVersion.get())
                        .editorConfigOverride(
                            mapOf(
                                "ktlint_standard_filename" to "disabled",
                                "ktlint_standard_trailing-comma-on-call-site" to "disabled",
                                "ktlint_standard_trailing-comma-on-declaration-site" to "disabled",
                            )
                        )

                    applyCommon()
                }
            }
        }

        target.afterEvaluate {
            if (!ext.licenseHeaderFile.isPresent) {
                return@afterEvaluate
            }

            spotless {
                java {
                    licenseHeaderFile(ext.licenseHeaderFile)
                }
            }

            target.plugins.withId(KOTLIN_JVM_PLUGIN_ID) {
                spotless {
                    kotlin {
                        licenseHeaderFile(ext.licenseHeaderFile)
                    }
                }
            }
        }
    }
}
