package com.palantir.gradle.revapi

import org.gradle.api.Project
import java.util.function.Predicate
import java.util.stream.Collectors

object RevApiShim {
    fun oldVersionsProvider(
        project: Project,
        filter: Predicate<String> = BetaFilter()
    ) = project.providers.provider<List<String>> {
        GitVersionUtils.previousGitTags(project)
            .filter(filter)
            .limit(3)
            .collect(Collectors.toList())
    }

    class BetaFilter : Predicate<String> {
        override fun test(t: String): Boolean {
            return !t.contains("-beta.") && !t.contains("-rc.")
        }
    }
}
