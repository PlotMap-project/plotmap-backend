package com.plotmap.backend.dto.response

import java.time.Instant

data class ProjectResponse(
    val id: String,
    val title: String,
    val type: String,
    val description: String,
    val createdAt: Instant
)

data class ProjectDetailResponse(
    val id: String,
    val title: String,
    val type: String,
    val description: String,
    val sourceText: String?,
    val events: List<GeneratedEventDto>,
    val connections: List<GeneratedConnectionDto>,
    val createdAt: Instant
)
