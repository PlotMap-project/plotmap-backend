package com.plotmap.backend.client

import com.plotmap.backend.config.YandexGptProperties
import com.plotmap.backend.exception.ContentFilteredException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
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

        val cleanedAgentId = properties.agentId.trim().removeSurrounding("\"")
        val cleanedOrganization = properties.organization.trim().removeSurrounding("\"")

        val requestBody = mapOf(
            "prompt" to mapOf("id" to cleanedAgentId),
            "input" to text
        )

        log.debug("Sending request to Yandex GPT agent, inputLength={}", text.length)

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

        log.debug("Yandex GPT response received: statusCode={}", response.statusCode())

        if (response.statusCode() !in 200..299) {
            log.error("Yandex GPT request failed: statusCode={}", response.statusCode())
            throw IllegalStateException(
                "Yandex Agent request failed with status: ${response.statusCode()}"
            )
        }

        val root = objectMapper.readTree(response.body())
        val status = root.path("status").asText("")
        val incompleteReason = root.path("incomplete_details").path("reason").asText("")

        if (status == "incomplete" && incompleteReason == "content_filter") {
            val refusalText = root.path("output")
                .takeIf { it.isArray && it.size() > 0 }
                ?.get(0)
                ?.path("content")
                ?.takeIf { it.isArray && it.size() > 0 }
                ?.get(0)
                ?.path("text")
                ?.asText("")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "Content filtered by model"

            throw ContentFilteredException("Chunk was filtered by model: $refusalText")
        }

        val outputText = extractOutputText(root)
        log.info("Yandex GPT generation completed: outputLength={}", outputText.length)
        return outputText
    }

    private fun extractOutputText(root: JsonNode): String {
        val directOutputText = root.path("output_text").asText("").trim()
        if (directOutputText.isNotBlank()) return directOutputText

        val directCamelOutputText = root.path("outputText").asText("").trim()
        if (directCamelOutputText.isNotBlank()) return directCamelOutputText

        val outputArray = root.path("output")
        if (outputArray.isArray) {
            val parts = outputArray.flatMap { outputItem ->
                val contentArray = outputItem.path("content")
                if (contentArray.isArray) {
                    contentArray
                        .filter { it.path("type").asText("") == "output_text" }
                        .mapNotNull {
                            it.path("text").asText("").trim().takeIf(String::isNotBlank)
                        }
                } else {
                    emptyList()
                }
            }

            val joined = parts.joinToString("\n").trim()
            if (joined.isNotBlank()) return joined
        }

        throw IllegalStateException("Agent response does not contain output text in expected format")
    }
}
