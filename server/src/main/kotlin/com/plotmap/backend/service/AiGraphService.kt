package com.plotmap.backend.service

import com.plotmap.backend.client.AiResponseParser
import com.plotmap.backend.client.YandexGptClient
import com.plotmap.backend.dto.ai.AiCharacterDto
import com.plotmap.backend.dto.ai.AiEdgeDto
import com.plotmap.backend.dto.ai.AiEventDto
import com.plotmap.backend.dto.ai.AiGraphResponse
import com.plotmap.backend.dto.ai.AiStoryArcDto
import com.plotmap.backend.exception.ContentFilteredException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AiGraphService(
    private val yandexGptClient: YandexGptClient,
    private val parser: AiResponseParser,
    private val validator: AiResponseValidator,
    private val textChunkingService: TextChunkingService
) {

    companion object {
        private const val MIN_RECOVERY_CHUNK_SIZE = 2000
    }

    private val log = LoggerFactory.getLogger(javaClass)

    fun generateGraph(text: String): AiGraphResponse {
        require(text.isNotBlank()) { "Text must not be empty" }

        if (!textChunkingService.needsChunking(text)) {
            log.info("Text fits in single request ({} chars)", text.length)
            return generateSingle(text)
        }

        val chunks = textChunkingService.chunk(text)
        log.info("Text chunked into {} parts (total {} chars)", chunks.size, text.length)

        val allResults = mutableListOf<AiGraphResponse>()

        chunks.forEachIndexed { index, chunk ->
            log.info("Processing chunk {}/{} ({} chars)", index + 1, chunks.size, chunk.length)

            try {
                allResults += generateSingle(chunk)
            } catch (e: ContentFilteredException) {
                log.warn("Chunk {} was filtered by model. Trying recovery split...", index + 1)
                val recoveredResults = tryRecoverFilteredChunk(chunk, index + 1)
                allResults += recoveredResults
            } catch (e: Exception) {
                log.error("Chunk {}/{} failed: {}", index + 1, chunks.size, e.message, e)
                throw e
            }
        }

        if (allResults.isEmpty()) {
            throw IllegalStateException("All chunks failed or were filtered")
        }

        return mergeChunkResults(allResults)
    }

    private fun generateSingle(text: String): AiGraphResponse {
        val rawResponse = yandexGptClient.generateRawGraphJson(text)
        log.info("Received raw response from YandexGPT")

        val parsed = parser.parse(rawResponse)
        log.info(
            "Parsed AI response: {} events, {} edges, {} characters",
            parsed.events.size, parsed.edges.size, parsed.characters.size
        )

        val validationResult = validator.validate(parsed)

        if (!validationResult.isValid) {
            val errorMsg = validationResult.errors.joinToString("; ")
            log.error("AI response validation failed: {}", errorMsg)
            throw IllegalStateException("AI response is invalid: $errorMsg")
        }

        return validationResult.sanitized!!
    }

    private fun tryRecoverFilteredChunk(chunk: String, chunkIndex: Int): List<AiGraphResponse> {
        if (chunk.length <= MIN_RECOVERY_CHUNK_SIZE) {
            log.warn(
                "Chunk {} is too small for further split and will be skipped after content filter",
                chunkIndex
            )
            return emptyList()
        }

        val splitIndex = findRecoverySplitIndex(chunk)
        val left = chunk.substring(0, splitIndex).trim()
        val right = chunk.substring(splitIndex).trim()

        val recoveredResults = mutableListOf<AiGraphResponse>()

        listOf(left, right).forEachIndexed { subIndex, subChunk ->
            if (subChunk.isBlank()) return@forEachIndexed

            try {
                log.info(
                    "Trying recovery subchunk {}.{} ({} chars)",
                    chunkIndex,
                    subIndex + 1,
                    subChunk.length
                )
                recoveredResults += generateSingle(subChunk)
            } catch (e: ContentFilteredException) {
                log.warn(
                    "Recovery subchunk {}.{} was also filtered and will be skipped",
                    chunkIndex,
                    subIndex + 1
                )
            } catch (e: Exception) {
                log.error(
                    "Recovery subchunk {}.{} failed unexpectedly",
                    chunkIndex,
                    subIndex + 1,
                    e
                )
            }
        }

        return recoveredResults
    }

    private fun findRecoverySplitIndex(chunk: String): Int {
        val middle = chunk.length / 2

        val paragraphSplit = chunk.lastIndexOf("\n\n", middle)
        if (paragraphSplit > chunk.length / 4) {
            return paragraphSplit + 2
        }

        val sentenceSplit = chunk.lastIndexOf(". ", middle)
        if (sentenceSplit > chunk.length / 4) {
            return sentenceSplit + 2
        }

        val lineSplit = chunk.lastIndexOf("\n", middle)
        if (lineSplit > chunk.length / 4) {
            return lineSplit + 1
        }

        return middle
    }

    private fun mergeChunkResults(results: List<AiGraphResponse>): AiGraphResponse {
        val allEvents = mutableListOf<AiEventDto>()
        val allEdges = mutableListOf<AiEdgeDto>()
        val characterMap = mutableMapOf<String, AiCharacterDto>()
        val arcMap = mutableMapOf<String, AiStoryArcDto>()

        var eventOffset = 0
        var charOffset = 0
        var arcOffset = 0

        results.forEach { result ->
            val eventIdRemap = mutableMapOf<String, String>()
            val charIdRemap = mutableMapOf<String, String>()
            val arcIdRemap = mutableMapOf<String, String>()

            result.characters.forEach { char ->
                val normalizedName = char.name.normalizeKey()
                val existing = characterMap.entries.find {
                    it.value.name.normalizeKey() == normalizedName
                }

                if (existing != null) {
                    charIdRemap[char.id] = existing.key
                } else {
                    charOffset++
                    val newId = "char_merged_$charOffset"
                    charIdRemap[char.id] = newId
                    characterMap[newId] = char.copy(id = newId)
                }
            }

            result.storyArcs.forEach { arc ->
                val normalizedTitle = arc.name.normalizeKey()
                val existing = arcMap.entries.find {
                    it.value.name.normalizeKey() == normalizedTitle
                }

                if (existing != null) {
                    arcIdRemap[arc.id] = existing.key
                } else {
                    arcOffset++
                    val newId = "arc_merged_$arcOffset"
                    arcIdRemap[arc.id] = newId
                    arcMap[newId] = arc.copy(id = newId)
                }
            }

            result.events.forEach { event ->
                eventOffset++
                val newId = "event_merged_$eventOffset"
                eventIdRemap[event.id] = newId

                allEvents += event.copy(
                    id = newId,
                    characterIds = event.characterIds.map { charIdRemap[it] ?: it },
                    storyArcIds = event.storyArcIds.map { arcIdRemap[it] ?: it }
                )
            }

            result.edges.forEach { edge ->
                val newSource = eventIdRemap[edge.sourceEventId]
                val newTarget = eventIdRemap[edge.targetEventId]

                if (newSource != null && newTarget != null) {
                    allEdges += edge.copy(
                        sourceEventId = newSource,
                        targetEventId = newTarget
                    )
                }
            }
        }

        log.info(
            "Merged {} chunks: {} events, {} edges, {} characters, {} arcs",
            results.size,
            allEvents.size,
            allEdges.size,
            characterMap.size,
            arcMap.size
        )

        return AiGraphResponse(
            events = allEvents,
            edges = allEdges,
            characters = characterMap.values.toList(),
            storyArcs = arcMap.values.toList()
        )
    }

    private fun String.normalizeKey(): String {
        return this.trim().lowercase().replace(Regex("\\s+"), " ")
    }
}
