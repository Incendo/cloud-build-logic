package org.incendo.cloudbuildlogic.javadoclinks

import org.gradle.api.GradleException
import org.gradle.api.logging.Logging
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

abstract class JavadocAvailabilityService : BuildService<BuildServiceParameters.None>,
    OperationCompletionListener, AutoCloseable {
    companion object {
        private val logger = Logging.getLogger(JavadocAvailabilityService::class.java)
    }

    private val cache = ConcurrentHashMap<String, Boolean>()

    fun areJavadocsAvailable(url: String): Boolean = cache.computeIfAbsent(url) {
        checkAvailability(if (it.endsWith('/')) it else "$it/")
    }

    private fun checkAvailability(url: String, attemptNo: Int = 0): Boolean {
        val urls = buildList {
            add(url + "element-list")
            add(url + "package-list/")
        }
        if (attemptNo > 2) {
            logger.error("Javadoc at '{}' is still not available after 3 attempts.", url)
            return false
        }
        val results = mutableListOf<Pair<String, Result<Int>>>()
        for (hostString in urls) {
            val hostUrl = URL(hostString)
            val response = runCatching {
                val connection = hostUrl.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.responseCode
            }
            results.add(hostString to response)
            if (response.getOrNull() == HttpURLConnection.HTTP_OK) {
                return true
            }
        }
        if (results.all { it.second.getOrNull() == 403 }) {
            if (url.startsWith("https://javadoc.io/")) {
                logger.lifecycle("Got 403 for element-list and package-list/ of '{}', will attempt to prime docs and then retry in 15s...", url)
                try {
                    primeJavadocIo(url)
                } catch (ex: IOException) {
                    logger.error("Failed to prime Javadocs at '{}'", url, ex)
                    return false
                }
                Thread.sleep(Duration.ofSeconds(15).toMillis())
                return checkAvailability(url, attemptNo + 1)
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
        return false
    }

    private fun primeJavadocIo(url: String) {
        val staticUrl = url.replace("https://javadoc.io/doc/", "https://javadoc.io/static/")
        val conn = URL(staticUrl)
            .openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true
        val responseCode = conn.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            logger.error("Attempt to prime docs via '{}' returned response code {}", staticUrl, responseCode)
        }
    }

    override fun onFinish(event: FinishEvent?) {
        // no-op, a workaround to keep the service alive for the entire build
        // see https://github.com/diffplug/spotless/pull/720#issuecomment-713399731
    }

    override fun close() {
        // see comments in onFinish
    }
}
