package com.plotmap.backend.dto.ai

data class AiGeneratedEventRawDto(
    val localId: String,
    val title: String,
    val description: String,
    val impactLevel: Int,
    val level: Int,
    val orderInLevel: Int
)

data class AiGeneratedConnectionRawDto(
    val sourceLocalId: String,
    val targetLocalId: String,
    val type: String
)

data class AiGeneratedProjectRawResponse(
    val events: List<AiGeneratedEventRawDto>,
    val connections: List<AiGeneratedConnectionRawDto>
)
