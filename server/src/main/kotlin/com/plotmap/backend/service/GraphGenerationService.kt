package com.plotmap.backend.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.plotmap.backend.client.YandexGptClient
import com.plotmap.backend.dto.ai.AiGeneratedProjectRawResponse
import com.plotmap.backend.dto.request.GenerateProjectRequest
import com.plotmap.backend.dto.response.GenerateProjectResponse
import com.plotmap.backend.dto.response.GeneratedConnectionDto
import com.plotmap.backend.dto.response.GeneratedEventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GraphGenerationService(
    private val yandexGptClient: YandexGptClient,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val allowedConnectionTypes = setOf(
        "casual",
        "temporal",
        "parallel",
        "contradiction",
    )

    fun generateProject(request: GenerateProjectRequest): GenerateProjectResponse {
        require(request.text.isNotBlank()) {
            "Text must not be empty"
        }

        val systemPrompt = buildSystemPrompt()
        val userPrompt = buildUserPrompt(request)
        val responseSchema = buildResponseSchema()

        val rawJson = yandexGptClient.completeWithJson(
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            responseSchema = responseSchema
        )

        log.info("YandexGPT extracted message text: {}", rawJson)

        val rawResponse = parseRawResponse(rawJson)

        validateRawResponse(rawResponse)

        return mapToPublicResponse(rawResponse)
    }

    private fun parseRawResponse(rawJson: String): AiGeneratedProjectRawResponse {
        val cleanedJson = rawJson
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return objectMapper.readValue(
            cleanedJson,
            AiGeneratedProjectRawResponse::class.java
        )
    }

    private fun validateRawResponse(raw: AiGeneratedProjectRawResponse) {
        require(raw.events.isNotEmpty()) {
            "Generated project must contain at least one event"
        }

        val localIds = raw.events.map { it.localId }
        require(localIds.toSet().size == localIds.size) {
            "Generated events contain duplicate localId values"
        }

        raw.events.forEach { event ->
            require(event.localId.isNotBlank()) {
                "Event localId must not be blank"
            }
            require(event.title.isNotBlank()) {
                "Event title must not be blank"
            }
            require(event.description.isNotBlank()) {
                "Event description must not be blank"
            }
            require(event.impactLevel in 1..10) {
                "Event impactLevel must be between 1 and 10"
            }
            require(event.level >= 0) {
                "Event level must be >= 0"
            }
            require(event.orderInLevel >= 0) {
                "Event orderInLevel must be >= 0"
            }
        }

        val eventIdSet = raw.events.map { it.localId }.toSet()

        raw.connections.forEach { connection ->
            require(connection.sourceLocalId in eventIdSet) {
                "Connection sourceLocalId ${connection.sourceLocalId} does not exist"
            }
            require(connection.targetLocalId in eventIdSet) {
                "Connection targetLocalId ${connection.targetLocalId} does not exist"
            }
            require(connection.type in allowedConnectionTypes) {
                "Connection type ${connection.type} is not allowed"
            }
        }
    }

    private fun mapToPublicResponse(raw: AiGeneratedProjectRawResponse): GenerateProjectResponse {
        val localIdToRealId = raw.events.associate { event ->
            event.localId to UUID.randomUUID().toString()
        }

        val events = raw.events.map { event ->
            GeneratedEventDto(
                id = localIdToRealId.getValue(event.localId),
                title = event.title,
                description = event.description,
                impactLevel = event.impactLevel,
                level = event.level,
                orderInLevel = event.orderInLevel
            )
        }

        val connections = raw.connections.map { connection ->
            GeneratedConnectionDto(
                sourceEventId = localIdToRealId.getValue(connection.sourceLocalId),
                targetEventId = localIdToRealId.getValue(connection.targetLocalId),
                type = connection.type
            )
        }

        return GenerateProjectResponse(
            events = events,
            connections = connections
        )
    }

    private fun buildSystemPrompt(): String = """
        Ты — аналитик художественного сюжета.
        Твоя задача — выделить из текста крупные сюжетные события и связи между ними.

        Верни результат СТРОГО в JSON.
        Не добавляй markdown.
        Не добавляй пояснения.
        Не добавляй текст до или после JSON.

        Правила:
        1. Выделяй только значимые сюжетные события, не слишком мелкие детали.
        2. Используй локальные id событий в формате event_1, event_2, event_3 и т.д.
        3. impactLevel — целое число от 1 до 10.
        4. level — уровень сюжетного развития:
           0 = начало / завязка
           1 = развитие
           2 = дальнейшее развитие / поворот
           3 = кульминация / развязка
        5. orderInLevel — порядок события внутри одного уровня, начиная с 0.
        6. Используй только следующие типы связей:
           casual, temporal, parallel, contradiction
        7. sourceLocalId и targetLocalId должны ссылаться только на реально существующие события.
        8. Если событие одно — connections может быть пустым массивом.
    """.trimIndent()

    private fun buildUserPrompt(request: GenerateProjectRequest): String = """
        Название проекта: ${request.name}
        Описание проекта: ${request.description}

        Текст:
        ${request.text}
    """.trimIndent()

    private fun buildResponseSchema(): JsonNode {
        val schemaJson = """
            {
              "type": "object",
              "additionalProperties": false,
              "required": ["events", "connections"],
              "properties": {
                "events": {
                  "type": "array",
                  "minItems": 1,
                  "items": {
                    "type": "object",
                    "additionalProperties": false,
                    "required": [
                      "localId",
                      "title",
                      "description",
                      "impactLevel",
                      "level",
                      "orderInLevel"
                    ],
                    "properties": {
                      "localId": {
                        "type": "string"
                      },
                      "title": {
                        "type": "string"
                      },
                      "description": {
                        "type": "string"
                      },
                      "impactLevel": {
                        "type": "integer",
                        "minimum": 1,
                        "maximum": 10
                      },
                      "level": {
                        "type": "integer",
                        "minimum": 0
                      },
                      "orderInLevel": {
                        "type": "integer",
                        "minimum": 0
                      }
                    }
                  }
                },
                "connections": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "additionalProperties": false,
                    "required": [
                      "sourceLocalId",
                      "targetLocalId",
                      "type"
                    ],
                    "properties": {
                      "sourceLocalId": {
                        "type": "string"
                      },
                      "targetLocalId": {
                        "type": "string"
                      },
                      "type": {
                        "type": "string",
                        "enum": [
                          "casual",
                          "temporal",
                          "parallel",
                          "contradiction"
                        ]
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        return objectMapper.readTree(schemaJson)
    }
}
