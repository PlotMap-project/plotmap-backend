package com.plotmap.backend.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.plotmap.backend.config.YandexGptProperties
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

    private val httpClient = HttpClient.newBuilder().build()

    fun generateRawGraphJson(text: String): String {
        require(text.isNotBlank()) { "Text must not be empty" }
        require(properties.apiKey.isNotBlank()) { "Yandex GPT API key is not configured" }
        require(properties.folderId.isNotBlank()) { "Yandex GPT folder id is not configured" }
        require(properties.modelUri.isNotBlank()) { "Yandex GPT model uri is not configured" }

        val requestBody = mapOf(
            "modelUri" to properties.modelUri,
            "completionOptions" to mapOf(
                "stream" to false,
                "temperature" to 0.3,
                "maxTokens" to "4000"
            ),
            "messages" to listOf(
                mapOf(
                    "role" to "system",
                    "text" to buildSystemPrompt()
                ),
                mapOf(
                    "role" to "user",
                    "text" to text
                )
            )
        )

        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://llm.api.cloud.yandex.net/foundationModels/v1/completion"))
            .header("Authorization", "Api-Key ${properties.apiKey}")
            .header("x-folder-id", properties.folderId)
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
            throw IllegalStateException(
                "Yandex GPT request failed: ${response.statusCode()} ${response.body()}"
            )
        }

        val root = objectMapper.readTree(response.body())

        return root.path("result")
            .path("alternatives")
            .path(0)
            .path("message")
            .path("text")
            .asText()
            .trim()
    }

    private fun buildSystemPrompt(): String {
        return """
        Ты — литературный аналитик. Проанализируй художественный текст и извлеки из него сюжетный граф.

        Верни ТОЛЬКО валидный JSON.
        Не используй markdown.
        Не добавляй пояснения.
        Не добавляй текст до JSON или после JSON.
        Не добавляй никакие поля, которых нет в схеме ниже.

        Схема ответа:
        {
          "events": [
            {
              "id": "event_1",
              "title": "string",
              "description": "string",
              "suggestedSystemRole": "INCITING_INCIDENT|RISING_ACTION|CLIMAX|FALLING_ACTION|RESOLUTION|PLOT_TWIST|REGULAR",
              "impactLevel": 1,
              "level": 0,
              "orderInLevel": 0,
              "color": "#FAFAD2",
              "characterIds": ["char_1"],
              "storyArcIds": ["arc_1"]
            }
          ],
          "edges": [
            {
              "sourceEventId": "event_1",
              "targetEventId": "event_2",
              "type": "CAUSAL|TEMPORAL|PARALLEL|CONTRADICTION",
              "strength": 5
            }
          ],
          "characters": [
            {
              "id": "char_1",
              "name": "string",
              "description": "string"
            }
          ],
          "storyArcs": [
            {
              "id": "arc_1",
              "name": "string",
              "description": "string"
            }
          ]
        }

        Обязательные правила:
        - suggestedSystemRole строго одно из: INCITING_INCIDENT, RISING_ACTION, CLIMAX, FALLING_ACTION, RESOLUTION, PLOT_TWIST, REGULAR
        - type строго одно из: CAUSAL, TEMPORAL, PARALLEL, CONTRADICTION
        - impactLevel — целое число от 1 до 10, показатель влияния события на сюжет
        - level — целое число, временной слой события (0 — самые ранние, чем больше — тем позже)
        - события, происходящие параллельно или независимо друг от друга, должны иметь одинаковый level
        - orderInLevel — порядок события внутри своего level, начинается с 0
        - в каждом level orderInLevel начинается с 0 и идёт без пропусков: 0, 1, 2, ...
        - color — выбери СТРОГО из набора: #FAFAD2, #FFEFD5, #FFE4B5, #FFDAB9, #EEE8AA
        - если не уверен в цвете — используй #FAFAD2
        - цвет может отражать сюжетную арку или роль события
        - strength — целое число от 1 до 10, показатель степени данной связи между событиями
        - id событий: event_1, event_2, event_3, ...
        - id персонажей: char_1, char_2, ...
        - id арок: arc_1, arc_2, ...
        - sourceEventId и targetEventId должны ссылаться только на существующие events.id
        - не создавай self-loop: sourceEventId не должен совпадать с targetEventId
        - в characterIds каждого event указывай ТОЛЬКО id из массива characters
        - в storyArcIds каждого event указывай ТОЛЬКО id из массива storyArcs
        - если в событии нет персонажей, characterIds должен быть пустым массивом []
        - если событие не относится ни к одной арке, storyArcIds должен быть пустым массивом []
        - выделяй только значимые события сюжета
        - создай от 5 до 12 событий, если текст достаточно содержательный
        - каждое событие желательно связать хотя бы одним ребром
        - выдели всех значимых персонажей и помести их в characters
        - выдели сюжетные арки (основные сюжетные линии) и помести их в storyArcs

        Запрещено:
        - не добавляй никакие другие поля вне указанной схемы

        Верни только JSON по этой схеме.
    """.trimIndent()
    }
}
