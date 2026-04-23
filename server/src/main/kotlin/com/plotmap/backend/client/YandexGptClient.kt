package com.plotmap.backend.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class YandexGptClient(
    @Value("\${yandex.gpt.api-key}")
    private val apiKey: String,

    @Value("\${yandex.gpt.folder-id}")
    private val folderId: String,

    @Value("\${yandex.gpt.model-uri}")
    private val modelUri: String,

    @Value("\${yandex.gpt.structured-output-enabled:true}")
    private val structuredOutputEnabled: Boolean,

    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val webClient = WebClient.builder()
        .baseUrl("https://llm.api.cloud.yandex.net/foundationModels/v1/completion")
        .defaultHeader("Authorization", "Api-Key $apiKey")
        .defaultHeader("x-folder-id", folderId)
        .build()

    fun completeWithJson(
        systemPrompt: String,
        userPrompt: String,
        responseSchema: JsonNode?
    ): String {
        val requestBody = mutableMapOf<String, Any>(
            "modelUri" to modelUri,
            "completionOptions" to mapOf(
                "stream" to false,
                "temperature" to 0.2,
                "maxTokens" to 4000
            ),
            "messages" to listOf(
                mapOf(
                    "role" to "system",
                    "text" to systemPrompt
                ),
                mapOf(
                    "role" to "user",
                    "text" to userPrompt
                )
            )
        )

        if (structuredOutputEnabled && responseSchema != null) {
            requestBody["responseFormat"] = mapOf(
                "type" to "json_schema",
                "jsonSchema" to mapOf(
                    "name" to "plotmap_project_generation",
                    "schema" to responseSchema
                )
            )
        }

        val rawResponse = webClient.post()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchangeToMono { response ->
                response.bodyToMono(String::class.java).flatMap { body ->

                    if (response.statusCode().isError) {
                        return@flatMap Mono.error(
                            RuntimeException(
                                "YandexGPT error ${response.statusCode()}: $body"
                            )
                        )
                    }

                    Mono.just(body)
                }
            }
            .block()

        log.info("YandexGPT raw HTTP response: {}", rawResponse)

        val root = objectMapper.readTree(rawResponse)

        val text = root
            .path("result")
            .path("alternatives")
            .path(0)
            .path("message")
            .path("text")
            .asText(null)

        require(!text.isNullOrBlank()) {
            "YandexGPT returned empty message text"
        }

        return text
    }
}
