package com.plotmap.backend.dto.response

import java.time.Instant

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
