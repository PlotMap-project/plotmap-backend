package com.plotmap.backend.dto.response

data class GeneratedEventDto(
    val id: String,
    val title: String,
    val description: String,
    val impactLevel: Int,
    val level: Int,
    val orderInLevel: Int
)

data class GeneratedConnectionDto(
    val sourceEventId: String,
    val targetEventId: String,
    val type: String
)

data class GenerateProjectResponse(
    val events: List<GeneratedEventDto>,
    val connections: List<GeneratedConnectionDto>
)
