package com.plotmap.backend.dto.ai

data class AiGraphResponse(
    val events: List<AiEventDto> = emptyList(),
    val edges: List<AiEdgeDto> = emptyList(),
    val characters: List<AiCharacterDto> = emptyList(),
    val storyArcs: List<AiStoryArcDto> = emptyList()
)

data class AiEventDto(
    val id: String,
    val title: String,
    val description: String = "",
    val suggestedSystemRole: String = "REGULAR",
    val impactLevel: Int = 5
)

data class AiEdgeDto(
    val sourceEventId: String,
    val targetEventId: String,
    val type: String = "TEMPORAL",
    val strength: Int = 5
)

data class AiCharacterDto(
    val id: String,
    val name: String,
    val description: String = ""
)

data class AiStoryArcDto(
    val id: String,
    val name: String,
    val description: String = ""
)
