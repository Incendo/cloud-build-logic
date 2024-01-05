package org.incendo.cloudbuildlogic

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

abstract class CloudSpotlessExtension(project: Project) {
    abstract val licenseHeaderFile: RegularFileProperty
    abstract val ktlintVersion: Property<String>

    init {
        init(project)
    }

    private fun init(project: Project) {
        licenseHeaderFile.convention(project.rootProject.layout.projectDirectory.file("HEADER"))
        ktlintVersion.convention("0.50.0")
    }
}
