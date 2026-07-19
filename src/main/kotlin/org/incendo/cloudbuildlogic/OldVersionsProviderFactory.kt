package org.incendo.cloudbuildlogic

import com.palantir.gradle.revapi.GitOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import java.lang.reflect.Method
import java.util.function.Predicate
import javax.inject.Inject

abstract class OldVersionsProviderFactory @Inject constructor(objects: ObjectFactory) {
    private val git = objects.newInstance(GitOperations::class.java)

    fun create(filter: Predicate<String> = BetaFilter()): Provider<List<String>> =
        collectVersions(previousGitTagFromRef(git, "HEAD"), filter, emptyList())

    private fun collectVersions(
        nextTag: Provider<String>,
        filter: Predicate<String>,
        versions: List<String>
    ): Provider<List<String>> = nextTag
        .flatMap { tag ->
            val version = tag.removePrefix("v")
            val collected = if (filter.test(version)) versions + version else versions
            if (collected.size == 3) {
                nextTag.map { collected }
            } else {
                collectVersions(previousGitTagFromRef(git, tag), filter, collected)
            }
        }
        .orElse(versions)

    @Suppress("UNCHECKED_CAST")
    private fun previousGitTagFromRef(git: GitOperations, ref: String): Provider<String> =
        PREVIOUS_GIT_TAG_FROM_REF.invoke(git, ref) as Provider<String>

    class BetaFilter : Predicate<String> {
        override fun test(version: String): Boolean =
            !version.contains("-beta.") && !version.contains("-rc.")
    }

    private companion object {
        val PREVIOUS_GIT_TAG_FROM_REF: Method = GitOperations::class.java
            .getDeclaredMethod("previousGitTagFromRef", String::class.java)
            .apply {
                check(trySetAccessible()) {
                    "Could not access GitOperations.previousGitTagFromRef"
                }
            }
    }
}
