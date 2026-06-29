package com.plotmap.backend.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.plotmap.backend.config.YandexGptProperties
import com.plotmap.backend.exception.ContentFilteredException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Component
class YandexGptClient(
    private val properties: YandexGptProperties,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    fun generateRawGraphJson(text: String): String {
        require(text.isNotBlank()) { "Text must not be empty" }

        val requestBody = mapOf(
            "prompt" to mapOf("id" to properties.agentId.trim()),
            "input" to text
        )

        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://ai.api.cloud.yandex.net/v1/responses"))
            .header("Authorization", "Bearer ${properties.apiKey}")
            .header("OpenAI-Organization", properties.organization.trim())
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(120))
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    objectMapper.writeValueAsString(requestBody)
                )
            )
            .build()

        log.info("Sending request to Yandex GPT, text length={}", text.length)
        val response = httpClient.send(
            httpRequest,
            HttpResponse.BodyHandlers.ofString()
        )

        if (response.statusCode() < 200 || response.statusCode() > 299) {
            log.error("Yandex GPT request failed: status={}", response.statusCode())
            log.debug("Yandex GPT error body: {}", response.body().take(1000))
            throw IllegalStateException(
                "Yandex GPT request failed with status ${response.statusCode()}"
            )
        }

        log.info("Yandex GPT responded: status={}, bodyLength={}", response.statusCode(), response.body().length)
        log.debug("Yandex GPT response body (first 500 chars): {}", response.body().take(500))

        val root = objectMapper.readTree(response.body())
        val status = root.path("status").asText()
        val incompleteReason = root.path("incomplete_details").path("reason").asText()

        if (status == "incomplete" && incompleteReason == "content_filter") {
            val refusalText = root.path("output")
                .takeIf { it.isArray && it.size() > 0 }
                ?.get(0)
                ?.path("content")
                ?.takeIf { it.isArray && it.size() > 0 }
                ?.get(0)
                ?.path("text")
                ?.asText()
                ?.trim()
                ?: "Content filtered by model"

            throw ContentFilteredException("Chunk was filtered by model: $refusalText")
        }
        val outputText = extractOutputText(root)
        log.info("Extracted output text length={}", outputText.length)
        log.debug("Extracted output text (first 500 chars): {}", outputText.take(500))
        return outputText
    }

    private fun extractOutputText(root: JsonNode): String {
        val outputArray = root.path("output")
        if (outputArray.isArray) {
            val parts = mutableListOf<String>()

            outputArray.forEach { outputItem ->
                val contentArray = outputItem.path("content")
                if (contentArray.isArray) {
                    contentArray.forEach { contentItem ->
                        val type = contentItem.path("type").asText()
                        val text = contentItem.path("text").asText()

                        if (type == "output_text" && text.isNotBlank()) {
                            parts.add(text)
                        }
                    }
                }
            }

            val joined = parts.joinToString("\n").trim()
            if (joined.isNotBlank()) {
                return joined
            }
        }

        val directOutputText = root.path("output_text").asText().trim()
        if (directOutputText.isNotBlank()) {
            return directOutputText
        }

        throw IllegalStateException("Agent response does not contain output text in expected format")
    }
}
