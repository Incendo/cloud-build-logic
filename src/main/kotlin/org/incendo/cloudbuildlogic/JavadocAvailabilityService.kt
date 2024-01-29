package org.incendo.cloudbuildlogic

import org.gradle.api.GradleException
import org.gradle.api.logging.Logging
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

abstract class JavadocAvailabilityService : BuildService<BuildServiceParameters.None>,
    OperationCompletionListener, AutoCloseable {
    companion object {
        private val logger = Logging.getLogger(JavadocAvailabilityService::class.java)
    }

    private val cache = ConcurrentHashMap<String, Boolean>()

    fun areJavadocsAvailable(url: String): Boolean = cache.computeIfAbsent(url) {
        val baseUrl = url.let { if (it.endsWith('/')) it else "$it/" }
        val urls = setOf("element-list", "package-list/").map { baseUrl + it }
        val results = mutableListOf<Pair<String, Result<Int>>>()
        for (hostString in urls) {
            val hostUrl = URL(hostString)
            val response = runCatching {
                val connection = hostUrl.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.getResponseCode()
            }
            results.add(hostString to response)
            if (response.getOrNull() == HttpURLConnection.HTTP_OK) {
                return@computeIfAbsent true
            }
        }
        var ex: Throwable? = null
        for (r in results) {
            val newEx = r.second.fold(
                { GradleException("Got $it response code from ${r.first}") },
                { GradleException("Error getting response from ${r.first}", it) }
            )

            if (ex == null) {
                ex = newEx
            } else {
                ex.addSuppressed(newEx)
            }
        }
        logger.error("Could not locate element-list or package-list for docs: '$url'", requireNotNull(ex))
        return@computeIfAbsent false
    }

    override fun onFinish(event: FinishEvent?) {
        // no-op, a workaround to keep the service alive for the entire build
        // see https://github.com/diffplug/spotless/pull/720#issuecomment-713399731
    }

    override fun close() {
        // see comments in onFinish
    }
}
