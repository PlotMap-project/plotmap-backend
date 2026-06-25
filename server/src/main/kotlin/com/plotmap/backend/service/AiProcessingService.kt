package com.plotmap.backend.service

import com.plotmap.backend.dto.ai.AiGraphResponse
import com.plotmap.backend.model.entity.Character
import com.plotmap.backend.model.entity.Event
import com.plotmap.backend.model.entity.EventEdge
import com.plotmap.backend.model.entity.EventToCharacter
import com.plotmap.backend.model.entity.StoryArc
import com.plotmap.backend.model.entity.StoryArcToEvent
import com.plotmap.backend.model.enum.CharacterRole
import com.plotmap.backend.model.enum.ConnectionType
import com.plotmap.backend.model.enum.EventSource
import com.plotmap.backend.model.enum.EventStatus
import com.plotmap.backend.model.enum.SystemEventRole
import com.plotmap.backend.repository.jpa.CharacterRepository
import com.plotmap.backend.repository.jpa.EventEdgeRepository
import com.plotmap.backend.repository.jpa.EventRepository
import com.plotmap.backend.repository.jpa.EventToCharacterRepository
import com.plotmap.backend.repository.jpa.StoryArcRepository
import com.plotmap.backend.repository.jpa.StoryArcToEventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AiProcessingService(
    private val eventRepository: EventRepository,
    private val eventEdgeRepository: EventEdgeRepository,
    private val characterRepository: CharacterRepository,
    private val eventToCharacterRepository: EventToCharacterRepository,
    private val storyArcRepository: StoryArcRepository,
    private val storyArcToEventRepository: StoryArcToEventRepository
) {

    @Transactional
    fun saveInitialGeneration(projectId: UUID, graph: AiGraphResponse) {
        saveGraphDelta(projectId, graph)
    }

    @Transactional
    fun appendChapterGeneration(projectId: UUID, graph: AiGraphResponse) {
        saveGraphDelta(projectId, graph)
    }

    private fun saveGraphDelta(projectId: UUID, graph: AiGraphResponse) {
        val characterIdMap = mutableMapOf<String, UUID>()
        val storyArcIdMap = mutableMapOf<String, UUID>()
        val eventIdMap = mutableMapOf<String, UUID>()

        graph.characters.forEach { aiCharacter ->
            val saved = characterRepository.save(
                Character(
                    projectId = projectId,
                    name = aiCharacter.name.trim(),
                    description = aiCharacter.description.trim(),
                    role = parseCharacterRole(aiCharacter.role)
                )
            )
            characterIdMap[aiCharacter.id] = saved.id
        }

        graph.storyArcs.forEach { aiArc ->
            val saved = storyArcRepository.save(
                StoryArc(
                    projectId = projectId,
                    title = aiArc.name.trim(),
                    description = aiArc.description.trim()
                )
            )
            storyArcIdMap[aiArc.id] = saved.id
        }

        graph.events.forEach { aiEvent ->
            val saved = eventRepository.save(
                Event(
                    projectId = projectId,
                    title = aiEvent.title.trim(),
                    description = aiEvent.description.trim(),
                    suggestedSystemRole = parseSystemRole(aiEvent.suggestedSystemRole),
                    status = EventStatus.IMPLEMENTED,
                    impactLevel = aiEvent.impactLevel.coerceIn(1, 10),
                    level = aiEvent.level.coerceAtLeast(0),
                    orderInLevel = aiEvent.orderInLevel.coerceAtLeast(0),
                    color = normalizeColor(aiEvent.color),
                    source = EventSource.AI_GENERATED,
                    sourceContext = aiEvent.sourceContext.trim()
                )
            )
            eventIdMap[aiEvent.id] = saved.id
        }

        val eventToCharacters = graph.events.flatMap { aiEvent ->
            val savedEventId = eventIdMap[aiEvent.id] ?: return@flatMap emptyList()

            aiEvent.characterIds.mapNotNull { characterId ->
                val savedCharacterId = characterIdMap[characterId] ?: return@mapNotNull null
                EventToCharacter(
                    idProject = projectId,
                    idEvent = savedEventId,
                    idCharacter = savedCharacterId
                )
            }
        }
        eventToCharacterRepository.saveAll(eventToCharacters)

        val storyArcToEvents = graph.events.flatMap { aiEvent ->
            val savedEventId = eventIdMap[aiEvent.id] ?: return@flatMap emptyList()

            aiEvent.storyArcIds.mapNotNull { arcId ->
                val savedArcId = storyArcIdMap[arcId] ?: return@mapNotNull null
                StoryArcToEvent(
                    idProject = projectId,
                    idArc = savedArcId,
                    idEvent = savedEventId
                )
            }
        }
        storyArcToEventRepository.saveAll(storyArcToEvents)

        val savedEdges = graph.edges.mapNotNull { aiEdge ->
            val savedSourceId = eventIdMap[aiEdge.sourceEventId] ?: return@mapNotNull null
            val savedTargetId = eventIdMap[aiEdge.targetEventId] ?: return@mapNotNull null

            EventEdge(
                idProject = projectId,
                sourceEventId = savedSourceId,
                targetEventId = savedTargetId,
                type = parseConnectionType(aiEdge.type),
                description = aiEdge.description.trim()
            )
        }
        eventEdgeRepository.saveAll(savedEdges)
    }

    private fun parseSystemRole(value: String?): SystemEventRole? {
        if (value.isNullOrBlank()) return null
        return try {
            SystemEventRole.valueOf(value.trim())
        } catch (_: IllegalArgumentException) {
            SystemEventRole.REGULAR
        }
    }

    private fun parseConnectionType(value: String?): ConnectionType {
        if (value.isNullOrBlank()) return ConnectionType.TEMPORAL
        return try {
            ConnectionType.valueOf(value.trim())
        } catch (_: IllegalArgumentException) {
            ConnectionType.TEMPORAL
        }
    }

    private fun normalizeColor(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val trimmed = value.trim()
        return if (Regex("^#[0-9A-Fa-f]{6}$").matches(trimmed)) trimmed else null
    }

    private fun parseCharacterRole(value: String?): CharacterRole {
        if (value.isNullOrBlank()) return CharacterRole.SUPPORTING
        return try {
            CharacterRole.valueOf(value.trim())
        } catch (_: IllegalArgumentException) {
            CharacterRole.SUPPORTING
        }
    }
}
