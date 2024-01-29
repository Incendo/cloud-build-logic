package org.incendo.cloudbuildlogic

import org.gradle.api.logging.Logging
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

abstract class JavadocHostAvailabilityService : BuildService<BuildServiceParameters.None>,
    OperationCompletionListener, AutoCloseable {
    companion object {
        private val logger = Logging.getLogger(JavadocHostAvailabilityService::class.java)
    }

    private val cache = ConcurrentHashMap<String, Boolean>()

    fun isHostOnline(url: String): Boolean {
        return cache.computeIfAbsent(url) {
            val hostString = url.let { if (it.endsWith('/')) it else "$it/" }
            val hostUrl = URL(hostString)
            val response = runCatching {
                val connection = hostUrl.openConnection() as HttpURLConnection
                connection.getResponseCode()
            }
            return@computeIfAbsent response.fold(
                {
                    if (it == HttpURLConnection.HTTP_OK) {
                        true
                    } else {
                        logger.error("Got $it response code from $hostString")
                        false
                    }
                },
                {
                    logger.error("Error getting response from $hostString", it)
                    false
                }
            )
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
