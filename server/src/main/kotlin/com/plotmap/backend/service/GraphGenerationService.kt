package com.plotmap.backend.service

import com.plotmap.backend.dto.request.GenerateProjectRequest
import com.plotmap.backend.dto.response.GenerateProjectResponse
import com.plotmap.backend.dto.response.GeneratedConnectionDto
import com.plotmap.backend.dto.response.GeneratedEventDto
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GraphGenerationService {

    fun generateGraph(
        projectId: UUID,
        request: GenerateProjectRequest
    ): GenerateProjectResponse {
        require(request.text.isNotBlank()) { "Text must not be empty" }

        val eventId1 = UUID.randomUUID().toString()
        val eventId2 = UUID.randomUUID().toString()
        val eventId3 = UUID.randomUUID().toString()

        val events = listOf(
            GeneratedEventDto(
                id = eventId1,
                title = "Начало истории",
                description = "Система выделила завязку сюжета из текста проекта \"${request.name}\"",
                impactLevel = 7,
                level = 0,
                orderInLevel = 0
            ),
            GeneratedEventDto(
                id = eventId2,
                title = "Развитие конфликта",
                description = "Главный конфликт начинает развиваться",
                impactLevel = 8,
                level = 1,
                orderInLevel = 0
            ),
            GeneratedEventDto(
                id = eventId3,
                title = "Параллельное событие",
                description = "Дополнительная сюжетная линия",
                impactLevel = 5,
                level = 1,
                orderInLevel = 1
            )
        )

        val connections = listOf(
            GeneratedConnectionDto(
                sourceEventId = eventId1,
                targetEventId = eventId2,
                type = "causes"
            ),
            GeneratedConnectionDto(
                sourceEventId = eventId1,
                targetEventId = eventId3,
                type = "parallel"
            )
        )

        return GenerateProjectResponse(
            events = events,
            connections = connections
        )
    }
}
