package com.plotmap.backend.service

import com.plotmap.backend.client.AiResponseParser
import com.plotmap.backend.client.YandexGptClient
import com.plotmap.backend.config.YandexGptProperties
import com.plotmap.backend.dto.ai.AiGraphResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AiGraphService(
    private val yandexGptClient: YandexGptClient,
    private val parser: AiResponseParser,
    private val validator: AiResponseValidator,
    private val properties: YandexGptProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun generateGraph(text: String): AiGraphResponse {

        if (text.isBlank()) {
            throw IllegalArgumentException("Text must not be empty")
        }
        if (text.length > properties.maxTextLength) {
            throw IllegalArgumentException(
                "Text is too long: ${text.length} characters. " +
                        "Maximum allowed: ${properties.maxTextLength}"
            )
        }

        log.info("Starting graph generation, text length: ${text.length}")
        val rawResponse = yandexGptClient.generateRawGraphJson(text)
        log.info("Received raw response from YandexGPT")
        val parsed = parser.parse(rawResponse)
        log.info(
            "Parsed AI response: ${parsed.events.size} events, " +
                    "${parsed.edges.size} edges, " +
                    "${parsed.characters.size} characters"
        )
        val validationResult = validator.validate(parsed)

        if (!validationResult.isValid) {
            val errorMsg = validationResult.errors.joinToString("; ")
            log.error("AI response validation failed: $errorMsg")
            throw IllegalStateException(
                "AI response is invalid: $errorMsg"
            )
        }
        log.info("Graph generation completed successfully")

        return validationResult.sanitized!!
    }
}
