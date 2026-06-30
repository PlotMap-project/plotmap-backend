package com.plotmap.backend.client

import tools.jackson.databind.ObjectMapper
import com.plotmap.backend.dto.ai.AiGraphResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AiResponseParser(
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun parse(rawText: String): AiGraphResponse {
        val cleaned = cleanRawText(rawText)

        return try {
            objectMapper.readValue(cleaned, AiGraphResponse::class.java)
        } catch (e: Exception) {
            log.error(
                "Failed to parse AI response: error={}, cleanedLength={}",
                e.message,
                cleaned.length
            )
            throw IllegalStateException(
                "AI returned invalid JSON that cannot be parsed: ${e.message}"
            )
        }
    }

    private fun cleanRawText(raw: String): String {
        var result = raw.trim()

        if (result.startsWith("```")) {
            result = result
                .removePrefix("```json")
                .removePrefix("```")
                .trim()
        }
        if (result.endsWith("```")) {
            result = result.removeSuffix("```").trim()
        }

        val jsonStart = result.indexOf('{')
        val jsonEnd = result.lastIndexOf('}')

        if (jsonStart == -1 || jsonEnd == -1) {
            throw IllegalStateException("AI response does not contain valid JSON object")
        }

        return result.substring(jsonStart, jsonEnd + 1)
    }
}
