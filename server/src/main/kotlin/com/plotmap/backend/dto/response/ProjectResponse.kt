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
    val events: List<EventDto>,
    val connections: List<ConnectionDto>,
    val characters: List<CharacterDto>,
    val storyArcs: List<StoryArcDto>,
    val tags: List<TagDto>,
    val createdAt: Instant
)

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

data class ConnectionDto(
    val id: String,
    val sourceEventId: String,
    val targetEventId: String,
    val type: String,
    val description: String
)

data class CharacterDto(
    val id: String,
    val name: String,
    val description: String,
    val role: String,
    val color: String?
)

data class StoryArcDto(
    val id: String,
    val title: String,
    val description: String,
    val color: String?
)

data class TagDto(
    val id: String,
    val name: String,
    val color: String?
)

data class ChapterDto(
    val id: String,
    val chapterOrder: Int,
    val title: String?,
    val createdAt: Instant
)
