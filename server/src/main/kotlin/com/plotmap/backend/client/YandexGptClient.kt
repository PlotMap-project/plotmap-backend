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

@Component
class YandexGptClient(
    private val properties: YandexGptProperties,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val httpClient = HttpClient.newBuilder().build()

    fun generateRawGraphJson(text: String): String {
        require(text.isNotBlank()) { "Text must not be empty" }
        require(properties.apiKey.isNotBlank()) { "API key is not configured" }
        require(properties.agentId.isNotBlank()) { "Agent ID is not configured" }
        require(properties.organization.isNotBlank()) { "Organization is not configured" }

        val cleanedAgentId = properties.agentId.trim().removePrefix("\"").removeSuffix("\"")
        val cleanedOrganization = properties.organization.trim().removePrefix("\"").removeSuffix("\"")

        log.info("Using agentId='{}', organization='{}'", cleanedAgentId, cleanedOrganization)

        val requestBody = mapOf(
            "prompt" to mapOf("id" to cleanedAgentId),
            "input" to text
        )

        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://ai.api.cloud.yandex.net/v1/responses"))
            .header("Authorization", "Bearer ${properties.apiKey}")
            .header("OpenAI-Organization", cleanedOrganization)
            .header("Content-Type", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    objectMapper.writeValueAsString(requestBody)
                )
            )
            .build()

        val response = httpClient.send(
            httpRequest,
            HttpResponse.BodyHandlers.ofString()
        )

        if (response.statusCode() < 200 || response.statusCode() > 299) {
            log.error("Yandex Agent request failed: {} {}", response.statusCode(), response.body())
            throw IllegalStateException(
                "Yandex Agent request failed: ${response.statusCode()} ${response.body()}"
            )
        }

        log.info("Full agent response body: {}", response.body())

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
        log.debug("Extracted output text: {}", outputText)
        return outputText
    }

    private fun extractOutputText(root: JsonNode): String {
        val directOutputText = root.path("output_text").asText().trim()
        if (directOutputText.isNotBlank()) {
            return directOutputText
        }

        val directCamelOutputText = root.path("outputText").asText().trim()
        if (directCamelOutputText.isNotBlank()) {
            return directCamelOutputText
        }

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

        throw IllegalStateException("Agent response does not contain output text in expected format")
    }
}
