package com.plotmap.backend.dto.response

import java.time.Instant

data class EventDto(
    val id: String,
    val title: String,
    val description: String,
    val suggestedSystemRole: String?,
    val impactLevel: Int,
    val status: String,
    val userNotes: String,
    val level: Int,
    val orderInLevel: Int,
    val customPositionX: Double?,
    val customPositionY: Double?,
    val color: String?,
    val source: String,
    val sourceContext: String,
    val characterIds: List<String>,
    val tagIds: List<String>,
    val storyArcIds: List<String>,
    val createdAt: Instant
)
